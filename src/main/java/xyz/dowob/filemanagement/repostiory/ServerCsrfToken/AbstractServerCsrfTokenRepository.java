package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import xyz.dowob.filemanagement.config.properties.SecurityProperties;

import java.time.Duration;

/**
 * 自定義的 CSRF Token 存儲庫，用於CSRF 的相關操作
 * 主要實現 CustomServerCsrfTokenRepository 接口
 * 用於生成、保存、加載、刪除 CSRF Token
 * 這裡省略了保存 Token 的操作，因為我們在生成 Token 的時候就已經保存，所以這裡只需要生成和加載即可
 * 這邊的屬性都可以在配置文件中配置 {@link SecurityProperties}
 *
 * @author yuan
 * @program FileManagement
 * @ClassName AbstractServerCsrfTokenRepository
 * @create 2025/3/6
 * @Version 1.0
 **/
public abstract class AbstractServerCsrfTokenRepository implements CustomServerCsrfTokenRepository {
    /**
     * CSRF Token 的 Header 名稱
     */
    protected final String csrfTokenHeader;

    /**
     * CSRF Token 的參數名稱
     */
    protected final String csrfTokenParameter;

    /**
     * CSRF Token 過期時間
     */
    protected final Duration expireTime;

    public AbstractServerCsrfTokenRepository(SecurityProperties securityProperties) {
        csrfTokenHeader = securityProperties.getCsrf().getHeaderName();
        csrfTokenParameter = securityProperties.getCsrf().getParameterName();
        expireTime = securityProperties.getCsrf().getExpiration();
    }
}
