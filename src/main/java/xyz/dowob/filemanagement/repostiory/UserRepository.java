package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;

/**
 * 用戶數據庫操作介面，用於操作User 實體與數據庫的轉換
 * 繼承ReactiveCrudRepository接口，實現對User數據庫的非阻塞操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserRepository
 * @description
 * @create 2024-09-23 14:01
 * @Version 1.0
 **/
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    /**
     * 根據用戶名查詢用戶
     *
     * @param username 用戶名
     *
     * @return Mono<User>
     */
    Mono<User> findByUsername(String username);

    /**
     * 根據郵箱查詢用戶
     *
     * @param email 信箱
     *
     * @return Mono<User>
     */
    Mono<User> findByEmail(String email);

}
