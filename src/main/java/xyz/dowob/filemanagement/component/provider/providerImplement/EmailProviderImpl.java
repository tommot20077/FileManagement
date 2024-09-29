package xyz.dowob.filemanagement.component.provider.providerImplement;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
public class EmailProviderImpl implements xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider {
    /**
     * JavaMailSender Java 郵件發送器
     */
    private final JavaMailSender javaMailSender;

    /**
     * 郵件配置，這裡使用了 Spring Boot 提供的 MailProperties
     * 1. 獲取郵件發送的郵箱
     */
    private final MailProperties mailProperties;


    /**
     * 發送郵件
     *
     * @param sendToEmail 收件人郵箱
     * @param subject     郵件主題
     * @param content     郵件內容
     */
    @Override
    public Mono<Void> sendEmail(String sendToEmail, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getUsername());
        message.setTo(sendToEmail);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
        return Mono.empty();
    }
}
