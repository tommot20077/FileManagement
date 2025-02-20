package xyz.dowob.filemanagement.service.serviceImpl.fileservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.data.file.po.CustomPatchPO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.entity.UserOnlineFile;
import xyz.dowob.filemanagement.entity.UserOnlineFileHistory;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.*;
import xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 在線文件服務實現類，用於實現在線文件的上傳、下載、刪除、編輯等操作
 * 繼承了AbstractFileService，實現了AbstractFileService中的方法
 * 在線文件是指用戶在線編輯的文件，因為一般檔案存在GridFS中，所以在線文件的內容是保存在數據庫中的
 * 提供線上檔案的各種操作並支援歷史版本還原
 *
 * @author yuan
 * @program FileManagement
 * @ClassName OnlineFileServiceImpl
 * @create 2025/2/8
 * @Version 1.0
 **/
@Service
@FileHandlerType(FileEnum.ONLINE_DOCUMENT)
public class OnlineFileServiceImpl extends AbstractFileService {
    /**
     * 在線文件數據庫操作接口
     */
    private final UserOnlineFileRepository userOnlineFileRepository;
    /**
     * 在線文件歷史數據庫操作接口
     */
    private final UserOnlineFileHistoryRepository userOnlineFileHistoryRepository;
    /**
     * 對象映射器
     */
    private final ObjectMapper objectMapper;
    /**
     * 預設空內容的JSON
     */
    private final String EMPTY_CONTENT = "{\"delta\":[]}";

    public OnlineFileServiceImpl(ServerFileMetaRepository serverFileMetaRepository, UserFileMetaRepository userFileMetaRepository, UserRepository userRepository, RedisProvider redisProvider, GridFsProvider gridFsProvider, TransfersTasksManager transfersTasksManager, FileProperties fileProperties, CircuitBreakerConfig circuitBreakerConfig, FolderListTreeProvider folderListTreeProvider, UserOnlineFileRepository userOnlineFileRepository, UserOnlineFileHistoryRepository userOnlineFileHistoryRepository, ObjectMapper objectMapper, R2dbcEntityOperations entityOperations) {
        super(serverFileMetaRepository,
              userFileMetaRepository,
              userOnlineFileRepository,
              userRepository,
              redisProvider,
              gridFsProvider,
              transfersTasksManager,
              fileProperties,
              circuitBreakerConfig,
              folderListTreeProvider,
              entityOperations
        );
        this.userOnlineFileRepository = userOnlineFileRepository;
        this.userOnlineFileHistoryRepository = userOnlineFileHistoryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 下載文件
     *
     * @param fileId 文件ID
     * @param user   用戶
     *
     * @return 文件數據業務對象
     */
    @Override
    public Mono<UserFileDataBO> downloadFile(String fileId, User user) {
        return findUserOnlineFileById(fileId)
                .flatMap(userOnlineFile -> validateUserPermission(user, userOnlineFile, false).then(Mono.just(userOnlineFile)))
                .flatMap(userOnlineFile -> userFileMetaRepository.findById(userOnlineFile.getId().toString()).flatMap(userFileMetadata -> {
                    try {
                        EditorContentDTO content = objectMapper.readValue(userOnlineFile.getContent(), EditorContentDTO.class);
                        userFileMetadata.setLastAccessTime(LocalDateTime.now());
                        userFileMetaRepository.save(userFileMetadata).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        return Mono.just(new UserFileDataBO(userOnlineFile, userFileMetadata, content));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
                    }
                }));
    }

    /**
     * 上傳文件
     *
     * @param fileMetadataDTO 文件元數據
     * @param user            用戶
     *
     * @return 上傳結果
     */
    @Override
    public Mono<UploadResponseDTO> uploadFile(FileMetadataDTO fileMetadataDTO, User user) {
        fileMetadataDTO.setUser(user);
        return Mono.defer(() -> {
            if (fileMetadataDTO.getParentFolderId() != null) {
                return checkParentFolderId(fileMetadataDTO.getParentFolderId(), user);
            }
            return Mono.empty();
        }).then(Mono.defer(() -> {
            UserFileMetadata userFileMetadata = new UserFileMetadata();
            userFileMetadata.setUserId(user.getId());
            userFileMetadata.setFilename(fileMetadataDTO.getFilename() + ".onf");
            userFileMetadata.setParentFolderId(fileMetadataDTO.getParentFolderId());
            userFileMetadata.setUploadTime(LocalDateTime.now());
            userFileMetadata.setLastAccessTime(LocalDateTime.now());
            userFileMetadata.setFileType(FileEnum.ONLINE_DOCUMENT);
            userFileMetadata.setIsStar(false);
            return userFileMetaRepository
                    .save(userFileMetadata)
                    .flatMap(newUserFileMetadata -> {
                        UserOnlineFile userOnlineFile = new UserOnlineFile();
                        userOnlineFile.setId(newUserFileMetadata.getId());
                        userOnlineFile.setContent(EMPTY_CONTENT);
                        userOnlineFile.setFileSize(0L);
                        userOnlineFile.setLastModifiedBy(user.getId());
                        userOnlineFile.setCurrentSnapshotCount(0);
                        userOnlineFile.setLastHistoryVersion(0L);
                        userOnlineFile.setIsMatchHistory(true);
                        return Mono.just(userOnlineFile);
                    })
                    .flatMap(userOnlineFile -> userOnlineFileRepository
                            .insertWithId(userOnlineFile)
                            .then(createInitialHistory(userOnlineFile))
                            .then(cleanUserListCache(user.getId(), userFileMetadata.getParentFolderId()))
                            .thenReturn(UploadResponseDTO
                                                .builder()
                                                .progress(100.0)
                                                .isSuccess(true)
                                                .isFinished(true)
                                                .message("上傳成功")
                                                .build()));
        }));
    }

    /**
     * 刪除文件
     *
     * @param fileId 文件ID
     * @param user   用戶
     *
     * @return 空Mono
     */
    @Override
    public Mono<Void> deleteFile(String fileId, User user) {
        return super.deleteFile(fileId, user);
    }

    /**
     * 編輯文件
     *
     * @param fileEditDTO 編輯文件數據傳輸對象
     * @param user        用戶
     *
     * @return 空Mono
     */
    @Override
    public Mono<Void> editFile(FileEditDTO fileEditDTO, User user) {
        return findUserOnlineFileById(fileEditDTO.getFileId()).flatMap(userOnlineFile -> switch (fileEditDTO.getEditType()) {
            case EDIT_METADATA -> super.editFile(fileEditDTO, user);
            case EDIT_CONTENT -> validateUserPermission(user, userOnlineFile, false).then(saveContent(userOnlineFile, fileEditDTO, user));
            case BUILD_HISTORY_RECORD ->
                    validateUserPermission(user, userOnlineFile, false).then(BuildHistoryRecord(userOnlineFile, fileEditDTO, user));
            case REVERT_HISTORY_RECORD -> validateUserPermission(user, userOnlineFile, false).then(revertHistoryRecord(userOnlineFile,
                                                                                                                       fileEditDTO.getVersion(),
                                                                                                                       user.getId()
            ));
        });
    }

    @Override
    @HideOverLength
    public Mono<PagedResponseDTO<FileVersionDTO>> getFileVersionList(User user, String fileId, Integer page, Integer size) {
        int pageSize = Objects.requireNonNullElse(size, fileProperties.getGlobal().getPageSize());
        int currentPage = Math.max(1, Objects.requireNonNullElse(page, 1));
        int offset = (currentPage - 1) * pageSize;
        return findUserOnlineFileById(fileId).flatMap(userOnlineFile -> validateUserPermission(user, userOnlineFile, false).then(
                userOnlineFileHistoryRepository
                        .findAllByFileIdOrderByVersionDesc(userOnlineFile.getId())
                        .collectList()
                        .flatMap(historyList -> {
                            int totalElements = historyList.size();
                            List<FileVersionDTO> fileVersionDTOList = historyList
                                    .subList(offset, Math.min(offset + pageSize, totalElements))
                                    .stream()
                                    .map(FileVersionDTO::new)
                                    .toList();

                            PagedResponseDTO<FileVersionDTO> pagedResponseDTO = new PagedResponseDTO<>();
                            pagedResponseDTO.setTotalElements(totalElements);
                            pagedResponseDTO.setTotalPages((int) Math.ceil((double) totalElements / pageSize));
                            pagedResponseDTO.setCurrentPage(currentPage);
                            pagedResponseDTO.setPageSize(pageSize);
                            pagedResponseDTO.setData(fileVersionDTOList);
                            return Mono.just(pagedResponseDTO);
                        })));
    }


    /**
     * 查詢文件
     *
     * @param fileId 文件ID
     *
     * @return 文件數據業務對象
     */
    private Mono<UserOnlineFile> findUserOnlineFileById(String fileId) {
        return userOnlineFileRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)));
    }

    /**
     * 驗證用戶權限
     *
     * @param user           用戶
     * @param userOnlineFile 用戶在線文件
     * @param ownerOnly      是否只檢查擁有者權限
     *
     * @return 空Mono
     */
    private Mono<Void> validateUserPermission(User user, UserOnlineFile userOnlineFile, boolean ownerOnly) {
        return userFileMetaRepository
                .findById(userOnlineFile.getId().toString())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE,
                                                                  userOnlineFile.getId().toString()
                )))
                .flatMap(userFileMetadata -> super.validateUserPermission(user,
                                                                          userFileMetadata.getUserId(),
                                                                          userFileMetadata.getSharedWithUsers(),
                                                                          userFileMetadata.getId(),
                                                                          ownerOnly
                ));
    }

    /**
     * 創建初始歷史記錄
     *
     * @param userOnlineFile 用戶在線文件
     *
     * @return 用戶在線文件歷史
     */
    private Mono<UserOnlineFileHistory> createInitialHistory(UserOnlineFile userOnlineFile) {
        UserOnlineFileHistory history = new UserOnlineFileHistory();
        history.setFileId(userOnlineFile.getId());
        history.setVersion(0L);
        history.setPreviousVersion(null);
        history.setModifiedBy(userOnlineFile.getLastModifiedBy());
        history.setModifiedTime(LocalDateTime.now());
        history.setIsSnapshot(true);
        history.setSnapshotContent(userOnlineFile.getContent());
        history.setNote("初始化歷史記錄");
        return userOnlineFileHistoryRepository.save(history);
    }

    /**
     * 保存文件內容
     *
     * @param userOnlineFile 用戶在線文件
     * @param fileEditDTO    編輯文件數據傳輸對象
     * @param user           用戶
     *
     * @return 空Mono
     */
    private Mono<Void> saveContent(UserOnlineFile userOnlineFile, FileEditDTO fileEditDTO, User user) {
        return Mono.defer(() -> {
            userOnlineFile.setLastModifiedBy(user.getId());
            userOnlineFile.setIsMatchHistory(false);
            if (fileEditDTO.getContent() == null || fileEditDTO.getContent().isEmpty()) {
                userOnlineFile.setContent(EMPTY_CONTENT);
                return Mono.just(userOnlineFile);
            }

            return formatObjectToJson(fileEditDTO.getContent()).flatMap(contentJson -> {
                userOnlineFile.setContent(contentJson);
                return Mono.just(userOnlineFile);
            });
        }).then(userOnlineFileRepository.save(userOnlineFile).then(updateUserFileMetadata(fileEditDTO.getFileId())));
    }

    /**
     * 創建歷史記錄
     *
     * @param userOnlineFile 用戶在線文件
     * @param fileEditDTO    編輯文件數據傳輸對象
     * @param user           用戶
     *
     * @return 空Mono
     */
    private Mono<Void> BuildHistoryRecord(UserOnlineFile userOnlineFile, FileEditDTO fileEditDTO, User user) {
        Mono<EditorContentDTO> lastContentJsonDTOMono;
        if (userOnlineFile.getIsMatchHistory()) {
            lastContentJsonDTOMono = formatJsonToEditorContentJsonDTO(userOnlineFile.getContent());
        } else {
            lastContentJsonDTOMono = userOnlineFileHistoryRepository
                    .findByFileIdAndVersion(userOnlineFile.getId(), userOnlineFile.getLastHistoryVersion())
                    .flatMap(history -> findHistoryChainToSnapshot(history).collectList().flatMap(this::applyHistoryChain));
        }


        return lastContentJsonDTOMono.flatMap(compareContentDTO -> Mono.defer(() -> {
                                         if (compareContentDTO.equals(fileEditDTO.getContent())) {
                        return Mono.error(new ValidationException(ValidationException.ErrorCode.NO_CHANGE_IN_CONTENT));
                    }

                                         Mono<String> diffResult = calculateFileContentDiff(compareContentDTO, fileEditDTO.getContent());

                    if (fileEditDTO.getContent() == null || fileEditDTO.getContent().isEmpty()) {
                        userOnlineFile.setContent(EMPTY_CONTENT);
                        return diffResult;
                    }
                    return formatObjectToJson(fileEditDTO.getContent()).flatMap(newContent -> {
                        userOnlineFile.setContent(newContent);
                        return diffResult;
                    });
                }))
                .flatMap(diffResult -> userOnlineFileHistoryRepository
                        .findTopByFileIdOrderByVersionDesc(userOnlineFile.getId())
                        .map(UserOnlineFileHistory::getVersion)
                        .defaultIfEmpty(0L)
                        .flatMap(version -> {
                            long newVersion = version + 1;
                            UserOnlineFileHistory userOnlineFileHistory = new UserOnlineFileHistory();
                            userOnlineFileHistory.setFileId(userOnlineFile.getId());
                            userOnlineFileHistory.setModifiedTime(LocalDateTime.now());
                            userOnlineFileHistory.setModifiedBy(user.getId());
                            userOnlineFileHistory.setVersion(newVersion);
                            userOnlineFileHistory.setNote(fileEditDTO.getNote());

                            return userOnlineFileHistoryRepository
                                    .findTopByFileIdOrderByVersionDesc(userOnlineFile.getId())
                                    .map(UserOnlineFileHistory::getVersion)
                                    .defaultIfEmpty(0L)
                                    .flatMap(lastVersion -> {
                                        userOnlineFile.setLastHistoryVersion(newVersion);
                                        userOnlineFile.setLastModifiedBy(user.getId());
                                        userOnlineFile.setCurrentSnapshotCount(userOnlineFile.getCurrentSnapshotCount() + 1);
                                        userOnlineFile.setIsMatchHistory(true);
                                        if (userOnlineFile.getCurrentSnapshotCount() % 5 == 0) {
                                            userOnlineFileHistory.setIsSnapshot(true);
                                            userOnlineFileHistory.setSnapshotContent(userOnlineFile.getContent());
                                            userOnlineFile.setCurrentSnapshotCount(0);
                                        } else {
                                            userOnlineFileHistory.setDiff(diffResult);
                                            userOnlineFileHistory.setPreviousVersion(lastVersion);
                                        }

                                        return userOnlineFileHistoryRepository
                                                .save(userOnlineFileHistory)
                                                .then(userOnlineFileRepository.save(userOnlineFile));
                                    });
                        })
                        .then(updateUserFileMetadata(fileEditDTO.getFileId())));
    }

    /**
     * 應用歷史鏈進行還原
     */
    private Mono<EditorContentDTO> applyHistoryChain(List<UserOnlineFileHistory> historyChain) {
        Collections.reverse(historyChain);

        if (historyChain.isEmpty() || !historyChain.getFirst().getIsSnapshot()) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_HISTORY_CHAIN));
        }

        String baseContent = historyChain.getFirst().getSnapshotContent();
        return formatJsonToEditorContentJsonDTO(baseContent).flatMap(contentInDB -> {
            List<String> restoredContentList = convertDeltaToLines(contentInDB);

            for (int i = 1; i < historyChain.size(); i++) {
                try {
                    restoredContentList = applyRevertDiff(restoredContentList, historyChain.get(i).getDiff());
                } catch (Exception e) {
                    return Mono.error(new RuntimeException("還原歷史版本時發生錯誤", e));
                }
            }
            return formatJsonToEditorContentJsonDTO(restoredContentList);
        });
    }


    /**
     * 查找歷史鏈到快照
     *
     * @param targetHistory 目標歷史
     *
     * @return 歷史鏈
     */
    private Flux<UserOnlineFileHistory> findHistoryChainToSnapshot(UserOnlineFileHistory targetHistory) {
        return Flux.defer(() -> {
            List<UserOnlineFileHistory> chain = new ArrayList<>();
            return findHistoryChainRecursive(targetHistory, chain).flatMapMany(Flux::fromIterable);
        });
    }

    /**
     * 遞歸查找歷史鏈
     *
     * @param currentHistory 當前歷史
     * @param chain          歷史鏈
     *
     * @return 歷史鏈
     */
    private Mono<List<UserOnlineFileHistory>> findHistoryChainRecursive(UserOnlineFileHistory currentHistory, List<UserOnlineFileHistory> chain) {
        chain.add(currentHistory);

        if (currentHistory.getIsSnapshot()) {
            return Mono.just(chain);
        }

        if (currentHistory.getPreviousVersion() == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_VERSION_CHAIN));
        }

        return userOnlineFileHistoryRepository
                .findByFileIdAndVersion(currentHistory.getFileId(), currentHistory.getPreviousVersion())
                .flatMap(previousHistory -> findHistoryChainRecursive(previousHistory, chain));
    }

    /**
     * 從快照還原
     *
     * @param userOnlineFile 用戶在線文件
     * @param snapshot       快照
     * @param userId         用戶ID
     *
     * @return 空Mono
     */
    private Mono<Void> restoreFromSnapshot(UserOnlineFile userOnlineFile, UserOnlineFileHistory snapshot, Long userId) {
        return saveFileHistory(userOnlineFile, userOnlineFile.getContent(), userId, snapshot.getVersion()).then(Mono.defer(() -> {
            userOnlineFile.setContent(snapshot.getSnapshotContent());
            userOnlineFile.setLastModifiedBy(userId);
            return userOnlineFileRepository.save(userOnlineFile).then(updateUserFileMetadata(userOnlineFile.getId().toString()));
        }));
    }

    /**
     * 格式化JSON為EditorContentJsonDTO
     *
     * @param json JSON
     *
     * @return EditorContentJsonDTO
     */
    private Mono<EditorContentDTO> formatJsonToEditorContentJsonDTO(String json) {
        return Mono.fromCallable(() -> objectMapper.readValue(json, EditorContentDTO.class))
                .onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
    }

    /**
     * 計算文件內容差異
     *
     * @param oldContent 舊內容
     * @param newContent 新內容
     *
     * @return 差異
     */

    private Mono<String> calculateFileContentDiff(EditorContentDTO oldContent, EditorContentDTO newContent) {
        return Mono.fromCallable(() -> {
            List<String> oldLines = convertDeltaToLines(oldContent);
            List<String> newLines = convertDeltaToLines(newContent);
            Patch<String> patch = DiffUtils.diff(oldLines, newLines);
            CustomPatchPO.Patch customPath = CustomPatchPO.Patch.fromPatch(patch);
            return objectMapper.writeValueAsString(customPath);
        }).onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.CALCULATE_CONTENT_DIFFERENCE_FAILED));
    }


    /**
     * 應用還原差異
     *
     * @param contents  文件內容
     * @param patchJson 差異
     *
     * @return 還原後的文件內容
     */
    private List<String> applyRevertDiff(List<String> contents, String patchJson) throws JsonProcessingException, PatchFailedException {
        if (patchJson == null || patchJson.isEmpty()) {
            return contents;
        }
        CustomPatchPO.Patch patchDTO = objectMapper.readValue(patchJson, CustomPatchPO.Patch.class);
        Patch<String> patch = patchDTO.toPatch();
        return DiffUtils.patch(contents, patch);
    }

    /**
     * 更新用戶文件元數據
     *
     * @param fileId 文件ID
     *
     * @return 空Mono
     */
    private Mono<Void> updateUserFileMetadata(String fileId) {
        return userFileMetaRepository.findById(fileId).flatMap(userFileMetadata -> {
            userFileMetadata.setLastAccessTime(LocalDateTime.now());
            return userFileMetaRepository.save(userFileMetadata);
        }).then().subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 格式化對象為JSON
     *
     * @param content 內容
     *
     * @return JSON
     */
    private Mono<String> formatObjectToJson(Object content) {
        try {
            return Mono.just(objectMapper.writeValueAsString(content));
        } catch (Exception e) {
            return Mono.error(new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
        }
    }

    /**
     * 將差異轉換為行
     *
     * @param content 內容
     *
     * @return 行
     */
    private List<String> convertDeltaToLines(EditorContentDTO content) {
        return content.getDelta().stream().map(delta -> {
            try {
                return objectMapper.writeValueAsString(delta);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("無法轉換EditorContentJsonDTO成Json", e);
            }
        }).toList();
    }

    /**
     * 還原歷史記錄
     *
     * @param userOnlineFile 用戶在線文件
     * @param targetVersion  目標版本
     * @param userId         用戶ID
     *
     * @return 空Mono
     */
    private Mono<Void> revertHistoryRecord(UserOnlineFile userOnlineFile, Long targetVersion, Long userId) {
        return Mono.defer(() -> {
            if (targetVersion == null || targetVersion < 0) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_VERSION_NUMBER, targetVersion));
            }

            return userOnlineFileHistoryRepository
                    .findByFileIdAndVersion(userOnlineFile.getId(), targetVersion)
                    .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_HISTORY_RECORD,
                                                                      targetVersion
                    )));
        }).flatMap(targetHistory -> {
            if (targetHistory.getIsSnapshot()) {
                return restoreFromSnapshot(userOnlineFile, targetHistory, userId);
            }

            return Mono.defer(() -> findHistoryChainToSnapshot(targetHistory)
                    .collectList()
                    .flatMap(historyChain -> applyHistoryChain(historyChain).flatMap(editorContentDTO -> formatObjectToJson(editorContentDTO).flatMap(
                            contentJson -> saveFileHistory(userOnlineFile, contentJson, userId, historyChain.getLast().getVersion()))))
                    .then(updateUserFileMetadata(userOnlineFile.getId().toString())));
        });
    }

    /**
     * 格式化JSON為EditorContentJsonDTO
     *
     * @param deltaList 差異列表
     *
     * @return EditorContentJsonDTO
     */
    private Mono<EditorContentDTO> formatJsonToEditorContentJsonDTO(List<String> deltaList) {
        try {
            List<EditorContentDTO.DeltaDTO> deltaDTOList = deltaList.stream().map(delta -> {
                try {
                    return objectMapper.readValue(delta, EditorContentDTO.DeltaDTO.class);
                } catch (Exception e) {
                    throw new RuntimeException("無法轉換Json成EditorContentJsonDTO.DeltaDTO", e);
                }
            }).collect(Collectors.toList());
            EditorContentDTO contentObject = new EditorContentDTO();
            contentObject.setDelta(deltaDTOList);
            return Mono.just(contentObject);
        } catch (Exception e) {
            return Mono.error(new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
        }
    }

    /**
     * 保存文件歷史
     *
     * @param userOnlineFile    用戶在線文件
     * @param editorContentJson 編輯內容JSON
     * @param userId            用戶ID
     * @param targetVersion     上一個版本
     *
     * @return 空Mono
     */
    private Mono<Void> saveFileHistory(UserOnlineFile userOnlineFile, String editorContentJson, Long userId, Long targetVersion) {
        return userOnlineFileHistoryRepository.findTopByFileIdOrderByVersionDesc(userOnlineFile.getId()).flatMap(lastHistory -> {
            Long newVersion = lastHistory.getVersion() + 1;
            UserOnlineFileHistory userOnlineFileHistory = new UserOnlineFileHistory();
            userOnlineFileHistory.setFileId(userOnlineFile.getId());
            userOnlineFileHistory.setModifiedTime(LocalDateTime.now());
            userOnlineFileHistory.setModifiedBy(userId);
            userOnlineFileHistory.setVersion(newVersion);
            userOnlineFileHistory.setPreviousVersion(null);
            userOnlineFileHistory.setIsSnapshot(true);
            userOnlineFileHistory.setSnapshotContent(userOnlineFile.getContent());
            String note = String.format("修改者:%s 還原到 %d 版本 (此為自動建立的快照，用於恢復到還原操作之前的版本)",
                                        userId,
                                        targetVersion
            );
            userOnlineFileHistory.setNote(note);

            userOnlineFile.setLastModifiedBy(userId);
            userOnlineFile.setContent(editorContentJson);

            return userOnlineFileHistoryRepository.save(userOnlineFileHistory).then(userOnlineFileRepository.save(userOnlineFile));
        }).then();
    }

}

