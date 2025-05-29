package xyz.dowob.filemanagement.service.serviceInterface;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.manager.CacheManager;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.FileScanProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.*;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.UploadTaskBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dao.ServerFileMetaCountDAO;
import xyz.dowob.filemanagement.data.file.dao.UserFileMetaWithDataDAO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.data.file.po.FluxDataPO;
import xyz.dowob.filemanagement.data.file.po.ShareUserEditPO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.*;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.CacheRule;
import xyz.dowob.filemanagement.repostiory.*;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 檔案服務的抽象類，包含共通的上傳、下載邏輯
 */
@RequiredArgsConstructor
@RecordLevel(LogLevelEnum.DEBUG)
public abstract class AbstractFileService implements FileService {
    /**
     * 用戶文件列表根目錄的緩存鍵格式
     */
    private static final String ROOT_PAGE_KEY_FORMAT = "fileList_folder:0_user:%s";

    /**
     * 用戶文件列表的緩存鍵格式
     */
    private static final String GENERAL_PAGE_KEY_FORMAT = "fileList_folder:%s";

    /**
     * 服務器文件元數據庫操作對象
     */
    protected final ServerFileMetaRepository serverFileMetaRepository;

    /**
     * 用戶文件元數據庫操作對象
     */
    protected final UserFileMetaRepository userFileMetaRepository;

    /**
     * 用戶在線文件數據庫操作對象
     */
    protected final UserOnlineFileRepository userOnlineFileRepository;

    /**
     * 用戶數據庫操作對象
     */
    protected final UserRepository userRepository;

    /**
     * Redis操作對象
     */
    protected final RedisProvider redisProvider;

    /**
     * GridFs操作對象
     */
    protected final GridFsProvider gridFsProvider;

    /**
     * 檔案掃描操作對象
     */
    protected final FileScanProvider fileScanProvider;

    /**
     * 文件上傳任務管理器
     */
    protected final TransfersTasksManager transfersTasksManager;

    /**
     * 文件配置屬性
     */
    protected final FileProperties fileProperties;

    /**
     * 斷路器設定
     */
    protected final CircuitBreakerConfig circuitBreakerConfig;

    /**
     * 速率器設定
     */
    protected final RateLimiterConfig rateLimiterConfig;

    /**
     * 文件列表樹提供者
     */
    protected final FolderListTreeProvider folderListTreeProvider;

    /**
     * 檔案垃圾桶記錄數據庫操作對象
     */
    protected final FileTrashRecordRepository fileTrashRecordRepository;

    /**
     * 用戶文件元數據庫操作對象
     */
    protected final R2dbcEntityOperations entityOperations;

    /**
     * 事務操作器
     */
    protected final TransactionalOperator transactionalOperator;

    /**
     * 用戶文件分享記錄數據庫操作對象
     */
    protected final UserFIleShareRecordRepository userFIleShareRecordRepository;

    /**
     * 映射轉換器
     */
    protected final ObjectMapper objectMapper;

    /**
     * 緩存管理器
     */
    protected final CacheManager cacheManager;

    /**
     * 每個分塊的大小
     */
    protected Long CHUNK_SIZE;


    /**
     * 初始化方法，獲取文件配置中的分塊大小
     */
    @PostConstruct
    public void init() {
        if (fileProperties.getUpload().getChunkSize().toBytes() <= 0) {
            throw new IllegalArgumentException("分塊大小必須大於0");
        }
        CHUNK_SIZE = fileProperties.getUpload().getChunkSize().toBytes();

        if (!fileProperties.getUpload().getMaxUploadDuration().isPositive()) {
            throw new IllegalArgumentException("單次最久上傳時間必須大於0");
        }
    }


    /**
     * 獲取用戶文件列表的共通實現，會先將過濾條件轉換成合法的查詢條件，
     * 若查詢近期文件，則使用會覆蓋過濾條件中的分頁大小，
     * 然後從用戶文件元數據庫中獲取文件元數據，並僅在查詢根目錄或特定文件夾時會寫入緩存。
     * 最後將轉換後的文件元數據轉換為用戶文件列表DTO並經過過濾和分頁處理後返回。
     *
     * @param user          用戶信息
     * @param fileFilterDTO 文件過濾條件
     *
     * @return Mono<PagedResponseDTO < UserFileListDTO>> 用戶文件列表分頁響應對象
     */
    @HideOverLength
    public Mono<PagedResponseDTO<UserFileListDTO>> getUserFileList(User user, FileFilterDTO fileFilterDTO) {
        String key = getUserFileListBaseKey(user.getId(), fileFilterDTO.getFolderId());

        if (fileFilterDTO.getPageSize() == null || fileFilterDTO.getPageSize() < 1) {
            fileFilterDTO.setPageSize(fileProperties.getGlobal().getPageSize());
        }

        if (fileFilterDTO.getPage() == null || fileFilterDTO.getPage() < 1) {
            fileFilterDTO.setPage(1);
        }

        if (Objects.equals(fileFilterDTO.getFolderId(), ReservedSearchIdEnum.RECENT_FILE_ID.getId())) {
            fileFilterDTO.setPageSize(fileProperties.getGlobal().getShowRecentFileCount());
        }

        Flux<UserFileMetaWithDataDAO> dataDAOs = userFileMetaRepository.getUserFileMetaWithDataDAO(user.getId(), fileFilterDTO, entityOperations);
        Flux<UserFileListDTO> getUserFileListDTOFlux = formatUnifiedDaoToDto(dataDAOs);

        if (fileFilterDTO.getFolderId() == null || fileFilterDTO.getFolderId() < 0) {
            return filterAndPageResponse(formatUnifiedDaoToDto(dataDAOs), fileFilterDTO);
        }

        CacheRule<UserFileListDTO> cacheRule = cacheManager.generateCacheRule(key, CacheProviderEnum.USER_FILE_LIST_CACHE);
        Flux<UserFileListDTO> listDTOFlux = cacheManager.runAndSetCache(key,
                                                                        UserFileListDTO.class,
                                                                        CacheProviderEnum.USER_FILE_LIST_CACHE,
                                                                        getUserFileListDTOFlux,
                                                                        Collections.singletonList(cacheRule)
        );
        return filterAndPageResponse(listDTOFlux, fileFilterDTO);
    }


    /**
     * 將獲取當查詢後的檔案數據並查詢分享用戶信息，最後將其轉換為用戶文件列表DTO流。
     *
     * @param unifiedDaoFlux 統一的DAO流
     *
     * @return Flux<UserFileListDTO> 用戶文件列表DTO流
     */
    private Flux<UserFileListDTO> formatUnifiedDaoToDto(Flux<UserFileMetaWithDataDAO> unifiedDaoFlux) {
        return unifiedDaoFlux.collectList().flatMapMany(daoList -> {
            if (daoList.isEmpty()) {
                return Flux.empty();
            }

            Set<Long> userFileIds = daoList.stream().map(UserFileMetaWithDataDAO::getUfmId).collect(Collectors.toSet());

            Mono<Map<Long, Set<Long>>> shareUserMapMono = userFIleShareRecordRepository
                    .findAllByFileIdIn(userFileIds)
                    .collectMultimap(UserFileShareRecord::getFileId, UserFileShareRecord::getUserId)
                    .map(multimap -> {
                        Map<Long, Set<Long>> map = new HashMap<>();
                        multimap.forEach((fileId, userIds) -> map.put(fileId, new HashSet<>(userIds)));
                        return map;
                    })
                    .defaultIfEmpty(Collections.emptyMap());
            return shareUserMapMono.flatMapMany(shareUserMap -> Flux
                    .fromIterable(daoList)
                    .map(dao -> new UserFileListDTO(dao, shareUserMap.getOrDefault(dao.getUfmId(), Collections.emptySet()))));
        });
    }


    /**
     * 過濾所需的檔案元素並分頁
     *
     * @param fileListDTOFlux 檔案列表流
     * @param fileFilterDTO   文件過濾DTO
     *
     * @return Mono<PagedResponseDTO < UserFileListDTO>> 檔案總數和分頁後的檔案列表
     */
    private Mono<PagedResponseDTO<UserFileListDTO>> filterAndPageResponse(Flux<UserFileListDTO> fileListDTOFlux, FileFilterDTO fileFilterDTO) {
        return filterPageElements(fileListDTOFlux, fileFilterDTO).flatMap(tuple -> {
            int pageSize = fileFilterDTO.getPageSize();
            int currentPage = fileFilterDTO.getPage();
            PagedResponseDTO<UserFileListDTO> pagedResponseDTO = new PagedResponseDTO<>();
            pagedResponseDTO.setData(tuple.getT2());
            pagedResponseDTO.setTotalElements(tuple.getT1());
            pagedResponseDTO.setPageSize(pageSize);
            pagedResponseDTO.setCurrentPage(currentPage);
            pagedResponseDTO.setTotalPages((int) Math.ceil((double) tuple.getT1() / pageSize));
            return Mono.just(pagedResponseDTO);
        });
    }


    /**
     * 過濾所需的檔案元素並分頁
     *
     * @param flux<UserFileListDTO> 檔案列表流
     * @param fileFilterDTO         文件過濾DTO
     *
     * @return Mono<Tuple2 < Integer, List < UserFileListDTO>>> 檔案總數和分頁後的檔案列表
     */
    private Mono<Tuple2<Integer, List<UserFileListDTO>>> filterPageElements(Flux<UserFileListDTO> flux, FileFilterDTO fileFilterDTO) {
        List<FileEnum> type = fileFilterDTO.getTypes();
        LocalDateTime startTime = fileFilterDTO.getStartTime();
        LocalDateTime endTime = fileFilterDTO.getEndTime();
        Predicate<UserFileListDTO> timeFilter = userFileListDTO -> {
            if (startTime == null && endTime == null) {
                return true;
            }

            boolean matchesStart = true;
            boolean matchesEnd = true;

            if (startTime != null) {
                matchesStart = !userFileListDTO.getLastAccessTime().isBefore(startTime);
            }

            if (endTime != null) {
                matchesEnd = !userFileListDTO.getLastAccessTime().isAfter(endTime);
            }

            return matchesStart && matchesEnd;
        };

        Predicate<UserFileListDTO> typeFilter = userFileListDTO -> {
            if (type == null || type.isEmpty()) {
                return true;
            }
            return type.contains(userFileListDTO.getFileType());
        };

        int currentPage = fileFilterDTO.getPage();
        int pageSize = fileFilterDTO.getPageSize();

        return flux.filter(typeFilter).filter(timeFilter).collectList().map(list -> {
            int size = list.size();
            int start = Math.min(Math.max((currentPage - 1), 0) * pageSize, size);
            int end = Math.min(start + pageSize, size);
            List<UserFileListDTO> subList = list.subList(start, end);
            return Tuples.of(size, subList);
        });
    }


    /**
     * 獲取用戶文件列表路徑的共通實現
     *
     * @param file 文件
     * @param user 用戶信息
     *
     * @return Mono<List < FolderListTreeProvider.FolderNode>> 用戶文件路徑節點
     */
    public Mono<List<FolderListTreeProvider.FolderNode>> getUserFilePaths(UserFileMetadata file, User user) {
        if (!Objects.equals(file.getUserId(), user.getId())) {
            List<FolderListTreeProvider.FolderNode> list = Collections.singletonList(new FolderListTreeProvider.FolderNode(null, "root"));
            return Mono.just(list);
        }

        return Mono.just(file).flatMap(userFileMetadata -> {
            if (folderListTreeProvider != null) {
                LogUnity.trace("使用 FolderListTreeProvider 獲取用戶文件路徑");
                List<FolderListTreeProvider.FolderNode> path = folderListTreeProvider.getPath(user.getId(), file.getId());
                return Mono.just(path);
            }

            LogUnity.trace("使用 UserFileMetaRepository 獲取用戶文件路徑");
            return Flux
                    .just(userFileMetadata)
                    .expand(metadata -> Optional
                            .ofNullable(metadata.getParentFolderId())
                            .map(folderId -> userFileMetaRepository.findById(folderId.toString()))
                            .orElse(Mono.empty()))
                    .map(FolderListTreeProvider.FolderNode::new)
                    .collectList()
                    .map(list -> {
                        if (file.getId() != 0L) {
                            list.add(new FolderListTreeProvider.FolderNode(null, "root"));
                        }
                        return list;
                    });
        }).switchIfEmpty(Mono.defer(() -> {
            if (file.getId() <= 0) {
                List<FolderListTreeProvider.FolderNode> list = Collections.singletonList(new FolderListTreeProvider.FolderNode(null, "root"));
                return Mono.just(list);
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, file.getId()));
        }));
    }


    /**
     * 獲取用戶文件列表的緩存鍵
     * 如果搜索ID為空，則使用根文件夾ID
     * 當搜索ID為根文件夾ID時，使用用戶ID作為緩存鍵名 {@code fileList_folder:0_user:%s}
     * 否則使用搜索ID作為緩存鍵名 {@code fileList_folder:%s}
     *
     * @param userId   用戶ID
     * @param searchId 搜索ID
     *
     * @return 緩存鍵名
     */
    @SkipRecord
    protected String getUserFileListBaseKey(Long userId, Long searchId) {
        Long chooseId = Objects.requireNonNullElse(searchId, ReservedSearchIdEnum.ROOT_FOLDER_ID.getId());
        if (chooseId.equals(ReservedSearchIdEnum.ROOT_FOLDER_ID.getId())) {
            return String.format(ROOT_PAGE_KEY_FORMAT, userId);
        }
        return String.format(GENERAL_PAGE_KEY_FORMAT, searchId);
    }


    /**
     * 搜索用戶文件，會從資料庫查詢對應的數據並轉換成 用戶文件列表DTO。
     * 最後經過過濾和分頁處理，返回符合條件的用戶文件列表。
     *
     * @param user          用戶信息
     * @param fileFilterDTO 文件過濾條件
     *
     * @return Mono<PagedResponseDTO < UserFileListDTO>> 用戶文件列表分頁響應對象
     */
    public Mono<PagedResponseDTO<UserFileListDTO>> searchUserFile(User user, FileFilterDTO fileFilterDTO) {
        if (fileFilterDTO.getPageSize() == null || fileFilterDTO.getPageSize() < 1) {
            fileFilterDTO.setPageSize(fileProperties.getGlobal().getPageSize());
        }

        if (fileFilterDTO.getPage() == null || fileFilterDTO.getPage() < 1) {
            fileFilterDTO.setPage(1);
        }

        Flux<UserFileMetaWithDataDAO> dataFlux = userFileMetaRepository.findAllByUserIdAndFilterDTO(user.getId(), fileFilterDTO, entityOperations);
        return filterAndPageResponse(formatUnifiedDaoToDto(dataFlux), fileFilterDTO);
    }


    /**
     * 上傳文件的共通實現
     *
     * @param fileMetadataDTO 文件元數據
     * @param user            用戶信息
     *
     * @return Mono<UploadResponseDTO>
     */
    public Mono<UploadResponseDTO> uploadFile(FileMetadataDTO fileMetadataDTO, User user) {
        fileMetadataDTO.setUser(user);
        return serverFileMetaRepository.findByMd5(fileMetadataDTO.getMd5()).flatMap(existingFile -> {
            existingFile.getOwners().add(user.getId());
            existingFile.setLastAccessTime(LocalDateTime.now());

            return serverFileMetaRepository
                    .save(existingFile)
                    .then(associateUserFile(existingFile, fileMetadataDTO).flatMap(userFileMetaRepository::save))
                    .then(handleUserStorage(user, existingFile.getFileSize(), false))
                    .then(cleanUserListCache(user.getId(), fileMetadataDTO.getParentFolderId()))
                    .thenReturn(UploadResponseDTO.builder().progress(100.0).isSuccess(true).isFinished(true).message("上傳成功").build());
        }).switchIfEmpty(initialUpload(fileMetadataDTO));
    }


    /**
     * 上傳文件分塊的共通實現
     *
     * @param uploadChunkDTO 上傳文件數據
     *
     * @return Mono<UploadResponseDTO> 上傳響應數據
     */
    public Mono<UploadResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO) {
        String transferTaskId = uploadChunkDTO.getTransferTaskId();
        String key = "upload_task:" + transferTaskId;
        String pendingChunkKey = key + ":pending_chunks";

        return Mono.defer(() -> redisProvider
                .getHashMap(key, "DTO")
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_UPLOAD_TASK, transferTaskId)))
                .flatMap(task -> redisProvider.isChunkSetPending(pendingChunkKey, uploadChunkDTO.getChunkIndex()).flatMap(isPending -> {
                    if (isPending) {
                        return processChunk(uploadChunkDTO, transferTaskId, key, pendingChunkKey);
                    }
                    return redisProvider.getHashMap(key, "uploaded_count").flatMap(uploadCount -> {
                        Long uploadCountLong = (Long) uploadCount;
                        double progress = (uploadCountLong.doubleValue() / uploadChunkDTO.getTotalChunks()) * 100.0;
                        String message = String.format("文件分塊: %d 已上傳", uploadChunkDTO.getChunkIndex());
                        UploadResponseDTO responseDTO = UploadResponseDTO
                                .builder()
                                .chunkIndex(uploadChunkDTO.getChunkIndex())
                                .transferTaskId(transferTaskId)
                                .progress(progress)
                                .isSuccess(true)
                                .isFinished(false)
                                .message(message)
                                .totalChunks(uploadChunkDTO.getTotalChunks())
                                .build();

                        if (uploadCountLong.intValue() == uploadChunkDTO.getTotalChunks()) {
                            fileCheck(transferTaskId, uploadChunkDTO.getTotalChunks()).subscribeOn(Schedulers.boundedElastic()).subscribe();
                            responseDTO.setIsFinished(true);
                        }
                        return Mono.just(responseDTO);
                    });
                })));
    }


    /**
     * 下載文件的共通實現
     *
     * @param userFileMetadata 文件
     * @param user             用戶信息
     * @param optional         其他可選參數(此處為請求頭的Range)
     *
     * @return Mono<UserFileDataBO> 文件數據對象
     */
    public Mono<UserFileDataBO> downloadFile(UserFileMetadata userFileMetadata, User user, String... optional) {
        return Mono.defer(() -> {
            userFileMetadata.setLastAccessTime(LocalDateTime.now());
            userFileMetaRepository.save(userFileMetadata).subscribeOn(Schedulers.boundedElastic()).subscribe();
            return getByServerFileMetadataId(userFileMetadata.getServerFileId()).switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.USER_HAVE_NOT_EXIST_SERVER_FILE,
                                                                                                                               userFileMetadata.getServerFileId(),
                                                                                                                               userFileMetadata.getId()
            )));
        }).flatMap(serverFileMetadata -> Mono.just(new UserFileDataBO(serverFileMetadata, userFileMetadata)).flatMap(userFileDataBO -> {
            Mono<FluxDataPO<DataBuffer>> fluxDataPOMono = Mono.defer(() -> {
                FluxDataPO<DataBuffer> dataBufferPO = new FluxDataPO<>();
                Flux<DataBuffer> dataBufferFlux = gridFsProvider
                        .findFileById(new ObjectId(userFileDataBO.getGridFsId()))
                        .switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.GRIDFS_FILE_NOT_FOUND,
                                                                       userFileDataBO.getServerFileId()
                        )))
                        .flatMap(gridFsProvider::getResource)
                        .flatMapMany(ReactiveGridFsResource::getDownloadStream)
                        .share();
                dataBufferPO.setTFlux(dataBufferFlux);
                return Mono.just(dataBufferPO);
            });

            return cacheManager
                    .runAndSetCache(userFileDataBO.getGridFsId(),
                                    FluxDataPO.class,
                                    CacheProviderEnum.FILE_STREAM_CACHE,
                                    fluxDataPOMono,
                                    Collections.singletonList(cacheManager.generateCacheRule(userFileDataBO.getGridFsId(),
                                                                                             CacheProviderEnum.FILE_STREAM_CACHE
                                    ))
                    )
                    .flatMap(dataBufferPO -> {
                        long[] range = getRangeFromHeader(optional[0], userFileDataBO.getFileSize());
                        FluxDataPO<DataBuffer> dataBuffer = new FluxDataPO<>();
                        Flux<DataBuffer> dataBufferFlux = dataBuffer.formatAndSet(dataBufferPO.getTFlux(), DataBuffer.class);

                        return Mono.defer(() -> {
                            if (userFileDataBO.getMimeType() == null) {
                                userFileDataBO.setDataBufferFlux(dataBufferFlux);
                                return updateUserFileMetaMimeType(serverFileMetadata, userFileDataBO);
                            }
                            return Mono.just(userFileDataBO);
                        }).doOnNext(fileDataBO -> fileDataBO.setDataBufferFlux(streamFileFromGridFS(dataBufferFlux, range[0], range[1])));
                    });
        }));
    }


    /**
     * 刪除文件的共通實現
     *
     * @param userFileMetadata 文件元數據
     * @param user             用戶信息
     *
     * @return Mono<Void>
     */
    public Mono<Void> deleteFile(UserFileMetadata userFileMetadata, User user) {
        return updateOwner(Collections.singletonList(userFileMetadata), user.getId()).then(Mono.defer(() -> {
            Mono<Void> deleteFile = Mono.empty();
            if (userFileMetadata.getServerFileId() != null) {
                deleteFile = calculateFileSize(user, Collections.singletonList(userFileMetadata.getServerFileId()));
            }
            return deleteFile.then(cleanUserListCache(user.getId(), userFileMetadata.getParentFolderId()).then(userFileMetaRepository.deleteById(
                    userFileMetadata.getId().toString())));
        }));
    }


    /**
     * 編輯文件的共通實現
     * 處理文件名、父文件夾ID、共享用戶ID的更新
     *
     * @param fileEditBO 文件編輯傳輸類
     * @param user       用戶信息
     *
     * @return Mono<Void>
     */
    public Mono<Void> editFile(FileEditBO fileEditBO, User user) {
        FileEditDTO fileEditDTO = fileEditBO.getFileEditDTO();
        UserFileMetadata userFileMetadata = fileEditBO.getUserFileMetadata();
        Mono<UserFileMetadata> processShareUserMono = processShareUser(Collections.singletonList(userFileMetadata), fileEditDTO).next();
        Mono<UserFileMetadata> processFileMono = cacheManager
                .deleteCache(getUserFileListBaseKey(user.getId(), userFileMetadata.getParentFolderId()), CacheProviderEnum.USER_FILE_LIST_CACHE)
                .then(Mono.defer(() -> {
                    userFileMetadata.setFilename(fileEditDTO.getFilename());
                    userFileMetadata.setParentFolderId(fileEditDTO.getParentFolderId());
                    userFileMetadata.setLastAccessTime(LocalDateTime.now());

                    FileShareTypeEnum shareType = Objects.requireNonNullElse(fileEditDTO.getShareType(), userFileMetadata.getShareType());
                    userFileMetadata.setShareType(shareType);

                    Boolean isStar = Objects.requireNonNullElse(fileEditDTO.getIsStar(), userFileMetadata.getIsStar());
                    userFileMetadata.setIsStar(isStar);
                    return userFileMetaRepository.save(userFileMetadata);
                }));

        return Mono
                .zip(processShareUserMono, processFileMono)
                .flatMap(tuple2 -> cleanUserListCache(user.getId(), tuple2.getT2().getParentFolderId()));
    }


    /**
     * 更新文件擁有者的共通實現
     * 若文件只有一個擁有者，則將文件的擁有者列表中移除用戶ID
     *
     * @param userFileList 文件元數據列表
     * @param userId       用戶ID
     *
     * @return Mono<Void>
     */
    protected Mono<Void> updateOwner(List<UserFileMetadata> userFileList, Long userId) {
        return Mono.defer(() -> {
            List<Long> serverFileIds = userFileList.stream().map(UserFileMetadata::getServerFileId).filter(Objects::nonNull).distinct().toList();
            if (userFileList.isEmpty() || serverFileIds.isEmpty()) {
                return Mono.empty();
            }
            return userFileMetaRepository
                    .countByServerFileIdInAndUserId(serverFileIds, userId, entityOperations)
                    .collectList()
                    .flatMap(serverFileMetaCountDaoList -> {
                        Set<Long> serverFileIdList = serverFileMetaCountDaoList
                                .stream()
                                .filter(serverFileMetaCountDAO -> serverFileMetaCountDAO.count() == 1)
                                .map(ServerFileMetaCountDAO::serverFileId)
                                .collect(Collectors.toSet());
                        if (serverFileIdList.isEmpty()) {
                            return Mono.empty();
                        }
                        return serverFileMetaRepository.findAllByIdIn(serverFileIdList).collectList().flatMap(serverFileMetadataList -> {
                            serverFileMetadataList.forEach(serverFileMetadata -> {
                                serverFileMetadata.getOwners().remove(userId);
                                serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                            });
                            return serverFileMetaRepository.saveAll(serverFileMetadataList).then();
                        });
                    });
        });
    }


    /**
     * 更新用戶儲存空間使用量，此方法會根據文件ID列表計算文件大小
     * 此為重載方法、計算刪除的文件大小
     *
     * @param user          用戶信息
     * @param serverFileIds 服務器文件ID列表
     *
     * @return Mono<Void>
     */
    protected Mono<Void> calculateFileSize(User user, List<Long> serverFileIds) {
        if (serverFileIds.isEmpty()) {
            return Mono.empty();
        }
        Map<Long, Long> serverFileIdMap = new ConcurrentHashMap<>();
        serverFileIds.forEach(serverFileId -> serverFileIdMap.put(serverFileId, serverFileIdMap.getOrDefault(serverFileId, 0L) + 1));

        return serverFileMetaRepository
                .findAllByIdIn(serverFileIdMap.keySet())
                .map(serverFileMetadata -> serverFileMetadata.getFileSize() * serverFileIdMap.get(serverFileMetadata.getId()))
                .reduce(0L, Long::sum)
                .flatMap(totalSize -> handleUserStorage(user, totalSize, true));
    }


    /**
     * 關聯用戶與文件的共通實現
     *
     * @param serverFileMetadata 服務器文件
     * @param fileMetadataDTO    文件元數據
     *
     * @return Mono<UserFileMetadata>
     */
    @SkipRecord
    protected Mono<UserFileMetadata> associateUserFile(ServerFileMetadata serverFileMetadata, FileMetadataDTO fileMetadataDTO) {
        UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setUserId(fileMetadataDTO.getUser().getId());
        userFileMetadata.setServerFileId(serverFileMetadata.getId());
        userFileMetadata.setFileType(serverFileMetadata.getFileType());
        userFileMetadata.setFilename(fileMetadataDTO.getFilename());
        userFileMetadata.setParentFolderId(fileMetadataDTO.getParentFolderId());
        userFileMetadata.setUploadTime(LocalDateTime.now());
        userFileMetadata.setLastAccessTime(LocalDateTime.now());
        return userFileMetaRepository.save(userFileMetadata);
    }


    /**
     * 更新用戶儲存空間使用量，此方法會根據文件大小計算用戶儲存空間使用量
     *
     * @param user     用戶信息
     * @param fileSize 要更新的文件大小
     * @param isDelete 是否為刪除操作
     *
     * @return Mono<Void>
     */
    private Mono<Void> handleUserStorage(User user, long fileSize, boolean isDelete) {
        return Mono.defer(() -> userRepository.findById(user.getId()).flatMap(userEntity -> {
            long newStorageUsed = isDelete ? Math.max(userEntity.getUsedStorage() - fileSize, 0) : userEntity.getUsedStorage() + fileSize;
            if (user.getStorageLimit() != -1 && newStorageUsed > user.getStorageLimit()) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.STORAGE_LIMIT_EXCEEDED,
                                                          ByteEnum.toReadableSize(user.getStorageLimit()),
                                                          ByteEnum.toReadableSize(user.getUsedStorage()),
                                                          ByteEnum.toReadableSize(fileSize)
                ));
            }
            userEntity.setUsedStorage(newStorageUsed);

            List<String> keys = Arrays.asList(user.getId().toString(), userEntity.getUsername());
            Mono<Void> cleanCache = cacheManager.deleteCaches(keys, CacheProviderEnum.USER_CACHE);
            return userRepository.save(userEntity).then(cleanCache);
        }));
    }


    /**
     * 清除用戶文件列表緩存的共通實現
     *
     * @param userId    用戶ID
     * @param folderIds 文件夾ID列表
     *
     * @return Mono<Void>
     */
    protected Mono<Void> cleanUserListCache(Long userId, Long... folderIds) {
        List<String> keys = Arrays.stream(folderIds).distinct().map(folderId -> getUserFileListBaseKey(userId, folderId)).toList();
        return cacheManager.deleteCaches(keys, CacheProviderEnum.USER_FILE_LIST_CACHE);
    }


    /**
     * 初始化上傳任務，若需要則返回Mono<String> taskId
     *
     * @param fileMetadataDTO 檔案元數據
     *
     * @return Mono<UploadResponseDTO>
     */
    protected Mono<UploadResponseDTO> initialUpload(FileMetadataDTO fileMetadataDTO) {
        String uploadTaskId = UUID.randomUUID().toString();
        return transfersTasksManager.registerUploadTask(fileMetadataDTO, uploadTaskId).then(Mono.defer(() -> {
            UploadTaskBO task = fileMetadataDTO.formatToTransferTask(uploadTaskId, "初始化任務成功");
            String key = "upload_task:" + uploadTaskId;
            int totalChunks = getTotalChunks(fileMetadataDTO.getFileSize());

            return redisProvider
                    .setHashMap(key, "DTO", task, fileProperties.getUpload().getMaxUploadDuration())
                    .then(redisProvider.setHashMap(key, "uploaded_count", 0, fileProperties.getUpload().getMaxUploadDuration()))
                    .then(redisProvider.setHashMap(key, "total_chunks", totalChunks, fileProperties.getUpload().getMaxUploadDuration()))
                    .then(redisProvider.generateChunkSet(key + ":pending_chunks", totalChunks))
                    .thenReturn(UploadResponseDTO
                                        .builder()
                                        .transferTaskId(uploadTaskId)
                                        .totalChunks(totalChunks)
                                        .chunkSize(CHUNK_SIZE)
                                        .progress(0.0)
                                        .isSuccess(true)
                                        .isFinished(false)
                                        .message("初始化任務成功")
                                        .build());
        }));
    }


    /**
     * 獲取總分塊數的共通實現
     *
     * @param fileSize 文件大小
     *
     * @return 總分塊數
     */
    @SkipRecord
    protected int getTotalChunks(long fileSize) {
        return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
    }


    /**
     * 處理檔案元數據的共享用戶的變更方法
     *
     * @param userFileMetadatas 用戶檔案元數據
     * @param fileEditDTO       檔案編輯DTO
     *
     * @return Flux<UserFileMetadata> 處理後的用戶檔案元數據
     */
    protected Flux<UserFileMetadata> processShareUser(Collection<UserFileMetadata> userFileMetadatas, FileEditDTO fileEditDTO) {
        List<UserFileShareRecord> removeRecords = new ArrayList<>();
        List<UserFileShareRecord> editRecords = new ArrayList<>();
        return Flux.fromIterable(userFileMetadatas).flatMap(userFileMetadata -> {
            Map<Long, ShareUserEditPO.EditTypeEnum> editUsers = fileEditDTO
                    .getShareUsers()
                    .stream()
                    .collect(Collectors.toMap(ShareUserEditPO::getUserId, ShareUserEditPO::getEditType));
            if (editUsers.isEmpty()) {
                return Mono.just(userFileMetadata);
            }

            return userFIleShareRecordRepository.findAllByUserIdInAndFileId(editUsers.keySet(), userFileMetadata.getId()).flatMap(record -> {
                ShareUserEditPO.EditTypeEnum editType = editUsers.remove(record.getUserId());
                switch (editType) {
                    case REMOVE -> removeRecords.add(record);
                    case UPDATE -> editRecords.add(record);
                }
                return Mono.just(userFileMetadata);
            }).then(Mono.defer(() -> {
                if (!editUsers.isEmpty()) {
                    editUsers.forEach((userId, editType) -> {
                        UserFileShareRecord record = new UserFileShareRecord(userId, userFileMetadata.getId());
                        editRecords.add(record);
                    });
                }
                return Mono.just(userFileMetadata);
            }));
        }).collectList().flatMapMany(metadataList -> {
            if (removeRecords.isEmpty() && editRecords.isEmpty()) {
                return Flux.fromIterable(metadataList);
            }
            return Mono
                    .when(userFIleShareRecordRepository.deleteAll(removeRecords), userFIleShareRecordRepository.saveAll(editRecords))
                    .thenMany(Flux.fromIterable(metadataList));
        });
    }


    /**
     * 獲取標頭中的範圍，若無則返回文件大小的範圍
     *
     * @param rangeHeader 範圍標頭
     * @param fileSize    文件大小
     *
     * @return 返回範圍數組
     */
    @SkipRecord
    private long[] getRangeFromHeader(String rangeHeader, long fileSize) {
        long start = 0;
        long end = -1;
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            start = Long.parseLong(ranges[0]);
            if (ranges.length > 1 && !ranges[1].isEmpty() && ranges[1].matches("\\d+")) {
                long endRange = Long.parseLong(ranges[1]);
                end = endRange < fileSize ? endRange : -1;
            }
        }
        return new long[]{start, end};
    }


    /**
     * 更新用戶文件元數據的MimeType
     * 此為版本補丁方法，用於補全舊版本的文件元數據數據
     *
     * @param serverFileMetadata 服務器文件元數據
     * @param fileDataBO         用戶文件數據
     *
     * @return Mono<UserFileDataBO> 更新後的用戶文件數據
     */
    protected Mono<UserFileDataBO> updateUserFileMetaMimeType(ServerFileMetadata serverFileMetadata, UserFileDataBO fileDataBO) {
        if (serverFileMetadata.getMimeType() != null) {
            return Mono.just(fileDataBO);
        }
        return FileEnum.getMediaType(fileDataBO.getDataBufferFlux(), fileDataBO.getFilename()).flatMap(record -> {
            serverFileMetadata.setMimeType(record.mimeType());
            serverFileMetadata.setLastAccessTime(LocalDateTime.now());

            fileDataBO.setDataBufferFlux(record.dataBufferFlux());
            fileDataBO.setMimeType(record.mimeType());
            return serverFileMetaRepository.save(serverFileMetadata).subscribeOn(Schedulers.boundedElastic()).thenReturn(fileDataBO);
        });
    }


    /**
     * 將檔案輸入流轉換為數據流
     *
     * @param dataBufferFlux 文件輸入流
     * @param start          開始位置
     * @param end            結束位置
     *
     * @return 返回數據流
     */
    @SkipRecord
    protected Flux<DataBuffer> streamFileFromGridFS(Flux<DataBuffer> dataBufferFlux, long start, long end) {
        return Flux.defer(() -> {
            Flux<DataBuffer> skippedFlux = DataBufferUtils.skipUntilByteCount(dataBufferFlux, start);
            if (end == -1L || end == Long.MAX_VALUE) {
                return skippedFlux;
            }
            return DataBufferUtils.takeUntilByteCount(skippedFlux, end - start + 1);
        });
    }


    /**
     * 合併分塊數據的共通實現
     *
     * @param byteArrays 分塊數據
     *
     * @return Mono<byte [ ]>
     */
    @SkipRecord
    protected Mono<byte[]> combineBytes(List<byte[]> byteArrays) {
        return Mono.fromCallable(() -> {
            int totalLength = byteArrays.stream().mapToInt(bytes -> bytes.length).sum();
            ByteBuffer buffer = ByteBuffer.allocate(totalLength);
            byteArrays.forEach(buffer::put);
            return buffer.array();
        }).doFinally(signalType -> byteArrays.clear());
    }


    /**
     * 檢查文件的狀態的共通實現
     * 根據傳入的數據和任務信息，檢查文件的大小和MD5值是否匹配
     * 如果不匹配，則更新傳輸任務的狀態為失敗並返回錯誤
     * 若果匹配，則返回空的Mono對象
     *
     * @param combinedBytes 合併後的數據
     * @param uploadTaskBO  任務
     *
     * @return Mono<Void>
     */
    protected Mono<Void> checkFileStatus(byte[] combinedBytes, UploadTaskBO uploadTaskBO) {
        return Mono.defer(() -> {
            if (combinedBytes.length != uploadTaskBO.getFileSize()) {
                return transfersTasksManager
                        .updateTransfersTask(uploadTaskBO.getMd5(),
                                             uploadTaskBO.getTransferTaskId(),
                                             TransfersStatusEnum.FAILED,
                                             "文件大小不匹配",
                                             null,
                                             true
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .then(Mono.error(new ProcessException(ProcessException.ErrorCode.FILE_SIZE_NOT_MATCH)));
            }

            String computedChunkMd5 = DigestUtils.md5Hex(combinedBytes);
            if (!uploadTaskBO.getMd5().equals(computedChunkMd5)) {
                return transfersTasksManager
                        .updateTransfersTask(uploadTaskBO.getMd5(),
                                             uploadTaskBO.getTransferTaskId(),
                                             TransfersStatusEnum.FAILED,
                                             "MD5校驗失敗",
                                             null,
                                             true
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .then(Mono.error(new ProcessException(ProcessException.ErrorCode.MD5_NOT_MATCH)));
            }
            return Mono.empty();
        });
    }


    /**
     * 合併已上傳的文件分塊的共通實現
     *
     * @param transferTaskId 任務ID
     * @param totalChunks    總分塊數
     *
     * @return Mono<byte [ ]> 合併後的數據
     */
    protected Mono<byte[]> combineChunks(String transferTaskId, int totalChunks) {
        RateLimiter rateLimiter = RateLimiter.of("combineChunk", this.rateLimiterConfig);
        CircuitBreaker chunkCircuitBreaker = CircuitBreaker.of("gridFsChunkReader", this.circuitBreakerConfig);
        record ChunkData(int index, byte[] data) {
            static Comparator<ChunkData> comparator() {
                return Comparator.comparingInt(ChunkData::index);
            }
        }
        Retry retryPolicy = Retry
                .backoff(5, Duration.ofSeconds(1))
                .filter(e -> e instanceof LimitationException)
                .maxBackoff(Duration.ofSeconds(5))
                .jitter(0.3);


        return Mono.defer(() -> {
            Flux<ChunkData> chunkFiles = Flux.range(1, totalChunks).flatMapSequential(index -> {
                Mono<ChunkData> chunkOperation = this.gridFsProvider
                        .findFileByFileName(transferTaskId + "_chunk_" + index)
                        .flatMap(this.gridFsProvider::getResource)
                        .flatMap(resource -> DataBufferUtils.join(resource.getDownloadStream()).onErrorResume(e -> {
                            String taskId = transferTaskId + "_chunk_" + index;
                            return Mono.error(new ProcessException(ProcessException.ErrorCode.CANNOT_GET_FILE_STREAM, e, taskId));
                        }))
                        .map(dataBuffer -> {
                            try {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                return new ChunkData(index, bytes);
                            } finally {
                                DataBufferUtils.release(dataBuffer);
                            }
                        })
                        .transformDeferred(CircuitBreakerOperator.of(chunkCircuitBreaker));

                return Mono.defer(() -> {
                    if (rateLimiter.acquirePermission()) {
                        return chunkOperation;
                    }
                    return Mono.error(new LimitationException(LimitationException.ErrorCode.FILE_CHUNK_EXCEED_LIMIT,
                                                              "上傳檔案超出了請求限制，請稍後再試"
                    ));
                }).retryWhen(retryPolicy);
            });
            return chunkFiles.sort(ChunkData.comparator()).map(ChunkData::data).collectList().flatMap(this::combineBytes);
        });
    }


    /**
     * 檢查文件的狀態和安全性
     * 根據傳入的數據和任務信息，檢查文件的大小和MD5值是否匹配
     * 如果不匹配，則更新傳輸任務的狀態為失敗並返回錯誤
     * 若果匹配，則返回空的Mono對象
     * <p>
     * 當發生錯誤時，會刪除暫存數據
     *
     * @param transferTaskId 任務ID
     * @param totalChunks    總分塊數
     *
     * @return Mono<Void>
     */
    protected Mono<Void> fileCheck(String transferTaskId, int totalChunks) {
        String key = "upload_task:" + transferTaskId;
        return redisProvider
                .getHashMap(key, "DTO", UploadTaskBO.class)
                .flatMap(uploadTaskBO -> combineChunks(transferTaskId, totalChunks).flatMap(combinedBytes -> {
                    Mono<Void> checkFileStatusMono = checkFileStatus(combinedBytes, uploadTaskBO);
                    Mono<Void> scanFileMono = checkFileSecurity(combinedBytes, uploadTaskBO);

                    return Mono
                            .when(checkFileStatusMono, scanFileMono)
                            .then(processFileAfterFileCheck(uploadTaskBO, combinedBytes))
                            .onErrorResume(e -> {
                                LogUnity.warn("檔案檢查失敗，將刪除暫存數據，上傳任務ID: %s ，錯誤原因", e, transferTaskId);
                                Mono<Void> removeTempDataMono = removeTempData(uploadTaskBO);
                                Mono<Void> updateTask = transfersTasksManager.updateTransfersTask(uploadTaskBO.getMd5(),
                                                                                                  uploadTaskBO.getTransferTaskId(),
                                                                                                  TransfersStatusEnum.FAILED,
                                                                                                  "檔案檢查失敗",
                                                                                                  null,
                                                                                                  true
                                );
                                return Mono.when(removeTempDataMono, updateTask);
                            });
                }));
    }


    /**
     * 處理檢查成功後的文件存儲邏輯的共通實現
     *
     * @param uploadTaskBO  任務
     * @param combinedBytes 合併後的數據
     *
     * @return Mono<Void>
     */
    protected Mono<Void> processFileAfterFileCheck(UploadTaskBO uploadTaskBO, byte[] combinedBytes) {
        return gridFsProvider
                .storeFile(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(combinedBytes)),
                           String.format("%s_output", uploadTaskBO.getTransferTaskId())
                )
                .flatMap(fileGridFsId -> {
                    String gridFsId = fileGridFsId.toHexString();

                    String mimeType = FileEnum.getMediaType(combinedBytes, uploadTaskBO.getFilename());
                    FileEnum fileType = FileEnum.fromMimeType(mimeType);

                    ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
                    serverFileMetadata.setFileSize(uploadTaskBO.getFileSize());
                    serverFileMetadata.setFileType(fileType);
                    serverFileMetadata.setMimeType(mimeType);
                    serverFileMetadata.setMd5(uploadTaskBO.getMd5());
                    serverFileMetadata.setGridFsId(gridFsId);
                    serverFileMetadata.setUploadTime(LocalDateTime.now());
                    serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                    serverFileMetadata.getOwners().add(uploadTaskBO.getUser().getId());

                    Mono<Void> calculateStorage = handleUserStorage(uploadTaskBO.getUser(), uploadTaskBO.getFileSize(), false);
                    Mono<Void> removeTempData = removeTempData(uploadTaskBO);
                    Mono<Void> cleanUserListCache = cleanUserListCache(uploadTaskBO.getUser().getId(), uploadTaskBO.getParentFolderId());
                    Mono<Void> updateTask = transfersTasksManager
                            .updateTransfersTask(uploadTaskBO.getMd5(),
                                                 uploadTaskBO.getTransferTaskId(),
                                                 TransfersStatusEnum.COMPLETED,
                                                 "檔案處理成功",
                                                 gridFsId,
                                                 true
                            )
                            .subscribeOn(Schedulers.boundedElastic());
                    return serverFileMetaRepository
                            .save(serverFileMetadata)
                            .flatMap(serverFile -> associateUserFile(serverFile, uploadTaskBO.formatToFileMetadata()))
                            .flatMap(userFileMetaRepository::save)
                            .then(Mono.when(calculateStorage, removeTempData, cleanUserListCache, updateTask));
                });
    }


    /**
     * 檢查文件的安全性的共通實現
     * 根據傳入的數據和任務信息，檢查文件是否存在病毒
     * 如果檢測到病毒，則更新傳輸任務的狀態為失敗並返回錯誤
     * 若果沒有檢測到病毒，則返回空的Mono對象
     *
     * @param combinedBytes 合併後的數據
     * @param uploadTaskBO  任務
     *
     * @return Mono<Void>
     */
    protected Mono<Void> checkFileSecurity(byte[] combinedBytes, UploadTaskBO uploadTaskBO) {
        return Mono.defer(() -> {
            if (fileScanProvider == null) {
                return Mono.empty();
            }
            ByteBuf byteBuf = Unpooled.wrappedBuffer(combinedBytes);
            return fileScanProvider.scanByteBuf(Flux.just(byteBuf), combinedBytes.length).flatMap(result -> {
                if (result.isSafe()) {
                    return Mono.empty();
                }
                LogUnity.warn("上傳任務中的檔案檢測到病毒，上傳任務ID: %s", uploadTaskBO.getTransferTaskId());
                return transfersTasksManager
                        .updateTransfersTask(uploadTaskBO.getMd5(),
                                             uploadTaskBO.getTransferTaskId(),
                                             TransfersStatusEnum.FAILED,
                                             "檔案檢測到病毒，已刪除",
                                             null,
                                             true
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .then(Mono.error(new ValidationException(ValidationException.ErrorCode.FILE_VIRUS_DETECTED,
                                                                 uploadTaskBO.getTransferTaskId()
                        )));
            });
        });
    }


    /**
     * 刪除暫存分塊數據的共通實現
     *
     * @param uploadTaskBO 傳輸任務
     *
     * @return Mono<Void>
     */
    protected Mono<Void> removeTempData(UploadTaskBO uploadTaskBO) {
        String key = "upload_task:" + uploadTaskBO.getTransferTaskId();
        return redisProvider
                .getHashMap(key, "total_chunks", Integer.class)
                .flatMapMany(totalChunks -> Flux
                        .range(1, totalChunks)
                        .flatMap(i -> gridFsProvider.deleteFileByFilename(uploadTaskBO.getTransferTaskId() + "_chunk_" + i))
                        .then(redisProvider.deleteHash(key).then(redisProvider.deleteSet(key + ":pending_chunks"))))
                .then();
    }


    /**
     * 處理文件分塊的共通實現
     *
     * @param uploadChunkDTO  上傳文件數據
     * @param transferTaskId  任務ID
     * @param key             任務Key
     * @param pendingChunkKey 待處理文件分塊Key
     *
     * @return Mono<UploadResponseDTO>
     */
    protected Mono<UploadResponseDTO> processChunk(UploadChunkDTO uploadChunkDTO, String transferTaskId, String key, String pendingChunkKey) {
        int chunkIndex = uploadChunkDTO.getChunkIndex();
        int totalChunks = uploadChunkDTO.getTotalChunks();

        Flux<DataBuffer> chunkData = uploadChunkDTO
                .getChunkDataFlux()
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO)));

        return redisProvider
                .deleteSet(pendingChunkKey, chunkIndex)
                .then(gridFsProvider.storeFile(chunkData, transferTaskId + "_chunk_" + chunkIndex))
                .then(redisProvider.incrementHashMap(key, "uploaded_count", 1, fileProperties.getUpload().getMaxUploadDuration()))
                .flatMap(uploadCount -> {
                    double progress = (uploadCount.doubleValue() / totalChunks) * 100.0;
                    String message = String.format("文件分塊: %d 上傳成功", chunkIndex);

                    UploadResponseDTO responseDTO = UploadResponseDTO
                            .builder()
                            .chunkIndex(chunkIndex)
                            .transferTaskId(transferTaskId)
                            .progress(progress)
                            .isSuccess(true)
                            .isFinished(false)
                            .message(message)
                            .totalChunks(totalChunks)
                            .build();

                    if (uploadCount.intValue() == totalChunks) {
                        fileCheck(transferTaskId, totalChunks).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        responseDTO.setIsFinished(true);
                    }
                    return Mono.just(responseDTO);
                })
                .onErrorResume(e -> redisProvider
                        .setSet(pendingChunkKey, chunkIndex)
                        .then(redisProvider.getHashMap(key, "uploaded_count").map(count -> {
                            long uploadCountLong = Long.parseLong(count.toString());
                            double progress = ((double) uploadCountLong / totalChunks) * 100.0;
                            return UploadResponseDTO
                                    .builder()
                                    .chunkIndex(chunkIndex)
                                    .transferTaskId(transferTaskId)
                                    .progress(progress)
                                    .isSuccess(false)
                                    .isFinished(false)
                                    .message("文件分塊上傳失敗")
                                    .totalChunks(totalChunks)
                                    .build();
                        })));
    }


    /**
     * 從回收站還原文件
     *
     * @param userFileMetadata 文件
     * @param user             用戶
     *
     * @return Mono<UserFileMetadata> 還原後的文件
     */
    public Mono<UserFileMetadata> restoreFile(UserFileMetadata userFileMetadata, User user) {
        return restoreFile(Collections.singletonList(userFileMetadata), user).next();
    }


    /**
     * 從回收站批量還原文件
     *
     * @param userFileMetadataIterable 文件列表
     * @param user                     用戶
     *
     * @return Flux<UserFileMetadata> 還原後的文件列表
     */
    public Flux<UserFileMetadata> restoreFile(Iterable<UserFileMetadata> userFileMetadataIterable, User user) {
        Map<Long, UserFileMetadata> userFileMetadataMap = new HashMap<>();
        Set<Long> problemFileIdsSet = new HashSet<>();
        userFileMetadataIterable.forEach(userFileMetadata -> {
            if (!userFileMetadata.getIsDeleted()) {
                problemFileIdsSet.add(userFileMetadata.getId());
            }
            userFileMetadataMap.put(userFileMetadata.getId(), userFileMetadata);
        });
        if (!problemFileIdsSet.isEmpty()) {
            return Flux.error(new ValidationException(ValidationException.ErrorCode.SOME_FILE_NOT_DELETED, problemFileIdsSet));
        }

        if (userFileMetadataMap.isEmpty()) {
            return Flux.empty();
        }
        return transactionalOperator
                .transactional(recoverFileMethod(userFileMetadataMap, problemFileIdsSet).then(Mono.just(true)))
                .thenMany(Flux.fromIterable(userFileMetadataMap.values()));
    }


    /**
     * 刪除文件到回收站
     *
     * @param userFileMetadata 文件
     * @param user             用戶
     *
     * @return Mono<Boolean> 是否刪除成功
     */
    public Mono<Boolean> removeFile(UserFileMetadata userFileMetadata, User user) {
        return removeFile(Collections.singletonList(userFileMetadata), user);
    }


    /**
     * 將檔案批量刪除到回收站
     *
     * @param userFileMetadataIterable 用戶文件元數據
     * @param user                     用戶信息
     *
     * @return Mono<Boolean> 是否刪除成功
     */
    public Mono<Boolean> removeFile(Iterable<UserFileMetadata> userFileMetadataIterable, User user) {
        return Mono.defer(() -> {
            LocalDateTime deleteTime = LocalDateTime.now().plusDays(fileProperties.getBackup().getRetentionTime().toDays());
            Set<FileTrashRecord> fileTrashRecords = new HashSet<>();
            Set<Long> problemFileIdsSet = new HashSet<>();
            userFileMetadataIterable.forEach(userFile -> {
                if (userFile.getIsDeleted()) {
                    problemFileIdsSet.add(userFile.getId());
                }
                FileTrashRecord fileTrashRecord = new FileTrashRecord(userFile, deleteTime);
                fileTrashRecords.add(fileTrashRecord);
                userFile.setIsDeleted(true);
            });
            if (!problemFileIdsSet.isEmpty()) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.SOME_FILE_ALREADY_DELETED, problemFileIdsSet));
            }

            Mono<Boolean> result = fileTrashRecordRepository
                    .insertAll(fileTrashRecords, entityOperations)
                    .thenMany(userFileMetaRepository.saveAll(userFileMetadataIterable))
                    .collectList()
                    .flatMap(fileMeta -> {
                        Long[] parentFolderIds = fileMeta.stream().map(UserFileMetadata::getParentFolderId).distinct().toArray(Long[]::new);
                        return cleanUserListCache(fileMeta.getFirst().getUserId(), parentFolderIds);
                    })
                    .thenReturn(true)
                    .onErrorReturn(false);
            return transactionalOperator.transactional(result);
        });
    }


    /**
     * 還原文件輔助方法
     *
     * @param userFileMetadataMap 文件Map
     * @param problemFileIdsSet   問題文件ID集合
     *
     * @return Flux<UserFileMetadata> 還原後的文件列表
     */
    private Flux<UserFileMetadata> recoverFileMethod(Map<Long, UserFileMetadata> userFileMetadataMap, Set<Long> problemFileIdsSet) {
        return fileTrashRecordRepository.findAllById(userFileMetadataMap.keySet()).flatMap(fileTrashRecord -> {
            UserFileMetadata userFileMetadata = userFileMetadataMap.get(fileTrashRecord.getFileId());
            if (userFileMetadata == null) {
                problemFileIdsSet.add(fileTrashRecord.getFileId());
                return Mono.empty();
            }
            userFileMetadata.setIsDeleted(false);
            userFileMetadata.setLastAccessTime(LocalDateTime.now());

            if (userFileMetadata.getParentFolderId() == null) {
                return Mono.just(userFileMetadata);
            }
            return userFileMetaRepository.findById(userFileMetadata.getParentFolderId().toString()).flatMap(fatherFolder -> {
                if (fatherFolder.getIsDeleted()) {
                    userFileMetadata.setParentFolderId(null);
                }
                return Mono.just(userFileMetadata);
            });
        }).collectList().flatMapMany(processedMetadata -> {
            if (!problemFileIdsSet.isEmpty()) {
                return Flux.error(new ProcessException(ProcessException.ErrorCode.NOT_EXISTING_FILE_TRASH_RECORD, problemFileIdsSet));
            }

            return fileTrashRecordRepository
                    .deleteAllById(userFileMetadataMap.keySet())
                    .thenMany(userFileMetaRepository.saveAll(userFileMetadataMap.values()))
                    .collectList()
                    .flatMapMany(metadataList -> {
                        Long[] parentFolderIds = metadataList.stream().map(UserFileMetadata::getParentFolderId).distinct().toArray(Long[]::new);
                        return cleanUserListCache(metadataList.getFirst().getUserId(), parentFolderIds).thenMany(Flux.fromIterable(metadataList));
                    });
        });
    }


    /**
     * 創建一個新的用戶文件元數據實體
     *
     * @return 返回一個新的實體對象
     */
    public Mono<UserFileMetadata> createUserFileMetadata() {
        return Mono.just(new UserFileMetadata());
    }


    /**
     * 根據ID獲取一個用戶文件元數據實體
     *
     * @param id 服務器文件元數據ID
     *
     * @return 用戶文件元數據實體對象
     */
    public Mono<UserFileMetadata> getUserFileMetadataById(Long id) {
        return userFileMetaRepository.findById(id.toString());
    }


    /**
     * 獲取所有用戶文件元數據實體
     */
    public Flux<UserFileMetadata> getAllUserFileMetadata() {
        return userFileMetaRepository.findAll();
    }


    /**
     * 更新一個用戶文件元數據實體
     *
     * @param entity 用戶文件元數據實體對象
     */
    public Mono<UserFileMetadata> updateUserFileMetadata(@NotNull UserFileMetadata entity) {
        return userFileMetaRepository.save(entity);
    }


    /**
     * 刪除一個用戶文件元數據實體
     *
     * @param entity 用戶文件元數據實體對象
     */
    public Mono<Void> deleteUserFileMetadata(UserFileMetadata entity) {
        return userFileMetaRepository.deleteById(entity.getId().toString());
    }


    /**
     * 創建一個新的服務器文件元數據實體
     *
     * @return 返回一個新的服務器文件元數據實體對象
     */
    public Mono<ServerFileMetadata> createServerFileMetadata() {
        return Mono.just(new ServerFileMetadata());
    }


    /**
     * 根據ID獲取一個服務器文件元數據實體
     *
     * @param id 服務器文件元數據ID
     *
     * @return 服務器文件元數據實體對象
     */
    public Mono<ServerFileMetadata> getByServerFileMetadataId(Long id) {
        return serverFileMetaRepository.findById(id.toString());
    }


    /**
     * 獲取所有服務器文件元數據實體
     */
    @RequirePermission(PermissionEnum.MANAGE)
    public Flux<ServerFileMetadata> getAllServerFileMetadata() {
        return serverFileMetaRepository.findAll();
    }


    /**
     * 更新一個服務器文件元數據實體
     *
     * @param entity 服務器文件元數據實體對象
     */
    @RequirePermission(PermissionEnum.MANAGE)
    public Mono<ServerFileMetadata> updateServerFileMetadata(@NotNull ServerFileMetadata entity) {
        return serverFileMetaRepository.save(entity);
    }


    /**
     * 刪除一個服務器文件元數據實體
     *
     * @param entity 服務器文件元數據實體對象
     */
    @RequirePermission(PermissionEnum.MANAGE)
    public Mono<Void> deleteServerFileMetadata(ServerFileMetadata entity) {
        return serverFileMetaRepository.deleteById(entity.getId().toString());
    }

}
