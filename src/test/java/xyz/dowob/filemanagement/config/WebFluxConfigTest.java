package xyz.dowob.filemanagement.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import xyz.dowob.filemanagement.config.properties.FileProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * WebFluxConfig 測試類別
 * 
 * <p>測試 WebFluxConfig Web 流配置類別的功能驗證，包含下列重點：</p>
 * 
 * <p>主要測試範圍：
 * 
 *   - WebFluxConfigurer 接口實現的正確性
 *   - 服務器編解碼器配置驗證
 *   - 最大記憶體大小設定的邊界測試
 *   - ResponseUnity 接口實現的一致性
 *   - 檔案屬性依賴注入的穩定性
 * 
 * </p>
 * 
 * <p>測試前置條件：
 * 
 *   - WebFluxConfig 可正常實例化
 *   - FileProperties 配置正確且可模擬
 *   - ServerCodecConfigurer 可進行依賴注入模擬
 * 
 * </p>
 * 
 * <p>測試策略：
 * 
 *   - 對配置方法進行全面的行為驗證
 *   - 驗證編解碼器配置的正確性和穩定性
 *   - 測試各種邊界條件和異常情況
 * 
 * </p>
 * 
 * <p>預期測試結果：
 * 
 *   - WebFlux 配置可正確初始化
 *   - 記憶體大小限制能準確根據檔案屬性設定
 *   - 接口實現符合預期行為
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("WebFluxConfig Web 流配置測試")
class WebFluxConfigTest {

    @Mock
    private FileProperties fileProperties;
    
    @Mock
    private FileProperties.Upload uploadProperties;
    
    @Mock
    private ServerCodecConfigurer serverCodecConfigurer;
    
    @Mock
    private ServerCodecConfigurer.ServerDefaultCodecs defaultCodecs;

    private WebFluxConfig webFluxConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 設置 FileProperties mock
        when(fileProperties.getUpload()).thenReturn(uploadProperties);
        when(uploadProperties.getPayloadLength()).thenReturn(DataSize.ofMegabytes(10)); // 10MB
        
        when(serverCodecConfigurer.defaultCodecs()).thenReturn(defaultCodecs);
        
        webFluxConfig = new WebFluxConfig(fileProperties);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - WebFlux 配置初始化")
    void testWebFluxConfigInitialization() {
        assertNotNull(webFluxConfig);
        assertTrue(webFluxConfig instanceof WebFluxConfigurer);
        assertTrue(webFluxConfig instanceof xyz.dowob.filemanagement.unity.ResponseUnity);
    }

    @Test
    @DisplayName("一般測試 - configureHttpMessageCodecs 配置 HTTP 消息編解碼器")
    void testConfigureHttpMessageCodecs_basicConfiguration() {
        DataSize expectedSize = DataSize.ofMegabytes(10);
        when(uploadProperties.getPayloadLength()).thenReturn(expectedSize);
        
        webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
        
        // 驗證調用了正確的方法並設置了正確的大小
        verify(serverCodecConfigurer).defaultCodecs();
        verify(defaultCodecs).maxInMemorySize((int) expectedSize.toBytes());
    }

    @Test
    @DisplayName("一般測試 - configureHttpMessageCodecs 不同大小配置")
    void testConfigureHttpMessageCodecs_differentSizes() {
        // 測試不同的檔案大小配置
        DataSize[] testSizes = {
            DataSize.ofKilobytes(500),      // 500KB
            DataSize.ofMegabytes(5),        // 5MB
            DataSize.ofMegabytes(50),       // 50MB
            DataSize.ofGigabytes(1)         // 1GB
        };
        
        for (DataSize size : testSizes) {
            reset(serverCodecConfigurer, defaultCodecs);
            when(serverCodecConfigurer.defaultCodecs()).thenReturn(defaultCodecs);
            when(uploadProperties.getPayloadLength()).thenReturn(size);
            
            webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
            
            verify(defaultCodecs).maxInMemorySize((int) size.toBytes());
        }
    }

    @Test
    @DisplayName("一般測試 - WebFluxConfigurer 接口方法")
    void testWebFluxConfigurerInterface() {
        // 驗證實現了 WebFluxConfigurer 接口
        assertTrue(webFluxConfig instanceof WebFluxConfigurer);
        
        // 驗證可以調用接口方法而不拋出異常
        assertDoesNotThrow(() -> {
            webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
        });
    }

    @Test
    @DisplayName("一般測試 - ResponseUnity 接口實現")
    void testResponseUnityInterface() {
        // 驗證實現了 ResponseUnity 接口
        assertTrue(webFluxConfig instanceof xyz.dowob.filemanagement.unity.ResponseUnity);
        
        // 驗證可以作為 ResponseUnity 使用
        xyz.dowob.filemanagement.unity.ResponseUnity responseUnity = webFluxConfig;
        assertNotNull(responseUnity);
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - 空 FileProperties 構造")
    void testWebFluxConfig_nullFileProperties() {
        // 驗證使用 null FileProperties 會拋出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            new WebFluxConfig(null);
        });
    }

    @Test
    @DisplayName("異常測試 - 空 ServerCodecConfigurer 配置")
    void testConfigureHttpMessageCodecs_nullConfigurer() {
        // 驗證使用 null configurer 會拋出異常
        assertThrows(NullPointerException.class, () -> {
            webFluxConfig.configureHttpMessageCodecs(null);
        });
    }

    @Test
    @DisplayName("異常測試 - FileProperties 返回 null Upload 配置")
    void testConfigureHttpMessageCodecs_nullUploadProperties() {
        when(fileProperties.getUpload()).thenReturn(null);
        
        assertThrows(NullPointerException.class, () -> {
            webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
        });
    }

    @Test
    @DisplayName("異常測試 - Upload 配置返回 null PayloadLength")
    void testConfigureHttpMessageCodecs_nullPayloadLength() {
        when(uploadProperties.getPayloadLength()).thenReturn(null);
        
        assertThrows(NullPointerException.class, () -> {
            webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
        });
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 極小檔案大小配置")
    void testConfigureHttpMessageCodecs_verySmallSize() {
        DataSize verySmallSize = DataSize.ofBytes(1); // 1 byte
        when(uploadProperties.getPayloadLength()).thenReturn(verySmallSize);
        
        webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
        
        verify(defaultCodecs).maxInMemorySize(1);
    }

    @Test
    @DisplayName("邊界測試 - 極大檔案大小配置")
    void testConfigureHttpMessageCodecs_veryLargeSize() {
        DataSize veryLargeSize = DataSize.ofTerabytes(1); // 1TB
        when(uploadProperties.getPayloadLength()).thenReturn(veryLargeSize);
        
        webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
        
        // 注意：這裡會轉換為 int，可能會溢出，但這是設計行為
        verify(defaultCodecs).maxInMemorySize((int) veryLargeSize.toBytes());
    }

    @Test
    @DisplayName("邊界測試 - 零大小檔案配置")
    void testConfigureHttpMessageCodecs_zeroSize() {
        DataSize zeroSize = DataSize.ofBytes(0);
        when(uploadProperties.getPayloadLength()).thenReturn(zeroSize);
        
        webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
        
        verify(defaultCodecs).maxInMemorySize(0);
    }

    @Test
    @DisplayName("邊界測試 - 標準檔案大小配置")
    void testConfigureHttpMessageCodecs_standardSizes() {
        // 測試一些標準的檔案大小
        DataSize[] standardSizes = {
            DataSize.ofBytes(1024),          // 1KB
            DataSize.ofKilobytes(1),         // 1KB (equivalent)
            DataSize.ofMegabytes(1),         // 1MB
            DataSize.ofMegabytes(10),        // 10MB
            DataSize.ofMegabytes(100),       // 100MB
            DataSize.ofGigabytes(1)          // 1GB
        };
        
        for (DataSize size : standardSizes) {
            reset(serverCodecConfigurer, defaultCodecs);
            when(serverCodecConfigurer.defaultCodecs()).thenReturn(defaultCodecs);
            when(uploadProperties.getPayloadLength()).thenReturn(size);
            
            assertDoesNotThrow(() -> {
                webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
            });
            
            verify(defaultCodecs).maxInMemorySize((int) size.toBytes());
        }
    }

    @Test
    @DisplayName("邊界測試 - 多次配置調用")
    void testConfigureHttpMessageCodecs_multipleCalls() {
        DataSize size = DataSize.ofMegabytes(5);
        when(uploadProperties.getPayloadLength()).thenReturn(size);
        
        // 多次調用配置方法
        for (int i = 0; i < 5; i++) {
            webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
        }
        
        // 驗證每次都會調用配置
        verify(serverCodecConfigurer, times(5)).defaultCodecs();
        verify(defaultCodecs, times(5)).maxInMemorySize((int) size.toBytes());
    }

    @Test
    @DisplayName("邊界測試 - 構造函數參數驗證")
    void testConstructor_parameterValidation() {
        // 測試正常構造
        assertDoesNotThrow(() -> {
            new WebFluxConfig(fileProperties);
        });
        
        // 驗證構造的對象不為空
        WebFluxConfig config = new WebFluxConfig(fileProperties);
        assertNotNull(config);
    }

    @Test
    @DisplayName("邊界測試 - 接口兼容性")
    void testInterfaceCompatibility() {
        // 驗證可以作為 WebFluxConfigurer 使用
        WebFluxConfigurer configurer = webFluxConfig;
        assertNotNull(configurer);
        
        // 驗證可以作為 ResponseUnity 使用
        xyz.dowob.filemanagement.unity.ResponseUnity responseUnity = webFluxConfig;
        assertNotNull(responseUnity);
        
        // 驗證兩個接口引用指向同一個對象
        assertSame(configurer, responseUnity);
    }

    @Test
    @DisplayName("邊界測試 - 內存溢出邊界")
    void testConfigureHttpMessageCodecs_integerOverflowBoundary() {
        // 測試接近 Integer.MAX_VALUE 的情況
        long nearMaxValue = (long) Integer.MAX_VALUE - 1000;
        DataSize nearMaxSize = DataSize.ofBytes(nearMaxValue);
        when(uploadProperties.getPayloadLength()).thenReturn(nearMaxSize);
        
        assertDoesNotThrow(() -> {
            webFluxConfig.configureHttpMessageCodecs(serverCodecConfigurer);
        });
        
        verify(defaultCodecs).maxInMemorySize((int) nearMaxValue);
    }
}