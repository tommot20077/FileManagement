package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.Token;

import java.time.LocalDateTime;

/**
 * 憑證數據庫操作介面，用於操作Token 實體與數據庫的轉換
 * 繼承ReactiveCrudRepository接口，實現對Token數據庫的非阻塞操作
 *
 * @author yuan
 * @program File-Management
 * @ClassName TokenRepository
 * @description
 * @create 2024-09-18 17:25
 * @Version 1.0
 **/
@Repository
public interface TokenRepository extends ReactiveCrudRepository<Token, Long> {
    /**
     * 通過用戶ID查詢Token
     *
     * @param userId 用戶ID
     *
     * @return Token
     */
    Mono<Token> findByUserId(long userId);

    /**
     * 查詢所有過期的Token
     *
     * @param expireTime 過期時間
     *
     * @return Token列表
     */
    Flux<Token> findAllByJwtTokenExpireTimeIsBefore(LocalDateTime expireTime);
}
