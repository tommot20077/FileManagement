package xyz.dowob.filemanagement.component.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
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
 * 線上檔案編輯的 WebSocket 處理器，用於同步更新檔案內容和歷史紀錄
 * 當前有多個用戶編輯同一檔案時，會使用 WebSocket 來實現即時更新
 * 使用戶可以即時看到其他用戶的編輯內容
 * 此類實現了 WebSocketHandler 接口，並且使用了 Spring WebFlux 的 WebSocket 支持
 * 並使用 ResponseUnity 來統一處理 WebSocket 的響應
 *
 * @author yuan
 * @program FileManagement
 * @ClassName OnlineFileWebSocketHandler
 * @create 2025/5/9
 * @Version 1.0
 **/
@Component
public class OnlineFileWebSocketHandler implements WebSocketHandler, ResponseUnity {
    /**
     * 單個文件的最大連線數量，設定為 Integer.MAX_VALUE (後續會添加到配置文件中)
     * 這是為了避免在多用戶編輯同一文件時，導致連線數量過多
     */
    private static final int MAX_SESSION_COUNT_PER_FILE = Integer.MAX_VALUE;

    /**
     * 用於存儲所有文件的編輯會話的 Map，key 為文件 ID，value 為 FileEditSessionMap
     */
    private static final ConcurrentHashMap<Long, FileEditSessionMap> FILE_EDIT_SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * ObjectMapper 用於將對象轉換為 JSON 字符串
     */
    private final ObjectMapper objectMapper;

    /**
     * OnlineFileServiceImpl 用於處理線上檔案的業務邏輯
     */
    private final OnlineFileServiceImpl onlineFileService;

    /**
     * ValidationService 用於驗證請求的業務邏輯
     */
    private final ValidationService validationService;

    /**
     * PermissionService 用於處理檔案的權限驗證
     */
    private final PermissionService<UserFileMetadata> permissionService;

    /**
     * FilePermissionRuleManager 用於處理檔案的權限規則
     */
    private final FilePermissionRuleManager filePermissionRuleManager;

    /**
     * 事件發送器，用於接收API請求的編輯消息，當使用API進行編輯時會通知訂閱進行更新
     */
    private final EventSink<FileEditedMessage> eventSink;


    /**
     * 構造函數，初始化 OnlineFileWebSocketHandler
     * 會檢查配置文件中的 WebSocket 路徑前綴和編輯文件的 WebSocket 路徑是否為空
     *
     * @param objectMapper              用於將對象轉換為 JSON 字符串
     * @param onlineFileService         用於處理線上檔案的業務邏輯
     * @param validationService         用於驗證請求的業務邏輯
     * @param permissionService         用於處理檔案的權限驗證
     * @param filePermissionRuleManager 用於處理檔案的權限規則
     * @param fileProperties            用於獲取檔案的配置屬性
     * @param eventSink                 事件發送器
     */
    public OnlineFileWebSocketHandler(ObjectMapper objectMapper, OnlineFileServiceImpl onlineFileService, ValidationService validationService, PermissionService<UserFileMetadata> permissionService, FilePermissionRuleManager filePermissionRuleManager, FileProperties fileProperties, EventSink<FileEditedMessage> eventSink) {
        this.objectMapper = objectMapper;
        this.onlineFileService = onlineFileService;
        this.validationService = validationService;
        this.permissionService = permissionService;
        this.filePermissionRuleManager = filePermissionRuleManager;
        this.eventSink = eventSink;

        Assert.hasText(fileProperties.getUpload().getEditOnlineFileWebSocketPath(), "請求編輯文件的 WebSocket 路徑不能為空");
    }


    /**
     * 初始化方法，訂閱事件發送器的消息，當接收到編輯消息時，會處理編輯結果
     * 並將編輯結果發送給所有連線的用戶
     */
    @PostConstruct
    public void init() {
        eventSink.subscribe().publishOn(Schedulers.boundedElastic()).flatMap(message -> {
            LogUnity.debug("接收到文件編輯消息: %s", message);
            if (message.type() == null) {
                return Mono.empty();
            }

            return handleEditResult(message.user(), message.type(), message.fileMetadata());
        }).subscribe();
    }

    /**
     * 處理編輯結果，根據編輯類型進行不同的處理
     * 當編輯類型為
     * - EDIT_CONTENT 時，會獲取檔案內容並發送給所有連線的用戶
     * - DELETE_HISTORY_RECORD 時，會獲取檔案歷史紀錄並發送給所有連線的用戶
     * - REVERT_HISTORY_RECORD 以及 BUILD_HISTORY_RECORD 時，會同時獲取檔案內容和歷史紀錄並發送給所有連線的用戶
     *
     * @param user         用戶
     * @param editType     編輯類型
     * @param fileMetadata 檔案元數據
     *
     * @return Mono<Void>
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
            LogUnity.error("OnlineFileWebSocketHandler 在處理更新消息時發生錯誤", e);
            return Mono.empty();
        });
    }

    /**
     * 廣播訊息給該檔案的所有連線
     *
     * @param fileId  檔案ID
     * @param message 要廣播的訊息
     *
     * @return Mono<Void>
     */
    public Mono<Void> boastMessage(Long fileId, Object message) {
        Collection<WebSocketSession> sessions = getAllSessions(fileId);
        return boastMessage(sessions, message);
    }

    /**
     * 獲取該檔案的所有連線Session
     *
     * @param fileId 檔案ID
     *
     * @return 所有連線Session
     */
    private Set<WebSocketSession> getAllSessions(Long fileId) {
        FileEditSessionMap fileEditSessionMap = FILE_EDIT_SESSION_MAP.get(fileId);
        if (fileEditSessionMap != null) {
            return fileEditSessionMap.sessionMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * 廣播消息給所有連線的用戶
     * 當 WebSocketSession 不可用時，會返回 Mono.empty()
     * 這邊使用併發的方式來發送消息，提升性能
     *
     * @param sessions 用戶的 WebSocketSession
     * @param message  消息內容
     *
     * @return Mono<Void>
     */
    public Mono<Void> boastMessage(Collection<WebSocketSession> sessions, Object message) {
        return formatDataToJson(message).flatMap(jsonString -> {
            List<Mono<Void>> sessionList = sessions
                    .stream()
                    .map(session -> checkSessionStatus(session).flatMap(checkedSession -> checkedSession
                            .send(Mono.just(session.textMessage(jsonString)))
                            .retryWhen(getRetryPolicy())
                            .onErrorResume(e -> {
                                LogUnity.warn("發送消息失敗，WebSocketSession: %s", session);
                                return Mono.empty();
                            })))
                    .toList();
            return Mono.when(sessionList);
        });
    }

    /**
     * 將對象轉換為 Json 字符串，若轉換中發生錯誤，將拋出 ProcessException
     *
     * @param data 對象
     *
     * @return Mono<String> Json 字符串
     */
    @SkipRecord
    private Mono<String> formatDataToJson(Object data) {
        return Mono
                .fromCallable(() -> objectMapper.writeValueAsString(data))
                .onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
    }

    /**
     * 檢查 WebSocketSession 是否可用
     * 當 WebSocketSession 不可用時，會返回 Mono.empty()
     *
     * @param session WebSocketSession
     *
     * @return Mono<WebSocketSession>
     */
    private Mono<WebSocketSession> checkSessionStatus(WebSocketSession session) {
        if (session == null || !session.isOpen()) {
            LogUnity.warn("無法發送消息，WebSocketSession: %s 不可用", session);
            return Mono.empty();
        }
        return Mono.just(session);
    }

    /**
     * 獲取重試策略，當發生 IOException 時，會重試 3 次，每次延遲 500 毫秒
     *
     * @return 重試策略
     */
    private static Retry getRetryPolicy() {
        return Retry
                .fixedDelay(3, Duration.ofMillis(500))
                .filter(ex -> ex instanceof IOException)
                .doAfterRetry(retrySignal -> LogUnity.warn("WebSocket連接異常，正在重試..."));
    }

    /**
     * 處理 WebSocket 連接，當連接成功時，會檢查是否具有該檔案的權限以及是否達到最大連線數量
     * 並將用戶的編輯會話添加到 FILE_EDIT_SESSION_MAP 中
     * 當後續有編輯請求時，會處理編輯請求並發送編輯結果給所有連線的用戶
     * 途中若是遇到錯誤，會將錯誤信息發送給用戶並根據類型進行錯誤處理
     *
     * @param session 用戶連線
     *
     * @return Mono<Void>
     */
    @NotNull
    @Override
    public Mono<Void> handle(@NotNull WebSocketSession session) {
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
     * 處理編輯結果，根據編輯類型進行不同的處理
     * 此為重載方法，使用 FileEditBO 來處理編輯結果
     *
     * @param user       用戶
     * @param fileEditBO 編輯請求的 DTO
     *
     * @return Mono<Void>
     */
    private Mono<Void> handleEditResult(User user, FileEditBO fileEditBO) {
        UserFileMetadata fileMetadata = fileEditBO.getUserFileMetadata();
        EditTypeEnum editType = fileEditBO.getFileEditDTO().getEditType();
        return handleEditResult(user, editType, fileMetadata);
    }

    /**
     * 處理錯誤，根據錯誤類型進行不同的處理
     * 當錯誤類型為 ValidationException 時，會將錯誤信息發送給用戶並關閉連線
     * 當錯誤類型為 AbortedException 時，會將錯誤信息發送給用戶並關閉連線
     * 當錯誤類型為其他異常時，會將錯誤信息發送給用戶並關閉連線
     *
     * @param session 用戶的 WebSocketSession
     * @param error   錯誤信息
     * @param status  關閉狀態
     *
     * @return Mono<Void>
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
     * 獲取 WebSocketSession 的接收消息
     * 當接收到消息時，會將消息轉換為 JsonNode
     * 並返回一個 Flux<JsonNode>
     *
     * @param session WebSocketSession
     *
     * @return Flux<JsonNode> 傳入的訊息流
     */
    private Flux<JsonNode> getReceiveMessage(WebSocketSession session) {
        return session.receive().flatMap(message -> {
            return Mono
                    .fromCallable(() -> objectMapper.readTree(message.getPayloadAsText()))
                    .onErrorMap(e -> new ValidationException(ValidationException.ErrorCode.INVALID_JSON_CONTENT));
        });
    }

    /**
     * 添加新的編輯會話到 FILE_EDIT_SESSION_MAP 中
     * 當用戶的角色為 VISITOR 時，會將最大連線數量設置為 Integer.MAX_VALUE
     * 否則會將最大連線數量設置為 MAX_SESSION_COUNT_PER_FILE
     * 當達到最大連線數量時，會發送消息給用戶並關閉連線
     *
     * @param user    用戶
     * @param fileId  檔案 ID
     * @param session WebSocketSession
     *
     * @return Mono<Boolean> 是否添加成功
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
            WebSocketResponse<?> response = createWebSocketResponse(WebsocketResponseType.INFO, "已達到該文件的最大連線數");
            LogUnity.info(session, "已達到該文件的最大連線數");
            return sendMessage(session, response).then(session.close()).thenReturn(false);
        }

        Set<WebSocketSession> editors = fileEditSessionMap.sessionMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        return boastMessage(editors,
                            createWebSocketResponse(WebSocketFileEditTypeEnum.EDITOR_COUNT_UPDATED, "當前在線人數", getFileEditSessionCount(fileId))
        ).then(Mono.just(true));

    }

    /**
     * 移除舊的編輯會話
     * 當檔案的編輯會話數量為 0 時，會將檔案從 FILE_EDIT_SESSION_MAP 中移除
     * 當連線數量不為 0 時，會廣播消息給所有連線的用戶新的會話數量
     *
     * @param fileId  檔案 ID
     * @param UserId  用戶 ID
     * @param session WebSocketSession
     *
     * @return Mono<Void>
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
     * 發送消息給 WebSocketSession
     * 當 WebSocketSession 不可用時，會返回 Mono.empty()
     *
     * @param session WebSocketSession
     * @param message 消息內容
     *
     * @return Mono<Void>
     */
    public Mono<Void> sendMessage(WebSocketSession session, Object message) {
        if (session == null || !session.isOpen()) {
            LogUnity.warn("無法發送消息，WebSocketSession: %s 不可用", session);
            return Mono.empty();
        }

        return Mono
                .fromCallable(() -> objectMapper.writeValueAsString(message))
                .onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e))
                .flatMap(jsonString -> session.send(Mono.just(session.textMessage(jsonString))).retryWhen(getRetryPolicy()));
    }

    /**
     * 處理傳入的訊息並根據類型選取相對應的操作處理方式以及結果
     *
     * @param session     用戶的 WebSocketSession
     * @param fileEditDTO 編輯請求的 DTO
     *
     * @return Mono<Void>
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
     * 獲取該檔案的編輯會話數量，當檔案未有任何編輯會話時，會返回 0
     *
     * @param fileId 檔案 ID
     *
     * @return 編輯會話數量
     */
    private int getFileEditSessionCount(Long fileId) {
        FileEditSessionMap fileEditSessionMap = FILE_EDIT_SESSION_MAP.get(fileId);
        if (fileEditSessionMap != null) {
            return fileEditSessionMap.getSessionCount();
        }
        return 0;
    }

    /**
     * 檢查用戶是否具有該檔案的編輯權限
     * 當用戶沒有編輯權限時，會返回 Mono.error()
     *
     * @param user     用戶
     * @param fileId   檔案 ID
     * @param editType 編輯類型
     *
     * @return Mono<UserFileMetadata> 檔案元數據
     */
    private Mono<UserFileMetadata> checkFilePermission(User user, Long fileId, EditTypeEnum editType) {
        if (editType == EditTypeEnum.EDIT_METADATA) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.UNSUPPORTED_OPERATION));
        }
        List<Permission<UserFileMetadata>> rules = Collections.singletonList(filePermissionRuleManager.getAllowShared());
        return permissionService.validateUserPermission(user, fileId, rules);
    }

    /**
     * 清除 WebSocketSession 的資源
     * 當 WebSocketSession 可用時，會關閉連線並返回 Mono.empty()
     * 當 WebSocketSession 不可用時，會返回 Mono.empty()
     *
     * @param webSocketSession WebSocketSession 用戶的連線
     *
     * @return Mono<Void>
     */
    private Mono<Void> cleanSessionResource(@NotNull WebSocketSession webSocketSession) {
        if (webSocketSession.isOpen()) {
            return webSocketSession.close(CloseStatus.NORMAL.withReason("終止當前連線"));
        }
        LogUnity.info(webSocketSession, "關閉 %s 的連接", webSocketSession);
        return Mono.empty();
    }

    /**
     * 轉換 JsonNode 為對象，若轉換中發生錯誤，則返回 Optional.empty()
     *
     * @param node  JsonNode
     * @param clazz 對象類型
     * @param <T>   對象類型
     *
     * @return Optional<T> 對象
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
     * WebSocket 編輯類型的枚舉類
     */
    public enum WebSocketFileEditTypeEnum {
        /**
         * 檔案內容更新
         */
        FILE_CONTENT_UPDATED,

        /**
         * 檔案歷史更新
         */
        FILE_HISTORY_UPDATED,

        /**
         * 編輯人數更新
         */
        EDITOR_COUNT_UPDATED,
    }


    /**
     * WebSocket 編輯會話的 Map，用於存儲所有編輯會話以及用戶的連線數量
     */
    private static class FileEditSessionMap {
        /**
         * 用戶連線儲存的 Map， key 為用戶 ID，value 為 WebSocketSession 的集合
         */
        private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();

        /**
         * 用戶連線數量
         */
        private final AtomicInteger sessionCount = new AtomicInteger(0);


        /**
         * 添加新的編輯會話到 sessionMap 中
         * 當用戶的連線數量達到限制時，會返回 false
         *
         * @param userId            用戶 ID
         * @param session           WebSocketSession
         * @param limitSessionCount 限制的連線數量
         *
         * @return boolean 是否添加成功
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
         * 移除舊的編輯會話
         * 當用戶的連線數量為 0 時，會將用戶從 sessionMap 中移除
         *
         * @param userId        用戶 ID
         * @param socketSession WebSocketSession
         */
        public void removeSession(Long userId, WebSocketSession socketSession) {
            Set<WebSocketSession> sessions = sessionMap.get(userId);
            sessions.removeIf(session -> session.equals(socketSession));
            sessionCount.decrementAndGet();
        }


        /**
         * 獲取用戶的連線數量
         *
         * @return int 用戶的連線數量
         */
        public int getSessionCount() {
            return sessionCount.get();
        }
    }
}
