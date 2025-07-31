package xyz.dowob.filemanagement.component.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.PermissionEnum;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadChunkDTO;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基於 WebSocket 的反應式檔案上傳處理器，支援分塊上傳和即時進度追蹤。
 *
 * <p>本處理器實現非阻塞的檔案上傳機制，透過 WebSocket 協定提供雙向即時通訊能力。
 * 支援大檔案分塊上傳，有效降低記憶體使用並提升上傳穩定性。每個上傳會話都受到使用者限制器控制，
 * 防止濫用系統資源。整合檔案驗證機制，確保上傳內容的安全性和合規性。</p>
 *
 * <p>採用反應式程式設計模型，利用 Mono 和 Flux 實現非阻塞 I/O 操作。
 * 自動管理會話生命週期，包括非活躍連線的清理和資源釋放。提供即時的上傳狀態回饋，
 * 支援初始化上傳和分塊上傳兩種操作模式。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@RecordLevel(LogLevelEnum.DEBUG)
public class FileUploadWebSocketHandler implements WebSocketHandler, ResponseUnity {
    /**
     * 使用者 ID 與 WebSocket 會話的並發安全映射表。
     *
     * <p>維護活躍使用者與其對應 WebSocket 連線的映射關係，支援多執行緒並發存取。
     * 使用 ConcurrentHashMap 確保執行緒安全性，防止並發修改時的資料競爭問題。</p>
     */
    private static final ConcurrentHashMap<Long, WebSocketSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    static {
        clearInactiveSession();
    }


    /**
     * JSON 序列化與反序列化處理器。
     *
     * <p>負責處理 WebSocket 訊息的 JSON 格式轉換，包括接收訊息的反序列化
     * 和回傳訊息的序列化操作。</p>
     */
    private final ObjectMapper objectMapper;

    /**
     * 檔案服務策略選擇器。
     *
     * <p>根據檔案類型和操作需求選擇適當的檔案處理服務實作，
     * 支援不同類型檔案的上傳、處理和儲存策略。</p>
     */
    private final FileServiceStrategy fileServiceStrategy;

    /**
     * 使用者限制器策略選擇器。
     *
     * <p>提供不同類型的使用者行為限制機制，包括上傳頻率限制、
     * 並發連線數限制等，防止系統資源濫用。</p>
     */
    private final UserLimiterStrategy userLimiterStrategy;

    /**
     * 資料驗證服務。
     *
     * <p>負責驗證檔案元資料、使用者權限和上傳請求的合法性，
     * 確保系統安全性和資料完整性。</p>
     */
    private final ValidationService validationService;


    /**
     * 清除非活躍的 WebSocket 會話，定期檢查並移除已關閉的連線。
     *
     * <p>使用定時執行器每五分鐘檢查一次會話映射表，移除已關閉的 WebSocket 連線，
     * 防止記憶體洩漏和資源浪費。該方法在類別初始化時自動啟動清理程序。</p>
     */
    public static void clearInactiveSession() {
        try (ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor()) {
            service.scheduleAtFixedRate(() -> USER_SESSION_MAP.entrySet().removeIf(entry -> !entry.getValue().isOpen()), 5, 5, TimeUnit.MINUTES);
        }
    }


    /**
     * WebSocket 連線處理核心方法，管理檔案上傳會話生命週期。
     *
     * <p>處理上傳相關的 WebSocket 訊息，包括初始化上傳和分塊上傳。
     * 驗證使用者權限和上傳限制，確保上傳操作的安全性。</p>
     *
     * @param session WebSocket 會話物件
     * @return 表示會話處理完成的 Mono
     */
    @NonNull
    @Override
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        CustomWebSocketSession customSession = (CustomWebSocketSession) session;
        LogUnity.info(customSession, "WebSocket連接成功，session: %s", customSession.getAttributes());
        return session
                .receive()
                .flatMap(webSocketMessage -> Mono.fromCallable(() -> {
                    JsonNode jsonNode = objectMapper.readTree(webSocketMessage.getPayloadAsText());
                    User user = customSession.getUser();
                    if (user == null || !user.getRole().hasPermissions(PermissionEnum.UPLOAD)) {
                        throw new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED);
                    }

                    String type = convertJsonToObject(jsonNode.get("type"),
                                                      String.class
                    ).orElseThrow(() -> new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "type"));
                    USER_SESSION_MAP.put(user.getId(), customSession);
                    return Tuples.of(user, type, jsonNode);
                }).onErrorMap(JsonProcessingException.class, e -> new ValidationException(ValidationException.ErrorCode.INVALID_JSON_CONTENT, e)))
                .flatMap(tuple3 -> {
                    return switch (tuple3.getT2()) {
                        case "initialUpload" -> handleInitialUpload(tuple3.getT1(), customSession, tuple3.getT3());
                        case "bufferUpload" -> handleBufferUpload(customSession, tuple3.getT3());
                        default -> Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "type"));
                    };
                })
                .onErrorContinue((e, o) -> {
                    CloseStatus closeStatus;
                    ApiResponseDTO<?> response;
                    if (e instanceof ValidationException validation) {
                        response = createApiResponse(customSession, validation.getErrorCode().getCode(), validation.getMessage(), null);
                        closeStatus = CloseStatus.NORMAL.withReason("終止當前連線");
                    } else {
                        response = createApiResponse(customSession, 500, "伺服器內部錯誤", null);
                        closeStatus = CloseStatus.SERVER_ERROR.withReason("伺服器內部錯誤");
                    }
                    sendMessage(customSession, response).then(customSession.close(closeStatus)).subscribeOn(Schedulers.boundedElastic()).subscribe();
                })
                .doFinally(signal -> removeSession(customSession.getUser().getId().toString()).subscribeOn(Schedulers.boundedElastic()).subscribe())
                .then();
    }


    /**
     * 處理分塊上傳任務，解析分塊資料並透過檔案服務進行處理。
     *
     * <p>從 JSON 節點中提取分塊上傳資料，驗證後交由檔案服務策略處理。
     * 處理完成後回傳上傳進度和狀態的響應訊息給客戶端。支援大檔案的分塊傳輸，
     * 降低單次傳輸的記憶體佔用。</p>
     *
     * @param session  當前的 WebSocket 會話物件
     * @param jsonNode 包含分塊上傳資料的 JSON 節點
     * @return 表示處理完成的 Mono
     */
    private Mono<Void> handleBufferUpload(WebSocketSession session, JsonNode jsonNode) {
        Optional<UploadChunkDTO> uploadChunkDTO = convertJsonToObject(jsonNode.get("data"), UploadChunkDTO.class);
        return uploadChunkDTO
                .map(chunkDTO -> fileServiceStrategy.getFileService().uploadFileChunk(chunkDTO).flatMap(uploadResponseDTO -> {
                    ApiResponseDTO<?> response = createApiResponse(session.getHandshakeInfo().getUri().getPath(), null, uploadResponseDTO);
                    String message = uploadResponseDTO.getIsFinished() ? "上傳任務完成" : "分塊上傳成功";
                    response.setMessage(message);
                    return sendMessage(session, response);
                }))
                .orElseGet(() -> Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "data")))
                .onErrorResume(Exception.class, e -> {
                    String errorMessage = String.format("分塊上傳失敗: %s", e.getMessage());
                    return sendMessage(session, createApiResponse(session.getHandshakeInfo().getUri().getPath(), 400, errorMessage, null));
                });
    }


    /**
     * 處理上傳初始化請求，建立上傳任務和驗證檔案元資料。
     *
     * <p>執行以下操作步驟：
     * <ol>
     *   <li>獲取使用者上傳限制器的許可</li>
     *   <li>驗證檔案元資料的合法性</li>
     *   <li>建立上傳任務或直接完成上傳</li>
     *   <li>釋放使用者限制器資源</li>
     * </ol>
     * </p>
     *
     * @param user     使用者實體
     * @param session  WebSocket 會話物件
     * @param jsonNode 包含檔案元資料的 JSON 節點
     * @return 表示處理完成的 Mono
     */
    private Mono<Void> handleInitialUpload(User user, WebSocketSession session, JsonNode jsonNode) {
        return Mono.defer(() -> {
            LogUnity.info(session, "用戶 %s 嘗試上傳檔案，獲取上傳任務的憑證", user.getId());
            Optional<FileMetadataDTO> fileMetadataOptional = convertJsonToObject(jsonNode.get("data"), FileMetadataDTO.class);
            if (fileMetadataOptional.isEmpty()) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "data"));
            }
            FileMetadataDTO fileMetadata = fileMetadataOptional.get();
            UserLimiter userLimiter = userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER);
            return userLimiter.tryAcquire(user.getId()).flatMap(acquired -> {
                LogUnity.info(session, "用戶 %s 嘗試上傳檔案，獲取上傳任務的憑證: %s", user.getId(), acquired);
                if (!acquired) {
                    return Mono.error(new LimitationException(LimitationException.ErrorCode.USER_EXCEED_LIMIT,
                                                              UserLimiterEnum.USER_UPLOAD_LIMITER.getError()
                    ));
                }
                return validationService
                        .validateFileMetadataDTO(fileMetadata, user)
                        .then(fileServiceStrategy.getFileService().uploadFile(fileMetadata, user))
                        .flatMap(uploadResponseDTO -> {
                            ApiResponseDTO<?> response = createApiResponse(session.getHandshakeInfo().getUri().getPath(), null, uploadResponseDTO);
                            String message = uploadResponseDTO.getIsFinished() ? "上傳任務完成" : "初始化上傳任務成功";
                            response.setMessage(message);
                            return sendMessage(session, response);
                        })
                        .doFinally(signalType -> userLimiter.release(user.getId()).subscribeOn(Schedulers.boundedElastic()).subscribe());
            });
        }).onErrorResume(ValidationException.class, e -> {
            String errorMessage = String.format("建立上傳任務失敗: %s", e.getMessage());
            int responseCode = e.getErrorCode().getCode();
            return sendMessage(session, createApiResponse(session.getHandshakeInfo().getUri().getPath(), responseCode, errorMessage, null));
        });
    }


    /**
     * 將 JSON 節點轉換為指定類型的物件，提供型別安全的轉換機制。
     *
     * <p>使用 ObjectMapper 將 JsonNode 轉換為目標類型物件。當轉換失敗或發生例外時，
     * 回傳空的 Optional 而非拋出例外，確保程式穩定性。</p>
     *
     * @param node  要轉換的 JSON 節點
     * @param clazz 目標類型的 Class 物件
     * @param <T>   目標類型參數
     * @return 包含轉換結果的 Optional，轉換失敗時為空
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
     * 透過 WebSocket 連線發送訊息給客戶端。
     *
     * <p>自動將訊息物件序列化為 JSON 格式，並檢查連線狀態。
     * 若連線不可用或序列化失敗，會適當處理錯誤情況。</p>
     *
     * @param session WebSocket 會話物件
     * @param message 要發送的訊息物件
     * @return 表示發送完成的 Mono
     */
    public Mono<Void> sendMessage(WebSocketSession session, Object message) {
        String messageStr;
        try {
            messageStr = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            messageStr = message.toString();
        }
        if (session != null && session.isOpen()) {
            return session.send(Mono.just(session.textMessage(messageStr)));
        }
        return Mono.empty();
    }


    /**
     * 移除指定使用者的 WebSocket 會話並關閉連線。
     *
     * <p>從會話映射表中移除指定使用者 ID 對應的 WebSocket 會話，
     * 並主動關閉該連線以釋放資源。通常在使用者登出或連線異常時調用。</p>
     *
     * @param userId 使用者 ID 字串
     * @return 表示關閉完成的 Mono
     */
    public Mono<Void> removeSession(String userId) {
        WebSocketSession session = USER_SESSION_MAP.remove(Long.parseLong(userId));
        return session.close();
    }

    /**
     * 發送訊息給指定使用者，透過使用者 ID 查找對應的 WebSocket 會話。
     *
     * <p>根據使用者 ID 從會話映射表中查找對應的 WebSocket 連線，
     * 並透過該連線發送訊息。若找不到對應的會話，則不執行任何操作。</p>
     *
     * @param userId  目標使用者的 ID 字串
     * @param message 要發送的訊息物件
     * @return 表示發送完成的 Mono
     */
    public Mono<Void> sendMessage(String userId, Object message) {
        WebSocketSession session = USER_SESSION_MAP.get(Long.parseLong(userId));
        return sendMessage(session, message);
    }

    /**
     * 廣播訊息給所有活躍的 WebSocket 連線。
     *
     * <p>遍歷所有已建立的 WebSocket 會話，過濾出仍然開啟的連線，
     * 並向這些連線發送相同的訊息。適用於系統通知或全域更新的場景。</p>
     *
     * @param message 要廣播的訊息物件
     * @return 表示廣播完成的 Mono
     */
    public Mono<Void> broadcast(Object message) {
        return Flux.fromIterable(USER_SESSION_MAP.values()).filter(WebSocketSession::isOpen).flatMap(session -> sendMessage(session, message)).then();
    }

    /**
     * 將 JSON 字串轉換為指定類型的物件，提供安全的反序列化機制。
     *
     * <p>使用 ObjectMapper 將 JSON 字串反序列化為目標類型物件。當反序列化失敗時，
     * 回傳空的 Optional 而非拋出例外，避免程式中斷並提供優雅的錯誤處理。</p>
     *
     * @param json  要轉換的 JSON 字串
     * @param clazz 目標類型的 Class 物件
     * @param <T>   目標類型參數
     * @return 包含轉換結果的 Optional，轉換失敗時為空
     */
    @SkipRecord
    private <T> Optional<T> convertJsonToObject(String json, Class<T> clazz) {
        try {
            return Optional.ofNullable(objectMapper.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }


    /**
     * 初始化參數名稱與類型的映射表，用於參數驗證和處理。
     *
     * <p>建立參數名稱與對應類型的映射關係，提供給訊息處理流程使用。
     * 主要用於 WebSocket 訊息的參數驗證和類型檢查。</p>
     *
     * @return 包含參數名稱與類型映射的 Map
     */
    @SkipRecord
    private Map<String, Class<?>> initParameterNames() {
        Map<String, Class<?>> map = new HashMap<>();
        map.put("userId", String.class);
        map.put("type", String.class);
        return map;
    }
}
