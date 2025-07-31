package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.entity.TransfersTask;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 檔案傳輸任務資料存取層介面，提供傳輸任務的響應式資料庫操作功能。
 * <p>
 * 此介面繼承自 Spring Data R2DBC 的 {@link ReactiveCrudRepository}，
 * 專門用於管理檔案上傳、下載和傳輸任務的生命週期。
 * 支援任務狀態追蹤、進度管理和失敗重試等進階功能。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>基本的 CRUD 操作（繼承自父介面）</li>
 *   <li>根據 MD5 值查詢傳輸任務（去重功能）</li>
 *   <li>根據任務 ID 查詢特定傳輸任務</li>
 *   <li>根據狀態批量查詢任務</li>
 *   <li>查詢過期或失敗的任務（用於清理）</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see TransfersTask
 * @see ReactiveCrudRepository
 * @see TransfersStatusEnum
 */
@Repository
public interface TransfersTasksRepository extends ReactiveCrudRepository<TransfersTask, Long> {
    /**
     * 根據 MD5 雜湊值查詢傳輸任務。
     * <p>
     * 此方法用於檔案去重功能，在上傳檔案時檢查是否已經存在相同內容的檔案。
     * 如果找到相同 MD5 值的任務，可以避免重複上傳，提高系統效率並節省儲存空間。
     * </p>
     *
     * @param md5 檔案內容的 MD5 雜湊值，不得為 null 或空字串
     * @return 包含相同 MD5 值傳輸任務的 {@link Mono}，如果找不到則為空
     */
    Mono<TransfersTask> findByMd5(String md5);


    /**
     * 根據傳輸任務 ID 查詢傳輸任務。
     * <p>
     * 此方法用於精確定位特定的傳輸任務，通常用於任務狀態查詢、
     * 進度更新和結果處理等場景。傳輸任務 ID 是系統產生的唯一識別碼，
     * 用於追蹤和管理每個上傳或下載任務。
     * </p>
     *
     * @param transferTaskId 傳輸任務的唯一識別碼，不得為 null 或空字串
     * @return 包含指定任務 ID 傳輸任務的 {@link Mono}，如果找不到則為空
     */
    Mono<TransfersTask> findByTransferTaskId(String transferTaskId);


    /**
     * 根據狀態清單批量查詢傳輸任務。
     * <p>
     * 此方法用於查詢處於指定狀態的傳輸任務，通常用於任務狀態管理、
     * 進度追蹤和批量處理等場景。例如查詢所有正在進行中的任務、
     * 已完成的任務或失敗的任務等。
     * </p>
     *
     * @param status 要查詢的狀態清單，不得為 null 或包含 null 元素
     * @return 包含所有符合指定狀態傳輸任務的 {@link Flux}，可能為空流
     * @see TransfersStatusEnum
     */
    Flux<TransfersTask> findAllByStatusIn(@Param("status") List<TransfersStatusEnum> status);


    /**
     * 根據狀態和開始時間查詢過期的傳輸任務。
     * <p>
     * 此方法主要用於系統的定時清理任務，找出長時間處於特定狀態的任務。
     * 例如查找在指定時間之前開始但仍然處於執行中狀態的任務，
     * 這些任務可能需要被標記為失敗或重新啟動。
     * </p>
     *
     * @param status 要查詢的狀態清單，不得為 null 或包含 null 元素
     * @param startTime 時間臨界點，早於此時間開始的任務將被視為過期
     * @return 包含所有符合條件過期任務的 {@link Flux}，可能為空流
     * @see TransfersStatusEnum
     */
    Flux<TransfersTask> findAllByStatusInAndStartTimeBefore(@Param("status") List<TransfersStatusEnum> status, LocalDateTime startTime);

}
