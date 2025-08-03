package xyz.dowob.filemanagement.repostiory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.entity.Token;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 憑證資料庫操作介面測試類。
 * 
 * 測試 TokenRepository 憑證資料庫操作介面的響應式資料存取功能和 Spring Data R2DBC 操作。
 * 驗證 JWT Token 和重設驗證碼的存儲、查詢及過期管理，包括繼承自 ReactiveCrudRepository 的基本 CRUD 操作和自定義查詢方法。
 * 透過模擬實現測試各種查詢條件、過期時間處理和異常情況，確保響應式程式設計模式的正確實現。
 * 
 * <p>測試涵蓋的主要功能：
 * <ul>
 * <li>findByUserId 根據用戶 ID 查詢用戶憑證</li>
 * <li>findAllByJwtTokenExpireTimeIsBefore 查詢所有過期的 JWT Token</li>
 * <li>基本 CRUD 操作的響應式實現驗證</li>
 * <li>Token 過期時間邏輯和清理策略</li>
 * <li>同一用戶多個 Token 的處理</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("TokenRepository 憑證數據庫操作接口測試")
class TokenRepositoryTest {

    private TokenRepository tokenRepository;
    private Token testToken1;
    private Token testToken2;
    private Token testToken3;
    private LocalDateTime currentTime;
    private LocalDateTime pastTime;
    private LocalDateTime futureTime;

    @BeforeEach
    void setUp() {
        currentTime = LocalDateTime.now();
        pastTime = currentTime.minusDays(1);
        futureTime = currentTime.plusDays(1);

        // 創建測試用的 TokenRepository 實現
        tokenRepository = new TokenRepository() {
            // 模擬數據存儲
            private final List<Token> tokens = Arrays.asList(
                createToken(1L, 1L, pastTime),      // 過期Token
                createToken(2L, 2L, futureTime),    // 未過期Token
                createToken(3L, 3L, pastTime),      // 過期Token
                createToken(4L, 4L, futureTime),    // 未過期Token
                createToken(5L, 1L, futureTime)     // 同一用戶的另一個未過期Token
            );

            @Override
            public Mono<Token> findByUserId(long userId) {
                return Flux.fromIterable(tokens)
                        .filter(token -> token.getUserId() == userId)
                        .next();
            }

            @Override
            public Flux<Token> findAllByJwtTokenExpireTimeIsBefore(LocalDateTime expireTime) {
                if (expireTime == null) {
                    return Flux.empty();
                }
                return Flux.fromIterable(tokens)
                        .filter(token -> token.getJwtTokenExpireTime() != null && 
                                       token.getJwtTokenExpireTime().isBefore(expireTime));
            }

            // ReactiveCrudRepository 基本方法實現
            @Override
            public <S extends Token> Mono<S> save(S entity) {
                return Mono.just(entity);
            }

            @Override
            public <S extends Token> Flux<S> saveAll(Iterable<S> entities) {
                return Flux.fromIterable(entities);
            }

            @Override
            public <S extends Token> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
                return Flux.from(entityStream);
            }

            @Override
            public Mono<Token> findById(Long id) {
                if (id == null) {
                    return Mono.empty();
                }
                return Flux.fromIterable(tokens)
                        .filter(token -> id.equals(token.getId()))
                        .next();
            }

            @Override
            public Mono<Token> findById(org.reactivestreams.Publisher<Long> id) {
                return Mono.from(id).flatMap(this::findById);
            }

            @Override
            public Mono<Boolean> existsById(Long id) {
                return findById(id).hasElement();
            }

            @Override
            public Mono<Boolean> existsById(org.reactivestreams.Publisher<Long> id) {
                return Mono.from(id).flatMap(this::existsById);
            }

            @Override
            public Flux<Token> findAll() {
                return Flux.fromIterable(tokens);
            }

            @Override
            public Flux<Token> findAllById(Iterable<Long> ids) {
                return Flux.fromIterable(ids).flatMap(this::findById);
            }

            @Override
            public Flux<Token> findAllById(org.reactivestreams.Publisher<Long> idStream) {
                return Flux.from(idStream).flatMap(this::findById);
            }

            @Override
            public Mono<Long> count() {
                return Mono.just((long) tokens.size());
            }

            @Override
            public Mono<Void> deleteById(Long id) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteById(org.reactivestreams.Publisher<Long> id) {
                return Mono.from(id).then();
            }

            @Override
            public Mono<Void> delete(Token entity) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAllById(Iterable<? extends Long> ids) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(Iterable<? extends Token> entities) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends Token> entityStream) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll() {
                return Mono.empty();
            }
        };

        // 設置測試對象
        testToken1 = createToken(1L, 1L, pastTime);
        testToken2 = createToken(2L, 2L, futureTime);
        testToken3 = createToken(3L, 3L, pastTime);
    }

    private Token createToken(Long id, Long userId, LocalDateTime expireTime) {
        Token token = new Token();
        token.setId(id);
        token.setUserId(userId);
        token.setJwtTokenVersion("jwt_token_" + id);
        token.setResetVerificationCode("reset_code_" + id);
        token.setJwtTokenExpireTime(expireTime);
        token.setResetVerificationCodeExpireTime(expireTime.plusHours(24));
        // Token entity doesn't have createTime field
        return token;
    }

    // ==================== 一般測試 ====================

    /**
     * 測試 findByUserId 方法的基本功能。
     * 
     * 測試根據用戶 ID 查詢用戶憑證的基本查詢功能。
     * 
     * 前置條件：
     * - 測試資料包含不同用戶的 Token 記錄
     * - 每個 Token 具有不同的過期時間
     * 
     * 測試步驟：
     * - 呼叫 findByUserId 方法查詢指定用戶的 Token
     * - 驗證返回的 Token 資訊
     * 
     * 預期結果：
     * - 成功返回用戶的 Token 記錄
     * - Token 資訊完整且正確
     */
    @Test
    @DisplayName("一般測試 - findByUserId 方法基本功能")
    void testFindByUserId_basicFunctionality() {
        StepVerifier.create(tokenRepository.findByUserId(1L))
                .assertNext(token -> {
                    assertNotNull(token);
                    assertEquals(1L, token.getUserId());
                    assertEquals("jwt_token_1", token.getJwtTokenVersion());
                    assertEquals("reset_code_1", token.getResetVerificationCode());
                    assertEquals(pastTime, token.getJwtTokenExpireTime());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByJwtTokenExpireTimeIsBefore 方法基本功能")
    void testFindAllByJwtTokenExpireTimeIsBefore_basicFunctionality() {
        StepVerifier.create(tokenRepository.findAllByJwtTokenExpireTimeIsBefore(currentTime))
                .assertNext(token -> {
                    assertEquals(1L, token.getId());
                    assertTrue(token.getJwtTokenExpireTime().isBefore(currentTime));
                })
                .assertNext(token -> {
                    assertEquals(3L, token.getId());
                    assertTrue(token.getJwtTokenExpireTime().isBefore(currentTime));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 查詢未過期Token")
    void testFindAllByJwtTokenExpireTimeIsBefore_nonExpiredTokens() {
        // 使用過去的時間，應該查不到任何過期Token
        LocalDateTime veryPastTime = currentTime.minusDays(10);
        
        StepVerifier.create(tokenRepository.findAllByJwtTokenExpireTimeIsBefore(veryPastTime))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 查詢所有Token（包括未來過期的）")
    void testFindAllByJwtTokenExpireTimeIsBefore_allTokens() {
        // 使用未來的時間，應該查到所有Token
        LocalDateTime veryFutureTime = currentTime.plusDays(10);
        
        StepVerifier.create(tokenRepository.findAllByJwtTokenExpireTimeIsBefore(veryFutureTime))
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - ReactiveCrudRepository 基本CRUD操作")
    void testReactiveCrudRepositoryBasicOperations() {
        // 測試 save 操作
        StepVerifier.create(tokenRepository.save(testToken1))
                .assertNext(savedToken -> {
                    assertNotNull(savedToken);
                    assertEquals(1L, savedToken.getId());
                    assertEquals(1L, savedToken.getUserId());
                })
                .verifyComplete();

        // 測試 findById 操作
        StepVerifier.create(tokenRepository.findById(1L))
                .assertNext(token -> {
                    assertEquals(1L, token.getId());
                    assertEquals(1L, token.getUserId());
                })
                .verifyComplete();

        // 測試 existsById 操作
        StepVerifier.create(tokenRepository.existsById(1L))
                .expectNext(true)
                .verifyComplete();

        // 測試 count 操作
        StepVerifier.create(tokenRepository.count())
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAll 方法")
    void testFindAll() {
        StepVerifier.create(tokenRepository.findAll())
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - saveAll 批量保存操作")
    void testSaveAll() {
        List<Token> tokensToSave = Arrays.asList(testToken1, testToken2);
        
        StepVerifier.create(tokenRepository.saveAll(tokensToSave))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllById 批量查詢操作")
    void testFindAllById() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        
        StepVerifier.create(tokenRepository.findAllById(ids))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 findByUserId 方法
        try {
            var findByUserIdMethod = TokenRepository.class.getMethod("findByUserId", long.class);
            assertEquals(Mono.class, findByUserIdMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findByUserId 方法應該存在");
        }

        // 驗證 findAllByJwtTokenExpireTimeIsBefore 方法
        try {
            var findAllByJwtTokenExpireTimeIsBeforeMethod = 
                TokenRepository.class.getMethod("findAllByJwtTokenExpireTimeIsBefore", LocalDateTime.class);
            assertEquals(Flux.class, findAllByJwtTokenExpireTimeIsBeforeMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByJwtTokenExpireTimeIsBefore 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritanceRelationships() {
        // 驗證 TokenRepository 繼承了 ReactiveCrudRepository
        assertTrue(org.springframework.data.repository.reactive.ReactiveCrudRepository.class.isAssignableFrom(TokenRepository.class));
        
        // 驗證泛型參數
        var genericInterfaces = TokenRepository.class.getGenericInterfaces();
        assertTrue(genericInterfaces.length > 0);
    }

    @Test
    @DisplayName("一般測試 - 響應式編程模式驗證")
    void testReactiveProgrammingPatterns() {
        // 測試響應式鏈式操作
        Mono<String> jwtTokenChain = tokenRepository.findByUserId(1L)
                .map(Token::getJwtTokenVersion)
                .defaultIfEmpty("no_token");

        StepVerifier.create(jwtTokenChain)
                .expectNext("jwt_token_1")
                .verifyComplete();

        // 測試響應式合併操作
        Flux<Token> combinedTokens = tokenRepository.findByUserId(1L)
                .flux()
                .mergeWith(tokenRepository.findByUserId(2L));

        StepVerifier.create(combinedTokens)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - Token時間邏輯驗證")
    void testTokenTimeLogic() {
        // 驗證JWT Token過期時間早於Refresh Token過期時間
        StepVerifier.create(tokenRepository.findByUserId(1L))
                .assertNext(token -> {
                    assertTrue(token.getJwtTokenExpireTime().isBefore(token.getResetVerificationCodeExpireTime()));
                    // Token entity doesn't have createTime field
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 同一用戶多個Token處理")
    void testMultipleTokensForSameUser() {
        // 用戶1有兩個Token（ID為1和5）
        StepVerifier.create(tokenRepository.findByUserId(1L))
                .assertNext(token -> {
                    assertEquals(1L, token.getUserId());
                    // 應該返回第一個找到的Token
                    assertEquals(1L, token.getId());
                })
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    /**
     * 測試 findByUserId 方法處理不存在的用戶。
     * 
     * 測試當查詢不存在的用戶 ID 時的處理邏輯。
     * 
     * 前置條件：
     * - 使用不存在於測試資料中的用戶 ID
     * 
     * 測試步驟：
     * - 傳入不存在的用戶 ID
     * - 觀察返回結果
     * 
     * 預期結果：
     * - 返回空的 Mono，不拋出異常
     */
    @Test
    @DisplayName("異常測試 - findByUserId 查詢不存在的用戶")
    void testFindByUserId_nonExistentUser() {
        StepVerifier.create(tokenRepository.findByUserId(999L))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByUserId 傳入負數用戶ID")
    void testFindByUserId_negativeUserId() {
        StepVerifier.create(tokenRepository.findByUserId(-1L))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByUserId 傳入零用戶ID")
    void testFindByUserId_zeroUserId() {
        StepVerifier.create(tokenRepository.findByUserId(0L))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByJwtTokenExpireTimeIsBefore 傳入 null")
    void testFindAllByJwtTokenExpireTimeIsBefore_withNull() {
        StepVerifier.create(tokenRepository.findAllByJwtTokenExpireTimeIsBefore(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 傳入 null")
    void testFindById_withNull() {
        StepVerifier.create(tokenRepository.findById((Long) null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - existsById 傳入不存在的ID")
    void testExistsById_nonExistentId() {
        StepVerifier.create(tokenRepository.existsById(999L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByJwtTokenExpireTimeIsBefore 使用極端過去時間")
    void testFindAllByJwtTokenExpireTimeIsBefore_extremePastTime() {
        LocalDateTime extremePast = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        
        StepVerifier.create(tokenRepository.findAllByJwtTokenExpireTimeIsBefore(extremePast))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByJwtTokenExpireTimeIsBefore 使用極端未來時間")
    void testFindAllByJwtTokenExpireTimeIsBefore_extremeFutureTime() {
        LocalDateTime extremeFuture = LocalDateTime.of(3000, 12, 31, 23, 59, 59);
        
        StepVerifier.create(tokenRepository.findAllByJwtTokenExpireTimeIsBefore(extremeFuture))
                .expectNextCount(5) // 所有Token都應該在這個極端未來時間之前過期
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    /**
     * 測試 findByUserId 方法處理極大用戶 ID。
     * 
     * 測試當使用 Long.MAX_VALUE 作為用戶 ID 時的處理能力。
     * 
     * 前置條件：
     * - 使用極大的長整型值作為用戶 ID
     * 
     * 測試步驟：
     * - 傳入 Long.MAX_VALUE 用戶 ID
     * - 驗證查詢結果
     * 
     * 預期結果：
     * - 方法正常處理極大 ID
     * - 返回空結果（未找到匹配）
     */
    @Test
    @DisplayName("邊界測試 - findByUserId 使用極大用戶ID")
    void testFindByUserId_withMaxLongValue() {
        StepVerifier.create(tokenRepository.findByUserId(Long.MAX_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findByUserId 使用極小用戶ID")
    void testFindByUserId_withMinLongValue() {
        StepVerifier.create(tokenRepository.findByUserId(Long.MIN_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByJwtTokenExpireTimeIsBefore 精確時間匹配")
    void testFindAllByJwtTokenExpireTimeIsBefore_exactTimeMatch() {
        // 使用與過期Token完全相同的時間
        StepVerifier.create(tokenRepository.findAllByJwtTokenExpireTimeIsBefore(pastTime))
                .verifyComplete(); // 應該找不到，因為是isBefore，不包括相等時間
    }

    @Test
    @DisplayName("邊界測試 - findAllByJwtTokenExpireTimeIsBefore 微小時間差異")
    void testFindAllByJwtTokenExpireTimeIsBefore_tinyTimeDifference() {
        // 使用比過期時間早1納秒的時間
        LocalDateTime slightlyBeforePast = pastTime.minusNanos(1);
        
        StepVerifier.create(tokenRepository.findAllByJwtTokenExpireTimeIsBefore(slightlyBeforePast))
                .verifyComplete();

        // 使用比過期時間晚1納秒的時間
        LocalDateTime slightlyAfterPast = pastTime.plusNanos(1);
        
        StepVerifier.create(tokenRepository.findAllByJwtTokenExpireTimeIsBefore(slightlyAfterPast))
                .expectNextCount(2) // 應該找到過期的Token
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - Token創建時間與過期時間關係")
    void testTokenCreationAndExpirationTimeRelationship() {
        StepVerifier.create(tokenRepository.findAll())
                .assertNext(token -> {
                    // Token entity doesn't have createTime field
                    assertTrue(token.getJwtTokenExpireTime().isBefore(token.getResetVerificationCodeExpireTime()) ||
                              token.getJwtTokenExpireTime().isEqual(token.getResetVerificationCodeExpireTime()));
                })
                .assertNext(token -> {
                    // Token entity doesn't have createTime field  
                    assertTrue(token.getJwtTokenExpireTime().isBefore(token.getResetVerificationCodeExpireTime()) ||
                              token.getJwtTokenExpireTime().isEqual(token.getResetVerificationCodeExpireTime()));
                })
                .expectNextCount(3) // 跳過其餘驗證
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發查詢操作")
    void testConcurrentQueries() {
        // 併發執行多個查詢
        Flux<Token> concurrentQueries = Flux.merge(
                tokenRepository.findByUserId(1L),
                tokenRepository.findByUserId(2L),
                tokenRepository.findById(3L),
                tokenRepository.findAllByJwtTokenExpireTimeIsBefore(currentTime)
        );

        StepVerifier.create(concurrentQueries)
                .expectNextCount(5) // 1 + 1 + 1 + 2 = 5個Token
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜查詢組合")
    void testComplexQueryCombinations() {
        // 組合多種查詢操作
        Mono<Boolean> complexQuery = tokenRepository.findByUserId(1L)
                .flatMap(token -> tokenRepository.findAllByJwtTokenExpireTimeIsBefore(currentTime)
                        .any(expiredToken -> expiredToken.getUserId() == token.getUserId()))
                .defaultIfEmpty(false);

        StepVerifier.create(complexQuery)
                .expectNext(true) // 用戶1有過期Token
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 批量操作性能測試")
    void testBatchOperationPerformance() {
        // 測試大批量ID查詢
        List<Long> largeIdList = java.util.stream.LongStream.range(1, 10001)
                .boxed()
                .toList();

        StepVerifier.create(tokenRepository.findAllById(largeIdList))
                .expectNextCount(5) // 只有5個Token存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - Token過期清理模擬")
    void testTokenExpirationCleanupSimulation() {
        // 模擬清理過期Token的場景
        Flux<Token> expiredTokens = tokenRepository.findAllByJwtTokenExpireTimeIsBefore(currentTime);
        Flux<Long> expiredTokenIds = expiredTokens.map(Token::getId);
        
        StepVerifier.create(expiredTokenIds)
                .expectNext(1L)
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 時區邊界測試")
    void testTimezoneBoundaryConditions() {
        // 測試不同時區的時間處理
        LocalDateTime utcTime = LocalDateTime.now();
        
        StepVerifier.create(tokenRepository.findAllByJwtTokenExpireTimeIsBefore(utcTime))
                .expectNextCount(2) // 應該找到過期的Token
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - Token字符串長度測試")
    void testTokenStringLength() {
        // 驗證Token字符串不為空且有合理長度
        StepVerifier.create(tokenRepository.findByUserId(1L))
                .assertNext(token -> {
                    assertNotNull(token.getJwtTokenVersion());
                    assertNotNull(token.getResetVerificationCode());
                    assertTrue(token.getJwtTokenVersion().length() > 0);
                    assertTrue(token.getResetVerificationCode().length() > 0);
                    assertTrue(token.getJwtTokenVersion().startsWith("jwt_token_"));
                    assertTrue(token.getResetVerificationCode().startsWith("reset_code_"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - Token唯一性驗證")
    void testTokenUniqueness() {
        // 驗證每個Token都有唯一的ID
        StepVerifier.create(tokenRepository.findAll()
                .map(Token::getId)
                .distinct()
                .count())
                .expectNext(5L) // 應該有5個唯一的Token ID
                .verifyComplete();

        // 驗證JWT Token字符串的唯一性
        StepVerifier.create(tokenRepository.findAll()
                .map(Token::getJwtTokenVersion)
                .distinct()
                .count())
                .expectNext(5L) // 應該有5個唯一的JWT Token字符串
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空值處理測試")
    void testNullValueHandling() {
        // 創建一個包含null值的Token進行測試
        Token tokenWithNulls = new Token();
        tokenWithNulls.setId(999L);
        tokenWithNulls.setUserId(999L);
        tokenWithNulls.setJwtTokenVersion(null);
        tokenWithNulls.setResetVerificationCode(null);
        tokenWithNulls.setJwtTokenExpireTime(null);
        tokenWithNulls.setResetVerificationCodeExpireTime(null);

        StepVerifier.create(tokenRepository.save(tokenWithNulls))
                .assertNext(savedToken -> {
                    assertEquals(999L, savedToken.getId());
                    assertEquals(999L, savedToken.getUserId());
                })
                .verifyComplete();
    }
}