package xyz.dowob.filemanagement.functionInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;

/**
 * 權限規則接口，用戶可以通過實現 PermissionRule 介面來自定義權限規則
 *
 * @author yuan
 * @program FileManagement
 * @ClassName Permissions
 * @create 2025/2/25
 * @Version 1.0
 **/

@FunctionalInterface
public interface Permission<T> {
    /**
     * 驗證用戶是否有權限
     *
     * @param user 用戶
     * @param t    驗證對象
     *
     * @return 是否驗證時有不通過的規則
     */
    Mono<Throwable> check(User user, T t);
}
