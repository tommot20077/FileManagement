package xyz.dowob.filemanagement.service.serviceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.repostiory.UserFIleShareRecordRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilePermissionServiceImpl 邏輯處理測試")
class FilePermissionServiceImplTest {

    @Mock
    private UserFileMetaRepository mockUserFileMetaRepository;

    @Mock
    private UserFIleShareRecordRepository mockShareRecordRepository;

    @Mock
    private R2dbcEntityOperations mockR2dbcEntityOperations;

    @InjectMocks
    private FilePermissionServiceImpl filePermissionServiceImplUnderTest;

    private Permission<UserFileMetadata> allowOwner;
    private Permission<UserFileMetadata> blockDeleted;
    private Permission<UserFileMetadata> blockNotSearchOperation;
    private Permission<UserFileMetadata> allowShared;

    private User ownerUser;
    private User nonOwnerUser;
    private UserFileMetadata fileMetadata;

    @BeforeEach
    void setUp() {

        allowOwner = (user, file) -> {
            if (file.getUserId().equals(user.getId())) {
                return Mono.empty();
            }
            return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
        };

        blockDeleted = (user, file) -> {
            if (file.getIsDeleted()) {
                return Mono.just(new ValidationException(ValidationException.ErrorCode.ALREADY_DELETED_FILE, file.getId()));
            }
            return Mono.empty();
        };

        blockNotSearchOperation = (user, file) -> {
            if (ReservedSearchIdEnum.format(file.getId()) == null) {
                return Mono.empty();
            }
            return Mono.just(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, file.getId()));
        };

        allowShared = (user, file) -> {
            if (file.getShareType() == FileShareTypeEnum.PUBLIC || file.getUserId().equals(user.getId())) {
                return Mono.empty();
            } else if (file.getShareType() == FileShareTypeEnum.NONE) {
                return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
            } else if (file.getShareType() == FileShareTypeEnum.PRIVATE) {
                return mockShareRecordRepository.existsByUserIdAndFileId(user.getId(), file.getId()).flatMap(hasRecord -> {
                    if (hasRecord) {
                        return Mono.empty();
                    }
                    return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
                });
            }

            return mockShareRecordRepository.existsByUserIdAndFileId(user.getId(), file.getId()).flatMap(hasRecord -> {
                if (hasRecord) {
                    return Mono.empty();
                }

                Mono<Boolean> existParentRecordMono;
                Mono<Optional<FileShareTypeEnum>> parentShareTypeOptionalMono;

                if (file.getParentFolderId() != null) {
                    existParentRecordMono = mockShareRecordRepository.existsByUserIdAndFileId(user.getId(), file.getParentFolderId());
                    parentShareTypeOptionalMono = mockUserFileMetaRepository
                            .getShareTypeByFileId(file.getParentFolderId(), mockR2dbcEntityOperations)
                            .map(Optional::of)
                            .switchIfEmpty(Mono.just(Optional.empty()));
                } else {
                    existParentRecordMono = Mono.just(false);
                    parentShareTypeOptionalMono = Mono.just(Optional.empty());
                }

                return Mono.zip(existParentRecordMono, parentShareTypeOptionalMono).flatMap(tuple -> {
                    Boolean parentExistRecord = tuple.getT1();
                    Optional<FileShareTypeEnum> parentShareTypeOptional = tuple.getT2();
                    if (parentShareTypeOptional.isPresent()) {
                        if (parentShareTypeOptional.get() == FileShareTypeEnum.PUBLIC || parentExistRecord && parentShareTypeOptional.get() != FileShareTypeEnum.NONE) {
                            return Mono.empty();
                        }
                    }
                    return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
                });
            });
        };

        ownerUser = new User();
        ownerUser.setId(1L);
        ownerUser.setRole(RoleEnum.ADMIN);

        nonOwnerUser = new User();
        nonOwnerUser.setId(2L);
        nonOwnerUser.setRole(RoleEnum.USER);

        fileMetadata = new UserFileMetadata();
        fileMetadata.setId(1L);
        fileMetadata.setUserId(1L);
        fileMetadata.setFileType(FileEnum.IMAGE);
        fileMetadata.setIsDeleted(false);
        fileMetadata.setServerFileId(1L);
        fileMetadata.setFilename("test.jpg");
        fileMetadata.setParentFolderId(null);
    }

    @Test
    @DisplayName("使用者擁有檔案權限，符合預設規則 - 驗證成功 - 預期驗證成功")
    void testNormalFileId_UserHasPermission_DefaultRules_Passes() {
        Long fileId = fileMetadata.getId();
        when(mockUserFileMetaRepository.findById(fileId.toString())).thenReturn(Mono.just(fileMetadata));
        List<Permission<UserFileMetadata>> defaultRules = List.of(allowOwner, blockDeleted, blockNotSearchOperation);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileId, defaultRules))
                .expectNextMatches(file -> file.getId().equals(fileId))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileId.toString());
    }

    @Test
    @DisplayName("使用者無檔案權限，符合預設規則 - 拋出 FILE_PERMISSION_DENIED - 預期拋出 FILE_PERMISSION_DENIED")
    void testNormalFileId_UserLacksPermission_DefaultRules_ThrowsPermissionDenied() {
        Long fileId = fileMetadata.getId();
        when(mockUserFileMetaRepository.findById(fileId.toString())).thenReturn(Mono.just(fileMetadata));
        List<Permission<UserFileMetadata>> defaultRules = List.of(allowOwner, blockDeleted, blockNotSearchOperation);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileId, defaultRules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileId.toString());
    }

    @Test
    @DisplayName("檔案已被刪除，使用者擁有權限，符合預設規則 - 拋出 ALREADY_DELETED_FILE - 預期拋出 ALREADY_DELETED_FILE")
    void testNormalFileId_UserLacksPermission_DefaultRules_ThrowsAlreadyDelete() {
        Long fileId = fileMetadata.getId();
        fileMetadata.setIsDeleted(true);
        when(mockUserFileMetaRepository.findById(fileId.toString())).thenReturn(Mono.just(fileMetadata));
        List<Permission<UserFileMetadata>> defaultRules = List.of(allowOwner, blockDeleted, blockNotSearchOperation);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileId, defaultRules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.ALREADY_DELETED_FILE)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileId.toString());
    }

    @Test
    @DisplayName("查無檔案紀錄 - 拋出 NOT_EXISTING_USER_FILE")
    void testNormalFileId_FileDoesNotExist_ThrowsNotExistingUserFile() {
        Long fileId = 999L;
        when(mockUserFileMetaRepository.findById(fileId.toString())).thenReturn(Mono.empty());
        List<Permission<UserFileMetadata>> defaultRules = List.of(allowOwner, blockDeleted, blockNotSearchOperation);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileId, defaultRules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NOT_EXISTING_USER_FILE)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileId.toString());
    }

    @Test
    @DisplayName("非保留ID且查無檔案紀錄 - 拋出 NOT_EXISTING_USER_FILE")
    void testReservedId_NonReservedIdNotInQuery_ThrowsNotExistingUserFile() {
        Long nonReservedId = -999L;
        when(mockUserFileMetaRepository.findById(nonReservedId.toString())).thenReturn(Mono.empty());
        List<Permission<UserFileMetadata>> defaultRules = List.of(allowOwner, blockDeleted, blockNotSearchOperation);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, nonReservedId, defaultRules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NOT_EXISTING_USER_FILE)
                .verify();

        verify(mockUserFileMetaRepository).findById(nonReservedId.toString());
    }

    @Test
    @DisplayName("保留ID查無檔案紀錄 - 拋出 NOT_EXISTING_USER_FILE")
    void testReservedId_DefaultRules_ThrowsNotExistingUserFile() {
        Long reservedFileId = ReservedSearchIdEnum.ALL_FILE_ID.getId();
        when(mockUserFileMetaRepository.findById(reservedFileId.toString())).thenReturn(Mono.empty());
        List<Permission<UserFileMetadata>> defaultRules = List.of(allowOwner, blockDeleted, blockNotSearchOperation);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, reservedFileId, defaultRules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NOT_EXISTING_USER_FILE)
                .verify();

        verify(mockUserFileMetaRepository).findById(reservedFileId.toString());
    }

    @Test
    @DisplayName("保留ID為回收筒，無搜尋限制 - 拋出 ALREADY_DELETED_FILE")
    void testReservedId_RecycleFileId_NoSearchRestriction_ThrowsAlreadyDeletedFile() {
        Long reservedFileId = ReservedSearchIdEnum.RECYCLE_FILE_ID.getId();
        when(mockUserFileMetaRepository.findById(reservedFileId.toString())).thenReturn(Mono.empty());
        List<Permission<UserFileMetadata>> rules = List.of(allowOwner, blockDeleted);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, reservedFileId, rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.ALREADY_DELETED_FILE)
                .verify();

        verify(mockUserFileMetaRepository).findById(reservedFileId.toString());
    }

    @Test
    @DisplayName("保留ID為非回收筒，無搜尋限制 - 回傳虛擬檔案資料")
    void testReservedId_NonRecycleFileId_NoSearchRestriction_Passes() {
        Long reservedFileId = ReservedSearchIdEnum.ALL_FILE_ID.getId();
        when(mockUserFileMetaRepository.findById(reservedFileId.toString())).thenReturn(Mono.empty());
        List<Permission<UserFileMetadata>> rules = List.of(allowOwner, blockDeleted);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, reservedFileId, rules))
                .expectNextMatches(file -> file.getId().equals(reservedFileId) && file
                        .getUserId()
                        .equals(ownerUser.getId()) && !file.getIsDeleted() && file.getFileType() == FileEnum.FOLDER)
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(reservedFileId.toString());
    }

    @Test
    @DisplayName("保留ID為回收筒，無搜尋與刪除限制 - 回傳虛擬回收筒資料")
    void testReservedId_RecycleFileId_NoSearchAndNoDeleteRestriction_Passes() {
        Long reservedFileId = ReservedSearchIdEnum.RECYCLE_FILE_ID.getId();
        when(mockUserFileMetaRepository.findById(reservedFileId.toString())).thenReturn(Mono.empty());
        List<Permission<UserFileMetadata>> rules = List.of(allowOwner);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, reservedFileId, rules))
                .expectNextMatches(file -> file.getId().equals(reservedFileId) && file
                        .getUserId()
                        .equals(ownerUser.getId()) && file.getIsDeleted() && file.getFileType() == FileEnum.FOLDER)
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(reservedFileId.toString());
    }


    @Test
    @DisplayName("保留ID為非回收筒，無搜尋與刪除限制 - 回傳虛擬檔案資料")
    void testReservedId_NonRecycleFileId_NoSearchAndNoDeleteRestriction_Passes() {
        Long reservedFileId = ReservedSearchIdEnum.ROOT_FOLDER_ID.getId();
        when(mockUserFileMetaRepository.findById(reservedFileId.toString())).thenReturn(Mono.empty());
        List<Permission<UserFileMetadata>> rules = List.of(allowOwner);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, reservedFileId, rules))
                .expectNextMatches(file -> file.getId().equals(reservedFileId) && file
                        .getUserId()
                        .equals(ownerUser.getId()) && !file.getIsDeleted() && file.getFileType() == FileEnum.FOLDER)
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(reservedFileId.toString());
    }

    @Test
    @DisplayName("檔案公開分享，非擁有者可存取 - 驗證成功")
    void testAllowShared_PublicShareType_NonOwner_Passes() {
        fileMetadata.setShareType(FileShareTypeEnum.PUBLIC);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
    }

    @Test
    @DisplayName("檔案無分享設定，非擁有者存取 - 拋出 FILE_PERMISSION_DENIED")
    void testAllowShared_NoneShareType_NonOwner_ThrowsPermissionDenied() {
        fileMetadata.setShareType(FileShareTypeEnum.NONE);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
    }

    @Test
    @DisplayName("檔案私人分享，且有分享紀錄，非擁有者可存取 - 驗證成功")
    void testAllowShared_PrivateShareType_WithShareRecord_NonOwner_Passes() {
        fileMetadata.setShareType(FileShareTypeEnum.PRIVATE);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(true));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
    }

    @Test
    @DisplayName("檔案私人分享，無分享紀錄，非擁有者存取 - 拋出 FILE_PERMISSION_DENIED")
    void testAllowShared_PrivateShareType_NoShareRecord_NonOwner_ThrowsPermissionDenied() {
        fileMetadata.setShareType(FileShareTypeEnum.PRIVATE);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
    }

    @Test
    @DisplayName("預設分享設定，父層為公開且有分享紀錄，非擁有者可存取 - 驗證成功")
    void testAllowShared_DefaultShareType_ParentPublic_WithParentRecord_NonOwner_Passes() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        fileMetadata.setParentFolderId(100L);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getParentFolderId())).thenReturn(Mono.just(true));
        when(mockUserFileMetaRepository.getShareTypeByFileId(fileMetadata.getParentFolderId(), mockR2dbcEntityOperations)).thenReturn(Mono.just(
                FileShareTypeEnum.PUBLIC));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getParentFolderId());
        verify(mockUserFileMetaRepository).getShareTypeByFileId(fileMetadata.getParentFolderId(), mockR2dbcEntityOperations);
    }

    @Test
    @DisplayName("預設分享設定，父層為私人且無分享紀錄，非擁有者存取 - 拋出 FILE_PERMISSION_DENIED")
    void testAllowShared_DefaultShareType_ParentRootPrivate_WithParentRecord_NonOwner_Passes() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
    }


    @Test
    @DisplayName("DEFAULT分享類型 + 有父資料夾(Private) + 有父層分享紀錄 + 非擁有者 - 通過")
    void testAllowShared_DefaultShareType_ParentFolderPrivate_WithParentRecord_NonOwner_Passes() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        fileMetadata.setParentFolderId(100L);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getParentFolderId())).thenReturn(Mono.just(true));
        when(mockUserFileMetaRepository.getShareTypeByFileId(fileMetadata.getParentFolderId(), mockR2dbcEntityOperations)).thenReturn(Mono.just(
                FileShareTypeEnum.PRIVATE));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getParentFolderId());
        verify(mockUserFileMetaRepository).getShareTypeByFileId(fileMetadata.getParentFolderId(), mockR2dbcEntityOperations);
    }

    @Test
    @DisplayName("DEFAULT分享類型 + 無父資料夾 + 有父層分享紀錄 + 非擁有者 - 拒絕")
    void testAllowShared_DefaultShareType_ParentNone_WithParentRecord_NonOwner_ThrowsPermissionDenied() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
    }

    @Test
    @DisplayName("DEFAULT分享類型 + 有父資料夾(Default) + 有父層分享紀錄 + 非擁有者 - 通過")
    void testAllowShared_DefaultShareType_ParentFolderDefault_WithParentRecord_NonOwner_Passes() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        fileMetadata.setParentFolderId(100L);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getParentFolderId())).thenReturn(Mono.just(true));
        when(mockUserFileMetaRepository.getShareTypeByFileId(fileMetadata.getParentFolderId(), mockR2dbcEntityOperations)).thenReturn(Mono.just(
                FileShareTypeEnum.DEFAULT));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getParentFolderId());
        verify(mockUserFileMetaRepository).getShareTypeByFileId(fileMetadata.getParentFolderId(), mockR2dbcEntityOperations);
    }

    @Test
    @DisplayName("DEFAULT分享類型 + 父資料夾為空 + 有父層分享紀錄 + 非擁有者 - 拒絕")
    void testAllowShared_DefaultShareType_ParentEmpty_WithParentRecord_NonOwner_ThrowsPermissionDenied() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
    }

    @Test
    @DisplayName("DEFAULT分享類型 + 有父資料夾(Public) + 無父層分享紀錄 + 非擁有者 - 通過")
    void testAllowShared_DefaultShareType_ParentFolderPublic_NoParentRecord_NonOwner_Passes() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        fileMetadata.setParentFolderId(100L);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getParentFolderId())).thenReturn(Mono.just(false));
        when(mockUserFileMetaRepository.getShareTypeByFileId(fileMetadata.getParentFolderId(), mockR2dbcEntityOperations)).thenReturn(Mono.just(
                FileShareTypeEnum.PUBLIC));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
    }

    @Test
    @DisplayName("DEFAULT分享類型 + 父資料夾為Private + 無父層分享紀錄 + 非擁有者 - 拒絕")
    void testAllowShared_DefaultShareType_ParentPrivate_NoParentRecord_NonOwner_ThrowsPermissionDenied() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
    }

    @Test
    @DisplayName("DEFAULT分享類型 + 無父資料夾 + 無父層分享紀錄 + 非擁有者 - 拒絕")
    void testAllowShared_DefaultShareType_ParentNone_NoParentRecord_NonOwner_ThrowsPermissionDenied() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
    }

    @Test
    @DisplayName("DEFAULT分享類型 + 父資料夾為Default + 無父層分享紀錄 + 非擁有者 - 拒絕")
    void testAllowShared_DefaultShareType_ParentDefault_NoParentRecord_NonOwner_ThrowsPermissionDenied() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
    }

    @Test
    @DisplayName("DEFAULT分享類型 + 父資料夾為空 + 無父層分享紀錄 + 非擁有者 -> 拒絕")
    void testAllowShared_DefaultShareType_ParentEmpty_NoParentRecord_NonOwner_ThrowsPermissionDenied() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(false));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
    }

    @Test
    @DisplayName("DEFAULT分享類型 + 檔案本身有分享紀錄 + 非擁有者 -> 通過")
    void testAllowShared_DefaultShareType_WithFileShareRecord_NonOwner_Passes() {
        fileMetadata.setShareType(FileShareTypeEnum.DEFAULT);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockShareRecordRepository.existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId())).thenReturn(Mono.just(true));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockShareRecordRepository).existsByUserIdAndFileId(nonOwnerUser.getId(), fileMetadata.getId());
    }

    @Test
    @DisplayName("PUBLIC分享類型 + 檔案已刪除 + 非擁有者 -> 已刪除例外")
    void testAllowShared_DefaultShareType_WithPublicAndDeleted_NonOwner_Passes() {
        fileMetadata.setShareType(FileShareTypeEnum.PUBLIC);
        fileMetadata.setIsDeleted(true);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        List<Permission<UserFileMetadata>> rules = List.of(allowShared, blockDeleted);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.ALREADY_DELETED_FILE)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
    }


    @Test
    @DisplayName("無自訂規則 (null) -> 使用預設規則(擁有者規則) 通過")
    void testCheckPermissions_NullRules_UsesDefaultOnlyOwnerRules() {
        List<Permission<UserFileMetadata>> defaultRules = List.of(allowOwner, blockDeleted, blockNotSearchOperation);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileMetadata.getId(), defaultRules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
    }

    @Test
    @DisplayName("空規則列表 -> 使用預設規則(擁有者規則) 通過")
    void testCheckPermissions_EmptyRules_UsesDefaultOnlyOwnerRules() {
        List<Permission<UserFileMetadata>> defaultRules = List.of(allowOwner, blockDeleted, blockNotSearchOperation);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileMetadata.getId(), defaultRules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
    }

    @Test
    @DisplayName("自訂規則全部通過 -> 通過")
    void testCheckPermissions_CustomRules_AllPass_ReturnsMetadata() {
        List<Permission<UserFileMetadata>> customRules = List.of(allowOwner);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileMetadata.getId(), customRules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
    }

    @Test
    @DisplayName("自訂規則有一條失敗 -> 權限拒絕例外")
    void testCheckPermissions_CustomRules_OneFails_ThrowsException() {
        List<Permission<UserFileMetadata>> customRules = List.of(allowOwner);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(nonOwnerUser, fileMetadata.getId(), customRules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
    }

    @Test
    @DisplayName("保留ID: ALL_FILE -> 回傳虛擬資料夾 (未刪除)")
    void testReservedSearchMethod_ReservedIdAllFile_ReturnsVirtualMetadata() {
        Long reservedId = ReservedSearchIdEnum.ALL_FILE_ID.getId();
        when(mockUserFileMetaRepository.findById(reservedId.toString())).thenReturn(Mono.empty());

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, reservedId, List.of(allowOwner)))
                .expectNextMatches(file -> file.getId().equals(reservedId) && file
                        .getUserId()
                        .equals(ownerUser.getId()) && !file.getIsDeleted() && file.getFileType() == FileEnum.FOLDER)
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(reservedId.toString());
    }

    @Test
    @DisplayName("保留ID: RECYCLE_FILE -> 回傳虛擬資料夾 (已刪除)")
    void testReservedSearchMethod_ReservedIdRecycleFile_ReturnsVirtualMetadataWithDeleted() {
        Long reservedId = ReservedSearchIdEnum.RECYCLE_FILE_ID.getId();
        when(mockUserFileMetaRepository.findById(reservedId.toString())).thenReturn(Mono.empty());

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, reservedId, List.of(allowOwner)))
                .expectNextMatches(file -> file.getId().equals(reservedId) && file
                        .getUserId()
                        .equals(ownerUser.getId()) && file.getIsDeleted() && file.getFileType() == FileEnum.FOLDER)
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(reservedId.toString());
    }

    @Test
    @DisplayName("非保留ID + 不存在檔案 -> 檔案不存在例外")
    void testReservedSearchMethod_NonReservedId_ThrowsNotExistingUserFile() {
        Long nonReservedId = -999L;
        when(mockUserFileMetaRepository.findById(nonReservedId.toString())).thenReturn(Mono.empty());

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, nonReservedId, List.of(allowOwner)))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NOT_EXISTING_USER_FILE)
                .verify();

        verify(mockUserFileMetaRepository).findById(nonReservedId.toString());
    }

    @Test
    @DisplayName("單一ID驗證存在檔案 -> 通過")
    void testValidateUserPermission_SingleId_FileExists_Passes() {
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        List<Permission<UserFileMetadata>> rules = List.of(allowOwner);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileMetadata.getId(), rules))
                .expectNextMatches(file -> file.getId().equals(fileMetadata.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
    }

    @Test
    @DisplayName("單一ID為保留ID -> 呼叫保留處理邏輯 通過")
    void testValidateUserPermission_SingleId_ReservedId_CallsReservedSearchMethod() {
        Long reservedId = ReservedSearchIdEnum.ALL_FILE_ID.getId();
        when(mockUserFileMetaRepository.findById(reservedId.toString())).thenReturn(Mono.empty());
        List<Permission<UserFileMetadata>> rules = List.of(allowOwner);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, reservedId, rules))
                .expectNextMatches(file -> file.getId().equals(reservedId) && file.getUserId().equals(ownerUser.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(reservedId.toString());
    }

    @Test
    @DisplayName("單一ID為不存在的非保留ID -> 檔案不存在例外")
    void testValidateUserPermission_SingleId_NonExistentNonReserved_ThrowsNotExistingUserFile() {
        Long nonExistentId = 999L;
        when(mockUserFileMetaRepository.findById(nonExistentId.toString())).thenReturn(Mono.empty());
        List<Permission<UserFileMetadata>> rules = List.of(allowOwner);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, nonExistentId, rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NOT_EXISTING_USER_FILE)
                .verify();

        verify(mockUserFileMetaRepository).findById(nonExistentId.toString());
    }

    @Test
    @DisplayName("多個檔案ID全部有效 -> 回傳檔案Metadata對應表")
    void testValidateUserPermission_MultipleIds_AllValid_ReturnsMetadataMap() {
        UserFileMetadata file2 = new UserFileMetadata();
        file2.setId(2L);
        file2.setUserId(1L);
        file2.setFileType(FileEnum.FOLDER);
        file2.setIsDeleted(false);
        file2.setFilename("testFolder");

        List<Long> fileIds = List.of(fileMetadata.getId(), file2.getId());
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockUserFileMetaRepository.findById(file2.getId().toString())).thenReturn(Mono.just(file2));
        List<Permission<UserFileMetadata>> rules = List.of(allowOwner);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileIds, rules))
                .expectNextMatches(map -> map.size() == 2 && map.get(fileMetadata.getId()).getId().equals(fileMetadata.getId()) && map
                        .get(file2.getId())
                        .getId()
                        .equals(file2.getId()))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockUserFileMetaRepository).findById(file2.getId().toString());
    }

    @Test
    @DisplayName("多個檔案ID，包含一般檔案與保留ID -> 回傳檔案Metadata對應表")
    void testValidateUserPermission_MultipleIds_MixedValidAndReserved_ReturnsPartialMap() {
        Long reservedId = ReservedSearchIdEnum.ALL_FILE_ID.getId();
        List<Long> fileIds = List.of(fileMetadata.getId(), reservedId);
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockUserFileMetaRepository.findById(reservedId.toString())).thenReturn(Mono.empty());
        List<Permission<UserFileMetadata>> rules = List.of(allowOwner);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileIds, rules))
                .expectNextMatches(map -> map.size() == 2 && map.get(fileMetadata.getId()).getId().equals(fileMetadata.getId()) && map
                        .get(reservedId)
                        .getId()
                        .equals(reservedId))
                .verifyComplete();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockUserFileMetaRepository).findById(reservedId.toString());
    }

    @Test
    @DisplayName("多個檔案ID，包含一般檔案與不存在的非保留ID，其中有一個檔案不具備權限 -> 拋出 FILE_PERMISSION_DENIED")
    void testValidateUserPermission_MultipleIds_SomeFail_ThrowsException() {
        UserFileMetadata file2 = new UserFileMetadata();
        file2.setId(2L);
        file2.setUserId(3L);
        file2.setFileType(FileEnum.FOLDER);
        file2.setIsDeleted(false);
        file2.setFilename("testFolder");

        List<Long> fileIds = List.of(fileMetadata.getId(), file2.getId());
        when(mockUserFileMetaRepository.findById(fileMetadata.getId().toString())).thenReturn(Mono.just(fileMetadata));
        when(mockUserFileMetaRepository.findById(file2.getId().toString())).thenReturn(Mono.just(file2));
        List<Permission<UserFileMetadata>> rules = List.of(allowOwner);

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileIds, rules))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verify();

        verify(mockUserFileMetaRepository).findById(fileMetadata.getId().toString());
        verify(mockUserFileMetaRepository).findById(file2.getId().toString());
    }

    @Test
    @DisplayName("空的檔案ID列表 -> 回傳空的Map")
    void testValidateUserPermission_MultipleIds_EmptyInput_ReturnsEmptyMap() {
        List<Long> fileIds = List.of();

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileIds, List.of(allowOwner)))
                .expectNextMatches(Map::isEmpty)
                .verifyComplete();

        verifyNoInteractions(mockUserFileMetaRepository);
    }

    @Test
    @DisplayName("null的檔案ID列表 -> 回傳空的Map")
    void testValidateUserPermission_MultipleIds_NullInput_ReturnsEmptyMap() {
        Iterable<Long> fileIds = null;

        StepVerifier
                .create(filePermissionServiceImplUnderTest.validateUserPermission(ownerUser, fileIds, List.of(allowOwner)))
                .expectNextMatches(Map::isEmpty)
                .verifyComplete();

        verifyNoInteractions(mockUserFileMetaRepository);
    }
}
