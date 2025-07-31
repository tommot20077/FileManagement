package xyz.dowob.filemanagement.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 檔案傳輸任務實體的測試實現。
 * 
 * <p>基於 GridFS 分散式檔案系統的非同步檔案傳輸追蹤機制。
 * 記錄傳輸任務 ID、檔案 MD5 雜湊值、GridFS 識別碼、檔案大小以及執行時間，
 * 支援傳輸狀態監控和錯誤處理，確保檔案上傳下載的可靠性。
 * 
 * <p>測試涵蓋傳輸狀態枚舉的完整性、時間區間計算的準確性和任務追蹤的有效性。
 * 驗證狀態轉換邏輯、物件相等性比較規則以及各種傳輸場景的處理機制。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("TransfersTask 檔案傳輸任務實體測試")
class TransfersTaskTest {

    private TransfersTask transfersTask;
    private LocalDateTime testStartTime;
    private LocalDateTime testFinishTime;

    @BeforeEach
    void setUp() {
        transfersTask = new TransfersTask();
        testStartTime = LocalDateTime.now();
        testFinishTime = testStartTime.plusMinutes(5);
        
        // 設置基本屬性
        transfersTask.setId(1L);
        transfersTask.setTransferTaskId("task-12345-abcde");
        transfersTask.setMd5("5d41402abc4b2a76b9719d911017c592");
        transfersTask.setGridFsId("grid-fs-67890-fghij");
        transfersTask.setFileSize(2048000L);
        transfersTask.setStartTime(testStartTime);
        transfersTask.setFinishTime(testFinishTime);
        transfersTask.setMessage("檔案傳輸完成");
        transfersTask.setStatus(TransfersStatusEnum.COMPLETED);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 創建傳輸任務並設置基本屬性")
    void testCreateTransfersTask_withBasicProperties() {
        assertNotNull(transfersTask);
        assertEquals(1L, transfersTask.getId());
        assertEquals("task-12345-abcde", transfersTask.getTransferTaskId());
        assertEquals("5d41402abc4b2a76b9719d911017c592", transfersTask.getMd5());
        assertEquals("grid-fs-67890-fghij", transfersTask.getGridFsId());
        assertEquals(2048000L, transfersTask.getFileSize());
        assertEquals(testStartTime, transfersTask.getStartTime());
        assertEquals(testFinishTime, transfersTask.getFinishTime());
        assertEquals("檔案傳輸完成", transfersTask.getMessage());
        assertEquals(TransfersStatusEnum.COMPLETED, transfersTask.getStatus());
    }

    @Test
    @DisplayName("一般測試 - 測試不同傳輸狀態枚舉")
    void testTransfersTask_differentStatusEnums() {
        for (TransfersStatusEnum status : TransfersStatusEnum.values()) {
            transfersTask.setStatus(status);
            assertEquals(status, transfersTask.getStatus());
        }
    }

    @Test
    @DisplayName("一般測試 - 測試 toString 方法")
    void testToString() {
        String taskString = transfersTask.toString();
        
        assertNotNull(taskString);
        assertTrue(taskString.contains("id=1"));
        assertTrue(taskString.contains("transferTaskId=task-12345-abcde"));
        assertTrue(taskString.contains("md5=5d41402abc4b2a76b9719d911017c592"));
        assertTrue(taskString.contains("gridFsId=grid-fs-67890-fghij"));
        assertTrue(taskString.contains("fileSize=2048000"));
        assertTrue(taskString.contains("message=檔案傳輸完成"));
        assertTrue(taskString.contains("status=COMPLETED"));
    }

    @Test
    @DisplayName("一般測試 - 測試任務執行時間計算")
    void testTaskExecutionTime() {
        LocalDateTime start = LocalDateTime.of(2024, 3, 15, 10, 0, 0);
        LocalDateTime finish = LocalDateTime.of(2024, 3, 15, 10, 5, 30);
        
        transfersTask.setStartTime(start);
        transfersTask.setFinishTime(finish);
        
        assertEquals(start, transfersTask.getStartTime());
        assertEquals(finish, transfersTask.getFinishTime());
        
        // 計算執行時間（秒）
        long executionSeconds = java.time.Duration.between(start, finish).getSeconds();
        assertEquals(330, executionSeconds); // 5分30秒 = 330秒
    }

    @Test
    @DisplayName("一般測試 - 測試進行中的任務（無完成時間）")
    void testInProgressTask() {
        transfersTask.setStatus(TransfersStatusEnum.UPLOADING);
        transfersTask.setFinishTime(null);
        transfersTask.setMessage("正在傳輸中...");
        
        assertEquals(TransfersStatusEnum.UPLOADING, transfersTask.getStatus());
        assertNull(transfersTask.getFinishTime());
        assertEquals("正在傳輸中...", transfersTask.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 測試失敗的任務")
    void testFailedTask() {
        transfersTask.setStatus(TransfersStatusEnum.FAILED);
        transfersTask.setMessage("傳輸失敗：網路連接中斷");
        
        assertEquals(TransfersStatusEnum.FAILED, transfersTask.getStatus());
        assertEquals("傳輸失敗：網路連接中斷", transfersTask.getMessage());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - equals 方法傳入 null")
    void testEquals_withNull() {
        assertFalse(transfersTask.equals(null));
    }

    @Test
    @DisplayName("異常測試 - equals 方法傳入不同類型對象")
    void testEquals_withDifferentType() {
        assertFalse(transfersTask.equals("not a transfers task"));
        assertFalse(transfersTask.equals(123));
        assertFalse(transfersTask.equals(new Object()));
    }

    @Test
    @DisplayName("異常測試 - hashCode 方法在 id 為 null 時")
    void testHashCode_withNullId() {
        TransfersTask taskWithNullId = new TransfersTask();
        taskWithNullId.setId(null);
        
        assertThrows(NullPointerException.class, taskWithNullId::hashCode);
    }

    @Test
    @DisplayName("異常測試 - equals 方法在 id 為 null 時")
    void testEquals_withNullId() {
        TransfersTask task1 = new TransfersTask();
        task1.setId(null);
        TransfersTask task2 = new TransfersTask();
        task2.setId(null);
        
        assertThrows(NullPointerException.class, () -> task1.equals(task2));
    }

    @Test
    @DisplayName("異常測試 - 設置 null 時間屬性")
    void testSetNullTimeProperties() {
        assertDoesNotThrow(() -> {
            transfersTask.setStartTime(null);
            transfersTask.setFinishTime(null);
        });
        
        assertNull(transfersTask.getStartTime());
        assertNull(transfersTask.getFinishTime());
    }

    @Test
    @DisplayName("異常測試 - 設置 null 字符串屬性")
    void testSetNullStringProperties() {
        assertDoesNotThrow(() -> {
            transfersTask.setTransferTaskId(null);
            transfersTask.setMd5(null);
            transfersTask.setGridFsId(null);
            transfersTask.setMessage(null);
        });
        
        assertNull(transfersTask.getTransferTaskId());
        assertNull(transfersTask.getMd5());
        assertNull(transfersTask.getGridFsId());
        assertNull(transfersTask.getMessage());
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同對象")
    void testEquals_sameObject() {
        assertTrue(transfersTask.equals(transfersTask));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同 id 的不同對象")
    void testEquals_sameId() {
        TransfersTask anotherTask = new TransfersTask();
        anotherTask.setId(1L);
        anotherTask.setTransferTaskId("different-task-id"); // 不同的任務ID
        
        assertTrue(transfersTask.equals(anotherTask));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試不同 id 的對象")
    void testEquals_differentId() {
        TransfersTask anotherTask = new TransfersTask();
        anotherTask.setId(2L);
        
        assertFalse(transfersTask.equals(anotherTask));
    }

    @Test
    @DisplayName("邊界測試 - hashCode 方法一致性")
    void testHashCode_consistency() {
        int hash1 = transfersTask.hashCode();
        int hash2 = transfersTask.hashCode();
        
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("邊界測試 - 相同 id 的對象 hashCode 相同")
    void testHashCode_sameId() {
        TransfersTask anotherTask = new TransfersTask();
        anotherTask.setId(1L);
        
        assertEquals(transfersTask.hashCode(), anotherTask.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試極大檔案大小")
    void testTransfersTask_maxFileSize() {
        transfersTask.setFileSize(Long.MAX_VALUE);
        
        assertEquals(Long.MAX_VALUE, transfersTask.getFileSize());
    }

    @Test
    @DisplayName("邊界測試 - 測試零檔案大小")
    void testTransfersTask_zeroFileSize() {
        transfersTask.setFileSize(0L);
        
        assertEquals(0L, transfersTask.getFileSize());
    }

    @Test
    @DisplayName("邊界測試 - 測試負數檔案大小")
    void testTransfersTask_negativeFileSize() {
        transfersTask.setFileSize(-1L);
        
        assertEquals(-1L, transfersTask.getFileSize());
    }

    @Test
    @DisplayName("邊界測試 - 測試空字符串屬性")
    void testTransfersTask_emptyStringProperties() {
        transfersTask.setTransferTaskId("");
        transfersTask.setMd5("");
        transfersTask.setGridFsId("");
        transfersTask.setMessage("");
        
        assertEquals("", transfersTask.getTransferTaskId());
        assertEquals("", transfersTask.getMd5());
        assertEquals("", transfersTask.getGridFsId());
        assertEquals("", transfersTask.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - 測試超長字符串屬性")
    void testTransfersTask_veryLongStringProperties() {
        String longString = "a".repeat(1000);
        
        transfersTask.setTransferTaskId(longString);
        transfersTask.setMd5(longString);
        transfersTask.setGridFsId(longString);
        transfersTask.setMessage(longString);
        
        assertEquals(longString, transfersTask.getTransferTaskId());
        assertEquals(longString, transfersTask.getMd5());
        assertEquals(longString, transfersTask.getGridFsId());
        assertEquals(longString, transfersTask.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - 測試特殊字符在屬性中")
    void testTransfersTask_specialCharactersInProperties() {
        String specialTaskId = "task@#$%^&*()";
        String specialMd5 = "md5<>&\"'`\n\t";
        String specialGridFs = "grid-fs+tag@domain.com";
        String specialMessage = "訊息包含特殊字符：<>&\"'`\n\t";
        
        transfersTask.setTransferTaskId(specialTaskId);
        transfersTask.setMd5(specialMd5);
        transfersTask.setGridFsId(specialGridFs);
        transfersTask.setMessage(specialMessage);
        
        assertEquals(specialTaskId, transfersTask.getTransferTaskId());
        assertEquals(specialMd5, transfersTask.getMd5());
        assertEquals(specialGridFs, transfersTask.getGridFsId());
        assertEquals(specialMessage, transfersTask.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - 測試過去時間")
    void testTransfersTask_pastTime() {
        LocalDateTime pastTime = LocalDateTime.now().minusYears(5);
        
        transfersTask.setStartTime(pastTime);
        transfersTask.setFinishTime(pastTime.plusHours(1));
        
        assertEquals(pastTime, transfersTask.getStartTime());
        assertEquals(pastTime.plusHours(1), transfersTask.getFinishTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試極遠未來時間")
    void testTransfersTask_farFutureTime() {
        LocalDateTime futureTime = LocalDateTime.now().plusYears(100);
        
        transfersTask.setStartTime(futureTime);
        transfersTask.setFinishTime(futureTime.plusHours(1));
        
        assertEquals(futureTime, transfersTask.getStartTime());
        assertEquals(futureTime.plusHours(1), transfersTask.getFinishTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試時間邊界值")
    void testTransfersTask_timeBoundaryValues() {
        // 測試最小時間
        LocalDateTime minTime = LocalDateTime.MIN;
        transfersTask.setStartTime(minTime);
        assertEquals(minTime, transfersTask.getStartTime());
        
        // 測試最大時間
        LocalDateTime maxTime = LocalDateTime.MAX;
        transfersTask.setFinishTime(maxTime);
        assertEquals(maxTime, transfersTask.getFinishTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試完成時間早於開始時間的異常情況")
    void testTransfersTask_finishBeforeStart() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime finish = start.minusHours(1); // 完成時間早於開始時間
        
        transfersTask.setStartTime(start);
        transfersTask.setFinishTime(finish);
        
        assertEquals(start, transfersTask.getStartTime());
        assertEquals(finish, transfersTask.getFinishTime());
        
        // 計算持續時間會是負數
        long duration = java.time.Duration.between(start, finish).getSeconds();
        assertTrue(duration < 0);
    }

    @Test
    @DisplayName("邊界測試 - toString 方法在屬性為 null 時")
    void testToString_withNullProperties() {
        TransfersTask nullTask = new TransfersTask();
        
        String taskString = nullTask.toString();
        assertNotNull(taskString);
        assertTrue(taskString.contains("id=null"));
        assertTrue(taskString.contains("transferTaskId=null"));
        assertTrue(taskString.contains("md5=null"));
        assertTrue(taskString.contains("gridFsId=null"));
        assertTrue(taskString.contains("fileSize=null"));
        assertTrue(taskString.contains("startTime=null"));
        assertTrue(taskString.contains("finishTime=null"));
        assertTrue(taskString.contains("message=null"));
        assertTrue(taskString.contains("status=null"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證實體註解存在")
    void testEntity_annotations() {
        // 驗證類級別註解
        assertTrue(TransfersTask.class.isAnnotationPresent(org.springframework.data.relational.core.mapping.Table.class));
        
        // 驗證字段註解
        try {
            assertTrue(TransfersTask.class.getDeclaredField("id").isAnnotationPresent(org.springframework.data.annotation.Id.class));
            assertTrue(TransfersTask.class.getDeclaredField("transferTaskId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(TransfersTask.class.getDeclaredField("md5").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(TransfersTask.class.getDeclaredField("gridFsId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(TransfersTask.class.getDeclaredField("fileSize").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(TransfersTask.class.getDeclaredField("startTime").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(TransfersTask.class.getDeclaredField("finishTime").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
        } catch (NoSuchFieldException e) {
            fail("預期的字段不存在: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試任務 ID 格式")
    void testTransfersTask_taskIdFormats() {
        String[] taskIds = {
            "task-12345",
            "TASK_ABCDE_67890",
            "transfer-2024-03-15-001",
            "upload_file_task_123456789",
            "下載任務_中文ID_001"
        };
        
        for (String taskId : taskIds) {
            transfersTask.setTransferTaskId(taskId);
            assertEquals(taskId, transfersTask.getTransferTaskId());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試 MD5 格式驗證")
    void testTransfersTask_md5Format() {
        // 測試標準 MD5 格式
        String standardMd5 = "5d41402abc4b2a76b9719d911017c592";
        transfersTask.setMd5(standardMd5);
        assertEquals(standardMd5, transfersTask.getMd5());
        
        // 測試大寫 MD5
        String upperMd5 = "5D41402ABC4B2A76B9719D911017C592";
        transfersTask.setMd5(upperMd5);
        assertEquals(upperMd5, transfersTask.getMd5());
        
        // 測試混合大小寫 MD5
        String mixedMd5 = "5d41402abC4B2a76B9719d911017c592";
        transfersTask.setMd5(mixedMd5);
        assertEquals(mixedMd5, transfersTask.getMd5());
    }

    @Test
    @DisplayName("邊界測試 - 測試 equals 方法的反射性、對稱性和傳遞性")
    void testEquals_properties() {
        TransfersTask task1 = new TransfersTask();
        task1.setId(1L);
        
        TransfersTask task2 = new TransfersTask();
        task2.setId(1L);
        
        TransfersTask task3 = new TransfersTask();
        task3.setId(1L);
        
        // 反射性：x.equals(x) 應該返回 true
        assertTrue(task1.equals(task1));
        
        // 對稱性：x.equals(y) 和 y.equals(x) 應該返回相同結果
        assertTrue(task1.equals(task2));
        assertTrue(task2.equals(task1));
        
        // 傳遞性：如果 x.equals(y) 和 y.equals(z)，則 x.equals(z) 應該為 true  
        assertTrue(task1.equals(task2));
        assertTrue(task2.equals(task3));
        assertTrue(task1.equals(task3));
    }

    @Test
    @DisplayName("邊界測試 - 測試 Unicode 字符在屬性中")
    void testTransfersTask_unicodeCharacters() {
        transfersTask.setTransferTaskId("傳輸任務_ID_001");
        transfersTask.setMessage("檔案上傳完成：檔案「重要文檔.pdf」已成功傳輸到伺服器");
        
        assertEquals("傳輸任務_ID_001", transfersTask.getTransferTaskId());
        assertEquals("檔案上傳完成：檔案「重要文檔.pdf」已成功傳輸到伺服器", transfersTask.getMessage());
        
        // 驗證 toString 方法能正確處理 Unicode
        String taskString = transfersTask.toString();
        assertTrue(taskString.contains("傳輸任務_ID_001"));
        assertTrue(taskString.contains("重要文檔.pdf"));
    }

    @Test
    @DisplayName("邊界測試 - 測試長時間運行的任務")
    void testTransfersTask_longRunningTask() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime finish = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        
        transfersTask.setStartTime(start);
        transfersTask.setFinishTime(finish);
        
        // 計算運行時間（約365天）
        long durationDays = java.time.Duration.between(start, finish).toDays();
        assertTrue(durationDays >= 364 && durationDays <= 365);
    }
}