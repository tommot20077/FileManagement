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
 * 基於 Spring Boot 的電子郵件發送提供者實現。
 *
 * <p>此類別實現了 {@link xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider} 介面，
 * 提供非同步且安全的電子郵件發送服務。通過 JavaMailSender 實現郵件傳送，支援 UTF-8 編碼，
 * 並提供基本的異常處理機制。</p>
 *
 * <p>特性：
 * <ul>
 *   <li>基於 Spring Boot JavaMailSender 實現</li>
 *   <li>支援非同步郵件發送（反應式編程）</li>
 *   <li>自動設定寄件人信箱</li>
 *   <li>安全性：使用 @HideSensitive 標記，防止敏感資訊洩露</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"spring.mail.username", "spring.mail.password", "global.email.mail-sender"})
public class EmailProviderImpl implements xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider {
    /**
     * Spring Boot 提供的 JavaMailSender，用於非同步且安全地傳送電子郵件。
     *
     * @see org.springframework.mail.javamail.JavaMailSender
     */
    private final JavaMailSender javaMailSender;

    /**
     * 全域設定屬性，用於取得並設定郵件發送的來源信箱。
     *
     * @see xyz.dowob.filemanagement.config.properties.GlobalProperties
     */
    private final GlobalProperties globalProperties;

    /**
     * 非同步發送電子郵件，支援 UTF-8 編碼。
     *
     * <p>此方法透過 Reactor 的 Mono 實現非阻塞郵件發送，並處理可能的發送異常。
     * 若發送失敗，將回傳 {@link xyz.dowob.filemanagement.exception.ProcessException}。</p>
     *
     * @param sendToEmail 收件人電子郵件地址
     * @param subject 郵件主旨
     * @param content 郵件內容（純文字格式）
     * @return {@link reactor.core.publisher.Mono<Void>} 表示郵件發送作業的非同步結果
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
