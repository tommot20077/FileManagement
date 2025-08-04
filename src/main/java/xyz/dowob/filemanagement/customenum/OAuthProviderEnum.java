package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OAuth 認證提供者枚舉，定義支援的 OAuth 服務提供者及其配置。
 * <p>
 * 此枚舉統一管理不同 OAuth 提供者的認證端點、權限範圍等配置資訊，
 * 簡化郵件服務的 OAuth 整合流程。每個提供者都有其特定的 token 端點模式和預設權限範圍。
 * <p>
 * 支援的提供者：
 * <ul>
 *   <li><strong>MICROSOFT</strong> - Microsoft/Office 365 郵件服務</li>
 *   <li><strong>GOOGLE</strong> - Google Workspace/Gmail 郵件服務</li>
 * </ul>
 * <p>
 * 使用範例：
 * <pre>
 * OAuthProviderEnum provider = OAuthProviderEnum.MICROSOFT;
 * String tokenUrl = String.format(provider.getTokenUrlPattern(), "common");
 * String scope = provider.getDefaultScope();
 * </pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum OAuthProviderEnum {
    /**
     * Microsoft OAuth 提供者。
     * <p>
     * 用於 Microsoft 365、Outlook.com 等 Microsoft 郵件服務的 OAuth 認證。
     * token URL 中的 %s 參數為租戶 ID，可使用 "common"、"organizations" 或具體的租戶 ID。
     */
    MICROSOFT("microsoft",
              "https://login.microsoftonline.com/%s/oauth2/v2.0/token",
              "https://graph.microsoft.com/Mail.Send"),

    /**
     * Google OAuth 提供者。
     * <p>
     * 用於 Gmail、Google Workspace 等 Google 郵件服務的 OAuth 認證。
     * 使用固定的 token 端點，不需要租戶參數。
     */
    GOOGLE("google",
           "https://oauth2.googleapis.com/token",
           "https://www.googleapis.com/auth/gmail.send");

    /**
     * 提供者的名稱標識。
     * <p>
     * 用於配置檔案中的識別和快取鍵的生成。
     */
    private final String name;

    /**
     * OAuth token 端點 URL 模式。
     * <p>
     * 用於獲取 access token 的端點 URL。
     * Microsoft 的 URL 包含 %s 佔位符用於租戶 ID。
     */
    private final String tokenUrlPattern;

    /**
     * 預設的權限範圍。
     * <p>
     * 定義應用程式請求的預設權限，用於郵件發送功能。
     * 不同提供者有不同的權限範圍格式。
     */
    private final String defaultScope;

    /**
     * 根據名稱查找對應的 OAuth 提供者。
     * <p>
     * 不區分大小寫的名稱匹配，方便從配置檔案讀取。
     *
     * @param name 提供者名稱
     * @return 對應的 OAuth 提供者枚舉
     * @throws IllegalArgumentException 當提供者名稱無效時
     */
    public static OAuthProviderEnum fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("OAuth 提供者名稱不能為 null");
        }
        
        for (OAuthProviderEnum provider : values()) {
            if (provider.name.equalsIgnoreCase(name)) {
                return provider;
            }
        }
        
        throw new IllegalArgumentException("不支援的 OAuth 提供者: " + name);
    }

    /**
     * 獲取格式化的 token URL。
     * <p>
     * 對於 Microsoft，需要提供租戶 ID；對於 Google，返回固定的 URL。
     *
     * @param tenantId 租戶 ID（僅 Microsoft 使用）
     * @return 完整的 token 端點 URL
     */
    public String getTokenUrl(String tenantId) {
        if (this == MICROSOFT) {
            String tenant = (tenantId != null && !tenantId.isEmpty()) ? tenantId : "common";
            return String.format(tokenUrlPattern, tenant);
        }
        return tokenUrlPattern;
    }
}