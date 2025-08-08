package xyz.dowob.filemanagement.service.serviceInterface;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.manager.CacheManager;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.FileScanProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.*;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.FileUploadResultBO;
import xyz.dowob.filemanagement.data.file.bo.UploadTaskBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dao.ServerFileMetaCountDAO;
import xyz.dowob.filemanagement.data.file.dao.UserFileMetaWithDataDAO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.data.file.po.FluxDataPO;
import xyz.dowob.filemanagement.data.file.po.ShareUserEditPO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.*;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.CacheRule;
import xyz.dowob.filemanagement.repostiory.*;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 檔案服務操作的抽象基底類別，提供檔案上傳、下載、管理等核心功能的通用實現。
 * <p>
 * 子類別可以繼承此類別來實現特定類型的檔案服務。提供檔案上傳與分塊處理、
 * 檔案下載與串流傳輸、檔案管理操作、權限與分享控制等主要功能。
 * <p>
 * 基於 Spring WebFlux 實現反應式非阻塞操作，支援高併發處理。
 * 內建檔案病毒掃描和安全驗證，整合快取機制和速率限制以優化效能。
 * <p>
 * 使用 MongoDB GridFS 作為檔案儲存後端，支援大檔案分塊上傳、重複檔案檢測、
 * 斷點續傳等進階功能。遵循 WebFlux 反應式錯誤處理模式，
 * 所有異常都透過響應式流傳播而非直接拋出。
 *
 * @author yuan
 * @version 1.0
 * @see FileService
 * @see GridFsProvider
 * @see TransfersTasksManager
 * @since 1.0
 */
@RequiredArgsConstructor
@RecordLevel(LogLevelEnum.DEBUG)
public abstract class AbstractFileService implements FileService {
    /**
     * 用戶根目錄檔案列表的快取鍵格式。
     * 當查詢用戶根目錄（folder ID = 0）的檔案列表時使用此格式，
     * 會包含用戶 ID 以確保不同用戶間的快取隔離。
     * 格式：fileList_folder:0_user:{userId}
     */
    private static final String ROOT_PAGE_KEY_FORMAT = "fileList_folder:0_user:%s";

    /**
     * 一般資料夾檔案列表的快取鍵格式。
     * 當查詢特定資料夾的檔案列表時使用此格式，
     * 直接使用資料夾 ID 作為快取鍵的一部分。
     * 格式：fileList_folder:{folderId}
     */
    private static final String GENERAL_PAGE_KEY_FORMAT = "fileList_folder:%s";

    /**
     * 伺服器檔案元資料儲存庫，負責管理實際檔案的元資料資訊。
     * 包含檔案大小、MD5 雜湊值、檔案類型、GridFS ID 等資訊，
     * 支援檔案去重和實際檔案內容的管理。
     */
    protected final ServerFileMetaRepository serverFileMetaRepository;

    /**
     * 使用者檔案元資料儲存庫，負責管理使用者的檔案資訊。
     * 包含檔案名稱、父資料夾、分享設定、上傳時間等使用者相關的檔案屬性，
     * 與伺服器檔案元資料形成多對一的關聯關係。
     */
    protected final UserFileMetaRepository userFileMetaRepository;

    /**
     * 使用者線上檔案儲存庫，負責管理可線上編輯的檔案資訊。
     * 支援檔案、試算表等可直接在瀏覽器中編輯的檔案類型，
     * 提供版本控制和協作編輯功能。
     */
    protected final UserOnlineFileRepository userOnlineFileRepository;

    /**
     * 使用者資料儲存庫，負責管理使用者帳戶相關資訊。
     * 包含使用者基本資料、儲存空間配額、權限設定等，
     * 用於檔案操作時的權限驗證和儲存配額管理。
     */
    protected final UserRepository userRepository;

    /**
     * Redis 緩存服務提供者，負責所有緩存相關操作。
     * 用於檔案列表緩存、上傳任務管理、速率限制、分散式鎖等功能，
     * 提供高效能的資料快取和任務狀態管理。
     */
    protected final RedisProvider redisProvider;

    /**
     * MongoDB GridFS 檔案儲存服務提供者。
     * 負責實際檔案內容的儲存、讀取和刪除操作，
     * 支援大檔案分塊儲存和串流讀取，適合處理各種檔案類型。
     */
    protected final GridFsProvider gridFsProvider;

    /**
     * 檔案安全掃描服務提供者。
     * 用於檔案上傳時的病毒掃描和安全檢查，
     * 防止惡意檔案進入系統，保障系統和使用者資料的安全。
     */
    protected final FileScanProvider fileScanProvider;

    /**
     * 檔案傳輸任務管理器。
     * 負責管理檔案上傳任務的狀態追蹤、進度更新和異常處理，
     * 支援大檔案分塊上傳和斷點續傳功能。
     */
    protected final TransfersTasksManager transfersTasksManager;

    /**
     * 檔案系統設定屬性。
     * 包含檔案上傳大小限制、分塊大小、上傳時間限制、
     * 備份保留時間等系統級設定參數。
     */
    protected final FileProperties fileProperties;

    /**
     * 斷路器設定。
     * 用於檔案操作的容錯性設計，當依賴服務（如 GridFS）出現故障時，
     * 自動開啟斷路器以防止系統級錯誤擴散。
     */
    protected final CircuitBreakerConfig circuitBreakerConfig;

    /**
     * 速率限制器設定。
     * 用於控制檔案操作的頻率，防止系統超載，
     * 特別在大檔案分塊處理時提供流量控制。
     */
    protected final RateLimiterConfig rateLimiterConfig;

    /**
     * 資料夾樹狀結構提供者。
     * 用於快速構建和維護使用者的資料夾樹狀結構，
     * 提供高效的路徑查詢和資料夾層級關係管理。
     */
    protected final FolderListTreeProvider folderListTreeProvider;

    /**
     * 檔案回收站記錄儲存庫。
     * 管理被刪除檔案的回收站資訊，包含刪除時間、預定永久刪除時間等，
     * 支援檔案還原和定時清理功能。
     */
    protected final FileTrashRecordRepository fileTrashRecordRepository;

    /**
     * R2DBC 資料庫實體操作器。
     * 提供非阻塞的資料庫操作能力，用於複雜的 SQL 查詢和批量操作，
     * 特別是當 Repository 接口無法滿足需求時的自定義查詢。
     */
    protected final R2dbcEntityOperations entityOperations;

    /**
     * 事務操作器。
     * 用於管理跨多個資料庫操作的事務一致性，
     * 確保檔案操作過程中的資料完整性和一致性。
     */
    protected final TransactionalOperator transactionalOperator;

    /**
     * 使用者檔案分享記錄儲存庫。
     * 管理檔案分享權限資訊，記錄哪些使用者可以存取特定檔案，
     * 支援檔案的共享和協作功能。
     */
    protected final UserFIleShareRecordRepository userFIleShareRecordRepository;

    /**
     * JSON 對象映射轉換器。
     * 用於 Java 對象與 JSON 之間的序列化和反序列化，
     * 特別在緩存和網路傳輸時需要轉換資料格式。
     */
    protected final ObjectMapper objectMapper;

    /**
     * 緩存管理器。
     * 統一管理系統中的各種緩存操作，包含檔案列表、使用者資料、檔案串流等，
     * 提供緩存策略的統一接口和緩存管理功能。
     */
    protected final CacheManager cacheManager;

    /**
     * 檔案分塊的大小（位元組）。
     * 由設定檔設定，用於大檔案上傳時的分塊處理，
     * 在初始化時從 {@link FileProperties} 中讀取。
     */
    protected Long CHUNK_SIZE;


    /**
     * 服務初始化方法，驗證並設定檔案處理的核心參數。
     * <p>
     * 此方法在 Spring 容器初始化完成後自動執行，負責：
     * <ul>
     *   <li>從設定檔讀取並驗證檔案分塊大小設定</li>
     *   <li>驗證檔案上傳時間限制設定</li>
     *   <li>確保所有關鍵參數都符合業務需求</li>
     * </ul>
     *
     * @throws IllegalArgumentException 當分塊大小小於等於 0 或上傳時間限制不為正數時
     */
    @PostConstruct
    public void init() {
        if (fileProperties.getUpload().getChunkSize().toBytes() <= 0) {
            throw new IllegalArgumentException("分塊大小必須大於0");
        }
        CHUNK_SIZE = fileProperties.getUpload().getChunkSize().toBytes();

        if (!fileProperties.getUpload().getMaxUploadDuration().isPositive()) {
            throw new IllegalArgumentException("單次最久上傳時間必須大於0");
        }
    }


    /**
     * 取得使用者的檔案列表，支援過濾條件和分頁查詢。
     * <p>
     * 根據提供的過濾條件查詢使用者的檔案，包含資料夾和一般檔案。
     * 對於根目錄和特定資料夾的查詢結果會進行快取以提升效能。
     * 查詢近期檔案時會自動調整分頁大小為系統設定值。
     * <p>
     * 處理流程：
     * <ol>
     *   <li>驗證並標準化查詢條件（分頁參數、資料夾 ID 等）</li>
     *   <li>從資料庫查詢符合條件的檔案元資料</li>
     *   <li>載入檔案分享資訊並組裝 DTO</li>
     *   <li>套用檔案類型和時間範圍過濾</li>
     *   <li>執行分頁處理並快取結果</li>
     * </ol>
     *
     * @param user          目標使用者資訊，用於權限驗證和查詢範圍限制
     * @param fileFilterDTO 檔案過濾條件，包含資料夾 ID、檔案類型、時間範圍、分頁參數等
     *
     * @return 包含檔案列表的分頁回應，透過 Mono 非同步回傳
     */
    @HideOverLength
    public Mono<PagedResponseDTO<UserFileListDTO>> getUserFileList(User user, FileFilterDTO fileFilterDTO) {
        String key = getUserFileListBaseKey(user.getId(), fileFilterDTO.getFolderId());

        if (fileFilterDTO.getPageSize() == null || fileFilterDTO.getPageSize() < 1) {
            fileFilterDTO.setPageSize(fileProperties.getGlobal().getPageSize());
        }

        if (fileFilterDTO.getPage() == null || fileFilterDTO.getPage() < 1) {
            fileFilterDTO.setPage(1);
        }

        if (Objects.equals(fileFilterDTO.getFolderId(), ReservedSearchIdEnum.RECENT_FILE_ID.getId())) {
            fileFilterDTO.setPageSize(fileProperties.getGlobal().getShowRecentFileCount());
        }

        Flux<UserFileMetaWithDataDAO> dataDAOs = userFileMetaRepository.getUserFileMetaWithDataDAO(user.getId(), fileFilterDTO, entityOperations);
        Flux<UserFileListDTO> getUserFileListDTOFlux = formatUnifiedDaoToDto(dataDAOs);

        if (fileFilterDTO.getFolderId() != null && fileFilterDTO.getFolderId() < 0) {
            return filterAndPageResponse(getUserFileListDTOFlux, fileFilterDTO);
        }

        CacheRule<UserFileListDTO> cacheRule = cacheManager.generateCacheRule(key, CacheProviderEnum.USER_FILE_LIST_CACHE);
        Flux<UserFileListDTO> listDTOFlux = cacheManager.runAndSetCache(key,
                                                                        UserFileListDTO.class,
                                                                        CacheProviderEnum.USER_FILE_LIST_CACHE,
                                                                        getUserFileListDTOFlux,
                                                                        Collections.singletonList(cacheRule)
        );
        return filterAndPageResponse(listDTOFlux, fileFilterDTO);
    }


    /**
     * 取得指定檔案的完整路徑節點列表，從根目錄到檔案所在位置。
     * <p>
     * 根據檔案的層級關係構建完整的路徑鏈，用於顯示檔案的位置導航。
     * 支援兩種路徑查詢方式：優先使用樹狀快取提供者，否則遞迴查詢資料庫。
     * <p>
     * 安全檢查：
     * 如果檔案不屬於當前使用者，會回傳僅包含根目錄的路徑以保護隱私。
     *
     * @param file 目標檔案的元資料，需包含父資料夾 ID 資訊
     * @param user 請求的使用者資訊，用於權限驗證
     *
     * @return 路徑節點列表，從檔案位置到根目錄的完整路徑，透過 Mono 非同步回傳
     */
    public Mono<List<FolderListTreeProvider.FolderNode>> getUserFilePaths(UserFileMetadata file, User user) {
        if (!Objects.equals(file.getUserId(), user.getId())) {
            List<FolderListTreeProvider.FolderNode> list = Collections.singletonList(new FolderListTreeProvider.FolderNode(null, "root"));
            return Mono.just(list);
        }

        return Mono.just(file).flatMap(userFileMetadata -> {
            if (folderListTreeProvider != null) {
                LogUnity.trace("使用 FolderListTreeProvider 獲取用戶: %s 檔案路徑", user.getUsername());
                List<FolderListTreeProvider.FolderNode> path = folderListTreeProvider.getPath(user.getId(), file.getId());
                return Mono.just(path);
            }

            LogUnity.trace("使用 UserFileMetaRepository 獲取用戶: %s 檔案路徑", user.getUsername());
            return Flux
                    .just(userFileMetadata)
                    .expand(metadata -> Optional
                            .ofNullable(metadata.getParentFolderId())
                            .map(folderId -> userFileMetaRepository.findById(folderId.toString()))
                            .orElse(Mono.empty()))
                    .map(FolderListTreeProvider.FolderNode::new)
                    .collectList()
                    .map(list -> {
                        if (file.getId() != 0L) {
                            list.add(new FolderListTreeProvider.FolderNode(null, "root"));
                        }
                        return list;
                    });
        }).switchIfEmpty(Mono.defer(() -> {
            if (file.getId() <= 0) {
                List<FolderListTreeProvider.FolderNode> list = Collections.singletonList(new FolderListTreeProvider.FolderNode(null, "root"));
                return Mono.just(list);
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, file.getId()));
        }));
    }


    /**
     * 搜尋使用者的檔案，支援關鍵字搜尋和多重過濾條件。
     * <p>
     * 根據提供的搜尋條件在使用者的所有檔案中執行全文搜尋，
     * 包含檔案名稱、檔案類型、建立時間等條件的組合查詢。
     * 搜尋結果不使用快取，確保資料的即時性。
     * <p>
     * 搜尋範圍：
     * <ul>
     *   <li>搜尋範圍僅限於當前使用者擁有的檔案</li>
     *   <li>支援跨資料夾的全域搜尋</li>
     *   <li>包含已分享給使用者的檔案</li>
     * </ul>
     *
     * @param user          目標使用者資訊，限制搜尋範圍
     * @param fileFilterDTO 搜尋條件，包含關鍵字、檔案類型、時間範圍、分頁參數等
     *
     * @return 符合搜尋條件的檔案列表分頁回應，透過 Mono 非同步回傳
     */
    public Mono<PagedResponseDTO<UserFileListDTO>> searchUserFile(User user, FileFilterDTO fileFilterDTO) {
        if (fileFilterDTO.getPageSize() == null || fileFilterDTO.getPageSize() < 1) {
            fileFilterDTO.setPageSize(fileProperties.getGlobal().getPageSize());
        }

        if (fileFilterDTO.getPage() == null || fileFilterDTO.getPage() < 1) {
            fileFilterDTO.setPage(1);
        }

        Flux<UserFileMetaWithDataDAO> dataFlux = userFileMetaRepository.findAllByUserIdAndFilterDTO(user.getId(), fileFilterDTO, entityOperations);
        return filterAndPageResponse(formatUnifiedDaoToDto(dataFlux), fileFilterDTO);
    }


    /**
     * 產生使用者檔案列表的快取鍵名。
     * <p>
     * 根據資料夾 ID 決定使用不同的快取鍵格式：
     * <ul>
     *   <li>根目錄（ID 為 null 或 0）：使用包含使用者 ID 的格式確保隔離</li>
     *   <li>一般資料夾：直接使用資料夾 ID 作為快取鍵</li>
     * </ul>
     *
     * @param userId   使用者的唯一識別碼
     * @param searchId 要查詢的資料夾 ID，null 時視為根目錄
     *
     * @return 對應的快取鍵名
     */
    @SkipRecord
    protected String getUserFileListBaseKey(Long userId, Long searchId) {
        Long chooseId = Objects.requireNonNullElse(searchId, ReservedSearchIdEnum.ROOT_FOLDER_ID.getId());
        if (chooseId.equals(ReservedSearchIdEnum.ROOT_FOLDER_ID.getId())) {
            return String.format(ROOT_PAGE_KEY_FORMAT, userId);
        }
        return String.format(GENERAL_PAGE_KEY_FORMAT, searchId);
    }


    /**
     * 將資料庫查詢結果轉換為用戶檔案列表 DTO 並補充分享資訊。
     * <p>
     * 將從資料庫查詢得到的統一 DAO 對象轉換為客戶端所需的 UserFileListDTO 格式。
     * 同時查詢並關聯每個檔案的分享用戶資訊，提供完整的檔案列表資訊。
     * <p>
     * 轉換流程：
     * <ul>
     *   <li>DAO 收集：將反應式流收集成列表進行批量處理</li>
     *   <li>檔案 ID 提取：從 DAO 對象中提取所有檔案 ID</li>
     *   <li>分享資訊查詢：一次性查詢所有檔案的分享用戶資訊</li>
     *   <li>資料合併：將檔案元資料與分享資訊合併</li>
     *   <li>DTO 轉換：將最終結果轉換為 UserFileListDTO 對象</li>
     * </ul>
     * <p>
     * 效能優化：
     * <ul>
     *   <li>批量查詢：使用 `findAllByFileIdIn` 一次性查詢所有分享記錄</li>
     *   <li>記憶體集結：使用 `collectMultimap` 快速組織關聯資料</li>
     *   <li>空值處理：提早回傳空流避免不必要的處理</li>
     * </ul>
     *
     * @param unifiedDaoFlux 包含檔案元資料和伺服器檔案資訊的統一 DAO 流
     *
     * @return 包含完整檔案資訊和分享用戶列表的 DTO 流
     */
    private Flux<UserFileListDTO> formatUnifiedDaoToDto(Flux<UserFileMetaWithDataDAO> unifiedDaoFlux) {
        return unifiedDaoFlux.collectList().flatMapMany(daoList -> {
            if (daoList.isEmpty()) {
                return Flux.empty();
            }

            Set<Long> userFileIds = daoList.stream().map(UserFileMetaWithDataDAO::getUfmId).collect(Collectors.toSet());

            Mono<Map<Long, Set<Long>>> shareUserMapMono = userFIleShareRecordRepository
                    .findAllByFileIdIn(userFileIds)
                    .collectMultimap(UserFileShareRecord::getFileId, UserFileShareRecord::getUserId)
                    .map(multimap -> {
                        Map<Long, Set<Long>> map = new HashMap<>();
                        multimap.forEach((fileId, userIds) -> map.put(fileId, new HashSet<>(userIds)));
                        return map;
                    })
                    .defaultIfEmpty(Collections.emptyMap());
            return shareUserMapMono.flatMapMany(shareUserMap -> Flux
                    .fromIterable(daoList)
                    .map(dao -> new UserFileListDTO(dao, shareUserMap.getOrDefault(dao.getUfmId(), Collections.emptySet()))));
        });
    }


    /**
     * 對檔案列表進行過濾和分頁處理，回傳結構化的分頁回應。
     * <p>
     * 接收檔案列表流和過濾條件，執行過濾和分頁操作，
     * 最終回傳包含分頁資訊的完整回應結構。
     * 這個方法為檔案列表查詢 API 提供最終的回應格式化。
     * <p>
     * 處理流程：
     * <ul>
     *   <li>過濾分頁：呼叫 `filterPageElements` 進行實際的過濾和分頁</li>
     *   <li>分頁計算：根據總數量和每頁大小計算總頁數</li>
     *   <li>回應封裝：將結果封裝成 `PagedResponseDTO` 物件</li>
     *   <li>元資料設定：設定當前頁數、每頁大小、總頁數等資訊</li>
     * </ul>
     * <p>
     * 分頁資訊包含：
     * <ul>
     *   <li>資料內容：當前頁的檔案列表</li>
     *   <li>總元素數：符合過濾條件的總檔案數量</li>
     *   <li>分頁參數：當前頁、每頁大小、總頁數</li>
     * </ul>
     *
     * @param fileListDTOFlux 待過濾和分頁的檔案列表流
     * @param fileFilterDTO   包含過濾條件和分頁參數的過濾器對象
     *
     * @return 包含分頁資訊和檔案列表的結構化回應
     */
    private Mono<PagedResponseDTO<UserFileListDTO>> filterAndPageResponse(Flux<UserFileListDTO> fileListDTOFlux, FileFilterDTO fileFilterDTO) {
        return filterPageElements(fileListDTOFlux, fileFilterDTO).flatMap(tuple -> {
            int pageSize = fileFilterDTO.getPageSize();
            int currentPage = fileFilterDTO.getPage();
            PagedResponseDTO<UserFileListDTO> pagedResponseDTO = new PagedResponseDTO<>();
            pagedResponseDTO.setData(tuple.getT2());
            pagedResponseDTO.setTotalElements(tuple.getT1());
            pagedResponseDTO.setPageSize(pageSize);
            pagedResponseDTO.setCurrentPage(currentPage);
            pagedResponseDTO.setTotalPages((int) Math.ceil((double) tuple.getT1() / pageSize));
            return Mono.just(pagedResponseDTO);
        });
    }


    /**
     * 執行檔案列表的過濾和分頁操作，回傳總數和分頁結果。
     * <p>
     * 這是檔案列表查詢的核心操作方法，負責根據使用者提供的過濾條件，
     * 對檔案列表進行篩選和分頁。支援按檔案類型和時間範圍進行過濾。
     * <p>
     * 過濾條件：
     * <ul>
     *   <li>檔案類型：支援多個檔案類型同時過濾</li>
     *   <li>時間範圍：按最後存取時間進行範圍篩選</li>
     *   <li>靈活組合：支援部分或全部過濾條件的組合</li>
     * </ul>
     * <p>
     * 時間過濾邏輯：
     * <ul>
     *   <li>起始時間：檔案最後存取時間不早於指定時間</li>
     *   <li>結束時間：檔案最後存取時間不晚於指定時間</li>
     *   <li>範圍支援：支援只設定起始或結束時間</li>
     * </ul>
     * <p>
     * 分頁計算：
     * <ul>
     *   <li>安全範圍：使用 `Math.min` 和 `Math.max` 確保索引不超出範圍</li>
     *   <li>空列表處理：正確處理空結果集的分頁操作</li>
     *   <li>索引計算：基於零的頁數轉換為列表索引</li>
     * </ul>
     *
     * @param flux          待過濾的檔案列表流
     * @param fileFilterDTO 包含過濾條件和分頁參數的過濾器對象
     *
     * @return 包含總數量和分頁結果的元組，透過 Mono<Tuple2<Integer, List<UserFileListDTO>>> 回傳
     */
    private Mono<Tuple2<Integer, List<UserFileListDTO>>> filterPageElements(Flux<UserFileListDTO> flux, FileFilterDTO fileFilterDTO) {
        List<FileEnum> type = fileFilterDTO.getTypes();
        LocalDateTime startTime = fileFilterDTO.getStartTime();
        LocalDateTime endTime = fileFilterDTO.getEndTime();
        Predicate<UserFileListDTO> timeFilter = userFileListDTO -> {
            if (startTime == null && endTime == null) {
                return true;
            }

            boolean matchesStart = true;
            boolean matchesEnd = true;

            if (startTime != null) {
                matchesStart = !userFileListDTO.getLastAccessTime().isBefore(startTime);
            }

            if (endTime != null) {
                matchesEnd = !userFileListDTO.getLastAccessTime().isAfter(endTime);
            }

            return matchesStart && matchesEnd;
        };

        Predicate<UserFileListDTO> typeFilter = userFileListDTO -> {
            if (type == null || type.isEmpty()) {
                return true;
            }
            return type.contains(userFileListDTO.getFileType());
        };

        int currentPage = fileFilterDTO.getPage();
        int pageSize = fileFilterDTO.getPageSize();

        return flux.filter(typeFilter).filter(timeFilter).collectList().map(list -> {
            int size = list.size();
            int start = Math.min(Math.max((currentPage - 1), 0) * pageSize, size);
            int end = Math.min(start + pageSize, size);
            List<UserFileListDTO> subList = list.subList(start, end);
            return Tuples.of(size, subList);
        });
    }


    /**
     * 處理檔案上傳請求，支援檔案去重和分塊上傳初始化。
     * <p>
     * 檢查上傳的檔案是否已存在於系統中（基於 MD5 雜湊值），
     * 如果檔案已存在則直接建立關聯而無需重複上傳，
     * 否則初始化分塊上傳任務供後續分塊上傳使用。
     * <p>
     * 處理邏輯：
     *
     * <ol>
     *   <li>檔案去重：檢查系統是否已有相同 MD5 的檔案</li>
     *   <li>直接關聯：已存在檔案時建立使用者關聯並更新儲存空間</li>
     *   <li>初始化上傳：新檔案時建立上傳任務並回傳任務資訊</li>
     * </ol>
     *
     * @param fileMetadataDTO 檔案元資料資訊，包含檔案名稱、大小、MD5、目標資料夾等
     * @param user            上傳使用者資訊，用於權限驗證和儲存空間檢查
     *
     * @return 上傳回應，包含是否完成、進度資訊、任務 ID 等，透過 Mono 非同步回傳
     */
    public Mono<UploadResponseDTO> uploadFile(FileMetadataDTO fileMetadataDTO, User user) {
        fileMetadataDTO.setUser(user);
        return serverFileMetaRepository.findByMd5(fileMetadataDTO.getMd5()).flatMap(existingFile -> {
            existingFile.getOwners().add(user.getId());
            existingFile.setLastAccessTime(LocalDateTime.now());

            return serverFileMetaRepository
                    .save(existingFile)
                    .then(associateUserFile(existingFile, fileMetadataDTO).flatMap(userFileMetaRepository::save))
                    .then(handleUserStorage(user, existingFile.getFileSize(), false))
                    .then(cleanUserListCache(user.getId(), fileMetadataDTO.getParentFolderId()))
                    .thenReturn(UploadResponseDTO
                                        .builder()
                                        .progress(100.0)
                                        .isSuccess(true)
                                        .isFinished(true)
                                        .message("上傳成功")
                                        .fileId(existingFile.getId())
                                        .build());
        }).switchIfEmpty(initialUpload(fileMetadataDTO));
    }


    /**
     * 處理檔案分塊上傳，支援大檔案的分片傳輸。
     * <p>
     * 接收單一檔案分塊並儲存到 GridFS，同時更新上傳進度。
     * 當所有分塊上傳完成後，會自動觸發檔案合併和安全檢查流程。
     * 支援重複上傳同一分塊而不會造成錯誤。
     * <p>
     * 處理流程：
     *
     * <ol>
     *   <li>驗證任務：檢查上傳任務是否存在且有效</li>
     *   <li>分塊處理：將分塊資料儲存到 GridFS</li>
     *   <li>進度更新：更新 Redis 中的上傳進度資訊</li>
     *   <li>完成檢查：若為最後一個分塊，觸發檔案合併流程</li>
     * </ol>
     *
     * @param uploadChunkDTO 分塊上傳資料，包含分塊索引、資料串流、任務 ID 等
     *
     * @return 上傳回應，包含當前進度、是否完成等資訊，透過 Mono 非同步回傳
     */
    public Mono<UploadResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO) {
        String transferTaskId = uploadChunkDTO.getTransferTaskId();
        String key = "upload_task:" + transferTaskId;
        String pendingChunkKey = key + ":pending_chunks";

        return Mono.defer(() -> redisProvider
                .getHashMap(key, "DTO")
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_UPLOAD_TASK, transferTaskId)))
                .flatMap(task -> redisProvider.isChunkSetPending(pendingChunkKey, uploadChunkDTO.getChunkIndex()).flatMap(isPending -> {
                    if (isPending) {
                        return processChunk(uploadChunkDTO, transferTaskId, key, pendingChunkKey);
                    }
                    return redisProvider.getHashMap(key, "uploaded_count").flatMap(uploadCount -> {
                        Long uploadCountLong = (Long) uploadCount;
                        double progress = (uploadCountLong.doubleValue() / uploadChunkDTO.getTotalChunks()) * 100.0;
                        String message = String.format("檔案分塊: %d 已上傳", uploadChunkDTO.getChunkIndex());
                        UploadResponseDTO responseDTO = UploadResponseDTO
                                .builder()
                                .chunkIndex(uploadChunkDTO.getChunkIndex())
                                .transferTaskId(transferTaskId)
                                .progress(progress)
                                .isSuccess(true)
                                .isFinished(false)
                                .message(message)
                                .totalChunks(uploadChunkDTO.getTotalChunks())
                                .build();

                        if (uploadCountLong.intValue() == uploadChunkDTO.getTotalChunks()) {
                            return fileCheck(transferTaskId, uploadChunkDTO.getTotalChunks()).flatMap(result -> {
                                responseDTO.setIsFinished(true);
                                responseDTO.setFileId(result.getUserFileId());
                                return Mono.just(responseDTO);
                            });
                        }
                        return Mono.just(responseDTO);
                    });
                })));
    }


    /**
     * 處理檔案下載請求，支援範圍下載和串流傳輸。
     * <p>
     * 從 GridFS 儲存系統中讀取檔案內容並以串流方式回傳，
     * 支援 HTTP Range 請求以實現部分下載和斷點續傳功能。
     * 同時會更新檔案的最後存取時間並檢查 MIME 類型。
     * <p>
     * 功能特點：
     *
     * <ul>
     *   <li>範圍下載：支援 HTTP Range 標頭指定下載範圍</li>
     *   <li>串流傳輸：使用反應式串流避免記憶體溢位</li>
     *   <li>快取機制：自動快取檔案串流以提升重複下載效能</li>
     *   <li>MIME 檢測：自動檢測並更新缺失的 MIME 類型資訊</li>
     * </ul>
     *
     * @param userFileMetadata 要下載的檔案元資料，包含檔案 ID 和基本資訊
     * @param user             請求下載的使用者資訊，用於權限驗證
     * @param optional         可選參數陣列，第一個參數為 HTTP Range 標頭內容
     *
     * @return 包含檔案資料串流的業務對象，透過 Mono 非同步回傳
     */
    public Mono<UserFileDataBO> downloadFile(UserFileMetadata userFileMetadata, User user, String... optional) {
        return Mono.defer(() -> {
            userFileMetadata.setLastAccessTime(LocalDateTime.now());
            userFileMetaRepository.save(userFileMetadata).subscribeOn(Schedulers.boundedElastic()).subscribe();
            return getByServerFileMetadataId(userFileMetadata.getServerFileId()).switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.USER_HAVE_NOT_EXIST_SERVER_FILE,
                                                                                                                               userFileMetadata.getServerFileId(),
                                                                                                                               userFileMetadata.getId()
            )));
        }).flatMap(serverFileMetadata -> Mono.just(new UserFileDataBO(serverFileMetadata, userFileMetadata)).flatMap(userFileDataBO -> {
            Mono<FluxDataPO<DataBuffer>> fluxDataPOMono = Mono.defer(() -> {
                FluxDataPO<DataBuffer> dataBufferPO = new FluxDataPO<>();
                Flux<DataBuffer> dataBufferFlux = gridFsProvider
                        .findFileById(new ObjectId(userFileDataBO.getGridFsId()))
                        .switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.GRIDFS_FILE_NOT_FOUND,
                                                                       userFileDataBO.getServerFileId()
                        )))
                        .flatMap(gridFsProvider::getResource)
                        .flatMapMany(ReactiveGridFsResource::getDownloadStream)
                        .share();
                dataBufferPO.setTFlux(dataBufferFlux);
                return Mono.just(dataBufferPO);
            });

            return cacheManager
                    .runAndSetCache(userFileDataBO.getGridFsId(),
                                    FluxDataPO.class,
                                    CacheProviderEnum.FILE_STREAM_CACHE,
                                    fluxDataPOMono,
                                    Collections.singletonList(cacheManager.generateCacheRule(userFileDataBO.getGridFsId(),
                                                                                             CacheProviderEnum.FILE_STREAM_CACHE
                                    ))
                    )
                    .flatMap(dataBufferPO -> {
                        long[] range = getRangeFromHeader(optional[0], userFileDataBO.getFileSize());
                        FluxDataPO<DataBuffer> dataBuffer = new FluxDataPO<>();
                        Flux<DataBuffer> dataBufferFlux = dataBuffer.formatAndSet(dataBufferPO.getTFlux(), DataBuffer.class);

                        return Mono.defer(() -> {
                            if (userFileDataBO.getMimeType() == null) {
                                userFileDataBO.setDataBufferFlux(dataBufferFlux);
                                return updateUserFileMetaMimeType(serverFileMetadata, userFileDataBO);
                            }
                            return Mono.just(userFileDataBO);
                        }).doOnNext(fileDataBO -> fileDataBO.setDataBufferFlux(streamFileFromGridFS(dataBufferFlux, range[0], range[1])));
                    });
        }));
    }


    /**
     * 永久刪除檔案，包含使用者關聯和實際檔案內容。
     * <p>
     * 從系統中永久移除檔案，包括使用者的檔案關聯記錄。
     * 如果檔案只有一個擁有者，會同時刪除伺服器上的實際檔案內容。
     * 會自動更新使用者的儲存空間使用量並清除相關快取。
     * <p>
     * 刪除邏輯：
     *
     * <ul>
     *   <li>多擁有者檔案：僅移除當前使用者的關聯，保留實際檔案</li>
     *   <li>單擁有者檔案：同時刪除使用者關聯和實際檔案內容</li>
     *   <li>儲存空間：自動扣除已刪除檔案占用的空間</li>
     *   <li>快取清理：清除相關的檔案列表快取</li>
     * </ul>
     *
     * @param userFileMetadata 要刪除的檔案元資料
     * @param user             執行刪除的使用者資訊，用於權限驗證
     *
     * @return 刪除完成的信號，透過 Mono<Void> 回傳
     */
    public Mono<Void> deleteFile(UserFileMetadata userFileMetadata, User user) {
        return updateOwner(Collections.singletonList(userFileMetadata), user.getId()).then(Mono.defer(() -> {
            Mono<Void> deleteFile = Mono.empty();
            if (userFileMetadata.getServerFileId() != null) {
                deleteFile = calculateFileSize(user, Collections.singletonList(userFileMetadata.getServerFileId()));
            }
            return deleteFile.then(cleanUserListCache(user.getId(), userFileMetadata.getParentFolderId()).then(userFileMetaRepository.deleteById(
                    userFileMetadata.getId().toString())));
        }));
    }


    /**
     * 編輯檔案的屬性資訊，包含名稱、位置、分享設定等。
     * <p>
     * 更新檔案的基本屬性，如檔案名稱、父資料夾、分享類型、我的最愛狀態等。
     * 同時處理檔案分享權限的新增、移除和更新操作。
     * 操作完成後會清除相關的快取以確保資料一致性。
     * <p>
     * 可編輯屬性：
     *
     * <ul>
     *   <li>檔案名稱：更新檔案顯示名稱</li>
     *   <li>父資料夾：移動檔案到其他資料夾</li>
     *   <li>分享設定：修改檔案的分享類型和權限</li>
     *   <li>我的最愛：設定或取消我的最愛標記</li>
     *   <li>分享用戶：新增、移除或更新分享給特定用戶的權限</li>
     * </ul>
     *
     * @param fileEditBO 檔案編輯資料物件，包含要更新的屬性和分享設定
     * @param user       執行編輯的使用者資訊，用於權限驗證
     *
     * @return 編輯完成的信號，透過 Mono<Void> 回傳
     */
    public Mono<Void> editFile(FileEditBO fileEditBO, User user) {
        FileEditDTO fileEditDTO = fileEditBO.getFileEditDTO();
        UserFileMetadata userFileMetadata = fileEditBO.getUserFileMetadata();
        List<Long> cleanCacheList = new ArrayList<>(List.of(userFileMetadata.getParentFolderId(), fileEditDTO.getParentFolderId()));
        Mono<UserFileMetadata> processShareUserMono = processShareUser(Collections.singletonList(userFileMetadata), fileEditDTO).next();
        Mono<UserFileMetadata> processFileMono = Mono.defer(() -> {
            userFileMetadata.setFilename(fileEditDTO.getFilename());
            userFileMetadata.setParentFolderId(fileEditDTO.getParentFolderId());
            userFileMetadata.setLastAccessTime(LocalDateTime.now());

            FileShareTypeEnum shareType = Objects.requireNonNullElse(fileEditDTO.getShareType(), userFileMetadata.getShareType());
            userFileMetadata.setShareType(shareType);

            Boolean isStar = Objects.requireNonNullElse(fileEditDTO.getIsStar(), userFileMetadata.getIsStar());
            userFileMetadata.setIsStar(isStar);

            return userFileMetaRepository.save(userFileMetadata);
        });

        return Mono.zip(processShareUserMono, processFileMono).then(cleanUserListCache(user.getId(), cleanCacheList.toArray(new Long[0])));
    }


    /**
     * 處理檔案分享設定的變更，支援新增、刪除和更新分享用戶。
     * <p>
     * 根據編輯要求中的分享用戶清單，執行對應的新增、移除或更新操作。
     * 此方法會與資料庫中的現有分享記錄進行比對，執行必要的更新。
     * <p>
     * 處理類型：
     * <p>
     * - **REMOVE**：移除特定用戶的分享權限
     * - **UPDATE**：更新特定用戶的分享設定
     * - **新用戶**：自動新增分享記錄
     *
     * @param userFileMetadatas 要處理的檔案元資料集合
     * @param fileEditDTO       包含分享設定變更的編輯資料
     *
     * @return 處理後的檔案元資料串流
     */
    protected Flux<UserFileMetadata> processShareUser(Collection<UserFileMetadata> userFileMetadatas, FileEditDTO fileEditDTO) {
        List<UserFileShareRecord> removeRecords = new ArrayList<>();
        List<UserFileShareRecord> editRecords = new ArrayList<>();
        return Flux.fromIterable(userFileMetadatas).flatMap(userFileMetadata -> {
            Map<Long, ShareUserEditPO.EditTypeEnum> editUsers = fileEditDTO
                    .getShareUsers()
                    .stream()
                    .collect(Collectors.toMap(ShareUserEditPO::getUserId, ShareUserEditPO::getEditType));
            if (editUsers.isEmpty()) {
                return Mono.just(userFileMetadata);
            }

            return userFIleShareRecordRepository.findAllByUserIdInAndFileId(editUsers.keySet(), userFileMetadata.getId()).flatMap(record -> {
                ShareUserEditPO.EditTypeEnum editType = editUsers.remove(record.getUserId());
                switch (editType) {
                    case REMOVE -> removeRecords.add(record);
                    case UPDATE -> editRecords.add(record);
                }
                return Mono.just(userFileMetadata);
            }).then(Mono.defer(() -> {
                if (!editUsers.isEmpty()) {
                    editUsers.forEach((userId, editType) -> {
                        UserFileShareRecord record = new UserFileShareRecord(userId, userFileMetadata.getId());
                        editRecords.add(record);
                    });
                }
                return Mono.just(userFileMetadata);
            }));
        }).collectList().flatMapMany(metadataList -> {
            if (removeRecords.isEmpty() && editRecords.isEmpty()) {
                return Flux.fromIterable(metadataList);
            }
            return Mono
                    .when(userFIleShareRecordRepository.deleteAll(removeRecords), userFIleShareRecordRepository.saveAll(editRecords))
                    .thenMany(Flux.fromIterable(metadataList));
        });
    }


    /**
     * 更新伺服器檔案的擁有者清單，用於檔案刪除後的清理作業。
     * <p>
     * 當使用者刪除檔案時，檢查每個伺服器檔案是否只剩該使用者擁有。
     * 如果伺服器檔案只有一個擁有者，則從擁有者清單中移除該使用者，
     * 這通常發生在檔案即將被永久刪除時。
     * <p>
     * 處理邏輯：
     * <p>
     * <ol>
     *   <li>統計每個伺服器檔案被該使用者關聯的次數</li>
     *   <li>找出只被關聯一次的檔案（即只有該使用者擁有）</li>
     *   <li>從這些檔案的擁有者清單中移除該使用者 ID</li>
     * </ol>
     *
     * @param userFileList 要處理的使用者檔案元資料列表
     * @param userId       要從擁有者清單中移除的使用者 ID
     *
     * @return 更新完成的信號
     */
    protected Mono<Void> updateOwner(List<UserFileMetadata> userFileList, Long userId) {
        return Mono.defer(() -> {
            List<Long> serverFileIds = userFileList.stream().map(UserFileMetadata::getServerFileId).filter(Objects::nonNull).distinct().toList();
            if (userFileList.isEmpty() || serverFileIds.isEmpty()) {
                return Mono.empty();
            }
            return userFileMetaRepository
                    .countByServerFileIdInAndUserId(serverFileIds, userId, entityOperations)
                    .collectList()
                    .flatMap(serverFileMetaCountDaoList -> {
                        Set<Long> serverFileIdList = serverFileMetaCountDaoList
                                .stream()
                                .filter(serverFileMetaCountDAO -> serverFileMetaCountDAO.count() == 1)
                                .map(ServerFileMetaCountDAO::serverFileId)
                                .collect(Collectors.toSet());
                        if (serverFileIdList.isEmpty()) {
                            return Mono.empty();
                        }
                        return serverFileMetaRepository.findAllByIdIn(serverFileIdList).collectList().flatMap(serverFileMetadataList -> {
                            serverFileMetadataList.forEach(serverFileMetadata -> {
                                serverFileMetadata.getOwners().remove(userId);
                                serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                            });
                            return serverFileMetaRepository.saveAll(serverFileMetadataList).then();
                        });
                    });
        });
    }


    /**
     * 計算並更新使用者的儲存空間使用量（刪除操作專用）。
     * <p>
     * 根據要刪除的伺服器檔案 ID 列表，計算總檔案大小並扣除使用者的儲存空間使用量。
     * 此方法專門用於檔案刪除操作，會自動處理重複檔案的大小計算。
     * <p>
     * 計算邏輯：
     * <p>
     * <ol>
     *   <li>統計每個伺服器檔案 ID 的出現次數（處理重複刪除）</li>
     *   <li>查詢每個檔案的實際大小</li>
     *   <li>計算總共要扣除的儲存空間</li>
     *   <li>更新使用者的儲存空間使用量</li>
     * </ol>
     *
     * @param user          要更新儲存空間的使用者
     * @param serverFileIds 要刪除的伺服器檔案 ID 列表
     *
     * @return 更新完成的信號
     */
    protected Mono<Void> calculateFileSize(User user, List<Long> serverFileIds) {
        if (serverFileIds.isEmpty()) {
            return Mono.empty();
        }
        Map<Long, Long> serverFileIdMap = new ConcurrentHashMap<>();
        serverFileIds.forEach(serverFileId -> serverFileIdMap.put(serverFileId, serverFileIdMap.getOrDefault(serverFileId, 0L) + 1));

        return serverFileMetaRepository
                .findAllByIdIn(serverFileIdMap.keySet())
                .map(serverFileMetadata -> serverFileMetadata.getFileSize() * serverFileIdMap.get(serverFileMetadata.getId()))
                .reduce(0L, Long::sum)
                .flatMap(totalSize -> handleUserStorage(user, totalSize, true));
    }


    /**
     * 解析 HTTP Range 標頭並轉換為檔案讀取範圍。
     * <p>
     * 支援 HTTP 範圍請求的解析，將標頭中的 "bytes=start-end" 格式
     * 轉換為數值範圍陣列，用於支援部分檔案下載和視頻串流。
     * 這個方法被標記為 @SkipRecord，避免在日誌中記錄敏感資訊。
     * <p>
     * 支援的範圍格式：
     * 完整範圍：bytes=0-1023 （從 0 位元組至 1023 位元組）
     * 從指定位置開始：bytes=500- （從 500 位元組至檔案結尾）
     * 預設範圍：沒有 Range 標頭時回傳完整檔案範圍
     * <p>
     * 解析邏輯：
     * <ol>
     *   <li>標頭驗證：檢查是否以 "bytes=" 開頭</li>
     *   <li>範圍分割：使用 "-" 作為分隔字元</li>
     *   <li>數值驗證：使用正則表達式驗證結束位置</li>
     *   <li>範圍檢查：確保結束位置不超過檔案大小</li>
     * </ol>
     * <p>
     * 回傳值格式：回傳長度為 2 的 long 陣列，[0] 為起始位置（包含），[1] 為結束位置（包含），-1 表示至檔案結尾
     *
     * @param rangeHeader HTTP Range 標頭字串，格式為 "bytes=start-end"
     * @param fileSize    檔案總大小，用於範圍驗證
     *
     * @return 包含起始和結束位置的陣列，格式為 [start, end]
     */
    @SkipRecord
    private long[] getRangeFromHeader(String rangeHeader, long fileSize) {
        long start = 0;
        long end = -1;
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            start = Long.parseLong(ranges[0]);
            if (ranges.length > 1 && !ranges[1].isEmpty() && ranges[1].matches("\\d+")) {
                long endRange = Long.parseLong(ranges[1]);
                end = endRange < fileSize ? endRange : -1;
            }
        }
        return new long[]{start, end};
    }


    /**
     * 自動檢測並更新檔案的 MIME 類型（向後相容性補丁）。
     * <p>
     * 針對舊版本系統中缺少 MIME 類型資訊的檔案，自動進行檢測和補全。
     * 通過分析檔案內容和檔案名稱來推斷正確的 MIME 類型，
     * 並更新伺服器檔案元資料以供後續使用。
     * <p>
     * 檢測邏輯：
     * <p>
     * - 如果 MIME 類型已存在，直接回傳原始資料
     * - 如果缺少 MIME 類型，分析檔案內容進行檢測
     * - 檢測完成後更新伺服器檔案元資料並保存
     *
     * @param serverFileMetadata 要更新的伺服器檔案元資料
     * @param fileDataBO         包含檔案資料串流的業務對象
     *
     * @return 更新 MIME 類型後的檔案資料對象
     */
    protected Mono<UserFileDataBO> updateUserFileMetaMimeType(ServerFileMetadata serverFileMetadata, UserFileDataBO fileDataBO) {
        if (serverFileMetadata.getMimeType() != null) {
            return Mono.just(fileDataBO);
        }
        return FileEnum.getMediaType(fileDataBO.getDataBufferFlux(), fileDataBO.getFilename()).flatMap(record -> {
            serverFileMetadata.setMimeType(record.mimeType());
            serverFileMetadata.setLastAccessTime(LocalDateTime.now());

            fileDataBO.setDataBufferFlux(record.dataBufferFlux());
            fileDataBO.setMimeType(record.mimeType());
            return serverFileMetaRepository.save(serverFileMetadata).subscribeOn(Schedulers.boundedElastic()).thenReturn(fileDataBO);
        });
    }


    /**
     * 從 GridFS 資料串流中提取指定範圍的資料。
     * <p>
     * 根據提供的起始和結束位置，從完整的檔案資料串流中提取所需的範圍資料。
     * 支援 HTTP Range 請求，用於實現檔案的部分下載和串流播放功能。
     * <p>
     * 範圍處理：
     * <p>
     * - **跳過前段**：跳過起始位置之前的所有資料
     * - **截取後段**：如果指定了結束位置，截取到該位置為止
     * - **完整後段**：如果未指定結束位置（-1），回傳剩餘所有資料
     *
     * @param dataBufferFlux 完整的檔案資料串流
     * @param start          開始位置（位元組偏移）
     * @param end            結束位置（位元組偏移），-1 表示到檔案末尾
     *
     * @return 指定範圍的資料串流
     */
    @SkipRecord
    protected Flux<DataBuffer> streamFileFromGridFS(Flux<DataBuffer> dataBufferFlux, long start, long end) {
        return Flux.defer(() -> {
            Flux<DataBuffer> skippedFlux = DataBufferUtils.skipUntilByteCount(dataBufferFlux, start);
            if (end == -1L || end == Long.MAX_VALUE) {
                return skippedFlux;
            }
            return DataBufferUtils.takeUntilByteCount(skippedFlux, end - start + 1);
        });
    }


    /**
     * 建立使用者與伺服器檔案的關聯記錄。
     * <p>
     * 在檔案上傳成功後，建立使用者檔案元資料記錄來關聯使用者與實際的伺服器檔案。
     * 這使得多個使用者可以共享同一個實際檔案，實現檔案去重功能。
     * <p>
     * 設定的屬性：
     *
     * <ul>
     *   <li>使用者 ID、伺服器檔案 ID、檔案類型</li>
     *   <li>使用者指定的檔案名稱和父資料夾</li>
     *   <li>上傳時間和最後存取時間</li>
     * </ul>
     *
     * @param serverFileMetadata 要關聯的伺服器檔案元資料
     * @param fileMetadataDTO    包含使用者設定的檔案資訊
     *
     * @return 新建立的使用者檔案元資料記錄
     */
    @SkipRecord
    protected Mono<UserFileMetadata> associateUserFile(ServerFileMetadata serverFileMetadata, FileMetadataDTO fileMetadataDTO) {
        UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setUserId(fileMetadataDTO.getUser().getId());
        userFileMetadata.setServerFileId(serverFileMetadata.getId());
        userFileMetadata.setFileType(serverFileMetadata.getFileType());
        userFileMetadata.setFilename(fileMetadataDTO.getFilename());
        userFileMetadata.setParentFolderId(fileMetadataDTO.getParentFolderId());
        userFileMetadata.setUploadTime(LocalDateTime.now());
        userFileMetadata.setLastAccessTime(LocalDateTime.now());
        return userFileMetaRepository.save(userFileMetadata);
    }


    /**
     * 管理使用者儲存空間配額，支援增加和減少儲存使用量。
     * <p>
     * 當使用者上傳或刪除檔案時，更新其儲存空間使用量統計。
     * 同時檢查是否超出儲存配額限制，並在操作完成後清理相關快取。
     * <p>
     * 儲存管理流程：
     * <ol>
     *   <li>重新從資料庫獲取最新的使用者資訊</li>
     *   <li>根據操作類型計算新的儲存使用量</li>
     *   <li>驗證是否超出使用者的儲存配額</li>
     *   <li>更新使用者的儲存使用量統計</li>
     *   <li>清除相關的使用者快取資料</li>
     * </ol>
     * <p>
     * 儲存計算規則：
     * 新增檔案時，儲存使用量 = 目前使用量 + 檔案大小
     * 刪除檔案時，儲存使用量 = max(目前使用量 - 檔案大小, 0)
     * 配額檢查只在增加使用量時執行，且配額不為 -1（無限制）
     * <p>
     * 錯誤處理：當超出儲存配額時，拋出 ValidationException 並提供配額限制大小、目前使用量、試圖新增的檔案大小
     *
     * @param user     要更新儲存統計的使用者
     * @param fileSize 檔案大小（位元組），正數表示新增，負數表示減少
     * @param isDelete 是否為刪除操作，如果為 true，則從使用量中減去檔案大小
     *
     * @return 儲存管理完成的信號，透過 Mono<Void> 回傳
     */
    private Mono<Void> handleUserStorage(User user, long fileSize, boolean isDelete) {
        return Mono.defer(() -> userRepository.findById(user.getId()).flatMap(userEntity -> {
            long newStorageUsed = isDelete ? Math.max(userEntity.getUsedStorage() - fileSize, 0) : userEntity.getUsedStorage() + fileSize;
            if (user.getStorageLimit() != -1 && newStorageUsed > user.getStorageLimit()) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.STORAGE_LIMIT_EXCEEDED,
                                                          ByteEnum.toReadableSize(user.getStorageLimit()),
                                                          ByteEnum.toReadableSize(user.getUsedStorage()),
                                                          ByteEnum.toReadableSize(fileSize)
                ));
            }
            userEntity.setUsedStorage(newStorageUsed);

            List<String> keys = Arrays.asList(user.getId().toString(), userEntity.getUsername());
            Mono<Void> cleanCache = cacheManager.deleteCaches(keys, CacheProviderEnum.USER_CACHE);
            return userRepository.save(userEntity).then(cleanCache);
        }));
    }


    /**
     * 清除使用者檔案列表的相關快取。
     * <p>
     * 當檔案列表發生異動時（新增、刪除、移動、編輯），需要清除相關的快取
     * 以確保使用者下次查詢時能取得最新的檔案列表資料。
     * <p>
     * 清除策略：
     * <p>
     * - 對每個受影響的資料夾產生對應的快取鍵名
     * - 批次清除所有相關的快取項目
     * - 自動去重複的資料夾 ID
     *
     * @param userId    使用者 ID，用於產生使用者專屬的快取鍵
     * @param folderIds 受影響的資料夾 ID 陣列，null 值會被視為根目錄
     *
     * @return 清除完成的信號
     */
    protected Mono<Void> cleanUserListCache(Long userId, Long... folderIds) {
        List<String> keys = Arrays.stream(folderIds).distinct().map(folderId -> getUserFileListBaseKey(userId, folderId)).toList();
        return cacheManager.deleteCaches(keys, CacheProviderEnum.USER_FILE_LIST_CACHE);
    }


    /**
     * 初始化新檔案的分塊上傳任務。
     * <p>
     * 當檔案不存在於系統中時，建立新的上傳任務以支援分塊上傳。
     * 此方法會產生唯一的任務 ID，並在 Redis 中設定相關的任務狀態和分塊追蹤資訊。
     * <p>
     * 初始化內容：
     * <p>
     * - 產生唯一的上傳任務 ID
     * - 在任務管理器中註冊任務
     * - 設定 Redis 中的任務資料、計數器和待上傳分塊集合
     * - 計算總分塊數和分塊大小
     *
     * @param fileMetadataDTO 要上傳的檔案元資料，包含大小、MD5、檔案名等資訊
     *
     * @return 上傳任務初始化回應，包含任務 ID、分塊資訊等
     */
    protected Mono<UploadResponseDTO> initialUpload(FileMetadataDTO fileMetadataDTO) {
        String uploadTaskId = UUID.randomUUID().toString();
        return transfersTasksManager.registerUploadTask(fileMetadataDTO, uploadTaskId).then(Mono.defer(() -> {
            UploadTaskBO task = fileMetadataDTO.formatToTransferTask(uploadTaskId, "初始化任務成功");
            String key = "upload_task:" + uploadTaskId;
            int totalChunks = getTotalChunks(fileMetadataDTO.getFileSize());

            return redisProvider
                    .setHashMap(key, "DTO", task, fileProperties.getUpload().getMaxUploadDuration())
                    .then(redisProvider.setHashMap(key, "uploaded_count", 0, fileProperties.getUpload().getMaxUploadDuration()))
                    .then(redisProvider.setHashMap(key, "total_chunks", totalChunks, fileProperties.getUpload().getMaxUploadDuration()))
                    .then(redisProvider.generateChunkSet(key + ":pending_chunks", totalChunks))
                    .thenReturn(UploadResponseDTO
                                        .builder()
                                        .transferTaskId(uploadTaskId)
                                        .totalChunks(totalChunks)
                                        .chunkSize(CHUNK_SIZE)
                                        .progress(0.0)
                                        .isSuccess(true)
                                        .isFinished(false)
                                        .message("初始化任務成功")
                                        .build());
        }));
    }


    /**
     * 計算檔案需要分成多少個分塊上傳。
     * <p>
     * 根據檔案大小和系統設定的分塊大小，計算出需要的總分塊數。
     * 使用向上取整確保最後一個不滿分塊也會被計算在內。
     *
     * @param fileSize 檔案大小（位元組）
     *
     * @return 需要的總分塊數
     */
    @SkipRecord
    protected int getTotalChunks(long fileSize) {
        return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
    }


    /**
     * 將多個位元組陣列合併成單一位元組陣列。
     * <p>
     * 用於檔案分塊上傳完成後，將所有分塊資料合併成完整的檔案內容。
     * 使用 ByteBuffer 進行高效率的位元組合併操作，並在完成後清空輸入清單以釋放記憶體。
     * <p>
     * 合併過程：
     * <p>
     * <ol>
     *   <li>計算所有分塊的總長度</li>
     *   <li>分配對應大小的 ByteBuffer</li>
     *   <li>依序將所有分塊資料寫入 Buffer</li>
     *   <li>轉換為位元組陣列並清空輸入清單</li>
     * </ol>
     *
     * @param byteArrays 要合併的分塊資料清單
     *
     * @return 合併後的完整位元組陣列
     */
    @SkipRecord
    protected Mono<byte[]> combineBytes(List<byte[]> byteArrays) {
        return Mono.fromCallable(() -> {
            int totalLength = byteArrays.stream().mapToInt(bytes -> bytes.length).sum();
            ByteBuffer buffer = ByteBuffer.allocate(totalLength);
            byteArrays.forEach(buffer::put);
            return buffer.array();
        }).doFinally(signalType -> byteArrays.clear());
    }


    /**
     * 驗證上傳檔案的完整性和正確性。
     * <p>
     * 對合併後的檔案資料進行完整性檢查，包含檔案大小和 MD5 雜湊值驗證。
     * 如果驗證失敗，會自動更新傳輸任務狀態為失敗並傳播相應的錯誤。
     * <p>
     * 驗證項目：
     * <p>
     * <ol>
     *   <li><strong>檔案大小驗證</strong>：比對實際大小與預期大小</li>
     *   <li><strong>MD5 雜湊驗證</strong>：計算並比對檔案的 MD5 值</li>
     *   <li><strong>失敗處理</strong>：驗證失敗時更新任務狀態並拋出錯誤</li>
     * </ol>
     *
     * @param combinedBytes 合併後的完整檔案資料
     * @param uploadTaskBO  包含預期檔案資訊的上傳任務對象
     *
     * @return 驗證成功時回傳空信號，失敗時透過錯誤信號傳播異常
     */
    protected Mono<Void> checkFileStatus(byte[] combinedBytes, UploadTaskBO uploadTaskBO) {
        return Mono.defer(() -> {
            if (combinedBytes.length != uploadTaskBO.getFileSize()) {
                return transfersTasksManager
                        .updateTransfersTask(uploadTaskBO.getMd5(),
                                             uploadTaskBO.getTransferTaskId(),
                                             TransfersStatusEnum.FAILED,
                                             "檔案大小不匹配",
                                             null,
                                             true
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .then(Mono.error(new ProcessException(ProcessException.ErrorCode.FILE_SIZE_NOT_MATCH)));
            }

            String computedChunkMd5 = DigestUtils.md5Hex(combinedBytes);
            if (!uploadTaskBO.getMd5().equals(computedChunkMd5)) {
                return transfersTasksManager
                        .updateTransfersTask(uploadTaskBO.getMd5(),
                                             uploadTaskBO.getTransferTaskId(),
                                             TransfersStatusEnum.FAILED,
                                             "MD5校驗失敗",
                                             null,
                                             true
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .then(Mono.error(new ProcessException(ProcessException.ErrorCode.MD5_NOT_MATCH)));
            }
            return Mono.empty();
        });
    }


    /**
     * 從 GridFS 讀取並合併所有檔案分塊。
     * <p>
     * 按順序從 GridFS 中讀取所有分塊檔案，並合併成完整的檔案資料。
     * 整合了速率限制、斷路器和重試機制以確保穩定性和效能。
     * <p>
     * 安全與效能機制：
     * <p>
     * - **速率限制**：防止過度頻繁的 GridFS 讀取操作
     * - **斷路器**：當 GridFS 讀取頻繁失敗時自動中斷操作
     * - **重試機制**：對於速率限制錯誤自動重試，最多 5 次
     * - **順序保證**：確保分塊按正確順序合併
     *
     * @param transferTaskId 上傳任務 ID，用於定位分塊檔案
     * @param totalChunks    總分塊數量
     *
     * @return 合併後的完整檔案資料
     */
    protected Mono<byte[]> combineChunks(String transferTaskId, int totalChunks) {
        RateLimiter rateLimiter = RateLimiter.of("combineChunk", this.rateLimiterConfig);
        CircuitBreaker chunkCircuitBreaker = CircuitBreaker.of("gridFsChunkReader", this.circuitBreakerConfig);
        record ChunkData(int index, byte[] data) {
            static Comparator<ChunkData> comparator() {
                return Comparator.comparingInt(ChunkData::index);
            }
        }
        Retry retryPolicy = Retry
                .backoff(5, Duration.ofSeconds(1))
                .filter(e -> e instanceof LimitationException)
                .maxBackoff(Duration.ofSeconds(5))
                .jitter(0.3);


        return Mono.defer(() -> {
            Flux<ChunkData> chunkFiles = Flux.range(1, totalChunks).flatMapSequential(index -> {
                Mono<ChunkData> chunkOperation = this.gridFsProvider
                        .findFileByFileName(transferTaskId + "_chunk_" + index)
                        .flatMap(this.gridFsProvider::getResource)
                        .flatMap(resource -> DataBufferUtils.join(resource.getDownloadStream()).onErrorResume(e -> {
                            String taskId = transferTaskId + "_chunk_" + index;
                            return Mono.error(new ProcessException(ProcessException.ErrorCode.CANNOT_GET_FILE_STREAM, e, taskId));
                        }))
                        .map(dataBuffer -> {
                            try {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                return new ChunkData(index, bytes);
                            } finally {
                                DataBufferUtils.release(dataBuffer);
                            }
                        })
                        .transformDeferred(CircuitBreakerOperator.of(chunkCircuitBreaker));

                return Mono.defer(() -> {
                    if (rateLimiter.acquirePermission()) {
                        return chunkOperation;
                    }
                    return Mono.error(new LimitationException(LimitationException.ErrorCode.FILE_CHUNK_EXCEED_LIMIT,
                                                              "上傳檔案超出了請求限制，請稍後再試"
                    ));
                }).retryWhen(retryPolicy);
            });
            return chunkFiles.sort(ChunkData.comparator()).map(ChunkData::data).collectList().flatMap(this::combineBytes);
        });
    }


    /**
     * 檢查上傳檔案的完整性和安全性。
     * <p>
     * 執行綜合檔案驗證流程，包括檔案完整性檢查和安全掃描。
     * 當所有檢查通過後，會繼續處理檔案儲存；如果任何檢查失敗，
     * 會自動清理暫存資料並更新傳輸任務狀態為失敗。
     * <p>
     * 檢查流程：
     * <p>
     * <ol>
     *   <li><strong>檔案重建</strong>：從 GridFS 讀取並合併所有分塊</li>
     *   <li><strong>完整性驗證</strong>：檢查檔案大小和 MD5 雜湊值</li>
     *   <li><strong>安全掃描</strong>：執行病毒檢測（如果啟用）</li>
     *   <li><strong>後續處理</strong>：通過檢查後進行檔案儲存</li>
     *   <li><strong>錯誤處理</strong>：失敗時清理暫存資料並更新任務狀態</li>
     * </ol>
     * <p>
     * 錯誤處理機制：
     * <p>
     * - **自動清理**：檢查失敗時自動刪除所有暫存分塊資料
     * - **狀態更新**：同步更新傳輸任務狀態為失敗
     * - **日誌記錄**：記錄詳細的失敗原因用於除錯
     *
     * @param transferTaskId 上傳任務的唯一識別碼
     * @param totalChunks    預期的檔案分塊總數
     *
     * @return FileUploadResultBO 上傳結果業務對象，包含檔案處理狀態和相關訊息
     */
    protected Mono<FileUploadResultBO> fileCheck(String transferTaskId, int totalChunks) {
        String key = "upload_task:" + transferTaskId;
        return redisProvider
                .getHashMap(key, "DTO", UploadTaskBO.class)
                .flatMap(uploadTaskBO -> combineChunks(transferTaskId, totalChunks).flatMap(combinedBytes -> {
                    Mono<Void> checkFileStatusMono = checkFileStatus(combinedBytes, uploadTaskBO);
                    Mono<Void> scanFileMono = checkFileSecurity(combinedBytes, uploadTaskBO);

                    return Mono
                            .when(checkFileStatusMono, scanFileMono)
                            .then(processFileAfterFileCheck(uploadTaskBO, combinedBytes))
                            .onErrorResume(e -> {
                                LogUnity.warn("檔案檢查失敗，將刪除暫存資料，上傳任務ID: %s ，錯誤原因", e, transferTaskId);
                                Mono<Void> removeTempDataMono = removeTempData(uploadTaskBO);
                                Mono<Void> updateTask = transfersTasksManager.updateTransfersTask(uploadTaskBO.getMd5(),
                                                                                                  uploadTaskBO.getTransferTaskId(),
                                                                                                  TransfersStatusEnum.FAILED,
                                                                                                  "檔案檢查失敗",
                                                                                                  null,
                                                                                                  true
                                );

                                return Mono.when(removeTempDataMono, updateTask).then(Mono.defer(() -> {
                                    if (e instanceof ProcessException || e instanceof ValidationException) {
                                        return Mono.error(e);
                                    }
                                    return Mono.error(new ProcessException(ProcessException.ErrorCode.FILE_CHECK_FAILED, e, transferTaskId));
                                }));
                            });
                }));
    }


    /**
     * 處理檔案驗證成功後的儲存和元資料建立邏輯。
     * <p>
     * 當檔案通過所有安全性和完整性檢查後，執行最終的檔案儲存流程。
     * 包括將檔案儲存至 GridFS、建立伺服器檔案元資料、關聯使用者檔案記錄、
     * 更新儲存空間統計、清理暫存資料以及更新傳輸任務狀態。
     * <p>
     * 處理流程：
     * <p>
     * <ol>
     *   <li>檔案儲存：將合併後的檔案儲存至 GridFS</li>
     *   <li>MIME 類型檢測：自動識別檔案類型和媒體類型</li>
     *   <li>元資料建立：建立伺服器檔案元資料記錄</li>
     *   <li>使用者關聯：建立使用者檔案元資料關聯</li>
     *   <li>儲存統計：更新使用者儲存空間使用量</li>
     *   <li>快取清理：清除相關的檔案列表快取</li>
     *   <li>任務完成：更新傳輸任務狀態為已完成</li>
     * </ol>
     * <p>
     * 並行處理：
     * <p>
     * 部分操作會並行執行以提升效能：
     * - 儲存空間計算
     * - 暫存資料清理
     * - 快取清理
     * - 任務狀態更新
     *
     * @param uploadTaskBO  包含上傳任務詳細資訊的業務對象
     * @param combinedBytes 已驗證的完整檔案資料
     *
     * @return FileUploadResultBO 上傳結果業務對象，包含伺服器檔案和使用者檔案元資料
     */
    protected Mono<FileUploadResultBO> processFileAfterFileCheck(UploadTaskBO uploadTaskBO, byte[] combinedBytes) {
        return gridFsProvider
                .storeFile(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(combinedBytes)),
                           String.format("%s_output", uploadTaskBO.getTransferTaskId())
                )
                .flatMap(fileGridFsId -> {
                    String gridFsId = fileGridFsId.toHexString();

                    String mimeType = FileEnum.getMediaType(combinedBytes, uploadTaskBO.getFilename());
                    FileEnum fileType = FileEnum.fromMimeType(mimeType);

                    ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
                    serverFileMetadata.setFileSize(uploadTaskBO.getFileSize());
                    serverFileMetadata.setFileType(fileType);
                    serverFileMetadata.setMimeType(mimeType);
                    serverFileMetadata.setMd5(uploadTaskBO.getMd5());
                    serverFileMetadata.setGridFsId(gridFsId);
                    serverFileMetadata.setUploadTime(LocalDateTime.now());
                    serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                    serverFileMetadata.getOwners().add(uploadTaskBO.getUser().getId());

                    Mono<Void> calculateStorage = handleUserStorage(uploadTaskBO.getUser(), uploadTaskBO.getFileSize(), false);
                    Mono<Void> removeTempData = removeTempData(uploadTaskBO);
                    Mono<Void> cleanUserListCache = cleanUserListCache(uploadTaskBO.getUser().getId(), uploadTaskBO.getParentFolderId());
                    Mono<Void> updateTask = transfersTasksManager
                            .updateTransfersTask(uploadTaskBO.getMd5(),
                                                 uploadTaskBO.getTransferTaskId(),
                                                 TransfersStatusEnum.COMPLETED,
                                                 "檔案處理成功",
                                                 gridFsId,
                                                 true
                            )
                            .subscribeOn(Schedulers.boundedElastic());
                    return serverFileMetaRepository
                            .save(serverFileMetadata)
                            .flatMap(serverFile -> associateUserFile(serverFile, uploadTaskBO.formatToFileMetadata()))
                            .flatMap(userFileMetaRepository::save)
                            .flatMap(userFileMetadata -> Mono
                                    .when(calculateStorage, removeTempData, cleanUserListCache, updateTask)
                                    .thenReturn(new FileUploadResultBO(userFileMetadata, serverFileMetadata)));
                });
    }


    /**
     * 執行檔案安全性掃描以檢測潛在威脅。
     * <p>
     * 使用已設定的檔案掃描提供者對上傳的檔案進行病毒和惡意軟體檢測。
     * 如果檢測到威脅，會立即標記傳輸任務為失敗並拋出驗證異常。
     * 如果未設定掃描提供者，則跳過安全檢查。
     * <p>
     * 安全檢查流程：
     * <p>
     * <ol>
     *   <li>掃描器檢查：驗證檔案掃描提供者是否已設定</li>
     *   <li>資料準備：將檔案資料包裝為 ByteBuf 進行掃描</li>
     *   <li>威脅檢測：執行病毒和惡意軟體掃描</li>
     *   <li>結果處理：根據掃描結果決定後續動作</li>
     * </ol>
     * <p>
     * 檢測失敗處理：
     * <p>
     * 當檢測到威脅時：
     * - 記錄安全警告日誌
     * - 更新傳輸任務狀態為失敗
     * - 拋出 `ValidationException` 阻止檔案儲存
     * <p>
     * 效能考量：
     * <p>
     * - 掃描操作在有界彈性執行緒池中執行
     * - 避免阻塞主要的反應式執行緒
     *
     * @param combinedBytes 要掃描的完整檔案資料
     * @param uploadTaskBO  包含任務資訊的業務對象，用於日誌和錯誤處理
     *
     * @return 掃描完成的信號，如果檔案安全則回傳空的 Mono；如果檢測到威脅則拋出異常
     */
    protected Mono<Void> checkFileSecurity(byte[] combinedBytes, UploadTaskBO uploadTaskBO) {
        return Mono.defer(() -> {
            if (fileScanProvider == null) {
                return Mono.empty();
            }
            ByteBuf byteBuf = Unpooled.wrappedBuffer(combinedBytes);
            return fileScanProvider.scanByteBuf(Flux.just(byteBuf), combinedBytes.length).flatMap(result -> {
                if (result.isSafe()) {
                    return Mono.empty();
                }
                LogUnity.warn("上傳任務中的檔案檢測到病毒，上傳任務ID: %s", uploadTaskBO.getTransferTaskId());
                return transfersTasksManager
                        .updateTransfersTask(uploadTaskBO.getMd5(),
                                             uploadTaskBO.getTransferTaskId(),
                                             TransfersStatusEnum.FAILED,
                                             "檔案檢測到病毒，已刪除",
                                             null,
                                             true
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .then(Mono.error(new ValidationException(ValidationException.ErrorCode.FILE_VIRUS_DETECTED,
                                                                 uploadTaskBO.getTransferTaskId()
                        )));
            });
        });
    }


    /**
     * 清理上傳過程中產生的暫存分塊資料。
     * <p>
     * 在檔案上傳完成或失敗後，清理所有相關的暫存資料，包括：
     * GridFS 中的分塊檔案、Redis 中的任務資訊和待處理分塊清單。
     * 這個操作確保系統不會累積過多的暫存資料。
     * <p>
     * 清理範圍：
     * <p>
     * <ol>
     *   <li>分塊檔案：刪除 GridFS 中所有相關的分塊檔案</li>
     *   <li>任務資訊：清除 Redis 中的上傳任務雜湊表</li>
     *   <li>待處理清單：刪除待處理分塊的集合</li>
     * </ol>
     * <p>
     * 清理流程：
     * <p>
     * <ol>
     *   <li>從 Redis 獲取總分塊數量</li>
     *   <li>逐一刪除 GridFS 中的分塊檔案</li>
     *   <li>清除 Redis 中的任務雜湊表</li>
     *   <li>清除待處理分塊的集合</li>
     * </ol>
     * <p>
     * 錯誤處理：
     * <p>
     * 即使部分清理操作失敗，也會繼續執行其他清理步驟，
     * 確保儘可能多的暫存資料被清理。
     *
     * @param uploadTaskBO 包含任務識別資訊的業務對象
     *
     * @return 清理完成的信號，透過 Mono<Void> 回傳
     */
    protected Mono<Void> removeTempData(UploadTaskBO uploadTaskBO) {
        String key = "upload_task:" + uploadTaskBO.getTransferTaskId();
        return redisProvider
                .getHashMap(key, "total_chunks", Integer.class)
                .flatMapMany(totalChunks -> Flux
                        .range(1, totalChunks)
                        .flatMap(i -> gridFsProvider.deleteFileByFilename(uploadTaskBO.getTransferTaskId() + "_chunk_" + i))
                        .then(redisProvider.deleteHash(key).then(redisProvider.deleteSet(key + ":pending_chunks"))))
                .then();
    }


    /**
     * 處理單一檔案分塊的上傳和進度追蹤。
     * <p>
     * 接收並處理來自客戶端的檔案分塊，將其儲存至 GridFS，
     * 更新上傳進度，並在所有分塊完成後觸發檔案完整性檢查。
     * 提供詳細的上傳狀態回饋給客戶端。
     * <p>
     * 處理流程：
     * <p>
     * <ol>
     *   <li>分塊驗證：檢查分塊資料是否有效</li>
     *   <li>狀態更新：從待處理清單中移除當前分塊</li>
     *   <li>分塊儲存：將分塊資料儲存至 GridFS</li>
     *   <li>進度計算：更新已上傳分塊計數並計算進度百分比</li>
     * </ol>
     * 5. 完成檢查：如果所有分塊都已上傳，觸發檔案檢查
     * 6. 回應建立：建立包含進度資訊的回應對象
     * <p>
     * 進度追蹤：
     * <p>
     * - 即時更新：每個分塊上傳後立即更新進度
     * - 百分比計算：基於已上傳分塊數與總分塊數的比例
     * - 完成狀態：當所有分塊上傳完成時標記為已完成
     * <p>
     * 錯誤處理：
     * <p>
     * 當分塊上傳失敗時：
     * - 將分塊重新加入待處理清單
     * - 提供錯誤狀態的回應
     * - 保持正確的進度計算
     * <p>
     * 非同步處理：
     * <p>
     * 檔案完整性檢查在有界彈性執行緒池中非同步執行，
     * 避免阻塞上傳回應的回傳。
     *
     * @param uploadChunkDTO  包含分塊資料和元資料的傳輸對象
     * @param transferTaskId  上傳任務的唯一識別碼
     * @param key             Redis 中任務資訊的鍵值
     * @param pendingChunkKey Redis 中待處理分塊清單的鍵值
     *
     * @return 包含上傳狀態和進度資訊的回應，透過 Mono<UploadResponseDTO> 回傳
     */
    protected Mono<UploadResponseDTO> processChunk(UploadChunkDTO uploadChunkDTO, String transferTaskId, String key, String pendingChunkKey) {
        int chunkIndex = uploadChunkDTO.getChunkIndex();
        int totalChunks = uploadChunkDTO.getTotalChunks();

        Flux<DataBuffer> chunkData = uploadChunkDTO
                .getChunkDataFlux()
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO)));

        return redisProvider
                .deleteSet(pendingChunkKey, chunkIndex)
                .then(gridFsProvider.storeFile(chunkData, transferTaskId + "_chunk_" + chunkIndex))
                .then(redisProvider.incrementHashMap(key, "uploaded_count", 1, fileProperties.getUpload().getMaxUploadDuration()))
                .flatMap(uploadCount -> {
                    double progress = (uploadCount.doubleValue() / totalChunks) * 100.0;
                    String message = String.format("檔案分塊: %d 上傳成功", chunkIndex);

                    UploadResponseDTO responseDTO = UploadResponseDTO
                            .builder()
                            .chunkIndex(chunkIndex)
                            .transferTaskId(transferTaskId)
                            .progress(progress)
                            .isSuccess(true)
                            .isFinished(false)
                            .message(message)
                            .totalChunks(totalChunks)
                            .build();

                    if (uploadCount.intValue() == totalChunks) {
                        return fileCheck(transferTaskId, uploadChunkDTO.getTotalChunks()).flatMap(result -> {
                            responseDTO.setIsFinished(true);
                            responseDTO.setFileId(result.getUserFileId());
                            return Mono.just(responseDTO);
                        });
                    }
                    return Mono.just(responseDTO);
                })
                .onErrorResume(e -> redisProvider
                        .setSet(pendingChunkKey, chunkIndex)
                        .then(redisProvider.getHashMap(key, "uploaded_count").map(count -> {
                            long uploadCountLong = Long.parseLong(count.toString());
                            double progress = ((double) uploadCountLong / totalChunks) * 100.0;
                            return UploadResponseDTO
                                    .builder()
                                    .chunkIndex(chunkIndex)
                                    .transferTaskId(transferTaskId)
                                    .progress(progress)
                                    .isSuccess(false)
                                    .isFinished(false)
                                    .message("檔案分塊上傳失敗")
                                    .totalChunks(totalChunks)
                                    .build();
                        })));
    }


    /**
     * 從回收站還原單一檔案。
     *
     * @param userFileMetadata 要還原的檔案元資料
     * @param user             執行還原的使用者資訊
     *
     * @return 還原後的檔案元資料，透過 Mono 非同步回傳
     */
    public Mono<UserFileMetadata> restoreFile(UserFileMetadata userFileMetadata, User user) {
        return restoreFile(Collections.singletonList(userFileMetadata), user).next();
    }


    /**
     * 從回收站批量還原檔案。
     * <p>
     * 批量還原多個被刪除的檔案，支援事務性操作以確保資料一致性。
     * 當原父資料夾也被刪除時，會自動將檔案移至根目錄。
     * <p>
     * 還原特性：
     * <p>
     * - 事務性：全部成功或全部回滾
     * - 智能移動：自動處理父資料夾狀態
     * - 快取清理：自動清除相關快取
     *
     * @param userFileMetadataIterable 要還原的檔案列表
     * @param user                     執行還原的使用者資訊
     *
     * @return 還原後的檔案列表，透過 Flux 非同步回傳
     */
    public Flux<UserFileMetadata> restoreFile(Iterable<UserFileMetadata> userFileMetadataIterable, User user) {
        Map<Long, UserFileMetadata> userFileMetadataMap = new HashMap<>();
        Set<Long> problemFileIdsSet = new HashSet<>();
        userFileMetadataIterable.forEach(userFileMetadata -> {
            if (!userFileMetadata.getIsDeleted()) {
                problemFileIdsSet.add(userFileMetadata.getId());
            }
            userFileMetadataMap.put(userFileMetadata.getId(), userFileMetadata);
        });
        if (!problemFileIdsSet.isEmpty()) {
            return Flux.error(new ValidationException(ValidationException.ErrorCode.SOME_FILE_NOT_DELETED, problemFileIdsSet));
        }

        if (userFileMetadataMap.isEmpty()) {
            return Flux.empty();
        }
        return transactionalOperator
                .transactional(recoverFileMethod(userFileMetadataMap, problemFileIdsSet).then(Mono.just(true)))
                .thenMany(Flux.fromIterable(userFileMetadataMap.values()));
    }


    /**
     * 將單一檔案移至回收站。
     *
     * @param userFileMetadata 要移至回收站的檔案元資料
     * @param user             執行操作的使用者資訊
     *
     * @return 是否成功移至回收站，透過 Mono<Boolean> 回傳
     */
    public Mono<Boolean> removeFile(UserFileMetadata userFileMetadata, User user) {
        return removeFile(Collections.singletonList(userFileMetadata), user);
    }


    /**
     * 將檔案批量移至回收站。
     * <p>
     * 批量將多個檔案移至回收站，而非永久刪除。
     * 檔案會按照系統設定的保留時間進行定時清理。
     * 支援事務性操作以確保資料一致性。
     * <p>
     * 回收站特性：
     * <p>
     * - 暂存狀態：檔案標記為已刪除但仍可還原
     * - 定時清理：超過保留時間後自動永久刪除
     * - 事務性：全部成功或全部回滾
     *
     * @param userFileMetadataIterable 要移至回收站的檔案列表
     * @param user                     執行操作的使用者資訊
     *
     * @return 是否成功移至回收站，透過 Mono<Boolean> 回傳
     */
    public Mono<Boolean> removeFile(Iterable<UserFileMetadata> userFileMetadataIterable, User user) {
        return Mono.defer(() -> {
            LocalDateTime deleteTime = LocalDateTime.now().plusDays(fileProperties.getBackup().getRetentionTime().toDays());
            Set<FileTrashRecord> fileTrashRecords = new HashSet<>();
            Set<Long> problemFileIdsSet = new HashSet<>();
            userFileMetadataIterable.forEach(userFile -> {
                if (userFile.getIsDeleted()) {
                    problemFileIdsSet.add(userFile.getId());
                }
                FileTrashRecord fileTrashRecord = new FileTrashRecord(userFile, deleteTime);
                fileTrashRecords.add(fileTrashRecord);
                userFile.setIsDeleted(true);
            });
            if (!problemFileIdsSet.isEmpty()) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.SOME_FILE_ALREADY_DELETED, problemFileIdsSet));
            }

            Mono<Boolean> result = fileTrashRecordRepository
                    .insertAll(fileTrashRecords, entityOperations)
                    .thenMany(userFileMetaRepository.saveAll(userFileMetadataIterable))
                    .collectList()
                    .flatMap(fileMeta -> {
                        Long[] parentFolderIds = fileMeta.stream().map(UserFileMetadata::getParentFolderId).distinct().toArray(Long[]::new);
                        return cleanUserListCache(fileMeta.getFirst().getUserId(), parentFolderIds);
                    })
                    .thenReturn(true)
                    .onErrorReturn(false);
            return transactionalOperator.transactional(result);
        });
    }


    /**
     * 執行檔案從回收站還原的核心業務邏輯。
     * <p>
     * 這是檔案還原功能的核心實現，負責處理從回收站還原檔案的複雜邏輯。
     * 包括回收站記錄驗證、檔案狀態更新、父資料夾檢查和最終的事務性儲存。
     * <p>
     * 還原流程：
     * 1. 回收站記錄查詢：根據檔案 ID 查詢對應的回收站記錄
     * 2. 記錄驗證：確保每個檔案都有對應的回收站記錄
     * 3. 狀態更新：將檔案標記為未刪除並更新存取時間
     * 4. 父資料夾檢查：檢查父資料夾是否仍存在，如被刪除則移至根目錄
     * 5. 批量處理：收集所有處理結果進行批量操作
     * 6. 記錄清理：刪除對應的回收站記錄
     * 7. 資料儲存：更新檔案元資料至資料庫
     * 8. 快取清理：清除相關的檔案列表快取
     * <p>
     * 特殊處理情境：
     * 孤兒檔案：父資料夾被刪除時，自動移至根目錄
     * 缺失記錄：當回收站記錄不存在時，記錄問題檔案並拋出異常
     * 批量操作：使用事務性操作確保資料一致性
     * <p>
     * 錯誤處理：當發現不存在的回收站記錄時，拋出 ProcessException 並提供詳細的問題檔案 ID 列表用於除錯
     *
     * @param userFileMetadataMap 要還原的檔案元資料映射，鍵為檔案 ID
     * @param problemFileIdsSet   用於收集問題檔案 ID 的集合，會在處理過程中更新
     *
     * @return 成功還原的檔案元資料流，透過 Flux<UserFileMetadata> 回傳
     */
    private Flux<UserFileMetadata> recoverFileMethod(Map<Long, UserFileMetadata> userFileMetadataMap, Set<Long> problemFileIdsSet) {
        return fileTrashRecordRepository.findAllById(userFileMetadataMap.keySet()).flatMap(fileTrashRecord -> {
            UserFileMetadata userFileMetadata = userFileMetadataMap.get(fileTrashRecord.getFileId());
            if (userFileMetadata == null) {
                problemFileIdsSet.add(fileTrashRecord.getFileId());
                return Mono.empty();
            }
            userFileMetadata.setIsDeleted(false);
            userFileMetadata.setLastAccessTime(LocalDateTime.now());

            if (userFileMetadata.getParentFolderId() == null) {
                return Mono.just(userFileMetadata);
            }
            return userFileMetaRepository.findById(userFileMetadata.getParentFolderId().toString()).flatMap(fatherFolder -> {
                if (fatherFolder.getIsDeleted()) {
                    userFileMetadata.setParentFolderId(null);
                }
                return Mono.just(userFileMetadata);
            });
        }).collectList().flatMapMany(processedMetadata -> {
            if (!problemFileIdsSet.isEmpty()) {
                return Flux.error(new ProcessException(ProcessException.ErrorCode.NOT_EXISTING_FILE_TRASH_RECORD, problemFileIdsSet));
            }

            return fileTrashRecordRepository
                    .deleteAllById(userFileMetadataMap.keySet())
                    .thenMany(userFileMetaRepository.saveAll(userFileMetadataMap.values()))
                    .collectList()
                    .flatMapMany(metadataList -> {
                        Long[] parentFolderIds = metadataList.stream().map(UserFileMetadata::getParentFolderId).distinct().toArray(Long[]::new);
                        return cleanUserListCache(metadataList.getFirst().getUserId(), parentFolderIds).thenMany(Flux.fromIterable(metadataList));
                    });
        });
    }


    /**
     * 建立一個新的使用者檔案元資料實體。
     *
     * @return 新的使用者檔案元資料實體，透過 Mono 非同步回傳
     */
    public Mono<UserFileMetadata> createUserFileMetadata() {
        return Mono.just(new UserFileMetadata());
    }


    /**
     * 根據 ID 取得使用者檔案元資料。
     *
     * @param id 使用者檔案元資料的唯一識別碼
     *
     * @return 對應的使用者檔案元資料，若不存在則回傳空的 Mono
     */
    public Mono<UserFileMetadata> getUserFileMetadataById(Long id) {
        return userFileMetaRepository.findById(id.toString());
    }


    /**
     * 取得所有使用者檔案元資料。
     * <p>
     * 警告：此方法會回傳系統中所有使用者的檔案資料，僅應用於管理功能。
     *
     * @return 所有使用者檔案元資料的串流，透過 Flux 非同步回傳
     */
    public Flux<UserFileMetadata> getAllUserFileMetadata() {
        return userFileMetaRepository.findAll();
    }


    /**
     * 更新使用者檔案元資料。
     *
     * @param entity 要更新的使用者檔案元資料實體，不可為 null
     *
     * @return 更新後的使用者檔案元資料，透過 Mono 非同步回傳
     */
    public Mono<UserFileMetadata> updateUserFileMetadata(@NotNull UserFileMetadata entity) {
        return userFileMetaRepository.save(entity);
    }


    /**
     * 刪除使用者檔案元資料。
     *
     * @param entity 要刪除的使用者檔案元資料實體
     *
     * @return 刪除完成的信號，透過 Mono<Void> 回傳
     */
    public Mono<Void> deleteUserFileMetadata(UserFileMetadata entity) {
        return userFileMetaRepository.deleteById(entity.getId().toString());
    }


    /**
     * 建立一個新的伺服器檔案元資料實體。
     *
     * @return 新的伺服器檔案元資料實體，透過 Mono 非同步回傳
     */
    public Mono<ServerFileMetadata> createServerFileMetadata() {
        return Mono.just(new ServerFileMetadata());
    }


    /**
     * 根據 ID 取得伺服器檔案元資料。
     *
     * @param id 伺服器檔案元資料的唯一識別碼
     *
     * @return 對應的伺服器檔案元資料，若不存在則回傳空的 Mono
     */
    public Mono<ServerFileMetadata> getByServerFileMetadataId(Long id) {
        return serverFileMetaRepository.findById(id);
    }


    /**
     * 取得所有伺服器檔案元資料。
     * <p>
     * 管理權限需求：此方法需要 MANAGE 權限才能執行。
     * 回傳系統中所有實際檔案的元資料資訊。
     *
     * @return 所有伺服器檔案元資料的串流，透過 Flux 非同步回傳
     */
    @RequirePermission(PermissionEnum.MANAGE)
    public Flux<ServerFileMetadata> getAllServerFileMetadata() {
        return serverFileMetaRepository.findAll();
    }


    /**
     * 更新伺服器檔案元資料。
     * <p>
     * 管理權限需求：此方法需要 MANAGE 權限才能執行。
     *
     * @param entity 要更新的伺服器檔案元資料實體，不可為 null
     *
     * @return 更新後的伺服器檔案元資料，透過 Mono 非同步回傳
     */
    @RequirePermission(PermissionEnum.MANAGE)
    public Mono<ServerFileMetadata> updateServerFileMetadata(@NotNull ServerFileMetadata entity) {
        return serverFileMetaRepository.save(entity);
    }


    /**
     * 刪除伺服器檔案元資料。
     * <p>
     * 管理權限需求：此方法需要 MANAGE 權限才能執行。
     *
     * @param entity 要刪除的伺服器檔案元資料實體
     *
     * @return 刪除完成的信號，透過 Mono<Void> 回傳
     */
    @RequirePermission(PermissionEnum.MANAGE)
    public Mono<Void> deleteServerFileMetadata(ServerFileMetadata entity) {
        return serverFileMetaRepository.deleteById(entity.getId());
    }

}
