package xyz.dowob.filemanagement.component.strategy;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

/**
 * 基於 EnumMap 的檔案服務策略實現，根據檔案類型動態選擇對應的檔案處理服務。
 *
 * <p>此實現採用策略模式，在應用程式啟動時自動掃描所有帶有 {@link FileHandlerType}
 * 註解的 {@link FileService} 實現，並將其註冊到內部映射表中。當請求特定檔案類型的
 * 服務時，會從映射表中返回對應的服務實例。
 *
 * <p>如果未指定檔案類型或找不到對應的服務，將返回 {@link FileEnum#OTHER} 類型的
 * 預設服務。所有查找失敗的情況都會拋出 {@link IllegalArgumentException}。
 *
 * <p>服務註冊過程中如果發現重複的檔案類型映射，構造函數會立即拋出異常以確保
 * 系統一致性。新增檔案類型支援僅需實作 {@link FileService} 介面並添加適當的
 * {@link FileHandlerType} 註解。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see FileService
 * @see FileHandlerType
 * @see FileEnum
 */
@Component
public class FileServiceStrategy {
    /**
     * 檔案服務映射表，使用 EnumMap 存儲檔案類型與對應服務實例的映射關係。
     * 此映射表在建構時初始化，提供高效的檔案類型查找性能。
     */
    private final EnumMap<FileEnum, FileService> fileStrategies;


    /**
     * 建構檔案服務策略並初始化服務映射表。
     *
     * <p>此建構函數會掃描所有提供的檔案服務，檢查其 {@link FileHandlerType} 註解，
     * 並將服務實例註冊到對應的檔案類型映射中。註冊過程中會驗證是否存在重複的
     * 檔案類型映射，如有重複則拋出異常。
     *
     * @param fileServices 所有可用的檔案服務實現列表，通過依賴注入提供
     *
     * @throws IllegalArgumentException 當發現重複的檔案類型映射或未找到任何有效服務時
     */
    public FileServiceStrategy(List<FileService> fileServices) {
        fileStrategies = new EnumMap<>(FileEnum.class);

        for (FileService service : fileServices) {
            FileHandlerType annotation = AnnotatedElementUtils.findMergedAnnotation(service.getClass(), FileHandlerType.class);
            if (annotation != null) {
                if (fileStrategies.containsKey(annotation.value())) {
                    throw new IllegalArgumentException("檔案處理方法重複，請檢查是否有添加 FileHandlerType 注解");
                }
                LogUnity.debug("註冊檔案處理方法: %s, 類型: %s", service.getClass().getName(), annotation.value());
                fileStrategies.put(annotation.value(), service);
            }
        }
        if (fileStrategies.isEmpty()) {
            throw new IllegalArgumentException("沒有找到對應的檔案處理方法，請檢查是否有添加 FileHandlerType 注解");
        }
    }


    /**
     * 獲取預設的檔案處理服務。
     *
     * <p>此方法相當於呼叫 {@code getFileService(null)}，會返回
     * {@link FileEnum#OTHER} 類型對應的服務實例。
     *
     * @return 預設的檔案處理服務實例
     *
     * @throws IllegalArgumentException 當未找到 OTHER 類型的服務時
     */
    public FileService getFileService() {
        return getFileService(null);
    }


    /**
     * 根據指定的檔案類型獲取對應的檔案處理服務。
     *
     * <p>當 fileEnum 為 null 時，返回 {@link FileEnum#OTHER} 類型對應的預設服務。
     * 如果指定的檔案類型存在對應的服務，則直接返回該服務實例。
     * 如果找不到指定檔案類型的服務，則回退到通用的 {@link FileEnum#OTHER} 服務。
     *
     * @param fileEnum 要查找的檔案類型，null 表示使用預設類型
     *
     * @return 對應的檔案處理服務實例
     *
     * @throws IllegalArgumentException 當找不到任何可用的服務時
     */
    public FileService getFileService(FileEnum fileEnum) {
        FileEnum chosenFileEnum = fileEnum == null ? FileEnum.OTHER : fileEnum;

        return Optional
                .ofNullable(fileStrategies.get(chosenFileEnum))
                .orElseGet(() -> Optional
                        .ofNullable(fileStrategies.get(FileEnum.OTHER))
                        .orElseThrow(() -> new IllegalArgumentException("沒有找到對應的檔案處理方法")));
    }

}
