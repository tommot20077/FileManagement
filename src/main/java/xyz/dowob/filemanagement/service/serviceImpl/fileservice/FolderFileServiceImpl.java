package xyz.dowob.filemanagement.service.serviceImpl.fileservice;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.ServerFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserOnlineFileRepository;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService;
import xyz.dowob.filemanagement.service.serviceInterface.FolderService;

import java.time.LocalDateTime;
import java.util.*;
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
    public FolderFileServiceImpl(ServerFileMetaRepository serverFileMetaRepository, UserFileMetaRepository userFileMetaRepository, UserOnlineFileRepository userOnlineFileRepository, UserRepository userRepository, RedisProvider redisProvider, GridFsProvider gridFsProvider, TransfersTasksManager transfersTasksManager, FileProperties fileProperties, DatabaseClient databaseClient, CircuitBreakerConfig circuitBreakerConfig, FolderListTreeProvider folderListTreeProvider) {
        super(serverFileMetaRepository,
              userFileMetaRepository,
              userOnlineFileRepository,
              userRepository,
              redisProvider,
              gridFsProvider,
              transfersTasksManager,
              fileProperties,
              databaseClient,
              circuitBreakerConfig,
              folderListTreeProvider
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
            if (fileEditDTO.getParentFolderId() != null) {
                return checkParentFolderId(fileEditDTO.getParentFolderId(), user);
            }
            return Mono.empty();
        }).then(Mono.defer(() -> {
            UserFileMetadata folder = new UserFileMetadata();
            folder.setUserId(user.getId());
            folder.setFilename(fileEditDTO.getFileName());
            folder.setIsFolder(true);
            folder.setParentFolderId(fileEditDTO.getParentFolderId());
            folder.setLastAccessTime(LocalDateTime.now());
            folder.setUploadTime(LocalDateTime.now());
            folder.setFileType(FileEnum.FOLDER);

            Set<Long> shareUserIds = fileEditDTO.getShareUserIds() == null ? new HashSet<>() : fileEditDTO.getShareUserIds();
            folder.setSharedWithUsers(shareUserIds);

            return userFileMetaRepository.save(folder).flatMap(newFolder -> {
                if (folderListTreeProvider != null) {
                    folderListTreeProvider.addFolder(user.getId(), new UserFileListDTO(newFolder));
                }
                return cleanUserListCache(user.getId(), newFolder.getParentFolderId());
            });
        }));
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
        return userFileMetaRepository
                .findById(fileEditDTO.getFileId())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE,
                                                                  fileEditDTO.getFileId()
                )))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata).then(isFileOrFolder(userFileMetadata, true)))
                .flatMap(userFileMetadata -> {
                    if (fileEditDTO.getParentFolderId() != null) {
                        return checkParentFolderId(fileEditDTO.getParentFolderId(),
                                                   user
                        ).then(Mono.defer(() -> getUserFilePaths(fileEditDTO.getParentFolderId(), user).flatMap(list -> {
                            if (list
                                    .stream()
                                    .filter(node -> Objects.nonNull(node.getFolderId()))
                                    .anyMatch(node -> node.getFolderId().equals(userFileMetadata.getId()))) {
                                return Mono.error(new ValidationException(ValidationException.ErrorCode.MOVE_TO_CHILD_FOLDER,
                                                                          fileEditDTO.getFileId(),
                                                                          fileEditDTO.getParentFolderId()
                                ));
                            }
                            return Mono.just(userFileMetadata);
                        })));
                    }
                    return Mono.just(userFileMetadata);
                })
                .flatMap(userFileMetadata -> redisProvider
                        .deleteZset(getUserFileListBaseKey(user.getId(), userFileMetadata.getParentFolderId()))
                        .then(Mono.just(userFileMetadata)))
                .flatMap(userFileMetadata -> {
                    userFileMetadata.setFilename(fileEditDTO.getFileName());
                    userFileMetadata.setParentFolderId(fileEditDTO.getParentFolderId());
                    userFileMetadata.setLastAccessTime(LocalDateTime.now());

                    Set<Long> sharedWithUsers = Objects.requireNonNullElse(fileEditDTO.getShareUserIds(),
                                                                           userFileMetadata.getSharedWithUsers()
                    );
                    userFileMetadata.setSharedWithUsers(sharedWithUsers);

                    if (folderListTreeProvider != null) {
                        folderListTreeProvider.updateFolder(user.getId(), userFileMetadata, fileEditDTO.getParentFolderId());
                    }
                    return userFileMetaRepository
                            .save(userFileMetadata)
                            .flatMap(newUserFileMetadata -> cleanUserListCache(user.getId(), newUserFileMetadata.getParentFolderId()
                            ));
                });
    }

    /**
     * 刪除文件夾的共通實現
     *
     * @param fileId 文件ID
     * @param user   用戶信息
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> deleteFolder(String fileId, User user) {
        return userFileMetaRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata).then(isFileOrFolder(userFileMetadata, true)))
                .flatMap(userFileMetadata -> {
                    if (folderListTreeProvider != null) {
                        folderListTreeProvider.deleteFolder(user.getId(), userFileMetadata);
                    }
                    List<UserFileMetadata> userFileList = new ArrayList<>();
                    List<Long> parentFolderIdList = new ArrayList<>(List.of(userFileMetadata.getId()));
                    Mono<Void> res = deleteFolderRecursive(user, parentFolderIdList, userFileList);
                    return res
                            .then(cleanUserListCache(user.getId(), userFileMetadata.getParentFolderId()))
                            .then(updateOwner(userFileList, user))
                            .then(userFileMetaRepository.delete(userFileMetadata));
                });
    }

    /**
     * 遞歸刪除文件夾的共通實現
     *
     * @param user               用戶信息
     * @param parentFolderIdList 父文件夾ID列表
     * @param userFileList       服務器文件列表
     *
     * @return Mono<Void>
     */
    private Mono<Void> deleteFolderRecursive(User user, List<Long> parentFolderIdList, List<UserFileMetadata> userFileList) {
        return findFoldersWithSameParentFolderId(user.getId(), parentFolderIdList, userFileList).flatMap(nextParentFolderIds -> {
            if (nextParentFolderIds.isEmpty()) {
                List<Long> serverFileIds = userFileList.stream().map(UserFileMetadata::getServerFileId).filter(Objects::nonNull).toList();
                return handleUserStorage(user, serverFileIds);
            }
            return deleteFolderRecursive(user, nextParentFolderIds, userFileList);
        });
    }

    /**
     * 查詢具有相同父文件夾ID的文件夾
     *
     * @param userId         用戶ID
     * @param parentFolderId 父文件夾ID
     * @param userFileList   服務器文件列表
     *
     * @return Mono<List < Long>>
     */
    private Mono<List<Long>> findFoldersWithSameParentFolderId(Long userId, List<Long> parentFolderId, List<UserFileMetadata> userFileList) {
        return userFileMetaRepository
                .findAllByUserIdAndParentFolderIdInOrderByIsFolder(userId, parentFolderId)
                .collectList()
                .map(userFileMetadataList -> {
                    userFileList.addAll(userFileMetadataList);
                    return userFileMetadataList
                            .stream()
                            .filter(UserFileMetadata::getIsFolder)
                            .map(UserFileMetadata::getId)
                            .collect(Collectors.toList());
                })
                .switchIfEmpty(Mono.just(Collections.emptyList()));
    }

}
