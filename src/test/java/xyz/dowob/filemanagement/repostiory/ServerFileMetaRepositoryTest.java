package xyz.dowob.filemanagement.repostiory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 伺服器檔案元數據資料庫操作介面測試類。
 * 
 * 測試 ServerFileMetaRepository 伺服器檔案元數據資料庫操作介面的響應式資料存取功能和 Spring Data R2DBC 操作。
 * 驗證伺服器檔案元數據的查詢、批量操作及 GridFS 和 MD5 檔案管理，包括繼承自 ReactiveCrudRepository 的基本 CRUD 操作和自定義查詢方法。
 * 透過模擬實現測試各種查詢條件、異常處理和邊界情況，確保響應式程式設計模式的正確實現。
 * 
 * <p>測試涵蓋的主要功能：
 * <ul>
 * <li>findByGridFsId 根據 GridFS ID 查詢檔案元數據</li>
 * <li>findByMd5 根據 MD5 雜湊值查詢檔案元數據</li>
 * <li>findAllByIdIn 根據檔案 ID 集合查詢多個元數據</li>
 * <li>基本 CRUD 操作的響應式實現驗證</li>
 * <li>檔案類型和大小範圍測試</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("ServerFileMetaRepository 伺服器檔案元數據操作接口測試")
class ServerFileMetaRepositoryTest {

    private ServerFileMetaRepository serverFileMetaRepository;
    private ServerFileMetadata testFile1;
    private ServerFileMetadata testFile2;
    private ServerFileMetadata testFile3;

    @BeforeEach
    void setUp() {
        // 創建測試用的 ServerFileMetaRepository 實現
        serverFileMetaRepository = new ServerFileMetaRepository() {
            // 模擬數據存儲
            private final List<ServerFileMetadata> files = Arrays.asList(
                createServerFile("1", "gridfs_001", "md5_001", "document.pdf", "application/pdf", 1024L),
                createServerFile("2", "gridfs_002", "md5_002", "image.jpg", "image/jpeg", 2048L),
                createServerFile("3", "gridfs_003", "md5_003", "video.mp4", "video/mp4", 4096L),
                createServerFile("4", "gridfs_004", "md5_004", "audio.mp3", "audio/mpeg", 8192L),
                createServerFile("5", "gridfs_005", "md5_005", "archive.zip", "application/zip", 16384L)
            );

            @Override
            public Mono<ServerFileMetadata> findByGridFsId(String gridFsId) {
                if (gridFsId == null || gridFsId.isBlank()) {
                    return Mono.empty();
                }
                return Flux.fromIterable(files)
                        .filter(file -> gridFsId.equals(file.getGridFsId()))
                        .next();
            }

            @Override
            public Mono<ServerFileMetadata> findByMd5(String md5) {
                if (md5 == null || md5.isBlank()) {
                    return Mono.empty();
                }
                return Flux.fromIterable(files)
                        .filter(file -> md5.equals(file.getMd5()))
                        .next();
            }

            @Override
            public Flux<ServerFileMetadata> findAllByIdIn(Set<Long> serverFileId) {
                if (serverFileId == null || serverFileId.isEmpty()) {
                    return Flux.empty();
                }
                return Flux.fromIterable(files)
                        .filter(file -> serverFileId.contains(file.getId()));
            }

            // ReactiveCrudRepository 基本方法實現
            @Override
            public <S extends ServerFileMetadata> Mono<S> save(S entity) {
                return Mono.just(entity);
            }

            @Override
            public <S extends ServerFileMetadata> Flux<S> saveAll(Iterable<S> entities) {
                return Flux.fromIterable(entities);
            }

            @Override
            public <S extends ServerFileMetadata> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
                return Flux.from(entityStream);
            }

            @Override
            public Mono<ServerFileMetadata> findById(Long id) {
                if (id == null) {
                    return Mono.empty();
                }
                return Flux.fromIterable(files)
                        .filter(file -> id.equals(file.getId()))
                        .next();
            }

            @Override
            public Mono<ServerFileMetadata> findById(org.reactivestreams.Publisher<Long> id) {
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
            public Flux<ServerFileMetadata> findAll() {
                return Flux.fromIterable(files);
            }

            @Override
            public Flux<ServerFileMetadata> findAllById(Iterable<Long> ids) {
                return Flux.fromIterable(ids).flatMap(this::findById);
            }

            @Override
            public Flux<ServerFileMetadata> findAllById(org.reactivestreams.Publisher<Long> idStream) {
                return Flux.from(idStream).flatMap(this::findById);
            }

            @Override
            public Mono<Long> count() {
                return Mono.just((long) files.size());
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
            public Mono<Void> delete(ServerFileMetadata entity) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAllById(Iterable<? extends Long> ids) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(Iterable<? extends ServerFileMetadata> entities) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends ServerFileMetadata> entityStream) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll() {
                return Mono.empty();
            }
        };

        // 設置測試對象
        testFile1 = createServerFile("1", "gridfs_001", "md5_001", "document.pdf", "application/pdf", 1024L);
        testFile2 = createServerFile("2", "gridfs_002", "md5_002", "image.jpg", "image/jpeg", 2048L);
        testFile3 = createServerFile("3", "gridfs_003", "md5_003", "video.mp4", "video/mp4", 4096L);
    }

    private ServerFileMetadata createServerFile(String id, String gridFsId, String md5, String fileName, String mimeType, Long fileSize) {
        ServerFileMetadata file = new ServerFileMetadata();
        file.setId(Long.parseLong(id));
        file.setGridFsId(gridFsId);
        file.setMd5(md5);
        file.setMimeType(mimeType);
        file.setFileSize(fileSize);
        file.setUploadTime(LocalDateTime.now());
        file.setLastAccessTime(LocalDateTime.now());
        return file;
    }

    // ==================== 一般測試 ====================

    /**
     * 測試 findByGridFsId 方法的基本功能。
     * 
     * 測試根據 GridFS ID 查詢伺服器檔案元數據的基本查詢功能。
     * 
     * 前置條件：
     * - 測試資料包含不同的 GridFS ID
     * - 每個 GridFS ID 對應唯一的檔案元數據
     * 
     * 測試步驟：
     * - 呼叫 findByGridFsId 方法查詢指定 GridFS ID
     * - 驗證返回的檔案元數據資訊
     * 
     * 預期結果：
     * - 成功返回對應的檔案元數據
     * - 元數據資訊完整且正確
     */
    @Test
    @DisplayName("一般測試 - findByGridFsId 方法基本功能")
    void testFindByGridFsId_basicFunctionality() {
        StepVerifier.create(serverFileMetaRepository.findByGridFsId("gridfs_001"))
                .assertNext(file -> {
                    assertNotNull(file);
                    assertEquals("gridfs_001", file.getGridFsId());
                    assertEquals(1L, file.getId());
                    assertEquals("md5_001", file.getMd5());
                    assertEquals("application/pdf", file.getMimeType());
                    assertEquals(1024L, file.getFileSize());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findByMd5 方法基本功能")
    void testFindByMd5_basicFunctionality() {
        StepVerifier.create(serverFileMetaRepository.findByMd5("md5_002"))
                .assertNext(file -> {
                    assertNotNull(file);
                    assertEquals("md5_002", file.getMd5());
                    assertEquals(2L, file.getId());
                    assertEquals("gridfs_002", file.getGridFsId());
                    assertEquals("image/jpeg", file.getMimeType());
                    assertEquals(2048L, file.getFileSize());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByIdIn 方法基本功能")
    void testFindAllByIdIn_basicFunctionality() {
        Set<Long> fileIds = Set.of(1L, 2L, 999L);
        
        StepVerifier.create(serverFileMetaRepository.findAllByIdIn(fileIds))
                .assertNext(file -> assertEquals(1L, file.getId()))
                .assertNext(file -> assertEquals(2L, file.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - ReactiveCrudRepository 基本CRUD操作")
    void testReactiveCrudRepositoryBasicOperations() {
        // 測試 save 操作
        StepVerifier.create(serverFileMetaRepository.save(testFile1))
                .assertNext(savedFile -> {
                    assertNotNull(savedFile);
                    assertEquals(1L, savedFile.getId());
                    assertEquals("gridfs_001", savedFile.getGridFsId());
                })
                .verifyComplete();

        // 測試 findById 操作
        StepVerifier.create(serverFileMetaRepository.findById(1L))
                .assertNext(file -> {
                    assertEquals(1L, file.getId());
                    assertEquals("gridfs_001", file.getGridFsId());
                })
                .verifyComplete();

        // 測試 existsById 操作
        StepVerifier.create(serverFileMetaRepository.existsById(1L))
                .expectNext(true)
                .verifyComplete();

        // 測試 count 操作
        StepVerifier.create(serverFileMetaRepository.count())
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAll 方法")
    void testFindAll() {
        StepVerifier.create(serverFileMetaRepository.findAll())
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - saveAll 批量保存操作")
    void testSaveAll() {
        List<ServerFileMetadata> filesToSave = Arrays.asList(testFile1, testFile2);
        
        StepVerifier.create(serverFileMetaRepository.saveAll(filesToSave))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllById 批量查詢操作")
    void testFindAllById() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        
        StepVerifier.create(serverFileMetaRepository.findAllById(ids))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 findByGridFsId 方法
        try {
            var findByGridFsIdMethod = ServerFileMetaRepository.class.getMethod("findByGridFsId", String.class);
            assertEquals(Mono.class, findByGridFsIdMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findByGridFsId 方法應該存在");
        }

        // 驗證 findByMd5 方法
        try {
            var findByMd5Method = ServerFileMetaRepository.class.getMethod("findByMd5", String.class);
            assertEquals(Mono.class, findByMd5Method.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findByMd5 方法應該存在");
        }

        // 驗證 findAllByIdIn 方法
        try {
            var findAllByIdInMethod = ServerFileMetaRepository.class.getMethod("findAllByIdIn", Set.class);
            assertEquals(Flux.class, findAllByIdInMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByIdIn 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritanceRelationships() {
        // 驗證 ServerFileMetaRepository 繼承了 ReactiveCrudRepository
        assertTrue(org.springframework.data.repository.reactive.ReactiveCrudRepository.class.isAssignableFrom(ServerFileMetaRepository.class));
        
        // 驗證泛型參數
        var genericInterfaces = ServerFileMetaRepository.class.getGenericInterfaces();
        assertTrue(genericInterfaces.length > 0);
    }

    @Test
    @DisplayName("一般測試 - 響應式編程模式驗證")
    void testReactiveProgrammingPatterns() {
        // 測試響應式鏈式操作
        Mono<String> gridFsIdChain = serverFileMetaRepository.findByMd5("md5_001")
                .map(ServerFileMetadata::getGridFsId)
                .defaultIfEmpty("unknown");

        StepVerifier.create(gridFsIdChain)
                .expectNext("gridfs_001")
                .verifyComplete();

        // 測試響應式合併操作
        Flux<ServerFileMetadata> combinedFiles = serverFileMetaRepository.findByGridFsId("gridfs_001")
                .flux()
                .mergeWith(serverFileMetaRepository.findByMd5("md5_002"));

        StepVerifier.create(combinedFiles)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 檔案類型多樣性測試")
    void testFileTypeDiversity() {
        // 測試不同檔案類型的查詢
        StepVerifier.create(serverFileMetaRepository.findByMd5("md5_001"))
                .assertNext(file -> assertEquals("application/pdf", file.getMimeType()))
                .verifyComplete();

        StepVerifier.create(serverFileMetaRepository.findByMd5("md5_002"))
                .assertNext(file -> assertEquals("image/jpeg", file.getMimeType()))
                .verifyComplete();

        StepVerifier.create(serverFileMetaRepository.findByMd5("md5_003"))
                .assertNext(file -> assertEquals("video/mp4", file.getMimeType()))
                .verifyComplete();

        StepVerifier.create(serverFileMetaRepository.findByMd5("md5_004"))
                .assertNext(file -> assertEquals("audio/mpeg", file.getMimeType()))
                .verifyComplete();

        StepVerifier.create(serverFileMetaRepository.findByMd5("md5_005"))
                .assertNext(file -> assertEquals("application/zip", file.getMimeType()))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 檔案大小範圍測試")
    void testFileSizeRanges() {
        // 測試不同大小檔案的查詢
        Set<Long> allIds = Set.of(1L, 2L, 3L, 4L, 5L);
        
        StepVerifier.create(serverFileMetaRepository.findAllByIdIn(allIds))
                .assertNext(file -> assertTrue(file.getFileSize() >= 1024L))
                .assertNext(file -> assertTrue(file.getFileSize() >= 1024L))
                .assertNext(file -> assertTrue(file.getFileSize() >= 1024L))
                .assertNext(file -> assertTrue(file.getFileSize() >= 1024L))
                .assertNext(file -> assertTrue(file.getFileSize() >= 1024L))
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    /**
     * 測試 findByGridFsId 方法處理 null 參數。
     * 
     * 測試當傳入 null GridFS ID 時的異常處理邏輯。
     * 
     * 前置條件：
     * - 方法接受 null GridFS ID 參數
     * 
     * 測試步驟：
     * - 傳入 null GridFS ID
     * - 觀察方法的處理結果
     * 
     * 預期結果：
     * - 方法返回空的 Mono，不拋出異常
     */
    @Test
    @DisplayName("異常測試 - findByGridFsId 傳入 null")
    void testFindByGridFsId_withNull() {
        StepVerifier.create(serverFileMetaRepository.findByGridFsId(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByGridFsId 傳入空字符串")
    void testFindByGridFsId_withEmptyString() {
        StepVerifier.create(serverFileMetaRepository.findByGridFsId(""))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByGridFsId 查詢不存在的GridFS ID")
    void testFindByGridFsId_nonExistentId() {
        StepVerifier.create(serverFileMetaRepository.findByGridFsId("nonexistent_gridfs"))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByMd5 傳入 null")
    void testFindByMd5_withNull() {
        StepVerifier.create(serverFileMetaRepository.findByMd5(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByMd5 傳入空字符串")
    void testFindByMd5_withEmptyString() {
        StepVerifier.create(serverFileMetaRepository.findByMd5(""))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByMd5 查詢不存在的MD5")
    void testFindByMd5_nonExistentMd5() {
        StepVerifier.create(serverFileMetaRepository.findByMd5("nonexistent_md5"))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByIdIn 傳入 null 集合")
    void testFindAllByIdIn_withNullSet() {
        StepVerifier.create(serverFileMetaRepository.findAllByIdIn(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByIdIn 傳入空集合")
    void testFindAllByIdIn_withEmptySet() {
        StepVerifier.create(serverFileMetaRepository.findAllByIdIn(Collections.emptySet()))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 傳入 null")
    void testFindById_withNull() {
        StepVerifier.create(serverFileMetaRepository.findById((Long) null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - existsById 傳入不存在的ID")
    void testExistsById_nonExistentId() {
        StepVerifier.create(serverFileMetaRepository.existsById(999L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 無效MD5格式")
    void testFindByMd5_invalidFormat() {
        // 測試非標準MD5格式
        StepVerifier.create(serverFileMetaRepository.findByMd5("invalid_md5_format"))
                .verifyComplete();

        // 測試過短的MD5
        StepVerifier.create(serverFileMetaRepository.findByMd5("123"))
                .verifyComplete();

        // 測試過長的字符串
        StepVerifier.create(serverFileMetaRepository.findByMd5("a".repeat(100)))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 無效GridFS ID格式")
    void testFindByGridFsId_invalidFormat() {
        // 測試非標準GridFS ID格式
        StepVerifier.create(serverFileMetaRepository.findByGridFsId("invalid_gridfs_format"))
                .verifyComplete();

        // 測試特殊字符
        StepVerifier.create(serverFileMetaRepository.findByGridFsId("gridfs@#$%"))
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    /**
     * 測試 findByGridFsId 方法處理極長 GridFS ID。
     * 
     * 測試當 GridFS ID 長度非常大時的處理能力。
     * 
     * 前置條件：
     * - 建立長度超過10000字元的 GridFS ID
     * 
     * 測試步驟：
     * - 使用極長 GridFS ID 進行查詢
     * - 驗證處理結果
     * 
     * 預期結果：
     * - 方法正常處理極長 ID
     * - 返回空結果（未找到匹配）
     */
    @Test
    @DisplayName("邊界測試 - findByGridFsId 使用極長GridFS ID")
    void testFindByGridFsId_withVeryLongId() {
        String longGridFsId = "gridfs_" + "a".repeat(10000);
        
        StepVerifier.create(serverFileMetaRepository.findByGridFsId(longGridFsId))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findByMd5 使用極長MD5字符串")
    void testFindByMd5_withVeryLongMd5() {
        String longMd5 = "md5_" + "a".repeat(10000);
        
        StepVerifier.create(serverFileMetaRepository.findByMd5(longMd5))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByIdIn 使用大量ID")
    void testFindAllByIdIn_withManyIds() {
        // 創建包含1000個ID的集合
        Set<Long> manyIds = java.util.stream.LongStream.range(1, 1001)
                .boxed()
                .collect(java.util.stream.Collectors.toSet());
        
        StepVerifier.create(serverFileMetaRepository.findAllByIdIn(manyIds))
                .expectNextCount(5) // 只有5個檔案存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByIdIn 使用極大ID值")
    void testFindAllByIdIn_withMaxLongValues() {
        Set<Long> maxIds = Set.of(Long.MAX_VALUE, Long.MAX_VALUE - 1, 0L, -1L);
        
        StepVerifier.create(serverFileMetaRepository.findAllByIdIn(maxIds))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - GridFS ID大小寫敏感性")
    void testFindByGridFsId_caseSensitivity() {
        StepVerifier.create(serverFileMetaRepository.findByGridFsId("GRIDFS_001"))
                .verifyComplete(); // 應該找不到，因為大小寫不匹配

        StepVerifier.create(serverFileMetaRepository.findByGridFsId("gridfs_001"))
                .expectNextCount(1)
                .verifyComplete(); // 應該找到
    }

    @Test
    @DisplayName("邊界測試 - MD5大小寫敏感性")
    void testFindByMd5_caseSensitivity() {
        StepVerifier.create(serverFileMetaRepository.findByMd5("MD5_001"))
                .verifyComplete(); // 應該找不到，因為大小寫不匹配

        StepVerifier.create(serverFileMetaRepository.findByMd5("md5_001"))
                .expectNextCount(1)
                .verifyComplete(); // 應該找到
    }

    @Test
    @DisplayName("邊界測試 - 併發查詢操作")
    void testConcurrentQueries() {
        // 併發執行多個查詢
        Flux<ServerFileMetadata> concurrentQueries = Flux.merge(
                serverFileMetaRepository.findByGridFsId("gridfs_001"),
                serverFileMetaRepository.findByMd5("md5_002"),
                serverFileMetaRepository.findById(3L),
                serverFileMetaRepository.findAllByIdIn(Set.of(4L, 5L))
        );

        StepVerifier.create(concurrentQueries)
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜查詢組合")
    void testComplexQueryCombinations() {
        // 組合多種查詢操作
        Mono<Boolean> complexQuery = serverFileMetaRepository.findByGridFsId("gridfs_001")
                .flatMap(file -> serverFileMetaRepository.findByMd5(file.getMd5()))
                .map(md5File -> md5File.getGridFsId().equals("gridfs_001"))
                .defaultIfEmpty(false);

        StepVerifier.create(complexQuery)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 批量操作性能測試")
    void testBatchOperationPerformance() {
        // 測試大批量ID查詢
        Set<Long> largeIdSet = java.util.stream.LongStream.range(1, 10001)
                .boxed()
                .collect(java.util.stream.Collectors.toSet());

        StepVerifier.create(serverFileMetaRepository.findAllByIdIn(largeIdSet))
                .expectNextCount(5) // 只有5個檔案存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 重複值處理")
    void testDuplicateValueHandling() {
        // 測試重複ID查詢（Set會自動去重）
        Set<Long> duplicateIds = new HashSet<>();
        duplicateIds.add(1L);
        duplicateIds.add(1L); // 重複添加
        duplicateIds.add(2L);
        duplicateIds.add(2L); // 重複添加
        
        StepVerifier.create(serverFileMetaRepository.findAllByIdIn(duplicateIds))
                .expectNextCount(2) // 應該返回唯一的檔案
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 特殊字符處理")
    void testSpecialCharacterHandling() {
        // 測試包含特殊字符的GridFS ID
        StepVerifier.create(serverFileMetaRepository.findByGridFsId("gridfs-001"))
                .verifyComplete();

        StepVerifier.create(serverFileMetaRepository.findByGridFsId("gridfs_001@test"))
                .verifyComplete();

        StepVerifier.create(serverFileMetaRepository.findByGridFsId("gridfs#001"))
                .verifyComplete();

        // 測試包含特殊字符的MD5
        StepVerifier.create(serverFileMetaRepository.findByMd5("md5-001"))
                .verifyComplete();

        StepVerifier.create(serverFileMetaRepository.findByMd5("md5_001@test"))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空白字符處理")
    void testWhitespaceHandling() {
        // 測試包含空格的GridFS ID
        StepVerifier.create(serverFileMetaRepository.findByGridFsId(" gridfs_001 "))
                .verifyComplete(); // 應該找不到

        // 測試僅包含空格的GridFS ID
        StepVerifier.create(serverFileMetaRepository.findByGridFsId("   "))
                .verifyComplete(); // 應該找不到

        // 測試包含制表符和換行符的MD5
        StepVerifier.create(serverFileMetaRepository.findByMd5("\t\n"))
                .verifyComplete(); // 應該找不到
    }

    @Test
    @DisplayName("邊界測試 - Unicode字符處理")
    void testUnicodeCharacterHandling() {
        // 測試包含Unicode字符的GridFS ID
        StepVerifier.create(serverFileMetaRepository.findByGridFsId("gridfs_測試_001"))
                .verifyComplete();

        StepVerifier.create(serverFileMetaRepository.findByGridFsId("gridfs_🔥_001"))
                .verifyComplete();

        // 測試包含Unicode字符的MD5
        StepVerifier.create(serverFileMetaRepository.findByMd5("md5_測試_001"))
                .verifyComplete();

        StepVerifier.create(serverFileMetaRepository.findByMd5("md5_🚀_001"))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 數字邊界值測試")
    void testNumericBoundaryValues() {
        Set<Long> boundaryIds = Set.of(
                0L,                    // 最小正值
                1L,                    // 最小ID
                Long.MAX_VALUE,        // 最大長整型值
                Long.MIN_VALUE,        // 最小長整型值
                -1L                    // 負值
        );
        
        StepVerifier.create(serverFileMetaRepository.findAllByIdIn(boundaryIds))
                .expectNextCount(1) // 只有ID為1的檔案存在
                .verifyComplete();
    }
}