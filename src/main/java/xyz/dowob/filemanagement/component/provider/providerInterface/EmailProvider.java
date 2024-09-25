package xyz.dowob.filemanagement.component.provider.providerInterface;

import reactor.core.publisher.Mono;

/**
 * 郵件提供者接口，用於定義郵件提供者的方法
 *
 * @author yuan
 * @program File-Management
 * @ClassName EmailProviderImpl
 * @description
 * @create 2024-09-20 13:16
 * @Version 1.0
 **/
public interface EmailProvider {
    /**
     * 發送郵件
     *
     * @param sendToEmail 收件人郵箱
     * @param subject     郵件主題
     * @param content     郵件內容
     */
    Mono<Void> sendEmail(String sendToEmail, String subject, String content);

}
