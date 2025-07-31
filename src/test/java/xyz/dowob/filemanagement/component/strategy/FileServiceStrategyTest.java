package xyz.dowob.filemanagement.component.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.service.serviceImpl.fileservice.FolderFileServiceImpl;
import xyz.dowob.filemanagement.service.serviceImpl.fileservice.GeneralFileServiceImpl;
import xyz.dowob.filemanagement.service.serviceImpl.fileservice.OnlineFileServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * FileServiceStrategyTest 測試類別。
 * 
 * <p>測試 FileServiceStrategy 的檔案服務策略選擇功能，包括：
 * <ul>
 * <li>構造函數初始化與驗證</li>
 * <li>存在的檔案服務類型的正確獲取</li>
 * <li>不存在的檔案服務類型的異常處理</li>
 * <li>重複檔案類型註解的驗證</li>
 * <li>預設檔案服務的獲取</li>
 * </ul>
 * 
 * <p>測試涵蓋策略模式下檔案服務的所有核心功能，包含正常情況、異常處理及系統穩定性。
 * 透過模擬不同的檔案服務實現，驗證策略選擇的正確性和依賴注入的健壯性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("FileServiceStrategy 邏輯處理測試")
@ExtendWith(MockitoExtension.class)
class FileServiceStrategyTest {

    @Mock
    private FolderFileServiceImpl folderFileService;

    @Mock
    private OnlineFileServiceImpl onlineFileService;

    @Mock
    private GeneralFileServiceImpl generalFileService;

    private FileServiceStrategy fileServiceStrategy;

    @BeforeEach
    void setup() {
        folderFileService = mock(FolderFileServiceImpl.class);
        onlineFileService = mock(OnlineFileServiceImpl.class);
        generalFileService = mock(GeneralFileServiceImpl.class);

        fileServiceStrategy = new FileServiceStrategy(List.of(folderFileService, onlineFileService, generalFileService));
    }

    @Test
    @DisplayName("測試構造函數初始化 - 正常案例")
    void constructorInitialization_NormalCase() {
        assertDoesNotThrow(() -> new FileServiceStrategy(List.of(folderFileService, onlineFileService, generalFileService)));
    }

    @Test
    @DisplayName("獲取檔案服務 - 存在對應類型應成功")
    void getExistingFileService_ShouldSuccess() {
        assertNotNull(fileServiceStrategy.getFileService(FileEnum.FOLDER));
        assertNotNull(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT));
        assertNotNull(fileServiceStrategy.getFileService(FileEnum.OTHER));
    }

    @Test
    @DisplayName("獲取檔案服務 - 不存在的類型應拋出IllegalArgumentException")
    void getNonExistFileService_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> fileServiceStrategy.getFileService(FileEnum.DOCUMENT));
    }

    @Test
    @DisplayName("重複檔案類型註解 - 構造時應拋出IllegalArgumentException")
    void duplicateFileTypeAnnotation_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new FileServiceStrategy(List.of(folderFileService, folderFileService)));
    }

    @Test
    @DisplayName("獲取默認檔案服務 - OTHER類型存在應成功")
    void getDefaultFileService_WhenOtherExists_ShouldSuccess() {
        assertNotNull(fileServiceStrategy.getFileService());
    }
}
