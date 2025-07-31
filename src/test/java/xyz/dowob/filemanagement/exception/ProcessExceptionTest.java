package xyz.dowob.filemanagement.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 內部處理異常測試。
 * 
 * <p>測試 ProcessException 內部處理異常類別的各種功能，驗證異常構造器的正常使用（帶和不帶原因）、
 * ErrorCode 枚舉各種屬性、格式化訊息處理、異常鏈的處理，以及原因異常的整合。
 * 
 * <p>前置條件：
 * <ul>
 * <li>ProcessException 類別正常載入</li>
 * <li>ErrorCode 枚舉定義完整</li>
 * </ul>
 * 
 * <p>測試步驟：
 * <ul>
 * <li>測試異常的創建和訊息格式化</li>
 * <li>測試帶原因的異常創建</li>
 * <li>測試 ErrorCode 的各種屬性訪問</li>
 * <li>測試邊界值和特殊情況</li>
 * </ul>
 * 
 * <p>預期結果：
 * <ul>
 * <li>所有異常創建和訊息格式化功能正常</li>
 * <li>異常鏈和原因處理正確</li>
 * <li>邊界條件得到適當處理</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("ProcessException 內部處理異常測試")
class ProcessExceptionTest {

    // ==================== 一般測試 ====================

    /**
     * 測試創建不帶原因的處理異常。
     * 
     * <p>驗證使用僅有 ErrorCode 的構造器創建異常。
     * 
     * <p>前置條件：
     * <ul>
     * <li>MD5_NOT_MATCH 錯誤碼已定義</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>使用特定 ErrorCode 創建異常</li>
     * <li>驗證異常屬性的正確性</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>異常創建成功且屬性正確</li>
     * <li>異常原因為 null</li>
     * </ul>
     */
    @Test
    @DisplayName("一般測試 - 創建異常不帶原因")
    void testCreateException_withoutCause() {
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.MD5_NOT_MATCH
        );
        
        assertEquals(ProcessException.ErrorCode.MD5_NOT_MATCH, exception.getErrorCode());
        assertEquals("MD5校驗失敗", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("一般測試 - 創建異常帶格式化參數")
    void testCreateException_withFormatArguments() {
        String taskId = "task-123";
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.CANNOT_GET_FILE_STREAM, taskId
        );
        
        assertEquals(ProcessException.ErrorCode.CANNOT_GET_FILE_STREAM, exception.getErrorCode());
        assertEquals("無法獲取檔案流 任務ID：task-123", exception.getMessage());
    }

    /**
     * 測試創建帶原因和格式化參數的處理異常。
     * 
     * <p>驗證異常鏈和訊息格式化的正確處理。
     * 
     * <p>前置條件：
     * <ul>
     * <li>USER_HAVE_NOT_EXIST_SERVER_FILE 錯誤碼已定義</li>
     * <li>原因異常已準備</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>創建帶原因和格式化參數的異常</li>
     * <li>驗證異常屬性和訊息格式化</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>異常創建成功且異常鏈正確</li>
     * <li>訊息包含格式化參數和原因訊息</li>
     * </ul>
     */
    @Test
    @DisplayName("一般測試 - 創建異常帶原因和參數")
    void testCreateException_withCauseAndArguments() {
        IOException cause = new IOException("File not found");
        String serverId = "server-456";
        String userId = "user-789";
        
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.USER_HAVE_NOT_EXIST_SERVER_FILE, 
            cause, serverId, userId
        );
        
        assertEquals(ProcessException.ErrorCode.USER_HAVE_NOT_EXIST_SERVER_FILE, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("server-456"));
        assertTrue(exception.getMessage().contains("user-789"));
        assertTrue(exception.getMessage().contains("原因: java.io.IOException: File not found"));
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("一般測試 - 驗證 ErrorCode 基本屬性")
    void testErrorCode_basicProperties() {
        ProcessException.ErrorCode errorCode = ProcessException.ErrorCode.BUILD_FILE_TREE_FAILED;
        
        assertEquals(1208, errorCode.getCode());
        assertEquals("構建檔案樹失敗: %s", errorCode.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 測試不需要參數的 ErrorCode")
    void testErrorCode_noParametersRequired() {
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.MD5_NOT_MATCH
        );
        
        assertEquals("MD5校驗失敗", exception.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 創建異常帶 null 原因")
    void testCreateException_withNullCause() {
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, 
            null
        );
        
        assertEquals("無法將資料格式化為JSON", exception.getMessage());
        assertNull(exception.getCause());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - 創建異常傳入 null ErrorCode")
    void testCreateException_nullErrorCode() {
        assertThrows(NullPointerException.class, () -> {
            new ProcessException(null);
        });
    }

    /**
     * 測試缺少必需格式化參數時的異常處理。
     * 
     * <p>驗證當 ErrorCode 需要格式化參數但未提供時的錯誤處理。
     * 
     * <p>前置條件：
     * <ul>
     * <li>CANNOT_GET_FILE_STREAM 錯誤碼需要格式化參數</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>不提供參數創建異常</li>
     * <li>驗證是否拋出預期異常</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>拋出 MissingFormatArgumentException</li>
     * </ul>
     */
    @Test
    @DisplayName("異常測試 - 創建異常缺少必需的格式化參數")
    void testCreateException_missingFormatArguments() {
        assertThrows(java.util.MissingFormatArgumentException.class, () -> {
            new ProcessException(ProcessException.ErrorCode.CANNOT_GET_FILE_STREAM);
        });
    }

    @Test
    @DisplayName("異常測試 - 創建帶原因異常傳入 null ErrorCode")
    void testCreateExceptionWithCause_nullErrorCode() {
        IOException cause = new IOException("Test cause");
        
        assertThrows(NullPointerException.class, () -> {
            new ProcessException(null, cause, "arg1");
        });
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 創建異常帶複雜原因鏈")
    void testCreateException_complexCauseChain() {
        RuntimeException rootCause = new RuntimeException("Root cause");
        IOException intermediateCause = new IOException("Intermediate cause", rootCause);
        
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.SEND_MAIL_FAILED, 
            intermediateCause
        );
        
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
        assertTrue(exception.getMessage().contains("原因: java.io.IOException"));
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶 null 格式化參數")
    void testCreateException_nullFormatArguments() {
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.GRIDFS_FILE_NOT_FOUND, 
            (Object) null
        );
        
        assertTrue(exception.getMessage().contains("null"));
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶超長格式化參數")
    void testCreateException_veryLongFormatArguments() {
        String longArg = "very_long_argument_".repeat(100);
        
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.DELETE_TEMP_FILE_FAILED, 
            longArg
        );
        
        assertTrue(exception.getMessage().contains(longArg));
        assertTrue(exception.getMessage().length() > 1000);
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶特殊字符參數")
    void testCreateException_specialCharacterArguments() {
        String specialArg = "<script>alert('xss')</script>&lt;&gt;\"'";
        
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.BUILD_FILE_TREE_FAILED, 
            specialArg
        );
        
        assertTrue(exception.getMessage().contains(specialArg));
    }

    @Test
    @DisplayName("邊界測試 - 測試 buildMessage 靜態方法的邊界情況")
    void testBuildMessage_boundaryConditions() {
        // 通過創建帶原因的異常來間接測試 buildMessage 方法
        RuntimeException cause = new RuntimeException("Test cause message");
        
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.AUTHENTICATION_ERROR, 
            cause
        );
        
        String message = exception.getMessage();
        assertTrue(message.contains("認證憑證時發生意外錯誤"));
        assertTrue(message.contains("原因: java.lang.RuntimeException: Test cause message"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證所有 ErrorCode 枚舉值的完整性")
    void testAllErrorCodes_completeness() {
        ProcessException.ErrorCode[] allCodes = ProcessException.ErrorCode.values();
        
        // 驗證至少有預期數量的錯誤碼
        assertTrue(allCodes.length >= 10, "應該有足夠數量的 ErrorCode");
        
        // 驗證每個錯誤碼的基本屬性
        for (ProcessException.ErrorCode errorCode : allCodes) {
            assertTrue(errorCode.getCode() > 0, 
                "ErrorCode " + errorCode.name() + " 的錯誤碼應為正數");
            assertNotNull(errorCode.getMessage(), 
                "ErrorCode " + errorCode.name() + " 的錯誤訊息不應為null");
            assertFalse(errorCode.getMessage().trim().isEmpty(), 
                "ErrorCode " + errorCode.name() + " 的錯誤訊息不應為空字符串");
        }
    }

    @Test
    @DisplayName("邊界測試 - 驗證錯誤碼的唯一性")
    void testErrorCodes_uniqueness() {
        ProcessException.ErrorCode[] allCodes = ProcessException.ErrorCode.values();
        
        for (int i = 0; i < allCodes.length; i++) {
            for (int j = i + 1; j < allCodes.length; j++) {
                assertNotEquals(allCodes[i].getCode(), allCodes[j].getCode(),
                    "發現重複的錯誤碼: " + allCodes[i].name() + " 和 " + allCodes[j].name() + 
                    " 都使用錯誤碼 " + allCodes[i].getCode());
            }
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試異常繼承關係")
    void testException_inheritance() {
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.MD5_NOT_MATCH
        );
        
        assertInstanceOf(Exception.class, exception);
        assertInstanceOf(Throwable.class, exception);
    }

    @Test
    @DisplayName("邊界測試 - 測試異常序列化兼容性")
    void testException_serialization() {
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED
        );
        
        assertInstanceOf(java.io.Serializable.class, exception);
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶多個格式化參數")
    void testCreateException_multipleFormatArguments() {
        String userId = "user123";
        String folderId = "folder456";
        
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.UPDATE_FOLDER_TREE_FAILED, 
            userId, folderId
        );
        
        assertTrue(exception.getMessage().contains(userId));
        assertTrue(exception.getMessage().contains(folderId));
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - 測試帶原因的異常堆棧跟踪")
    void testExceptionWithCause_stackTrace() {
        RuntimeException cause = new RuntimeException("Original exception");
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.SEND_MAIL_FAILED, 
            cause
        );
        
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
        
        // 驗證原因異常也有堆棧跟踪
        StackTraceElement[] causeStackTrace = exception.getCause().getStackTrace();
        assertNotNull(causeStackTrace);
        assertTrue(causeStackTrace.length > 0);
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶 Unicode 字符參數")
    void testCreateException_unicodeArguments() {
        String unicodeArg = "檔案路徑：/用戶/文檔/📁重要檔案夾/🔒機密檔案.txt";
        
        ProcessException exception = new ProcessException(
            ProcessException.ErrorCode.DELETE_TEMP_FILE_FAILED, 
            unicodeArg
        );
        
        assertTrue(exception.getMessage().contains("📁"));
        assertTrue(exception.getMessage().contains("🔒"));
        assertTrue(exception.getMessage().contains(unicodeArg));
    }

    @Test
    @DisplayName("邊界測試 - 驗證修復後的錯誤碼正確性")
    void testFixedErrorCodes_correctness() {
        // 驗證之前重複的錯誤碼現在已經分別使用不同的編號
        ProcessException exception1 = new ProcessException(
            ProcessException.ErrorCode.CREATE_STREAM_FAILED
        );
        ProcessException exception2 = new ProcessException(
            ProcessException.ErrorCode.WRITE_CACHE_TO_REDIS_FAILED
        );
        
        // 驗證兩個 ErrorCode 現在使用不同的錯誤碼
        assertEquals(1201, exception1.getErrorCode().getCode());
        assertEquals(1218, exception2.getErrorCode().getCode());
        assertNotEquals(exception1.getErrorCode().getCode(), exception2.getErrorCode().getCode());
        
        // 驗證訊息也不同
        assertNotEquals(exception1.getMessage(), exception2.getMessage());
        assertEquals("創建檔案流失敗", exception1.getMessage());
        assertEquals("寫入緩存到 Redis 失敗", exception2.getMessage());
    }
}