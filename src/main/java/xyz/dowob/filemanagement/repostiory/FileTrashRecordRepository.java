package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.FileTrashRecord;

import java.time.LocalDateTime;

/**
 * 文件回收站記錄數據庫操作接口，用於操作文件回收站記錄數據庫
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileTrashCanRepository
 * @create 2025/2/22
 * @Version 1.0
 **/
@Repository
public interface FileTrashRecordRepository extends ReactiveCrudRepository<FileTrashRecord, Long> {
    /**
     * 批量插入檔案回收站記錄
     *
     * @param entities         檔案回收站記錄
     * @param entityOperations 數據庫操作
     *
     * @return 插入的檔案回收站記錄
     */
    default Flux<FileTrashRecord> insertAll(Iterable<FileTrashRecord> entities, R2dbcEntityOperations entityOperations) {
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
     * 插入檔案回收站記錄
     *
     * @param entities         檔案回收站記錄
     * @param entityOperations 數據庫操作
     *
     * @return 插入的檔案回收站記錄
     */
    default Mono<FileTrashRecord> insert(FileTrashRecord entities, R2dbcEntityOperations entityOperations) {
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
     * 查詢用戶過期的檔案回收站記錄
     *
     * @param userId 用戶ID
     *
     * @return 過期的檔案回收站記錄
     */
    Flux<FileTrashRecord> findAllByUserIdAndDeleteTimeBefore(Long userId, LocalDateTime deleteTime);
}
