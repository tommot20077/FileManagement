package xyz.dowob.filemanagement.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 認證異常測試。
 * 
 * <p>測試 JwtAuthenticationException JWT 驗證異常類別的各種功能，驗證異常的繼承關係、構造器的正常使用、
 * 異常訊息的處理，以及與 Spring Security 整合相關特性。
 * 
 * <p>前置條件：
 * <ul>
 * <li>JwtAuthenticationException 類別正常載入</li>
 * <li>Spring Security 框架可用</li>
 * </ul>
 * 
 * <p>測試步驟：
 * <ul>
 * <li>測試異常的創建和訊息傳遞</li>
 * <li>測試繼承自 AuthenticationException 的特性</li>
 * <li>測試各種邊界條件和異常情況</li>
 * </ul>
 * 
 * <p>預期結果：
 * <ul>
 * <li>異常正確繼承 AuthenticationException</li>
 * <li>異常訊息正確傳遞和保存</li>
 * <li>各種邊界條件得到適當處理</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("JwtAuthenticationException JWT驗證異常測試")
class JwtAuthenticationExceptionTest {

    // ==================== 一般測試 ====================

    /**
     * 測試創建帶訊息的 JWT 認證異常。
     * 
     * <p>測試異常的基本創建功能和訊息傳遞。
     * 
     * <p>前置條件：
     * <ul>
     * <li>JwtAuthenticationException 類別正常可用</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>使用指定訊息創建異常</li>
     * <li>驗證異常訊息的正確性</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>異常創建成功</li>
     * <li>異常訊息與輸入訊息一致</li>
     * </ul>
     */
    @Test
    @DisplayName("一般測試 - 創建異常帶訊息")
    void testCreateException_withMessage() {
        String message = "JWT token is invalid";
        JwtAuthenticationException exception = new JwtAuthenticationException(message);
        
        assertEquals(message, exception.getMessage());
    }

    /**
     * 測試 JWT 認證異常的繼承關係。
     * 
     * <p>驗證 JwtAuthenticationException 正確繼承自 Spring Security 的 AuthenticationException。
     * 
     * <p>前置條件：
     * <ul>
     * <li>Spring Security 框架可用</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>創建 JwtAuthenticationException 實例</li>
     * <li>驗證繼承關係的正確性</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>正確繼承 AuthenticationException</li>
     * <li>符合異常層次結構</li>
     * </ul>
     */
    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritance_isAuthenticationException() {
        JwtAuthenticationException exception = new JwtAuthenticationException("test message");
        
        assertInstanceOf(AuthenticationException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
        assertInstanceOf(Exception.class, exception);
    }

    @Test
    @DisplayName("一般測試 - 創建異常帶複雜訊息")
    void testCreateException_withComplexMessage() {
        String complexMessage = "JWT 驗證失敗：令牌已過期，過期時間：2024-01-01 12:00:00";
        JwtAuthenticationException exception = new JwtAuthenticationException(complexMessage);
        
        assertEquals(complexMessage, exception.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 創建異常帶 JSON 格式訊息")
    void testCreateException_withJsonMessage() {
        String jsonMessage = "{\"error\":\"invalid_token\",\"description\":\"The JWT token is malformed\"}";
        JwtAuthenticationException exception = new JwtAuthenticationException(jsonMessage);
        
        assertEquals(jsonMessage, exception.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 驗證異常類別名稱")
    void testClassName() {
        JwtAuthenticationException exception = new JwtAuthenticationException("test");
        
        assertEquals("JwtAuthenticationException", exception.getClass().getSimpleName());
    }

    // ==================== 異常測試 ====================

    /**
     * 測試傳入 null 訊息時的異常處理。
     * 
     * <p>驗證當傳入 null 訊息時異常的正確處理。
     * 
     * <p>前置條件：
     * <ul>
     * <li>異常類別支持 null 訊息參數</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>以 null 作為訊息參數創建異常</li>
     * <li>驗證異常訊息為 null</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>異常創建成功</li>
     * <li>異常訊息為 null</li>
     * </ul>
     */
    @Test
    @DisplayName("異常測試 - 創建異常傳入 null 訊息")
    void testCreateException_nullMessage() {
        JwtAuthenticationException exception = new JwtAuthenticationException(null);
        
        assertNull(exception.getMessage());
    }

    @Test
    @DisplayName("異常測試 - 創建異常傳入空字符串")
    void testCreateException_emptyMessage() {
        JwtAuthenticationException exception = new JwtAuthenticationException("");
        
        assertEquals("", exception.getMessage());
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 創建異常帶超長訊息")
    void testCreateException_veryLongMessage() {
        String longMessage = "JWT 驗證錯誤：" + "詳細信息".repeat(1000);
        JwtAuthenticationException exception = new JwtAuthenticationException(longMessage);
        
        assertEquals(longMessage, exception.getMessage());
        assertTrue(exception.getMessage().length() > 4000, 
            "實際長度: " + exception.getMessage().length() + " 應該大於 4000");
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶特殊字符")
    void testCreateException_specialCharacters() {
        String specialMessage = "JWT錯誤：<>&\"'`\n\t\r特殊字符測試@#$%^&*()";
        JwtAuthenticationException exception = new JwtAuthenticationException(specialMessage);
        
        assertEquals(specialMessage, exception.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶 Unicode 字符")
    void testCreateException_unicodeCharacters() {
        String unicodeMessage = "JWT驗證失敗：😀🔐🚫令牌無效⚠️💀";
        JwtAuthenticationException exception = new JwtAuthenticationException(unicodeMessage);
        
        assertEquals(unicodeMessage, exception.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - 創建異常帶多行訊息")
    void testCreateException_multilineMessage() {
        String multilineMessage = "JWT 驗證錯誤：\n" +
                                 "錯誤類型：令牌無效\n" +
                                 "錯誤時間：2024-01-01 12:00:00\n" +
                                 "詳細信息：令牌簽名不匹配";
        JwtAuthenticationException exception = new JwtAuthenticationException(multilineMessage);
        
        assertEquals(multilineMessage, exception.getMessage());
        assertTrue(exception.getMessage().contains("\n"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證作為 AuthenticationException 的使用")
    void testUsage_asAuthenticationException() {
        String message = "JWT authentication failed";
        AuthenticationException exception = new JwtAuthenticationException(message);
        
        assertEquals(message, exception.getMessage());
        assertInstanceOf(JwtAuthenticationException.class, exception);
    }

    @Test
    @DisplayName("邊界測試 - 異常堆棧跟踪")
    void testException_stackTrace() {
        JwtAuthenticationException exception = new JwtAuthenticationException("Stack trace test");
        
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
        
        // 驗證堆棧跟踪包含當前測試方法
        boolean foundTestMethod = false;
        for (StackTraceElement element : stackTrace) {
            if (element.getMethodName().equals("testException_stackTrace")) {
                foundTestMethod = true;
                break;
            }
        }
        assertTrue(foundTestMethod, "堆棧跟踪應該包含當前測試方法");
    }

    @Test
    @DisplayName("邊界測試 - 異常相等性比較")
    void testException_equality() {
        String message = "Same message";
        JwtAuthenticationException exception1 = new JwtAuthenticationException(message);
        JwtAuthenticationException exception2 = new JwtAuthenticationException(message);
        
        // 異常對象應該不相等（即使訊息相同）
        assertNotEquals(exception1, exception2);
        
        // 但訊息應該相等
        assertEquals(exception1.getMessage(), exception2.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - toString 方法")
    void testException_toString() {
        String message = "JWT token validation failed";
        JwtAuthenticationException exception = new JwtAuthenticationException(message);
        
        String toStringResult = exception.toString();
        assertNotNull(toStringResult);
        assertTrue(toStringResult.contains("JwtAuthenticationException"));
        assertTrue(toStringResult.contains(message));
    }

    @Test
    @DisplayName("邊界測試 - 異常序列化兼容性")
    void testException_serialization() {
        JwtAuthenticationException exception = new JwtAuthenticationException("Serialization test");
        
        // 驗證異常類實現了 Serializable（通過父類）
        assertInstanceOf(java.io.Serializable.class, exception);
    }
}