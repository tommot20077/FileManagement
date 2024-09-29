package xyz.dowob.filemanagement.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用於隱藏AOP中日誌禁止顯示其返回值並標記為隱藏的敏感訊息
 * 可以標記在方法或類上
 *
 * @author yuan
 * @program FileManagement
 * @ClassName HideSensitive
 * @description
 * @create 2024-09-23 15:19
 * @Version 1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface HideSensitive {}