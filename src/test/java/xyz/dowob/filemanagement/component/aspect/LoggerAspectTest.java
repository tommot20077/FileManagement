package xyz.dowob.filemanagement.component.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature().getName()).thenReturn("testMethod");

        customRequestContextHolderMockedStatic = Mockito.mockStatic(CustomRequestContextHolder.class);
        customRequestContextHolderMockedStatic.when(CustomRequestContextHolder::getExchange).thenReturn(Mono.just(exchange));

        logUnityMockedStatic = Mockito.mockStatic(LogUnity.class);
    }


    @Test
    void logAround_withMonoResult() throws Throwable {
        String testResult = "test";
        Mono<String> monoResult = Mono.just(testResult);
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);


        Object result = loggerAspect.logAround(proceedingJoinPoint);

        StepVerifier.create((Mono<?>) result).expectNextMatches(actual -> actual.equals(testResult)).verifyComplete();
        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.info((ServerWebExchange) isNull(), eq("所屬類: %s | 使用方法: %s | 返回值: %s"), anyString(), anyString(), anyString()));
    }


    @AfterEach
    void tearDown() {
        customRequestContextHolderMockedStatic.close();
        logUnityMockedStatic.close();
    }


    @Test
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
        logUnityMockedStatic.verify(() -> LogUnity.info((ServerWebExchange) isNull(), eq("所屬類: %s | 使用方法: %s | 返回值: %s"), anyString(), anyString(), anyString()));
    }


    @Test
    void logAround_withObjectResult() throws Throwable {
        String testResult = "test";
        when(proceedingJoinPoint.proceed()).thenReturn(testResult);

        Object result = loggerAspect.logAround(proceedingJoinPoint);

        assertEquals(testResult, result);
        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.info(eq(exchange), eq("所屬類: %s | 使用方法: %s | 返回值: %s"), anyString(), anyString(), anyString()));
    }


    @Test
    void logAround_withException() throws Throwable {
        RuntimeException exception = new RuntimeException("Test exception");
        when(proceedingJoinPoint.proceed()).thenThrow(exception);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});

        assertThrows(RuntimeException.class, () -> loggerAspect.logAround(proceedingJoinPoint));

        verify(proceedingJoinPoint).proceed();
        logUnityMockedStatic.verify(() -> LogUnity.error(eq(exchange), anyString()));
    }

    private static class TestClassWithAnnotations {
        @RecordLevel(LogLevelEnum.INFO)
        @SkipRecord(LogLevelEnum.DEBUG)
        @HideOverLength
        public void testMethodWithAnnotations() {
        }
    }
}
