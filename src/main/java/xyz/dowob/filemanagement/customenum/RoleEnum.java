package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * 系統使用者角色權限列舉。
 * <p>定義檔案管理系統中不同角色的權限集合和儲存空間限制。包含管理員、高級使用者、
 * 一般使用者、訪客和匿名使用者等角色，每個角色對應不同的操作權限和預設儲存配額。
 * 角色權限採用累進式設計，高階角色包含低階角色的所有權限。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */


@Getter
@RequiredArgsConstructor
public enum RoleEnum {
    /**
     * 系統管理員角色。
     * 擁有完整的系統管理權限，包括檔案的寫入、讀取、刪除、上傳、下載、分享、更新和系統管理功能。
     * 儲存空間無限制，可執行所有檔案操作和系統維護功能。
     */
    ADMIN(Set.of(PermissionEnum.WRITE,
                 PermissionEnum.READ,
                 PermissionEnum.DELETE,
                 PermissionEnum.UPLOAD,
                 PermissionEnum.DOWNLOAD,
                 PermissionEnum.SHARE,
                 PermissionEnum.UPDATE, PermissionEnum.MANAGE
    ), -1L),

    /**
     * 高級使用者角色。
     * 擁有進階檔案操作權限，包括檔案的寫入、讀取、刪除、上傳、下載、分享和更新功能。
     * 預設儲存空間限制為 50GB，適用於需要大容量儲存和完整檔案管理功能的使用者。
     */
    ADVANCED_USER(Set.of(PermissionEnum.WRITE,
                         PermissionEnum.READ,
                         PermissionEnum.DELETE,
                         PermissionEnum.UPLOAD,
                         PermissionEnum.DOWNLOAD,
                         PermissionEnum.UPDATE, PermissionEnum.SHARE
    ), ByteEnum.convertToByte(50L)),

    /**
     * 一般使用者角色。
     * 擁有基本檔案操作權限，包括檔案的寫入、讀取、刪除、上傳、下載和更新功能。
     * 預設儲存空間限制為 15GB，適用於一般個人檔案管理需求。
     */
    USER(Set.of(PermissionEnum.WRITE,
                PermissionEnum.READ,
                PermissionEnum.DELETE,
                PermissionEnum.UPLOAD,
                PermissionEnum.DOWNLOAD, PermissionEnum.UPDATE
    ), ByteEnum.convertToByte(15L)),

    /**
     * 訪客角色。
     * 僅擁有檔案讀取權限，無法進行檔案上傳、修改或刪除操作。
     * 無儲存空間配額，僅能瀏覽和下載已分享的檔案。
     */
    VISITOR(Set.of(PermissionEnum.READ), 0L),

    /**
     * 匿名使用者角色。
     * 無任何檔案操作權限，僅能存取公開的系統資源。
     * 無儲存空間配額，主要用於未登入狀態的基本系統存取。
     */
    ANONYMOUS(Set.of(), 0L)
    ;

    /**
     * 角色對應的權限集合。
     * 定義該角色可執行的檔案操作類型，包括讀取、寫入、刪除、上傳、下載、分享、更新和管理等權限。
     */
    private final Set<PermissionEnum> permissions;

    /**
     * 角色預設儲存空間限制。
     * 以位元組為單位的儲存配額，-1 表示無限制，0 表示無儲存權限。
     */
    private final Long defaultStorageLimit;

    /**
     * 檢查角色是否擁有指定的操作權限。
     * 驗證當前角色的權限集合是否包含所有必要的權限，支援檢查單一或多個權限。
     *
     * @param requiredPermissions 需要檢查的權限陣列，可為空或 null
     * @return 若擁有所有指定權限則回傳 true，否則回傳 false；若未指定權限則檢查是否有任何權限
     */
    public boolean hasPermissions(PermissionEnum... requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return !permissions.isEmpty();
        }
        for (PermissionEnum requiredPermission : requiredPermissions) {
            if (!permissions.contains(requiredPermission)) {
                return false;
            }
        }
        return true;
    }
}
