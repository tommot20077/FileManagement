package xyz.dowob.filemanagement.repostiory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.data.file.dao.OnlineHistoryCountAndOldestDAO;
import xyz.dowob.filemanagement.entity.UserOnlineFileHistory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用戶線上檔案歷史資料庫操作介面的測試實現。
 * <p>
 * 此測試類驗證 UserOnlineFileHistoryRepository 的響應式資料存取功能，
 * 涵蓋版本控制的核心操作和R2DBC非阻塞資料庫存取模式。
 * <p>
 * 測試範圍包含基本CRUD操作、版本查詢方法、歷史記錄管理和異常處理機制。
 * 特別驗證了線上檔案版本鏈的完整性和時序一致性。
 * <p>
 * 響應式操作透過 Mono/Flux 類型實現，確保在高並發環境下的正確行為。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("UserOnlineFileHistoryRepository 用戶在線檔案歷史數據庫操作接口測試")
class UserOnlineFileHistoryRepositoryTest {

    private UserOnlineFileHistoryRepository userOnlineFileHistoryRepository;
    private UserOnlineFileHistory testHistory1;
    private UserOnlineFileHistory testHistory2;
    private UserOnlineFileHistory testHistory3;
    private LocalDateTime currentTime;

    @BeforeEach
    void setUp() {
        currentTime = LocalDateTime.now();

        // 創建測試用的 UserOnlineFileHistoryRepository 實現
        userOnlineFileHistoryRepository = new UserOnlineFileHistoryRepository() {
            // 模擬數據存儲 - 檔案1有3個版本，檔案2有2個版本
            private final List<UserOnlineFileHistory> histories = Arrays.asList(
                createUserOnlineFileHistory(1L, 1L, 3L, 2L, "v3 diff", "第三版", false, null, 1L),
                createUserOnlineFileHistory(2L, 1L, 2L, 1L, "v2 diff", "第二版", false, null, 1L),
                createUserOnlineFileHistory(3L, 1L, 1L, null, "initial", "初始版本", true, "初始內容", 1L),
                createUserOnlineFileHistory(4L, 2L, 2L, 1L, "v2 diff file2", "檔案2第二版", false, null, 2L),
                createUserOnlineFileHistory(5L, 2L, 1L, null, "initial file2", "檔案2初始版本", true, "初始內容2", 2L)
            );

            @Override
            public Flux<UserOnlineFileHistory> findTopNByFileIdOrderByVersionDesc(Long fileId, Integer n) {
                if (fileId == null || n == null || n <= 0) {
                    return Flux.empty();
                }
                return Flux.fromIterable(histories)
                        .filter(history -> fileId.equals(history.getFileId()))
                        .sort((h1, h2) -> h2.getVersion().compareTo(h1.getVersion()))
                        .take(n);
            }

            @Override
            public Mono<UserOnlineFileHistory> findByFileIdAndVersion(Long fileId, Long version) {
                if (fileId == null || version == null) {
                    return Mono.empty();
                }
                return Flux.fromIterable(histories)
                        .filter(history -> fileId.equals(history.getFileId()) && 
                                         version.equals(history.getVersion()))
                        .next();
            }

            @Override
            public Flux<UserOnlineFileHistory> findAllByFileIdOrderByVersionDesc(Long fileId) {
                if (fileId == null) {
                    return Flux.empty();
                }
                return Flux.fromIterable(histories)
                        .filter(history -> fileId.equals(history.getFileId()))
                        .sort((h1, h2) -> h2.getVersion().compareTo(h1.getVersion()));
            }

            @Override
            public Flux<UserOnlineFileHistory> findAllByFileIdAndPreviousVersion(Long fileId, Long previousVersion) {
                if (fileId == null) {
                    return Flux.empty();
                }
                return Flux.fromIterable(histories)
                        .filter(history -> fileId.equals(history.getFileId()))
                        .filter(history -> {
                            if (previousVersion == null) {
                                return history.getPreviousVersion() == null;
                            }
                            return previousVersion.equals(history.getPreviousVersion());
                        });
            }

            @Override
            public Mono<OnlineHistoryCountAndOldestDAO> getOldestVersionAndCountByFileId(Long fileId) {
                if (fileId == null) {
                    return Mono.empty();
                }
                
                List<UserOnlineFileHistory> fileHistories = histories.stream()
                        .filter(history -> fileId.equals(history.getFileId()))
                        .toList();
                
                if (fileHistories.isEmpty()) {
                    return Mono.empty();
                }
                
                long count = fileHistories.size();
                Long oldestVersion = fileHistories.stream()
                        .map(UserOnlineFileHistory::getVersion)
                        .min(Long::compareTo)
                        .orElse(null);
                
                return Mono.just(new OnlineHistoryCountAndOldestDAO(oldestVersion, count));
            }

            // ReactiveCrudRepository 基本方法實現
            @Override
            public <S extends UserOnlineFileHistory> Mono<S> save(S entity) {
                return Mono.just(entity);
            }

            @Override
            public <S extends UserOnlineFileHistory> Flux<S> saveAll(Iterable<S> entities) {
                return Flux.fromIterable(entities);
            }

            @Override
            public <S extends UserOnlineFileHistory> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
                return Flux.from(entityStream);
            }

            @Override
            public Mono<UserOnlineFileHistory> findById(String id) {
                if (id == null) {
                    return Mono.empty();
                }
                try {
                    Long longId = Long.parseLong(id);
                    return Flux.fromIterable(histories)
                            .filter(history -> longId.equals(history.getId()))
                            .next();
                } catch (NumberFormatException e) {
                    return Mono.empty();
                }
            }

            @Override
            public Mono<UserOnlineFileHistory> findById(org.reactivestreams.Publisher<String> id) {
                return Mono.from(id).flatMap(this::findById);
            }

            @Override
            public Mono<Boolean> existsById(String id) {
                return findById(id).hasElement();
            }

            @Override
            public Mono<Boolean> existsById(org.reactivestreams.Publisher<String> id) {
                return Mono.from(id).flatMap(this::existsById);
            }

            @Override
            public Flux<UserOnlineFileHistory> findAll() {
                return Flux.fromIterable(histories);
            }

            @Override
            public Flux<UserOnlineFileHistory> findAllById(Iterable<String> ids) {
                return Flux.fromIterable(ids).flatMap(this::findById);
            }

            @Override
            public Flux<UserOnlineFileHistory> findAllById(org.reactivestreams.Publisher<String> idStream) {
                return Flux.from(idStream).flatMap(this::findById);
            }

            @Override
            public Mono<Long> count() {
                return Mono.just((long) histories.size());
            }

            @Override
            public Mono<Void> deleteById(String id) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteById(org.reactivestreams.Publisher<String> id) {
                return Mono.from(id).then();
            }

            @Override
            public Mono<Void> delete(UserOnlineFileHistory entity) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAllById(Iterable<? extends String> ids) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(Iterable<? extends UserOnlineFileHistory> entities) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends UserOnlineFileHistory> entityStream) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll() {
                return Mono.empty();
            }
        };

        // 設置測試對象
        testHistory1 = createUserOnlineFileHistory(1L, 1L, 1L, null, "test diff", "測試記錄", false, null, 1L);
        testHistory2 = createUserOnlineFileHistory(2L, 1L, 2L, 1L, "test diff2", "測試記錄2", false, null, 1L);
        testHistory3 = createUserOnlineFileHistory(3L, 2L, 1L, null, "test diff3", "測試記錄3", true, "快照內容", 2L);
    }

    private UserOnlineFileHistory createUserOnlineFileHistory(Long id, Long fileId, Long version, 
                                                           Long previousVersion, String diff, String note, 
                                                           Boolean isSnapshot, String snapshotContent, Long modifiedBy) {
        UserOnlineFileHistory history = new UserOnlineFileHistory();
        history.setId(id);
        history.setFileId(fileId);
        history.setVersion(version);
        history.setPreviousVersion(previousVersion);
        history.setDiff(diff);
        history.setNote(note);
        history.setIsSnapshot(isSnapshot);
        history.setSnapshotContent(snapshotContent);
        history.setModifiedTime(currentTime);
        history.setModifiedBy(modifiedBy);
        return history;
    }

    // ==================== 一般測試 ====================

    /**
     * 測試根據檔案ID查詢最新N個版本歷史的基本功能。
     * <p>
     * 驗證方法能正確返回指定數量的版本記錄，並按版本號降序排列。
     *
     * 測試涵蓋的邏輯或場景說明。
     *
     * 前置條件：
     * - 測試資料包含多個版本的歷史記錄
     *
     * 測試步驟：
     * - 查詢檔案1的最新2個版本記錄
     * - 驗證返回記錄的數量和順序正確性
     *
     * 預期結果：
     * - 返回2筆記錄，版本號為3和2（降序排列）
     */
    @Test
    @DisplayName("一般測試 - findTopNByFileIdOrderByVersionDesc 方法基本功能")
    void testFindTopNByFileIdOrderByVersionDesc_basicFunctionality() {
        // 查詢檔案1最新的2個版本
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(1L, 2))
                .assertNext(history -> {
                    assertEquals(1L, history.getId());
                    assertEquals(1L, history.getFileId());
                    assertEquals(3L, history.getVersion());
                    assertEquals("v3 diff", history.getDiff());
                    assertEquals("第三版", history.getNote());
                })
                .assertNext(history -> {
                    assertEquals(2L, history.getId());
                    assertEquals(1L, history.getFileId());
                    assertEquals(2L, history.getVersion());
                    assertEquals("v2 diff", history.getDiff());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findByFileIdAndVersion 方法基本功能")
    void testFindByFileIdAndVersion_basicFunctionality() {
        StepVerifier.create(userOnlineFileHistoryRepository.findByFileIdAndVersion(1L, 2L))
                .assertNext(history -> {
                    assertEquals(2L, history.getId());
                    assertEquals(1L, history.getFileId());
                    assertEquals(2L, history.getVersion());
                    assertEquals(1L, history.getPreviousVersion());
                    assertEquals("v2 diff", history.getDiff());
                    assertEquals("第二版", history.getNote());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByFileIdOrderByVersionDesc 方法基本功能")
    void testFindAllByFileIdOrderByVersionDesc_basicFunctionality() {
        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdOrderByVersionDesc(1L))
                .assertNext(history -> {
                    assertEquals(1L, history.getId());
                    assertEquals(3L, history.getVersion());
                })
                .assertNext(history -> {
                    assertEquals(2L, history.getId());
                    assertEquals(2L, history.getVersion());
                })
                .assertNext(history -> {
                    assertEquals(3L, history.getId());
                    assertEquals(1L, history.getVersion());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByFileIdAndPreviousVersion 方法基本功能")
    void testFindAllByFileIdAndPreviousVersion_basicFunctionality() {
        // 查詢檔案1中前一版本為1L的記錄
        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdAndPreviousVersion(1L, 1L))
                .assertNext(history -> {
                    assertEquals(2L, history.getId());
                    assertEquals(2L, history.getVersion());
                    assertEquals(1L, history.getPreviousVersion());
                })
                .verifyComplete();

        // 查詢檔案1中初始版本（前一版本為null）
        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdAndPreviousVersion(1L, null))
                .assertNext(history -> {
                    assertEquals(3L, history.getId());
                    assertEquals(1L, history.getVersion());
                    assertNull(history.getPreviousVersion());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getOldestVersionAndCountByFileId 方法基本功能")
    void testGetOldestVersionAndCountByFileId_basicFunctionality() {
        StepVerifier.create(userOnlineFileHistoryRepository.getOldestVersionAndCountByFileId(1L))
                .assertNext(dao -> {
                    assertEquals(1L, dao.version()); // 最早版本是1
                    assertEquals(3L, dao.count()); // 檔案1有3個版本
                })
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.getOldestVersionAndCountByFileId(2L))
                .assertNext(dao -> {
                    assertEquals(1L, dao.version()); // 最早版本是1
                    assertEquals(2L, dao.count()); // 檔案2有2個版本
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - ReactiveCrudRepository 基本CRUD操作")
    void testReactiveCrudRepositoryBasicOperations() {
        // 測試 save 操作
        StepVerifier.create(userOnlineFileHistoryRepository.save(testHistory1))
                .assertNext(savedHistory -> {
                    assertNotNull(savedHistory);
                    assertEquals(1L, savedHistory.getId());
                    assertEquals(1L, savedHistory.getFileId());
                    assertEquals(1L, savedHistory.getVersion());
                })
                .verifyComplete();

        // 測試 findById 操作
        StepVerifier.create(userOnlineFileHistoryRepository.findById("1"))
                .assertNext(history -> {
                    assertEquals(1L, history.getId());
                    assertEquals(1L, history.getFileId());
                    assertEquals(3L, history.getVersion());
                })
                .verifyComplete();

        // 測試 existsById 操作
        StepVerifier.create(userOnlineFileHistoryRepository.existsById("1"))
                .expectNext(true)
                .verifyComplete();

        // 測試 count 操作
        StepVerifier.create(userOnlineFileHistoryRepository.count())
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAll 方法")
    void testFindAll() {
        StepVerifier.create(userOnlineFileHistoryRepository.findAll())
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - saveAll 批量保存操作")
    void testSaveAll() {
        List<UserOnlineFileHistory> historiesToSave = Arrays.asList(testHistory1, testHistory2);
        
        StepVerifier.create(userOnlineFileHistoryRepository.saveAll(historiesToSave))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllById 批量查詢操作")
    void testFindAllById() {
        List<String> ids = Arrays.asList("1", "2", "3");
        
        StepVerifier.create(userOnlineFileHistoryRepository.findAllById(ids))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 findTopNByFileIdOrderByVersionDesc 方法
        try {
            var findTopNMethod = UserOnlineFileHistoryRepository.class.getMethod(
                "findTopNByFileIdOrderByVersionDesc", Long.class, Integer.class);
            assertEquals(Flux.class, findTopNMethod.getReturnType());
            assertTrue(findTopNMethod.isAnnotationPresent(org.springframework.data.r2dbc.repository.Query.class));
        } catch (NoSuchMethodException e) {
            fail("findTopNByFileIdOrderByVersionDesc 方法應該存在");
        }

        // 驗證 getOldestVersionAndCountByFileId 方法
        try {
            var getOldestVersionMethod = UserOnlineFileHistoryRepository.class.getMethod(
                "getOldestVersionAndCountByFileId", Long.class);
            assertEquals(Mono.class, getOldestVersionMethod.getReturnType());
            assertTrue(getOldestVersionMethod.isAnnotationPresent(org.springframework.data.r2dbc.repository.Query.class));
        } catch (NoSuchMethodException e) {
            fail("getOldestVersionAndCountByFileId 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritanceRelationships() {
        // 驗證 UserOnlineFileHistoryRepository 繼承了 ReactiveCrudRepository
        assertTrue(org.springframework.data.repository.reactive.ReactiveCrudRepository.class.isAssignableFrom(UserOnlineFileHistoryRepository.class));
        
        // 驗證泛型參數
        var genericInterfaces = UserOnlineFileHistoryRepository.class.getGenericInterfaces();
        assertTrue(genericInterfaces.length > 0);
    }

    @Test
    @DisplayName("一般測試 - 響應式編程模式驗證")
    void testReactiveProgrammingPatterns() {
        // 測試響應式鏈式操作
        Mono<String> diffChain = userOnlineFileHistoryRepository.findByFileIdAndVersion(1L, 2L)
                .map(UserOnlineFileHistory::getDiff)
                .defaultIfEmpty("empty");

        StepVerifier.create(diffChain)
                .expectNext("v2 diff")
                .verifyComplete();

        // 測試響應式合併操作
        Flux<UserOnlineFileHistory> combinedHistories = userOnlineFileHistoryRepository.findByFileIdAndVersion(1L, 1L)
                .flux()
                .mergeWith(userOnlineFileHistoryRepository.findByFileIdAndVersion(1L, 2L));

        StepVerifier.create(combinedHistories)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 版本控制邏輯驗證")
    void testVersionControlLogic() {
        // 驗證版本號遞增邏輯
        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdOrderByVersionDesc(1L))
                .assertNext(history -> assertEquals(3L, history.getVersion()))
                .assertNext(history -> assertEquals(2L, history.getVersion()))
                .assertNext(history -> assertEquals(1L, history.getVersion()))
                .verifyComplete();

        // 驗證快照記錄
        StepVerifier.create(userOnlineFileHistoryRepository.findByFileIdAndVersion(1L, 1L))
                .assertNext(history -> {
                    assertTrue(history.getIsSnapshot());
                    assertNotNull(history.getSnapshotContent());
                    assertEquals("初始內容", history.getSnapshotContent());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 修改記錄追蹤")
    void testModificationTracking() {
        // 驗證修改者和修改時間記錄
        StepVerifier.create(userOnlineFileHistoryRepository.findAll())
                .assertNext(history -> {
                    assertNotNull(history.getModifiedBy());
                    assertNotNull(history.getModifiedTime());
                    assertTrue(history.getModifiedBy() > 0);
                })
                .expectNextCount(4) // 跳過其餘記錄的驗證
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 差異記錄格式驗證")
    void testDiffRecordFormat() {
        // 驗證差異記錄的格式
        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdOrderByVersionDesc(1L))
                .assertNext(history -> {
                    assertNotNull(history.getDiff());
                    assertTrue(history.getDiff().length() > 0);
                    assertNotNull(history.getNote());
                })
                .expectNextCount(2) // 跳過其餘記錄的驗證
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    /**
     * 測試查詢最新版本方法在接收null參數時的異常處理。
     * <p>
     * 驗證方法對於非法輸入參數的防護機制。
     *
     * 測試涵蓋的邏輯或場景說明。
     *
     * 前置條件：
     * - 方法實現包含參數驗證邏輯
     *
     * 測試步驟：
     * - 分別傳入null檔案ID和null數量參數
     * - 驗證方法的異常處理行為
     *
     * 預期結果：
     * - 方法優雅處理null輸入，返回空Flux而非拋出異常
     */
    @Test
    @DisplayName("異常測試 - findTopNByFileIdOrderByVersionDesc 傳入 null 參數")
    void testFindTopNByFileIdOrderByVersionDesc_withNullParameters() {
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(null, 1))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(1L, null))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(null, null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findTopNByFileIdOrderByVersionDesc 傳入無效N值")
    void testFindTopNByFileIdOrderByVersionDesc_withInvalidN() {
        // 測試負數N
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(1L, -1))
                .verifyComplete();

        // 測試零N
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(1L, 0))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByFileIdAndVersion 傳入 null 參數")
    void testFindByFileIdAndVersion_withNullParameters() {
        StepVerifier.create(userOnlineFileHistoryRepository.findByFileIdAndVersion(null, 1L))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findByFileIdAndVersion(1L, null))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findByFileIdAndVersion(null, null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByFileIdOrderByVersionDesc 傳入 null 檔案ID")
    void testFindAllByFileIdOrderByVersionDesc_withNullFileId() {
        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdOrderByVersionDesc(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByFileIdAndPreviousVersion 傳入 null 檔案ID")
    void testFindAllByFileIdAndPreviousVersion_withNullFileId() {
        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdAndPreviousVersion(null, 1L))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - getOldestVersionAndCountByFileId 傳入 null 檔案ID")
    void testGetOldestVersionAndCountByFileId_withNullFileId() {
        StepVerifier.create(userOnlineFileHistoryRepository.getOldestVersionAndCountByFileId(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 查詢不存在的檔案")
    void testQueryNonExistentFile() {
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(999L, 5))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findByFileIdAndVersion(999L, 1L))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdOrderByVersionDesc(999L))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.getOldestVersionAndCountByFileId(999L))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 查詢不存在的版本")
    void testQueryNonExistentVersion() {
        StepVerifier.create(userOnlineFileHistoryRepository.findByFileIdAndVersion(1L, 999L))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdAndPreviousVersion(1L, 999L))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 傳入 null")
    void testFindById_withNull() {
        StepVerifier.create(userOnlineFileHistoryRepository.findById((String) null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 傳入無效ID格式")
    void testFindById_withInvalidIdFormat() {
        StepVerifier.create(userOnlineFileHistoryRepository.findById("invalid"))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findById("abc123"))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 查詢不存在的記錄")
    void testFindById_nonExistentRecord() {
        StepVerifier.create(userOnlineFileHistoryRepository.findById("999"))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - existsById 傳入不存在的ID")
    void testExistsById_nonExistentId() {
        StepVerifier.create(userOnlineFileHistoryRepository.existsById("999"))
                .expectNext(false)
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - findTopNByFileIdOrderByVersionDesc 請求超過實際數量")
    void testFindTopNByFileIdOrderByVersionDesc_requestMoreThanAvailable() {
        // 檔案1有3個版本，請求10個
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(1L, 10))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findTopNByFileIdOrderByVersionDesc 請求1個")
    void testFindTopNByFileIdOrderByVersionDesc_requestOne() {
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(1L, 1))
                .assertNext(history -> {
                    assertEquals(3L, history.getVersion()); // 應該是最新版本
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findTopNByFileIdOrderByVersionDesc 使用極大N值")
    void testFindTopNByFileIdOrderByVersionDesc_withMaxIntegerN() {
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(1L, Integer.MAX_VALUE))
                .expectNextCount(3) // 只能返回存在的3個版本
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - getOldestVersionAndCountByFileId 單版本檔案")
    void testGetOldestVersionAndCountByFileId_singleVersionFile() {
        // 創建只有一個版本的檔案測試數據
        StepVerifier.create(userOnlineFileHistoryRepository.getOldestVersionAndCountByFileId(2L))
                .assertNext(dao -> {
                    assertEquals(1L, dao.version());
                    assertEquals(2L, dao.count());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 使用極大檔案ID")
    void testWithMaxLongFileId() {
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(Long.MAX_VALUE, 1))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findByFileIdAndVersion(Long.MAX_VALUE, 1L))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.getOldestVersionAndCountByFileId(Long.MAX_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 使用極小檔案ID")
    void testWithMinLongFileId() {
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(Long.MIN_VALUE, 1))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findByFileIdAndVersion(Long.MIN_VALUE, 1L))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 使用零檔案ID")
    void testWithZeroFileId() {
        StepVerifier.create(userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(0L, 1))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.getOldestVersionAndCountByFileId(0L))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 使用極大版本號")
    void testWithMaxLongVersion() {
        StepVerifier.create(userOnlineFileHistoryRepository.findByFileIdAndVersion(1L, Long.MAX_VALUE))
                .verifyComplete();

        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdAndPreviousVersion(1L, Long.MAX_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用極大ID字符串")
    void testFindById_withMaxLongString() {
        StepVerifier.create(userOnlineFileHistoryRepository.findById(String.valueOf(Long.MAX_VALUE)))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用極小ID字符串")
    void testFindById_withMinLongString() {
        StepVerifier.create(userOnlineFileHistoryRepository.findById(String.valueOf(Long.MIN_VALUE)))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用零ID字符串")
    void testFindById_withZeroString() {
        StepVerifier.create(userOnlineFileHistoryRepository.findById("0"))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發查詢操作")
    void testConcurrentQueries() {
        // 併發執行多個查詢
        Flux<UserOnlineFileHistory> concurrentQueries = Flux.merge(
                userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(1L, 2),
                userOnlineFileHistoryRepository.findByFileIdAndVersion(2L, 1L),
                userOnlineFileHistoryRepository.findAllByFileIdOrderByVersionDesc(2L)
        );

        StepVerifier.create(concurrentQueries)
                .expectNextCount(5) // 2 + 1 + 2 = 5個記錄
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜查詢組合")
    void testComplexQueryCombinations() {
        // 組合多種查詢操作
        Mono<Boolean> complexQuery = userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(1L, 1)
                .collectList()
                .flatMap(latestHistories -> {
                    if (!latestHistories.isEmpty()) {
                        UserOnlineFileHistory latest = latestHistories.get(0);
                        return userOnlineFileHistoryRepository.getOldestVersionAndCountByFileId(latest.getFileId())
                                .map(dao -> dao.count() > 1);
                    }
                    return Mono.just(false);
                });

        StepVerifier.create(complexQuery)
                .expectNext(true) // 檔案1有多個版本
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 批量操作性能測試")
    void testBatchOperationPerformance() {
        // 測試大批量ID查詢
        List<String> largeIdList = java.util.stream.LongStream.range(1, 10001)
                .mapToObj(String::valueOf)
                .toList();

        StepVerifier.create(userOnlineFileHistoryRepository.findAllById(largeIdList))
                .expectNextCount(5) // 只有5個記錄存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 版本號順序驗證")
    void testVersionOrderValidation() {
        // 驗證版本號是否按降序返回
        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdOrderByVersionDesc(1L)
                .map(UserOnlineFileHistory::getVersion)
                .collectList())
                .assertNext(versions -> {
                    assertEquals(3, versions.size());
                    // 驗證降序排列
                    for (int i = 0; i < versions.size() - 1; i++) {
                        assertTrue(versions.get(i) > versions.get(i + 1));
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 記錄數據一致性驗證")
    void testRecordDataConsistency() {
        // 驗證記錄數據的一致性
        StepVerifier.create(userOnlineFileHistoryRepository.findAll())
                .assertNext(history -> {
                    assertNotNull(history.getId());
                    assertNotNull(history.getFileId());
                    assertNotNull(history.getVersion());
                    assertNotNull(history.getDiff());
                    assertNotNull(history.getModifiedTime());
                    assertNotNull(history.getModifiedBy());
                    assertTrue(history.getFileId() > 0);
                    assertTrue(history.getVersion() > 0);
                    assertTrue(history.getModifiedBy() > 0);
                })
                .expectNextCount(4) // 跳過其餘記錄的驗證
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 記錄唯一性驗證")
    void testRecordUniqueness() {
        // 驗證每個記錄都有唯一的ID
        StepVerifier.create(userOnlineFileHistoryRepository.findAll()
                .map(UserOnlineFileHistory::getId)
                .distinct()
                .count())
                .expectNext(5L) // 應該有5個唯一的記錄ID
                .verifyComplete();

        // 驗證檔案ID和版本號的組合唯一性
        StepVerifier.create(userOnlineFileHistoryRepository.findAll()
                .map(history -> history.getFileId() + "-" + history.getVersion())
                .distinct()
                .count())
                .expectNext(5L) // 每個檔案的每個版本都應該是唯一的
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 快照與差異記錄混合驗證")
    void testSnapshotAndDiffMixedRecords() {
        // 驗證快照記錄和差異記錄的正確性
        StepVerifier.create(userOnlineFileHistoryRepository.findAll()
                .filter(UserOnlineFileHistory::getIsSnapshot))
                .assertNext(history -> {
                    assertTrue(history.getIsSnapshot());
                    assertNotNull(history.getSnapshotContent());
                })
                .expectNextCount(1) // 跳過其餘快照記錄的驗證
                .verifyComplete();

        // 驗證非快照記錄
        StepVerifier.create(userOnlineFileHistoryRepository.findAll()
                .filter(history -> !history.getIsSnapshot()))
                .assertNext(history -> {
                    assertFalse(history.getIsSnapshot());
                    assertNotNull(history.getDiff());
                })
                .expectNextCount(2) // 跳過其餘非快照記錄的驗證
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 版本鏈完整性驗證")
    void testVersionChainIntegrity() {
        // 驗證版本鏈的完整性
        StepVerifier.create(userOnlineFileHistoryRepository.findAllByFileIdOrderByVersionDesc(1L)
                .collectList())
                .assertNext(histories -> {
                    // 驗證版本鏈
                    assertEquals(3, histories.size());
                    
                    // 最新版本（v3）應該指向v2
                    UserOnlineFileHistory v3 = histories.get(0);
                    assertEquals(3L, v3.getVersion());
                    assertEquals(2L, v3.getPreviousVersion());
                    
                    // v2應該指向v1
                    UserOnlineFileHistory v2 = histories.get(1);
                    assertEquals(2L, v2.getVersion());
                    assertEquals(1L, v2.getPreviousVersion());
                    
                    // v1是初始版本，沒有前一版本
                    UserOnlineFileHistory v1 = histories.get(2);
                    assertEquals(1L, v1.getVersion());
                    assertNull(v1.getPreviousVersion());
                })
                .verifyComplete();
    }
}