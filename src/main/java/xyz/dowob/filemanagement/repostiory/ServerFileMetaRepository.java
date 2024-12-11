package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.User;

/**
 * 伺服器檔案元數據操作介面，使用Spring Data R2DBC來操作數據庫，繼承ReactiveCrudRepository。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ServerFileMetaRepository
 * @description
 * @create 2024-09-26 18:45
 * @Version 1.0
 **/
@Repository
public interface ServerFileMetaRepository extends ReactiveCrudRepository<ServerFileMetadata, String> {
    /**
     * 根據GridFS ID查詢文件元數據
     *
     * @param gridFsId GridFS ID
     *
     * @return Mono<ServerFileMetadata>
     */
    Mono<ServerFileMetadata> findByGridFsId(String gridFsId);

    /**
     * 根據MD5值查詢文件元數據
     *
     * @param md5 MD5值
     *
     * @return Mono<ServerFileMetadata>
     */
    Mono<ServerFileMetadata> findByMd5(String md5);

    /**
     * 查詢指定檔案的所有擁有者
     *
     * @param serverFileId 伺服器檔案ID
     *
     * @return Flux<User>
     */
    @Query("SELECT u.* FROM users u JOIN user_file_metadata ufm ON u.id = ufm.user_id WHERE ufm.server_file_id = :serverFileId;")
    Flux<User> findAllOwnersByServerFileMetaId(Long serverFileId);
}
