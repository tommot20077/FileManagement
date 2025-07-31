package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

/**
 * 憑證實體類別，對應資料庫中的 tokens 表。
 * <p>
 * 此實體類別管理系統中用戶的認證憑證和安全權束，包括 JWT 憑證的版本控制
 * 和密碼重設驗證機制。每個用戶對應一個憑證記錄，用於追蹤和管理
 * 其認證狀態和安全相關的操作。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>JWT 憑證版本管理（支援憑證撤銷機制）</li>
 *   <li>憑證有效期管理（自動過期檢測）</li>
 *   <li>密碼重設驗證碼管理（限時有效）</li>
 *   <li>用戶身份認證和權限控制</li>
 * </ul>
 * </p>
 * <p>
 * 安全特性：
 * <ul>
 *   <li>憑證版本控制：防止既有憑證被惡意使用</li>
 *   <li>時間限制：驗證碼和憑證都有明確的有效期</li>
 *   <li>單一性：每個用戶只有一個有效的憑證記錄</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see java.util.UUID
 * @see java.time.LocalDateTime
 */
@Getter
@Setter
@Table(name = "tokens")
public class Token {
    /**
     * 憑證記錄的唯一主鍵識別碼。
     * <p>
     * 此欄位作為表的主鍵，由資料庫自動生成，確保每個憑證記錄的唯一性。
     * 用於識別和區分不同的憑證記錄，並作為關聯操作的基礎。
     * </p>
     */
    @Id
    private long id;

    /**
     * 憑證擁有者的用戶識別碼。
     * <p>
     * 建立與 {@link User} 實體的外鍵關聯，指明此憑證記錄所屬的用戶。
     * 每個用戶在系統中只有一個有效的憑證記錄，用於管理其認證狀態和安全相關操作。
     * </p>
     */
    @Column("user_id")
    private long userId;

    /**
     * JWT 憑證的版本識別碼。
     * <p>
     * 用於實現 JWT 憑證的版本控制機制，當用戶重新登入、登出或憑證被撤銷時，
     * 會生成新的版本號。系統會檢查 JWT 憑證中的版本號是否與資料庫中的版本一致，
     * 以確保憑證的有效性和安全性。
     * </p>
     * 
     * @see #generateJwtTokenVersion()
     */
    @Column("jwt_token_version")
    private String jwtTokenVersion;

    /**
     * JWT 憑證的過期時間戳。
     * <p>
     * 記錄 JWT 憑證的有效期限，系統會定期檢查此時間以確定憑證是否已過期。
     * 過期的憑證會被視為無效，用戶需要重新登入以獲取新的憑證。
     * 此機制提供了額外的安全保障，限制憑證的使用時間。
     * </p>
     */
    @Column("jwt_token_expire_time")
    private LocalDateTime jwtTokenExpireTime;

    /**
     * 密碼重設功能的驗證碼。
     * <p>
     * 當用戶申請密碼重設時，系統會生成一個隨機的驗證碼並存儲在此欄位。
     * 用戶需要在指定時間內輸入此驗證碼以完成密碼重設操作。
     * 為了安全考量，驗證碼會在使用後或過期後被清除。
     * </p>
     */
    @Column("reset_verification_code")
    private String resetVerificationCode;

    /**
     * 密碼重設驗證碼的過期時間戳。
     * <p>
     * 定義密碼重設驗證碼的有效期限，通常設定為數分鐘到數小時不等。
     * 超過此時間的驗證碼將被視為無效，用戶需要重新申請密碼重設。
     * 此機制防止驗證碼被惡意使用，提供時間限制的安全保障。
     * </p>
     */
    @Column("reset_verification_code_expire_time")
    private LocalDateTime resetVerificationCodeExpireTime;

    /**
     * 生成唯一的 JWT 憑證版本號。
     * <p>
     * 此静態方法使用 UUID 生成一個唯一的憑證版本識別碼，
     * 用於 JWT 憑證的版本控制和撤銷機制。通過截取 UUID 的前 18 個字元，
     * 確保生成的版本號既唯一又相對簡潔。
     * </p>
     * <p>
     * 當用戶重新登入或憑證被撤銷時，會產生新的版本號，
     * 使旧的 JWT 憑證失效，提供額外的安全保障。
     * </p>
     *
     * @return 18 位元的唯一版本識別碼字串
     * @see UUID#randomUUID()
     */
    public static String generateJwtTokenVersion() {
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(0, 18);
    }

    /**
     * 計算憑證物件的雜湊碼，基於憑證的唯一識別碼（ID）。
     * <p>
     * 此實現確保具有相同 ID 的憑證物件具有相同的雜湊碼，
     * 這對於在集合類別（如 HashSet、HashMap）中正確運作是必要的。
     * 使用 {@link Long#hashCode(long)} 方法確保一致的雜湊值計算。
     * </p>
     *
     * @return 基於憑證 ID 的雜湊碼
     * @see #equals(Object)
     * @see Long#hashCode(long)
     */
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    /**
     * 判斷兩個憑證物件是否相等，基於憑證的唯一識別碼（ID）。
     * <p>
     * 此方法遵循 equals 方法的標準實現模式，確保具有相同 ID 的憑證物件
     * 被視為相等。這對於憑證的唯一性識別和緩存操作至關重要。
     * </p>
     *
     * @param o 用於比較的物件
     * @return 如果兩個憑證物件的 ID 相同則回傳 true，否則回傳 false
     * @see #hashCode()
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Token token = (Token) o;
        return id == token.id;
    }

    /**
     * 將憑證資訊轉換為可讀的字串表示形式。
     * <p>
     * 此方法將憑證的關鍵資訊組織成 HashMap 結構並轉換為字串，
     * 方便進行日誌記錄和除錯。出於安全考量，不包含敏感資訊
     * 如 JWT 憑證內容、重設密碼驗證碼等。
     * </p>
     *
     * @return 包含憑證基本資訊的字串表示，格式為 HashMap 的字串形式
     */
    @Override
    public String toString() {
        HashMap<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("id", id);
        tokenMap.put("userId", userId);
        return tokenMap.toString();
    }
}
