package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.UserLimiterEnum;

import java.lang.annotation.*;

/**
 * 標記用戶限流器的類型，用於區分不同的用戶限流器
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserLimiterType
 * @create 2025/1/20
 * @Version 1.0
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UserLimiterType {
    /**
     * 使用 UserLimiterEnum 來標記用戶限流器的類型
     *
     * @return UserLimiterEnum
     */
    UserLimiterEnum value();
}
