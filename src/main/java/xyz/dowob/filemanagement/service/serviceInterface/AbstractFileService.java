package xyz.dowob.filemanagement.service.serviceInterface;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.Tika;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.ByteEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;
import xyz.dowob.filemanagement.data.file.bo.UploadTaskBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dao.ServerFileMetaCountDao;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.ServerFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserOnlineFileRepository;
import xyz.dowob.filemanagement.repostiory.UserRepository;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 檔案服務的抽象類，包含共通的上傳、下載邏輯
 */
@RequiredArgsConstructor
public abstract class AbstractFileService implements FileService {
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
     * 文件上傳任務管理器
     */
    protected final TransfersTasksManager transfersTasksManager;
    /**
     * 文件配置屬性
     */
    protected final FileProperties fileProperties;
    /**
     * 數據庫操作對象
     */
    protected final DatabaseClient databaseClient;
    /**
     * 斷路器配置
     */
    protected final CircuitBreakerConfig circuitBreakerConfig;

    /**
     * 文件列表樹提供者
     */
    protected final FolderListTreeProvider folderListTreeProvider;
    /**
     * 檔案類型檢驗器
     */
    private final Tika tika = new Tika();
    /**
     * 每個分塊的大小
     */
    protected Long CHUNK_SIZE;

    private static final String PAGE_KEY_FORMAT = "fileList_user:%s_folder:%s";
    private static final Duration DEFAULT_CACHE_DURATION = Duration.ofHours(1);


    /**
     * 初始化方法，獲取文件配置中的分塊大小
     */
    @PostConstruct
    public void init() {
        CHUNK_SIZE = (long) fileProperties.getUpload().getChunkSize() * 1024 * 1024;
    }

    /**
     * 獲取用戶文件列表的共通實現
     *
     * @param user 用戶信息
     *
     * @return Flux<UserFileListDTO>
     */
    @HideOverLength
    public Mono<PagedResponseDTO<UserFileListDTO>> getUserFileList(User user, Long fatherFolderId, Integer page, Integer size, List<FileEnum> types) {
        int pageSize = Objects.requireNonNullElse(size, fileProperties.getGlobal().getPageSize());
        int currentPage = Math.max(1, Objects.requireNonNullElse(page, 1));

        String key = getUserFileListBaseKey(user.getId(), fatherFolderId);

        return getFileListFormCache(key).collectList().flatMap(cachedList -> {
            if (!cachedList.isEmpty()) {
                return filterAndPageResponse(cachedList, types, currentPage, pageSize);
            }
            return getFileListFormDB(user, fatherFolderId).collectList().flatMap(dbList -> {
                if (!dbList.isEmpty()) {
                    return cacheUserFileList(user, fatherFolderId, Flux.fromIterable(dbList)).then(filterAndPageResponse(dbList,
                                                                                                                         types,
                                                                                                                         currentPage,
                                                                                                                         pageSize
                    ));
                }
                return filterAndPageResponse(dbList, types, currentPage, pageSize);
            });
        });
    }

    protected String getUserFileListBaseKey(Long userId, Long parentFolderId) {
        return String.format(PAGE_KEY_FORMAT, userId, Objects.requireNonNullElse(parentFolderId, 0L));
    }

    /**
     * 上傳文件分塊的共通實現
     *
     * @param uploadChunkDTO 上傳文件數據
     *
     * @return Mono<UploadResponseDTO>
     */

    public Mono<UploadResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO) {
        String transferTaskId = uploadChunkDTO.getTransferTaskId();
        String key = "upload_task:" + transferTaskId;
        String pendingChunkKey = key + ":pending_chunks";

        return Mono.defer(() -> redisProvider
                .getHashMap(key, "DTO")
                .switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.NOT_EXISTING_UPLOAD_TASK, transferTaskId)))
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
                            combineChunks(transferTaskId, uploadChunkDTO.getTotalChunks())
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .subscribe();
                            responseDTO.setIsFinished(true);
                        }
                        return Mono.just(responseDTO);
                    });
                })));
    }

    /**
     * 下載文件的共通實現
     *
     * @param fileId 文件ID
     * @param user   用戶信息
     *
     * @return Mono<UserFileDataBO>
     */
    public Mono<UserFileDataBO> downloadFile(String fileId, User user) {
        return userFileMetaRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)))
                .flatMap(userFileMetadata -> validateUserPermission(user,
                                                                    userFileMetadata.getUserId(),
                                                                    userFileMetadata.getSharedWithUsers(),
                                                                    userFileMetadata.getId(),
                                                                    false
                ).then(isFileOrFolder(userFileMetadata, false)))
                .flatMap(userFileMetadata -> serverFileMetaRepository
                        .findById(userFileMetadata.getServerFileId().toString())
                        .switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.USER_HAVE_NOT_EXIST_SERVER_FILE,
                                                                       userFileMetadata.getServerFileId(),
                                                                       fileId
                        )))
                        .map(serverFileMetadata -> new UserFileDataBO(serverFileMetadata, userFileMetadata)))
                .flatMap(fileData -> gridFsProvider
                        .findFileById(new ObjectId(fileData.getGridFsId()))
                        .switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.GRIDFS_FILE_NOT_FOUND,
                                                                       fileData.getServerFileId()
                        )))
                        .flatMap(gridFsFile -> gridFsProvider.getResource(gridFsFile).map(resource -> {
                            Flux<DataBuffer> dataStream = resource.getDownloadStream().map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
                            });
                            fileData.setDataStream(dataStream);
                            return fileData;
                        })));
    }

    /**
     * 從緩存中獲取用戶文件列表
     *
     * @param key 緩存Key
     *
     * @return Flux<UserFileListDTO> 檔案列表流
     */
    private Flux<UserFileListDTO> getFileListFormCache(String key) {
        return redisProvider.getList(key, UserFileListDTO.class);
    }

    /**
     * 過濾所需的檔案元素並分頁
     *
     * @param list        檔案列表
     * @param types       檔案類型
     * @param currentPage 當前頁碼
     * @param pageSize    每頁條數
     *
     * @return Mono<PagedResponseDTO < UserFileListDTO>> 檔案總數和分頁後的檔案列表
     */
    private Mono<PagedResponseDTO<UserFileListDTO>> filterAndPageResponse(List<UserFileListDTO> list, List<FileEnum> types, int currentPage, int pageSize) {
        return filterPageElements(Flux.fromIterable(list), types, currentPage, pageSize).flatMap(tuple -> {
            PagedResponseDTO<UserFileListDTO> pagedResponseDTO = new PagedResponseDTO<>();
            pagedResponseDTO.setData(tuple.getT2());
            pagedResponseDTO.setTotalElements(tuple.getT1());
            pagedResponseDTO.setPageSize(pageSize);
            pagedResponseDTO.setCurrentPage(currentPage);
            pagedResponseDTO.setTotalPages((int) Math.ceil((double) tuple.getT1() / pageSize));
            return Mono.just(pagedResponseDTO);
        });
    }


    public Mono<List<FolderListTreeProvider.FolderNode>> getUserFilePaths(Long fileId, User user) {
        return userFileMetaRepository
                .findById(fileId.toString())
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata).then(isFileOrFolder(userFileMetadata, true)))
                .flatMap(userFileMetadata -> {
                    if (folderListTreeProvider != null) {
                        List<FolderListTreeProvider.FolderNode> path = folderListTreeProvider.getPath(user.getId(), fileId);
                        return Mono.just(path);
                    }
                    return Flux
                            .just(userFileMetadata)
                            .expand(metadata -> metadata.getParentFolderId() == null ? Mono.empty() : userFileMetaRepository.findById(
                                    metadata.getParentFolderId().toString())).map(FolderListTreeProvider.FolderNode::new)
                            .collectList()
                            .map(list -> {
                                list.add(new FolderListTreeProvider.FolderNode(null, "root"));
                                return list;
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    if (fileId == 0L || fileId == -1L) {
                        List<FolderListTreeProvider.FolderNode> list = List.of(new FolderListTreeProvider.FolderNode(null, "root"));
                        return Mono.just(list);
                    }
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId));
                }));
    }

    /**
     * 從數據庫中獲取用戶文件列表
     *
     * @param user           用戶信息
     * @param fatherFolderId 父文件夾ID
     *
     * @return Flux<UserFileListDTO> 檔案列表流
     */
    private Flux<UserFileListDTO> getFileListFormDB(User user, Long fatherFolderId) {
        Flux<UserFileMetadata> userFileMetadataFlux;
        if (fatherFolderId == null || fatherFolderId == 0) {
            userFileMetadataFlux = userFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(user.getId());
        } else if (fatherFolderId == -1) {
            userFileMetadataFlux = userFileMetaRepository.findAllByUserId(user.getId());
        } else {
            userFileMetadataFlux = userFileMetaRepository.findAllByUserIdAndParentFolderIdInOrderByIsFolder(user.getId(),
                                                                                                            List.of(fatherFolderId)
            );
        }
        Set<Long> serverFileIds = new HashSet<>();
        List<UserFileMetadata> folderMetadata = new ArrayList<>();
        HashMap<String, UserFileMetadata> onlineFileMap = new HashMap<>();

        return userFileMetadataFlux.collectList().flatMap(userFileMetadataList -> {
            userFileMetadataList.forEach(userFileMetadata -> {
                if (userFileMetadata.getServerFileId() != null) {
                    serverFileIds.add(userFileMetadata.getServerFileId());
                }
                if (userFileMetadata.getFileType() == FileEnum.ONLINE_DOCUMENT) {
                    onlineFileMap.put(userFileMetadata.getId().toString(), userFileMetadata);
                }
                if (userFileMetadata.getIsFolder()) {
                    folderMetadata.add(userFileMetadata);
                }
            });
            return Mono.empty();
        }).thenMany(Flux.defer(() -> {
            Flux<UserFileListDTO> folderListDTO = Flux.fromIterable(folderMetadata).map(UserFileListDTO::new);
            Flux<UserFileListDTO> fileListDTO = Flux.empty();
            Flux<UserFileListDTO> onlineFileListDTO = Flux.empty();

            if (!serverFileIds.isEmpty()) {
                fileListDTO = serverFileMetaRepository
                        .findAllByIdIn(serverFileIds)
                        .collectMap(ServerFileMetadata::getId)
                        .flatMapMany(serverFileMetadataMap -> userFileMetadataFlux
                                .filter(userFileMetadata -> userFileMetadata.getServerFileId() != null)
                                .map(userFileMetadata -> {
                                    ServerFileMetadata serverFileMetadata = serverFileMetadataMap.get(userFileMetadata.getServerFileId());
                                    return new UserFileListDTO(serverFileMetadata, userFileMetadata);
                                }));
            }

            if (!onlineFileMap.isEmpty()) {
                onlineFileListDTO = userOnlineFileRepository
                        .findAllById(onlineFileMap.keySet())
                        .map(userOnlineFile -> new UserFileListDTO(userOnlineFile, onlineFileMap.get(userOnlineFile.getId().toString())));
            }
            return Flux.concat(folderListDTO, onlineFileListDTO, fileListDTO);
        }));
    }

    /**
     * 緩存用戶文件列表
     *
     * @param user                用戶信息
     * @param fatherFolderId      父文件夾ID
     * @param userFileListDTOFlux 檔案列表流
     *
     * @return Mono<Void>
     */
    private Mono<Void> cacheUserFileList(User user, Long fatherFolderId, Flux<UserFileListDTO> userFileListDTOFlux) {
        if (fatherFolderId == null || fatherFolderId == -1) {
            return Mono.empty();
        }
        String key = getUserFileListBaseKey(user.getId(), fatherFolderId);
        return userFileListDTOFlux.flatMap(userFileListDTO -> redisProvider.insertList(key, userFileListDTO, false, DEFAULT_CACHE_DURATION))
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 過濾所需的檔案元素並分頁
     *
     * @param flux        檔案列表流
     * @param type        檔案類型
     * @param currentPage 當前頁碼
     * @param pageSize    每頁條數
     *
     * @return Mono<Tuple2 < Integer, List < UserFileListDTO>>> 檔案總數和分頁後的檔案列表
     */
    private Mono<Tuple2<Integer, List<UserFileListDTO>>> filterPageElements(Flux<UserFileListDTO> flux, List<FileEnum> type, int currentPage, int pageSize) {
        if (type != null && !type.isEmpty()) {
            flux = flux.filter(userFileListDTO -> type.contains(userFileListDTO.getFileType()));
        }
        return flux.collectList().map(list -> {
            int size = list.size();
            int start = Math.min(Math.max((currentPage - 1), 0) * pageSize, size);
            int end = Math.min(start + pageSize, size);
            List<UserFileListDTO> subList = list.subList(start, end);
            return Tuples.of(size, subList);
        });
    }

    /**
     * 獲取總分塊數的共通實現
     *
     * @param fileSize 文件大小
     *
     * @return 總分塊數
     */
    protected int getTotalChunks(long fileSize) {
        return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
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
        return Mono.defer(() -> {
            if (fileMetadataDTO.getParentFolderId() != null) {
                return checkParentFolderId(fileMetadataDTO.getParentFolderId(), user);
            }
            return Mono.empty();
        }).then(serverFileMetaRepository.findByMd5(fileMetadataDTO.getMd5()).flatMap(existingFile -> {
            existingFile.getOwners().add(user.getId());
            existingFile.setLastAccessTime(LocalDateTime.now());
            return serverFileMetaRepository.save(existingFile).then(associateUserFile(existingFile, fileMetadataDTO)
                                  .flatMap(userFileMetaRepository::save)
                                  .then(handleUserStorage(user, existingFile.getFileSize(), false))
                                  .thenReturn(UploadResponseDTO
                                                      .builder()
                                                      .progress(100.0)
                                                      .isSuccess(true)
                                                      .isFinished(true)
                                                      .message("上傳成功")
                                                      .build()));
        }).switchIfEmpty(initialUpload(fileMetadataDTO)));
    }

    /**
     * 檢查父文件夾的實體是否存在以及相關用戶的權限
     * 當檔案不存在或是用戶無權限時，拋出ValidationException
     *
     * @param parentFolderId 父文件夾ID
     * @param user           用戶信息
     *
     * @return Mono<Void>
     */
    protected Mono<Void> checkParentFolderId(Long parentFolderId, User user) {
        return userFileMetaRepository
                .findById(parentFolderId.toString())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, parentFolderId)))
                .flatMap(parentFolder -> validateUserPermission(user, parentFolder).then(isFileOrFolder(parentFolder, true)))
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, parentFolderId)))
                .then();
    }

    /**
     * 關聯用戶與文件的共通實現
     *
     * @param serverFileMetadata 服務器文件ID
     * @param fileMetadataDTO    文件元數據
     *
     * @return Mono<UserFileMetadata>
     */
    protected Mono<UserFileMetadata> associateUserFile(ServerFileMetadata serverFileMetadata, FileMetadataDTO fileMetadataDTO) {
        UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setUserId(fileMetadataDTO.getUser().getId());
        userFileMetadata.setServerFileId(serverFileMetadata.getId());
        userFileMetadata.setFileType(serverFileMetadata.getFileType());
        userFileMetadata.setFilename(fileMetadataDTO.getFileName());
        userFileMetadata.setParentFolderId(fileMetadataDTO.getParentFolderId());
        userFileMetadata.setUploadTime(LocalDateTime.now());
        userFileMetadata.setLastAccessTime(LocalDateTime.now());
        return userFileMetaRepository.save(userFileMetadata);
    }


    public FileEnum detectFileType(byte[] fileBytes) {
        String mimeType = tika.detect(fileBytes);
        return FileEnum.fromMimeType(mimeType);
    }

    /**
     * 合併分塊數據的共通實現
     *
     * @param byteArrays 分塊數據
     *
     * @return Mono<byte [ ]>
     */
    protected Mono<byte[]> combineBytes(List<byte[]> byteArrays) {
        return Mono.fromCallable(() -> {
            int totalLength = byteArrays.stream().mapToInt(bytes -> bytes.length).sum();
            ByteBuffer buffer = ByteBuffer.allocate(totalLength);
            byteArrays.forEach(buffer::put);
            return buffer.array();
        });
    }

    /**
     * 檢驗用戶檔案權限的共通實現，此為重載方法默認限定只有擁有者才有權限
     *
     * @param user             用戶信息
     * @param userFileMetadata 用戶文件元數據
     *
     * @return Mono<Void>
     */
    protected Mono<Void> validateUserPermission(User user, UserFileMetadata userFileMetadata) {
        return validateUserPermission(user,
                                      userFileMetadata.getUserId(),
                                      userFileMetadata.getSharedWithUsers(),
                                      userFileMetadata.getId(),
                                      true
        );
    }

    /**
     * 刪除文件的共通實現
     *
     * @param fileId 文件ID
     * @param user   用戶信息
     *
     * @return Mono<Void>
     */
    public Mono<Void> deleteFile(String fileId, User user) {
        return userFileMetaRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata).then(isFileOrFolder(userFileMetadata, false)))
                .flatMap(userFileMetadata -> updateOwner(List.of(userFileMetadata), user).then(Mono.defer(() -> {
                    Mono<Void> deleteFile = Mono.empty();
                    if (userFileMetadata.getServerFileId() != null) {
                        deleteFile = handleUserStorage(user, List.of(userFileMetadata.getServerFileId()));
                    }
                    return deleteFile.then(cleanUserListCache(user.getId(), userFileMetadata.getParentFolderId()).then(
                            userFileMetaRepository.deleteById(userFileMetadata.getId().toString())));
                })));
    }

    /**
     * 編輯文件的共通實現
     * 處理文件名、父文件夾ID、共享用戶ID的更新
     *
     * @param fileEditDTO 文件ID
     * @param user        用戶信息
     *
     * @return Mono<Void>
     */
    public Mono<Void> editFile(FileEditDTO fileEditDTO, User user) {
        return userFileMetaRepository
                .findById(fileEditDTO.getFileId())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE,
                                                                  fileEditDTO.getFileId()
                )))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata)
                        .then(isFileOrFolder(userFileMetadata, false))
                        .then(Mono.defer(() -> {
                            if (fileEditDTO.getParentFolderId() == null) {
                                return Mono.just(userFileMetadata);
                            }
                            return userFileMetaRepository
                                    .findById(fileEditDTO.getParentFolderId().toString())
                                    .flatMap(parentFolder -> validateUserPermission(user, parentFolder)
                                            .then(Mono.just(userFileMetadata))
                                            .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED,
                                                                                              fileEditDTO.getParentFolderId()
                                            ))))
                                    .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE,
                                                                                      fileEditDTO.getParentFolderId()
                                    )));
                        })).then(redisProvider.deleteList(getUserFileListBaseKey(user.getId(), userFileMetadata.getParentFolderId())))
                        .then(Mono.just(userFileMetadata)))
                .flatMap(userFileMetadata -> {
                    userFileMetadata.setFilename(fileEditDTO.getFileName());
                    userFileMetadata.setParentFolderId(fileEditDTO.getParentFolderId());
                    userFileMetadata.setLastAccessTime(LocalDateTime.now());

                    Set<Long> sharedWithUsers = Objects.requireNonNullElse(fileEditDTO.getShareUserIds(),
                                                                           userFileMetadata.getSharedWithUsers()
                    );
                    userFileMetadata.setSharedWithUsers(sharedWithUsers);

                    return userFileMetaRepository.save(userFileMetadata);
                }).flatMap(newUserFileMetadata -> cleanUserListCache(user.getId(), newUserFileMetadata.getParentFolderId()));
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
        return redisProvider.getHashMap(key, "total_chunks", Integer.class)
                .flatMapMany(totalChunks -> Flux
                        .range(1, totalChunks)
                        .flatMap(i -> gridFsProvider.deleteFileByFilename(uploadTaskBO.getTransferTaskId() + "_chunk_" + i))
                        .then(redisProvider.deleteHash(key).then(redisProvider.deleteSet(key + ":pending_chunks"))))
                .then();
    }

    private Mono<Void> handleUserStorage(User user, long fileSize, boolean isDelete) {
        return Mono.defer(() -> {
            if (isDelete) {
                return userRepository.findById(user.getId()).flatMap(userEntity -> {
                    long newStorageUsed = Math.max(userEntity.getUsedStorage() - fileSize, 0);
                    userEntity.setUsedStorage(newStorageUsed);
                    return userRepository.save(userEntity).then();
                });
            }

            if (user.getStorageLimit() == -1 || user.getStorageLimit() - fileSize >= 0) {
                return userRepository.findById(user.getId()).flatMap(userEntity -> {
                    long newStorageUsed = userEntity.getUsedStorage() + fileSize;
                    userEntity.setUsedStorage(newStorageUsed);
                    return userRepository.save(userEntity).then();
                });
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.STORAGE_LIMIT_EXCEEDED,
                                                      ByteEnum.toReadableSize(user.getStorageLimit()),
                                                      ByteEnum.toReadableSize(user.getUsedStorage()),
                                                      ByteEnum.toReadableSize(fileSize)
            ));
        });
    }

    /**
     * 驗證用戶權限的抽象方法
     *
     * @param user       用戶信息
     * @param Owner      文件擁有者
     * @param shareUsers 共享用戶ID
     * @param fileId     文件ID
     * @param ownerOnly  是否僅擁有者有權限
     *
     * @return Mono<Void>
     */
    protected Mono<Void> validateUserPermission(User user, Long Owner, Set<Long> shareUsers, Long fileId, boolean ownerOnly) {
        boolean isOwner = Owner.equals(user.getId());
        boolean isShared = !ownerOnly && shareUsers.contains(user.getId());
        if (isOwner || isShared) {
            return Mono.empty();
        }
        return Mono.error(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, fileId));
    }

    protected Mono<UserFileMetadata> isFileOrFolder(UserFileMetadata userFileMetadata, boolean isFolder) {
        if (userFileMetadata.getIsFolder() == isFolder) {
            return Mono.just(userFileMetadata);
        }
        ValidationException.ErrorCode errorCode = isFolder ? ValidationException.ErrorCode.THIS_OBJECT_ID_NOT_FOLDER : ValidationException.ErrorCode.THIS_OBJECT_ID_NOT_FILE;
        return Mono.error(new ValidationException(errorCode, userFileMetadata.getId()));
    }

    /**
     * 合併已上傳的文件分塊的共通實現
     *
     * @param transferTaskId 任務ID
     * @param totalChunks    總分塊數
     *
     * @return Mono<ObjectId>
     */
    protected Mono<ObjectId> combineChunks(String transferTaskId, int totalChunks) {
        record ChunkData(int index, byte[] data) {
            static Comparator<Object> comparator() {
                return Comparator.comparingInt(a -> ((ChunkData) a).index);
            }
        }

        String key = "upload_task:" + transferTaskId;
        return redisProvider.getHashMap(key, "DTO", UploadTaskBO.class).flatMap(uploadTaskBO -> Mono.defer(() -> {
            Flux<byte[]> chunkFiles = Flux
                    .range(1, totalChunks)
                    .parallel(transfersTasksManager.getAvailableThreadCount())
                    .runOn(Schedulers.boundedElastic())
                    .flatMap(index -> CircuitBreakerOperator
                            .of(CircuitBreaker.of("chunkProcessor", this.circuitBreakerConfig))
                            .apply(this.gridFsProvider
                                           .findFileByFileName(transferTaskId + "_chunk_" + index)
                                           .flatMap(this.gridFsProvider::getResource)
                                           .flatMap(resource -> DataBufferUtils
                                                   .join(resource.getDownloadStream())
                                                   .onErrorResume(e -> Mono.error(new ProcessException(ProcessException.ErrorCode.CANNOT_GET_FILE_STREAM,
                                                                                                       transferTaskId + "_chunk_" + index
                                                   )))
                                                   .map(dataBuffer -> {
                                                       try {
                                                           byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                                           dataBuffer.read(bytes);
                                                           return new ChunkData(index, bytes);
                                                       } finally {
                                                           DataBufferUtils.release(dataBuffer);
                                                       }
                                                   }))))
                    .sequential()
                    .sort(ChunkData.comparator())
                    .cast(ChunkData.class)
                    .map((ChunkData -> ChunkData.data));
            return chunkFiles
                    .collectList()
                    .flatMap(this::combineBytes)
                    .flatMap(combinedBytes -> checkFileStatus(combinedBytes, uploadTaskBO).then(Mono.defer(() -> {
                        uploadTaskBO.setFileType(detectFileType(combinedBytes));
                        processFileAfterFileCheck(uploadTaskBO, combinedBytes).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        return Mono.just(new ObjectId());
                    })));
        }));
    }

    protected Mono<Void> checkFileStatus(byte[] combinedBytes, UploadTaskBO uploadTaskBO) {
        return Mono.defer(() -> {
            if (combinedBytes.length != uploadTaskBO.getFileSize()) {
                transfersTasksManager
                        .updateTransfersTask(uploadTaskBO.getMd5(),
                                             uploadTaskBO.getTransferTaskId(),
                                             TransfersStatusEnum.FAILED,
                                             "文件大小不匹配",
                                             null,
                                             false
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe();
                return Mono.defer(() -> removeTempData(uploadTaskBO).then(Mono.error(new ProcessException(ProcessException.ErrorCode.FILE_SIZE_NOT_MATCH))));
            }

            String computedChunkMd5 = DigestUtils.md5Hex(combinedBytes);
            if (!uploadTaskBO.getMd5().equals(computedChunkMd5)) {
                transfersTasksManager
                        .updateTransfersTask(uploadTaskBO.getMd5(),
                                             uploadTaskBO.getTransferTaskId(),
                                             TransfersStatusEnum.FAILED,
                                             "MD5校驗失敗",
                                             null,
                                             false
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe();
                return Mono.defer(() -> removeTempData(uploadTaskBO).then(Mono.error(new ProcessException(ProcessException.ErrorCode.MD5_NOT_MATCH))));
            }
            return Mono.empty();
        });
    }

    /**
     * 處理MD5校驗成功後的文件存儲邏輯的共通實現
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
                    ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
                    serverFileMetadata.setFileSize(uploadTaskBO.getFileSize());
                    serverFileMetadata.setFileType(uploadTaskBO.getFileType());
                    serverFileMetadata.setMd5(uploadTaskBO.getMd5());
                    serverFileMetadata.setGridFsId(fileGridFsId.toHexString());
                    serverFileMetadata.setUploadTime(LocalDateTime.now());
                    serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                    serverFileMetadata.getOwners().add(uploadTaskBO.getUser().getId());
                    transfersTasksManager
                            .finishTransfersTask(uploadTaskBO.getMd5(), uploadTaskBO.getTransferTaskId(), fileGridFsId.toHexString())
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();

                    Mono<Void> result = handleUserStorage(uploadTaskBO.getUser(), uploadTaskBO.getFileSize(), false);
                    return result
                            .then(serverFileMetaRepository
                                          .save(serverFileMetadata)
                                          .flatMap(serverFile -> associateUserFile(serverFile, uploadTaskBO.formatToFileMetadata()))
                                          .flatMap(userFileMetadata -> userFileMetaRepository
                                                  .save(userFileMetadata)
                                                  .then(cleanUserListCache(userFileMetadata.getUserId(),
                                                                           userFileMetadata.getParentFolderId()
                                                  ))))
                            .then(removeTempData(uploadTaskBO))
                            .then();
                });
    }

    /**
     * 更新文件擁有者的共通實現
     * 若文件只有一個擁有者，則將文件的擁有者列表中移除用戶ID
     *
     * @param userFileList 文件元數據列表
     * @param user         用戶信息
     *
     * @return Mono<Void>
     */
    protected Mono<Void> updateOwner(List<UserFileMetadata> userFileList, User user) {
        return Mono.defer(() -> {
            List<Long> serverFileIds = userFileList
                    .stream()
                    .map(UserFileMetadata::getServerFileId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (userFileList.isEmpty()) {
                return Mono.empty();
            }
            if (serverFileIds.isEmpty()) {
                return Mono.empty();
            }
            return userFileMetaRepository
                    .countByServerFileIdInAndUserId(serverFileIds, user.getId(), databaseClient)
                    .collectList()
                    .flatMap(serverFileMetaCountDaoList -> {
                        Set<Long> serverFileIdList = serverFileMetaCountDaoList
                                .stream()
                                .filter(serverFileMetaCountDao -> serverFileMetaCountDao.count() == 1)
                                .map(ServerFileMetaCountDao::serverFileId)
                                .collect(Collectors.toSet());
                        if (serverFileIdList.isEmpty()) {
                            return Mono.empty();
                        }
                        return serverFileMetaRepository.findAllByIdIn(serverFileIdList).collectList().flatMap(serverFileMetadataList -> {
                            serverFileMetadataList.forEach(serverFileMetadata -> {
                                serverFileMetadata.getOwners().remove(user.getId());
                                serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                            });
                            return serverFileMetaRepository.saveAll(serverFileMetadataList).then();
                        });
                    });
        });
    }

    //todo 線上檔案沒有serverFileId暫不紀錄
    protected Mono<Void> handleUserStorage(User user, List<Long> serverFileIds) {
        if (serverFileIds.isEmpty()) {
            return Mono.empty();
        }
        AtomicLong totalSize = new AtomicLong(0);
        Map<Long, Long> serverFileIdMap = new ConcurrentHashMap<>();
        serverFileIds.forEach(serverFileId -> serverFileIdMap.put(serverFileId, serverFileIdMap.getOrDefault(serverFileId, 0L) + 1));

        return serverFileMetaRepository.findAllByIdIn(serverFileIdMap.keySet()).collectList().flatMap(serverFileMetadataList -> {
            serverFileMetadataList.forEach(serverFileMetadata -> {
                long size = serverFileMetadata.getFileSize() * serverFileIdMap.get(serverFileMetadata.getId());
                totalSize.addAndGet(size);
            });
            return handleUserStorage(user, totalSize.get(), true);
        });
    }

    protected Mono<Void> cleanUserListCache(Long userId, Long... folderIds) {
        CompletableFuture.runAsync(() -> {
            Arrays
                    .stream(folderIds)
                    .distinct()
                    .forEach(folderId -> redisProvider.deleteList(getUserFileListBaseKey(userId, folderId)).subscribe());
        });
        return Mono.empty();
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
        return transfersTasksManager.registerUploadTask(fileMetadataDTO, uploadTaskId).flatMap(isRegisterSuccess -> {
            if (isRegisterSuccess) {
                UploadTaskBO task = fileMetadataDTO.formatToTransferTask(uploadTaskId, "初始化任務成功");
                String key = "upload_task:" + uploadTaskId;
                int totalChunks = getTotalChunks(fileMetadataDTO.getFileSize());

                return redisProvider
                        .setHashMap(key, "DTO", task, Duration.ofHours(6))
                        .then(redisProvider.setHashMap(key, "uploaded_count", 0, Duration.ofHours(6)))
                        .then(redisProvider.setHashMap(key, "total_chunks", totalChunks, Duration.ofHours(6)))
                        .then(redisProvider.generateChunkSet(key + ":pending_chunks", totalChunks))
                        .then(Mono.just(UploadResponseDTO
                                                .builder()
                                                .transferTaskId(uploadTaskId)
                                                .totalChunks(totalChunks)
                                                .chunkSize(CHUNK_SIZE)
                                                .progress(0.0)
                                                .isSuccess(true)
                                                .isFinished(false)
                                                .message("初始化任務成功")
                                                .build()));
            } else {
                String md5 = fileMetadataDTO.getMd5();
                return Mono.error(new ValidationException(ValidationException.ErrorCode.EXISTING_TRANSFER_TASK,
                                                          md5,
                                                          transfersTasksManager
                                                                  .getTransfersTask(md5, TransfersStatusEnum.UPLOADING)
                                                                  .getFirst()
                                                                  .getTransferTaskId()
                ));
            }
        });
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

        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
        Flux<DataBuffer> chunkData = Flux.just(dataBufferFactory.wrap(uploadChunkDTO.getChunkData()));

        return redisProvider
                .deleteSet(pendingChunkKey, chunkIndex)
                .then(gridFsProvider.storeFile(chunkData, transferTaskId + "_chunk_" + chunkIndex))
                .then(redisProvider.incrementHashMap(key, "uploaded_count", 1, Duration.ofHours(1)))
                .flatMap(uploadCount -> {
                    Long uploadCountLong = (Long) uploadCount;
                    double progress = (uploadCountLong.doubleValue() / totalChunks) * 100.0;
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

                    if (uploadCountLong.intValue() == totalChunks) {
                        combineChunks(transferTaskId, totalChunks).subscribeOn(Schedulers.boundedElastic()).subscribe();
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
     * 創建一個新的用戶文件元數據實體
     *
     * @return 返回一個新的實體對象
     */

    public Mono<UserFileMetadata> createUserFileMetadata() {
        return null;
    }

    /**
     * 根據ID獲取一個用戶文件元數據實體
     *
     * @param id 服務器文件元數據ID
     *
     * @return 用戶文件元數據實體對象
     */

    public Mono<UserFileMetadata> getUserFileMetadataById(Long id) {
        if (id == null) {
            return Mono.empty();
        }
        return userFileMetaRepository.findById(id.toString());
    }

    /**
     * 獲取所有用戶文件元數據實體
     */

    public Flux<UserFileMetadata> getAllUserFileMetadata() {
        return null;
    }

    /**
     * 更新一個用戶文件元數據實體
     *
     * @param entity 用戶文件元數據實體對象
     */

    public Mono<Void> updateUserFileMetadata(UserFileMetadata entity) {
        return null;
    }

    /**
     * 刪除一個用戶文件元數據實體
     *
     * @param entity 用戶文件元數據實體對象
     */

    public Mono<Void> deleteUserFileMetadata(UserFileMetadata entity) {
        return null;
    }

    /**
     * 創建一個新的服務器文件元數據實體
     *
     * @return 返回一個新的服務器文件元數據實體對象
     */

    public Mono<ServerFileMetadata> createServerFileMetadata() {
        return null;
    }

    /**
     * 根據ID獲取一個服務器文件元數據實體
     *
     * @param id 服務器文件元數據ID
     *
     * @return 服務器文件元數據實體對象
     */

    public Mono<ServerFileMetadata> getByServerFileMetadataId(Long id) {
        return null;
    }

    /**
     * 獲取所有服務器文件元數據實體
     */

    public Flux<ServerFileMetadata> getAllServerFileMetadata() {
        return null;
    }

    /**
     * 更新一個服務器文件元數據實體
     *
     * @param entity 服務器文件元數據實體對象
     */

    public Mono<ServerFileMetadata> updateServerFileMetadata(ServerFileMetadata entity) {
        return null;
    }

    /**
     * 刪除一個服務器文件元數據實體
     *
     * @param entity 服務器文件元數據實體對象
     */

    public Mono<ServerFileMetadata> deleteServerFileMetadata(ServerFileMetadata entity) {
        return null;
    }
}
