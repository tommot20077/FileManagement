package xyz.dowob.filemanagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.stream.Stream;

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
