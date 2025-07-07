package xyz.dowob.filemanagement.component.provider.provider;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Log4j2
@DisplayName("FolderListTreeProvider 邏輯處理測試")
@ExtendWith(MockitoExtension.class)
class FolderListTreeProviderTest {

    @Mock
    private FileProperties mockFileProperties;

    private FolderListTreeProvider folderListTreeProviderUnderTest;
    private UserFileMetadata testFolder;
    private UserFileMetadata childFolder;

    @BeforeEach
    void setUp() {
        FileProperties.Global globalConfig = new FileProperties.Global();
        globalConfig.setMaxFolderDepth(10);
        when(mockFileProperties.getGlobal()).thenReturn(globalConfig);


        folderListTreeProviderUnderTest = new FolderListTreeProvider(mockFileProperties);

        testFolder = new UserFileMetadata();
        testFolder.setId(1L);
        testFolder.setFilename("testFolder");
        testFolder.setFileType(FileEnum.FOLDER);

        childFolder = new UserFileMetadata();
        childFolder.setId(2L);
        childFolder.setFilename("childFolder");
        childFolder.setFileType(FileEnum.FOLDER);
        childFolder.setParentFolderId(1L);
    }

    @Test
    @DisplayName("成功添加新資料夾 - 應正確建立父子關係")
    void addFolder_shouldCreateParentChildRelationship() {
        StepVerifier.create(Mono.fromRunnable(() -> {
            try {
                folderListTreeProviderUnderTest.addFolder(1L, testFolder);
                folderListTreeProviderUnderTest.addFolder(1L, childFolder);
            } catch (ProcessException | ValidationException e) {
                throw new RuntimeException(e);
            }
        })).verifyComplete();

        FolderListTreeProvider.FolderTree tree = folderListTreeProviderUnderTest.getFileTree(1L);
        assertThat(getFolderMapViaReflection(tree).get(2L).getParentFolder().getFolderId()).isEqualTo(1L);
    }

    private Map<Long, FolderListTreeProvider.FolderNode> getFolderMapViaReflection(FolderListTreeProvider.FolderTree tree) {
        try {
            Field field = FolderListTreeProvider.FolderTree.class.getDeclaredField("folderMap");
            field.setAccessible(true);
            return (Map<Long, FolderListTreeProvider.FolderNode>) field.get(tree);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed for folderMap", e);
        }
    }

    @Test
    @DisplayName("添加資料夾超過最大深度限制 - 應拋出ValidationException")
    void addFolder_whenExceedMaxDepth_throwValidationException() {
        FileProperties.Global globalConfig = new FileProperties.Global();
        globalConfig.setMaxFolderDepth(3);
        when(mockFileProperties.getGlobal()).thenReturn(globalConfig);
        FolderListTreeProvider testProvider = new FolderListTreeProvider(mockFileProperties);

        List<UserFileMetadata> folders = generateDeepFolderStructure(4);

        StepVerifier.create(Mono.defer(() -> {
            try {
                testProvider.addFolders(1L, folders);
                return Mono.empty();
            } catch (ProcessException | ValidationException e) {
                return Mono.error(e);
            }
        })).verifyErrorSatisfies(e -> {
            assertThat(e)
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ValidationException.ErrorCode.EXCEED_MAX_FOLDER_DEPTH);
        });
    }

    private List<UserFileMetadata> generateDeepFolderStructure(int depth) {
        List<UserFileMetadata> folders = new ArrayList<>();
        for (long i = 1; i <= depth; i++) {
            UserFileMetadata folder = new UserFileMetadata();
            folder.setId(i);
            folder.setFileType(FileEnum.FOLDER);
            folder.setFilename("folder" + i);
            if (i > 1) {
                folder.setParentFolderId(i - 1);
            }
            folders.add(folder);
        }
        return folders;
    }

    @Test
    @DisplayName("移動資料夾造成循環引用 - 應拋出ProcessException")
    void updateFolder_causeCircularReference_throwProcessException() {
        UserFileMetadata folderA = createFolder(1L, "A", null);
        UserFileMetadata folderB = createFolder(2L, "B", 1L);
        UserFileMetadata folderC = createFolder(3L, "C", 2L);

        StepVerifier.create(Mono.defer(() -> {
            try {
                folderListTreeProviderUnderTest.addFolders(1L, Arrays.asList(folderA, folderB, folderC));
                return Mono.empty();
            } catch (ProcessException | ValidationException e) {
                return Mono.error(e);
            }
        })).verifyComplete();

        FileEditDTO editDTO = new FileEditDTO();
        editDTO.setParentFolderId(3L);
        StepVerifier.create(Mono.defer(() -> {
            try {
                folderListTreeProviderUnderTest.updateFolder(1L, folderA, editDTO);
                return Mono.empty();
            } catch (ValidationException | ProcessException e) {
                return Mono.error(e);
            }
        })).verifyErrorSatisfies(e -> {
            assertThat(e)
                    .isInstanceOf(ProcessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ProcessException.ErrorCode.FOLDER_TREE_EXISTING_CYCLE);
        });
    }

    private UserFileMetadata createFolder(Long id, String name, Long parentId) {
        UserFileMetadata folder = new UserFileMetadata();
        folder.setId(id);
        folder.setFilename(name);
        folder.setFileType(FileEnum.FOLDER);
        folder.setParentFolderId(parentId);
        return folder;
    }

    @Test
    @DisplayName("移動資料夾造成自身循環引用 - 應拋出ProcessException")
    void updateFolder_causeSelfParenting_throwProcessException() {
        UserFileMetadata folderA = createFolder(1L, "A", null);

        StepVerifier.create(Mono.defer(() -> {
            try {
                folderListTreeProviderUnderTest.addFolder(1L, folderA);
                return Mono.empty();
            } catch (ProcessException | ValidationException e) {
                return Mono.error(e);
            }
        })).verifyComplete();

        FileEditDTO editDTO = new FileEditDTO();
        editDTO.setParentFolderId(1L);
        StepVerifier.create(Mono.defer(() -> {
            try {
                folderListTreeProviderUnderTest.updateFolder(1L, folderA, editDTO);
                return Mono.empty();
            } catch (ValidationException | ProcessException e) {
                return Mono.error(e);
            }
        })).verifyErrorSatisfies(e -> {
            assertThat(e)
                    .isInstanceOf(ProcessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ProcessException.ErrorCode.FOLDER_TREE_EXISTING_CYCLE);
        });
    }

    @Test
    @DisplayName("移動父資料夾到其子資料夾下方造成循環引用 - 應拋出ProcessException")
    void updateFolder_causeParentUnderChild_throwProcessException() {
        UserFileMetadata folderA = createFolder(1L, "A", null);
        UserFileMetadata folderB = createFolder(2L, "B", 1L);

        StepVerifier.create(Mono.defer(() -> {
            try {
                folderListTreeProviderUnderTest.addFolders(1L, Arrays.asList(folderA, folderB));
                return Mono.empty();
            } catch (ProcessException | ValidationException e) {
                return Mono.error(e);
            }
        })).verifyComplete();

        FileEditDTO editDTO = new FileEditDTO();
        editDTO.setParentFolderId(2L);
        StepVerifier.create(Mono.defer(() -> {
            try {
                folderListTreeProviderUnderTest.updateFolder(1L, folderA, editDTO);
                return Mono.empty();
            } catch (ValidationException | ProcessException e) {
                return Mono.error(e);
            }
        })).verifyErrorSatisfies(e -> {
            assertThat(e)
                    .isInstanceOf(ProcessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ProcessException.ErrorCode.FOLDER_TREE_EXISTING_CYCLE);
        });
    }

    @Test
    @DisplayName("成功移動資料夾 - 應正確更新父子關係")
    void updateFolder_successfulMove_shouldUpdateParent() {
        UserFileMetadata folderA = createFolder(1L, "A", null);
        UserFileMetadata folderB = createFolder(2L, "B", 1L);
        UserFileMetadata folderX = createFolder(10L, "X", null);
        StepVerifier.create(Mono.defer(() -> {
            try {
                folderListTreeProviderUnderTest.addFolders(1L, Arrays.asList(folderA, folderB, folderX));
                return Mono.empty();
            } catch (ProcessException | ValidationException e) {
                return Mono.error(e);
            }
        })).verifyComplete();

        FileEditDTO editDTO = new FileEditDTO();
        editDTO.setParentFolderId(10L);
        StepVerifier.create(Mono.defer(() -> {
            try {
                folderListTreeProviderUnderTest.updateFolder(1L, folderA, editDTO);
                return Mono.empty();
            } catch (ValidationException | ProcessException e) {
                return Mono.error(e);
            }
        })).verifyComplete();
        FolderListTreeProvider.FolderTree tree = folderListTreeProviderUnderTest.getFileTree(1L);
        Map<Long, FolderListTreeProvider.FolderNode> folderMap = getFolderMapViaReflection(tree);

        assertThat(folderMap.get(1L).getParentFolder().getFolderId()).isEqualTo(10L);
        assertThat(folderMap.get(2L).getParentFolder().getFolderId()).isEqualTo(1L);
        assertThat(folderMap.get(10L).getChildren()).containsKey(1L);
        assertThat(folderMap.get(0L).getChildren()).doesNotContainKey(1L);
    }

    @Test
    @DisplayName("刪除資料夾 - 應移除所有子節點")
    void deleteFolder_shouldRemoveAllChildren() {
        StepVerifier.create(Mono.fromRunnable(() -> {
            try {
                folderListTreeProviderUnderTest.addFolder(1L, testFolder);
                folderListTreeProviderUnderTest.addFolder(1L, childFolder);
            } catch (ProcessException | ValidationException e) {
                throw new RuntimeException(e);
            }

            folderListTreeProviderUnderTest.deleteFolderSync(1L, 1L);
        })).verifyComplete();

        FolderListTreeProvider.FolderTree tree = folderListTreeProviderUnderTest.getFileTree(1L);
        assertThat(getFolderMapViaReflection(tree)).doesNotContainKeys(1L, 2L);
    }

    @Test
    @DisplayName("獲取資料夾路徑 - 應返回正確層級結構")
    void getPath_shouldReturnCorrectHierarchy() {
        StepVerifier.create(Mono.fromRunnable(() -> {
            try {
                folderListTreeProviderUnderTest.addFolder(1L, testFolder);
                folderListTreeProviderUnderTest.addFolder(1L, childFolder);
            } catch (ProcessException | ValidationException e) {
                throw new RuntimeException(e);
            }
        })).verifyComplete();

        List<FolderListTreeProvider.FolderNode> path = folderListTreeProviderUnderTest.getPath(1L, 2L);
        assertThat(path).extracting("folderId").containsExactly(2L, 1L, 0L);
    }

    @Test
    @DisplayName("初始化樹結構包含無效父資料夾ID - 應拋出ProcessException")
    void initializeTree_withInvalidParentId_throwProcessException() {
        UserFileListDTO invalidFolder = new UserFileListDTO();
        invalidFolder.setId(99L);
        invalidFolder.setFileType(FileEnum.FOLDER);
        invalidFolder.setParentFolderId(999L);

        StepVerifier.create(Mono.defer(() -> {
            try {
                folderListTreeProviderUnderTest.initializeTree(1L, List.of(invalidFolder), true);
                return Mono.empty();
            } catch (ProcessException | ValidationException e) {
                return Mono.error(e);
            }
        })).verifyErrorSatisfies(e -> {
            assertThat(e)
                    .isInstanceOf(ProcessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ProcessException.ErrorCode.BUILD_FILE_TREE_FAILED)
                    .hasMessageContaining("構建文件樹失敗: 無效的 parentFolderId: " + invalidFolder.getParentFolderId());
        });
    }
}
