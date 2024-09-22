package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.CacheProviderEnum;

import java.lang.annotation.*;

/**
 * 標記緩存提供器的類型，用於區分不同的緩存提供器
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CacheProviderType
 * @create 2025/3/15
 * @Version 1.0
 **/
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheProviderType {
    /**
     * 使用 CacheProviderEnum 來標記緩存提供器的類型
     *
     * @return CacheProviderEnum 緩存提供器的類型
     */
    CacheProviderEnum value();
}
