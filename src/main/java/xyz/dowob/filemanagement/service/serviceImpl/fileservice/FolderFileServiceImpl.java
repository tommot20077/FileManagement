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
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.Assert;
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
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.*;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.*;
import xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService;
import xyz.dowob.filemanagement.service.serviceInterface.FolderService;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件夾文件服務實現，處理文件夾的相關操作
 * 繼承自AbstractFileService，實現了FolderService接口
 * AbstractFileService中定義了文件服務的共通操作，而FolderService定義了文件夾服務的操作
 * 通過FileHandlerType註解標記為文件夾文件服務
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FolderFileServiceImpl
 * @create 2025/2/15
 * @Version 1.0
 **/
@Service
@RecordLevel(LogLevelEnum.DEBUG)
@FileHandlerType(FileEnum.FOLDER)
public class FolderFileServiceImpl extends AbstractFileService implements FolderService {
    /**
     * 文件夾下載的臨時路徑
     */
    private final String downloadFolderPath;

    /**
     * 文件夾下載的緩衝區大小，當此值設定為0或負數時，則使用默認值4096
     */
    private final int bufferSize;

    /**
     * 單個文件夾下載的最大併發限制，當此值設定為0或負數時，則使用默認值5
     */
    private final int maxConcurrentLimit;


    /**
     * 文件夾文件服務實現類，繼承 @see {@link AbstractFileService}
     *
     * @param serverFileMetaRepository      伺服器檔案元數據操作介面
     * @param userFileMetaRepository        用戶檔案元數據操作介面
     * @param redisProvider                 Redis提供者
     * @param gridFsProvider                GridFS提供者
     * @param transfersTasksManager         傳輸任務管理器
     * @param fileProperties                檔案屬性配置
     * @param circuitBreakerConfig          CircuitBreaker配置
     * @param userRepository                用戶操作介面
     * @param userOnlineFileRepository      用戶在線檔案操作介面
     * @param entityOperations              R2DBC實體操作介面
     * @param fileTrashRecordRepository     檔案垃圾桶記錄操作介面
     * @param transactionalOperator         事務操作介面
     * @param rateLimiterConfig             RateLimiter配置
     * @param userFIleShareRecordRepository 用戶檔案分享記錄操作介面
     */
    public FolderFileServiceImpl(ServerFileMetaRepository serverFileMetaRepository, UserFileMetaRepository userFileMetaRepository, RedisProvider redisProvider, GridFsProvider gridFsProvider, TransfersTasksManager transfersTasksManager, FileProperties fileProperties, CircuitBreakerConfig circuitBreakerConfig, UserRepository userRepository, UserOnlineFileRepository userOnlineFileRepository, R2dbcEntityOperations entityOperations, FileTrashRecordRepository fileTrashRecordRepository, TransactionalOperator transactionalOperator, RateLimiterConfig rateLimiterConfig, UserFIleShareRecordRepository userFIleShareRecordRepository, ObjectMapper objectMapper, CacheManager cacheManager,
                                 @Nullable FolderListTreeProvider folderListTreeProvider,
                                 @Nullable FileScanProvider fileScanProvider) throws ProcessException {
        super(serverFileMetaRepository,
              userFileMetaRepository,
              userOnlineFileRepository,
              userRepository,
              redisProvider,
              gridFsProvider, fileScanProvider,
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
        Assert.isTrue(fileProperties.getDownload().getFolderDownloadConcurrentLimit() > 0, "資料夾下載併發限制必須大於0");
        this.maxConcurrentLimit = fileProperties.getDownload().getFolderDownloadConcurrentLimit();

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
     * 創建文件夾的實現
     *
     * @param fileEditDTO 文件編輯數據
     * @param user        用戶信息
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> createFolder(FileEditDTO fileEditDTO, User user) {
        return Mono.defer(() -> {
            UserFileMetadata folder = new UserFileMetadata();
            folder.setUserId(user.getId());
            folder.setFilename(fileEditDTO.getFilename());
            folder.setParentFolderId(fileEditDTO.getParentFolderId());
            folder.setLastAccessTime(LocalDateTime.now());
            folder.setUploadTime(LocalDateTime.now());
            folder.setFileType(FileEnum.FOLDER);
            Mono<Void> action = userFileMetaRepository.save(folder).flatMap(newFolder -> {
                if (folderListTreeProvider != null) {
                    try {
                        folderListTreeProvider.addFolder(user.getId(), folder);
                    } catch (ProcessException | ValidationException e) {
                        return Mono.error(e);
                    }
                }
                List<UserFileShareRecord> userFileShareRecords = new ArrayList<>();
                fileEditDTO.getShareUsers().forEach(shareUserEditPO -> {
                    userFileShareRecords.add(new UserFileShareRecord(shareUserEditPO.getUserId(), newFolder.getId()));
                });
                return userFIleShareRecordRepository
                        .saveAll(userFileShareRecords)
                        .then(cleanUserListCache(user.getId(), newFolder.getParentFolderId()));

            });
            return transactionalOperator.transactional(action).then();
        });
    }


    /**
     * 編輯文件夾的實現
     *
     * @param fileEditDTO 文件編輯數據
     * @param user        用戶信息
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> editFolder(FileEditDTO fileEditDTO, User user) {
        Long oldParentFolderId = fileEditDTO.getParentFolderId();
        return Mono.defer(() -> {
            if (fileEditDTO.getParentFolderFileMetadata() != null) {
                return getUserFilePaths(fileEditDTO.getParentFolderFileMetadata(), user).flatMap(nodeList -> {
                    if (nodeList
                            .stream()
                            .filter(node -> Objects.nonNull(node.getFolderId()))
                            .anyMatch(node -> node.getFolderId().toString().equals(fileEditDTO.getFileId()))) {
                        return Mono.error(new ValidationException(ValidationException.ErrorCode.MOVE_TO_CHILD_FOLDER,
                                                                  fileEditDTO.getFileId(),
                                                                  fileEditDTO.getParentFolderId()
                        ));
                    }
                    return Mono.just(fileEditDTO.getUserFileMetadata());
                });
            }
            return Mono.just(fileEditDTO.getUserFileMetadata());
        }).flatMap(userFileMetadata -> {
            if (folderListTreeProvider != null) {
                try {
                    folderListTreeProvider.updateFolder(user.getId(), userFileMetadata, fileEditDTO);
                } catch (ValidationException e) {
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

            Mono<Void> handleChildMono = Mono.empty();
            if (fileEditDTO.getRecursiveSetting()) {
                handleChildMono = Mono.defer(() -> {
                    findAllChildFolder(Collections.singletonList(userFileMetadata.getId()), new ArrayList<>()).flatMap(childFolderList -> {
                        Mono<Void> processShareUserMono = processShareUser(childFolderList, fileEditDTO).then();
                        Mono<Void> settingChildFolder = Mono.defer(() -> {
                            childFolderList.forEach(childFolder -> {
                                childFolder.setShareType(shareType);
                            });
                            return userFileMetaRepository.saveAll(childFolderList).then();
                        });

                        Set<Long> cleanChildCacheFolder = new HashSet<>();
                        childFolderList.forEach(childFolder -> {
                            cleanChildCacheFolder.add(childFolder.getParentFolderId());
                        });

                        return Mono
                                .when(processShareUserMono, settingChildFolder)
                                .then(cleanUserListCache(user.getId(), cleanChildCacheFolder.toArray(new Long[0])));

                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                    return Mono.empty();
                });
            }

            Mono<UserFileMetadata> processShareUserMono = processShareUser(Collections.singletonList(userFileMetadata), fileEditDTO).next();
            return transactionalOperator
                    .transactional(handleChildMono.then(Mono.when(processShareUserMono, userFileMetaRepository.save(userFileMetadata))))
                    .then(Mono.defer(() -> cleanUserListCache(user.getId(), cleanMainCacheFolder.toArray(new Long[0]))));
        });
    }


    /**
     * 刪除文件夾的實現
     *
     * @param folder 文件
     * @param user   用戶信息
     *
     * @return Mono<Void>
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
     * 恢復文件夾的實現
     *
     * @param folder 資料夾
     * @param user   用戶
     *
     * @return Mono<UserFileMetadata> 資料夾
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
     * 批量恢復文件夾的實現
     *
     * @param folders 資料夾
     * @param user    用戶
     *
     * @return Flux<UserFileMetadata> 資料夾
     */
    @Override
    public Flux<UserFileMetadata> restoreFile(Iterable<UserFileMetadata> folders, User user) {
        return Flux.fromIterable(folders).flatMap(folder -> restoreFile(folder, user));
    }


    /**
     * 刪除文件夾的實現
     *
     * @param folder 文件夾
     * @param user   用戶
     *
     * @return Mono<Boolean> 是否刪除成功
     */
    @Override
    public Mono<Boolean> removeFile(UserFileMetadata folder, User user) {
        return findAllChildFolder(Collections.singletonList(folder.getId()), new ArrayList<>(List.of(folder))).flatMap(childFolderList -> {
            boolean isAnyDeleted = childFolderList.stream().anyMatch(UserFileMetadata::getIsDeleted);
            if (isAnyDeleted) {
                return Mono.just(false);
            }
            return Mono.defer(() -> {
                LocalDateTime deleteTime = LocalDateTime.now().plusDays(fileProperties.getBackup().getRetentionTime().toDays());
                FileTrashRecord fileTrashRecord = new FileTrashRecord(childFolderList.getFirst(), deleteTime);
                childFolderList.forEach(userFile -> userFile.setIsDeleted(true));
                Mono<Boolean> result = fileTrashRecordRepository
                        .insert(fileTrashRecord, entityOperations)
                        .thenMany(userFileMetaRepository.saveAll(childFolderList))
                        .collectList()
                        .flatMap(userFileList -> {
                            Long[] parentFolderIds = userFileList.stream().map(UserFileMetadata::getParentFolderId).distinct().toArray(Long[]::new);
                            return cleanUserListCache(userFileList.getFirst().getUserId(), parentFolderIds);
                        })
                        .then(Mono.defer(() -> {
                            if (folderListTreeProvider != null) {
                                folderListTreeProvider.deleteFolder(user.getId(), folder.getId());
                            }
                            return Mono.just(true);
                        }))
                        .onErrorReturn(false);
                return transactionalOperator.transactional(result);
            });
        });
    }


    /**
     * 批量刪除文件夾的實現
     *
     * @param folders 文件夾
     * @param user    用戶
     *
     * @return Mono<Boolean> 是否刪除成功
     */
    @Override
    public Mono<Boolean> removeFile(Iterable<UserFileMetadata> folders, User user) {
        return Flux.fromIterable(folders).flatMap(folder -> removeFile(folder, user)).all(Boolean::booleanValue);
    }


    /**
     * 下載文件夾的實現
     * 此方法會查詢所有子文件夾和文件，並將其壓縮成一個zip文件後返回成 UserFileDataBO對象
     * <p>
     * 對於創建 ZipOutputStream、關閉 ZipOutputStream、刪除臨時檔案可能會回傳 Mono.error
     *
     * @param rootFolder 根文件夾
     * @param user       用戶
     *
     * @return Mono<UserFileDataBO> 文件數據
     */
    @Override
    public Mono<UserFileDataBO> downloadFolder(UserFileMetadata rootFolder, User user) {
        try {
            String zipFileName = getTempZipFilename(rootFolder);
            String tempDownloadPath = downloadFolderPath + zipFileName;
            return Mono
                    .usingWhen(Mono.just(new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tempDownloadPath)))),
                               zos -> processFolder(zos, rootFolder, rootFolder.getFilename()),
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
                                .builder().fileSize(fileSize).filename(zipFileName).fileType(FileEnum.ZIP).dataBufferFlux(dataFlux)
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
     * 處理文件夾的壓縮
     * 根據所查詢到的當前目錄下的檔案進行處理
     * 若當前目錄下有子文件夾，則會遞迴調用此方法
     * 若當前目錄下有文件且該文件存在於 GridFS 中，則會調用 zipFileBatch 方法進行檔案壓縮
     *
     * @param zipOutputStream 壓縮輸出流
     * @param folder          文件夾
     * @param parentPath      父路徑
     *
     * @return Mono<Void>
     */
    private Mono<Void> processFolder(ZipOutputStream zipOutputStream, UserFileMetadata folder, String parentPath) {
        return Mono.defer(() -> findFilesWithSameParentFolderId(Collections.singletonList(folder.getId())).flatMap(files -> {
            List<UserFileMetadata> subFolders = new ArrayList<>();
            List<UserFileMetadata> generalFile = new ArrayList<>();
            List<UserFileMetadata> onlineFile = new ArrayList<>();
            files.forEach(file -> {
                if (file.getFileType() == FileEnum.FOLDER) {
                    subFolders.add(file);
                } else if (file.getFileType() == FileEnum.ONLINE_DOCUMENT) {
                    onlineFile.add(file);
                } else if (file.getServerFileId() != null) {
                    generalFile.add(file);
                }
            });
            Map<FileEnum, List<UserFileMetadata>> filesMap = new HashMap<>();
            filesMap.put(FileEnum.ONLINE_DOCUMENT, onlineFile);
            filesMap.put(FileEnum.OTHER, generalFile);

            Flux<Map.Entry<InputStream, List<UserFileMetadata>>> fileResourceFlux = getGeneralFileResource(filesMap.get(FileEnum.OTHER));
            Flux<Map.Entry<InputStream, List<UserFileMetadata>>> onlineFileFlux = getOnlineFileResource(filesMap.get(FileEnum.ONLINE_DOCUMENT));

            Mono<Void> filesProcessing = Mono.when(handleGeneralFiles(zipOutputStream, fileResourceFlux, parentPath),
                                                   handleGeneralFiles(zipOutputStream, onlineFileFlux, parentPath)
            );

            Mono<Void> foldersProcessing = Flux.fromIterable(subFolders).flatMap(subFolder -> {
                String newPath = parentPath + "/" + subFolder.getFilename();
                return Mono.fromCallable(() -> {
                    synchronized (zipOutputStream) {
                        zipOutputStream.putNextEntry(new ZipEntry(newPath + "/"));
                        zipOutputStream.closeEntry();
                    }
                    return null;
                }).subscribeOn(Schedulers.boundedElastic()).then(processFolder(zipOutputStream, subFolder, newPath));
            }).then();

            return filesProcessing.then(foldersProcessing);
        }));
    }


    /**
     * 獲取文件資源
     * 給定一個文件列表，將返回一個包含 檔案 InputStream 和對應的用戶文件元數據的 Flux
     * 其鍵為 InputStream，值為對應的用戶文件元數據列表
     *
     * @param files 文件列表
     *
     * @return Flux<Map.Entry < InputStream, List < UserFileMetadata>>>
     */
    private Flux<Map.Entry<InputStream, List<UserFileMetadata>>> getGeneralFileResource(List<UserFileMetadata> files) {
        ConcurrentMap<Long, List<UserFileMetadata>> serverIds = files
                .stream()
                .filter(file -> file.getServerFileId() != null)
                .collect(Collectors.groupingByConcurrent(UserFileMetadata::getServerFileId));
        if (serverIds.isEmpty()) {
            return Flux.empty();
        }

        return serverFileMetaRepository.findAllByIdIn(serverIds.keySet()).collectList().flatMapMany(serverFileMetadataList -> {
            Map<ObjectId, ServerFileMetadata> serverFileMetadataMap = serverFileMetadataList
                    .stream()
                    .collect(Collectors.toMap(metadata -> new ObjectId(metadata.getGridFsId()), serverFileMetadata -> serverFileMetadata));

            return gridFsProvider
                    .findFilesById(serverFileMetadataMap.keySet())
                    .flatMapMany(gridFsFilesMap -> Flux
                            .fromIterable(gridFsFilesMap.values())
                            .flatMap(gridFSFile -> gridFsProvider
                                    .getResource(gridFSFile)
                                    .flatMap(ReactiveGridFsResource::getInputStream)
                                    .flatMap(inputStream -> {
                                        List<UserFileMetadata> metadatas = serverIds.get(serverFileMetadataMap.get(gridFSFile.getObjectId()).getId());
                                        return Mono.just(new AbstractMap.SimpleEntry<>(inputStream, metadatas));
                                    })));
        });
    }


    /**
     * 獲取線上文件資源
     * 給定一個文件列表，將返回一個包含 檔案 InputStream 和對應的用戶文件元數據的 Flux
     * 其鍵為 InputStream，值為對應的用戶文件元數據列表
     *
     * @param files 文件列表
     *
     * @return Flux<Map.Entry < InputStream, List < UserFileMetadata>>>
     */
    private Flux<Map.Entry<InputStream, List<UserFileMetadata>>> getOnlineFileResource(List<UserFileMetadata> files) {
        Map<String, UserFileMetadata> userFileMetadataMap = files
                .stream()
                .collect(Collectors.toMap(metadata -> metadata.getId().toString(), userFileMetadata -> userFileMetadata));

        ContentConvertProvider convertProvider = ContentConvertProviderFactory.createProvider(ConvertProviderEnum.DOCX, new ConvertConfig());
        return userOnlineFileRepository
                .findAllById(userFileMetadataMap.keySet())
                .flatMap(userOnlineFile -> convertProvider.convertToInputStream(userOnlineFile.getContent()).flatMap(inputStream -> {
                    UserFileMetadata userFileMetadata = userFileMetadataMap.get(userOnlineFile.getId().toString());
                    String name = userFileMetadata.getFilename().split("\\.")[0] + "." + ConvertProviderEnum.DOCX.getSuffix();
                    userFileMetadata.setFilename(name);
                    return Mono.just(new AbstractMap.SimpleEntry<>(inputStream, Collections.singletonList(userFileMetadata)));
                }));
    }


    /**
     * 批量壓縮文件
     * 對於給定的文件列表，將其壓縮到指定的 ZipOutputStream 中
     * 這個方法會遍歷所有的文件資源，並將其寫入到 ZipOutputStream 中
     * 如果文件資源的輸入流無法獲取或是寫入過程中發生錯誤，則會拋出異常
     *
     * @param zipOutputStream 壓縮輸出流
     * @param flux            文件資源的 Flux
     * @param parentPath      父路徑
     *
     * @return Mono<Void>
     */
    private Mono<Void> handleGeneralFiles(ZipOutputStream zipOutputStream, Flux<Map.Entry<InputStream, List<UserFileMetadata>>> flux, String parentPath) {
        return flux.flatMap(entry -> {
            InputStream inputStream = entry.getKey();
            List<UserFileMetadata> userFileMetadatas = entry.getValue();

            return Flux.fromIterable(userFileMetadatas).flatMap(file -> {
                String filePath = parentPath + "/" + file.getFilename();
                return writeIntoZip(inputStream, zipOutputStream, filePath);
            }, maxConcurrentLimit);
        }).then();
    }


    /**
     * 將文件寫入到 ZipOutputStream 中
     * 此方法會將給定的 InputStream 寫入到 ZipOutputStream 中
     * 如果寫入過程中發生錯誤，則會拋出異常
     *
     * @param inputStream     輸入流
     * @param zipOutputStream 壓縮輸出流
     * @param filePath        文件路徑
     *
     * @return Mono<Void>
     */
    private Mono<Void> writeIntoZip(InputStream inputStream, ZipOutputStream zipOutputStream, String filePath) {
        return Mono.usingWhen(Mono.just(inputStream), resourceInputStream -> Mono.fromCallable(() -> {
            try {
                synchronized (zipOutputStream) {
                    zipOutputStream.putNextEntry(new ZipEntry(filePath));
                    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                    ReadableByteChannel channel = Channels.newChannel(resourceInputStream);
                    WritableByteChannel outputChannel = Channels.newChannel(zipOutputStream);

                    while (channel.read(buffer) != -1) {
                        buffer.flip();
                        outputChannel.write(buffer);
                        buffer.clear();
                    }

                    zipOutputStream.closeEntry();
                }
                return true;
            } catch (IOException e) {
                throw Exceptions.propagate(e);
            }
        }).then().subscribeOn(Schedulers.boundedElastic()), stream -> Mono.fromRunnable(() -> {
            try {
                stream.close();
            } catch (IOException e) {
                throw Exceptions.propagate(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()));
    }


    /**
     * 找尋指定文件夾的所有子文件夾
     *
     * @param parentFolderIdList 父文件夾ID列表
     * @param childFolderList    子文件夾列表
     *
     * @return Mono<Void>
     */
    private Mono<List<UserFileMetadata>> findAllChildFolder(List<Long> parentFolderIdList, List<UserFileMetadata> childFolderList) {
        return findFilesWithSameParentFolderId(parentFolderIdList).flatMap(subFile -> {
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
     * 查詢具有相同父文件夾ID的文件夾
     *
     * @param parentFolderId 父文件夾ID
     *
     * @return Mono<List < Long>>
     */
    private Mono<List<UserFileMetadata>> findFilesWithSameParentFolderId(List<Long> parentFolderId) {
        return userFileMetaRepository.findAllByParentFolderIdIn(parentFolderId, entityOperations)
                .collectList()
                .switchIfEmpty(Mono.just(Collections.emptyList()));
    }


    /**
     * 獲取臨時壓縮文件名
     *
     * @param folder 文件夾
     *
     * @return String
     */
    @SkipRecord
    private String getTempZipFilename(UserFileMetadata folder) {
        return folder.getId() + "_" + folder.getFilename() + ".zip";
    }
}

