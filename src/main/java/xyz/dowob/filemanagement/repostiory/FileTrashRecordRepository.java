package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.FileTrashRecord;

import java.time.LocalDateTime;

/**
 * 檔案回收站記錄的響應式資料庫接口，支援非阻塞的檔案回收站操作
 *
 * <p>提供對檔案回收站記錄的新增、查詢等基本操作，採用響應式編程風格</p>
 *
 * @author yuan
 * @version 1.0
 * @since 2025/2/22
 */
@Repository
public interface FileTrashRecordRepository extends ReactiveCrudRepository<FileTrashRecord, Long> {
    /**
     * 批量非同步插入檔案回收站記錄，支援多筆資料同時新增。
     * <p>
     * 此方法使用響應式編程方式處理批量插入操作，能夠高效處理大量檔案回收站記錄的新增需求。
     * 在檔案刪除、批量刪除等場景中廣泛使用，提供非阻塞的資料庫寫入能力。
     * </p>
     *
     * @param entities 要插入的檔案回收站記錄集合，不得為 null
     * @param entityOperations 用於執行資料庫操作的 R2DBC 操作介面，提供底層資料庫存取能力
     * @return 響應式串流，逐個發出已成功插入的檔案回收站記錄
     * @throws IllegalArgumentException 當傳入實體集合為 null 時拋出
     */
    default Flux<FileTrashRecord> insertAll(Iterable<FileTrashRecord> entities, R2dbcEntityOperations entityOperations) {
        if (entities == null) {
            return Flux.error(new IllegalArgumentException("實體不可為空"));
        }
        
        String sql = "INSERT INTO file_trash_record (file_id, user_id, parent_folder_id, delete_time) VALUES (:fileId, :userId, :parentFolderId, :deleteTime)";

        return Flux
                .fromIterable(entities)
                .flatMap(record -> entityOperations
                        .getDatabaseClient()
                        .sql(sql)
                        .bind("fileId", record.getFileId())
                        .bind("userId", record.getUserId())
                        .bindNull("parentFolderId", Long.class)
                        .bind("deleteTime", record.getDeleteTime())
                        .fetch()
                        .rowsUpdated()
                        .thenReturn(record));
    }


    /**
     * 非同步插入單一檔案回收站記錄。
     * <p>
     * 此方法用於處理單一檔案的刪除操作，將檔案資訊記錄到回收站中。
     * 採用響應式編程模式，確保插入操作不會阻塞當前執行緒，適合用於即時的檔案刪除場景。
     * </p>
     *
     * @param entities 要插入的檔案回收站記錄，不得為 null
     * @param entityOperations 用於執行資料庫操作的 R2DBC 操作介面，提供底層資料庫存取能力
     * @return 響應式 Mono，發出已成功插入的檔案回收站記錄
     * @throws IllegalArgumentException 當傳入實體為 null 時拋出
     */
    default Mono<FileTrashRecord> insert(FileTrashRecord entities, R2dbcEntityOperations entityOperations) {
        if (entities == null) {
            return Mono.error(new IllegalArgumentException("實體不可為空"));
        }
        
        String sql = "INSERT INTO file_trash_record (file_id, user_id, parent_folder_id, delete_time) VALUES (:fileId, :userId, :parentFolderId, :deleteTime)";

        return Mono
                .just(entities)
                .flatMap(record -> entityOperations
                        .getDatabaseClient()
                        .sql(sql)
                        .bind("fileId", record.getFileId())
                        .bind("userId", record.getUserId())
                        .bindNull("parentFolderId", Long.class)
                        .bind("deleteTime", record.getDeleteTime())
                        .fetch()
                        .rowsUpdated()
                        .thenReturn(record));
    }


    /**
     * 查詢指定使用者已逾期的檔案回收站記錄。
     * <p>
     * 此方法用於定期清理系統中過期的回收站記錄，通常配合定時任務使用。
     * 透過比較刪除時間與指定的臨界時間點，找出需要永久刪除的檔案記錄。
     * 這是系統垃圾收集機制的重要組成部分。
     * </p>
     *
     * @param userId 使用者唯一識別碼，指定要查詢的使用者
     * @param deleteTime 用於判斷過期的時間臨界點，早於此時間的記錄將被視為過期
     * @return 響應式串流，逐個發出符合過期條件的檔案回收站記錄
     */
    Flux<FileTrashRecord> findAllByUserIdAndDeleteTimeBefore(Long userId, LocalDateTime deleteTime);
}
