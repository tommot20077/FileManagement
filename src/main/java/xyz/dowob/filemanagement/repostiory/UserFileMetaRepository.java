package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName UserFileMetaRepository
 * @description
 * @create 2024-09-26 18:46
 * @Version 1.0
 **/
@Repository
public interface UserFileMetaRepository extends ReactiveCrudRepository<UserFileMetadata, String> {

    Flux<UserFileMetadata> findAllByUserId(Long userId);

    Mono<UserFileMetadata> findByUserIdAndFilename(Long userId, String filename);

    Mono<UserFileMetadata> findByUserIdAndServerFileId(Long userId, Long serverFileId);

    @Query("select u.* from shared_files sf join users u on sf.user_id = u.id where sf.user_file_id = :id;")
    Flux<User> findAllShareUsersById(Long id);

}
