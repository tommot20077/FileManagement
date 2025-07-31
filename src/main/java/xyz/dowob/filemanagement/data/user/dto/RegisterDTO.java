package xyz.dowob.filemanagement.data.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 使用者註冊的資料傳輸物件（DTO）。
 *
 * <p>此類別封裝新使用者註冊所需的必要資訊，包含使用者名稱、密碼、確認密碼及電子信箱。
 * 所有欄位均具有驗證約束，確保資料的有效性與安全性。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Setter
@Getter
public class RegisterDTO {
    /**
     * 使用者名稱。
     *
     * <p>用於身份認證的唯一識別符，長度必須在 4-30 字元之間。
     * 系統將檢查用戶名稱的唯一性與格式規範。</p>
     *
     * @see jakarta.validation.constraints.NotBlank
     * @see jakarta.validation.constraints.Size
     */
    @NotBlank(message = "用户名稱不能為空")
    @Size(min = 4,
          max = 30,
          message = "用户名稱長度需大於4,小於30")
    private String username;

    /**
     * 使用者密碼。
     *
     * <p>用於身份驗證的機密資訊，長度必須在 6-30 字元之間。
     * 出於安全考量，密碼將經過加密處理後儲存。</p>
     *
     * @see jakarta.validation.constraints.NotBlank
     * @see jakarta.validation.constraints.Size
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6,
          max = 30,
          message = "密碼長度需大於6,小於30")
    private String password;

    /**
     * 確認密碼。
     *
     * <p>用於驗證使用者輸入密碼的一致性，防止密碼輸入錯誤。
     * 長度約束與密碼欄位相同。</p>
     *
     * @see jakarta.validation.constraints.NotBlank
     * @see jakarta.validation.constraints.Size
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6,
          max = 30,
          message = "確認密碼長度需大於6,小於30")
    private String confirmPassword;

    /**
     * 電子信箱地址。
     *
     * <p>用於帳號驗證、密碼重設及系統通知的聯絡方式。
     * 電子信箱必須符合標準格式與長度限制。</p>
     *
     * @see jakarta.validation.constraints.Email
     * @see jakarta.validation.constraints.NotBlank
     * @see jakarta.validation.constraints.Size
     */
    @Email(message = "郵箱格式不正確")
    @NotBlank(message = "郵箱不能為空")
    @Size(max = 50,
          message = "郵箱長度需小於50")
    private String email;
}
