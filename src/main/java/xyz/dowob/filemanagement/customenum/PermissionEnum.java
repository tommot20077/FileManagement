package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 權限枚舉，用於定義權限操作
 *
 * @author yuan
 * @program File-Management
 * @ClassName PermissionEnum
 * @description
 * @create 2024-09-14 17:09
 * @Version 1.0
 **/

@Getter
@RequiredArgsConstructor
public enum PermissionEnum {
    /**
     * 寫入權限
     */
    WRITE("寫入"),
    /**
     * 讀取權限
     */
    READ("讀取"),
    /**
     * 刪除權限
     */
    DELETE("刪除"),
    /**
     * 上傳權限
     */
    UPLOAD("上傳"),
    /**
     * 下載權限
     */
    DOWNLOAD("下載"),
    /**
     * 分享權限
     */
    SHARE("分享"),
    /**
     * 更新權限
     */
    UPDATE("更新"),
    /**
     * 管理權限
     */
    MANAGE("管理");

    /**
     * 權限名稱
     */
    private final String permission;

}
