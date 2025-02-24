package xyz.dowob.filemanagement.component.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.repostiory.UserRepository;

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
@Log4j2
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"file.global.enable-user-folder-list-tree"}, havingValue = "true", matchIfMissing = true)
public class FolderListTreeManager implements ApplicationRunner {
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
    private final FileServiceStrategy fileServiceStrategy;

    /**
     * 檔案屬性
     */
    private final FileProperties fileProperties;

    /**
     * 初始化用戶的檔案列表樹
     *
     * @param args 啟動參數
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("初始化用戶的檔案列表樹");
        initializeTree();
    }

    /**
     * 初始化用戶的檔案列表樹，當用戶ID為空時，初始化所有用戶的檔案列表樹，否則初始化指定用戶的檔案列表樹
     *
     * @param userIds 用戶ID
     */
    public void initializeTree(Long... userIds) {
        Flux<User> userMono = userIds.length == 0 ? userRepository.findAll() : userRepository.findAllById(Flux.fromArray(userIds));

        userMono
                .doOnNext(user -> folderListTreeProvider
                        .getUserFileListTree()
                        .computeIfPresent(user.getId(), (id, node) -> folderListTreeProvider.getUserFileListTree().remove(id)))
                .flatMap(user -> fetchAllUserFiles(user).doOnNext(pageList -> {

                    try {
                        folderListTreeProvider.initializeTree(user.getId(),
                                                              pageList.getData(),
                                                              pageList.getCurrentPage() == pageList.getTotalPages()
                        );
                    } catch (ProcessException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .subscribe();
    }

    private Flux<PagedResponseDTO<UserFileListDTO>> fetchAllUserFiles(User user) {
        int pageSize = fileProperties.getGlobal().getPageSize();
        return fileServiceStrategy
                .getFileService()
                .getUserFileList(user, ReservedSearchIdEnum.ALL_FILE_ID.getId(), 1, pageSize, null)
                .expand(pagedResponseDTO -> {
            int nextPage = pagedResponseDTO.getCurrentPage() + 1;
            return nextPage <= pagedResponseDTO.getTotalPages() ? (fileServiceStrategy
                    .getFileService().getUserFileList(user, ReservedSearchIdEnum.ALL_FILE_ID.getId(), nextPage, pageSize, null)) : Mono.empty();
        }).limitRate(20).takeUntil(pagedResponseDTO -> pagedResponseDTO.getCurrentPage() == pagedResponseDTO.getTotalPages());
    }
}
