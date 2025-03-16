package xyz.dowob.filemanagement.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 不記錄日誌的標記，當有些方法重複性太高或是對於除錯沒有幫助時，可以使用此標記來跳過日誌的記錄
 * 在方法上使用此標記，則此方法不會被記錄日誌
 *
 * @author yuan
 * @program FileManagement
 * @ClassName SkipRecord
 * @create 2025/3/16
 * @Version 1.0
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
public @interface SkipRecord {
}
