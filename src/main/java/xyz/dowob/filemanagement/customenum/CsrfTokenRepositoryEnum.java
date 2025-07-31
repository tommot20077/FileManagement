package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CSRF 標記儲存管理枚舉，提供不同的安全標記儲存策略。
 *
 * <p>此枚舉類型提供一個高度可擴展的 CSRF 防護標記儲存方案，可以根據不同的應用場景選擇適合的儲存策略。</p>
 *
 * <p>CSRF（跨站請求偽造攻擊）是一種常見的 Web 安全威脅，通過控制標記來防止未經授權的請求。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum CsrfTokenRepositoryEnum {
    /**
     * 本地儲存，適用於小型應用程式或單機環境，利用內存作為 CSRF 標記的儲存介質。
     */
    LOCAL,

    /**
     * Redis 儲存，適用於分散式系統和高並發環境，提供共享式 CSRF 標記管理。
     */
    REDIS
}
