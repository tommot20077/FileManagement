package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.LogLevelEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日誌級別註解，用於標註方法、類別或欄位的日誌級別
 * 當標記了此註解的元素被調用時，會根據指定的日誌級別進行日誌記錄
 *
 * @author yuan
 * @program FileManagement
 * @ClassName RecordLevel
 * @create 2025/4/18
 * @Version 1.0
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
public @interface RecordLevel {
    /**
     * 日誌級別
     *
     * @return 日誌級別
     */
    LogLevelEnum value();
}

