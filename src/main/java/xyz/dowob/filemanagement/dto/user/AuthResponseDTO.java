package xyz.dowob.filemanagement.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import xyz.dowob.filemanagement.annotation.HideSensitive;

/**
 * 用於定義用戶登錄響應的數據傳輸對象，用於返回 JWT 驗證令牌
 *
 * @author yuan
 * @program FileManagement
 * @ClassName AuthResponseDTO
 * @description
 * @create 2024-10-02 13:45
 * @Version 1.0
 **/
@Data
@AllArgsConstructor
public class AuthResponseDTO {
    /**
     * JWT 驗證令牌
     */
    @HideSensitive
    @JsonProperty("JWT 驗證令牌")
    private String jwtToken;

}
