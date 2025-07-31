package xyz.dowob.filemanagement.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.dowob.filemanagement.customenum.FileEnum;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 伺服器檔案元數據實體的測試實現。
 * 
 * <p>基於 GridFS 分散式檔案儲存系統的元數據管理，支援多用戶共享檔案和訪問追蹤。
 * 維護檔案大小、類型、MIME 類型、MD5 雜湊值以及擁有者集合，確保檔案完整性和權限控制。
 * 
 * <p>測試涵蓋實體屬性的完整性驗證、擁有者集合操作、檔案類型枚舉處理和時間戳記錄。
 * 驗證字串格式化、物件相等性比較規則以及各種邊界條件的處理機制。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("ServerFileMetadata 伺服器檔案元數據實體測試")
class ServerFileMetadataTest {

    private ServerFileMetadata serverFile;
    private LocalDateTime testTime;
    private Set<Long> testOwners;

    @BeforeEach
    void setUp() {
        serverFile = new ServerFileMetadata();
        testTime = LocalDateTime.now();
        
        // 設置基本屬性
        serverFile.setId(1L);
        serverFile.setFileSize(1024000L);
        serverFile.setFileType(FileEnum.DOCUMENT);
        serverFile.setMimeType("text/plain");
        serverFile.setUploadTime(testTime);
        serverFile.setLastAccessTime(testTime.plusMinutes(30));
        serverFile.setGridFsId("grid-fs-123-abc");
        serverFile.setMd5("d41d8cd98f00b204e9800998ecf8427e");
        
        // 設置擁有者集合
        testOwners = new HashSet<>();
        testOwners.add(100L);
        testOwners.add(200L);
        serverFile.setOwners(testOwners);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 創建服務器檔案元數據並設置基本屬性")
    void testCreateServerFileMetadata_withBasicProperties() {
        assertNotNull(serverFile);
        assertEquals(1L, serverFile.getId());
        assertEquals(1024000L, serverFile.getFileSize());
        assertEquals(FileEnum.DOCUMENT, serverFile.getFileType());
        assertEquals("text/plain", serverFile.getMimeType());
        assertEquals(testTime, serverFile.getUploadTime());
        assertEquals(testTime.plusMinutes(30), serverFile.getLastAccessTime());
        assertEquals("grid-fs-123-abc", serverFile.getGridFsId());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", serverFile.getMd5());
        assertEquals(testOwners, serverFile.getOwners());
    }

    @Test
    @DisplayName("一般測試 - 測試默認擁有者集合")
    void testServerFileMetadata_defaultOwners() {
        ServerFileMetadata newFile = new ServerFileMetadata();
        
        assertNotNull(newFile.getOwners());
        assertTrue(newFile.getOwners().isEmpty());
        assertInstanceOf(HashSet.class, newFile.getOwners());
    }

    @Test
    @DisplayName("一般測試 - 測試不同檔案類型枚舉")
    void testServerFileMetadata_differentFileTypes() {
        for (FileEnum fileType : FileEnum.values()) {
            serverFile.setFileType(fileType);
            assertEquals(fileType, serverFile.getFileType());
        }
    }

    @Test
    @DisplayName("一般測試 - 測試 toString 方法")
    void testToString() {
        String fileString = serverFile.toString();
        
        assertNotNull(fileString);
        assertTrue(fileString.contains("id=1"));
        assertTrue(fileString.contains("fileSize=1024000"));
        assertTrue(fileString.contains("fileType=DOCUMENT"));
        assertTrue(fileString.contains("mimeType=text/plain"));
        assertTrue(fileString.contains("gridFsId=grid-fs-123-abc"));
        assertTrue(fileString.contains("md5=d41d8cd98f00b204e9800998ecf8427e"));
        assertTrue(fileString.contains("owners=[100, 200]"));
    }

    @Test
    @DisplayName("一般測試 - 測試擁有者集合操作")
    void testOwnersSetOperations() {
        Set<Long> owners = serverFile.getOwners();
        
        // 添加新擁有者
        owners.add(300L);
        assertTrue(owners.contains(300L));
        assertEquals(3, owners.size());
        
        // 移除擁有者
        owners.remove(100L);
        assertFalse(owners.contains(100L));
        assertEquals(2, owners.size());
        
        // 驗證集合特性（不允許重複）
        owners.add(200L);
        assertEquals(2, owners.size());
    }

    @Test
    @DisplayName("一般測試 - 測試時間屬性設置")
    void testTimeProperties() {
        LocalDateTime uploadTime = LocalDateTime.of(2024, 3, 15, 10, 0, 0);
        LocalDateTime accessTime = LocalDateTime.of(2024, 3, 15, 11, 30, 0);
        
        serverFile.setUploadTime(uploadTime);
        serverFile.setLastAccessTime(accessTime);
        
        assertEquals(uploadTime, serverFile.getUploadTime());
        assertEquals(accessTime, serverFile.getLastAccessTime());
    }

    @Test
    @DisplayName("一般測試 - 測試 GridFS ID 設置")
    void testGridFsIdHandling() {
        String gridFsId = "507f1f77bcf86cd799439011";
        
        serverFile.setGridFsId(gridFsId);
        
        assertEquals(gridFsId, serverFile.getGridFsId());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - equals 方法傳入 null")
    void testEquals_withNull() {
        assertFalse(serverFile.equals(null));
    }

    @Test
    @DisplayName("異常測試 - equals 方法傳入不同類型對象")
    void testEquals_withDifferentType() {
        assertFalse(serverFile.equals("not a server file"));
        assertFalse(serverFile.equals(123));
        assertFalse(serverFile.equals(new Object()));
    }

    @Test
    @DisplayName("異常測試 - hashCode 方法在 id 為 null 時")
    void testHashCode_withNullId() {
        ServerFileMetadata fileWithNullId = new ServerFileMetadata();
        fileWithNullId.setId(null);
        
        assertThrows(NullPointerException.class, fileWithNullId::hashCode);
    }

    @Test
    @DisplayName("異常測試 - equals 方法在 id 為 null 時")
    void testEquals_withNullId() {
        ServerFileMetadata file1 = new ServerFileMetadata();
        file1.setId(null);
        ServerFileMetadata file2 = new ServerFileMetadata();
        file2.setId(null);
        
        assertThrows(NullPointerException.class, () -> file1.equals(file2));
    }

    @Test
    @DisplayName("異常測試 - 設置 null 擁有者集合")
    void testSetNullOwners() {
        assertDoesNotThrow(() -> {
            serverFile.setOwners(null);
        });
        
        assertNull(serverFile.getOwners());
    }

    @Test
    @DisplayName("異常測試 - 設置 null 時間屬性")
    void testSetNullTimeProperties() {
        assertDoesNotThrow(() -> {
            serverFile.setUploadTime(null);
            serverFile.setLastAccessTime(null);
        });
        
        assertNull(serverFile.getUploadTime());
        assertNull(serverFile.getLastAccessTime());
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同對象")
    void testEquals_sameObject() {
        assertTrue(serverFile.equals(serverFile));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同 id 的不同對象")
    void testEquals_sameId() {
        ServerFileMetadata anotherFile = new ServerFileMetadata();
        anotherFile.setId(1L);
        anotherFile.setFileSize(999L); // 不同的檔案大小
        
        assertTrue(serverFile.equals(anotherFile));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試不同 id 的對象")
    void testEquals_differentId() {
        ServerFileMetadata anotherFile = new ServerFileMetadata();
        anotherFile.setId(2L);
        
        assertFalse(serverFile.equals(anotherFile));
    }

    @Test
    @DisplayName("邊界測試 - hashCode 方法一致性")
    void testHashCode_consistency() {
        int hash1 = serverFile.hashCode();
        int hash2 = serverFile.hashCode();
        
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("邊界測試 - 相同 id 的對象 hashCode 相同")
    void testHashCode_sameId() {
        ServerFileMetadata anotherFile = new ServerFileMetadata();
        anotherFile.setId(1L);
        
        assertEquals(serverFile.hashCode(), anotherFile.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試極大檔案大小")
    void testServerFileMetadata_maxFileSize() {
        serverFile.setFileSize(Long.MAX_VALUE);
        
        assertEquals(Long.MAX_VALUE, serverFile.getFileSize());
    }

    @Test
    @DisplayName("邊界測試 - 測試零檔案大小")
    void testServerFileMetadata_zeroFileSize() {
        serverFile.setFileSize(0L);
        
        assertEquals(0L, serverFile.getFileSize());
    }

    @Test
    @DisplayName("邊界測試 - 測試負數檔案大小")
    void testServerFileMetadata_negativeFileSize() {
        serverFile.setFileSize(-1L);
        
        assertEquals(-1L, serverFile.getFileSize());
    }

    @Test
    @DisplayName("邊界測試 - 測試空字符串屬性")
    void testServerFileMetadata_emptyStringProperties() {
        serverFile.setMimeType("");
        serverFile.setGridFsId("");
        serverFile.setMd5("");
        
        assertEquals("", serverFile.getMimeType());
        assertEquals("", serverFile.getGridFsId());
        assertEquals("", serverFile.getMd5());
    }

    @Test
    @DisplayName("邊界測試 - 測試 null 字符串屬性")
    void testServerFileMetadata_nullStringProperties() {
        serverFile.setMimeType(null);
        serverFile.setGridFsId(null);
        serverFile.setMd5(null);
        
        assertNull(serverFile.getMimeType());
        assertNull(serverFile.getGridFsId());
        assertNull(serverFile.getMd5());
    }

    @Test
    @DisplayName("邊界測試 - 測試超長字符串屬性")
    void testServerFileMetadata_veryLongStringProperties() {
        String longString = "a".repeat(1000);
        
        serverFile.setMimeType(longString);
        serverFile.setGridFsId(longString);
        serverFile.setMd5(longString);
        
        assertEquals(longString, serverFile.getMimeType());
        assertEquals(longString, serverFile.getGridFsId());
        assertEquals(longString, serverFile.getMd5());
    }

    @Test
    @DisplayName("邊界測試 - 測試特殊字符在屬性中")
    void testServerFileMetadata_specialCharactersInProperties() {
        String specialMime = "application/json; charset=utf-8";
        String specialGridFs = "grid-fs@#$%^&*()";
        String specialMd5 = "md5<>&\"'`\n\t";
        
        serverFile.setMimeType(specialMime);
        serverFile.setGridFsId(specialGridFs);
        serverFile.setMd5(specialMd5);
        
        assertEquals(specialMime, serverFile.getMimeType());
        assertEquals(specialGridFs, serverFile.getGridFsId());
        assertEquals(specialMd5, serverFile.getMd5());
    }

    @Test
    @DisplayName("邊界測試 - 測試過去時間")
    void testServerFileMetadata_pastTime() {
        LocalDateTime pastTime = LocalDateTime.now().minusYears(5);
        
        serverFile.setUploadTime(pastTime);
        serverFile.setLastAccessTime(pastTime);
        
        assertEquals(pastTime, serverFile.getUploadTime());
        assertEquals(pastTime, serverFile.getLastAccessTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試極遠未來時間")
    void testServerFileMetadata_farFutureTime() {
        LocalDateTime futureTime = LocalDateTime.now().plusYears(100);
        
        serverFile.setUploadTime(futureTime);
        serverFile.setLastAccessTime(futureTime);
        
        assertEquals(futureTime, serverFile.getUploadTime());
        assertEquals(futureTime, serverFile.getLastAccessTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試時間邊界值")
    void testServerFileMetadata_timeBoundaryValues() {
        // 測試最小時間
        LocalDateTime minTime = LocalDateTime.MIN;
        serverFile.setUploadTime(minTime);
        assertEquals(minTime, serverFile.getUploadTime());
        
        // 測試最大時間
        LocalDateTime maxTime = LocalDateTime.MAX;
        serverFile.setLastAccessTime(maxTime);
        assertEquals(maxTime, serverFile.getLastAccessTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試大量擁有者")
    void testServerFileMetadata_manyOwners() {
        Set<Long> manyOwners = new HashSet<>();
        for (long i = 1; i <= 1000; i++) {
            manyOwners.add(i);
        }
        
        serverFile.setOwners(manyOwners);
        
        assertEquals(1000, serverFile.getOwners().size());
        assertTrue(serverFile.getOwners().contains(1L));
        assertTrue(serverFile.getOwners().contains(1000L));
    }

    @Test
    @DisplayName("邊界測試 - 測試擁有者集合邊界 ID 值")
    void testServerFileMetadata_ownersBoundaryIds() {
        Set<Long> boundaryOwners = new HashSet<>();
        boundaryOwners.add(0L);
        boundaryOwners.add(-1L);
        boundaryOwners.add(Long.MAX_VALUE);
        boundaryOwners.add(Long.MIN_VALUE);
        
        serverFile.setOwners(boundaryOwners);
        
        assertTrue(serverFile.getOwners().contains(0L));
        assertTrue(serverFile.getOwners().contains(-1L));
        assertTrue(serverFile.getOwners().contains(Long.MAX_VALUE));
        assertTrue(serverFile.getOwners().contains(Long.MIN_VALUE));
    }

    @Test
    @DisplayName("邊界測試 - toString 方法在屬性為 null 時")
    void testToString_withNullProperties() {
        ServerFileMetadata nullFile = new ServerFileMetadata();
        
        String fileString = nullFile.toString();
        assertNotNull(fileString);
        assertTrue(fileString.contains("id=null"));
        assertTrue(fileString.contains("fileSize=null"));
        assertTrue(fileString.contains("fileType=null"));
        assertTrue(fileString.contains("mimeType=null"));
        assertTrue(fileString.contains("owners=[]"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證實體註解存在")
    void testEntity_annotations() {
        // 驗證類級別註解
        assertTrue(ServerFileMetadata.class.isAnnotationPresent(org.springframework.data.relational.core.mapping.Table.class));
        
        // 驗證字段註解
        try {
            assertTrue(ServerFileMetadata.class.getDeclaredField("id").isAnnotationPresent(org.springframework.data.annotation.Id.class));
            assertTrue(ServerFileMetadata.class.getDeclaredField("fileSize").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(ServerFileMetadata.class.getDeclaredField("fileType").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(ServerFileMetadata.class.getDeclaredField("mimeType").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(ServerFileMetadata.class.getDeclaredField("uploadTime").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(ServerFileMetadata.class.getDeclaredField("lastAccessTime").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(ServerFileMetadata.class.getDeclaredField("gridFsId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            
            // 驗證 JSON 格式註解
            assertTrue(ServerFileMetadata.class.getDeclaredField("uploadTime").isAnnotationPresent(com.fasterxml.jackson.annotation.JsonFormat.class));
            assertTrue(ServerFileMetadata.class.getDeclaredField("lastAccessTime").isAnnotationPresent(com.fasterxml.jackson.annotation.JsonFormat.class));
        } catch (NoSuchFieldException e) {
            fail("預期的字段不存在: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試 MD5 格式驗證")
    void testServerFileMetadata_md5Format() {
        // 測試標準 MD5 格式
        String standardMd5 = "5d41402abc4b2a76b9719d911017c592";
        serverFile.setMd5(standardMd5);
        assertEquals(standardMd5, serverFile.getMd5());
        
        // 測試大寫 MD5
        String upperMd5 = "5D41402ABC4B2A76B9719D911017C592";
        serverFile.setMd5(upperMd5);
        assertEquals(upperMd5, serverFile.getMd5());
        
        // 測試混合大小寫 MD5
        String mixedMd5 = "5d41402abC4B2a76B9719d911017c592";
        serverFile.setMd5(mixedMd5);
        assertEquals(mixedMd5, serverFile.getMd5());
    }

    @Test
    @DisplayName("邊界測試 - 測試 MIME 類型格式")
    void testServerFileMetadata_mimeTypeFormats() {
        String[] mimeTypes = {
            "text/plain",
            "application/json",
            "image/jpeg",
            "video/mp4",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "multipart/form-data; boundary=something"
        };
        
        for (String mimeType : mimeTypes) {
            serverFile.setMimeType(mimeType);
            assertEquals(mimeType, serverFile.getMimeType());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試 equals 方法的反射性、對稱性和傳遞性")
    void testEquals_properties() {
        ServerFileMetadata file1 = new ServerFileMetadata();
        file1.setId(1L);
        
        ServerFileMetadata file2 = new ServerFileMetadata();
        file2.setId(1L);
        
        ServerFileMetadata file3 = new ServerFileMetadata();
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
    @DisplayName("邊界測試 - 測試 Unicode 字符在屬性中")
    void testServerFileMetadata_unicodeCharacters() {
        serverFile.setMimeType("檔案類型/純文字");
        serverFile.setGridFsId("網格存儲-123-檔案");
        
        assertEquals("檔案類型/純文字", serverFile.getMimeType());
        assertEquals("網格存儲-123-檔案", serverFile.getGridFsId());
        
        // 驗證 toString 方法能正確處理 Unicode
        String fileString = serverFile.toString();
        assertTrue(fileString.contains("檔案類型/純文字"));
        assertTrue(fileString.contains("網格存儲-123-檔案"));
    }
}