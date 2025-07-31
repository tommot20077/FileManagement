package xyz.dowob.filemanagement.data.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 使用者電子信箱的資料傳輸物件（DTO）。
 *
 * <p>此類別封裝使用者電子信箱地址，用於處理需要信箱驗證的操作。
 * 包含密碼重設申請、帳號驗證等功能，確保信箱格式的正確性。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
public class UserEmailDTO {
    /**
     * 使用者電子信箱地址。
     *
     * <p>必須為有效的電子信箱格式，用於接收系統通知與驗證信件。
     * 系統將驗證信箱格式的正確性與存在性。</p>
     *
     * @see jakarta.validation.constraints.NotBlank
     * @see jakarta.validation.constraints.Email
     */
    @NotBlank(message = "信箱不能為空")
    @Email(message = "信箱格式不正確")
    private String email;
}
