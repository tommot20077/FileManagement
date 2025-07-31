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

/**
 * FolderListTreeManager 資料夾清單樹狀管理測試類別。
 *
 * 測試 FolderListTreeManager 的資料夾清單樹狀結構初始化和管理功能，
 * 包括應用程式啟動時的初始化、特定用戶的樹狀結構建立和錯誤處理等。
 *
 * 前置條件：
 * - 初始化所有必要的 Mock 依賴項目
 * - 設置測試用戶和檔案清單資料
 * - 確保線程池執行器的正確設定
 *
 * 測試步驟：
 * - 測試應用程式啟動時的樹狀初始化
 * - 測試特定用戶的樹狀初始化
 * - 測試所有用戶的批量初始化
 * - 測試檔案服務失敗時的處理機制
 *
 * 預期結果：
 * - 樹狀結構應被正確初始化
 * - 異常情況應被適當處理
 * - 並行操作應正確執行
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
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


    /**
     * 測試應用程式啟動時初始化檔案清單樹的功能。
     *
     * 測試步驟：
     * - 建立測試用戶和分頁響應資料
     * - 模擬儲存庫和檔案服務操作
     * - 執行應用程式啟動程序
     * - 驗證樹狀結構初始化被正確調用
     *
     * 預期結果：所有用戶的檔案清單樹應被初始化
     */
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


    /**
     * 建立測試用戶的輔助方法。
     *
     * @param id 用戶 ID
     * @return 測試用戶物件
     */
    private User createTestUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("testUser" + id);
        user.setRole(RoleEnum.USER);
        return user;
    }


    /**
     * 建立測試分頁響應資料的輔助方法。
     *
     * @param currentPage 當前頁碼
     * @param totalPages 總頁數
     * @return 分頁響應資料物件
     */
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


    /**
     * 測試為特定用戶初始化檔案清單樹的功能。
     *
     * 測試步驟：
     * - 指定特定用戶 ID
     * - 模擬儲存庫和檔案服務操作
     * - 執行初始化操作
     * - 驗證特定用戶的樹狀結構被初始化
     *
     * 預期結果：指定用戶的檔案清單樹應被初始化
     */
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


    /**
     * 測試為所有用戶初始化檔案清單樹的功能。
     *
     * 測試步驟：
     * - 建立多個測試用戶
     * - 模擬儲存庫和檔案服務操作
     * - 執行所有用戶的初始化
     * - 驗證每個用戶的樹狀結構都被初始化
     *
     * 預期結果：所有用戶的檔案清單樹都應被初始化
     */
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


    /**
     * 測試檔案服務拋出異常時的處理機制。
     *
     * 測試步驟：
     * - 建立多個測試用戶
     * - 設置其中一個用戶的檔案服務拋出異常
     * - 設置另一個用戶的檔案服務正常遇作
     * - 執行初始化操作
     * - 驗證異常情況下的處理結果
     *
     * 預期結果：異常用戶的初始化應失敗，但不影響正常用戶
     */
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
