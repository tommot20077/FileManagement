package xyz.dowob.filemanagement.component.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.FileStrategy;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.dto.file.FileMetadata;
import xyz.dowob.filemanagement.dto.file.UploadChunkDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;
import xyz.dowob.filemanagement.service.ServiceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
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
    private static final ConcurrentHashMap<Long, WebSocketSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    static {
        clearInactiveSession();
    }

    private final ObjectMapper objectMapper;

    private final FileStrategy fileStrategy;

    private final ValidationService validationService;

    private final UserService userService;


    /**
     * 清除未活躍的 WebSocket 會話
     */
    public static void clearInactiveSession() {
        try (ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor()) {
            service.scheduleAtFixedRate(() -> USER_SESSION_MAP.entrySet().removeIf(entry -> !entry.getValue().isOpen()),
                                        5,
                                        5,
                                        java.util.concurrent.TimeUnit.MINUTES);
        }
    }

    /**
     * 處理當前 WebSocket 會話並將用戶 ID 與會話對應
     *
     * @param session WebSocket 會話
     *
     * @return Mono<Void>
     */
    // todo 訊息包含 userId 、 type 、data
    @Override
    @NonNull
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        CustomWebSocketSession customSession = (CustomWebSocketSession) session;
        return session.receive().flatMap(webSocketMessage -> {
            try {
                JsonNode jsonNode = objectMapper.readTree(webSocketMessage.getPayloadAsText());
                Long userId = Long.parseLong(customSession.getUserId());
                String type = convertJsonToObject(jsonNode.get("type"), String.class).orElseThrow(() -> new ValidationException(
                        ValidationException.ErrorCode.REQUEST_IS_INVALID,
                        "type"));
                USER_SESSION_MAP.put(userId, customSession);
                return switch (type) {
                    case "initialUpload" -> handleInitialUpload(userId, customSession, jsonNode);
                    case "bufferUpload" -> handleBufferUpload(customSession, jsonNode);
                    default -> {
                        ApiResponseDTO<?> response = createResponse(customSession.getHandshakeInfo().getUri().getPath(),
                                                                    400,
                                                                    "未知的請求類型",
                                                                    null);
                        yield sendMessage(customSession, response);
                    }
                };
            } catch (JsonProcessingException | ValidationException e) {
                return Mono.error(e);
            }
        }).then();
    }

    Logger log = LoggerFactory.getLogger(FileUploadWebSocketHandler.class);
    private Mono<Void> handleInitialUpload(Long userId, WebSocketSession session, JsonNode jsonNode) {
        Optional<FileMetadata> fileMetadataOptional = convertJsonToObject(jsonNode.get("data"), FileMetadata.class);
        log.info("fileMetadataOptional: {}", fileMetadataOptional);
        return fileMetadataOptional
                .map(fileMetadata -> userService
                        .getById(userId)
                        .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, userId.toString())))
                        .flatMap(user -> validationService
                                .validateFileMetadataDTO(fileMetadata)
                                .then(fileStrategy.getFileService(FileEnum.IMAGE).uploadFile(fileMetadata, user))
                                .flatMap(transferResponseDTO -> {
                                    ApiResponseDTO<?> response = createResponse(session.getHandshakeInfo().getUri().getPath(),
                                                                                null,
                                                                                transferResponseDTO);
                                    if (transferResponseDTO.getIsFinished()) {
                                        response.setMessage("上傳任務完成");
                                    } else {
                                        response.setMessage("初始化上傳任務成功");
                                    }
                                    return sendMessage(session, response);
                                })))
                .orElseGet(() -> Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "data")))
                .onErrorResume(ValidationException.class, e -> {
                    String errorMessage = String.format("建立上傳任務失敗: %s", e.getMessage());
                    int responseCode = e.getErrorCode().getCode();
                    return sendMessage(session,
                                       createResponse(session.getHandshakeInfo().getUri().getPath(), responseCode, errorMessage, null));
                });
    }

    private Mono<Void> handleBufferUpload(WebSocketSession session, JsonNode jsonNode) {
        Optional<UploadChunkDTO> uploadChunkDTO = convertJsonToObject(jsonNode.get("data"), UploadChunkDTO.class);
        return uploadChunkDTO
                .map(chunkDTO -> fileStrategy.getFileService(FileEnum.IMAGE).uploadFileChunk(chunkDTO).flatMap(transferResponseDTO -> {
                    ApiResponseDTO<?> response = createResponse(session.getHandshakeInfo().getUri().getPath(), null, transferResponseDTO);
                    if (transferResponseDTO.getIsFinished()) {
                        response.setMessage("上傳任務完成");
                    } else {
                        response.setMessage("分塊上傳成功");
                    }
                    return sendMessage(session, response);
                }))
                .orElseGet(() -> Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "data")))
                .onErrorResume(ValidationException.class, e -> {
                    String errorMessage = String.format("上傳失敗: %s", e.getMessage());
                    return sendMessage(session, createResponse(session.getHandshakeInfo().getUri().getPath(), 400, errorMessage, null));
                });
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
        WebSocketSession session = USER_SESSION_MAP.get(userId);
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
        return Flux
                .fromIterable(USER_SESSION_MAP.values())
                .filter(WebSocketSession::isOpen)
                .flatMap(session -> sendMessage(session, message))
                .then();
    }

    /**
     * 移除用戶 ID 對應的 WebSocket 會話
     *
     * @param userId 用戶 ID
     */
    public Mono<Void> removeSession(String userId) {
        WebSocketSession session = USER_SESSION_MAP.remove(userId);
        return session.close();
    }

    private <T> Optional<T> convertJsonToObject(JsonNode node, Class<T> clazz) {
        try {
            return Optional.ofNullable(objectMapper.treeToValue(node, clazz));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private <T> Optional<T> convertJsonToObject(String json, Class<T> clazz) {
        try {
            return Optional.ofNullable(objectMapper.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private Map<String, Class<?>> initParameterNames() {
        Map<String, Class<?>> map = new HashMap<>();
        map.put("userId", String.class);
        map.put("type", String.class);
        return map;
    }
}

