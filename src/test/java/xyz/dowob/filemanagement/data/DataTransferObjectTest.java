package xyz.dowob.filemanagement.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.EditTypeEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.data.event.FileEditedMessage;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.UploadTaskBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dao.OnlineHistoryCountAndOldestDAO;
import xyz.dowob.filemanagement.data.file.dao.ServerFileMetaCountDAO;
import xyz.dowob.filemanagement.data.file.dao.UserFileMetaWithDataDAO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.data.file.po.CustomPatchPO;
import xyz.dowob.filemanagement.data.file.po.FluxDataPO;
import xyz.dowob.filemanagement.data.file.po.QuillContentPO;
import xyz.dowob.filemanagement.data.file.po.ShareUserEditPO;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.data.response.WebSocketResponse;
import xyz.dowob.filemanagement.data.user.dto.*;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 資料傳輸物件測試類，驗證系統中所有 DTO、BO、DAO、PO 等資料物件的功能性和完整性。
 * 
 * <p>測試涵蓋所有資料傳輸物件的基本功能，包括 Response DTOs、User DTOs、File DTOs、
 * Business Objects、Data Access Objects、Persistent Objects 以及事件物件的建立、
 * 序列化、反序列化、驗證規則和建構模式等完整功能。
 * 
 * <p>特別關注 JSON 序列化反序列化的正確性、Bean Validation 約束驗證的有效性，
 * 以及響應式程式設計中資料流處理的準確性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.data.response.ApiResponseDTO
 * @see xyz.dowob.filemanagement.data.file.dto.FileEditDTO
 * @see xyz.dowob.filemanagement.data.file.bo.UserFileDataBO
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DataTransferObject 數據傳輸對象測試")
class DataTransferObjectTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==================== Response DTOs 測試 ====================

    /**
     * 測試 ApiResponseDTO 基本功能建立和存取操作。
     * 
     * 驗證 API 響應資料傳輸物件的基本建構和屬性存取功能，確保 Builder 模式
     * 正確運作且所有欄位值能夠正確設定和取得。
     * 
     * 前置條件：
     * - ApiResponseDTO 類別正常載入
     * - Builder 模式正常運作
     * 
     * 測試步驟：
     * - 使用 Builder 模式建立 ApiResponseDTO 實例
     * - 設定時間戳記、狀態碼、路徑、訊息和資料等屬性
     * - 驗證所有設定的屬性值是否正確
     * 
     * 預期結果：
     * - 物件建立成功且不為 null
     * - 所有屬性值與設定值完全一致
     */
    @Test
    @DisplayName("一般測試 - ApiResponseDTO 基本功能")
    void testApiResponseDTO_basicFunctionality() {
        LocalDateTime now = LocalDateTime.now();
        String testData = "test data";
        
        // 使用 Builder 創建
        ApiResponseDTO<String> response = ApiResponseDTO.<String>builder()
                .timestamp(now)
                .status(200)
                .path("/api/test")
                .message("成功")
                .data(testData)
                .build();
        
        assertNotNull(response);
        assertEquals(now, response.getTimestamp());
        assertEquals(200, response.getStatus());
        assertEquals("/api/test", response.getPath());
        assertEquals("成功", response.getMessage());
        assertEquals(testData, response.getData());
    }

    /**
     * 測試 ApiResponseDTO JSON 序列化和反序列化功能。
     * 
     * 驗證 API 響應物件在 JSON 格式轉換過程中的正確性，包括日期時間格式化、
     * 特殊字元處理和資料完整性保持。
     * 
     * 前置條件：
     * - ObjectMapper 正確配置 JavaTimeModule
     * - 序列化和反序列化配置正常
     * 
     * 測試步驟：
     * - 建立包含特定時間戳記的 ApiResponseDTO
     * - 執行 JSON 序列化操作
     * - 驗證序列化結果包含正確格式的時間和資料
     * - 執行反序列化操作並驗證資料完整性
     * 
     * 預期結果：
     * - JSON 序列化成功且格式正確
     * - 反序列化物件保持原始資料完整性
     */
    @Test
    @DisplayName("一般測試 - ApiResponseDTO JSON 序列化")
    void testApiResponseDTO_jsonSerialization() throws Exception {
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0, 0, 123456000);
        ApiResponseDTO<String> response = ApiResponseDTO.<String>builder()
                .timestamp(now)
                .status(200)
                .path("/api/test")
                .message("成功")
                .data("test")
                .build();
        
        String json = objectMapper.writeValueAsString(response);
        assertNotNull(json);
        assertTrue(json.contains("2024-01-01T12:00:00.123456"));
        assertTrue(json.contains("\"status\":200"));
        assertTrue(json.contains("\"message\":\"成功\""));
        
        // 反序列化
        ApiResponseDTO<?> deserialized = objectMapper.readValue(json, ApiResponseDTO.class);
        assertNotNull(deserialized);
        assertEquals(200, deserialized.getStatus());
        assertEquals("成功", deserialized.getMessage());
    }

    /**
     * 測試 PagedResponseDTO 分頁響應功能。
     * 
     * 驗證分頁響應資料傳輸物件的建構和分頁資訊處理功能，確保分頁相關屬性
     * 能夠正確設定和取得。
     * 
     * 前置條件：
     * - PagedResponseDTO 類別正常載入
     * - Builder 模式正常運作
     * 
     * 測試步驟：
     * - 準備測試資料清單
     * - 使用 Builder 建立 PagedResponseDTO 並設定分頁資訊
     * - 驗證資料內容、總頁數、當前頁面、頁面大小和總元素數
     * 
     * 預期結果：
     * - 分頁響應物件建立成功
     * - 所有分頁相關屬性值正確
     */
    @Test
    @DisplayName("一般測試 - PagedResponseDTO 分頁響應")
    void testPagedResponseDTO_pagination() {
        List<String> testData = List.of("item1", "item2", "item3");
        
        PagedResponseDTO<String> pagedResponse = PagedResponseDTO.<String>builder()
                .data(testData)
                .totalPages(5)
                .currentPage(1)
                .pageSize(10)
                .totalElements(50L)
                .build();
        
        assertNotNull(pagedResponse);
        assertEquals(testData, pagedResponse.getData());
        assertEquals(5, pagedResponse.getTotalPages());
        assertEquals(1, pagedResponse.getCurrentPage());
        assertEquals(10, pagedResponse.getPageSize());
        assertEquals(50L, pagedResponse.getTotalElements());
    }

    /**
     * 測試 WebSocketResponse WebSocket 響應基本功能。
     * 
     * 驗證 WebSocket 響應物件的建構和基本屬性存取功能，確保即時通訊
     * 響應資料能夠正確處理。
     * 
     * 前置條件：
     * - WebSocketResponse 類別正常載入
     * - 建構函式正常運作
     * 
     * 測試步驟：
     * - 使用建構函式建立 WebSocketResponse 實例
     * - 設定時間戳記、類型、訊息和資料
     * - 驗證所有屬性值的正確性
     * 
     * 預期結果：
     * - WebSocket 響應物件建立成功
     * - 所有屬性值與設定值一致
     */
    @Test
    @DisplayName("一般測試 - WebSocketResponse WebSocket 響應")
    void testWebSocketResponse_basicFunctionality() {
        LocalDateTime now = LocalDateTime.now();
        WebSocketResponse<String> response = new WebSocketResponse<>(now, "FILE_UPLOAD_PROGRESS", "上傳進度更新", "50%");
        
        assertNotNull(response);
        assertEquals("FILE_UPLOAD_PROGRESS", response.getType());
        assertEquals("上傳進度更新", response.getMessage());
        assertEquals("50%", response.getData());
    }

    // ==================== User DTOs 測試 ====================

    /**
     * 測試 AuthRequestDTO 登入請求基本功能。
     * 
     * 驗證認證請求資料傳輸物件的建構和屬性存取功能，確保使用者登入
     * 資料能夠正確處理。
     * 
     * 前置條件：
     * - AuthRequestDTO 類別正常載入
     * - 有參和無參建構函式正常運作
     * 
     * 測試步驟：
     * - 使用有參建構函式建立認證請求物件
     * - 驗證使用者名稱和密碼屬性值
     * - 測試無參建構函式建立空物件
     * - 驗證空物件的屬性為 null
     * 
     * 預期結果：
     * - 認證請求物件建立成功
     * - 屬性值正確設定和取得
     */
    @Test
    @DisplayName("一般測試 - AuthRequestDTO 登錄請求")
    void testAuthRequestDTO_basicFunctionality() {
        AuthRequestDTO authRequest = new AuthRequestDTO("testuser", "password123");
        
        assertNotNull(authRequest);
        assertEquals("testuser", authRequest.getUsername());
        assertEquals("password123", authRequest.getPassword());
        
        // 測試無參構造函數
        AuthRequestDTO emptyRequest = new AuthRequestDTO();
        assertNotNull(emptyRequest);
        assertNull(emptyRequest.getUsername());
        assertNull(emptyRequest.getPassword());
    }

    /**
     * 測試 AuthRequestDTO 驗證約束功能。
     * 
     * 驗證認證請求物件的 Bean Validation 約束規則，確保無效資料能夠
     * 被正確識別和拒絕。
     * 
     * 前置條件：
     * - Bean Validation 框架正確配置
     * - AuthRequestDTO 定義驗證約束
     * 
     * 測試步驟：
     * - 建立包含空使用者名稱的請求物件並驗證
     * - 建立包含空密碼的請求物件並驗證
     * - 建立包含 null 值的請求物件並驗證
     * - 建立有效請求物件並驗證
     * 
     * 預期結果：
     * - 無效資料產生驗證錯誤
     * - 有效資料通過驗證
     */
    @Test
    @DisplayName("異常測試 - AuthRequestDTO 驗證約束")
    void testAuthRequestDTO_validation() {
        // 空用戶名
        AuthRequestDTO invalidRequest1 = new AuthRequestDTO("", "password");
        Set<ConstraintViolation<AuthRequestDTO>> violations1 = validator.validate(invalidRequest1);
        assertFalse(violations1.isEmpty());
        
        // 空密碼
        AuthRequestDTO invalidRequest2 = new AuthRequestDTO("username", "");
        Set<ConstraintViolation<AuthRequestDTO>> violations2 = validator.validate(invalidRequest2);
        assertFalse(violations2.isEmpty());
        
        // null 值
        AuthRequestDTO invalidRequest3 = new AuthRequestDTO(null, null);
        Set<ConstraintViolation<AuthRequestDTO>> violations3 = validator.validate(invalidRequest3);
        assertFalse(violations3.isEmpty());
        assertTrue(violations3.size() >= 2);
        
        // 有效請求
        AuthRequestDTO validRequest = new AuthRequestDTO("username", "password");
        Set<ConstraintViolation<AuthRequestDTO>> violations4 = validator.validate(validRequest);
        assertTrue(violations4.isEmpty());
    }

    /**
     * 測試 AuthResponseDTO 登入響應基本功能。
     * 
     * 驗證認證響應資料傳輸物件的建構和 JWT 權杖處理功能。
     * 
     * 前置條件：
     * - AuthResponseDTO 類別正常載入
     * - Setter 方法正常運作
     * 
     * 測試步驟：
     * - 建立 AuthResponseDTO 實例
     * - 設定 JWT 權杖值
     * - 驗證權杖值的正確性
     * 
     * 預期結果：
     * - 響應物件建立成功
     * - JWT 權杖值正確設定和取得
     */
    @Test
    @DisplayName("一般測試 - AuthResponseDTO 登錄響應")
    void testAuthResponseDTO_basicFunctionality() {
        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setJwtToken("jwt.token.here");
        
        assertNotNull(authResponse);
        assertEquals("jwt.token.here", authResponse.getJwtToken());
    }

    /**
     * 測試 RegisterDTO 註冊請求基本功能。
     * 
     * 驗證使用者註冊資料傳輸物件的建構和屬性設定功能。
     * 
     * 前置條件：
     * - RegisterDTO 類別正常載入
     * - Setter 方法正常運作
     * 
     * 測試步驟：
     * - 建立 RegisterDTO 實例
     * - 設定使用者名稱、電子郵件和密碼
     * - 驗證所有屬性值的正確性
     * 
     * 預期結果：
     * - 註冊請求物件建立成功
     * - 所有屬性值正確設定和取得
     */
    @Test
    @DisplayName("一般測試 - RegisterDTO 註冊請求")
    void testRegisterDTO_basicFunctionality() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setEmail("newuser@example.com");
        registerDTO.setPassword("password123");
        
        assertNotNull(registerDTO);
        assertEquals("newuser", registerDTO.getUsername());
        assertEquals("newuser@example.com", registerDTO.getEmail());
        assertEquals("password123", registerDTO.getPassword());
    }

    /**
     * 測試 ResetPasswordDTO 重置密碼請求基本功能。
     * 
     * 驗證密碼重置資料傳輸物件的建構和屬性設定功能。
     * 
     * 前置條件：
     * - ResetPasswordDTO 類別正常載入
     * - Setter 方法正常運作
     * 
     * 測試步驟：
     * - 建立 ResetPasswordDTO 實例
     * - 設定電子郵件、驗證碼、新密碼和確認密碼
     * - 驗證所有屬性值的正確性
     * 
     * 預期結果：
     * - 重置密碼請求物件建立成功
     * - 所有屬性值正確設定和取得
     */
    @Test
    @DisplayName("一般測試 - ResetPasswordDTO 重置密碼請求")
    void testResetPasswordDTO_basicFunctionality() {
        ResetPasswordDTO resetDTO = new ResetPasswordDTO();
        resetDTO.setEmail("user@example.com");
        resetDTO.setVerificationCode("123456");
        resetDTO.setNewPassword("newpassword123");
        resetDTO.setConfirmPassword("newpassword123");
        
        assertNotNull(resetDTO);
        assertEquals("user@example.com", resetDTO.getEmail());
        assertEquals("123456", resetDTO.getVerificationCode());
        assertEquals("newpassword123", resetDTO.getNewPassword());
        assertEquals("newpassword123", resetDTO.getConfirmPassword());
    }

    /**
     * 測試 UserEmailDTO 使用者郵箱基本功能。
     * 
     * 驗證使用者電子郵件資料傳輸物件的建構和屬性設定功能。
     * 
     * 前置條件：
     * - UserEmailDTO 類別正常載入
     * - Setter 方法正常運作
     * 
     * 測試步驟：
     * - 建立 UserEmailDTO 實例
     * - 設定電子郵件地址
     * - 驗證電子郵件屬性值的正確性
     * 
     * 預期結果：
     * - 使用者郵箱物件建立成功
     * - 電子郵件屬性值正確設定和取得
     */
    @Test
    @DisplayName("一般測試 - UserEmailDTO 用戶郵箱")
    void testUserEmailDTO_basicFunctionality() {
        UserEmailDTO emailDTO = new UserEmailDTO();
        emailDTO.setEmail("user@example.com");
        
        assertNotNull(emailDTO);
        assertEquals("user@example.com", emailDTO.getEmail());
    }

    // ==================== File DTOs 測試 ====================

    /**
     * 測試 FileEditDTO 檔案編輯基本功能。
     * 
     * 驗證檔案編輯資料傳輸物件的建構和編輯內容處理功能，包括 Delta 格式
     * 的編輯器內容封裝。
     * 
     * 前置條件：
     * - FileEditDTO 和 EditorContentDTO 類別正常載入
     * - Builder 模式正常運作
     * 
     * 測試步驟：
     * - 建立 DeltaDTO 並設定編輯內容
     * - 使用 Builder 建立 EditorContentDTO
     * - 建立 FileEditDTO 並設定檔案資訊和內容
     * - 驗證所有屬性值的正確性
     * 
     * 預期結果：
     * - 檔案編輯物件建立成功
     * - 編輯內容正確封裝和取得
     */
    @Test
    @DisplayName("一般測試 - FileEditDTO 檔案編輯")
    void testFileEditDTO_basicFunctionality() {
        EditorContentDTO.DeltaDTO deltaDTO = new EditorContentDTO.DeltaDTO();
        deltaDTO.setInsert("test content");
        
        EditorContentDTO content = EditorContentDTO.builder()
                .delta(List.of(deltaDTO))
                .build();
        
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("1");
        fileEditDTO.setFilename("test.txt");
        fileEditDTO.setContent(content);
        
        assertNotNull(fileEditDTO);
        assertEquals("1", fileEditDTO.getFileId());
        assertEquals("test.txt", fileEditDTO.getFilename());
        assertEquals(content, fileEditDTO.getContent());
    }

    /**
     * 測試 FileMetadataDTO 檔案元資料基本功能。
     * 
     * 驗證檔案元資料傳輸物件的建構和檔案基本資訊處理功能。
     * 
     * 前置條件：
     * - FileMetadataDTO 類別正常載入
     * - FileEnum 列舉正常運作
     * 
     * 測試步驟：
     * - 建立 FileMetadataDTO 實例
     * - 設定檔案名稱、檔案類型和父資料夾 ID
     * - 驗證所有元資料屬性的正確性
     * 
     * 預期結果：
     * - 檔案元資料物件建立成功
     * - 所有屬性值正確設定和取得
     */
    @Test
    @DisplayName("一般測試 - FileMetadataDTO 檔案元數據")
    void testFileMetadataDTO_basicFunctionality() {
        FileMetadataDTO metadataDTO = new FileMetadataDTO();
        metadataDTO.setFilename("document.pdf");
        metadataDTO.setFileType(FileEnum.DOCUMENT);
        metadataDTO.setParentFolderId(100L);
        
        assertNotNull(metadataDTO);
        assertEquals("document.pdf", metadataDTO.getFilename());
        assertEquals(FileEnum.DOCUMENT, metadataDTO.getFileType());
        assertEquals(100L, metadataDTO.getParentFolderId());
    }

    /**
     * 測試 FileFilterDTO 檔案過濾基本功能。
     * 
     * 驗證檔案過濾資料傳輸物件的建構和過濾條件處理功能。
     * 
     * 前置條件：
     * - FileFilterDTO 類別正常載入
     * - FileEnum 列舉正常運作
     * 
     * 測試步驟：
     * - 準備檔案類型過濾清單
     * - 使用建構函式建立包含過濾條件的 FileFilterDTO
     * - 驗證關鍵字、分頁資訊和檔案類型過濾條件
     * 
     * 預期結果：
     * - 檔案過濾物件建立成功
     * - 所有過濾條件正確設定
     */
    @Test
    @DisplayName("一般測試 - FileFilterDTO 檔案過濾")
    void testFileFilterDTO_basicFunctionality() {
        List<FileEnum> types = List.of(FileEnum.DOCUMENT, FileEnum.IMAGE);
        FileFilterDTO filterDTO = new FileFilterDTO("test", null, types, 1, 20, null, null, false, false);
        
        assertNotNull(filterDTO);
        assertEquals(1, filterDTO.getPage());
        assertEquals(20, filterDTO.getPageSize());
        assertEquals(types, filterDTO.getTypes());
        assertEquals("test", filterDTO.getKeyword());
    }

    /**
     * 測試 UploadChunkDTO 上傳分塊基本功能。
     * 
     * 驗證檔案分塊上傳資料傳輸物件的建構和分塊資料處理功能。
     * 
     * 前置條件：
     * - UploadChunkDTO 類別正常載入
     * - Setter 方法正常運作
     * 
     * 測試步驟：
     * - 建立 UploadChunkDTO 實例
     * - 設定傳輸任務 ID、總分塊數、分塊索引和分塊資料
     * - 驗證所有分塊相關屬性的正確性
     * 
     * 預期結果：
     * - 上傳分塊物件建立成功
     * - 分塊資料正確設定和長度驗證
     */
    @Test
    @DisplayName("一般測試 - UploadChunkDTO 上傳分塊")
    void testUploadChunkDTO_basicFunctionality() {
        UploadChunkDTO chunkDTO = new UploadChunkDTO();
        chunkDTO.setTransferTaskId("task123");
        chunkDTO.setTotalChunks(5);
        chunkDTO.setChunkIndex(1);
        chunkDTO.setChunkData(new byte[]{1, 2, 3, 4, 5});
        
        assertNotNull(chunkDTO);
        assertEquals("task123", chunkDTO.getTransferTaskId());
        assertEquals(5, chunkDTO.getTotalChunks());
        assertEquals(1, chunkDTO.getChunkIndex());
        assertNotNull(chunkDTO.getChunkData());
        assertEquals(5, chunkDTO.getChunkData().length);
    }

    /**
     * 測試 UploadResponseDTO 上傳響應基本功能。
     * 
     * 驗證檔案上傳響應資料傳輸物件的建構和上傳進度資訊處理功能。
     * 
     * 前置條件：
     * - UploadResponseDTO 類別正常載入
     * - Builder 模式正常運作
     * 
     * 測試步驟：
     * - 使用 Builder 建立包含完整上傳資訊的響應物件
     * - 設定任務 ID、分塊資訊、進度、狀態和訊息
     * - 驗證所有上傳響應屬性的正確性
     * 
     * 預期結果：
     * - 上傳響應物件建立成功
     * - 所有進度和狀態資訊正確
     */
    @Test
    @DisplayName("一般測試 - UploadResponseDTO 上傳響應")
    void testUploadResponseDTO_basicFunctionality() {
        UploadResponseDTO uploadResponse = UploadResponseDTO.builder()
                .transferTaskId("task123")
                .totalChunks(10)
                .chunkSize(1024L)
                .chunkIndex(5)
                .progress(0.5)
                .isSuccess(true)
                .isFinished(false)
                .message("上傳成功")
                .build();
        
        assertNotNull(uploadResponse);
        assertEquals("task123", uploadResponse.getTransferTaskId());
        assertEquals(10, uploadResponse.getTotalChunks());
        assertEquals(1024L, uploadResponse.getChunkSize());
        assertEquals(5, uploadResponse.getChunkIndex());
        assertEquals(0.5, uploadResponse.getProgress());
        assertTrue(uploadResponse.getIsSuccess());
        assertFalse(uploadResponse.getIsFinished());
        assertEquals("上傳成功", uploadResponse.getMessage());
    }

    /**
     * 測試 UserFileListDTO 使用者檔案清單基本功能。
     * 
     * 驗證使用者檔案清單資料傳輸物件的建構和檔案清單資訊處理功能。
     * 
     * 前置條件：
     * - UserFileListDTO 類別正常載入
     * - FileEnum 列舉正常運作
     * 
     * 測試步驟：
     * - 建立 UserFileListDTO 實例
     * - 設定檔案 ID、名稱、類型、大小、時間戳記和標記狀態
     * - 驗證所有檔案清單屬性的正確性
     * 
     * 預期結果：
     * - 檔案清單物件建立成功
     * - 所有檔案資訊正確設定
     */
    @Test
    @DisplayName("一般測試 - UserFileListDTO 用戶檔案列表")
    void testUserFileListDTO_basicFunctionality() {
        LocalDateTime now = LocalDateTime.now();
        
        UserFileListDTO fileListDTO = new UserFileListDTO();
        fileListDTO.setId(1L);
        fileListDTO.setFilename("myfile.doc");
        fileListDTO.setFileType(FileEnum.DOCUMENT);
        fileListDTO.setFileSize(2048L);
        fileListDTO.setCreateTime(now);
        fileListDTO.setLastAccessTime(now);
        fileListDTO.setIsStar(true);
        
        assertNotNull(fileListDTO);
        assertEquals(1L, fileListDTO.getId());
        assertEquals("myfile.doc", fileListDTO.getFilename());
        assertEquals(FileEnum.DOCUMENT, fileListDTO.getFileType());
        assertEquals(2048L, fileListDTO.getFileSize());
        assertEquals(now, fileListDTO.getCreateTime());
        assertEquals(now, fileListDTO.getLastAccessTime());
        assertTrue(fileListDTO.getIsStar());
    }

    /**
     * 測試 EditorContentDTO 編輯器內容基本功能。
     * 
     * 驗證編輯器內容資料傳輸物件的建構和 Delta 格式內容處理功能。
     * 
     * 前置條件：
     * - EditorContentDTO 和其內部類別正常載入
     * - Builder 模式正常運作
     * 
     * 測試步驟：
     * - 建立 DeltaDTO 並設定純文字內容
     * - 使用 Builder 建立 EditorContentDTO
     * - 驗證 Delta 內容的正確性
     * 
     * 預期結果：
     * - 編輯器內容物件建立成功
     * - Delta 內容正確封裝
     */
    @Test
    @DisplayName("一般測試 - EditorContentDTO 編輯器內容")
    void testEditorContentDTO_basicFunctionality() {
        EditorContentDTO.DeltaDTO deltaDTO = new EditorContentDTO.DeltaDTO();
        deltaDTO.setInsert("plain text content");
        
        EditorContentDTO contentDTO = EditorContentDTO.builder()
                .delta(List.of(deltaDTO))
                .build();
        
        assertNotNull(contentDTO);
        assertNotNull(contentDTO.getDelta());
        assertEquals(1, contentDTO.getDelta().size());
        assertEquals("plain text content", contentDTO.getDelta().get(0).getInsert());
    }

    // ==================== File BOs 測試 ====================

    /**
     * 測試 UserFileDataBO 使用者檔案資料業務物件基本功能。
     * 
     * 驗證使用者檔案資料業務物件的建構和完整檔案資訊封裝功能。
     * 
     * 前置條件：
     * - UserFileDataBO 類別正常載入
     * - Builder 模式和列舉類型正常運作
     * 
     * 測試步驟：
     * - 使用 Builder 建立包含完整檔案資訊的業務物件
     * - 設定使用者檔案 ID、伺服器檔案 ID、檔案基本資訊和分享類型
     * - 驗證所有業務物件屬性的正確性
     * 
     * 預期結果：
     * - 業務物件建立成功
     * - 所有檔案資料屬性正確封裝
     */
    @Test
    @DisplayName("一般測試 - UserFileDataBO 用戶檔案數據業務對象")
    void testUserFileDataBO_basicFunctionality() {
        LocalDateTime now = LocalDateTime.now();
        
        UserFileDataBO fileDataBO = UserFileDataBO.builder()
                .userFileId(1L)
                .serverFileId(2L)
                .userId(3L)
                .filename("testfile.txt")
                .fileType(FileEnum.DOCUMENT)
                .fileSize(1024L)
                .mimeType("text/plain")
                .uploadTime(now)
                .lastAccessTime(now)
                .shareType(FileShareTypeEnum.PRIVATE)
                .build();
        
        assertNotNull(fileDataBO);
        assertEquals(1L, fileDataBO.getUserFileId());
        assertEquals(2L, fileDataBO.getServerFileId());
        assertEquals(3L, fileDataBO.getUserId());
        assertEquals("testfile.txt", fileDataBO.getFilename());
        assertEquals(FileEnum.DOCUMENT, fileDataBO.getFileType());
        assertEquals(1024L, fileDataBO.getFileSize());
        assertEquals("text/plain", fileDataBO.getMimeType());
        assertEquals(now, fileDataBO.getUploadTime());
        assertEquals(now, fileDataBO.getLastAccessTime());
        assertEquals(FileShareTypeEnum.PRIVATE, fileDataBO.getShareType());
    }

    /**
     * 測試 UserFileDataBO 建構函式功能。
     * 
     * 驗證使用者檔案資料業務物件的特殊建構函式，測試從實體物件建立業務物件的功能。
     * 
     * 前置條件：
     * - UserFileDataBO、ServerFileMetadata 和 UserFileMetadata 類別正常載入
     * - 建構函式正常運作
     * 
     * 測試步驟：
     * - 建立並設定 ServerFileMetadata 實體物件
     * - 建立並設定 UserFileMetadata 實體物件
     * - 使用雙參數建構函式建立 UserFileDataBO
     * - 驗證所有屬性從實體物件正確對應
     * 
     * 預期結果：
     * - 業務物件成功從實體物件建立
     * - 所有屬性值正確對應和轉換
     */
    @Test
    @DisplayName("一般測試 - UserFileDataBO 構造函數測試")
    void testUserFileDataBO_constructors() {
        // 測試 ServerFileMetadata + UserFileMetadata 構造函數
        ServerFileMetadata serverMeta = new ServerFileMetadata();
        serverMeta.setId(1L);
        serverMeta.setMimeType("text/plain");
        serverMeta.setFileSize(1024L);
        serverMeta.setGridFsId("gridfs123");
        serverMeta.setMd5("md5hash");
        
        UserFileMetadata userMeta = new UserFileMetadata();
        userMeta.setId(2L);
        userMeta.setUserId(3L);
        userMeta.setFilename("test.txt");
        userMeta.setFileType(FileEnum.DOCUMENT);
        userMeta.setShareType(FileShareTypeEnum.PRIVATE);
        
        UserFileDataBO fileDataBO = new UserFileDataBO(serverMeta, userMeta);
        
        assertNotNull(fileDataBO);
        assertEquals(2L, fileDataBO.getUserFileId());
        assertEquals(1L, fileDataBO.getServerFileId());
        assertEquals(3L, fileDataBO.getUserId());
        assertEquals("test.txt", fileDataBO.getFilename());
        assertEquals(FileEnum.DOCUMENT, fileDataBO.getFileType());
        assertEquals("text/plain", fileDataBO.getMimeType());
        assertEquals(1024L, fileDataBO.getFileSize());
        assertEquals("gridfs123", fileDataBO.getGridFsId());
        assertEquals("md5hash", fileDataBO.getMd5());
        assertEquals(FileShareTypeEnum.PRIVATE, fileDataBO.getShareType());
    }

    /**
     * 測試 UserFileDataBO 響應式資料流功能。
     * 
     * 驗證使用者檔案資料業務物件的響應式程式設計資料流處理功能，包括
     * DataBuffer Flux 和 Mono 的設定與取得。
     * 
     * 前置條件：
     * - UserFileDataBO 類別正常載入
     * - Reactor 響應式框架正常運作
     * 
     * 測試步驟：
     * - 建立 DataBuffer 和對應的 Flux 資料流
     * - 建立 byte 陣列和對應的 Mono 資料流
     * - 設定業務物件的響應式資料流屬性
     * - 驗證資料流屬性的正確性
     * 
     * 預期結果：
     * - 響應式資料流正確設定
     * - Flux 和 Mono 物件不為 null
     */
    @Test
    @DisplayName("一般測試 - UserFileDataBO 響應式數據流")
    void testUserFileDataBO_reactiveStreams() {
        DataBuffer buffer = new DefaultDataBufferFactory().allocateBuffer(1024);
        Flux<DataBuffer> dataFlux = Flux.just(buffer);
        Mono<byte[]> dataMono = Mono.just(new byte[]{1, 2, 3, 4, 5});
        
        UserFileDataBO fileDataBO = new UserFileDataBO();
        fileDataBO.setDataBufferFlux(dataFlux);
        fileDataBO.setDataBufferByte(dataMono);
        
        assertNotNull(fileDataBO.getDataBufferFlux());
        assertNotNull(fileDataBO.getDataBufferByte());
    }

    /**
     * 測試 FileEditBO 檔案編輯業務物件基本功能。
     * 
     * 驗證檔案編輯業務物件的建構和編輯資料封裝功能。
     * 
     * 前置條件：
     * - FileEditBO 和 FileEditDTO 類別正常載入
     * - Builder 模式正常運作
     * 
     * 測試步驟：
     * - 建立包含編輯內容的 DeltaDTO 和 EditorContentDTO
     * - 使用 Builder 建立 FileEditDTO
     * - 使用 FileEditDTO 建立 FileEditBO
     * - 驗證業務物件封裝的編輯資料正確性
     * 
     * 預期結果：
     * - 檔案編輯業務物件建立成功
     * - 編輯內容正確封裝和存取
     */
    @Test
    @DisplayName("一般測試 - FileEditBO 檔案編輯業務對象")
    void testFileEditBO_basicFunctionality() {
        EditorContentDTO.DeltaDTO deltaDTO = new EditorContentDTO.DeltaDTO();
        deltaDTO.setInsert("edited content");
        
        EditorContentDTO content = EditorContentDTO.builder()
                .delta(List.of(deltaDTO))
                .build();
        
        FileEditDTO fileEditDTO = FileEditDTO.builder()
                .fileId("1")
                .filename("edited.txt")
                .content(content)
                .build();
        
        FileEditBO fileEditBO = new FileEditBO(fileEditDTO);
        
        assertNotNull(fileEditBO);
        assertEquals(fileEditDTO, fileEditBO.getFileEditDTO());
        assertEquals("1", fileEditBO.getFileEditDTO().getFileId());
        assertEquals("edited.txt", fileEditBO.getFileEditDTO().getFilename());
        assertEquals("edited content", fileEditBO.getFileEditDTO().getContent().getDelta().get(0).getInsert());
    }

    /**
     * 測試 UploadTaskBO 上傳任務業務物件基本功能。
     * 
     * 驗證上傳任務業務物件的建構和任務資訊封裝功能。
     * 
     * 前置條件：
     * - UploadTaskBO 和 User 類別正常載入
     * - Setter 方法正常運作
     * 
     * 測試步驟：
     * - 建立測試用的 User 實體物件
     * - 建立 UploadTaskBO 並設定任務相關資訊
     * - 設定檔案資訊、使用者、父資料夾和訊息
     * - 驗證所有任務屬性的正確性
     * 
     * 預期結果：
     * - 上傳任務業務物件建立成功
     * - 所有任務資訊正確設定
     */
    @Test
    @DisplayName("一般測試 - UploadTaskBO 上傳任務業務對象")
    void testUploadTaskBO_basicFunctionality() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("task123");
        uploadTaskBO.setFilename("upload.zip");
        uploadTaskBO.setFileSize(5120L);
        uploadTaskBO.setMd5("abc123def456");
        uploadTaskBO.setUser(testUser);
        uploadTaskBO.setParentFolderId(100L);
        uploadTaskBO.setMessage("上傳任務初始化成功");
        
        assertNotNull(uploadTaskBO);
        assertEquals("task123", uploadTaskBO.getTransferTaskId());
        assertEquals("upload.zip", uploadTaskBO.getFilename());
        assertEquals(5120L, uploadTaskBO.getFileSize());
        assertEquals("abc123def456", uploadTaskBO.getMd5());
        assertEquals(testUser, uploadTaskBO.getUser());
        assertEquals(100L, uploadTaskBO.getParentFolderId());
        assertEquals("上傳任務初始化成功", uploadTaskBO.getMessage());
    }

    // ==================== File DAOs 測試 ====================

    /**
     * 測試 UserFileMetaWithDataDAO 使用者檔案元資料和資料存取物件基本功能。
     * 
     * 驗證使用者檔案元資料和資料存取物件的建構和完整檔案資訊封裝功能，
     * 包括使用者檔案元資料、伺服器檔案元資料和線上檔案資訊的組合。
     * 
     * 前置條件：
     * - UserFileMetaWithDataDAO 類別正常載入
     * - 相關列舉類型正常運作
     * 
     * 測試步驟：
     * - 建立 UserFileMetaWithDataDAO 實例
     * - 設定使用者檔案元資料相關屬性
     * - 設定伺服器檔案元資料相關屬性
     * - 設定線上檔案和擁有者資訊
     * - 驗證所有屬性的正確性
     * 
     * 預期結果：
     * - 資料存取物件建立成功
     * - 所有複合資料屬性正確設定
     */
    @Test
    @DisplayName("一般測試 - UserFileMetaWithDataDAO 用戶檔案元數據和數據")
    void testUserFileMetaWithDataDAO_basicFunctionality() {
        LocalDateTime now = LocalDateTime.now();
        
        UserFileMetaWithDataDAO dao = new UserFileMetaWithDataDAO();
        dao.setUfmId(1L);
        dao.setUfmFilename("test.txt");
        dao.setUfmParentFolderId(2L);
        dao.setUfmIsStar(true);
        dao.setUfmFileType(FileEnum.DOCUMENT);
        dao.setUfmShareType(FileShareTypeEnum.PRIVATE);
        dao.setUfmUploadTime(now);
        dao.setUfmLastAccessTime(now);
        dao.setUfmIsDeleted(false);
        dao.setSfmId(3L);
        dao.setSfmFileSize(1024L);
        dao.setSfmMimeType("text/plain");
        dao.setSfmGridFsId("gridfs123");
        dao.setSfmMd5("md5hash");
        dao.setUofId(4L);
        dao.setUfmUserId(5L);
        dao.setOwnerUsername("testuser");
        
        assertNotNull(dao);
        assertEquals(1L, dao.getUfmId());
        assertEquals("test.txt", dao.getUfmFilename());
        assertEquals(2L, dao.getUfmParentFolderId());
        assertTrue(dao.getUfmIsStar());
        assertEquals(FileEnum.DOCUMENT, dao.getUfmFileType());
        assertEquals(FileShareTypeEnum.PRIVATE, dao.getUfmShareType());
        assertEquals(now, dao.getUfmUploadTime());
        assertEquals(now, dao.getUfmLastAccessTime());
        assertFalse(dao.getUfmIsDeleted());
        assertEquals(3L, dao.getSfmId());
        assertEquals(1024L, dao.getSfmFileSize());
        assertEquals("text/plain", dao.getSfmMimeType());
        assertEquals("gridfs123", dao.getSfmGridFsId());
        assertEquals("md5hash", dao.getSfmMd5());
        assertEquals(4L, dao.getUofId());
        assertEquals(5L, dao.getUfmUserId());
        assertEquals("testuser", dao.getOwnerUsername());
    }

    /**
     * 測試 ServerFileMetaCountDAO 伺服器檔案元資料計數記錄基本功能。
     * 
     * 驗證伺服器檔案元資料計數記錄的建構和屬性存取功能。
     * 
     * 前置條件：
     * - ServerFileMetaCountDAO record 類別正常載入
     * - Record 建構函式正常運作
     * 
     * 測試步驟：
     * - 使用建構函式建立包含檔案 ID 和計數的記錄
     * - 驗證檔案 ID 和計數屬性的正確性
     * 
     * 預期結果：
     * - 計數記錄建立成功
     * - 屬性值正確存取
     */
    @Test
    @DisplayName("一般測試 - ServerFileMetaCountDAO 服務器檔案元數據計數")
    void testServerFileMetaCountDAO_basicFunctionality() {
        ServerFileMetaCountDAO dao = new ServerFileMetaCountDAO(1L, 100L);
        
        assertNotNull(dao);
        assertEquals(1L, dao.serverFileId());
        assertEquals(100L, dao.count());
    }

    /**
     * 測試 OnlineHistoryCountAndOldestDAO 線上歷史計數和最舊記錄基本功能。
     * 
     * 驗證線上歷史計數和最舊記錄的建構和屬性存取功能。
     * 
     * 前置條件：
     * - OnlineHistoryCountAndOldestDAO record 類別正常載入
     * - Record 建構函式正常運作
     * 
     * 測試步驟：
     * - 使用建構函式建立包含版本和計數的記錄
     * - 驗證版本和計數屬性的正確性
     * 
     * 預期結果：
     * - 歷史記錄建立成功
     * - 屬性值正確存取
     */
    @Test
    @DisplayName("一般測試 - OnlineHistoryCountAndOldestDAO 在線歷史計數和最舊記錄")
    void testOnlineHistoryCountAndOldestDAO_basicFunctionality() {
        OnlineHistoryCountAndOldestDAO dao = new OnlineHistoryCountAndOldestDAO(1L, 50L);
        
        assertNotNull(dao);
        assertEquals(1L, dao.version());
        assertEquals(50L, dao.count());
    }

    // ==================== File POs 測試 ====================

    /**
     * 測試 QuillContentPO 靜態工具類基本功能。
     * 
     * 驗證 Quill 編輯器內容持久化物件的靜態內部類別功能，包括
     * Operation 和 Delta 類別的建構和屬性處理。
     * 
     * 前置條件：
     * - QuillContentPO 及其內部類別正常載入
     * - 靜態內部類別正常運作
     * 
     * 測試步驟：
     * - 建立 Operation 實例並設定內容和屬性
     * - 建立 Delta 實例並設定操作清單
     * - 驗證所有屬性的正確性
     * 
     * 預期結果：
     * - 內部類別實例建立成功
     * - 編輯器內容正確封裝
     */
    @Test
    @DisplayName("一般測試 - QuillContentPO 靜態工具類")
    void testQuillContentPO_staticUtilityClass() {
        // 測試 QuillContentPO 是純工具類，不需要實例化
        // 實際業務中使用的是靜態內部類 QuillContentPO.Delta, QuillContentPO.Operation
        
        // 創建測試用的 Operation
        QuillContentPO.Operation operation = new QuillContentPO.Operation();
        operation.insert = "test content";
        operation.attributes = Map.of("bold", true, "italic", false);
        
        assertNotNull(operation);
        assertEquals("test content", operation.insert);
        assertNotNull(operation.attributes);
        assertEquals(true, operation.attributes.get("bold"));
        assertEquals(false, operation.attributes.get("italic"));
        
        // 創建測試用的 Delta
        QuillContentPO.Delta delta = new QuillContentPO.Delta();
        delta.delta = List.of(operation);
        
        assertNotNull(delta);
        assertNotNull(delta.delta);
        assertEquals(1, delta.delta.size());
        assertEquals("test content", delta.delta.get(0).insert);
    }

    /**
     * 測試 CustomPatchPO 靜態工具類基本功能。
     * 
     * 驗證自訂補丁持久化物件的靜態內部類別功能，包括 Chunk 類別的
     * 建構、屬性設定和轉換功能。
     * 
     * 前置條件：
     * - CustomPatchPO 及其內部類別正常載入
     * - difflib 函式庫正常運作
     * 
     * 測試步驟：
     * - 建立 Chunk 實例並設定位置、行數和變更位置
     * - 驗證所有屬性的正確性
     * - 測試轉換為 difflib Chunk 的功能
     * 
     * 預期結果：
     * - Chunk 實例建立成功
     * - 轉換為 difflib Chunk 正確
     */
    @Test
    @DisplayName("一般測試 - CustomPatchPO 靜態工具類")
    void testCustomPatchPO_staticUtilityClass() {
        // 測試 CustomPatchPO 是純工具類，不需要實例化
        // 實際業務中使用的是靜態內部類 CustomPatchPO.Patch, CustomPatchPO.Delta, CustomPatchPO.Chunk
        
        // 創建測試用的 Chunk
        CustomPatchPO.Chunk chunk = new CustomPatchPO.Chunk();
        chunk.setPosition(0);
        chunk.setLines(List.of("test line"));
        chunk.setChangePosition(List.of(0));
        
        assertNotNull(chunk);
        assertEquals(0, chunk.getPosition());
        assertEquals(List.of("test line"), chunk.getLines());
        assertEquals(List.of(0), chunk.getChangePosition());
        
        // 測試轉換為 difflib 的 Chunk
        com.github.difflib.patch.Chunk<String> difflibChunk = chunk.toChunk();
        assertNotNull(difflibChunk);
        assertEquals(0, difflibChunk.getPosition());
        assertEquals(List.of("test line"), difflibChunk.getLines());
    }

    /**
     * 測試 FluxDataPO Flux 資料持久化物件基本功能。
     * 
     * 驗證 Flux 資料持久化物件的建構和響應式資料流封裝功能。
     * 
     * 前置條件：
     * - FluxDataPO 類別正常載入
     * - Reactor Flux 框架正常運作
     * 
     * 測試步驟：
     * - 建立測試用的 Flux 資料流
     * - 使用有參建構函式建立 FluxDataPO
     * - 測試無參建構函式建立空實例
     * - 驗證 Flux 屬性的正確性
     * 
     * 預期結果：
     * - 持久化物件建立成功
     * - Flux 資料流正確封裝
     */
    @Test
    @DisplayName("一般測試 - FluxDataPO Flux 數據持久化對象")
    void testFluxDataPO_basicFunctionality() {
        Flux<String> testFlux = Flux.just("test1", "test2", "test3");
        
        FluxDataPO<String> po = new FluxDataPO<>(testFlux);
        
        assertNotNull(po);
        assertNotNull(po.getFlux());
        
        // 測試無參構造函數
        FluxDataPO<String> emptyPO = new FluxDataPO<>();
        assertNotNull(emptyPO);
        assertNotNull(emptyPO.getFlux());
    }

    /**
     * 測試 ShareUserEditPO 共享使用者編輯持久化物件基本功能。
     * 
     * 驗證共享使用者編輯持久化物件的建構和編輯資訊封裝功能。
     * 
     * 前置條件：
     * - ShareUserEditPO 類別正常載入
     * - Setter 方法正常運作
     * 
     * 測試步驟：
     * - 建立 ShareUserEditPO 實例
     * - 設定使用者 ID、檔案 ID、編輯內容、時間和權限
     * - 驗證所有編輯相關屬性的正確性
     * 
     * 預期結果：
     * - 共享編輯物件建立成功
     * - 所有編輯資訊正確設定
     */
    @Test
    @DisplayName("一般測試 - ShareUserEditPO 共享用戶編輯持久化對象")
    void testShareUserEditPO_basicFunctionality() {
        LocalDateTime now = LocalDateTime.now();
        
        ShareUserEditPO po = new ShareUserEditPO();
        po.setUserId(1L);
        po.setFileId(2L);
        po.setEditContent("edited content");
        po.setEditTime(now);
        po.setPermission("WRITE");
        
        assertNotNull(po);
        assertEquals(1L, po.getUserId());
        assertEquals(2L, po.getFileId());
        assertEquals("edited content", po.getEditContent());
        assertEquals(now, po.getEditTime());
        assertEquals("WRITE", po.getPermission());
    }

    // ==================== Event Objects 測試 ====================

    /**
     * 測試 FileEditedMessage 檔案編輯訊息事件基本功能。
     * 
     * 驗證檔案編輯訊息事件記錄的建構和事件資訊封裝功能。
     * 
     * 前置條件：
     * - FileEditedMessage record 類別正常載入
     * - UserFileMetadata、User 和 EditTypeEnum 正常運作
     * 
     * 測試步驟：
     * - 建立測試用的 UserFileMetadata 和 User 實體
     * - 使用 record 建構函式建立事件訊息
     * - 驗證檔案元資料、使用者和編輯類型的正確性
     * 
     * 預期結果：
     * - 事件訊息記錄建立成功
     * - 所有事件屬性正確封裝
     */
    @Test
    @DisplayName("一般測試 - FileEditedMessage 檔案編輯消息事件")
    void testFileEditedMessage_basicFunctionality() {
        // 創建測試用的UserFileMetadata
        UserFileMetadata fileMetadata = new UserFileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setFilename("edited.txt");
        
        // 創建測試用的User
        User user = new User();
        user.setId(2L);
        user.setUsername("testuser");
        
        // 創建測試用的EditTypeEnum
        EditTypeEnum editType = EditTypeEnum.EDIT_CONTENT;
        
        // 使用record的構造函數
        FileEditedMessage message = new FileEditedMessage(fileMetadata, user, editType);
        
        assertNotNull(message);
        assertEquals(fileMetadata, message.fileMetadata());
        assertEquals(user, message.user());
        assertEquals(editType, message.type());
        assertEquals(1L, message.fileMetadata().getId());
        assertEquals("edited.txt", message.fileMetadata().getFilename());
        assertEquals(2L, message.user().getId());
        assertEquals("testuser", message.user().getUsername());
    }

    // ==================== 邊界測試 ====================

    /**
     * 測試 ApiResponseDTO 泛型類型邊界情況。
     * 
     * 驗證 API 響應物件在不同泛型類型下的正確運作，包括集合類型、
     * 基本類型和 void 類型的處理。
     * 
     * 前置條件：
     * - ApiResponseDTO 泛型機制正常運作
     * - Builder 模式支援不同泛型類型
     * 
     * 測試步驟：
     * - 建立 List<String> 泛型的響應物件
     * - 建立 Integer 泛型的響應物件
     * - 建立 Void 泛型的響應物件
     * - 驗證各種泛型類型的正確處理
     * 
     * 預期結果：
     * - 所有泛型類型正確處理
     * - 資料類型保持完整性
     */
    @Test
    @DisplayName("邊界測試 - ApiResponseDTO 泛型類型")
    void testApiResponseDTO_genericTypes() {
        // 測試不同泛型類型
        ApiResponseDTO<List<String>> listResponse = ApiResponseDTO.<List<String>>builder()
                .data(List.of("item1", "item2"))
                .status(200)
                .message("成功")
                .build();
        
        ApiResponseDTO<Integer> intResponse = ApiResponseDTO.<Integer>builder()
                .data(42)
                .status(200)
                .message("成功")
                .build();
        
        ApiResponseDTO<Void> voidResponse = ApiResponseDTO.<Void>builder()
                .data(null)
                .status(204)
                .message("無內容")
                .build();
        
        assertNotNull(listResponse);
        assertEquals(List.of("item1", "item2"), listResponse.getData());
        
        assertNotNull(intResponse);
        assertEquals(42, intResponse.getData());
        
        assertNotNull(voidResponse);
        assertNull(voidResponse.getData());
    }

    /**
     * 測試 PagedResponseDTO 空資料處理邊界情況。
     * 
     * 驗證分頁響應物件在無資料情況下的正確處理，確保空清單和零值
     * 分頁資訊的正確封裝。
     * 
     * 前置條件：
     * - PagedResponseDTO 類別正常載入
     * - 空集合處理正常運作
     * 
     * 測試步驟：
     * - 使用空清單建立分頁響應物件
     * - 設定所有分頁計數為零
     * - 驗證空資料的正確處理
     * 
     * 預期結果：
     * - 空分頁響應物件建立成功
     * - 所有零值屬性正確設定
     */
    @Test
    @DisplayName("邊界測試 - PagedResponseDTO 空數據處理")
    void testPagedResponseDTO_emptyData() {
        PagedResponseDTO<String> emptyResponse = PagedResponseDTO.<String>builder()
                .data(Collections.emptyList())
                .totalPages(0)
                .currentPage(0)
                .pageSize(10)
                .totalElements(0L)
                .build();
        
        assertNotNull(emptyResponse);
        assertTrue(emptyResponse.getData().isEmpty());
        assertEquals(0, emptyResponse.getTotalPages());
        assertEquals(0, emptyResponse.getCurrentPage());
        assertEquals(0L, emptyResponse.getTotalElements());
    }

    /**
     * 測試 UserFileDataBO 空集合處理邊界情況。
     * 
     * 驗證使用者檔案資料業務物件在空集合和集合操作的正確處理。
     * 
     * 前置條件：
     * - UserFileDataBO 類別正常載入
     * - 集合操作正常運作
     * 
     * 測試步驟：
     * - 建立新的業務物件並檢查空集合狀態
     * - 新增共享使用者到集合中
     * - 驗證集合大小和內容的正確性
     * 
     * 預期結果：
     * - 初始集合為空且不為 null
     * - 集合操作正確執行
     */
    @Test
    @DisplayName("邊界測試 - UserFileDataBO 空集合處理")
    void testUserFileDataBO_emptyCollections() {
        UserFileDataBO fileDataBO = new UserFileDataBO();
        
        // 測試空的共享用戶集合
        assertEquals(0, fileDataBO.getShareUsers().size());
        
        // 添加共享用戶
        fileDataBO.getShareUsers().add(1L);
        fileDataBO.getShareUsers().add(2L);
        
        assertEquals(2, fileDataBO.getShareUsers().size());
        assertTrue(fileDataBO.getShareUsers().contains(1L));
        assertTrue(fileDataBO.getShareUsers().contains(2L));
    }

    /**
     * 測試 JSON 序列化特殊字元處理邊界情況。
     * 
     * 驗證 JSON 序列化和反序列化在處理特殊字元時的正確性，包括中文、
     * 表情符號、引號和反斜線等特殊字元。
     * 
     * 前置條件：
     * - ObjectMapper 正確配置特殊字元處理
     * - UTF-8 編碼正常運作
     * 
     * 測試步驟：
     * - 建立包含各種特殊字元的響應物件
     * - 執行 JSON 序列化操作
     * - 執行反序列化操作並比較內容
     * 
     * 預期結果：
     * - 特殊字元正確序列化和反序列化
     * - 內容完整性保持
     */
    @Test
    @DisplayName("邊界測試 - JSON 序列化特殊字符")
    void testJsonSerialization_specialCharacters() throws Exception {
        ApiResponseDTO<String> response = ApiResponseDTO.<String>builder()
                .message("包含特殊字符: 中文, emoji 😀, \"引號\", \\反斜線")
                .data("測試數據 with special chars: @#$%^&*()")
                .status(200)
                .build();
        
        String json = objectMapper.writeValueAsString(response);
        assertNotNull(json);
        
        ApiResponseDTO<?> deserialized = objectMapper.readValue(json, ApiResponseDTO.class);
        assertNotNull(deserialized);
        assertEquals("包含特殊字符: 中文, emoji 😀, \"引號\", \\反斜線", deserialized.getMessage());
    }

    /**
     * 測試大數值處理邊界情況。
     * 
     * 驗證資料傳輸物件在處理 Long 類型最大值時的正確性。
     * 
     * 前置條件：
     * - UserFileDataBO 類別正常載入
     * - Long 類型邊界值處理正常
     * 
     * 測試步驟：
     * - 設定各種 Long 類型屬性為最大值
     * - 驗證數值設定和取得的正確性
     * 
     * 預期結果：
     * - 大數值正確處理和儲存
     * - 無溢位或精度損失
     */
    @Test
    @DisplayName("邊界測試 - 大數值處理")
    void testLargeNumberHandling() {
        UserFileDataBO fileDataBO = new UserFileDataBO();
        fileDataBO.setFileSize(Long.MAX_VALUE);
        fileDataBO.setUserFileId(Long.MAX_VALUE);
        fileDataBO.setServerFileId(Long.MAX_VALUE);
        
        assertEquals(Long.MAX_VALUE, fileDataBO.getFileSize());
        assertEquals(Long.MAX_VALUE, fileDataBO.getUserFileId());
        assertEquals(Long.MAX_VALUE, fileDataBO.getServerFileId());
    }

    /**
     * 測試 null 值處理邊界情況。
     * 
     * 驗證資料傳輸物件在處理 null 值時的正確性和健壯性。
     * 
     * 前置條件：
     * - UserFileDataBO 類別正常載入
     * - Null 值處理機制正常運作
     * 
     * 測試步驟：
     * - 設定各種屬性為 null 值
     * - 驗證 null 值的正確設定和取得
     * - 檢查集合屬性不為 null 的保證
     * 
     * 預期結果：
     * - Null 值正確處理
     * - 集合屬性保持非 null 狀態
     */
    @Test
    @DisplayName("邊界測試 - null 值處理")
    void testNullValueHandling() {
        UserFileDataBO fileDataBO = new UserFileDataBO();
        
        // 測試 null 值設定
        fileDataBO.setFilename(null);
        fileDataBO.setFileType(null);
        fileDataBO.setMimeType(null);
        
        assertNull(fileDataBO.getFilename());
        assertNull(fileDataBO.getFileType());
        assertNull(fileDataBO.getMimeType());
        
        // 測試集合不應該為 null
        assertNotNull(fileDataBO.getShareUsers());
    }
}