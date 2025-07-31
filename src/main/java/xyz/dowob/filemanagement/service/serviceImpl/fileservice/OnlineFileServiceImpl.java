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
import xyz.dowob.filemanagement.annotation.*;
import xyz.dowob.filemanagement.component.manager.CacheManager;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.factory.ContentConvertProviderFactory;
import xyz.dowob.filemanagement.component.provider.factory.config.ConvertConfig;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.ContentConvertProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.FileScanProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.*;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dao.OnlineHistoryCountAndOldestDAO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.data.file.po.CustomPatchPO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
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
 * 線上檔案服務實現類，提供線上檔案的完整生命週期管理功能。
 * <p>
 * 繼承 {@link xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService}，
 * 專門處理使用者線上編輯的檔案操作。支援即時編輯、版本控制、歷史記錄管理、
 * 協作功能等進階文檔處理能力。
 * <p>
 * 採用反應式編程模式實現非阻塞式檔案操作。整合差異演算法進行高效內容同步，
 * 提供完整的版本歷史追蹤和還原機制。與 GridFS 系統整合，實現混合儲存策略
 * 以最佳化系統效能。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService
 */
@Service
@RecordLevel(LogLevelEnum.DEBUG)
@FileHandlerType(FileEnum.ONLINE_DOCUMENT)
public class OnlineFileServiceImpl extends AbstractFileService {
    /**
     * 在線檔案資料庫操作接口
     */
    private final UserOnlineFileRepository userOnlineFileRepository;

    /**
     * 在線檔案歷史資料庫操作接口
     */
    private final UserOnlineFileHistoryRepository userOnlineFileHistoryRepository;

    /**
     * 預設空內容的JSON
     */
    private final String EMPTY_CONTENT = "{\"delta\":[]}";


    /**
     * 線上檔案服務實現類的建構子。
     * <p>
     * 初始化線上檔案服務的所有必要依賴項目，包括資料庫操作介面、
     * 儲存提供者、緩存管理器、版本控制系統等核心組件。
     * 所有參數都會傳遞給父類進行統一初始化。
     *
     * @param userOnlineFileHistoryRepository 用戶線上檔案歷史資料庫操作介面
     * @param serverFileMetaRepository 伺服器檔案元資料操作介面
     * @param userFileMetaRepository 用戶檔案元資料操作介面
     * @param redisProvider Redis 緩存提供者
     * @param gridFsProvider GridFS 儲存提供者
     * @param transfersTasksManager 檔案傳輸任務管理器
     * @param fileProperties 檔案設定屬性
     * @param circuitBreakerConfig 斷路器設定
     * @param userRepository 用戶資料庫操作介面
     * @param userOnlineFileRepository 用戶線上檔案操作介面
     * @param entityOperations R2DBC 實體操作介面
     * @param fileTrashRecordRepository 檔案回收站記錄操作介面
     * @param transactionalOperator 事務操作器
     * @param rateLimiterConfig 限流器設定
     * @param userFIleShareRecordRepository 用戶檔案分享記錄操作介面
     * @param objectMapper JSON 序列化工具
     * @param cacheManager 緩存管理器
     * @param folderListTreeProvider 資料夾樹狀結構提供者（可選）
     * @param fileScanProvider 檔案安全掃描提供者（可選）
     */
    public OnlineFileServiceImpl(UserOnlineFileHistoryRepository userOnlineFileHistoryRepository, ServerFileMetaRepository serverFileMetaRepository, UserFileMetaRepository userFileMetaRepository, RedisProvider redisProvider, GridFsProvider gridFsProvider, TransfersTasksManager transfersTasksManager, FileProperties fileProperties, CircuitBreakerConfig circuitBreakerConfig, UserRepository userRepository, UserOnlineFileRepository userOnlineFileRepository, R2dbcEntityOperations entityOperations, FileTrashRecordRepository fileTrashRecordRepository, TransactionalOperator transactionalOperator, RateLimiterConfig rateLimiterConfig, UserFIleShareRecordRepository userFIleShareRecordRepository, ObjectMapper objectMapper, CacheManager cacheManager,
                                 @Nullable FolderListTreeProvider folderListTreeProvider, @Nullable FileScanProvider fileScanProvider) {
        super(serverFileMetaRepository,
              userFileMetaRepository,
              userOnlineFileRepository,
              userRepository,
              redisProvider,
              gridFsProvider,
              fileScanProvider,
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
     * 上傳線上檔案並建立初始文檔結構。
     * <p>
     * 根據檔案元資料建立新的線上檔案記錄，設定初始空內容和基本屬性。
     * 完成後清理相關快取並回傳上傳結果。
     *
     * @param fileMetadataDTO 檔案元資料，包含檔案名稱、父目錄等資訊
     * @param user 當前操作的用戶
     * @return 上傳結果的響應式包裝，包含進度和成功狀態
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
     * 下載線上檔案內容或轉換為指定格式。
     * <p>
     * 根據檔案元資料查找線上檔案，支援兩種下載模式：直接下載 JSON 格式內容
     * 或透過內容轉換提供者轉換為其他格式（如 DOCX）。更新檔案最後存取時間。
     *
     * @param userFileMetadata 檔案元資料，包含檔案 ID 和基本資訊
     * @param user 當前操作的用戶
     * @param optional 可選參數，指定下載類型
     * @return 包含檔案資料和元資料的業務物件
     */
    @Override
    public Mono<UserFileDataBO> downloadFile(UserFileMetadata userFileMetadata, User user, String... optional) {
        return findUserOnlineFileById(userFileMetadata.getId().toString()).flatMap(userOnlineFile -> {
            userFileMetadata.setLastAccessTime(LocalDateTime.now());
            userFileMetaRepository.save(userFileMetadata).subscribeOn(Schedulers.boundedElastic()).subscribe();

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
                return Mono.just(new UserFileDataBO(userOnlineFile, userFileMetadata, content));
            } catch (JsonProcessingException e) {
                return Mono.error(new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
            }
        });
    }


    /**
     * 刪除線上檔案。
     * <p>
     * 委託給父類執行標準的檔案刪除操作，包括邏輯刪除、
     * 快取清理和相關記錄更新。
     *
     * @param fileMetadata 檔案元資料
     * @param user 當前操作的用戶
     * @return 表示刪除操作完成的響應式信號
     */
    @Override
    public Mono<Void> deleteFile(UserFileMetadata fileMetadata, User user) {
        return super.deleteFile(fileMetadata, user);
    }


    /**
     * 編輯線上檔案內容或屬性。
     * <p>
     * 根據編輯類型執行不同操作：編輯元資料、編輯內容、建立歷史記錄、
     * 還原歷史版本或刪除歷史記錄。使用策略模式處理各種編輯操作。
     *
     * @param fileEditBO 編輯檔案的業務物件，包含檔案 ID 和編輯類型
     * @param user 當前操作的用戶
     * @return 表示編輯操作完成的響應式信號
     */
    @Override
    @RequirePermission(PermissionEnum.WRITE)
    public Mono<Void> editFile(FileEditBO fileEditBO, User user) {
        FileEditDTO fileEditDTO = fileEditBO.getFileEditDTO();
        return findUserOnlineFileById(fileEditDTO.getFileId()).flatMap(userOnlineFile -> switch (fileEditDTO.getEditType()) {
            case EDIT_METADATA -> super.editFile(fileEditBO, user);
            case EDIT_CONTENT -> saveContent(userOnlineFile, fileEditBO, user);
            case BUILD_HISTORY_RECORD -> buildHistoryRecord(userOnlineFile, fileEditBO, user);
            case REVERT_HISTORY_RECORD -> revertHistoryRecord(userOnlineFile, fileEditBO, user);
            case DELETE_HISTORY_RECORD -> deleteHistoryRecord(userOnlineFile, fileEditDTO.getVersion());
        });
    }


    /**
     * 更新用戶檔案元資料的存取時間。
     * <p>
     * 設定檔案的最後存取時間為目前時間，然後儲存至資料庫。
     * 操作在有界調度器中執行以確保非阻塞性。
     *
     * @param userFileMetadata 用戶檔案元資料物件
     * @return 更新後的檔案元資料
     */
    public Mono<UserFileMetadata> updateUserFileMetadata(UserFileMetadata userFileMetadata) {
        return Mono.defer(() -> {
            userFileMetadata.setLastAccessTime(LocalDateTime.now());
            return userFileMetaRepository.save(userFileMetadata);
        }).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * 根據檔案 ID 查找用戶線上檔案。
     * <p>
     * 查找指定 ID 的線上檔案記錄。若檔案不存在則拋出驗證異常。
     *
     * @param fileId 檔案的唯一識別碼
     * @return 線上檔案物件的響應式包裝
     */
    private Mono<UserOnlineFile> findUserOnlineFileById(String fileId) {
        return userOnlineFileRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)));
    }


    /**
     * 取得線上檔案的版本歷史列表。
     * <p>
     * 根據檔案 ID 查詢所有歷史版本，支援分頁顯示。使用版本號由新到舊的順序排列。
     *
     * @param user 當前操作的用戶
     * @param fileMetadata 檔案元資料
     * @param page 當前頁碼
     * @param size 每頁顯示數量
     * @return 包含版本資訊的分頁響應結果
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
     * 建立線上檔案的初始歷史記錄。
     * <p>
     * 為檔案建立版本 0 的初始快照記錄，記錄當前的檔案內容、
     * 修改者和時間等資訊。這是版本控制系統的起始點。
     *
     * @param userOnlineFile 線上檔案物件
     * @param fileEditDTO 檔案編輯資訊
     * @param contentJson 檔案內容的 JSON 字串
     * @return 初始歷史記錄物件
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
     * 儲存編輯後的檔案內容。
     * <p>
     * 操作步驟：格式化新內容為 JSON 形式，更新檔案的修改者和歷史理合狀態，
     * 然後同步更新檔案內容和元資料。空內容會被設為預設的空 JSON 結構。
     *
     * @param userOnlineFile 線上檔案物件
     * @param fileEditBO 檔案編輯業務物件
     * @param user 當前操作的用戶
     * @return 表示儲存操作完成的響應式信號
     */
    private Mono<Void> saveContent(UserOnlineFile userOnlineFile, FileEditBO fileEditBO, User user) {
        return Mono.defer(() -> {
            userOnlineFile.setLastModifiedBy(user.getId());
            userOnlineFile.setIsMatchHistory(false);
            FileEditDTO fileEditDTO = fileEditBO.getFileEditDTO();
            if (fileEditDTO.getContent() == null || fileEditDTO.getContent().isEmpty()) {
                userOnlineFile.setContent(EMPTY_CONTENT);
                return Mono.just(userOnlineFile);
            }

            return formatObjectToJson(fileEditDTO.getContent()).flatMap(contentJson -> {
                userOnlineFile.setContent(contentJson);
                return Mono.just(userOnlineFile);
            });
        }).then(userOnlineFileRepository.save(userOnlineFile).then(updateUserFileMetadata(fileEditBO.getUserFileMetadata()))).then();
    }


    /**
     * 根據檔案編輯內容創建新的歷史記錄。
     * <p>
     * 該方法會根據當前檔案內容和修改記錄生成新的歷史記錄。若檔案內容無變動，則會回傳錯誤。
     *
     * @param userOnlineFile 用戶在線檔案對象，包含當前檔案的基本信息。
     * @param fileEditBO     編輯檔案資料傳輸對象，包含檔案的修改內容。
     * @param user           當前操作的用戶。
     *
     * @return Mono<Void> 空的 Mono，表示操作完成。
     */
    private Mono<Void> buildHistoryRecord(UserOnlineFile userOnlineFile, FileEditBO fileEditBO, User user) {
        FileEditDTO fileEditDTO = fileEditBO.getFileEditDTO();
        if (userOnlineFile.getIsMatchHistory() == null || userOnlineFile.getLastHistoryVersion() == null) {
            return formatObjectToJson(fileEditDTO.getContent()).flatMap(newContent -> {
                userOnlineFile.setContent(newContent);
                userOnlineFile.setIsMatchHistory(true);
                userOnlineFile.setLastHistoryVersion(0L);
                userOnlineFile.setCurrentSnapshotCount(0);
                userOnlineFile.setLastModifiedBy(user.getId());
                return createInitialHistory(userOnlineFile, fileEditDTO, newContent);
            }).then(userOnlineFileRepository.save(userOnlineFile).then(updateUserFileMetadata(fileEditBO.getUserFileMetadata()))).then();
        }

        Mono<EditorContentDTO> lastContentJsonDTOMono;
        if (userOnlineFile.getIsMatchHistory()) {
            lastContentJsonDTOMono = formatJsonToEditorContentJsonDTO(userOnlineFile.getContent());
        } else {
            lastContentJsonDTOMono = userOnlineFileHistoryRepository
                    .findByFileIdAndVersion(userOnlineFile.getId(), userOnlineFile.getLastHistoryVersion())
                    .flatMap(this::getCompleteContent);
        }


        return lastContentJsonDTOMono
                .flatMap(compareContentDTO -> Mono.defer(() -> {
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
                        .findTopNByFileIdOrderByVersionDesc(userOnlineFile.getId(), 1)
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
                        })
                        .then(Mono.when(updateUserFileMetadata(fileEditBO.getUserFileMetadata()), deleteExcessHistoryRecord(userOnlineFile))));
    }


    /**
     * 將指定的歷史記錄轉換成完整的檔案內容。
     * <p>
     * 根據歷史記錄和必要的補丁，還原出檔案的最終內容。
     *
     * @param targetHistory 目標歷史記錄，包含檔案的某個歷史版本。
     *
     * @return Mono<EditorContentDTO> 還原後的檔案內容。
     */
    private Mono<EditorContentDTO> getCompleteContent(UserOnlineFileHistory targetHistory) {
        return Mono.defer(() -> {
            Stack<UserOnlineFileHistory> historyStack = new Stack<>();
            return findHistoryChainRecursive(targetHistory, historyStack);
        }).flatMap(historyChain -> {
            if (historyChain.isEmpty() || !historyChain.getLast().getIsSnapshot()) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_HISTORY_CHAIN,
                                                          targetHistory.getFileId(),
                                                          targetHistory.getId()
                ));
            }

            String baseContent = historyChain.getLast().getSnapshotContent();
            return formatJsonToEditorContentJsonDTO(baseContent).flatMap(editorContentDTO -> applyPatchToContent(editorContentDTO, historyChain));
        });
    }


    /**
     * 遞歸查找並構建歷史記錄鏈。
     * <p>
     * 該方法會遞歸查找歷史記錄的鏈，直到找到快照版本為止，並將結果回傳。
     *
     * @param currentHistory 當前的歷史記錄。
     * @param chain          歷史記錄的鏈，會逐步添加歷史記錄。
     *
     * @return Mono<Stack < UserOnlineFileHistory>> 回傳完整的歷史記錄鏈。
     */
    private Mono<Stack<UserOnlineFileHistory>> findHistoryChainRecursive(UserOnlineFileHistory currentHistory, Stack<UserOnlineFileHistory> chain) {
        chain.push(currentHistory);

        if (currentHistory.getIsSnapshot()) {
            return Mono.just(chain);
        }

        if (currentHistory.getPreviousVersion() == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_VERSION_CHAIN,
                                                      currentHistory.getVersion(),
                                                      String.format("檔案: %d 沒有前一版本", currentHistory.getFileId())
            ));
        }

        return userOnlineFileHistoryRepository
                .findByFileIdAndVersion(currentHistory.getFileId(), currentHistory.getPreviousVersion())
                .flatMap(previousHistory -> findHistoryChainRecursive(previousHistory, chain));
    }


    /**
     * 還原歷史記錄
     * 此方法根據給定的版本號還原檔案的歷史記錄。如果是快照版本，將直接還原；如果是增量版本，則會計算並應用補丁。
     *
     * @param userOnlineFile 用戶正在編輯的在線檔案。
     * @param fileEditBO     用於編輯檔案的資料傳輸對象，包含檔案內容和版本號。
     * @param user           當前執行還原操作的用戶。
     *
     * @return 空Mono
     */
    private Mono<Void> revertHistoryRecord(UserOnlineFile userOnlineFile, FileEditBO fileEditBO, User user) {
        Long targetVersion = fileEditBO.getFileEditDTO().getVersion();
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
        }).then(Mono.when(updateUserFileMetadata(fileEditBO.getUserFileMetadata()), deleteExcessHistoryRecord(userOnlineFile)));
    }


    /**
     * 計算兩個檔案內容的差異。
     * <p>
     * 該方法將會比較舊內容和新內容之間的差異，並回傳差異的表示。
     *
     * @param oldContent 舊檔案內容。
     * @param newContent 新檔案內容。
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
     * 此方法用於應用還原差異，將給定的差異（patch）應用於當前的檔案內容。若無效或無法解析差異，會拋出相應的異常。
     *
     * @param contents  檔案內容
     * @param patchJson 差異
     *
     * @return 還原後的檔案內容
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
     * 將物件格式化為 JSON 字串的通用轉換方法。
     * <p>
     * 使用 Jackson ObjectMapper 將任意物件序列化為 JSON 格式，
     * 主要用於線上檔案內容的持久化儲存。支援複雜的巢狀物件結構，
     * 確保資料完整性和格式一致性。
     * <p>
     * <strong>轉換特性：</strong>
     * <ul>
     *   <li>支援所有可序列化的 Java 物件</li>
     *   <li>自動處理日期、集合、陣列等複雜類型</li>
     *   <li>保持原始資料結構和類型資訊</li>
     *   <li>統一的錯誤處理和異常轉換</li>
     * </ul>
     * <p>
     * <strong>使用場景：</strong>
     * <ul>
     *   <li>線上檔案內容的資料庫儲存</li>
     *   <li>檔案歷史版本的快照建立</li>
     *   <li>檔案編輯內容的暫存處理</li>
     * </ul>
     *
     * @param content 需要轉換的物件，可以是任何可序列化的類型
     * @return 包含 JSON 字串的響應式包裝，轉換失敗時會發出錯誤信號
     */
    @SkipRecord
    private Mono<String> formatObjectToJson(Object content) {
        try {
            return Mono.just(objectMapper.writeValueAsString(content));
        } catch (Exception e) {
            return Mono.error(new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
        }
    }


    /**
     * 將編輯器內容 Delta 格式轉換為字串行列表。
     * <p>
     * 此方法是差異演算法的核心組件，將 Quill 編輯器的 Delta 格式
     * 轉換為可比較的字串行。每個 Delta 操作都會被序列化為獨立的 JSON 字串，
     * 便於後續的文字差異計算和版本比較。
     * <p>
     * <strong>轉換流程：</strong>
     * <ol>
     *   <li>遍歷編輯器內容的所有 Delta 操作</li>
     *   <li>將每個 Delta 操作序列化為 JSON 字串</li>
     *   <li>組合成字串行列表供差異演算法使用</li>
     * </ol>
     * <p>
     * <strong>應用場景：</strong>
     * <ul>
     *   <li>檔案版本間的內容比較</li>
     *   <li>歷史記錄的差異計算</li>
     *   <li>協作編輯的衝突檢測</li>
     * </ul>
     *
     * @param content 編輯器內容 DTO，包含 Delta 操作序列
     * @return 字串行列表，每行代表一個 Delta 操作的 JSON 表示
     */
    @SkipRecord
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
     * 將JSON格式的字符串轉換為EditorContentJsonDTO對象。
     * <p>
     * 該方法將會嘗試將JSON字符串解析成指定的DTO對象，並處理解析過程中的錯誤。
     *
     * @param json JSON格式的字符串，表示檔案的內容。
     *
     * @return Mono<EditorContentDTO> 轉換後的EditorContentDTO對象。
     */
    private Mono<EditorContentDTO> formatJsonToEditorContentJsonDTO(String json) {
        return Mono
                .fromCallable(() -> objectMapper.readValue(json, EditorContentDTO.class))
                .onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
    }


    /**
     * 將字串行列表格式化為編輯器內容 DTO 物件。
     * <p>
     * 這是差異演算法還原過程的關鍵方法，將經過差異處理的字串行列表
     * 重新轉換為可用的編輯器內容格式。每個字串行都代表一個 Delta 操作，
     * 需要反序列化並重組為完整的編輯器內容結構。
     * <p>
     * <strong>轉換流程：</strong>
     * <ol>
     *   <li>逐行解析 JSON 字串為 Delta DTO 物件</li>
     *   <li>驗證每個 Delta 操作的格式正確性</li>
     *   <li>組合所有 Delta 操作為完整的編輯器內容</li>
     *   <li>回傳可直接使用的編輯器內容 DTO</li>
     * </ol>
     * <p>
     * <strong>錯誤處理：</strong>
     * <ul>
     *   <li>JSON 解析錯誤會被包裝為 ProcessException</li>
     *   <li>格式不正確的 Delta 操作會導致還原失敗</li>
     *   <li>空列表會產生空的編輯器內容</li>
     * </ul>
     *
     * @param deltaList 差異字串行列表，每行包含一個 Delta 操作的 JSON 表示
     * @return 包含完整編輯器內容的響應式包裝，還原失敗時會發出錯誤信號
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
     * 保存檔案歷史
     * 此方法保存一個新的檔案歷史記錄。它會根據檔案內容創建新的快照或增量版本。
     *
     * @param userOnlineFile    用戶正在編輯的在線檔案
     * @param editorContentJson 檔案內容的 JSON 字符串
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
     * 若刪除的是快照版本，則會更新歷史鏈中的其他版本，並處理檔案版本的增量更新或重設。
     *
     * @param onlineFile 用戶正在編輯的在線檔案
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
     * 應用補丁至檔案內容
     * 此方法將歷史版本的補丁應用到檔案內容，並回傳最終還原後的內容。
     *
     * @param editorContentDTO 當前檔案內容的 DTO。
     * @param historyChain     歷史版本鏈，按時間順序保存。
     *
     * @return 還原後的檔案內容
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
     * @param onlineFile 用戶在線檔案
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

