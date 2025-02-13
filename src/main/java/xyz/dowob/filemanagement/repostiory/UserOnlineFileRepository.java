package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.UserOnlineFile;

/**
 * 用戶在線文件數據庫操作接口，用於對用戶在線文件進行增刪改查
 * @author yuan
 * @program FileManagement
 * @ClassName UserOnlineFileRepository
 * @create 2025/2/8
 * @Version 1.0
 **/
@Repository
public interface UserOnlineFileRepository extends ReactiveCrudRepository<UserOnlineFile, String> {


    /**
     * 插入一條帶有id的用戶在線文件數據
     *
     * @param file 用戶在線文件
     *
     * @return 插入的用戶在線文件
     */
    @Query("INSERT INTO user_online_file (id, file_size, last_modified_by, content, current_snapshot_count) VALUES (:#{#file.id}, :#{#file.fileSize}, :#{#file.lastModifiedBy}, :#{#file.content}, :#{#file.currentSnapshotCount})")
    Mono<UserOnlineFile> insertWithId(UserOnlineFile file);


}
