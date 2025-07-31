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
 * 基於策略模式的用戶限流器管理服務實作。
 *
 * <p>本類別使用 EnumMap 存儲不同類型的限流器實例，提供高效的限流器查詢和管理。
 * 在初始化時會自動掃描所有標記了 {@link UserLimiterType} 註解的限流器實作，
 * 並根據註解值將其註冊到對應的枚舉類型中。重複類型的限流器註冊會拋出例外。</p>
 *
 * <p>限流器的選擇基於 {@link UserLimiterEnum} 枚舉值，支援執行時動態切換限流策略。
 * 當查詢不存在的限流器類型時，返回 null 值。新增限流器類型只需要實作 {@link UserLimiter} 
 * 介面並使用適當的 {@link UserLimiterType} 註解標記。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
public class UserLimiterStrategy {
    /**
     * 存儲不同限流器類型與實作映射的 EnumMap。
     * 
     * <p>使用 EnumMap 確保高效的查詢性能和類型安全，鍵為 {@link UserLimiterEnum} 枚舉值，
     * 值為對應的 {@link UserLimiter} 實作實例。</p>
     */
    private final EnumMap<UserLimiterEnum, UserLimiter> userLimiterEnumMap;

    /**
     * 建構限流器策略管理服務，自動註冊所有可用的限流器實作。
     *
     * <p>透過依賴注入接收所有 {@link UserLimiter} 實作的 Bean 列表，
     * 掃描每個實作類別上的 {@link UserLimiterType} 註解，並將其註冊到對應的枚舉類型映射中。
     * 如果發現重複的限流器類型註冊，將拋出 {@link IllegalArgumentException}。</p>
     *
     * @param userLimiters 系統中所有 UserLimiter 實作的 Bean 列表，可為 null
     * @throws IllegalArgumentException 當存在重複的限流器類型註冊時
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
     * 根據指定的限流器類型獲取對應的限流器實作。
     *
     * <p>從內部 EnumMap 中查詢並返回指定類型的限流器實例。
     * 此方法提供 O(1) 時間複雜度的查詢性能。</p>
     *
     * @param userLimiterEnum 要查詢的限流器類型枚舉，不可為 null
     * @return 對應類型的限流器實作，如果該類型未註冊則返回 null
     */
    public UserLimiter getUserLimiter(UserLimiterEnum userLimiterEnum) {
        return userLimiterEnumMap.get(userLimiterEnum);
    }
}