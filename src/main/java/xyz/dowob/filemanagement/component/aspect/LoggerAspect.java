package xyz.dowob.filemanagement.component.aspect;

import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 日誌切面，實現應用程式各層（Component、Service、Controller）的橫向日誌記錄。
 *
 * <p>本切面利用 AOP 技術，攔截並記錄方法的執行過程，包括：
 * <ul>
 *   <li>方法執行時的上下文資訊</li>
 *   <li>方法回傳值</li>
 *   <li>異常處理與錯誤記錄</li>
 * </ul>
 * </p>
 *
 * <p>特點：
 * <ul>
 *   <li>支援 WebFlux 反應式編程模型</li>
 *   <li>自動處理 {@link reactor.core.publisher.Mono} 和 {@link reactor.core.publisher.Flux} 回傳類型</li>
 *   <li>根據客製化標記（如 {@link xyz.dowob.filemanagement.annotation.SkipRecord}）控制日誌輸出</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Aspect
@Component
@NoArgsConstructor
@SuppressWarnings("all")
public class LoggerAspect {
    /**
     * 日誌記錄器，用於記錄 LoggerAspect 的執行資訊和錯誤訊息。
     */
    private static final Logger log = LogManager.getLogger(LoggerAspect.class);


    /**
     * 定義 Service 層切入點，攔截所有 service 包下的方法執行。
     *
     * <p>此切入點將攔截 {@code xyz.dowob.filemanagement.service} 包及其子包下的所有方法。</p>
     */
    @Pointcut("within(xyz.dowob.filemanagement.service..*)")
    public void serviceLayerPointcut() {
    }


    /**
     * 定義 Component 層切入點，攔截所有 component 包下的方法執行。
     *
     * <p>此切入點將攔截 {@code xyz.dowob.filemanagement.component} 包及其子包下的所有方法。</p>
     */
    @Pointcut("within(xyz.dowob.filemanagement.component..*)")
    public void componentLayerPointcut() {
    }


    /**
     * 定義 Controller 層切入點，攔截所有 controller 包下的方法執行。
     *
     * <p>此切入點將攔截 {@code xyz.dowob.filemanagement.controller} 包及其子包下的所有方法。</p>
     */
    @Pointcut("within(xyz.dowob.filemanagement.controller..*)")
    public void controllerLayerPointcut() {
    }


    /**
     * 通過 AOP 環繞通知攔截並記錄方法執行的日誌資訊。
     *
     * <p>處理三種不同回傳類型的方法：
     * <ul>
     *   <li>回傳 {@link reactor.core.publisher.Mono} 的非阻塞響應方法</li>
     *   <li>回傳 {@link reactor.core.publisher.Flux} 的資料流方法</li>
     *   <li>回傳普通同步對象的傳統方法</li>
     * </ul>
     * </p>
     *
     * <p>日誌處理特性：
     * <ul>
     *   <li>轉換 Mono/Flux 中的錯誤資訊</li>
     *   <li>交由 {@link xyz.dowob.filemanagement.controller.exception.ExceptionController} 處理異常</li>
     *   <li>記錄方法執行上下文、回傳值和可能的異常資訊</li>
     * </ul>
     * </p>
     *
     * @param joinPoint 方法執行的切入點，提供方法執行的上下文資訊
     * @return 原方法的執行結果，包裝後回傳
     * @throws Throwable 如果方法執行過程中發生任何異常
     */
    @Around("serviceLayerPointcut() || componentLayerPointcut() || controllerLayerPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        try {
            Object result = joinPoint.proceed();

            if (result instanceof Mono<?>) {
                return ((Mono<?>) result).transformDeferredContextual((momo, context) -> {
                    ServerWebExchange exchange = context.getOrDefault(ServerWebExchange.class, null);
                    return momo.doOnNext(resp -> {
                        LogInfo info = processMethodSignature(method, resp);
                        logOperation(exchange, joinPoint, info, null);
                    }).doOnError(e -> {
                        logOperation(exchange, joinPoint, null, e);
                    });
                });
            } else if (result instanceof Flux<?>) {
                return ((Flux<?>) result).transformDeferredContextual((flux, context) -> {
                    ServerWebExchange exchange = context.getOrDefault(ServerWebExchange.class, null);
                    AtomicInteger count = new AtomicInteger(0);
                    return flux.doOnNext(item -> count.incrementAndGet()).doOnComplete(() -> {
                        String value = String.format("Flux<%s> 內元素數量: %d", method.getReturnType().getSimpleName(), count.get());
                        logOperation(exchange, joinPoint, processMethodSignature(method, value), null);
                    }).doOnError(error -> {
                        LogInfo info = new LogInfo(LogLevelEnum.ERROR, "Flux 內處理失敗，已處理元素數量: " + count.get());
                        logOperation(exchange, joinPoint, info, error);
                    });
                });
            } else {
                LogInfo info = processMethodSignature(method, result);
                logWithExchange(joinPoint, info, null);
                return result;
            }
        } catch (Throwable e) {
            logWithExchange(joinPoint, null, e);
            throw e;
        }
    }


    /**
     * 根據方法上的注釋標記處理日誌訊息的顯示格式和內容。
     *
     * <p>處理標記的優先順序如下：</p>
     * <ol>
     *   <li>檢查 {@link RecordLevel} 標記，決定日誌級別（預設為 {@link LogLevelEnum#TRACE}）</li>
     *   <li>檢查 {@link SkipRecord} 標記，若符合條件則跳過記錄（優先處理方法標記，其次處理類別標記）</li>
     *   <li>處理 null 回傳值，顯示「無回傳值」訊息</li>
     *   <li>檢查 {@link HideSensitive} 標記，隱藏敏感訊息</li>
     *   <li>檢查 {@link HideOverLength} 標記，自動截斷超過 300 字元的訊息</li>
     * </ol>
     *
     * @param method 被執行的方法對象
     * @param result 方法的回傳值
     * @return 處理後的日誌訊息對象，包含日誌級別和顯示內容；若不需記錄則回傳 null
     */
    private LogInfo processMethodSignature(Method method, Object result) {
        Class<?> declaringClass = method.getDeclaringClass();
        LogLevelEnum logLevelEnum = LogLevelEnum.TRACE;

        if (method.isAnnotationPresent(RecordLevel.class)) {
            logLevelEnum = method.getAnnotation(RecordLevel.class).value();
        } else if (declaringClass != null && declaringClass.isAnnotationPresent(RecordLevel.class)) {
            logLevelEnum = declaringClass.getAnnotation(RecordLevel.class).value();
        }

        if (!log.isEnabled(logLevelEnum.getLevel())) {
            return null;
        }

        LogLevelEnum[] skipRecordLevel = null;
        if (method.isAnnotationPresent(SkipRecord.class)) {
            skipRecordLevel = method.getAnnotation(SkipRecord.class).value();
        } else if (declaringClass.isAnnotationPresent(SkipRecord.class)) {
            skipRecordLevel = declaringClass.getAnnotation(SkipRecord.class).value();
        }

        if (skipRecordLevel != null) {
            for (LogLevelEnum skipLevel : skipRecordLevel) {
                if (logLevelEnum.getLevel().intLevel() >= skipLevel.getLevel().intLevel()) {
                    return null;
                }
            }
        }

        if (result == null) {
            return new LogInfo(logLevelEnum, "無回傳值");
        }

        boolean isSensitive = method.isAnnotationPresent(HideSensitive.class);
        boolean isOverLength = method.isAnnotationPresent(HideOverLength.class) || declaringClass.isAnnotationPresent(HideOverLength.class);

        if (isSensitive) {
            return new LogInfo(logLevelEnum, "[隱藏敏感訊息]");
        }

        if (isOverLength && result.toString().length() > 300) {
            return new LogInfo(logLevelEnum, result.toString().substring(0, 300) + "...");
        }
        return new LogInfo(logLevelEnum, result.toString());
    }


    /**
     * 記錄方法執行的操作資訊，包括類別名稱、方法名稱、回傳值和異常資訊。
     *
     * <p>此方法會根據不同情況進行日誌記錄：</p>
     * <ul>
     *   <li>若有異常發生，記錄錯誤訊息和相關的執行上下文</li>
     *   <li>若正常執行，根據 {@link LogInfo} 的級別輸出對應的日誌</li>
     * </ul>
     *
     * @param exchange 伺服器請求交換對象，包含請求上下文資訊
     * @param joinPoint 方法執行的切入點對象
     * @param info 包含日誌級別和訊息內容的日誌資訊對象
     * @param error 方法執行過程中發生的異常，若無異常則為 null
     */
    private void logOperation(ServerWebExchange exchange, ProceedingJoinPoint joinPoint, LogInfo info, Throwable error) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        if (error != null) {
            boolean isValidationException = error instanceof ValidationException;
            if (isValidationException && log.isDebugEnabled()) {
                String format = "所屬類: %s | 使用方法: %s | 警告訊息: %s";
                Object[] args = new Object[]{className, methodName, error.getMessage()};

                LogUnity.debug(exchange, String.format(format, args));
            } else if (!isValidationException && log.isErrorEnabled()) {
                Object[] argsArray = joinPoint.getArgs();
                String formattedArgs = (argsArray == null || argsArray.length == 0) ? "" : Arrays
                        .stream(argsArray)
                        .map(arg -> arg != null ? arg.toString() : "null")
                        .map(argStr -> argStr.length() > 500 ? argStr.substring(0, 500) + "..." : argStr)
                        .collect(Collectors.joining(", "));

                String format = "所屬類: %s | 使用方法: %s | 傳入參數: %s | 錯誤訊息: %s";
                Object[] args = new Object[]{className, methodName, formattedArgs, error.getMessage(), error};
                LogUnity.error(exchange, String.format(format, args));
            }
        } else {
            if (info == null) {
                return;
            }
            String format = "所屬類: %s | 使用方法: %s | 回傳值: %s";
            Object[] args = new Object[]{className, methodName, info.message()};

            switch (info.logLevel()) {
                case TRACE -> LogUnity.trace(exchange, format, args);
                case DEBUG -> LogUnity.debug(exchange, format, args);
                case INFO -> LogUnity.info(exchange, format, args);
                case WARN -> LogUnity.warn(exchange, format, args);
                case ERROR -> LogUnity.error(exchange, format, args);
                case FATAL -> LogUnity.fatal(exchange, format, args);
            }
        }
    }


    /**
     * 處理非反應式方法的日誌輸出，嘗試獲取 ServerWebExchange 對象進行上下文日誌記錄。
     *
     * <p>此方法主要用於處理傳統同步方法的日誌記錄，會嘗試從 {@link CustomRequestContextHolder} 獲取
     * ServerWebExchange 對象。若獲取成功則包含請求上下文資訊，否則進行無上下文的日誌記錄。</p>
     *
     * @param joinPoint 方法執行的切入點對象
     * @param result 包含日誌級別和訊息內容的日誌資訊對象
     * @param error 方法執行過程中發生的異常，若無異常則為 null
     */
    private void logWithExchange(ProceedingJoinPoint joinPoint, LogInfo result, Throwable error) {
        CustomRequestContextHolder.getExchange().doOnNext(exchange -> {
            logOperation(exchange, joinPoint, result, error);
        }).switchIfEmpty(Mono.defer(() -> {
            logOperation(null, joinPoint, result, error);
            return Mono.empty();
        })).subscribe();
    }


    /**
     * 日誌資訊記錄類，用於儲存日誌級別和訊息內容。
     *
     * @param logLevel 日誌級別，決定日誌輸出的重要性等級
     * @param message 日誌訊息內容
     */
    record LogInfo(LogLevelEnum logLevel, String message) {
    }

    /**
     * 使用者請求資訊類，用於儲存請求相關的識別資訊。
     *
     * <p>此類別目前未被使用，保留作為未來擴展請求追蹤功能的基礎結構。</p>
     */
    private class UserRequestInfo {
        /**
         * 請求者的識別名稱。
         */
        private String identify;

        /**
         * 請求的唯一識別碼。
         */
        private String requestId;

        /**
         * 發起請求的 IP 位址。
         */
        private String requestIp;
    }
}
