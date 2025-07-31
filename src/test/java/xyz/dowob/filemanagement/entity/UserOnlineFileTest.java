package xyz.dowob.filemanagement.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用戶線上檔案實體的測試實現。
 * 
 * <p>基於即時協作編輯的檔案內容管理系統，儲存線上檔案的當前狀態和元數據。
 * 管理檔案大小、內容資料、最後修改者和快照計數，支援版本同步檢查和歷史追蹤，
 * 確保多用戶協作編輯的即時性和資料完整性。
 * 
 * <p>測試涵蓋內容格式化的安全性、版本控制邏輯的準確性和協作狀態的同步機制。
 * 驗證檔案大小計算、物件相等性比較規則以及各種協作編輯場景的處理能力。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("UserOnlineFile 用戶在線檔案實體測試")
class UserOnlineFileTest {

    private UserOnlineFile onlineFile;
    private String testContent;

    @BeforeEach
    void setUp() {
        onlineFile = new UserOnlineFile();
        testContent = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"這是測試內容\"}]}]}";
        
        // 設置基本屬性
        onlineFile.setId(1L);
        onlineFile.setFileSize(2048L);
        onlineFile.setContent(testContent);
        onlineFile.setLastModifiedBy(100L);
        onlineFile.setCurrentSnapshotCount(5);
        onlineFile.setLastHistoryVersion(10L);
        onlineFile.setIsMatchHistory(true);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 創建在線檔案並設置基本屬性")
    void testCreateOnlineFile_withBasicProperties() {
        assertNotNull(onlineFile);
        assertEquals(1L, onlineFile.getId());
        assertEquals(2048L, onlineFile.getFileSize());
        assertEquals(testContent, onlineFile.getContent());
        assertEquals(100L, onlineFile.getLastModifiedBy());
        assertEquals(5, onlineFile.getCurrentSnapshotCount());
        assertEquals(10L, onlineFile.getLastHistoryVersion());
        assertTrue(onlineFile.getIsMatchHistory());
    }

    @Test
    @DisplayName("一般測試 - 測試默認檔案大小")
    void testOnlineFile_defaultFileSize() {
        UserOnlineFile newFile = new UserOnlineFile();
        
        assertEquals(0L, newFile.getFileSize());
    }

    @Test
    @DisplayName("一般測試 - 測試 toString 方法")
    void testToString() {
        String fileString = onlineFile.toString();
        
        assertNotNull(fileString);
        assertTrue(fileString.contains("id=1"));
        assertTrue(fileString.contains("fileSize=2048"));
        assertTrue(fileString.contains("lastModifiedBy=100"));
        assertTrue(fileString.contains("currentSnapshotCount=5"));
        // 內容應該被格式化（截取前30字符）
        String expectedTruncated = testContent.substring(0, 30) + "...";
        assertTrue(fileString.contains("content=" + expectedTruncated));
    }

    @Test
    @DisplayName("一般測試 - 測試檔案內容設置")
    void testFileContentHandling() {
        String jsonContent = "{\"delta\":{\"ops\":[{\"insert\":\"Hello World\\n\"}]}}";
        
        onlineFile.setContent(jsonContent);
        
        assertEquals(jsonContent, onlineFile.getContent());
    }

    @Test
    @DisplayName("一般測試 - 測試版本控制屬性")
    void testVersionControlProperties() {
        onlineFile.setLastHistoryVersion(25L);
        onlineFile.setCurrentSnapshotCount(3);
        onlineFile.setIsMatchHistory(false);
        
        assertEquals(25L, onlineFile.getLastHistoryVersion());
        assertEquals(3, onlineFile.getCurrentSnapshotCount());
        assertFalse(onlineFile.getIsMatchHistory());
    }

    @Test
    @DisplayName("一般測試 - 測試檔案修改者設置")
    void testLastModifiedByHandling() {
        Long modifierId = 999L;
        
        onlineFile.setLastModifiedBy(modifierId);
        
        assertEquals(modifierId, onlineFile.getLastModifiedBy());
    }

    @Test
    @DisplayName("一般測試 - 測試快照計數邏輯")
    void testSnapshotCountLogic() {
        // 模擬快照計數遞增
        for (int i = 0; i <= 10; i++) {
            onlineFile.setCurrentSnapshotCount(i);
            assertEquals(i, onlineFile.getCurrentSnapshotCount());
        }
    }

    @Test
    @DisplayName("一般測試 - 測試歷史版本匹配邏輯")
    void testHistoryMatchLogic() {
        // 測試匹配歷史版本
        onlineFile.setIsMatchHistory(true);
        onlineFile.setLastHistoryVersion(5L);
        
        assertTrue(onlineFile.getIsMatchHistory());
        assertEquals(5L, onlineFile.getLastHistoryVersion());
        
        // 測試不匹配歷史版本
        onlineFile.setIsMatchHistory(false);
        assertFalse(onlineFile.getIsMatchHistory());
    }

    @Test
    @DisplayName("一般測試 - 測試空內容檔案")
    void testEmptyContentFile() {
        onlineFile.setContent("");
        onlineFile.setFileSize(0L);
        
        assertEquals("", onlineFile.getContent());
        assertEquals(0L, onlineFile.getFileSize());
    }

    @Test
    @DisplayName("一般測試 - 測試檔案大小更新")
    void testFileSizeUpdate() {
        String shortContent = "短內容";
        String longContent = "這是一個很長的內容".repeat(100);
        
        onlineFile.setContent(shortContent);
        onlineFile.setFileSize((long) shortContent.getBytes().length);
        
        assertEquals(shortContent, onlineFile.getContent());
        
        onlineFile.setContent(longContent);
        onlineFile.setFileSize((long) longContent.getBytes().length);
        
        assertEquals(longContent, onlineFile.getContent());
        assertTrue(onlineFile.getFileSize() > shortContent.getBytes().length);
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - equals 方法傳入 null")
    void testEquals_withNull() {
        assertFalse(onlineFile.equals(null));
    }

    @Test
    @DisplayName("異常測試 - equals 方法傳入不同類型對象")
    void testEquals_withDifferentType() {
        assertFalse(onlineFile.equals("not an online file"));
        assertFalse(onlineFile.equals(123));
        assertFalse(onlineFile.equals(new Object()));
    }

    @Test
    @DisplayName("異常測試 - hashCode 方法在 id 為 null 時")
    void testHashCode_withNullId() {
        UserOnlineFile fileWithNullId = new UserOnlineFile();
        fileWithNullId.setId(null);
        
        assertThrows(NullPointerException.class, fileWithNullId::hashCode);
    }

    @Test
    @DisplayName("異常測試 - equals 方法在 id 為 null 時")
    void testEquals_withNullId() {
        UserOnlineFile file1 = new UserOnlineFile();
        file1.setId(null);
        UserOnlineFile file2 = new UserOnlineFile();
        file2.setId(null);
        
        assertThrows(NullPointerException.class, () -> file1.equals(file2));
    }

    @Test
    @DisplayName("異常測試 - 設置 null 內容")
    void testSetNullContent() {
        assertDoesNotThrow(() -> {
            onlineFile.setContent(null);
        });
        
        assertNull(onlineFile.getContent());
    }

    @Test
    @DisplayName("異常測試 - 設置 null 數值屬性")
    void testSetNullNumericProperties() {
        assertDoesNotThrow(() -> {
            onlineFile.setFileSize(null);
            onlineFile.setLastModifiedBy(null);
            onlineFile.setCurrentSnapshotCount(null);
            onlineFile.setLastHistoryVersion(null);
            onlineFile.setIsMatchHistory(null);
        });
        
        assertNull(onlineFile.getFileSize());
        assertNull(onlineFile.getLastModifiedBy());
        assertNull(onlineFile.getCurrentSnapshotCount());
        assertNull(onlineFile.getLastHistoryVersion());
        assertNull(onlineFile.getIsMatchHistory());
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同對象")
    void testEquals_sameObject() {
        assertTrue(onlineFile.equals(onlineFile));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同 id 的不同對象")
    void testEquals_sameId() {
        UserOnlineFile anotherFile = new UserOnlineFile();
        anotherFile.setId(1L);
        anotherFile.setContent("不同內容"); // 不同的內容
        
        assertTrue(onlineFile.equals(anotherFile));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試不同 id 的對象")
    void testEquals_differentId() {
        UserOnlineFile anotherFile = new UserOnlineFile();
        anotherFile.setId(2L);
        
        assertFalse(onlineFile.equals(anotherFile));
    }

    @Test
    @DisplayName("邊界測試 - hashCode 方法一致性")
    void testHashCode_consistency() {
        int hash1 = onlineFile.hashCode();
        int hash2 = onlineFile.hashCode();
        
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("邊界測試 - 相同 id 的對象 hashCode 相同")
    void testHashCode_sameId() {
        UserOnlineFile anotherFile = new UserOnlineFile();
        anotherFile.setId(1L);
        
        assertEquals(onlineFile.hashCode(), anotherFile.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試極大檔案大小")
    void testOnlineFile_maxFileSize() {
        onlineFile.setFileSize(Long.MAX_VALUE);
        
        assertEquals(Long.MAX_VALUE, onlineFile.getFileSize());
    }

    @Test
    @DisplayName("邊界測試 - 測試零檔案大小")
    void testOnlineFile_zeroFileSize() {
        onlineFile.setFileSize(0L);
        
        assertEquals(0L, onlineFile.getFileSize());
    }

    @Test
    @DisplayName("邊界測試 - 測試負數檔案大小")
    void testOnlineFile_negativeFileSize() {
        onlineFile.setFileSize(-1L);
        
        assertEquals(-1L, onlineFile.getFileSize());
    }

    @Test
    @DisplayName("邊界測試 - 測試極大 ID 值")
    void testOnlineFile_maxIdValues() {
        onlineFile.setId(Long.MAX_VALUE);
        onlineFile.setLastModifiedBy(Long.MAX_VALUE);
        onlineFile.setLastHistoryVersion(Long.MAX_VALUE);
        
        assertEquals(Long.MAX_VALUE, onlineFile.getId());
        assertEquals(Long.MAX_VALUE, onlineFile.getLastModifiedBy());
        assertEquals(Long.MAX_VALUE, onlineFile.getLastHistoryVersion());
    }

    @Test
    @DisplayName("邊界測試 - 測試極大快照計數")
    void testOnlineFile_maxSnapshotCount() {
        onlineFile.setCurrentSnapshotCount(Integer.MAX_VALUE);
        
        assertEquals(Integer.MAX_VALUE, onlineFile.getCurrentSnapshotCount());
    }

    @Test
    @DisplayName("邊界測試 - 測試負數快照計數")
    void testOnlineFile_negativeSnapshotCount() {
        onlineFile.setCurrentSnapshotCount(-1);
        
        assertEquals(-1, onlineFile.getCurrentSnapshotCount());
    }

    @Test
    @DisplayName("邊界測試 - 測試內容格式化方法（短內容）")
    void testContentFormatting_shortContent() {
        String shortContent = "短內容測試";
        onlineFile.setContent(shortContent);
        
        String fileString = onlineFile.toString();
        assertTrue(fileString.contains("content=" + shortContent));
    }

    @Test
    @DisplayName("邊界測試 - 測試內容格式化方法（長內容）")
    void testContentFormatting_longContent() {
        String longContent = "這是一個非常長的內容，超過了30個字符的限制，應該被截取並添加省略號";
        onlineFile.setContent(longContent);
        
        String fileString = onlineFile.toString();
        // 應該被截取為前30個字符並添加省略號
        String expectedTruncated = longContent.substring(0, 30) + "...";
        assertTrue(fileString.contains("content=" + expectedTruncated));
        assertFalse(fileString.contains(longContent));
    }

    @Test
    @DisplayName("邊界測試 - 測試內容格式化方法（null 內容）")
    void testContentFormatting_nullContent() {
        onlineFile.setContent(null);
        
        String fileString = onlineFile.toString();
        assertTrue(fileString.contains("content=null"));
    }

    @Test
    @DisplayName("邊界測試 - 測試內容格式化方法（空內容）")
    void testContentFormatting_emptyContent() {
        onlineFile.setContent("");
        
        String fileString = onlineFile.toString();
        assertTrue(fileString.contains("content="));
    }

    @Test
    @DisplayName("邊界測試 - 測試內容格式化方法（正好30字符）")
    void testContentFormatting_exactly30Characters() {
        String exactContent = "1234567890123456789012345678901"; // 31字符
        String thirtyCharContent = exactContent.substring(0, 30); // 正好30字符
        
        onlineFile.setContent(thirtyCharContent);
        
        String fileString = onlineFile.toString();
        assertTrue(fileString.contains("content=" + thirtyCharContent));
        assertFalse(fileString.contains("..."));
    }

    @Test
    @DisplayName("邊界測試 - 測試超長 JSON 內容")
    void testOnlineFile_veryLongJsonContent() {
        StringBuilder longJsonBuilder = new StringBuilder("{\"content\":[");
        for (int i = 0; i < 1000; i++) {
            longJsonBuilder.append("{\"type\":\"text\",\"text\":\"段落").append(i).append("\"},");
        }
        longJsonBuilder.append("]}");
        String longJsonContent = longJsonBuilder.toString();
        
        onlineFile.setContent(longJsonContent);
        
        assertEquals(longJsonContent, onlineFile.getContent());
        
        // toString 應該截取內容
        String fileString = onlineFile.toString();
        assertTrue(fileString.contains("content=" + longJsonContent.substring(0, 30) + "..."));
    }

    @Test
    @DisplayName("邊界測試 - 測試特殊字符在內容中")
    void testOnlineFile_specialCharactersInContent() {
        String specialContent = "{\"content\":\"特殊字符: <>&\\\"'`\\n\\t🔒📄\"}";
        
        onlineFile.setContent(specialContent);
        
        assertEquals(specialContent, onlineFile.getContent());
    }

    @Test
    @DisplayName("邊界測試 - toString 方法在屬性為 null 時")
    void testToString_withNullProperties() {
        UserOnlineFile nullFile = new UserOnlineFile();
        
        String fileString = nullFile.toString();
        assertNotNull(fileString);
        assertTrue(fileString.contains("id=null"));
        assertTrue(fileString.contains("fileSize=0")); // 默認值
        assertTrue(fileString.contains("content=null"));
        assertTrue(fileString.contains("lastModifiedBy=null"));
        assertTrue(fileString.contains("currentSnapshotCount=null"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證實體註解存在")
    void testEntity_annotations() {
        // 驗證類級別註解
        assertTrue(UserOnlineFile.class.isAnnotationPresent(org.springframework.data.relational.core.mapping.Table.class));
        
        // 驗證字段註解
        try {
            assertTrue(UserOnlineFile.class.getDeclaredField("id").isAnnotationPresent(org.springframework.data.annotation.Id.class));
            assertTrue(UserOnlineFile.class.getDeclaredField("fileSize").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserOnlineFile.class.getDeclaredField("lastModifiedBy").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserOnlineFile.class.getDeclaredField("currentSnapshotCount").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserOnlineFile.class.getDeclaredField("lastHistoryVersion").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserOnlineFile.class.getDeclaredField("isMatchHistory").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
        } catch (NoSuchFieldException e) {
            fail("預期的字段不存在: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試 equals 方法的反射性、對稱性和傳遞性")
    void testEquals_properties() {
        UserOnlineFile file1 = new UserOnlineFile();
        file1.setId(1L);
        
        UserOnlineFile file2 = new UserOnlineFile();
        file2.setId(1L);
        
        UserOnlineFile file3 = new UserOnlineFile();
        file3.setId(1L);
        
        // 反射性：x.equals(x) 應該返回 true
        assertTrue(file1.equals(file1));
        
        // 對稱性：x.equals(y) 和 y.equals(x) 應該返回相同結果
        assertTrue(file1.equals(file2));
        assertTrue(file2.equals(file1));
        
        // 傳遞性：如果 x.equals(y) 和 y.equals(z)，則 x.equals(z) 應該為 true  
        assertTrue(file1.equals(file2));
        assertTrue(file2.equals(file3));
        assertTrue(file1.equals(file3));
    }

    @Test
    @DisplayName("邊界測試 - 測試版本控制業務邏輯組合")
    void testOnlineFile_versionControlCombinations() {
        // 場景1：新檔案，無歷史版本
        onlineFile.setLastHistoryVersion(null);
        onlineFile.setCurrentSnapshotCount(0);
        onlineFile.setIsMatchHistory(false);
        
        assertNull(onlineFile.getLastHistoryVersion());
        assertEquals(0, onlineFile.getCurrentSnapshotCount());
        assertFalse(onlineFile.getIsMatchHistory());
        
        // 場景2：有歷史版本且匹配
        onlineFile.setLastHistoryVersion(5L);
        onlineFile.setCurrentSnapshotCount(0);
        onlineFile.setIsMatchHistory(true);
        
        assertEquals(5L, onlineFile.getLastHistoryVersion());
        assertEquals(0, onlineFile.getCurrentSnapshotCount());
        assertTrue(onlineFile.getIsMatchHistory());
        
        // 場景3：有歷史版本但不匹配，需要創建新快照
        onlineFile.setLastHistoryVersion(5L);
        onlineFile.setCurrentSnapshotCount(10);
        onlineFile.setIsMatchHistory(false);
        
        assertEquals(5L, onlineFile.getLastHistoryVersion());
        assertEquals(10, onlineFile.getCurrentSnapshotCount());
        assertFalse(onlineFile.getIsMatchHistory());
    }

    @Test
    @DisplayName("邊界測試 - 測試 Unicode 字符在內容中")
    void testOnlineFile_unicodeCharacters() {
        String unicodeContent = "{\"content\":\"中文內容：你好世界！ 🌍 表情符號 📝 文檔\"}";
        
        onlineFile.setContent(unicodeContent);
        
        assertEquals(unicodeContent, onlineFile.getContent());
        
        // 驗證 toString 方法能正確處理 Unicode
        String fileString = onlineFile.toString();
        assertTrue(fileString.contains("你好世界"));
        assertTrue(fileString.contains("🌍"));
    }

    @Test
    @DisplayName("邊界測試 - 測試多行 JSON 內容")
    void testOnlineFile_multilineJsonContent() {
        String multilineContent = "{\n  \"type\": \"doc\",\n  \"content\": [\n    {\n      \"type\": \"paragraph\",\n      \"content\": [\n        {\n          \"type\": \"text\",\n          \"text\": \"多行內容測試\"\n        }\n      ]\n    }\n  ]\n}";
        
        onlineFile.setContent(multilineContent);
        
        assertEquals(multilineContent, onlineFile.getContent());
    }

    @Test
    @DisplayName("邊界測試 - 測試檔案協作場景")
    void testOnlineFile_collaborationScenario() {
        // 模擬多用戶協作編輯場景
        
        // 用戶A創建檔案
        onlineFile.setLastModifiedBy(100L);
        onlineFile.setContent("{\"ops\":[{\"insert\":\"用戶A的內容\\n\"}]}");
        onlineFile.setCurrentSnapshotCount(1);
        
        assertEquals(100L, onlineFile.getLastModifiedBy());
        assertEquals(1, onlineFile.getCurrentSnapshotCount());
        
        // 用戶B修改檔案
        onlineFile.setLastModifiedBy(200L);
        onlineFile.setContent("{\"ops\":[{\"insert\":\"用戶A的內容\\n用戶B的修改\\n\"}]}");
        onlineFile.setCurrentSnapshotCount(2);
        
        assertEquals(200L, onlineFile.getLastModifiedBy());
        assertEquals(2, onlineFile.getCurrentSnapshotCount());
        
        // 用戶C繼續修改
        onlineFile.setLastModifiedBy(300L);
        onlineFile.setCurrentSnapshotCount(3);
        
        assertEquals(300L, onlineFile.getLastModifiedBy());
        assertEquals(3, onlineFile.getCurrentSnapshotCount());
    }
}