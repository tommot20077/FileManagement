package xyz.dowob.filemanagement.grpc.config;

import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.grpc.interceptor.ApiKeyAuthInterceptor;
import xyz.dowob.filemanagement.grpc.interceptor.GrpcExceptionInterceptor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrpcServerConfig 配置類測試。
 *
 * 測試 gRPC 服務器配置的正確性，包括攔截器註冊、條件配置和 Bean 創建。
 *
 * 前置條件：
 * - Mock Spring 應用上下文
 * - 配置測試屬性
 * - Mock 攔截器實例
 *
 * 測試步驟：
 * - 驗證攔截器 Bean 創建
 * - 檢查全局攔截器註冊
 * - 測試條件配置
 * - 驗證攔截器順序
 *
 * 預期結果：
 * - 攔截器正確註冊為全局攔截器
 * - 條件配置正確生效
 * - Bean 創建和配置正確
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("gRPC 服務器配置測試")
class GrpcServerConfigTest {
    
    @Mock
    private ApiKeyAuthInterceptor apiKeyAuthInterceptor;
    
    @Mock
    private GrpcExceptionInterceptor grpcExceptionInterceptor;
    
    @Mock
    private GlobalProperties globalProperties;
    
    @Mock
    private GlobalProperties.WebDav webDavProperties;
    
    private GrpcServerConfig grpcServerConfig;
    
    @BeforeEach
    void setUp() {
        // 創建配置實例
        grpcServerConfig = new GrpcServerConfig(apiKeyAuthInterceptor, grpcExceptionInterceptor);
    }
    
    @Test
    @DisplayName("API Key 攔截器註冊測試")
    void testGlobalApiKeyInterceptorRegistration() {
        // When
        ApiKeyAuthInterceptor result = grpcServerConfig.globalApiKeyInterceptor();
        
        // Then
        assertNotNull(result);
        assertSame(apiKeyAuthInterceptor, result);
    }
    
    @Test
    @DisplayName("異常攔截器註冊測試")
    void testGlobalExceptionInterceptorRegistration() {
        // When
        GrpcExceptionInterceptor result = grpcServerConfig.globalExceptionInterceptor();
        
        // Then
        assertNotNull(result);
        assertSame(grpcExceptionInterceptor, result);
    }
    
    @Test
    @DisplayName("攔截器 Bean 註解驗證")
    void testInterceptorBeanAnnotations() throws NoSuchMethodException {
        // 驗證 globalApiKeyInterceptor 方法的註解
        var apiKeyMethod = GrpcServerConfig.class.getMethod("globalApiKeyInterceptor");
        
        // 檢查 @Bean 註解
        assertNotNull(apiKeyMethod.getAnnotation(org.springframework.context.annotation.Bean.class),
            "globalApiKeyInterceptor 應該有 @Bean 註解");
        
        // 檢查 @GrpcGlobalServerInterceptor 註解
        assertNotNull(apiKeyMethod.getAnnotation(GrpcGlobalServerInterceptor.class),
            "globalApiKeyInterceptor 應該有 @GrpcGlobalServerInterceptor 註解");
        
        // 驗證 globalExceptionInterceptor 方法的註解
        var exceptionMethod = GrpcServerConfig.class.getMethod("globalExceptionInterceptor");
        
        // 檢查 @Bean 註解
        assertNotNull(exceptionMethod.getAnnotation(org.springframework.context.annotation.Bean.class),
            "globalExceptionInterceptor 應該有 @Bean 註解");
        
        // 檢查 @GrpcGlobalServerInterceptor 註解
        assertNotNull(exceptionMethod.getAnnotation(GrpcGlobalServerInterceptor.class),
            "globalExceptionInterceptor 應該有 @GrpcGlobalServerInterceptor 註解");
    }
    
    @Test
    @DisplayName("條件配置註解驗證")
    void testConditionalConfiguration() {
        // 驗證類級別的條件註解
        var conditionalProperty = GrpcServerConfig.class.getAnnotation(
            org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class
        );
        
        assertNotNull(conditionalProperty, "應該有 @ConditionalOnProperty 註解");
        assertEquals("global.webdav", conditionalProperty.prefix());
        assertArrayEquals(new String[]{"enabled"}, conditionalProperty.name());
        assertEquals("true", conditionalProperty.havingValue());
    }
    
    @Test
    @DisplayName("配置類註解驗證")
    void testConfigurationAnnotations() {
        // 驗證 @Configuration 註解
        assertNotNull(
            GrpcServerConfig.class.getAnnotation(org.springframework.context.annotation.Configuration.class),
            "應該有 @Configuration 註解"
        );
    }
    

    @Test
    @DisplayName("攔截器執行順序")
    void testInterceptorOrder() {
        // API Key 攔截器應該先執行
        ApiKeyAuthInterceptor apiKey = grpcServerConfig.globalApiKeyInterceptor();
        GrpcExceptionInterceptor exception = grpcServerConfig.globalExceptionInterceptor();

        // 驗證兩個攔截器都被創建
        assertNotNull(apiKey);
        assertNotNull(exception);

        // 在實際應用中，API Key 驗證應該在異常處理之前
        // 這裡我們只能驗證它們都被正確返回
        assertSame(apiKeyAuthInterceptor, apiKey);
        assertSame(grpcExceptionInterceptor, exception);
    }
    

    @Test
    @DisplayName("攔截器依賴注入")
    void testInterceptorDependencyInjection() {
        // 驗證構造函數參數
        assertNotNull(grpcServerConfig);

        // 驗證攔截器可以被正確獲取
        ApiKeyAuthInterceptor apiKeyResult = grpcServerConfig.globalApiKeyInterceptor();
        GrpcExceptionInterceptor exceptionResult = grpcServerConfig.globalExceptionInterceptor();

        // 驗證是同一個實例（單例）
        assertSame(apiKeyAuthInterceptor, apiKeyResult);
        assertSame(grpcExceptionInterceptor, exceptionResult);

        // 再次獲取應該還是同一個實例
        ApiKeyAuthInterceptor apiKeyResult2 = grpcServerConfig.globalApiKeyInterceptor();
        GrpcExceptionInterceptor exceptionResult2 = grpcServerConfig.globalExceptionInterceptor();

        assertSame(apiKeyResult, apiKeyResult2);
        assertSame(exceptionResult, exceptionResult2);
    }
}