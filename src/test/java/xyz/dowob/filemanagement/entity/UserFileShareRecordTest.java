package xyz.dowob.filemanagement.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用戶檔案分享記錄實體的測試實現。
 * 
 * <p>基於檔案權限管理的分享追蹤機制，記錄用戶與檔案之間的分享關係。
 * 維護用戶 ID 與檔案 ID 的映射關係，支援多對多分享模式和權限查詢，
 * 確保檔案存取控制和協作功能的正確執行。
 * 
 * <p>測試涵蓋關聯關係的完整性驗證、構造方法的正確性和業務邏輯的有效性。
 * 驗證批量分享操作、關係唯一性約束以及物件相等性比較規則。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("UserFileShareRecord 用戶檔案分享記錄實體測試")
class UserFileShareRecordTest {

    private UserFileShareRecord shareRecord;

    @BeforeEach
    void setUp() {
        shareRecord = new UserFileShareRecord();
        
        // 設置基本屬性
        shareRecord.setId(1L);
        shareRecord.setUserId(100L);
        shareRecord.setFileId(200L);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 創建分享記錄並設置基本屬性")
    void testCreateShareRecord_withBasicProperties() {
        assertNotNull(shareRecord);
        assertEquals(1L, shareRecord.getId());
        assertEquals(100L, shareRecord.getUserId());
        assertEquals(200L, shareRecord.getFileId());
    }

    @Test
    @DisplayName("一般測試 - 使用無參數構造方法")
    void testNoArgsConstructor() {
        UserFileShareRecord newRecord = new UserFileShareRecord();
        
        assertNotNull(newRecord);
        assertNull(newRecord.getId());
        assertNull(newRecord.getUserId());
        assertNull(newRecord.getFileId());
    }

    @Test
    @DisplayName("一般測試 - 使用帶參數構造方法")
    void testParameterizedConstructor() {
        Long userId = 300L;
        Long fileId = 400L;
        
        UserFileShareRecord parameterizedRecord = new UserFileShareRecord(userId, fileId);
        
        assertEquals(userId, parameterizedRecord.getUserId());
        assertEquals(fileId, parameterizedRecord.getFileId());
        assertNull(parameterizedRecord.getId()); // ID 未設置，應為 null
    }

    @Test
    @DisplayName("一般測試 - 測試 toString 方法")
    void testToString() {
        String recordString = shareRecord.toString();
        
        assertNotNull(recordString);
        assertTrue(recordString.contains("id=1"));
        assertTrue(recordString.contains("userId=100"));
        assertTrue(recordString.contains("fileId=200"));
    }

    @Test
    @DisplayName("一般測試 - 測試用戶與檔案的關聯性")
    void testUserFileAssociation() {
        Long userId = 500L;
        Long fileId = 600L;
        
        shareRecord.setUserId(userId);
        shareRecord.setFileId(fileId);
        
        assertEquals(userId, shareRecord.getUserId());
        assertEquals(fileId, shareRecord.getFileId());
    }

    @Test
    @DisplayName("一般測試 - 測試多個分享記錄的創建")
    void testMultipleShareRecords() {
        UserFileShareRecord record1 = new UserFileShareRecord(101L, 201L);
        UserFileShareRecord record2 = new UserFileShareRecord(102L, 202L);
        UserFileShareRecord record3 = new UserFileShareRecord(103L, 203L);
        
        assertEquals(101L, record1.getUserId());
        assertEquals(201L, record1.getFileId());
        
        assertEquals(102L, record2.getUserId());
        assertEquals(202L, record2.getFileId());
        
        assertEquals(103L, record3.getUserId());
        assertEquals(203L, record3.getFileId());
    }

    @Test
    @DisplayName("一般測試 - 測試同一用戶分享多個檔案")
    void testSameUserMultipleFiles() {
        Long userId = 100L;
        
        UserFileShareRecord record1 = new UserFileShareRecord(userId, 201L);
        UserFileShareRecord record2 = new UserFileShareRecord(userId, 202L);
        UserFileShareRecord record3 = new UserFileShareRecord(userId, 203L);
        
        assertEquals(userId, record1.getUserId());
        assertEquals(userId, record2.getUserId());
        assertEquals(userId, record3.getUserId());
        
        assertEquals(201L, record1.getFileId());
        assertEquals(202L, record2.getFileId());
        assertEquals(203L, record3.getFileId());
    }

    @Test
    @DisplayName("一般測試 - 測試同一檔案被多個用戶分享")
    void testSameFileMultipleUsers() {
        Long fileId = 200L;
        
        UserFileShareRecord record1 = new UserFileShareRecord(101L, fileId);
        UserFileShareRecord record2 = new UserFileShareRecord(102L, fileId);
        UserFileShareRecord record3 = new UserFileShareRecord(103L, fileId);
        
        assertEquals(fileId, record1.getFileId());
        assertEquals(fileId, record2.getFileId());
        assertEquals(fileId, record3.getFileId());
        
        assertEquals(101L, record1.getUserId());
        assertEquals(102L, record2.getUserId());
        assertEquals(103L, record3.getUserId());
    }

    @Test
    @DisplayName("一般測試 - 測試分享記錄的業務邏輯")
    void testShareRecordBusinessLogic() {
        // 模擬業務場景：用戶分享檔案
        Long shareUserId = 100L;
        Long sharedFileId = 200L;
        
        UserFileShareRecord businessRecord = new UserFileShareRecord(shareUserId, sharedFileId);
        businessRecord.setId(1L);
        
        // 驗證分享記錄包含正確的用戶和檔案信息
        assertEquals(shareUserId, businessRecord.getUserId());
        assertEquals(sharedFileId, businessRecord.getFileId());
        assertNotNull(businessRecord.getId());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - equals 方法傳入 null")
    void testEquals_withNull() {
        assertFalse(shareRecord.equals(null));
    }

    @Test
    @DisplayName("異常測試 - equals 方法傳入不同類型對象")
    void testEquals_withDifferentType() {
        assertFalse(shareRecord.equals("not a share record"));
        assertFalse(shareRecord.equals(123));
        assertFalse(shareRecord.equals(new Object()));
    }

    @Test
    @DisplayName("異常測試 - hashCode 方法在 id 為 null 時")
    void testHashCode_withNullId() {
        UserFileShareRecord recordWithNullId = new UserFileShareRecord();
        recordWithNullId.setId(null);
        
        assertThrows(NullPointerException.class, recordWithNullId::hashCode);
    }

    @Test
    @DisplayName("異常測試 - equals 方法在 id 為 null 時")
    void testEquals_withNullId() {
        UserFileShareRecord record1 = new UserFileShareRecord();
        record1.setId(null);
        UserFileShareRecord record2 = new UserFileShareRecord();
        record2.setId(null);
        
        assertThrows(NullPointerException.class, () -> record1.equals(record2));
    }

    @Test
    @DisplayName("異常測試 - 帶參數構造方法傳入 null 參數")
    void testParameterizedConstructor_nullParameters() {
        // 允許傳入 null 參數，但需要驗證行為
        assertDoesNotThrow(() -> {
            UserFileShareRecord recordWithNullUserId = new UserFileShareRecord(null, 200L);
            UserFileShareRecord recordWithNullFileId = new UserFileShareRecord(100L, null);
            UserFileShareRecord recordWithBothNull = new UserFileShareRecord(null, null);
            
            assertNull(recordWithNullUserId.getUserId());
            assertEquals(200L, recordWithNullUserId.getFileId());
            
            assertNull(recordWithNullFileId.getFileId());
            assertEquals(100L, recordWithNullFileId.getUserId());
            
            assertNull(recordWithBothNull.getUserId());
            assertNull(recordWithBothNull.getFileId());
        });
    }

    @Test
    @DisplayName("異常測試 - 設置 null ID 屬性")
    void testSetNullIdProperties() {
        assertDoesNotThrow(() -> {
            shareRecord.setUserId(null);
            shareRecord.setFileId(null);
        });
        
        assertNull(shareRecord.getUserId());
        assertNull(shareRecord.getFileId());
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同對象")
    void testEquals_sameObject() {
        assertTrue(shareRecord.equals(shareRecord));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同 id 的不同對象")
    void testEquals_sameId() {
        UserFileShareRecord anotherRecord = new UserFileShareRecord();
        anotherRecord.setId(1L);
        anotherRecord.setUserId(999L); // 不同的用戶ID
        anotherRecord.setFileId(888L); // 不同的檔案ID
        
        assertTrue(shareRecord.equals(anotherRecord));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試不同 id 的對象")
    void testEquals_differentId() {
        UserFileShareRecord anotherRecord = new UserFileShareRecord();
        anotherRecord.setId(2L);
        
        assertFalse(shareRecord.equals(anotherRecord));
    }

    @Test
    @DisplayName("邊界測試 - hashCode 方法一致性")
    void testHashCode_consistency() {
        int hash1 = shareRecord.hashCode();
        int hash2 = shareRecord.hashCode();
        
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("邊界測試 - 相同 id 的對象 hashCode 相同")
    void testHashCode_sameId() {
        UserFileShareRecord anotherRecord = new UserFileShareRecord();
        anotherRecord.setId(1L);
        
        assertEquals(shareRecord.hashCode(), anotherRecord.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試極大 ID 值")
    void testShareRecord_maxIdValues() {
        shareRecord.setId(Long.MAX_VALUE);
        shareRecord.setUserId(Long.MAX_VALUE);
        shareRecord.setFileId(Long.MAX_VALUE);
        
        assertEquals(Long.MAX_VALUE, shareRecord.getId());
        assertEquals(Long.MAX_VALUE, shareRecord.getUserId());
        assertEquals(Long.MAX_VALUE, shareRecord.getFileId());
    }

    @Test
    @DisplayName("邊界測試 - 測試零 ID 值")
    void testShareRecord_zeroIdValues() {
        shareRecord.setId(0L);
        shareRecord.setUserId(0L);
        shareRecord.setFileId(0L);
        
        assertEquals(0L, shareRecord.getId());
        assertEquals(0L, shareRecord.getUserId());
        assertEquals(0L, shareRecord.getFileId());
    }

    @Test
    @DisplayName("邊界測試 - 測試負數 ID 值")
    void testShareRecord_negativeIdValues() {
        shareRecord.setId(-1L);
        shareRecord.setUserId(-100L);
        shareRecord.setFileId(-200L);
        
        assertEquals(-1L, shareRecord.getId());
        assertEquals(-100L, shareRecord.getUserId());
        assertEquals(-200L, shareRecord.getFileId());
    }

    @Test
    @DisplayName("邊界測試 - toString 方法在屬性為 null 時")
    void testToString_withNullProperties() {
        UserFileShareRecord nullRecord = new UserFileShareRecord();
        
        String recordString = nullRecord.toString();
        assertNotNull(recordString);
        assertTrue(recordString.contains("id=null"));
        assertTrue(recordString.contains("userId=null"));
        assertTrue(recordString.contains("fileId=null"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證實體註解存在")
    void testEntity_annotations() {
        // 驗證類級別註解
        assertTrue(UserFileShareRecord.class.isAnnotationPresent(org.springframework.data.relational.core.mapping.Table.class));
        
        // 驗證字段註解
        try {
            assertTrue(UserFileShareRecord.class.getDeclaredField("id").isAnnotationPresent(org.springframework.data.annotation.Id.class));
        } catch (NoSuchFieldException e) {
            fail("預期的字段不存在: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試 equals 方法的反射性、對稱性和傳遞性")
    void testEquals_properties() {
        UserFileShareRecord record1 = new UserFileShareRecord();
        record1.setId(1L);
        
        UserFileShareRecord record2 = new UserFileShareRecord();
        record2.setId(1L);
        
        UserFileShareRecord record3 = new UserFileShareRecord();
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
    @DisplayName("邊界測試 - 測試帶參數構造方法與 setter 的一致性")
    void testParameterizedConstructor_consistency() {
        Long userId = 100L;
        Long fileId = 200L;
        
        // 使用帶參數構造方法
        UserFileShareRecord constructorRecord = new UserFileShareRecord(userId, fileId);
        
        // 使用 setter 方法
        UserFileShareRecord setterRecord = new UserFileShareRecord();
        setterRecord.setUserId(userId);
        setterRecord.setFileId(fileId);
        
        // 驗證結果一致
        assertEquals(constructorRecord.getUserId(), setterRecord.getUserId());
        assertEquals(constructorRecord.getFileId(), setterRecord.getFileId());
    }

    @Test
    @DisplayName("邊界測試 - 測試大量分享記錄的創建")
    void testMassiveShareRecords() {
        int recordCount = 1000;
        UserFileShareRecord[] records = new UserFileShareRecord[recordCount];
        
        // 創建大量分享記錄
        for (int i = 0; i < recordCount; i++) {
            records[i] = new UserFileShareRecord((long) i, (long) (i * 2));
        }
        
        // 驗證所有記錄都正確創建
        for (int i = 0; i < recordCount; i++) {
            assertEquals((long) i, records[i].getUserId());
            assertEquals((long) (i * 2), records[i].getFileId());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試分享記錄的唯一性約束模擬")
    void testShareRecord_uniquenessConstraintSimulation() {
        // 模擬業務約束：同一用戶不能重複分享同一檔案
        Long userId = 100L;
        Long fileId = 200L;
        
        UserFileShareRecord record1 = new UserFileShareRecord(userId, fileId);
        record1.setId(1L);
        
        UserFileShareRecord record2 = new UserFileShareRecord(userId, fileId);
        record2.setId(2L);
        
        // 雖然用戶ID和檔案ID相同，但主鍵ID不同，所以對象不相等
        assertNotEquals(record1, record2);
        assertNotEquals(record1.hashCode(), record2.hashCode());
        
        // 但業務屬性相同
        assertEquals(record1.getUserId(), record2.getUserId());
        assertEquals(record1.getFileId(), record2.getFileId());
    }

    @Test
    @DisplayName("邊界測試 - 測試分享記錄的組合場景")
    void testShareRecord_combinationScenarios() {
        // 場景1：用戶A分享檔案X
        UserFileShareRecord scenario1 = new UserFileShareRecord(100L, 200L);
        scenario1.setId(1L);
        
        // 場景2：用戶B分享檔案X（同一檔案被多人分享）
        UserFileShareRecord scenario2 = new UserFileShareRecord(101L, 200L);
        scenario2.setId(2L);
        
        // 場景3：用戶A分享檔案Y（同一用戶分享多個檔案）
        UserFileShareRecord scenario3 = new UserFileShareRecord(100L, 201L);
        scenario3.setId(3L);
        
        // 驗證各場景的獨立性
        assertNotEquals(scenario1, scenario2);
        assertNotEquals(scenario1, scenario3);
        assertNotEquals(scenario2, scenario3);
        
        // 驗證業務關聯性
        assertEquals(scenario1.getFileId(), scenario2.getFileId()); // 同一檔案
        assertEquals(scenario1.getUserId(), scenario3.getUserId()); // 同一用戶
    }

    @Test
    @DisplayName("邊界測試 - 測試 ID 範圍的完整性")
    void testShareRecord_idRangeCompleteness() {
        Long[] testIds = {
            Long.MIN_VALUE,
            -1000L,
            -1L,
            0L,
            1L,
            1000L,
            Long.MAX_VALUE
        };
        
        for (Long testId : testIds) {
            UserFileShareRecord record = new UserFileShareRecord();
            record.setId(testId);
            record.setUserId(testId);
            record.setFileId(testId);
            
            assertEquals(testId, record.getId());
            assertEquals(testId, record.getUserId());
            assertEquals(testId, record.getFileId());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試對象狀態的不變性")
    void testShareRecord_objectStateImmutability() {
        UserFileShareRecord originalRecord = new UserFileShareRecord(100L, 200L);
        originalRecord.setId(1L);
        
        // 記錄原始狀態
        Long originalId = originalRecord.getId();
        Long originalUserId = originalRecord.getUserId();
        Long originalFileId = originalRecord.getFileId();
        
        // 創建另一個對象進行操作
        UserFileShareRecord anotherRecord = new UserFileShareRecord();
        anotherRecord.setId(2L);
        anotherRecord.setUserId(999L);
        anotherRecord.setFileId(888L);
        
        // 驗證原始對象狀態未被影響
        assertEquals(originalId, originalRecord.getId());
        assertEquals(originalUserId, originalRecord.getUserId());
        assertEquals(originalFileId, originalRecord.getFileId());
    }
}