package xyz.dowob.filemanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import xyz.dowob.filemanagement.customenum.ByteEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * 基於 Spring Data R2DBC 的用戶實體類，對應資料庫中的 users 表。
 * <p>
 * 此實體類提供檔案管理系統中用戶身份驗證、授權和存儲配額管理功能。
 * 整合 Spring Security 認證機制，支援角色基礎的存取控制和存儲空間限制。
 * 每個用戶具有唯一識別碼、基本身份資訊（用戶名、密碼、電子郵件）、
 * 系統角色以及相關的存儲配額設定。
 * <p>
 * 用戶密碼在 JSON 序列化時會被自動忽略以確保安全性。
 * 存儲限制根據用戶角色自動設定預設值，管理員角色通常擁有更高的存儲配額。
 * 用戶權限透過 {@code getAuthorities()} 方法與 Spring Security 整合，
 * 支援基於角色的方法級安全控制。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see RoleEnum
 * @see GrantedAuthority
 * @see SimpleGrantedAuthority
 */

@Table(name = "users")
@Getter
@Setter
public class User {
    /**
     * 用戶的唯一識別碼，作為資料庫主鍵。
     * <p>
     * 此欄位在資料庫中自動生成，用於唯一標識每個用戶實體。
     * 在進行用戶比較、雜湊計算和關聯查詢時作為主要依據。
     */
    @Id
    private Long id;

    /**
     * 用戶登入系統時使用的唯一用戶名稱。
     * <p>
     * 此欄位必須在系統中保持唯一性，用於用戶身份識別和登入驗證。
     * 通常由用戶在註冊時自行設定，後續可能允許修改。
     */
    private String username;

    /**
     * 用戶登入密碼的加密儲存值。
     * <p>
     * 此欄位儲存經過加密處理的密碼，通常使用 BCrypt 等安全雜湊演算法。
     * 透過 {@code @JsonIgnore} 註解確保在 JSON 序列化時不會洩露密碼資訊，
     * 提供額外的安全保護層級。
     */
    @JsonIgnore
    private String password;

    /**
     * 用戶的電子郵件地址，用於通知和密碼重置功能。
     * <p>
     * 此欄位在系統中必須保持唯一性，作為用戶身份驗證的替代方式。
     * 用於發送系統通知、密碼重置連結和其他重要的帳戶相關訊息。
     */
    private String email;

    /**
     * 用戶在系統中的角色等級，決定存取權限和功能範圍。
     * <p>
     * 角色類型由 {@link RoleEnum} 定義，預設為 {@code USER}。
     * 不同角色擁有不同的系統權限和存儲配額限制，
     * 管理員角色通常擁有更高的權限和更大的存儲空間。
     */
    private RoleEnum role = RoleEnum.USER;

    /**
     * 用戶可使用的最大存儲空間限制，單位為位元組。
     * <p>
     * 此值根據用戶角色自動設定預設限制，可由管理員調整。
     * 當用戶嘗試上傳檔案時，系統會檢查是否超過此限制。
     * 預設值來自於用戶角色的 {@code getDefaultStorageLimit()} 方法。
     */
    @Column("storage_limit")
    private Long storageLimit = RoleEnum.USER.getDefaultStorageLimit();

    /**
     * 用戶目前已使用的存儲空間大小，單位為位元組。
     * <p>
     * 此值會在用戶上傳或刪除檔案時動態更新，
     * 用於計算剩餘可用存儲空間和實施存儲配額控制。
     * 初始值為 0，表示新用戶尚未使用任何存儲空間。
     */
    @Column("used_storage")
    private Long usedStorage = 0L;


    /**
     * 計算用戶物件的雜湊碼，基於用戶的唯一識別碼（ID）。
     * <p>
     * 此實現確保具有相同 ID 的用戶物件具有相同的雜湊碼，
     * 這對於在集合類別（如 HashSet、HashMap）中正確運作是必要的。
     * 遵循 equals-hashCode 合約的要求。
     * </p>
     *
     * @return 基於用戶 ID 的雜湊碼
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }


    /**
     * 判斷兩個用戶物件是否相等，基於用戶的唯一識別碼（ID）。
     * <p>
     * 此方法遵循 equals 方法的標準實現模式：
     * <ul>
     *   <li>反射性：{@code x.equals(x)} 回傳 true</li>
     *   <li>對稱性：{@code x.equals(y)} 與 {@code y.equals(x)} 結果相同</li>
     *   <li>傳遞性：如果 {@code x.equals(y)} 且 {@code y.equals(z)}，則 {@code x.equals(z)}</li>
     *   <li>一致性：多次呼叫結果保持一致</li>
     *   <li>非空性：{@code x.equals(null)} 回傳 false</li>
     * </ul>
     * </p>
     *
     * @param o 用於比較的物件
     * @return 如果兩個使用者物件的 ID 相同則回傳 true，否則回傳 false
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
        User user = (User) o;
        return id.equals(user.id);
    }


    /**
     * 將用戶資訊轉換為可讀的字串表示形式。
     * <p>
     * 此方法將用戶的關鍵資訊組織成 HashMap 結構並轉換為字串，
     * 方便進行日誌記錄、除錯和資料展示。存儲相關的數值會自動
     * 轉換為可讀的格式（如 1GB、500MB 等）。
     * </p>
     * <p>
     * 注意：出於安全考量，密碼不會包含在字串表示中。
     * </p>
     *
     * @return 包含用戶資訊的字串表示，格式為 HashMap 的字串形式
     * @see ByteEnum#toReadableSize(long)
     */
    @Override
    public String toString() {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("id", id);
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("role", role);
        userMap.put("storageLimit", ByteEnum.toReadableSize(storageLimit));
        userMap.put("usedStorage", ByteEnum.toReadableSize(usedStorage));
        return userMap.toString();
    }


    /**
     * 獲取用戶的權限集合，用於 Spring Security 認證授權。
     * <p>
     * 此方法將用戶的角色（{@link RoleEnum}）轉換為 Spring Security 
     * 可識別的權限格式（ROLE_前綴），例如：USER 角色轉換為 ROLE_USER。
     * </p>
     * <p>
     * 權限格式遵循 Spring Security 的標準約定，以 "ROLE_" 為前綴，
     * 後接角色名稱的大寫形式。
     * </p>
     *
     * @return 包含用戶權限的集合，通常包含一個基於角色的權限
     * @see GrantedAuthority
     * @see SimpleGrantedAuthority  
     * @see RoleEnum
     */
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}