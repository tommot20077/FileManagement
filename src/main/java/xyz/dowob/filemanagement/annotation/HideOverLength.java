package xyz.dowob.filemanagement.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用於標記隱藏超過長度的字串
 *
 * @author yuan
 * @program FileManagement
 * @ClassName HideOverlength
 * @create 2025/1/21
 * @Version 1.0
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface HideOverLength {
}
