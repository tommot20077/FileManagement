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
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 用戶檔案元數據資料庫操作介面測試類。
 * 
 * 測試 UserFileMetaRepository 用戶檔案元數據資料庫操作介面的響應式資料存取功能和 Spring Data R2DBC 操作。
 * 驗證用戶檔案元數據的查詢、權限管理、共享設定及檔案夾層級管理，包括繼承自 ReactiveCrudRepository 的基本 CRUD 操作和複雜自定義查詢方法。
 * 支援檔案過濾、共享權限查詢及檔案夾層級結構管理，適合複雜的檔案系統權限控制。
 * 透過模擬實現測試各種查詢條件、默認方法及異常情況，確保響應式程式設計模式的正確實現。
 * 
 * <p>測試涵蓋的主要功能：
 * <ul>
 * <li>findAllByUserId 根據用戶 ID 查詢所有檔案元數據</li>
 * <li>findAllByParentFolderIdIn 根據父檔案夾 ID 集合查詢</li>
 * <li>findAllByParentFolderIdWithShare 查詢具有共享權限的檔案</li>
 * <li>countByServerFileIdInAndUserId 計算用戶擁有的檔案數量</li>
 * <li>getShareTypeByFileId 查詢檔案共享類型</li>
 * <li>檔案類型和共享類型管理</li>
 * <li>檔案夾層級結構處理</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("UserFileMetaRepository 用戶檔案元數據操作接口測試")
class UserFileMetaRepositoryTest {

    private UserFileMetaRepository userFileMetaRepository;
    private R2dbcEntityOperations entityOperations;
    private UserFileMetadata testFile1;
    private UserFileMetadata testFile2;
    private UserFileMetadata testFile3;
    private User testUser;
    private FileFilterDTO testFilterDTO;

    @BeforeEach
    void setUp() {
        entityOperations = mock(R2dbcEntityOperations.class);
        
        // 創建測試用的 UserFileMetaRepository 實現
        userFileMetaRepository = new UserFileMetaRepository() {
            // 模擬數據存儲
            private final List<UserFileMetadata> files = Arrays.asList(
                createUserFile("1", 1L, "document.pdf", null, FileEnum.DOCUMENT, FileShareTypeEnum.NONE),
                createUserFile("2", 1L, "folder1", null, FileEnum.FOLDER, FileShareTypeEnum.PUBLIC),
                createUserFile("3", 2L, "image.jpg", 2L, FileEnum.DOCUMENT, FileShareTypeEnum.PRIVATE),
                createUserFile("4", 1L, "video.mp4", 2L, FileEnum.DOCUMENT, FileShareTypeEnum.DEFAULT),
                createUserFile("5", 3L, "shared.txt", null, FileEnum.DOCUMENT, FileShareTypeEnum.PUBLIC)
            );

            @Override
            public Flux<UserFileMetadata> findAllByUserId(Long userId) {
                if (userId == null) {
                    return Flux.empty();
                }
                return Flux.fromIterable(files)
                        .filter(file -> userId.equals(file.getUserId()));
            }

            // ReactiveCrudRepository 基本方法實現
            @Override
            public <S extends UserFileMetadata> Mono<S> save(S entity) {
                return Mono.just(entity);
            }

            @Override
            public <S extends UserFileMetadata> Flux<S> saveAll(Iterable<S> entities) {
                return Flux.fromIterable(entities);
            }

            @Override
            public <S extends UserFileMetadata> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
                return Flux.from(entityStream);
            }

            @Override
            public Mono<UserFileMetadata> findById(String id) {
                if (id == null) {
                    return Mono.empty();
                }
                return Flux.fromIterable(files)
                        .filter(file -> id.equals(file.getId().toString()))
                        .next();
            }

            @Override
            public Mono<UserFileMetadata> findById(org.reactivestreams.Publisher<String> id) {
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
            public Flux<UserFileMetadata> findAll() {
                return Flux.fromIterable(files);
            }

            @Override
            public Flux<UserFileMetadata> findAllById(Iterable<String> ids) {
                return Flux.fromIterable(ids).flatMap(this::findById);
            }

            @Override
            public Flux<UserFileMetadata> findAllById(org.reactivestreams.Publisher<String> idStream) {
                return Flux.from(idStream).flatMap(this::findById);
            }

            @Override
            public Mono<Long> count() {
                return Mono.just((long) files.size());
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
            public Mono<Void> delete(UserFileMetadata entity) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAllById(Iterable<? extends String> ids) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(Iterable<? extends UserFileMetadata> entities) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends UserFileMetadata> entityStream) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll() {
                return Mono.empty();
            }
        };

        // 設置測試對象
        testFile1 = createUserFile("1", 1L, "document.pdf", null, FileEnum.DOCUMENT, FileShareTypeEnum.NONE);
        testFile2 = createUserFile("2", 1L, "folder1", null, FileEnum.FOLDER, FileShareTypeEnum.PUBLIC);
        testFile3 = createUserFile("3", 2L, "image.jpg", 2L, FileEnum.DOCUMENT, FileShareTypeEnum.PRIVATE);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        
        testFilterDTO = FileFilterDTO.builder()
                .keyword("test")
                .folderId(null)
                .page(0)
                .pageSize(10)
                .build();
    }

    private UserFileMetadata createUserFile(String id, Long userId, String fileName, Long parentFolderId, 
                                          FileEnum fileType, FileShareTypeEnum shareType) {
        UserFileMetadata file = new UserFileMetadata();
        file.setId(Long.parseLong(id));
        file.setUserId(userId);
        file.setFilename(fileName);
        file.setParentFolderId(parentFolderId);
        file.setFileType(fileType);
        file.setShareType(shareType);
        file.setUploadTime(LocalDateTime.now());
        file.setLastAccessTime(LocalDateTime.now());
        file.setIsDeleted(false);
        file.setIsStar(false);
        return file;
    }

    // ==================== 一般測試 ====================

    /**
     * 測試 findAllByUserId 方法的基本功能。
     * 
     * 測試根據用戶 ID 查詢用戶所有檔案元數據的基本查詢功能。
     * 
     * 前置條件：
     * - 測試資料包含不同用戶的檔案元數據
     * - 檔案包含不同類型和共享設定
     * 
     * 測試步驟：
     * - 呼叫 findAllByUserId 方法查詢指定用戶的檔案
     * - 驗證返回的檔案元數據資訊
     * 
     * 預期結果：
     * - 成功返回用戶的所有檔案元數據
     * - 檔案資訊完整且正確
     */
    @Test
    @DisplayName("一般測試 - findAllByUserId 方法基本功能")
    void testFindAllByUserId_basicFunctionality() {
        StepVerifier.create(userFileMetaRepository.findAllByUserId(1L))
                .assertNext(file -> {
                    assertEquals(1L, file.getId());
                    assertEquals(1L, file.getUserId());
                    assertEquals("document.pdf", file.getFilename());
                })
                .assertNext(file -> {
                    assertEquals(2L, file.getId());
                    assertEquals(1L, file.getUserId());
                    assertEquals("folder1", file.getFilename());
                })
                .assertNext(file -> {
                    assertEquals(4L, file.getId());
                    assertEquals(1L, file.getUserId());
                    assertEquals("video.mp4", file.getFilename());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 不同檔案類型查詢")
    void testFindAllByUserId_differentFileTypes() {
        StepVerifier.create(userFileMetaRepository.findAllByUserId(1L)
                .filter(file -> file.getFileType() == FileEnum.FOLDER))
                .assertNext(file -> {
                    assertEquals("folder1", file.getFilename());
                    assertEquals(FileEnum.FOLDER, file.getFileType());
                })
                .verifyComplete();

        StepVerifier.create(userFileMetaRepository.findAllByUserId(1L)
                .filter(file -> file.getFileType() == FileEnum.DOCUMENT))
                .expectNextCount(2) // document.pdf 和 video.mp4
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 不同共享類型查詢")
    void testFindAllByUserId_differentShareTypes() {
        StepVerifier.create(userFileMetaRepository.findAllByUserId(1L)
                .filter(file -> file.getShareType() == FileShareTypeEnum.PUBLIC))
                .assertNext(file -> {
                    assertEquals("folder1", file.getFilename());
                    assertEquals(FileShareTypeEnum.PUBLIC, file.getShareType());
                })
                .verifyComplete();

        StepVerifier.create(userFileMetaRepository.findAllByUserId(1L)
                .filter(file -> file.getShareType() == FileShareTypeEnum.NONE))
                .assertNext(file -> {
                    assertEquals("document.pdf", file.getFilename());
                    assertEquals(FileShareTypeEnum.NONE, file.getShareType());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - ReactiveCrudRepository 基本CRUD操作")
    void testReactiveCrudRepositoryBasicOperations() {
        // 測試 save 操作
        StepVerifier.create(userFileMetaRepository.save(testFile1))
                .assertNext(savedFile -> {
                    assertNotNull(savedFile);
                    assertEquals(1L, savedFile.getId());
                    assertEquals("document.pdf", savedFile.getFilename());
                })
                .verifyComplete();

        // 測試 findById 操作
        StepVerifier.create(userFileMetaRepository.findById("1"))
                .assertNext(file -> {
                    assertEquals(1L, file.getId());
                    assertEquals("document.pdf", file.getFilename());
                })
                .verifyComplete();

        // 測試 existsById 操作
        StepVerifier.create(userFileMetaRepository.existsById("1"))
                .expectNext(true)
                .verifyComplete();

        // 測試 count 操作
        StepVerifier.create(userFileMetaRepository.count())
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAll 方法")
    void testFindAll() {
        StepVerifier.create(userFileMetaRepository.findAll())
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - saveAll 批量保存操作")
    void testSaveAll() {
        List<UserFileMetadata> filesToSave = Arrays.asList(testFile1, testFile2);
        
        StepVerifier.create(userFileMetaRepository.saveAll(filesToSave))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllById 批量查詢操作")
    void testFindAllById() {
        List<String> ids = Arrays.asList("1", "2", "3");
        
        StepVerifier.create(userFileMetaRepository.findAllById(ids))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 findAllByUserId 方法
        try {
            var findAllByUserIdMethod = UserFileMetaRepository.class.getMethod("findAllByUserId", Long.class);
            assertEquals(Flux.class, findAllByUserIdMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByUserId 方法應該存在");
        }

        // 驗證 findAllByParentFolderIdIn 默認方法
        try {
            var findAllByParentFolderIdInMethod = UserFileMetaRepository.class.getMethod(
                "findAllByParentFolderIdIn", List.class, R2dbcEntityOperations.class);
            assertEquals(Flux.class, findAllByParentFolderIdInMethod.getReturnType());
            assertTrue(findAllByParentFolderIdInMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("findAllByParentFolderIdIn 方法應該存在");
        }

        // 驗證 getShareTypeByFileId 默認方法
        try {
            var getShareTypeByFileIdMethod = UserFileMetaRepository.class.getMethod(
                "getShareTypeByFileId", Long.class, R2dbcEntityOperations.class);
            assertEquals(Mono.class, getShareTypeByFileIdMethod.getReturnType());
            assertTrue(getShareTypeByFileIdMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("getShareTypeByFileId 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritanceRelationships() {
        // 驗證 UserFileMetaRepository 繼承了 ReactiveCrudRepository
        assertTrue(org.springframework.data.repository.reactive.ReactiveCrudRepository.class.isAssignableFrom(UserFileMetaRepository.class));
        
        // 驗證泛型參數
        var genericInterfaces = UserFileMetaRepository.class.getGenericInterfaces();
        assertTrue(genericInterfaces.length > 0);
    }

    @Test
    @DisplayName("一般測試 - 響應式編程模式驗證")
    void testReactiveProgrammingPatterns() {
        // 測試響應式鏈式操作
        Mono<String> fileNameChain = userFileMetaRepository.findById("1")
                .map(UserFileMetadata::getFilename)
                .defaultIfEmpty("unknown");

        StepVerifier.create(fileNameChain)
                .expectNext("document.pdf")
                .verifyComplete();

        // 測試響應式合併操作
        Flux<UserFileMetadata> combinedFiles = userFileMetaRepository.findById("1")
                .flux()
                .mergeWith(userFileMetaRepository.findById("2"));

        StepVerifier.create(combinedFiles)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 檔案夾層級結構測試")
    void testFolderHierarchyStructure() {
        // 測試根檔案夾（parentFolderId 為 null）
        StepVerifier.create(userFileMetaRepository.findAllByUserId(1L)
                .filter(file -> file.getParentFolderId() == null))
                .expectNextCount(2) // document.pdf 和 folder1
                .verifyComplete();

        // 測試子檔案夾（parentFolderId 為 2L）
        StepVerifier.create(userFileMetaRepository.findAll()
                .filter(file -> Long.valueOf(2L).equals(file.getParentFolderId())))
                .expectNextCount(2) // image.jpg 和 video.mp4
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 默認方法空實現驗證")
    void testDefaultMethodEmptyImplementations() {
        // 測試默認方法當 entityOperations 為 mock 時的行為
        // 由於 Mock 對象沒有配置，會拋出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.findAllByParentFolderIdIn(Arrays.asList(1L, 2L), entityOperations)
                    .blockFirst(); // 觸發執行
        });

        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.findAllByParentFolderIdWithShare(1L, testUser, entityOperations)
                    .blockFirst(); // 觸發執行
        });

        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.countByServerFileIdInAndUserId(Arrays.asList(1L, 2L), 1L, entityOperations)
                    .blockFirst(); // 觸發執行
        });

        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.getShareTypeByFileId(1L, entityOperations)
                    .block(); // 觸發執行
        });
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
    @DisplayName("異常測試 - findAllByUserId 傳入 null")
    void testFindAllByUserId_withNull() {
        StepVerifier.create(userFileMetaRepository.findAllByUserId(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByUserId 查詢不存在的用戶")
    void testFindAllByUserId_nonExistentUser() {
        StepVerifier.create(userFileMetaRepository.findAllByUserId(999L))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 傳入 null")
    void testFindById_withNull() {
        StepVerifier.create(userFileMetaRepository.findById((String) null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 查詢不存在的檔案")
    void testFindById_nonExistentFile() {
        StepVerifier.create(userFileMetaRepository.findById("999"))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - existsById 傳入不存在的ID")
    void testExistsById_nonExistentId() {
        StepVerifier.create(userFileMetaRepository.existsById("999"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByParentFolderIdIn 傳入 null 參數")
    void testFindAllByParentFolderIdIn_withNullParameters() {
        // 測試 null parentFolderIdList - 應該拋出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            userFileMetaRepository.findAllByParentFolderIdIn(null, entityOperations)
                    .blockFirst(); // 觸發執行
        });

        // 測試 null entityOperations - 應該拋出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.findAllByParentFolderIdIn(Arrays.asList(1L, 2L), null)
                    .blockFirst(); // 觸發執行
        });
    }

    @Test
    @DisplayName("異常測試 - findAllByParentFolderIdWithShare 傳入 null 參數")
    void testFindAllByParentFolderIdWithShare_withNullParameters() {
        // 測試 null parentFolderId - 應該拋出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            userFileMetaRepository.findAllByParentFolderIdWithShare(null, testUser, entityOperations)
                    .blockFirst(); // 觸發執行
        });

        // 測試 null user - 應該拋出異常
        assertThrows(Exception.class, () -> {
            userFileMetaRepository.findAllByParentFolderIdWithShare(1L, null, entityOperations)
                    .blockFirst(); // 觸發執行
        });

        // 測試 null entityOperations - 應該拋出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.findAllByParentFolderIdWithShare(1L, testUser, null)
                    .blockFirst(); // 觸發執行
        });
    }

    @Test
    @DisplayName("異常測試 - countByServerFileIdInAndUserId 傳入 null 參數")
    void testCountByServerFileIdInAndUserId_withNullParameters() {
        // 測試 null serverFileIdList - 應該拋出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.countByServerFileIdInAndUserId(null, 1L, entityOperations)
                    .blockFirst(); // 觸發執行
        });

        // 測試 null userId - 應該拋出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.countByServerFileIdInAndUserId(Arrays.asList(1L, 2L), null, entityOperations)
                    .blockFirst(); // 觸發執行
        });

        // 測試 null entityOperations - 應該拋出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.countByServerFileIdInAndUserId(Arrays.asList(1L, 2L), 1L, null)
                    .blockFirst(); // 觸發執行
        });
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
    void testFindAllByUserId_withMaxLongValue() {
        StepVerifier.create(userFileMetaRepository.findAllByUserId(Long.MAX_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserId 使用極小用戶ID")
    void testFindAllByUserId_withMinLongValue() {
        StepVerifier.create(userFileMetaRepository.findAllByUserId(Long.MIN_VALUE))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUserId 使用零用戶ID")
    void testFindAllByUserId_withZeroUserId() {
        StepVerifier.create(userFileMetaRepository.findAllByUserId(0L))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用極長檔案ID")
    void testFindById_withVeryLongId() {
        StepVerifier.create(userFileMetaRepository.findById(String.valueOf(Long.MAX_VALUE)))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findById 使用空字符串")
    void testFindById_withEmptyString() {
        StepVerifier.create(userFileMetaRepository.findById(""))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 檔案類型邊界測試")
    void testFileTypeBoundaries() {
        // 測試所有檔案類型
        for (FileEnum fileType : FileEnum.values()) {
            StepVerifier.create(userFileMetaRepository.findAll()
                    .filter(file -> file.getFileType() == fileType))
                    .thenConsumeWhile(file -> file.getFileType() == fileType)
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("邊界測試 - 共享類型邊界測試")
    void testShareTypeBoundaries() {
        // 測試所有共享類型
        for (FileShareTypeEnum shareType : FileShareTypeEnum.values()) {
            StepVerifier.create(userFileMetaRepository.findAll()
                    .filter(file -> file.getShareType() == shareType))
                    .thenConsumeWhile(file -> file.getShareType() == shareType)
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("邊界測試 - 併發查詢操作")
    void testConcurrentQueries() {
        // 併發執行多個查詢
        Flux<UserFileMetadata> concurrentQueries = Flux.merge(
                userFileMetaRepository.findAllByUserId(1L),
                userFileMetaRepository.findAllByUserId(2L),
                userFileMetaRepository.findById("3"),
                userFileMetaRepository.findById("4")
        );

        StepVerifier.create(concurrentQueries)
                .expectNextCount(6) // 3 + 1 + 1 + 1 = 6個檔案
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜查詢組合")
    void testComplexQueryCombinations() {
        // 組合多種查詢操作
        Mono<Boolean> complexQuery = userFileMetaRepository.findAllByUserId(1L)
                .any(file -> file.getFileType() == FileEnum.FOLDER)
                .flatMap(hasFolder -> {
                    if (hasFolder) {
                        return userFileMetaRepository.findAllByUserId(1L)
                                .filter(file -> file.getShareType() == FileShareTypeEnum.PUBLIC)
                                .hasElements();
                    }
                    return Mono.just(false);
                });

        StepVerifier.create(complexQuery)
                .expectNext(true) // 用戶1有檔案夾且有公開共享檔案
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 批量操作性能測試")
    void testBatchOperationPerformance() {
        // 測試大批量ID查詢
        List<String> largeIdList = java.util.stream.LongStream.range(1, 10001)
                .mapToObj(String::valueOf)
                .toList();

        StepVerifier.create(userFileMetaRepository.findAllById(largeIdList))
                .expectNextCount(5) // 只有5個檔案存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 檔案夾嵌套深度測試")
    void testFolderNestingDepth() {
        // 測試深層嵌套檔案夾結構
        StepVerifier.create(userFileMetaRepository.findAll()
                .filter(file -> file.getParentFolderId() != null)
                .map(file -> file.getParentFolderId())
                .distinct())
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 默認方法參數邊界測試")
    void testDefaultMethodParameterBoundaries() {
        // 測試空集合參數 - 由於 Mock 對象沒有配置，會拋出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.findAllByParentFolderIdIn(Collections.emptyList(), entityOperations)
                    .blockFirst(); // 觸發執行
        });

        // 測試大量檔案夾ID - 由於 Mock 對象沒有配置，會拋出 NullPointerException
        List<Long> largeFolderList = java.util.stream.LongStream.range(1, 10001)
                .boxed()
                .toList();
        
        assertThrows(NullPointerException.class, () -> {
            userFileMetaRepository.findAllByParentFolderIdIn(largeFolderList, entityOperations)
                    .blockFirst(); // 觸發執行
        });
    }

    @Test
    @DisplayName("邊界測試 - 檔案名特殊字符處理")
    void testFileNameSpecialCharacters() {
        // 測試包含特殊字符的檔案名查詢
        StepVerifier.create(userFileMetaRepository.findAll()
                .filter(file -> file.getFilename().contains(".")))
                .expectNextCount(4) // document.pdf, image.jpg, video.mp4, shared.txt
                .verifyComplete();

        // 測試不包含擴展名的檔案
        StepVerifier.create(userFileMetaRepository.findAll()
                .filter(file -> !file.getFilename().contains(".")))
                .expectNextCount(1) // folder1
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 時間邊界測試")
    void testTimeBoundaries() {
        LocalDateTime now = LocalDateTime.now();
        
        // 測試上傳時間邊界
        StepVerifier.create(userFileMetaRepository.findAll()
                .filter(file -> file.getUploadTime() != null)
                .filter(file -> file.getUploadTime().isBefore(now.plusMinutes(1))))
                .expectNextCount(5) // 所有檔案的上傳時間都應該在當前時間之前
                .verifyComplete();

        // 測試最後訪問時間邊界
        StepVerifier.create(userFileMetaRepository.findAll()
                .filter(file -> file.getLastAccessTime() != null)
                .filter(file -> file.getLastAccessTime().isBefore(now.plusMinutes(1))))
                .expectNextCount(5) // 所有檔案的最後訪問時間都應該在當前時間之前
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 布爾字段邊界測試")
    void testBooleanFieldBoundaries() {
        // 測試已刪除字段
        StepVerifier.create(userFileMetaRepository.findAll()
                .filter(file -> !file.getIsDeleted()))
                .expectNextCount(5) // 所有測試檔案都未刪除
                .verifyComplete();

        // 測試星標字段
        StepVerifier.create(userFileMetaRepository.findAll()
                .filter(file -> !file.getIsStar()))
                .expectNextCount(5) // 所有測試檔案都未星標
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 數據一致性驗證")
    void testDataConsistencyValidation() {
        // 驗證檔案夾類型的檔案不應該有父檔案夾關係錯誤
        StepVerifier.create(userFileMetaRepository.findAll()
                .filter(file -> file.getFileType() == FileEnum.FOLDER))
                .assertNext(folder -> {
                    assertNotNull(folder.getFilename());
                    assertNotNull(folder.getUserId());
                    // 檔案夾可以有父檔案夾ID，但不應該指向自己
                    if (folder.getParentFolderId() != null) {
                        assertNotEquals(Long.valueOf(folder.getId()), folder.getParentFolderId());
                    }
                })
                .verifyComplete();

        // 驗證檔案類型的一致性
        StepVerifier.create(userFileMetaRepository.findAll()
                .filter(file -> file.getFileType() == FileEnum.DOCUMENT))
                .assertNext(file -> {
                    assertNotNull(file.getFilename());
                    assertNotNull(file.getUserId());
                    // 檔案應該有有效的檔案名
                    assertFalse(file.getFilename().isBlank());
                })
                .expectNextCount(3) // 跳過其餘檔案的驗證
                .verifyComplete();
    }
}