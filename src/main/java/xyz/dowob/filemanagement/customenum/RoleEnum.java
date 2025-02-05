package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * 角色枚舉類
 *
 * @author yuan
 * @program File-Management
 * @ClassName RoleEnum
 * @description
 * @create 2024-09-14 17:09
 * @Version 1.0
 **/


@Getter
@RequiredArgsConstructor
public enum RoleEnum {
    /**
     * 管理員所擁有的權限: 寫入、讀取、刪除、上傳、下載、分享、更新、管理
     * 管理員的存儲限制為無限制
     */
    ADMIN(Set.of(PermissionEnum.WRITE,
                 PermissionEnum.READ,
                 PermissionEnum.DELETE,
                 PermissionEnum.UPLOAD,
                 PermissionEnum.DOWNLOAD,
                 PermissionEnum.SHARE,
                 PermissionEnum.UPDATE, PermissionEnum.MANAGE
    ), ByteEnum.convertToByte(-1L)),
    /**
     * 高級用戶所擁有的權限: 寫入、讀取、刪除、上傳、下載、分享、更新
     * 高級用戶的存儲限制為 50GB
     */
    ADVANCED_USER(Set.of(PermissionEnum.WRITE,
                         PermissionEnum.READ,
                         PermissionEnum.DELETE,
                         PermissionEnum.UPLOAD,
                         PermissionEnum.DOWNLOAD,
                         PermissionEnum.UPDATE, PermissionEnum.SHARE
    ), ByteEnum.convertToByte(50L)),
    /**
     * 用戶所擁有的權限: 寫入、讀取、刪除、上傳、下載、更新
     * 用戶的存儲限制為 15GB
     */
    USER(Set.of(PermissionEnum.WRITE,
                PermissionEnum.READ,
                PermissionEnum.DELETE,
                PermissionEnum.UPLOAD,
                PermissionEnum.DOWNLOAD, PermissionEnum.UPDATE
    ), ByteEnum.convertToByte(15L)),
    /**
     * 訪客所擁有的權限: 讀取
     * 訪客的存儲限制為 0GB
     */
    VISITOR(Set.of(PermissionEnum.READ), 0L),

    /**
     * 匿名用戶所擁有的權限: 無
     * 匿名用戶的存儲限制為 0GB
     */
    ANONYMOUS(Set.of(), 0L),
    ;

    /**
     * 角色名稱以及對應的權限
     */
    private final Set<PermissionEnum> permissions;
    private final Long defaultStorageLimit;

    /**
     * 判斷是否擁有指定的權限
     *
     * @param requiredPermissions 權限
     *
     * @return 是否擁有權限
     */
    public boolean hasPermissions(PermissionEnum[] requiredPermissions) {
        for (PermissionEnum requiredPermission : requiredPermissions) {
            if (!permissions.contains(requiredPermission)) {
                return false;
            }
        }
        return true;
    }
}
