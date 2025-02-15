package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.file.dao.ServerFileMetaCountDao;
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

    @Query("SELECT * FROM user_file_metadata WHERE user_id = :userId ORDER BY CASE WHEN parent_folder_id IS NULL THEN 0 ELSE 1 END, filename LIMIT :limit OFFSET :offset")
    Flux<UserFileMetadata> findAllByUserIdWithPagination(
            @Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 根據用戶ID和檔案名稱查詢檔案元數據
     *
     * @param userId   用戶ID
     * @param filename 檔案名稱
     *
     * @return Mono<UserFileMetadata>
     */
    Mono<UserFileMetadata> findByUserIdAndFilenameOrderByIsFolder(Long userId, String filename);

    /**
     * 根據用戶ID和父文件夾ID查詢檔案元數據，此方法可以蒐尋多個父文件夾ID並返回所有符合條件的檔案元數據
     *
     * @param userId         用戶ID
     * @param parentFolderId 父文件夾ID
     *
     * @return Flux<UserFileMetadata> 返回所有符合條件的檔案元數據
     */
    Flux<UserFileMetadata> findAllByUserIdAndParentFolderIdInOrderByIsFolder(Long userId, List<Long> parentFolderId);

    /**
     * 根據用戶ID和父文件夾ID查詢檔案元數據，此方法可以蒐尋多個父文件夾ID並返回所有符合條件的檔案元數據，並進行分頁
     *
     * @param userId         用戶ID
     * @param parentFolderId 父文件夾ID
     * @param limit          限制條數
     * @param offset         偏移量
     *
     * @return Flux<UserFileMetadata> 返回所有符合條件的檔案元數據
     */
    @Query("SELECT * FROM user_file_metadata WHERE user_id = :userId AND parent_folder_id IN (:parentFolderId) ORDER BY IF(is_folder = 1, 0, 1), filename LIMIT :limit OFFSET :offset")
    Flux<UserFileMetadata> findAllByUserIdAndParentFolderIdInWithPagination(
            @Param("userId") Long userId,
            @Param("parentFolderId") List<Long> parentFolderId, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 根據用戶ID和父文件夾ID查詢檔案元數據(此方法為查詢根文件夾)
     *
     * @param userId 用戶ID
     *
     * @return Flux<UserFileMetadata> 返回根文件夾下的所有檔案元數據
     */
    @Query("SELECT * FROM user_file_metadata WHERE user_id = :userId AND parent_folder_id IS NULL ORDER BY CASE WHEN parent_folder_id IS NULL THEN 0 ELSE 1 END, filename")
    Flux<UserFileMetadata> findAllByUserIdAndParentFolderIdIsNull(@Param("userId") Long userId);

    /**
     * 根據用戶ID和父文件夾ID查詢檔案元數據(此方法為查詢根文件夾)，並進行分頁
     *
     * @param userId 用戶ID
     * @param limit  限制條數
     * @param offset 偏移量
     *
     * @return Flux<UserFileMetadata> 返回根文件夾下的所有檔案元數據
     */
    @Query("SELECT * FROM user_file_metadata WHERE user_id = :userId AND parent_folder_id IS NULL ORDER BY CASE WHEN parent_folder_id IS NULL THEN 0 ELSE 1 END, filename LIMIT :limit OFFSET :offset")
    Flux<UserFileMetadata> findAllByUserIdAndParentFolderIdIsNullWithPagination(
            @Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);


    /**
     * 計算該用戶擁有同一伺服器檔案的檔案數量
     *
     * @param userId         用戶ID
     * @param serverFileIds  伺服器檔案ID
     * @param databaseClient 數據庫客戶端
     *
     * @return Mono<Long> 返回檔案數量
     */
    default Flux<ServerFileMetaCountDao> countByServerFileIdInAndUserId(
            @Param("serverFileIds") List<Long> serverFileIds, @Param("userId") Long userId, DatabaseClient databaseClient) {
        return databaseClient
                .sql("SELECT server_file_id as serverFileId, COUNT(*) AS count FROM user_file_metadata WHERE server_file_id IN (:serverFileIds) AND user_id = :userId GROUP BY server_file_id")
                .bind("serverFileIds", serverFileIds)
                .bind("userId", userId)
                .fetch()
                .all()
                .mapNotNull(row -> {
                    Long serverFileId = (Long) row.get("serverFileId");
                    Long count = (Long) row.get("count");
                    if (serverFileId != null && count != null) {
                        return new ServerFileMetaCountDao(serverFileId, count);
                    }
                    return null;
                });
    }
}
