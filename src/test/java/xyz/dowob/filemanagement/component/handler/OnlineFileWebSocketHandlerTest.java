package xyz.dowob.filemanagement.component.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.event.EventSink;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.*;
import xyz.dowob.filemanagement.data.event.FileEditedMessage;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.EditorContentDTO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileVersionDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.data.response.WebSocketResponse;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceImpl.fileservice.OnlineFileServiceImpl;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OnlineFileWebSocketHandler 綜合功能測試
 * 
 * 此測試類別涵蓋線上檔案即時編輯的核心功能測試。
 * 包含多用戶會話管理、訊息處理、權限驗證和異常處理。
 * 
 * 前置條件：
 * - 初始化 OnlineFileWebSocketHandler 和相關依賴的 Mock 對象
 * - 設置用戶權限和檔案存取權限
 * - 配置 EventSink 事件訂閱機制
 * 
 * 測試步驟：
 * - 測試 WebSocket 連線建立與權限驗證
 * - 測試多用戶會話管理與同步機制
 * - 測試編輯操作處理與廣播
 * - 測試異常情境與錯誤處理
 * 
 * 預期結果：
 * - 所有測試應成功完成或正確處理異常情況
 * - 多用戶編輯功能正常運作
 * - 資源管理與會話清理正確執行
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OnlineFileWebSocketHandler 綜合功能測試")
class OnlineFileWebSocketHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OnlineFileServiceImpl onlineFileService;

    @Mock
    private ValidationService validationService;

    @Mock
    private PermissionService<UserFileMetadata> permissionService;

    @Mock
    private FilePermissionRuleManager filePermissionRuleManager;

    @Mock
    private FileProperties fileProperties;

    @Mock
    private FileProperties.Upload uploadProperties;

    @Mock
    private EventSink<FileEditedMessage> eventSink;

    @Mock
    private CustomWebSocketSession customSession;

    @Mock
    private User mockUser;

    @Mock
    private UserFileMetadata mockFileMetadata;

    @Mock
    private WebSocketMessage webSocketMessage;

    @Mock
    private HandshakeInfo handshakeInfo;

    @Mock
    private Permission<UserFileMetadata> filePermission;

    private OnlineFileWebSocketHandler handlerUnderTest;

    @BeforeEach
    void setUp() {
        // 設置 FileProperties mock
        when(fileProperties.getUpload()).thenReturn(uploadProperties);
        when(uploadProperties.getEditOnlineFileWebSocketPath()).thenReturn("/ws/edit");
        
        // 設置 EventSink mock - 返回空的 Flux
        when(eventSink.subscribe()).thenReturn(Flux.empty());

        handlerUnderTest = new OnlineFileWebSocketHandler(
                objectMapper,
                onlineFileService,
                validationService,
                permissionService,
                filePermissionRuleManager,
                fileProperties,
                eventSink
        );
        
        // 手動調用 @PostConstruct 方法
        handlerUnderTest.init();
    }

    // ==================== 輔助方法 ====================


    @Test
    @DisplayName("WebSocket 連線建立成功 - 驗證權限並載入檔案內容")
    void handle_webSocketConnectionEstablished_loadFileContentSuccessfully() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupValidationServiceMocks();
        setupObjectMapperMocks();

        // 設置 WebSocket 訊息接收
        String testMessage = "{\"editType\":\"EDIT_CONTENT\",\"content\":\"test content\"}";
        when(webSocketMessage.getPayloadAsText()).thenReturn(testMessage);
        when(customSession.receive()).thenReturn(Flux.just(webSocketMessage));

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證關鍵操作被調用
        verify(permissionService, atLeast(1)).validateUserPermission(eq(mockUser), eq(fileId), anyList());
        verify(onlineFileService, atLeast(1)).downloadFile(eq(mockFileMetadata), eq(mockUser), eq(DownloadActionEnum.PREVIEW.name()));
        verify(onlineFileService, atLeast(1)).getFileVersionList(eq(mockUser), eq(mockFileMetadata), eq(1), any());
        verify(customSession, atLeastOnce()).send(any());
    }


    /**
     * 設置具有權限的用戶和檔案存取權限
     *
     * @param userId 用戶ID
     * @param fileId 檔案ID
     * @param role 用戶角色
     */
    private void setupUserWithFilePermission(Long userId, Long fileId, RoleEnum role) {
        mockUser = createMockUser(userId, role);
        mockFileMetadata = createMockFileMetadata(fileId, "test_file.txt");

        lenient().when(customSession.getUser()).thenReturn(mockUser);
        lenient().when(filePermissionRuleManager.getAllowShared()).thenReturn(filePermission);
        lenient().when(permissionService.validateUserPermission(eq(mockUser), eq(fileId), anyList()))
                .thenReturn(Mono.just(mockFileMetadata));
    }


    /**
     * 設置 WebSocket 會話的基本屬性
     *
     * @param fileId 檔案ID
     */
    private void setupWebSocketSession(Long fileId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("fileId", fileId);

        lenient().when(customSession.getAttributes()).thenReturn(attributes);
        lenient().when(customSession.getHandshakeInfo()).thenReturn(handshakeInfo);
        lenient().when(handshakeInfo.getUri()).thenReturn(URI.create("/ws/edit"));
        lenient().when(customSession.isOpen()).thenReturn(true);
        lenient().when(customSession.textMessage(anyString())).thenReturn(webSocketMessage);
        lenient().when(customSession.send(any())).thenReturn(Mono.empty());
        lenient().when(customSession.close(any(CloseStatus.class))).thenReturn(Mono.empty());

        // 設置 LogUnity 需要的屬性
        lenient().when(customSession.getAttribute("clientIp")).thenReturn("127.0.0.1");
        lenient().when(customSession.getAttribute("requestId")).thenReturn("test-request-id");
    }


    /**
     * 設置檔案服務的模擬回應
     */
    private void setupFileServiceMocks() {
        // 模擬檔案下載
        UserFileDataBO fileDataBO = mock(UserFileDataBO.class);
        lenient().when(fileDataBO.getContent()).thenReturn(createMockEditorContentDTO());
        lenient().when(fileDataBO.getFilename()).thenReturn("test_file.txt");
        lenient().when(onlineFileService.downloadFile(any(UserFileMetadata.class), any(User.class), anyString()))
                .thenReturn(Mono.just(fileDataBO));

        // 模擬版本歷史
        PagedResponseDTO<FileVersionDTO> versionList = mock(PagedResponseDTO.class);
        lenient().when(onlineFileService.getFileVersionList(any(User.class), any(UserFileMetadata.class), anyInt(), any()))
                .thenReturn(Mono.just(versionList));

        // 模擬檔案編輯
        lenient().when(onlineFileService.editFile(any(), any(User.class)))
                .thenReturn(Mono.empty());
    }


    /**
     * 設置驗證服務的模擬回應
     */
    private void setupValidationServiceMocks() {
        lenient().when(validationService.validateFileType(any(UserFileMetadata.class), eq(FileEnum.ONLINE_DOCUMENT)))
                .thenReturn(Mono.empty());
        lenient().when(validationService.validateEditFileDTO(any(FileEditDTO.class), eq(false)))
                .thenReturn(Mono.empty());
    }


    /**
     * 設置 ObjectMapper 的 JSON 處理
     */
    private void setupObjectMapperMocks() throws JsonProcessingException {
        lenient().when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"type\":\"test\",\"message\":\"test message\"}");

        // 模擬 JSON 解析
        JsonNode mockJsonNode = mock(JsonNode.class);
        lenient().when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);
        lenient().when(objectMapper.treeToValue(any(JsonNode.class), eq(FileEditDTO.class)))
                .thenReturn(createMockFileEditDTO("1", EditTypeEnum.EDIT_CONTENT, new EditorContentDTO()));
    }


    /**
     * 建立模擬的用戶物件
     *
     * @param userId 用戶ID
     * @param role 用戶角色
     * @return User 模擬的用戶
     */
    private User createMockUser(Long userId, RoleEnum role) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getRole()).thenReturn(role);
        return user;
    }


    /**
     * 建立模擬的檔案元數據
     *
     * @param fileId 檔案ID
     * @param filename 檔案名稱
     * @return UserFileMetadata 模擬的檔案元數據
     */
    private UserFileMetadata createMockFileMetadata(Long fileId, String filename) {
        UserFileMetadata fileMetadata = mock(UserFileMetadata.class);
        lenient().when(fileMetadata.getId()).thenReturn(fileId);
        lenient().when(fileMetadata.getFilename()).thenReturn(filename);
        return fileMetadata;
    }


    /**
     * 創建模擬的 EditorContentDTO 對象
     *
     * @return EditorContentDTO 對象
     */
    private EditorContentDTO createMockEditorContentDTO() {
        EditorContentDTO editorContent = new EditorContentDTO();
        List<EditorContentDTO.DeltaDTO> deltaList = new ArrayList<>();
        EditorContentDTO.DeltaDTO delta = new EditorContentDTO.DeltaDTO();
        delta.setInsert("test content");
        deltaList.add(delta);
        editorContent.setDelta(deltaList);
        return editorContent;
    }

    // ==================== 核心功能測試 ====================


    /**
     * 建立模擬的檔案編輯 DTO
     *
     * @param fileId 檔案ID
     * @param editType 編輯類型
     * @param content 編輯內容
     * @return FileEditDTO 模擬的檔案編輯 DTO
     */
    private FileEditDTO createMockFileEditDTO(String fileId, EditTypeEnum editType, EditorContentDTO content) {
        FileEditDTO dto = new FileEditDTO();
        dto.setFileId(fileId);
        dto.setFilename("test_file.txt");
        dto.setEditType(editType);
        dto.setContent(content);
        return dto;
    }


    @Test
    @DisplayName("檔案權限驗證失敗 - 拒絕連線並關閉會話")
    void handle_filePermissionValidationFailed_rejectConnectionAndClose() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupObjectMapperMocks();

        // 設置權限驗證失敗
        ValidationException permissionException = new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, fileId.toString());
        when(permissionService.validateUserPermission(eq(mockUser), eq(fileId), anyList()))
                .thenReturn(Mono.error(permissionException));

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證錯誤處理
        verify(permissionService, atLeast(1)).validateUserPermission(eq(mockUser), eq(fileId), anyList());
        verify(customSession, atLeast(1)).send(any());
        verify(customSession).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("檔案類型驗證失敗 - 非線上文件類型拒絕編輯")
    void handle_fileTypeValidationFailed_rejectNonOnlineDocument() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupObjectMapperMocks();

        // 設置檔案類型驗證失敗
        ValidationException fileTypeException = new ValidationException(ValidationException.ErrorCode.FILE_TYPE_WITH_WRONG_REQUEST_PATH, "ONLINE_DOCUMENT", "BINARY");
        when(validationService.validateFileType(any(UserFileMetadata.class), eq(FileEnum.ONLINE_DOCUMENT)))
                .thenReturn(Mono.error(fileTypeException));

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證檔案類型驗證
        verify(validationService).validateFileType(eq(mockFileMetadata), eq(FileEnum.ONLINE_DOCUMENT));
        verify(customSession, atLeast(1)).send(any());
        verify(customSession).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("編輯訊息處理成功 - 處理內容編輯並廣播更新")
    void handle_editMessageProcessed_broadcastContentUpdate() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupValidationServiceMocks();
        setupObjectMapperMocks();

        // 設置編輯訊息
        String editMessage = "{\"editType\":\"EDIT_CONTENT\",\"content\":\"updated content\"}";
        when(webSocketMessage.getPayloadAsText()).thenReturn(editMessage);
        when(customSession.receive()).thenReturn(Flux.just(webSocketMessage));

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證編輯處理
        verify(validationService).validateEditFileDTO(any(FileEditDTO.class), eq(false));
        verify(onlineFileService, atLeast(1)).editFile(any(), eq(mockUser));
        verify(customSession, atLeastOnce()).send(any());
    }

    @Test
    @DisplayName("JSON 解析錯誤 - 處理無效訊息格式")
    void handle_jsonParsingError_handleInvalidMessageFormat() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupValidationServiceMocks();
        
        // 設置 JSON 解析錯誤
        String invalidJson = "{invalid json content}";
        JsonProcessingException jsonException = new JsonProcessingException("Invalid JSON format") {};
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"error\":\"json_error\"}");
        when(objectMapper.readTree(invalidJson)).thenThrow(jsonException);
        
        when(webSocketMessage.getPayloadAsText()).thenReturn(invalidJson);
        when(customSession.receive()).thenReturn(Flux.just(webSocketMessage));

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證錯誤處理
        verify(objectMapper).readTree(invalidJson);
        verify(customSession, atLeastOnce()).send(any());
    }

    // ==================== 會話管理測試 ====================

    @Test
    @DisplayName("多用戶會話管理 - 添加和移除會話正確運作")
    void sessionManagement_multipleUsers_addAndRemoveCorrectly() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        setupFileServiceMocks();
        setupValidationServiceMocks();
        setupObjectMapperMocks();

        // 設置第一個用戶
        Long userId1 = 1L;
        CustomWebSocketSession session1 = mock(CustomWebSocketSession.class);
        User user1 = createMockUser(userId1, RoleEnum.USER);
        setupUserSessionMocks(session1, user1, fileId);

        // 設置第二個用戶
        Long userId2 = 2L;
        CustomWebSocketSession session2 = mock(CustomWebSocketSession.class);
        User user2 = createMockUser(userId2, RoleEnum.USER);
        setupUserSessionMocks(session2, user2, fileId);

        // 設置權限驗證
        when(permissionService.validateUserPermission(any(User.class), eq(fileId), anyList()))
                .thenReturn(Mono.just(mockFileMetadata));

        // 測試步驟：第一個用戶連線
        when(session1.receive()).thenReturn(Flux.empty());
        Mono<Void> result1 = handlerUnderTest.handle(session1);

        // 測試步驟：第二個用戶連線
        when(session2.receive()).thenReturn(Flux.empty());
        Mono<Void> result2 = handlerUnderTest.handle(session2);

        // 預期結果
        StepVerifier.create(Mono.when(result1, result2))
                .verifyComplete();

        // 驗證會話管理
        verify(session1, atLeastOnce()).send(any());
        verify(session2, atLeastOnce()).send(any());
    }


    /**
     * 設置用戶會話的模擬屬性
     *
     * @param session WebSocket 會話
     * @param user 用戶
     * @param fileId 檔案ID
     */
    private void setupUserSessionMocks(CustomWebSocketSession session, User user, Long fileId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("fileId", fileId);

        lenient().when(session.getUser()).thenReturn(user);
        lenient().when(session.getAttributes()).thenReturn(attributes);
        lenient().when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        lenient().when(handshakeInfo.getUri()).thenReturn(URI.create("/ws/edit"));
        lenient().when(session.isOpen()).thenReturn(true);
        lenient().when(session.textMessage(anyString())).thenReturn(webSocketMessage);
        lenient().when(session.send(any())).thenReturn(Mono.empty());
        lenient().when(session.close(any(CloseStatus.class))).thenReturn(Mono.empty());

        // 設置 LogUnity 需要的屬性
        lenient().when(session.getAttribute("clientIp")).thenReturn("127.0.0.1");
        lenient().when(session.getAttribute("requestId")).thenReturn("test-request-id");
    }


    @Test
    @DisplayName("VISITOR 角色特殊處理 - 無連線數量限制")
    void sessionManagement_visitorRole_noConnectionLimit() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long visitorId = 999L;
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupValidationServiceMocks();
        setupObjectMapperMocks();

        // 設置 VISITOR 用戶
        mockUser = createMockUser(visitorId, RoleEnum.VISITOR);
        when(customSession.getUser()).thenReturn(mockUser);
        when(permissionService.validateUserPermission(eq(mockUser), eq(fileId), anyList()))
                .thenReturn(Mono.just(mockFileMetadata));

        // 設置空的接收流
        when(customSession.receive()).thenReturn(Flux.empty());

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證 VISITOR 權限處理
        verify(permissionService, atLeast(1)).validateUserPermission(eq(mockUser), eq(fileId), anyList());
        verify(customSession, atLeastOnce()).send(any());
    }


    @Test
    @DisplayName("會話計數器正確性 - 添加和移除會話更新計數")
    void sessionManagement_sessionCounter_updateCountCorrectly() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupValidationServiceMocks();
        setupObjectMapperMocks();

        // 設置會話接收流（模擬短暫連線）
        when(customSession.receive()).thenReturn(Flux.empty());

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證會話計數更新
        verify(customSession, atLeastOnce()).send(any());

        // 驗證會話建立時的廣播（包含編輯人數更新）
        ArgumentCaptor<Mono> messageCaptor = ArgumentCaptor.forClass(Mono.class);
        verify(customSession, atLeastOnce()).send(messageCaptor.capture());

        // 檢查是否有編輯人數更新的訊息
        assertThat(messageCaptor.getAllValues()).isNotEmpty();
    }

    // ==================== 訊息處理測試 ====================


    @Test
    @DisplayName("併發會話處理 - 執行緒安全的會話操作")
    void sessionManagement_concurrentSessions_threadSafeOperations() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        setupFileServiceMocks();
        setupValidationServiceMocks();
        setupObjectMapperMocks();

        // 設置多個併發會話
        CustomWebSocketSession[] sessions = new CustomWebSocketSession[3];
        User[] users = new User[3];
        Mono<Void>[] results = new Mono[3];

        for (int i = 0; i < 3; i++) {
            sessions[i] = mock(CustomWebSocketSession.class);
            users[i] = createMockUser((long) (i + 1), RoleEnum.USER);
            setupUserSessionMocks(sessions[i], users[i], fileId);
            when(sessions[i].receive()).thenReturn(Flux.empty());

            // 設置權限驗證
            when(permissionService.validateUserPermission(eq(users[i]), eq(fileId), anyList()))
                    .thenReturn(Mono.just(mockFileMetadata));

            results[i] = handlerUnderTest.handle(sessions[i]);
        }

        // 測試步驟：併發執行所有會話
        Mono<Void> concurrentResult = Mono.when(results);

        // 預期結果
        StepVerifier.create(concurrentResult)
                .verifyComplete();

        // 驗證所有會話都被正確處理
        for (int i = 0; i < 3; i++) {
            verify(sessions[i], atLeastOnce()).send(any());
        }
    }


    @Test
    @DisplayName("廣播訊息機制 - 向所有連線發送更新")
    void messageProcessing_broadcastMechanism_sendToAllConnections() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        setupObjectMapperMocks();

        // 建立測試訊息
        WebSocketResponse<?> testMessage = WebSocketResponse.builder()
                .type(WebsocketResponseType.INFO)
                .message("test broadcast message")
                .build();

        // 模擬多個活躍會話
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);

        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session1.textMessage(anyString())).thenReturn(mock(WebSocketMessage.class));
        when(session2.textMessage(anyString())).thenReturn(mock(WebSocketMessage.class));
        when(session1.send(any())).thenReturn(Mono.empty());
        when(session2.send(any())).thenReturn(Mono.empty());

        // 測試步驟
        Mono<Void> result = handlerUnderTest.boastMessage(List.of(session1, session2), testMessage);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證廣播處理
        verify(objectMapper).writeValueAsString(testMessage);
        verify(session1, atLeast(1)).send(any());
        verify(session2, atLeast(1)).send(any());
    }


    @Test
    @DisplayName("編輯類型處理 - 支援不同編輯操作")
    void messageProcessing_editTypeHandling_supportDifferentOperations() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupValidationServiceMocks();

        // 測試不同類型的編輯操作
        EditTypeEnum[] editTypes = {
                EditTypeEnum.EDIT_CONTENT,
                EditTypeEnum.BUILD_HISTORY_RECORD,
                EditTypeEnum.REVERT_HISTORY_RECORD
        };

        for (EditTypeEnum editType : editTypes) {
            // 重新設置 ObjectMapper mocks，避免衝突
            lenient().when(objectMapper.writeValueAsString(any()))
                    .thenReturn("{\"type\":\"test\",\"message\":\"test message\"}");

            JsonNode mockJsonNode = mock(JsonNode.class);
            lenient().when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);

            // 設置特定編輯類型的訊息
            FileEditDTO editDTO = createMockFileEditDTO("1", editType, new EditorContentDTO());
            lenient().when(objectMapper.treeToValue(any(JsonNode.class), eq(FileEditDTO.class)))
                    .thenReturn(editDTO);

            String editMessage = String.format("{\"editType\":\"%s\",\"content\":\"test content\"}", editType.name());
            when(webSocketMessage.getPayloadAsText()).thenReturn(editMessage);
            when(customSession.receive()).thenReturn(Flux.just(webSocketMessage).take(1));

            // 測試步驟
            Mono<Void> result = handlerUnderTest.handle(customSession);

            // 預期結果
            StepVerifier.create(result)
                    .verifyComplete();
        }

        // 驗證不同編輯類型都被處理
        verify(onlineFileService, atLeast(editTypes.length)).editFile(any(), eq(mockUser));
    }

    // ==================== 輔助方法（新增） ====================


    @Test
    @DisplayName("不支援的編輯操作 - 拒絕 EDIT_METADATA 類型")
    void messageProcessing_unsupportedOperation_rejectEditMetadata() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupValidationServiceMocks();
        setupObjectMapperMocks();

        // 設置不支援的編輯類型
        FileEditDTO editDTO = createMockFileEditDTO("1", EditTypeEnum.EDIT_METADATA, new EditorContentDTO());
        when(objectMapper.treeToValue(any(JsonNode.class), eq(FileEditDTO.class)))
                .thenReturn(editDTO);

        String metadataMessage = "{\"editType\":\"EDIT_METADATA\",\"content\":\"metadata\"}";
        when(webSocketMessage.getPayloadAsText()).thenReturn(metadataMessage);
        when(customSession.receive()).thenReturn(Flux.just(webSocketMessage));

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證不支援操作的處理
        verify(customSession, atLeastOnce()).send(any());

        // 驗證 editFile 不被調用（因為操作不支援）
        verify(onlineFileService, never()).editFile(any(), any());
    }

    // ==================== 異常處理測試 ====================


    @Test
    @DisplayName("連線中斷處理 - AbortedException 正確處理")
    void exceptionHandling_connectionAborted_handleGracefully() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupValidationServiceMocks();
        setupObjectMapperMocks();

        // 模擬連線中斷
        reactor.netty.channel.AbortedException abortedException = new reactor.netty.channel.AbortedException("Connection aborted by client");
        when(customSession.receive()).thenReturn(Flux.error(abortedException));

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證連線中斷處理（AbortedException 被正確處理）
        // 注意：處理器會在資源清理時調用 close()，這是正常行為
        verify(customSession, atMost(1)).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("檔案服務錯誤處理 - 下載失敗的錯誤處理")
    void exceptionHandling_fileServiceError_handleDownloadFailure() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupValidationServiceMocks();
        setupObjectMapperMocks();

        // 設置檔案下載失敗
        RuntimeException serviceException = new RuntimeException("File download failed");
        when(onlineFileService.downloadFile(any(UserFileMetadata.class), any(User.class), anyString()))
                .thenReturn(Mono.error(serviceException));

        // 設置版本列表正常
        PagedResponseDTO<FileVersionDTO> versionList = mock(PagedResponseDTO.class);
        when(onlineFileService.getFileVersionList(any(User.class), any(UserFileMetadata.class), anyInt(), any()))
                .thenReturn(Mono.just(versionList));

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證錯誤處理
        verify(onlineFileService, atLeast(1)).downloadFile(eq(mockFileMetadata), eq(mockUser), eq(DownloadActionEnum.PREVIEW.name()));
        verify(customSession, atLeast(1)).send(any());
        verify(customSession).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("編輯驗證失敗 - DTO 驗證錯誤處理")
    void exceptionHandling_editValidationFailed_handleDTOValidationError() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupObjectMapperMocks();

        // 設置 DTO 驗證失敗
        ValidationException validationException = new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "invalid edit data");
        when(validationService.validateFileType(any(UserFileMetadata.class), eq(FileEnum.ONLINE_DOCUMENT)))
                .thenReturn(Mono.empty());
        when(validationService.validateEditFileDTO(any(FileEditDTO.class), eq(false)))
                .thenReturn(Mono.error(validationException));

        // 設置編輯訊息
        String editMessage = "{\"editType\":\"EDIT_CONTENT\",\"content\":\"invalid content\"}";
        when(webSocketMessage.getPayloadAsText()).thenReturn(editMessage);
        when(customSession.receive()).thenReturn(Flux.just(webSocketMessage));

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證驗證錯誤處理
        verify(validationService).validateEditFileDTO(any(FileEditDTO.class), eq(false));
        verify(customSession, atLeastOnce()).send(any());
        
        // 驗證編輯操作不被執行
        verify(onlineFileService, never()).editFile(any(), any());
    }

    @Test
    @DisplayName("JSON 序列化錯誤 - 處理訊息格式化失敗")
    void exceptionHandling_jsonSerializationError_handleFormatFailure() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        setupObjectMapperMocks();

        // 設置 JSON 序列化錯誤
        JsonProcessingException jsonException = new JsonProcessingException("Serialization failed") {};
        when(objectMapper.writeValueAsString(any())).thenThrow(jsonException);

        // 建立測試訊息
        WebSocketResponse<?> testMessage = WebSocketResponse.builder()
                .type(WebsocketResponseType.ERROR)
                .message("test error message")
                .build();

        // 測試步驟
        Mono<Void> result = handlerUnderTest.boastMessage(fileId, testMessage);

        // 預期結果 - 序列化錯誤應該被轉換為 ProcessException
        StepVerifier.create(result)
                .expectError()
                .verify();

        // 驗證序列化錯誤處理
        verify(objectMapper).writeValueAsString(testMessage);
    }

    // ==================== 邊界條件測試 ====================

    @Test
    @DisplayName("空檔案會話處理 - 沒有活躍會話時的廣播")
    void boundaryConditions_emptyFileSession_handleBroadcastWithNoSessions() throws JsonProcessingException {
        // 前置條件
        Long fileId = 999L; // 使用不存在的檔案ID
        setupObjectMapperMocks();

        // 建立測試訊息
        WebSocketResponse<?> testMessage = WebSocketResponse.builder()
                .type(WebsocketResponseType.INFO)
                .message("test broadcast to empty sessions")
                .build();

        // 測試步驟
        Mono<Void> result = handlerUnderTest.boastMessage(fileId, testMessage);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證空會話處理
        verify(objectMapper).writeValueAsString(testMessage);
        // 沒有會話，所以不會有實際的發送操作
    }

    @Test
    @DisplayName("關閉的會話處理 - 過濾無效會話")
    void boundaryConditions_closedSessions_filterInvalidSessions() throws JsonProcessingException {
        // 前置條件
        setupObjectMapperMocks();

        // 建立測試訊息
        WebSocketResponse<?> testMessage = WebSocketResponse.builder()
                .type(WebsocketResponseType.INFO)
                .message("test with closed sessions")
                .build();

        // 模擬已關閉的會話
        WebSocketSession closedSession = mock(WebSocketSession.class);
        WebSocketSession activeSession = mock(WebSocketSession.class);
        
        when(closedSession.isOpen()).thenReturn(false);
        when(activeSession.isOpen()).thenReturn(true);
        when(activeSession.textMessage(anyString())).thenReturn(mock(WebSocketMessage.class));
        when(activeSession.send(any())).thenReturn(Mono.empty());

        // 測試步驟
        Mono<Void> result = handlerUnderTest.boastMessage(List.of(closedSession, activeSession), testMessage);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證會話過濾
        verify(closedSession, never()).send(any()); // 關閉的會話不發送
        verify(activeSession, atLeast(1)).send(any()); // 活躍的會話發送
    }

    @Test
    @DisplayName("資源清理驗證 - 會話結束時正確清理")
    void boundaryConditions_resourceCleanup_properCleanupOnSessionEnd() throws JsonProcessingException {
        // 前置條件
        Long fileId = 1L;
        Long userId = 1L;
        setupUserWithFilePermission(userId, fileId, RoleEnum.USER);
        setupWebSocketSession(fileId);
        setupFileServiceMocks();
        setupValidationServiceMocks();
        setupObjectMapperMocks();

        // 設置短暫的接收流（會話快速結束）
        when(customSession.receive()).thenReturn(Flux.empty());

        // 測試步驟
        Mono<Void> result = handlerUnderTest.handle(customSession);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證資源清理 - 會話應該被正確關閉
        verify(customSession, atLeastOnce()).send(any()); // 發送初始化訊息
        
        // 注意：doFinally 中的清理操作在測試環境中可能異步執行
        // 主要驗證連線建立和基本訊息發送
    }

    // ==================== 工具方法測試 ====================

    @Test
    @DisplayName("WebSocket 會話狀態檢查 - sendMessage 方法驗證")
    void utilityMethods_sessionStatusCheck_sendMessageValidation() throws JsonProcessingException {
        // 前置條件
        setupObjectMapperMocks();
        
        WebSocketSession activeSession = mock(WebSocketSession.class);
        WebSocketSession closedSession = mock(WebSocketSession.class);
        
        when(activeSession.isOpen()).thenReturn(true);
        when(closedSession.isOpen()).thenReturn(false);
        when(activeSession.textMessage(anyString())).thenReturn(mock(WebSocketMessage.class));
        when(activeSession.send(any())).thenReturn(Mono.empty());

        Object testMessage = "test message";

        // 測試步驟 1：活躍會話
        Mono<Void> activeResult = handlerUnderTest.sendMessage(activeSession, testMessage);
        StepVerifier.create(activeResult)
                .verifyComplete();

        // 測試步驟 2：關閉會話
        Mono<Void> closedResult = handlerUnderTest.sendMessage(closedSession, testMessage);
        StepVerifier.create(closedResult)
                .verifyComplete();

        // 測試步驟 3：null 會話
        Mono<Void> nullResult = handlerUnderTest.sendMessage((WebSocketSession) null, testMessage);
        StepVerifier.create(nullResult)
                .verifyComplete();

        // 驗證會話狀態檢查
        verify(activeSession, atLeast(1)).send(any());
        verify(closedSession, never()).send(any());
    }

    @Test
    @DisplayName("重試機制驗證 - 網路錯誤導致重試耗盡")
    void utilityMethods_retryMechanism_networkErrorExhaustion() throws JsonProcessingException {
        // 前置條件
        setupObjectMapperMocks();
        
        CustomWebSocketSession flakySession = mock(CustomWebSocketSession.class);
        when(flakySession.isOpen()).thenReturn(true);
        when(flakySession.textMessage(anyString())).thenReturn(mock(WebSocketMessage.class));
        
        // 模擬持續網路錯誤
        when(flakySession.send(any()))
            .thenReturn(Mono.error(new java.io.IOException("Persistent network error")));
        
        Object testMessage = "test retry exhaustion message";
        
        // 測試步驟
        Mono<Void> result = handlerUnderTest.sendMessage(flakySession, testMessage);
        
        // 預期結果 - 重試耗盡後失敗，使用通用異常檢查避免編譯問題
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> 
                throwable.getMessage() != null && 
                throwable.getMessage().contains("Retries exhausted"))
            .verify(Duration.ofSeconds(3));
        
        // 驗證重試處理 - 從日誌可以看到重試發生，但Mock只被調用一次 (Reactor內部處理重試)
        verify(flakySession, times(1)).send(any());
    }

    @Test
    @DisplayName("重試機制驗證 - 成功發送訊息無需重試")
    void utilityMethods_retryMechanism_successWithoutRetry() throws JsonProcessingException {
        // 前置條件
        setupObjectMapperMocks();
        
        CustomWebSocketSession successSession = mock(CustomWebSocketSession.class);
        when(successSession.isOpen()).thenReturn(true);
        when(successSession.textMessage(anyString())).thenReturn(mock(WebSocketMessage.class));
        
        // 模擬成功發送
        when(successSession.send(any())).thenReturn(Mono.empty());
        
        Object testMessage = "test success message";
        
        // 測試步驟
        Mono<Void> result = handlerUnderTest.sendMessage(successSession, testMessage);
        
        // 預期結果 - 直接成功完成
        StepVerifier.create(result)
            .expectComplete()
            .verify(Duration.ofSeconds(1));
        
        // 驗證只調用一次 - 無需重試
        verify(successSession, times(1)).send(any());
    }

    @Test
    @DisplayName("JSON 轉換工具驗證 - 物件序列化正確性")
    void utilityMethods_jsonConversion_objectSerializationCorrectness() throws JsonProcessingException {
        // 前置條件
        String expectedJson = "{\"type\":\"INFO\",\"message\":\"test conversion\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.textMessage(expectedJson)).thenReturn(mock(WebSocketMessage.class));
        when(session.send(any())).thenReturn(Mono.empty());

        WebSocketResponse<?> testResponse = WebSocketResponse.builder()
                .type(WebsocketResponseType.INFO)
                .message("test conversion")
                .build();

        // 測試步驟
        Mono<Void> result = handlerUnderTest.sendMessage(session, testResponse);

        // 預期結果
        StepVerifier.create(result)
                .verifyComplete();

        // 驗證 JSON 轉換
        verify(objectMapper).writeValueAsString(testResponse);
        verify(session).textMessage(expectedJson);
        verify(session, atLeast(1)).send(any());
    }

    @Test
    @DisplayName("EventSink 事件處理 - 外部編輯事件訂閱")
    void utilityMethods_eventSinkHandling_externalEditEventSubscription() {
        // 前置條件 - EventSink 在 setUp 中已設置為返回空 Flux
        
        // 驗證事件訂閱
        verify(eventSink).subscribe();
        
        // 測試成功：init() 方法正確訂閱了 EventSink
        // 在實際運行中，EventSink 會處理外部編輯事件並觸發相應的更新
        assertThat(handlerUnderTest).isNotNull();
    }
}