package xyz.dowob.filemanagement.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 認證令牌實體的測試實現。
 * 
 * <p>基於 JWT 機制的用戶身份驗證和密碼重設功能實現。
 * 管理 JWT 令牌版本、過期時間以及密碼重設驗證碼，確保安全的用戶會話管理。
 * 
 * <p>測試涵蓋令牌版本生成的唯一性、時間戳記錄的準確性和敏感資料的安全處理。
 * 驗證靜態工廠方法的功能、物件相等性比較規則以及字串表示的安全性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("Token 憑證實體測試")
class TokenTest {

    private Token token;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        token = new Token();
        testTime = LocalDateTime.now();
        
        token.setId(1L);
        token.setUserId(100L);
        token.setJwtTokenVersion("abc123-def456-ghi");
        token.setJwtTokenExpireTime(testTime.plusHours(1));
        token.setResetVerificationCode("VERIFY123");
        token.setResetVerificationCodeExpireTime(testTime.plusMinutes(30));
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 創建令牌對象並設置基本屬性")
    void testCreateToken_withBasicProperties() {
        assertNotNull(token);
        assertEquals(1L, token.getId());
        assertEquals(100L, token.getUserId());
        assertEquals("abc123-def456-ghi", token.getJwtTokenVersion());
        assertEquals(testTime.plusHours(1), token.getJwtTokenExpireTime());
        assertEquals("VERIFY123", token.getResetVerificationCode());
        assertEquals(testTime.plusMinutes(30), token.getResetVerificationCodeExpireTime());
    }

    @Test
    @DisplayName("一般測試 - 測試 JWT 令牌版本生成")
    void testGenerateJwtTokenVersion() {
        String version = Token.generateJwtTokenVersion();
        
        assertNotNull(version);
        assertEquals(18, version.length());
        assertTrue(version.matches("[a-f0-9\\-]+"));
    }

    @Test
    @DisplayName("一般測試 - 測試多次生成 JWT 版本的唯一性")
    void testGenerateJwtTokenVersion_uniqueness() {
        String version1 = Token.generateJwtTokenVersion();
        String version2 = Token.generateJwtTokenVersion();
        String version3 = Token.generateJwtTokenVersion();
        
        assertNotEquals(version1, version2);
        assertNotEquals(version2, version3);
        assertNotEquals(version1, version3);
    }

    @Test
    @DisplayName("一般測試 - 測試 toString 方法")
    void testToString() {
        String tokenString = token.toString();
        
        assertNotNull(tokenString);
        assertTrue(tokenString.contains("id=1"));
        assertTrue(tokenString.contains("userId=100"));
        
        // 驗證敏感信息不會出現在 toString 中
        assertFalse(tokenString.contains("abc123-def456-ghi"));
        assertFalse(tokenString.contains("VERIFY123"));
    }

    @Test
    @DisplayName("一般測試 - 測試時間屬性設置")
    void testTimeProperties() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureTime = now.plusDays(1);
        
        token.setJwtTokenExpireTime(futureTime);
        token.setResetVerificationCodeExpireTime(now.plusHours(2));
        
        assertEquals(futureTime, token.getJwtTokenExpireTime());
        assertEquals(now.plusHours(2), token.getResetVerificationCodeExpireTime());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - equals 方法傳入 null")
    void testEquals_withNull() {
        assertFalse(token.equals(null));
    }

    @Test
    @DisplayName("異常測試 - equals 方法傳入不同類型對象")
    void testEquals_withDifferentType() {
        assertFalse(token.equals("not a token"));
        assertFalse(token.equals(123));
        assertFalse(token.equals(new Object()));
    }

    @Test
    @DisplayName("異常測試 - 設置 null 時間屬性")
    void testSetNullTimeProperties() {
        // 設置 null 時間應該不拋出異常
        assertDoesNotThrow(() -> {
            token.setJwtTokenExpireTime(null);
            token.setResetVerificationCodeExpireTime(null);
        });
        
        assertNull(token.getJwtTokenExpireTime());
        assertNull(token.getResetVerificationCodeExpireTime());
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同對象")
    void testEquals_sameObject() {
        assertTrue(token.equals(token));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同 id 的不同對象")
    void testEquals_sameId() {
        Token anotherToken = new Token();
        anotherToken.setId(1L);
        anotherToken.setUserId(999L); // 不同的用戶ID
        
        assertTrue(token.equals(anotherToken));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試不同 id 的對象")
    void testEquals_differentId() {
        Token anotherToken = new Token();
        anotherToken.setId(2L);
        
        assertFalse(token.equals(anotherToken));
    }

    @Test
    @DisplayName("邊界測試 - hashCode 方法一致性")
    void testHashCode_consistency() {
        int hash1 = token.hashCode();
        int hash2 = token.hashCode();
        
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("邊界測試 - 相同 id 的對象 hashCode 相同")
    void testHashCode_sameId() {
        Token anotherToken = new Token();
        anotherToken.setId(1L);
        
        assertEquals(token.hashCode(), anotherToken.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 不同 id 的對象 hashCode 不同")
    void testHashCode_differentId() {
        Token token1 = new Token();
        token1.setId(1L);
        
        Token token2 = new Token();
        token2.setId(2L);
        
        assertNotEquals(token1.hashCode(), token2.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試 id 為 0 的情況")
    void testToken_zeroId() {
        token.setId(0L);
        
        assertEquals(0L, token.getId());
        assertEquals(Long.hashCode(0L), token.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試極大 id 值")
    void testToken_maxId() {
        token.setId(Long.MAX_VALUE);
        
        assertEquals(Long.MAX_VALUE, token.getId());
        assertEquals(Long.hashCode(Long.MAX_VALUE), token.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試負數 id")
    void testToken_negativeId() {
        token.setId(-1L);
        
        assertEquals(-1L, token.getId());
        assertEquals(Long.hashCode(-1L), token.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試空字符串屬性")
    void testToken_emptyStringProperties() {
        token.setJwtTokenVersion("");
        token.setResetVerificationCode("");
        
        assertEquals("", token.getJwtTokenVersion());
        assertEquals("", token.getResetVerificationCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試 null 字符串屬性")
    void testToken_nullStringProperties() {
        token.setJwtTokenVersion(null);
        token.setResetVerificationCode(null);
        
        assertNull(token.getJwtTokenVersion());
        assertNull(token.getResetVerificationCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試超長字符串屬性")
    void testToken_veryLongStringProperties() {
        String longString = "a".repeat(1000);
        
        token.setJwtTokenVersion(longString);
        token.setResetVerificationCode(longString);
        
        assertEquals(longString, token.getJwtTokenVersion());
        assertEquals(longString, token.getResetVerificationCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試特殊字符在屬性中")
    void testToken_specialCharactersInProperties() {
        String specialVersion = "token@#$%^&*()";
        String specialCode = "code<>&\"'`\n\t";
        
        token.setJwtTokenVersion(specialVersion);
        token.setResetVerificationCode(specialCode);
        
        assertEquals(specialVersion, token.getJwtTokenVersion());
        assertEquals(specialCode, token.getResetVerificationCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試過去時間")
    void testToken_pastTime() {
        LocalDateTime pastTime = LocalDateTime.now().minusYears(1);
        
        token.setJwtTokenExpireTime(pastTime);
        token.setResetVerificationCodeExpireTime(pastTime);
        
        assertEquals(pastTime, token.getJwtTokenExpireTime());
        assertEquals(pastTime, token.getResetVerificationCodeExpireTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試極遠未來時間")
    void testToken_farFutureTime() {
        LocalDateTime futureTime = LocalDateTime.now().plusYears(100);
        
        token.setJwtTokenExpireTime(futureTime);
        token.setResetVerificationCodeExpireTime(futureTime);
        
        assertEquals(futureTime, token.getJwtTokenExpireTime());
        assertEquals(futureTime, token.getResetVerificationCodeExpireTime());
    }

    @Test
    @DisplayName("邊界測試 - JWT 版本生成格式驗證")
    void testGenerateJwtTokenVersion_format() {
        for (int i = 0; i < 10; i++) {
            String version = Token.generateJwtTokenVersion();
            
            // 驗證長度
            assertEquals(18, version.length());
            
            // 驗證格式（UUID 的前18個字符，包含連字符）
            assertTrue(version.matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}$"), 
                "版本格式不正確: " + version);
        }
    }

    @Test
    @DisplayName("邊界測試 - toString 方法在屬性為 null 時")
    void testToString_withNullProperties() {
        Token nullToken = new Token();
        // id 默認為 0，userId 默認為 0
        
        String tokenString = nullToken.toString();
        assertNotNull(tokenString);
        assertTrue(tokenString.contains("id=0"));
        assertTrue(tokenString.contains("userId=0"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證實體註解存在")
    void testEntity_annotations() {
        // 驗證類級別註解
        assertTrue(Token.class.isAnnotationPresent(org.springframework.data.relational.core.mapping.Table.class));
        
        // 驗證字段註解
        try {
            assertTrue(Token.class.getDeclaredField("id").isAnnotationPresent(org.springframework.data.annotation.Id.class));
            assertTrue(Token.class.getDeclaredField("userId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(Token.class.getDeclaredField("jwtTokenVersion").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(Token.class.getDeclaredField("jwtTokenExpireTime").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(Token.class.getDeclaredField("resetVerificationCode").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(Token.class.getDeclaredField("resetVerificationCodeExpireTime").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
        } catch (NoSuchFieldException e) {
            fail("預期的字段不存在: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("邊界測試 - UUID 生成的隨機性統計測試")
    void testGenerateJwtTokenVersion_randomnessStatistics() {
        // 生成多個版本並檢查字符分佈的合理性
        java.util.Set<String> generatedVersions = new java.util.HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            String version = Token.generateJwtTokenVersion();
            generatedVersions.add(version);
        }
        
        // 100個版本應該都是唯一的
        assertEquals(100, generatedVersions.size(), "生成的JWT版本應該都是唯一的");
    }
}