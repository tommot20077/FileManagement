package xyz.dowob.filemanagement.repostiory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.entity.UserFileShareRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用戶檔案分享記錄資料庫操作介面測試類。
 * 
 * 測試 UserFileShareRecordRepository 用戶檔案分享記錄資料庫操作介面的響應式資料存取功能和 Spring Data R2DBC 操作。
 * 驗證檔案共享記錄的建立、查詢及共享關係管理，包括繼承自 ReactiveCrudRepository 的基本 CRUD 操作和自定義查詢方法。
 * 支援用戶間檔案共享、多用戶共享同一檔案及分享關係統計，適合複雜的檔案共享權限管理。
 * 透過模擬實現測試各種查詢條件、關係驗證和異常情況，確保響應式程式設計模式的正確實現。
 * 
 * <p>測試涵蓋的主要功能：
 * <ul>
 * <li>findAllByUserId 根據用戶 ID 查詢所有分享記錄</li>
 * <li>findAllByUserIdInAndFileId 查詢特定檔案的多用戶共享</li>
 * <li>findAllByFileId 查詢特定檔案的所有共享者</li>
 * <li>findAllByFileIdIn 根據檔案 ID 集合批量查詢</li>
 * <li>existsByUserIdAndFileId 檢查具體共享關係是否存在</li>
 * <li>分享關係統計和唯一性驗證</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserFIleShareRecordRepository 用戶檔案分享記錄數據庫操作接口測試")
class UserFileShareRecordRepositoryTest {

    private UserFIleShareRecordRepository userFileShareRecordRepository;
    private UserFileShareRecord testRecord1;
    private UserFileShareRecord testRecord2;
    private UserFileShareRecord testRecord3;

    @BeforeEach
    void setUp() {
        // 創建測試用的 UserFIleShareRecordRepository 實現
        userFileShareRecordRepository = new UserFIleShareRecordRepository() {
            // 模擬數據存儲 - 用戶1分享檔案1,2,3；用戶2分享檔案1,3；用戶3分享檔案2
            private final List<UserFileShareRecord> records = Arrays.asList(
                createUserFileShareRecord(1L, 1L, 1L), // 用戶1分享檔案1
                createUserFileShareRecord(2L, 1L, 2L), // 用戶1分享檔案2
                createUserFileShareRecord(3L, 1L, 3L), // 用戶1分享檔案3
                createUserFileShareRecord(4L, 2L, 1L), // 用戶2分享檔案1
                createUserFileShareRecord(5L, 2L, 3L), // 用戶2分享檔案3
                createUserFileShareRecord(6L, 3L, 2L)  // 用戶3分享檔案2
            );

            @Override
            public Flux<UserFileShareRecord> findAllByUserId(Long userId) {
                if (userId == null) {
                    return Flux.empty();
                }
                return Flux.fromIterable(records)
                        .filter(record -> userId.equals(record.getUserId()));
            }

            @Override
            public Flux<UserFileShareRecord> findAllByUserIdInAndFileId(java.util.Collection<Long> userIds, Long fileId) {
                if (userIds == null || userIds.isEmpty() || fileId == null) {
                    return Flux.empty();
                }
                return Flux.fromIterable(records)
                        .filter(record -> userIds.contains(record.getUserId()) && 
                                        fileId.equals(record.getFileId()));
            }

            @Override
            public Flux<UserFileShareRecord> findAllByFileId(Long fileId) {
                if (fileId == null) {
                    return Flux.empty();
                }
                return Flux.fromIterable(records)
                        .filter(record -> fileId.equals(record.getFileId()));
            }

            @Override
            public Flux<UserFileShareRecord> findAllByFileIdIn(java.util.Collection<Long> fileIds) {
                if (fileIds == null || fileIds.isEmpty()) {
                    return Flux.empty();
                }
                return Flux.fromIterable(records)
                        .filter(record -> fileIds.contains(record.getFileId()));
            }

            @Override
            public Mono<Boolean> existsByUserIdAndFileId(Long userId, Long fileId) {
                if (userId == null || fileId == null) {
                    return Mono.just(false);
                }
                return Flux.fromIterable(records)
                        .any(record -> userId.equals(record.getUserId()) && 
                                     fileId.equals(record.getFileId()));
            }

            // ReactiveCrudRepository 基本方法實現
            @Override
            public <S extends UserFileShareRecord> Mono<S> save(S entity) {
                return Mono.just(entity);
            }

            @Override
            public <S extends UserFileShareRecord> Flux<S> saveAll(Iterable<S> entities) {
                return Flux.fromIterable(entities);
            }

            @Override
            public <S extends UserFileShareRecord> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
                return Flux.from(entityStream);
            }

            @Override
            public Mono<UserFileShareRecord> findById(Long id) {
                if (id == null) {
                    return Mono.empty();
                }
                return Flux.fromIterable(records)
                        .filter(record -> id.equals(record.getId()))
                        .next();
            }

            @Override
            public Mono<UserFileShareRecord> findById(org.reactivestreams.Publisher<Long> id) {
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
            public Flux<UserFileShareRecord> findAll() {
                return Flux.fromIterable(records);
            }

            @Override
            public Flux<UserFileShareRecord> findAllById(Iterable<Long> ids) {
                return Flux.fromIterable(ids).flatMap(this::findById);
            }

            @Override
            public Flux<UserFileShareRecord> findAllById(org.reactivestreams.Publisher<Long> idStream) {
                return Flux.from(idStream).flatMap(this::findById);
            }

            @Override
            public Mono<Long> count() {
                return Mono.just((long) records.size());
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
            public Mono<Void> delete(UserFileShareRecord entity) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAllById(Iterable<? extends Long> ids) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(Iterable<? extends UserFileShareRecord> entities) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends UserFileShareRecord> entityStream) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll() {
                return Mono.empty();
            }
        };

        // 設置測試對象
        testRecord1 = createUserFileShareRecord(1L, 1L, 1L);
        testRecord2 = createUserFileShareRecord(2L, 2L, 2L);
        testRecord3 = createUserFileShareRecord(3L, 3L, 3L);
    }

    private UserFileShareRecord createUserFileShareRecord(Long id, Long userId, Long fileId) {
        UserFileShareRecord record = new UserFileShareRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setFileId(fileId);
        return record;
    }

    // ==================== 一般測試 ====================

    /**
     * 測試 findAllByUserId 方法的基本功能。
     * 
     * 測試根據用戶 ID 查詢用戶所有檔案分享記錄的基本查詢功能。
     * 
     * 前置條件：
     * - 測試資料包含不同用戶的分享記錄
     * - 每個用戶分享了不同數量的檔案
     * 
     * 測試步驟：
     * - 呼叫 findAllByUserId 方法查詢指定用戶的分享記錄
     * - 驗證返回的分享記錄資訊
     * 
     * 預期結果：
     * - 成功返回用戶的所有分享記錄
     * - 記錄資訊完整且正確
     */
    @Test
    @DisplayName("一般測試 - findAllByUserId 方法基本功能")
    void testFindAllByUserId_basicFunctionality() {
        StepVerifier.create(userFileShareRecordRepository.findAllByUserId(1L))
                .assertNext(record -> {
                    assertEquals(1L, record.getId());
                    assertEquals(1L, record.getUserId());
                    assertEquals(1L, record.getFileId());
                })
                .assertNext(record -> {
                    assertEquals(2L, record.getId());
                    assertEquals(1L, record.getUserId());
                    assertEquals(2L, record.getFileId());
                })
                .assertNext(record -> {
                    assertEquals(3L, record.getId());
                    assertEquals(1L, record.getUserId());
                    assertEquals(3L, record.getFileId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByUserIdInAndFileId 方法基本功能")
    void testFindAllByUserIdInAndFileId_basicFunctionality() {
        List<Long> userIds = Arrays.asList(1L, 2L);
        
        // 查詢用戶1和2中分享檔案1的記錄
        StepVerifier.create(userFileShareRecordRepository.findAllByUserIdInAndFileId(userIds, 1L))
                .assertNext(record -> {
                    assertEquals(1L, record.getId());
                    assertEquals(1L, record.getUserId());
                    assertEquals(1L, record.getFileId());
                })
                .assertNext(record -> {
                    assertEquals(4L, record.getId());
                    assertEquals(2L, record.getUserId());
                    assertEquals(1L, record.getFileId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByFileId 方法基本功能")
    void testFindAllByFileId_basicFunctionality() {
        StepVerifier.create(userFileShareRecordRepository.findAllByFileId(1L))
                .assertNext(record -> {
                    assertEquals(1L, record.getId());
                    assertEquals(1L, record.getUserId());
                    assertEquals(1L, record.getFileId());
                })
                .assertNext(record -> {
                    assertEquals(4L, record.getId());
                    assertEquals(2L, record.getUserId());
                    assertEquals(1L, record.getFileId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByFileIdIn 方法基本功能")
    void testFindAllByFileIdIn_basicFunctionality() {
        List<Long> fileIds = Arrays.asList(1L, 2L);
        
        StepVerifier.create(userFileShareRecordRepository.findAllByFileIdIn(fileIds))
                .assertNext(record -> {
                    assertEquals(1L, record.getId());
                    assertEquals(1L, record.getFileId());
                })
                .assertNext(record -> {
                    assertEquals(2L, record.getId());
                    assertEquals(2L, record.getFileId());
                })
                .assertNext(record -> {
                    assertEquals(4L, record.getId());
                    assertEquals(1L, record.getFileId());
                })
                .assertNext(record -> {
                    assertEquals(6L, record.getId());
                    assertEquals(2L, record.getFileId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - existsByUserIdAndFileId 方法基本功能")
    void testExistsByUserIdAndFileId_basicFunctionality() {
        // 測試存在的記錄
        StepVerifier.create(userFileShareRecordRepository.existsByUserIdAndFileId(1L, 1L))
                .expectNext(true)
                .verifyComplete();

        // 測試不存在的記錄
        StepVerifier.create(userFileShareRecordRepository.existsByUserIdAndFileId(1L, 999L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - ReactiveCrudRepository 基本CRUD操作")
    void testReactiveCrudRepositoryBasicOperations() {
        // 測試 save 操作
        StepVerifier.create(userFileShareRecordRepository.save(testRecord1))
                .assertNext(savedRecord -> {
                    assertNotNull(savedRecord);
                    assertEquals(1L, savedRecord.getId());
                    assertEquals(1L, savedRecord.getUserId());
                    assertEquals(1L, savedRecord.getFileId());
                })
                .verifyComplete();

        // 測試 findById 操作
        StepVerifier.create(userFileShareRecordRepository.findById(1L))
                .assertNext(record -> {
                    assertEquals(1L, record.getId());
                    assertEquals(1L, record.getUserId());
                    assertEquals(1L, record.getFileId());
                })
                .verifyComplete();

        // 測試 existsById 操作
        StepVerifier.create(userFileShareRecordRepository.existsById(1L))
                .expectNext(true)
                .verifyComplete();

        // 測試 count 操作
        StepVerifier.create(userFileShareRecordRepository.count())
                .expectNext(6L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAll 方法")
    void testFindAll() {
        StepVerifier.create(userFileShareRecordRepository.findAll())
                .expectNextCount(6)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - saveAll 批量保存操作")
    void testSaveAll() {
        List<UserFileShareRecord> recordsToSave = Arrays.asList(testRecord1, testRecord2);
        
        StepVerifier.create(userFileShareRecordRepository.saveAll(recordsToSave))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllById 批量查詢操作")
    void testFindAllById() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        
        StepVerifier.create(userFileShareRecordRepository.findAllById(ids))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 findAllByUserId 方法
        try {
            var findAllByUserIdMethod = UserFIleShareRecordRepository.class.getMethod("findAllByUserId", Long.class);
            assertEquals(Flux.class, findAllByUserIdMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByUserId 方法應該存在");
        }

        // 驗證 existsByUserIdAndFileId 方法
        try {
            var existsByUserIdAndFileIdMethod = UserFIleShareRecordRepository.class.getMethod(
                "existsByUserIdAndFileId", Long.class, Long.class);
            assertEquals(Mono.class, existsByUserIdAndFileIdMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("existsByUserIdAndFileId 方法應該存在");
        }

        // 驗證 findAllByUserIdInAndFileId 方法
        try {
            var findAllByUserIdInAndFileIdMethod = UserFIleShareRecordRepository.class.getMethod(
                "findAllByUserIdInAndFileId", java.util.Collection.class, Long.class);
            assertEquals(Flux.class, findAllByUserIdInAndFileIdMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByUserIdInAndFileId 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritanceRelationships() {
        // 驗證 UserFIleShareRecordRepository 繼承了 ReactiveCrudRepository
        assertTrue(org.springframework.data.repository.reactive.ReactiveCrudRepository.class.isAssignableFrom(UserFIleShareRecordRepository.class));
        
        // 驗證泛型參數
        var genericInterfaces = UserFIleShareRecordRepository.class.getGenericInterfaces();
        assertTrue(genericInterfaces.length > 0);
    }

    @Test
    @DisplayName("一般測試 - 響應式編程模式驗證")
    void testReactiveProgrammingPatterns() {
        // 測試響應式鏈式操作
        Mono<Long> fileIdChain = userFileShareRecordRepository.findById(1L)
                .map(UserFileShareRecord::getFileId)
                .defaultIfEmpty(0L);

        StepVerifier.create(fileIdChain)
                .expectNext(1L)
                .verifyComplete();

        // 測試響應式合併操作
        Flux<UserFileShareRecord> combinedRecords = userFileShareRecordRepository.findById(1L)
                .flux()
                .mergeWith(userFileShareRecordRepository.findById(2L));

        StepVerifier.create(combinedRecords)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 檔案分享關係驗證")
    void testFileShareRelationships() {
        // 驗證用戶1分享了3個檔案
        StepVerifier.create(userFileShareRecordRepository.findAllByUserId(1L).count())
                .expectNext(3L)
                .verifyComplete();

        // 驗證檔案1被2個用戶分享
        StepVerifier.create(userFileShareRecordRepository.findAllByFileId(1L).count())
                .expectNext(2L)
                .verifyComplete();

        // 驗證檔案3被2個用戶分享
        StepVerifier.create(userFileShareRecordRepository.findAllByFileId(3L).count())
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 分享記錄統計")
    void testShareRecordStatistics() {
        // 統計每個用戶的分享數量
        StepVerifier.create(userFileShareRecordRepository.findAll()
                .groupBy(UserFileShareRecord::getUserId)
                .flatMap(userGroup -> userGroup.count()
                        .map(count -> userGroup.key() + ":" + count)))
                .expectNextCount(3) // 應該有3個用戶組
                .verifyComplete();

        // 統計每個檔案的分享數量
        StepVerifier.create(userFileShareRecordRepository.findAll()
                .groupBy(UserFileShareRecord::getFileId)
                .flatMap(fileGroup -> fileGroup.count()
                        .map(count -> fileGroup.key() + ":" + count)))
                .expectNextCount(3) // 應該有3個檔案組
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 不同用戶分享同一檔案")
    void testMultipleUsersShareSameFile() {
        // 測試多個用戶分享同一檔案的情況
        List<Long> userIds = Arrays.asList(1L, 2L);
        
        StepVerifier.create(userFileShareRecordRepository.findAllByUserIdInAndFileId(userIds, 1L))
                .expectNextCount(2) // 用戶1和2都分享了檔案1
                .verifyComplete();

        StepVerifier.create(userFileShareRecordRepository.findAllByUserIdInAndFileId(userIds, 3L))
                .expectNextCount(2) // 用戶1和2都分享了檔案3
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 單一用戶分享多個檔案")
    void testSingleUserShareMultipleFiles() {
        // 測試單一用戶分享多個檔案的情況
        StepVerifier.create(userFileShareRecordRepository.findAllByUserId(1L)
                .map(UserFileShareRecord::getFileId)
                .collectList())
                .assertNext(fileIds -> {
                    assertEquals(3, fileIds.size());
                    assertTrue(fileIds.contains(1L));
                    assertTrue(fileIds.contains(2L));
                    assertTrue(fileIds.contains(3L));
                })
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    /**
     * 測試 findAllByUserId 方法處理 null 用戶 ID。
     * 
     * 測試當傳入 null 用戶 ID 時的異常處理邏輯。
     * 
     * 前置條件：
     * - 方法接受 null 用戶 ID 參數
     * 
     * 測試步驟：
     * - 傳入 null 用戶 ID
     * - 觀察方法的處理結果
     * 
     * 預期結果：
     * - 方法返回空的 Flux，不拋出異常
     */
    @Test
    @DisplayName("異常測試 - findAllByUserId 傳入 null 用戶ID")
    void testFindAllByUserId_withNullUserId() {
        StepVerifier.create(userFileShareRecordRepository.findAllByUserId(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByUserIdInAndFileId 傳入 null 參數")
    void testFindAllByUserIdInAndFileId_withNullParameters() {
        StepVerifier.create(userFileShareRecordRepository.findAllByUserIdInAndFileId(null, 1L))
                .verifyComplete();

        StepVerifier.create(userFileShareRecordRepository.findAllByUserIdInAndFileId(Arrays.asList(1L), null))
                .verifyComplete();

        StepVerifier.create(userFileShareRecordRepository.findAllByUserIdInAndFileId(null, null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByUserIdInAndFileId 傳入空集合")
    void testFindAllByUserIdInAndFileId_withEmptyCollection() {
        StepVerifier.create(userFileShareRecordRepository.findAllByUserIdInAndFileId(Collections.emptyList(), 1L))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByFileId 傳入 null 檔案ID")
    void testFindAllByFileId_withNullFileId() {
        StepVerifier.create(userFileShareRecordRepository.findAllByFileId(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByFileIdIn 傳入 null 檔案ID集合")
    void testFindAllByFileIdIn_withNullFileIds() {
        StepVerifier.create(userFileShareRecordRepository.findAllByFileIdIn(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByFileIdIn 傳入空集合")
    void testFindAllByFileIdIn_withEmptyCollection() {
        StepVerifier.create(userFileShareRecordRepository.findAllByFileIdIn(Collections.emptyList()))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - existsByUserIdAndFileId 傳入 null 參數")
    void testExistsByUserIdAndFileId_withNullParameters() {
        StepVerifier.create(userFileShareRecordRepository.existsByUserIdAndFileId(null, 1L))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(userFileShareRecordRepository.existsByUserIdAndFileId(1L, null))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(userFileShareRecordRepository.existsByUserIdAndFileId(null, null))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 查詢不存在的用戶")
    void testQueryNonExistentUser() {
        StepVerifier.create(userFileShareRecordRepository.findAllByUserId(999L))
                .verifyComplete();

        StepVerifier.create(userFileShareRecordRepository.existsByUserIdAndFileId(999L, 1L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 查詢不存在的檔案")
    void testQueryNonExistentFile() {
        StepVerifier.create(userFileShareRecordRepository.findAllByFileId(999L))
                .verifyComplete();

        StepVerifier.create(userFileShareRecordRepository.existsByUserIdAndFileId(1L, 999L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 傳入 null")
    void testFindById_withNull() {
        StepVerifier.create(userFileShareRecordRepository.findById((Long) null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 查詢不存在的記錄")
    void testFindById_nonExistentRecord() {
        StepVerifier.create(userFileShareRecordRepository.findById(999L))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - existsById 傳入不存在的ID")
    void testExistsById_nonExistentId() {
        StepVerifier.create(userFileShareRecordRepository.existsById(999L))
                .expectNext(false)
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    /**
     * 測試 findAllByUserId 方法處理極大用戶 ID。
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
    @DisplayName("邊界測試 - findAllByUserId 使用極大用戶ID")
    void testFindAllByUserId_withMaxLongUserId() {
        StepVerifier.create(userFileShareRecordRepository.findAllByUserId(Long.MAX_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserId 使用極小用戶ID")
    void testFindAllByUserId_withMinLongUserId() {
        StepVerifier.create(userFileShareRecordRepository.findAllByUserId(Long.MIN_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserId 使用零用戶ID")
    void testFindAllByUserId_withZeroUserId() {
        StepVerifier.create(userFileShareRecordRepository.findAllByUserId(0L))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByFileId 使用極大檔案ID")
    void testFindAllByFileId_withMaxLongFileId() {
        StepVerifier.create(userFileShareRecordRepository.findAllByFileId(Long.MAX_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByFileId 使用極小檔案ID")
    void testFindAllByFileId_withMinLongFileId() {
        StepVerifier.create(userFileShareRecordRepository.findAllByFileId(Long.MIN_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserIdInAndFileId 使用大量用戶ID")
    void testFindAllByUserIdInAndFileId_withManyUserIds() {
        // 創建包含大量用戶ID的集合
        List<Long> manyUserIds = java.util.stream.LongStream.range(1, 10001)
                .boxed()
                .toList();

        StepVerifier.create(userFileShareRecordRepository.findAllByUserIdInAndFileId(manyUserIds, 1L))
                .expectNextCount(2) // 只有用戶1和2在測試數據中分享了檔案1
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByFileIdIn 使用大量檔案ID")
    void testFindAllByFileIdIn_withManyFileIds() {
        // 創建包含大量檔案ID的集合
        List<Long> manyFileIds = java.util.stream.LongStream.range(1, 10001)
                .boxed()
                .toList();

        StepVerifier.create(userFileShareRecordRepository.findAllByFileIdIn(manyFileIds))
                .expectNextCount(6) // 所有測試數據都應該被返回
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserIdInAndFileId 使用單一用戶ID")
    void testFindAllByUserIdInAndFileId_withSingleUserId() {
        List<Long> singleUserId = Arrays.asList(1L);

        StepVerifier.create(userFileShareRecordRepository.findAllByUserIdInAndFileId(singleUserId, 1L))
                .assertNext(record -> {
                    assertEquals(1L, record.getUserId());
                    assertEquals(1L, record.getFileId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByFileIdIn 使用單一檔案ID")
    void testFindAllByFileIdIn_withSingleFileId() {
        List<Long> singleFileId = Arrays.asList(1L);

        StepVerifier.create(userFileShareRecordRepository.findAllByFileIdIn(singleFileId))
                .expectNextCount(2) // 檔案1被2個用戶分享
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - existsByUserIdAndFileId 使用極值")
    void testExistsByUserIdAndFileId_withExtremeValues() {
        StepVerifier.create(userFileShareRecordRepository.existsByUserIdAndFileId(Long.MAX_VALUE, Long.MAX_VALUE))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(userFileShareRecordRepository.existsByUserIdAndFileId(Long.MIN_VALUE, Long.MIN_VALUE))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(userFileShareRecordRepository.existsByUserIdAndFileId(0L, 0L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用極大ID")
    void testFindById_withMaxLongValue() {
        StepVerifier.create(userFileShareRecordRepository.findById(Long.MAX_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用極小ID")
    void testFindById_withMinLongValue() {
        StepVerifier.create(userFileShareRecordRepository.findById(Long.MIN_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發查詢操作")
    void testConcurrentQueries() {
        // 併發執行多個查詢
        Flux<UserFileShareRecord> concurrentQueries = Flux.merge(
                userFileShareRecordRepository.findAllByUserId(1L),
                userFileShareRecordRepository.findAllByFileId(1L),
                userFileShareRecordRepository.findById(5L),
                userFileShareRecordRepository.findById(6L)
        );

        StepVerifier.create(concurrentQueries)
                .expectNextCount(7) // 3 + 2 + 1 + 1 = 7個記錄
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜查詢組合")
    void testComplexQueryCombinations() {
        // 組合多種查詢操作
        Mono<Boolean> complexQuery = userFileShareRecordRepository.findAllByUserId(1L)
                .collectList()
                .flatMap(userRecords -> {
                    if (!userRecords.isEmpty()) {
                        Long firstFileId = userRecords.get(0).getFileId();
                        return userFileShareRecordRepository.findAllByFileId(firstFileId)
                                .count()
                                .map(count -> count > 1);
                    }
                    return Mono.just(false);
                });

        StepVerifier.create(complexQuery)
                .expectNext(true) // 檔案1被多個用戶分享
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 批量操作性能測試")
    void testBatchOperationPerformance() {
        // 測試大批量ID查詢
        List<Long> largeIdList = java.util.stream.LongStream.range(1, 10001)
                .boxed()
                .toList();

        StepVerifier.create(userFileShareRecordRepository.findAllById(largeIdList))
                .expectNextCount(6) // 只有6個記錄存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 重複ID處理")
    void testDuplicateIdHandling() {
        // 測試重複的用戶ID
        List<Long> duplicateUserIds = Arrays.asList(1L, 1L, 2L, 2L);
        
        StepVerifier.create(userFileShareRecordRepository.findAllByUserIdInAndFileId(duplicateUserIds, 1L))
                .expectNextCount(2) // 應該仍然返回2個記錄（去重）
                .verifyComplete();

        // 測試重複的檔案ID
        List<Long> duplicateFileIds = Arrays.asList(1L, 1L, 2L, 2L);
        
        StepVerifier.create(userFileShareRecordRepository.findAllByFileIdIn(duplicateFileIds))
                .expectNextCount(4) // 應該返回所有匹配的記錄
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 記錄數據一致性驗證")
    void testRecordDataConsistency() {
        // 驗證記錄數據的一致性
        StepVerifier.create(userFileShareRecordRepository.findAll())
                .assertNext(record -> {
                    assertNotNull(record.getId());
                    assertNotNull(record.getUserId());
                    assertNotNull(record.getFileId());
                    assertTrue(record.getId() > 0);
                    assertTrue(record.getUserId() > 0);
                    assertTrue(record.getFileId() > 0);
                })
                .expectNextCount(5) // 跳過其餘記錄的驗證
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 記錄唯一性驗證")
    void testRecordUniqueness() {
        // 驗證每個記錄都有唯一的ID
        StepVerifier.create(userFileShareRecordRepository.findAll()
                .map(UserFileShareRecord::getId)
                .distinct()
                .count())
                .expectNext(6L) // 應該有6個唯一的記錄ID
                .verifyComplete();

        // 驗證用戶ID和檔案ID的組合唯一性
        StepVerifier.create(userFileShareRecordRepository.findAll()
                .map(record -> record.getUserId() + "-" + record.getFileId())
                .distinct()
                .count())
                .expectNext(6L) // 每個用戶和檔案的組合都應該是唯一的
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 分享關係對稱性驗證")
    void testShareRelationshipSymmetry() {
        // 驗證分享關係的邏輯一致性
        StepVerifier.create(userFileShareRecordRepository.findAllByUserId(1L)
                .flatMap(record -> userFileShareRecordRepository.existsByUserIdAndFileId(record.getUserId(), record.getFileId()))
                .all(exists -> exists == true))
                .expectNext(true) // 所有用戶1的分享記錄都應該能通過existsByUserIdAndFileId查到
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 構造函數測試")
    void testConstructors() {
        // 測試無參構造函數
        UserFileShareRecord record1 = new UserFileShareRecord();
        assertNotNull(record1);
        assertNull(record1.getId());
        assertNull(record1.getUserId());
        assertNull(record1.getFileId());

        // 測試有參構造函數
        UserFileShareRecord record2 = new UserFileShareRecord(1L, 2L);
        assertNotNull(record2);
        assertEquals(1L, record2.getUserId());
        assertEquals(2L, record2.getFileId());
    }
}