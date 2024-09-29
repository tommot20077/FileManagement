package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.User;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName ServerFileMetaRepository
 * @description
 * @create 2024-09-26 18:45
 * @Version 1.0
 **/
@Repository
public interface ServerFileMetaRepository extends ReactiveCrudRepository<ServerFileMetadata, String> {

    Mono<ServerFileMetadata> findByGridFsId(String gridFsId);

    Mono<ServerFileMetadata> findByMd5(String md5);

    @Query("SELECT u.* FROM users u JOIN user_file_metadata ufm ON u.id = ufm.user_id WHERE ufm.server_file_id = :serverFileId;")
    Flux<User> findAllOwnersByServerFileMetaId(Long serverFileId);
}
