package xyz.dowob.filemanagement.data.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.dowob.filemanagement.annotation.HideSensitive;

/**
 * 使用者身份認證響應的資料傳輸物件（DTO）。
 *
 * <p>此類別封裝認證成功後的回應資訊，主要包含 JWT 驗證權杖。
 * 權杖用於後續請求的身份驗證與授權，包含敏感資訊並已標記為隱藏。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    /**
     * JWT 驗證權杖。
     *
     * <p>此欄位包含經過簽署的 JWT 權杖，用於使用者的身份認證與授權。
     * 權杖包含使用者身份資訊及權限資料，屬於敏感資訊。</p>
     *
     * @see xyz.dowob.filemanagement.annotation.HideSensitive
     */
    @HideSensitive
    @JsonProperty("token")
    private String jwtToken;


}
