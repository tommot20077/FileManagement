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
 * 一般檔案服務實現類，處理通用檔案類型的業務邏輯操作。
 * <p>
 * 繼承 {@link xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService}，
 * 專門處理除了線上檔案和資料夾以外的所有檔案類型，包括圖片、文檔、壓縮檔、媒體檔案等。
 * <p>
 * <strong>核心功能：</strong>
 * <ul>
 *   <li>多種檔案格式的上傳、下載和管理</li>
 *   <li>整合 GridFS 分散式檔案儲存系統</li>
 *   <li>檔案完整性驗證和安全掃描功能</li>
 *   <li>反應式編程模式支援高並發操作</li>
 *   <li>檔案分塊上傳和斷點續傳機制</li>
 *   <li>自動化檔案類型檢測和處理</li>
 * </ul>
 * <p>
 * <strong>支援的檔案類型：</strong>
 * <ul>
 *   <li><strong>圖片：</strong>JPG、PNG、GIF、BMP、WEBP、SVG 等常見圖片格式</li>
 *   <li><strong>文檔：</strong>PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX、TXT 等辦公文檔</li>
 *   <li><strong>壓縮檔：</strong>ZIP、RAR、7Z、TAR、GZ 等壓縮格式</li>
 *   <li><strong>媒體：</strong>MP4、AVI、MP3、WAV、MKV 等音視頻檔案</li>
 *   <li><strong>程式碼：</strong>各種程式語言原始碼檔案</li>
 *   <li><strong>其他：</strong>除線上文檔和資料夾外的所有檔案類型</li>
 * </ul>
 * <p>
 * <strong>技術特性：</strong>
 * <ul>
 *   <li><strong>反應式處理：</strong>採用 WebFlux 響應式編程，支援非阻塞式檔案操作</li>
 *   <li><strong>分散式儲存：</strong>與 MongoDB GridFS 深度整合，實現檔案分塊儲存</li>
 *   <li><strong>安全掃描：</strong>可選的檔案安全掃描功能，預防惡意檔案上傳</li>
 *   <li><strong>完整性校驗：</strong>自動檔案 MD5 校驗，確保檔案傳輸完整性</li>
 *   <li><strong>錯誤恢復：</strong>內建斷路器和重試機制，提升系統穩定性</li>
 *   <li><strong>快取整合：</strong>與 Redis 快取系統整合，提升檔案存取效能</li>
 * </ul>
 * <p>
 * <strong>使用範例：</strong>
 * <pre>{@code
 * // 透過策略模式自動選擇服務
 * FileService fileService = fileServiceStrategy.getFileService(FileEnum.OTHER);
 * 
 * // 上傳檔案
 * Mono<UploadResponseDTO> uploadResult = fileService.uploadFile(fileMetadata, user);
 * 
 * // 下載檔案
 * Mono<UserFileDataBO> downloadResult = fileService.downloadFile(metadata, user);
 * }</pre>
 * <p>
 * <strong>注意事項：</strong>
 * <ul>
 *   <li>所有檔案操作都是異步非阻塞的，請正確處理 Mono/Flux 響應</li>
 *   <li>大檔案上傳會自動進行分塊處理，無需額外配置</li>
 *   <li>檔案安全掃描功能為可選，可透過配置開啟或關閉</li>
 *   <li>系統會自動進行檔案去重，相同內容的檔案只會儲存一份</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService
 * @see xyz.dowob.filemanagement.component.strategy.FileServiceStrategy
 * @see xyz.dowob.filemanagement.customenum.FileEnum#OTHER
 */
@Service
@RecordLevel(LogLevelEnum.DEBUG)
@FileHandlerType(FileEnum.OTHER)
public class GeneralFileServiceImpl extends AbstractFileService {

    /**
     * 一般檔案服務實現類的建構子。
     * <p>
     * 初始化一般檔案服務的所有必要依賴組件，包括資料庫操作介面、
     * 儲存提供者、緩存管理器、事務管理器等核心功能。
     * 所有參數都會傳遞給父類 {@link AbstractFileService} 進行統一初始化，
     * 確保繼承的檔案操作功能正常運作。
     * <p>
     * <strong>初始化組件說明：</strong>
     * <ul>
     *   <li><strong>資料庫層：</strong>伺服器檔案元資料、用戶檔案元資料、線上檔案等資料庫操作介面</li>
     *   <li><strong>儲存層：</strong>GridFS 分散式檔案儲存和 Redis 快取系統</li>
     *   <li><strong>業務層：</strong>檔案傳輸任務管理器、快取管理器、資料夾樹狀結構提供者</li>
     *   <li><strong>安全層：</strong>檔案安全掃描提供者（可選）和限流器配置</li>
     *   <li><strong>系統層：</strong>斷路器配置、事務操作器、JSON 序列化工具</li>
     * </ul>
     * <p>
     * <strong>設計模式：</strong>
     * <ul>
     *   <li><strong>依賴注入：</strong>透過 Spring 容器自動注入所有必要依賴</li>
     *   <li><strong>策略模式：</strong>支援多種檔案處理策略的動態選擇</li>
     *   <li><strong>模板模式：</strong>繼承父類的通用檔案操作方法</li>
     *   <li><strong>裝飾者模式：</strong>可選組件（如安全掃描）的動態裝配</li>
     * </ul>
     *
     * @param serverFileMetaRepository 伺服器檔案元資料資料庫操作介面，用於管理實體檔案資訊
     * @param userFileMetaRepository 用戶檔案元資料資料庫操作介面，用於管理用戶檔案關聯
     * @param redisProvider Redis 緩存提供者，用於檔案快取和效能最佳化
     * @param gridFsProvider GridFS 儲存提供者，用於分散式檔案儲存和檢索
     * @param transfersTasksManager 檔案傳輸任務管理器，用於處理檔案上傳下載任務
     * @param fileProperties 檔案相關設定屬性，包含上傳限制、儲存路徑等配置
     * @param circuitBreakerConfig 斷路器設定，用於系統穩定性和錯誤恢復
     * @param userRepository 用戶資料庫操作介面，用於用戶資訊查詢和驗證
     * @param userOnlineFileRepository 用戶線上檔案資料庫操作介面，雖然此服務不處理線上檔案，但需要支援檔案類型轉換
     * @param entityOperations R2DBC 實體操作介面，用於非阻塞式資料庫操作
     * @param fileTrashRecordRepository 檔案回收站記錄資料庫操作介面，用於檔案刪除和恢復功能
     * @param transactionalOperator 事務操作器，用於響應式事務管理
     * @param rateLimiterConfig 限流器設定，用於控制檔案操作頻率和保護系統資源
     * @param userFIleShareRecordRepository 用戶檔案分享記錄資料庫操作介面，用於檔案分享功能
     * @param objectMapper JSON 序列化工具，用於資料轉換和 API 回應格式化
     * @param cacheManager 緩存管理器，統一管理各種快取操作和策略
     * @param folderListTreeProvider 資料夾樹狀結構提供者（可選），用於優化檔案夾結構查詢效能
     * @param fileScanProvider 檔案安全掃描提供者（可選），用於上傳檔案的安全性檢查
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
