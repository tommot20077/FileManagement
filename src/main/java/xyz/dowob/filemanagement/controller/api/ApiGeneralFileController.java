package xyz.dowob.filemanagement.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseFileController;
import xyz.dowob.filemanagement.controller.base.BaseGeneralFileController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 基於 Spring WebFlux 反應式編程的一般檔案管理 RESTful API 控制器實現。
 * <p>
 * 提供完整的非阻塞檔案管理功能，採用反應式流處理大檔案上傳和下載操作。
 * 系統支援分塊上傳、斷點續傳、檔案預覽、軟刪除、檔案還原和進階檔案搜尋等核心功能。
 * 透過策略模式實現可插拔的檔案服務，支援本地存儲和雲端存儲的動態切換。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>檔案上傳 - 支援分塊上傳和多種傳輸方式</li>
 *   <li>檔案下載 - 提供預覽和下載兩種模式</li>
 *   <li>檔案管理 - 編輯、重命名、移動和刪除操作</li>
 *   <li>回收站功能 - 軟刪除和檔案還原</li>
 *   <li>進階搜尋 - 多維度條件過濾和分頁查詢</li>
 *   <li>檔案清單 - 支援檔案類型過濾和排序</li>
 * </ul>
 * <p>
 * 安全性設計：
 * <ul>
 *   <li>所有端點要求用戶身份認證</li>
 *   <li>基於角色的權限控制（RBAC）</li>
 *   <li>檔案存取權限驗證</li>
 *   <li>用戶資源使用限制</li>
 *   <li>請求頻率限制</li>
 * </ul>
 * <p>
 * 性能優化：
 * <ul>
 *   <li>反應式流處理避免記憶體溢出</li>
 *   <li>非阻塞 I/O 提升並發處理能力</li>
 *   <li>分塊上傳減少網路延遲</li>
 *   <li>檔案元資料快取提升查詢效率</li>
 * </ul>
 * <p>
 * 繼承自 {@link BaseGeneralFileController}，透過依賴注入整合各種服務組件。
 * 採用 AOP 實現日誌記錄、權限控制和異常處理的橫切關注點。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see BaseGeneralFileController
 * @see FileServiceStrategy
 * @see UserLimiterStrategy
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/api/v1/files")
public class ApiGeneralFileController extends BaseGeneralFileController {

    /**
     * 初始化一般檔案 API 控制器，透過依賴注入設定所需的服務組件和配置。
     * <p>
     * 構造函數負責整合檔案管理系統的核心組件，包括用戶管理、檔案服務策略、
     * 權限控制、資源限制和驗證服務等。所有注入的服務都透過 Spring 容器
     * 進行生命週期管理，確保系統的穩定性和可維護性。
     * <p>
     * 主要組件功能：
     * <ul>
     *   <li>用戶服務 - 處理用戶身份認證、授權和基本資料管理</li>
     *   <li>檔案服務策略 - 提供可插拔的檔案操作實現（本地/雲端存儲）</li>
     *   <li>用戶限制策略 - 控制上傳大小、檔案數量等資源使用限制</li>
     *   <li>驗證服務 - 執行請求參數的格式檢查和業務邏輯驗證</li>
     *   <li>權限服務 - 實現基於角色和檔案層級的存取權限控制</li>
     *   <li>權限規則管理器 - 動態管理檔案存取規則和權限繼承</li>
     * </ul>
     *
     * @param userService 用戶服務實例，負責用戶身份認證、授權和基本資料操作
     * @param fileServiceStrategy 檔案服務策略實例，提供多種檔案存儲和操作實現
     * @param userLimiterStrategy 用戶限制策略實例，控制用戶資源使用限制和頻率限制
     * @param validationService 驗證服務實例，執行請求參數格式檢查和業務邏輯驗證
     * @param fileProperties 檔案配置屬性實例，定義檔案處理的基本參數和限制
     * @param objectMapper Jackson JSON 對象映射器，處理請求和響應的序列化與反序列化
     * @param permissionService 權限服務實例，實現用戶檔案操作的細粒度權限驗證
     * @param filePermissionRuleManager 檔案權限規則管理器實例，動態管理檔案存取規則
     */
    public ApiGeneralFileController(UserService userService, FileServiceStrategy fileServiceStrategy, UserLimiterStrategy userLimiterStrategy, ValidationService validationService, FileProperties fileProperties, ObjectMapper objectMapper, PermissionService<UserFileMetadata> permissionService, FilePermissionRuleManager filePermissionRuleManager) {
        super(userService,
              fileServiceStrategy,
              fileProperties,
              validationService,
              permissionService,
              userLimiterStrategy,
              objectMapper,
              filePermissionRuleManager
        );
    }


    /**
     * 初始化檔案上傳處理，驗證檔案元資料並準備分塊上傳作業。
     * <p>
     * 此端點為檔案上傳的第一階段，負責驗證上傳請求的合法性並初始化上傳會話。
     * 支援大檔案分塊上傳，避免記憶體溢出問題。系統會檢查用戶權限、檔案大小限制、
     * 存儲空間配額等條件，確保上傳操作的安全性和可行性。
     * <p>
     * HTTP 端點資訊：
     * <ul>
     *   <li><strong>方法：</strong>POST</li>
     *   <li><strong>路徑：</strong>/api/v1/files/upload</li>
     *   <li><strong>內容類型：</strong>application/json</li>
     *   <li><strong>認證：</strong>需要有效的 JWT Token</li>
     * </ul>
     * <p>
     * 驗證項目：
     * <ul>
     *   <li>檔案名稱格式和長度檢查</li>
     *   <li>檔案大小是否超過系統限制</li>
     *   <li>檔案類型是否在允許清單中</li>
     *   <li>用戶存儲配額檢查</li>
     *   <li>目標資料夾權限驗證</li>
     *   <li>檔案名稱衝突檢查</li>
     * </ul>
     * <p>
     * 成功響應包含：
     * <ul>
     *   <li>上傳會話 ID</li>
     *   <li>分塊大小建議</li>
     *   <li>上傳端點 URL</li>
     *   <li>檔案唯一識別碼</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>400 Bad Request - 請求參數無效</li>
     *   <li>401 Unauthorized - 身份認證失敗</li>
     *   <li>403 Forbidden - 權限不足</li>
     *   <li>413 Payload Too Large - 檔案超過大小限制</li>
     *   <li>507 Insufficient Storage - 存儲空間不足</li>
     * </ul>
     * <p>
     * 請求範例：
     * <pre>
     * {
     *   "filename": "document.pdf",
     *   "fileSize": 1048576,
     *   "mimeType": "application/pdf",
     *   "parentFolderId": 123,
     *   "checksum": "sha256hash"
     * }
     * </pre>
     *
     * @param fileMetadataDTO 檔案元資料傳輸物件，包含檔案名稱、大小、類型、目標位置等資訊
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份、請求標頭和上下文資訊
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞響應，成功時返回上傳會話資訊，失敗時返回錯誤詳情
     * @see FileMetadataDTO
     * @see BaseGeneralFileController#uploadFile(FileMetadataDTO, ServerWebExchange)
     */
    @PostMapping("/upload")
    public Mono<ResponseEntity<?>> uploadFile(@RequestBody FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        return super.uploadFile(fileMetadataDTO, exchange);
    }


    /**
     * 下載或預覽指定檔案，提供靈活的檔案存取方式。
     * <p>
     * 此端點支援兩種主要操作模式：檔案預覽和檔案下載。使用反應式流處理，
     * 支援大檔案的非阻塞傳輸，有效避免記憶體溢出問題。系統會根據檔案類型
     * 和用戶權限動態調整響應行為。
     * <p>
     * HTTP 端點資訊：
     * <ul>
     *   <li><strong>方法：</strong>GET</li>
     *   <li><strong>路徑：</strong>/api/v1/files/{id}</li>
     *   <li><strong>認證：</strong>需要有效的 JWT Token</li>
     *   <li><strong>支援範圍請求：</strong>支援 HTTP Range 標頭，用於斷點續傳</li>
     * </ul>
     * <p>
     * 操作模式：
     * <ul>
     *   <li><strong>preview：</strong>預覽模式，適合在瀏覽器中直接顯示檔案內容</li>
     *   <li><strong>download：</strong>下載模式，觸發瀏覽器下載對話框</li>
     * </ul>
     * <p>
     * 權限檢查：
     * <ul>
     *   <li>驗證用戶對檔案的讀取權限</li>
     *   <li>檢查檔案分享狀態和可見性</li>
     *   <li>確認檔案未被標記為已刪除</li>
     *   <li>驗證檔案存取時間限制</li>
     * </ul>
     * <p>
     * 響應標頭設定：
     * <ul>
     *   <li><strong>Content-Type：</strong>根據檔案類型設定 MIME 類型</li>
     *   <li><strong>Content-Length：</strong>檔案大小資訊</li>
     *   <li><strong>Content-Disposition：</strong>根據模式設定 inline 或 attachment</li>
     *   <li><strong>Cache-Control：</strong>快取控制策略</li>
     *   <li><strong>ETag：</strong>檔案版本標識，支援條件請求</li>
     * </ul>
     * <p>
     * 效能優化：
     * <ul>
     *   <li>使用反應式 DataBuffer 流避免記憶體溢出</li>
     *   <li>支援 HTTP 快取機制提升重複存取效率</li>
     *   <li>分塊傳輸適應不同網路環境</li>
     *   <li>支援範圍請求實現斷點續傳</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>400 Bad Request - 無效的檔案 ID 或操作參數</li>
     *   <li>401 Unauthorized - 身份認證失敗</li>
     *   <li>403 Forbidden - 權限不足</li>
     *   <li>404 Not Found - 檔案不存在或已被刪除</li>
     *   <li>416 Range Not Satisfiable - 範圍請求無效</li>
     * </ul>
     * <p>
     * 使用範例：
     * <pre>
     * // 預覽檔案
     * GET /api/v1/files/12345?action=preview
     * 
     * // 下載檔案
     * GET /api/v1/files/12345?action=download
     * 
     * // 範圍請求（斷點續傳）
     * GET /api/v1/files/12345
     * Range: bytes=1024-2047
     * </pre>
     *
     * @param action 操作類型參數，預設值為 "preview"，可選值為 "download"，控制瀏覽器行為
     * @param id 檔案唯一識別碼，用於定位目標檔案
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份、請求標頭和上下文資訊
     * @return Mono&lt;ResponseEntity&lt;Flux&lt;DataBuffer&gt;&gt;&gt; 非阻塞響應，包含檔案資料流和適當的 HTTP 標頭
     * @see BaseGeneralFileController#downloadFile(String, Long, ServerWebExchange)
     * @see ValidationException
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(
            @RequestParam(value = "action", defaultValue = "preview", required = false) String action,
            @PathVariable Long id, ServerWebExchange exchange) {
        return super.downloadFile(action, id, exchange).onErrorResume(ValidationException.class, e -> handleDownloadValidationError(e, exchange));
    }


    /**
     * 永久刪除指定檔案，執行不可逆的檔案移除操作。
     * <p>
     * 此端點執行檔案的永久性刪除，與移至回收站的軟刪除不同，
     * 此操作將完全移除檔案的所有資料和元資料，且無法恢復。
     * 適用於確定不再需要的檔案或清理回收站中的檔案。
     * <p>
     * HTTP 端點資訊：
     * <ul>
     *   <li><strong>方法：</strong>DELETE</li>
     *   <li><strong>路徑：</strong>/api/v1/files/{id}</li>
     *   <li><strong>認證：</strong>需要有效的 JWT Token</li>
     *   <li><strong>權限要求：</strong>檔案擁有者或具有刪除權限的用戶</li>
     * </ul>
     * <p>
     * 刪除範圍：
     * <ul>
     *   <li>檔案的實際內容資料</li>
     *   <li>檔案的元資料記錄</li>
     *   <li>檔案的版本歷史記錄</li>
     *   <li>檔案的分享記錄和權限設定</li>
     *   <li>檔案的索引和快取資料</li>
     * </ul>
     * <p>
     * 安全檢查：
     * <ul>
     *   <li>驗證用戶身份和權限</li>
     *   <li>確認檔案所有權或刪除權限</li>
     *   <li>檢查檔案當前狀態（是否已被鎖定）</li>
     *   <li>記錄刪除操作的審計日誌</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>400 Bad Request - 無效的檔案 ID 格式</li>
     *   <li>401 Unauthorized - 身份認證失敗</li>
     *   <li>403 Forbidden - 權限不足，非檔案擁有者</li>
     *   <li>404 Not Found - 檔案不存在</li>
     *   <li>409 Conflict - 檔案正在使用中，無法刪除</li>
     *   <li>500 Internal Server Error - 刪除過程中發生系統錯誤</li>
     * </ul>
     * <p>
     * 重要警告：
     * <ul>
     *   <li><strong>不可逆操作：</strong>刪除後檔案無法恢復</li>
     *   <li><strong>影響範圍：</strong>將影響所有引用此檔案的分享連結</li>
     *   <li><strong>審計記錄：</strong>刪除操作將被記錄在系統日誌中</li>
     * </ul>
     * <p>
     * 使用建議：
     * <ul>
     *   <li>在執行永久刪除前，建議先使用軟刪除（移至回收站）</li>
     *   <li>重要檔案建議進行備份後再執行刪除</li>
     *   <li>定期清理回收站中的檔案以釋放存儲空間</li>
     * </ul>
     *
     * @param id 檔案唯一識別碼，必須為有效的檔案 ID
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份認證和請求上下文資訊
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞響應，成功時返回 200 OK，失敗時返回對應的錯誤狀態
     * @see BaseGeneralFileController#deleteFile(String, ServerWebExchange)
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFile(@PathVariable String id, ServerWebExchange exchange) {
        return super.deleteFile(id, exchange);
    }


    /**
     * 編輯和更新檔案的各種屬性，包括名稱、位置、分享設定等。
     * <p>
     * 此端點提供全面的檔案編輯功能，支援檔案重命名、移動、分享設定修改等操作。
     * 所有編輯操作都會進行完整的權限檢查和業務邏輯驗證，確保資料一致性和安全性。
     * 採用原子性操作設計，要麼全部成功，要麼全部失敗。
     * <p>
     * HTTP 端點資訊：
     * <ul>
     *   <li><strong>方法：</strong>PUT</li>
     *   <li><strong>路徑：</strong>/api/v1/files</li>
     *   <li><strong>內容類型：</strong>application/json</li>
     *   <li><strong>認證：</strong>需要有效的 JWT Token</li>
     * </ul>
     * <p>
     * 支援的編輯操作：
     * <ul>
     *   <li><strong>檔案重命名：</strong>修改檔案的顯示名稱</li>
     *   <li><strong>檔案移動：</strong>變更檔案所在的資料夾位置</li>
     *   <li><strong>分享設定：</strong>調整檔案的公開性和分享權限</li>
     *   <li><strong>描述編輯：</strong>更新檔案的描述資訊</li>
     *   <li><strong>標籤管理：</strong>新增或移除檔案標籤</li>
     * </ul>
     * <p>
     * 驗證機制：
     * <ul>
     *   <li>檔案 ID 有效性檢查</li>
     *   <li>檔案名稱格式和長度驗證</li>
     *   <li>目標資料夾存在性和權限檢查</li>
     *   <li>檔案名稱衝突檢查</li>
     *   <li>用戶對檔案的編輯權限驗證</li>
     * </ul>
     * <p>
     * 安全措施：
     * <ul>
     *   <li>防止路徑遍歷攻擊</li>
     *   <li>檔案擁有權驗證</li>
     *   <li>防止惡意檔案名稱</li>
     *   <li>審計日誌記錄</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>400 Bad Request - 請求參數無效或格式錯誤</li>
     *   <li>401 Unauthorized - 身份認證失敗</li>
     *   <li>403 Forbidden - 權限不足</li>
     *   <li>404 Not Found - 檔案或目標資料夾不存在</li>
     *   <li>409 Conflict - 檔案名稱衝突或目標位置無效</li>
     * </ul>
     * <p>
     * 請求範例：
     * <pre>
     * {
     *   "fileId": "12345",
     *   "filename": "新檔案名稱.pdf",
     *   "parentFolderId": 67890,
     *   "isShared": true,
     *   "description": "更新後的檔案描述"
     * }
     * </pre>
     *
     * @param fileEditDTO 檔案編輯資料傳輸物件，包含要修改的檔案屬性和新值
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份認證和請求上下文資訊
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞響應，成功時返回更新結果，失敗時返回錯誤資訊
     * @see FileEditDTO
     * @see BaseGeneralFileController#editFile(FileEditDTO, ServerWebExchange)
     */
    @PutMapping("")
    public Mono<ResponseEntity<?>> editFile(@Validated @RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.editFile(fileEditDTO, exchange);
    }


    /**
     * 上傳檔案分塊資料，實現大檔案的分段傳輸功能。
     * <p>
     * 此端點為檔案上傳的第二階段，接收檔案的實際內容資料。支援多種傳輸方式
     * 和分塊上傳策略，有效處理大檔案的上傳需求。使用反應式流處理，
     * 避免大檔案上傳時的記憶體溢出問題。
     * <p>
     * HTTP 端點資訊：
     * <ul>
     *   <li><strong>方法：</strong>POST</li>
     *   <li><strong>路徑：</strong>/api/v1/files/upload-chunk</li>
     *   <li><strong>內容類型：</strong>multipart/form-data 或 application/octet-stream</li>
     *   <li><strong>認證：</strong>需要有效的 JWT Token</li>
     *   <li><strong>前置條件：</strong>必須先呼叫 uploadFile 初始化上傳會話</li>
     * </ul>
     * <p>
     * 支援的傳輸類型：
     * <ul>
     *   <li><strong>standard：</strong>標準分塊上傳，適用於一般檔案</li>
     *   <li><strong>websocket：</strong>WebSocket 即時上傳，支援進度回報</li>
     *   <li><strong>multipart：</strong>多部分表單上傳，相容性最佳</li>
     *   <li><strong>stream：</strong>串流上傳，適用於大檔案</li>
     * </ul>
     * <p>
     * 分塊處理特性：
     * <ul>
     *   <li>支援斷點續傳，上傳中斷後可從斷點繼續</li>
     *   <li>自動分塊大小調整，根據網路狀況優化</li>
     *   <li>並行分塊上傳，提升上傳效率</li>
     *   <li>分塊完整性檢查，確保資料準確性</li>
     *   <li>上傳進度即時回報</li>
     * </ul>
     * <p>
     * 安全機制：
     * <ul>
     *   <li>上傳會話驗證，防止未授權上傳</li>
     *   <li>檔案大小限制檢查</li>
     *   <li>檔案類型驗證</li>
     *   <li>惡意內容掃描</li>
     *   <li>上傳速率限制</li>
     * </ul>
     * <p>
     * 效能優化：
     * <ul>
     *   <li>使用 DataBuffer 避免記憶體複製</li>
     *   <li>非阻塞 I/O 提升並發能力</li>
     *   <li>分塊並行處理</li>
     *   <li>智慧重試機制</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>400 Bad Request - 無效的上傳會話或分塊資料</li>
     *   <li>401 Unauthorized - 身份認證失敗</li>
     *   <li>403 Forbidden - 權限不足或超出配額</li>
     *   <li>413 Payload Too Large - 分塊大小超過限制</li>
     *   <li>422 Unprocessable Entity - 檔案內容檢查失敗</li>
     *   <li>507 Insufficient Storage - 存儲空間不足</li>
     * </ul>
     * <p>
     * 上傳流程：
     * <ol>
     *   <li>呼叫 uploadFile 端點初始化上傳會話</li>
     *   <li>使用此端點逐一上傳檔案分塊</li>
     *   <li>系統自動組合分塊並驗證完整性</li>
     *   <li>上傳完成後返回檔案資訊</li>
     * </ol>
     *
     * @param transmissionType 傳輸類型參數，指定使用的上傳方式（standard、websocket、multipart、stream）
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份、請求資料和上下文資訊
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞響應，成功時返回上傳進度，失敗時返回錯誤詳情
     * @see BaseGeneralFileController#uploadFileData(String, ServerWebExchange)
     */
    @PostMapping("/upload-chunk")
    public Mono<ResponseEntity<?>> uploadFileData(
            @RequestParam(name = "type", required = false) String transmissionType, ServerWebExchange exchange) {
        return handleError(super.uploadFileData(transmissionType, exchange), exchange);
    }


    /**
     * 獲取用戶檔案清單，提供灵活的分頁和過濾功能。
     * <p>
     * 此端點提供完整的用戶檔案管理清單，支援多維度的過濾和排序功能。
     * 使用反應式分頁機制，適合處理大量檔案的情況。所有返回的檔案
     * 都會通過權限檢查，確保用戶只能看到具有存取權限的檔案。
     * <p>
     * HTTP 端點資訊：
     * <ul>
     *   <li><strong>方法：</strong>GET</li>
     *   <li><strong>路徑：</strong>/api/v1/files/user-file-list</li>
     *   <li><strong>認證：</strong>需要有效的 JWT Token</li>
     *   <li><strong>響應格式：</strong>application/json</li>
     * </ul>
     * <p>
     * 支援的過濾選項：
     * <ul>
     *   <li><strong>檔案類型：</strong>按照 MIME 類型進行過濾（image、document、video 等）</li>
     *   <li><strong>上傳時間：</strong>按照檔案創建時間進行範圍過濾</li>
     *   <li><strong>檔案大小：</strong>按照檔案大小進行範圍過濾</li>
     *   <li><strong>分享狀態：</strong>显示公開分享或私人檔案</li>
     * </ul>
     * <p>
     * 分頁參數：
     * <ul>
     *   <li><strong>page：</strong>頁碼，從 1 開始，預設為第 1 頁</li>
     *   <li><strong>size：</strong>每頁項目數量，預設為系統設定值</li>
     *   <li><strong>無穷分頁：</strong>支援無究滿動加載模式</li>
     * </ul>
     * <p>
     * 返回資料結構：
     * <ul>
     *   <li><strong>檔案列表：</strong>包含檔案基本資訊的頁面化清單</li>
     *   <li><strong>分頁資訊：</strong>總數量、當前頁數、總頁數等</li>
     *   <li><strong>排序資訊：</strong>當前的排序方式和字段</li>
     *   <li><strong>過濾資訊：</strong>當前套用的過濾條件</li>
     * </ul>
     * <p>
     * 性能優化：
     * <ul>
     *   <li>使用資料庫索引提升查詢效率</li>
     *   <li>支援結果集快取，減少重複查詢</li>
     *   <li>懶性加載檔案內容，避免不必要的資料傳輸</li>
     *   <li>分塊加載大量檔案清單</li>
     * </ul>
     * <p>
     * 安全機制：
     * <ul>
     *   <li>按照用戶權限過濾結果</li>
     *   <li>防止數據泄漏和越權存取</li>
     *   <li>限制查詢參數範圍，防止暗力攻擊</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>400 Bad Request - 無效的分頁參數或過濾條件</li>
     *   <li>401 Unauthorized - 身份認證失敗</li>
     *   <li>403 Forbidden - 權限不足</li>
     * </ul>
     * <p>
     * 使用範例：
     * <pre>
     * // 獲取第 2 頁，每頁 20 個項目
     * GET /api/v1/files/user-file-list?page=2&size=20
     * 
     * // 獲取只包含圖片和文檔的清單
     * GET /api/v1/files/user-file-list?type=image&type=document
     * 
     * // 組合過濾條件
     * GET /api/v1/files/user-file-list?page=1&size=10&type=image
     * </pre>
     *
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份認證和請求上下文資訊
     * @param page 分頁頁碼，從 1 開始計數，預設值為 1
     * @param size 每頁显示的檔案數量，預設使用系統設定值
     * @param types 檔案類型過濾清單，支援多選（image、document、video、audio 等）
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞響應，包含分頁化的檔案清單資料
     * @see BaseGeneralFileController#getUserFileList(ServerWebExchange, Integer, Integer, List)
     * @see HideOverLength
     */
    @HideOverLength
    @GetMapping("/user-file-list")
    public Mono<ResponseEntity<?>> getUserFileList(ServerWebExchange exchange,
                                                   @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                   @RequestParam(value = "size", required = false) Integer size,
                                                   @RequestParam(value = "type", required = false) List<String> types) {
        return super.getUserFileList(exchange, page, size, types);
    }


    /**
     * 獲取指定檔案的詳細元資料資訊，不包含檔案內容本身。
     * <p>
     * 此端點提供檔案的完整元資料資訊，包括檔案屬性、統計資訊、
     * 權限設定和歷史資料等。適用於檔案管理、屬性查看和統計分析等場景。
     * 所有返回的資訊都會經過權限篩選，確保不會洩漏敏感資訊。
     * <p>
     * HTTP 端點資訊：
     * <ul>
     *   <li><strong>方法：</strong>GET</li>
     *   <li><strong>路徑：</strong>/api/v1/files/{id}/info</li>
     *   <li><strong>認證：</strong>需要有效的 JWT Token</li>
     *   <li><strong>響應格式：</strong>application/json</li>
     * </ul>
     * <p>
     * 返回的檔案資訊包括：
     * <ul>
     *   <li><strong>基本資訊：</strong>檔案名稱、大小、類型、擁有者</li>
     *   <li><strong>時間資訊：</strong>建立時間、修改時間、最後存取時間</li>
     *   <li><strong>存儲資訊：</strong>存儲位置、檢查和、版本資訊</li>
     *   <li><strong>權限資訊：</strong>存取權限、分享狀態、可見性</li>
     *   <li><strong>統計資訊：</strong>下載次數、存取頁率、使用統計</li>
     *   <li><strong>關聯資訊：</strong>所在資料夾、標籤、關鍵字</li>
     * </ul>
     * <p>
     * 權限檢查：
     * <ul>
     *   <li>驗證用戶對檔案的讀取權限</li>
     *   <li>支援共享檔案的資訊查看</li>
     *   <li>按照權限等級篩選異示資訊</li>
     * </ul>
     * <p>
     * 安全考量：
     * <ul>
     *   <li>不顯示系統內部路徑和敏感資訊</li>
     *   <li>對敏感欄位進行遮蔽或加密</li>
     *   <li>記錄資訊存取審計日誌</li>
     * </ul>
     * <p>
     * 使用場景：
     * <ul>
     *   <li><strong>檔案屬性查看：</strong>在檔案管理器中显示詳細資訊</li>
     *   <li><strong>下載準備：</strong>在下載前獲取檔案大小和類型</li>
     *   <li><strong>分享管理：</strong>查看和管理檔案的分享設定</li>
     *   <li><strong>統計分析：</strong>分析檔案使用情況和趨勢</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>400 Bad Request - 無效的檔案 ID 格式</li>
     *   <li>401 Unauthorized - 身份認證失敗</li>
     *   <li>403 Forbidden - 權限不足或檔案禁止存取</li>
     *   <li>404 Not Found - 檔案不存在或已被刪除</li>
     * </ul>
     *
     * @param id 檔案唯一識別碼，用於定位目標檔案
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份認證和請求上下文資訊
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞響應，包含檔案的詳細元資料資訊
     * @see BaseGeneralFileController#getFileType(Long, ServerWebExchange)
     */
    @GetMapping("/{id}/info")
    public Mono<ResponseEntity<?>> getFileInfo(@PathVariable Long id, ServerWebExchange exchange) {
        return super.getFileType(id, exchange);
    }


    /**
     * 將指定檔案移動到回收站，實現安全的軟刪除功能。
     * <p>
     * 此端點執行檔案的軟刪除操作，與永久刪除不同，檔案被移動到回收站後
     * 仍可以被還原。這種設計提供了安全網，防止意外刪除重要檔案。
     * 操作後檔案將在一般檔案清單中隱藏，但在回收站中可見。
     * <p>
     * HTTP 端點資訊：
     * <ul>
     *   <li><strong>方法：</strong>POST</li>
     *   <li><strong>路徑：</strong>/api/v1/files/remove/{id}</li>
     *   <li><strong>認證：</strong>需要有效的 JWT Token</li>
     *   <li><strong>權限要求：</strong>檔案擁有者或具有刪除權限的用戶</li>
     * </ul>
     * <p>
     * 軟刪除特性：
     * <ul>
     *   <li><strong>可還原性：</strong>檔案可以通過還原功能重新啟用</li>
     *   <li><strong>完整保存：</strong>檔案內容和元資料都完整保留</li>
     *   <li><strong>權限維持：</strong>原有的分享和權限設定保持不變</li>
     *   <li><strong>回收站管理：</strong>所有軟刪除的檔案都可在回收站中管理</li>
     * </ul>
     * <p>
     * 操作流程：
     * <ol>
     *   <li>驗證用戶身份和權限</li>
     *   <li>檢查檔案當前狀態（是否已被刪除）</li>
     *   <li>更新檔案狀態為已刪除</li>
     *   <li>記錄刪除操作審計日誌</li>
     *   <li>更新用戶存儲統計</li>
     * </ol>
     * <p>
     * 安全機制：
     * <ul>
     *   <li>只有檔案擁有者或授權用戶才能執行</li>
     *   <li>防止重複刪除已在回收站中的檔案</li>
     *   <li>記錄操作者身份和時間</li>
     * </ul>
     * <p>
     * 和永久刪除的區別：
     * <ul>
     *   <li><strong>軟刪除：</strong>檔案移入回收站，可還原</li>
     *   <li><strong>永久刪除：</strong>完全移除檔案，不可還原</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>400 Bad Request - 無效的檔案 ID 或檔案已在回收站</li>
     *   <li>401 Unauthorized - 身份認證失敗</li>
     *   <li>403 Forbidden - 權限不足，非檔案擁有者</li>
     *   <li>404 Not Found - 檔案不存在</li>
     *   <li>409 Conflict - 檔案正在使用中，無法刪除</li>
     * </ul>
     * <p>
     * 使用建議：
     * <ul>
     *   <li>在執行永久刪除前，建議先使用軟刪除</li>
     *   <li>定期清理回收站以釋放存儲空間</li>
     *   <li>為重要檔案設置額外的確認步驟</li>
     * </ul>
     *
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份認證和請求上下文資訊
     * @param id 檔案唯一識別碼，指定要移動到回收站的檔案
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞響應，成功時返回 200 OK，失敗時返回錯誤詳情
     * @see BaseFileController#removeFile(ServerWebExchange, String, xyz.dowob.filemanagement.customenum.FileEnum)
     */
    @PostMapping("/remove/{id}")
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.removeFile(exchange, id, null);
    }


    /**
     * 從回收站還原指定檔案，重新啟用已軟刪除的檔案。
     * <p>
     * 此端點提供檔案的還原功能，將之前移入回收站的檔案重新啟用。
     * 還原後的檔案將恢復到原來的位置和狀態，所有原有的權限和分享設定
     * 都會被保持。這是軟刪除機制的重要組成部分。
     * <p>
     * HTTP 端點資訊：
     * <ul>
     *   <li><strong>方法：</strong>POST</li>
     *   <li><strong>路徑：</strong>/api/v1/files/restore/{id}</li>
     *   <li><strong>認證：</strong>需要有效的 JWT Token</li>
     *   <li><strong>權限要求：</strong>檔案擁有者或具有還原權限的用戶</li>
     * </ul>
     * <p>
     * 還原流程：
     * <ol>
     *   <li>驗證用戶身份和權限</li>
     *   <li>檢查檔案是否在回收站中</li>
     *   <li>驗證目標位置的有效性（原來的父資料夾是否仍存在）</li>
     *   <li>檢查檔案名稱衝突（同位置是否已有同名檔案）</li>
     *   <li>還原檔案狀態為正常</li>
     *   <li>更新系統索引和快取</li>
     * </ol>
     * <p>
     * 還原特性：
     * <ul>
     *   <li><strong>狀態恢復：</strong>檔案從已刪除狀態變為正常狀態</li>
     *   <li><strong>位置恢復：</strong>檔案返回到原來的資料夾位置</li>
     *   <li><strong>權限恢復：</strong>所有原有的分享和權限設定恢復</li>
     *   <li><strong>索引更新：</strong>檔案重新出現在正常檔案清單中</li>
     * </ul>
     * <p>
     * 智慧處理：
     * <ul>
     *   <li><strong>位置檢查：</strong>如果原來的父資料夾不存在，可選擇新位置</li>
     *   <li><strong>名稱衝突：</strong>自動處理同名檔案衝突（添加後綴或編號）</li>
     *   <li><strong>權限繼承：</strong>如果目標位置權限有變，自動調整</li>
     * </ul>
     * <p>
     * 安全機制：
     * <ul>
     *   <li>只有檔案擁有者或授權用戶才能還原</li>
     *   <li>驗證檔案在還原前確實在回收站中</li>
     *   <li>記錄還原操作的審計日誌</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>400 Bad Request - 檔案不在回收站中或無法還原</li>
     *   <li>401 Unauthorized - 身份認證失敗</li>
     *   <li>403 Forbidden - 權限不足，非檔案擁有者</li>
     *   <li>404 Not Found - 檔案不存在或已被永久刪除</li>
     *   <li>409 Conflict - 目標位置有同名檔案衝突</li>
     * </ul>
     * <p>
     * 使用建議：
     * <ul>
     *   <li>在還原前確認目標位置的有效性</li>
     *   <li>對於重要檔案，建議還原後驗證完整性</li>
     *   <li>還原大量檔案時考慮性能影響</li>
     * </ul>
     *
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份認證和請求上下文資訊
     * @param id 檔案唯一識別碼，指定要從回收站還原的檔案
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞響應，成功時返回 200 OK，失敗時返回錯誤詳情
     * @see BaseFileController#restoreFile(ServerWebExchange, String, xyz.dowob.filemanagement.customenum.FileEnum)
     */
    @PostMapping("/restore/{id}")
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.restoreFile(exchange, id, null);
    }


    /**
     * 執行全方位的進階檔案搜尋，支援多維度條件組合和智慧過濾。
     * <p>
     * 此端點提供強大的檔案搜尋功能，支援文字搜尋、類型過濾、時間範圍、位置限制等多種條件的組合。
     * 採用高效的索引機制和智慧匹配算法，能夠快速處理大量檔案的搜尋需求。
     * 所有搜尋結果都會經過權限篩選，確保用戶只能看到有權限存取的檔案。
     * <p>
     * HTTP 端點資訊：
     * <ul>
     *   <li><strong>方法：</strong>GET</li>
     *   <li><strong>路徑：</strong>/api/v1/files/search</li>
     *   <li><strong>認證：</strong>需要有效的 JWT Token</li>
     *   <li><strong>響應格式：</strong>application/json</li>
     * </ul>
     * <p>
     * 支援的搜尋條件：
     * <ul>
     *   <li><strong>關鍵字搜尋：</strong>支援檔案名稱的模糊匹配和全文搜尋</li>
     *   <li><strong>檔案類型：</strong>按照 MIME 類型或檔案擴展名過濾</li>
     *   <li><strong>位置限制：</strong>限定搜尋範圍在特定資料夾及其子目錄</li>
     *   <li><strong>時間範圍：</strong>按照檔案創建或修改時間過濾</li>
     *   <li><strong>狀態過濾：</strong>包含或排除已刪除、已分享的檔案</li>
     *   <li><strong>大小範圍：</strong>按照檔案大小進行範圍過濾</li>
     * </ul>
     * <p>
     * 搜尋算法特性：
     * <ul>
     *   <li><strong>模糊匹配：</strong>支援部分關鍵字匹配和拼寫容錯</li>
     *   <li><strong>優先級排序：</strong>根據相關性和匹配度智慧排序</li>
     *   <li><strong>即時搜尋：</strong>支援搜尋建議和自動完成</li>
     *   <li><strong>多語言支援：</strong>支援中文、英文等多種語言的搜尋</li>
     * </ul>
     * <p>
     * 分頁和排序：
     * <ul>
     *   <li><strong>靈活分頁：</strong>支援自訂分頁大小和頁碼</li>
     *   <li><strong>多重排序：</strong>支援按相關性、時間、大小等多種方式排序</li>
     *   <li><strong>無限制模式：</strong>size=0 時返回所有符合條件的結果</li>
     * </ul>
     * <p>
     * 權限和安全：
     * <ul>
     *   <li>只返回用戶有權限存取的檔案</li>
     *   <li>支援搜尋共享給用戶的檔案</li>
     *   <li>防止透過搜尋進行資訊偵察</li>
     *   <li>搜尋歷史和行為分析</li>
     * </ul>
     * <p>
     * 性能優化：
     * <ul>
     *   <li>使用全文索引提升搜尋速度</li>
     *   <li>智慧快取機制減少重複查詢</li>
     *   <li>分散式搜尋支援大規模部署</li>
     *   <li>懶性載入搜尋結果詳情</li>
     * </ul>
     * <p>
     * 搜尋範例：
     * <ul>
     *   <li><strong>基本搜尋：</strong>/search?keyword=報告</li>
     *   <li><strong>類型過濾：</strong>/search?keyword=設計&type=image&type=document</li>
     *   <li><strong>位置限制：</strong>/search?keyword=文檔&folder=123</li>
     *   <li><strong>時間範圍：</strong>/search?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59</li>
     *   <li><strong>狀態過濾：</strong>/search?keyword=備份&deleted=true</li>
     *   <li><strong>組合條件：</strong>/search?keyword=專案&type=document&folder=456&shared=true</li>
     * </ul>
     * <p>
     * 響應結構：
     * <ul>
     *   <li><strong>搜尋結果：</strong>符合條件的檔案清單</li>
     *   <li><strong>統計資訊：</strong>總數量、類型分布、大小統計</li>
     *   <li><strong>相關建議：</strong>相關搜尋詞和檔案推薦</li>
     *   <li><strong>分頁資訊：</strong>當前頁數、總頁數、總結果數</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>400 Bad Request - 無效的搜尋參數或日期格式</li>
     *   <li>401 Unauthorized - 身份認證失敗</li>
     *   <li>403 Forbidden - 權限不足或搜尋範圍受限</li>
     *   <li>422 Unprocessable Entity - 搜尋條件過於複雜或衝突</li>
     * </ul>
     *
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份認證和請求上下文資訊
     * @param keyword 搜尋關鍵字，支援檔案名稱模糊匹配和全文搜尋，可選參數
     * @param folderId 指定搜尋範圍的資料夾識別碼，限制搜尋在特定目錄及其子目錄中，可選參數
     * @param types 檔案類型過濾清單，支援多選（image、document、video、audio 等），可選參數
     * @param page 分頁頁碼，從 1 開始計數，預設值為 1
     * @param size 每頁項目數量，預設值為 0（返回所有結果），可設定具體數值進行分頁
     * @param deleted 是否包含已刪除的檔案，預設值為 false，設為 true 時搜尋回收站內容
     * @param shared 是否只包含已分享的檔案，預設值為 false，設為 true 時只搜尋公開或共享檔案
     * @param startDate 檔案創建時間範圍的起始時間，ISO 8601 格式，用於時間範圍過濾，可選參數
     * @param endDate 檔案創建時間範圍的結束時間，ISO 8601 格式，與 startDate 配合使用，可選參數
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞響應，包含分頁化的搜尋結果和相關統計資訊
     * @see FileFilterDTO
     * @see BaseGeneralFileController#searchFile(ServerWebExchange, FileFilterDTO)
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<?>> search(ServerWebExchange exchange,
                                          @RequestParam(value = "keyword", required = false) String keyword,
                                          @RequestParam(value = "folder", required = false) Long folderId,
                                          @RequestParam(value = "type", required = false) List<String> types,
                                          @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                          @RequestParam(value = "size", required = false, defaultValue = "0") Integer size,
                                          @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
                                          @RequestParam(value = "shared", required = false, defaultValue = "false") Boolean shared,
                                          @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                          LocalDateTime startDate,
                                          @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                          LocalDateTime endDate) {
        FileFilterDTO fileFilterDTO = new FileFilterDTO(keyword, folderId, getFileEnums(types), page, size, startDate, endDate, deleted, shared);
        return super.searchFile(exchange, fileFilterDTO);
    }
}
