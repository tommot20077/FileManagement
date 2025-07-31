package xyz.dowob.filemanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 設定類，用於設定 Swagger 的 OpenAPI 規範和文檔屬性。
 * <p>
 * 該設定類提供 API 文檔的基本信息設定，包括 API 標題、版本、描述、
 * 授權信息等。透過 OpenAPI 規範生成的文檔可用於 API 測試和客戶端整合。
 * <p>
 * 支持 Spring WebFlux 的非同步 API 文檔生成，提供完整的 REST API 文檔和交互式探索介面。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class OpenApiConfig {
    /**
     * 創建 OpenAPI 設定 Bean，用於定義 API 文檔的基本信息。
     * <p>
     * 該方法設定 API 文檔的以下屬性：
     * <p>
     * - 標題：用戶檔案管理系統
     * <p>
     * - 版本：1.0.0
     * <p>
     * - 描述：用戶檔案管理系統相關 API
     * <p>
     * - 授權：Apache 2.0 許可證
     * <p>
     * - 服務條款：Swagger 官方服務條款
     *
     * @return 設定好的 OpenAPI 實例，包含所有 API 文檔信息
     */
    @Bean
    public OpenAPI customOpenApi() {
        Info info = new Info()
                .title("用戶檔案管理系統")
                .version("1.0.0")
                .description("用戶檔案管理系統 相關API")
                .termsOfService("http://swagger.io/terms/")
                .license(new License().name("Apache 2.0").url("http://springdoc.org"));
        return new OpenAPI().info(info);
    }
}
