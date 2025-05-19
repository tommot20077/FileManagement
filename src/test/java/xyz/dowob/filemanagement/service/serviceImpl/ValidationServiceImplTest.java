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
@DisplayName("ValidationServiceImpl 處理驗證測試")
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
    void testValidateRegisterDTO_DTONull_ThrowsValidationException() {
        Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(null);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                .verify();
    }


    @Test
    @DisplayName("當密碼與確認密碼不一致時應拋出 CONFIRM_PASSWORD_NOT_MATCH")
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
    @DisplayName("當使用者名稱已存在時應拋出 USERNAME_INVALID")
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
    @DisplayName("當使用者名稱包含非法字元時應拋出 USERNAME_INVALID")
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
    @DisplayName("當信箱已存在時應拋出 EMAIL_ALREADY_EXISTS")
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
    @DisplayName("當密碼為回文時應拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
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
    @DisplayName("當密碼缺少大寫字母時應拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
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
    @DisplayName("當輸入合法時驗證成功")
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
    @DisplayName("當查詢使用者名稱時發生錯誤應拋出例外")
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
    @DisplayName("當查詢信箱時發生錯誤應拋出例外")
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
    @DisplayName("當使用者名稱為空時應拋出 BLANK_FIELD")
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
    @DisplayName("當密碼為空時應拋出 BLANK_FIELD")
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
    @DisplayName("當重設密碼輸入合法時驗證成功")
    void testValidateResetPasswordDTO_ValidInput_Success() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setNewPassword("NewPass123");
        resetPasswordDTO.setConfirmPassword("NewPass123");

        Mono<Void> result = validationServiceImplUnderTest.validateResetPasswordDTO(resetPasswordDTO);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("當 ResetPasswordDTO 為 null 時應拋出 NULL_DTO")
    void testValidateResetPasswordDTO_DTONull_ThrowsValidationException() {
        Mono<Void> result = validationServiceImplUnderTest.validateResetPasswordDTO(null);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                .verify();
    }

    @Test
    @DisplayName("當新密碼與確認密碼不一致時應拋出 CONFIRM_PASSWORD_NOT_MATCH")
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
    @DisplayName("當新密碼為回文時應拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
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
    @DisplayName("當新密碼缺少大寫字母時應拋出 PASSWORD_IS_NOT_STRONG_ENOUGH")
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
    @DisplayName("當新密碼為空時應拋出 BLANK_FIELD")
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
    @DisplayName("當上傳檔案資料合法時驗證成功")
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
    @DisplayName("當 FileMetadataDTO 為 null 時應拋出 NULL_DTO")
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
    @DisplayName("當檔案名稱含非法字元時應拋出 INVALID_FILE_NAME")
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
    @DisplayName("當檔案名稱過長時應拋出 NAME_TOO_LONG")
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
    @DisplayName("當檔案大小超出使用者空間限制時應拋出 STORAGE_LIMIT_EXCEEDED")
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
    @DisplayName("當檔案名稱長度剛好為上限時驗證成功")
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
    @DisplayName("當編輯檔案中繼資料且檔案名稱合法時驗證成功")
    void testValidateEditFileDTO_EditMetadataValidFile_Success() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.EDIT_METADATA);
        fileEditDTO.setFilename("newname.txt");

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("當編輯資料夾中繼資料且名稱合法時驗證成功")
    void testValidateEditFileDTO_EditMetadataValidFolder_Success() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.EDIT_METADATA);
        fileEditDTO.setFilename("newfolder");

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, true);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("當 FileEditDTO 為 null 時應拋出 NULL_DTO")
    void testValidateEditFileDTO_DTONull_ThrowsValidationException() {
        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(null, false);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                .verify();
    }

    @Test
    @DisplayName("當編輯類型為 null 時應拋出 REQUEST_IS_INVALID")
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
    @DisplayName("當檔案名稱中含非法字元時應拋出 INVALID_FILE_NAME")
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
    @DisplayName("當內容超過長度限制時應拋出 FIELD_LENGTH_TOO_LONG")
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
    @DisplayName("當建立歷史記錄時內容與備註合法應驗證成功")
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
    @DisplayName("當建立歷史記錄備註過長時應拋出 FIELD_LENGTH_TOO_LONG")
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
    @DisplayName("當執行回復歷史紀錄時驗證成功")
    void testValidateEditFileDTO_RevertHistoryRecord_Success() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setEditType(EditTypeEnum.REVERT_HISTORY_RECORD);

        Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("當編輯內容剛好為長度上限時驗證成功")
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
}

