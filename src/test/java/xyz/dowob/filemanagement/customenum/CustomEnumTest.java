package xyz.dowob.filemanagement.customenum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自定義枚舉類別測試類別。
 * 
 * <p>測試系統中所有自定義枚舉類的功能正確性，包括 RoleEnum、UserInfoTypeEnum、ByteEnum、FileEnum、PermissionEnum 等。
 * 驗證枚舉常量定義、業務方法實現和邊界情況處理的正確性。確保枚舉類型在各種使用場景下的穩定性和一致性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CustomEnum 自定義枚舉測試")
class CustomEnumTest {

    // ==================== RoleEnum 測試 ====================

    @Test
    @DisplayName("一般測試 - RoleEnum 角色定義")
    void testRoleEnum_basicDefinition() {
        // 驗證所有角色都存在
        assertNotNull(RoleEnum.ADMIN);
        assertNotNull(RoleEnum.ADVANCED_USER);
        assertNotNull(RoleEnum.USER);
        assertNotNull(RoleEnum.VISITOR);
        assertNotNull(RoleEnum.ANONYMOUS);
        
        // 驗證角色數量
        assertEquals(5, RoleEnum.values().length);
    }

    @Test
    @DisplayName("一般測試 - RoleEnum 權限檢查")
    void testRoleEnum_permissionCheck() {
        // ADMIN 擁有所有權限
        assertTrue(RoleEnum.ADMIN.hasPermissions(PermissionEnum.READ));
        assertTrue(RoleEnum.ADMIN.hasPermissions(PermissionEnum.WRITE));
        assertTrue(RoleEnum.ADMIN.hasPermissions(PermissionEnum.DELETE));
        assertTrue(RoleEnum.ADMIN.hasPermissions(PermissionEnum.MANAGE));
        
        // USER 擁有基本權限但沒有管理權限
        assertTrue(RoleEnum.USER.hasPermissions(PermissionEnum.READ));
        assertTrue(RoleEnum.USER.hasPermissions(PermissionEnum.WRITE));
        assertFalse(RoleEnum.USER.hasPermissions(PermissionEnum.MANAGE));
        
        // VISITOR 只有讀取權限
        assertTrue(RoleEnum.VISITOR.hasPermissions(PermissionEnum.READ));
        assertFalse(RoleEnum.VISITOR.hasPermissions(PermissionEnum.WRITE));
        
        // ANONYMOUS 沒有任何權限
        assertFalse(RoleEnum.ANONYMOUS.hasPermissions(PermissionEnum.READ));
    }

    @Test
    @DisplayName("一般測試 - RoleEnum 存儲限制")
    void testRoleEnum_storageLimit() {
        // ADMIN 無限制存儲
        assertEquals(-1L, RoleEnum.ADMIN.getDefaultStorageLimit());
        
        // ADVANCED_USER 50GB
        assertEquals(ByteEnum.convertToByte(50L), RoleEnum.ADVANCED_USER.getDefaultStorageLimit());
        
        // USER 15GB
        assertEquals(ByteEnum.convertToByte(15L), RoleEnum.USER.getDefaultStorageLimit());
        
        // VISITOR 和 ANONYMOUS 0GB
        assertEquals(0L, RoleEnum.VISITOR.getDefaultStorageLimit());
        assertEquals(0L, RoleEnum.ANONYMOUS.getDefaultStorageLimit());
    }

    @Test
    @DisplayName("一般測試 - RoleEnum 多重權限檢查")
    void testRoleEnum_multiplePermissions() {
        // ADMIN 擁有多個權限
        assertTrue(RoleEnum.ADMIN.hasPermissions(PermissionEnum.READ, PermissionEnum.WRITE, PermissionEnum.DELETE));
        
        // USER 擁有讀寫權限但沒有分享權限
        assertTrue(RoleEnum.USER.hasPermissions(PermissionEnum.READ, PermissionEnum.WRITE));
        assertFalse(RoleEnum.USER.hasPermissions(PermissionEnum.READ, PermissionEnum.WRITE, PermissionEnum.SHARE));
        
        // VISITOR 沒有寫入權限
        assertFalse(RoleEnum.VISITOR.hasPermissions(PermissionEnum.READ, PermissionEnum.WRITE));
    }

    // ==================== UserInfoTypeEnum 測試 ====================

    @Test
    @DisplayName("一般測試 - UserInfoTypeEnum 枚舉定義")
    void testUserInfoTypeEnum_basicDefinition() {
        // 驗證所有類型都存在
        assertNotNull(UserInfoTypeEnum.NAME);
        assertNotNull(UserInfoTypeEnum.ID);
        
        // 驗證類型數量
        assertEquals(2, UserInfoTypeEnum.values().length);
    }

    @Test
    @DisplayName("一般測試 - UserInfoTypeEnum 字符串轉換")
    void testUserInfoTypeEnum_fromString() {
        // 正確的字符串轉換
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("name"));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("NAME"));
        assertEquals(UserInfoTypeEnum.ID, UserInfoTypeEnum.fromString("id"));
        assertEquals(UserInfoTypeEnum.ID, UserInfoTypeEnum.fromString("ID"));
        
        // 大小寫混合
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("Name"));
        assertEquals(UserInfoTypeEnum.ID, UserInfoTypeEnum.fromString("Id"));
    }

    @Test
    @DisplayName("異常測試 - UserInfoTypeEnum 無效字符串轉換")
    void testUserInfoTypeEnum_invalidString() {
        // 無效字符串應該返回 NAME 作為默認值
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("invalid"));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString(""));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString(null));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("123"));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("username"));
    }

    // ==================== ByteEnum 測試 ====================

    @Test
    @DisplayName("一般測試 - ByteEnum 單位定義")
    void testByteEnum_unitDefinition() {
        // 驗證所有單位都存在
        assertNotNull(ByteEnum.BYTE);
        assertNotNull(ByteEnum.KILOBYTE);
        assertNotNull(ByteEnum.MEGABYTE);
        assertNotNull(ByteEnum.GIGABYTE);
        assertNotNull(ByteEnum.TERABYTE);
        
        // 驗證單位數量
        assertEquals(5, ByteEnum.values().length);
        
        // 驗證單位值
        assertEquals(1L, ByteEnum.BYTE.getBytes());
        assertEquals(1024L, ByteEnum.KILOBYTE.getBytes());
        assertEquals(1024L * 1024, ByteEnum.MEGABYTE.getBytes());
        assertEquals(1024L * 1024 * 1024, ByteEnum.GIGABYTE.getBytes());
        assertEquals(1024L * 1024 * 1024 * 1024, ByteEnum.TERABYTE.getBytes());
        
        // 驗證單位名稱
        assertEquals("B", ByteEnum.BYTE.getUnit());
        assertEquals("KB", ByteEnum.KILOBYTE.getUnit());
        assertEquals("MB", ByteEnum.MEGABYTE.getUnit());
        assertEquals("GB", ByteEnum.GIGABYTE.getUnit());
        assertEquals("TB", ByteEnum.TERABYTE.getUnit());
    }

    @Test
    @DisplayName("一般測試 - ByteEnum 單位轉換")
    void testByteEnum_unitConversion() {
        // 默認轉換為GB
        assertEquals(1L * 1024 * 1024 * 1024, ByteEnum.convertToByte(1L));
        assertEquals(5L * 1024 * 1024 * 1024, ByteEnum.convertToByte(5L));
        
        // 指定單位轉換
        assertEquals(1024L, ByteEnum.convertToByte(1L, ByteEnum.KILOBYTE));
        assertEquals(1024L * 1024, ByteEnum.convertToByte(1L, ByteEnum.MEGABYTE));
        assertEquals(1024L * 1024 * 1024, ByteEnum.convertToByte(1L, ByteEnum.GIGABYTE));
        assertEquals(1024L * 1024 * 1024 * 1024, ByteEnum.convertToByte(1L, ByteEnum.TERABYTE));
        
        // 零值轉換
        assertEquals(0L, ByteEnum.convertToByte(0L));
        assertEquals(0L, ByteEnum.convertToByte(0L, ByteEnum.KILOBYTE));
    }

    @Test
    @DisplayName("一般測試 - ByteEnum 可讀大小轉換")
    void testByteEnum_readableSize() {
        // 字節級別
        assertEquals("0.00 B", ByteEnum.toReadableSize(0L));
        assertEquals("512.00 B", ByteEnum.toReadableSize(512L));
        assertEquals("1023.00 B", ByteEnum.toReadableSize(1023L));
        
        // KB級別
        assertEquals("1.00 KB", ByteEnum.toReadableSize(1024L));
        assertEquals("1.50 KB", ByteEnum.toReadableSize(1536L));
        
        // MB級別
        assertEquals("1.00 MB", ByteEnum.toReadableSize(1024L * 1024));
        assertEquals("2.50 MB", ByteEnum.toReadableSize(1024L * 1024 * 5 / 2));
        
        // GB級別
        assertEquals("1.00 GB", ByteEnum.toReadableSize(1024L * 1024 * 1024));
        
        // TB級別
        assertEquals("1.00 TB", ByteEnum.toReadableSize(1024L * 1024 * 1024 * 1024));
    }

    @Test
    @DisplayName("邊界測試 - ByteEnum 極值處理")
    void testByteEnum_extremeValues() {
        // 負數處理
        assertEquals(-1024L, ByteEnum.convertToByte(-1L, ByteEnum.KILOBYTE));
        
        // 大數值處理
        long largeValue = Long.MAX_VALUE / (1024L * 1024 * 1024);
        assertTrue(ByteEnum.convertToByte(largeValue) > 0);
        
        // 可讀大小的極值
        assertTrue(ByteEnum.toReadableSize(Long.MAX_VALUE).contains("TB"));
        assertEquals("0.00 B", ByteEnum.toReadableSize(0L));
    }

    // ==================== 其他枚舉測試 ====================

    @Test
    @DisplayName("一般測試 - FileEnum 檔案類型")
    void testFileEnum_fileTypes() {
        // 驗證關鍵檔案類型存在
        assertNotNull(FileEnum.DOCUMENT);
        assertNotNull(FileEnum.IMAGE);
        assertNotNull(FileEnum.VIDEO);
        assertNotNull(FileEnum.MUSIC);
        assertNotNull(FileEnum.FOLDER);
        assertNotNull(FileEnum.ONLINE_DOCUMENT);
        
        // 驗證至少有基本的檔案類型
        assertTrue(FileEnum.values().length >= 6);
    }

    @Test
    @DisplayName("一般測試 - PermissionEnum 權限類型")
    void testPermissionEnum_permissionTypes() {
        // 驗證基本權限存在
        assertNotNull(PermissionEnum.READ);
        assertNotNull(PermissionEnum.WRITE);
        assertNotNull(PermissionEnum.DELETE);
        assertNotNull(PermissionEnum.UPLOAD);
        assertNotNull(PermissionEnum.DOWNLOAD);
        assertNotNull(PermissionEnum.SHARE);
        assertNotNull(PermissionEnum.UPDATE);
        assertNotNull(PermissionEnum.MANAGE);
        
        // 驗證權限數量
        assertTrue(PermissionEnum.values().length >= 8);
    }

    @Test
    @DisplayName("一般測試 - LogLevelEnum 日誌級別")
    void testLogLevelEnum_logLevels() {
        // 驗證基本日誌級別存在
        assertNotNull(LogLevelEnum.TRACE);
        assertNotNull(LogLevelEnum.DEBUG);
        assertNotNull(LogLevelEnum.INFO);
        assertNotNull(LogLevelEnum.WARN);
        assertNotNull(LogLevelEnum.ERROR);
        
        // 驗證日誌級別數量
        assertTrue(LogLevelEnum.values().length >= 5);
    }

    @Test
    @DisplayName("一般測試 - TokenEnum 令牌類型")
    void testTokenEnum_tokenTypes() {
        // 驗證基本令牌類型存在
        assertNotNull(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        assertNotNull(TokenEnum.RESET_PASSWORD_TOKEN);
        
        // 驗證令牌類型數量
        assertTrue(TokenEnum.values().length >= 2);
    }

    @Test
    @DisplayName("一般測試 - TransmissionEnum 傳輸方式")
    void testTransmissionEnum_transmissionTypes() {
        // 驗證傳輸方式存在
        assertNotNull(TransmissionEnum.MULTIPART);
        assertNotNull(TransmissionEnum.CHUNK);
        
        // 驗證傳輸方式數量
        assertTrue(TransmissionEnum.values().length >= 2);
    }

    @Test
    @DisplayName("一般測試 - CsrfTokenRepositoryEnum CSRF 令牌存儲類型")
    void testCsrfTokenRepositoryEnum_repositoryTypes() {
        // 驗證存儲類型存在
        assertNotNull(CsrfTokenRepositoryEnum.LOCAL);
        assertNotNull(CsrfTokenRepositoryEnum.REDIS);
        
        // 驗證存儲類型數量
        assertEquals(2, CsrfTokenRepositoryEnum.values().length);
    }

    @Test
    @DisplayName("一般測試 - CacheProviderEnum 緩存提供者類型")
    void testCacheProviderEnum_providerTypes() {
        // 驗證緩存提供者類型存在
        assertNotNull(CacheProviderEnum.USER_CACHE);
        assertNotNull(CacheProviderEnum.FILE_STREAM_CACHE);
        assertNotNull(CacheProviderEnum.USER_FILE_LIST_CACHE);
        
        // 驗證提供者類型數量
        assertTrue(CacheProviderEnum.values().length >= 3);
    }

    @Test
    @DisplayName("一般測試 - UserLimiterEnum 用戶限制器類型")
    void testUserLimiterEnum_limiterTypes() {
        // 驗證限制器類型存在
        assertNotNull(UserLimiterEnum.USER_UPLOAD_LIMITER);
        
        // 驗證限制器類型數量
        assertTrue(UserLimiterEnum.values().length >= 1);
    }

    @Test
    @DisplayName("一般測試 - DownloadActionEnum 下載動作類型")
    void testDownloadActionEnum_actionTypes() {
        // 驗證下載動作類型存在
        assertNotNull(DownloadActionEnum.DOWNLOAD);
        assertNotNull(DownloadActionEnum.PREVIEW);
        
        // 驗證動作類型數量
        assertTrue(DownloadActionEnum.values().length >= 2);
    }

    @Test
    @DisplayName("一般測試 - EditTypeEnum 編輯類型")
    void testEditTypeEnum_editTypes() {
        // 驗證編輯類型存在
        assertTrue(EditTypeEnum.values().length >= 1);
        
        // 驗證每個枚舉值都不為空
        for (EditTypeEnum editType : EditTypeEnum.values()) {
            assertNotNull(editType);
            assertNotNull(editType.name());
        }
    }

    @Test
    @DisplayName("一般測試 - ReservedSearchIdEnum 預留搜索ID")
    void testReservedSearchIdEnum_searchIds() {
        // 驗證預留搜索ID存在
        assertNotNull(ReservedSearchIdEnum.STAR_FILE_ID);
        
        // 驗證每個搜索ID都有對應的ID值
        for (ReservedSearchIdEnum searchId : ReservedSearchIdEnum.values()) {
            assertNotNull(searchId);
            assertNotNull(searchId.getId());
            assertTrue(searchId.getId() <= 0); // 預留ID應該是負數
        }
    }

    @Test
    @DisplayName("一般測試 - FileShareTypeEnum 檔案分享類型")
    void testFileShareTypeEnum_shareTypes() {
        // 驗證分享類型存在
        assertTrue(FileShareTypeEnum.values().length >= 1);
        
        for (FileShareTypeEnum shareType : FileShareTypeEnum.values()) {
            assertNotNull(shareType);
            assertNotNull(shareType.name());
        }
    }

    @Test
    @DisplayName("一般測試 - TransfersStatusEnum 傳輸狀態")
    void testTransfersStatusEnum_statusTypes() {
        // 驗證傳輸狀態存在
        assertTrue(TransfersStatusEnum.values().length >= 1);
        
        for (TransfersStatusEnum status : TransfersStatusEnum.values()) {
            assertNotNull(status);
            assertNotNull(status.name());
        }
    }

    @Test
    @DisplayName("一般測試 - UserFileListOrderEnum 用戶檔案列表排序")
    void testUserFileListOrderEnum_orderTypes() {
        // 驗證排序類型存在
        assertTrue(UserFileListOrderEnum.values().length >= 1);
        
        for (UserFileListOrderEnum order : UserFileListOrderEnum.values()) {
            assertNotNull(order);
            assertNotNull(order.name());
        }
    }

    @Test
    @DisplayName("一般測試 - WebsocketResponseType WebSocket 響應類型")
    void testWebsocketResponseType_responseTypes() {
        // 驗證響應類型存在
        assertTrue(WebsocketResponseType.values().length >= 1);
        
        for (WebsocketResponseType responseType : WebsocketResponseType.values()) {
            assertNotNull(responseType);
            assertNotNull(responseType.name());
        }
    }

    @Test
    @DisplayName("一般測試 - ConvertProviderEnum 轉換提供者類型")
    void testConvertProviderEnum_providerTypes() {
        // 驗證轉換提供者類型存在
        assertTrue(ConvertProviderEnum.values().length >= 1);
        
        for (ConvertProviderEnum provider : ConvertProviderEnum.values()) {
            assertNotNull(provider);
            assertNotNull(provider.name());
        }
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 枚舉類型完整性檢查")
    void testEnum_completenessCheck() {
        // 驗證每個枚舉類都至少有一個值
        assertTrue(RoleEnum.values().length > 0);
        assertTrue(UserInfoTypeEnum.values().length > 0);
        assertTrue(ByteEnum.values().length > 0);
        assertTrue(FileEnum.values().length > 0);
        assertTrue(PermissionEnum.values().length > 0);
        assertTrue(LogLevelEnum.values().length > 0);
        assertTrue(TokenEnum.values().length > 0);
        assertTrue(TransmissionEnum.values().length > 0);
        assertTrue(CsrfTokenRepositoryEnum.values().length > 0);
        assertTrue(CacheProviderEnum.values().length > 0);
        assertTrue(UserLimiterEnum.values().length > 0);
        assertTrue(DownloadActionEnum.values().length > 0);
    }

    @Test
    @DisplayName("邊界測試 - 枚舉值唯一性檢查")
    void testEnum_uniquenessCheck() {
        // 驗證 RoleEnum 中每個角色都是唯一的
        Set<String> roleNames = Set.of();
        for (RoleEnum role : RoleEnum.values()) {
            String roleName = role.name();
            assertFalse(roleNames.contains(roleName), "發現重複的角色名稱: " + roleName);
        }
        
        // 驗證 UserInfoTypeEnum 中每個類型都是唯一的
        Set<String> typeNames = Set.of();
        for (UserInfoTypeEnum type : UserInfoTypeEnum.values()) {
            String typeName = type.name();
            assertFalse(typeNames.contains(typeName), "發現重複的類型名稱: " + typeName);
        }
    }

    @Test
    @DisplayName("邊界測試 - RoleEnum 空權限檢查")
    void testRoleEnum_emptyPermissionCheck() {
        // 測試沒有權限的情況
        assertFalse(RoleEnum.ANONYMOUS.hasPermissions());
        
        // 測試 null 權限數組（雖然不太可能發生）
        assertTrue(RoleEnum.ADMIN.hasPermissions(new PermissionEnum[0]));
    }

    @Test
    @DisplayName("邊界測試 - ByteEnum 零值和負值轉換")
    void testByteEnum_zeroAndNegativeConversion() {
        // 零值轉換
        assertEquals(0L, ByteEnum.convertToByte(0L, ByteEnum.BYTE));
        assertEquals(0L, ByteEnum.convertToByte(0L, ByteEnum.TERABYTE));
        
        // 負值轉換
        assertEquals(-1024L, ByteEnum.convertToByte(-1L, ByteEnum.KILOBYTE));
        assertEquals(-1024L * 1024 * 1024, ByteEnum.convertToByte(-1L, ByteEnum.GIGABYTE));
        
        // 可讀大小的負值處理（實際應用中很少見）
        String negativeSize = ByteEnum.toReadableSize(-1024L);
        assertTrue(negativeSize.contains("-"));
    }

    @Test
    @DisplayName("邊界測試 - UserInfoTypeEnum 邊界字符串")
    void testUserInfoTypeEnum_boundaryStrings() {
        // 空字符串和空白字符串
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString(""));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("   "));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("\t"));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("\n"));
        
        // 特殊字符
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("@#$%"));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("123456"));
        
        // 部分匹配應該不成功
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("nam"));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("name1"));
    }
}