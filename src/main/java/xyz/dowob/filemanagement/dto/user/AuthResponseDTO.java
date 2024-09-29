package xyz.dowob.filemanagement.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import xyz.dowob.filemanagement.annotation.HideSensitive;

/**
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
    @HideSensitive
    @JsonProperty("JWT 驗證令牌")
    private String jwtToken;

}
