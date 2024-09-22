package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.FileEnum;

import java.lang.annotation.*;

/**
 * 標記檔案的處理類型，用於區分不同的檔案類型
 *
 * @author yuan
 * @program File-Management
 * @ClassName FileHandlerType
 * @description
 * @create 2024-09-22 16:34
 * @Version 1.0
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FileHandlerType {
    /**
     * 使用 FileEnum 來標記檔案的處理類型
     *
     * @return FileEnum
     */
    FileEnum value();

}
