package xyz.dowob.filemanagement.component.provider.providerImplement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.TokenRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetTokenProvider 邏輯處理測試")
class PasswordResetTokenProviderImplTest {

    @Mock
    private TokenRepository mockTokenRepository;

    @Mock
    private SecurityProperties mockSecurityProperties;

    @Mock
    private SecurityProperties.resetPasswordToken mockResetPasswordToken;

    private PasswordResetTokenProviderImpl passwordResetTokenProviderImplUnderTest;


    @BeforeEach
    void setUp() {
        passwordResetTokenProviderImplUnderTest = new PasswordResetTokenProviderImpl(mockTokenRepository, mockSecurityProperties);
    }


    @Test
    @DisplayName("生成重置密碼憑證 - 用戶無現有憑證 - 成功生成並保存新憑證")
    void generateToken_noExistingToken_createsAndSavesNewToken() {
        User user = new User();
        user.setId(1L);

        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(mockResetPasswordToken);
        when(mockTokenRepository.findByUserId(anyLong())).thenReturn(Mono.empty());
        when(mockTokenRepository.save(any(Token.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(mockResetPasswordToken.getLength()).thenReturn(6);
        when(mockResetPasswordToken.getExpiration()).thenReturn(Duration.ofMinutes(10));

        StepVerifier
                .create(passwordResetTokenProviderImplUnderTest.generateToken(user))
                .expectNextMatches(token -> token.matches("\\d{6}"))
                .verifyComplete();

        verify(mockTokenRepository, times(1)).findByUserId(user.getId());
        verify(mockTokenRepository, times(1)).save(any(Token.class));
    }


    @Test
    @DisplayName("生成重置密碼憑證 - 用戶有現有憑證 - 成功更新並保存現有憑證")
    void generateToken_existingToken_updatesAndSavesExistingToken() {
        User user = new User();
        user.setId(1L);
        Token existingToken = new Token();
        existingToken.setUserId(user.getId());
        existingToken.setJwtTokenVersion("oldVersion");

        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(mockResetPasswordToken);
        when(mockTokenRepository.findByUserId(anyLong())).thenReturn(Mono.just(existingToken));
        when(mockTokenRepository.save(any(Token.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(mockResetPasswordToken.getLength()).thenReturn(6);
        when(mockResetPasswordToken.getExpiration()).thenReturn(Duration.ofMinutes(10));

        StepVerifier
                .create(passwordResetTokenProviderImplUnderTest.generateToken(user))
                .expectNextMatches(token -> token.matches("\\d{6}"))
                .verifyComplete();

        verify(mockTokenRepository, times(1)).findByUserId(user.getId());
        verify(mockTokenRepository, times(1)).save(argThat(token -> Objects.equals(token.getUserId(),
                                                                                   user.getId()
        ) && token.getResetVerificationCode() != null && token.getResetVerificationCodeExpireTime() != null && token
                .getJwtTokenVersion()
                .equals("oldVersion")));
    }


    @Test
    @DisplayName("驗證憑證 - 憑證有效 - 返回用戶ID")
    void validateToken_validToken_returnsUserId() {
        long userId = 1L;
        String validCode = "123456";
        Token tokenEntity = new Token();
        tokenEntity.setUserId(userId);
        tokenEntity.setResetVerificationCode(validCode);
        tokenEntity.setResetVerificationCodeExpireTime(LocalDateTime.now().plusMinutes(5));

        when(mockTokenRepository.findByUserId(anyLong())).thenReturn(Mono.just(tokenEntity));

        StepVerifier.create(passwordResetTokenProviderImplUnderTest.validateToken(validCode, userId)).expectNext(userId).verifyComplete();

        verify(mockTokenRepository, times(1)).findByUserId(userId);
    }


    @Test
    @DisplayName("驗證憑證 - 憑證不存在 - 拋出 VERIFICATION_CODE_ERROR 錯誤")
    void validateToken_tokenNotFound_throwsVerificationCodeError() {
        long userId = 1L;
        String code = "123456";

        when(mockTokenRepository.findByUserId(anyLong())).thenReturn(Mono.empty());

        StepVerifier
                .create(passwordResetTokenProviderImplUnderTest.validateToken(code, userId))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)
                .verify();

        verify(mockTokenRepository, times(1)).findByUserId(userId);
    }


    @Test
    @DisplayName("驗證憑證 - 憑證不匹配 - 拋出 VERIFICATION_CODE_ERROR 錯誤")
    void validateToken_tokenMismatch_throwsVerificationCodeError() {
        long userId = 1L;
        String providedCode = "123456";
        String storedCode = "654321";
        Token tokenEntity = new Token();
        tokenEntity.setUserId(userId);
        tokenEntity.setResetVerificationCode(storedCode);
        tokenEntity.setResetVerificationCodeExpireTime(LocalDateTime.now().plusMinutes(5));

        when(mockTokenRepository.findByUserId(anyLong())).thenReturn(Mono.just(tokenEntity));

        StepVerifier
                .create(passwordResetTokenProviderImplUnderTest.validateToken(providedCode, userId))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)
                .verify();

        verify(mockTokenRepository, times(1)).findByUserId(userId);
    }


    @Test
    @DisplayName("驗證憑證 - 憑證已過期 - 拋出 VERIFICATION_CODE_ERROR 錯誤")
    void validateToken_tokenExpired_throwsVerificationCodeError() {
        long userId = 1L;
        String code = "123456";
        Token tokenEntity = new Token();
        tokenEntity.setUserId(userId);
        tokenEntity.setResetVerificationCode(code);
        tokenEntity.setResetVerificationCodeExpireTime(LocalDateTime.now().minusMinutes(5));

        when(mockTokenRepository.findByUserId(anyLong())).thenReturn(Mono.just(tokenEntity));

        StepVerifier
                .create(passwordResetTokenProviderImplUnderTest.validateToken(code, userId))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)
                .verify();

        verify(mockTokenRepository, times(1)).findByUserId(userId);
    }


    @Test
    @DisplayName("撤銷憑證 - 憑證存在 - 成功撤銷並保存")
    void revokeToken_tokenExists_successfullyRevokesAndSaves() {
        long userId = 1L;
        Token tokenEntity = new Token();
        tokenEntity.setUserId(userId);
        tokenEntity.setResetVerificationCode("123456");
        tokenEntity.setResetVerificationCodeExpireTime(LocalDateTime.now().plusMinutes(5));

        when(mockTokenRepository.findByUserId(anyLong())).thenReturn(Mono.just(tokenEntity));
        when(mockTokenRepository.save(any(Token.class))).thenReturn(Mono.just(tokenEntity));

        StepVerifier.create(passwordResetTokenProviderImplUnderTest.revokeToken(userId)).verifyComplete();

        verify(mockTokenRepository, times(1)).findByUserId(userId);
        verify(mockTokenRepository, times(1)).save(argThat(token -> Objects.equals(token.getUserId(),
                                                                                   userId
        ) && token.getResetVerificationCode() == null && token.getResetVerificationCodeExpireTime() != null));
    }


    @Test
    @DisplayName("撤銷憑證 - 憑證不存在 - 完成操作不拋出錯誤")
    void revokeToken_tokenDoesNotExist_completesWithoutError() {
        long userId = 1L;

        when(mockTokenRepository.findByUserId(anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(passwordResetTokenProviderImplUnderTest.revokeToken(userId)).verifyComplete();

        verify(mockTokenRepository, times(1)).findByUserId(userId);
        verify(mockTokenRepository, never()).save(any(Token.class));
    }
}
