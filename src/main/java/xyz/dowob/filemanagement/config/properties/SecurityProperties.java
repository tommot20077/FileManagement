package xyz.dowob.filemanagement.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
@Data
@Configuration
@ConfigurationProperties(prefix = "security")
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
     * 遊客用戶配置
     */
    private GuestUser guestUser = new GuestUser();

    /**
     * 登錄配置
     */
    private Login login = new Login();

    /**
     * 路徑規則配置
     */
    private Paths paths = new Paths();


    /**
     * 進行初始化操作
     * 1. 檢查 JWT 密鑰是否配置
     * 2. 設定使用的路徑規則
     */
    @PostConstruct
    public void init() {
        if (jwtToken.getSecret() == null || jwtToken.getSecret().trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 密鑰需要配置，請在 application 中配置 security.jwt-token.secret");
        }

        if (this.paths != null) {
            this.paths.getEffectiveRules();
        }

    }


    /**
     * JWT 令牌設定
     */
    @Data
    public static class JwtToken {
        /**
         * JWT 加密密鑰
         */
        private String secret;

        /**
         * JWT 令牌過期時間，默認為 1 天
         */
        private Duration expiration = Duration.ofDays(1);

        /**
         * WebSocket 連線的 JWT 令牌前綴，默認為 jwt.
         */
        private String webSocketTokenPrefix = "jwt.";
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
         * 重置密碼憑證的過期時間，默認為 30 分鐘
         */
        private Duration expiration = Duration.ofMinutes(30);
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

        /**
         * Cookie 憑證的名稱，默認為 jwtToken
         */
        private String tokenName = "jwtToken";
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
         * 跨域請求預檢的暫存時間，默認為 1 小時
         */
        private Duration maxAge = Duration.ofHours(1);

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
        private Duration expiration = Duration.ofMinutes(5);

        /**
         * CSRF 憑證的存儲方式，默認為 LOCAL， 可選值為 LOCAL 和 REDIS
         */
        private CsrfTokenRepositoryEnum csrfTokenRepository = CsrfTokenRepositoryEnum.REDIS;

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
         * HSTS 的最大時間，默認為 365 天
         */
        private Duration maxAge = Duration.ofDays(365);

        /**
         * 是否啟用 includeSubDomains，默認為 true
         */
        private boolean includeSubDomains = true;

        /**
         * 是否啟用 preload，默認為 true
         */
        private boolean preload = true;
    }

    @Data
    public static class GuestUser {
        /**
         * 是否啟用訪客用戶，默認為 true
         */
        private boolean enable = true;
    }

    @Data
    public static class Login {
        /**
         * 登錄失敗的限流器提供者，默認為 redis
         * 可選值為 redis、local 和 none
         * none 表示不使用限流器
         */
        private LimiterProviderType limiterProvider = LimiterProviderType.local;

        /**
         * 登錄失敗的最大次數，默認為 5 次
         * 當用戶登錄失敗次數達到此值時，將會鎖定用戶，若用戶登錄成功，則會清除用戶的登錄失敗次數
         * 若設定小於等於0，則會產生 illegalArgumentException 錯誤
         */
        private int maxFailure = 5;

        /**
         * 登錄失敗的鎖定時間，默認為 30 分鐘
         * 若時間設定小於等於0，則會產生 illegalArgumentException 錯誤
         */
        private Duration lockTime = Duration.ofMinutes(30);

        /**
         * 限制器提供者的類型
         */
        private enum LimiterProviderType {
            /**
             * Redis 限制器提供者
             */
            redis,

            /**
             * 本地限制器提供者
             */
            local,

            /**
             * 不使用限制器
             */
            none
        }
    }


    /**
     * 路徑規則配置
     * 若有需要覆蓋的路徑規則，可以在 application.yml 中配置
     * 假設我要更改 WebSocket 的規則成所有人都可以使用Get方法則可以這樣配置:
     * security:
     *   paths:
     *     rules:
     *       websocket:
     *         pattern: /ws/**
     *         method: GET
     *         role: anonymous
     * 若要設定 userInfoApi 成只有進階用戶可以使用並允許所有方式的話則可以這樣配置:
     * security:
     *   paths:
     *     rules:
     *       user-info-api:
     *       pattern: /api/v1/user/info
     *       role: advanced_user
     * 需特別注意設定的鍵值名稱必須與預設的路徑規則名稱一致，否則不會生效
     */
    @Getter
    public static class Paths {
        /**
         * 預設路徑規則配置，此處不提供 setter 方法，因此在 YML 中即使設定了也不會覆蓋
         * key: 規則名稱
         * value: 規則配置
         */
        private final Map<String, PathRuleConfig> defaultRules = new LinkedHashMap<>();

        /**
         * 自定義路徑規則配置
         * key: 規則名稱
         * value: 規則配置
         */
        @Setter
        private Map<String, PathRuleConfig> rules = new LinkedHashMap<>();

        /**
         * 用於存儲最終生效的路徑規則配置，使用 transient 關鍵字避免序列化
         * key: 規則名稱
         * value: 規則配置
         */
        private transient Collection<PathRuleConfig> effectiveRulesCache;

        /**
         * Paths 的建構子，將會初始化預設路徑規則
         */
        public Paths() {
            initializeDefaultPathRules();
        }

        /**
         * 初始化預設路徑規則
         * 此方法會將預設路徑規則添加到 defaultRules 中
         */
        private void initializeDefaultPathRules() {
            Map<String, PathRuleConfig> dr = this.defaultRules;

            // 所有人都可以訪問 (ANONYMOUS)，即使沒有開啟訪客用戶功能
            dr.put("api-guest", new PathRuleConfig("/api/v1/guest/**", null, RoleEnum.ANONYMOUS));
            dr.put("web-guest", new PathRuleConfig("/web/v1/guest/**", null, RoleEnum.ANONYMOUS));
            dr.put("actuator-health", new PathRuleConfig("/actuator/health", null, RoleEnum.ANONYMOUS));
            dr.put("swagger-docs", new PathRuleConfig("/docs/**", null, RoleEnum.ANONYMOUS));
            dr.put("websocket", new PathRuleConfig("/ws/**", null, RoleEnum.ANONYMOUS));


            // 訪客用戶 (VISITOR)，當前訪客用戶功能開啟時，這些路徑規則會生效，若關閉則調用預設的 User 規則
            dr.put("user-info-api", new PathRuleConfig("/api/v1/user/info", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("user-info-web", new PathRuleConfig("/web/v1/user/info", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("folders-api-get-item", new PathRuleConfig("/api/v1/folders/*", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("folders-web-get-item", new PathRuleConfig("/web/v1/folders/*", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("folder-download-api", new PathRuleConfig("/api/v1/folders/*/download", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("folder-download-web", new PathRuleConfig("/web/v1/folders/*/download", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("files-api-get-item", new PathRuleConfig("/api/v1/files/*", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("files-web-get-item", new PathRuleConfig("/web/v1/files/*", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("file-info-api", new PathRuleConfig("/api/v1/files/*/info", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("file-info-web", new PathRuleConfig("/web/v1/files/*/info", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("api-v1-docs-get-item", new PathRuleConfig("/api/v1/docs/*", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("web-v1-docs-get-item", new PathRuleConfig("/web/v1/docs/*", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("api-v1-docs-history", new PathRuleConfig("/api/v1/docs/history/*", HttpMethod.GET, RoleEnum.VISITOR));
            dr.put("web-v1-docs-history", new PathRuleConfig("/web/v1/docs/history/*", HttpMethod.GET, RoleEnum.VISITOR));


            // 一般用戶 (USER)，此處開始都需要進行登入後才能訪問
            dr.put("folders-star-api", new PathRuleConfig("/api/v1/folders/star", null, RoleEnum.USER));
            dr.put("folders-star-web", new PathRuleConfig("/web/v1/folders/star", null, RoleEnum.USER));
            dr.put("folders-recently-api", new PathRuleConfig("/api/v1/folders/recently", null, RoleEnum.USER));
            dr.put("folders-recently-web", new PathRuleConfig("/web/v1/folders/recently", null, RoleEnum.USER));
            dr.put("folders-recycle-api", new PathRuleConfig("/api/v1/folders/recycle", null, RoleEnum.USER));
            dr.put("folders-recycle-web", new PathRuleConfig("/web/v1/folders/recycle", null, RoleEnum.USER));
            dr.put("folders-shared-api", new PathRuleConfig("/api/v1/folders/shared", null, RoleEnum.USER));
            dr.put("folders-shared-web", new PathRuleConfig("/web/v1/folders/shared", null, RoleEnum.USER));
            dr.put("folders-all-api", new PathRuleConfig("/api/v1/folders/all", null, RoleEnum.USER));
            dr.put("folders-all-web", new PathRuleConfig("/web/v1/folders/all", null, RoleEnum.USER));
            dr.put("folders-path-api", new PathRuleConfig("/api/v1/folders/path/*", null, RoleEnum.USER));
            dr.put("folders-path-web", new PathRuleConfig("/web/v1/folders/path/*", null, RoleEnum.USER));
            dr.put("user-file-list-api", new PathRuleConfig("/api/v1/files/user-file-list", null, RoleEnum.USER));
            dr.put("user-file-list-web", new PathRuleConfig("/web/v1/files/user-file-list", null, RoleEnum.USER));
            dr.put("files-search-api", new PathRuleConfig("/api/v1/files/search", null, RoleEnum.USER));
            dr.put("files-search-web", new PathRuleConfig("/web/v1/files/search", null, RoleEnum.USER));


            // ADVANCED_USER 進階用戶


            // 管理員 (ADMIN)，僅限管理員使用
            dr.put("actuator-admin", new PathRuleConfig("/actuator/**", null, RoleEnum.ADMIN));
            dr.put("all-user-info", new PathRuleConfig("/api/v1/user/info/all", null, RoleEnum.ADMIN));
        }


        /**
         * 獲取最終生效的路徑規則配置
         * 此方法會合併預設路徑規則和自定義路徑規則，並返回最終生效的路徑規則配置
         * 當檢測到不在預設路徑規則中的自定義路徑規則時，會將其添加到預設路徑規則中
         *
         * @return 最終生效的路徑規則配置
         */
        public Collection<PathRuleConfig> getEffectiveRules() {
            if (effectiveRulesCache == null) {
                Map<String, PathRuleConfig> merged = new LinkedHashMap<>(this.defaultRules);
                LogUnity.trace("預設路徑規則: " + merged);
                this.rules.forEach((key, customRule) -> {
                    LogUnity.trace("自定義路徑規則: " + key + " -> " + customRule);
                    PathRuleConfig ruleToUpdate = merged.get(key);
                    if (ruleToUpdate != null) {
                        if (customRule.getPattern() != null) {
                            ruleToUpdate.setPattern(customRule.getPattern());
                        }
                        if (customRule.getMethod() != null) {
                            ruleToUpdate.setMethod(customRule.getMethod());
                        }
                        if (customRule.getRole() != null) {
                            ruleToUpdate.setRole(customRule.getRole());
                        }
                    } else {
                        LogUnity.trace("自定義路徑規則不在預設路徑規則中，設定 Key: " + key + " -> " + customRule);
                        merged.put(key, customRule);
                    }
                });
                effectiveRulesCache = merged.values();
            }
            return effectiveRulesCache;
        }


        /**
         * 路徑規則配置
         * pattern: 路徑模式
         * method: 請求方法
         * role: 角色基準
         */
        @Data
        public static class PathRuleConfig {
            /**
             * 路徑模式
             */
            private String pattern;

            /**
             * 請求方法，當為 null 時表示所有方法
             */
            private HttpMethod method;

            /**
             * 角色基準，當為 null 時表示所有人，此處設定的角色為一個基準值，會自動設定具有更高權限的角色也可以訪問
             * 角色基準值的優先級為：ANONYMOUS < VISITOR < USER < ADVANCED_USER < ADMIN
             */
            private RoleEnum role;

            /**
             * PathRuleConfig 的建構子
             */
            public PathRuleConfig() {
            }

            /**
             * PathRuleConfig 的建構子
             *
             * @param pattern 路徑模式
             * @param method  請求方法
             * @param role    角色基準
             */
            public PathRuleConfig(String pattern, HttpMethod method, RoleEnum role) {
                this.pattern = pattern;
                this.method = method;
                this.role = role;
            }
        }
    }
}
