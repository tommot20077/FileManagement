package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName UserLimiterEnum
 * @create 2025/1/20
 * @Version 1.0
 **/
@RequiredArgsConstructor
@Getter
public enum UserLimiterEnum {
    USER_UPLOAD_LIMITER("用戶上傳限流器", "當前已經達到最大上傳數量限制");

    private final String description;
    private final String error;
}
