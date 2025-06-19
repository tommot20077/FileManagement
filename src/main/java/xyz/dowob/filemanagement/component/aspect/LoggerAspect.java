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
import xyz.dowob.filemanagement.controller.exception.ExceptionController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 用於記錄 Component 和 ServiceInterface 層的日誌切面
 * 當業務方法執行或發生異常時，記錄請求者、所屬類、使用方法、返回值等信息到日誌中
 *
 * @author yuan
 * @program File-Management
 * @ClassName LoggerAspect
 * @description
 * @create 2024-09-23 17:19
 * @Version 1.0
 **/
@Aspect
@Component
@NoArgsConstructor
@SuppressWarnings("all")
public class LoggerAspect {
    /**
     * 日誌的記錄器
     */
    private static final Logger log = LogManager.getLogger(LoggerAspect.class);


    /**
     * 定義 ServiceInterface 層切入點
     */
    @Pointcut("within(xyz.dowob.filemanagement.service..*)")
    public void serviceLayerPointcut() {
    }


    /**
     * 定義 Component 層切入點
     */
    @Pointcut("within(xyz.dowob.filemanagement.component..*)")
    public void componentLayerPointcut() {
    }


    /**
     * 定義 Controller 層切入點
     */
    @Pointcut("within(xyz.dowob.filemanagement.controller..*)")
    public void controllerLayerPointcut() {
    }


    /**
     * 環繞通知，用於記錄 Component 、 Service 和 Controller 層的日誌
     * 當業務方法執行或發生異常時，記錄請求者、所屬類、使用方法、返回值等信息到日誌中
     * 區分2種情況：
     * 1. 方法返回值為 Mono 或 Flux，因為這兩種類型是非阻塞的，所以需要特別處理
     * 需要轉換Mono中的錯誤信息並提取出來，最後交由錯誤控制器處理 {@link ExceptionController}
     * 2. 方法返回值為普通對象，可以直接紀錄並返回
     *
     * @param joinPoint 切入點
     *
     * @return Object 方法的返回值
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
     * 根據方法的是否有額外的標記注釋，來判斷是否在日誌中的返回值是否進行處理
     * 處理標記的順序如下:
     * 1. 獲取是否具有日誌級別的標記 {@link RecordLevel}，如果有則取代預設值 {@link LogLevelEnum#TRACE}
     * - 會檢查獲取到的日誌級別是否包含在當前日誌級別中，如果不符合則直接返回 null
     * 2. 是否有 {@link SkipRecord} 標記，並檢查是否有與當前日誌級別相同的跳過標記，此為第一優先處理，會覆蓋其他標記
     * - 優先處理方法的標記
     * - 其次處理類的標記
     * 3. 返回值為 null 時，則返回無返回值的訊息
     * 4. 是否有 {@link HideSensitive} 標記，隱藏敏感訊息
     * 5. 是否有 {@link HideOverLength} 標記，自動截斷過長的訊息
     * - 會將訊息截斷為300個字元，並在最後加上省略號
     * 最後返回處理後的日誌訊息
     *
     * @param method 方法
     * @param result 方法的返回值
     *
     * @return String 處理後的日誌顯示的返回值
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
                if (logLevelEnum == skipLevel) {
                    return null;
                }
            }
        }

        if (result == null) {
            return new LogInfo(logLevelEnum, "無返回值");
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
     * 記錄操作信息，包括請求ID、請求者名稱、所屬類、使用方法、返回值等信息
     * 若處理結果有發生異常，則記錄錯誤訊息
     * 並依照照日誌紀錄 {@link LogInfo} 的級別進行日誌輸出
     *
     * @param exchange   伺服器交換協議對象
     * @param className  類名
     * @param methodName 方法名
     * @param info       日誌紀錄訊息
     * @param error      錯誤
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
            String format = "所屬類: %s | 使用方法: %s | 返回值: %s";
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
     * 此方法為處理一般狀況下的日誌輸出，因為無法直接獲取 ServerWebExchange 對象
     * 所以需要進行判斷，如果為空則直接輸出日誌，否則獲取 ServerWebExchange 對象進行日誌輸出
     *
     * @param joinPoint 切入點
     * @param result    返回值
     * @param error     錯誤
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
     * 日誌信息類，用於存儲日誌級別和日誌訊息
     */
    record LogInfo(LogLevelEnum logLevel, String message) {
    }

    /**
     * 用戶請求信息類，用於存儲請求ID、請求者名稱和請求IP
     */
    private class UserRequestInfo {
        /**
         * 請求者的辨識名稱
         */
        private String identify;

        /**
         * 請求ID
         */
        private String requestId;

        /**
         * 請求IP
         */
        private String requestIp;
    }
}
