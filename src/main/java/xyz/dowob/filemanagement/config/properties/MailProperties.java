package xyz.dowob.filemanagement.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import xyz.dowob.filemanagement.customenum.OAuthProviderEnum;

/**
 * 郵件服務配置屬性類，管理郵件發送相關的設定參數。
 * <p>
 * 此類整合了傳統 SMTP 認證和現代 OAuth 2.0 認證兩種郵件發送方式的配置。
 * 透過 Spring Boot 的 {@code @ConfigurationProperties} 機制，可在 application.yml 中使用 "spring.mail" 前綴進行設定。
 * 支援動態切換認證方式，提供企業級郵件服務的彈性配置能力。
 * <p>
 * 設定範例：
 * <pre>
 * spring:
 *   mail:
 *     oauth:
 *       enabled: true
 *       provider: microsoft
 *       client-id: your-client-id
 *       client-secret: your-client-secret
 *       tenant-id: your-tenant-id
 *       scope: https://graph.microsoft.com/Mail.Send
 * </pre>
 * <p>
 * 主要功能：
 * - 支援傳統的帳號密碼 SMTP 認證
 * - 支援 OAuth 2.0 認證（Microsoft、Google 等）
 * - 提供認證方式的動態切換能力
 * - 支援多種 OAuth 提供者的配置
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {
    /**
     * OAuth 2.0 認證配置實例。
     * <p>
     * 包含啟用狀態、提供者類型、客戶端憑證等 OAuth 認證所需的完整配置。
     */
    private OAuth oauth = new OAuth();

    /**
     * 郵件發送者設定實例。
     * <p>
     * 包含系統郵件發送時使用的發送者電子郵件地址等相關參數。
     */
    private MailSender mailSender = new MailSender();

    /**
     * OAuth 2.0 認證配置內部類。
     * <p>
     * 管理 OAuth 2.0 認證流程所需的各項參數，支援多種主流郵件服務提供者的認證配置。
     * 當啟用 OAuth 認證時，系統將使用這些配置獲取存取令牌並進行郵件發送。
     */
    @Data
    public static class OAuth {
        /**
         * 是否啟用 OAuth 2.0 認證。
         * <p>
         * 當設定為 {@code true} 時，系統將使用 OAuth 2.0 認證方式發送郵件，
         * 否則使用傳統的 SMTP 帳號密碼認證。
         * <p>
         * 預設值：{@code false}
         */
        private boolean enabled = false;

        /**
         * OAuth 提供者類型。
         * <p>
         * 指定使用的 OAuth 認證提供者，不同提供者有不同的認證端點和 API 格式。
         * 支援的提供者：
         * - {@code MICROSOFT} - Microsoft/Office 365
         * - {@code GOOGLE} - Google Workspace/Gmail
         * <p>
         * 預設值：{@code OAuthProviderEnum.MICROSOFT}
         */
        private OAuthProviderEnum provider = OAuthProviderEnum.MICROSOFT;
        
        /**
         * OAuth 應用程式的客戶端 ID。
         * <p>
         * 在 OAuth 提供者的應用程式註冊頁面獲得的唯一識別碼。
         * 此值用於識別您的應用程式身份。
         * <p>
         * 必填項目（當啟用 OAuth 時）
         */
        private String clientId;

        /**
         * OAuth 應用程式的客戶端密鑰。
         * <p>
         * 在 OAuth 提供者的應用程式註冊頁面獲得的密鑰。
         * 此值應該安全儲存，不應提交到版本控制系統。
         * <p>
         * 必填項目（當啟用 OAuth 時）
         */
        private String clientSecret;

        /**
         * Microsoft Azure AD 租戶 ID。
         * <p>
         * 僅在使用 Microsoft OAuth 時需要。指定 Azure AD 租戶的唯一識別碼。
         * 可以是具體的租戶 ID 或使用 "common"、"organizations"、"consumers" 等預設值。
         * <p>
         * 預設值：{@code null}（Microsoft 專用）
         */
        private String tenantId;

        /**
         * OAuth 權限範圍。
         * <p>
         * 定義應用程式需要的權限範圍，決定可以執行哪些操作。
         * 不同的提供者有不同的權限範圍格式：
         * - Microsoft: {@code "https://graph.microsoft.com/Mail.Send"}
         * - Google: {@code "https://www.googleapis.com/auth/gmail.send"}
         * <p>
         * 預設值：{@code "https://graph.microsoft.com/Mail.Send"}
         */
        private String scope = "https://graph.microsoft.com/Mail.Send";

        /**
         * OAuth 認證端點 URL。
         * <p>
         * OAuth 提供者的授權伺服器端點，用於獲取存取令牌。
         * 如果未設定，系統會根據 provider 自動選擇預設端點。
         * <p>
         * 預設值：{@code null}（自動選擇）
         */
        private String tokenUrl;

        /**
         * 存取令牌的快取時間（秒）。
         * <p>
         * 定義獲取的存取令牌在本地快取的時間，避免頻繁請求新令牌。
         * 應該設定為略小於令牌實際有效期的值。
         * <p>
         * 預設值：3300（55 分鐘）
         */
        private int tokenCacheDuration = 3300;


        /**
         * 設定 OAuth 提供者（支援字串配置）。
         * <p>
         * 允許從配置檔案中使用字串設定提供者，會自動轉換為對應的枚舉值。
         *
         * @param provider 提供者名稱（不區分大小寫）
         * @throws IllegalArgumentException 當提供者名稱無效時
         */
        public void setProvider(String provider) {
            this.provider = OAuthProviderEnum.fromName(provider);
        }
    }

    /**
     * 郵件發送設定內部類。
     * <p>
     * 管理系統發送郵件時使用的發送者資訊和相關參數。
     */
    @Data
    public static class MailSender {
        /**
         * 系統郵件發送者的電子郵件地址。
         * <p>
         * 定義系統發送各種通知郵件（如密碼重設、帳號驗證等）時使用的發送者地址。
         * 此地址會顯示在收件者的郵件客戶端中作為發送者資訊。
         * <p>
         * 注意：此地址必須與 SMTP 伺服器設定中的認證資訊相符，
         * 否則可能導致郵件發送失敗或被標記為垃圾郵件。
         * <p>
         * 預設值：{@code "sender@example.com"}
         */
        private String mailSender = "sender@example.com";
    }
}