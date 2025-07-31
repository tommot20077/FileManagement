package xyz.dowob.filemanagement.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用戶線上檔案歷史記錄實體的測試實現。
 * 
 * <p>基於協作編輯的版本控制系統，追蹤線上檔案的修改歷史和內容變更。
 * 管理版本序號、差異內容、快照資料和修改者資訊，支援增量式版本儲存和完整快照備份，
 * 確保多用戶協作編輯的資料一致性和可追溯性。
 * 
 * <p>測試涵蓋版本鏈結構的完整性、內容格式化的準確性和快照機制的有效性。
 * 驗證差異資料處理、時間戳記錄以及協作編輯場景下的版本管理邏輯。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("UserOnlineFileHistory 用戶在線檔案歷史數據實體測試")
class UserOnlineFileHistoryTest {

    private UserOnlineFileHistory historyRecord;
    private LocalDateTime testModifiedTime;
    private String testDiff;
    private String testSnapshotContent;

    @BeforeEach
    void setUp() {
        historyRecord = new UserOnlineFileHistory();
        testModifiedTime = LocalDateTime.now();
        testDiff = "{\"ops\":[{\"retain\":10},{\"insert\":\"新增內容\"},{\"delete\":5}]}";
        testSnapshotContent = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"完整快照內容\"}]}]}";
        
        // 設置基本屬性
        historyRecord.setId(1L);
        historyRecord.setFileId(100L);
        historyRecord.setVersion(5L);
        historyRecord.setPreviousVersion(4L);
        historyRecord.setDiff(testDiff);
        historyRecord.setNote("修改備註");
        historyRecord.setIsSnapshot(false);
        historyRecord.setSnapshotContent(null);
        historyRecord.setModifiedTime(testModifiedTime);
        historyRecord.setModifiedBy(200L);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 創建歷史記錄並設置基本屬性")
    void testCreateHistoryRecord_withBasicProperties() {
        assertNotNull(historyRecord);
        assertEquals(1L, historyRecord.getId());
        assertEquals(100L, historyRecord.getFileId());
        assertEquals(5L, historyRecord.getVersion());
        assertEquals(4L, historyRecord.getPreviousVersion());
        assertEquals(testDiff, historyRecord.getDiff());
        assertEquals("修改備註", historyRecord.getNote());
        assertFalse(historyRecord.getIsSnapshot());
        assertNull(historyRecord.getSnapshotContent());
        assertEquals(testModifiedTime, historyRecord.getModifiedTime());
        assertEquals(200L, historyRecord.getModifiedBy());
    }

    @Test
    @DisplayName("一般測試 - 使用無參數構造方法")
    void testNoArgsConstructor() {
        UserOnlineFileHistory newRecord = new UserOnlineFileHistory();
        
        assertNotNull(newRecord);
        assertNull(newRecord.getId());
        assertNull(newRecord.getFileId());
        assertNull(newRecord.getVersion());
        assertNull(newRecord.getPreviousVersion());
        assertNull(newRecord.getDiff());
        assertNull(newRecord.getNote());
        assertFalse(newRecord.getIsSnapshot()); // 默認值
        assertNull(newRecord.getSnapshotContent());
        assertNull(newRecord.getModifiedTime());
        assertNull(newRecord.getModifiedBy());
    }

    @Test
    @DisplayName("一般測試 - 測試 toString 方法")
    void testToString() {
        String recordString = historyRecord.toString();
        
        assertNotNull(recordString);
        assertTrue(recordString.contains("id=1"));
        assertTrue(recordString.contains("fileId=100"));
        assertTrue(recordString.contains("version=5"));
        assertTrue(recordString.contains("note=修改備註"));
        assertTrue(recordString.contains("isSnapshot=false"));
        assertTrue(recordString.contains("modifiedBy=200"));
        // diff 內容應該被格式化（截取前30字符）
        String expectedTruncatedDiff = testDiff.substring(0, 30) + "...";
        assertTrue(recordString.contains("diff=" + expectedTruncatedDiff));
        assertTrue(recordString.contains("snapshotContent=null"));
    }

    @Test
    @DisplayName("一般測試 - 測試版本控制屬性")
    void testVersionControlProperties() {
        historyRecord.setVersion(10L);
        historyRecord.setPreviousVersion(9L);
        
        assertEquals(10L, historyRecord.getVersion());
        assertEquals(9L, historyRecord.getPreviousVersion());
    }

    @Test
    @DisplayName("一般測試 - 測試差異記錄")
    void testDiffRecording() {
        String diffContent = "{\"ops\":[{\"retain\":5},{\"insert\":\"新文字\"},{\"delete\":3}]}";
        
        historyRecord.setDiff(diffContent);
        
        assertEquals(diffContent, historyRecord.getDiff());
    }

    @Test
    @DisplayName("一般測試 - 測試快照記錄")
    void testSnapshotRecording() {
        historyRecord.setIsSnapshot(true);
        historyRecord.setSnapshotContent(testSnapshotContent);
        historyRecord.setDiff(null); // 快照通常不需要 diff
        
        assertTrue(historyRecord.getIsSnapshot());
        assertEquals(testSnapshotContent, historyRecord.getSnapshotContent());
        assertNull(historyRecord.getDiff());
    }

    @Test
    @DisplayName("一般測試 - 測試修改時間和修改者")
    void testModificationTracking() {
        LocalDateTime modTime = LocalDateTime.of(2024, 3, 15, 14, 30, 0);
        Long modifierId = 999L;
        
        historyRecord.setModifiedTime(modTime);
        historyRecord.setModifiedBy(modifierId);
        
        assertEquals(modTime, historyRecord.getModifiedTime());
        assertEquals(modifierId, historyRecord.getModifiedBy());
    }

    @Test
    @DisplayName("一般測試 - 測試備註功能")
    void testNoteHandling() {
        String note = "這是一個重要的修改：添加了新功能";
        
        historyRecord.setNote(note);
        
        assertEquals(note, historyRecord.getNote());
    }

    @Test
    @DisplayName("一般測試 - 測試版本鏈關係")
    void testVersionChaining() {
        // 模擬版本鏈：1 -> 2 -> 3 -> 4 -> 5
        historyRecord.setVersion(5L);
        historyRecord.setPreviousVersion(4L);
        
        assertEquals(5L, historyRecord.getVersion());
        assertEquals(4L, historyRecord.getPreviousVersion());
        
        // 驗證版本遞增關係
        assertTrue(historyRecord.getVersion() > historyRecord.getPreviousVersion());
    }

    @Test
    @DisplayName("一般測試 - 測試初始版本（無前版本）")
    void testInitialVersion() {
        historyRecord.setVersion(1L);
        historyRecord.setPreviousVersion(null); // 初始版本沒有前版本
        
        assertEquals(1L, historyRecord.getVersion());
        assertNull(historyRecord.getPreviousVersion());
    }

    @Test
    @DisplayName("一般測試 - 測試檔案關聯")
    void testFileAssociation() {
        Long fileId = 12345L;
        
        historyRecord.setFileId(fileId);
        
        assertEquals(fileId, historyRecord.getFileId());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - equals 方法傳入 null")
    void testEquals_withNull() {
        assertFalse(historyRecord.equals(null));
    }

    @Test
    @DisplayName("異常測試 - equals 方法傳入不同類型對象")
    void testEquals_withDifferentType() {
        assertFalse(historyRecord.equals("not a history record"));
        assertFalse(historyRecord.equals(123));
        assertFalse(historyRecord.equals(new Object()));
    }

    @Test
    @DisplayName("異常測試 - hashCode 方法在 id 為 null 時")
    void testHashCode_withNullId() {
        UserOnlineFileHistory recordWithNullId = new UserOnlineFileHistory();
        recordWithNullId.setId(null);
        
        assertThrows(NullPointerException.class, recordWithNullId::hashCode);
    }

    @Test
    @DisplayName("異常測試 - equals 方法在 id 為 null 時")
    void testEquals_withNullId() {
        UserOnlineFileHistory record1 = new UserOnlineFileHistory();
        record1.setId(null);
        UserOnlineFileHistory record2 = new UserOnlineFileHistory();
        record2.setId(null);
        
        assertThrows(NullPointerException.class, () -> record1.equals(record2));
    }

    @Test
    @DisplayName("異常測試 - 設置 null 內容屬性")
    void testSetNullContentProperties() {
        assertDoesNotThrow(() -> {
            historyRecord.setDiff(null);
            historyRecord.setNote(null);
            historyRecord.setSnapshotContent(null);
        });
        
        assertNull(historyRecord.getDiff());
        assertNull(historyRecord.getNote());
        assertNull(historyRecord.getSnapshotContent());
    }

    @Test
    @DisplayName("異常測試 - 設置 null 數值屬性")
    void testSetNullNumericProperties() {
        assertDoesNotThrow(() -> {
            historyRecord.setFileId(null);
            historyRecord.setVersion(null);
            historyRecord.setPreviousVersion(null);
            historyRecord.setModifiedBy(null);
            historyRecord.setModifiedTime(null);
            historyRecord.setIsSnapshot(null);
        });
        
        assertNull(historyRecord.getFileId());
        assertNull(historyRecord.getVersion());
        assertNull(historyRecord.getPreviousVersion());
        assertNull(historyRecord.getModifiedBy());
        assertNull(historyRecord.getModifiedTime());
        assertNull(historyRecord.getIsSnapshot());
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同對象")
    void testEquals_sameObject() {
        assertTrue(historyRecord.equals(historyRecord));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同 id 的不同對象")
    void testEquals_sameId() {
        UserOnlineFileHistory anotherRecord = new UserOnlineFileHistory();
        anotherRecord.setId(1L);
        anotherRecord.setVersion(999L); // 不同的版本
        
        assertTrue(historyRecord.equals(anotherRecord));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試不同 id 的對象")
    void testEquals_differentId() {
        UserOnlineFileHistory anotherRecord = new UserOnlineFileHistory();
        anotherRecord.setId(2L);
        
        assertFalse(historyRecord.equals(anotherRecord));
    }

    @Test
    @DisplayName("邊界測試 - hashCode 方法一致性")
    void testHashCode_consistency() {
        int hash1 = historyRecord.hashCode();
        int hash2 = historyRecord.hashCode();
        
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("邊界測試 - 相同 id 的對象 hashCode 相同")
    void testHashCode_sameId() {
        UserOnlineFileHistory anotherRecord = new UserOnlineFileHistory();
        anotherRecord.setId(1L);
        
        assertEquals(historyRecord.hashCode(), anotherRecord.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試極大 ID 值")
    void testHistoryRecord_maxIdValues() {
        historyRecord.setId(Long.MAX_VALUE);
        historyRecord.setFileId(Long.MAX_VALUE);
        historyRecord.setVersion(Long.MAX_VALUE);
        historyRecord.setPreviousVersion(Long.MAX_VALUE - 1);
        historyRecord.setModifiedBy(Long.MAX_VALUE);
        
        assertEquals(Long.MAX_VALUE, historyRecord.getId());
        assertEquals(Long.MAX_VALUE, historyRecord.getFileId());
        assertEquals(Long.MAX_VALUE, historyRecord.getVersion());
        assertEquals(Long.MAX_VALUE - 1, historyRecord.getPreviousVersion());
        assertEquals(Long.MAX_VALUE, historyRecord.getModifiedBy());
    }

    @Test
    @DisplayName("邊界測試 - 測試零和負數 ID 值")
    void testHistoryRecord_zeroAndNegativeIdValues() {
        historyRecord.setId(0L);
        historyRecord.setFileId(-1L);
        historyRecord.setVersion(0L);
        historyRecord.setPreviousVersion(-1L);
        
        assertEquals(0L, historyRecord.getId());
        assertEquals(-1L, historyRecord.getFileId());
        assertEquals(0L, historyRecord.getVersion());
        assertEquals(-1L, historyRecord.getPreviousVersion());
    }

    @Test
    @DisplayName("邊界測試 - 測試空字符串屬性")
    void testHistoryRecord_emptyStringProperties() {
        historyRecord.setDiff("");
        historyRecord.setNote("");
        historyRecord.setSnapshotContent("");
        
        assertEquals("", historyRecord.getDiff());
        assertEquals("", historyRecord.getNote());
        assertEquals("", historyRecord.getSnapshotContent());
    }

    @Test
    @DisplayName("邊界測試 - 測試超長字符串屬性")
    void testHistoryRecord_veryLongStringProperties() {
        String longString = "超長內容".repeat(200);
        
        historyRecord.setDiff(longString);
        historyRecord.setNote(longString);
        historyRecord.setSnapshotContent(longString);
        
        assertEquals(longString, historyRecord.getDiff());
        assertEquals(longString, historyRecord.getNote());
        assertEquals(longString, historyRecord.getSnapshotContent());
    }

    @Test
    @DisplayName("邊界測試 - 測試內容格式化方法（短內容）")
    void testContentFormatting_shortContent() {
        String shortDiff = "短差異內容";
        String shortSnapshot = "短快照內容";
        
        historyRecord.setDiff(shortDiff);
        historyRecord.setSnapshotContent(shortSnapshot);
        
        String recordString = historyRecord.toString();
        assertTrue(recordString.contains("diff=" + shortDiff));
        assertTrue(recordString.contains("snapshotContent=" + shortSnapshot));
    }

    @Test
    @DisplayName("邊界測試 - 測試內容格式化方法（長內容）")
    void testContentFormatting_longContent() {
        String longDiff = "這是一個非常長的差異內容，超過了30個字符的限制，應該被截取並添加省略號";
        String longSnapshot = "這是一個非常長的快照內容，超過了30個字符的限制，應該被截取並添加省略號";
        
        historyRecord.setDiff(longDiff);
        historyRecord.setSnapshotContent(longSnapshot);
        
        String recordString = historyRecord.toString();
        
        String expectedTruncatedDiff = longDiff.substring(0, 30) + "...";
        String expectedTruncatedSnapshot = longSnapshot.substring(0, 30) + "...";
        
        assertTrue(recordString.contains("diff=" + expectedTruncatedDiff));
        assertTrue(recordString.contains("snapshotContent=" + expectedTruncatedSnapshot));
        assertFalse(recordString.contains(longDiff));
        assertFalse(recordString.contains(longSnapshot));
    }

    @Test
    @DisplayName("邊界測試 - 測試內容格式化方法（null 內容）")
    void testContentFormatting_nullContent() {
        historyRecord.setDiff(null);
        historyRecord.setSnapshotContent(null);
        
        String recordString = historyRecord.toString();
        assertTrue(recordString.contains("diff=null"));
        assertTrue(recordString.contains("snapshotContent=null"));
    }

    @Test
    @DisplayName("邊界測試 - 測試內容格式化方法（正好30字符）")
    void testContentFormatting_exactly30Characters() {
        String exactContent = "1234567890123456789012345678901"; // 31字符
        String thirtyCharContent = exactContent.substring(0, 30); // 正好30字符
        
        historyRecord.setDiff(thirtyCharContent);
        historyRecord.setSnapshotContent(thirtyCharContent);
        
        String recordString = historyRecord.toString();
        assertTrue(recordString.contains("diff=" + thirtyCharContent));
        assertTrue(recordString.contains("snapshotContent=" + thirtyCharContent));
        assertFalse(recordString.contains("..."));
    }

    @Test
    @DisplayName("邊界測試 - 測試時間邊界值")
    void testHistoryRecord_timeBoundaryValues() {
        // 測試最小時間
        LocalDateTime minTime = LocalDateTime.MIN;
        historyRecord.setModifiedTime(minTime);
        assertEquals(minTime, historyRecord.getModifiedTime());
        
        // 測試最大時間
        LocalDateTime maxTime = LocalDateTime.MAX;
        historyRecord.setModifiedTime(maxTime);
        assertEquals(maxTime, historyRecord.getModifiedTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試過去和未來時間")
    void testHistoryRecord_pastAndFutureTime() {
        LocalDateTime pastTime = LocalDateTime.now().minusYears(10);
        LocalDateTime futureTime = LocalDateTime.now().plusYears(10);
        
        historyRecord.setModifiedTime(pastTime);
        assertEquals(pastTime, historyRecord.getModifiedTime());
        
        historyRecord.setModifiedTime(futureTime);
        assertEquals(futureTime, historyRecord.getModifiedTime());
    }

    @Test
    @DisplayName("邊界測試 - toString 方法在屬性為 null 時")
    void testToString_withNullProperties() {
        UserOnlineFileHistory nullRecord = new UserOnlineFileHistory();
        
        String recordString = nullRecord.toString();
        assertNotNull(recordString);
        assertTrue(recordString.contains("id=null"));
        assertTrue(recordString.contains("fileId=null"));
        assertTrue(recordString.contains("version=null"));
        assertTrue(recordString.contains("diff=null"));
        assertTrue(recordString.contains("note=null"));
        assertTrue(recordString.contains("isSnapshot=false")); // 默認值
        assertTrue(recordString.contains("snapshotContent=null"));
        assertTrue(recordString.contains("modifiedTime=null"));
        assertTrue(recordString.contains("modifiedBy=null"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證實體註解存在")
    void testEntity_annotations() {
        // 驗證類級別註解
        assertTrue(UserOnlineFileHistory.class.isAnnotationPresent(org.springframework.data.relational.core.mapping.Table.class));
        
        // 驗證字段註解
        try {
            assertTrue(UserOnlineFileHistory.class.getDeclaredField("id").isAnnotationPresent(org.springframework.data.annotation.Id.class));
            assertTrue(UserOnlineFileHistory.class.getDeclaredField("fileId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserOnlineFileHistory.class.getDeclaredField("previousVersion").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserOnlineFileHistory.class.getDeclaredField("isSnapshot").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserOnlineFileHistory.class.getDeclaredField("snapshotContent").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserOnlineFileHistory.class.getDeclaredField("modifiedTime").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserOnlineFileHistory.class.getDeclaredField("modifiedBy").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
        } catch (NoSuchFieldException e) {
            fail("預期的字段不存在: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試 equals 方法的反射性、對稱性和傳遞性")
    void testEquals_properties() {
        UserOnlineFileHistory record1 = new UserOnlineFileHistory();
        record1.setId(1L);
        
        UserOnlineFileHistory record2 = new UserOnlineFileHistory();
        record2.setId(1L);
        
        UserOnlineFileHistory record3 = new UserOnlineFileHistory();
        record3.setId(1L);
        
        // 反射性：x.equals(x) 應該返回 true
        assertTrue(record1.equals(record1));
        
        // 對稱性：x.equals(y) 和 y.equals(x) 應該返回相同結果
        assertTrue(record1.equals(record2));
        assertTrue(record2.equals(record1));
        
        // 傳遞性：如果 x.equals(y) 和 y.equals(z)，則 x.equals(z) 應該為 true  
        assertTrue(record1.equals(record2));
        assertTrue(record2.equals(record3));
        assertTrue(record1.equals(record3));
    }

    @Test
    @DisplayName("邊界測試 - 測試版本控制業務場景")
    void testHistoryRecord_versionControlScenarios() {
        // 場景1：初始版本
        UserOnlineFileHistory initialVersion = new UserOnlineFileHistory();
        initialVersion.setVersion(1L);
        initialVersion.setPreviousVersion(null);
        initialVersion.setIsSnapshot(true);
        
        assertEquals(1L, initialVersion.getVersion());
        assertNull(initialVersion.getPreviousVersion());
        assertTrue(initialVersion.getIsSnapshot());
        
        // 場景2：增量版本
        UserOnlineFileHistory incrementalVersion = new UserOnlineFileHistory();
        incrementalVersion.setVersion(2L);
        incrementalVersion.setPreviousVersion(1L);
        incrementalVersion.setIsSnapshot(false);
        incrementalVersion.setDiff("{\"ops\":[{\"insert\":\"新內容\"}]}");
        
        assertEquals(2L, incrementalVersion.getVersion());
        assertEquals(1L, incrementalVersion.getPreviousVersion());
        assertFalse(incrementalVersion.getIsSnapshot());
        assertNotNull(incrementalVersion.getDiff());
        
        // 場景3：快照版本
        UserOnlineFileHistory snapshotVersion = new UserOnlineFileHistory();
        snapshotVersion.setVersion(10L);
        snapshotVersion.setPreviousVersion(9L);
        snapshotVersion.setIsSnapshot(true);
        snapshotVersion.setSnapshotContent("{\"complete\":\"snapshot\"}");
        snapshotVersion.setDiff(null);
        
        assertEquals(10L, snapshotVersion.getVersion());
        assertEquals(9L, snapshotVersion.getPreviousVersion());
        assertTrue(snapshotVersion.getIsSnapshot());
        assertNotNull(snapshotVersion.getSnapshotContent());
        assertNull(snapshotVersion.getDiff());
    }

    @Test
    @DisplayName("邊界測試 - 測試特殊字符在內容中")
    void testHistoryRecord_specialCharactersInContent() {
        String specialDiff = "{\"ops\":[{\"insert\":\"特殊字符: <>&\\\"'`\\n\\t🔒📄\"}]}";
        String specialNote = "備註包含特殊字符：<>&\"'`\n\t";
        String specialSnapshot = "{\"content\":\"快照內容：🌍 表情符號 📝 文檔\"}";
        
        historyRecord.setDiff(specialDiff);
        historyRecord.setNote(specialNote);
        historyRecord.setSnapshotContent(specialSnapshot);
        
        assertEquals(specialDiff, historyRecord.getDiff());
        assertEquals(specialNote, historyRecord.getNote());
        assertEquals(specialSnapshot, historyRecord.getSnapshotContent());
    }

    @Test
    @DisplayName("邊界測試 - 測試 Unicode 字符在內容中")
    void testHistoryRecord_unicodeCharacters() {
        String unicodeDiff = "{\"ops\":[{\"insert\":\"中文內容：你好世界！ 🌍\"}]}";
        String unicodeNote = "中文備註：修改了重要內容 📝";
        String unicodeSnapshot = "{\"content\":\"完整中文快照：文檔內容 📄\"}";
        
        historyRecord.setDiff(unicodeDiff);
        historyRecord.setNote(unicodeNote);
        historyRecord.setSnapshotContent(unicodeSnapshot);
        
        assertEquals(unicodeDiff, historyRecord.getDiff());
        assertEquals(unicodeNote, historyRecord.getNote());
        assertEquals(unicodeSnapshot, historyRecord.getSnapshotContent());
        
        // 驗證 toString 方法能正確處理 Unicode（但可能被截取）
        String recordString = historyRecord.toString();
        // 由於內容可能被格式化截取，只驗證原始數據正確設置
        assertTrue(recordString.length() > 0);
    }

    @Test
    @DisplayName("邊界測試 - 測試多行 JSON 內容")
    void testHistoryRecord_multilineJsonContent() {
        String multilineDiff = "{\n  \"ops\": [\n    {\n      \"retain\": 10\n    },\n    {\n      \"insert\": \"多行內容\\n測試\"\n    }\n  ]\n}";
        String multilineSnapshot = "{\n  \"type\": \"doc\",\n  \"content\": [\n    {\n      \"type\": \"paragraph\",\n      \"content\": [\n        {\n          \"type\": \"text\",\n          \"text\": \"多行快照內容\"\n        }\n      ]\n    }\n  ]\n}";
        
        historyRecord.setDiff(multilineDiff);
        historyRecord.setSnapshotContent(multilineSnapshot);
        
        assertEquals(multilineDiff, historyRecord.getDiff());
        assertEquals(multilineSnapshot, historyRecord.getSnapshotContent());
    }

    @Test
    @DisplayName("邊界測試 - 測試協作編輯歷史場景")
    void testHistoryRecord_collaborationHistory() {
        // 模擬多用戶協作編輯的歷史記錄
        
        // 用戶A的修改
        historyRecord.setVersion(1L);
        historyRecord.setModifiedBy(100L);
        historyRecord.setModifiedTime(LocalDateTime.now().minusHours(2));
        historyRecord.setNote("用戶A的初始修改");
        
        assertEquals(1L, historyRecord.getVersion());
        assertEquals(100L, historyRecord.getModifiedBy());
        assertEquals("用戶A的初始修改", historyRecord.getNote());
        
        // 用戶B的修改
        historyRecord.setVersion(2L);
        historyRecord.setPreviousVersion(1L);
        historyRecord.setModifiedBy(200L);
        historyRecord.setModifiedTime(LocalDateTime.now().minusHours(1));
        historyRecord.setNote("用戶B的後續修改");
        
        assertEquals(2L, historyRecord.getVersion());
        assertEquals(1L, historyRecord.getPreviousVersion());
        assertEquals(200L, historyRecord.getModifiedBy());
        assertEquals("用戶B的後續修改", historyRecord.getNote());
        
        // 用戶C的修改
        historyRecord.setVersion(3L);
        historyRecord.setPreviousVersion(2L);
        historyRecord.setModifiedBy(300L);
        historyRecord.setModifiedTime(LocalDateTime.now());
        historyRecord.setNote("用戶C的最新修改");
        
        assertEquals(3L, historyRecord.getVersion());
        assertEquals(2L, historyRecord.getPreviousVersion());
        assertEquals(300L, historyRecord.getModifiedBy());
        assertEquals("用戶C的最新修改", historyRecord.getNote());
    }
}