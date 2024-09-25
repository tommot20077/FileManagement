package xyz.dowob.filemanagement.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 用於規範用戶重置密碼的數據傳輸對象
 * @author yuan
 * @program File-Management
 * @ClassName ResetPasswordDTO
 * @description
 * @create 2024-09-20 02:58
 * @Version 1.0
 **/
@Getter
@Setter
public class ResetPasswordDTO {
    /**
     * 用戶信箱
     */
    @Email(message = "信箱格式不正確")
    @NotBlank(message = "信箱不能為空")
    private String email;

    /**
     * 驗證碼
     */
    @NotBlank(message = "驗證碼不能為空")
    private String verificationCode;

    /**
     * 新密碼
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6,
          max = 30,
          message = "密碼長度需大於6,小於30")
    private String newPassword;

    /**
     * 確認新密碼
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6,
          max = 30,
          message = "確認密碼長度需大於6,小於30")
    private String confirmPassword;
}
