package xyz.dowob.filemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableScheduling
@EnableAspectJAutoProxy
@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
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
