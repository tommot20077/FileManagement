package xyz.dowob.filemanagement.data.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 使用者密碼重設的資料傳輸物件（DTO）。
 *
 * <p>此類別封裝使用者密碼重設操作所需的資訊，包含電子信箱、驗證碼及新密碼。
 * 所有安全驗證步驟都必須通過，確保密碼重設操作的安全性。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@Setter
public class ResetPasswordDTO {
    /**
     * 使用者電子信箱地址。
     *
     * <p>用於驗證密碼重設權限的電子信箱，必須與帳號註冊時使用的信箱一致。
     * 系統將驗證信箱格式的正確性。</p>
     *
     * @see jakarta.validation.constraints.Email
     * @see jakarta.validation.constraints.NotBlank
     */
    @Email(message = "信箱格式不正確")
    @NotBlank(message = "信箱不能為空")
    private String email;

    /**
     * 驗證碼。
     *
     * <p>由系統發送至使用者信箱的一次性驗證碼，用於確認密碼重設權限。
     * 驗證碼具有時效性，必須在有效期內使用。</p>
     *
     * @see jakarta.validation.constraints.NotBlank
     */
    @NotBlank(message = "驗證碼不能為空")
    private String verificationCode;

    /**
     * 新密碼。
     *
     * <p>使用者指定的新密碼，長度必須在 6-30 字元之間。
     * 密碼將經過加密處理後儲存，取代原有密碼。</p>
     *
     * @see jakarta.validation.constraints.NotBlank
     * @see jakarta.validation.constraints.Size
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6,
          max = 30,
          message = "密碼長度需大於6,小於30")
    private String newPassword;

    /**
     * 確認新密碼。
     *
     * <p>用於驗證新密碼輸入的一致性，防止用戶輸入錯誤。
     * 必須與新密碼欄位完全相同。</p>
     *
     * @see jakarta.validation.constraints.NotBlank
     * @see jakarta.validation.constraints.Size
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6,
          max = 30,
          message = "確認密碼長度需大於6,小於30")
    private String confirmPassword;
}
