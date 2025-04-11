package xyz.dowob.filemanagement.component.strategy;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.annotation.UserLimiterType;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;

import java.util.EnumMap;
import java.util.List;

/**
 * 用戶限流器策略，用於根據不同的限流器類型返回不同的限流器
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserLimiterStrategy
 * @create 2025/1/20
 * @Version 1.0
 **/
@Component
public class UserLimiterStrategy {
    /**
     * 用於存儲不同類型的用戶限流器
     */
    private final EnumMap<UserLimiterEnum, UserLimiter> userLimiterEnumMap;

    /**
     * 用於構造 UserLimiterStrategy 對象
     *
     * @param userLimiters 用戶限流器列表
     */
    public UserLimiterStrategy(List<UserLimiter> userLimiters) {
        userLimiterEnumMap = new EnumMap<>(UserLimiterEnum.class);

        for (UserLimiter userLimiter : userLimiters) {
            UserLimiterType userLimiterType = AnnotatedElementUtils.findMergedAnnotation(userLimiter.getClass(), UserLimiterType.class);
            if (userLimiterType != null) {
                userLimiterEnumMap.put(userLimiterType.value(), userLimiter);
            }
        }
    }


    /**
     * 根據用戶限流器類型獲取用戶限流器
     *
     * @param userLimiterEnum 用戶限流器類型
     *
     * @return 用戶限流器
     */
    @SkipRecord
    public UserLimiter getUserLimiter(UserLimiterEnum userLimiterEnum) {
        return userLimiterEnumMap.get(userLimiterEnum);
    }
}
