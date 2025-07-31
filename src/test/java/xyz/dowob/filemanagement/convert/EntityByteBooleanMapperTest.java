package xyz.dowob.filemanagement.convert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.dowob.filemanagement.convert.EntityByteBooleanMapper.BooleanToByteConverter;
import xyz.dowob.filemanagement.convert.EntityByteBooleanMapper.ByteToBooleanConverter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EntityByteBooleanMapper 數據類型轉換測試類別。
 * 
 * <p>測試 {@link xyz.dowob.filemanagement.convert.EntityByteBooleanMapper} 中的 ByteToBooleanConverter 和 BooleanToByteConverter 轉換器。
 * 驗證 MySQL TINYINT(1) 與 Boolean 之間的雙向轉換正確性，包括正常值、邊界值和異常情況的處理。
 * 確保轉換邏輯準確性和資料一致性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntityByteBooleanMapper 測試")
class EntityByteBooleanMapperTest {

    private ByteToBooleanConverter byteToBooleanConverter;
    private BooleanToByteConverter booleanToByteConverter;

    @BeforeEach
    void setUp() {
        byteToBooleanConverter = new ByteToBooleanConverter();
        booleanToByteConverter = new BooleanToByteConverter();
    }

    // ==================== ByteToBooleanConverter 一般測試 ====================

    /**
     * 測試 Byte 0 轉換為 Boolean false。
     * 
     * 驗證 ByteToBooleanConverter 將 Byte 0 正確轉換為 Boolean false 的基本功能。
     * 
     * 前置條件：
     * - ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備 Byte 0 作為輸入
     * - 執行轉換操作
     * - 驗證結果為 false
     * 
     * 預期結果：
     * - 轉換成功且結果為 Boolean false
     */
    @Test
    @DisplayName("一般測試 - Byte 0 轉換為 false")
    void testByteToBooleanConverter_ZeroToFalse() {
        // 準備測試資料
        Byte input = (byte) 0;
        
        // 執行測試
        Boolean result = byteToBooleanConverter.convert(input);
        
        // 驗證結果
        assertNotNull(result);
        assertFalse(result);
    }

    /**
     * 測試 Byte 1 轉換為 Boolean true。
     * 
     * 驗證 ByteToBooleanConverter 將 Byte 1 正確轉換為 Boolean true 的基本功能。
     * 
     * 前置條件：
     * - ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備 Byte 1 作為輸入
     * - 執行轉換操作
     * - 驗證結果為 true
     * 
     * 預期結果：
     * - 轉換成功且結果為 Boolean true
     */
    @Test
    @DisplayName("一般測試 - Byte 1 轉換為 true")
    void testByteToBooleanConverter_OneToTrue() {
        // 準備測試資料
        Byte input = (byte) 1;
        
        // 執行測試
        Boolean result = byteToBooleanConverter.convert(input);
        
        // 驗證結果
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * 測試正數 Byte 轉換為 Boolean true。
     * 
     * 驗證 ByteToBooleanConverter 將任意正數 Byte 值正確轉換為 Boolean true。
     * 
     * 前置條件：
     * - ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備正數 Byte 5 作為輸入
     * - 執行轉換操作
     * - 驗證結果為 true
     * 
     * 預期結果：
     * - 轉換成功且結果為 Boolean true
     */
    @Test
    @DisplayName("一般測試 - 正數 Byte 轉換為 true")
    void testByteToBooleanConverter_PositiveToTrue() {
        // 準備測試資料
        Byte input = (byte) 5;
        
        // 執行測試
        Boolean result = byteToBooleanConverter.convert(input);
        
        // 驗證結果
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * 測試負數 Byte 轉換為 Boolean true。
     * 
     * 驗證 ByteToBooleanConverter 將任意負數 Byte 值正確轉換為 Boolean true。
     * 
     * 前置條件：
     * - ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備負數 Byte -1 作為輸入
     * - 執行轉換操作
     * - 驗證結果為 true
     * 
     * 預期結果：
     * - 轉換成功且結果為 Boolean true
     */
    @Test
    @DisplayName("一般測試 - 負數 Byte 轉換為 true")
    void testByteToBooleanConverter_NegativeToTrue() {
        // 準備測試資料
        Byte input = (byte) -1;
        
        // 執行測試
        Boolean result = byteToBooleanConverter.convert(input);
        
        // 驗證結果
        assertNotNull(result);
        assertTrue(result);
    }

    // ==================== ByteToBooleanConverter 邊界測試 ====================

    /**
     * 測試 Byte 最大值轉換為 Boolean true。
     * 
     * 驗證 ByteToBooleanConverter 在處理極值 Byte.MAX_VALUE (127) 時的正確性。
     * 
     * 前置條件：
     * - ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備 Byte.MAX_VALUE (127) 作為輸入
     * - 執行轉換操作
     * - 驗證結果為 true
     * 
     * 預期結果：
     * - 轉換成功且結果為 Boolean true
     */
    @Test
    @DisplayName("邊界測試 - Byte 最大值轉換為 true")
    void testByteToBooleanConverter_MaxValueToTrue() {
        // 準備測試資料
        Byte input = Byte.MAX_VALUE; // 127
        
        // 執行測試
        Boolean result = byteToBooleanConverter.convert(input);
        
        // 驗證結果
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * 測試 Byte 最小值轉換為 Boolean true。
     * 
     * 驗證 ByteToBooleanConverter 在處理極值 Byte.MIN_VALUE (-128) 時的正確性。
     * 
     * 前置條件：
     * - ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備 Byte.MIN_VALUE (-128) 作為輸入
     * - 執行轉換操作
     * - 驗證結果為 true
     * 
     * 預期結果：
     * - 轉換成功且結果為 Boolean true
     */
    @Test
    @DisplayName("邊界測試 - Byte 最小值轉換為 true")
    void testByteToBooleanConverter_MinValueToTrue() {
        // 準備測試資料
        Byte input = Byte.MIN_VALUE; // -128
        
        // 執行測試
        Boolean result = byteToBooleanConverter.convert(input);
        
        // 驗證結果
        assertNotNull(result);
        assertTrue(result);
    }

    // ==================== ByteToBooleanConverter 異常測試 ====================

    /**
     * 測試傳入 null Byte 應拋出 NullPointerException。
     * 
     * 驗證 ByteToBooleanConverter 在處理 null 輸入時的異常處理機制。
     * 
     * 前置條件：
     * - ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 傳入 null 作為參數
     * - 執行轉換操作
     * - 驗證拋出 NullPointerException
     * 
     * 預期結果：
     * - 拋出 NullPointerException 異常
     */
    @Test
    @DisplayName("異常測試 - 傳入 null Byte 應拋出異常")
    void testByteToBooleanConverter_NullInput() {
        // 執行測試並驗證異常
        assertThrows(NullPointerException.class, () -> {
            byteToBooleanConverter.convert(null);
        });
    }

    // ==================== BooleanToByteConverter 一般測試 ====================

    /**
     * 測試 Boolean true 轉換為 Byte 1。
     * 
     * 驗證 BooleanToByteConverter 將 Boolean true 正確轉換為 Byte 1 的基本功能。
     * 
     * 前置條件：
     * - BooleanToByteConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備 Boolean true 作為輸入
     * - 執行轉換操作
     * - 驗證結果為 Byte 1
     * 
     * 預期結果：
     * - 轉換成功且結果為 Byte 1
     */
    @Test
    @DisplayName("一般測試 - Boolean true 轉換為 Byte 1")
    void testBooleanToByteConverter_TrueToOne() {
        // 準備測試資料
        Boolean input = true;
        
        // 執行測試
        Byte result = booleanToByteConverter.convert(input);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals((byte) 1, result);
    }

    /**
     * 測試 Boolean false 轉換為 Byte 0。
     * 
     * 驗證 BooleanToByteConverter 將 Boolean false 正確轉換為 Byte 0 的基本功能。
     * 
     * 前置條件：
     * - BooleanToByteConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備 Boolean false 作為輸入
     * - 執行轉換操作
     * - 驗證結果為 Byte 0
     * 
     * 預期結果：
     * - 轉換成功且結果為 Byte 0
     */
    @Test
    @DisplayName("一般測試 - Boolean false 轉換為 Byte 0")
    void testBooleanToByteConverter_FalseToZero() {
        // 準備測試資料
        Boolean input = false;
        
        // 執行測試
        Byte result = booleanToByteConverter.convert(input);
        
        // 驗證結果
        assertNotNull(result);
        assertEquals((byte) 0, result);
    }

    // ==================== BooleanToByteConverter 異常測試 ====================

    /**
     * 測試傳入 null Boolean 應拋出 NullPointerException。
     * 
     * 驗證 BooleanToByteConverter 在處理 null 輸入時的異常處理機制。
     * 
     * 前置條件：
     * - BooleanToByteConverter 實例已初始化
     * 
     * 測試步驟：
     * - 傳入 null 作為參數
     * - 執行轉換操作
     * - 驗證拋出 NullPointerException
     * 
     * 預期結果：
     * - 拋出 NullPointerException 異常
     */
    @Test
    @DisplayName("異常測試 - 傳入 null Boolean 應拋出異常")
    void testBooleanToByteConverter_NullInput() {
        // 執行測試並驗證異常
        assertThrows(NullPointerException.class, () -> {
            booleanToByteConverter.convert(null);
        });
    }

    // ==================== 雙向轉換測試 ====================

    /**
     * 測試雙向轉換 true -> 1 -> true 的一致性。
     * 
     * 驗證 Boolean true 經過雙向轉換後仍保持原始值的一致性。
     * 
     * 前置條件：
     * - BooleanToByteConverter 和 ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備 Boolean true 作為輸入
     * - 執行 Boolean -> Byte 轉換
     * - 執行 Byte -> Boolean 轉換
     * - 驗證結果一致性
     * 
     * 預期結果：
     * - 雙向轉換後值保持一致
     */
    @Test
    @DisplayName("雙向轉換測試 - true -> 1 -> true")
    void testBidirectionalConversion_True() {
        // 準備測試資料
        Boolean originalValue = true;
        
        // 執行雙向轉換
        Byte byteValue = booleanToByteConverter.convert(originalValue);
        Boolean convertedBack = byteToBooleanConverter.convert(byteValue);
        
        // 驗證結果
        assertEquals(originalValue, convertedBack);
        assertEquals((byte) 1, byteValue);
    }

    /**
     * 測試雙向轉換 false -> 0 -> false 的一致性。
     * 
     * 驗證 Boolean false 經過雙向轉換後仍保持原始值的一致性。
     * 
     * 前置條件：
     * - BooleanToByteConverter 和 ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備 Boolean false 作為輸入
     * - 執行 Boolean -> Byte 轉換
     * - 執行 Byte -> Boolean 轉換
     * - 驗證結果一致性
     * 
     * 預期結果：
     * - 雙向轉換後值保持一致
     */
    @Test
    @DisplayName("雙向轉換測試 - false -> 0 -> false")
    void testBidirectionalConversion_False() {
        // 準備測試資料
        Boolean originalValue = false;
        
        // 執行雙向轉換
        Byte byteValue = booleanToByteConverter.convert(originalValue);
        Boolean convertedBack = byteToBooleanConverter.convert(byteValue);
        
        // 驗證結果
        assertEquals(originalValue, convertedBack);
        assertEquals((byte) 0, byteValue);
    }

    /**
     * 測試非標準 Byte 值的雙向轉換行為。
     * 
     * 驗證各種非 0 非 1 的 Byte 值在轉換過程中的正確性。
     * 
     * 前置條件：
     * - BooleanToByteConverter 和 ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 準備多個非標準 Byte 值陣列
     * - 對每個值執行 Byte -> Boolean 轉換
     * - 驗證非零值轉為 true
     * - 對 true 執行 Boolean -> Byte 轉換
     * - 驗證結果為 Byte 1
     * 
     * 預期結果：
     * - 所有非零 Byte 值轉為 true，true 轉為 Byte 1
     */
    @Test
    @DisplayName("雙向轉換測試 - 非標準 Byte 值的轉換")
    void testBidirectionalConversion_NonStandardByteValues() {
        // 測試多個非標準 Byte 值
        byte[] testValues = {2, -1, 5, -128, 127, 42};
        
        for (byte testValue : testValues) {
            // 執行 Byte -> Boolean 轉換
            Boolean booleanResult = byteToBooleanConverter.convert(testValue);
            
            // 驗證結果：所有非零值都應該轉換為 true
            assertTrue(booleanResult, "Byte 值 " + testValue + " 應該轉換為 true");
            
            // 執行 Boolean -> Byte 轉換（true 應該變成 1）
            Byte byteResult = booleanToByteConverter.convert(booleanResult);
            assertEquals((byte) 1, byteResult, "true 應該轉換為 Byte 1");
        }
    }

    // ==================== 性能測試 ====================

    /**
     * 測試大量轉換操作的性能表現。
     * 
     * 驗證轉換器在大量操作下的性能穩定性和正確性。
     * 
     * 前置條件：
     * - BooleanToByteConverter 和 ByteToBooleanConverter 實例已初始化
     * 
     * 測試步驟：
     * - 執行 10000 次雙向轉換操作
     * - 記錄執行時間
     * - 驗證每次轉換的正確性
     * 
     * 預期結果：
     * - 所有轉換都正確且在 1 秒內完成
     */
    @Test
    @DisplayName("性能測試 - 大量轉換操作")
    void testPerformance_MassConversions() {
        // 準備測試資料
        int iterations = 10000;
        long startTime = System.currentTimeMillis();
        
        // 執行大量轉換
        for (int i = 0; i < iterations; i++) {
            // Boolean -> Byte 轉換
            Boolean boolValue = (i % 2 == 0);
            Byte byteValue = booleanToByteConverter.convert(boolValue);
            
            // Byte -> Boolean 轉換
            Boolean resultValue = byteToBooleanConverter.convert(byteValue);
            
            // 驗證轉換正確性
            assertEquals(boolValue, resultValue);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 驗證性能：應該在合理時間內完成
        assertTrue(duration < 1000, "大量轉換操作應該在 1 秒內完成，實際耗時: " + duration + "ms");
    }
}