package xyz.dowob.filemanagement.service.serviceImpl.fileservice;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import jakarta.annotation.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.repostiory.ServerFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserOnlineFileRepository;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService;

/**
 * 圖片文件業務邏輯實現類，實現接口 @see {@link AbstractFileService}
 * 主要實現圖片文件的業務邏輯
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ImageFileServiceImpl
 * @description
 * @create 2024-09-27 00:48
 * @Version 1.0
 **/
@Service
@FileHandlerType(FileEnum.OTHER)
public class GeneralFileServiceImpl extends AbstractFileService {
    public GeneralFileServiceImpl(ServerFileMetaRepository serverFileMetaRepository, UserFileMetaRepository userFileMetaRepository, RedisProvider redisProvider, GridFsProvider gridFsProvider, TransfersTasksManager transfersTasksManager, FileProperties fileProperties, DatabaseClient databaseClient, CircuitBreakerConfig circuitBreakerConfig,
                                  @Nullable
                                  FolderListTreeProvider folderListTreeProvider, UserRepository userRepository, UserOnlineFileRepository userOnlineFileRepository) {
        super(serverFileMetaRepository,
              userFileMetaRepository, userOnlineFileRepository,
              userRepository,
              redisProvider,
              gridFsProvider,
              transfersTasksManager,
              fileProperties,
              databaseClient,
              circuitBreakerConfig,
              folderListTreeProvider
        );
    }
}
