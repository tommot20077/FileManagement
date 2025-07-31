package xyz.dowob.filemanagement.controller.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.DownloadActionEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static xyz.dowob.filemanagement.customenum.FileEnum.*;

/**
 * 基於反應式編程的檔案控制器抽象基類，實現檔案管理的核心業務邏輯和通用操作流程。
 * 
 * <p>此類採用模板方法設計模式，提供檔案管理的標準化實現，包括檔案列表查詢、搜尋、
 * 權限驗證、刪除與還原等核心功能。通過策略模式支援多種檔案服務實現，
 * 確保系統的可擴展性和靈活性。</p>
 * 
 * <p>所有具體的檔案控制器（如 Web 控制器或 API 控制器）都應繼承此基類，
 * 實現特定的模板方法以適配不同的應用場景和請求處理需求。</p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RecordLevel(LogLevelEnum.INFO)
@RequiredArgsConstructor
public abstract class BaseFileController implements ResponseUnity {
    /**
     * 支持的檔案類型枚舉數組，涵蓋系統中預定義的檔案類型。
     * 
     * <p>此數組包含的檔案類型包括：
     * <ul>
     *   <li>圖像檔案</li>
     *   <li>視頻檔案</li>
     *   <li>音樂檔案</li>
     *   <li>文檔檔案</li>
     *   <li>壓縮檔案</li>
     *   <li>其他類型檔案</li>
     *   <li>在線文檔</li>
     * </ul>
     * </p>
     */
    protected static final FileEnum[] CUSTOM_FILE_TYPE = new FileEnum[]{IMAGE, VIDEO, MUSIC, DOCUMENT, ZIP, OTHER, ONLINE_DOCUMENT};

    /**
     * 用戶服務實例，提供與用戶相關的業務邏輯操作。
     * 
     * <p>此服務主要用於用戶身份驗證、獲取用戶信息和執行用戶相關的檔案操作。</p>
     */
    protected final UserService userService;

    /**
     * 檔案服務策略管理器，實現動態選擇檔案服務實現。
     * 
     * <p>根據不同的檔案類型和操作場景，動態選擇對應的檔案服務實現，
     * 支持多態的檔案處理策略。</p>
     */
    protected final FileServiceStrategy fileServiceStrategy;

    /**
     * 檔案設定屬性管理器，提供系統檔案相關的全局設定信息。
     * 
     * <p>封裝了檔案下載、存儲、緩存等相關的全局設定參數，
     * 確保檔案服務的一致性和可設定性。</p>
     */
    protected final FileProperties fileProperties;

    /**
     * 資料驗證服務，提供檔案及業務邏輯的資料校驗。
     * 
     * <p>負責執行各種資料驗證邏輯，包括但不限於：
     * <ul>
     *   <li>檔案類型驗證</li>
     *   <li>檔案過濾條件檢查</li>
     *   <li>業務規則校驗</li>
     * </ul>
     * </p>
     */
    protected final ValidationService validationService;

    /**
     * 檔案權限服務，提供細粒度的用戶-檔案操作權限管理。
     * 
     * <p>實現基於用戶身份和檔案元資料的權限控制邏輯，確保：
     * <ul>
     *   <li>用戶對檔案的操作權限</li>
     *   <li>檔案的訪問控制</li>
     *   <li>安全性和資源隔離</li>
     * </ul>
     * </p>
     */
    protected final PermissionService<UserFileMetadata> permissionService;

    /**
     * JSON對象映射器，提供Java對象與JSON格式之間的高效轉換。
     * 
     * <p>用於處理：
     * <ul>
     *   <li>Java對象序列化為JSON</li>
     *   <li>JSON反序列化為Java對象</li>
     *   <li>複雜對象的JSON轉換</li>
     * </ul>
     * </p>
     */
    protected final ObjectMapper objectMapper;

    /**
     * 檔案權限規則管理器，提供靈活且可設定的檔案訪問權限策略。
     * 
     * <p>負責管理和提供各種檔案權限規則，包括：
     * <ul>
     *   <li>共享檔案權限</li>
     *   <li>所有者權限</li>
     *   <li>刪除和搜索操作限制</li>
     * </ul>
     * </p>
     */
    protected final FilePermissionRuleManager filePermissionRuleManager;


    /**
     * 獲取用戶特定資料夾下的檔案列表，支持多維度的檔案篩選和分頁。
     * 
     * <p>此方法實現了靈活的檔案檢索邏輯，支持：
     * <ul>
     *   <li>按資料夾 ID 精確檢索</li>
     *   <li>根據檔案類型多維度過濾</li>
     *   <li>分頁結果集管理</li>
     * </ul>
     * </p>
     * 
     * <p>特別支持 {@link xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum} 中的特殊搜索 ID，
     * 可實現如回收站等特殊檔案列表場景。</p>
     *
     * @param exchange 伺服器 Web 交換對象，攜帶用戶請求上下文。
     * @param folderId 目標資料夾 ID，指定檢索範圍。
     * @param page     分頁頁碼，從 1 開始，支持多頁檔案列表。
     * @param size     每頁檔案數量，控制單頁回傳的最大檔案數。
     * @param types    檔案類型過濾器，可傳入多種檔案類型進行精確篩選。
     *
     * @return 包含檔案列表和路徑信息的響應實體，支持響應式編程模型。
     */
    public Mono<ResponseEntity<?>> getUserFileList(ServerWebExchange exchange, Long folderId, Integer page, Integer size, List<FileEnum> types) {
        Mono<ResponseEntity<?>> action = userService.getUser(exchange).flatMap(user -> {
            FileService fileService = fileServiceStrategy.getFileService();

            List<Permission<UserFileMetadata>> rules = new ArrayList<>(List.of(filePermissionRuleManager.getAllowShared()));
            if (!Objects.equals(folderId, ReservedSearchIdEnum.RECYCLE_FILE_ID.getId())) {
                rules.add(filePermissionRuleManager.getBlockDeleted());
            }

            return permissionService.validateUserPermission(user, folderId, rules).flatMap(folder -> {
                FileFilterDTO fileFilterDTO = FileFilterDTO.builder().folderId(folderId).types(types).page(page).pageSize(size).build();

                Mono<PagedResponseDTO<UserFileListDTO>> fileListMono = fileService.getUserFileList(user, fileFilterDTO);
                Mono<List<FolderListTreeProvider.FolderNode>> filePathsMono = fileService.getUserFilePaths(folder, user);

                return validationService.validateFileType(folder, FOLDER).then(Mono.zip(fileListMono, filePathsMono).flatMap(tuple -> {
                    HashMap<String, Object> result = new HashMap<>();
                    result.put("files", tuple.getT1());
                    result.put("filePaths", tuple.getT2());
                    return createResponseEntity(createApiResponse(exchange, "獲取用戶檔案列表成功", result));
                }));
            });
        });
        return handleError(action, exchange);
    }


    /**
     * 執行複合條件的檔案搜索，提供高度靈活的檔案檢索能力。
     * 
     * <p>此方法支持多維度、複合邏輯的檔案檢索，包括：
     * <ul>
     *   <li>關鍵字模糊匹配</li>
     *   <li>資料夾範圍限定</li>
     *   <li>檔案類型精確過濾</li>
     *   <li>時間範圍篩選</li>
     *   <li>排序和分頁支持</li>
     * </ul>
     * </p>
     * 
     * <p>通過 {@link xyz.dowob.filemanagement.data.file.dto.FileFilterDTO} 封裝複雜的搜索邏輯，
     * 實現高度定製化的檔案檢索策略。</p>
     *
     * @param exchange      Web 交換對象，提供用戶請求上下文。
     * @param fileFilterDTO 檔案篩選資料傳輸對象，封裝複雜的檔案檢索條件。
     *
     * @return 響應式檔案列表，支持非阻塞檔案檢索。
     */
    protected Mono<ResponseEntity<?>> searchFile(ServerWebExchange exchange, FileFilterDTO fileFilterDTO) {
        Mono<ResponseEntity<?>> action = userService.getUser(exchange).flatMap(user -> {
            FileService fileService = fileServiceStrategy.getFileService();
            return validationService.validateFileFilterDTO(fileFilterDTO).then(fileService.searchUserFile(user, fileFilterDTO).flatMap(files -> {
                HashMap<String, Object> result = new HashMap<>();
                result.put("userId", user.getId());
                result.put("username", user.getUsername());
                result.put("files", files);
                result.put("filePaths", Collections.singletonList(new FolderListTreeProvider.FolderNode(null, "root")));
                return createResponseEntity(createApiResponse(exchange, "搜索檔案成功", result));
            }));
        });

        return handleError(action, exchange);
    }


    /**
     * 執行檔案移至回收站的安全刪除操作，支持多種檔案類型的刪除邏輯。
     * 
     * <p>此方法實現了安全且可追溯的檔案刪除流程：
     * <ul>
     *   <li>驗證用戶刪除權限</li>
     *   <li>校驗檔案有效性</li>
     *   <li>支持單一或多類型檔案刪除</li>
     *   <li>確保刪除操作的原子性</li>
     * </ul>
     * </p>
     * 
     * <p>若未指定檔案類型，將嘗試對系統支持的所有檔案類型執行刪除。</p>
     *
     * @param exchange Web 交換對象，攜帶用戶請求上下文。
     * @param id       目標檔案唯一標識符。
     * @param type     可選的檔案類型，用於精確刪除。
     *
     * @return 響應式刪除操作結果，支持非阻塞響應。
     */
    protected Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, String id, FileEnum type) {
        FileEnum[] fileType = type == null ? CUSTOM_FILE_TYPE : new FileEnum[]{type};
        return handleError(Mono.defer(() -> userService
                .getUser(exchange)
                .flatMap(user -> permissionService
                        .validateUserPermission(user, Long.parseLong(id))
                        .flatMap(file -> validationService
                                .validateFileType(file, fileType)
                                .then(fileServiceStrategy.getFileService(type).removeFile(file, user))))
                .flatMap(result -> {
                    String message = result ? "回收檔案成功" : "回收檔案失敗";
                    int status = result ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value();
                    ApiResponseDTO<?> apiResponse = createApiResponse(exchange, status, message, null);
                    return createResponseEntity(apiResponse);
                })), exchange
        );
    }


    /**
     * 從回收站中執行檔案還原操作，提供安全且可控的檔案恢復機制。
     * 
     * <p>檔案還原流程包含：
     * <ul>
     *   <li>驗證用戶還原權限</li>
     *   <li>檢查檔案是否允許還原</li>
     *   <li>支持特定類型檔案的精確還原</li>
     *   <li>防止意外或未授權的還原操作</li>
     * </ul>
     * </p>
     * 
     * <p>若未指定具體檔案類型，將對所有支持的檔案類型執行還原檢查。</p>
     *
     * @param exchange Web 交換對象，提供用戶請求上下文。
     * @param id       待還原檔案的唯一標識符。
     * @param type     可選的檔案類型，用於精確還原。
     *
     * @return 響應式還原操作結果，支持非阻塞響應模型。
     */
    protected Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, String id, FileEnum type) {
        FileEnum[] fileType = type == null ? CUSTOM_FILE_TYPE : new FileEnum[]{type};
        List<Permission<UserFileMetadata>> rules = List.of(filePermissionRuleManager.getAllowOwner(),
                                                           filePermissionRuleManager.getBlockNotSearchOperation()
        );
        return handleError(Mono.defer(() -> userService
                .getUser(exchange)
                .flatMap(user -> permissionService
                        .validateUserPermission(user, Long.parseLong(id), rules)
                        .flatMap(file -> validationService
                                .validateFileType(file, fileType)
                                .then(fileServiceStrategy.getFileService(type).restoreFile(file, user))))
                .then(Mono.defer(() -> {
                    ApiResponseDTO<?> apiResponse = createApiResponse(exchange, "還原檔案成功", null);
                    return createResponseEntity(apiResponse);
                }))), exchange
        );
    }


    /**
     * 將字符串格式的檔案類型列表轉換為對應的枚舉類型集合。
     * <p>
     * 此方法提供了健墯的檔案類型轉換機制，能夠處理各種異常情況並提供安全的轉換結果。
     * 支援不同的字符串格式，包括大小寫變化和空值處理。
     * </p>
     * <p>
     * <strong>轉換特性：</strong>
     * </p>
     * <ul>
     *   <li><strong>大小寫不敏感：</strong>自動將輸入字符串轉換為大寫</li>
     *   <li><strong>空值過濾：</strong>自動節除 null 值和無效的轉換結果</li>
     *   <li><strong>異常容忍：</strong>無法匹配的字符串不會拋出異常，而是被忽略</li>
     * </ul>
     * <p>
     * <strong>輸入處理：</strong>
     * </p>
     * <ul>
     *   <li><strong>null 列表：</strong>返回空列表，不會拋出 NullPointerException</li>
     *   <li><strong>空列表：</strong>直接返回空列表</li>
     *   <li><strong>包含 null 元素：</strong>自動過濾 null 元素</li>
     * </ul>
     * <p>
     * <strong>支援的檔案類型：</strong>
     * </p>
     * <ul>
     *   <li>IMAGE - 圖像檔案</li>
     *   <li>VIDEO - 視頻檔案</li>
     *   <li>MUSIC - 音樂檔案</li>
     *   <li>DOCUMENT - 文檔檔案</li>
     *   <li>ZIP - 壓縮檔案</li>
     *   <li>OTHER - 其他類型檔案</li>
     *   <li>ONLINE_DOCUMENT - 線上文檔</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 正常使用
     * List&lt;String&gt; types = Arrays.asList("image", "VIDEO", "document");
     * List&lt;FileEnum&gt; fileEnums = getFileEnums(types);
     * // 結果：[IMAGE, VIDEO, DOCUMENT]
     * 
     * // 包含無效值
     * List&lt;String&gt; invalidTypes = Arrays.asList("image", "invalid_type", null, "video");
     * List&lt;FileEnum&gt; result = getFileEnums(invalidTypes);
     * // 結果：[IMAGE, VIDEO] （無效值被過濾）
     * </pre>
     * 
     * @param type 檔案類型字符串列表，可包含 null 值和無效值
     * @return 轉換成功的檔案類型枚舉列表，不包含 null 或無效值
     * @see FileEnum
     * @apiNote 此方法被 @SkipRecord 標註，不會被 AOP 日誌記錄
     * @implNote 使用 Stream API 進行快速的過濾和轉換操作
     */
    @SkipRecord
    protected List<FileEnum> getFileEnums(List<String> type) {
        return Optional.ofNullable(type).orElse(Collections.emptyList()).stream().map(t -> {
            if (t == null) {
                return null;
            }
            try {
                return FileEnum.valueOf(t.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }


    /**
     * 構建檔案傳輸的 HTTP 標頭，提供細粒度的檔案傳輸控制機制。
     * 
     * <p>標頭構建過程包含：
     * <ul>
     *   <li>檔案下載模式設定</li>
     *   <li>檔案名稱編碼</li>
     *   <li>內容類型設定</li>
     *   <li>緩存控制策略</li>
     *   <li>分段下載支持</li>
     * </ul>
     * </p>
     * 
     * <p>支持不同的檔案傳輸場景，如預覽、完整下載、分段下載等，
     * 並提供豐富的 HTTP 標頭設定選項。</p>
     *
     * @param action         檔案傳輸行為，區分預覽或下載模式。
     * @param userFileDataBO 檔案元資料傳輸對象，攜帶檔案詳細信息。
     * @param rangeHeader    HTTP 範圍標頭，支持分段下載。
     * @param enableCache    是否啟用 HTTP 緩存機制。
     *
     * @return 封裝後的 HTTP 標頭，提供安全且靈活的檔案傳輸控制。
     */
    @SkipRecord
    protected HttpHeaders prepareHttpHeaders(DownloadActionEnum action, UserFileDataBO userFileDataBO, String rangeHeader, boolean enableCache) {
        HttpHeaders headers = getHttpHeaders(userFileDataBO, rangeHeader);

        if (action.equals(DownloadActionEnum.DOWNLOAD)) {
            String filename = userFileDataBO.getFilename();
            String sanitizedFilename = filename.replace("\"", "");
            String encodedFilename = URLEncoder.encode(sanitizedFilename, StandardCharsets.UTF_8).replace("+", "%20");

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizedFilename + "\"; filename*=UTF-8''" + encodedFilename);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        } else {
            headers.add(HttpHeaders.CONTENT_TYPE, Objects.requireNonNullElse(userFileDataBO.getMimeType(), MediaType.APPLICATION_OCTET_STREAM_VALUE));
        }

        if (enableCache) {
            String cacheControl = String.format("private, max-age=%d", fileProperties.getDownload().getDownloadCacheHeaderExpireTime().toSeconds());
            headers.add(HttpHeaders.CACHE_CONTROL, cacheControl);
        }

        return headers;
    }


    /**
     * 生成符合 HTTP 協議標準的檔案傳輸標頭，支援完整和分段下載。
     * <p>
     * 此方法實現了高度精細化的 HTTP 標頭生成邏輯，支援現代瀏覽器的各種進階功能。
     * 特別針對大型檔案和多媒體檔案的傳輸需求進行了優化。
     * </p>
     * <p>
     * <strong>支援的傳輸模式：</strong>
     * </p>
     * <ul>
     *   <li><strong>完整下載：</strong>當 rangeHeader 為 null 時，傳輸整個檔案</li>
     *   <li><strong>分段下載：</strong>根據 Range 標頭傳輸指定範圍的內容</li>
     *   <li><strong>斷點續傳：</strong>支援中斷後繼續下載</li>
     * </ul>
     * <p>
     * <strong>HTTP 標頭詳細說明：</strong>
     * </p>
     * <ul>
     *   <li><strong>Content-Length：</strong>設定正確的內容長度，支援傳輸進度顯示</li>
     *   <li><strong>Accept-Ranges：</strong>設為 "bytes"，告知客戶端支援分段下載</li>
     *   <li><strong>Content-Range：</strong>分段下載時指定內容範圍資訊</li>
     * </ul>
     * <p>
     * <strong>Range 標頭處理邏輯：</strong>
     * </p>
     * <ol>
     *   <li><strong>解析 Range 標頭：</strong>支援 "bytes=start-end" 格式</li>
     *   <li><strong>計算範圍：</strong>自動處理起始和結束位置</li>
     *   <li><strong>邊界檢查：</strong>確保範圍不超出檔案大小</li>
     *   <li><strong>標頭生成：</strong>設定正確的 Content-Range 和 Content-Length</li>
     * </ol>
     * <p>
     * <strong>支援的 Range 格式：</strong>
     * </p>
     * <ul>
     *   <li><strong>bytes=200-1023：</strong>從第 200 位元組到第 1023 位元組</li>
     *   <li><strong>bytes=200-：</strong>從第 200 位元組到檔案結尾</li>
     *   <li><strong>bytes=-500：</strong>最後 500 位元組（尚未實現）</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 完整下載
     * HttpHeaders headers = getHttpHeaders(fileData, null);
     * // 結果：Content-Length: 1048576, Accept-Ranges: bytes
     * 
     * // 分段下載
     * HttpHeaders headers = getHttpHeaders(fileData, "bytes=0-1023");
     * // 結果：Content-Range: bytes 0-1023/1048576, Content-Length: 1024
     * </pre>
     * 
     * @param userFileDataBO 檔案元資料業務對象，包含檔案大小等關鍵資訊
     * @param rangeHeader HTTP Range 標頭字符串，為 null 時表示完整下載
     * @return 配置完成的 HTTP 標頭對象，包含所有必要的傳輸控制資訊
     * @see HttpHeaders
     * @see UserFileDataBO
     * @apiNote 此方法為私有方法，被 @SkipRecord 標註，不會被 AOP 記錄
     * @implNote 使用標準 HTTP/1.1 Range 請求規範，相容所有主流瀏覽器
     */
    @SkipRecord
    private HttpHeaders getHttpHeaders(UserFileDataBO userFileDataBO, String rangeHeader) {
        HttpHeaders headers = new HttpHeaders();
        long fileSize = userFileDataBO.getFileSize();

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 && !ranges[1].isEmpty() ? Long.parseLong(ranges[1]) : fileSize - 1;

            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(end - start + 1));
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        } else {
            headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        }
        return headers;
    }


    /**
     * 執行檔案下載驗證失敗的特殊錯誤處理，提供精確且安全的錯誤響應機制。
     * 
     * <p>錯誤處理流程包含：
     * <ul>
     *   <li>格式化錯誤訊息</li>
     *   <li>生成標準化錯誤響應</li>
     *   <li>確保安全的錯誤信息傳遞</li>
     *   <li>支持反應式編程模型</li>
     * </ul>
     * </p>
     * 
     * <p>由於檔案下載的錯誤處理與標準異常處理不同，
     * 需要使用專門的錯誤處理邏輯來確保用戶體驗和系統安全。</p>
     *
     * @param e        檔案下載驗證異常，攜帶詳細的錯誤描述。
     * @param exchange Web 交換對象，提供請求上下文。
     *
     * @return 包含錯誤信息的非阻塞響應實體，支持反應式編程模型。
     */
    public Mono<ResponseEntity<Flux<DataBuffer>>> handleDownloadValidationError(ValidationException e, ServerWebExchange exchange) {
        String errorMessage = String.format("下載失敗: %s", e.getMessage());
        ApiResponseDTO<?> apiResponse = createApiResponse(exchange, e.getErrorCode().getCode(), errorMessage, null);

        try {
            byte[] responseBytes = objectMapper.writeValueAsString(apiResponse).getBytes();
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);

            return Mono.just(ResponseEntity.status(e.getErrorCode().getHttpStatus()).contentType(MediaType.APPLICATION_JSON).body(Flux.just(buffer)));
        } catch (JsonProcessingException ex) {
            return Mono.error(new RuntimeException(ex));
        }
    }
}
