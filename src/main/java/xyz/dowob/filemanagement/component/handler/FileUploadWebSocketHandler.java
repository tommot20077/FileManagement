package xyz.dowob.filemanagement.component.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadChunkDTO;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 文件上傳 WebSocket 處理器，用於處理文件上傳任務
 * 實現 WebSocketHandler 接口，並使用 Spring WebFlux 的 Mono 和 Flux 來處理非阻塞的請求
 * 以及 ResponseUnity 接口來統一響應格式
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileUploadWebSocketHandler
 * @description
 * @create 2024-10-04 23:41
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
public class FileUploadWebSocketHandler implements WebSocketHandler, ResponseUnity {
    /**
     * 當前用戶與 WebSocket 會話的映射
     */
    private static final ConcurrentHashMap<Long, WebSocketSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    static {
        clearInactiveSession();
    }


    /**
     * ObjectMapper 用於 JSON 資料的序列化與反序列化
     */
    private final ObjectMapper objectMapper;

    /**
     * 檔案處理策略模式
     */
    private final FileServiceStrategy fileServiceStrategy;

    /**
     * 用戶限制器策略模式
     */
    private final UserLimiterStrategy userLimiterStrategy;

    /**
     * 驗證服務
     */
    private final ValidationService validationService;

    /**
     * 用戶服務
     */
    private final UserService userService;

    /**
     * 清除未活躍的 WebSocket 會話
     */
    public static void clearInactiveSession() {
        try (ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor()) {
            service.scheduleAtFixedRate(() -> USER_SESSION_MAP.entrySet().removeIf(entry -> !entry.getValue().isOpen()),
                                        5,
                                        5,
                                        java.util.concurrent.TimeUnit.MINUTES
            );
        }
    }


    /**
     * 處理當前 WebSocket 會話並將用戶 ID 與會話對應
     *
     * @param session WebSocket 會話
     *
     * @return Mono<Void>
     */
    @Override
    @NonNull
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        CustomWebSocketSession customSession = (CustomWebSocketSession) session;
        return session.receive().flatMap(webSocketMessage -> {
            try {
                JsonNode jsonNode = objectMapper.readTree(webSocketMessage.getPayloadAsText());
                Long userId = Long.parseLong(customSession.getUserId());
                String type = convertJsonToObject(jsonNode.get("type"),
                                                  String.class
                ).orElseThrow(() -> new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "type"));
                USER_SESSION_MAP.put(userId, customSession);
                return switch (type) {
                    case "initialUpload" -> handleInitialUpload(userId, customSession, jsonNode);
                    case "bufferUpload" -> handleBufferUpload(customSession, jsonNode);
                    default -> {
                        ApiResponseDTO<?> response = createResponse(customSession.getHandshakeInfo().getUri().getPath(), 400, "未知的請求類型", null);
                        yield sendMessage(customSession, response);
                    }
                };
            } catch (JsonProcessingException | ValidationException e) {
                return Mono.error(e);
            }
        }).then();
    }


    /**
     * 處理初始化上傳任務，將文件元數據保存到數據庫
     * 當接收到註冊任務請求時，先檢查用戶以及其限制器是否符合要求
     * 然後判斷是否直接完成上傳任務，若是則直接返回完成的響應
     * 沒有則建立上傳任務並返回初始化成功的響應
     * 若中途發生錯誤，則返回錯誤響應
     *
     * @param userId   用戶 ID
     * @param session  WebSocket 會話
     * @param jsonNode JSON 資料
     *
     * @return Mono<Void>
     */
    private Mono<Void> handleInitialUpload(Long userId, WebSocketSession session, JsonNode jsonNode) {
        Optional<FileMetadataDTO> fileMetadataOptional = convertJsonToObject(jsonNode.get("data"), FileMetadataDTO.class);
        return fileMetadataOptional
                .map(fileMetadata -> userService
                        .getById(userId)
                        .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, userId.toString())))
                        .flatMap(user -> {
                            UserLimiter userLimiter = userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER);
                            if (!userLimiter.tryAcquire(user.getId())) {
                                return Mono.error(new LimitationException(LimitationException.ErrorCode.USER_EXCEED_LIMIT,
                                                                          UserLimiterEnum.USER_UPLOAD_LIMITER.getError()
                                ));
                            }
                            return validationService
                                    .validateFileMetadataDTO(fileMetadata, user)
                                    .then(fileServiceStrategy.getFileService().uploadFile(fileMetadata, user))
                                    .flatMap(transferResponseDTO -> {
                                        ApiResponseDTO<?> response = createResponse(session.getHandshakeInfo().getUri().getPath(),
                                                                                    null,
                                                                                    transferResponseDTO
                                        );
                                        if (transferResponseDTO.getIsFinished()) {
                                            response.setMessage("上傳任務完成");
                                        } else {
                                            response.setMessage("初始化上傳任務成功");
                                        }
                                        return sendMessage(session, response);
                                    })
                                    .doFinally(signalType -> userLimiter.release(user.getId()));
                        }))
                .orElseGet(() -> Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "data")))
                .onErrorResume(ValidationException.class, e -> {
                    String errorMessage = String.format("建立上傳任務失敗: %s", e.getMessage());
                    int responseCode = e.getErrorCode().getCode();
                    return sendMessage(session, createResponse(session.getHandshakeInfo().getUri().getPath(), responseCode, errorMessage, null));
                });
    }


    /**
     * 將 JSON 資料轉換為指定類型的物件
     * 當 JSON 資料無法轉換時，返回空的 Optional
     *
     * @param node  JSON 資料
     * @param clazz 類型
     * @param <T>   類型
     *
     * @return Optional<T>
     */
    private <T> Optional<T> convertJsonToObject(JsonNode node, Class<T> clazz) {
        try {
            return Optional.ofNullable(objectMapper.treeToValue(node, clazz));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }


    /**
     * 發送消息給指定用戶，依照用戶 ID 查找對應的 WebSocket 會話
     *
     * @param userId  用戶 ID
     * @param message 消息
     *
     * @return Mono<Void>
     */
    public Mono<Void> sendMessage(String userId, Object message) {
        WebSocketSession session = USER_SESSION_MAP.get(Long.parseLong(userId));
        return sendMessage(session, message);
    }


    /**
     * 發送消息給指定用戶，使用 WebSocket 會話
     *
     * @param session WebSocket 會話
     * @param message 將消息轉換為 JSON 字符串發送
     *
     * @return Mono<Void>
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
     * 傳送訊息給所有連線的用戶
     *
     * @param message 訊息內容
     */
    public Mono<Void> broadcast(Object message) {
        return Flux.fromIterable(USER_SESSION_MAP.values()).filter(WebSocketSession::isOpen).flatMap(session -> sendMessage(session, message)).then();
    }


    /**
     * 移除用戶 ID 對應的 WebSocket 會話
     *
     * @param userId 用戶 ID
     */
    public Mono<Void> removeSession(String userId) {
        WebSocketSession session = USER_SESSION_MAP.remove(Long.parseLong(userId));
        return session.close();
    }


    /**
     * 處理分塊上傳任務，解析 JSON 資料並將交給檔案服務進行處理
     * 最後返回上傳結果的響應
     *
     * @param session  WebSocket 會話
     * @param jsonNode JSON 資料
     *
     * @return Mono<Void>
     */
    private Mono<Void> handleBufferUpload(WebSocketSession session, JsonNode jsonNode) {
        Optional<UploadChunkDTO> uploadChunkDTO = convertJsonToObject(jsonNode.get("data"), UploadChunkDTO.class);
        return uploadChunkDTO
                .map(chunkDTO -> fileServiceStrategy.getFileService().uploadFileChunk(chunkDTO).flatMap(transferResponseDTO -> {
                    ApiResponseDTO<?> response = createResponse(session.getHandshakeInfo().getUri().getPath(), null, transferResponseDTO);
                    String message = transferResponseDTO.getIsFinished() ? "上傳任務完成" : "分塊上傳成功";
                    response.setMessage(message);
                    return sendMessage(session, response);
                }))
                .orElseGet(() -> Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "data")))
                .onErrorResume(Exception.class, e -> {
                    String errorMessage = String.format("分塊上傳失敗: %s", e.getMessage());
                    return sendMessage(session, createResponse(session.getHandshakeInfo().getUri().getPath(), 400, errorMessage, null));
                });
    }


    /**
     * 將 JSON 字符串轉換為指定類型的物件
     *
     * @param json  JSON 字符串
     * @param clazz 類型
     * @param <T>   類型
     *
     * @return Optional<T>
     */
    private <T> Optional<T> convertJsonToObject(String json, Class<T> clazz) {
        try {
            return Optional.ofNullable(objectMapper.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }


    /**
     * 初始化參數名稱
     *
     * @return Map<String, Class < ?>>
     */
    private Map<String, Class<?>> initParameterNames() {
        Map<String, Class<?>> map = new HashMap<>();
        map.put("userId", String.class);
        map.put("type", String.class);
        return map;
    }
}

