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
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.unity.DynamicThreadPoolExecutor;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 基於響應式編程的用戶檔案目錄樹管理器，負責系統啟動時檔案目錄結構的初始化和維護。
 *
 * <p>此管理器在應用程式啟動時自動建構用戶的檔案目錄樹狀結構，使用非阻塞式處理支援大量併發操作。
 * 透過 {@link FolderListTreeProvider} 管理樹狀結構的快取和更新操作，採用分頁查詢機制處理大量檔案資料。
 * 可透過設定屬性 {@code file.global.enable-user-folder-list-tree} 控制是否啟用此功能，預設為啟用狀態。
 *
 * <p>實作採用動態線程池執行初始化任務，確保系統啟動效能不受影響。支援全量用戶初始化或指定用戶的增量初始化操作。
 * 初始化過程中會清除既有的目錄樹快取，重新建構完整的檔案結構樹。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see FolderListTreeProvider
 * @see ApplicationRunner
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"file.global.enable-user-folder-list-tree"}, havingValue = "true", matchIfMissing = true)
public class FolderListTreeManager implements ApplicationRunner {
    /**
     * 用戶資料存取儲存庫，提供用戶實體的查詢和操作介面。
     * 用於獲取需要初始化檔案目錄樹的用戶清單。
     */
    private final UserRepository userRepository;

    /**
     * 用戶檔案目錄樹提供者，負責管理檔案樹狀結構的快取和操作。
     * 提供目錄樹的初始化、更新和查詢功能。
     */
    private final FolderListTreeProvider folderListTreeProvider;

    /**
     * 檔案服務策略，提供檔案操作的統一介面。
     * 用於獲取用戶的檔案清單以建構目錄樹結構。
     */
    private final FileServiceStrategy fileServiceStrategy;

    /**
     * 檔案系統配置屬性，包含分頁大小等系統參數。
     * 用於控制檔案查詢的批次處理大小和相關設定。
     */
    private final FileProperties fileProperties;

    /**
     * 動態線程池執行器，用於並行處理目錄樹初始化任務。
     * 在應用程式啟動時創建，支援動態調整線程數量以優化效能。
     */
    private DynamicThreadPoolExecutor dynamicThreadPoolExecutor;


    /**
     * 應用程式啟動時執行的初始化方法，建立動態線程池並啟動檔案目錄樹初始化程序。
     *
     * <p>此方法為 {@link ApplicationRunner} 介面的實作，在 Spring Boot 應用程式完成基本初始化後自動執行。
     * 建立核心線程數為 2、最大線程數為 10 的動態線程池，並啟動全量用戶的檔案目錄樹初始化作業。
     *
     * @param args 應用程式啟動參數，包含命令列傳遞的參數資訊
     */
    @Override
    public void run(ApplicationArguments args) {
        LogUnity.info("初始化用戶的檔案列表樹");
        dynamicThreadPoolExecutor = new DynamicThreadPoolExecutor(2, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1024));
        initializeTree();
    }


    /**
     * 執行檔案目錄樹的初始化作業，支援全量或指定用戶的目錄樹建構。
     *
     * <p>當未提供用戶 ID 時，系統會查詢所有用戶並初始化其檔案目錄樹。若提供特定用戶 ID，
     * 則僅針對指定用戶執行初始化。初始化過程採用響應式非阻塞方式處理，先清除既有的目錄樹快取，
     * 然後分頁獲取用戶檔案資料並重新建構完整的樹狀結構。
     *
     * <p>處理流程包括：清除現有快取 → 分頁查詢檔案 → 並行建構目錄樹 → 異常處理與記錄。
     * 所有初始化任務均提交至動態線程池執行，避免阻塞主執行緒。
     *
     * @param userIds 需要初始化目錄樹的用戶 ID 陣列，若為空則初始化所有用戶
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
     * 分頁獲取指定用戶的所有資料夾檔案清單，用於建構檔案目錄樹結構。
     *
     * <p>此方法採用響應式分頁查詢機制，逐頁獲取用戶的資料夾類型檔案。查詢範圍為所有檔案區域，
     * 使用 {@code expand} 操作符實現自動分頁遍歷，直到獲取所有頁面的資料。透過限流機制控制
     * 併發查詢數量，避免對系統造成過大負載。
     *
     * <p>查詢條件固定為資料夾類型檔案，分頁大小由檔案配置屬性決定。方法會持續擴展查詢
     * 直到達到最後一頁，確保獲取用戶的完整資料夾結構資訊。
     *
     * @param user 需要查詢檔案清單的用戶實體
     * @return 包含分頁資料夾檔案清單的響應式流，每個元素代表一頁查詢結果
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
