package xyz.dowob.filemanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用於配置Swagger的OpenApi設置類別
 *
 * @author yuan
 * @program File-Management
 * @ClassName OpenApiConfig
 * @description
 * @create 2024-09-19 04:07
 * @Version 1.0
 **/
@Configuration
public class OpenApiConfig {
    /**
     * 配置Swagger的OpenApi設置
     *
     * @return OpenAPI
     */
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI().info(new Info().title("用戶檔案管理系統")
                                            .version("1.0")
                                            .description("用戶檔案管理系統 相關API")
                                            .termsOfService("http://swagger.io/terms/")
                                            .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }

}
