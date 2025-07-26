package xyz.dowob.filemanagement.service.serviceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.customenum.EditTypeEnum;
import xyz.dowob.filemanagement.data.file.dto.EditorContentDTO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.UserRepository;

import java.util.List;

import static org.mockito.Mockito.when;
@DisplayName("ValidationServiceI 邏輯處理測試")
@ExtendWith(MockitoExtension.class)
class ValidationServiceImplTest {

    @Mock
    private UserRepository mockUserRepository;

    private ValidationServiceImpl validationServiceImplUnderTest;

    @BeforeEach
    void setUp() {
        validationServiceImplUnderTest = new ValidationServiceImpl(mockUserRepository);
    }

    @Test
    @DisplayName("驗證 RegisterDTO - DTO 為 null - 拋出 ValidationException")
    void testValidateRegisterDTO_DTONull_ThrowsValidationException() {
        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(null);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                .verify();
    }


    @Test
    @DisplayName("驗證 RegisterDTO - 密碼與確認密碼不一致 - 拋出 CONFIRM_PASSWORD_NOT_MATCH")
    void testValidateRegisterDTO_PasswordsNotMatch_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("validUser123");
        registerDTO.setPassword("Password123");
        registerDTO.setConfirmPassword("Different123");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("validUser123")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.CONFIRM_PASSWORD_NOT_MATCH)
                .verify();
    }

    @Test
    @DisplayName("驗證 RegisterDTO - 使用者名稱已存在 - 拋出 USERNAME_INVALID")
    void testValidateRegisterDTO_UsernameExists_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("existingUser");
        registerDTO.setPassword("Password123");
        registerDTO.setConfirmPassword("Password123");
        registerDTO.setEmail("test@example.com");

        User existingUser = new User();
        existingUser.setUsername("existingUser");
        when(mockUserRepository.findByUsername("existingUser")).thenReturn(Mono.just(existingUser));
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.USERNAME_INVALID)
                .verify();
    }


    @Test
    @DisplayName("驗證 RegisterDTO - 使用者名稱包含非法字元 - 拋出 USERNAME_INVALID")
    void testValidateRegisterDTO_UsernameInvalidCharacters_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("user@name");
        registerDTO.setPassword("Password123");
        registerDTO.setConfirmPassword("Password123");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("user@name")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.USERNAME_INVALID)
                .verify();
    }

    @Test
    @DisplayName("驗證 RegisterDTO - 信箱已存在 - 拋出 EMAIL_ALREADY_EXISTS")
    void testValidateRegisterDTO_EmailExists_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("validUser123");
        registerDTO.setPassword("Password123");
        registerDTO.setConfirmPassword("Password123");
        registerDTO.setEmail("existing@example.com");

        when(mockUserRepository.findByUsername("validUser123")).thenReturn(Mono.empty());
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        when(mockUserRepository.findByEmail("existing@example.com")).thenReturn(Mono.just(existingUser));

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.EMAIL_ALREADY_EXISTS)
                .verify();
    }

    @Test
    @DisplayName("驗證 RegisterDTO - 密碼為回文 - 拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
    void testValidateRegisterDTO_PasswordIsPalindrome_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("validUser123");
        registerDTO.setPassword("deked");
        registerDTO.setConfirmPassword("deked");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("validUser123")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH)
                .verify();
    }

    @Test
    @DisplayName("驗證 RegisterDTO - 密碼缺少大寫字母 - 拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
    void testValidateRegisterDTO_PasswordNoUppercase_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("validUser123");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("validUser123")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH)
                .verify();
    }

    @Test
    @DisplayName("驗證 RegisterDTO - 輸入合法 - 驗證成功")
    void testValidateRegisterDTO_ValidInput_Success() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("validUser123");
        registerDTO.setPassword("Password123");
        registerDTO.setConfirmPassword("Password123");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("validUser123")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("驗證 RegisterDTO - 查詢使用者名稱時發生錯誤 - 拋出例外")
    void testValidateRegisterDTO_UsernameQueryError_ThrowsException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("validUser123");
        registerDTO.setPassword("Password123");
        registerDTO.setConfirmPassword("Password123");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("validUser123")).thenReturn(Mono.error(new RuntimeException("DB Error")));
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("DB Error"))
                .verify();
    }

    @Test
    @DisplayName("驗證 RegisterDTO - 查詢信箱時發生錯誤 - 拋出例外")
    void testValidateRegisterDTO_EmailQueryError_ThrowsException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("validUser123");
        registerDTO.setPassword("Password123");
        registerDTO.setConfirmPassword("Password123");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("validUser123")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.error(new RuntimeException("DB Error")));

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("DB Error"))
                .verify();
    }

    @Test
    @DisplayName("驗證 RegisterDTO - 使用者名稱為空 - 拋出 BLANK_FIELD")
    void testValidateRegisterDTO_EmptyUsername_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("");
        registerDTO.setPassword("Password123");
        registerDTO.setConfirmPassword("Password123");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.BLANK_FIELD)
                .verify();
    }

    @Test
    @DisplayName("驗證 RegisterDTO - 密碼為空 - 拋出 BLANK_FIELD")
    void testValidateRegisterDTO_EmptyPassword_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("validUser123");
        registerDTO.setPassword("");
        registerDTO.setConfirmPassword("");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("validUser123")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.BLANK_FIELD)
                .verify();
    }


    @Test
    @DisplayName("驗證 ResetPasswordDTO - 輸入合法 - 驗證成功")
    void testValidateResetPasswordDTO_ValidInput_Success() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setNewPassword("NewPass123");
        resetPasswordDTO.setConfirmPassword("NewPass123");

        Mono<Void> result = validationServiceImplUnderTest.validateResetPasswordDTO(resetPasswordDTO);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("驗證 ResetPasswordDTO - DTO 為 null - 拋出 NULL_DTO")
    void testValidateResetPasswordDTO_DTONull_ThrowsValidationException() {
        Mono<Void> result = validationServiceImplUnderTest.validateResetPasswordDTO(null);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                .verify();
    }

    @Test
    @DisplayName("驗證 ResetPasswordDTO - 新密碼與確認密碼不一致 - 拋出 CONFIRM_PASSWORD_NOT_MATCH")
    void testValidateResetPasswordDTO_PasswordsNotMatch_ThrowsValidationException() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setNewPassword("NewPass123");
        resetPasswordDTO.setConfirmPassword("Different123");

        Mono<Void> result = validationServiceImplUnderTest.validateResetPasswordDTO(resetPasswordDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.CONFIRM_PASSWORD_NOT_MATCH)
                .verify();
    }

    @Test
    @DisplayName("驗證 ResetPasswordDTO - 新密碼為回文 - 拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
    void testValidateResetPasswordDTO_PasswordIsPalindrome_ThrowsValidationException() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setNewPassword("deked");
        resetPasswordDTO.setConfirmPassword("deked");

        Mono<Void> result = validationServiceImplUnderTest.validateResetPasswordDTO(resetPasswordDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH)
                .verify();
    }

    @Test
    @DisplayName("驗證 ResetPasswordDTO - 新密碼缺少大寫字母 - 拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
    void testValidateResetPasswordDTO_PasswordNoUppercase_ThrowsValidationException() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setNewPassword("newpass123");
        resetPasswordDTO.setConfirmPassword("newpass123");

        Mono<Void> result = validationServiceImplUnderTest.validateResetPasswordDTO(resetPasswordDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH)
                .verify();
    }

    @Test
    @DisplayName("驗證 ResetPasswordDTO - 新密碼為空 - 拋出 BLANK_FIELD")
    void testValidateResetPasswordDTO_EmptyPassword_ThrowsValidationException() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setNewPassword("");
        resetPasswordDTO.setConfirmPassword("");

        Mono<Void> result = validationServiceImplUnderTest.validateResetPasswordDTO(resetPasswordDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.BLANK_FIELD)
                .verify();
    }


    @Test
    @DisplayName("驗證 FileMetadataDTO - 上傳檔案資料合法 - 驗證成功")
    void testValidateFileMetadataDTO_ValidInput_Success() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("document.txt");
        fileMetadataDTO.setFileSize(1024L);
        User user = new User();
        user.setStorageLimit(1048576L);
        user.setUsedStorage(0L);

        Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(fileMetadataDTO, user);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("驗證 FileMetadataDTO - DTO 為 null - 拋出 NULL_DTO")
    void testValidateFileMetadataDTO_DTONull_ThrowsValidationException() {
        User user = new User();
        FileMetadataDTO userFileMetadata = null;

        Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(userFileMetadata, user);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                .verify();
    }

    @Test
    @DisplayName("驗證 FileMetadataDTO - 檔案名稱含非法字元 - 拋出 INVALID_FILE_NAME")
    void testValidateFileMetadataDTO_InvalidFileName_ThrowsValidationException() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("doc/ument.txt");
        fileMetadataDTO.setFileSize(1024L);
        User user = new User();
        user.setStorageLimit(1048576L);
        user.setUsedStorage(0L);


        Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(fileMetadataDTO, user);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.INVALID_FILE_NAME)
                .verify();
    }

    @Test
    @DisplayName("驗證 FileMetadataDTO - 檔案名稱過長 - 拋出 NAME_TOO_LONG")
    void testValidateFileMetadataDTO_FileNameTooLong_ThrowsValidationException() {
        String longFileName = "a".repeat(201);
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename(longFileName + ".txt");
        fileMetadataDTO.setFileSize(1024L);
        User user = new User();
        user.setStorageLimit(1048576L);
        user.setUsedStorage(0L);

        Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(fileMetadataDTO, user);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NAME_TOO_LONG)
                .verify();
    }

    @Test
    @DisplayName("驗證 FileMetadataDTO - 檔案大小超出使用者空間限制 - 拋出 STORAGE_LIMIT_EXCEEDED")
    void testValidateFileMetadataDTO_FileSizeExceedsStorageLimit_ThrowsValidationException() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("document.txt");
        fileMetadataDTO.setFileSize(2097152L);
        User user = new User();
        user.setStorageLimit(1048576L);
        user.setUsedStorage(0L);


        Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(fileMetadataDTO, user);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.STORAGE_LIMIT_EXCEEDED)
                .verify();
    }

    @Test
    @DisplayName("驗證 FileMetadataDTO - 檔案名稱長度剛好為上限 - 驗證成功")
    void testValidateFileMetadataDTO_BorderlineFileNameLength_Success() {
        String borderlineFileName = "a".repeat(196);
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename(borderlineFileName + ".txt");
        fileMetadataDTO.setFileSize(1024L);
        User user = new User();
        user.setStorageLimit(1048576L);
        user.setUsedStorage(0L);

        Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(fileMetadataDTO, user);

        StepVerifier.create(result).verifyComplete();
    }


    @Test
    @DisplayName("驗證 EditFileDTO - 編輯檔案中繼資料且檔案名稱合法 - 驗證成功")
    void testValidateEditFileDTO_EditMetadataValidFile_Success() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.EDIT_METADATA);
        fileEditDTO.setFilename("newname.txt");

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("驗證 EditFileDTO - 編輯資料夾中繼資料且名稱合法 - 驗證成功")
    void testValidateEditFileDTO_EditMetadataValidFolder_Success() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.EDIT_METADATA);
        fileEditDTO.setFilename("newfolder");

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, true);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("驗證 EditFileDTO - DTO 為 null - 拋出 NULL_DTO")
    void testValidateEditFileDTO_DTONull_ThrowsValidationException() {
        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(null, false);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                .verify();
    }

    @Test
    @DisplayName("驗證 EditFileDTO - 編輯類型為 null - 拋出 REQUEST_IS_INVALID")
    void testValidateEditFileDTO_EditTypeNull_ThrowsValidationException() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(null);

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.REQUEST_IS_INVALID)
                .verify();
    }

    @Test
    @DisplayName("驗證 EditFileDTO - 檔案名稱中含非法字元 - 拋出 INVALID_FILE_NAME")
    void testValidateEditFileDTO_EditMetadataInvalidFileName_ThrowsValidationException() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.EDIT_METADATA);
        fileEditDTO.setFilename("new/name.txt");

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.INVALID_FILE_NAME)
                .verify();
    }

    @Test
    @DisplayName("驗證 EditFileDTO - 內容超過長度限制 - 拋出 FIELD_LENGTH_TOO_LONG")
    void testValidateEditFileDTO_EditContentTooLong_ThrowsValidationException() {
        EditorContentDTO editorContentDTO = new EditorContentDTO();
        EditorContentDTO.DeltaDTO deltaDTO = new EditorContentDTO.DeltaDTO();
        deltaDTO.setInsert("a".repeat((int) Math.pow(2, 20) + 1));
        editorContentDTO.setDelta(List.of(deltaDTO));


        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.EDIT_CONTENT);
        fileEditDTO.setContent(editorContentDTO);

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FIELD_LENGTH_TOO_LONG)
                .verify();
    }

    @Test
    @DisplayName("驗證 EditFileDTO - 建立歷史記錄時內容與備註合法 - 驗證成功")
    void testValidateEditFileDTO_BuildHistoryRecordValid_Success() {
        EditorContentDTO editorContentDTO = new EditorContentDTO();
        EditorContentDTO.DeltaDTO deltaDTO = new EditorContentDTO.DeltaDTO();
        deltaDTO.setInsert("a".repeat((int) Math.pow(2, 10) + 1));
        editorContentDTO.setDelta(List.of(deltaDTO));

        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.BUILD_HISTORY_RECORD);
        fileEditDTO.setContent(editorContentDTO);
        fileEditDTO.setNote("note");

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("驗證 EditFileDTO - 建立歷史記錄備註過長 - 拋出 FIELD_LENGTH_TOO_LONG")
    void testValidateEditFileDTO_BuildHistoryRecordNoteTooLong_ThrowsValidationException() {
        EditorContentDTO editorContentDTO = new EditorContentDTO();
        EditorContentDTO.DeltaDTO deltaDTO = new EditorContentDTO.DeltaDTO();
        deltaDTO.setInsert("a".repeat((int) Math.pow(2, 10) + 1));
        editorContentDTO.setDelta(List.of(deltaDTO));

        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.BUILD_HISTORY_RECORD);
        fileEditDTO.setContent(editorContentDTO);
        fileEditDTO.setNote("a".repeat(1001));
        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FIELD_LENGTH_TOO_LONG)
                .verify();
    }

    @Test
    @DisplayName("驗證 EditFileDTO - 執行回復歷史紀錄 - 驗證成功")
    void testValidateEditFileDTO_RevertHistoryRecord_Success() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.REVERT_HISTORY_RECORD);

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("驗證 EditFileDTO - 編輯內容剛好為長度上限 - 驗證成功")
    void testValidateEditFileDTO_BorderlineContentLength_Success() {
        EditorContentDTO editorContentDTO = new EditorContentDTO();
        EditorContentDTO.DeltaDTO deltaDTO = new EditorContentDTO.DeltaDTO();
        deltaDTO.setInsert("a".repeat((int) Math.pow(2, 10)));

        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.EDIT_CONTENT);
        fileEditDTO.setContent(editorContentDTO);

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("驗證檔案名稱 - 包含非法字符 - 拋出 INVALID_FILE_NAME")
    void testValidateFileName_InvalidCharacters_ThrowsValidationException() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("file/name");
        fileMetadataDTO.setFileSize(1024L);

        User user = new User();
        user.setStorageLimit(10485760L);
        user.setUsedStorage(0L);

        Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(fileMetadataDTO, user);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && 
                        ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.INVALID_FILE_NAME)
                .verify();
    }

    @Test
    @DisplayName("驗證存儲限制 - 超出用戶存儲限制 - 拋出 STORAGE_LIMIT_EXCEEDED")
    void testValidateStorageLimit_ExceedsLimit_ThrowsValidationException() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("largefile.txt");
        fileMetadataDTO.setFileSize(5242880L); // 5MB

        User user = new User();
        user.setStorageLimit(1048576L); // 1MB limit
        user.setUsedStorage(0L);

        Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(fileMetadataDTO, user);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && 
                        ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.STORAGE_LIMIT_EXCEEDED)
                .verify();
    }

    @Test
    @DisplayName("驗證密碼強度 - 密碼太短 - 拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
    void testValidatePasswordStrength_TooShort_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser123"); // 有效的字母數字用戶名
        registerDTO.setPassword("123"); // 太短
        registerDTO.setConfirmPassword("123");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("testuser123")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && 
                        ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH)
                .verify();
    }

    @Test
    @DisplayName("驗證密碼強度 - 密碼是回文 - 拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
    void testValidatePasswordStrength_Palindrome_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser123"); // 有效的字母數字用戶名
        registerDTO.setPassword("Password123321drowssaP"); // 回文
        registerDTO.setConfirmPassword("Password123321drowssaP");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("testuser123")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && 
                        ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH)
                .verify();
    }

    @Test
    @DisplayName("驗證密碼強度 - 密碼沒有大小寫字母和數字 - 拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
    void testValidatePasswordStrength_NoMixedCase_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser123"); // 有效的字母數字用戶名
        registerDTO.setPassword("password"); // 沒有大寫字母和數字
        registerDTO.setConfirmPassword("password");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("testuser123")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && 
                        ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH)
                .verify();
    }

    @Test
    @DisplayName("驗證用戶名稱 - 包含非字母數字字符 - 拋出 USERNAME_INVALID")
    void testValidateUsername_NonAlphanumeric_ThrowsValidationException() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("user-name"); // 包含連字符
        registerDTO.setPassword("Password123");
        registerDTO.setConfirmPassword("Password123");
        registerDTO.setEmail("test@example.com");

        when(mockUserRepository.findByUsername("user-name")).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());

        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && 
                        ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.USERNAME_INVALID)
                .verify();
    }

    @Test
    @DisplayName("驗證文件過濾器 - FileFilterDTO 為 null - 拋出 NULL_DTO")
    void testValidateFileFilterDTO_Null_ThrowsValidationException() {
        Mono<Void> result = validationServiceImplUnderTest.validateFileFilterDTO(null);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && 
                        ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                .verify();
    }

    @Test
    @DisplayName("驗證 AuthRequestDTO - DTO 為 null - 拋出 NULL_DTO")
    void testValidateAuthRequestDTO_Null_ThrowsValidationException() {
        Mono<Void> result = validationServiceImplUnderTest.validateAuthRequestDTO(null);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && 
                        ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                .verify();
    }

    @Test
    @DisplayName("驗證資料夾名稱 - 包含非法字符的資料夾 - 拋出 INVALID_FOLDER_NAME")
    void testValidateFolderName_InvalidCharacters_ThrowsValidationException() {
        FileEditDTO folderEditDTO = new FileEditDTO();
        folderEditDTO.setFilename("folder\\name"); // 包含反斜杠
        folderEditDTO.setEditType(EditTypeEnum.EDIT_METADATA);

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(folderEditDTO, true);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && 
                        ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.INVALID_FOLDER_NAME)
                .verify();
    }

    @Test
    @DisplayName("驗證 EditFileDTO - 內容為 null 的編輯操作 - 驗證成功")
    void testValidateEditFileDTO_NullContentForEdit_Success() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.EDIT_CONTENT);
        fileEditDTO.setContent(null); // 內容可以為null

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("驗證存儲限制 - 邊界情況：使用空間加新檔案剛好等於限制 - 驗證成功")
    void testValidateStorageLimit_ExactLimit_Success() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("exactfile.txt");
        fileMetadataDTO.setFileSize(1024L); // 1KB

        User user = new User();
        user.setStorageLimit(2048L); // 2KB limit
        user.setUsedStorage(1024L); // 已使用1KB

        Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(fileMetadataDTO, user);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("驗證空的檔案名稱 - 空字符串 - 拋出 INVALID_FILE_NAME")
    void testValidateFileName_EmptyString_ThrowsValidationException() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename(""); // 空字符串
        fileMetadataDTO.setFileSize(1024L);

        User user = new User();
        user.setStorageLimit(10485760L);
        user.setUsedStorage(0L);

        Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(fileMetadataDTO, user);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && 
                        ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.INVALID_FILE_NAME)
                .verify();
    }
}
