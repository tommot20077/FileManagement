package xyz.dowob.filemanagement.component.manager;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;

/**
 * 用戶檔案列表樹管理器，用於初始化用戶的檔案列表樹
 * 當啟用用戶檔案列表樹時，會在啟動時初始化用戶的檔案列表樹
 * 此配置在配置文件中設置 file.global.enable-user-folder-list-tree，默認為 true
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileListTreeManager
 * @create 2025/1/31
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"file.global.enable-user-folder-list-tree"}, havingValue = "true", matchIfMissing = true)
public class FolderListTreeManager {
    /**
     * 用戶資料庫操作類
     */
    private final UserRepository userRepository;
    /**
     * 用戶檔案列表樹提供者
     */
    private final FolderListTreeProvider folderListTreeProvider;
    /**
     * 檔案服務類
     */
    private final FileService fileService;

    /**
     * 初始化用戶的檔案列表樹
     */
    @PostConstruct
    public void init() {
        initializeTree();
    }

    /**
     * 初始化用戶的檔案列表樹，當用戶ID為空時，初始化所有用戶的檔案列表樹，否則初始化指定用戶的檔案列表樹
     *
     * @param userIds 用戶ID
     */
    public void initializeTree(Long... userIds) {
        folderListTreeProvider.getUserFileListTree().clear();
        Flux<User> userMono;
        if (userIds.length == 0) {
            userMono = userRepository.findAll();
        } else {
            userMono = userRepository.findAllById(Flux.fromArray(userIds));
        }
        userMono.flatMap(user -> fileService.getUserFileList(user, -1L).collectList().flatMap(userFileListDTOS -> {
            try {
                folderListTreeProvider.initializeTree(user.getId(), userFileListDTOS);
                return Mono.empty();
            } catch (ProcessException e) {
                return Mono.error(new RuntimeException(e));
            }
        })).subscribe();
    }
}
