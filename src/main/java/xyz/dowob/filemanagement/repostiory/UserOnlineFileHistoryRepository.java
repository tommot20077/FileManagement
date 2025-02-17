package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.UserOnlineFileHistory;

/**
 * 用戶在線文件歷史數據庫操作接口，用於對用戶在線文件歷史進行增刪改查
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserOnlineFileHistoryRepository
 * @create 2025/2/8
 * @Version 1.0
 **/
@Repository
public interface UserOnlineFileHistoryRepository extends ReactiveCrudRepository<UserOnlineFileHistory, String> {

    /**
     * 查詢用戶最新的在線文件歷史數據
     *
     * @param fileId 文件id
     *
     * @return 用戶在線文件歷史數據
     */
    Mono<UserOnlineFileHistory> findTopByFileIdOrderByVersionDesc(Long fileId);

    /**
     * 使用文件id和版本查詢用戶在線文件歷史數據
     *
     * @param fileId  文件id
     * @param version 版本
     *
     * @return 用戶在線文件歷史數據
     */
    Mono<UserOnlineFileHistory> findByFileIdAndVersion(Long fileId, Long version);

    /**
     * 使用文件id查詢用戶在線文件歷史數據
     *
     * @param fileId 文件id
     *
     * @return 用戶在線文件歷史數據
     */
    Flux<UserOnlineFileHistory> findAllByFileIdOrderByVersionDesc(Long fileId);
}
