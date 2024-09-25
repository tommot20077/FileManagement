package xyz.dowob.filemanagement.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用於規範用戶登錄的數據傳輸對象
 *
 * @author yuan
 * @program File-Management
 * @ClassName AuthRequestDTO
 * @description
 * @create 2024-09-16 02:53
 * @Version 1.0
 **/
@Data
public class AuthRequestDTO {
    /**
     * 用戶名稱
     */
    @NotBlank(message = "用户名不能為空")
    private String username;

    /**
     * 用戶密碼
     */
    @NotBlank(message = "密碼不能為空")
    private String password;
}
