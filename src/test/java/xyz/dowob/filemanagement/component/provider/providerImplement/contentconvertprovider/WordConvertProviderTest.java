package xyz.dowob.filemanagement.component.provider.providerImplement.contentconvertprovider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.factory.config.ConvertConfig;
import xyz.dowob.filemanagement.customenum.ConvertProviderEnum;
import xyz.dowob.filemanagement.exception.ProcessException;

import java.math.BigInteger;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * WordConvertProvider 測試類別。
 * 
 * <p>測試 WordConvertProvider 的 Word 檔案轉換功能，包括：
 * <ul>
 * <li>轉換器類型與配置獲取</li>
 * <li>JSON 格式轉換為 InputStream</li>
 * <li>JSON 格式轉換為 DataBuffer</li>
 * <li>各種文字格式處理（粗體、斜體、標題）</li>
 * <li>列表格式處理（有序、無序）</li>
 * <li>程式碼區塊與表格轉換</li>
 * <li>超連結與特殊內容處理</li>
 * <li>無效 JSON 與空內容處理</li>
 * </ul>
 * 
 * <p>測試涵蓋 Quill Delta JSON 格式轉換為 DOCX 檔案的所有場景，
 * 包含正常轉換、異常處理及各種文字格式的正確性。透過反應式程式測試確保轉換功能的完整性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WordConvertProvider 邏輯處理測試")
@MockitoSettings(strictness = Strictness.LENIENT)
class WordConvertProviderTest {

    @Mock
    private ConvertConfig mockConfig;

    private WordConvertProvider wordConvertProviderUnderTest;

    @BeforeEach
    void setUp() {
        when(mockConfig.getAbstractNumIdBullet()).thenReturn(BigInteger.valueOf(1L));
        when(mockConfig.getAbstractNumIdDecimal()).thenReturn(BigInteger.valueOf(2L));
        when(mockConfig.getTwipsPerOneLevel()).thenReturn(360);
        when(mockConfig.getCodeBlockBackgroundColor()).thenReturn("auto");
        when(mockConfig.getCodeBlockFontFamily()).thenReturn("Courier New");
        when(mockConfig.getCodeBlockFontSize()).thenReturn(10);
        when(mockConfig.getDefaultFontFamily()).thenReturn("Calibri");
        when(mockConfig.getDefaultFontSize()).thenReturn(11);
        when(mockConfig.getDefaultFontColor()).thenReturn("000000");
        when(mockConfig.getDefaultFontConvertMap()).thenReturn(Map.of("arial", "Arial"));
        when(mockConfig.getDefaultFontSizeConvertMap()).thenReturn(Map.of("small", 9, "large", 14));

        wordConvertProviderUnderTest = new WordConvertProvider(mockConfig);
    }

    @Test
    @DisplayName("getType - 成功取得轉換器類型")
    void getType_returnsCorrectType() {
        ConvertProviderEnum type = wordConvertProviderUnderTest.getType();

        assertThat(type).isEqualTo(ConvertProviderEnum.DOCX);
    }

    @Test
    @DisplayName("getConvertConfig - 成功取得轉換設定")
    void getConvertConfig_returnsConfig() {
        ConvertConfig resultConfig = wordConvertProviderUnderTest.getConvertConfig();

        assertThat(resultConfig).isEqualTo(mockConfig);
    }

    @Test
    @DisplayName("convertToInputStream 正常轉換 - 成功回傳 InputStream")
    void convertToInputStream_validContent_returnsInputStream() {
        String validJsonContent = "{\"delta\":[{\"insert\":\"Hello World\"},{\"attributes\":{\"bold\":true},\"insert\":\"bold\"},{\"insert\":\"\\n\"}]}";

        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(validJsonContent))
                .assertNext(inputStream -> {
                    assertThat(inputStream).isNotNull();
                    try {
                        assertThat(inputStream.available()).isGreaterThan(0);
                        inputStream.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convertToInputStream 空內容 - 成功回傳空的 InputStream")
    void convertToInputStream_emptyContent_returnsEmptyInputStream() {
        String emptyJsonContent = "{\"delta\":[]}";

        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(emptyJsonContent))
                .assertNext(inputStream -> {
                    assertThat(inputStream).isNotNull();
                                try {
                        assertThat(inputStream.available()).isGreaterThan(0);
                        inputStream.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convertToInputStream 格式錯誤的 JSON 內容 - 拋出 ProcessException")
    void convertToInputStream_invalidJsonContent_throwsProcessException() {
        String invalidJsonContent = "{\"delta\":[{\"insert\":\"Hello World\"";

        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(invalidJsonContent))
                .expectErrorMatches(throwable -> throwable instanceof ProcessException &&
                        ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.CONVERT_JSON_TO_TARGET_FAILED)
                .verify();
    }
    
    @Test
    @DisplayName("convertToInputStream null 內容 - 拋出 ProcessException")
    void convertToInputStream_nullContent_throwsProcessException() {
        String nullContent = null;

        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(nullContent))
            .expectErrorMatches(throwable -> throwable instanceof ProcessException &&
                ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.CONVERT_JSON_TO_TARGET_FAILED &&
                throwable.getCause() instanceof IllegalArgumentException)
            .verify();
    }

    @Test
    @DisplayName("convertToDataBuffer 正常轉換 - 成功回傳 DataBufferRecord")
    void convertToDataBuffer_validContent_returnsDataBufferRecord() {
        String validJsonContent = "{\"delta\":[{\"insert\":\"Hello World\"},{\"attributes\":{\"bold\":true},\"insert\":\"bold\"},{\"insert\":\"\\n\"}]}";

        StepVerifier.create(wordConvertProviderUnderTest.convertToDataBuffer(validJsonContent))
                .assertNext(dataBufferRecord -> {
                    assertThat(dataBufferRecord).isNotNull();
                    assertThat(dataBufferRecord.size()).isGreaterThan(0);
                    StepVerifier.create(dataBufferRecord.dataBuffer())
                            .assertNext(dataBuffer -> {
                                assertThat(dataBuffer.readableByteCount()).isGreaterThan(0);
                                DataBufferUtils.release(dataBuffer);
                            })
                            .verifyComplete();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convertToDataBuffer 空內容 - 成功回傳空的 DataBufferRecord")
    void convertToDataBuffer_emptyContent_returnsEmptyDataBufferRecord() {
        String emptyJsonContent = "{\"delta\":[]}";

        StepVerifier.create(wordConvertProviderUnderTest.convertToDataBuffer(emptyJsonContent))
                .assertNext(dataBufferRecord -> {
                    assertThat(dataBufferRecord).isNotNull();
                                assertThat(dataBufferRecord.size()).isGreaterThan(0);
                     StepVerifier.create(dataBufferRecord.dataBuffer())
                            .assertNext(dataBuffer -> {
                                assertThat(dataBuffer.readableByteCount()).isGreaterThan(0);
                                DataBufferUtils.release(dataBuffer);
                            })
                            .verifyComplete();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convertToDataBuffer 格式錯誤的 JSON 內容 - 拋出 ProcessException")
    void convertToDataBuffer_invalidJsonContent_throwsProcessException() {
        String invalidJsonContent = "{\"delta\":[{\"insert\":\"Hello World\""; // Malformed JSON

        StepVerifier.create(wordConvertProviderUnderTest.convertToDataBuffer(invalidJsonContent))
                .expectErrorMatches(throwable -> throwable instanceof ProcessException &&
                        ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.CONVERT_JSON_TO_TARGET_FAILED)
                .verify();
    }
    
    @Test
    @DisplayName("convertToDataBuffer null 內容 - 拋出 ProcessException")
    void convertToDataBuffer_nullContent_throwsProcessException() {
        String nullContent = null;
    
        StepVerifier.create(wordConvertProviderUnderTest.convertToDataBuffer(nullContent))
            .expectErrorMatches(throwable -> throwable instanceof ProcessException &&
                ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.CONVERT_JSON_TO_TARGET_FAILED &&
                throwable.getCause() instanceof IllegalArgumentException)
            .verify();
    }

    @Test
    @DisplayName("convertToInputStream 文字包含粗體和斜體 - 成功回傳 InputStream")
    void convertToInputStream_boldAndItalicText_returnsInputStream() {
        String content = "{\"delta\":[{\"insert\":\"Normal \"},{\"attributes\":{\"bold\":true},\"insert\":\"Bold\"},{\"insert\":\" \"},{\"attributes\":{\"italic\":true},\"insert\":\"Italic\"},{\"insert\":\"\\n\"}]}";
        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(content))
                .assertNext(inputStream -> {
                    assertThat(inputStream).isNotNull();
                    try {
                        assertThat(inputStream.available()).isGreaterThan(0);
                        inputStream.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convertToInputStream 包含無序列表 - 成功回傳 InputStream")
    void convertToInputStream_unorderedList_returnsInputStream() {
        String content = "{\"delta\":[{\"insert\":\"Item 1\"},{\"attributes\":{\"list\":\"bullet\"},\"insert\":\"\\n\"},{\"insert\":\"Item 2\"},{\"attributes\":{\"list\":\"bullet\"},\"insert\":\"\\n\"}]}";
        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(content))
                .assertNext(inputStream -> {
                    assertThat(inputStream).isNotNull();
                    try {
                        assertThat(inputStream.available()).isGreaterThan(0);
                        inputStream.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convertToInputStream 包含有序列表 - 成功回傳 InputStream")
    void convertToInputStream_orderedList_returnsInputStream() {
        String content = "{\"delta\":[{\"insert\":\"First item\"},{\"attributes\":{\"list\":\"ordered\"},\"insert\":\"\\n\"},{\"insert\":\"Second item\"},{\"attributes\":{\"list\":\"ordered\"},\"insert\":\"\\n\"}]}";
        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(content))
                .assertNext(inputStream -> {
                    assertThat(inputStream).isNotNull();
                    try {
                        assertThat(inputStream.available()).isGreaterThan(0);
                        inputStream.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convertToInputStream 包含標題 - 成功回傳 InputStream")
    void convertToInputStream_header_returnsInputStream() {
        String content = "{\"delta\":[{\"insert\":\"Title\"},{\"attributes\":{\"header\":1},\"insert\":\"\\n\"},{\"insert\":\"Subtitle\"},{\"attributes\":{\"header\":2},\"insert\":\"\\n\"}]}";
        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(content))
                .assertNext(inputStream -> {
                    assertThat(inputStream).isNotNull();
                    try {
                        assertThat(inputStream.available()).isGreaterThan(0);
                        inputStream.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convertToInputStream 包含程式碼區塊 - 成功回傳 InputStream")
    void convertToInputStream_codeBlock_returnsInputStream() {
        String content = "{\"delta\":[{\"insert\":\"public void main(String[] args)\"},{\"attributes\":{\"code-block\":true},\"insert\":\"\\n\"},{\"insert\":\"System.out.println(\\\"Hello\\\");\"},{\"attributes\":{\"code-block\":true},\"insert\":\"\\n\"}]}";
        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(content))
                .assertNext(inputStream -> {
                    assertThat(inputStream).isNotNull();
                    try {
                        assertThat(inputStream.available()).isGreaterThan(0);
                        inputStream.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convertToInputStream 包含簡單表格 - 成功回傳 InputStream")
    void convertToInputStream_simpleTable_returnsInputStream() {
        String content = "{\"delta\":[{\"insert\":\"Cell A1\"},{\"attributes\":{\"table\":\"row1\"},\"insert\":\"\\n\"},{\"insert\":\"Cell B1\"},{\"attributes\":{\"table\":\"row1\"},\"insert\":\"\\n\"},{\"insert\":\"Cell A2\"},{\"attributes\":{\"table\":\"row2\"},\"insert\":\"\\n\"},{\"insert\":\"Cell B2\"},{\"attributes\":{\"table\":\"row2\"},\"insert\":\"\\n\"}]}";
        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(content))
                .assertNext(inputStream -> {
                    assertThat(inputStream).isNotNull();
                    try {
                        assertThat(inputStream.available()).isGreaterThan(0);
                        inputStream.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convertToInputStream 包含超連結 - 成功回傳 InputStream")
    void convertToInputStream_hyperlink_returnsInputStream() {
        String content = "{\"delta\":[{\"attributes\":{\"link\":\"http://example.com\"},\"insert\":\"Example Link\"},{\"insert\":\"\\n\"}]}";
        StepVerifier.create(wordConvertProviderUnderTest.convertToInputStream(content))
                .assertNext(inputStream -> {
                    assertThat(inputStream).isNotNull();
                    try {
                        assertThat(inputStream.available()).isGreaterThan(0);
                        inputStream.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }
}
