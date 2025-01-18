package xyz.dowob.filemanagement.component.strategy;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;

import java.util.EnumMap;
import java.util.List;

/**
 * 檔案策略類，用於根據不同的檔案類型選擇不同的檔案處理方法，並將其封裝在一個類中
 * 當需要擴展新的檔案類型時，只需要在此類中添加對應的檔案處理方法即可
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileStrategy
 * @description
 * @create 2024-09-26 22:10
 * @Version 1.0
 **/

@Log4j2
@Component
public class FileStrategy {
    /**
     * 檔案處理策略，使用 EnumMap 來存儲不同類型的檔案處理方法
     */
    private final EnumMap<FileEnum, FileService> fileStrategies;

    /**
     * 構造方法，用於注入所有的檔案處理方法
     * 會根據該類型的 FileHandlerType 注解來將對應的檔案處理方法存儲到 fileStrategies 中
     *
     * @param fileServices 檔案處理方法
     */
    public FileStrategy(List<FileService> fileServices) {
        fileStrategies = new EnumMap<>(FileEnum.class);

        for (FileService service : fileServices) {
            FileHandlerType annotation = AnnotatedElementUtils.findMergedAnnotation(service.getClass(), FileHandlerType.class);
            if (annotation != null) {
                fileStrategies.put(annotation.value(), service);
            }
        }
    }

    /**
     * 根據檔案類型獲取對應的檔案處理方法
     *
     * @param fileEnum 檔案類型
     *
     * @return 返回對應的檔案處理方法
     */
    public FileService getFileService(FileEnum fileEnum) {
        FileService fileService = fileStrategies.get(fileEnum);
        if (fileService == null) {
            throw new IllegalArgumentException("無法找到對應的檔案處理方法");
        }
        return fileService;
    }
}
