package xyz.dowob.filemanagement.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用戶檔案元數據實體的測試實現。
 * 
 * <p>基於用戶視角的檔案管理系統，支援階層式資料夾結構和檔案分享機制。
 * 關聯伺服器檔案實體並維護用戶特定的檔案名稱、星標狀態、刪除標記和分享類型，
 * 提供個人化的檔案組織和存取控制功能。
 * 
 * <p>測試涵蓋檔案類型枚舉、分享類型設定、星標與刪除標記的布林邏輯處理。
 * 驗證資料夾階層關係、時間戳記錄的準確性以及物件相等性比較規則。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("UserFileMetadata 用戶檔案元數據實體測試")
class UserFileMetadataTest {

    private UserFileMetadata userFile;
    private LocalDateTime testUploadTime;
    private LocalDateTime testAccessTime;

    @BeforeEach
    void setUp() {
        userFile = new UserFileMetadata();
        testUploadTime = LocalDateTime.now();
        testAccessTime = testUploadTime.plusMinutes(30);
        
        // 設置基本屬性
        userFile.setId(1L);
        userFile.setUserId(100L);
        userFile.setServerFileId(200L);
        userFile.setFilename("測試檔案.pdf");
        userFile.setParentFolderId(10L);
        userFile.setIsStar(true);
        userFile.setFileType(FileEnum.DOCUMENT);
        userFile.setShareType(FileShareTypeEnum.PUBLIC);
        userFile.setUploadTime(testUploadTime);
        userFile.setLastAccessTime(testAccessTime);
        userFile.setIsDeleted(false);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 創建用戶檔案元數據並設置基本屬性")
    void testCreateUserFileMetadata_withBasicProperties() {
        assertNotNull(userFile);
        assertEquals(1L, userFile.getId());
        assertEquals(100L, userFile.getUserId());
        assertEquals(200L, userFile.getServerFileId());
        assertEquals("測試檔案.pdf", userFile.getFilename());
        assertEquals(10L, userFile.getParentFolderId());
        assertTrue(userFile.getIsStar());
        assertEquals(FileEnum.DOCUMENT, userFile.getFileType());
        assertEquals(FileShareTypeEnum.PUBLIC, userFile.getShareType());
        assertEquals(testUploadTime, userFile.getUploadTime());
        assertEquals(testAccessTime, userFile.getLastAccessTime());
        assertFalse(userFile.getIsDeleted());
    }

    @Test
    @DisplayName("一般測試 - 測試默認值設置")
    void testUserFileMetadata_defaultValues() {
        UserFileMetadata newFile = new UserFileMetadata();
        
        assertFalse(newFile.getIsStar());
        assertEquals(FileEnum.OTHER, newFile.getFileType());
        assertEquals(FileShareTypeEnum.DEFAULT, newFile.getShareType());
        assertFalse(newFile.getIsDeleted());
    }

    @Test
    @DisplayName("一般測試 - 測試不同檔案類型枚舉")
    void testUserFileMetadata_differentFileTypes() {
        for (FileEnum fileType : FileEnum.values()) {
            userFile.setFileType(fileType);
            assertEquals(fileType, userFile.getFileType());
        }
    }

    @Test
    @DisplayName("一般測試 - 測試不同分享類型枚舉")
    void testUserFileMetadata_differentShareTypes() {
        for (FileShareTypeEnum shareType : FileShareTypeEnum.values()) {
            userFile.setShareType(shareType);
            assertEquals(shareType, userFile.getShareType());
        }
    }

    @Test
    @DisplayName("一般測試 - 測試 toString 方法")
    void testToString() {
        String fileString = userFile.toString();
        
        assertNotNull(fileString);
        assertTrue(fileString.contains("id=1"));
        assertTrue(fileString.contains("user=100"));
        assertTrue(fileString.contains("serverFile=200"));
        assertTrue(fileString.contains("parentFolder=10"));
        assertTrue(fileString.contains("filename=測試檔案.pdf"));
        assertTrue(fileString.contains("isDeleted=false"));
        assertTrue(fileString.contains("isStar=true"));
        assertTrue(fileString.contains("fileType=DOCUMENT"));
    }

    @Test
    @DisplayName("一般測試 - 測試時間屬性設置")
    void testTimeProperties() {
        LocalDateTime uploadTime = LocalDateTime.of(2024, 3, 15, 10, 0, 0);
        LocalDateTime accessTime = LocalDateTime.of(2024, 3, 15, 11, 30, 0);
        
        userFile.setUploadTime(uploadTime);
        userFile.setLastAccessTime(accessTime);
        
        assertEquals(uploadTime, userFile.getUploadTime());
        assertEquals(accessTime, userFile.getLastAccessTime());
    }

    @Test
    @DisplayName("一般測試 - 測試檔案星標標記功能")
    void testStarMarkFunctionality() {
        // 測試設置為星標
        userFile.setIsStar(true);
        assertTrue(userFile.getIsStar());
        
        // 測試取消星標
        userFile.setIsStar(false);
        assertFalse(userFile.getIsStar());
    }

    @Test
    @DisplayName("一般測試 - 測試檔案刪除標記功能")
    void testDeleteMarkFunctionality() {
        // 測試標記為刪除
        userFile.setIsDeleted(true);
        assertTrue(userFile.getIsDeleted());
        
        // 測試取消刪除標記
        userFile.setIsDeleted(false);
        assertFalse(userFile.getIsDeleted());
    }

    @Test
    @DisplayName("一般測試 - 測試父資料夾ID設置（包括根目錄）")
    void testParentFolderIdHandling() {
        // 測試設置父資料夾ID
        userFile.setParentFolderId(999L);
        assertEquals(999L, userFile.getParentFolderId());
        
        // 測試根目錄（null）
        userFile.setParentFolderId(null);
        assertNull(userFile.getParentFolderId());
    }

    @Test
    @DisplayName("一般測試 - 測試與服務器檔案的關聯")
    void testServerFileAssociation() {
        Long serverFileId = 12345L;
        userFile.setServerFileId(serverFileId);
        
        assertEquals(serverFileId, userFile.getServerFileId());
    }

    @Test
    @DisplayName("一般測試 - 測試資料夾類型檔案")
    void testFolderTypeFile() {
        userFile.setFileType(FileEnum.FOLDER);
        userFile.setServerFileId(null); // 資料夾通常沒有服務器檔案ID
        
        assertEquals(FileEnum.FOLDER, userFile.getFileType());
        assertNull(userFile.getServerFileId());
    }

    @Test
    @DisplayName("一般測試 - 測試線上檔案類型")
    void testOnlineDocumentType() {
        userFile.setFileType(FileEnum.ONLINE_DOCUMENT);
        userFile.setFilename("線上文檔.txt");
        
        assertEquals(FileEnum.ONLINE_DOCUMENT, userFile.getFileType());
        assertEquals("線上文檔.txt", userFile.getFilename());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - equals 方法傳入 null")
    void testEquals_withNull() {
        assertFalse(userFile.equals(null));
    }

    @Test
    @DisplayName("異常測試 - equals 方法傳入不同類型對象")
    void testEquals_withDifferentType() {
        assertFalse(userFile.equals("not a user file"));
        assertFalse(userFile.equals(123));
        assertFalse(userFile.equals(new Object()));
    }

    @Test
    @DisplayName("異常測試 - hashCode 方法在 id 為 null 時")
    void testHashCode_withNullId() {
        UserFileMetadata fileWithNullId = new UserFileMetadata();
        fileWithNullId.setId(null);
        
        assertThrows(NullPointerException.class, fileWithNullId::hashCode);
    }

    @Test
    @DisplayName("異常測試 - equals 方法在 id 為 null 時")
    void testEquals_withNullId() {
        UserFileMetadata file1 = new UserFileMetadata();
        file1.setId(null);
        UserFileMetadata file2 = new UserFileMetadata();
        file2.setId(null);
        
        assertThrows(NullPointerException.class, () -> file1.equals(file2));
    }

    @Test
    @DisplayName("異常測試 - 設置 null 時間屬性")
    void testSetNullTimeProperties() {
        assertDoesNotThrow(() -> {
            userFile.setUploadTime(null);
            userFile.setLastAccessTime(null);
        });
        
        assertNull(userFile.getUploadTime());
        assertNull(userFile.getLastAccessTime());
    }

    @Test
    @DisplayName("異常測試 - 設置 null 字符串屬性")
    void testSetNullStringProperties() {
        assertDoesNotThrow(() -> {
            userFile.setFilename(null);
        });
        
        assertNull(userFile.getFilename());
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同對象")
    void testEquals_sameObject() {
        assertTrue(userFile.equals(userFile));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試相同 id 的不同對象")
    void testEquals_sameId() {
        UserFileMetadata anotherFile = new UserFileMetadata();
        anotherFile.setId(1L);
        anotherFile.setFilename("不同檔案名.txt"); // 不同的檔案名
        
        assertTrue(userFile.equals(anotherFile));
    }

    @Test
    @DisplayName("邊界測試 - equals 方法測試不同 id 的對象")
    void testEquals_differentId() {
        UserFileMetadata anotherFile = new UserFileMetadata();
        anotherFile.setId(2L);
        
        assertFalse(userFile.equals(anotherFile));
    }

    @Test
    @DisplayName("邊界測試 - hashCode 方法一致性")
    void testHashCode_consistency() {
        int hash1 = userFile.hashCode();
        int hash2 = userFile.hashCode();
        
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("邊界測試 - 相同 id 的對象 hashCode 相同")
    void testHashCode_sameId() {
        UserFileMetadata anotherFile = new UserFileMetadata();
        anotherFile.setId(1L);
        
        assertEquals(userFile.hashCode(), anotherFile.hashCode());
    }

    @Test
    @DisplayName("邊界測試 - 測試極大 ID 值")
    void testUserFileMetadata_maxIdValues() {
        userFile.setId(Long.MAX_VALUE);
        userFile.setUserId(Long.MAX_VALUE);
        userFile.setServerFileId(Long.MAX_VALUE);
        userFile.setParentFolderId(Long.MAX_VALUE);
        
        assertEquals(Long.MAX_VALUE, userFile.getId());
        assertEquals(Long.MAX_VALUE, userFile.getUserId());
        assertEquals(Long.MAX_VALUE, userFile.getServerFileId());
        assertEquals(Long.MAX_VALUE, userFile.getParentFolderId());
    }

    @Test
    @DisplayName("邊界測試 - 測試零 ID 值")
    void testUserFileMetadata_zeroIdValues() {
        userFile.setId(0L);
        userFile.setUserId(0L);
        userFile.setServerFileId(0L);
        userFile.setParentFolderId(0L);
        
        assertEquals(0L, userFile.getId());
        assertEquals(0L, userFile.getUserId());
        assertEquals(0L, userFile.getServerFileId());
        assertEquals(0L, userFile.getParentFolderId());
    }

    @Test
    @DisplayName("邊界測試 - 測試負數 ID 值")
    void testUserFileMetadata_negativeIdValues() {
        userFile.setId(-1L);
        userFile.setUserId(-100L);
        userFile.setServerFileId(-200L);
        userFile.setParentFolderId(-10L);
        
        assertEquals(-1L, userFile.getId());
        assertEquals(-100L, userFile.getUserId());
        assertEquals(-200L, userFile.getServerFileId());
        assertEquals(-10L, userFile.getParentFolderId());
    }

    @Test
    @DisplayName("邊界測試 - 測試空字符串檔案名")
    void testUserFileMetadata_emptyFilename() {
        userFile.setFilename("");
        
        assertEquals("", userFile.getFilename());
    }

    @Test
    @DisplayName("邊界測試 - 測試超長檔案名")
    void testUserFileMetadata_veryLongFilename() {
        String longFilename = "很長的檔案名".repeat(100) + ".txt";
        
        userFile.setFilename(longFilename);
        
        assertEquals(longFilename, userFile.getFilename());
    }

    @Test
    @DisplayName("邊界測試 - 測試特殊字符在檔案名中")
    void testUserFileMetadata_specialCharactersInFilename() {
        String specialFilename = "檔案@#$%^&*()<>&\"'`\n\t.pdf";
        
        userFile.setFilename(specialFilename);
        
        assertEquals(specialFilename, userFile.getFilename());
    }

    @Test
    @DisplayName("邊界測試 - 測試過去時間")
    void testUserFileMetadata_pastTime() {
        LocalDateTime pastTime = LocalDateTime.now().minusYears(5);
        
        userFile.setUploadTime(pastTime);
        userFile.setLastAccessTime(pastTime);
        
        assertEquals(pastTime, userFile.getUploadTime());
        assertEquals(pastTime, userFile.getLastAccessTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試極遠未來時間")
    void testUserFileMetadata_farFutureTime() {
        LocalDateTime futureTime = LocalDateTime.now().plusYears(100);
        
        userFile.setUploadTime(futureTime);
        userFile.setLastAccessTime(futureTime);
        
        assertEquals(futureTime, userFile.getUploadTime());
        assertEquals(futureTime, userFile.getLastAccessTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試時間邊界值")
    void testUserFileMetadata_timeBoundaryValues() {
        // 測試最小時間
        LocalDateTime minTime = LocalDateTime.MIN;
        userFile.setUploadTime(minTime);
        assertEquals(minTime, userFile.getUploadTime());
        
        // 測試最大時間
        LocalDateTime maxTime = LocalDateTime.MAX;
        userFile.setLastAccessTime(maxTime);
        assertEquals(maxTime, userFile.getLastAccessTime());
    }

    @Test
    @DisplayName("邊界測試 - 測試 Boolean 屬性的所有組合")
    void testUserFileMetadata_booleanCombinations() {
        // 測試所有可能的布爾值組合
        Boolean[] booleanValues = {true, false, null};
        
        for (Boolean starValue : booleanValues) {
            for (Boolean deleteValue : booleanValues) {
                userFile.setIsStar(starValue);
                userFile.setIsDeleted(deleteValue);
                
                assertEquals(starValue, userFile.getIsStar());
                assertEquals(deleteValue, userFile.getIsDeleted());
            }
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試檔案名格式驗證")
    void testUserFileMetadata_filenameFormats() {
        String[] filenames = {
            "document.pdf",
            "image.png",
            "video.mp4",
            "archive.zip",
            "no_extension",
            ".hidden_file",
            "file with spaces.txt",
            "檔案名稱.中文擴展",
            "very.long.extension.name.document.pdf",
            "123456789.numbers"
        };
        
        for (String filename : filenames) {
            userFile.setFilename(filename);
            assertEquals(filename, userFile.getFilename());
        }
    }

    @Test
    @DisplayName("邊界測試 - toString 方法在屬性為 null 時")
    void testToString_withNullProperties() {
        UserFileMetadata nullFile = new UserFileMetadata();
        
        String fileString = nullFile.toString();
        assertNotNull(fileString);
        assertTrue(fileString.contains("id=null"));
        assertTrue(fileString.contains("user=null"));
        assertTrue(fileString.contains("serverFile=null"));
        assertTrue(fileString.contains("parentFolder=null"));
        assertTrue(fileString.contains("filename=null"));
        assertTrue(fileString.contains("uploadTime=null"));
        assertTrue(fileString.contains("lastAccessTime=null"));
        assertTrue(fileString.contains("isDeleted=false"));
        assertTrue(fileString.contains("isStar=false"));
        assertTrue(fileString.contains("fileType=OTHER"));
    }

    @Test
    @DisplayName("邊界測試 - 驗證實體註解存在")
    void testEntity_annotations() {
        // 驗證類級別註解
        assertTrue(UserFileMetadata.class.isAnnotationPresent(org.springframework.data.relational.core.mapping.Table.class));
        
        // 驗證字段註解
        try {
            assertTrue(UserFileMetadata.class.getDeclaredField("id").isAnnotationPresent(org.springframework.data.annotation.Id.class));
            assertTrue(UserFileMetadata.class.getDeclaredField("userId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserFileMetadata.class.getDeclaredField("serverFileId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserFileMetadata.class.getDeclaredField("parentFolderId").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserFileMetadata.class.getDeclaredField("isStar").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserFileMetadata.class.getDeclaredField("fileType").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserFileMetadata.class.getDeclaredField("shareType").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserFileMetadata.class.getDeclaredField("uploadTime").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserFileMetadata.class.getDeclaredField("lastAccessTime").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            assertTrue(UserFileMetadata.class.getDeclaredField("isDeleted").isAnnotationPresent(org.springframework.data.relational.core.mapping.Column.class));
            
            // 驗證 JSON 格式註解
            assertTrue(UserFileMetadata.class.getDeclaredField("uploadTime").isAnnotationPresent(com.fasterxml.jackson.annotation.JsonFormat.class));
            assertTrue(UserFileMetadata.class.getDeclaredField("lastAccessTime").isAnnotationPresent(com.fasterxml.jackson.annotation.JsonFormat.class));
        } catch (NoSuchFieldException e) {
            fail("預期的字段不存在: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試 equals 方法的反射性、對稱性和傳遞性")
    void testEquals_properties() {
        UserFileMetadata file1 = new UserFileMetadata();
        file1.setId(1L);
        
        UserFileMetadata file2 = new UserFileMetadata();
        file2.setId(1L);
        
        UserFileMetadata file3 = new UserFileMetadata();
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
    @DisplayName("邊界測試 - 測試複雜的父子資料夾關係")
    void testUserFileMetadata_complexFolderHierarchy() {
        // 創建多層資料夾結構
        UserFileMetadata rootFolder = new UserFileMetadata();
        rootFolder.setId(1L);
        rootFolder.setFileType(FileEnum.FOLDER);
        rootFolder.setParentFolderId(null); // 根資料夾
        
        UserFileMetadata subFolder = new UserFileMetadata();
        subFolder.setId(2L);
        subFolder.setFileType(FileEnum.FOLDER);
        subFolder.setParentFolderId(1L); // 子資料夾
        
        UserFileMetadata file = new UserFileMetadata();
        file.setId(3L);
        file.setFileType(FileEnum.DOCUMENT);
        file.setParentFolderId(2L); // 檔案在子資料夾中
        
        assertNull(rootFolder.getParentFolderId());
        assertEquals(1L, subFolder.getParentFolderId());
        assertEquals(2L, file.getParentFolderId());
    }

    @Test
    @DisplayName("邊界測試 - 測試 Unicode 字符在屬性中")
    void testUserFileMetadata_unicodeCharacters() {
        userFile.setFilename("重要文檔📄.pdf");
        
        assertEquals("重要文檔📄.pdf", userFile.getFilename());
        
        // 驗證 toString 方法能正確處理 Unicode
        String fileString = userFile.toString();
        assertTrue(fileString.contains("重要文檔📄.pdf"));
    }

    @Test
    @DisplayName("邊界測試 - 測試訪問時間晚於上傳時間的正常情況")
    void testUserFileMetadata_accessAfterUpload() {
        LocalDateTime upload = LocalDateTime.now();
        LocalDateTime access = upload.plusDays(1);
        
        userFile.setUploadTime(upload);
        userFile.setLastAccessTime(access);
        
        assertEquals(upload, userFile.getUploadTime());
        assertEquals(access, userFile.getLastAccessTime());
        
        // 驗證訪問時間確實晚於上傳時間
        assertTrue(access.isAfter(upload));
    }

    @Test
    @DisplayName("邊界測試 - 測試訪問時間早於上傳時間的異常情況")
    void testUserFileMetadata_accessBeforeUpload() {
        LocalDateTime upload = LocalDateTime.now();
        LocalDateTime access = upload.minusHours(1); // 訪問時間早於上傳時間
        
        userFile.setUploadTime(upload);
        userFile.setLastAccessTime(access);
        
        assertEquals(upload, userFile.getUploadTime());
        assertEquals(access, userFile.getLastAccessTime());
        
        // 驗證訪問時間確實早於上傳時間（業務邏輯異常但技術上允許）
        assertTrue(access.isBefore(upload));
    }

    @Test
    @DisplayName("邊界測試 - 測試星標已刪除檔案的組合狀態")
    void testUserFileMetadata_starredDeletedFile() {
        // 測試已刪除但仍被標記為星標的檔案
        userFile.setIsStar(true);
        userFile.setIsDeleted(true);
        
        assertTrue(userFile.getIsStar());
        assertTrue(userFile.getIsDeleted());
        
        // 這種組合在技術上是允許的，可能用於回收站中的星標檔案
    }

    @Test
    @DisplayName("邊界測試 - 測試所有枚舉類型組合")
    void testUserFileMetadata_allEnumCombinations() {
        // 測試檔案類型和分享類型的所有組合
        for (FileEnum fileType : FileEnum.values()) {
            for (FileShareTypeEnum shareType : FileShareTypeEnum.values()) {
                userFile.setFileType(fileType);
                userFile.setShareType(shareType);
                
                assertEquals(fileType, userFile.getFileType());
                assertEquals(shareType, userFile.getShareType());
            }
        }
    }
}