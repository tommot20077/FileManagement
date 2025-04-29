package xyz.dowob.filemanagement.service.serviceImpl.fileservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.annotation.Nullable;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.manager.CacheManager;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.FileScanProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.repostiory.*;
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
@RecordLevel(LogLevelEnum.DEBUG)
@FileHandlerType(FileEnum.OTHER)
public class GeneralFileServiceImpl extends AbstractFileService {

    /**
     * 一般檔案業務邏輯實現類，繼承 @see {@link AbstractFileService}
     *
     * @param serverFileMetaRepository      伺服器檔案元數據操作介面
     * @param userFileMetaRepository        用戶檔案元數據操作介面
     * @param redisProvider                 Redis提供者
     * @param gridFsProvider                GridFS提供者
     * @param transfersTasksManager         傳輸任務管理器
     * @param fileProperties                檔案屬性配置
     * @param circuitBreakerConfig          CircuitBreaker配置
     * @param userRepository                用戶操作介面
     * @param userOnlineFileRepository      用戶在線檔案操作介面
     * @param entityOperations              R2DBC實體操作介面
     * @param fileTrashRecordRepository     檔案垃圾桶記錄操作介面
     * @param transactionalOperator         事務操作介面
     * @param rateLimiterConfig             RateLimiter配置
     * @param userFIleShareRecordRepository 用戶檔案分享記錄操作介面
     */
    public GeneralFileServiceImpl(ServerFileMetaRepository serverFileMetaRepository, UserFileMetaRepository userFileMetaRepository, RedisProvider redisProvider, GridFsProvider gridFsProvider, TransfersTasksManager transfersTasksManager, FileProperties fileProperties, CircuitBreakerConfig circuitBreakerConfig, UserRepository userRepository, UserOnlineFileRepository userOnlineFileRepository, R2dbcEntityOperations entityOperations, FileTrashRecordRepository fileTrashRecordRepository, TransactionalOperator transactionalOperator, RateLimiterConfig rateLimiterConfig, UserFIleShareRecordRepository userFIleShareRecordRepository, ObjectMapper objectMapper, CacheManager cacheManager,
                                  @Nullable FolderListTreeProvider folderListTreeProvider, @Nullable FileScanProvider fileScanProvider) {
        super(serverFileMetaRepository,
              userFileMetaRepository,
              userOnlineFileRepository,
              userRepository,
              redisProvider,
              gridFsProvider, fileScanProvider,
              transfersTasksManager,
              fileProperties,
              circuitBreakerConfig,
              rateLimiterConfig,
              folderListTreeProvider,
              fileTrashRecordRepository,
              entityOperations,
              transactionalOperator,
              userFIleShareRecordRepository, objectMapper, cacheManager
        );
    }
}
