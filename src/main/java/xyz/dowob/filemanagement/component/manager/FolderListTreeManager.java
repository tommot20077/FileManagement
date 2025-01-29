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
    private final UserRepository userRepository;
    private final FolderListTreeProvider folderListTreeProvider;
    private final FileService fileService;

    @PostConstruct
    public void init() {
        initializeTree();
    }

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
