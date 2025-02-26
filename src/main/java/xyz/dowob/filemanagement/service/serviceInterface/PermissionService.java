package xyz.dowob.filemanagement.service.serviceInterface;

import jakarta.annotation.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.functionInterface.Permission;

import java.util.Collection;

/**
 * 權限服務接口，用戶可以通過實現 PermissionService 介面來自定義權限驗證邏輯
 * 通過調用 validateUserPermission 方法來驗證用戶是否有權限
 * 用戶可以通過設置不同的權限規則來實現不同的權限驗證
 *
 * @author yuan
 * @program FileManagement
 * @ClassName permissionService
 * @create 2025/2/25
 * @Version 1.0
 **/

public interface PermissionService<T> {
    /**
     * 驗證用戶是否有權限
     *
     * @param user   用戶
     * @param fileId 文件ID
     *
     * @return 是否有權限
     */
    Mono<T> validateUserPermission(User user, Long fileId, @Nullable Collection<Permission<T>> rules);

    /**
     * 驗證用戶是否有權限多個檔案Id
     *
     * @param user   用戶
     * @param fileId 文件ID
     *
     * @return 是否有權限
     */
    Flux<T> validateUserPermission(User user, Iterable<Long> fileId, @Nullable Collection<Permission<T>> rules);


    /**
     * 驗證用戶是否有權限，使用默認的權限規則
     *
     * @param user   用戶
     * @param fileId 文件ID
     *
     * @return 是否有權限
     */
    default Mono<T> validateUserPermission(User user, Long fileId) {
        return validateUserPermission(user, fileId, null);
    }

    /**
     * 驗證用戶是否有權限多個檔案Id，使用默認的權限規則
     *
     * @param user   用戶
     * @param fileId 文件ID
     *
     * @return 是否有權限
     */
    default Flux<T> validateUserPermission(User user, Iterable<Long> fileId) {
        return validateUserPermission(user, fileId, null);
    }
}
