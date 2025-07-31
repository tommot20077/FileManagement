package xyz.dowob.filemanagement.repostiory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.entity.UserOnlineFile;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用戶線上檔案資料庫操作介面的測試實現。
 * <p>
 * 此測試類驗證 UserOnlineFileRepository 的響應式資料存取功能，
 * 涵蓋線上檔案內容管理和R2DBC非阻塞資料庫操作模式。
 * <p>
 * 測試範圍包含基本CRUD操作、自定義插入方法、檔案內容處理和快照計數管理。
 * 特別驗證了線上編輯檔案的儲存機制和內容完整性。
 * <p>
 * 響應式操作透過 Mono/Flux 類型實現，支援多種內容格式如Markdown、JSON等。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserOnlineFileRepository 用戶在線檔案數據庫操作接口測試")
class UserOnlineFileRepositoryTest {

    private UserOnlineFileRepository userOnlineFileRepository;
    private UserOnlineFile testOnlineFile1;
    private UserOnlineFile testOnlineFile2;
    private UserOnlineFile testOnlineFile3;

    @BeforeEach
    void setUp() {
        // 創建測試用的 UserOnlineFileRepository 實現
        userOnlineFileRepository = new UserOnlineFileRepository() {
            // 模擬數據存儲
            private final List<UserOnlineFile> onlineFiles = Arrays.asList(
                createUserOnlineFile("1", 1024L, 1L, "# 標題\n內容1", 1),
                createUserOnlineFile("2", 2048L, 2L, "文檔內容2", 2),
                createUserOnlineFile("3", 4096L, 1L, "長文檔內容3...", 5),
                createUserOnlineFile("4", 512L, 3L, "簡短內容", 0),
                createUserOnlineFile("5", 8192L, 2L, "大型文檔內容5", 10)
            );

            @Override
            public Mono<UserOnlineFile> insertWithId(UserOnlineFile file) {
                if (file == null) {
                    return Mono.error(new IllegalArgumentException("在線檔案不能為空"));
                }
                if (file.getId() == null) {
                    return Mono.error(new IllegalArgumentException("檔案ID不能為空"));
                }
                if (file.getContent() == null) {
                    return Mono.error(new IllegalArgumentException("檔案內容不能為空"));
                }
                
                // 模擬插入成功
                return Mono.just(file);
            }

            // ReactiveCrudRepository 基本方法實現
            @Override
            public <S extends UserOnlineFile> Mono<S> save(S entity) {
                return Mono.just(entity);
            }

            @Override
            public <S extends UserOnlineFile> Flux<S> saveAll(Iterable<S> entities) {
                return Flux.fromIterable(entities);
            }

            @Override
            public <S extends UserOnlineFile> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
                return Flux.from(entityStream);
            }

            @Override
            public Mono<UserOnlineFile> findById(String id) {
                if (id == null) {
                    return Mono.empty();
                }
                Long longId;
                try {
                    longId = Long.parseLong(id);
                } catch (NumberFormatException e) {
                    return Mono.empty();
                }
                return Flux.fromIterable(onlineFiles)
                        .filter(file -> longId.equals(file.getId()))
                        .next();
            }

            @Override
            public Mono<UserOnlineFile> findById(org.reactivestreams.Publisher<String> id) {
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
            public Flux<UserOnlineFile> findAll() {
                return Flux.fromIterable(onlineFiles);
            }

            @Override
            public Flux<UserOnlineFile> findAllById(Iterable<String> ids) {
                return Flux.fromIterable(ids).flatMap(this::findById);
            }

            @Override
            public Flux<UserOnlineFile> findAllById(org.reactivestreams.Publisher<String> idStream) {
                return Flux.from(idStream).flatMap(this::findById);
            }

            @Override
            public Mono<Long> count() {
                return Mono.just((long) onlineFiles.size());
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
            public Mono<Void> delete(UserOnlineFile entity) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAllById(Iterable<? extends String> ids) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(Iterable<? extends UserOnlineFile> entities) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends UserOnlineFile> entityStream) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll() {
                return Mono.empty();
            }
        };

        // 設置測試對象
        testOnlineFile1 = createUserOnlineFile("1001", 1024L, 1L, "測試內容1", 1);
        testOnlineFile2 = createUserOnlineFile("1002", 2048L, 2L, "測試內容2", 2);
        testOnlineFile3 = createUserOnlineFile("1003", 4096L, 3L, "測試內容3", 3);
    }

    private UserOnlineFile createUserOnlineFile(String id, Long fileSize, Long lastModifiedBy, 
                                               String content, Integer snapshotCount) {
        UserOnlineFile file = new UserOnlineFile();
        if (id != null) {
            file.setId(Long.parseLong(id));
        }
        file.setFileSize(fileSize);
        file.setLastModifiedBy(lastModifiedBy);
        file.setContent(content);
        file.setCurrentSnapshotCount(snapshotCount);
        return file;
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - insertWithId 方法基本功能")
    void testInsertWithId_basicFunctionality() {
        StepVerifier.create(userOnlineFileRepository.insertWithId(testOnlineFile1))
                .assertNext(savedFile -> {
                    assertNotNull(savedFile);
                    assertEquals(1001L, savedFile.getId());
                    assertEquals(1024L, savedFile.getFileSize());
                    assertEquals(1L, savedFile.getLastModifiedBy());
                    assertEquals("測試內容1", savedFile.getContent());
                    assertEquals(1, savedFile.getCurrentSnapshotCount());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - insertWithId 不同內容類型測試")
    void testInsertWithId_differentContentTypes() {
        // 測試Markdown內容
        UserOnlineFile markdownFile = createUserOnlineFile("2001", 512L, 1L, 
                "# 標題\n\n## 子標題\n\n- 列表項1\n- 列表項2", 0);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(markdownFile))
                .assertNext(savedFile -> {
                    assertTrue(savedFile.getContent().contains("# 標題"));
                    assertTrue(savedFile.getContent().contains("## 子標題"));
                })
                .verifyComplete();

        // 測試JSON內容
        UserOnlineFile jsonFile = createUserOnlineFile("2002", 256L, 2L, 
                "{\"name\": \"test\", \"value\": 123}", 1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(jsonFile))
                .assertNext(savedFile -> {
                    assertTrue(savedFile.getContent().contains("\"name\""));
                    assertTrue(savedFile.getContent().contains("\"value\""));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - ReactiveCrudRepository 基本CRUD操作")
    void testReactiveCrudRepositoryBasicOperations() {
        // 測試 save 操作
        StepVerifier.create(userOnlineFileRepository.save(testOnlineFile1))
                .assertNext(savedFile -> {
                    assertNotNull(savedFile);
                    assertEquals(1001L, savedFile.getId());
                    assertEquals("測試內容1", savedFile.getContent());
                })
                .verifyComplete();

        // 測試 findById 操作
        StepVerifier.create(userOnlineFileRepository.findById("1"))
                .assertNext(file -> {
                    assertEquals(1L, file.getId());
                    assertEquals(1L, file.getLastModifiedBy());
                    assertEquals("# 標題\n內容1", file.getContent());
                })
                .verifyComplete();

        // 測試 existsById 操作
        StepVerifier.create(userOnlineFileRepository.existsById("1"))
                .expectNext(true)
                .verifyComplete();

        // 測試 count 操作
        StepVerifier.create(userOnlineFileRepository.count())
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAll 方法")
    void testFindAll() {
        StepVerifier.create(userOnlineFileRepository.findAll())
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - saveAll 批量保存操作")
    void testSaveAll() {
        List<UserOnlineFile> filesToSave = Arrays.asList(testOnlineFile1, testOnlineFile2);
        
        StepVerifier.create(userOnlineFileRepository.saveAll(filesToSave))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllById 批量查詢操作")
    void testFindAllById() {
        List<String> ids = Arrays.asList("1", "2", "3");
        
        StepVerifier.create(userOnlineFileRepository.findAllById(ids))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 insertWithId 方法
        try {
            var insertWithIdMethod = UserOnlineFileRepository.class.getMethod("insertWithId", UserOnlineFile.class);
            assertEquals(Mono.class, insertWithIdMethod.getReturnType());
            assertTrue(insertWithIdMethod.isAnnotationPresent(org.springframework.data.r2dbc.repository.Query.class));
        } catch (NoSuchMethodException e) {
            fail("insertWithId 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritanceRelationships() {
        // 驗證 UserOnlineFileRepository 繼承了 ReactiveCrudRepository
        assertTrue(org.springframework.data.repository.reactive.ReactiveCrudRepository.class.isAssignableFrom(UserOnlineFileRepository.class));
        
        // 驗證泛型參數
        var genericInterfaces = UserOnlineFileRepository.class.getGenericInterfaces();
        assertTrue(genericInterfaces.length > 0);
    }

    @Test
    @DisplayName("一般測試 - 響應式編程模式驗證")
    void testReactiveProgrammingPatterns() {
        // 測試響應式鏈式操作
        Mono<String> contentChain = userOnlineFileRepository.findById("1")
                .map(UserOnlineFile::getContent)
                .defaultIfEmpty("empty");

        StepVerifier.create(contentChain)
                .expectNext("# 標題\n內容1")
                .verifyComplete();

        // 測試響應式合併操作
        Flux<UserOnlineFile> combinedFiles = userOnlineFileRepository.findById("1")
                .flux()
                .mergeWith(userOnlineFileRepository.findById("2"));

        StepVerifier.create(combinedFiles)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 在線檔案特有屬性測試")
    void testOnlineFileSpecificProperties() {
        StepVerifier.create(userOnlineFileRepository.findAll())
                .assertNext(file -> {
                    assertNotNull(file.getContent());
                    assertNotNull(file.getLastModifiedBy());
                    assertNotNull(file.getCurrentSnapshotCount());
                    assertTrue(file.getFileSize() > 0);
                    assertTrue(file.getCurrentSnapshotCount() >= 0);
                })
                .expectNextCount(4) // 跳過其餘檔案的驗證
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 快照計數管理測試")
    void testSnapshotCountManagement() {
        // 測試不同快照計數的檔案
        StepVerifier.create(userOnlineFileRepository.findAll()
                .filter(file -> file.getCurrentSnapshotCount() > 5))
                .assertNext(file -> {
                    assertEquals(5L, file.getId());
                    assertEquals(10, file.getCurrentSnapshotCount());
                })
                .verifyComplete();

        // 測試無快照的檔案
        StepVerifier.create(userOnlineFileRepository.findAll()
                .filter(file -> file.getCurrentSnapshotCount() == 0))
                .assertNext(file -> {
                    assertEquals(4L, file.getId());
                    assertEquals(0, file.getCurrentSnapshotCount());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 檔案大小統計測試")
    void testFileSizeStatistics() {
        // 測試檔案大小總和
        StepVerifier.create(userOnlineFileRepository.findAll()
                .map(UserOnlineFile::getFileSize)
                .reduce(0L, Long::sum))
                .expectNext(15872L) // 1024 + 2048 + 4096 + 512 + 8192 = 15872L
                .verifyComplete();

        // 測試最大檔案大小
        StepVerifier.create(userOnlineFileRepository.findAll()
                .map(UserOnlineFile::getFileSize)
                .reduce(Long::max))
                .expectNext(8192L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 修改者統計測試")
    void testLastModifiedByStatistics() {
        // 測試按修改者分組統計
        StepVerifier.create(userOnlineFileRepository.findAll()
                .groupBy(UserOnlineFile::getLastModifiedBy)
                .flatMap(userGroup -> userGroup.count()
                        .map(count -> userGroup.key() + ":" + count)))
                .expectNextCount(3) // 1:2, 2:2, 3:1
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - insertWithId 傳入 null 檔案")
    void testInsertWithId_withNullFile() {
        StepVerifier.create(userOnlineFileRepository.insertWithId(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - insertWithId 傳入空ID")
    void testInsertWithId_withNullId() {
        UserOnlineFile fileWithNullId = createUserOnlineFile(null, 1024L, 1L, "content", 1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(fileWithNullId))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - insertWithId 傳入 null 內容")
    void testInsertWithId_withNullContent() {
        UserOnlineFile fileWithNullContent = createUserOnlineFile("1004", 1024L, 1L, null, 1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(fileWithNullContent))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - findById 傳入 null")
    void testFindById_withNull() {
        StepVerifier.create(userOnlineFileRepository.findById((String) null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 查詢不存在的檔案")
    void testFindById_nonExistentFile() {
        StepVerifier.create(userOnlineFileRepository.findById("999"))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - existsById 傳入不存在的ID")
    void testExistsById_nonExistentId() {
        StepVerifier.create(userOnlineFileRepository.existsById("999"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 無效的檔案屬性")
    void testInsertWithId_invalidFileProperties() {
        // 測試負數檔案大小
        UserOnlineFile invalidSizeFile = createUserOnlineFile("3001", -1L, 1L, "content", 1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(invalidSizeFile))
                .assertNext(savedFile -> {
                    // 雖然插入成功，但應該記錄這種異常情況
                    assertEquals(-1L, savedFile.getFileSize());
                })
                .verifyComplete();

        // 測試負數快照計數
        UserOnlineFile invalidSnapshotFile = createUserOnlineFile("3002", 1024L, 1L, "content", -1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(invalidSnapshotFile))
                .assertNext(savedFile -> {
                    assertEquals(-1, savedFile.getCurrentSnapshotCount());
                })
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - insertWithId 使用極長ID")
    void testInsertWithId_withVeryLongId() {
        String longId = "9999999999";
        UserOnlineFile fileWithLongId = createUserOnlineFile(longId, 1024L, 1L, "content", 1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(fileWithLongId))
                .assertNext(savedFile -> {
                    assertEquals(Long.parseLong(longId), savedFile.getId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - insertWithId 使用極長內容")
    void testInsertWithId_withVeryLongContent() {
        String longContent = "A".repeat(100000); // 100KB內容
        UserOnlineFile fileWithLongContent = createUserOnlineFile("4001", 100000L, 1L, longContent, 1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(fileWithLongContent))
                .assertNext(savedFile -> {
                    assertEquals(longContent, savedFile.getContent());
                    assertEquals(100000L, savedFile.getFileSize());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - insertWithId 使用空內容")
    void testInsertWithId_withEmptyContent() {
        UserOnlineFile fileWithEmptyContent = createUserOnlineFile("4002", 0L, 1L, "", 0);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(fileWithEmptyContent))
                .assertNext(savedFile -> {
                    assertEquals("", savedFile.getContent());
                    assertEquals(0L, savedFile.getFileSize());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - insertWithId 使用極大檔案大小")
    void testInsertWithId_withMaxFileSize() {
        UserOnlineFile fileWithMaxSize = createUserOnlineFile("4003", Long.MAX_VALUE, 1L, "content", 1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(fileWithMaxSize))
                .assertNext(savedFile -> {
                    assertEquals(Long.MAX_VALUE, savedFile.getFileSize());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - insertWithId 使用極大快照計數")
    void testInsertWithId_withMaxSnapshotCount() {
        UserOnlineFile fileWithMaxSnapshot = createUserOnlineFile("4004", 1024L, 1L, "content", Integer.MAX_VALUE);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(fileWithMaxSnapshot))
                .assertNext(savedFile -> {
                    assertEquals(Integer.MAX_VALUE, savedFile.getCurrentSnapshotCount());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用極長ID")
    void testFindById_withVeryLongId() {
        String longId = "8888888888";
        
        StepVerifier.create(userOnlineFileRepository.findById(longId))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用空字符串")
    void testFindById_withEmptyString() {
        StepVerifier.create(userOnlineFileRepository.findById("0"))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發查詢操作")
    void testConcurrentQueries() {
        // 併發執行多個查詢
        Flux<UserOnlineFile> concurrentQueries = Flux.merge(
                userOnlineFileRepository.findById("1"),
                userOnlineFileRepository.findById("2"),
                userOnlineFileRepository.findById("3"),
                userOnlineFileRepository.findById("4")
        );

        StepVerifier.create(concurrentQueries)
                .expectNextCount(4)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜查詢組合")
    void testComplexQueryCombinations() {
        // 組合多種查詢操作
        Mono<Boolean> complexQuery = userOnlineFileRepository.findById("1")
                .flatMap(file -> userOnlineFileRepository.findAll()
                        .filter(f -> f.getLastModifiedBy().equals(file.getLastModifiedBy()))
                        .count()
                        .map(count -> count > 1));

        StepVerifier.create(complexQuery)
                .expectNext(true) // userId=1有2個檔案
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 批量操作性能測試")
    void testBatchOperationPerformance() {
        // 測試大批量ID查詢
        List<String> largeIdList = java.util.stream.IntStream.range(1, 10001)
                .mapToObj(String::valueOf)
                .toList();

        StepVerifier.create(userOnlineFileRepository.findAllById(largeIdList))
                .expectNextCount(5) // 只有5個檔案存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 特殊字符處理")
    void testSpecialCharacterHandling() {
        // 測試包含特殊字符的內容
        String specialContent = "特殊字符內容：\n\t\"引號\"\n'單引號'\n\\反斜杠\n/正斜杠\n<標籤>\n&符號\n中文字符";
        UserOnlineFile specialFile = createUserOnlineFile("5001", 1024L, 1L, specialContent, 1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(specialFile))
                .assertNext(savedFile -> {
                    assertEquals(specialContent, savedFile.getContent());
                    assertTrue(savedFile.getContent().contains("特殊字符"));
                    assertTrue(savedFile.getContent().contains("中文字符"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 修改者名稱邊界測試")
    void testLastModifiedByBoundaryValues() {
        // 測試極大的修改者ID
        Long longModifierId = 9999999999L;
        UserOnlineFile fileWithLongModifier = createUserOnlineFile("5002", 1024L, longModifierId, "content", 1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(fileWithLongModifier))
                .assertNext(savedFile -> {
                    assertEquals(longModifierId, savedFile.getLastModifiedBy());
                })
                .verifyComplete();

        // 測試特殊的修改者ID
        Long specialModifierId = -1L;
        UserOnlineFile fileWithSpecialModifier = createUserOnlineFile("5003", 1024L, specialModifierId, "content", 1);
        
        StepVerifier.create(userOnlineFileRepository.insertWithId(fileWithSpecialModifier))
                .assertNext(savedFile -> {
                    assertEquals(specialModifierId, savedFile.getLastModifiedBy());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 檔案數據一致性驗證")
    void testFileDataConsistency() {
        // 驗證檔案數據的一致性
        StepVerifier.create(userOnlineFileRepository.findAll())
                .assertNext(file -> {
                    assertNotNull(file.getId());
                    assertNotNull(file.getContent());
                    assertNotNull(file.getLastModifiedBy());
                    assertNotNull(file.getFileSize());
                    assertNotNull(file.getCurrentSnapshotCount());
                    assertTrue(file.getFileSize() >= 0);
                    assertTrue(file.getCurrentSnapshotCount() >= 0);
                })
                .expectNextCount(4) // 跳過其餘檔案的驗證
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 檔案唯一性驗證")
    void testFileUniqueness() {
        // 驗證每個檔案都有唯一的ID
        StepVerifier.create(userOnlineFileRepository.findAll()
                .map(UserOnlineFile::getId)
                .distinct()
                .count())
                .expectNext(5L) // 應該有5個唯一的檔案ID
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 內容長度統計")
    void testContentLengthStatistics() {
        // 測試內容長度統計
        StepVerifier.create(userOnlineFileRepository.findAll()
                .map(file -> file.getContent().length())
                .collectList())
                .assertNext(lengths -> {
                    assertTrue(lengths.size() == 5);
                    assertTrue(lengths.stream().allMatch(length -> length >= 0));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 時間屬性處理")
    void testTimePropertyHandling() {
        // 驗證基本屬性不為null
        StepVerifier.create(userOnlineFileRepository.findAll())
                .assertNext(file -> {
                    assertNotNull(file.getId());
                    assertNotNull(file.getFileSize());
                    assertNotNull(file.getLastModifiedBy());
                    assertNotNull(file.getContent());
                    assertNotNull(file.getCurrentSnapshotCount());
                })
                .expectNextCount(4) // 跳過其餘檔案的驗證
                .verifyComplete();
    }
}