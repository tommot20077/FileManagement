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
     */
    ADMIN(Set.of(PermissionEnum.WRITE,
                 PermissionEnum.READ,
                 PermissionEnum.DELETE,
                 PermissionEnum.UPLOAD,
                 PermissionEnum.DOWNLOAD,
                 PermissionEnum.SHARE,
                 PermissionEnum.UPDATE,
                 PermissionEnum.MANAGE)),
    /**
     * 高級用戶所擁有的權限: 寫入、讀取、刪除、上傳、下載、分享、更新
     */
    ADVANCED_USER(Set.of(PermissionEnum.WRITE,
                         PermissionEnum.READ,
                         PermissionEnum.DELETE,
                         PermissionEnum.UPLOAD,
                         PermissionEnum.DOWNLOAD,
                         PermissionEnum.UPDATE,
                         PermissionEnum.SHARE)),
    /**
     * 用戶所擁有的權限: 寫入、讀取、刪除、上傳、下載、更新
     */
    USER(Set.of(PermissionEnum.WRITE,
                PermissionEnum.READ,
                PermissionEnum.DELETE,
                PermissionEnum.UPLOAD,
                PermissionEnum.DOWNLOAD,
                PermissionEnum.UPDATE)),
    /**
     * 訪客所擁有的權限: 讀取
     */
    VISITOR(Set.of(PermissionEnum.READ));

    /**
     * 角色名稱以及對應的權限
     */
    private final Set<PermissionEnum> permissions;
}
