package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用於定義憑證類型
 *
 * @author yuan
 * @program File-Management
 * @ClassName TransfersStatusEnum
 * @description
 * @create 2024-012-11 17:09
 * @Version 1.0
 */

@Getter
@RequiredArgsConstructor
public enum TokenEnum {
    /**
     * JWT 憑證
     */

    JWT_AUTHORIZATION_TOKEN("JWT 憑證"),
    /**
     * 重製密碼憑證
     */
    RESET_PASSWORD_TOKEN("重製密碼憑證");

    /**
     * 憑證類型
     */
    private final String tokenType;

}
