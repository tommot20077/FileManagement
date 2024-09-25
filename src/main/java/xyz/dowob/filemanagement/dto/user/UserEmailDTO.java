package xyz.dowob.filemanagement.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用於接收用戶信箱的數據傳輸對象
 * @author yuan
 * @program File-Management
 * @ClassName UserEmailDTO
 * @description
 * @create 2024-09-20 16:01
 * @Version 1.0
 **/
@Data
public class UserEmailDTO {
    /**
     * 用戶信箱
     */
    @NotBlank(message = "信箱不能為空")
    @Email(message = "信箱格式不正確")
    private String email;
}
