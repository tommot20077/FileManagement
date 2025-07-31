package xyz.dowob.filemanagement.data.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 使用者身份認證請求的資料傳輸物件（DTO）。
 *
 * <p>此類別封裝使用者登入時的基本認證資訊，用於在應用程式不同層次間傳遞登入憑證。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.data.user.dto.AuthResponseDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {
    /**
     * 使用者登入帳號名稱。
     *
     * <p>此欄位用於身份認證，必須為非空字串。系統將驗證輸入的使用者名稱是否有效。</p>
     *
     * @see jakarta.validation.constraints.NotBlank
     */
    @NotBlank(message = "用戶名稱不能為空")
    private String username;

    /**
     * 使用者登入密碼。
     *
     * <p>此欄位存儲使用者用於身份驗證的密碼，必須為非空字串。出於安全考慮，系統不會直接儲存明文密碼。</p>
     *
     * @see jakarta.validation.constraints.NotBlank
     */
    @NotBlank(message = "密碼不能為空")
    private String password;
}
