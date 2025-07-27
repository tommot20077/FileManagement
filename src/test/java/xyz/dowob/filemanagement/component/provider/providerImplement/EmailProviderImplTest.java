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
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.exception.ProcessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailProvider 邏輯處理測試")
class EmailProviderImplTest {

    @Mock
    private JavaMailSender mockJavaMailSender;
    @Mock
    private GlobalProperties globalProperties;
    @Mock
    private GlobalProperties.Email mockEmail;

    private EmailProviderImpl emailProviderImplUnderTest;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        emailProviderImplUnderTest = new EmailProviderImpl(mockJavaMailSender, globalProperties);
        mimeMessage = new MimeMessage((Session) null);
    }

    @Test
    @DisplayName("成功發送郵件 - 返回 Mono.empty")
    void sendEmail_validInput_sendsEmailSuccessfully() {
        String sendToEmail = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        String fromEmail = "sender@example.com";

        when(globalProperties.getEmail()).thenReturn(mockEmail);
        when(mockEmail.getMailSender()).thenReturn(fromEmail);
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

        when(globalProperties.getEmail()).thenReturn(mockEmail);
        when(mockEmail.getMailSender()).thenReturn(fromEmail);
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

        when(globalProperties.getEmail()).thenReturn(mockEmail);
        when(mockEmail.getMailSender()).thenReturn(null);
        when(mockJavaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        var result = emailProviderImplUnderTest.sendEmail(sendToEmail, subject, content);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ProcessException && ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.SEND_MAIL_FAILED && (throwable.getCause() instanceof MessagingException || throwable.getCause() instanceof IllegalArgumentException || throwable.getCause() instanceof NullPointerException))
                .verify();
        verify(mockJavaMailSender, never()).send(any(MimeMessage.class));
    }
}
