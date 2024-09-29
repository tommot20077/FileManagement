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
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.controller.exception.WebExceptionController;

import java.lang.reflect.Field;
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
    public void serviceLayerPointcut() {}

    /**
     * 定義 Component 層切入點
     */
    @Pointcut("within(xyz.dowob.filemanagement.component..*)")
    public void componentLayerPointcut() {}

    /**
     * 定義 Controller 層切入點
     */
    @Pointcut("within(xyz.dowob.filemanagement.controller..*)")
    public void controllerLayerPointcut() {}

    /**
     * 環繞通知，用於記錄 Component 和 ServiceInterface 層的日誌
     * 當業務方法執行或發生異常時，記錄請求者、所屬類、使用方法、返回值等信息到日誌中
     * 區分2種情況：
     * 1. 方法返回值為 Mono 或 Flux，因為這兩種類型是非阻塞的，所以需要特別處理
     * 需要轉換Mono中的錯誤信息並提取出來，最後交由錯誤控制器處理 {@link WebExceptionController}
     * 2. 方法返回值為普通對象，可以直接紀錄並返回
     *
     * @param joinPoint 切入點
     *
     * @return Object 方法的返回值
     */
    @Around("serviceLayerPointcut() || componentLayerPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        ServerWebExchange exchange = getCurrentServerWebExchange();
        String requestUsername;

        if (exchange != null) {
            WebSession session = exchange.getSession().block();
            if (session != null) {
                requestUsername = session.getAttribute("username");
            } else {
                requestUsername = "No Session";
            }
        } else {
            requestUsername = "Unknown";
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        try {
            Object result = joinPoint.proceed();

            if (result instanceof Mono<?>) {
                return ((Mono<?>) result).doOnSuccess(resp -> {
                    String value = processMethodSignature(method, resp);
                    infoLog(requestUsername, className, methodName, value);
                }).doOnError(e -> {
                    createErrorLog(requestUsername, className, methodName, e);
                });
            } else if (result instanceof Flux<?>) {
                return ((Flux<?>) result).doOnNext(resp -> {
                    String value = processMethodSignature(method, resp);
                    infoLog(requestUsername, className, methodName, value);
                }).doOnError(e -> {
                    createErrorLog(requestUsername, className, methodName, e);
                });
            } else {
                String value = processMethodSignature(method, result);
                infoLog(requestUsername, className, methodName, value);
                return result;
            }
        } catch (Throwable e) {
            createErrorLog(requestUsername, className, methodName, e);
            throw e;
        }
    }

    /**
     * 獲取當前的 ServerWebExchange
     *
     * @return ServerWebExchange 當前的 ServerWebExchange
     */
    private ServerWebExchange getCurrentServerWebExchange() {
        try {
            return (ServerWebExchange) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        } catch (IllegalStateException e) {
            return null;
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
    private String processMethodSignature(Method method, Object result) {
        boolean isSensitive = method.isAnnotationPresent(HideSensitive.class);
        if (isSensitive) {
            return "[隱藏敏感訊息]";
        }
        if (result == null) {
            return "無返回值";
        }
        return result.toString();
    }

    private void infoLog(String requestUsername, String className, String methodName, Object result) {
        log.debug("請求者: {} | 所屬類: {} | 使用方法: {} | 返回值: {}", requestUsername, className, methodName, result);
    }

    private void createErrorLog(String requestUsername, String className, String methodName, Throwable e) {
        log.error("請求者: {} | 所屬類: {} | 使用方法: {} | 錯誤訊息: {}", requestUsername, className, methodName, e.getMessage());
    }
}
