package xyz.dowob.filemanagement.component.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.ApplicationArguments;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.unity.DynamicThreadPoolExecutor;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FolderListTreeManager 邏輯處理測試")
class FolderListTreeManagerTest {

    @Mock
    private UserRepository mockUserRepository;

    @Mock
    private FolderListTreeProvider mockFolderListTreeProvider;

    @Mock
    private FileServiceStrategy mockFileServiceStrategy;

    @Mock
    private FileProperties mockFileProperties;

    @Mock
    private FileService mockFileService;

    @Mock
    private ApplicationArguments mockArgs;

    @Mock
    private FileProperties.Global mockGlobal;

    @Mock
    private DynamicThreadPoolExecutor mockDynamicThreadPoolExecutor;

    private FolderListTreeManager folderListTreeManagerUnderTest;


    @BeforeEach
    void setUp() {
        clearInvocations(mockUserRepository, mockFolderListTreeProvider, mockDynamicThreadPoolExecutor);

        folderListTreeManagerUnderTest = new FolderListTreeManager(mockUserRepository,
                                                                   mockFolderListTreeProvider,
                                                                   mockFileServiceStrategy,
                                                                   mockFileProperties
        );

        when(mockFileProperties.getGlobal()).thenReturn(mockGlobal);
        when(mockGlobal.getPageSize()).thenReturn(10);
        when(mockFileServiceStrategy.getFileService()).thenReturn(mockFileService);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(mockDynamicThreadPoolExecutor).submit(any(Runnable.class));

        try {
            var field = FolderListTreeManager.class.getDeclaredField("dynamicThreadPoolExecutor");
            field.setAccessible(true);
            field.set(folderListTreeManagerUnderTest, mockDynamicThreadPoolExecutor);

            when(mockDynamicThreadPoolExecutor.getActiveCount()).thenReturn(0);
            when(mockDynamicThreadPoolExecutor.getQueue()).thenReturn(new LinkedBlockingQueue<>());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set dynamicThreadPoolExecutor", e);
        }
    }


    @Test
    @DisplayName("測試應用啟動時初始化檔案列表樹 - 正常執行")
    void run_shouldInitializeTreeOnStartup() throws ProcessException, ValidationException, InterruptedException {
        User user = createTestUser(1L);
        PagedResponseDTO<UserFileListDTO> pagedResponse = createTestPagedResponse(1, 1);
        Map<Long, FolderListTreeProvider.FolderTree> treeMap = new ConcurrentHashMap<>();
        AtomicInteger count = new AtomicInteger(0);

        when(mockUserRepository.findAll()).thenReturn(Flux.just(user));
        when(mockFolderListTreeProvider.getUserFileListTree()).thenReturn(treeMap);
        when(mockFileService.getUserFileList(eq(user), any(FileFilterDTO.class))).thenReturn(Mono.just(pagedResponse));

        folderListTreeManagerUnderTest.run(mockArgs);

        Thread.sleep(200);
        verify(mockUserRepository, times(1)).findAll();
        verify(mockFolderListTreeProvider).initializeTree(eq(user.getId()), any(), eq(true));
        verifyNoMoreInteractions(mockDynamicThreadPoolExecutor);
    }


    private User createTestUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("testUser" + id);
        user.setRole(RoleEnum.USER);
        return user;
    }


    private PagedResponseDTO<UserFileListDTO> createTestPagedResponse(int currentPage, int totalPages) {
        UserFileListDTO fileListDTO = new UserFileListDTO();
        fileListDTO.setId(1L);
        fileListDTO.setFilename("test-folder");
        fileListDTO.setFileType(FileEnum.FOLDER);

        return PagedResponseDTO
                .<UserFileListDTO>builder()
                .currentPage(currentPage)
                .totalPages(totalPages)
                .data(Collections.singletonList(fileListDTO))
                .build();
    }


    @Test
    @DisplayName("測試初始化特定用戶的檔案列表樹 - 正常執行")
    void initializeTree_withSpecificUser_shouldInitializeTreeForUser() throws ProcessException, ValidationException, InterruptedException {
        User user = createTestUser(1L);
        PagedResponseDTO<UserFileListDTO> pagedResponse = createTestPagedResponse(1, 1);
        Map<Long, FolderListTreeProvider.FolderTree> treeMap = new ConcurrentHashMap<>();

        when(mockUserRepository.findAllById(any(Flux.class))).thenReturn(Flux.just(user));
        when(mockFolderListTreeProvider.getUserFileListTree()).thenReturn(treeMap);
        when(mockFileService.getUserFileList(eq(user), any(FileFilterDTO.class))).thenReturn(Mono.just(pagedResponse));

        folderListTreeManagerUnderTest.initializeTree(1L);

        Thread.sleep(200);
        verify(mockUserRepository, times(1)).findAllById(any(Flux.class));
        verify(mockDynamicThreadPoolExecutor).submit(any(Runnable.class));
        verify(mockFolderListTreeProvider).initializeTree(eq(user.getId()), any(), eq(true));
        verifyNoMoreInteractions(mockDynamicThreadPoolExecutor);
    }


    @Test
    @DisplayName("測試初始化所有用戶的檔案列表樹 - 正常執行")
    void initializeTree_withNoUserSpecified_shouldInitializeTreeForAllUsers() throws ProcessException, ValidationException, InterruptedException {
        User user1 = createTestUser(1L);
        User user2 = createTestUser(2L);
        PagedResponseDTO<UserFileListDTO> pagedResponse = createTestPagedResponse(1, 1);
        Map<Long, FolderListTreeProvider.FolderTree> treeMap = new ConcurrentHashMap<>();

        when(mockUserRepository.findAll()).thenReturn(Flux.just(user1, user2));
        when(mockFolderListTreeProvider.getUserFileListTree()).thenReturn(treeMap);
        when(mockFileService.getUserFileList(any(User.class), any(FileFilterDTO.class))).thenReturn(Mono.just(pagedResponse));

        folderListTreeManagerUnderTest.initializeTree();

        Thread.sleep(200);
        verify(mockUserRepository, times(1)).findAll();
        verify(mockDynamicThreadPoolExecutor, times(2)).submit(any(Runnable.class));

        InOrder inOrder = inOrder(mockFolderListTreeProvider);
        inOrder.verify(mockFolderListTreeProvider).initializeTree(eq(user1.getId()), any(), eq(true));
        inOrder.verify(mockFolderListTreeProvider).initializeTree(eq(user2.getId()), any(), eq(true));
        verifyNoMoreInteractions(mockDynamicThreadPoolExecutor);
    }


    @Test
    @DisplayName("測試檔案服務拋出異常時的處理 - 異常測試")
    void initializeTree_whenFileServiceFails_shouldContinueProcessing() throws ProcessException, ValidationException, InterruptedException {
        User user1 = createTestUser(1L);
        User user2 = createTestUser(2L);
        PagedResponseDTO<UserFileListDTO> pagedResponse = createTestPagedResponse(1, 1);
        Map<Long, FolderListTreeProvider.FolderTree> treeMap = new ConcurrentHashMap<>();
        RuntimeException expectedException = new RuntimeException("Service error");

        when(mockUserRepository.findAll()).thenReturn(Flux.just(user1, user2));
        when(mockFolderListTreeProvider.getUserFileListTree()).thenReturn(treeMap);
        when(mockFileService.getUserFileList(eq(user1), any(FileFilterDTO.class))).thenReturn(Mono.error(expectedException));
        when(mockFileService.getUserFileList(eq(user2), any(FileFilterDTO.class))).thenReturn(Mono.just(pagedResponse));
        when(mock(DynamicThreadPoolExecutor.class).submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        });

        folderListTreeManagerUnderTest.initializeTree();

        Thread.sleep(100);
        verify(mockUserRepository, times(1)).findAll();

        verify(mockUserRepository).findAll();
        verify(mockFolderListTreeProvider, never()).initializeTree(eq(user1.getId()), any(), anyBoolean());
        verifyNoMoreInteractions(mockDynamicThreadPoolExecutor);
    }
}
