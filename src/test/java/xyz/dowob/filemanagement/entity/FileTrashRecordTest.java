package xyz.dowob.filemanagement.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 檔案回收站記錄實體的測試實現。
 * 
 * <p>基於已刪除檔案元數據的追蹤機制，支援檔案恢復和永久刪除操作。
 * 每個記錄關聯用戶檔案元數據並記錄刪除時間，維持與原始檔案的映射關係。
 * 
 * <p>測試涵蓋實體屬性的完整性驗證、構造方法的正確性、物件相等性比較規則
 * 以及字串表示格式。驗證時間屬性處理、關聯物件創建和邊界條件處理。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("FileTrashRecord 檔案回收站記錄實體測試")
class FileTrashRecordTest {

    private FileTrashRecord trashRecord;
    private LocalDateTime testTime;
    private UserFileMetadata testFileMetadata;

    @BeforeEach
    void setUp() {
        trashRecord = new FileTrashRecord();
        testTime = LocalDateTime.now();
        
        // 設置基本屬性
        trashRecord.setFileId(1L);
        trashRecord.setUserId(100L);
        trashRecord.setParentFolderId(10L);
        trashRecord.setDeleteTime(testTime);
        
        // 創建測試用的 UserFileMetadata
        testFileMetadata = new UserFileMetadata();
        testFileMetadata.setId(2L);
        testFileMetadata.setUserId(200L);
        testFileMetadata.setParentFolderId(20L);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 創建回收站記錄並設置基本屬性")
    void testCreateTrashRecord_withBasicProperties() {
        assertNotNull(trashRecord);
        assertEquals(1L, trashRecord.getFileId());
        assertEquals(100L, trashRecord.getUserId());
        assertEquals(10L, trashRecord.getParentFolderId());
        assertEquals(testTime, trashRecord.getDeleteTime());
    }

    @Test
    @DisplayName("一般測試 - 使用無參數構造方法")
    void testNoArgsConstructor() {
        FileTrashRecord newRecord = new FileTrashRecord();
        
        assertNotNull(newRecord);
        assertNull(newRecord.getFileId());
        assertNull(newRecord.getUserId());
        assertNull(newRecord.getParentFolderId());
        assertNull(newRecord.getDeleteTime());
    }

    @Test
    @DisplayName("一般測試 - 使用帶參數構造方法")
    void testParameterizedConstructor() {
        LocalDateTime deleteTime = LocalDateTime.now().minusHours(1);
        
        FileTrashRecord parameterizedRecord = new FileTrashRecord(testFileMetadata, deleteTime);
        
        assertEquals(2L, parameterizedRecord.getFileId());
        assertEquals(200L, parameterizedRecord.getUserId());
        assertEquals(20L, parameterizedRecord.getParentFolderId());
        assertEquals(deleteTime, parameterizedRecord.getDeleteTime());
    }

    @Test
    @DisplayName("一般測試 - 測試 toString 方法")
    void testToString() {
        String recordString = trashRecord.toString();
        
        assertNotNull(recordString);
        assertTrue(recordString.contains("fileId=1"));
        assertTrue(recordString.contains("userId=100"));
        assertTrue(recordString.contains("parentFolderId=10"));
        assertTrue(recordString.contains("deleteTime=" + testTime.toString()));
    }

    @Test
    @DisplayName("一般測試 - 測試檔案ID與用戶檔案元數據的關聯")
    void testFileIdAssociation() {
        FileTrashRecord associatedRecord = new FileTrashRecord(testFileMetadata, testTime);
        
        // 驗證檔案ID與原始檔案元數據ID一致
        assertEquals(testFileMetadata.getId(), associatedRecord.getFileId());
        assertEquals(testFileMetadata.getUserId(), associatedRecord.getUserId());
        assertEquals(testFileMetadata.getParentFolderId(), associatedRecord.getParentFolderId());
    }

    @Test
    @DisplayName("一般測試 - 測試刪除時間設置")
    void testDeleteTimeHandling() {
        LocalDateTime specificTime = LocalDateTime.of(2024, 3, 15, 10, 30, 0);
        
        trashRecord.setDeleteTime(specificTime);
        
        assertEquals(specificTime, trashRecord.getDeleteTime());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - equals 方法傳入 null")
    void testEquals_withNull() {
        assertFalse(trashRecord.equals(null));
    }

    @Test
    @DisplayName("異常測試 - equals 方法傳入不同類型對象")
    void testEquals_withDifferentType() {
        assertFalse(trashRecord.equals("not a trash record"));
        assertFalse(trashRecord.equals(123));
        assertFalse(trashRecord.equals(new Object()));
    }

    @Test
    @DisplayName("異常測試 - 帶參數構造方法傳入 null UserFileMetadata")
    void testParameterizedConstructor_nullUserFileMetadata() {
        LocalDateTime deleteTime = LocalDateTime.now();
        
        assertThrows(NullPointerException.class, () -> {
            new FileTrashRecord(null, deleteTime);
        });
    }

    @Test
    @DisplayName("異常測試 - equals 方法在 fileId 為 null 時")
    void testEquals_withNullFileId() {
        FileTrashRecord record1 = new FileTrashRecord();
        record1.setFileId(null);
        FileTrashRecord record2 = new FileTrashRecord();
        record2.setFileId(null);
        
        assertThrows(NullPointerException.class, () -> {
            record1.equals(record2);
        });
    }

    @Test
    @DisplayName("異常測試 - 設置 null 時間屬性")
    void testSetNullDeleteTime() {
        assertDoesNotThrow(() -> {
            trashRecord.setDeleteTime(null);
        });
        
        assertNull(trashRecord.getDeleteTime());
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同對象")
    void testEquals_sameObject() {
        assertTrue(trashRecord.equals(trashRecord));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同 fileId 的不同對象")
    void testEquals_sameFileId() {
        FileTrashRecord anotherRecord = new FileTrashRecord();
        anotherRecord.setFileId(1L);
        anotherRecord.setUserId(999L); // 不同的用戶ID
        
        assertTrue(trashRecord.equals(anotherRecord));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試不同 fileId 的對象")
    void testEquals_differentFileId() {
        FileTrashRecord anotherRecord = new FileTrashRecord();
        anotherRecord.setFileId(2L);
        
        assertFalse(trashRecord.equals(anotherRecord));
    }

    @Test
    @DisplayName("邊界測試 - 測試極大 ID 值")
    void testTrashRecord_maxIdValues() {
        trashRecord.setFileId(Long.MAX_VALUE);
        trashRecord.setUserId(Long.MAX_VALUE);
        trashRecord.setParentFolderId(Long.MAX_VALUE);
        
        assertEquals(Long.MAX_VALUE, trashRecord.getFileId());
        assertEquals(Long.MAX_VALUE, trashRecord.getUserId());
        assertEquals(Long.MAX_VALUE, trashRecord.getParentFolderId());
    }

    @Test
    @DisplayName("邊界測試 - 測試零 ID 值")
    void testTrashRecord_zeroIdValues() {
        trashRecord.setFileId(0L);
        trashRecord.setUserId(0L);
        trashRecord.setParentFolderId(0L);
        
        assertEquals(0L, trashRecord.getFileId());
        assertEquals(0L, trashRecord.getUserId());
        assertEquals(0L, trashRecord.getParentFolderId());
    }

    @Test
    @DisplayName("邊界測試 - 測試負數 ID 值")
    void testTrashRecord_negativeIdValues() {
        trashRecord.setFileId(-1L);
        trashRecord.setUserId(-100L);
        trashRecord.setParentFolderId(-10L);
        
        assertEquals(-1L, trashRecord.getFileId());
        assertEquals(-100L, trashRecord.getUserId());
        assertEquals(-10L, trashRecord.getParentFolderId());
    }

    @Test
    @DisplayName("邊界測試 - 測試過去時間")
    void testTrashRecord_pastTime() {
        LocalDateTime pastTime = LocalDateTime.now().minusYears(5);
        
        trashRecord.setDeleteTime(pastTime);
        
        assertEquals(pastTime, trashRecord.getDeleteTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試極遠未來時間")
    void testTrashRecord_farFutureTime() {
        LocalDateTime futureTime = LocalDateTime.now().plusYears(100);
        
        trashRecord.setDeleteTime(futureTime);
        
        assertEquals(futureTime, trashRecord.getDeleteTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試時間邊界值")
    void testTrashRecord_timeBoundaryValues() {
        // 測試最小時間
        LocalDateTime minTime = LocalDateTime.MIN;
        trashRecord.setDeleteTime(minTime);
        assertEquals(minTime, trashRecord.getDeleteTime());
        
        // 測試最大時間
        LocalDateTime maxTime = LocalDateTime.MAX;
        trashRecord.setDeleteTime(maxTime);
        assertEquals(maxTime, trashRecord.getDeleteTime());
    }

    @Test
    @DisplayName("邊界測試 - toString 方法在屬性為 null 時")
    void testToString_withNullProperties() {
        FileTrashRecord nullRecord = new FileTrashRecord();
        
        String recordString = nullRecord.toString();
        assertNotNull(recordString);
        assertTrue(recordString.contains("fileId=null"));
        assertTrue(recordString.contains("userId=null"));
        assertTrue(recordString.contains("parentFolderId=null"));
        assertTrue(recordString.contains("deleteTime=null"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證實體註解存在")
    void testEntity_annotations() {
        // 驗證類級別註解
        assertTrue(FileTrashRecord.class.isAnnotationPresent(org.springframework.data.relational.core.mapping.Table.class));
        
        // 驗證字段註解
        try {
            assertTrue(FileTrashRecord.class.getDeclaredField("fileId").isAnnotationPresent(org.springframework.data.annotation.Id.class));
            assertTrue(FileTrashRecord.class.getDeclaredField("fileId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(FileTrashRecord.class.getDeclaredField("userId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(FileTrashRecord.class.getDeclaredField("parentFolderId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(FileTrashRecord.class.getDeclaredField("deleteTime").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
        } catch (NoSuchFieldException e) {
            fail("預期的字段不存在: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("邊界測試 - 帶參數構造方法處理用戶檔案元數據邊界值")
    void testParameterizedConstructor_boundaryValues() {
        // 創建邊界值的用戶檔案元數據
        UserFileMetadata boundaryMetadata = new UserFileMetadata();
        boundaryMetadata.setId(Long.MAX_VALUE);
        boundaryMetadata.setUserId(0L);
        boundaryMetadata.setParentFolderId(null);
        
        LocalDateTime deleteTime = LocalDateTime.now();
        
        FileTrashRecord boundaryRecord = new FileTrashRecord(boundaryMetadata, deleteTime);
        
        assertEquals(Long.MAX_VALUE, boundaryRecord.getFileId());
        assertEquals(0L, boundaryRecord.getUserId());
        assertNull(boundaryRecord.getParentFolderId());
        assertEquals(deleteTime, boundaryRecord.getDeleteTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試與用戶檔案元數據 ID 關聯的一致性")
    void testFileIdConsistency_withUserFileMetadata() {
        // 創建多個測試用例確保一致性
        for (long id = 1L; id <= 5L; id++) {
            UserFileMetadata metadata = new UserFileMetadata();
            metadata.setId(id);
            metadata.setUserId(id * 10);
            metadata.setParentFolderId(id * 100);
            
            FileTrashRecord record = new FileTrashRecord(metadata, testTime);
            
            assertEquals(id, record.getFileId());
            assertEquals(id * 10, record.getUserId());
            assertEquals(id * 100, record.getParentFolderId());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試帶參數構造方法的 null 刪除時間")
    void testParameterizedConstructor_nullDeleteTime() {
        FileTrashRecord recordWithNullTime = new FileTrashRecord(testFileMetadata, null);
        
        assertEquals(testFileMetadata.getId(), recordWithNullTime.getFileId());
        assertEquals(testFileMetadata.getUserId(), recordWithNullTime.getUserId());
        assertEquals(testFileMetadata.getParentFolderId(), recordWithNullTime.getParentFolderId());
        assertNull(recordWithNullTime.getDeleteTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試 equals 方法的反射性、對稱性和傳遞性")
    void testEquals_properties() {
        FileTrashRecord record1 = new FileTrashRecord();
        record1.setFileId(1L);
        
        FileTrashRecord record2 = new FileTrashRecord();
        record2.setFileId(1L);
        
        FileTrashRecord record3 = new FileTrashRecord();
        record3.setFileId(1L);
        
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
}