package xyz.dowob.filemanagement.component.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.customenum.PermissionEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.util.Optional;

/**
 * 權限切面 AOP，用於動態攔截具有 {@link xyz.dowob.filemanagement.annotation.RequirePermission} 標記的方法。
 *
 * <p>此切面實現了基於方法級別的反應式權限驗證邏輯，確保只有具有足夠權限的使用者可以存取特定方法。</p>
 *
 * <p>主要特性：
 * <ul>
 *   <li>支援 WebFlux 的反應式編程模型</li>
 *   <li>支援 {@link reactor.core.publisher.Mono} 和 {@link reactor.core.publisher.Flux} 回傳類型</li>
 *   <li>若使用者權限不足，拋出 {@link xyz.dowob.filemanagement.exception.ValidationException}</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Aspect
@Component
@SuppressWarnings("all")
public class PermissionAspect {
    /**
     * 定義權限檢查的環繞通知，攔截標記有 {@link RequirePermission} 注釋的方法。
     *
     * <p>此方法為反應式權限驗證的核心邏輯，支援 {@link Mono} 和 {@link Flux} 回傳類型。
     * 在方法執行前進行權限檢查，確保使用者具有足夠的權限才能繼續執行目標方法。</p>
     *
     * @param joinPoint 切入點，表示被攔截的方法執行點
     * @param requirePermission 標記在方法上的權限要求注釋
     * @return 原方法的執行結果，包裝後的反應式結果
     * @throws UnsupportedOperationException 當方法回傳類型不是 Mono 或 Flux 時拋出
     * @throws RuntimeException 當權限檢查過程中發生異常時拋出
     */
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) {
        PermissionEnum[] requiredPermissions = requirePermission.value();
        try {
            Object result = joinPoint.proceed();

            if (result instanceof Mono<?> monoResult) {
                return monoResult.transformDeferredContextual((mono, context) -> {
                    ServerWebExchange exchange = context.getOrDefault(ServerWebExchange.class, null);
                    return Mono.defer(() -> checkUserPermission(requiredPermissions)).then(mono);
                });
            } else if (result instanceof Flux<?> fluxResult) {
                return fluxResult.transformDeferredContextual((flux, context) -> {
                    ServerWebExchange exchange = context.getOrDefault(ServerWebExchange.class, null);
                    return Mono.defer(() -> checkUserPermission(requiredPermissions)).thenMany(flux);
                });
            } else {
                throw new UnsupportedOperationException("RequirePermission 只支援 Mono 或 Flux");
            }
        } catch (Throwable e) {
            throw new RuntimeException("權限檢查失敗", e);
        }
    }

    /**
     * 檢查當前使用者的權限，確保其符合存取方法的最低權限要求。
     *
     * <p>此方法透過 {@link ReactiveSecurityContextHolder} 獲取當前使用者的身份驗證資訊，
     * 並從中提取角色資訊進行權限比對。若使用者未通過身份驗證或無法獲取角色資訊，
     * 則預設為 {@link RoleEnum#VISITOR} 角色。</p>
     *
     * @param requiredPermissions 執行方法所需的權限列表
     * @return {@link Mono}&lt;{@link Void}&gt; 表示權限驗證的非阻塞結果，若驗證成功則為空 Mono
     * @throws ValidationException 當使用者權限不足時透過 Mono.error 拋出
     */
    private Mono<Void> checkUserPermission(PermissionEnum[] requiredPermissions) {
        return ReactiveSecurityContextHolder.getContext().map(securityContext -> {
            Optional<String> roleNameOptional = securityContext
                    .getAuthentication()
                    .getAuthorities()
                    .stream()
                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                    .findFirst();
            return roleNameOptional.map(roleName -> {
                return RoleEnum.valueOf(roleName);
            }).orElse(RoleEnum.VISITOR);
        }).switchIfEmpty(Mono.just(RoleEnum.VISITOR)).flatMap(roleEnum -> {
            if (roleEnum.hasPermissions(requiredPermissions)) {
                return Mono.empty();
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.FORBIDDEN));
        });
    }
}
