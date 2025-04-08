package xyz.dowob.filemanagement.component.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.customenum.PermissionEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.exception.ValidationException;

/**
 * 權限切面AOP，用於檢測用戶是否有權限訪問某個方法
 * 而這個方法是由 RequirePermission 註解標記的
 * 當用戶沒有權限時，會拋出 ValidationException
 *
 * @author yuan
 * @program FileManagement
 * @ClassName PermissionAspect
 * @create 2025/2/3
 * @Version 1.0
 **/
@SuppressWarnings("all")
@Aspect
@Component
public class PermissionAspect {

    /**
     * 定義切入點，當方法上有 @RequirePermission 註解時進行攔截
     * 若此方法為Mono或Flux，則會在返回結果之前檢查用戶的權限
     * 否則因為無法獲取 ServerWebExchange 對象，無法檢查權限
     *
     * @param joinPoint         切入點
     * @param requirePermission RequirePermission 註解
     *
     * @return Object 返回結果
     */
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) {
        PermissionEnum[] requiredPermissions = requirePermission.value();
        try {
            Object result = joinPoint.proceed();

            if (result instanceof Mono<?> monoResult) {
                return monoResult.transformDeferredContextual((mono, context) -> {
                    ServerWebExchange exchange = context.getOrDefault(ServerWebExchange.class, null);
                    return Mono.defer(() -> checkUserPermission(exchange, requiredPermissions)).then(mono);
                });
            } else if (result instanceof Flux<?> fluxResult) {
                return fluxResult.transformDeferredContextual((flux, context) -> {
                    ServerWebExchange exchange = context.getOrDefault(ServerWebExchange.class, null);
                    return Mono.defer(() -> checkUserPermission(exchange, requiredPermissions)).thenMany(flux);
                });
            } else {
                throw new UnsupportedOperationException("RequirePermission 只支援 Mono 或 Flux");
            }
        } catch (Throwable e) {
            throw new RuntimeException("權限檢查失敗", e);
        }
    }


    /**
     * 檢查用戶的權限
     * 如果用戶沒有權限，則拋出 ValidationException
     *
     * @param exchange            ServerWebExchange 對象
     * @param requiredPermissions 所需的權限
     *
     * @return Mono<Void>
     */
    private Mono<Void> checkUserPermission(ServerWebExchange exchange, PermissionEnum[] requiredPermissions) {
        if (exchange == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED));
        }

        return Mono
                .justOrEmpty(((RoleEnum) exchange.getAttribute("role")))
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED)))
                .flatMap(role -> {
                    if (role.hasPermissions(requiredPermissions)) {
                        return Mono.empty();
                    }
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.FORBIDDEN));
                });
    }
}
