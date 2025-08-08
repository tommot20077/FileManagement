package xyz.dowob.filemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 啟動類
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 **/
@EnableScheduling
@EnableAspectJAutoProxy
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration.class,
    net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration.class
})
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
