package xyz.dowob.filemanagement.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 用於規範用戶註冊的數據傳輸對象
 * @author yuan
 * @program File-Management
 * @ClassName RegisterDTO
 * @description
 * @create 2024-09-15 02:15
 * @Version 1.0
 **/
@Setter
@Getter
public class RegisterDTO {
    /**
     * 用戶名稱
     */
    @NotBlank(message = "用户名稱不能為空")
    @Size(min = 4,
          max = 30,
          message = "用户名稱長度需大於4,小於30")
    private String username;

    /**
     * 用戶密碼
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6,
          max = 30,
          message = "密碼長度需大於6,小於30")
    private String password;

    /**
     * 確認密碼
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6,
          max = 30,
          message = "確認密碼長度需大於6,小於30")
    private String confirmPassword;

    /**
     * 信箱
     */
    @Email(message = "郵箱格式不正確")
    @NotBlank(message = "郵箱不能為空")
    @Size(max = 50,
          message = "郵箱長度需小於50")
    private String email;
}
