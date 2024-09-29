package xyz.dowob.filemanagement.component.strategy;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.service.ServiceImpl.ImageFileServiceImpl;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;

import java.util.EnumMap;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName FileStrategy
 * @description
 * @create 2024-09-26 22:10
 * @Version 1.0
 **/

//todo 使用註釋處理service
@Component
@Log4j2
public class FileStrategy {
    private final EnumMap<FileEnum, FileService> fileStrategies;

    public FileStrategy(ImageFileServiceImpl imageFileServiceImpl) {
        fileStrategies = new EnumMap<>(FileEnum.class);
        fileStrategies.put(FileEnum.IMAGE, imageFileServiceImpl);
             /*
        Logger logger = LoggerFactory.getLogger(FileStrategy.class);
        for (FileService service : fileServices) {
            FileHandlerType annotation = service.getClass().getAnnotation(FileHandlerType.class);
            logger.info("得到FileHandlerType: " + annotation);
            if (annotation != null) {
                logger.info("得到FileEnum: " + annotation.value());
                fileStrategies.put(annotation.value(), service);
            } else {
                logger.info("沒有得到FileHandlerType");
            }
        }
              */
    }


    public FileService getFileService(FileEnum fileEnum) {
        FileService fileService = fileStrategies.get(fileEnum);
        if (fileService == null) {
            throw new IllegalArgumentException("無法找到對應的檔案處理方法");
        }
        return fileService;
    }
}
