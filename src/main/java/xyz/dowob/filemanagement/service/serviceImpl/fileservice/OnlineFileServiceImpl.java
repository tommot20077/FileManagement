package xyz.dowob.filemanagement.service.serviceImpl.fileservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.annotation.Nullable;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.component.manager.CacheManager;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.factory.ContentConvertProviderFactory;
import xyz.dowob.filemanagement.component.provider.factory.config.ConvertConfig;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.ContentConvertProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.ConvertProviderEnum;
import xyz.dowob.filemanagement.customenum.DownloadActionEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dao.OnlineHistoryCountAndOldestDAO;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

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
     * 預設空內容的JSON
     */
    private final String EMPTY_CONTENT = "{\"delta\":[]}";

    public OnlineFileServiceImpl(UserOnlineFileHistoryRepository userOnlineFileHistoryRepository, ServerFileMetaRepository serverFileMetaRepository, UserFileMetaRepository userFileMetaRepository, RedisProvider redisProvider, GridFsProvider gridFsProvider, TransfersTasksManager transfersTasksManager, FileProperties fileProperties, CircuitBreakerConfig circuitBreakerConfig, UserRepository userRepository, UserOnlineFileRepository userOnlineFileRepository, R2dbcEntityOperations entityOperations, FileTrashRecordRepository fileTrashRecordRepository, TransactionalOperator transactionalOperator, RateLimiterConfig rateLimiterConfig, UserFIleShareRecordRepository userFIleShareRecordRepository, ObjectMapper objectMapper, CacheManager cacheManager,
                                 @Nullable FolderListTreeProvider folderListTreeProvider) {
        super(serverFileMetaRepository,
              userFileMetaRepository,
              userOnlineFileRepository,
              userRepository,
              redisProvider,
              gridFsProvider,
              transfersTasksManager,
              fileProperties,
              circuitBreakerConfig,
              rateLimiterConfig,
              folderListTreeProvider,
              fileTrashRecordRepository,
              entityOperations,
              transactionalOperator,
              userFIleShareRecordRepository,
              objectMapper,
              cacheManager
        );
        this.userOnlineFileRepository = userOnlineFileRepository;
        this.userOnlineFileHistoryRepository = userOnlineFileHistoryRepository;
    }

    /**
     * 下載指定文件。
     * <p>
     * 根據文件元數據查找用戶文件，並返回文件內容。如果文件內容轉換為DTO對象失敗，將會返回錯誤。
     *
     * @param userFileMetadata 文件的元數據，包含文件ID和其他元數據信息。
     * @param user             當前操作的用戶。
     * @param optional         可選參數，此處為下載類型
     *
     * @return Mono<UserFileDataBO> 包含文件數據和元數據的業務對象。
     */
    @Override
    public Mono<UserFileDataBO> downloadFile(UserFileMetadata userFileMetadata, User user, String... optional) {
        return findUserOnlineFileById(userFileMetadata.getId().toString()).flatMap(userOnlineFile -> {
            if (Objects.equals(optional[0], DownloadActionEnum.DOWNLOAD.name())) {
                ContentConvertProvider convertProvider = ContentConvertProviderFactory.createProvider(ConvertProviderEnum.DOCX, new ConvertConfig());
                return convertProvider.convertToDataBuffer(userOnlineFile.getContent()).flatMap(dataBufferSize -> {
                    UserFileDataBO userFileDataBO = new UserFileDataBO();
                    userFileDataBO.setFilename(userFileMetadata.getFilename());
                    userFileDataBO.setFileType(FileEnum.ONLINE_DOCUMENT);
                    userFileDataBO.setFileSize(dataBufferSize.size());
                    userFileDataBO.setDataBufferFlux(dataBufferSize.dataBuffer());

                    String name = userFileMetadata.getFilename().split("\\.")[0] + "." + ConvertProviderEnum.DOCX.getSuffix();
                    userFileDataBO.setFilename(name);
                    return Mono.just(userFileDataBO);
                });
            }
            try {
                EditorContentDTO content = objectMapper.readValue(userOnlineFile.getContent(), EditorContentDTO.class);
                userFileMetadata.setLastAccessTime(LocalDateTime.now());
                userFileMetaRepository.save(userFileMetadata).subscribeOn(Schedulers.boundedElastic()).subscribe();
                return Mono.just(new UserFileDataBO(userOnlineFile, userFileMetadata, content));
            } catch (JsonProcessingException e) {
                return Mono.error(new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
            }
        });
    }

    /**
     * 上傳指定文件。
     * <p>
     * 根據給定的文件元數據創建文件元數據並保存，然後上傳文件內容。如果操作成功，將返回上傳結果。
     *
     * @param fileMetadataDTO 文件的元數據，包含文件名稱、父目錄等信息。
     * @param user            當前操作的用戶。
     *
     * @return Mono<UploadResponseDTO> 上傳結果，包含進度、是否成功等信息。
     */
    @Override
    public Mono<UploadResponseDTO> uploadFile(FileMetadataDTO fileMetadataDTO, User user) {
        fileMetadataDTO.setUser(user);
        return Mono.defer(() -> {
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
                        userOnlineFile.setCurrentSnapshotCount(null);
                        userOnlineFile.setLastHistoryVersion(null);
                        userOnlineFile.setIsMatchHistory(null);
                        return Mono.just(userOnlineFile);
                    })
                    .flatMap(userOnlineFile -> userOnlineFileRepository
                            .insertWithId(userOnlineFile)
                            .then(cleanUserListCache(user.getId(), userFileMetadata.getParentFolderId()))
                            .thenReturn(UploadResponseDTO.builder().progress(100.0).isSuccess(true).isFinished(true).message("上傳成功").build()));
        });
    }

    /**
     * 刪除指定文件。
     * <p>
     * 根據文件元數據刪除指定文件的記錄。該方法目前委託給父類執行具體操作。
     *
     * @param fileMetadata 文件的元數據，包含文件ID等信息。
     * @param user         當前操作的用戶。
     *
     * @return Mono<Void> 空的 Mono 表示刪除操作已完成。
     */
    @Override
    public Mono<Void> deleteFile(UserFileMetadata fileMetadata, User user) {
        return super.deleteFile(fileMetadata, user);
    }

    /**
     * 編輯指定文件。
     * <p>
     * 根據文件編輯類型執行不同的操作（例如，編輯元數據、編輯內容、構建歷史記錄等）。
     *
     * @param fileEditDTO 編輯文件數據傳輸對象，包含文件ID和編輯類型。
     * @param user        當前操作的用戶。
     *
     * @return Mono<Void> 空的 Mono 表示編輯操作已完成。
     */
    @Override
    public Mono<Void> editFile(FileEditDTO fileEditDTO, User user) {
        return findUserOnlineFileById(fileEditDTO.getFileId()).flatMap(userOnlineFile -> switch (fileEditDTO.getEditType()) {
            case EDIT_METADATA -> super.editFile(fileEditDTO, user);
            case EDIT_CONTENT -> saveContent(userOnlineFile, fileEditDTO, user);
            case BUILD_HISTORY_RECORD -> buildHistoryRecord(userOnlineFile, fileEditDTO, user);
            case REVERT_HISTORY_RECORD -> revertHistoryRecord(userOnlineFile, fileEditDTO, user);
            case DELETE_HISTORY_RECORD -> deleteHistoryRecord(userOnlineFile, fileEditDTO.getVersion());
        });
    }

    /**
     * 獲取指定文件的版本列表。
     * <p>
     * 根據文件ID和頁碼返回文件的版本列表，支持分頁顯示。
     *
     * @param user         當前操作的用戶。
     * @param fileMetadata 文件的元數據，包含文件ID。
     * @param page         當前頁碼。
     * @param size         每頁顯示的數量。
     *
     * @return Mono<PagedResponseDTO < FileVersionDTO>> 文件版本的分頁結果。
     */
    @Override
    @HideOverLength
    public Mono<PagedResponseDTO<FileVersionDTO>> getFileVersionList(User user, UserFileMetadata fileMetadata, Integer page, Integer size) {
        int pageSize = Objects.requireNonNullElse(size, fileProperties.getGlobal().getPageSize());
        int currentPage = Math.max(1, Objects.requireNonNullElse(page, 1));
        int offset = (currentPage - 1) * pageSize;
        return findUserOnlineFileById(fileMetadata.getId().toString()).flatMap(userOnlineFile -> userOnlineFileHistoryRepository
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
                }));
    }


    /**
     * 根據文件ID查找用戶在線文件。
     * <p>
     * 這個方法會查找指定ID的用戶在線文件。如果找不到文件，將會拋出文件不存在的錯誤。
     *
     * @param fileId 文件的唯一ID。
     *
     * @return Mono<UserOnlineFile> 返回查詢到的用戶在線文件。
     */
    private Mono<UserOnlineFile> findUserOnlineFileById(String fileId) {
        return userOnlineFileRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)));
    }

    /**
     * 創建指定文件的初始歷史記錄。
     * <p>
     * 這個方法會生成初始版本的歷史記錄，並保存為歷史數據。該記錄包括文件的初始內容、修改者等信息。
     *
     * @param userOnlineFile 用戶在線文件對象，包含文件的基本信息。
     * @param fileEditDTO    編輯文件數據傳輸對象，包含文件編輯的具體信息。
     * @param contentJson    文件內容的JSON表示。
     *
     * @return Mono<UserOnlineFileHistory> 返回創建並保存的初始歷史記錄。
     */
    private Mono<UserOnlineFileHistory> createInitialHistory(UserOnlineFile userOnlineFile, FileEditDTO fileEditDTO, String contentJson) {
        UserOnlineFileHistory history = new UserOnlineFileHistory();
        history.setFileId(userOnlineFile.getId());
        history.setVersion(0L);
        history.setPreviousVersion(null);
        history.setModifiedBy(userOnlineFile.getLastModifiedBy());
        history.setModifiedTime(LocalDateTime.now());
        history.setIsSnapshot(true);
        history.setSnapshotContent(contentJson);
        history.setNote(fileEditDTO.getNote());
        return userOnlineFileHistoryRepository.save(history);
    }

    /**
     * 保存編輯後的文件內容。
     * <p>
     * 根據編輯傳入的內容格式化並保存文件內容。若文件內容為空，則保存為空內容。
     *
     * @param userOnlineFile 用戶在線文件對象，包含當前文件內容。
     * @param fileEditDTO    編輯文件數據傳輸對象，包含文件內容。
     * @param user           當前操作的用戶。
     *
     * @return Mono<Void> 空的 Mono，表示操作完成。
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
        }).then(userOnlineFileRepository.save(userOnlineFile).then(updateUserFileMetadata(fileEditDTO.getUserFileMetadata())));
    }

    /**
     * 根據文件編輯內容創建新的歷史記錄。
     * <p>
     * 該方法會根據當前文件內容和修改記錄生成新的歷史記錄。若文件內容無變動，則會返回錯誤。
     *
     * @param userOnlineFile 用戶在線文件對象，包含當前文件的基本信息。
     * @param fileEditDTO    編輯文件數據傳輸對象，包含文件的修改內容。
     * @param user           當前操作的用戶。
     *
     * @return Mono<Void> 空的 Mono，表示操作完成。
     */
    private Mono<Void> buildHistoryRecord(UserOnlineFile userOnlineFile, FileEditDTO fileEditDTO, User user) {
        if (userOnlineFile.getIsMatchHistory() == null || userOnlineFile.getLastHistoryVersion() == null) {
            return formatObjectToJson(fileEditDTO.getContent()).flatMap(newContent -> {
                userOnlineFile.setContent(newContent);
                userOnlineFile.setIsMatchHistory(true);
                userOnlineFile.setLastHistoryVersion(0L);
                userOnlineFile.setCurrentSnapshotCount(0);
                userOnlineFile.setLastModifiedBy(user.getId());
                return createInitialHistory(userOnlineFile, fileEditDTO, newContent);
            }).then(userOnlineFileRepository.save(userOnlineFile).then(updateUserFileMetadata(fileEditDTO.getUserFileMetadata())));
        }

        Mono<EditorContentDTO> lastContentJsonDTOMono;
        if (userOnlineFile.getIsMatchHistory()) {
            lastContentJsonDTOMono = formatJsonToEditorContentJsonDTO(userOnlineFile.getContent());
        } else {
            lastContentJsonDTOMono = userOnlineFileHistoryRepository
                    .findByFileIdAndVersion(userOnlineFile.getId(), userOnlineFile.getLastHistoryVersion())
                    .flatMap(this::getCompleteContent);
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
        })).flatMap(diffResult -> userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(userOnlineFile.getId(), 1)
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
                                userOnlineFileHistory.setPreviousVersion(version);
                            }
                            return userOnlineFileHistoryRepository.save(userOnlineFileHistory).then(userOnlineFileRepository.save(userOnlineFile));
                        }).then(Mono.when(updateUserFileMetadata(fileEditDTO.getUserFileMetadata()), deleteExcessHistoryRecord(userOnlineFile))));
    }

    /**
     * 將指定的歷史紀錄轉換成完整的文件內容。
     * <p>
     * 根據歷史紀錄和必要的補丁，還原出文件的最終內容。
     *
     * @param targetHistory 目標歷史紀錄，包含文件的某個歷史版本。
     *
     * @return Mono<EditorContentDTO> 還原後的文件內容。
     */
    private Mono<EditorContentDTO> getCompleteContent(UserOnlineFileHistory targetHistory) {
        return Mono.defer(() -> {
            Stack<UserOnlineFileHistory> historyStack = new Stack<>();
            return findHistoryChainRecursive(targetHistory, historyStack);
        }).flatMap(historyChain -> {
            if (historyChain.isEmpty() || !historyChain.getLast().getIsSnapshot()) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_HISTORY_CHAIN));
            }

            String baseContent = historyChain.getLast().getSnapshotContent();
            return formatJsonToEditorContentJsonDTO(baseContent).flatMap(editorContentDTO -> applyPatchToContent(editorContentDTO, historyChain));
        });
    }

    /**
     * 遞歸查找並構建歷史紀錄鏈。
     * <p>
     * 該方法會遞歸查找歷史紀錄的鏈，直到找到快照版本為止，並將結果返回。
     *
     * @param currentHistory 當前的歷史紀錄。
     * @param chain          歷史紀錄的鏈，會逐步添加歷史紀錄。
     *
     * @return Mono<Stack < UserOnlineFileHistory>> 返回完整的歷史紀錄鏈。
     */
    private Mono<Stack<UserOnlineFileHistory>> findHistoryChainRecursive(UserOnlineFileHistory currentHistory, Stack<UserOnlineFileHistory> chain) {
        chain.push(currentHistory);

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
     * 將JSON格式的字符串轉換為EditorContentJsonDTO對象。
     * <p>
     * 該方法將會嘗試將JSON字符串解析成指定的DTO對象，並處理解析過程中的錯誤。
     *
     * @param json JSON格式的字符串，表示文件的內容。
     *
     * @return Mono<EditorContentDTO> 轉換後的EditorContentDTO對象。
     */
    private Mono<EditorContentDTO> formatJsonToEditorContentJsonDTO(String json) {
        return Mono.fromCallable(() -> objectMapper.readValue(json, EditorContentDTO.class))
                .onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
    }

    /**
     * 計算兩個文件內容的差異。
     * <p>
     * 該方法將會比較舊內容和新內容之間的差異，並返回差異的表示。
     *
     * @param oldContent 舊文件內容。
     * @param newContent 新文件內容。
     *
     * @return Mono<String> 差異的表示，通常為一個格式化的差異字符串。
     */
    private Mono<String> calculateFileContentDiff(EditorContentDTO oldContent, EditorContentDTO newContent) {
        return Mono.fromCallable(() -> {
            List<String> oldLines = convertDeltaToLines(oldContent);
            List<String> newLines = convertDeltaToLines(newContent);
            Patch<String> patch = DiffUtils.diff(oldLines, newLines);
            CustomPatchPO.Patch customPath = CustomPatchPO.Patch.fromPatch(patch);
            return objectMapper.writeValueAsString(customPath);
        }).onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.CALCULATE_CONTENT_DIFFERENCE_FAILED, e));
    }


    /**
     * 應用還原差異
     * 此方法用於應用還原差異，將給定的差異（patch）應用於當前的文件內容。若無效或無法解析差異，會拋出相應的異常。
     *
     * @param contents  文件內容
     * @param patchJson 差異
     *
     * @return 還原後的文件內容
     *
     * @throws JsonProcessingException 當差異JSON無法解析時拋出
     * @throws PatchFailedException    當應用差異失敗時拋出
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
     * 此方法用於更新用戶文件的元數據。它會更新 lastAccessTime 並將新的元數據保存到數據庫中。
     *
     * @param userFileMetadata: 用戶文件的元數據對象，包含文件的各種信息。
     *
     * @return 操作已完成的 Mono
     */
    public Mono<Void> updateUserFileMetadata(UserFileMetadata userFileMetadata) {
        return Mono.defer(() -> {
            userFileMetadata.setLastAccessTime(LocalDateTime.now());
            return userFileMetaRepository.save(userFileMetadata);
        }).then().subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 格式化對象為JSON
     * 此方法將任何對象轉換為 JSON 格式的字符串。若轉換失敗，會返回錯誤。
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
     * 此方法根據給定的版本號還原文件的歷史記錄。如果是快照版本，將直接還原；如果是增量版本，則會計算並應用補丁。
     *
     * @param userOnlineFile 用戶正在編輯的在線文件。
     * @param editDTO        用於編輯文件的數據傳輸對象，包含文件內容和版本號。
     * @param user           當前執行還原操作的用戶。
     *
     * @return 空Mono
     */
    private Mono<Void> revertHistoryRecord(UserOnlineFile userOnlineFile, FileEditDTO editDTO, User user) {
        Long targetVersion = editDTO.getVersion();
        return Mono.defer(() -> {
            if (targetVersion == null || targetVersion < 0) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_VERSION_NUMBER, targetVersion));
            }

            return userOnlineFileHistoryRepository
                    .findByFileIdAndVersion(userOnlineFile.getId(), targetVersion)
                    .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_HISTORY_RECORD, targetVersion)));
        }).flatMap(targetHistory -> {
            if (targetHistory.getIsSnapshot()) {
                return saveFileHistory(userOnlineFile, targetHistory.getSnapshotContent(), user, targetHistory.getVersion());
            }
            return getCompleteContent(targetHistory)
                    .flatMap(this::formatObjectToJson)
                    .flatMap(contentJson -> saveFileHistory(userOnlineFile, contentJson, user, targetHistory.getVersion()));
        }).then(Mono.when(updateUserFileMetadata(editDTO.getUserFileMetadata()), deleteExcessHistoryRecord(userOnlineFile)));
    }

    /**
     * 格式化JSON為EditorContentJsonDTO
     * 此方法將 JSON 字符串轉換為 EditorContentDTO 對象，用於表示文件的編輯內容。
     *
     * @param deltaList 差異列表
     *
     * @return 返回 EditorContentDTO，表示已解析的文件內容。
     */
    private Mono<EditorContentDTO> formatJsonToEditorContentJsonDTO(List<String> deltaList) {
        return Flux.fromIterable(deltaList).flatMapSequential(delta -> {
            try {
                EditorContentDTO.DeltaDTO deltaDTO = objectMapper.readValue(delta, EditorContentDTO.DeltaDTO.class);
                return Mono.just(deltaDTO);
            } catch (Exception e) {
                return Mono.error(new RuntimeException("無法轉換Json成EditorContentJsonDTO.DeltaDTO", e));
            }
        }).collectList().map(deltaDTOList -> {
            EditorContentDTO contentObject = new EditorContentDTO();
            contentObject.setDelta(deltaDTOList);
            return contentObject;
        }).onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
    }


    /**
     * 保存文件歷史
     * 此方法保存一個新的文件歷史記錄。它會根據文件內容創建新的快照或增量版本。
     *
     * @param userOnlineFile    用戶正在編輯的在線文件
     * @param editorContentJson 文件內容的 JSON 字符串
     * @param user              當前執行保存操作的用戶
     * @param targetVersion     上一個版本號，通常是本次操作的基礎版本。
     *
     * @return 空Mono
     */
    private Mono<Void> saveFileHistory(UserOnlineFile userOnlineFile, String editorContentJson, User user, Long targetVersion) {
        return userOnlineFileHistoryRepository.findTopNByFileIdOrderByVersionDesc(userOnlineFile.getId(), 1).flatMap(lastHistory -> {
            Long newVersion = lastHistory.getVersion() + 1;
            UserOnlineFileHistory userOnlineFileHistory = new UserOnlineFileHistory();
            userOnlineFileHistory.setFileId(userOnlineFile.getId());
            userOnlineFileHistory.setModifiedTime(LocalDateTime.now());
            userOnlineFileHistory.setModifiedBy(user.getId());
            userOnlineFileHistory.setVersion(newVersion);
            userOnlineFileHistory.setPreviousVersion(null);
            userOnlineFileHistory.setIsSnapshot(true);
            userOnlineFileHistory.setSnapshotContent(userOnlineFile.getContent());
            String noteFormat = "修改者:%s 還原到 %d 版本 (此為自動建立的快照，可以使用此快照恢復到執行還原操作當下的版本)";
            String note = String.format(noteFormat, user.getUsername(), targetVersion);
            userOnlineFileHistory.setNote(note);

            userOnlineFile.setLastModifiedBy(user.getId());
            userOnlineFile.setContent(editorContentJson);

            return userOnlineFileHistoryRepository.save(userOnlineFileHistory).then(userOnlineFileRepository.save(userOnlineFile));
        }).then();
    }

    /**
     * 刪除歷史記錄
     * 此方法刪除指定的歷史版本，並計算並應用補丁來更新後續的歷史版本。還會處理版本鏈的修正。
     * 該方法的目的是刪除指定版本的歷史記錄，並計算並應用補丁以更新隨後的歷史版本。
     * 若刪除的是快照版本，則會更新歷史鏈中的其他版本，並處理文件版本的增量更新或重設。
     *
     * @param onlineFile 用戶正在編輯的在線文件
     * @param version    要刪除的歷史版本號
     *
     * @return 空Mono
     */
    private Mono<Void> deleteHistoryRecord(UserOnlineFile onlineFile, Long version) {
        if (version == null || version < 0) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_VERSION_NUMBER, version));
        }

        return userOnlineFileHistoryRepository
                .findByFileIdAndVersion(onlineFile.getId(), version)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_HISTORY_RECORD, version)))
                .flatMap(toDeleteHistory -> {
                    Mono<Optional<EditorContentDTO>> baseContentDTOMono;
                    Mono<EditorContentDTO> deleteHistoryContentMono = getCompleteContent(toDeleteHistory);
                    Mono<List<UserOnlineFileHistory>> combineHistoryListMono = userOnlineFileHistoryRepository
                            .findAllByFileIdAndPreviousVersion(onlineFile.getId(), version)
                            .collectList();

                    if (toDeleteHistory.getIsSnapshot()) {
                        baseContentDTOMono = Mono.just(Optional.empty());
                    } else {
                        baseContentDTOMono = userOnlineFileHistoryRepository
                                .findByFileIdAndVersion(onlineFile.getId(), toDeleteHistory.getPreviousVersion())
                                .switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.EXISTING_DIFF_AND_SNAPSHOT,
                                                                               toDeleteHistory.getFileId(),
                                                                               toDeleteHistory.getPreviousVersion()
                                )))
                                .flatMap(history -> getCompleteContent(history).map(Optional::of));
                    }
                    Mono<OnlineHistoryCountAndOldestDAO> countDaoMono = userOnlineFileHistoryRepository.getOldestVersionAndCountByFileId(onlineFile.getId());
                    return Mono.zip(Mono.just(toDeleteHistory), deleteHistoryContentMono, baseContentDTOMono, combineHistoryListMono, countDaoMono);
                })
                .flatMap(tuple5 -> {
                    UserOnlineFileHistory deleteHistory = tuple5.getT1();
                    EditorContentDTO deleteHistoryContent = tuple5.getT2();
                    EditorContentDTO baseContentDTO = tuple5.getT3().isPresent() ? tuple5.getT3().get() : deleteHistoryContent;
                    List<UserOnlineFileHistory> historyList = tuple5.getT4();
                    boolean isDeleteFileSnapshot = deleteHistory.getIsSnapshot();


                    Mono<List<UserOnlineFileHistory>> combineHistoryList = Flux.fromIterable(historyList).flatMap(combineHistory -> {
                        Stack<UserOnlineFileHistory> patchStack = new Stack<>();
                        patchStack.push(combineHistory);
                        return applyPatchToContent(deleteHistoryContent, patchStack).flatMap(restoreContent -> {
                            if (isDeleteFileSnapshot) {
                                return formatObjectToJson(restoreContent);
                            }
                            return calculateFileContentDiff(baseContentDTO, restoreContent);
                        }).flatMap(combineContent -> {
                            if (isDeleteFileSnapshot) {
                                combineHistory.setPreviousVersion(null);
                                combineHistory.setIsSnapshot(true);
                                combineHistory.setSnapshotContent(combineContent);
                                combineHistory.setDiff(null);
                            } else {
                                combineHistory.setPreviousVersion(deleteHistory.getPreviousVersion());
                                combineHistory.setDiff(combineContent);
                            }
                            combineHistory.setModifiedTime(LocalDateTime.now());
                            return Mono.just(combineHistory);
                        });
                    }).collectList();
                    Mono<Void> actionMono = combineHistoryList
                            .flatMapMany(userOnlineFileHistoryRepository::saveAll)
                            .then(userOnlineFileHistoryRepository.delete(deleteHistory));
                    return transactionalOperator.transactional(actionMono).then(Mono.defer(() -> {
                        if (tuple5.getT5().count() <= 1) {
                            onlineFile.setLastHistoryVersion(null);
                            onlineFile.setIsMatchHistory(null);
                            onlineFile.setCurrentSnapshotCount(null);
                            return userOnlineFileRepository.save(onlineFile).then();
                        }
                        return Mono.empty();
                    }));
                });
    }


    /**
     * 應用補丁至文件內容
     * 此方法將歷史版本的補丁應用到文件內容，並返回最終還原後的內容。
     *
     * @param editorContentDTO 當前文件內容的 DTO。
     * @param historyChain     歷史版本鏈，按時間順序保存。
     *
     * @return 還原後的文件內容
     */
    private Mono<EditorContentDTO> applyPatchToContent(EditorContentDTO editorContentDTO, Stack<UserOnlineFileHistory> historyChain) {
        return Mono.defer(() -> {
            List<String> restoredContentList = convertDeltaToLines(editorContentDTO);
            while (!historyChain.isEmpty()) {
                try {
                    restoredContentList = applyRevertDiff(restoredContentList, historyChain.pop().getDiff());
                } catch (Exception e) {
                    return Mono.error(new RuntimeException("還原歷史版本時發生錯誤", e));
                }
            }
            return formatJsonToEditorContentJsonDTO(restoredContentList);
        }).onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.APPLY_PATCH_TO_CONTENT_FAILED, e));
    }

    /**
     * 刪除過多的歷史記錄
     * 此方法刪除超過數量限制的歷史記錄{@link FileProperties}。如果歷史記錄超過該限制，則會刪除最舊的歷史記錄。
     *
     * @param onlineFile 用戶在線文件
     *
     * @return 空Mono
     */
    private Mono<Void> deleteExcessHistoryRecord(UserOnlineFile onlineFile) {
        int maxOnlineHistoryCount = fileProperties.getBackup().getMaxOnlineHistoryCount();
        if (maxOnlineHistoryCount <= 0) {
            return Mono.empty();
        }

        return userOnlineFileHistoryRepository.getOldestVersionAndCountByFileId(onlineFile.getId()).flatMap(dao -> {
            if (dao.count() > maxOnlineHistoryCount) {
                return deleteHistoryRecord(onlineFile, dao.version());
            }
            return Mono.empty();
        });
    }
}

