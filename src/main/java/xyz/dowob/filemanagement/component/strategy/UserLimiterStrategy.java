package xyz.dowob.filemanagement.component.strategy;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.annotation.UserLimiterType;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;
import xyz.dowob.filemanagement.unity.LogUnity;

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
 */
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
        if (userLimiters == null) {
            return;
        }

        for (UserLimiter userLimiter : userLimiters) {
            UserLimiterType userLimiterTypeAnnotation = AnnotatedElementUtils.findMergedAnnotation(userLimiter.getClass(), UserLimiterType.class);

            if (userLimiterTypeAnnotation != null) {
                UserLimiterEnum typeEnum = userLimiterTypeAnnotation.value();
                LogUnity.trace("檢查限制器: %s, 註解: %s", userLimiter.getClass().getName(), typeEnum);

                if (userLimiterEnumMap.containsKey(typeEnum)) {
                    String msg = String.format("用戶限流器類型重複: %s ， %s 嘗試註冊，但已經被 %s 註冊",
                                               typeEnum,
                                               userLimiter.getClass().getName(),
                                               userLimiterEnumMap.get(typeEnum).getClass().getName()
                    );
                    throw new IllegalArgumentException(msg);
                } else {
                    userLimiterEnumMap.put(typeEnum, userLimiter);
                    LogUnity.debug("註冊限制器: %s, 類型: %s", userLimiter.getClass().getName(), typeEnum);
                }
            } else {
                LogUnity.debug("限制器: %s 沒有註解 UserLimiterType，不會被註冊", userLimiter.getClass().getName());
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
    public UserLimiter getUserLimiter(UserLimiterEnum userLimiterEnum) {
        return userLimiterEnumMap.get(userLimiterEnum);
    }
}