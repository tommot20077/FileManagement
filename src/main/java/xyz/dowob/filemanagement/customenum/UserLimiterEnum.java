package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 使用者權限限制器類型列舉。
 * <p>定義系統中不同類型的使用者操作限制器，用於控制使用者的操作頻率和系統資源使用。
 * 包括檔案上傳限制和登入對次限制，每個限制器都有独立的描述和錯誤訊息，用於提供明確的使用者回饋。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RequiredArgsConstructor
@Getter
public enum UserLimiterEnum {
    /**
     * 使用者檔案上傳限制器。
     * 控制使用者同時進行的檔案上傳數量，防止過度使用系統資源和網路頁寬。
     */
    USER_UPLOAD_LIMITER("用戶上傳限流器", "當前已經達到最大上傳數量限制"),

    /**
     * 使用者登入對次限制器。
     * 控制使用者登入對次頻率，防止暴力破解攻擊和恶意登入對次，增強系統安全性。
     */
    USER_LOGIN_LIMITER("用戶登錄限流器", "當前已經達到最大登錄次數限制");

    /**
     * 限制器功能描述。
     * 提供限制器的中文功能說明，用於系統日誌記錄和管理介面顯示。
     */
    private final String description;

    /**
     * 限制觸發時的錯誤訊息。
     * 當使用者觸發限制條件時向客戶端回傳的錯誤說明，提供明確的限制原因和指導。
     */
    private final String error;
}
