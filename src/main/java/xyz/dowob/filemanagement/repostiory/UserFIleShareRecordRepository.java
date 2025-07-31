package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.UserFileShareRecord;

import java.util.Collection;

/**
 * 用戶檔案分享記錄資料存取層介面，提供檔案分享記錄的響應式資料庫操作功能。
 * <p>
 * 此介面繼承自 Spring Data R2DBC 的 {@link ReactiveCrudRepository}，
 * 專門用於管理用戶之間的檔案分享關係和權限控制。
 * 支援分享檔案的授權、撤銷和權限查詢等功能。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>基本的 CRUD 操作（繼承自父介面）</li>
 *   <li>根據用戶 ID 查詢分享記錄</li>
 *   <li>根據檔案 ID 查詢分享記錄</li>
 *   <li>批量查詢和權限驗證</li>
 *   <li>分享關係存在性檢查</li>
 * </ul>
 * </p>
 * <p>
 * 操作對象表：data_file_share_record
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserFileShareRecord
 * @see ReactiveCrudRepository
 */
@Repository
public interface UserFIleShareRecordRepository extends ReactiveCrudRepository<UserFileShareRecord, Long> {
    /**
     * 根據用戶 ID 查詢所有與該用戶相關的檔案分享記錄。
     * <p>
     * 此方法用於查詢特定用戶參與的所有檔案分享活動，
     * 包括作為分享者和被分享者的所有記錄。通常用於用戶檔案分享歷史查詢。
     * </p>
     *
     * @param userId 用戶的唯一識別碼，不得為 null
     * @return 包含用戶所有分享記錄的 {@link Flux}，可能為空流
     */
    Flux<UserFileShareRecord> findAllByUserId(Long userId);

    /**
     * 根據用戶 ID 集合和檔案 ID 批量查詢分享記錄。
     * <p>
     * 此方法用於檢查特定檔案是否已經分享給指定的用戶集合，
     * 通常用於批量權限驗證和分享狀態檢查。這是分享功能中的核心查詢方法之一。
     * </p>
     *
     * @param userIds 要查詢的用戶 ID 集合，不得為 null 或包含 null 元素
     * @param fileId 檔案的唯一識別碼，不得為 null
     * @return 包含所有符合條件分享記錄的 {@link Flux}，可能為空流
     */
    Flux<UserFileShareRecord> findAllByUserIdInAndFileId(Collection<Long> userIds, Long fileId);

    /**
     * 根據檔案 ID 查詢所有相關的分享記錄。
     * <p>
     * 此方法用於查詢特定檔案的所有分享記錄，了解哪些用戶擁有該檔案的存取權限。
     * 通常用於檔案權限管理、分享記錄查詢和檔案存取控制等場景。
     * </p>
     *
     * @param fileId 檔案的唯一識別碼，不得為 null
     * @return 包含檔案所有分享記錄的 {@link Flux}，可能為空流
     */
    Flux<UserFileShareRecord> findAllByFileId(Long fileId);

    /**
     * 根據檔案 ID 集合批量查詢分享記錄。
     * <p>
     * 此方法用於批量查詢多個檔案的分享記錄，提供高效的批量權限查詢能力。
     * 通常用於檔案清單顯示時的權限檢查，以及批量檔案操作時的權限驗證。
     * </p>
     *
     * @param fileIds 要查詢的檔案 ID 集合，不得為 null 或包含 null 元素
     * @return 包含所有符合條件分享記錄的 {@link Flux}，可能為空流
     */
    Flux<UserFileShareRecord> findAllByFileIdIn(Collection<Long> fileIds);

    /**
     * 檢查特定用戶和檔案之間是否存在分享關係。
     * <p>
     * 此方法用於快速驗證特定用戶是否已經被授權存取特定檔案。
     * 這是檔案權限驗證系統中的核心方法，用於快速權限檢查而不需要載入完整的記錄資料。
     * </p>
     *
     * @param userId 用戶的唯一識別碼，不得為 null
     * @param fileId 檔案的唯一識別碼，不得為 null
     * @return 包含是否存在分享關係的 {@link Mono}， true 表示存在，false 表示不存在
     */
    Mono<Boolean> existsByUserIdAndFileId(Long userId, Long fileId);
}
