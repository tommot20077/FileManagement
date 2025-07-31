package xyz.dowob.filemanagement.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import xyz.dowob.filemanagement.customenum.RoleEnum;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用戶實體的測試實現。
 * 
 * <p>基於 Spring Security 身份驗證體系的用戶管理實體，整合角色權限和儲存配額控制。
 * 維護用戶名稱、密碼、電子郵件、角色類型以及儲存空間限制，支援個人化設定和資源管理，
 * 確保系統安全性和用戶體驗的平衡。
 * 
 * <p>測試涵蓋權限集成的正確性、儲存配額計算的準確性和角色枚舉的完整性。
 * 驗證密碼安全處理、物件相等性比較規則以及 Spring Security 整合的有效性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("User 用戶實體測試")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setEmail("test@example.com");
        user.setRole(RoleEnum.USER);
        user.setStorageLimit(1000000L);
        user.setUsedStorage(50000L);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 創建用戶對象並設置基本屬性")
    void testCreateUser_withBasicProperties() {
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("password123", user.getPassword());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(RoleEnum.USER, user.getRole());
        assertEquals(1000000L, user.getStorageLimit());
        assertEquals(50000L, user.getUsedStorage());
    }

    @Test
    @DisplayName("一般測試 - 測試默認值設置")
    void testUser_defaultValues() {
        User newUser = new User();
        
        assertEquals(RoleEnum.USER, newUser.getRole());
        assertEquals(RoleEnum.USER.getDefaultStorageLimit(), newUser.getStorageLimit());
        assertEquals(0L, newUser.getUsedStorage());
    }

    @Test
    @DisplayName("一般測試 - 測試管理員角色用戶")
    void testUser_adminRole() {
        user.setRole(RoleEnum.ADMIN);
        
        assertEquals(RoleEnum.ADMIN, user.getRole());
        
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("一般測試 - 測試 getAuthorities 方法")
    void testGetAuthorities() {
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("一般測試 - 測試 toString 方法")
    void testToString() {
        String userString = user.toString();
        
        assertNotNull(userString);
        assertTrue(userString.contains("id=1"));
        assertTrue(userString.contains("username=testuser"));
        assertTrue(userString.contains("email=test@example.com"));
        assertTrue(userString.contains("role=USER"));
        // 注意：密碼不應該出現在 toString 中
        assertFalse(userString.contains("password"));
    }

    @Test
    @DisplayName("一般測試 - 測試存儲容量計算")
    void testStorageCalculation() {
        assertEquals(1000000L, user.getStorageLimit());
        assertEquals(50000L, user.getUsedStorage());
        
        // 剩餘存儲空間計算
        long remainingStorage = user.getStorageLimit() - user.getUsedStorage();
        assertEquals(950000L, remainingStorage);
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - equals 方法傳入 null")
    void testEquals_withNull() {
        assertFalse(user.equals(null));
    }

    @Test
    @DisplayName("異常測試 - equals 方法傳入不同類型對象")
    void testEquals_withDifferentType() {
        assertFalse(user.equals("not a user"));
        assertFalse(user.equals(123));
    }

    @Test
    @DisplayName("異常測試 - hashCode 方法在 id 為 null 時")
    void testHashCode_withNullId() {
        User userWithNullId = new User();
        userWithNullId.setId(null);
        
        assertThrows(NullPointerException.class, userWithNullId::hashCode);
    }

    @Test
    @DisplayName("異常測試 - equals 方法在 id 為 null 時")
    void testEquals_withNullId() {
        User user1 = new User();
        user1.setId(null);
        User user2 = new User();
        user2.setId(null);
        
        assertThrows(NullPointerException.class, () -> user1.equals(user2));
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同對象")
    void testEquals_sameObject() {
        assertTrue(user.equals(user));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同 id 的不同對象")
    void testEquals_sameId() {
        User anotherUser = new User();
        anotherUser.setId(1L);
        anotherUser.setUsername("differentuser");
        anotherUser.setEmail("different@example.com");
        
        assertTrue(user.equals(anotherUser));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試不同 id 的對象")
    void testEquals_differentId() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        
        assertFalse(user.equals(anotherUser));
    }

    @Test
    @DisplayName("邊界測試 - hashCode 方法一致性")
    void testHashCode_consistency() {
        int hash1 = user.hashCode();
        int hash2 = user.hashCode();
        
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("邊界測試 - 相同 id 的對象 hashCode 相同")
    void testHashCode_sameId() {
        User anotherUser = new User();
        anotherUser.setId(1L);
        
        assertEquals(user.hashCode(), anotherUser.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試極大存儲限制")
    void testUser_maxStorageLimit() {
        user.setStorageLimit(Long.MAX_VALUE);
        user.setUsedStorage(Long.MAX_VALUE - 1);
        
        assertEquals(Long.MAX_VALUE, user.getStorageLimit());
        assertEquals(Long.MAX_VALUE - 1, user.getUsedStorage());
    }

    @Test
    @DisplayName("邊界測試 - 測試零存儲使用量")
    void testUser_zeroUsedStorage() {
        user.setUsedStorage(0L);
        
        assertEquals(0L, user.getUsedStorage());
    }

    @Test
    @DisplayName("邊界測試 - 測試負數存儲值")
    void testUser_negativeStorageValues() {
        // 允許設置負數（可能用於特殊業務場景）
        user.setStorageLimit(-1L);
        user.setUsedStorage(-100L);
        
        assertEquals(-1L, user.getStorageLimit());
        assertEquals(-100L, user.getUsedStorage());
    }

    @Test
    @DisplayName("邊界測試 - 測試空字符串屬性")
    void testUser_emptyStringProperties() {
        user.setUsername("");
        user.setPassword("");
        user.setEmail("");
        
        assertEquals("", user.getUsername());
        assertEquals("", user.getPassword());
        assertEquals("", user.getEmail());
    }

    @Test
    @DisplayName("邊界測試 - 測試 null 字符串屬性")
    void testUser_nullStringProperties() {
        user.setUsername(null);
        user.setPassword(null);
        user.setEmail(null);
        
        assertNull(user.getUsername());
        assertNull(user.getPassword());
        assertNull(user.getEmail());
    }

    @Test
    @DisplayName("邊界測試 - 測試超長字符串屬性")
    void testUser_veryLongStringProperties() {
        String longString = "a".repeat(1000);
        
        user.setUsername(longString);
        user.setPassword(longString);
        user.setEmail(longString + "@example.com");
        
        assertEquals(longString, user.getUsername());
        assertEquals(longString, user.getPassword());
        assertEquals(longString + "@example.com", user.getEmail());
    }

    @Test
    @DisplayName("邊界測試 - 測試特殊字符在屬性中")
    void testUser_specialCharactersInProperties() {
        String specialUsername = "user@#$%^&*()";
        String specialPassword = "pass<>&\"'`\n\t";
        String specialEmail = "test+tag@sub.domain.co.uk";
        
        user.setUsername(specialUsername);
        user.setPassword(specialPassword);
        user.setEmail(specialEmail);
        
        assertEquals(specialUsername, user.getUsername());
        assertEquals(specialPassword, user.getPassword());
        assertEquals(specialEmail, user.getEmail());
    }

    @Test
    @DisplayName("邊界測試 - 測試 Unicode 字符")
    void testUser_unicodeCharacters() {
        user.setUsername("用戶名稱");
        user.setEmail("測試@例子.中國");
        
        assertEquals("用戶名稱", user.getUsername());
        assertEquals("測試@例子.中國", user.getEmail());
        
        // 驗證 toString 方法能正確處理 Unicode
        String userString = user.toString();
        assertTrue(userString.contains("用戶名稱"));
        assertTrue(userString.contains("測試@例子.中國"));
    }

    @Test
    @DisplayName("邊界測試 - 測試所有角色枚舉值")
    void testUser_allRoleEnumValues() {
        for (RoleEnum role : RoleEnum.values()) {
            user.setRole(role);
            
            assertEquals(role, user.getRole());
            
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_" + role.name())));
        }
    }

    @Test
    @DisplayName("邊界測試 - toString 方法在屬性為 null 時")
    void testToString_withNullProperties() {
        User nullUser = new User();
        
        String userString = nullUser.toString();
        assertNotNull(userString);
        // toString 應該能處理 null 值而不拋出異常
    }

    @Test
    @DisplayName("邊界測試 - 驗證實體註解存在")
    void testEntity_annotations() {
        // 驗證類級別註解
        assertTrue(User.class.isAnnotationPresent(org.springframework.data.relational.core.mapping.Table.class));
        
        // 驗證字段註解
        try {
            assertTrue(User.class.getDeclaredField("id").isAnnotationPresent(org.springframework.data.annotation.Id.class));
            assertTrue(User.class.getDeclaredField("storageLimit").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(User.class.getDeclaredField("usedStorage").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
        } catch (NoSuchFieldException e) {
            fail("預期的字段不存在: " + e.getMessage());
        }
    }
}