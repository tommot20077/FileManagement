package xyz.dowob.filemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Spring Boot 啟動類
 *
 * @Author yuan
 * @Program File-Management
 * @ClassName FileManagementApplication
 * @description Spring Boot 啟動類
 * @create 2024-09-14 17:08
 * @Version 1.0
 **/
@SpringBootApplication
@EnableAspectJAutoProxy
public class FileManagementApplication {
    /**
     * Spring Boot 啟動方法
     *
     * @param args 啟動參數
     */
    public static void main(String[] args) {
        SpringApplication.run(FileManagementApplication.class, args);
    }

}
