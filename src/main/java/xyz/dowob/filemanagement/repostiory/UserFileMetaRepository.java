package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.util.List;

/**
 * 用戶檔案元數據操作介面，使用Spring Data R2DBC來操作數據庫，繼承ReactiveCrudRepository。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserFileMetaRepository
 * @description
 * @create 2024-09-26 18:46
 * @Version 1.0
 **/
@Repository
public interface UserFileMetaRepository extends ReactiveCrudRepository<UserFileMetadata, String> {

    /**
     * 根據用戶ID查詢所有檔案元數據
     *
     * @param userId 用戶ID
     *
     * @return Flux<UserFileMetadata>
     */
    Flux<UserFileMetadata> findAllByUserId(Long userId);

    /**
     * 根據用戶ID和檔案名稱查詢檔案元數據
     *
     * @param userId   用戶ID
     * @param filename 檔案名稱
     *
     * @return Mono<UserFileMetadata>
     */
    Mono<UserFileMetadata> findByUserIdAndFilename(Long userId, String filename);

    Flux<UserFileMetadata> findAllByUserIdAndParentFolderIdIn(Long userId, List<Long> parentFolderId);

    @Query("SELECT * FROM user_file_metadata WHERE user_id = :userId AND parent_folder_id IS NULL")
    Flux<UserFileMetadata> findAllByUserIdAndParentFolderIdIsNull(@Param("userId") Long userId);


    /**
     * 計算該用戶擁有同一伺服器檔案的檔案數量
     *
     * @param userId         用戶ID
     * @param serverFileId   伺服器檔案ID
     * @param databaseClient 數據庫客戶端
     *
     * @return Mono<Long> 返回檔案數量
     */
    default Mono<Long> countByServerFileIdAndUserId(
            @Param("serverFileId") Long serverFileId, @Param("userId") Long userId, DatabaseClient databaseClient) {
        return databaseClient
                .sql("SELECT COUNT(*) AS count FROM user_file_metadata WHERE server_file_id = :serverFileId AND user_id = :userId")
                .bind("serverFileId", serverFileId)
                .bind("userId", userId)
                .map(row -> row.get("count", Long.class))
                .one();
    }
}
