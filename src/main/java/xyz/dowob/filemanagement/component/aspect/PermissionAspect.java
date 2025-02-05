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
 * @author yuan
 * @program FileManagement
 * @ClassName PermissionAspect
 * @create 2025/2/3
 * @Version 1.0
 **/
@Aspect
@Component
public class PermissionAspect {

    @SuppressWarnings("all")
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
