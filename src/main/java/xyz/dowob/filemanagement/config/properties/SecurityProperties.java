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
 * 安全設定屬性類，管理應用程式的所有安全相關設定參數。
 * <p>
 * 此類包含 JWT 認證、CSRF 保護、CORS 跨域設定、登入限制、路徑權限控制等所有安全機制。
 * 透過 Spring Boot 的 {@code @ConfigurationProperties} 機制，可在 application.yml 中使用 "security" 前綴進行設定。
 * <p>
 * 重要安全提醒：
 * - JWT 密鑰必須在生產環境中使用強密碼
 * - CORS 設定應只允許可信的來源網域
 * - 定期輪換 JWT 密鑰和 CSRF 令牌
 * - 監控登入失敗記錄和異常活動
 * <p>
 * 設定範例：
 * <pre>
 * security:
 *   jwt-token:
 *     secret: your-secret-key-here
 *     expiration: 24h
 *   cors:
 *     allowed-origins: ["https://trusted-domain.com"]
 *   csrf:
 *     expiration: 5m
 *   login:
 *     max-failure: 5
 *     lock-time: 30m
 * </pre>
 * <p>
 * 主要功能模組包括：
 * - {@link JwtToken} - JWT 認證令牌設定
 * - {@link Csrf} - CSRF 保護設定
 * - {@link Cors} - CORS 跨域設定
 * - {@link Login} - 登入限制設定
 * - {@link Paths} - 路徑權限控制
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * JWT 認證令牌設定實例。
     * <p>
     * 包含 JWT 令牌的密鑰、過期時間和 WebSocket 連接設定。
     * 安全警告：密鑰的安全性直接影響整個系統的安全。
     */
    private JwtToken jwtToken = new JwtToken();

    /**
     * 密碼重設令牌設定實例。
     * <p>
     * 管理用戶密碼重設時使用的临時令牌設定，包括令牌長度和過期時間。
     */
    private resetPasswordToken resetPasswordToken = new resetPasswordToken();

    /**
     * Cookie 安全設定實例。
     * <p>
     * 定義 Cookie 的安全屬性，包括 Secure、HttpOnly、SameSite 等設定。
     */
    private Cookie cookie = new Cookie();

    /**
     * CORS 跨域資源共享設定實例。
     * <p>
     * 控制哪些來源網域可以訪問應用程式的 API，包括允許的來源、方法和標頭。
     */
    private Cors cors = new Cors();

    /**
     * CSRF 跨站請求偽造保護設定實例。
     * <p>
     * 提供對抗 CSRF 攻擊的防護機制，包括令牌產生、驗證和儲存設定。
     */
    private Csrf csrf = new Csrf();

    /**
     * HSTS HTTP 嚴格傳輸安全設定實例。
     * <p>
     * 強制瀏覽器使用 HTTPS 連接，提升傳輸層的安全性。
     */
    private Hsts hsts = new Hsts();

    /**
     * 訪客用戶功能設定實例。
     * <p>
     * 控制是否允許未登入的訪客用戶訪問系統的部分功能。
     */
    private GuestUser guestUser = new GuestUser();

    /**
     * 用戶登入安全設定實例。
     * <p>
     * 管理登入失敗限制、帳號鎖定機制等安全措施，防止暴力破解攻擊。
     */
    private Login login = new Login();

    /**
     * API 路徑權限控制設定實例。
     * <p>
     * 定義不同路徑的訪問權限要求，包括角色限制、HTTP 方法限制等。
     */
    private Paths paths = new Paths();


    /**
     * 進行初始化操作
     * 1. 檢查 JWT 密鑰是否設定
     * 2. 設定使用的路徑規則
     */
    @PostConstruct
    public void init() {
        if (jwtToken.getSecret() == null || jwtToken.getSecret().trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 密鑰需要設定，請在 application 中設定 security.jwt-token.secret");
        }

        if (this.paths != null) {
            this.paths.getEffectiveRules();
        }

    }


    /**
     * JWT 認證令牌設定內部類。
     * <p>
     * 管理 JSON Web Token 的產生、驗證和安全設定，包括加密密鑰、過期時間和 WebSocket 認證支援。
     * JWT 是系統身份驗證的核心機制，其安全性直接影響整個系統的安全等級。
     */
    @Data
    public static class JwtToken {
        /**
         * JWT 加密和簽章使用的密鑰字串。
         * <p>
         * 此密鑰用於 JWT 令牌的數位簽章和驗證過程。密鑰的強度直接影響令牌的安全性：
         * - 生產環境必須使用長度至少 32 字元的隨機字串
         * - 建議定期輪換密鑰以提升安全性
         * - 密鑰洩漏將導致所有已發行的令牌都可被偽造
         * <p>
         * 安全提醒：此值為敏感資訊，不應記錄在日誌中或暴露給未授權人員。
         */
        private String secret;

        /**
         * JWT 令牌的有效期限制。
         * <p>
         * 定義令牌從發行到失效的時間長度。過期的令牌將無法通過驗證。
         * 建議根據安全需求和用戶體驗平衡設定：
         * - 較短的有效期提升安全性但可能影響用戶體驗
         * - 較長的有效期提升便利性但增加安全風險
         * <p>
         * 預設值：1 天
         */
        private Duration expiration = Duration.ofDays(1);

        /**
         * WebSocket 連接使用的 JWT 令牌前綴。
         * <p>
         * 用於區分 WebSocket 連接中傳遞的 JWT 令牌與其他類型的訊息。
         * 客戶端在建立 WebSocket 連接時，需要在令牌前加上此前綴。
         * <p>
         * 預設值：{@code "jwt."}
         */
        private String webSocketTokenPrefix = "jwt.";
    }


    /**
     * 密碼重設令牌設定內部類。
     * <p>
     * 管理用戶密碼重設流程中使用的一次性驗證令牌，包括令牌長度、過期時間等安全參數。
     * 此機制用於確保密碼重設請求的安全性和時效性。
     */
    @Data
    public static class resetPasswordToken {
        /**
         * 密碼重設驗證碼的字元長度。
         * <p>
         * 定義系統產生的密碼重設驗證碼包含的字元數量。較長的驗證碼提供更高的安全性，
         * 但可能影響用戶輸入的便利性。建議在安全性和可用性間找到平衡：
         * - 6 位數字：適合簡訊或郵件驗證
         * - 8-12 位混合字元：適合高安全性要求
         * <p>
         * 預設值：6
         */
        private int length = 6;

        /**
         * 密碼重設令牌的有效期限制。
         * <p>
         * 定義密碼重設驗證碼從產生到失效的時間長度。過期的驗證碼將無法用於密碼重設。
         * 較短的有效期可以降低令牌被惡意使用的風險，但可能給用戶操作帶來時間壓力。
         * <p>
         * 建議設定為 15-60 分鐘，平衡安全性和用戶體驗。
         * <p>
         * 預設值：30 分鐘
         */
        private Duration expiration = Duration.ofMinutes(30);
    }

    /**
     * Cookie 安全設定內部類。
     * <p>
     * 定義 HTTP Cookie 的安全屬性設定，包括加密傳輸、XSS 防護、CSRF 防護等安全機制。
     * 正確的 Cookie 設定是防止各種 Web 攻擊的重要防線。
     */
    @Data
    public static class Cookie {
        /**
         * Cookie 是否僅透過安全連接（HTTPS）傳輸。
         * <p>
         * 當設為 {@code true} 時，Cookie 只會在 HTTPS 連接中傳送，防止在不安全的 HTTP 連接中洩漏。
         * 這是防止中間人攻擊和 Cookie 竊取的重要安全措施。
         * <p>
         * 注意：在生產環境中強烈建議設為 {@code true}，開發環境可根據需要調整。
         * <p>
         * 預設值：{@code true}
         */
        private boolean secure = true;

        /**
         * Cookie 是否僅允許 HTTP 協議存取。
         * <p>
         * 當設為 {@code true} 時，Cookie 無法透過 JavaScript 的 {@code document.cookie} API 存取，
         * 這有效防止 XSS（跨站腳本攻擊）竊取用戶的認證資訊。
         * <p>
         * 強烈建議保持啟用狀態以提升安全性。
         * <p>
         * 預設值：{@code true}
         */
        private boolean httpOnly = true;

        /**
         * Cookie 的 SameSite 屬性設定。
         * <p>
         * 控制 Cookie 在跨站請求中的傳送行為，防止 CSRF（跨站請求偽造）攻擊：
         * - {@code "Strict"} - 最嚴格，僅在同站請求中傳送
         * - {@code "Lax"} - 適中，在安全的跨站導航中傳送（如連結點擊）
         * - {@code "None"} - 最寬鬆，所有跨站請求都傳送（需要 Secure 屬性）
         * <p>
         * 預設值：{@code "Lax"}
         */
        private String sameSite = "Lax";

        /**
         * 存儲 JWT 令牌的 Cookie 名稱。
         * <p>
         * 定義在 HTTP 回應和後續請求中用於識別 JWT 令牌的 Cookie 名稱。
         * 此名稱應具有一定的唯一性，避免與其他系統或元件的 Cookie 名稱衝突。
         * <p>
         * 預設值：{@code "jwtToken"}
         */
        private String tokenName = "jwtToken";
    }

    /**
     * CORS 跨域資源共享設定內部類。
     * <p>
     * 管理跨來源 HTTP 請求的安全策略，控制哪些外部網域可以存取應用程式的 API。
     * 正確的 CORS 設定既能支援前後端分離架構，又能防止未授權的跨域存取。
     */
    @Data
    public static class Cors {
        /**
         * 允許進行跨域請求的來源網域列表。
         * <p>
         * 指定具體的網域 URL（包括協議、主機名和埠號）來限制跨域存取。
         * 空列表表示不允許任何明確指定的來源，但可能受其他設定影響。
         * <p>
         * 範例：{@code ["https://example.com:3000", "https://app.example.com"]}
         * <p>
         * 預設值：空列表
         */
        private List<String> allowedOrigins = List.of();

        /**
         * 允許進行跨域請求的來源網域正則表達式列表。
         * <p>
         * 使用正則表達式模式匹配來源網域，提供更靈活的網域控制。
         * 適用於需要支援多個子網域或動態網域的情況。
         * <p>
         * 範例：{@code ["^https://.*\\.example\\.com$", "^http://localhost:[0-9]+$"]}
         * <p>
         * 預設值：空列表
         */
        private List<String> allowedOriginsPattern = List.of();

        /**
         * 允許客戶端存取的 HTTP 回應標頭列表。
         * <p>
         * 指定在跨域請求回應中，瀏覽器允許 JavaScript 讀取的額外標頭。
         * 系統會自動添加 {@code Content-Disposition}、{@code Authorization}、{@code X-CSRF-TOKEN} 等常用標頭。
         * <p>
         * 預設值：空列表（僅包含自動添加的標頭）
         */
        private List<String> allowExposedHeaders = List.of();

        /**
         * 允許的 HTTP 請求方法列表。
         * <p>
         * 指定跨域請求中允許使用的 HTTP 方法。使用 {@code "*"} 表示允許所有方法。
         * 為了安全考量，建議明確指定需要的方法而非使用萬用字元。
         * <p>
         * 預設值：{@code ["*"]}（所有方法）
         */
        private List<String> allowedMethods = List.of("*");

        /**
         * 允許的 HTTP 請求標頭列表。
         * <p>
         * 指定跨域請求中允許攜帶的 HTTP 標頭。使用 {@code "*"} 表示允許所有標頭。
         * 建議根據應用需求明確指定標頭以提升安全性。
         * <p>
         * 預設值：{@code ["*"]}（所有標頭）
         */
        private List<String> allowedHeaders = List.of("*");

        /**
         * CORS 預檢請求的快取時間。
         * <p>
         * 定義瀏覽器快取 CORS 預檢請求結果的時間長度。在此期間內，
         * 相同的跨域請求不需要重新發送預檢請求，可以提升效能。
         * <p>
         * 預設值：1 小時
         */
        private Duration maxAge = Duration.ofHours(1);

        /**
         * 是否允許跨域請求攜帶認證資訊。
         * <p>
         * 當設為 {@code true} 時，跨域請求可以攜帶 Cookie、Authorization 標頭等認證資訊。
         * <p>
         * 重要限制：啟用此選項時，{@link #allowedOrigins} 和 {@link #allowedOriginsPattern} 
         * 不能使用萬用字元 {@code "*"}，必須明確指定允許的來源網域。
         * <p>
         * 預設值：{@code true}
         */
        private boolean allowCredentials = true;
    }

    /**
     * CSRF 跨站請求偽造保護設定內部類。
     * <p>
     * 管理 CSRF 攻擊防護機制，透過令牌驗證確保請求來自合法的用戶操作。
     * CSRF 保護是防止惡意網站代替用戶執行未授權操作的重要安全措施。
     */
    @Data
    public static class Csrf {
        /**
         * CSRF 令牌在 HTTP 請求標頭中的名稱。
         * <p>
         * 用於 AJAX 或 Fetch API 等 JavaScript 請求中傳遞 CSRF 令牌。
         * 前端應用需要在每個需要 CSRF 保護的請求中包含此標頭。
         * <p>
         * 預設值：{@code "X-Csrf-Token"}
         */
        private String headerName = "X-Csrf-Token";

        /**
         * CSRF 令牌在表單參數中的名稱。
         * <p>
         * 用於傳統 HTML 表單提交中傳遞 CSRF 令牌。
         * 前端需要在表單中包含此隱藏欄位來攜帶令牌值。
         * <p>
         * 預設值：{@code "_csrf"}
         */
        private String parameterName = "_csrf";

        /**
         * CSRF 令牌的有效期限制。
         * <p>
         * 定義 CSRF 令牌從產生到失效的時間長度。過期的令牌將無法通過驗證。
         * 較短的有效期可以降低令牌被重用攻擊的風險，但不應設定過短以免影響用戶體驗。
         * <p>
         * 安全建議：不建議設定超過 30 分鐘，以維持適當的安全等級。
         * <p>
         * 預設值：5 分鐘
         */
        private Duration expiration = Duration.ofMinutes(5);

        /**
         * CSRF 令牌的儲存實現方式。
         * <p>
         * 決定 CSRF 令牌在伺服器端的儲存後端：
         * - {@code LOCAL} - 使用本地記憶體儲存，適合單實例部署
         * - {@code REDIS} - 使用 Redis 儲存，適合分散式部署環境
         * <p>
         * 預設值：{@code CsrfTokenRepositoryEnum.REDIS}
         */
        private CsrfTokenRepositoryEnum csrfTokenRepository = CsrfTokenRepositoryEnum.REDIS;

        /**
         * 允許獲取 CSRF 令牌的 Referer 來源正則表達式。
         * <p>
         * 控制哪些來源網域可以請求獲取 CSRF 令牌，用於限制令牌獲取的安全性。
         * 只有 Referer 標頭匹配此模式的請求才能成功獲取令牌。
         * <p>
         * 安全建議：在生產環境中應該限制為可信的前端網域，
         * 例如：{@code "^https://your-frontend-domain\\.com$"}
         * <p>
         * 預設值：{@code "^https?://.*$"}（允許所有 HTTP/HTTPS 來源）
         */
        private String allowRefererPatten = "^https?://.*$";
    }

    /**
     * HSTS HTTP 嚴格傳輸安全設定內部類。
     * <p>
     * 管理 HTTP 嚴格傳輸安全策略，強制瀏覽器只能透過 HTTPS 連接存取網站。
     * HSTS 是防止協議降級攻擊和 Cookie 劫持的重要安全機制。
     */
    @Data
    public static class Hsts {
        /**
         * HSTS 策略的有效期時間。
         * <p>
         * 定義瀏覽器記住並強制執行 HSTS 策略的時間長度。
         * 在此期間內，瀏覽器會自動將所有 HTTP 請求重導向為 HTTPS。
         * <p>
         * 較長的時間提供更好的安全保護，但在需要回退到 HTTP 時會造成困難。
         * <p>
         * 預設值：365 天
         */
        private Duration maxAge = Duration.ofDays(365);

        /**
         * 是否將 HSTS 策略套用到所有子網域。
         * <p>
         * 當設為 {@code true} 時，HSTS 策略會自動套用到當前網域的所有子網域。
         * 這提供了更全面的安全保護，但需要確保所有子網域都支援 HTTPS。
         * <p>
         * 預設值：{@code true}
         */
        private boolean includeSubDomains = true;

        /**
         * 是否啟用 HSTS 預載入功能。
         * <p>
         * 當設為 {@code true} 時，表示網站已準備好被加入瀏覽器的 HSTS 預載入清單。
         * 預載入清單中的網域會在用戶首次訪問前就被強制使用 HTTPS。
         * <p>
         * 注意：啟用此選項需要額外向瀏覽器廠商提交預載入申請。
         * <p>
         * 預設值：{@code true}
         */
        private boolean preload = true;
    }

    /**
     * 訪客用戶功能設定內部類。
     * <p>
     * 控制系統是否允許未登入的訪客用戶存取特定功能。
     * 訪客模式可以提升用戶體驗，但需要仔細平衡便利性和安全性。
     */
    @Data
    public static class GuestUser {
        /**
         * 是否啟用訪客用戶功能。
         * <p>
         * 當設為 {@code true} 時，未登入的用戶可以存取具有 {@code VISITOR} 權限的資源。
         * 當設為 {@code false} 時，所有需要 {@code VISITOR} 權限的資源都會要求用戶登入。
         * <p>
         * 安全考量：啟用訪客功能可能增加系統暴露面，建議僅開放必要的唯讀功能給訪客。
         * <p>
         * 預設值：{@code true}
         */
        private boolean enable = true;
    }

    /**
     * 用戶登入安全設定內部類。
     * <p>
     * 管理登入失敗限制和帳號鎖定機制，防止暴力破解攻擊和惡意登入嘗試。
     * 透過限制登入失敗次數和臨時鎖定帳號，有效提升帳號安全性。
     */
    @Data
    public static class Login {
        /**
         * 登入失敗限制器的實現類型。
         * <p>
         * 決定登入失敗記錄的儲存後端和限制機制的實現方式：
         * - {@code redis} - 使用 Redis 實現分散式限制，適合多實例部署
         * - {@code local} - 使用本地記憶體實現，適合單實例部署
         * - {@code none} - 停用登入限制功能（不建議在生產環境使用）
         * <p>
         * 預設值：{@code LimiterProviderType.local}
         */
        private LimiterProviderType limiterProvider = LimiterProviderType.local;

        /**
         * 觸發帳號鎖定的最大登入失敗次數。
         * <p>
         * 定義用戶在 {@link #lockTime} 時間內允許的最大登入失敗次數。
         * 當失敗次數達到此閾值時，帳號將被臨時鎖定。成功登入會重置失敗計數。
         * <p>
         * 安全建議：設定為 3-10 次之間，平衡安全性和用戶體驗。
         * <p>
         * 約束條件：此值必須大於 0，否則會拋出 {@code IllegalArgumentException}。
         * <p>
         * 預設值：5
         */
        private int maxFailure = 5;

        /**
         * 帳號鎖定的持續時間。
         * <p>
         * 定義帳號被鎖定後無法嘗試登入的時間長度。
         * 鎖定期間的登入嘗試會被直接拒絕，不會進一步增加失敗計數。
         * <p>
         * 安全建議：設定為 15-60 分鐘，既能防止自動化攻擊又不會過度影響正常用戶。
         * <p>
         * 約束條件：此值必須大於 0，否則會拋出 {@code IllegalArgumentException}。
         * <p>
         * 預設值：30 分鐘
         */
        private Duration lockTime = Duration.ofMinutes(30);

        /**
         * 登入限制器實現類型列舉。
         * <p>
         * 定義不同的登入失敗限制實現方案。
         */
        private enum LimiterProviderType {
            /**
             * Redis 分散式限制器。
             * <p>
             * 使用 Redis 儲存登入失敗記錄，支援多實例環境下的分散式限制。
             * 適合叢集部署，所有實例共享登入狀態。
             */
            redis,

            /**
             * 本地記憶體限制器。
             * <p>
             * 使用應用程式本地記憶體儲存登入失敗記錄。
             * 適合單實例部署，效能較高但不支援分散式環境。
             */
            local,

            /**
             * 停用登入限制。
             * <p>
             * 完全停用登入失敗限制功能，所有登入嘗試都不會被限制。
             * 僅建議在開發環境或特殊情況下使用。
             */
            none
        }
    }


    /**
     * API 路徑權限控制設定內部類。
     * <p>
     * 管理不同 API 路徑的存取權限、HTTP 方法限制和角色要求。
     * 支援預設規則和自定義規則的合併，提供靈活的權限控制機制。
     * <p>
     * 設定覆蓋範例：
     * <pre>
     * security:
     *   paths:
     *     rules:
     *       websocket:
     *         pattern: /ws/**
     *         method: GET
     *         role: anonymous
     *       user-info-api:
     *         pattern: /api/v1/user/info
     *         role: advanced_user
     * </pre>
     * <p>
     * 重要提醒：自定義規則的鍵值名稱必須與預設規則名稱一致才能生效。
     */
    @Getter
    public static class Paths {
        /**
         * 系統預設的路徑權限規則集合。
         * <p>
         * 包含所有內建的 API 路徑權限設定，按照角色等級分組管理。
         * 此映射為唯讀，不可透過外部設定直接修改，僅能透過 {@link #rules} 進行覆蓋。
         * <p>
         * 映射結構：規則名稱 → 路徑規則設定
         */
        private final Map<String, PathRuleConfig> defaultRules = new LinkedHashMap<>();

        /**
         * 用戶自定義的路徑權限規則集合。
         * <p>
         * 允許用戶在 application.yml 中覆蓋或新增路徑權限規則。
         * 當自定義規則的鍵值與預設規則相同時，會覆蓋預設設定。
         * <p>
         * 映射結構：規則名稱 → 路徑規則設定
         */
        @Setter
        private Map<String, PathRuleConfig> rules = new LinkedHashMap<>();

        /**
         * 快取的最終生效路徑規則集合。
         * <p>
         * 存儲合併後的預設規則和自定義規則，避免重複計算。
         * 使用 {@code transient} 關鍵字避免序列化，每次應用啟動時重新計算。
         */
        private transient Collection<PathRuleConfig> effectiveRulesCache;

        /**
         * 建構函數。
         * <p>
         * 初始化預設路徑權限規則，建立完整的 API 權限体系。
         */
        public Paths() {
            initializeDefaultPathRules();
        }

        /**
         * 初始化系統預設的路徑權限規則。
         * <p>
         * 按照角色等級建立完整的 API 路徑權限体系：
         * - ANONYMOUS：所有人可存取（包括未登入用戶）
         * - VISITOR：訪客用戶可存取（需要啟用訪客功能）
         * - USER：一般登入用戶可存取
         * - ADVANCED_USER：進階用戶可存取
         * - ADMIN：僅管理員可存取
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
         * 獲取最終生效的路徑權限規則集合。
         * <p>
         * 合併預設規則和用戶自定義規則，產生最終生效的權限設定。
         * 合併策略：
         * - 自定義規則可以覆蓋相同鍵值的預設規則
         * - 自定義規則可以新增不存在的路徑規則
         * - 覆蓋時僅更新非 null 的屬性值
         * <p>
         * 結果會被快取以提升後續查詢效能。
         *
         * @return 合併後的路徑權限規則集合
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
         * 路徑權限規則設定類。
         * <p>
         * 定義單個 API 路徑的安全限制，包括路徑模式、HTTP 方法和角色要求。
         * 支援基於角色等級的權限繼承機制。
         */
        @Data
        public static class PathRuleConfig {
            /**
             * API 路徑的模式匹配表達式。
             * <p>
             * 支援 Spring 路徑模式語法，包括萬用字元和路徑變數：
             * - {@code /api/v1/user/**} - 匹配所有以 /api/v1/user/ 開頭的路徑
             * - {@code /api/v1/files/*} - 匹配單層路徑結構
             * - {@code /api/v1/user/{id}} - 匹配路徑變數
             */
            private String pattern;

            /**
             * 允許的 HTTP 請求方法。
             * <p>
             * 指定此路徑規則套用的 HTTP 方法。當為 {@code null} 時表示允許所有 HTTP 方法。
             * 常用值：GET、POST、PUT、DELETE、PATCH 等。
             */
            private HttpMethod method;

            /**
             * 最低要求的用戶角色等級。
             * <p>
             * 定義存取此路徑所需的最低角色權限。系統會自動允許更高等級的角色存取。
             * <p>
             * 角色等級排序（由低到高）：
             * {@code ANONYMOUS} < {@code VISITOR} < {@code USER} < {@code ADVANCED_USER} < {@code ADMIN}
             * <p>
             * 當為 {@code null} 時表示不需要任何角色要求（等同於 ANONYMOUS）。
             */
            private RoleEnum role;

            /**
             * 預設建構函數。
             */
            public PathRuleConfig() {
            }

            /**
             * 建構函數。
             *
             * @param pattern 路徑模式匹配表達式
             * @param method  允許的 HTTP 請求方法，{@code null} 表示所有方法
             * @param role    最低要求的用戶角色等級，{@code null} 表示不限制
             */
            public PathRuleConfig(String pattern, HttpMethod method, RoleEnum role) {
                this.pattern = pattern;
                this.method = method;
                this.role = role;
            }
        }
    }
}
