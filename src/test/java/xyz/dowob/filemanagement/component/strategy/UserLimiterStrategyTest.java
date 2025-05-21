package xyz.dowob.filemanagement.component.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import xyz.dowob.filemanagement.annotation.UserLimiterType;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UserLimiterStrategy 邏輯處理測試")
@ExtendWith(MockitoExtension.class)
class UserLimiterStrategyTest {

    @Mock
    private UserLimiter userLimiter1;

    @Mock
    private UserLimiter userLimiter2;

    private UserLimiterStrategy userLimiterStrategy;

    @Test
    @DisplayName("測試構造函數初始化 - 正常案例")
    void constructorInitialization_NormalCase() {
        List<UserLimiter> limiters = Arrays.asList(userLimiter1, userLimiter2);
        assertDoesNotThrow(() -> new UserLimiterStrategy(limiters));
    }

    @Test
    @DisplayName("獲取限流器 - 存在對應類型應成功")
    void getExistingLimiter_ShouldSuccess() {
        try (MockedStatic<AnnotatedElementUtils> utilities = mockStatic(AnnotatedElementUtils.class)) {
            UserLimiterType handler = mock(UserLimiterType.class);
            when(handler.value()).thenReturn(UserLimiterEnum.USER_UPLOAD_LIMITER);
            utilities.when(() -> AnnotatedElementUtils.findMergedAnnotation(userLimiter1.getClass(), UserLimiterType.class)).thenReturn(handler);

            List<UserLimiter> limiters = List.of(userLimiter1);
            userLimiterStrategy = new UserLimiterStrategy(limiters);

            assertNotNull(userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER));
        }
    }

    @Test
    @DisplayName("獲取限流器 - 不存在的類型應返回null")
    void getNonExistLimiter_ShouldReturnNull() {
        List<UserLimiter> limiters = Arrays.asList(userLimiter1, userLimiter2);
        userLimiterStrategy = new UserLimiterStrategy(limiters);

        assertNull(userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER));
    }

    @Test
    @DisplayName("重複限流器類型 - 構造時應拋出IllegalArgumentException")
    void duplicateLimiterType_ShouldThrowException() {
        try (MockedStatic<AnnotatedElementUtils> mockedUtils = mockStatic(AnnotatedElementUtils.class)) {
            UserLimiterType annotationMock = mock(UserLimiterType.class);
            when(annotationMock.value()).thenReturn(UserLimiterEnum.USER_UPLOAD_LIMITER);

            mockedUtils
                    .when(() -> AnnotatedElementUtils.findMergedAnnotation(userLimiter1.getClass(), UserLimiterType.class))
                    .thenReturn(annotationMock);
            mockedUtils
                    .when(() -> AnnotatedElementUtils.findMergedAnnotation(userLimiter2.getClass(), UserLimiterType.class))
                    .thenReturn(annotationMock);

            List<UserLimiter> limiters = Arrays.asList(userLimiter1, userLimiter2);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new UserLimiterStrategy(limiters));

            assertTrue(exception.getMessage().contains("用戶限流器類型重複"));
            assertTrue(exception.getMessage().contains(UserLimiterEnum.USER_UPLOAD_LIMITER.toString()));
        }
    }
}
