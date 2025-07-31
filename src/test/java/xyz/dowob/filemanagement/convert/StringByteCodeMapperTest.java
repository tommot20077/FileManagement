package xyz.dowob.filemanagement.convert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringByteCodeMapper 編解碼轉換測試類別。
 * 
 * <p>測試 {@link xyz.dowob.filemanagement.convert.StringByteCodeMapper} 中的 Redis 編解碼功能。
 * 驗證 RedisCodec&lt;String, byte[]&gt; 接口實現的正確性，包括 decodeKey()、decodeValue()、encodeKey() 和 encodeValue() 方法。
 * 確保 UTF-8 字符編碼的正確性和雙向轉換的一致性，包括特殊字符、邊界值和異常情況的處理。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StringByteCodeMapper 測試")
class StringByteCodeMapperTest {

    private StringByteCodeMapper codec;

    @BeforeEach
    void setUp() {
        codec = new StringByteCodeMapper();
    }

    // ==================== decodeKey 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 基本字符串解碼")
    void testDecodeKey_BasicString() {
        // 準備測試資料
        String originalString = "hello";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        String result = codec.decodeKey(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(originalString, result);
    }

    @Test
    @DisplayName("一般測試 - 空字符串解碼")
    void testDecodeKey_EmptyString() {
        // 準備測試資料
        String originalString = "";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        String result = codec.decodeKey(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(originalString, result);
    }

    @Test
    @DisplayName("一般測試 - 中文字符串解碼")
    void testDecodeKey_ChineseString() {
        // 準備測試資料
        String originalString = "你好世界";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        String result = codec.decodeKey(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(originalString, result);
    }

    @Test
    @DisplayName("一般測試 - 英文數字混合字符串解碼")
    void testDecodeKey_AlphanumericString() {
        // 準備測試資料
        String originalString = "Test123ABC";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        String result = codec.decodeKey(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(originalString, result);
    }

    // ==================== decodeKey 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 特殊字符解碼")
    void testDecodeKey_SpecialCharacters() {
        // 準備測試資料
        String originalString = "!@#$%^&*()_+-={}[]|\\:;\"'<>?,./";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        String result = codec.decodeKey(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(originalString, result);
    }

    @Test
    @DisplayName("邊界測試 - Unicode 字符解碼")
    void testDecodeKey_UnicodeCharacters() {
        // 準備測試資料：包含各種 Unicode 字符
        String originalString = "🌟🚀💻🎉";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        String result = codec.decodeKey(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(originalString, result);
    }

    @Test
    @DisplayName("邊界測試 - 長字符串解碼")
    void testDecodeKey_LongString() {
        // 準備測試資料：創建一個很長的字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("LongString").append(i);
        }
        String originalString = sb.toString();
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        String result = codec.decodeKey(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(originalString, result);
    }

    // ==================== decodeValue 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 基本 byte 陣列解碼")
    void testDecodeValue_BasicByteArray() {
        // 準備測試資料
        byte[] originalBytes = {1, 2, 3, 4, 5};
        ByteBuffer buffer = ByteBuffer.wrap(originalBytes);
        
        // 執行測試
        byte[] result = codec.decodeValue(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertArrayEquals(originalBytes, result);
    }

    @Test
    @DisplayName("一般測試 - 空 byte 陣列解碼")
    void testDecodeValue_EmptyByteArray() {
        // 準備測試資料
        byte[] originalBytes = {};
        ByteBuffer buffer = ByteBuffer.wrap(originalBytes);
        
        // 執行測試
        byte[] result = codec.decodeValue(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertArrayEquals(originalBytes, result);
    }

    @Test
    @DisplayName("一般測試 - 包含負數的 byte 陣列解碼")
    void testDecodeValue_NegativeBytes() {
        // 準備測試資料
        byte[] originalBytes = {-1, -50, -128, 127, 0};
        ByteBuffer buffer = ByteBuffer.wrap(originalBytes);
        
        // 執行測試
        byte[] result = codec.decodeValue(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertArrayEquals(originalBytes, result);
    }

    // ==================== decodeValue 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 大型 byte 陣列解碼")
    void testDecodeValue_LargeByteArray() {
        // 準備測試資料：創建一個大的 byte 陣列
        byte[] originalBytes = new byte[10000];
        for (int i = 0; i < originalBytes.length; i++) {
            originalBytes[i] = (byte) (i % 256 - 128);
        }
        ByteBuffer buffer = ByteBuffer.wrap(originalBytes);
        
        // 執行測試
        byte[] result = codec.decodeValue(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertArrayEquals(originalBytes, result);
    }

    @Test
    @DisplayName("邊界測試 - Byte 極值陣列解碼")
    void testDecodeValue_ExtremeValues() {
        // 準備測試資料
        byte[] originalBytes = {Byte.MIN_VALUE, Byte.MAX_VALUE, 0};
        ByteBuffer buffer = ByteBuffer.wrap(originalBytes);
        
        // 執行測試
        byte[] result = codec.decodeValue(buffer);
        
        // 驗證結果
        assertNotNull(result);
        assertArrayEquals(originalBytes, result);
    }

    // ==================== encodeKey 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 基本字符串編碼")
    void testEncodeKey_BasicString() {
        // 準備測試資料
        String originalString = "hello";
        ByteBuffer expectedBuffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        ByteBuffer result = codec.encodeKey(originalString);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(expectedBuffer, result);
    }

    @Test
    @DisplayName("一般測試 - 空字符串編碼")
    void testEncodeKey_EmptyString() {
        // 準備測試資料
        String originalString = "";
        
        // 執行測試
        ByteBuffer result = codec.encodeKey(originalString);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(0, result.remaining());
    }

    @Test
    @DisplayName("一般測試 - 中文字符串編碼")
    void testEncodeKey_ChineseString() {
        // 準備測試資料
        String originalString = "測試字符串";
        ByteBuffer expectedBuffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        ByteBuffer result = codec.encodeKey(originalString);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(expectedBuffer, result);
    }

    // ==================== encodeKey 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - Unicode 字符編碼")
    void testEncodeKey_UnicodeCharacters() {
        // 準備測試資料
        String originalString = "🎯🔥💎⭐";
        ByteBuffer expectedBuffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        ByteBuffer result = codec.encodeKey(originalString);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(expectedBuffer, result);
    }

    @Test
    @DisplayName("邊界測試 - 長字符串編碼")
    void testEncodeKey_LongString() {
        // 準備測試資料
        String originalString = "A".repeat(5000);
        ByteBuffer expectedBuffer = StandardCharsets.UTF_8.encode(originalString);
        
        // 執行測試
        ByteBuffer result = codec.encodeKey(originalString);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(expectedBuffer, result);
    }

    // ==================== encodeValue 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 基本 byte 陣列編碼")
    void testEncodeValue_BasicByteArray() {
        // 準備測試資料
        byte[] originalBytes = {10, 20, 30, 40, 50};
        
        // 執行測試
        ByteBuffer result = codec.encodeValue(originalBytes);
        
        // 驗證結果
        assertNotNull(result);
        byte[] resultBytes = new byte[result.remaining()];
        result.get(resultBytes);
        assertArrayEquals(originalBytes, resultBytes);
    }

    @Test
    @DisplayName("一般測試 - 空 byte 陣列編碼")
    void testEncodeValue_EmptyByteArray() {
        // 準備測試資料
        byte[] originalBytes = {};
        
        // 執行測試
        ByteBuffer result = codec.encodeValue(originalBytes);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals(0, result.remaining());
    }

    // ==================== encodeValue 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 包含極值的 byte 陣列編碼")
    void testEncodeValue_ExtremeValues() {
        // 準備測試資料
        byte[] originalBytes = {Byte.MIN_VALUE, Byte.MAX_VALUE, 0, -1, 1};
        
        // 執行測試
        ByteBuffer result = codec.encodeValue(originalBytes);
        
        // 驗證結果
        assertNotNull(result);
        byte[] resultBytes = new byte[result.remaining()];
        result.get(resultBytes);
        assertArrayEquals(originalBytes, resultBytes);
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - decodeKey 傳入 null ByteBuffer")
    void testDecodeKey_NullByteBuffer() {
        // 執行測試並驗證異常
        assertThrows(NullPointerException.class, () -> {
            codec.decodeKey(null);
        });
    }

    @Test
    @DisplayName("異常測試 - decodeValue 傳入 null ByteBuffer")
    void testDecodeValue_NullByteBuffer() {
        // 執行測試並驗證異常
        assertThrows(NullPointerException.class, () -> {
            codec.decodeValue(null);
        });
    }

    @Test
    @DisplayName("異常測試 - encodeKey 傳入 null String")
    void testEncodeKey_NullString() {
        // 執行測試並驗證異常
        assertThrows(NullPointerException.class, () -> {
            codec.encodeKey(null);
        });
    }

    @Test
    @DisplayName("異常測試 - encodeValue 傳入 null byte 陣列")
    void testEncodeValue_NullByteArray() {
        // 執行測試並驗證異常
        assertThrows(NullPointerException.class, () -> {
            codec.encodeValue(null);
        });
    }

    // ==================== 雙向轉換測試 ====================

    @Test
    @DisplayName("雙向轉換測試 - String 編碼解碼一致性")
    void testBidirectionalConversion_StringKey() {
        // 準備測試資料
        String originalString = "TestKey123你好";
        
        // 執行雙向轉換
        ByteBuffer encoded = codec.encodeKey(originalString);
        String decoded = codec.decodeKey(encoded);
        
        // 驗證結果
        assertEquals(originalString, decoded);
    }

    @Test
    @DisplayName("雙向轉換測試 - byte[] 編碼解碼一致性")
    void testBidirectionalConversion_ByteArrayValue() {
        // 準備測試資料
        byte[] originalBytes = {-128, -1, 0, 1, 127, 50, -50};
        
        // 執行雙向轉換
        ByteBuffer encoded = codec.encodeValue(originalBytes);
        byte[] decoded = codec.decodeValue(encoded);
        
        // 驗證結果
        assertArrayEquals(originalBytes, decoded);
    }

    @Test
    @DisplayName("雙向轉換測試 - 複雜場景")
    void testBidirectionalConversion_ComplexScenario() {
        // 準備測試資料
        String[] testStrings = {
            "",
            "simple",
            "中文測試",
            "🌟🚀💻",
            "Special!@#$%^&*()chars",
            "VeryLongString".repeat(100)
        };
        
        byte[][] testByteArrays = {
            {},
            {0},
            {1, 2, 3, 4, 5},
            {-128, -1, 0, 1, 127},
            new byte[1000] // 大陣列
        };
        
        // 填充大陣列
        Arrays.fill(testByteArrays[4], (byte) 42);
        
        // 測試所有字符串
        for (String testString : testStrings) {
            ByteBuffer encoded = codec.encodeKey(testString);
            String decoded = codec.decodeKey(encoded);
            assertEquals(testString, decoded, "字符串雙向轉換失敗: " + testString);
        }
        
        // 測試所有 byte 陣列
        for (byte[] testBytes : testByteArrays) {
            ByteBuffer encoded = codec.encodeValue(testBytes);
            byte[] decoded = codec.decodeValue(encoded);
            assertArrayEquals(testBytes, decoded, "byte 陣列雙向轉換失敗");
        }
    }

    // ==================== 併發測試 ====================

    @Test
    @DisplayName("併發測試 - 多線程同時編解碼")
    void testConcurrentEncodeDecode() throws InterruptedException {
        // 準備測試資料
        int threadCount = 10;
        int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        final boolean[] results = new boolean[threadCount];
        
        // 創建多個線程同時進行編解碼操作
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String testString = "Thread" + threadIndex + "Op" + j;
                        byte[] testBytes = {(byte) threadIndex, (byte) j, (byte) (threadIndex + j)};
                        
                        // String 編解碼測試
                        ByteBuffer encodedString = codec.encodeKey(testString);
                        String decodedString = codec.decodeKey(encodedString);
                        if (!testString.equals(decodedString)) {
                            results[threadIndex] = false;
                            return;
                        }
                        
                        // byte[] 編解碼測試
                        ByteBuffer encodedBytes = codec.encodeValue(testBytes);
                        byte[] decodedBytes = codec.decodeValue(encodedBytes);
                        if (!Arrays.equals(testBytes, decodedBytes)) {
                            results[threadIndex] = false;
                            return;
                        }
                    }
                    results[threadIndex] = true;
                } catch (Exception e) {
                    results[threadIndex] = false;
                }
            });
        }
        
        // 啟動所有線程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有線程完成
        for (Thread thread : threads) {
            thread.join(5000); // 最多等待 5 秒
        }
        
        // 驗證所有線程都成功完成
        for (int i = 0; i < threadCount; i++) {
            assertTrue(results[i], "線程 " + i + " 執行失敗");
        }
    }

    // ==================== 性能測試 ====================

    @Test
    @DisplayName("性能測試 - 大量編解碼操作")
    void testPerformance_MassEncodeDecode() {
        // 準備測試資料
        String testString = "PerformanceTestString測試";
        byte[] testBytes = {1, 2, 3, 4, 5, -1, -2, -3};
        int iterations = 10000;
        
        long startTime = System.currentTimeMillis();
        
        // 執行大量編解碼操作
        for (int i = 0; i < iterations; i++) {
            // String 編解碼
            ByteBuffer encodedString = codec.encodeKey(testString);
            String decodedString = codec.decodeKey(encodedString);
            assertEquals(testString, decodedString);
            
            // byte[] 編解碼
            ByteBuffer encodedBytes = codec.encodeValue(testBytes);
            byte[] decodedBytes = codec.decodeValue(encodedBytes);
            assertArrayEquals(testBytes, decodedBytes);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 驗證性能：應該在合理時間內完成
        assertTrue(duration < 5000, "大量編解碼操作應該在 5 秒內完成，實際耗時: " + duration + "ms");
    }
}