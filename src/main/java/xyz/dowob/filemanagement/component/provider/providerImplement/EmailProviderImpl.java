package xyz.dowob.filemanagement.component.provider.providerImplement;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.exception.ProcessException;

/**
 * 電子郵件提供者實現類，可以發送電子郵件
 * 實現了 EmailProvider 接口
 * 這裡使用了 Spring Boot 提供的 JavaMailSender 來發送郵件
 *
 * @author yuan
 * @program File-Management
 * @ClassName EmailProviderImpl
 * @description
 * @create 2024-09-20 00:28
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"spring.mail.username", "spring.mail.password", "global.email.mail-sender"})
public class EmailProviderImpl implements xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider {
    /**
     * JavaMailSender Java 郵件發送器
     */
    private final JavaMailSender javaMailSender;

    /**
     * 全局配置，這裡配置發送郵件的信箱位置
     */
    private final GlobalProperties globalProperties;

    /**
     * 發送郵件
     *
     * @param sendToEmail 收件人郵箱
     * @param subject     郵件主題
     * @param content     郵件內容
     */
    @Override
    @HideSensitive
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<Void> sendEmail(String sendToEmail, String subject, String content) {
        return Mono.defer(() -> {
            try {
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(globalProperties.getEmail().getMailSender(), "帳號安全管理組");
                helper.setTo(sendToEmail);
                helper.setSubject(subject);
                helper.setText(content, false);

                javaMailSender.send(mimeMessage);
                return Mono.empty();
            } catch (Exception e) {
                return Mono.error(new ProcessException(ProcessException.ErrorCode.SEND_MAIL_FAILED, e));
            }
        });
    }
}
