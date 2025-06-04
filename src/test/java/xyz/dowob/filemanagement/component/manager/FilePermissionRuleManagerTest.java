package xyz.dowob.filemanagement.component.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.UserFIleShareRecordRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilePermissionRuleManager 邏輯處理測試")
class FilePermissionRuleManagerTest {

    @Mock
    private UserFIleShareRecordRepository mockShareRecordRepository;

    @Mock
    private UserFileMetaRepository mockUserFileMetaRepository;

    @Mock
    private R2dbcEntityOperations mockR2dbcEntityOperations;

    private FilePermissionRuleManager filePermissionRuleManagerUnderTest;

    private User testUser;

    private UserFileMetadata testFile;


    @BeforeEach
    void setUp() {
        filePermissionRuleManagerUnderTest = new FilePermissionRuleManager(mockShareRecordRepository,
                                                                           mockUserFileMetaRepository,
                                                                           mockR2dbcEntityOperations
        );
        filePermissionRuleManagerUnderTest.initPermissions();

        testUser = new User();
        testUser.setId(1L);

        testFile = new UserFileMetadata();
        testFile.setId(1L);
        testFile.setUserId(2L);
        testFile.setIsDeleted(false);
        testFile.setShareType(FileShareTypeEnum.PRIVATE);
    }


    @Test
    @DisplayName("測試檔案擁有者權限規則 - 應允許訪問")
    void allowOwner_whenUserIsOwner_shouldAllowAccess() {
        testFile.setUserId(testUser.getId());

        StepVerifier.create(filePermissionRuleManagerUnderTest.getAllowOwner().check(testUser, testFile)).verifyComplete();
    }


    @Test
    @DisplayName("測試非檔案擁有者權限規則 - 應拒絕訪問")
    void allowOwner_whenUserIsNotOwner_shouldDenyAccess() {
        StepVerifier
                .create(filePermissionRuleManagerUnderTest.getAllowOwner().check(testUser, testFile))
                .expectNextMatches(ex -> ex instanceof ValidationException && ((ValidationException) ex).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verifyComplete();
    }


    @Test
    @DisplayName("測試公開共享檔案權限規則 - 應允許訪問")
    void allowShared_whenFileIsPublic_shouldAllowAccess() {
        testFile.setShareType(FileShareTypeEnum.PUBLIC);

        StepVerifier.create(filePermissionRuleManagerUnderTest.getAllowShared().check(testUser, testFile)).verifyComplete();
    }


    @Test
    @DisplayName("測試私有檔案權限規則 - 當用戶有共享記錄時應允許訪問")
    void allowShared_whenFileIsPrivateAndUserHasShareRecord_shouldAllowAccess() {
        when(mockShareRecordRepository.existsByUserIdAndFileId(testUser.getId(), testFile.getId())).thenReturn(Mono.just(true));

        StepVerifier.create(filePermissionRuleManagerUnderTest.getAllowShared().check(testUser, testFile)).verifyComplete();
    }


    @Test
    @DisplayName("測試私有檔案權限規則 - 當用戶無共享記錄時應拒絕訪問")
    void allowShared_whenFileIsPrivateAndUserHasNoShareRecord_shouldDenyAccess() {
        when(mockShareRecordRepository.existsByUserIdAndFileId(testUser.getId(), testFile.getId())).thenReturn(Mono.just(false));

        StepVerifier
                .create(filePermissionRuleManagerUnderTest.getAllowShared().check(testUser, testFile))
                .expectNextMatches(ex -> ex instanceof ValidationException && ((ValidationException) ex).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verifyComplete();
    }


    @Test
    @DisplayName("測試默認共享檔案權限規則 - 當父資料夾為公開時應允許訪問")
    void allowShared_whenFileIsDefaultAndParentFolderIsPublic_shouldAllowAccess() {
        testFile.setShareType(FileShareTypeEnum.DEFAULT);
        testFile.setParentFolderId(3L);

        when(mockShareRecordRepository.existsByUserIdAndFileId(testUser.getId(), testFile.getId())).thenReturn(Mono.just(false));
        when(mockShareRecordRepository.existsByUserIdAndFileId(testUser.getId(), 3L)).thenReturn(Mono.just(false));
        when(mockUserFileMetaRepository.getShareTypeByFileId(eq(3L), any())).thenReturn(Mono.just(FileShareTypeEnum.PUBLIC));

        StepVerifier.create(filePermissionRuleManagerUnderTest.getAllowShared().check(testUser, testFile)).verifyComplete();
    }


    @Test
    @DisplayName("測試默認共享檔案權限規則 - 當父資料夾不可訪問時應拒絕訪問")
    void allowShared_whenFileIsDefaultAndParentFolderDeniesAccess_shouldDenyAccess() {
        testFile.setShareType(FileShareTypeEnum.DEFAULT);
        testFile.setParentFolderId(3L);

        when(mockShareRecordRepository.existsByUserIdAndFileId(testUser.getId(), testFile.getId())).thenReturn(Mono.just(false));
        when(mockShareRecordRepository.existsByUserIdAndFileId(testUser.getId(), 3L)).thenReturn(Mono.just(false));
        when(mockUserFileMetaRepository.getShareTypeByFileId(eq(3L), any())).thenReturn(Mono.just(FileShareTypeEnum.NONE));

        StepVerifier
                .create(filePermissionRuleManagerUnderTest.getAllowShared().check(testUser, testFile))
                .expectNextMatches(ex -> ex instanceof ValidationException && ((ValidationException) ex).getErrorCode() == ValidationException.ErrorCode.FILE_PERMISSION_DENIED)
                .verifyComplete();
    }


    @Test
    @DisplayName("測試搜索操作權限規則 - 當檔案ID非保留搜索ID時應允許訪問")
    void blockNotSearchOperation_whenFileIdIsNotReserved_shouldAllowAccess() {
        StepVerifier.create(filePermissionRuleManagerUnderTest.getBlockNotSearchOperation().check(testUser, testFile)).verifyComplete();
    }


    @Test
    @DisplayName("測試搜索操作權限規則 - 當檔案ID為保留搜索ID時應拒絕訪問")
    void blockNotSearchOperation_whenFileIdIsReserved_shouldDenyAccess() {
        testFile.setId(ReservedSearchIdEnum.ALL_FILE_ID.getId());

        StepVerifier
                .create(filePermissionRuleManagerUnderTest.getBlockNotSearchOperation().check(testUser, testFile))
                .expectNextMatches(ex -> ex instanceof ValidationException && ((ValidationException) ex).getErrorCode() == ValidationException.ErrorCode.NOT_EXISTING_USER_FILE)
                .verifyComplete();
    }


    @Test
    @DisplayName("測試已刪除檔案權限規則 - 當檔案未刪除時應允許訪問")
    void blockDeleted_whenFileIsNotDeleted_shouldAllowAccess() {
        StepVerifier.create(filePermissionRuleManagerUnderTest.getBlockDeleted().check(testUser, testFile)).verifyComplete();
    }


    @Test
    @DisplayName("測試已刪除檔案權限規則 - 當檔案已刪除時應拒絕訪問")
    void blockDeleted_whenFileIsDeleted_shouldDenyAccess() {
        testFile.setIsDeleted(true);

        StepVerifier
                .create(filePermissionRuleManagerUnderTest.getBlockDeleted().check(testUser, testFile))
                .expectNextMatches(ex -> ex instanceof ValidationException && ((ValidationException) ex).getErrorCode() == ValidationException.ErrorCode.ALREADY_DELETED_FILE)
                .verifyComplete();
    }


    @Test
    @DisplayName("測試預設權限規則ONLY_OWNER - 應返回正確的權限規則列表")
    void defaultRule_onlyOwner_shouldReturnCorrectRules() {
        var rules = FilePermissionRuleManager.DefaultRule.ONLY_OWNER.getRules(filePermissionRuleManagerUnderTest);

        assertThat(rules).containsExactly(filePermissionRuleManagerUnderTest.getAllowOwner(),
                                          filePermissionRuleManagerUnderTest.getBlockDeleted(),
                                          filePermissionRuleManagerUnderTest.getBlockNotSearchOperation()
        );
    }


    @Test
    @DisplayName("測試預設權限規則WITH_SHARED - 應返回正確的權限規則列表")
    void defaultRule_withShared_shouldReturnCorrectRules() {
        var rules = FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManagerUnderTest);

        assertThat(rules).containsExactly(filePermissionRuleManagerUnderTest.getAllowShared(),
                                          filePermissionRuleManagerUnderTest.getBlockDeleted(),
                                          filePermissionRuleManagerUnderTest.getBlockNotSearchOperation()
        );
    }
}
