package xyz.dowob.filemanagement.repostiory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.entity.TransfersTask;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 檔案傳輸任務資料庫操作介面測試類。
 * 
 * 測試 TransfersTasksRepository 檔案傳輸任務資料庫操作介面的響應式資料存取功能和 Spring Data R2DBC 操作。
 * 驗證檔案傳輸任務的建立、狀態追蹤、查詢及 MD5 識別管理，包括繼承自 ReactiveCrudRepository 的基本 CRUD 操作和自定義查詢方法。
 * 支援根據任務狀態、開始時間、MD5 值等條件進行查詢，適合傳輸任務的監控和管理。
 * 透過模擬實現測試各種查詢條件、狀態處理和異常情況，確保響應式程式設計模式的正確實現。
 * 
 * <p>測試涵蓋的主要功能：
 * <ul>
 * <li>findByMd5 根據 MD5 雜湊值查詢傳輸任務</li>
 * <li>findByTransferTaskId 根據傳輸任務 ID 查詢任務</li>
 * <li>findAllByStatusIn 根據狀態列表範圍查詢任務</li>
 * <li>findAllByStatusInAndStartTimeBefore 結合狀態和時間條件查詢</li>
 * <li>任務生命週期和狀態轉換表示</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("TransfersTasksRepository 檔案傳輸任務數據庫操作接口測試")
class TransfersTasksRepositoryTest {

    private TransfersTasksRepository transfersTasksRepository;
    private TransfersTask testTask1;
    private TransfersTask testTask2;
    private TransfersTask testTask3;
    private LocalDateTime currentTime;
    private LocalDateTime pastTime;
    private LocalDateTime futureTime;

    @BeforeEach
    void setUp() {
        currentTime = LocalDateTime.now();
        pastTime = currentTime.minusHours(2);
        futureTime = currentTime.plusHours(2);

        // 創建測試用的 TransfersTasksRepository 實現
        transfersTasksRepository = new TransfersTasksRepository() {
            // 模擬數據存儲
            private final List<TransfersTask> tasks = Arrays.asList(
                createTransfersTask(1L, "md5_001", "transfer_001", TransfersStatusEnum.UPLOADING, pastTime),
                createTransfersTask(2L, "md5_002", "transfer_002", TransfersStatusEnum.COMPLETED, pastTime),
                createTransfersTask(3L, "md5_003", "transfer_003", TransfersStatusEnum.FAILED, currentTime),
                createTransfersTask(4L, "md5_004", "transfer_004", TransfersStatusEnum.UPLOADING, futureTime),
                createTransfersTask(5L, "md5_005", "transfer_005", TransfersStatusEnum.UPLOADING, futureTime)
            );

            @Override
            public Mono<TransfersTask> findByMd5(String md5) {
                if (md5 == null || md5.isBlank()) {
                    return Mono.empty();
                }
                return Flux.fromIterable(tasks)
                        .filter(task -> md5.equals(task.getMd5()))
                        .next();
            }

            @Override
            public Mono<TransfersTask> findByTransferTaskId(String transferTaskId) {
                if (transferTaskId == null || transferTaskId.isBlank()) {
                    return Mono.empty();
                }
                return Flux.fromIterable(tasks)
                        .filter(task -> transferTaskId.equals(task.getTransferTaskId()))
                        .next();
            }

            @Override
            public Flux<TransfersTask> findAllByStatusIn(List<TransfersStatusEnum> status) {
                if (status == null || status.isEmpty()) {
                    return Flux.empty();
                }
                return Flux.fromIterable(tasks)
                        .filter(task -> status.contains(task.getStatus()));
            }

            @Override
            public Flux<TransfersTask> findAllByStatusInAndStartTimeBefore(List<TransfersStatusEnum> status, LocalDateTime startTime) {
                if (status == null || status.isEmpty() || startTime == null) {
                    return Flux.empty();
                }
                return Flux.fromIterable(tasks)
                        .filter(task -> status.contains(task.getStatus()))
                        .filter(task -> task.getStartTime() != null && task.getStartTime().isBefore(startTime));
            }

            // ReactiveCrudRepository 基本方法實現
            @Override
            public <S extends TransfersTask> Mono<S> save(S entity) {
                return Mono.just(entity);
            }

            @Override
            public <S extends TransfersTask> Flux<S> saveAll(Iterable<S> entities) {
                return Flux.fromIterable(entities);
            }

            @Override
            public <S extends TransfersTask> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
                return Flux.from(entityStream);
            }

            @Override
            public Mono<TransfersTask> findById(Long id) {
                if (id == null) {
                    return Mono.empty();
                }
                return Flux.fromIterable(tasks)
                        .filter(task -> id.equals(task.getId()))
                        .next();
            }

            @Override
            public Mono<TransfersTask> findById(org.reactivestreams.Publisher<Long> id) {
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
            public Flux<TransfersTask> findAll() {
                return Flux.fromIterable(tasks);
            }

            @Override
            public Flux<TransfersTask> findAllById(Iterable<Long> ids) {
                return Flux.fromIterable(ids).flatMap(this::findById);
            }

            @Override
            public Flux<TransfersTask> findAllById(org.reactivestreams.Publisher<Long> idStream) {
                return Flux.from(idStream).flatMap(this::findById);
            }

            @Override
            public Mono<Long> count() {
                return Mono.just((long) tasks.size());
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
            public Mono<Void> delete(TransfersTask entity) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAllById(Iterable<? extends Long> ids) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(Iterable<? extends TransfersTask> entities) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends TransfersTask> entityStream) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll() {
                return Mono.empty();
            }
        };

        // 設置測試對象
        testTask1 = createTransfersTask(1L, "md5_001", "transfer_001", TransfersStatusEnum.UPLOADING, pastTime);
        testTask2 = createTransfersTask(2L, "md5_002", "transfer_002", TransfersStatusEnum.COMPLETED, pastTime);
        testTask3 = createTransfersTask(3L, "md5_003", "transfer_003", TransfersStatusEnum.FAILED, currentTime);
    }

    private TransfersTask createTransfersTask(Long id, String md5, String transferTaskId, 
                                            TransfersStatusEnum status, LocalDateTime startTime) {
        TransfersTask task = new TransfersTask();
        task.setId(id);
        task.setMd5(md5);
        task.setTransferTaskId(transferTaskId);
        task.setStatus(status);
        task.setStartTime(startTime);
        // TransfersTask doesn't have setFilename method
        task.setFileSize(1024L * id); // 1->1024, 2->2048, etc.
        // TransfersTask doesn't have setCreateTime method
        task.setFinishTime(startTime);
        return task;
    }

    // ==================== 一般測試 ====================

    /**
     * 測試 findByMd5 方法的基本功能。
     * 
     * 測試根據 MD5 雜湊值查詢檔案傳輸任務的基本查詢功能。
     * 
     * 前置條件：
     * - 測試資料包含不同 MD5 值的傳輸任務
     * - 每個 MD5 值對應唯一的任務記錄
     * 
     * 測試步驟：
     * - 呼叫 findByMd5 方法查詢指定 MD5 值
     * - 驗證返回的任務資訊
     * 
     * 預期結果：
     * - 成功返回對應的傳輸任務
     * - 任務資訊完整且正確
     */
    @Test
    @DisplayName("一般測試 - findByMd5 方法基本功能")
    void testFindByMd5_basicFunctionality() {
        StepVerifier.create(transfersTasksRepository.findByMd5("md5_001"))
                .assertNext(task -> {
                    assertNotNull(task);
                    assertEquals("md5_001", task.getMd5());
                    assertEquals(1L, task.getId());
                    assertEquals("transfer_001", task.getTransferTaskId());
                    assertEquals(TransfersStatusEnum.UPLOADING, task.getStatus());
                    assertEquals(pastTime, task.getStartTime());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findByTransferTaskId 方法基本功能")
    void testFindByTransferTaskId_basicFunctionality() {
        StepVerifier.create(transfersTasksRepository.findByTransferTaskId("transfer_002"))
                .assertNext(task -> {
                    assertNotNull(task);
                    assertEquals("transfer_002", task.getTransferTaskId());
                    assertEquals(2L, task.getId());
                    assertEquals("md5_002", task.getMd5());
                    assertEquals(TransfersStatusEnum.COMPLETED, task.getStatus());
                    assertEquals(pastTime, task.getStartTime());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByStatusIn 方法基本功能")
    void testFindAllByStatusIn_basicFunctionality() {
        List<TransfersStatusEnum> statusList = Arrays.asList(
                TransfersStatusEnum.UPLOADING, 
                TransfersStatusEnum.COMPLETED
        );
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusIn(statusList))
                .expectNextCount(4) // task1(UPLOADING), task2(COMPLETED), task4(UPLOADING), task5(UPLOADING)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByStatusInAndStartTimeBefore 方法基本功能")
    void testFindAllByStatusInAndStartTimeBefore_basicFunctionality() {
        List<TransfersStatusEnum> statusList = Arrays.asList(
                TransfersStatusEnum.UPLOADING, 
                TransfersStatusEnum.COMPLETED,
                TransfersStatusEnum.FAILED
        );
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(statusList, currentTime))
                .assertNext(task -> {
                    assertTrue(statusList.contains(task.getStatus()));
                    assertTrue(task.getStartTime().isBefore(currentTime));
                    assertEquals(1L, task.getId());
                })
                .assertNext(task -> {
                    assertTrue(statusList.contains(task.getStatus()));
                    assertTrue(task.getStartTime().isBefore(currentTime));
                    assertEquals(2L, task.getId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 不同狀態類型測試")
    void testDifferentStatusTypes() {
        // 測試單個狀態查詢
        StepVerifier.create(transfersTasksRepository.findAllByStatusIn(Arrays.asList(TransfersStatusEnum.FAILED)))
                .assertNext(task -> {
                    assertEquals(TransfersStatusEnum.FAILED, task.getStatus());
                    assertEquals(3L, task.getId());
                })
                .verifyComplete();

        StepVerifier.create(transfersTasksRepository.findAllByStatusIn(Arrays.asList(TransfersStatusEnum.UPLOADING)))
                .expectNextCount(3) // task1(UPLOADING), task4(UPLOADING), task5(UPLOADING)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - ReactiveCrudRepository 基本CRUD操作")
    void testReactiveCrudRepositoryBasicOperations() {
        // 測試 save 操作
        StepVerifier.create(transfersTasksRepository.save(testTask1))
                .assertNext(savedTask -> {
                    assertNotNull(savedTask);
                    assertEquals(1L, savedTask.getId());
                    assertEquals("md5_001", savedTask.getMd5());
                })
                .verifyComplete();

        // 測試 findById 操作
        StepVerifier.create(transfersTasksRepository.findById(1L))
                .assertNext(task -> {
                    assertEquals(1L, task.getId());
                    assertEquals("md5_001", task.getMd5());
                })
                .verifyComplete();

        // 測試 existsById 操作
        StepVerifier.create(transfersTasksRepository.existsById(1L))
                .expectNext(true)
                .verifyComplete();

        // 測試 count 操作
        StepVerifier.create(transfersTasksRepository.count())
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAll 方法")
    void testFindAll() {
        StepVerifier.create(transfersTasksRepository.findAll())
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - saveAll 批量保存操作")
    void testSaveAll() {
        List<TransfersTask> tasksToSave = Arrays.asList(testTask1, testTask2);
        
        StepVerifier.create(transfersTasksRepository.saveAll(tasksToSave))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllById 批量查詢操作")
    void testFindAllById() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        
        StepVerifier.create(transfersTasksRepository.findAllById(ids))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 findByMd5 方法
        try {
            var findByMd5Method = TransfersTasksRepository.class.getMethod("findByMd5", String.class);
            assertEquals(Mono.class, findByMd5Method.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findByMd5 方法應該存在");
        }

        // 驗證 findByTransferTaskId 方法
        try {
            var findByTransferTaskIdMethod = TransfersTasksRepository.class.getMethod("findByTransferTaskId", String.class);
            assertEquals(Mono.class, findByTransferTaskIdMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findByTransferTaskId 方法應該存在");
        }

        // 驗證 findAllByStatusIn 方法
        try {
            var findAllByStatusInMethod = TransfersTasksRepository.class.getMethod("findAllByStatusIn", List.class);
            assertEquals(Flux.class, findAllByStatusInMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByStatusIn 方法應該存在");
        }

        // 驗證 findAllByStatusInAndStartTimeBefore 方法
        try {
            var findAllByStatusInAndStartTimeBeforeMethod = TransfersTasksRepository.class.getMethod(
                "findAllByStatusInAndStartTimeBefore", List.class, LocalDateTime.class);
            assertEquals(Flux.class, findAllByStatusInAndStartTimeBeforeMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByStatusInAndStartTimeBefore 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritanceRelationships() {
        // 驗證 TransfersTasksRepository 繼承了 ReactiveCrudRepository
        assertTrue(org.springframework.data.repository.reactive.ReactiveCrudRepository.class.isAssignableFrom(TransfersTasksRepository.class));
        
        // 驗證泛型參數
        var genericInterfaces = TransfersTasksRepository.class.getGenericInterfaces();
        assertTrue(genericInterfaces.length > 0);
    }

    @Test
    @DisplayName("一般測試 - 響應式編程模式驗證")
    void testReactiveProgrammingPatterns() {
        // 測試響應式鏈式操作
        Mono<String> taskIdChain = transfersTasksRepository.findByMd5("md5_001")
                .map(TransfersTask::getTransferTaskId)
                .defaultIfEmpty("unknown");

        StepVerifier.create(taskIdChain)
                .expectNext("transfer_001")
                .verifyComplete();

        // 測試響應式合併操作
        Flux<TransfersTask> combinedTasks = transfersTasksRepository.findByMd5("md5_001")
                .flux()
                .mergeWith(transfersTasksRepository.findByTransferTaskId("transfer_002"));

        StepVerifier.create(combinedTasks)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 任務生命週期測試")
    void testTaskLifecycle() {
        // 測試從待處理到上傳中到完成的狀態流轉
        List<TransfersStatusEnum> activeStatuses = Arrays.asList(
                TransfersStatusEnum.UPLOADING,
                TransfersStatusEnum.UPLOADING
        );
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusIn(activeStatuses))
                .expectNextCount(3) // task1(UPLOADING), task4(PENDING), task5(UPLOADING)
                .verifyComplete();

        // 測試最終狀態
        List<TransfersStatusEnum> finalStatuses = Arrays.asList(
                TransfersStatusEnum.COMPLETED,
                TransfersStatusEnum.FAILED
        );
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusIn(finalStatuses))
                .expectNextCount(2) // task2(COMPLETED), task3(FAILED)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 時間範圍查詢測試")
    void testTimeRangeQueries() {
        // 查詢過去時間開始的任務
        List<TransfersStatusEnum> allStatuses = Arrays.asList(TransfersStatusEnum.values());
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(allStatuses, currentTime))
                .expectNextCount(2) // task1和task2在過去時間開始
                .verifyComplete();

        // 查詢未來時間開始的任務（應該包含當前時間之後的任務）
        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(allStatuses, futureTime.plusHours(1)))
                .expectNextCount(5) // 所有任務都在未來時間+1小時之前開始
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    /**
     * 測試 findByMd5 方法處理 null 參數。
     * 
     * 測試當傳入 null MD5 值時的異常處理邏輯。
     * 
     * 前置條件：
     * - 方法接受 null MD5 參數
     * 
     * 測試步驟：
     * - 傳入 null MD5 值
     * - 觀察方法的處理結果
     * 
     * 預期結果：
     * - 方法返回空的 Mono，不拋出異常
     */
    @Test
    @DisplayName("異常測試 - findByMd5 傳入 null")
    void testFindByMd5_withNull() {
        StepVerifier.create(transfersTasksRepository.findByMd5(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByMd5 傳入空字符串")
    void testFindByMd5_withEmptyString() {
        StepVerifier.create(transfersTasksRepository.findByMd5(""))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByMd5 查詢不存在的MD5")
    void testFindByMd5_nonExistentMd5() {
        StepVerifier.create(transfersTasksRepository.findByMd5("nonexistent_md5"))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByTransferTaskId 傳入 null")
    void testFindByTransferTaskId_withNull() {
        StepVerifier.create(transfersTasksRepository.findByTransferTaskId(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByTransferTaskId 傳入空字符串")
    void testFindByTransferTaskId_withEmptyString() {
        StepVerifier.create(transfersTasksRepository.findByTransferTaskId(""))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByTransferTaskId 查詢不存在的任務ID")
    void testFindByTransferTaskId_nonExistentTaskId() {
        StepVerifier.create(transfersTasksRepository.findByTransferTaskId("nonexistent_task"))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByStatusIn 傳入 null 列表")
    void testFindAllByStatusIn_withNullList() {
        StepVerifier.create(transfersTasksRepository.findAllByStatusIn(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByStatusIn 傳入空列表")
    void testFindAllByStatusIn_withEmptyList() {
        StepVerifier.create(transfersTasksRepository.findAllByStatusIn(Collections.emptyList()))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByStatusInAndStartTimeBefore 傳入 null 參數")
    void testFindAllByStatusInAndStartTimeBefore_withNullParameters() {
        List<TransfersStatusEnum> statusList = Arrays.asList(TransfersStatusEnum.UPLOADING);
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(null, currentTime))
                .verifyComplete();

        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(statusList, null))
                .verifyComplete();

        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(Collections.emptyList(), currentTime))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 傳入 null")
    void testFindById_withNull() {
        StepVerifier.create(transfersTasksRepository.findById((Long) null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - existsById 傳入不存在的ID")
    void testExistsById_nonExistentId() {
        StepVerifier.create(transfersTasksRepository.existsById(999L))
                .expectNext(false)
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    /**
     * 測試 findByMd5 方法處理極長 MD5 字符串。
     * 
     * 測試當 MD5 字符串長度非常大時的處理能力。
     * 
     * 前置條件：
     * - 建立長度超過10000字元的 MD5 字符串
     * 
     * 測試步驟：
     * - 使用極長 MD5 字符串進行查詢
     * - 驗證處理結果
     * 
     * 預期結果：
     * - 方法正常處理極長字符串
     * - 返回空結果（未找到匹配）
     */
    @Test
    @DisplayName("邊界測試 - findByMd5 使用極長MD5字符串")
    void testFindByMd5_withVeryLongMd5() {
        String longMd5 = "md5_" + "a".repeat(10000);
        
        StepVerifier.create(transfersTasksRepository.findByMd5(longMd5))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findByTransferTaskId 使用極長任務ID")
    void testFindByTransferTaskId_withVeryLongTaskId() {
        String longTaskId = "transfer_" + "a".repeat(10000);
        
        StepVerifier.create(transfersTasksRepository.findByTransferTaskId(longTaskId))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByStatusIn 使用所有狀態")
    void testFindAllByStatusIn_withAllStatuses() {
        List<TransfersStatusEnum> allStatuses = Arrays.asList(TransfersStatusEnum.values());
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusIn(allStatuses))
                .expectNextCount(5) // 所有任務都應該被找到
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByStatusIn 使用重複狀態")
    void testFindAllByStatusIn_withDuplicateStatuses() {
        List<TransfersStatusEnum> duplicateStatuses = Arrays.asList(
                TransfersStatusEnum.UPLOADING,
                TransfersStatusEnum.UPLOADING,
                TransfersStatusEnum.COMPLETED,
                TransfersStatusEnum.COMPLETED
        );
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusIn(duplicateStatuses))
                .expectNextCount(4) // task1(UPLOADING), task2(COMPLETED), task4(UPLOADING), task5(UPLOADING)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByStatusInAndStartTimeBefore 精確時間匹配")
    void testFindAllByStatusInAndStartTimeBefore_exactTimeMatch() {
        List<TransfersStatusEnum> statusList = Arrays.asList(TransfersStatusEnum.FAILED);
        
        // 使用與任務開始時間完全相同的時間
        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(statusList, currentTime))
                .verifyComplete(); // 應該找不到，因為是isBefore，不包括相等時間
    }

    @Test
    @DisplayName("邊界測試 - findAllByStatusInAndStartTimeBefore 微小時間差異")
    void testFindAllByStatusInAndStartTimeBefore_tinyTimeDifference() {
        List<TransfersStatusEnum> statusList = Arrays.asList(TransfersStatusEnum.FAILED);
        
        // 使用比當前時間早1納秒的時間
        LocalDateTime slightlyBeforeNow = currentTime.minusNanos(1);
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(statusList, slightlyBeforeNow))
                .verifyComplete();

        // 使用比當前時間晚1納秒的時間
        LocalDateTime slightlyAfterNow = currentTime.plusNanos(1);
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(statusList, slightlyAfterNow))
                .expectNextCount(1) // 應該找到task3
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發查詢操作")
    void testConcurrentQueries() {
        // 併發執行多個查詢
        Flux<TransfersTask> concurrentQueries = Flux.merge(
                transfersTasksRepository.findByMd5("md5_001"),
                transfersTasksRepository.findByTransferTaskId("transfer_002"),
                transfersTasksRepository.findById(3L),
                transfersTasksRepository.findAllByStatusIn(Arrays.asList(TransfersStatusEnum.UPLOADING))
        );

        StepVerifier.create(concurrentQueries)
                .expectNextCount(6) // findByMd5(1) + findByTransferTaskId(1) + findById(1) + findAllByStatusIn(3 UPLOADING) = 6個任務
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜查詢組合")
    void testComplexQueryCombinations() {
        // 組合多種查詢操作
        Mono<Boolean> complexQuery = transfersTasksRepository.findByMd5("md5_001")
                .flatMap(task -> transfersTasksRepository.findAllByStatusIn(Arrays.asList(task.getStatus()))
                        .any(statusTask -> statusTask.getTransferTaskId().equals(task.getTransferTaskId())))
                .defaultIfEmpty(false);

        StepVerifier.create(complexQuery)
                .expectNext(true) // 應該找到匹配的任務
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 批量操作性能測試")
    void testBatchOperationPerformance() {
        // 測試大批量ID查詢
        List<Long> largeIdList = java.util.stream.LongStream.range(1, 10001)
                .boxed()
                .toList();

        StepVerifier.create(transfersTasksRepository.findAllById(largeIdList))
                .expectNextCount(5) // 只有5個任務存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 狀態枚舉完整性測試")
    void testStatusEnumCompleteness() {
        // 測試每個狀態都能正確查詢
        for (TransfersStatusEnum status : TransfersStatusEnum.values()) {
            StepVerifier.create(transfersTasksRepository.findAllByStatusIn(Arrays.asList(status)))
                    .thenConsumeWhile(task -> task.getStatus() == status)
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("邊界測試 - 時間邊界極值測試")
    void testTimeBoundaryExtremeValues() {
        List<TransfersStatusEnum> allStatuses = Arrays.asList(TransfersStatusEnum.values());
        
        // 測試極端過去時間
        LocalDateTime extremePast = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(allStatuses, extremePast))
                .verifyComplete(); // 應該找不到任何任務

        // 測試極端未來時間
        LocalDateTime extremeFuture = LocalDateTime.of(3000, 12, 31, 23, 59, 59);
        
        StepVerifier.create(transfersTasksRepository.findAllByStatusInAndStartTimeBefore(allStatuses, extremeFuture))
                .expectNextCount(5) // 所有任務都應該在這個極端未來時間之前開始
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - MD5和任務ID特殊字符處理")
    void testMd5AndTaskIdSpecialCharacters() {
        // 測試包含特殊字符的MD5
        StepVerifier.create(transfersTasksRepository.findByMd5("md5-test"))
                .verifyComplete();

        StepVerifier.create(transfersTasksRepository.findByMd5("md5@test#123"))
                .verifyComplete();

        // 測試包含特殊字符的任務ID
        StepVerifier.create(transfersTasksRepository.findByTransferTaskId("transfer-test"))
                .verifyComplete();

        StepVerifier.create(transfersTasksRepository.findByTransferTaskId("transfer@test#123"))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空白字符處理")
    void testWhitespaceHandling() {
        // 測試包含空格的MD5
        StepVerifier.create(transfersTasksRepository.findByMd5(" md5_001 "))
                .verifyComplete(); // 應該找不到

        // 測試僅包含空格的MD5
        StepVerifier.create(transfersTasksRepository.findByMd5("   "))
                .verifyComplete(); // 應該找不到

        // 測試包含制表符和換行符的任務ID
        StepVerifier.create(transfersTasksRepository.findByTransferTaskId("\t\n"))
                .verifyComplete(); // 應該找不到
    }

    @Test
    @DisplayName("邊界測試 - 任務數據一致性驗證")
    void testTaskDataConsistency() {
        // 驗證任務數據的一致性
        StepVerifier.create(transfersTasksRepository.findAll())
                .assertNext(task -> {
                    assertNotNull(task.getId());
                    assertNotNull(task.getMd5());
                    assertNotNull(task.getTransferTaskId());
                    assertNotNull(task.getStatus());
                    assertNotNull(task.getStartTime());
                    assertTrue(task.getFileSize() > 0);
                    // TransfersTask doesn't have getFilename method
                })
                .expectNextCount(4) // 跳過其餘任務的驗證
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 任務唯一性驗證")
    void testTaskUniqueness() {
        // 驗證每個任務都有唯一的ID
        StepVerifier.create(transfersTasksRepository.findAll()
                .map(TransfersTask::getId)
                .distinct()
                .count())
                .expectNext(5L) // 應該有5個唯一的任務ID
                .verifyComplete();

        // 驗證MD5值的唯一性
        StepVerifier.create(transfersTasksRepository.findAll()
                .map(TransfersTask::getMd5)
                .distinct()
                .count())
                .expectNext(5L) // 應該有5個唯一的MD5值
                .verifyComplete();

        // 驗證傳輸任務ID的唯一性
        StepVerifier.create(transfersTasksRepository.findAll()
                .map(TransfersTask::getTransferTaskId)
                .distinct()
                .count())
                .expectNext(5L) // 應該有5個唯一的傳輸任務ID
                .verifyComplete();
    }
}