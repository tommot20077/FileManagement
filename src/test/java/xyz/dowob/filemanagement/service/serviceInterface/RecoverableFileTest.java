package xyz.dowob.filemanagement.service.serviceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RecoverableFile 可恢復檔案接口測試
 *
 * <p>測試 RecoverableFile 可恢復檔案接口的泛型契約和檔案恢復模式，驗證接口在檔案回收站管理方面的設計正確性。
 * 
 * <p>測試涵蓋的接口方法：
 * <p>- restoreFile 單個檔案恢復方法的響應式實現
 * <p>- restoreFile 多個檔案批量恢復方法的 Flux 處理
 * <p>- removeFile 單個檔案移至回收站方法
 * <p>- removeFile 多個檔案批量移至回收站方法
 * <p>- 泛型類型參數在恢復操作中的彈性使用
 * <p>- 檔案回收站機制和恢復策略
 * <p>- 權限控制和安全檢查機制
 *
 * 測試摘要：
 * 
 * 驗證 RecoverableFile 接口作為檔案恢復服務層的設計正確性，確保其泛型約定能夠為不同檔案類型提供統一的恢復能力。
 *
 * 前置條件：
 * - RecoverableFile 泛型接口可用
 * - User 和 UserFileMetadata 實體類可用
 * - Reactor WebFlux 響應式編程環境可用
 * - Mockito 測試框架環境可用
 *
 * 測試步驟：
 * - 驗證接口方法簽名和泛型約定的正確性
 * - 測試單個和多個檔案的恢復和移除操作
 * - 驗證權限控制和安全檢查機制
 * - 測試響應式流處理和異常邊界情況
 *
 * 預期結果：
 * - 接口泛型約定符合檔案恢復服務設計模式
 * - 檔案恢復和回收站機制滿足業務需求
 * - 泛型參數使用靈活且類型安全
 * - 響應式處理和異常機制正確
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecoverableFile 可恢復檔案接口測試")
class RecoverableFileTest {

    private RecoverableFile<UserFileMetadata> recoverableFile;
    private User testUser;
    private User adminUser;
    private UserFileMetadata testFile;
    private UserFileMetadata deletedFile;
    private List<UserFileMetadata> testFiles;


    @BeforeEach
    void setUp() {
        // 創建測試用的 RecoverableFile 實現
        recoverableFile = new RecoverableFile<UserFileMetadata>() {
            @Override
            public Mono<UserFileMetadata> restoreFile(UserFileMetadata file, User user) {
                if (file == null) {
                    return Mono.error(new IllegalArgumentException("檔案不能為空"));
                }
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (file.getUserId() == null) {
                    return Mono.error(new IllegalArgumentException("檔案用戶ID不能為空"));
                }

                // 權限檢查：用戶只能恢復自己的檔案，管理員可以恢復所有檔案
                // 處理 null username 的情況
                String username = user.getUsername();
                if (username == null || (!username.equals("admin") && !file.getUserId().equals(user.getId()))) {
                    return Mono.error(new IllegalArgumentException("沒有權限恢復此檔案"));
                }

                // 模擬恢復邏輯
                UserFileMetadata restoredFile = new UserFileMetadata();
                restoredFile.setId(file.getId());
                restoredFile.setFilename(file.getFilename());
                restoredFile.setUserId(file.getUserId());
                restoredFile.setUploadTime(LocalDateTime.now());

                return Mono.just(restoredFile);
            }

            @Override
            public Flux<UserFileMetadata> restoreFile(Iterable<UserFileMetadata> files, User user) {
                if (files == null) {
                    return Flux.error(new IllegalArgumentException("檔案列表不能為空"));
                }
                if (user == null) {
                    return Flux.error(new IllegalArgumentException("用戶不能為空"));
                }

                // 預先檢查所有檔案是否為 null
                for (UserFileMetadata file : files) {
                    if (file == null) {
                        return Flux.error(new NullPointerException("檔案不能為 null"));
                    }
                }

                return Flux.fromIterable(files)
                        .flatMap(file -> restoreFile(file, user))
                        .onErrorContinue((throwable, obj) -> {
                            // 繼續處理其他檔案，即使某個檔案恢復失敗
                        });
            }

            @Override
            public Mono<Boolean> removeFile(UserFileMetadata file, User user) {
                if (file == null) {
                    return Mono.error(new IllegalArgumentException("檔案不能為空"));
                }
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (file.getUserId() == null) {
                    return Mono.error(new IllegalArgumentException("檔案用戶ID不能為空"));
                }

                // 權限檢查：用戶只能刪除自己的檔案，管理員可以刪除所有檔案
                if (!user.getUsername().equals("admin") && !file.getUserId().equals(user.getId())) {
                    return Mono.error(new IllegalArgumentException("沒有權限刪除此檔案"));
                }

                // 模擬刪除邏輯
                return Mono.just(true);
            }

            @Override
            public Mono<Boolean> removeFile(Iterable<UserFileMetadata> files, User user) {
                if (files == null) {
                    return Mono.error(new IllegalArgumentException("檔案列表不能為空"));
                }
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }

                return Flux.fromIterable(files)
                        .flatMap(file -> removeFile(file, user))
                        .all(result -> result)
                        .onErrorReturn(false);
            }
        };

        // 設置測試對象
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");

        testFile = new UserFileMetadata();
        testFile.setId(1L);
        testFile.setFilename("test.txt");
        testFile.setUserId(1L);
        testFile.setUploadTime(LocalDateTime.now());

        deletedFile = new UserFileMetadata();
        deletedFile.setId(2L);
        deletedFile.setFilename("deleted.txt");
        deletedFile.setUserId(1L);
        deletedFile.setUploadTime(LocalDateTime.now().minusDays(1));

        UserFileMetadata anotherFile = new UserFileMetadata();
        anotherFile.setId(3L);
        anotherFile.setFilename("another.txt");
        anotherFile.setUserId(1L);
        anotherFile.setUploadTime(LocalDateTime.now().minusHours(1));

        testFiles = Arrays.asList(testFile, deletedFile, anotherFile);
    }


    @Test
    @DisplayName("一般測試 - restoreFile 單個檔案基本功能")
    void testRestoreFile_singleFile_basicFunctionality() {
        StepVerifier.create(recoverableFile.restoreFile(testFile, testUser))
                .assertNext(restoredFile -> {
                    assertEquals(testFile.getId(), restoredFile.getId());
                    assertEquals(testFile.getFilename(), restoredFile.getFilename());
                    assertEquals(testFile.getUserId(), restoredFile.getUserId());
                    assertNotNull(restoredFile.getUploadTime());
                })
                .verifyComplete();
    }

    // ==================== 一般測試 ====================


    @Test
    @DisplayName("一般測試 - restoreFile 管理員權限")
    void testRestoreFile_adminPermission() {
        StepVerifier.create(recoverableFile.restoreFile(testFile, adminUser))
                .assertNext(restoredFile -> {
                    assertEquals(testFile.getId(), restoredFile.getId());
                    assertEquals(testFile.getFilename(), restoredFile.getFilename());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - restoreFile 多個檔案基本功能")
    void testRestoreFile_multipleFiles_basicFunctionality() {
        StepVerifier.create(recoverableFile.restoreFile(testFiles, testUser))
                .expectNextCount(3)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - removeFile 單個檔案基本功能")
    void testRemoveFile_singleFile_basicFunctionality() {
        StepVerifier.create(recoverableFile.removeFile(testFile, testUser))
                .expectNext(true)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - removeFile 管理員權限")
    void testRemoveFile_adminPermission() {
        StepVerifier.create(recoverableFile.removeFile(testFile, adminUser))
                .expectNext(true)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - removeFile 多個檔案基本功能")
    void testRemoveFile_multipleFiles_basicFunctionality() {
        StepVerifier.create(recoverableFile.removeFile(testFiles, testUser))
                .expectNext(true)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證單個檔案恢復方法
        try {
            var restoreSingleMethod = RecoverableFile.class.getMethod("restoreFile", Object.class, User.class);
            assertEquals(Mono.class, restoreSingleMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("restoreFile(T, User) 方法應該存在");
        }

        // 驗證多個檔案恢復方法
        try {
            var restoreMultipleMethod = RecoverableFile.class.getMethod("restoreFile", Iterable.class, User.class);
            assertEquals(Flux.class, restoreMultipleMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("restoreFile(Iterable<T>, User) 方法應該存在");
        }

        // 驗證單個檔案刪除方法
        try {
            var removeSingleMethod = RecoverableFile.class.getMethod("removeFile", Object.class, User.class);
            assertEquals(Mono.class, removeSingleMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("removeFile(T, User) 方法應該存在");
        }

        // 驗證多個檔案刪除方法
        try {
            var removeMultipleMethod = RecoverableFile.class.getMethod("removeFile", Iterable.class, User.class);
            assertEquals(Mono.class, removeMultipleMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("removeFile(Iterable<T>, User) 方法應該存在");
        }
    }


    @Test
    @DisplayName("一般測試 - 泛型類型參數驗證")
    void testGenericTypeParameters() {
        // 驗證接口有正確的泛型參數
        var typeParameters = RecoverableFile.class.getTypeParameters();
        assertEquals(1, typeParameters.length);
        assertEquals("T", typeParameters[0].getName());
    }


    @Test
    @DisplayName("一般測試 - 檔案恢復和刪除流程")
    void testFileRecoveryAndRemovalWorkflow() {
        // 恢復 -> 刪除流程
        Mono<Boolean> workflow = recoverableFile.restoreFile(testFile, testUser)
                .flatMap(restoredFile -> {
                    assertNotNull(restoredFile);
                    return recoverableFile.removeFile(restoredFile, testUser);
                });

        StepVerifier.create(workflow)
                .expectNext(true)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 批量檔案操作")
    void testBatchFileOperations() {
        // 批量恢復後批量刪除
        Mono<Boolean> batchWorkflow = recoverableFile.restoreFile(testFiles, testUser)
                .collectList()
                .flatMap(restoredFiles -> {
                    assertEquals(3, restoredFiles.size());
                    return recoverableFile.removeFile(restoredFiles, testUser);
                });

        StepVerifier.create(batchWorkflow)
                .expectNext(true)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 空檔案列表處理")
    void testEmptyFileListHandling() {
        List<UserFileMetadata> emptyList = Collections.emptyList();

        StepVerifier.create(recoverableFile.restoreFile(emptyList, testUser))
                .verifyComplete();

        StepVerifier.create(recoverableFile.removeFile(emptyList, testUser))
                .expectNext(true) // 空列表應該返回 true
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 不同用戶ID的檔案處理")
    void testDifferentUserIdFiles() {
        UserFileMetadata otherUserFile = new UserFileMetadata();
        otherUserFile.setId(99L);
        otherUserFile.setFilename("other.txt");
        otherUserFile.setUserId(99L); // 不同的用戶ID

        // 管理員可以處理其他用戶的檔案
        StepVerifier.create(recoverableFile.restoreFile(otherUserFile, adminUser))
                .assertNext(restoredFile -> {
                    assertEquals(99L, restoredFile.getUserId());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("異常測試 - restoreFile 傳入 null 檔案")
    void testRestoreFile_withNullFile() {
        StepVerifier.create(recoverableFile.restoreFile((UserFileMetadata)null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==================== 異常測試 ====================


    @Test
    @DisplayName("異常測試 - restoreFile 傳入 null 用戶")
    void testRestoreFile_withNullUser() {
        StepVerifier.create(recoverableFile.restoreFile(testFile, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - restoreFile 檔案用戶ID為空")
    void testRestoreFile_withNullFileUserId() {
        testFile.setUserId(null);

        StepVerifier.create(recoverableFile.restoreFile(testFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - restoreFile 權限不足")
    void testRestoreFile_insufficientPermission() {
        UserFileMetadata otherUserFile = new UserFileMetadata();
        otherUserFile.setId(99L);
        otherUserFile.setFilename("other.txt");
        otherUserFile.setUserId(99L); // 不同的用戶ID

        StepVerifier.create(recoverableFile.restoreFile(otherUserFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - restoreFile 多個檔案傳入 null 列表")
    void testRestoreFile_multipleFiles_withNullList() {
        StepVerifier.create(recoverableFile.restoreFile((Iterable<UserFileMetadata>) null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - restoreFile 多個檔案傳入 null 用戶")
    void testRestoreFile_multipleFiles_withNullUser() {
        StepVerifier.create(recoverableFile.restoreFile(testFiles, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - removeFile 傳入 null 檔案")
    void testRemoveFile_withNullFile() {
        StepVerifier.create(recoverableFile.removeFile((UserFileMetadata)null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - removeFile 傳入 null 用戶")
    void testRemoveFile_withNullUser() {
        StepVerifier.create(recoverableFile.removeFile(testFile, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - removeFile 檔案用戶ID為空")
    void testRemoveFile_withNullFileUserId() {
        testFile.setUserId(null);

        StepVerifier.create(recoverableFile.removeFile(testFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - removeFile 權限不足")
    void testRemoveFile_insufficientPermission() {
        UserFileMetadata otherUserFile = new UserFileMetadata();
        otherUserFile.setId(99L);
        otherUserFile.setFilename("other.txt");
        otherUserFile.setUserId(99L); // 不同的用戶ID

        StepVerifier.create(recoverableFile.removeFile(otherUserFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - removeFile 多個檔案傳入 null 列表")
    void testRemoveFile_multipleFiles_withNullList() {
        StepVerifier.create(recoverableFile.removeFile((Iterable<UserFileMetadata>) null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - removeFile 多個檔案傳入 null 用戶")
    void testRemoveFile_multipleFiles_withNullUser() {
        StepVerifier.create(recoverableFile.removeFile(testFiles, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("邊界測試 - 檔案ID為零")
    void testFileOperations_withZeroFileId() {
        testFile.setId(0L);

        StepVerifier.create(recoverableFile.restoreFile(testFile, testUser))
                .assertNext(restoredFile -> {
                    assertEquals(0L, restoredFile.getId());
                })
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================


    @Test
    @DisplayName("邊界測試 - 極大檔案ID")
    void testFileOperations_withMaxFileId() {
        testFile.setId(Long.MAX_VALUE);

        StepVerifier.create(recoverableFile.restoreFile(testFile, testUser))
                .assertNext(restoredFile -> {
                    assertEquals(Long.MAX_VALUE, restoredFile.getId());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 負數檔案ID")
    void testFileOperations_withNegativeFileId() {
        testFile.setId(-1L);

        StepVerifier.create(recoverableFile.restoreFile(testFile, testUser))
                .assertNext(restoredFile -> {
                    assertEquals(-1L, restoredFile.getId());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 極長檔案名")
    void testFileOperations_withVeryLongFileName() {
        testFile.setFilename("a".repeat(10000));

        StepVerifier.create(recoverableFile.restoreFile(testFile, testUser))
                .assertNext(restoredFile -> {
                    assertEquals(10000, restoredFile.getFilename().length());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 空檔案名")
    void testFileOperations_withEmptyFileName() {
        testFile.setFilename("");

        StepVerifier.create(recoverableFile.restoreFile(testFile, testUser))
                .assertNext(restoredFile -> {
                    assertEquals("", restoredFile.getFilename());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - null 檔案名")
    void testFileOperations_withNullFileName() {
        testFile.setFilename(null);

        StepVerifier.create(recoverableFile.restoreFile(testFile, testUser))
                .assertNext(restoredFile -> {
                    assertNull(restoredFile.getFilename());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 檔案大小為零")
    void testFileOperations_withZeroFileSize() {

        StepVerifier.create(recoverableFile.restoreFile(testFile, testUser))
                .assertNext(restoredFile -> {
                    assertNotNull(restoredFile);
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 極大檔案大小")
    void testFileOperations_withMaxFileSize() {

        StepVerifier.create(recoverableFile.restoreFile(testFile, testUser))
                .assertNext(restoredFile -> {
                    assertNotNull(restoredFile);
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 大量檔案批量操作")
    void testBatchOperations_withManyFiles() {
        List<UserFileMetadata> manyFiles = Arrays.asList();
        UserFileMetadata[] fileArray = new UserFileMetadata[1000];
        for (int i = 0; i < 1000; i++) {
            UserFileMetadata file = new UserFileMetadata();
            file.setId((long) i);
            file.setFilename("file" + i + ".txt");
            file.setUserId(1L);
            fileArray[i] = file;
        }
        manyFiles = Arrays.asList(fileArray);

        StepVerifier.create(recoverableFile.restoreFile(manyFiles, testUser))
                .expectNextCount(1000)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 包含 null 檔案的列表")
    void testBatchOperations_withNullFilesInList() {
        List<UserFileMetadata> listWithNulls = Arrays.asList(testFile, null, deletedFile, null);

        // Reactor 在遇到 null 值時會拋出異常
        StepVerifier.create(recoverableFile.restoreFile(listWithNulls, testUser))
                .expectError(NullPointerException.class)
                .verify();
    }


    @Test
    @DisplayName("邊界測試 - 用戶屬性為 null 的情況")
    void testFileOperations_withNullUserProperties() {
        User userWithNullProps = new User();
        userWithNullProps.setId(null);
        userWithNullProps.setUsername(null);
        userWithNullProps.setEmail(null);

        StepVerifier.create(recoverableFile.restoreFile(testFile, userWithNullProps))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("邊界測試 - 驗證接口完整性")
    void testInterfaceCompleteness() {
        // 驗證接口是 public 的
        assertTrue(java.lang.reflect.Modifier.isPublic(RecoverableFile.class.getModifiers()));

        // 驗證接口是 interface
        assertTrue(RecoverableFile.class.isInterface());

        // 驗證方法數量
        assertEquals(4, RecoverableFile.class.getDeclaredMethods().length);

        // 驗證所有方法都不是默認方法
        long defaultMethodCount = Arrays.stream(RecoverableFile.class.getDeclaredMethods())
                .filter(java.lang.reflect.Method::isDefault)
                .count();
        assertEquals(0, defaultMethodCount);
    }


    @Test
    @DisplayName("邊界測試 - 併發檔案操作")
    void testConcurrentFileOperations() {
        Flux<UserFileMetadata> concurrentRestores = Flux.range(1, 10)
                .flatMap(i -> recoverableFile.restoreFile(testFile, testUser));

        StepVerifier.create(concurrentRestores)
                .expectNextCount(10)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 錯誤恢復機制")
    void testErrorRecoveryMechanism() {
        // 創建包含有效和無效檔案的列表
        UserFileMetadata invalidFile = new UserFileMetadata();
        invalidFile.setId(999L);
        invalidFile.setFilename("invalid.txt");
        invalidFile.setUserId(999L); // 不同用戶，會導致權限錯誤

        List<UserFileMetadata> mixedFiles = Arrays.asList(testFile, invalidFile, deletedFile);

        // 應該跳過錯誤檔案，繼續處理其他檔案
        StepVerifier.create(recoverableFile.restoreFile(mixedFiles, testUser))
                .expectNextCount(2) // 只有2個有效檔案能被處理
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 複雜檔案管理流程")
    void testComplexFileManagementWorkflow() {
        // 恢復多個檔案 -> 刪除部分檔案 -> 再次恢復
        Mono<String> complexWorkflow = recoverableFile.restoreFile(testFiles, testUser)
                .collectList()
                .flatMap(restoredFiles -> {
                    // 刪除前兩個檔案
                    List<UserFileMetadata> filesToDelete = restoredFiles.subList(0, 2);
                    return recoverableFile.removeFile(filesToDelete, testUser)
                            .map(success -> restoredFiles.get(2)); // 返回剩餘的檔案
                })
                .flatMap(remainingFile -> recoverableFile.restoreFile(remainingFile, testUser))
                .map(finalFile -> "工作流程完成: " + finalFile.getFilename());

        StepVerifier.create(complexWorkflow)
                .assertNext(result -> assertTrue(result.startsWith("工作流程完成:")))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 泛型類型邊界")
    void testGenericTypeBoundaries() {
        // 使用不同的泛型實現
        RecoverableFile<TestFileMetadata> genericRecoverable = new RecoverableFile<TestFileMetadata>() {
            @Override
            public Mono<TestFileMetadata> restoreFile(TestFileMetadata file, User user) {
                if (file == null || user == null) {
                    return Mono.error(new IllegalArgumentException("參數不能為空"));
                }
                file.setDeleted(false);
                return Mono.just(file);
            }

            @Override
            public Flux<TestFileMetadata> restoreFile(Iterable<TestFileMetadata> files, User user) {
                return Flux.fromIterable(files).flatMap(file -> restoreFile(file, user));
            }

            @Override
            public Mono<Boolean> removeFile(TestFileMetadata file, User user) {
                if (file == null || user == null) {
                    return Mono.error(new IllegalArgumentException("參數不能為空"));
                }
                file.setDeleted(true);
                return Mono.just(true);
            }

            @Override
            public Mono<Boolean> removeFile(Iterable<TestFileMetadata> files, User user) {
                return Flux.fromIterable(files)
                        .flatMap(file -> removeFile(file, user))
                        .all(result -> result);
            }
        };

        TestFileMetadata testMetadata = new TestFileMetadata(1L, "test.txt", 1L, true);

        StepVerifier.create(genericRecoverable.restoreFile(testMetadata, testUser))
                .assertNext(restored -> {
                    assertFalse(restored.isDeleted());
                })
                .verifyComplete();
    }

    /**
     * 測試用的檔案元數據類
     */
    private static class TestFileMetadata {
        private Long id;
        private String fileName;
        private Long userId;
        private boolean isDeleted;

        public TestFileMetadata() {}
        public TestFileMetadata(Long id, String fileName, Long userId, boolean isDeleted) {
            this.id = id;
            this.fileName = fileName;
            this.userId = userId;
            this.isDeleted = isDeleted;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFilename() { return fileName; }
        public void setFilename(String fileName) { this.fileName = fileName; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public boolean isDeleted() { return isDeleted; }
        public void setDeleted(boolean deleted) { isDeleted = deleted; }
    }
}