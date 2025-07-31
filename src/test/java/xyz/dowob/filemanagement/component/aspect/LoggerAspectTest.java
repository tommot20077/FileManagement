package xyz.dowob.filemanagement.component.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/**
 * LoggerAspect 日誌切面的單元測試類別。
 *
 * <p>本測試類別旨在驗證日誌切面（LoggerAspect）的各種日誌記錄行為和異常處理機制。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("LoggerAspect 日誌切面測試")
@ExtendWith(MockitoExtension.class)
class LoggerAspectTest {

    @InjectMocks
    private LoggerAspect loggerAspect;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private Method mockMethod;

    private MockedStatic<CustomRequestContextHolder> customRequestContextHolderMockedStatic;

    private MockedStatic<LogUnity> logUnityMockedStatic;


    @BeforeEach
    void setUp() throws NoSuchMethodException {
        Method realMethod = TestClassWithAnnotations.class.getMethod("testMethodWithAnnotations");
        when(methodSignature.getMethod()).thenReturn(realMethod);

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        lenient().when(proceedingJoinPoint.getSignature().getName()).thenReturn("testMethod");

        customRequestContextHolderMockedStatic = Mockito.mockStatic(CustomRequestContextHolder.class);
        customRequestContextHolderMockedStatic.when(CustomRequestContextHolder::getExchange).thenReturn(Mono.just(exchange));

        logUnityMockedStatic = Mockito.mockStatic(LogUnity.class);
    }


    /**
     * 測試 Mono 返回結果的日誌記錄功能。
     *
     * 測試涵蓋的邏輯或場景說明：
     * 驗證當方法返回 Mono 類型時，日誌切面能正確記錄執行過程和返回值。
     *
     * 前置條件：
     * - Mock 方法簽名和切入點
     * - 設置 Mono 返回值
     * - 配置日誌工具模擬
     *
     * 測試步驟：
     * - 執行日誌切面方法
     * - 驗證 Mono 流程正確執行
     * - 確認日誌記錄被正確調用
     *
     * 預期結果：
     * - Mono 流成功完成並返回預期值
     * - 日誌工具被調用並記錄相關訊息
     */
    @Test
    @DisplayName("測試 Mono 返回結果 - 正常處理並記錄日誌")
    void logAround_withMonoResult() throws Throwable {
        String testResult = "test";
        Mono<String> monoResult = Mono.just(testResult);
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);


        Object result = loggerAspect.logAround(proceedingJoinPoint);

        StepVerifier.create((Mono<?>) result).expectNextMatches(actual -> actual.equals(testResult)).verifyComplete();
        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.info((ServerWebExchange) isNull(), eq("所屬類: %s | 使用方法: %s | 回傳值: %s"), anyString(), anyString(), anyString()));
    }


    @AfterEach
    void tearDown() {
        customRequestContextHolderMockedStatic.close();
        logUnityMockedStatic.close();
    }


    /**
     * 測試 Flux 返回結果的日誌記錄功能。
     *
     * 測試涵蓋的邏輯或場景說明：
     * 驗證當方法返回 Flux 類型時，日誌切面能正確記錄執行過程和返回值。
     *
     * 前置條件：
     * - Mock 方法簽名和切入點
     * - 設置 Flux 返回值
     * - 配置日誌工具模擬
     *
     * 測試步驟：
     * - 執行日誌切面方法
     * - 驗證 Flux 流程正確執行
     * - 確認日誌記錄被正確調用
     *
     * 預期結果：
     * - Flux 流成功完成並返回預期值序列
     * - 日誌工具被調用並記錄相關訊息
     */
    @Test
    @DisplayName("測試 Flux 返回結果 - 正常處理並記錄日誌")
    void logAround_withFluxResult() throws Throwable {
        String testResult1 = "test1";
        String testResult2 = "test2";
        Flux<String> fluxResult = Flux.just(testResult1, testResult2);
        when(proceedingJoinPoint.proceed()).thenReturn(fluxResult);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        StepVerifier
                .create((Flux<?>) result)
                .expectNextMatches(actual -> actual.equals(testResult1))
                .expectNextMatches(actual -> actual.equals(testResult2))
                .verifyComplete();

        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.info((ServerWebExchange) isNull(), eq("所屬類: %s | 使用方法: %s | 回傳值: %s"), anyString(), anyString(), anyString()));
    }


    /**
     * 測試普通對象返回結果的日誌記錄功能。
     *
     * 測試涵蓋的邏輯或場景說明：
     * 驗證當方法返回普通對象時，日誌切面能正確記錄執行過程和返回值。
     *
     * 前置條件：
     * - Mock 方法簽名和切入點
     * - 設置普通對象返回值
     * - 配置日誌工具和請求上下文模擬
     *
     * 測試步驟：
     * - 執行日誌切面方法
     * - 驗證方法正常執行
     * - 確認日誌記錄被正確調用
     *
     * 預期結果：
     * - 方法成功執行並返回預期對象
     * - 日誌工具被調用並記錄相關訊息
     */
    @Test
    @DisplayName("測試普通對象返回結果 - 正常處理並記錄日誌")
    void logAround_withObjectResult() throws Throwable {
        String testResult = "test";
        when(proceedingJoinPoint.proceed()).thenReturn(testResult);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        assertEquals(testResult, result);
        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.info(eq(exchange), eq("所屬類: %s | 使用方法: %s | 回傳值: %s"), anyString(), anyString(), anyString()));
    }


    /**
     * 測試方法拋出異常時的日誌記錄功能。
     *
     * 測試涵蓋的邏輯或場景說明：
     * 驗證當被攔截的方法拋出異常時，日誌切面能正確記錄錯誤訊息。
     *
     * 前置條件：
     * - Mock 方法簽名和切入點
     * - 設置方法拋出 RuntimeException
     * - 配置日誌工具模擬
     *
     * 測試步驟：
     * - 執行日誌切面方法並期望異常
     * - 驗證異常被正確拋出
     * - 確認錯誤日誌被記錄
     *
     * 預期結果：
     * - RuntimeException 被成功拋出
     * - 日誌工具被調用並記錄錯誤訊息
     */
    @Test
    @DisplayName("測試方法拋出異常 - 記錄錯誤日誌")
    void logAround_withException() throws Throwable {
        RuntimeException exception = new RuntimeException("Test exception");
        when(proceedingJoinPoint.proceed()).thenThrow(exception);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});

        assertThrows(RuntimeException.class, () -> loggerAspect.logAround(proceedingJoinPoint));

        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.error(eq(exchange), anyString()));
    }

    /**
     * 測試 ValidationException 的特殊日誌處理。
     *
     * 測試涵蓋的邏輯或場景說明：
     * 驗證當方法拋出 ValidationException 時，日誌切面使用調試級別記錄而非錯誤級別。
     *
     * 前置條件：
     * - Mock 方法簽名和切入點
     * - 設置方法拋出 ValidationException
     * - 配置日誌工具模擬
     *
     * 測試步驟：
     * - 執行日誌切面方法並期望異常
     * - 驗證 ValidationException 被正確拋出
     * - 確認使用調試級別記錄日誌
     *
     * 預期結果：
     * - ValidationException 被成功拋出
     * - 日誌工具的 debug 方法被調用
     */
    @Test
    @DisplayName("測試 ValidationException - 記錄調試級別日誌")
    void logAround_withValidationException() throws Throwable {
        ValidationException exception = new ValidationException(ValidationException.ErrorCode.FORBIDDEN);
        when(proceedingJoinPoint.proceed()).thenThrow(exception);

        assertThrows(ValidationException.class, () -> loggerAspect.logAround(proceedingJoinPoint));

        verify(proceedingJoinPoint).proceed();
        // ValidationException 只會在 debug 級別才記錄
        logUnityMockedStatic.verify(() -> LogUnity.debug(eq(exchange), anyString()));
    }

    @Test
    @DisplayName("測試空返回值 - 顯示無回傳值訊息")
    void logAround_withNullResult() throws Throwable {
        when(proceedingJoinPoint.proceed()).thenReturn(null);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        assertEquals(null, result);
        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.info(eq(exchange), eq("所屬類: %s | 使用方法: %s | 回傳值: %s"), anyString(), anyString(), eq("無回傳值")));
    }

    @Test
    @DisplayName("測試 Mono 錯誤流 - 正確傳播異常")
    void logAround_withMonoError() throws Throwable {
        RuntimeException exception = new RuntimeException("Mono error");
        Mono<String> monoResult = Mono.error(exception);
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        StepVerifier.create((Mono<?>) result)
                .expectError(RuntimeException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試 Flux 錯誤流 - 正確傳播異常")
    void logAround_withFluxError() throws Throwable {
        RuntimeException exception = new RuntimeException("Flux error");
        Flux<String> fluxResult = Flux.error(exception);
        when(proceedingJoinPoint.proceed()).thenReturn(fluxResult);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        StepVerifier.create((Flux<?>) result)
                .expectError(RuntimeException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試空 Flux 流 - 正常完成並記錄日誌")
    void logAround_withEmptyFlux() throws Throwable {
        Flux<String> fluxResult = Flux.empty();
        when(proceedingJoinPoint.proceed()).thenReturn(fluxResult);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        StepVerifier.create((Flux<?>) result)
                .verifyComplete();

        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.info((ServerWebExchange) isNull(), eq("所屬類: %s | 使用方法: %s | 回傳值: %s"), anyString(), anyString(), anyString()));
    }

    @Test
    @DisplayName("測試空 Mono 流 - 正常完成")
    void logAround_withEmptyMono() throws Throwable {
        Mono<String> monoResult = Mono.empty();
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        StepVerifier.create((Mono<?>) result)
                .verifyComplete();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試請求上下文為空 - 使用 null exchange 記錄日誌")
    void logAround_withRequestContextEmpty() throws Throwable {
        String testResult = "test";
        when(proceedingJoinPoint.proceed()).thenReturn(testResult);
        customRequestContextHolderMockedStatic.when(CustomRequestContextHolder::getExchange)
                .thenReturn(Mono.empty());

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        assertEquals(testResult, result);
        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.info((ServerWebExchange) isNull(), eq("所屬類: %s | 使用方法: %s | 回傳值: %s"), anyString(), anyString(), anyString()));
    }

    @Test
    @DisplayName("測試長字符串返回值 - 自動截斷並添加省略號")
    void logAround_withLongStringResult() throws Throwable {
        String longString = "a".repeat(400);
        when(proceedingJoinPoint.proceed()).thenReturn(longString);
        Method longMethod = TestClassWithLongAnnotation.class.getMethod("testMethodWithLongAnnotation");
        when(methodSignature.getMethod()).thenReturn(longMethod);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        assertEquals(longString, result);
        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.info(eq(exchange), eq("所屬類: %s | 使用方法: %s | 回傳值: %s"), anyString(), anyString(), contains("..."))); // 驗證有截斷
    }

    @Test
    @DisplayName("測試敏感信息方法 - 隱藏返回值內容")
    void logAround_withSensitiveMethod() throws Throwable {
        String sensitiveResult = "sensitive data";
        when(proceedingJoinPoint.proceed()).thenReturn(sensitiveResult);
        Method sensitiveMethod = TestClassWithSensitiveAnnotation.class.getMethod("testMethodWithSensitiveAnnotation");
        when(methodSignature.getMethod()).thenReturn(sensitiveMethod);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        assertEquals(sensitiveResult, result);
        verify(proceedingJoinPoint).proceed();
        // 敏感方法會隱藏返回值
        logUnityMockedStatic.verify(() -> LogUnity.info(eq(exchange), eq("所屬類: %s | 使用方法: %s | 回傳值: %s"), anyString(), anyString(), eq("[隱藏敏感訊息]")));
    }

    @Test
    @DisplayName("測試跳過記錄的方法 - 不記錄任何日誌")
    void logAround_withSkipRecordMethod() throws Throwable {
        String testResult = "test";
        when(proceedingJoinPoint.proceed()).thenReturn(testResult);
        Method skipMethod = TestClassWithSkipAnnotation.class.getMethod("testMethodWithSkipAnnotation");
        when(methodSignature.getMethod()).thenReturn(skipMethod);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        assertEquals(testResult, result);
        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verifyNoInteractions();
    }

    @Test
    @DisplayName("測試調試級別方法 - 使用 debug 級別記錄日誌")
    void logAround_withDebugLevelMethod() throws Throwable {
        String testResult = "test";
        when(proceedingJoinPoint.proceed()).thenReturn(testResult);
        Method debugMethod = TestClassWithDebugAnnotation.class.getMethod("testMethodWithDebugAnnotation");
        when(methodSignature.getMethod()).thenReturn(debugMethod);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        assertEquals(testResult, result);
        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.debug(eq(exchange), eq("所屬類: %s | 使用方法: %s | 回傳值: %s"), anyString(), anyString(), anyString()));
    }

    @Test
    @DisplayName("測試錯誤級別方法 - 使用 error 級別記錄日誌")
    void logAround_withErrorLevelMethod() throws Throwable {
        String testResult = "test";
        when(proceedingJoinPoint.proceed()).thenReturn(testResult);
        Method errorMethod = TestClassWithErrorAnnotation.class.getMethod("testMethodWithErrorAnnotation");
        when(methodSignature.getMethod()).thenReturn(errorMethod);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        assertEquals(testResult, result);
        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.error(eq(exchange), eq("所屬類: %s | 使用方法: %s | 回傳值: %s"), anyString(), anyString(), anyString()));
    }

    @Test
    @DisplayName("測試複雜參數的異常情況 - 記錄參數和錯誤訊息")
    void logAround_withComplexArgs() throws Throwable {
        String testResult = "test";
        Object[] complexArgs = {"arg1", null, "a".repeat(600), 123};
        RuntimeException exception = new RuntimeException("Complex args exception");
        when(proceedingJoinPoint.proceed()).thenThrow(exception);
        when(proceedingJoinPoint.getArgs()).thenReturn(complexArgs);

        assertThrows(RuntimeException.class, () -> loggerAspect.logAround(proceedingJoinPoint));

        verify(proceedingJoinPoint).proceed();
        // 非 ValidationException 會記錄到 error 級別並調用 getArgs()
        verify(proceedingJoinPoint).getArgs();
        logUnityMockedStatic.verify(() -> LogUnity.error(eq(exchange), anyString()));
    }

    private static class TestClassWithAnnotations {
        @RecordLevel(LogLevelEnum.INFO)
        @SkipRecord(LogLevelEnum.DEBUG)
        @HideOverLength
        public void testMethodWithAnnotations() {
        }
    }

    private static class TestClassWithLongAnnotation {
        @HideOverLength
        @RecordLevel(LogLevelEnum.INFO)
        public void testMethodWithLongAnnotation() {
        }
    }

    private static class TestClassWithSensitiveAnnotation {
        @HideSensitive
        @RecordLevel(LogLevelEnum.INFO)
        public void testMethodWithSensitiveAnnotation() {
        }
    }

    private static class TestClassWithSkipAnnotation {
        @SkipRecord(LogLevelEnum.INFO)
        public void testMethodWithSkipAnnotation() {
        }
    }

    private static class TestClassWithDebugAnnotation {
        @RecordLevel(LogLevelEnum.DEBUG)
        public void testMethodWithDebugAnnotation() {
        }
    }

    private static class TestClassWithErrorAnnotation {
        @RecordLevel(LogLevelEnum.ERROR)
        public void testMethodWithErrorAnnotation() {
        }
    }
}
