package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import xyz.dowob.filemanagement.config.properties.SecurityProperties;

import java.time.Duration;

/**
 * CSRF Token 存放庫的抽象基礎類別，提供 CSRF 防護功能的核心實作基礎。
 * <p>
 * 此抽象類別實作了 {@link CustomServerCsrfTokenRepository} 介面，
 * 定義了 CSRF Token 的基本屬性和設定，為具體的實作類別提供統一的基礎架構。
 * 支援可設定的 Token 參數名稱、標頭名稱和過期時間等安全性設定。
 * </p>
 * <p>
 * 子類別需要實作具體的 Token 生成、載入、儲存和刪除邏輯，
 * 可根據不同的儲存策略（如本地快取、Redis 等）提供不同的實作方式。
 * 所有安全性相關的設定參數均可透過 {@link SecurityProperties} 進行設定。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see CustomServerCsrfTokenRepository
 * @see SecurityProperties
 */
public abstract class AbstractServerCsrfTokenRepository implements CustomServerCsrfTokenRepository {
    /**
     * CSRF Token 的 HTTP 標頭名稱。
     * <p>
     * 定義客戶端在 HTTP 請求中應使用的標頭名稱來傳遞 CSRF Token，
     * 通常設定為 "X-CSRF-TOKEN" 或其他自定義名稱。
     * </p>
     */
    protected final String csrfTokenHeader;

    /**
     * CSRF Token 的參數名稱。
     * <p>
     * 定義在表單提交或 URL 參數中使用的 CSRF Token 參數名稱，
     * 作為 HTTP 標頭的替代傳遞方式。
     * </p>
     */
    protected final String csrfTokenParameter;

    /**
     * CSRF Token 的過期時間。
     * <p>
     * 設定 CSRF Token 的有效期限，超過此時間的 Token 將被視為無效。
     * 較短的過期時間提供更高的安全性，但可能影響使用者體驗。
     * </p>
     */
    protected final Duration expireTime;

    /**
     * 建構子，根據安全性設定初始化 CSRF Token 相關參數。
     * <p>
     * 從安全性設定中讀取 CSRF Token 的標頭名稱、參數名稱和過期時間，
     * 為子類別提供統一的設定基礎。
     * </p>
     *
     * @param securityProperties 安全性設定屬性，包含 CSRF 相關設定
     */
    public AbstractServerCsrfTokenRepository(SecurityProperties securityProperties) {
        csrfTokenHeader = securityProperties.getCsrf().getHeaderName();
        csrfTokenParameter = securityProperties.getCsrf().getParameterName();
        expireTime = securityProperties.getCsrf().getExpiration();
    }
}
