package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.UserOnlineFile;

/**
 * 用戶線上檔案資料存取層介面，提供響應式的資料庫操作功能。
 * <p>
 * 此介面繼承自 Spring Data R2DBC 的 {@link ReactiveCrudRepository}，
 * 專門用於管理用戶線上編輯檔案的基本資料和內容操作。
 * 支援非阻塞的資料庫存取，確保高效能的檔案操作和即時協作功能。
 * </p>
 * <p>
 * 主要功能包括線上檔案的建立、查詢、更新和刪除操作，
 * 特別針對即時編輯和協作場景進行最佳化，支援高併發的檔案編輯需求。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserOnlineFile
 * @see ReactiveCrudRepository
 * @see Repository
 */
@Repository
public interface UserOnlineFileRepository extends ReactiveCrudRepository<UserOnlineFile, String> {


    /**
     * 插入一筆具有指定 ID 的用戶線上檔案記錄。
     * <p>
     * 此方法提供自定義 ID 的插入功能，通常用於檔案匯入、資料遷移
     * 或需要保持特定 ID 關聯的場景。使用自定義 SQL 查詢確保
     * 精確的資料插入控制。
     * </p>
     *
     * @param file 要插入的用戶線上檔案物件，不得為 null，且必須包含有效的 ID
     * @return 插入成功後的檔案物件 {@link Mono}
     */
    @Query("INSERT INTO user_online_file (id, file_size, last_modified_by, content, current_snapshot_count) VALUES (:#{#file.id}, :#{#file.fileSize}, :#{#file.lastModifiedBy}, :#{#file.content}, :#{#file.currentSnapshotCount})")
    Mono<UserOnlineFile> insertWithId(UserOnlineFile file);


}
