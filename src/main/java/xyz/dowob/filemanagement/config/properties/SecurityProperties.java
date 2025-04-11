package xyz.dowob.filemanagement.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum;

import java.util.List;

/**
 * 安全配置文件，用於配置安全相關的參數，在 application 中配置 security
 *
 * @author yuan
 * @program FileManagement
 * @ClassName SecurityConfig
 * @description
 * @create 2024-10-03 22:38
 * @Version 1.0
 **/

@ConfigurationProperties(prefix = "security")
@Configuration
@Data
public class SecurityProperties {

    /**
     * JWT 令牌
     */
    private JwtToken jwtToken = new JwtToken();

    /**
     * 重置密碼憑證配置
     */
    private resetPasswordToken resetPasswordToken = new resetPasswordToken();

    /**
     * Cookie 憑證配置
     */
    private Cookie cookie = new Cookie();

    /**
     * 跨域配置
     */
    private Cors cors = new Cors();

    /**
     * CSRF 憑證配置
     */
    private Csrf csrf = new Csrf();

    /**
     * HSTS 配置
     */
    private Hsts hsts = new Hsts();


    /**
     * 驗證 JWT 密鑰是否配置
     */
    @PostConstruct
    public void validateJwtSecret() {
        if (jwtToken.getSecret() == null || jwtToken.getSecret().trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 密鑰需要配置，請在 application 中配置 security.jwt-token.secret");
        }
    }


    /**
     * JWT 令牌設定
     */
    @Data
    public static class JwtToken {
        /**
         * JWT 密鑰
         */
        private String secret;

        /**
         * JWT 令牌過期時間，單位為分鐘，默認為 1440 分鐘
         */
        private int expiration = 1440;
    }


    /**
     * 重置密碼憑證配置
     */
    @Data
    public static class resetPasswordToken {
        /**
         * 重置密碼憑證的長度，默認為 6 位
         */
        private int length = 6;

        /**
         * 重置密碼憑證的過期時間，單位為分鐘，默認為 30 分鐘
         */
        private int expiration = 30;
    }

    @Data
    public static class Cookie {
        /**
         * Cookie 憑證是否加密，默認為 true
         */
        private boolean secure = true;

        /**
         * Cookie 憑證是否只能通過 HTTP 協議訪問，默認為 true
         */
        private boolean httpOnly = true;

        /**
         * Cookie 憑證的 SameSite 屬性，默認為 Lax
         */
        private String sameSite = "Lax";
    }

    @Data
    public static class Cors {
        /**
         * 跨域請求的允許來源，默認為空
         */
        private List<String> allowedOrigins = List.of();

        /**
         * 跨域請求的允許來源正則表達式，默認為空
         */
        private List<String> allowedOriginsPattern = List.of();

        /**
         * 跨域請求的允許暴露標頭，默認為空
         * Content-Disposition、Authorization、X-CSRF-TOKEN 會自動添加
         */
        private List<String> allowExposedHeaders = List.of();

        /**
         * 跨域請求的允許方法，默認為全部方法
         */
        private List<String> allowedMethods = List.of("*");

        /**
         * 跨域請求的允許標頭，默認為全部標頭
         */
        private List<String> allowedHeaders = List.of("*");

        /**
         * 跨域請求的暫存時間，默認為 3600 秒
         */
        private long maxAge = 3600;

        /**
         * 跨域請求是否允許憑證，默認為 true
         * 請注意如果這邊開啟的話，在設定跨域來源中就不可以使用 *，必須指定來源
         */
        private boolean allowCredentials = true;
    }

    @Data
    public static class Csrf {
        /**
         * CSRF 憑證的標頭名稱，用於前端ajax或fetch請求驗證用，默認為 X-CSRF-TOKEN
         */
        private String headerName = "X-Csrf-Token";

        /**
         * CSRF 憑證的參數名稱，用於前端表單驗證用，默認為 _csrf
         */
        private String parameterName = "_csrf";

        /**
         * CSRF 憑證的過期時間，默認為 5 分鐘
         * 此值不應該設置過長，否則會導致 CSRF 憑證的安全性降低
         */
        private long expiration = 5;

        /**
         * CSRF 憑證的存儲方式，默認為 LOCAL， 可選值為 LOCAL 和 REDIS
         */
        private CsrfTokenRepositoryEnum csrfTokenRepository = CsrfTokenRepositoryEnum.LOCAL;

        /**
         * 允許的參考來源的正則表達式，默認允許所有的 http和 https來源
         * 此配置值為管理CSRF TOKEN的獲取安全性，指定的 Referer 可以獲取 CSRF TOKEN
         * 如果有需要保護獲取CSRF token的請求，則需要設置這個參數成你前端的域名
         */
        private String allowRefererPatten = "^https?://.*$";
    }

    @Data
    public static class Hsts {
        /**
         * HSTS 的最大時間，默認為 1 年， 單位為分鐘
         */
        private long maxAge = 60 * 24 * 365;

        /**
         * 是否啟用 includeSubDomains，默認為 true
         */
        private boolean includeSubDomains = true;

        /**
         * 是否啟用 preload，默認為 true
         */
        private boolean preload = true;
    }


}
