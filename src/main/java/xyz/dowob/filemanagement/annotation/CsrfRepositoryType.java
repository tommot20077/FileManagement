package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum;

import java.lang.annotation.*;

/**
 * 用於標記 CsrfTokenRepository 的實現類型，並提供給策略模式使用
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CsrfRepositoryType
 * @create 2025/3/6
 * @Version 1.0
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CsrfRepositoryType {
    /**
     * 使用 CsrfTokenRepositoryEnum 來標記 CsrfTokenRepository 的實現類型
     *
     * @return CsrfTokenRepositoryEnum
     */
    CsrfTokenRepositoryEnum value();
}
