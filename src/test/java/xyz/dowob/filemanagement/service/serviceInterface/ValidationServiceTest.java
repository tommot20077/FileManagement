package xyz.dowob.filemanagement.service.serviceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ValidationService 驗證服務接口測試
 *
 * <p>測試 ValidationService 驗證服務接口的全面驗證契約和數據檢查模式，驗證接口在數據安全和業務規則檢查方面的設計正確性。
 * 
 * <p>測試涵蓋的接口方法：
 * <p>- DTO 驗證方法：Register、ResetPassword、FileMetadata、FileEdit、FileFilter、Auth
 * <p>- 檔案類型驗證方法的 FileEnum 枚舉處理
 * <p>- 用戶搜索列表驗證方法的權限控制
 * <p>- validateNotNull 默認方法的空值檢查
 * <p>- validSpecifyColumns 默認方法的欄位指定驗證
 * <p>- validLength 默認方法的長度約束檢查
 * <p>- 不同驗證場景的規則引擎和異常處理
 *
 * 測試摘要：
 * 
 * 驗證 ValidationService 接口作為數據驗證服務層的設計正確性，確保其能夠為系統提供完整的數據安全檢查能力。
 *
 * 前置條件：
 * - ValidationService 接口及其驗證規則可用
 * - 各種 DTO 數據傳輸對象和枚舉類可用
 * - ValidationException 自定義驗證異常可用
 * - User 和 UserFileMetadata 實體類可用
 * - Reactor WebFlux 響應式編程環境可用
 *
 * 測試步驟：
 * - 驗證接口方法簽名和默認方法的正確性
 * - 測試各類型 DTO 的驗證規則和約束條件
 * - 驗證默認方法的通用檢查邏輯和作用範圍
 * - 測試異常拋出機制和邊界條件處理
 *
 * 預期結果：
 * - 接口契約符合數據驗證服務設計模式
 * - 驗證規則和約束條件滿足業務安全需求
 * - 默認方法通用性和可重用性完善
 * - 驗證異常處理和程式健壯性正確
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationService 驗證服務接口測試")
class ValidationServiceTest {

    private ValidationService validationService;
    private RegisterDTO testRegisterDTO;
    private ResetPasswordDTO testResetPasswordDTO;
    private FileMetadataDTO testFileMetadataDTO;
    private FileEditDTO testFileEditDTO;
    private FileFilterDTO testFileFilterDTO;
    private AuthRequestDTO testAuthRequestDTO;
    private User testUser;
    private UserFileMetadata testFileMetadata;


    @BeforeEach
    void setUp() {
        // 創建測試用的 ValidationService 實現
        validationService = new ValidationService() {
            @Override
            public Mono<Void> validateRegisterDTO(RegisterDTO registerDTO) {
                if (registerDTO == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
                }
                if (registerDTO.getUsername() == null || registerDTO.getUsername().trim().isEmpty()) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "username"));
                }
                if (registerDTO.getEmail() == null || !registerDTO.getEmail().contains("@")) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "email"));
                }
                if (registerDTO.getPassword() == null || registerDTO.getPassword().length() < 6) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.FIELD_LENGTH_TOO_SHORT, "password", 6, registerDTO.getPassword() != null ? registerDTO.getPassword().length() : 0));
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> validateResetPasswordDTO(ResetPasswordDTO resetPasswordDTO) {
                if (resetPasswordDTO == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
                }
                if (resetPasswordDTO.getEmail() == null || !resetPasswordDTO.getEmail().contains("@")) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "email"));
                }
                if (resetPasswordDTO.getVerificationCode() == null || resetPasswordDTO.getVerificationCode().trim().isEmpty()) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "verificationCode"));
                }
                if (resetPasswordDTO.getNewPassword() == null || resetPasswordDTO.getNewPassword().length() < 6) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.FIELD_LENGTH_TOO_SHORT, "newPassword", 6, resetPasswordDTO.getNewPassword() != null ? resetPasswordDTO.getNewPassword().length() : 0));
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> validateFileMetadataDTO(FileMetadataDTO fileMetadataDTO, User user) {
                if (fileMetadataDTO == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
                }
                if (user == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
                }
                if (fileMetadataDTO.getFilename() == null || fileMetadataDTO.getFilename().trim().isEmpty()) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "fileName"));
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> validateEditFileDTO(FileEditDTO fileEditDTO, boolean isFolder) {
                if (fileEditDTO == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
                }
                if (fileEditDTO.getFileId() == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "fileId"));
                }
                if (!isFolder && (fileEditDTO.getFilename() == null || fileEditDTO.getFilename().trim().isEmpty())) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "filename"));
                }
                return Mono.empty();
            }

            @Override
            public Mono<UserFileMetadata> validateFileType(UserFileMetadata file, FileEnum... fileType) {
                if (file == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
                }
                if (fileType == null || fileType.length == 0) {
                    return Mono.just(file);
                }

                // 模擬檔案類型驗證
                String fileName = file.getFilename();
                if (fileName == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "fileName"));
                }

                // 獲取檔案擴展名
                final String extension;
                int lastDotIndex = fileName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    extension = fileName.substring(lastDotIndex + 1).toLowerCase();
                } else {
                    extension = "";
                }

                // 檢查是否匹配任何一種檔案類型
                boolean typeMatched = Arrays.stream(fileType)
                        .anyMatch(type -> isValidExtensionForType(extension, type));

                if (!typeMatched) {
                    String expectedTypes = Arrays.stream(fileType)
                            .map(FileEnum::name)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("UNKNOWN");
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.FILE_TYPE_WITH_WRONG_REQUEST_PATH, expectedTypes, extension));
                }

                return Mono.just(file);
            }

            private boolean isValidExtensionForType(String extension, FileEnum fileType) {
                switch (fileType) {
                    case IMAGE:
                        return Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension);
                    case VIDEO:
                        return Arrays.asList("mp4", "mpeg", "webm").contains(extension);
                    case MUSIC:
                        return Arrays.asList("mp3", "wav", "ogg").contains(extension);
                    case DOCUMENT:
                        return Arrays.asList("pdf", "doc", "docx", "txt").contains(extension);
                    case ZIP:
                        return Arrays.asList("zip", "rar", "7z").contains(extension);
                    default:
                        return true; // OTHER type accepts any extension
                }
            }

            @Override
            public Mono<Void> validateFileFilterDTO(FileFilterDTO fileFilterDTO) {
                if (fileFilterDTO == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> validateUserSearchList(java.util.Collection<String> searchList) {
                if (searchList == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
                }
                if (searchList.size() > 100) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.FIELD_LENGTH_TOO_LONG, "searchList", 100, searchList.size()));
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> validateAuthRequestDTO(AuthRequestDTO authRequestDTO) {
                if (authRequestDTO == null) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
                }
                if (authRequestDTO.getUsername() == null || authRequestDTO.getUsername().trim().isEmpty()) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "username"));
                }
                if (authRequestDTO.getPassword() == null || authRequestDTO.getPassword().trim().isEmpty()) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "password"));
                }
                return Mono.empty();
            }

        };

        // 設置測試對象
        testRegisterDTO = new RegisterDTO();
        testRegisterDTO.setUsername("testuser");
        testRegisterDTO.setEmail("test@example.com");
        testRegisterDTO.setPassword("password123");

        testResetPasswordDTO = new ResetPasswordDTO();
        testResetPasswordDTO.setEmail("test@example.com");
        testResetPasswordDTO.setVerificationCode("123456");
        testResetPasswordDTO.setNewPassword("newpassword123");

        testFileMetadataDTO = new FileMetadataDTO();
        testFileMetadataDTO.setFilename("test.txt");
        testFileMetadataDTO.setFileSize(1024L);

        testFileEditDTO = new FileEditDTO();
        testFileEditDTO.setFileId("1");
        testFileEditDTO.setFilename("newname.txt");

        testFileFilterDTO = FileFilterDTO.builder()
                .keyword("test")
                .build();

        testAuthRequestDTO = new AuthRequestDTO("testuser", "password");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testFileMetadata = new UserFileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setFilename("test.txt");
        testFileMetadata.setUserId(1L);
        testFileMetadata.setUploadTime(LocalDateTime.now());
    }


    @Test
    @DisplayName("一般測試 - validateRegisterDTO 方法基本功能")
    void testValidateRegisterDTO_basicFunctionality() {
        StepVerifier.create(validationService.validateRegisterDTO(testRegisterDTO))
                .verifyComplete();
    }

    // ==================== 一般測試 ====================


    @Test
    @DisplayName("一般測試 - validateResetPasswordDTO 方法基本功能")
    void testValidateResetPasswordDTO_basicFunctionality() {
        StepVerifier.create(validationService.validateResetPasswordDTO(testResetPasswordDTO))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - validateFileMetadataDTO 方法基本功能")
    void testValidateFileMetadataDTO_basicFunctionality() {
        StepVerifier.create(validationService.validateFileMetadataDTO(testFileMetadataDTO, testUser))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - validateEditFileDTO 方法基本功能")
    void testValidateEditFileDTO_basicFunctionality() {
        StepVerifier.create(validationService.validateEditFileDTO(testFileEditDTO, false))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - validateFileType 方法基本功能")
    void testValidateFileType_basicFunctionality() {
        StepVerifier.create(validationService.validateFileType(testFileMetadata, FileEnum.DOCUMENT))
                .expectNext(testFileMetadata)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - validateFileFilterDTO 方法基本功能")
    void testValidateFileFilterDTO_basicFunctionality() {
        StepVerifier.create(validationService.validateFileFilterDTO(testFileFilterDTO))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - validateUserSearchList 方法基本功能")
    void testValidateUserSearchList_basicFunctionality() {
        List<String> searchList = Arrays.asList("keyword1", "keyword2", "keyword3");

        StepVerifier.create(validationService.validateUserSearchList(searchList))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - validateAuthRequestDTO 方法基本功能")
    void testValidateAuthRequestDTO_basicFunctionality() {
        StepVerifier.create(validationService.validateAuthRequestDTO(testAuthRequestDTO))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - validateNotNull 默認方法")
    void testValidateNotNull_defaultMethod() {
        TestDTO validDTO = new TestDTO("test", "test@example.com", 25);

        StepVerifier.create(validationService.validateNotNull(validDTO))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - validSpecifyColumns 默認方法")
    void testValidSpecifyColumns_defaultMethod() {
        TestDTO validDTO = new TestDTO("test", "test@example.com", 25);

        StepVerifier.create(validationService.validSpecifyColumns(validDTO, "name", "email"))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - validLength 默認方法（只有最大長度）")
    void testValidLength_maxOnlyDefaultMethod() {
        StepVerifier.create(validationService.validLength("test", (Number) 10, "testField"))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - validLength 默認方法（最小和最大長度）")
    void testValidLength_minMaxDefaultMethod() {
        StepVerifier.create(validationService.validLength("test", (Number) 2, (Number) 10, "testField"))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證主要方法
        try {
            var validateRegisterMethod = ValidationService.class.getMethod("validateRegisterDTO", RegisterDTO.class);
            assertEquals(Mono.class, validateRegisterMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("validateRegisterDTO 方法應該存在");
        }

        try {
            var validateResetPasswordMethod = ValidationService.class.getMethod("validateResetPasswordDTO", ResetPasswordDTO.class);
            assertEquals(Mono.class, validateResetPasswordMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("validateResetPasswordDTO 方法應該存在");
        }

        try {
            var validateFileMetadataMethod = ValidationService.class.getMethod("validateFileMetadataDTO", FileMetadataDTO.class, User.class);
            assertEquals(Mono.class, validateFileMetadataMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("validateFileMetadataDTO 方法應該存在");
        }

        try {
            var validateFileTypeMethod = ValidationService.class.getMethod("validateFileType", UserFileMetadata.class, FileEnum[].class);
            assertEquals(Mono.class, validateFileTypeMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("validateFileType 方法應該存在");
        }
    }


    @Test
    @DisplayName("一般測試 - 檔案夾編輯驗證")
    void testValidateEditFileDTO_folderEdit() {
        testFileEditDTO.setFilename(null); // 檔案夾可以沒有新名稱

        StepVerifier.create(validationService.validateEditFileDTO(testFileEditDTO, true))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 多種檔案類型驗證")
    void testValidateFileType_multipleTypes() {
        testFileMetadata.setFilename("document.pdf");

        StepVerifier.create(validationService.validateFileType(testFileMetadata, FileEnum.DOCUMENT, FileEnum.IMAGE))
                .expectNext(testFileMetadata)
                .verifyComplete();
    }


    @Test
    @DisplayName("異常測試 - validateRegisterDTO 傳入 null")
    void testValidateRegisterDTO_withNull() {
        StepVerifier.create(validationService.validateRegisterDTO(null))
                .expectError(ValidationException.class)
                .verify();
    }

    // ==================== 異常測試 ====================


    @Test
    @DisplayName("異常測試 - validateRegisterDTO 用戶名為空")
    void testValidateRegisterDTO_withEmptyUsername() {
        testRegisterDTO.setUsername("");

        StepVerifier.create(validationService.validateRegisterDTO(testRegisterDTO))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateRegisterDTO 無效郵箱")
    void testValidateRegisterDTO_withInvalidEmail() {
        testRegisterDTO.setEmail("invalid-email");

        StepVerifier.create(validationService.validateRegisterDTO(testRegisterDTO))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateRegisterDTO 密碼過短")
    void testValidateRegisterDTO_withShortPassword() {
        testRegisterDTO.setPassword("123");

        StepVerifier.create(validationService.validateRegisterDTO(testRegisterDTO))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateResetPasswordDTO 傳入 null")
    void testValidateResetPasswordDTO_withNull() {
        StepVerifier.create(validationService.validateResetPasswordDTO(null))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateResetPasswordDTO 無效郵箱")
    void testValidateResetPasswordDTO_withInvalidEmail() {
        testResetPasswordDTO.setEmail("invalid-email");

        StepVerifier.create(validationService.validateResetPasswordDTO(testResetPasswordDTO))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateResetPasswordDTO 驗證碼為空")
    void testValidateResetPasswordDTO_withEmptyVerificationCode() {
        testResetPasswordDTO.setVerificationCode("");

        StepVerifier.create(validationService.validateResetPasswordDTO(testResetPasswordDTO))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateFileMetadataDTO 傳入 null DTO")
    void testValidateFileMetadataDTO_withNullDTO() {
        StepVerifier.create(validationService.validateFileMetadataDTO(null, testUser))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateFileMetadataDTO 傳入 null 用戶")
    void testValidateFileMetadataDTO_withNullUser() {
        StepVerifier.create(validationService.validateFileMetadataDTO(testFileMetadataDTO, null))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateFileMetadataDTO 檔案名為空")
    void testValidateFileMetadataDTO_withEmptyFileName() {
        testFileMetadataDTO.setFilename("");

        StepVerifier.create(validationService.validateFileMetadataDTO(testFileMetadataDTO, testUser))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateEditFileDTO 傳入 null")
    void testValidateEditFileDTO_withNull() {
        StepVerifier.create(validationService.validateEditFileDTO(null, false))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateEditFileDTO 檔案ID為空")
    void testValidateEditFileDTO_withNullFileId() {
        testFileEditDTO.setFileId(null);

        StepVerifier.create(validationService.validateEditFileDTO(testFileEditDTO, false))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateEditFileDTO 非檔案夾新名稱為空")
    void testValidateEditFileDTO_nonFolderWithEmptyName() {
        testFileEditDTO.setFilename("");

        StepVerifier.create(validationService.validateEditFileDTO(testFileEditDTO, false))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateFileType 傳入 null 檔案")
    void testValidateFileType_withNullFile() {
        StepVerifier.create(validationService.validateFileType(null, FileEnum.DOCUMENT))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateFileType 檔案類型不匹配")
    void testValidateFileType_withMismatchedType() {
        testFileMetadata.setFilename("document.pdf");

        StepVerifier.create(validationService.validateFileType(testFileMetadata, FileEnum.IMAGE))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateUserSearchList 搜索列表過大")
    void testValidateUserSearchList_withTooLargeList() {
        List<String> largeList = Collections.nCopies(101, "keyword");

        StepVerifier.create(validationService.validateUserSearchList(largeList))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateAuthRequestDTO 傳入 null")
    void testValidateAuthRequestDTO_withNull() {
        StepVerifier.create(validationService.validateAuthRequestDTO(null))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validateNotNull 傳入 null")
    void testValidateNotNull_withNull() {
        StepVerifier.create(validationService.validateNotNull(null))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validSpecifyColumns 傳入 null DTO")
    void testValidSpecifyColumns_withNullDTO() {
        StepVerifier.create(validationService.validSpecifyColumns(null, "name"))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validSpecifyColumns 字段不存在")
    void testValidSpecifyColumns_withNonExistentField() {
        TestDTO dto = new TestDTO("test", "test@example.com", 25);

        StepVerifier.create(validationService.validSpecifyColumns(dto, "nonExistentField"))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validSpecifyColumns 字段為空")
    void testValidSpecifyColumns_withBlankField() {
        TestDTO dto = new TestDTO("", "test@example.com", 25);

        StepVerifier.create(validationService.validSpecifyColumns(dto, "name"))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validLength 字符串過短")
    void testValidLength_tooShort() {
        StepVerifier.create(validationService.validLength("ab", (Number) 5, (Number) 10, "testField"))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - validLength 字符串過長")
    void testValidLength_tooLong() {
        StepVerifier.create(validationService.validLength("verylongstring", (Number) 10, "testField"))
                .expectError(ValidationException.class)
                .verify();
    }


    @Test
    @DisplayName("邊界測試 - validateRegisterDTO 最短有效密碼")
    void testValidateRegisterDTO_withMinValidPassword() {
        testRegisterDTO.setPassword("123456"); // 剛好6位

        StepVerifier.create(validationService.validateRegisterDTO(testRegisterDTO))
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================


    @Test
    @DisplayName("邊界測試 - validateFileType 無檔案類型限制")
    void testValidateFileType_withNoTypeRestriction() {
        StepVerifier.create(validationService.validateFileType(testFileMetadata))
                .expectNext(testFileMetadata)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - validateFileType 空檔案類型數組")
    void testValidateFileType_withEmptyTypeArray() {
        FileEnum[] emptyTypes = new FileEnum[0];

        StepVerifier.create(validationService.validateFileType(testFileMetadata, emptyTypes))
                .expectNext(testFileMetadata)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - validateUserSearchList 空列表")
    void testValidateUserSearchList_withEmptyList() {
        StepVerifier.create(validationService.validateUserSearchList(Collections.emptyList()))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - validateUserSearchList 最大允許大小")
    void testValidateUserSearchList_withMaxAllowedSize() {
        List<String> maxSizeList = Collections.nCopies(100, "keyword");

        StepVerifier.create(validationService.validateUserSearchList(maxSizeList))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - validLength null 值處理")
    void testValidLength_withNullValue() {
        StepVerifier.create(validationService.validLength(null, (Number) 5, (Number) 10, "testField"))
                .verifyComplete(); // null 值應該被忽略
    }


    @Test
    @DisplayName("邊界測試 - validLength 負數長度限制")
    void testValidLength_withNegativeLimits() {
        StepVerifier.create(validationService.validLength("test", (Number) (-1), (Number) (-1), "testField"))
                .verifyComplete(); // 負數應該被忽略
    }


    @Test
    @DisplayName("邊界測試 - validLength null 最小長度")
    void testValidLength_withNullMinLength() {
        StepVerifier.create(validationService.validLength("test", (Number) null, (Number) 10, "testField"))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - validLength null 最大長度")
    void testValidLength_withNullMaxLength() {
        StepVerifier.create(validationService.validLength("test", (Number) 2, (Number) null, "testField"))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - validSpecifyColumns 多個字段")
    void testValidSpecifyColumns_withMultipleFields() {
        TestDTO dto = new TestDTO("test", "test@example.com", 25);

        StepVerifier.create(validationService.validSpecifyColumns(dto, "name", "email", "age"))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - validateRegisterDTO 極長用戶名")
    void testValidateRegisterDTO_withVeryLongUsername() {
        testRegisterDTO.setUsername("a".repeat(1000));

        StepVerifier.create(validationService.validateRegisterDTO(testRegisterDTO))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - validateFileType 極長檔案名")
    void testValidateFileType_withVeryLongFileName() {
        testFileMetadata.setFilename("a".repeat(1000) + ".txt");

        StepVerifier.create(validationService.validateFileType(testFileMetadata, FileEnum.DOCUMENT))
                .expectNext(testFileMetadata)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - validateAuthRequestDTO 特殊字符")
    void testValidateAuthRequestDTO_withSpecialCharacters() {
        AuthRequestDTO specialAuthDTO = new AuthRequestDTO("user<>&\"'`", "pass<>&\"'`");

        StepVerifier.create(validationService.validateAuthRequestDTO(specialAuthDTO))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - validateRegisterDTO Unicode 字符")
    void testValidateRegisterDTO_withUnicodeCharacters() {
        testRegisterDTO.setUsername("用戶名測試🔒");
        testRegisterDTO.setPassword("密碼測試🔑123");

        StepVerifier.create(validationService.validateRegisterDTO(testRegisterDTO))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 驗證接口完整性")
    void testInterfaceCompleteness() {
        // 驗證接口是 public 的
        assertTrue(java.lang.reflect.Modifier.isPublic(ValidationService.class.getModifiers()));

        // 驗證接口是 interface
        assertTrue(ValidationService.class.isInterface());

        // 驗證方法數量（包括默認方法）
        assertTrue(ValidationService.class.getDeclaredMethods().length >= 10);

        // 驗證默認方法數量
        long defaultMethodCount = Arrays.stream(ValidationService.class.getDeclaredMethods())
                .filter(java.lang.reflect.Method::isDefault)
                .count();
        assertTrue(defaultMethodCount >= 3);
    }


    @Test
    @DisplayName("邊界測試 - 複雜驗證流程")
    void testComplexValidationWorkflow() {
        // 多步驗證流程
        Mono<String> complexValidation = validationService.validateRegisterDTO(testRegisterDTO)
                .then(validationService.validateAuthRequestDTO(testAuthRequestDTO))
                .then(validationService.validateFileMetadataDTO(testFileMetadataDTO, testUser))
                .then(validationService.validateNotNull(testUser))
                .thenReturn("所有驗證完成");

        StepVerifier.create(complexValidation)
                .expectNext("所有驗證完成")
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 併發驗證操作")
    void testConcurrentValidation() {
        Mono<Long> concurrentValidations = reactor.core.publisher.Flux.range(1, 10)
                .flatMap(i -> validationService.validateRegisterDTO(testRegisterDTO).then(Mono.just(i)))
                .count();

        StepVerifier.create(concurrentValidations)
                .expectNext(10L)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 錯誤恢復和重試")
    void testErrorRecoveryAndRetry() {
        // 先嘗試無效數據，然後重試有效數據
        RegisterDTO invalidDTO = new RegisterDTO();
        invalidDTO.setUsername("");
        invalidDTO.setEmail("invalid");
        invalidDTO.setPassword("123");

        Mono<String> retryValidation = validationService.validateRegisterDTO(invalidDTO)
                .onErrorResume(error -> validationService.validateRegisterDTO(testRegisterDTO))
                .thenReturn("重試成功");

        StepVerifier.create(retryValidation)
                .expectNext("重試成功")
                .verifyComplete();
    }

    /**
     * 測試用的 DTO 類
     */
    private static class TestDTO {
        private String name;
        private String email;
        private Integer age;

        public TestDTO() {}
        public TestDTO(String name, String email, Integer age) {
            this.name = name;
            this.email = email;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }
}