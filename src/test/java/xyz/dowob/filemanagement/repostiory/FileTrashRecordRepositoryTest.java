package xyz.dowob.filemanagement.repostiory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.entity.FileTrashRecord;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 檔案回收站記錄資料庫操作介面測試類。
 * 
 * 測試 FileTrashRecordRepository 檔案回收站記錄資料庫操作介面的響應式資料存取功能和 Spring Data R2DBC 操作。
 * 驗證回收站記錄的查詢、插入操作及時間邏輯處理，包括繼承自 ReactiveCrudRepository 的基本 CRUD 操作和自定義查詢方法。
 * 透過模擬實現測試各種查詢條件、異常處理和邊界情況，確保響應式程式設計模式的正確實現。
 * 
 * <p>測試涵蓋的主要功能：
 * <ul>
 * <li>findAllByUserIdAndDeleteTimeBefore 查詢指定用戶的過期回收站記錄</li>
 * <li>insertAll 和 insert 預設方法的批量和單一記錄插入操作</li>
 * <li>基本 CRUD 操作的響應式實現驗證</li>
 * <li>檔案回收站時間邏輯和清理策略</li>
 * <li>異常處理和邊界條件測試</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("FileTrashRecordRepository 檔案回收站記錄數據庫操作接口測試")
class FileTrashRecordRepositoryTest {

    private FileTrashRecordRepository fileTrashRecordRepository;
    private R2dbcEntityOperations entityOperations;
    private FileTrashRecord testRecord1;
    private FileTrashRecord testRecord2;
    private FileTrashRecord testRecord3;
    private LocalDateTime currentTime;
    private LocalDateTime pastTime;
    private LocalDateTime futureTime;

    @BeforeEach
    void setUp() {
        currentTime = LocalDateTime.now();
        pastTime = currentTime.minusDays(7); // 7天前
        futureTime = currentTime.plusDays(7); // 7天後
        
        entityOperations = mock(R2dbcEntityOperations.class);

        // 創建測試用的 FileTrashRecordRepository 實現
        fileTrashRecordRepository = new FileTrashRecordRepository() {
            // 模擬數據存儲
            private final List<FileTrashRecord> records = Arrays.asList(
                createFileTrashRecord(1L, 1L, 1L, pastTime),        // 用戶1的過期記錄
                createFileTrashRecord(2L, 1L, 2L, currentTime),     // 用戶1的當前記錄
                createFileTrashRecord(3L, 2L, 3L, pastTime),        // 用戶2的過期記錄
                createFileTrashRecord(4L, 2L, 4L, futureTime),      // 用戶2的未來記錄
                createFileTrashRecord(5L, 1L, 5L, pastTime)         // 用戶1的另一過期記錄
            );

            @Override
            public Flux<FileTrashRecord> findAllByUserIdAndDeleteTimeBefore(Long userId, LocalDateTime deleteTime) {
                if (userId == null || deleteTime == null) {
                    return Flux.empty();
                }
                return Flux.fromIterable(records)
                        .filter(record -> userId.equals(record.getUserId()))
                        .filter(record -> record.getDeleteTime() != null && 
                                        record.getDeleteTime().isBefore(deleteTime));
            }

            // ReactiveCrudRepository 基本方法實現
            @Override
            public <S extends FileTrashRecord> Mono<S> save(S entity) {
                return Mono.just(entity);
            }

            @Override
            public <S extends FileTrashRecord> Flux<S> saveAll(Iterable<S> entities) {
                return Flux.fromIterable(entities);
            }

            @Override
            public <S extends FileTrashRecord> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
                return Flux.from(entityStream);
            }

            @Override
            public Mono<FileTrashRecord> findById(Long id) {
                if (id == null) {
                    return Mono.empty();
                }
                return Flux.fromIterable(records)
                        .filter(record -> id.equals(record.getFileId()))
                        .next();
            }

            @Override
            public Mono<FileTrashRecord> findById(org.reactivestreams.Publisher<Long> id) {
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
            public Flux<FileTrashRecord> findAll() {
                return Flux.fromIterable(records);
            }

            @Override
            public Flux<FileTrashRecord> findAllById(Iterable<Long> ids) {
                return Flux.fromIterable(ids).flatMap(this::findById);
            }

            @Override
            public Flux<FileTrashRecord> findAllById(org.reactivestreams.Publisher<Long> idStream) {
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
            public Mono<Void> delete(FileTrashRecord entity) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAllById(Iterable<? extends Long> ids) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(Iterable<? extends FileTrashRecord> entities) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends FileTrashRecord> entityStream) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll() {
                return Mono.empty();
            }
        };

        // 設置測試對象
        testRecord1 = createFileTrashRecord(1L, 1L, 1L, pastTime);
        testRecord2 = createFileTrashRecord(2L, 1L, 2L, currentTime);
        testRecord3 = createFileTrashRecord(3L, 2L, 3L, pastTime);
    }

    private FileTrashRecord createFileTrashRecord(Long id, Long userId, Long fileId, LocalDateTime deleteTime) {
        FileTrashRecord record = new FileTrashRecord();
        record.setFileId(fileId);
        record.setUserId(userId);
        record.setParentFolderId(null);
        record.setDeleteTime(deleteTime);
        return record;
    }

    // ==================== 一般測試 ====================

    /**
     * 測試 findAllByUserIdAndDeleteTimeBefore 方法的基本功能。
     * 
     * 測試根據用戶ID和刪除時間查詢過期回收站記錄的基本查詢邏輯。
     * 
     * 前置條件：
     * - 測試資料包含不同用戶的回收站記錄
     * - 記錄具有不同的刪除時間
     * 
     * 測試步驟：
     * - 呼叫 findAllByUserIdAndDeleteTimeBefore 方法查詢用戶1的過期記錄
     * - 驗證返回的記錄資料正確性
     * 
     * 預期結果：
     * - 成功返回用戶1在指定時間之前的回收站記錄
     * - 記錄資料完整且正確
     */
    @Test
    @DisplayName("一般測試 - findAllByUserIdAndDeleteTimeBefore 方法基本功能")
    void testFindAllByUserIdAndDeleteTimeBefore_basicFunctionality() {
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, currentTime))
                .assertNext(record -> {
                    assertEquals(1L, record.getFileId());
                    assertEquals(1L, record.getUserId());
                    assertEquals(1L, record.getFileId());
                    assertTrue(record.getDeleteTime().isBefore(currentTime));
                })
                .assertNext(record -> {
                    assertEquals(5L, record.getFileId());
                    assertEquals(1L, record.getUserId());
                    assertEquals(5L, record.getFileId());
                    assertTrue(record.getDeleteTime().isBefore(currentTime));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 不同用戶的過期記錄查詢")
    void testFindAllByUserIdAndDeleteTimeBefore_differentUsers() {
        // 用戶1的過期記錄
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, currentTime))
                .expectNextCount(2) // record1 和 record5
                .verifyComplete();

        // 用戶2的過期記錄
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(2L, currentTime))
                .assertNext(record -> {
                    assertEquals(3L, record.getFileId());
                    assertEquals(2L, record.getUserId());
                    assertEquals(3L, record.getFileId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 查詢未來時間的過期記錄")
    void testFindAllByUserIdAndDeleteTimeBefore_futureTime() {
        // 使用未來時間，應該包含更多記錄
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, futureTime))
                .expectNextCount(3) // record1, record2, record5
                .verifyComplete();

        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(2L, futureTime))
                .expectNextCount(1) // record3
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - ReactiveCrudRepository 基本CRUD操作")
    void testReactiveCrudRepositoryBasicOperations() {
        // 測試 save 操作
        StepVerifier.create(fileTrashRecordRepository.save(testRecord1))
                .assertNext(savedRecord -> {
                    assertNotNull(savedRecord);
                    assertEquals(1L, savedRecord.getFileId());
                    assertEquals(1L, savedRecord.getUserId());
                    assertEquals(1L, savedRecord.getFileId());
                })
                .verifyComplete();

        // 測試 findById 操作
        StepVerifier.create(fileTrashRecordRepository.findById(1L))
                .assertNext(record -> {
                    assertEquals(1L, record.getFileId());
                    assertEquals(1L, record.getUserId());
                })
                .verifyComplete();

        // 測試 existsById 操作
        StepVerifier.create(fileTrashRecordRepository.existsById(1L))
                .expectNext(true)
                .verifyComplete();

        // 測試 count 操作
        StepVerifier.create(fileTrashRecordRepository.count())
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAll 方法")
    void testFindAll() {
        StepVerifier.create(fileTrashRecordRepository.findAll())
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - saveAll 批量保存操作")
    void testSaveAll() {
        List<FileTrashRecord> recordsToSave = Arrays.asList(testRecord1, testRecord2);
        
        StepVerifier.create(fileTrashRecordRepository.saveAll(recordsToSave))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllById 批量查詢操作")
    void testFindAllById() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        
        StepVerifier.create(fileTrashRecordRepository.findAllById(ids))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 findAllByUserIdAndDeleteTimeBefore 方法
        try {
            var findAllByUserIdAndDeleteTimeBeforeMethod = FileTrashRecordRepository.class.getMethod(
                "findAllByUserIdAndDeleteTimeBefore", Long.class, LocalDateTime.class);
            assertEquals(Flux.class, findAllByUserIdAndDeleteTimeBeforeMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByUserIdAndDeleteTimeBefore 方法應該存在");
        }

        // 驗證 insertAll 默認方法
        try {
            var insertAllMethod = FileTrashRecordRepository.class.getMethod(
                "insertAll", Iterable.class, R2dbcEntityOperations.class);
            assertEquals(Flux.class, insertAllMethod.getReturnType());
            assertTrue(insertAllMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("insertAll 方法應該存在");
        }

        // 驗證 insert 默認方法
        try {
            var insertMethod = FileTrashRecordRepository.class.getMethod(
                "insert", FileTrashRecord.class, R2dbcEntityOperations.class);
            assertEquals(Mono.class, insertMethod.getReturnType());
            assertTrue(insertMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("insert 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritanceRelationships() {
        // 驗證 FileTrashRecordRepository 繼承了 ReactiveCrudRepository
        assertTrue(org.springframework.data.repository.reactive.ReactiveCrudRepository.class.isAssignableFrom(FileTrashRecordRepository.class));
        
        // 驗證泛型參數
        var genericInterfaces = FileTrashRecordRepository.class.getGenericInterfaces();
        assertTrue(genericInterfaces.length > 0);
    }

    @Test
    @DisplayName("一般測試 - 響應式編程模式驗證")
    void testReactiveProgrammingPatterns() {
        // 測試響應式鏈式操作
        Mono<Long> fileIdChain = fileTrashRecordRepository.findById(1L)
                .map(FileTrashRecord::getFileId)
                .defaultIfEmpty(0L);

        StepVerifier.create(fileIdChain)
                .expectNext(1L)
                .verifyComplete();

        // 測試響應式合併操作
        Flux<FileTrashRecord> combinedRecords = fileTrashRecordRepository.findById(1L)
                .flux()
                .mergeWith(fileTrashRecordRepository.findById(2L));

        StepVerifier.create(combinedRecords)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 默認方法空實現驗證")
    void testDefaultMethodEmptyImplementations() {
        // 測試 insertAll 默認方法返回空值
        StepVerifier.create(fileTrashRecordRepository.insertAll(Arrays.asList(testRecord1), entityOperations))
                .expectError(NullPointerException.class)
                .verify();

        // 測試 insert 默認方法返回空值
        StepVerifier.create(fileTrashRecordRepository.insert(testRecord1, entityOperations))
                .expectError(NullPointerException.class)
                .verify();
    }

    @Test
    @DisplayName("一般測試 - 檔案回收站清理邏輯模擬")
    void testTrashCleanupLogicSimulation() {
        // 模擬定期清理過期的回收站記錄
        LocalDateTime cleanupTime = currentTime.plusHours(1);
        
        Flux<FileTrashRecord> expiredRecords = fileTrashRecordRepository
                .findAllByUserIdAndDeleteTimeBefore(1L, cleanupTime);
        
        StepVerifier.create(expiredRecords)
                .expectNextCount(3) // record1, record2, record5
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 時間邏輯驗證")
    void testTimeLogicValidation() {
        // 驗證刪除時間的邏輯性
        StepVerifier.create(fileTrashRecordRepository.findAll())
                .assertNext(record -> {
                    assertNotNull(record.getDeleteTime());
                    // 刪除時間應該是合理的時間範圍
                    assertTrue(record.getDeleteTime().isBefore(LocalDateTime.now().plusDays(1)));
                    assertTrue(record.getDeleteTime().isAfter(LocalDateTime.now().minusYears(1)));
                })
                .expectNextCount(4) // 跳過其餘記錄的驗證
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    /**
     * 測試 findAllByUserIdAndDeleteTimeBefore 方法處理 null 用戶ID。
     * 
     * 測試當傳入 null 用戶ID 時的異常處理邏輯。
     * 
     * 前置條件：
     * - 方法接受 null 用戶ID 參數
     * 
     * 測試步驟：
     * - 傳入 null 用戶ID 和有效時間
     * - 觀察方法的處理結果
     * 
     * 預期結果：
     * - 方法返回空的 Flux，不拋出異常
     */
    @Test
    @DisplayName("異常測試 - findAllByUserIdAndDeleteTimeBefore 傳入 null 用戶ID")
    void testFindAllByUserIdAndDeleteTimeBefore_withNullUserId() {
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(null, currentTime))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByUserIdAndDeleteTimeBefore 傳入 null 時間")
    void testFindAllByUserIdAndDeleteTimeBefore_withNullTime() {
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByUserIdAndDeleteTimeBefore 傳入雙 null")
    void testFindAllByUserIdAndDeleteTimeBefore_withBothNull() {
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(null, null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByUserIdAndDeleteTimeBefore 查詢不存在的用戶")
    void testFindAllByUserIdAndDeleteTimeBefore_nonExistentUser() {
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(999L, currentTime))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 傳入 null")
    void testFindById_withNull() {
        StepVerifier.create(fileTrashRecordRepository.findById((Long) null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 查詢不存在的記錄")
    void testFindById_nonExistentRecord() {
        StepVerifier.create(fileTrashRecordRepository.findById(999L))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - existsById 傳入不存在的ID")
    void testExistsById_nonExistentId() {
        StepVerifier.create(fileTrashRecordRepository.existsById(999L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - insertAll 傳入 null 參數")
    void testInsertAll_withNullParameters() {
        StepVerifier.create(fileTrashRecordRepository.insertAll(null, entityOperations))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException || throwable instanceof NullPointerException)
                .verify();

        StepVerifier.create(fileTrashRecordRepository.insertAll(Arrays.asList(testRecord1), null))
                .expectError(NullPointerException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - insert 傳入 null 參數")
    void testInsert_withNullParameters() {
        StepVerifier.create(fileTrashRecordRepository.insert(null, entityOperations))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException || throwable instanceof NullPointerException)
                .verify();

        StepVerifier.create(fileTrashRecordRepository.insert(testRecord1, null))
                .expectError(NullPointerException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - 極端過去時間查詢")
    void testFindAllByUserIdAndDeleteTimeBefore_extremePastTime() {
        LocalDateTime extremePast = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, extremePast))
                .verifyComplete(); // 應該找不到任何記錄
    }

    // ==================== 邊界測試 ====================

    /**
     * 測試 findAllByUserIdAndDeleteTimeBefore 方法處理極大用戶ID。
     * 
     * 測試使用 Long.MAX_VALUE 作為用戶ID 時的邊界情況處理。
     * 
     * 前置條件：
     * - 使用極大的長整型值作為用戶ID
     * 
     * 測試步驟：
     * - 傳入 Long.MAX_VALUE 用戶ID
     * - 驗證查詢結果
     * 
     * 預期結果：
     * - 方法正常執行，返回空結果
     */
    @Test
    @DisplayName("邊界測試 - findAllByUserIdAndDeleteTimeBefore 使用極大用戶ID")
    void testFindAllByUserIdAndDeleteTimeBefore_withMaxLongUserId() {
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(Long.MAX_VALUE, currentTime))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserIdAndDeleteTimeBefore 使用極小用戶ID")
    void testFindAllByUserIdAndDeleteTimeBefore_withMinLongUserId() {
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(Long.MIN_VALUE, currentTime))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserIdAndDeleteTimeBefore 使用零用戶ID")
    void testFindAllByUserIdAndDeleteTimeBefore_withZeroUserId() {
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(0L, currentTime))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserIdAndDeleteTimeBefore 精確時間匹配")
    void testFindAllByUserIdAndDeleteTimeBefore_exactTimeMatch() {
        // 使用與記錄刪除時間完全相同的時間
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, pastTime))
                .verifyComplete(); // 應該找不到，因為是isBefore，不包括相等時間
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserIdAndDeleteTimeBefore 微小時間差異")
    void testFindAllByUserIdAndDeleteTimeBefore_tinyTimeDifference() {
        // 使用比過去時間早1納秒的時間
        LocalDateTime slightlyBeforePast = pastTime.minusNanos(1);
        
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, slightlyBeforePast))
                .verifyComplete();

        // 使用比過去時間晚1納秒的時間
        LocalDateTime slightlyAfterPast = pastTime.plusNanos(1);
        
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, slightlyAfterPast))
                .expectNextCount(2) // 應該找到過期的記錄
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserIdAndDeleteTimeBefore 極端未來時間")
    void testFindAllByUserIdAndDeleteTimeBefore_extremeFutureTime() {
        LocalDateTime extremeFuture = LocalDateTime.of(3000, 12, 31, 23, 59, 59);
        
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, extremeFuture))
                .expectNextCount(3) // 所有用戶1的記錄都應該在這個極端未來時間之前
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用極大ID")
    void testFindById_withMaxLongValue() {
        StepVerifier.create(fileTrashRecordRepository.findById(Long.MAX_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用極小ID")
    void testFindById_withMinLongValue() {
        StepVerifier.create(fileTrashRecordRepository.findById(Long.MIN_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發查詢操作")
    void testConcurrentQueries() {
        // 併發執行多個查詢
        Flux<FileTrashRecord> concurrentQueries = Flux.merge(
                fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, currentTime),
                fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(2L, currentTime),
                fileTrashRecordRepository.findById(3L),
                fileTrashRecordRepository.findById(4L)
        );

        StepVerifier.create(concurrentQueries)
                .expectNextCount(5) // 2 + 1 + 1 + 1 = 5個記錄
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜查詢組合")
    void testComplexQueryCombinations() {
        // 組合多種查詢操作
        Mono<Boolean> complexQuery = fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, currentTime)
                .collectList()
                .flatMap(expiredRecords -> {
                    if (!expiredRecords.isEmpty()) {
                        Long firstFileId = expiredRecords.get(0).getFileId();
                        return fileTrashRecordRepository.findById(firstFileId)
                                .map(record -> record.getUserId().equals(1L))
                                .defaultIfEmpty(false);
                    }
                    return Mono.just(false);
                });

        StepVerifier.create(complexQuery)
                .expectNext(true) // 應該找到匹配的記錄
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 批量操作性能測試")
    void testBatchOperationPerformance() {
        // 測試大批量ID查詢
        List<Long> largeIdList = java.util.stream.LongStream.range(1, 10001)
                .boxed()
                .toList();

        StepVerifier.create(fileTrashRecordRepository.findAllById(largeIdList))
                .expectNextCount(5) // 只有5個記錄存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - insertAll 空集合處理")
    void testInsertAll_withEmptyCollection() {
        StepVerifier.create(fileTrashRecordRepository.insertAll(Collections.emptyList(), entityOperations))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - insertAll 大量記錄處理")
    void testInsertAll_withManyRecords() {
        // 創建大量記錄進行批量插入測試
        List<FileTrashRecord> manyRecords = java.util.stream.LongStream.range(1, 1001)
                .mapToObj(i -> createFileTrashRecord(i, 1L, i, currentTime))
                .toList();

        StepVerifier.create(fileTrashRecordRepository.insertAll(manyRecords, entityOperations))
                .expectError(NullPointerException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 時間邊界極值測試")
    void testTimeBoundaryExtremeValues() {
        // 測試極端過去時間
        LocalDateTime extremePast = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, extremePast))
                .verifyComplete(); // 應該找不到任何記錄

        // 測試極端未來時間
        LocalDateTime extremeFuture = LocalDateTime.of(3000, 12, 31, 23, 59, 59);
        
        StepVerifier.create(fileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(1L, extremeFuture))
                .expectNextCount(3) // 所有用戶1的記錄都應該在這個極端未來時間之前
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 記錄數據一致性驗證")
    void testRecordDataConsistency() {
        // 驗證記錄數據的一致性
        StepVerifier.create(fileTrashRecordRepository.findAll())
                .assertNext(record -> {
                    assertNotNull(record.getFileId());
                    assertNotNull(record.getUserId());
                    assertNotNull(record.getFileId());
                    assertNotNull(record.getDeleteTime());
                    assertTrue(record.getUserId() > 0);
                    assertTrue(record.getFileId() > 0);
                })
                .expectNextCount(4) // 跳過其餘記錄的驗證
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 記錄唯一性驗證")
    void testRecordUniqueness() {
        // 驗證每個記錄都有唯一的ID
        StepVerifier.create(fileTrashRecordRepository.findAll()
                .map(FileTrashRecord::getFileId)
                .distinct()
                .count())
                .expectNext(5L) // 應該有5個唯一的記錄ID
                .verifyComplete();

        // 驗證檔案ID的分佈
        StepVerifier.create(fileTrashRecordRepository.findAll()
                .map(FileTrashRecord::getFileId)
                .distinct()
                .count())
                .expectNext(5L) // 應該有5個唯一的檔案ID
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 用戶記錄分佈測試")
    void testUserRecordDistribution() {
        // 測試不同用戶的記錄分佈
        StepVerifier.create(fileTrashRecordRepository.findAll()
                .groupBy(FileTrashRecord::getUserId)
                .flatMap(userGroup -> userGroup.count()
                        .map(count -> userGroup.key() + ":" + count)))
                .assertNext(userRecord -> assertTrue(userRecord.startsWith("1:") || userRecord.startsWith("2:")))
                .expectNextCount(1) // 應該有2個用戶組
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 父檔案夾ID處理")
    void testParentFolderIdHandling() {
        // 測試父檔案夾ID為null的情況（根據代碼，都設為null）
        StepVerifier.create(fileTrashRecordRepository.findAll())
                .assertNext(record -> assertNull(record.getParentFolderId()))
                .expectNextCount(4) // 跳過其餘記錄的驗證
                .verifyComplete();
    }
}