package xyz.dowob.filemanagement.component.strategy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.service.ServiceImpl.ImageFileServiceImpl;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;

import java.util.EnumMap;
import java.util.List;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName FileStrategy
 * @description
 * @create 2024-09-26 22:10
 * @Version 1.0
 **/
@Component
public class FileStrategy {
    private final EnumMap<FileEnum, FileService> fileStrategies;

    public FileStrategy(ImageFileServiceImpl imageFileServiceImpl, List<FileService> fileServices) {
        fileStrategies = new EnumMap<>(FileEnum.class);
        // Logger logger = LoggerFactory.getLogger(FileStrategy.class);
        // for (FileService service : fileServices) {
        //     //fileStrategies.put(FileEnum.IMAGE, imageFileServiceImpl);
        //     logger.info("得到FileService: " + service);
        //     FileHandlerType annotation = service.getClass().getAnnotation(FileHandlerType.class);
        //     logger.info("得到FileHandlerType: " + annotation);
        //     if (annotation != null) {
        //         logger.info("得到FileEnum: " + annotation.value());
        //         fileStrategies.put(annotation.value(), service);
        //     } else {
        //         logger.info("沒有得到FileHandlerType");
        //     }
        // }
    }

    @PostConstruct
    public void init(List<FileService> fileServices) {
        Logger logger = LoggerFactory.getLogger(FileStrategy.class);
        logger.error("開始初始化 FileStrategy，總共有 {} 個 FileService", fileServices.size());
        for (FileService service : fileServices) {
            logger.info("正在處理 FileService: " + service.getClass().getName());
            FileHandlerType annotation = service.getClass().getAnnotation(FileHandlerType.class);
            if (annotation != null) {
                logger.info("檔案處理類型: " + annotation.value());
                fileStrategies.put(annotation.value(), service);
            } else {
                logger.warn("沒有找到 @FileHandlerType 註解於: " + service.getClass().getName());
            }
        }
        logger.info("FileStrategy 初始化完成。");
    }



    public FileService getFileService(FileEnum fileEnum) {
        FileService fileService = fileStrategies.get(fileEnum);
        if (fileService == null) {
            throw new IllegalArgumentException("無法找到對應的檔案處理方法");
        }
        return fileService;
    }
}
