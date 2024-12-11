package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

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

    /**
     * 根據用戶ID和伺服器檔案ID查詢檔案元數據
     *
     * @param userId       用戶ID
     * @param serverFileId 伺服器檔案ID
     *
     * @return Mono<UserFileMetadata>
     */
    Mono<UserFileMetadata> findByUserIdAndServerFileId(Long userId, Long serverFileId);

    /**
     * 查詢指定檔案的所有分享用戶
     *
     * @param id 用戶檔案ID
     *
     * @return Flux<User>
     */
    @Query("select u.* from shared_files sf join users u on sf.user_id = u.id where sf.user_file_id = :id;")
    Flux<User> findAllShareUsersById(Long id);

}
