package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用戶限流器的枚舉類，用於定義用戶限流器
 * @author yuan
 * @program FileManagement
 * @ClassName UserLimiterEnum
 * @create 2025/1/20
 * @Version 1.0
 **/
@RequiredArgsConstructor
@Getter
public enum UserLimiterEnum {
    /**
     * 用戶上傳限流器
     */
    USER_UPLOAD_LIMITER("用戶上傳限流器", "當前已經達到最大上傳數量限制"),

    /**
     * 用戶登錄限流器
     */
    USER_LOGIN_LIMITER("用戶登錄限流器", "當前已經達到最大登錄次數限制");

    /**
     * 限流器描述
     */
    private final String description;

    /**
     * 限流器錯誤信息
     */
    private final String error;
}
