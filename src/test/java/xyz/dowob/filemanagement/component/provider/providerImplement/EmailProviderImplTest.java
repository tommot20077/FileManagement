package xyz.dowob.filemanagement.component.provider.providerImplement;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.config.properties.MailProperties;
import xyz.dowob.filemanagement.exception.ProcessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EmailProviderImpl 測試類別。
 *
 * <p>測試 EmailProviderImpl 的郵件發送功能，包括：
 * <ul>
 * <li>正常郵件發送操作</li>
 * <li>JavaMailSender 發送失敗處理</li>
 * <li>MimeMessage 創建失敗處理</li>
 * <li>寄件人資訊異常處理</li>
 * <li>各種郵件參數的驗證</li>
 * </ul>
 *
 * <p>測試涵蓋郵件服務的所有核心操作，包含正常發送流程、
 * 各種異常情況及錯誤處理。透過模擬 JavaMailSender 驗證郵件發送功能的健壯性。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailProvider 邏輯處理測試")
class EmailProviderImplTest {

    @Mock
    private JavaMailSender mockJavaMailSender;

    @Mock
    private MailProperties customMailProperties;

    @Mock
    private org.springframework.boot.autoconfigure.mail.MailProperties springMailProperties;

    @Mock
    private MailProperties.MailSender mockEmailSender;

    @Mock
    private MailProperties.OAuth mockOAuth;

    private EmailProviderImpl emailProviderImplUnderTest;

    private MimeMessage mimeMessage;


    @BeforeEach
    void setUp() {
        emailProviderImplUnderTest = new EmailProviderImpl(mockJavaMailSender, customMailProperties, springMailProperties);
        mimeMessage = new MimeMessage((Session) null);
        
        // 設置 OAuth Mock 預設行為
        when(customMailProperties.getOauth()).thenReturn(mockOAuth);
        when(mockOAuth.isEnabled()).thenReturn(false);  // 預設不啟用 OAuth
    }


    @Test
    @DisplayName("成功發送郵件 - 返回 Mono.empty")
    void sendEmail_validInput_sendsEmailSuccessfully() {
        String sendToEmail = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        String fromEmail = "sender@example.com";

        when(customMailProperties.getMailSender()).thenReturn(mockEmailSender);
        when(mockEmailSender.getMailSender()).thenReturn(fromEmail);
        when(mockJavaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        var result = emailProviderImplUnderTest.sendEmail(sendToEmail, subject, content);

        StepVerifier.create(result).verifyComplete();

        verify(mockJavaMailSender).send(any(MimeMessage.class));
    }


    @Test
    @DisplayName("發送郵件時 JavaMailSender 拋出 MailException - 拋出 ProcessException")
    void sendEmail_whenSendThrowsMailException_throwsProcessException() {
        String sendToEmail = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        String fromEmail = "sender@example.com";

        when(customMailProperties.getMailSender()).thenReturn(mockEmailSender);
        when(mockEmailSender.getMailSender()).thenReturn(fromEmail);
        when(mockJavaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailException("Simulated send failure") {
        }).when(mockJavaMailSender).send(any(MimeMessage.class));

        var result = emailProviderImplUnderTest.sendEmail(sendToEmail, subject, content);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ProcessException && ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.SEND_MAIL_FAILED && throwable.getCause() instanceof MailException)
                .verify();
        verify(mockJavaMailSender).send(any(MimeMessage.class));
    }


    @Test
    @DisplayName("建立 MimeMessage 失敗 - 拋出 ProcessException")
    void sendEmail_whenCreateMimeMessageFails_throwsProcessException() {
        String sendToEmail = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";

        when(mockJavaMailSender.createMimeMessage()).thenThrow(new RuntimeException("Simulated MimeMessage creation failure"));

        var result = emailProviderImplUnderTest.sendEmail(sendToEmail, subject, content);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ProcessException && ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.SEND_MAIL_FAILED && throwable.getCause() instanceof RuntimeException)
                .verify();
        verify(mockJavaMailSender, never()).send(any(MimeMessage.class));
    }


    @Test
    @DisplayName("設定郵件寄件人時因用戶名為空導致失敗 - 拋出 ProcessException")
    void sendEmail_whenSetFromFailsDueToNullUsername_throwsProcessException() {
        String sendToEmail = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";

        when(customMailProperties.getMailSender()).thenReturn(mockEmailSender);
        when(mockEmailSender.getMailSender()).thenReturn(null);
        when(mockJavaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        var result = emailProviderImplUnderTest.sendEmail(sendToEmail, subject, content);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ProcessException && ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.SEND_MAIL_FAILED && (throwable.getCause() instanceof MessagingException || throwable.getCause() instanceof IllegalArgumentException || throwable.getCause() instanceof NullPointerException))
                .verify();
        verify(mockJavaMailSender, never()).send(any(MimeMessage.class));
    }
}
