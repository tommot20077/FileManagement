package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.UserFileShareRecord;

import java.util.Collection;

/**
 * 用戶檔案分享記錄操作介面，使用Spring Data R2DBC來操作數據庫，繼承ReactiveCrudRepository。
 * 用於操作數據庫中的user_file_share_record表
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserFIleShareRecordRepository
 * @create 2025/3/5
 * @Version 1.0
 **/
@Repository
public interface UserFIleShareRecordRepository extends ReactiveCrudRepository<UserFileShareRecord, Long> {
    /**
     * 根據用戶ID查詢用戶檔案分享記錄
     *
     * @param userId 用戶ID
     *
     * @return 用戶檔案分享記錄
     */
    Flux<UserFileShareRecord> findAllByUserId(Long userId);

    /**
     * 根據用戶ID集合查詢用戶檔案分享記錄
     *
     * @param userIds 用戶ID集合
     *
     * @return 用戶檔案分享記錄
     */
    Flux<UserFileShareRecord> findAllByUserIdInAndFileId(Collection<Long> userIds, Long fileId);

    /**
     * 根據檔案ID查詢用戶檔案分享記錄
     *
     * @param fileId 檔案ID
     *
     * @return 用戶檔案分享記錄
     */
    Flux<UserFileShareRecord> findAllByFileId(Long fileId);

    /**
     * 根據檔案ID集合查詢用戶檔案分享記錄
     *
     * @param fileIds 檔案ID集合
     *
     * @return 用戶檔案分享記錄
     */
    Flux<UserFileShareRecord> findAllByFileIdIn(Collection<Long> fileIds);

    /**
     * 根據用戶ID和檔案ID查詢用戶檔案分享記錄
     *
     * @param userId 用戶ID
     * @param fileId 檔案ID
     *
     * @return 用戶檔案分享記錄
     */
    Mono<UserFileShareRecord> findByUserIdAndFileId(Long userId, Long fileId);
}
