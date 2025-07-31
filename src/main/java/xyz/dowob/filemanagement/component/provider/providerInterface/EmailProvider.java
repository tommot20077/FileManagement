package xyz.dowob.filemanagement.component.provider.providerInterface;

import reactor.core.publisher.Mono;

/**
 * 非阻塞郵件發送提供者介面，提供網絡郵件發送的反應式機制。
 *
 * <p>本介面設計依據 WebFlux 的非阻塞編程模型，提供一個高效能、可擴展的郵件發送機制。
 * 確保郵件發送過程不會阻團應用程式的執行線程。</p>
 *
 * <p>主要特性：
 * <ul>
 *   <li>支援非同步郵件發送</li>
 *   <li>動態地控制發送對象和內容</li>
 *   <li>可以轉換為不同的郵件提供者實現</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
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
