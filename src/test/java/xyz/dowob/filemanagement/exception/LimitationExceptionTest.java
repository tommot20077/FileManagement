package xyz.dowob.filemanagement.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用戶限制異常測試。
 * 
 * <p>測試 LimitationException 用戶限制異常類別的各種功能，驗證異常構造器的正常使用、
 * ErrorCode 枚舉各種屬性、格式化訊息處理，以及與 HTTP 狀態碼的整合。
 * 
 * <p>前置條件：
 * <ul>
 * <li>LimitationException 類別正常載入</li>
 * <li>ErrorCode 枚舉定義完整</li>
 * </ul>
 * 
 * <p>測試步驟：
 * <ul>
 * <li>測試異常的創建和訊息格式化</li>
 * <li>測試 ErrorCode 的各種屬性訪問</li>
 * <li>測試限制相關的業務邏輯</li>
 * <li>測試邊界值和特殊情況</li>
 * </ul>
 * 
 * <p>預期結果：
 * <ul>
 * <li>所有異常創建和訊息格式化功能正常</li>
 * <li>ErrorCode 的各種屬性正確設置</li>
 * <li>邊界條件得到適當處理</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("LimitationException 用戶限制異常測試")
class LimitationExceptionTest {

    // ==================== 一般測試 ====================

    /**
     * 測試創建用戶超出限制異常。
     * 
     * <p>驗證當用戶操作超出限制時異常的正確創建和訊息處理。
     * 
     * <p>前置條件：
     * <ul>
     * <li>USER_EXCEED_LIMIT 錯誤碼已定義</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>使用特定錯誤碼和訊息創建異常</li>
     * <li>驗證異常屬性的正確性</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>異常創建成功且錯誤碼正確</li>
     * <li>異常訊息與預期一致</li>
     * </ul>
     */
    @Test
    @DisplayName("一般測試 - 創建用戶超出限制異常")
    void testCreateException_userExceedLimit() {
        String limitMessage = "用戶每分鐘最多只能上傳 5 個檔案";
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.USER_EXCEED_LIMIT, limitMessage
        );
        
        assertEquals(LimitationException.ErrorCode.USER_EXCEED_LIMIT, exception.getErrorCode());
        assertEquals(limitMessage, exception.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 創建檔案分塊超出限制異常")
    void testCreateException_fileChunkExceedLimit() {
        String chunkMessage = "檔案分塊請求超出限制，每秒最多 10 次請求";
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.FILE_CHUNK_EXCEED_LIMIT, chunkMessage
        );
        
        assertEquals(LimitationException.ErrorCode.FILE_CHUNK_EXCEED_LIMIT, exception.getErrorCode());
        assertEquals(chunkMessage, exception.getMessage());
    }

    /**
     * 測試 ErrorCode 的基本屬性。
     * 
     * <p>驗證 LimitationException.ErrorCode 枚舉值的基本屬性設定是否正確。
     * 
     * <p>前置條件：
     * <ul>
     * <li>USER_EXCEED_LIMIT 錯誤碼已定義</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>獲取 ErrorCode 的錯誤碼、HTTP 狀態碼和訊息</li>
     * <li>驗證每個屬性的正確性</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>錯誤碼、HTTP 狀態碼和訊息符合預期</li>
     * </ul>
     */
    @Test
    @DisplayName("一般測試 - 驗證 ErrorCode 基本屬性")
    void testErrorCode_basicProperties() {
        LimitationException.ErrorCode userExceedLimit = LimitationException.ErrorCode.USER_EXCEED_LIMIT;
        
        assertEquals(1301, userExceedLimit.getCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, userExceedLimit.getHttpStatus());
        assertEquals("%s", userExceedLimit.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 驗證檔案分塊限制 ErrorCode 屬性")
    void testErrorCode_fileChunkLimitProperties() {
        LimitationException.ErrorCode fileChunkLimit = LimitationException.ErrorCode.FILE_CHUNK_EXCEED_LIMIT;
        
        assertEquals(1302, fileChunkLimit.getCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, fileChunkLimit.getHttpStatus());
        assertEquals("%s", fileChunkLimit.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 驗證所有 ErrorCode 使用相同的 HTTP 狀態碼")
    void testErrorCode_allUseSameHttpStatus() {
        for (LimitationException.ErrorCode errorCode : LimitationException.ErrorCode.values()) {
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, errorCode.getHttpStatus(),
                "所有 LimitationException.ErrorCode 都應該使用 TOO_MANY_REQUESTS 狀態碼");
        }
    }

    @Test
    @DisplayName("一般測試 - 創建異常帶複雜格式化參數")
    void testCreateException_complexFormatting() {
        String detailedMessage = String.format(
            "用戶 ID: %s 在 %s 操作超出限制，當前請求數: %d，限制數: %d",
            "user123", "檔案上傳", 15, 10
        );
        
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.USER_EXCEED_LIMIT, detailedMessage
        );
        
        assertTrue(exception.getMessage().contains("user123"));
        assertTrue(exception.getMessage().contains("檔案上傳"));
        assertTrue(exception.getMessage().contains("15"));
        assertTrue(exception.getMessage().contains("10"));
    }

    // ==================== 異常測試 ====================

    /**
     * 測試傳入 null ErrorCode 時的異常處理。
     * 
     * <p>驗證當傳入 null ErrorCode 時應該拋出 NullPointerException。
     * 
     * <p>前置條件：
     * <ul>
     * <li>異常構造器不允許 null ErrorCode</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>以 null ErrorCode 嘗試創建異常</li>
     * <li>驗證是否拋出預期異常</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>拋出 NullPointerException</li>
     * </ul>
     */
    @Test
    @DisplayName("異常測試 - 創建異常傳入 null ErrorCode")
    void testCreateException_nullErrorCode() {
        assertThrows(NullPointerException.class, () -> {
            new LimitationException(null, "test message");
        });
    }

    @Test
    @DisplayName("異常測試 - 創建異常不傳入格式化參數")
    void testCreateException_noFormatArguments() {
        // 當 ErrorCode 的 message 包含 %s 但沒有提供參數時，應該拋出異常
        assertThrows(java.util.MissingFormatArgumentException.class, () -> {
            new LimitationException(LimitationException.ErrorCode.USER_EXCEED_LIMIT);
        });
    }

    @Test
    @DisplayName("異常測試 - 創建異常傳入 null 格式化參數")
    void testCreateException_nullFormatArgument() {
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.USER_EXCEED_LIMIT, (Object) null
        );
        
        assertEquals("null", exception.getMessage());
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 創建異常帶空字符串參數")
    void testCreateException_emptyStringArgument() {
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.USER_EXCEED_LIMIT, ""
        );
        
        assertEquals("", exception.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶超長字符串參數")
    void testCreateException_veryLongStringArgument() {
        String longMessage = "用戶限制訊息：" + "詳細信息".repeat(500);
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.USER_EXCEED_LIMIT, longMessage
        );
        
        assertEquals(longMessage, exception.getMessage());
        assertTrue(exception.getMessage().length() > 2000);
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶特殊字符參數")
    void testCreateException_specialCharactersArgument() {
        String specialMessage = "限制信息：<script>alert('xss')</script>&lt;&gt;\"'";
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.FILE_CHUNK_EXCEED_LIMIT, specialMessage
        );
        
        assertEquals(specialMessage, exception.getMessage());
        assertTrue(exception.getMessage().contains("<script>"));
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶多個格式化參數（但只需要一個）")
    void testCreateException_tooManyFormatArguments() {
        // ErrorCode 的 message 只有一個 %s，但提供多個參數
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.USER_EXCEED_LIMIT, 
            "primary message", "extra1", "extra2"
        );
        
        assertEquals("primary message", exception.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶數字參數")
    void testCreateException_numericArguments() {
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.FILE_CHUNK_EXCEED_LIMIT, 
            "請求頻率超限：當前 100 次/秒，限制 50 次/秒"
        );
        
        assertTrue(exception.getMessage().contains("100"));
        assertTrue(exception.getMessage().contains("50"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證所有 ErrorCode 枚舉值的完整性")
    void testAllErrorCodes_completeness() {
        LimitationException.ErrorCode[] allCodes = LimitationException.ErrorCode.values();
        
        // 驗證至少有預期的錯誤碼
        assertTrue(allCodes.length >= 2, "至少應該有 2 個 ErrorCode");
        
        // 驗證每個錯誤碼的基本屬性
        for (LimitationException.ErrorCode errorCode : allCodes) {
            assertTrue(errorCode.getCode() > 0, 
                "ErrorCode " + errorCode.name() + " 的錯誤碼應為正數");
            assertNotNull(errorCode.getHttpStatus(), 
                "ErrorCode " + errorCode.name() + " 的HTTP狀態碼不應為null");
            assertNotNull(errorCode.getMessage(), 
                "ErrorCode " + errorCode.name() + " 的錯誤訊息不應為null");
        }
    }

    @Test
    @DisplayName("邊界測試 - 驗證錯誤碼的唯一性")
    void testErrorCodes_uniqueness() {
        LimitationException.ErrorCode[] allCodes = LimitationException.ErrorCode.values();
        
        for (int i = 0; i < allCodes.length; i++) {
            for (int j = i + 1; j < allCodes.length; j++) {
                assertNotEquals(allCodes[i].getCode(), allCodes[j].getCode(),
                    "發現重複的錯誤碼: " + allCodes[i].name() + " 和 " + allCodes[j].name() + 
                    " 都使用錯誤碼 " + allCodes[i].getCode());
            }
        }
    }

    @Test
    @DisplayName("邊界測試 - 驗證異常繼承關係")
    void testException_inheritance() {
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.USER_EXCEED_LIMIT, "test"
        );
        
        assertInstanceOf(Exception.class, exception);
        assertInstanceOf(Throwable.class, exception);
    }

    @Test
    @DisplayName("邊界測試 - 驗證異常可序列化")
    void testException_serializable() {
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.FILE_CHUNK_EXCEED_LIMIT, "serialization test"
        );
        
        assertInstanceOf(java.io.Serializable.class, exception);
    }

    @Test
    @DisplayName("邊界測試 - 測試 ErrorCode 枚舉的 toString 方法")
    void testErrorCode_toString() {
        LimitationException.ErrorCode errorCode = LimitationException.ErrorCode.USER_EXCEED_LIMIT;
        String toStringResult = errorCode.toString();
        
        assertEquals("USER_EXCEED_LIMIT", toStringResult);
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶 Unicode 字符參數")
    void testCreateException_unicodeArguments() {
        String unicodeMessage = "用戶限制：🚫 超出上傳限制 📁 請稍後再試 ⏰";
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.USER_EXCEED_LIMIT, unicodeMessage
        );
        
        assertEquals(unicodeMessage, exception.getMessage());
        assertTrue(exception.getMessage().contains("🚫"));
        assertTrue(exception.getMessage().contains("📁"));
        assertTrue(exception.getMessage().contains("⏰"));
    }
}