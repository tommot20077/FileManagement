package xyz.dowob.filemanagement.component.aspect;

import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.controller.exception.ExceptionController;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;

import java.lang.reflect.Method;

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
@Log4j2
@NoArgsConstructor
@SuppressWarnings("all")
public class LoggerAspect {

    /**
     * 定義 ServiceInterface 層切入點
     */
    @Pointcut("within(xyz.dowob.filemanagement.service..*)")
    public void serviceLayerPointcut () {
    }

    /**
     * 定義 Component 層切入點
     */
    @Pointcut("within(xyz.dowob.filemanagement.component..*)")
    public void componentLayerPointcut () {
    }

    /**
     * 定義 Controller 層切入點
     */
    @Pointcut("within(xyz.dowob.filemanagement.controller..*)")
    public void controllerLayerPointcut () {
    }

    /**
     * 環繞通知，用於記錄 Component 和 ServiceInterface 層的日誌
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
    public Object logAround (ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        try {
            Object result = joinPoint.proceed();

            if (result instanceof Mono<?>) {
                return ((Mono<?>) result).transformDeferredContextual((momo, context) -> {
                    ServerWebExchange exchange = context.getOrDefault(ServerWebExchange.class, null);
                    return momo.doOnNext(resp -> {
                        String value = processMethodSignature(method, resp);
                        logOperation(exchange, joinPoint, value, null);
                    }).doOnError(e -> {
                        logOperation(exchange, joinPoint, null, e);
                    });
                });
            } else if (result instanceof Flux<?>) {
                return ((Flux<?>) result).transformDeferredContextual((flux, context) -> {
                    ServerWebExchange exchange = context.getOrDefault(ServerWebExchange.class, null);
                    return flux.doOnNext(resp -> {
                        String value = processMethodSignature(method, resp);
                        logOperation(exchange, joinPoint, value, null);
                    }).doOnError(e -> {
                        logOperation(exchange, joinPoint, null, e);
                    });
                });
            } else {
                String value = processMethodSignature(method, result);
                logWithExchange(joinPoint, value, null);
                return result;
            }
        } catch (Throwable e) {
            logWithExchange(joinPoint, null, e);
            throw e;
        }
    }

    /**
     * 根據方法的是否有額外的標記注釋，來判斷是否在日誌中的返回值是否進行處理
     *
     * @param method 方法
     * @param result 方法的返回值
     *
     * @return String 處理後的日誌顯示的返回值
     */
    private String processMethodSignature (Method method, Object result) {
        boolean isSensitive = method.isAnnotationPresent(HideSensitive.class);
        if (isSensitive) {
            return "[隱藏敏感訊息]";
        }
        if (result == null) {
            return "無返回值";
        }
        return result.toString();
    }

    /**
     * 記錄操作信息
     *
     * @param exchange   伺服器交換協議對象
     * @param className  類名
     * @param methodName 方法名
     * @param result     返回值
     * @param error      錯誤
     */
    private void logOperation(ServerWebExchange exchange, ProceedingJoinPoint joinPoint, Object result, Throwable error) {
        String[] userNameAndUserId = getUserNameAndUserId(exchange);

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        if (error != null) {
            if (error instanceof ValidationException) {
                log.warn("請求者: {} {}| 所屬類: {} | 使用方法: {} | 警告訊息: {}",
                         userNameAndUserId[0],
                         userNameAndUserId[1] != null ? "(ID:" + userNameAndUserId[1] + ") " : "",
                         className,
                         methodName,
                         error.getMessage()
                );
            } else {
                log.error("請求者: {} {}| 所屬類: {} | 使用方法: {} | 錯誤訊息: {}",
                          userNameAndUserId[0],
                          userNameAndUserId[1] != null ? "(ID:" + userNameAndUserId[1] + ") " : "",
                          className,
                          methodName,
                          error.getMessage()
                );
            }
        } else {
            log.debug("請求者: {} {}| 所屬類: {} | 使用方法: {} | 返回值: {}",
                      userNameAndUserId[0],
                      userNameAndUserId[1] != null ? "(ID:" + userNameAndUserId[1] + ") " : "",
                      className,
                      methodName,
                      result
            );
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
    private void logWithExchange(ProceedingJoinPoint joinPoint, Object result, Throwable error) {
        CustomRequestContextHolder.getExchange().doOnNext(exchange -> {
            logOperation(exchange, joinPoint, result, error);
        }).switchIfEmpty(Mono.defer(() -> {
            logOperation(null, joinPoint, result, error);
            return Mono.empty();
        })).subscribe();
    }


    private String[] getUserNameAndUserId(ServerWebExchange exchange) {
        String requestUsername;
        String requsetUserId;
        if (exchange == null || exchange.getAttribute("username") == null || exchange.getAttribute("userId") == null) {
            requestUsername = "未知";
            requsetUserId = null;
        } else {
            requestUsername = (String) exchange.getAttribute("username");
            requsetUserId = ((Long) exchange.getAttribute("userId")).toString();
        }
        return new String[]{requestUsername, requsetUserId};
    }
}
