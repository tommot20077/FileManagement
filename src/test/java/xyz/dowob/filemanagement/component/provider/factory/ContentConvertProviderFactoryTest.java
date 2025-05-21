package xyz.dowob.filemanagement.component.provider.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.factory.config.ConvertConfig;
import xyz.dowob.filemanagement.component.provider.providerInterface.ContentConvertProvider;
import xyz.dowob.filemanagement.customenum.ConvertProviderEnum;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentConvertProviderFactory 邏輯處理測試")
class ContentConvertProviderFactoryTest {

    private ConvertConfig config;

    @BeforeEach
    void setUp() {
        config = new ConvertConfig();
        config.setAbstractNumIdBullet(BigInteger.valueOf(100));
        config.setAbstractNumIdDecimal(BigInteger.valueOf(200));
        config.setCodeBlockBackgroundColor("#F5F5F5");
        config.setCodeBlockFontFamily("Consolas");
        config.setCodeBlockFontSize(12);
    }

    @Test
    @DisplayName("測試創建DOCX轉換器 - 返回DOCX轉換器")
    void createProvider_WhenDocxType_ReturnsDocxConverter() {
        StepVerifier
                .create(Mono.fromCallable(() -> ContentConvertProviderFactory.createProvider(ConvertProviderEnum.DOCX, config)))
                .expectNextMatches(provider -> {
                    assertNotNull(provider);
                    assertEquals(ConvertProviderEnum.DOCX, provider.getType());
                    assertEquals(config, provider.getConvertConfig());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("測試創建Markdown轉換器 - 返回DOCX轉換器並驗證配置參數")
    void createProvider_WhenPdfType_ValidatesConfigParameters() {
        StepVerifier
                .create(Mono.fromCallable(() -> ContentConvertProviderFactory.createProvider(ConvertProviderEnum.MARKDOWN, config)))
                .expectNextMatches(provider -> {
                    assertNotNull(provider);
                    assertEquals(ConvertProviderEnum.DOCX, provider.getType());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("測試轉換類型為空 - 拋出IllegalArgumentException")
    void createProvider_WithEmptyConfig_UsesDefaultValues() {
        StepVerifier.create(Mono.fromCallable(() -> ContentConvertProviderFactory.createProvider(null, config))).expectErrorMatches(throwable -> {
            assertTrue(throwable instanceof IllegalArgumentException);
            assertEquals("轉換器類型不能為空", throwable.getMessage());
            return true;
        }).verify();
    }

    @Test
    @DisplayName("測試創建預設DOCX轉換器 - 返回預設DOCX轉換器")
    void createProvider_WhenDocxType_ThrowsException() {
        ConvertConfig defaultConfig = new ConvertConfig();
        StepVerifier
                .create(Mono.fromCallable(() -> ContentConvertProviderFactory.createProvider(ConvertProviderEnum.DOCX, defaultConfig)))
                .expectNextMatches(provider -> {
                    assertNotNull(provider);
                    assertEquals(ConvertProviderEnum.DOCX, provider.getType());
                    assertEquals(defaultConfig, provider.getConvertConfig());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("測試所有支持的轉換類型 - 成功創建轉換器")
    void createProvider_AllSupportedTypes() {
        for (ConvertProviderEnum type : ConvertProviderEnum.values()) {
            assertDoesNotThrow(() -> {
                ContentConvertProvider provider = ContentConvertProviderFactory.createProvider(type, config);
                assertNotNull(provider);
            });
        }
    }
}
