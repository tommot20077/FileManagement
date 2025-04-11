package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.PermissionEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 需要權限的註解，用於標記需要哪些權限才能訪問的接口
 *
 * @author yuan
 * @program FileManagement
 * @ClassName RequirePermission
 * @create 2025/2/3
 * @Version 1.0
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequirePermission {
    /**
     * 使用 PermissionEnum 來標記需要的權限
     *
     * @return PermissionEnum[]
     */
    PermissionEnum[] value();
}

