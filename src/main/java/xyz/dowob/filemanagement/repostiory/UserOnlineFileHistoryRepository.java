package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.file.dao.OnlineHistoryCountAndOldestDAO;
import xyz.dowob.filemanagement.entity.UserOnlineFileHistory;

/**
 * 用戶線上檔案歷史資料存取層介面，提供檔案版本歷史的響應式資料庫操作功能。
 * <p>
 * 此介面繼承自 Spring Data R2DBC 的 {@link ReactiveCrudRepository}，
 * 專門用於管理線上檔案的版本歷史記錄和版本控制功能。
 * 支援檔案版本查詢、歷史版本統計和版本回溯等功能。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>基本的 CRUD 操作（繼承自父介面）</li>
 *   <li>按版本號查詢歷史記錄</li>
 *   <li>獲取檔案的最新版本歷史</li>
 *   <li>統計版本數量和版本範圍</li>
 *   <li>版本鏈追蹤功能</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserOnlineFileHistory
 * @see ReactiveCrudRepository
 */
@Repository
public interface UserOnlineFileHistoryRepository extends ReactiveCrudRepository<UserOnlineFileHistory, String> {

    /**
     * 查詢指定檔案的最新版本歷史記錄。
     * <p>
     * 此方法用於獲取檔案的前 N 個最新版本歷史，按版本號降序排列。
     * 通常用於檔案版本清單顯示、最近版本快速存取等場景。
     * </p>
     *
     * @param fileId 檔案的唯一識別碼，不得為 null
     * @param n 要獲取的歷史記錄數量，必須為正整數
     * @return 包含最新 N 個版本歷史記錄的 {@link Flux}，按版本號降序排列
     */
    @Query("SELECT * FROM user_online_file_history WHERE file_id = :fileId ORDER BY version DESC LIMIT :n")
    Flux<UserOnlineFileHistory> findTopNByFileIdOrderByVersionDesc(Long fileId, Integer n);

    /**
     * 根據檔案 ID 和版本號查詢特定的檔案歷史記錄。
     * <p>
     * 此方法用於精確定位檔案的特定版本歷史記錄，
     * 通常用於版本回溯、版本比較和歷史內容查看等功能。
     * </p>
     *
     * @param fileId 檔案的唯一識別碼，不得為 null
     * @param version 版本號，不得為 null 或負數
     * @return 包含指定版本歷史記錄的 {@link Mono}，如果找不到則為空
     */
    Mono<UserOnlineFileHistory> findByFileIdAndVersion(Long fileId, Long version);

    /**
     * 根據檔案 ID 查詢所有版本歷史記錄。
     * <p>
     * 此方法用於獲取指定檔案的完整版本歷史，按版本號降序排列。
     * 通常用於版本歷史瀏覽、版本對比和完整歷史追蹤等功能。
     * </p>
     *
     * @param fileId 檔案的唯一識別碼，不得為 null
     * @return 包含所有版本歷史記錄的 {@link Flux}，按版本號降序排列，可能為空流
     */
    Flux<UserOnlineFileHistory> findAllByFileIdOrderByVersionDesc(Long fileId);

    /**
     * 根據檔案 ID 和前一版本號查詢相關的版本歷史記錄。
     * <p>
     * 此方法用於查詢基於特定前一版本號的版本歷史記錄，
     * 主要用於版本鏈追蹤、版本依賴關係分析和版本樹構建等功能。
     * </p>
     *
     * @param fileId 檔案的唯一識別碼，不得為 null
     * @param previousVersion 前一版本號，不得為 null
     * @return 包含相關版本歷史記錄的 {@link Flux}，可能為空流
     */
    Flux<UserOnlineFileHistory> findAllByFileIdAndPreviousVersion(Long fileId, Long previousVersion);

    /**
     * 統計指定檔案的版本歷史數量並獲取最早版本號。
     * <p>
     * 此方法用於獲取檔案版本歷史的統計資訊，包括總版本數量和最早的版本號。
     * 通常用於版本管理介面顯示、版本清理策略和版本範圍判斷等功能。
     * </p>
     *
     * @param fileId 檔案的唯一識別碼，不得為 null
     * @return 包含版本數量和最早版本號的資料傳輸物件 {@link OnlineHistoryCountAndOldestDAO}
     * @see OnlineHistoryCountAndOldestDAO
     */
    @Query("SELECT COUNT(*) AS count, MIN(version) as version FROM user_online_file_history WHERE file_id = :fileId")
    Mono<OnlineHistoryCountAndOldestDAO> getOldestVersionAndCountByFileId(Long fileId);
}
