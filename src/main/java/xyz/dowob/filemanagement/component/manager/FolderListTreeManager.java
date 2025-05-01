package xyz.dowob.filemanagement.component.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.unity.DynamicThreadPoolExecutor;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
     * 動態線程池執行器
     */
    private DynamicThreadPoolExecutor dynamicThreadPoolExecutor;

    /**
     * 初始化用戶的檔案列表樹
     *
     * @param args 啟動參數
     */
    @Override
    public void run(ApplicationArguments args) {
        LogUnity.info("初始化用戶的檔案列表樹");
        dynamicThreadPoolExecutor = new DynamicThreadPoolExecutor(2, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1));
        initializeTree();
    }


    /**
     * 初始化用戶的檔案列表樹，當用戶ID為空時，初始化所有用戶的檔案列表樹，否則初始化指定用戶的檔案列表樹
     *
     * @param userIds 用戶ID
     */
    @RecordLevel(LogLevelEnum.INFO)
    public void initializeTree(Long... userIds) {
        Flux<User> userFlux = userIds.length == 0 ? userRepository.findAll() : userRepository.findAllById(Flux.fromArray(userIds));

        userFlux.flatMap(user -> {
            folderListTreeProvider
                    .getUserFileListTree()
                    .computeIfPresent(user.getId(), (id, node) -> folderListTreeProvider.getUserFileListTree().remove(id));
            return fetchAllUserFiles(user).flatMap(pageList -> {
                dynamicThreadPoolExecutor.submit(() -> {
                    try {
                        boolean isLastPage = pageList.getCurrentPage() == pageList.getTotalPages();
                        folderListTreeProvider.initializeTree(user.getId(), pageList.getData(), isLastPage);
                    } catch (Exception e) {
                        LogUnity.error("初始化用戶 %s 的檔案列表樹失敗", e, user.getId());
                    }
                });
                return Mono.just(user);
            });
        }).subscribe();
    }


    /**
     * 獲取所有用戶的檔案列表
     *
     * @param user 用戶
     *
     * @return 返回所有用戶的檔案列表
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    private Flux<PagedResponseDTO<UserFileListDTO>> fetchAllUserFiles(User user) {
        int pageSize = fileProperties.getGlobal().getPageSize();
        FileFilterDTO fileFilterDTO = FileFilterDTO
                .builder()
                .folderId(ReservedSearchIdEnum.ALL_FILE_ID.getId())
                .pageSize(pageSize)
                .types(Collections.singletonList(FileEnum.FOLDER))
                .build();

        return fileServiceStrategy.getFileService().getUserFileList(user, fileFilterDTO).expand(pagedResponseDTO -> {
            int nextPage = pagedResponseDTO.getCurrentPage() + 1;
            fileFilterDTO.setPage(nextPage);
            return nextPage <= pagedResponseDTO.getTotalPages() ? (fileServiceStrategy
                    .getFileService()
                    .getUserFileList(user, fileFilterDTO)) : Mono.empty();
        }).limitRate(20).takeUntil(pagedResponseDTO -> pagedResponseDTO.getCurrentPage() == pagedResponseDTO.getTotalPages());
    }
}
