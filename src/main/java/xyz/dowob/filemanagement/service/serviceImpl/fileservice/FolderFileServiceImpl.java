package xyz.dowob.filemanagement.service.serviceImpl.fileservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.annotation.Nullable;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
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
import xyz.dowob.filemanagement.customenum.ConvertProviderEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.*;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.*;
import xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService;
import xyz.dowob.filemanagement.service.serviceInterface.FolderService;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 資料夾檔案服務實現類，提供資料夾相關操作的響應式處理功能。
 * <p>
 * 繼承 {@link AbstractFileService} 並實現 {@link FolderService} 介面，
 * 專門處理資料夾的完整生命週期管理。支援建立、編輯、刪除、恢復、下載等複雜操作，
 * 採用響應式編程模式確保高效能的非阻塞式處理。
 * <p>
 * 提供遞迴資料夾操作和深度遍歷功能，整合智慧型快取管理和樹狀結構維護。
 * 支援資料夾 ZIP 打包下載、檔案名稱衝突處理、多檔案類型支援（一般檔案、線上檔案）、
 * 自動格式轉換等進階功能。內建暫存檔案管理和清理機制，確保系統資源的有效利用。
 *
 * @author yuan
 * @version 1.0
 * @see AbstractFileService
 * @see FolderService
 * @see FileEnum#FOLDER
 * @since 1.0
 */
@Service
@RecordLevel(LogLevelEnum.DEBUG)
@FileHandlerType(FileEnum.FOLDER)
public class FolderFileServiceImpl extends AbstractFileService implements FolderService {
    /**
     * 檔案夾下載的臨時路徑
     */
    private final String downloadFolderPath;

    /**
     * 檔案夾下載的緩衝區大小，當此值設定為0或負數時，則使用默認值4096
     */
    private final int bufferSize;


    /**
     * 檔案夾檔案服務實現類的構造函數。
     * <p>
     * 初始化檔案夾服務的所有必要依賴項目，包括資料庫操作介面、
     * 儲存提供者、緩存管理器、ZIP 壓縮相關設定等核心組件。
     * <p>
     * 特別初始化項目：
     * <ul>
     *   <li>創建並驗證臨時下載目錄</li>
     *   <li>設定 ZIP 壓縮緩衝區大小</li>
     *   <li>初始化檔案夾樹狀結構提供者</li>
     * </ul>
     * <p>
     * <strong>初始化異常處理：</strong>
     * <ul>
     *   <li>如果無法創建臨時下載目錄，拋出 {@code ProcessException}</li>
     *   <li>如果緩衝區大小設定無效，使用預設值 4096</li>
     * </ul>
     * <p>
     * <strong>依賴注入示例：</strong>
     * <pre>{@code
     * @Service
     * public class FolderFileServiceImpl extends AbstractFileService implements FolderService {
     *     public FolderFileServiceImpl(
     *         ServerFileMetaRepository serverRepo,
     *         UserFileMetaRepository userRepo,
     *         RedisProvider redisProvider,
     *         GridFsProvider gridFsProvider,
     *         FileProperties fileProperties
     *     ) throws ProcessException {
     *         super(serverRepo, userRepo, redisProvider, gridFsProvider, ...);
     *         this.downloadFolderPath = initDownloadPath(fileProperties);
     *         this.bufferSize = initBufferSize(fileProperties);
     *     }
     * }
     * }</pre>
     *
     * @param serverFileMetaRepository      伺服器檔案元資料資料庫操作介面
     * @param userFileMetaRepository        用戶檔案元資料資料庫操作介面
     * @param redisProvider                 Redis 緩存提供者，用於緩存管理
     * @param gridFsProvider                GridFS 儲存提供者，用於分散式檔案儲存
     * @param transfersTasksManager         檔案傳輸任務管理器
     * @param fileProperties                檔案相關設定屬性，包含下載路徑和緩衝區設定
     * @param circuitBreakerConfig          斷路器設定，用於系統穩定性保護
     * @param userRepository                用戶資料庫操作介面
     * @param userOnlineFileRepository      用戶線上檔案資料庫操作介面
     * @param entityOperations              R2DBC 實體操作介面，用於非阻塞式資料庫操作
     * @param fileTrashRecordRepository     檔案回收站記錄資料庫操作介面
     * @param transactionalOperator         事務操作器，用於響應式事務管理
     * @param rateLimiterConfig             限流器設定，用於控制請求頻率
     * @param userFIleShareRecordRepository 用戶檔案分享記錄資料庫操作介面
     * @param objectMapper                  JSON 序列化工具，用於物件轉換
     * @param cacheManager                  緩存管理器，統一管理各種緩存操作
     * @param folderListTreeProvider        檔案夾樹狀結構提供者（可選）
     * @param fileScanProvider              檔案安全掃描提供者（可選）
     *
     * @throws ProcessException 當無法創建臨時下載目錄時拋出
     */
    public FolderFileServiceImpl(ServerFileMetaRepository serverFileMetaRepository, UserFileMetaRepository userFileMetaRepository, RedisProvider redisProvider, GridFsProvider gridFsProvider, TransfersTasksManager transfersTasksManager, FileProperties fileProperties, CircuitBreakerConfig circuitBreakerConfig, UserRepository userRepository, UserOnlineFileRepository userOnlineFileRepository, R2dbcEntityOperations entityOperations, FileTrashRecordRepository fileTrashRecordRepository, TransactionalOperator transactionalOperator, RateLimiterConfig rateLimiterConfig, UserFIleShareRecordRepository userFIleShareRecordRepository, ObjectMapper objectMapper, CacheManager cacheManager,
                                 @Nullable FolderListTreeProvider folderListTreeProvider,
                                 @Nullable FileScanProvider fileScanProvider) throws ProcessException {
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

        String tempDownloadPath = fileProperties.getDownload().getFolderTempDownloadPath();
        if (!tempDownloadPath.endsWith("/")) {
            tempDownloadPath = tempDownloadPath + "/";
        }
        this.downloadFolderPath = tempDownloadPath;

        File file = new File(tempDownloadPath);
        if (!file.exists() && !file.mkdirs()) {
            throw new ProcessException(ProcessException.ErrorCode.CREATE_TEMP_DOWNLOAD_FOLDER_FAILED, tempDownloadPath);
        }

        int bs = (int) fileProperties.getDownload().getZipBufferSize().toBytes();
        if (bs <= 0) {
            bs = 4096;
        }
        this.bufferSize = bs;
    }


    /**
     * 建立新的資料夾。
     * <p>
     * 根據提供的編輯資料在資料庫中建立新的資料夾記錄。設定屬性包括所有者、
     * 建立時間、父資料夾關係等。支援資料夾分享功能，操作完成後清理相關快取。
     *
     * @param fileEditDTO 資料夾編輯資訊，包含資料夾名稱、父資料夾 ID 等
     * @param user        當前操作的用戶
     *
     * @return 表示建立操作完成的響應式信號
     */
    @Override
    public Mono<UserFileMetadata> createFolder(FileEditDTO fileEditDTO, User user) {
        return Mono.defer(() -> {
            UserFileMetadata folder = new UserFileMetadata();
            folder.setUserId(user.getId());
            folder.setFilename(fileEditDTO.getFilename());
            folder.setParentFolderId(fileEditDTO.getParentFolderId());
            folder.setLastAccessTime(LocalDateTime.now());
            folder.setUploadTime(LocalDateTime.now());
            folder.setFileType(FileEnum.FOLDER);
            Mono<UserFileMetadata> action = userFileMetaRepository.save(folder).flatMap(newFolder -> {
                if (folderListTreeProvider != null) {
                    try {
                        folderListTreeProvider.addFolder(user.getId(), folder);
                    } catch (ProcessException | ValidationException e) {
                        return Mono.error(e);
                    }
                }
                List<UserFileShareRecord> userFileShareRecords = new ArrayList<>();

                if (fileEditDTO.getShareUsers() != null && !fileEditDTO.getShareUsers().isEmpty()) {
                    fileEditDTO.getShareUsers().forEach(shareUserEditPO -> {
                        userFileShareRecords.add(new UserFileShareRecord(shareUserEditPO.getUserId(), newFolder.getId()));
                    });
                }

                return userFIleShareRecordRepository
                        .saveAll(userFileShareRecords)
                        .then(cleanUserListCache(user.getId(), newFolder.getParentFolderId()))
                        .thenReturn(newFolder);
            });
            return transactionalOperator.transactional(action);
        });
    }


    /**
     * 編輯資料夾屬性和設定。
     * <p>
     * 支援修改資料夾名稱、移動位置、更新分享設定、遞迴更新子資料夾屬性等操作。
     * 特別優化快取管理，確保在資料夾移動時正確清理原始位置和更新新位置的快取。
     * 支援遞迴更新所有子資料夾的相關屬性。
     *
     * @param fileEditBO 資料夾編輯業務物件
     * @param user       執行編輯操作的用戶
     *
     * @return 表示編輯操作完成的響應式信號
     */
    @Override
    public Mono<Void> editFolder(FileEditBO fileEditBO, User user) {
        FileEditDTO fileEditDTO = fileEditBO.getFileEditDTO();
        Long oldParentFolderId = fileEditBO.getUserFileMetadata().getParentFolderId();
        return Mono.defer(() -> {
            if (fileEditBO.getParentFolderFileMetadata() != null) {
                return getUserFilePaths(fileEditBO.getParentFolderFileMetadata(), user).flatMap(nodeList -> {
                    if (nodeList
                            .stream()
                            .filter(node -> Objects.nonNull(node.getFolderId()))
                            .anyMatch(node -> node.getFolderId().toString().equals(fileEditDTO.getFileId()))) {
                        return Mono.error(new ValidationException(ValidationException.ErrorCode.MOVE_TO_CHILD_FOLDER,
                                                                  fileEditDTO.getFileId(),
                                                                  fileEditDTO.getParentFolderId()
                        ));
                    }
                    return Mono.just(fileEditBO.getUserFileMetadata());
                });
            }
            return Mono.just(fileEditBO.getUserFileMetadata());
        }).flatMap(userFileMetadata -> {
            if (folderListTreeProvider != null) {
                try {
                    folderListTreeProvider.updateFolder(user.getId(), userFileMetadata, fileEditDTO);
                } catch (ValidationException | ProcessException e) {
                    return Mono.error(e);
                }
            }

            userFileMetadata.setFilename(fileEditDTO.getFilename());
            userFileMetadata.setParentFolderId(fileEditDTO.getParentFolderId());
            userFileMetadata.setLastAccessTime(LocalDateTime.now());

            FileShareTypeEnum shareType = Objects.requireNonNullElse(fileEditDTO.getShareType(), userFileMetadata.getShareType());
            userFileMetadata.setShareType(shareType);

            Boolean isStar = Objects.requireNonNullElse(fileEditDTO.getIsStar(), userFileMetadata.getIsStar());
            userFileMetadata.setIsStar(isStar);

            Set<Long> cleanMainCacheFolder = new HashSet<>();
            cleanMainCacheFolder.add(oldParentFolderId);
            cleanMainCacheFolder.add(fileEditDTO.getParentFolderId());

            if (!Objects.equals(oldParentFolderId, fileEditDTO.getParentFolderId())) {
                cleanMainCacheFolder.add(userFileMetadata.getId());
            }

            Mono<Void> handleChildMono = Mono.empty();
            if (fileEditDTO.getRecursiveSetting()) {
                handleChildMono = Mono.defer(() -> findAllChildFolder(Collections.singletonList(userFileMetadata.getId()),
                                                                      new ArrayList<>(List.of(userFileMetadata))
                ).flatMap(toEditFolderList -> {
                    Mono<Void> processShareUserMono = processShareUser(toEditFolderList, fileEditDTO).then();
                    Mono<Void> settingChildFolder = Mono.defer(() -> {
                        toEditFolderList.forEach(fileMetadata -> {
                            fileMetadata.setShareType(shareType);
                        });
                        return userFileMetaRepository.saveAll(toEditFolderList).then();
                    });

                    Set<Long> cleanChildCacheFolder = new HashSet<>();
                    toEditFolderList.forEach(fileMetadata -> {
                        cleanChildCacheFolder.add(fileMetadata.getParentFolderId());
                        cleanChildCacheFolder.add(fileMetadata.getId());
                    });

                    return Mono
                            .when(processShareUserMono, settingChildFolder)
                            .then(cleanUserListCache(user.getId(), cleanChildCacheFolder.toArray(new Long[0])));

                }).subscribeOn(Schedulers.boundedElastic()));
            }

            Mono<UserFileMetadata> processShareUserMono = processShareUser(Collections.singletonList(userFileMetadata), fileEditDTO).next();
            return transactionalOperator
                    .transactional(handleChildMono.then(Mono.when(processShareUserMono, userFileMetaRepository.save(userFileMetadata))))
                    .then(Mono.defer(() -> cleanUserListCache(user.getId(), cleanMainCacheFolder.toArray(new Long[0]))));
        });
    }


    /**
     * 響應式永久刪除檔案夾的實現，支持邏輯刪除和緩存管理。
     * <p>
     * 執行檔案夾刪除的完整流程，包括：
     * <ul>
     *   <li>遞迴查找並刪除所有子檔案夾和檔案</li>
     *   <li>更新用戶檔案元資料</li>
     *   <li>清理相關緩存</li>
     *   <li>更新檔案夾列表樹</li>
     * </ul>
     *
     * <p>注意：此方法執行邏輯刪除，不會立即從資料庫中移除檔案夾，而是標記為已刪除。</p>
     *
     * @param folder 要刪除的檔案夾元資料
     * @param user   執行刪除操作的用戶
     *
     * @return {@link reactor.core.publisher.Mono}<{@link Void}> 表示刪除操作的響應式完成信號
     */
    @Override
    public Mono<Void> deleteFolder(UserFileMetadata folder, User user) {
        return Mono.just(folder).flatMap(userFileMetadata -> {
            if (folderListTreeProvider != null) {
                folderListTreeProvider.deleteFolder(user.getId(), userFileMetadata.getId());
            }
            List<UserFileMetadata> userFileList = new ArrayList<>();
            List<Long> parentFolderIdList = Collections.singletonList(userFileMetadata.getId());
            return findAllChildFolder(parentFolderIdList, userFileList)
                    .flatMap(childFolderList -> {
                        List<Long> serverFileIds = childFolderList.stream().map(UserFileMetadata::getServerFileId).filter(Objects::nonNull).toList();
                        return calculateFileSize(user, serverFileIds);
                    })
                    .then(Mono.when(cleanUserListCache(user.getId(), userFileMetadata.getParentFolderId()), updateOwner(userFileList, user.getId())))
                    .then(userFileMetaRepository.delete(userFileMetadata));
        });
    }


    /**
     * 下載資料夾為 ZIP 壓縮檔。
     * <p>
     * 執行資料夾的遞迴打包操作，支援多種檔案類型：一般檔案、線上檔案、子資料夾。
     * 自動處理檔案名稱衝突，生成 ZIP 壓縮檔案供下載。使用有界調度器確保非阻塞操作，
     * 下載完成後自動清理暫存檔案。
     *
     * @param rootFolder 要下載的根資料夾元資料
     * @param user       執行下載操作的用戶
     *
     * @return 包含 ZIP 壓縮檔資料的業務物件
     */
    @Override
    public Mono<UserFileDataBO> downloadFolder(UserFileMetadata rootFolder, User user) {
        try {
            String zipFileName = getTempZipFilename(rootFolder);
            String tempDownloadPath = downloadFolderPath + zipFileName;
            Map<String, AtomicInteger> zipEntryNameCountMap = new ConcurrentHashMap<>();
            return Mono
                    .usingWhen(Mono.just(new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tempDownloadPath)))),
                               zos -> processFolder(zos, rootFolder, rootFolder.getFilename(), zipEntryNameCountMap, user),
                               zos -> Mono.fromRunnable(() -> {
                                   try {
                                       zos.close();
                                   } catch (IOException e) {
                                       throw Exceptions.propagate(e);
                                   }
                               }).subscribeOn(Schedulers.boundedElastic())
                    )
                    .then(Mono.defer(() -> {
                        File zipFile = new File(tempDownloadPath);
                        long fileSize = zipFile.length();
                        Flux<DataBuffer> dataFlux = DataBufferUtils.read(zipFile.toPath(), DefaultDataBufferFactory.sharedInstance, bufferSize);

                        UserFileDataBO userFileDataBO = UserFileDataBO
                                .builder()
                                .fileSize(fileSize)
                                .filename(zipFileName)
                                .fileType(FileEnum.ZIP)
                                .dataBufferFlux(dataFlux)
                                .build();
                        return Mono.just(userFileDataBO);
                    }).doFinally(signal -> {
                        File file = new File(tempDownloadPath);
                        if (file.exists() && !file.delete()) {
                            throw new RuntimeException(new ProcessException(ProcessException.ErrorCode.DELETE_TEMP_FILE_FAILED, tempDownloadPath));
                        }
                    }));
        } catch (FileNotFoundException e) {
            throw Exceptions.propagate(e);
        }
    }


    /**
     * 響應式處理檔案夾遞迴壓縮的核心邏輯，支持複雜的檔案夾結構和多檔案類型。
     * <p>
     * 操作流程：
     * <ul>
     *   <li>查詢當前目錄下的所有檔案和子檔案夾</li>
     *   <li>遍歷子檔案夾，遞迴將所有檔案壓縮到ZIP檔案中</li>
     *   <li>支持各種檔案類型：
     *     <ul>
     *       <li>GridFS儲存的一般檔案</li>
     *       <li>線上文檔</li>
     *       <li>子檔案夾</li>
     *     </ul>
     *   </li>
     *   <li>自動解決檔案名稱衝突</li>
     * </ul>
     * </p>
     *
     * <p>
     * 核心特性：
     * <ul>
     *   <li>完全非阻塞的檔案壓縮遍歷</li>
     *   <li>支持複雜的檔案夾和檔案結構</li>
     *   <li>高效率的資源處理</li>
     * </ul>
     * </p>
     *
     * @param zipOutputStream      要寫入的ZIP壓縮輸出流
     * @param folder               當前必需壓縮的檔案夾元資料
     * @param parentPath           父目錄路徑，用於設定壓縮檔案的目錄結構
     * @param zipEntryNameCountMap 用於處理重複檔案名稱的映射
     * @param user                 執行下載操作的用戶
     *
     * @return 表示壓縮操作的響應式完成信號
     */
    private Mono<Void> processFolder(ZipOutputStream zipOutputStream, UserFileMetadata folder, String parentPath, Map<String, AtomicInteger> zipEntryNameCountMap, User user) {
        return Mono.defer(() -> findFilesWithSameParentFolderIds(folder.getId(), user).flatMap(files -> {
            List<UserFileMetadata> subFolders = new ArrayList<>();
            List<UserFileMetadata> generalFileMetadatas = new ArrayList<>();
            List<UserFileMetadata> onlineFileMetadatas = new ArrayList<>();

            files.forEach(file -> {
                if (file.getFileType() == FileEnum.FOLDER) {
                    subFolders.add(file);
                } else if (file.getFileType() == FileEnum.ONLINE_DOCUMENT) {
                    onlineFileMetadatas.add(file);
                } else if (file.getServerFileId() != null) {
                    generalFileMetadatas.add(file);
                }
            });

            Flux<Pair<String, Flux<DataBuffer>>> generalFileWriteTasks = getGeneralFileResource(generalFileMetadatas).map(pair -> {
                Flux<DataBuffer> dataFlux = pair.getFirst();
                UserFileMetadata metadata = pair.getSecond();
                String filePath = parentPath + "/" + metadata.getFilename();
                String zipEntryName = resolveUniqueEntryName(filePath, zipEntryNameCountMap);
                return Pair.of(zipEntryName, dataFlux);
            });

            Flux<Pair<String, Flux<DataBuffer>>> onlineFileWriteTasks = getOnlineFileResource(onlineFileMetadatas).flatMap(pair -> {
                Flux<DataBuffer> dataFlux = pair.getFirst();
                UserFileMetadata metadata = pair.getSecond();
                String filePath = parentPath + "/" + metadata.getFilename();
                String zipEntryName = resolveUniqueEntryName(filePath, zipEntryNameCountMap);
                return Mono.just(Pair.of(zipEntryName, dataFlux));
            });

            Mono<Void> filesProcessing = Flux.merge(generalFileWriteTasks, onlineFileWriteTasks).concatMap(taskPair -> {
                String zipEntryName = taskPair.getFirst();
                Flux<DataBuffer> dataBufferFlux = taskPair.getSecond();
                return writeIntoZip(dataBufferFlux, zipOutputStream, zipEntryName);
            }).then();

            Mono<Void> foldersProcessing = Flux.fromIterable(subFolders).concatMap(subFolder -> {
                String originalPath = parentPath + "/" + subFolder.getFilename();
                String uniqueFolderPath = resolveUniqueEntryName(originalPath, zipEntryNameCountMap);
                Mono<Void> createFolderEntry = Mono.fromRunnable(() -> {
                    try {
                        synchronized (zipOutputStream) {
                            zipOutputStream.putNextEntry(new ZipEntry(uniqueFolderPath + "/"));
                            zipOutputStream.closeEntry();
                        }
                    } catch (IOException e) {
                        throw Exceptions.propagate(e);
                    }
                }).subscribeOn(Schedulers.boundedElastic()).then();
                return createFolderEntry.then(processFolder(zipOutputStream, subFolder, uniqueFolderPath, zipEntryNameCountMap, user));
            }).then();
            return filesProcessing.then(foldersProcessing);
        }));
    }    /**
     * 恢復資料夾及其所有子內容。
     * <p>
     * 恢復已刪除的資料夾，包括檢查父資料夾狀態、遞迴恢復所有子資料夾、
     * 刪除回收站記錄、更新相關快取和資料夾樹狀結構。
     *
     * @param folder 要恢復的資料夾
     * @param user   當前操作的用戶
     *
     * @return 恢復後的資料夾元資料
     */
    @Override
    public Mono<UserFileMetadata> restoreFile(UserFileMetadata folder, User user) {
        return Mono
                .defer(() -> {
                    if (!folder.getIsDeleted()) {
                        return Mono.error(new ValidationException(ValidationException.ErrorCode.SOME_FILE_NOT_DELETED, folder.getId()));
                    }

                    folder.setIsDeleted(false);
                    folder.setLastAccessTime(LocalDateTime.now());
                    if (folder.getParentFolderId() != null) {
                        return userFileMetaRepository.findById(folder.getParentFolderId().toString()).flatMap(parentFolder -> {
                            if (parentFolder.getIsDeleted()) {
                                folder.setParentFolderId(null);
                            }
                            return Mono.just(folder);
                        });
                    }
                    return Mono.just(folder);
                })
                .then(Mono.defer(() -> findAllChildFolder(Collections.singletonList(folder.getId()), new ArrayList<>(List.of(folder))).flatMap(
                        childFolderList -> {
                            childFolderList.forEach(userFile -> userFile.setIsDeleted(false));
                            return fileTrashRecordRepository
                                    .deleteById(folder.getId())
                                    .thenMany(userFileMetaRepository.saveAll(childFolderList))
                                    .collectList()
                                    .flatMap(userfileList -> {
                                        Long[] parentFolderIds = userfileList
                                                .stream()
                                                .map(UserFileMetadata::getParentFolderId)
                                                .distinct()
                                                .toArray(Long[]::new);
                                        return cleanUserListCache(userfileList.getFirst().getUserId(), parentFolderIds);
                                    })
                                    .then(Mono.defer(() -> {
                                        if (folderListTreeProvider != null) {
                                            try {
                                                folderListTreeProvider.addFolders(user.getId(), childFolderList);
                                            } catch (ProcessException | ValidationException e) {
                                                return Mono.error(e);
                                            }
                                        }
                                        return Mono.just(folder);
                                    }));
                        })));
    }


    /**
     * 批量恢復多個資料夾。
     * <p>
     * 對指定的資料夾集合逐個執行恢復操作。
     *
     * @param folders 要恢復的資料夾集合
     * @param user    當前操作的用戶
     *
     * @return 恢復結果的響應式流
     */
    @Override
    public Flux<UserFileMetadata> restoreFile(Iterable<UserFileMetadata> folders, User user) {
        return Flux.fromIterable(folders).flatMap(folder -> restoreFile(folder, user));
    }


    /**
     * 刪除資料夾及其所有內容。
     * <p>
     * 執行資料夾的刪除操作，包括遞迴查找所有子資料夾和檔案、
     * 建立回收站記錄、標記為已刪除、清理相關快取和更新資料夾樹。
     * 使用延遲雙刪模式確保快取一致性。
     *
     * @param folder 要刪除的資料夾
     * @param user   當前操作的用戶
     *
     * @return 是否刪除成功
     */
    @Override
    public Mono<Boolean> removeFile(UserFileMetadata folder, User user) {
        return findAllChildFolder(Collections.singletonList(folder.getId()), new ArrayList<>(List.of(folder))).flatMap(toDeleteFolderList -> {
            boolean isAnyDeleted = toDeleteFolderList.stream().anyMatch(UserFileMetadata::getIsDeleted);
            if (isAnyDeleted) {
                return Mono.just(false);
            }

            return Mono.defer(() -> {
                LocalDateTime deleteTime = LocalDateTime.now().plusDays(fileProperties.getBackup().getRetentionTime().toDays());
                FileTrashRecord fileTrashRecord = new FileTrashRecord(toDeleteFolderList.getFirst(), deleteTime);
                toDeleteFolderList.forEach(userFile -> userFile.setIsDeleted(true));

                Long[] parentFolderIds = toDeleteFolderList.stream().map(UserFileMetadata::getParentFolderId).distinct().toArray(Long[]::new);
                Long userId = toDeleteFolderList.getFirst().getUserId();

                Mono<List<UserFileMetadata>> databaseOperation = fileTrashRecordRepository
                        .insert(fileTrashRecord, entityOperations)
                        .thenMany(userFileMetaRepository.saveAll(toDeleteFolderList))
                        .collectList();

                Mono<Void> cacheCleanupOperation = cleanUserListCache(userId, parentFolderIds).doOnSuccess(v -> {
                    if (folderListTreeProvider != null) {
                        try {
                            folderListTreeProvider.deleteFolder(user.getId(), folder.getId());
                        } catch (Exception e) {
                            throw new RuntimeException("更新檔案夾列表樹時發生錯誤", e);
                        }
                    }
                });
                return transactionalOperator.transactional(databaseOperation).then(cacheCleanupOperation).thenReturn(true).onErrorReturn(false);
            });
        });
    }


    /**
     * 批量刪除多個資料夾。
     * <p>
     * 對指定的資料夾集合逐個執行刪除操作。
     *
     * @param folders 要刪除的資料夾集合
     * @param user    當前操作的用戶
     *
     * @return 所有資料夾是否都刪除成功
     */
    @Override
    public Mono<Boolean> removeFile(Iterable<UserFileMetadata> folders, User user) {
        return Flux.fromIterable(folders).flatMap(folder -> removeFile(folder, user)).all(Boolean::booleanValue);
    }





    /**
     * 響應式獲取一般檔案資源的高效方法，支持批次處理和高併發檔案讀取。
     *
     * <p>檔案資源機制：
     * <ul>
     *   <li>按照GridFS檔案ID分組檔案資源</li>
     *   <li>立即讀取檔案的資料緩存作為 DataBuffer</li>
     *   <li>保證高效率和記憶體效能</li>
     * </ul>
     * </p>
     *
     * <p>核心特性：
     * <ul>
     *   <li>完全非阻塞的檔案讀取</li>
     *   <li>支持大量檔案的分批處理</li>
     *   <li>自動管理資源釋放</li>
     * </ul>
     * </p>
     *
     * @param files 需要獲取的檔案元資料列表
     *
     * @return 檔案資源的響應式流
     */
    private Flux<Pair<Flux<DataBuffer>, UserFileMetadata>> getGeneralFileResource(List<UserFileMetadata> files) {
        Map<Long, List<UserFileMetadata>> userMetadatasByServerId = files
                .stream()
                .filter(file -> file.getServerFileId() != null)
                .collect(Collectors.groupingBy(UserFileMetadata::getServerFileId));

        if (userMetadatasByServerId.isEmpty()) {
            return Flux.empty();
        }

        return serverFileMetaRepository
                .findAllByIdIn(userMetadatasByServerId.keySet())
                .collectMap(ServerFileMetadata::getId, serverMeta -> serverMeta)
                .flatMapMany(serverFileMetadatasMap -> Flux.fromIterable(userMetadatasByServerId.entrySet()).flatMap(entry -> {
                    Long serverFileId = entry.getKey();
                    List<UserFileMetadata> userMetadatasForThisServerFile = entry.getValue();
                    ServerFileMetadata serverFileMeta = serverFileMetadatasMap.get(serverFileId);
                    return gridFsProvider
                            .findFileById(new ObjectId(serverFileMeta.getGridFsId()))
                            .flatMapMany(gridFSFile -> Flux.fromIterable(userMetadatasForThisServerFile).map(userMeta -> {
                                Flux<DataBuffer> newDataFlux = gridFsProvider
                                        .getResource(gridFSFile)
                                        .flatMapMany(ReactiveGridFsResource::getDownloadStream);
                                return Pair.of(newDataFlux, userMeta);
                            }));
                }));
    }


    /**
     * 響應式獲取線上檔案資源的高級實現，支持多檔案類型和動態轉換。
     *
     * <p>檔案轉換機制：
     * <ul>
     *   <li>支持多種線上檔案格式的轉換</li>
     *   <li>使用內建的檔案轉換提供程式</li>
     *   <li>自動處理檔案名稱和檔案後綴</li>
     * </ul>
     * </p>
     *
     * <p>核心特性：
     * <ul>
     *   <li>完全非阻塞的檔案轉換</li>
     *   <li>支持大量檔案的線上轉換</li>
     *   <li>高效率的資源管理</li>
     * </ul>
     * </p>
     *
     * @param files 需要轉換的線上檔案元資料列表
     *
     * @return 檔案轉換資源的響應式流
     */
    private Flux<Pair<Flux<DataBuffer>, UserFileMetadata>> getOnlineFileResource(List<UserFileMetadata> files) {
        Map<String, UserFileMetadata> userFileMetadataMap = files
                .stream()
                .collect(Collectors.toMap(metadata -> metadata.getId().toString(), userFileMetadata -> userFileMetadata));

        ContentConvertProvider convertProvider = ContentConvertProviderFactory.createProvider(ConvertProviderEnum.DOCX, new ConvertConfig());
        return userOnlineFileRepository
                .findAllById(userFileMetadataMap.keySet())
                .flatMap(userOnlineFile -> convertProvider.convertToDataBuffer(userOnlineFile.getContent()).flatMap(record -> {
                    UserFileMetadata userFileMetadata = userFileMetadataMap.get(userOnlineFile.getId().toString());
                    String name = userFileMetadata.getFilename().split("\\.")[0] + "." + ConvertProviderEnum.DOCX.getSuffix();
                    userFileMetadata.setFilename(name);
                    return Mono.just(Pair.of(record.dataBuffer(), userFileMetadata));
                }));
    }


    /**
     * 將檔案寫入到 ZipOutputStream 中
     * 此方法會將給定的 Flux<DataBuffer> 寫入到 ZipOutputStream 中
     * 如果寫入過程中發生錯誤，則會拋出異常
     *
     * @param dataBufferFlux  輸入流
     * @param zipOutputStream 壓縮輸出流
     * @param filePath        檔案路徑
     *
     * @return Mono<Void>
     */
    private Mono<Void> writeIntoZip(Flux<DataBuffer> dataBufferFlux, ZipOutputStream zipOutputStream, String filePath) {
        Mono<Void> putEntryMono = Mono.fromRunnable(() -> {
            try {
                synchronized (zipOutputStream) {
                    zipOutputStream.putNextEntry(new ZipEntry(filePath));
                }
            } catch (IOException e) {
                throw Exceptions.propagate(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();

        Mono<Void> writeDataMono = dataBufferFlux.publishOn(Schedulers.boundedElastic()).doOnNext(buffer -> {
            try {
                synchronized (zipOutputStream) {
                    int readableBytes = buffer.readableByteCount();
                    byte[] bytes = new byte[readableBytes];
                    buffer.read(bytes);
                    zipOutputStream.write(bytes);
                }
            } catch (IOException e) {
                throw Exceptions.propagate(e);
            } finally {
                DataBufferUtils.release(buffer);
            }
        }).then();

        Mono<Void> closeEntryMono = Mono.fromRunnable(() -> {
            try {
                synchronized (zipOutputStream) {
                    zipOutputStream.closeEntry();
                }
            } catch (IOException e) {
                throw Exceptions.propagate(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
        return putEntryMono.then(writeDataMono).onErrorResume(e -> closeEntryMono.then(Mono.error(e))).then(closeEntryMono);
    }


    /**
     * 找尋指定檔案夾的所有子檔案夾
     *
     * @param parentFolderIdList 父檔案夾ID列表
     * @param childFolderList    子檔案夾列表
     *
     * @return List<UserFileMetadata> 子檔案夾列表
     */
    private Mono<List<UserFileMetadata>> findAllChildFolder(List<Long> parentFolderIdList, List<UserFileMetadata> childFolderList) {
        return findFilesWithSameParentFolderIds(parentFolderIdList).flatMap(subFile -> {
            List<UserFileMetadata> nextSubFolder = subFile
                    .stream()
                    .filter(userFileMetadata -> userFileMetadata.getFileType() == FileEnum.FOLDER)
                    .toList();
            if (nextSubFolder.isEmpty()) {
                return Mono.just(childFolderList);
            }
            childFolderList.addAll(nextSubFolder);
            return findAllChildFolder(nextSubFolder.stream().map(UserFileMetadata::getId).toList(), childFolderList);
        });
    }


    /**
     * 查詢具有相同父檔案夾ID的檔案列表
     *
     * @param parentFolderIds 父檔案夾ID列表
     *
     * @return Mono<List < UserFileMetadata>>  檔案列表
     */
    private Mono<List<UserFileMetadata>> findFilesWithSameParentFolderIds(List<Long> parentFolderIds) {
        return userFileMetaRepository
                .findAllByParentFolderIdIn(parentFolderIds, entityOperations)
                .collectList()
                .switchIfEmpty(Mono.just(Collections.emptyList()));
    }


    /**
     * 查詢具有相同父檔案夾ID的檔案列表，此為重載方法
     * 將會只查詢資料夾以及指定用戶所擁有權限的檔案
     *
     * @param parentFolderId 父檔案夾ID
     * @param shareUser      分享用戶
     *
     * @return Mono<List < UserFileMetadata>> 檔案列表
     */
    private Mono<List<UserFileMetadata>> findFilesWithSameParentFolderIds(Long parentFolderId, User shareUser) {
        return userFileMetaRepository
                .findAllByParentFolderIdWithShare(parentFolderId, shareUser, entityOperations)
                .collectList()
                .switchIfEmpty(Mono.just(Collections.emptyList()));
    }


    /**
     * 獲取臨時壓縮檔案名
     *
     * @param folder 檔案夾
     *
     * @return String 臨時壓縮檔案名
     */
    @SkipRecord
    private String getTempZipFilename(UserFileMetadata folder) {
        return folder.getId() + "_" + folder.getFilename() + ".zip";
    }


    /**
     * 獲取唯一的壓縮檔案名稱
     * 當檔案名稱重複時，會在檔案名稱後面加上 "(n)" 的格式
     *
     * @param originalPath 原始路徑
     * @param nameMap      名稱映射
     *
     * @return String 唯一的壓縮檔案名稱
     */
    private String resolveUniqueEntryName(String originalPath, Map<String, AtomicInteger> nameMap) {
        AtomicInteger counter = nameMap.computeIfAbsent(originalPath, k -> new AtomicInteger(0));
        int count = counter.getAndIncrement();

        if (count == 0) {
            return originalPath;
        }

        int dotIndex = originalPath.lastIndexOf('.');
        if (dotIndex != -1 && !originalPath.endsWith("/")) {
            String name = originalPath.substring(0, dotIndex);
            String ext = originalPath.substring(dotIndex);
            return name + " (" + count + ")" + ext;
        } else {
            return originalPath + " (" + count + ")";
        }
    }
}
