package xyz.dowob.filemanagement.service.serviceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.repostiory.UserRepository;

import java.util.Set;

import static org.mockito.Mockito.when;

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
    void testValidateRegisterDTO() {
        // Setup
        final RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("username");
        registerDTO.setPassword("password");
        registerDTO.setConfirmPassword("confirmPassword");
        registerDTO.setEmail("email");

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        // Configure UserRepository.findByEmail(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("password");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono1 = Mono.just(user1);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono1);

        // Run the test
        final Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        // Verify the results
    }

    @Test
    void testValidateRegisterDTO_UserRepositoryFindByUsernameReturnsNoItem() {
        // Setup
        final RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("username");
        registerDTO.setPassword("password");
        registerDTO.setConfirmPassword("confirmPassword");
        registerDTO.setEmail("email");

        when(mockUserRepository.findByUsername("username")).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        // Run the test
        final Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        // Verify the results
    }

    @Test
    void testValidateRegisterDTO_UserRepositoryFindByUsernameReturnsError() {
        // Setup
        final RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("username");
        registerDTO.setPassword("password");
        registerDTO.setConfirmPassword("confirmPassword");
        registerDTO.setEmail("email");

        // Configure UserRepository.findByUsername(...).
        final Mono<User> userMono = Mono.error(new Exception("message"));
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono1 = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono1);

        // Run the test
        final Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        // Verify the results
    }

    @Test
    void testValidateRegisterDTO_UserRepositoryFindByEmailReturnsNoItem() {
        // Setup
        final RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("username");
        registerDTO.setPassword("password");
        registerDTO.setConfirmPassword("confirmPassword");
        registerDTO.setEmail("email");

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockUserRepository.findByEmail("email")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        // Verify the results
    }

    @Test
    void testValidateRegisterDTO_UserRepositoryFindByEmailReturnsError() {
        // Setup
        final RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("username");
        registerDTO.setPassword("password");
        registerDTO.setConfirmPassword("confirmPassword");
        registerDTO.setEmail("email");

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        // Configure UserRepository.findByEmail(...).
        final Mono<User> userMono1 = Mono.error(new Exception("message"));
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono1);

        // Run the test
        final Mono<Void> result = validationServiceImplUnderTest.validateRegisterDTO(registerDTO);

        // Verify the results
    }

    @Test
    void testValidateResetPasswordDTO() {
        // Setup
        final ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        resetPasswordDTO.setVerificationCode("verificationCode");
        resetPasswordDTO.setNewPassword("newPassword");
        resetPasswordDTO.setConfirmPassword("confirmPassword");

        // Run the test
        final Mono<Void> result = validationServiceImplUnderTest.validateResetPasswordDTO(resetPasswordDTO);

        // Verify the results
    }

    @Test
    void testValidateFileMetadataDTO() {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("fileName");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUser(new User());

        // Run the test
        final Mono<Void> result = validationServiceImplUnderTest.validateFileMetadataDTO(fileMetadataDTO, new User());

        // Verify the results
    }

    @Test
    void testValidateEditFileDTO() {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFilename("fileName");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        // Run the test
        final Mono<Void> result = validationServiceImplUnderTest.validateEditFileDTO(fileEditDTO, false);

        // Verify the results
    }
}
