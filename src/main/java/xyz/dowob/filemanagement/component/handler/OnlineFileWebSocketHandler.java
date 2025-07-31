package xyz.dowob.filemanagement.component.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.channel.AbortedException;
import reactor.util.retry.Retry;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.event.EventSink;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.*;
import xyz.dowob.filemanagement.data.event.FileEditedMessage;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileVersionDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.data.response.WebSocketResponse;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceImpl.fileservice.OnlineFileServiceImpl;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 基於 WebSocket 的線上檔案協作編輯處理器，支援多使用者同步編輯功能。
 *
 * <p>本處理器實現即時協作編輯機制，當多個使用者同時編輯同一檔案時，透過 WebSocket 連線同步更新內容。
 * 支援檔案內容變更、版本歷史管理和線上編輯人數統計等功能。整合檔案權限驗證機制，
 * 確保只有具備適當權限的使用者能夠參與協作編輯。</p>
 *
 * <p>採用事件驅動架構，透過 EventSink 訂閱檔案編輯事件，自動廣播更新通知給所有連線的編輯者。
 * 支援檔案內容編輯、歷史記錄操作和版本回溯等多種編輯類型。提供重試機制和錯誤處理，
 * 確保在網路不穩定情況下的服務可靠性。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
public class OnlineFileWebSocketHandler implements WebSocketHandler, ResponseUnity {
    /**
     * 單個檔案的最大連線數量限制。
     *
     * <p>目前設定為 Integer.MAX_VALUE，表示理論上不限制連線數量。此設定主要用於控制
     * 多使用者同時編輯同一檔案時的連線管理，避免資源過度消耗。未來會考慮將此值移至
     * 設定檔案中進行動態配置。</p>
     *
     * @since 1.0
     */
    private static final int MAX_SESSION_COUNT_PER_FILE = Integer.MAX_VALUE;

    /**
     * 全域檔案編輯會話映射表。
     *
     * <p>使用執行緒安全的 ConcurrentHashMap 儲存所有正在進行協作編輯的檔案會話。
     * 鍵為檔案ID，值為包含該檔案所有編輯者連線的 FileEditSessionMap 物件。
     * 當檔案編輯會話結束時會自動清理對應的條目。</p>
     *
     * @since 1.0
     */
    private static final ConcurrentHashMap<Long, FileEditSessionMap> FILE_EDIT_SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * JSON 序列化與反序列化處理器。
     *
     * <p>用於處理 WebSocket 訊息的 JSON 格式轉換，包括將 Java 物件序列化為 JSON 字串
     * 以及將接收到的 JSON 字串反序列化為對應的 DTO 物件。</p>
     *
     * @since 1.0
     */
    private final ObjectMapper objectMapper;

    /**
     * 線上檔案業務邏輯服務。
     *
     * <p>提供線上檔案的核心業務功能，包括檔案內容的讀取、編輯、版本管理和歷史記錄操作。
     * 透過此服務處理協作編輯過程中的檔案操作需求。</p>
     *
     * @since 1.0
     */
    private final OnlineFileServiceImpl onlineFileService;

    /**
     * 資料驗證服務。
     *
     * <p>負責驗證來自 WebSocket 的編輯請求資料完整性和有效性，包括檔案編輯 DTO 的格式驗證、
     * 檔案類型檢查等。確保所有編輯操作都基於有效的資料進行。</p>
     *
     * @since 1.0
     */
    private final ValidationService validationService;

    /**
     * 檔案權限驗證服務。
     *
     * <p>處理使用者對特定檔案的存取權限驗證，確保只有具備適當權限的使用者
     * 能夠參與協作編輯。支援多種權限規則的組合驗證。</p>
     *
     * @since 1.0
     */
    private final PermissionService<UserFileMetadata> permissionService;

    /**
     * 檔案權限規則管理器。
     *
     * <p>提供檔案權限規則的取得和管理功能，包括共享檔案的存取規則定義。
     * 透過此管理器可以動態取得適用於不同情境的權限驗證規則。</p>
     *
     * @since 1.0
     */
    private final FilePermissionRuleManager filePermissionRuleManager;

    /**
     * 檔案編輯事件接收器。
     *
     * <p>訂閱來自 API 請求的檔案編輯事件，當使用者透過 REST API 對檔案進行編輯時，
     * 此事件接收器會收到通知並將更新廣播給所有線上的協作編輯者，實現跨介面的即時同步。</p>
     *
     * @since 1.0
     */
    private final EventSink<FileEditedMessage> eventSink;


    /**
     * 建構方法，初始化線上檔案編輯 WebSocket 處理器。
     *
     * <p>驗證必要的設定參數，初始化所有依賴組件。</p>
     *
     * @param objectMapper              JSON 序列化處理器
     * @param onlineFileService         線上檔案業務服務
     * @param validationService         資料驗證服務
     * @param permissionService         檔案權限驗證服務
     * @param filePermissionRuleManager 檔案權限規則管理器
     * @param fileProperties            檔案相關設定屬性
     * @param eventSink                 檔案編輯事件發送器
     * @throws IllegalArgumentException 當必要的設定參數缺失時
     */
    public OnlineFileWebSocketHandler(ObjectMapper objectMapper, OnlineFileServiceImpl onlineFileService, ValidationService validationService, PermissionService<UserFileMetadata> permissionService, FilePermissionRuleManager filePermissionRuleManager, FileProperties fileProperties, EventSink<FileEditedMessage> eventSink) {
        this.objectMapper = objectMapper;
        this.onlineFileService = onlineFileService;
        this.validationService = validationService;
        this.permissionService = permissionService;
        this.filePermissionRuleManager = filePermissionRuleManager;
        this.eventSink = eventSink;

        Assert.hasText(fileProperties.getUpload().getEditOnlineFileWebSocketPath(), "請求編輯檔案的 WebSocket 路徑不能為空");
    }


    /**
     * 元件初始化後的事件訂閱設定。
     *
     * <p>在 Spring 容器完成依賴注入後自動執行，建立對檔案編輯事件的訂閱機制。
     * 監聽來自 EventSink 的檔案編輯訊息，並根據編輯類型將更新廣播給對應檔案的
     * 所有線上協作編輯者，確保跨介面的即時同步效果。</p>
     *
     * <p>訂閱過程採用非阻塞方式執行，使用 boundedElastic 調度器處理事件，
     * 避免影響主執行緒的效能。</p>
     *
     * @since 1.0
     */
    @PostConstruct
    public void init() {
        eventSink.subscribe().publishOn(Schedulers.boundedElastic()).flatMap(message -> {
            LogUnity.debug("接收到檔案編輯訊息: %s", message);
            if (message.type() == null) {
                return Mono.empty();
            }

            return handleEditResult(message.user(), message.type(), message.fileMetadata());
        }).subscribe();
    }

    /**
     * 根據編輯類型處理檔案編輯結果並廣播更新。
     *
     * <p>此方法會根據不同的編輯類型執行對應的處理邏輯：</p>
     * <ul>
     *   <li>EDIT_CONTENT: 獲取最新檔案內容並廣播給所有編輯者</li>
     *   <li>DELETE_HISTORY_RECORD: 獲取更新後的歷史記錄列表並廣播</li>
     *   <li>REVERT_HISTORY_RECORD/BUILD_HISTORY_RECORD: 同時獲取檔案內容和歷史記錄並廣播</li>
     * </ul>
     *
     * <p>廣播過程採用非阻塞方式，確保單一編輯者的操作能即時同步給其他協作者。
     * 當處理過程中發生錯誤時，會記錄錯誤訊息但不中斷其他編輯者的正常操作。</p>
     *
     * @param user         執行編輯操作的使用者
     * @param editType     編輯操作類型，決定需要更新的內容範圍
     * @param fileMetadata 被編輯檔案的元資料
     * @return 表示處理完成的 Mono 信號
     * @since 1.0
     */
    private Mono<Void> handleEditResult(User user, EditTypeEnum editType, UserFileMetadata fileMetadata) {
        Mono<Void> updateContent = onlineFileService.downloadFile(fileMetadata, user, DownloadActionEnum.PREVIEW.name()).flatMap(fileDataBO -> {
            WebSocketResponse<?> response = createWebSocketResponse(WebSocketFileEditTypeEnum.FILE_CONTENT_UPDATED,
                                                                    "檔案內容更新",
                                                                    fileDataBO.getContent()
            );
            return boastMessage(fileMetadata.getId(), response);
        });

        Mono<Void> updateHistory = onlineFileService.getFileVersionList(user, fileMetadata, 1, null).flatMap(fileVersionList -> {
            WebSocketResponse<?> response = createWebSocketResponse(WebSocketFileEditTypeEnum.FILE_HISTORY_UPDATED, "檔案歷史更新", fileVersionList);
            return boastMessage(fileMetadata.getId(), response);
        });

        Mono<Void> action = switch (editType) {
            case EDIT_CONTENT -> updateContent;
            case DELETE_HISTORY_RECORD -> updateHistory;
            case REVERT_HISTORY_RECORD, BUILD_HISTORY_RECORD -> Mono.when(updateHistory, updateContent);
            default -> Mono.empty();
        };
        return action.onErrorResume(e -> {
            LogUnity.error("OnlineFileWebSocketHandler 在處理更新訊息時發生錯誤", e);
            return Mono.empty();
        });
    }

    /**
     * 向指定檔案的所有協作編輯者廣播訊息。
     *
     * <p>獲取該檔案所有活躍的 WebSocket 連線，並將指定訊息同時發送給所有編輯者。
     * 此方法是實現即時協作同步的核心功能之一。</p>
     *
     * @param fileId  目標檔案的唯一識別碼
     * @param message 要廣播的訊息物件，將被序列化為 JSON 格式發送
     * @return 表示廣播操作完成的 Mono 信號
     * @since 1.0
     */
    public Mono<Void> boastMessage(Long fileId, Object message) {
        Collection<WebSocketSession> sessions = getAllSessions(fileId);
        return boastMessage(sessions, message);
    }

    /**
     * 獲取指定檔案的所有活躍 WebSocket 連線。
     *
     * <p>從檔案編輯會話映射表中提取指定檔案的所有編輯者連線，
     * 將分散在不同使用者下的連線整合為一個統一的集合。</p>
     *
     * @param fileId 檔案的唯一識別碼
     * @return 包含該檔案所有編輯者連線的集合，若檔案無活躍編輯者則回傳空集合
     * @since 1.0
     */
    private Set<WebSocketSession> getAllSessions(Long fileId) {
        FileEditSessionMap fileEditSessionMap = FILE_EDIT_SESSION_MAP.get(fileId);
        if (fileEditSessionMap != null) {
            return fileEditSessionMap.sessionMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * 向指定的 WebSocket 連線集合並行廣播訊息。
     *
     * <p>使用併發方式同時向多個 WebSocket 連線發送訊息，提升廣播效能。
     * 對於不可用的連線會自動跳過，確保其他正常連線不受影響。
     * 每個連線的發送操作都配置了重試機制以提高可靠性。</p>
     *
     * <p>訊息發送過程中若遇到網路異常，會自動重試最多3次，
     * 每次重試間隔500毫秒。若重試後仍失敗，會記錄警告但不影響其他連線。</p>
     *
     * @param sessions 目標 WebSocket 連線集合
     * @param message  要廣播的訊息物件，將被序列化為 JSON 格式
     * @return 表示所有廣播操作完成的 Mono 信號
     * @since 1.0
     */
    public Mono<Void> boastMessage(Collection<WebSocketSession> sessions, Object message) {
        return formatDataToJson(message).flatMap(jsonString -> {
            List<Mono<Void>> sessionList = sessions
                    .stream()
                    .map(session -> checkSessionStatus(session).flatMap(checkedSession -> checkedSession
                            .send(Mono.just(session.textMessage(jsonString)))
                            .retryWhen(getRetryPolicy())
                            .onErrorResume(e -> {
                                LogUnity.warn("發送訊息失敗，WebSocketSession: %s", session);
                                return Mono.empty();
                            })))
                    .toList();
            return Mono.when(sessionList);
        });
    }

    /**
     * 將 Java 物件序列化為 JSON 字串。
     *
     * <p>使用 ObjectMapper 將任意 Java 物件轉換為 JSON 格式的字串，
     * 供 WebSocket 訊息傳輸使用。轉換過程在獨立的可呼叫函數中執行，
     * 避免阻塞主執行緒。</p>
     *
     * @param data 待序列化的 Java 物件
     * @return 包含 JSON 字串的 Mono，轉換失敗時會發出 ProcessException
     * @since 1.0
     */
    @SkipRecord
    private Mono<String> formatDataToJson(Object data) {
        return Mono
                .fromCallable(() -> objectMapper.writeValueAsString(data))
                .onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
    }

    /**
     * 驗證 WebSocket 連線的可用性。
     *
     * <p>檢查連線是否為 null 或已關閉狀態。對於不可用的連線會記錄警告訊息
     * 並回傳空的 Mono，確保後續的訊息發送操作能夠正確處理無效連線。</p>
     *
     * @param session 待檢查的 WebSocket 連線
     * @return 若連線可用則回傳包含該連線的 Mono，否則回傳空 Mono
     * @since 1.0
     */
    private Mono<WebSocketSession> checkSessionStatus(WebSocketSession session) {
        if (session == null || !session.isOpen()) {
            LogUnity.warn("無法發送訊息，WebSocketSession: %s 不可用", session);
            return Mono.empty();
        }
        return Mono.just(session);
    }

    /**
     * 建立 WebSocket 訊息發送的重試策略。
     *
     * <p>針對 IOException 類型的異常配置固定延遲重試機制，最多重試3次，
     * 每次重試間隔500毫秒。此策略主要用於處理網路連線不穩定導致的暫時性失敗。</p>
     *
     * <p>重試過程中會記錄警告訊息，便於監控和除錯。對於非 IOException 類型的異常
     * 不會觸發重試，避免無意義的重複操作。</p>
     *
     * @return 配置好的重試策略物件
     * @since 1.0
     */
    private static Retry getRetryPolicy() {
        return Retry
                .fixedDelay(3, Duration.ofMillis(500))
                .filter(ex -> ex instanceof IOException)
                .doAfterRetry(retrySignal -> LogUnity.warn("WebSocket連接異常，正在重試..."));
    }

    /**
     * 處理新的 WebSocket 連線請求並建立協作編輯會話。
     *
     * <p>此方法為 WebSocket 連線的主要處理入口，執行以下關鍵步驟：</p>
     * <ol>
     *   <li>驗證使用者對目標檔案的編輯權限</li>
     *   <li>檢查並控制同一檔案的最大連線數量</li>
     *   <li>將新連線加入檔案編輯會話映射表</li>
     *   <li>發送初始檔案內容和版本歷史給新編輯者</li>
     *   <li>建立持續的訊息接收處理機制</li>
     *   <li>處理連線生命週期管理和資源清理</li>
     * </ol>
     *
     * <p>連線建立後會持續監聽來自客戶端的編輯請求，並即時處理和廣播更新。
     * 當連線發生異常或正常關閉時，會自動清理相關資源並更新線上編輯者數量。</p>
     *
     * @param session WebSocket 連線會話，必須為 CustomWebSocketSession 類型且包含使用者資訊
     * @return 表示連線處理完成的 Mono 信號
     * @throws ClassCastException 當 session 不是 CustomWebSocketSession 類型時
     * @since 1.0
     */
    @Nonnull
    @Override
    public Mono<Void> handle(@Nonnull WebSocketSession session) {
        CustomWebSocketSession customSession = (CustomWebSocketSession) session;
        LogUnity.info(customSession, "WebSocket連接成功，session: %s", customSession.getAttributes());
        Map<String, Object> customAttributes = customSession.getAttributes();
        User user = customSession.getUser();
        Long fileId = (Long) customAttributes.get("fileId");

        return checkFilePermission(customSession.getUser(), fileId, null)
                .flatMap(file -> addNewSession(user, fileId, customSession).flatMap(isAdd -> {
                    if (!isAdd) {
                        return Mono.empty();
                    }
                    return validationService.validateFileType(file, FileEnum.ONLINE_DOCUMENT).then(Mono.defer(() -> {
                        Mono<UserFileDataBO> downloadMono = onlineFileService.downloadFile(file, user, DownloadActionEnum.PREVIEW.name());
                        Mono<PagedResponseDTO<FileVersionDTO>> getVersionMono = onlineFileService.getFileVersionList(user, file, 1, null);
                        return Mono.zip(downloadMono, getVersionMono);
                    })).flatMap(tuple2 -> {
                        Map<String, Object> fileDetailMap = Map.of("content", tuple2.getT1().getContent(), "filename", tuple2.getT1().getFilename());
                        WebSocketResponse<?> fileContentResponse = createWebSocketResponse(WebSocketFileEditTypeEnum.FILE_CONTENT_UPDATED,
                                                                                           "檔案內容更新",
                                                                                           fileDetailMap
                        );
                        WebSocketResponse<?> fileVersionResponse = createWebSocketResponse(WebSocketFileEditTypeEnum.FILE_HISTORY_UPDATED,
                                                                                           "檔案歷史更新",
                                                                                           tuple2.getT2()
                        );

                        return Mono
                                .when(sendMessage(session, fileContentResponse), sendMessage(session, fileVersionResponse))
                                .then(getReceiveMessage(customSession).flatMap(jsonNode -> {
                                    Optional<FileEditDTO> optionalFileEditDTO = convertJsonToObject(jsonNode, FileEditDTO.class);
                                    if (optionalFileEditDTO.isEmpty()) {
                                        return sendMessage(customSession, createWebSocketResponse(WebsocketResponseType.ERROR, "無法解析請求"));
                                    }
                                    optionalFileEditDTO.get().setFileId(fileId.toString());
                                    return handleMessage(customSession, optionalFileEditDTO.get());
                                }).onErrorContinue((e, o) -> {
                                    handleError(customSession, e, null).subscribeOn(Schedulers.boundedElastic()).subscribe();
                                }).doFinally(signal -> {
                                    removeOldSession(fileId, user.getId(), customSession)
                                            .then(cleanSessionResource(customSession))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .subscribe();
                                }).then());
                    });
                }))
                .onErrorResume((e) -> handleError(customSession, e, null));
    }

    /**
     * 處理檔案編輯結果的重載方法。
     *
     * <p>從 FileEditBO 物件中提取檔案元資料和編輯類型，
     * 然後委託給主要的 handleEditResult 方法進行處理。
     * 此重載方法簡化了從業務物件到核心處理邏輯的轉換過程。</p>
     *
     * @param user       執行編輯操作的使用者
     * @param fileEditBO 包含編輯請求詳細資訊的業務物件
     * @return 表示編輯結果處理完成的 Mono 信號
     * @since 1.0
     */
    private Mono<Void> handleEditResult(User user, FileEditBO fileEditBO) {
        UserFileMetadata fileMetadata = fileEditBO.getUserFileMetadata();
        EditTypeEnum editType = fileEditBO.getFileEditDTO().getEditType();
        return handleEditResult(user, editType, fileMetadata);
    }

    /**
     * 處理 WebSocket 連線過程中的錯誤狀況。
     *
     * <p>根據不同的異常類型採取適當的處理策略：</p>
     * <ul>
     *   <li>ValidationException: 使用者輸入驗證錯誤，發送錯誤訊息後正常關閉連線</li>
     *   <li>AbortedException: 客戶端主動中止連線，僅記錄訊息不進行額外處理</li>
     *   <li>其他異常: 伺服器內部錯誤，發送通用錯誤訊息並關閉連線</li>
     * </ul>
     *
     * <p>所有錯誤處理過程都會記錄適當的日誌訊息，便於問題追蹤和系統監控。
     * 在發送錯誤訊息給客戶端後會優雅地關閉 WebSocket 連線。</p>
     *
     * @param session 發生錯誤的 WebSocket 連線會話
     * @param error   具體的錯誤異常物件
     * @param status  指定的連線關閉狀態，若為 null 則使用預設的 SERVER_ERROR 狀態
     * @return 表示錯誤處理完成的 Mono 信號
     * @since 1.0
     */
    private Mono<Void> handleError(CustomWebSocketSession session, Throwable error, CloseStatus status) {
        WebsocketResponseType type = WebsocketResponseType.CONNECTION_ERROR;
        String message = "伺服器內部錯誤，請稍後再試";
        CloseStatus closeStatus = status == null ? CloseStatus.SERVER_ERROR : status;

        if (error instanceof ValidationException e) {
            message = e.getMessage();
            closeStatus = CloseStatus.NORMAL;
            LogUnity.info(session, "關閉連線: %s 用戶驗證錯誤: %s", session, e.getMessage());
        } else if (error instanceof AbortedException abortedException) {
            LogUnity.info(session, "連線已由客戶端中止: %s", session, abortedException.getMessage());
            return Mono.empty();
        } else {
            LogUnity.error(session, "關閉連線: %s WebSocket 發生未知錯誤", error, session);
        }

        WebSocketResponse<?> errorMsg = createWebSocketResponse(type, message);
        return sendMessage(session, errorMsg).then(session.close(closeStatus));
    }

    /**
     * 建立 WebSocket 訊息接收串流。
     *
     * <p>持續監聽來自客戶端的 WebSocket 訊息，並將文字格式的訊息
     * 解析為 JsonNode 物件。解析過程採用非阻塞方式，確保不會影響
     * 其他連線的訊息處理效能。</p>
     *
     * <p>當接收到無效的 JSON 格式訊息時，會拋出 ValidationException
     * 並由上層錯誤處理機制統一處理。</p>
     *
     * @param session 要監聽訊息的 WebSocket 連線
     * @return 持續發出 JsonNode 的 Flux 串流
     * @since 1.0
     */
    private Flux<JsonNode> getReceiveMessage(WebSocketSession session) {
        return session.receive().flatMap(message -> {
            return Mono
                    .fromCallable(() -> objectMapper.readTree(message.getPayloadAsText()))
                    .onErrorMap(e -> new ValidationException(ValidationException.ErrorCode.INVALID_JSON_CONTENT));
        });
    }

    /**
     * 將新的編輯會話加入檔案編輯映射表。
     *
     * <p>根據使用者角色設定不同的連線數量限制：</p>
     * <ul>
     *   <li>VISITOR 角色: 設定為 Integer.MAX_VALUE，實際上不限制連線數</li>
     *   <li>其他角色: 使用預設的 MAX_SESSION_COUNT_PER_FILE 限制</li>
     * </ul>
     *
     * <p>若達到連線數量限制，會向使用者發送提示訊息並關閉連線。
     * 成功加入後會向所有編輯者廣播最新的線上人數資訊。</p>
     *
     * @param user    要加入編輯會話的使用者
     * @param fileId  目標檔案的唯一識別碼
     * @param session 使用者的 WebSocket 連線會話
     * @return 包含加入結果的 Mono，true 表示成功加入，false 表示達到限制被拒絕
     * @since 1.0
     */
    private Mono<Boolean> addNewSession(User user, Long fileId, WebSocketSession session) {
        FileEditSessionMap fileEditSessionMap = FILE_EDIT_SESSION_MAP.computeIfAbsent(fileId, k -> new FileEditSessionMap());

        boolean isAdd;
        if (user.getRole() == RoleEnum.VISITOR) {
            isAdd = fileEditSessionMap.addSession(user.getId(), session, Integer.MAX_VALUE);
        } else {
            isAdd = fileEditSessionMap.addSession(user.getId(), session, MAX_SESSION_COUNT_PER_FILE);
        }

        if (!isAdd) {
            WebSocketResponse<?> response = createWebSocketResponse(WebsocketResponseType.INFO, "已達到該檔案的最大連線數");
            LogUnity.info(session, "已達到該檔案的最大連線數");
            return sendMessage(session, response).then(session.close()).thenReturn(false);
        }

        Set<WebSocketSession> editors = fileEditSessionMap.sessionMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        return boastMessage(editors,
                            createWebSocketResponse(WebSocketFileEditTypeEnum.EDITOR_COUNT_UPDATED, "當前在線人數", getFileEditSessionCount(fileId))
        ).then(Mono.just(true));

    }

    /**
     * 從檔案編輯映射表中移除編輯會話。
     *
     * <p>當使用者離開協作編輯時，清理其在檔案編輯會話中的連線記錄。
     * 若該檔案的所有編輯會話都已結束，會將整個檔案條目從映射表中移除，
     * 釋放記憶體資源。</p>
     *
     * <p>若仍有其他編輯者在線，會向剩餘的編輯者廣播更新後的線上人數，
     * 確保所有參與者都能即時了解當前的協作狀況。</p>
     *
     * @param fileId  目標檔案的唯一識別碼
     * @param UserId  要移除的使用者識別碼
     * @param session 要移除的 WebSocket 連線會話
     * @return 表示移除操作完成的 Mono 信號
     * @since 1.0
     */
    private Mono<Void> removeOldSession(Long fileId, Long UserId, WebSocketSession session) {
        FileEditSessionMap fileEditSessionMap = FILE_EDIT_SESSION_MAP.get(fileId);
        if (fileEditSessionMap != null) {
            fileEditSessionMap.removeSession(UserId, session);
            LogUnity.trace(session, "移除 %s 的連接", session);
            if (fileEditSessionMap.getSessionCount() == 0) {
                FILE_EDIT_SESSION_MAP.remove(fileId);
                return Mono.empty();
            }
            Set<WebSocketSession> editors = fileEditSessionMap.sessionMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            return boastMessage(editors,
                                createWebSocketResponse(WebSocketFileEditTypeEnum.EDITOR_COUNT_UPDATED,
                                                        "當前在線人數",
                                                        getFileEditSessionCount(fileId)
                                )
            );
        }
        return Mono.empty();
    }

    /**
     * 向指定的 WebSocket 連線發送訊息。
     *
     * <p>將 Java 物件序列化為 JSON 格式後透過 WebSocket 連線發送給客戶端。
     * 發送過程包含連線狀態檢查和重試機制，確保訊息傳遞的可靠性。</p>
     *
     * <p>對於不可用的連線會自動跳過並記錄警告訊息。發送失敗時會根據
     * 重試策略進行最多3次的重試嘗試。</p>
     *
     * @param session 目標 WebSocket 連線
     * @param message 要發送的訊息物件，將被序列化為 JSON 格式
     * @return 表示發送操作完成的 Mono 信號
     * @since 1.0
     */
    public Mono<Void> sendMessage(WebSocketSession session, Object message) {
        if (session == null || !session.isOpen()) {
            LogUnity.warn("無法發送訊息，WebSocketSession: %s 不可用", session);
            return Mono.empty();
        }

        return Mono
                .fromCallable(() -> objectMapper.writeValueAsString(message))
                .onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e))
                .flatMap(jsonString -> session.send(Mono.just(session.textMessage(jsonString))).retryWhen(getRetryPolicy()));
    }

    /**
     * 處理來自客戶端的檔案編輯請求訊息。
     *
     * <p>解析並驗證客戶端傳送的編輯請求，執行以下處理流程：</p>
     * <ol>
     *   <li>驗證編輯請求 DTO 的格式和內容有效性</li>
     *   <li>檢查使用者對目標檔案的編輯權限</li>
     *   <li>驗證檔案類型是否支援線上編輯</li>
     *   <li>執行具體的編輯操作</li>
     *   <li>處理編輯結果並廣播更新給其他編輯者</li>
     * </ol>
     *
     * <p>不支援 EDIT_METADATA 類型的編輯操作，會向客戶端回傳相應的提示訊息。
     * 所有驗證錯誤都會轉換為適當的錯誤回應發送給客戶端。</p>
     *
     * @param session     發送編輯請求的 WebSocket 連線會話
     * @param fileEditDTO 包含編輯操作詳細資訊的資料傳輸物件
     * @return 表示訊息處理完成的 Mono 信號
     * @since 1.0
     */
    private Mono<Void> handleMessage(CustomWebSocketSession session, FileEditDTO fileEditDTO) {
        User user = session.getUser();
        return validationService
                .validateEditFileDTO(fileEditDTO, false)
                .then(checkFilePermission(user, Long.valueOf(fileEditDTO.getFileId()), fileEditDTO.getEditType()))
                .flatMap(file -> {
                    if (fileEditDTO.getEditType() == null || fileEditDTO.getEditType() == EditTypeEnum.EDIT_METADATA) {
                        return sendMessage(session, createWebSocketResponse(WebsocketResponseType.INFO, "不支援的操作"));
                    }
                    FileEditBO fileEditBO = new FileEditBO(fileEditDTO);
                    fileEditBO.setUserFileMetadata(file);
                    return validationService
                            .validateFileType(file, FileEnum.ONLINE_DOCUMENT)
                            .then(onlineFileService.editFile(fileEditBO, user))
                            .then(handleEditResult(user, fileEditBO));
                })
                .onErrorResume(ValidationException.class,
                               e -> sendMessage(session, createWebSocketResponse(WebsocketResponseType.ERROR, e.getMessage()))
                );
    }

    /**
     * 獲取指定檔案的當前編輯會話總數。
     *
     * <p>統計該檔案所有線上編輯者的連線數量，用於向客戶端提供即時的
     * 協作人數資訊。當檔案尚無任何編輯會話時回傳0。</p>
     *
     * @param fileId 檔案的唯一識別碼
     * @return 該檔案當前的編輯會話總數
     * @since 1.0
     */
    private int getFileEditSessionCount(Long fileId) {
        FileEditSessionMap fileEditSessionMap = FILE_EDIT_SESSION_MAP.get(fileId);
        if (fileEditSessionMap != null) {
            return fileEditSessionMap.getSessionCount();
        }
        return 0;
    }

    /**
     * 驗證使用者對指定檔案的編輯權限。
     *
     * <p>檢查使用者是否具備對目標檔案進行指定類型編輯操作的權限。
     * 目前不支援 EDIT_METADATA 類型的編輯操作，若檢測到此類型會拋出異常。</p>
     *
     * <p>權限驗證使用共享檔案存取規則，確保只有具備適當權限的使用者
     * 能夠參與協作編輯。驗證通過後回傳完整的檔案元資料供後續操作使用。</p>
     *
     * @param user     要驗證權限的使用者
     * @param fileId   目標檔案的唯一識別碼
     * @param editType 請求的編輯操作類型，用於權限等級判斷
     * @return 包含檔案元資料的 Mono，權限驗證失敗時會發出錯誤信號
     * @since 1.0
     */
    private Mono<UserFileMetadata> checkFilePermission(User user, Long fileId, EditTypeEnum editType) {
        if (editType == EditTypeEnum.EDIT_METADATA) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.UNSUPPORTED_OPERATION));
        }
        List<Permission<UserFileMetadata>> rules = Collections.singletonList(filePermissionRuleManager.getAllowShared());
        return permissionService.validateUserPermission(user, fileId, rules);
    }

    /**
     * 清理 WebSocket 連線資源。
     *
     * <p>安全地關閉 WebSocket 連線並清理相關資源。若連線仍處於開啟狀態，
     * 會發送正常關閉信號並記錄相關資訊。對於已關閉的連線僅記錄清理動作。</p>
     *
     * <p>此方法確保連線資源的正確釋放，避免記憶體洩漏和連線累積問題。</p>
     *
     * @param webSocketSession 要清理的 WebSocket 連線會話
     * @return 表示清理操作完成的 Mono 信號
     * @since 1.0
     */
    private Mono<Void> cleanSessionResource(@NotNull WebSocketSession webSocketSession) {
        if (webSocketSession.isOpen()) {
            return webSocketSession.close(CloseStatus.NORMAL.withReason("終止當前連線"));
        }
        LogUnity.info(webSocketSession, "關閉 %s 的連接", webSocketSession);
        return Mono.empty();
    }

    /**
     * 將 JsonNode 反序列化為指定類型的 Java 物件。
     *
     * <p>使用 ObjectMapper 的 treeToValue 方法將 JSON 節點轉換為具體的 Java 物件。
     * 轉換過程中若發生任何異常，會回傳空的 Optional 而非拋出異常，
     * 便於上層程式進行優雅的錯誤處理。</p>
     *
     * @param node  要轉換的 JSON 節點
     * @param clazz 目標 Java 物件的類型
     * @param <T>   目標物件的泛型類型
     * @return 包含轉換結果的 Optional，轉換失敗時為空
     * @since 1.0
     */
    @SkipRecord
    private <T> Optional<T> convertJsonToObject(JsonNode node, Class<T> clazz) {
        try {
            return Optional.ofNullable(objectMapper.treeToValue(node, clazz));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }


    /**
     * WebSocket 檔案編輯事件類型列舉。
     *
     * <p>定義在 WebSocket 協作編輯過程中可能觸發的各種事件類型，
     * 用於區分不同的廣播訊息內容和客戶端處理邏輯。</p>
     *
     * @since 1.0
     */
    public enum WebSocketFileEditTypeEnum {
        /**
         * 檔案內容更新事件。
         *
         * <p>當檔案內容被編輯並保存後觸發，通知所有線上編輯者同步最新的檔案內容。</p>
         */
        FILE_CONTENT_UPDATED,

        /**
         * 檔案歷史記錄更新事件。
         *
         * <p>當檔案版本歷史發生變化時觸發，包括新增歷史記錄、刪除記錄或回滾操作。</p>
         */
        FILE_HISTORY_UPDATED,

        /**
         * 線上編輯人數更新事件。
         *
         * <p>當有編輯者加入或離開協作編輯時觸發，更新所有參與者的線上人數顯示。</p>
         */
        EDITOR_COUNT_UPDATED,
    }


    /**
     * 檔案編輯會話映射容器。
     *
     * <p>管理單一檔案的所有編輯會話，包括每個使用者的多重連線和總連線數統計。
     * 使用執行緒安全的資料結構確保在高併發環境下的正確性。</p>
     *
     * <p>此類別負責維護檔案編輯會話的生命週期，包括會話的新增、移除和數量統計，
     * 為協作編輯功能提供基礎的連線管理能力。</p>
     *
     * @since 1.0
     */
    private static class FileEditSessionMap {
        /**
         * 使用者連線會話映射表。
         *
         * <p>以使用者ID為鍵，儲存該使用者所有的 WebSocket 連線會話集合。
         * 支援單一使用者的多重連線（如多個瀏覽器分頁同時編輯同一檔案）。
         * 使用 ConcurrentHashMap 確保執行緒安全性。</p>
         *
         * @since 1.0
         */
        private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();

        /**
         * 檔案編輯會話總數計數器。
         *
         * <p>使用原子整數統計該檔案當前的總連線數量，包括所有使用者的所有連線。
         * 提供執行緒安全的計數操作，確保在併發環境下的準確性。</p>
         *
         * @since 1.0
         */
        private final AtomicInteger sessionCount = new AtomicInteger(0);


        /**
         * 新增編輯會話到映射表中。
         *
         * <p>將新的 WebSocket 連線加入指定使用者的會話集合中。若該使用者的連線數
         * 已達到指定限制，則拒絕新增並回傳 false。成功新增後會同步更新總連線數計數器。</p>
         *
         * <p>使用 computeIfAbsent 方法確保在併發環境下的安全性，
         * 避免多執行緒同時操作導致的資料不一致問題。</p>
         *
         * @param userId            使用者的唯一識別碼
         * @param session           要新增的 WebSocket 連線會話
         * @param limitSessionCount 該使用者允許的最大連線數量
         * @return true 表示成功新增，false 表示達到連線限制被拒絕
         * @since 1.0
         */
        public boolean addSession(Long userId, WebSocketSession session, int limitSessionCount) {
            Set<WebSocketSession> sessions = sessionMap.computeIfAbsent(userId, k -> new HashSet<>());
            if (sessions.size() >= limitSessionCount) {
                return false;
            }

            sessions.add(session);
            sessionCount.incrementAndGet();
            return true;
        }


        /**
         * 從映射表中移除指定的編輯會話。
         *
         * <p>從指定使用者的會話集合中移除對應的 WebSocket 連線。移除操作完成後
         * 會同步更新總連線數計數器。使用 removeIf 方法確保正確移除相等的會話物件。</p>
         *
         * <p>此方法不會檢查使用者會話集合是否變空，上層程式需要根據需要
         * 進行額外的清理操作。</p>
         *
         * @param userId        使用者的唯一識別碼
         * @param socketSession 要移除的 WebSocket 連線會話
         * @since 1.0
         */
        public void removeSession(Long userId, WebSocketSession socketSession) {
            Set<WebSocketSession> sessions = sessionMap.get(userId);
            sessions.removeIf(session -> session.equals(socketSession));
            sessionCount.decrementAndGet();
        }


        /**
         * 獲取當前檔案的總編輯會話數量。
         *
         * <p>回傳該檔案所有使用者的連線總數，用於統計當前協作編輯的參與人數。
         * 此數值包括同一使用者的多重連線。</p>
         *
         * @return 當前檔案的總編輯會話數量
         * @since 1.0
         */
        public int getSessionCount() {
            return sessionCount.get();
        }
    }
}
