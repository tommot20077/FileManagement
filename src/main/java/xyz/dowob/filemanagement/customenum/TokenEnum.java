package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 系統認證憑證類型列舉。
 * <p>定義檔案管理系統中使用的不同類型認證憑證，包括 JWT 授權憑證和密碼重設憑證。
 * 每種憑證類型具有不同的使用場景和有效期限，用於支援安全的使用者認證和授權流程。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

@Getter
@RequiredArgsConstructor
public enum TokenEnum {
    /**
     * JSON Web Token 授權憑證。
     * 用於使用者身份驗證和 API 存取授權，包含使用者訊息、角色權限和有效期限。
     */

    JWT_AUTHORIZATION_TOKEN("JWT 憑證"),
    /**
     * 密碼重設憑證。
     * 用於使用者密碼重設流程的一次性驗證憑證，具有短期有效性和單次使用限制。
     */
    RESET_PASSWORD_TOKEN("重製密碼憑證");

    /**
     * 憑證類型描述字串。
     * 提供憑證類型的中文描述，用於日誌記錄、錯誤訊息和使用者介面顯示。
     */
    private final String tokenType;

}
