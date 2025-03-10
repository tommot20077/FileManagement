package xyz.dowob.filemanagement.service.serviceImpl.fileservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.annotation.Nullable;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.FileTrashRecord;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileShareRecord;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.*;
import xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService;
import xyz.dowob.filemanagement.service.serviceInterface.FolderService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
@FileHandlerType(FileEnum.FOLDER)
public class FolderFileServiceImpl extends AbstractFileService implements FolderService {
    public FolderFileServiceImpl(ServerFileMetaRepository serverFileMetaRepository, UserFileMetaRepository userFileMetaRepository, RedisProvider redisProvider, GridFsProvider gridFsProvider, TransfersTasksManager transfersTasksManager, FileProperties fileProperties, CircuitBreakerConfig circuitBreakerConfig, UserRepository userRepository, UserOnlineFileRepository userOnlineFileRepository, R2dbcEntityOperations entityOperations, FileTrashRecordRepository fileTrashRecordRepository, TransactionalOperator transactionalOperator, RateLimiterConfig rateLimiterConfig, UserFIleShareRecordRepository userFIleShareRecordRepository, ObjectMapper objectMapper,
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
              folderListTreeProvider, fileTrashRecordRepository, entityOperations, transactionalOperator, userFIleShareRecordRepository, objectMapper
        );
    }
    //todo 後期加入下載資料夾的功能

    /**
     * 創建文件夾的共通實現
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

            return userFileMetaRepository.save(folder).flatMap(newFolder -> {
                List<UserFileShareRecord> userFileShareRecords = new ArrayList<>();
                fileEditDTO.getShareUsers().forEach(shareUserEditPO -> {
                    userFileShareRecords.add(new UserFileShareRecord(shareUserEditPO.getUserId(), newFolder.getId()));
                });

                if (folderListTreeProvider != null) {
                    folderListTreeProvider.addFolder(user.getId(), newFolder);
                }
                return userFIleShareRecordRepository
                        .saveAll(userFileShareRecords)
                        .then(cleanUserListCache(user.getId(), newFolder.getParentFolderId()));
            });
        });
    }

    /**
     * 編輯文件夾的共通實現
     *
     * @param fileEditDTO 文件編輯數據
     * @param user        用戶信息
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> editFolder(FileEditDTO fileEditDTO, User user) {
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
                })
                .flatMap(userFileMetadata -> redisProvider
                        .deleteList(getUserFileListBaseKey(user.getId(), userFileMetadata.getParentFolderId()))
                        .then(Mono.just(userFileMetadata)))
                .flatMap(userFileMetadata -> {
                    userFileMetadata.setFilename(fileEditDTO.getFilename());
                    userFileMetadata.setParentFolderId(fileEditDTO.getParentFolderId());
                    userFileMetadata.setLastAccessTime(LocalDateTime.now());

                    Boolean isStar = Objects.requireNonNullElse(fileEditDTO.getIsStar(), userFileMetadata.getIsStar());
                    userFileMetadata.setIsStar(isStar);

                    if (folderListTreeProvider != null) {
                        folderListTreeProvider.updateFolder(user.getId(), userFileMetadata, fileEditDTO.getParentFolderId());
                    }

                    Mono<UserFileMetadata> processShareUserMono = processShareUser(userFileMetadata, fileEditDTO);

                    return Mono
                            .zip(processShareUserMono, userFileMetaRepository.save(userFileMetadata))
                            .flatMap(tuple2 -> cleanUserListCache(user.getId(), tuple2.getT2().getParentFolderId()));
                });
    }

    /**
     * 刪除文件夾的共通實現
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
                        return handleUserStorage(user, serverFileIds);
                    })
                    .then(cleanUserListCache(user.getId(), userFileMetadata.getParentFolderId()))
                    .then(updateOwner(userFileList, user.getId()))
                    .then(userFileMetaRepository.delete(userFileMetadata));
        });
    }

    /**
     * 恢復文件夾的共通實現
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
                                            folderListTreeProvider.addFolders(user.getId(), childFolderList);
                                        }
                                        return Mono.just(folder);
                                    }));
                        })));
    }

    /**
     * 批量恢復文件夾的共通實現
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
     * 刪除文件夾的共通實現
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
                LocalDateTime deleteTime = LocalDateTime.now().plusDays(fileProperties.getGlobal().getRetentionTime());
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
     * 批量刪除文件夾的共通實現
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
     * 遞歸刪除文件夾的共通實現
     *
     * @param parentFolderIdList 父文件夾ID列表
     * @param childFolderList    服務器文件列表
     *
     * @return Mono<Void>
     */
    private Mono<List<UserFileMetadata>> findAllChildFolder(List<Long> parentFolderIdList, List<UserFileMetadata> childFolderList) {
        return findFoldersWithSameParentFolderId(parentFolderIdList).flatMap(nextChildFolder -> {
            childFolderList.addAll(nextChildFolder);
            if (nextChildFolder.isEmpty()) {
                return childFolderList.isEmpty() ? Mono.empty() : Mono.just(childFolderList);
            }
            return findAllChildFolder(nextChildFolder.stream().map(UserFileMetadata::getId).toList(), childFolderList);
        });
    }

    /**
     * 查詢具有相同父文件夾ID的文件夾
     *
     * @param parentFolderId 父文件夾ID
     *
     * @return Mono<List < Long>>
     */
    private Mono<List<UserFileMetadata>> findFoldersWithSameParentFolderId(List<Long> parentFolderId) {
        return userFileMetaRepository.findAllByParentFolderIdIn(parentFolderId, entityOperations)
                .collectList()
                .map(userFileMetadataList -> userFileMetadataList
                        .stream()
                        .filter(fileMeta -> fileMeta.getFileType() == FileEnum.FOLDER)
                        .collect(Collectors.toList()))
                .switchIfEmpty(Mono.just(Collections.emptyList()));
    }
}
