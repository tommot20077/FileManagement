package xyz.dowob.filemanagement.controller.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.event.EventSink;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.DownloadActionEnum;
import xyz.dowob.filemanagement.customenum.EditTypeEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.event.FileEditedMessage;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.*;

/**
 * 基於 Spring WebFlux 反應式程式設計的線上檔案控制器抽象基類，提供完整的協作式文檔管理功能。
 *
 * <p>此抽象基類專門處理線上檔案（Online Document）的核心業務邏輯，在 {@link BaseFileController}
 * 的基礎檔案操作之上，新增了協作編輯、版本控制、即時通知等進階功能。
 * 整合事件驅動架構，實現真正的多使用者即時協作能力。</p>
 *
 * <h3>核心功能領域：</h3>
 * <ul>
 *   <li><strong>協作編輯</strong>：多使用者同時編輯文檔，即時同步變更</li>
 *   <li><strong>版本控制</strong>：自動檔案版本管理、歷史記錄查詢和版本回滾</li>
 *   <li><strong>即時通知</strong>：文檔變更事件的即時發布和通知機制</li>
 *   <li><strong>權限管理</strong>：細粒度的文檔存取控制和協作者管理</li>
 *   <li><strong>檔案生命週期</strong>：文檔建立、編輯、分享、刪除的完整流程</li>
 * </ul>
 *
 * <h3>技術架構特色：</h3>
 * <ul>
 *   <li><strong>事件驅動設計</strong>：透過 {@link EventSink} 實現非同步事件發布</li>
 *   <li><strong>強類型安全</strong>：使用範型的權限驗證和檔案類型檢查</li>
 *   <li><strong>反應式處理</strong>：全程非阻塞操作，支援高併發環境</li>
 *   <li><strong>功能組合</strong>：繼承 BaseFileController 的標準檔案操作，擴展線上功能</li>
 * </ul>
 *
 * <h3>檔案操作支援：</h3>
 * <ul>
 *   <li><strong>上傳檔案</strong>：支援各種檔案格式的上傳和編輯環境初始化</li>
 *   <li><strong>下載檔案</strong>：提供預覽和下載模式，支援動態內容生成</li>
 *   <li><strong>編輯檔案</strong>：支援內容編輯、屬性修改和結構調整</li>
 *   <li><strong>刪除檔案</strong>：提供軟刪除（回收站）和恢復功能</li>
 * </ul>
 *
 * <h3>協作編輯特性：</h3>
 * <ul>
 *   <li><strong>即時同步</strong>：文檔變更即時通知所有協作者</li>
 *   <li><strong>版本追蹤</strong>：自動記錄每次修改，支援歷史版本查詢</li>
 *   <li><strong>變更履歷</strong>：完整的編輯歷史記錄和修改者追蹤</li>
 *   <li><strong>權限控制</strong>：細粒度的讀取、編輯、管理權限分配</li>
 * </ul>
 *
 * <h3>使用範例：</h3>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/online-files")
 * public class ApiOnlineFileController extends BaseOnlineFileController {
 *     
 *     @PostMapping("/upload")
 *     public Mono<ResponseEntity<?>> upload(@RequestBody FileMetadataDTO fileMetadata,
 *                                           ServerWebExchange exchange) {
 *         return super.uploadFile(fileMetadata, exchange);
 *     }
 *     
 *     @PostMapping("/edit")
 *     public Mono<ResponseEntity<?>> edit(@RequestBody FileEditDTO editRequest,
 *                                         ServerWebExchange exchange) {
 *         return super.editFile(editRequest, exchange);
 *     }
 * }
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see BaseFileController
 * @see EventSink
 * @see FileEditedMessage
 * @see FileServiceStrategy
 * @see FilePermissionRuleManager
 */
@RecordLevel(LogLevelEnum.INFO)
public class BaseOnlineFileController extends BaseFileController {
    /**
     * 文檔編輯事件的反應式事件發布器，實現線上文檔變更的即時通知機制。
     *
     * <p>此事件發布器是協作編輯系統的核心元件，負責在文檔發生變更時向所有
     * 相關的協作者和系統組件發送通知。支援以下類型的事件：</p>
     * <ul>
     *   <li><strong>內容編輯</strong>：文檔內容的修改、新增、刪除</li>
     *   <li><strong>屬性更新</strong>：文檔名稱、描述、樊目位置等元資料變更</li>
     *   <li><strong>權限變更</strong>：分享設定、協作者權限調整</li>
     *   <li><strong>版本事件</strong>：新版本建立、版本回滾等操作</li>
     * </ul>
     *
     * <p><strong>事件發布機制：</strong></p>
     * <ul>
     *   <li>採用非阻塞式事件發布，不影響主業務流程的執行效率</li>
     *   <li>支援事件的可靠性传递和失敗重試機制</li>
     *   <li>提供事件的組成和過濾功能，減少不必要的通知</li>
     *   <li>整合系統監控和日誌記錄，提供完整的事件追蹤</li>
     * </ul>
     *
     * @see EventSink
     * @see FileEditedMessage
     */
    private final EventSink<FileEditedMessage> eventSink;

    /**
     * 建構線上檔案控制器基礎實例，初始化所有必要的業務服務和組件。
     *
     * <p>此建構函式在 {@link BaseFileController} 的基礎功能之上，新增了線上文檔
     * 特有的事件驅動機制，提供完整的協作編輯和即時通知能力。</p>
     *
     * <p><strong>初始化組件說明：</strong></p>
     * <ul>
     *   <li><strong>核心服務</strong>：繼承父類別的所有基礎檔案管理服務</li>
     *   <li><strong>事件系統</strong>：新增反應式事件發布器，支援協作通知</li>
     *   <li><strong>權限管理</strong>：強化細粒度權限控制，支援協作編輯權限</li>
     *   <li><strong>檔案策略</strong>：針對線上文檔的特定處理策略</li>
     * </ul>
     *
     * <p><strong>依賴注入系統：</strong></p>
     * <p>透過 Spring 的依賴注入機制，確保所有服務組件的正確初始化和生命週期管理，
     * 遵循單一職責原則和依賴倒置原則，提高系統的可測試性和可維護性。</p>
     *
     * @param userService 使用者業務層服務，提供用戶認證和資訊管理功能
     * @param fileServiceStrategy 檔案服務策略，用於動態選擇適當的檔案處理服務
     * @param fileProperties 檔案系統配置屬性，包含上傳限制、儲存路徑等設定
     * @param validationService 資料驗證服務，提供全方位的輸入驗證能力
     * @param permissionService 使用者檔案權限服務，實現細粒度的存取控制
     * @param objectMapper JSON 序列化和反序列化物件映射器
     * @param filePermissionRuleManager 檔案權限規則管理器，管理複雜的權限邏輯
     * @param eventSink 文檔編輯事件發布器，實現協作編輯的即時通知功能
     *
     * @see BaseFileController#BaseFileController
     * @see EventSink
     * @see FileServiceStrategy
     * @see FilePermissionRuleManager
     */
    public BaseOnlineFileController(UserService userService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, PermissionService<UserFileMetadata> permissionService, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager, EventSink<FileEditedMessage> eventSink) {
        super(userService, fileServiceStrategy, fileProperties, validationService, permissionService, objectMapper, filePermissionRuleManager);
        this.eventSink = eventSink;
    }


    /**
     * 處理線上文檔上傳請求，建立新的可編輯文檔並初始化編輯環境。
     *
     * <p>此方法專門處理線上文檔的上傳操作，與一般檔案上傳不同，
     * 它會建立一個支援協作編輯的線上文檔環境，包含版本控制、權限管理等進階功能。</p>
     *
     * <h3>上傳處理流程：</h3>
     * <ol>
     *   <li><strong>用戶認證</strong>：驗證當前用戶的身份和登入狀態</li>
     *   <li><strong>權限檢查</strong>：驗證用戶對目標資料夾的上傳權限</li>
     *   <li><strong>資料驗證</strong>：檢查檔案元資料的完整性和有效性</li>
     *   <li><strong>檔案建立</strong>：透過檔案服務建立線上文檔實例</li>
     *   <li><strong>環境初始化</strong>：設定版本控制、權限配置等</li>
     * </ol>
     *
     * <h3>線上文檔特性：</h3>
     * <ul>
     *   <li><strong>即時編輯</strong>：支援在線的即時編輯和內容修改</li>
     *   <li><strong>版本管理</strong>：自動建立初始版本，支援後續版本追蹤</li>
     *   <li><strong>協作支援</strong>：支援多使用者同時存取和編輯</li>
     *   <li><strong>分享機制</strong>：支援細粒度的分享權限設定</li>
     * </ul>
     *
     * <h3>權限驗證：</h3>
     * <ul>
     *   <li><strong>資料夾權限</strong>：驗證用戶對目標資料夾的寫入權限</li>
     *   <li><strong>儲存配額</strong>：檢查用戶的可用儲存空間</li>
     *   <li><strong>檔案類型</strong>：確認檔案類型符合線上編輯要求</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>上傳成功（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "上傳成功",
     *   "data": {
     *     "fileId": 12345,
     *     "fileName": "document.docx",
     *     "fileSize": 1048576,
     *     "uploadTime": "2024-01-01T12:00:00Z",
     *     "editUrl": "/edit/12345"
     *   },
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * @param fileMetadataDTO 檔案元資料傳輸物件，包含檔案名稱、大小、資料夾位置等資訊
     * @param exchange WebFlux 伺服器請求交換物件，包含請求內容和用戶資訊
     *
     * @return Mono<ResponseEntity<?>> 包含上傳結果和文檔資訊的反應式回應物件
     *
     * @see FileMetadataDTO
     * @see UserService#getUser(ServerWebExchange)
     * @see PermissionService#validateUserPermission
     * @see FileServiceStrategy#getFileService(FileEnum)
     */
    public Mono<ResponseEntity<?>> uploadFile(FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> action = userService.getUser(exchange).flatMap(user -> {
            Mono<UserFileMetadata> parentFolderMono = Mono.empty();
            if (fileMetadataDTO.getParentFolderId() != null) {
                parentFolderMono = permissionService
                        .validateUserPermission(user, fileMetadataDTO.getParentFolderId())
                        .flatMap(file -> validationService.validateFileType(file, FileEnum.FOLDER));
            }

            Mono<ResponseEntity<?>> responseEntityMono = fileServiceStrategy
                    .getFileService(FileEnum.ONLINE_DOCUMENT)
                    .uploadFile(fileMetadataDTO, user)
                    .flatMap(uploadResponseDTO -> createResponseEntity(createApiResponse(exchange, "上傳成功", uploadResponseDTO)));

            return parentFolderMono.then(responseEntityMono);
        });

        return handleError(action, exchange);
    }


    /**
     * 處理線上文檔下載請求，支援多種下載模式和即時內容生成。
     *
     * <p>此方法針對線上文檔提供灵活的下載方式，支援預覽模式和完整下載模式。
     * 系統會根據請求參數自動選擇適當的處理方式，確保最佳的使用者體驗。</p>
     *
     * <h3>下載模式支援：</h3>
     * <ul>
     *   <li><strong>預覽模式（action=preview）</strong>：
     *     <ul>
     *       <li>返回 JSON 格式的文檔內容和元資料</li>
     *       <li>適合在線檢視和快速預覽</li>
     *       <li>小容量傳輸，支援 AJAX 請求</li>
     *     </ul>
     *   </li>
     *   <li><strong>下載模式（action=download）</strong>：
     *     <ul>
     *       <li>直接下載檔案到本地設備</li>
     *       <li>支援斷點續傳和大檔案傳輸</li>
     *       <li>提供完整的檔案元資料和安全性檢查</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>權限驗證機制：</h3>
     * <ul>
     *   <li><strong>基本權限</strong>：驗證用戶對檔案的讀取權限</li>
     *   <li><strong>分享權限</strong>：支援透過分享鏈接存取的檔案</li>
     *   <li><strong>時效性檢查</strong>：驗證分享權限的有效期限</li>
     *   <li><strong>存取記錄</strong>：記錄下載操作的安全日誌</li>
     * </ul>
     *
     * <h3>內容處理流程：</h3>
     * <ol>
     *   <li><strong>用戶認證</strong>：驗證當前用戶的身份和權限</li>
     *   <li><strong>檔案驗證</strong>：確認檔案存在且為線上文檔類型</li>
     *   <li><strong>內容獲取</strong>：從儲存系統獲取最新的檔案內容</li>
     *   <li><strong>格式轉換</strong>：根據下載模式進行適當的內容轉換</li>
     *   <li><strong>回應生成</strong>：生成適當的 HTTP 回應和標頭</li>
     * </ol>
     *
     * <h3>錯誤處理：</h3>
     * <ul>
     *   <li><strong>403 Forbidden</strong>：用戶無權限存取檔案</li>
     *   <li><strong>404 Not Found</strong>：檔案不存在或已被刪除</li>
     *   <li><strong>410 Gone</strong>：檔案已過期或分享已失效</li>
     *   <li><strong>500 Internal Server Error</strong>：檔案讀取或轉換錯誤</li>
     * </ul>
     *
     * @param action 下載動作類型，支援 "preview"（預覽）和 "download"（下載）
     * @param id 檔案唯一識別碼，用於定位目標檔案
     * @param exchange WebFlux 伺服器請求交換物件，包含用戶資訊和請求參數
     *
     * @return Mono<ResponseEntity<Flux<DataBuffer>>> 包含檔案內容或預覽資訊的反應式回應
     *
     * @see DownloadActionEnum
     * @see FilePermissionRuleManager.DefaultRule#WITH_SHARED
     * @see ValidationService#validateFileType(UserFileMetadata, FileEnum...)
     * @see FileService#downloadFile
     */
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(String action, String id, ServerWebExchange exchange) {
        FileService fileService = fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT);
        DownloadActionEnum actionEnum = DownloadActionEnum.getType(action);
        Collection<Permission<UserFileMetadata>> rules = FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManager);
        return userService.getUser(exchange).flatMap(user -> {
            return permissionService.validateUserPermission(user, Long.parseLong(id), rules).flatMap(file -> {
                return validationService
                        .validateFileType(file, FileEnum.ONLINE_DOCUMENT)
                        .then(fileService.downloadFile(file, user, actionEnum.name()).flatMap(userFileDataBO -> {
                            try {
                                if (actionEnum == DownloadActionEnum.PREVIEW) {
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("content", userFileDataBO.getContent());
                                    data.put("filename", userFileDataBO.getFilename());
                                    ApiResponseDTO<?> apiResponse = createApiResponse(exchange, "下載成功", data);
                                    byte[] responseBytes = objectMapper.writeValueAsString(apiResponse).getBytes();
                                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
                                    return Mono.just(ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(Flux.just(buffer)));
                                }

                                HttpHeaders headers = prepareHttpHeaders(DownloadActionEnum.DOWNLOAD, userFileDataBO, null, false);
                                return Mono.just(ResponseEntity.status(200).headers(headers).body(userFileDataBO.getDataBufferFlux()));
                            } catch (JsonProcessingException ex) {
                                return Mono.error(new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, ex));
                            }
                        }));
            });
        }).onErrorResume(ValidationException.class, e -> handleDownloadValidationError(e, exchange));
    }


    /**
     * 處理線上文檔刪除請求，執行安全的檔案刪除操作並清理相關資源。
     *
     * <p>此方法專門處理線上文檔的刪除操作，與一般檔案刪除相比，
     * 需要額外處理協作編輯會話、版本歷史、分享連結等線上文檔特有的資源。</p>
     *
     * <h3>刪除處理流程：</h3>
     * <ol>
     *   <li><strong>用戶認證</strong>：驗證當前用戶的身份和登入狀態</li>
     *   <li><strong>權限驗證</strong>：確認用戶具有檔案的刪除權限（擁有者權限）</li>
     *   <li><strong>檔案驗證</strong>：確認目標檔案存在且為線上文檔類型</li>
     *   <li><strong>資源清理</strong>：刪除檔案內容、版本歷史、分享記錄等</li>
     *   <li><strong>通知發送</strong>：通知所有協作者檔案已被刪除</li>
     * </ol>
     *
     * <h3>權限要求：</h3>
     * <ul>
     *   <li><strong>擁有者權限</strong>：只有檔案擁有者才能執行刪除操作</li>
     *   <li><strong>非搜尋操作</strong>：禁止對搜尋結果中的檔案進行刪除</li>
     *   <li><strong>活躍狀態</strong>：只能刪除非回收站狀態的檔案</li>
     * </ul>
     *
     * <h3>刪除影響範圍：</h3>
     * <ul>
     *   <li><strong>檔案內容</strong>：從儲存系統中完全移除檔案內容</li>
     *   <li><strong>版本歷史</strong>：清除所有歷史版本和變更記錄</li>
     *   <li><strong>協作會話</strong>：結束所有活躍的編輯會話</li>
     *   <li><strong>分享連結</strong>：使所有分享連結失效</li>
     *   <li><strong>權限記錄</strong>：清除所有相關的權限設定</li>
     * </ul>
     *
     * <h3>安全性考量：</h3>
     * <ul>
     *   <li><strong>權限驗證</strong>：嚴格的擁有者權限檢查，防止未授權刪除</li>
     *   <li><strong>操作記錄</strong>：記錄刪除操作的完整安全日誌</li>
     *   <li><strong>不可回滾</strong>：刪除操作不可逆轉，需要謹慎執行</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>刪除成功（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "刪除成功",
     *   "data": null,
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * @param id 檔案唯一識別碼，用於定位要刪除的目標檔案
     * @param exchange WebFlux 伺服器請求交換物件，包含用戶資訊和請求內容
     *
     * @return Mono<ResponseEntity<?>> 包含刪除結果的反應式回應物件
     *
     * @see FilePermissionRuleManager#getAllowOwner()
     * @see FilePermissionRuleManager#getBlockNotSearchOperation()
     * @see ValidationService#validateFileType(UserFileMetadata, FileEnum...)
     * @see FileService#deleteFile
     */
    public Mono<ResponseEntity<?>> deleteFile(String id, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> action = userService.getUser(exchange).flatMap(user -> {
            List<Permission<UserFileMetadata>> rules = new ArrayList<>(List.of(filePermissionRuleManager.getAllowOwner(),
                                                                               filePermissionRuleManager.getBlockNotSearchOperation()
            ));
            return permissionService
                    .validateUserPermission(user, Long.parseLong(id), rules)
                    .flatMap(file -> validationService
                            .validateFileType(file, FileEnum.ONLINE_DOCUMENT)
                            .then(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT).deleteFile(file, user))
                            .then(createResponseEntity(createApiResponse(exchange, "刪除成功", null))));
        });

        return handleError(action, exchange);
    }


    /**
     * 處理線上文檔編輯請求，實現多種編輯模式和協作功能。
     *
     * <p>此方法是線上文檔系統的核心功能，支援多種編輯類型和協作模式。
     * 整合版本控制、權限管理和即時通知機制，提供完整的協作編輯體驗。</p>
     *
     * <h3>編輯類型支援：</h3>
     * <ul>
     *   <li><strong>內容編輯（EDIT_CONTENT）</strong>：
     *     <ul>
     *       <li>文檔內容的即時修改和更新</li>
     *       <li>支援多使用者同時編輯</li>
     *       <li>自動版本管理和衝突解決</li>
     *     </ul>
     *   </li>
     *   <li><strong>元資料編輯（EDIT_METADATA）</strong>：
     *     <ul>
     *       <li>檔案名稱、描述、標籤等屬性修改</li>
     *       <li>資料夾位置變更和組織結構調整</li>
     *       <li>權限設定和分享配置修改</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>權限管理機制：</h3>
     * <ul>
     *   <li><strong>內容編輯權限</strong>：支援分享用戶的协作編輯權限</li>
     *   <li><strong>元資料編輯權限</strong>：僅檔案擁有者可修改檔案屬性</li>
     *   <li><strong>資料夾權限</strong>：移動檔案時需要目標資料夾的寫入權限</li>
     * </ul>
     *
     * <h3>編輯處理流程：</h3>
     * <ol>
     *   <li><strong>資料驗證</strong>：驗證編輯請求資料的完整性和格式</li>
     *   <li><strong>用戶認證</strong>：驗證當前用戶的身份和登入狀態</li>
     *   <li><strong>權限檢查</strong>：根據編輯類型驗證相應的操作權限</li>
     *   <li><strong>檔案驗證</strong>：確認目標檔案和資料夾的有效性</li>
     *   <li><strong>編輯執行</strong>：透過檔案服務執行實際的編輯操作</li>
     *   <li><strong>事件通知</strong>：發送編輯事件通知所有協作者</li>
     * </ol>
     *
     * <h3>協作編輯特性：</h3>
     * <ul>
     *   <li><strong>即時同步</strong>：編輯完成後即時通知所有在線編輯者</li>
     *   <li><strong>版本管理</strong>：自動保存編輯歷史和版本資訊</li>
     *   <li><strong>衝突處理</strong>：智能合併同時編輯的衝突變更</li>
     *   <li><strong>狀態追蹤</strong>：實時追蹤所有編輯者的操作狀態</li>
     * </ul>
     *
     * <h3>事件通知機制：</h3>
     * <p>編輯成功後，系統會透過 {@link EventSink} 發送 {@link FileEditedMessage} 事件，
     * 通知所有相關的協作者和系統組件檔案已更新。</p>
     *
     * <h3>回應格式：</h3>
     * <p><strong>編輯成功（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "編輯成功",
     *   "data": null,
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * @param fileEditDTO 檔案編輯資料傳輸物件，包含編輯類型、目標檔案和編輯內容
     * @param exchange WebFlux 伺服器請求交換物件，包含用戶資訊和請求內容
     *
     * @return Mono<ResponseEntity<?>> 包含編輯結果的反應式回應物件
     *
     * @see FileEditDTO
     * @see EditTypeEnum
     * @see FileEditBO
     * @see ValidationService#validateEditFileDTO(FileEditDTO, boolean)
     * @see FileService#editFile
     * @see EventSink#emit(Object)
     */
    public Mono<ResponseEntity<?>> editFile(FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> action = validationService
                .validateEditFileDTO(fileEditDTO, false)
                .then(userService.getUser(exchange))
                .flatMap(user -> {
                    List<Long> fileIds = new ArrayList<>();
                    List<Permission<UserFileMetadata>> rules = new ArrayList<>();
                    Long fileId = Long.parseLong(fileEditDTO.getFileId());
                    fileIds.add(fileId);

                    if (fileEditDTO.getParentFolderId() != null && fileEditDTO.getEditType() == EditTypeEnum.EDIT_METADATA) {
                        fileIds.add(fileEditDTO.getParentFolderId());
                        rules.add(filePermissionRuleManager.getAllowOwner());
                    }
                    if (fileEditDTO.getEditType() != EditTypeEnum.EDIT_METADATA) {
                        rules.add(filePermissionRuleManager.getAllowShared());
                    }

                    FileEditBO fileEditBO = new FileEditBO(fileEditDTO);
                    return permissionService.validateUserPermission(user, fileIds, rules).flatMap(map -> {
                        fileEditBO.setParentFolderFileMetadata(map.get(fileEditDTO.getParentFolderId()));
                        fileEditBO.setUserFileMetadata(map.get(fileId));
                        return validationService
                                .validateFileType(fileEditBO.getUserFileMetadata(), FileEnum.ONLINE_DOCUMENT)
                                .then(validationService.validateFileType(fileEditBO.getParentFolderFileMetadata(), FileEnum.FOLDER))
                                .then(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT).editFile(fileEditBO, user))
                                .doOnSuccess(v -> {
                                    eventSink.emit(FileEditedMessage.of(fileEditBO.getUserFileMetadata(), user, fileEditDTO.getEditType()));
                                });
                    });
                })
                .then(createResponseEntity(createApiResponse(exchange, "編輯成功", null)));

        return handleError(action, exchange);
    }


    /**
     * 處理線上文檔版本歷史查詢請求，提供完整的文檔變更追蹤記錄。
     *
     * <p>此方法提供線上文檔的詳細版本歷史資訊，包含每次編輯的時間、作者、
     * 變更內容等詳細記錄。支援分頁查詢，適合處理大量歷史記錄的文檔。</p>
     *
     * <h3>歷史記錄內容：</h3>
     * <ul>
     *   <li><strong>版本資訊</strong>：版本編號、建立時間、版本大小</li>
     *   <li><strong>作者資訊</strong>：編輯者名稱、編輯時間、編輯類型</li>
     *   <li><strong>變更內容</strong>：變更範圍、修改註釋、影響範圍</li>
     *   <li><strong>統計資料</strong>：文字数量變化、編輯次數、協作者數量</li>
     * </ul>
     *
     * <h3>查詢功能特性：</h3>
     * <ul>
     *   <li><strong>分頁查詢</strong>：支援大量歷史記錄的分頁瀏覽</li>
     *   <li><strong>權限過濾</strong>：僅顯示用戶有權存取的版本資訊</li>
     *   <li><strong>時間排序</strong>：按照時間順序組織歷史記錄</li>
     *   <li><strong>快速存取</strong>：優化的查詢效率，支援大型文檔</li>
     * </ul>
     *
     * <h3>權限驗證：</h3>
     * <ul>
     *   <li><strong>讀取權限</strong>：驗證用戶對檔案的讀取或協作權限</li>
     *   <li><strong>分享權限</strong>：支援透過分享連結存取的檔案歷史</li>
     *   <li><strong>時效檢查</strong>：驗證分享權限的有效期限</li>
     * </ul>
     *
     * <h3>分頁參數說明：</h3>
     * <ul>
     *   <li><strong>page</strong>：當前頁碼，從 0 開始計數</li>
     *   <li><strong>pageSize</strong>：每頁顯示的記錄數量，建議範圍 10-100</li>
     *   <li><strong>預設值</strong>：未指定時使用系統預設的分頁設定</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>查詢成功（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "獲取歷程記錄成功",
     *   "data": {
     *     "content": [
     *       {
     *         "version": "1.2.3",
     *         "createdAt": "2024-01-01T12:00:00Z",
     *         "author": "testuser",
     *         "editType": "EDIT_CONTENT",
     *         "changes": "Updated chapter 3"
     *       }
     *     ],
     *     "pageable": {
     *       "page": 0,
     *       "size": 20,
     *       "totalElements": 45,
     *       "totalPages": 3
     *     }
     *   },
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * @param exchange WebFlux 伺服器請求交換物件，包含用戶資訊和請求參數
     * @param id 檔案唯一識別碼，用於定位目標檔案
     * @param page 當前頁碼，從 0 開始，用於分頁查詢
     * @param pageSize 每頁記錄數量，控制單次返回的資料量
     *
     * @return Mono<ResponseEntity<?>> 包含分頁歷史記錄的反應式回應物件
     *
     * @see FilePermissionRuleManager.DefaultRule#WITH_SHARED
     * @see ValidationService#validateFileType(UserFileMetadata, FileEnum...)
     * @see FileService#getFileVersionList
     */
    public Mono<ResponseEntity<?>> getHistory(ServerWebExchange exchange, String id, Integer page, Integer pageSize) {
        Mono<ResponseEntity<?>> responseEntityMono = userService.getUser(exchange).flatMap(user -> {
            return permissionService
                    .validateUserPermission(user,
                                            Long.parseLong(id),
                                            FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManager)
                    )
                    .flatMap(file -> validationService
                            .validateFileType(file, FileEnum.ONLINE_DOCUMENT)
                            .then(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT).getFileVersionList(user, file, page, pageSize))
                            .flatMap(history -> createResponseEntity(createApiResponse(exchange, "獲取歷程記錄成功", history))));
        });
        return handleError(responseEntityMono, exchange);
    }


    /**
     * 處理線上文檔移至回收站請求，實現安全的軟刪除功能。
     *
     * <p>此方法實現線上文檔的軟刪除功能，將檔案移動到回收站而非立即刪除。
     * 這樣設計提供了一個安全網，讓用戶能夠在需要時恢復意外刪除的檔案。</p>
     *
     * <p><strong>軟刪除特性：</strong></p>
     * <ul>
     *   <li>檔案被標記為已刪除狀態，但實際內容保持完整</li>
     *   <li>所有版本歷史和協作記錄都會被保留</li>
     *   <li>分享連結會被暫時停用，但不會完全刪除</li>
     *   <li>用戶可以在一定時間內從回收站恢復檔案</li>
     * </ul>
     *
     * <p><strong>權限要求：</strong></p>
     * <p>此操作繼承父類別的回收站權限設定，通常要求擁有者權限。</p>
     *
     * @param exchange WebFlux 伺服器請求交換物件，包含用戶資訊和請求內容
     * @param id 檔案唯一識別碼，用於定位要移入回收站的目標檔案
     *
     * @return Mono<ResponseEntity<?>> 包含移入回收站結果的反應式回應物件
     *
     * @see BaseFileController#removeFile(ServerWebExchange, String, FileEnum)
     */
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, String id) {
        return super.removeFile(exchange, id, null);
    }


    /**
     * 處理線上文檔從回收站恢復請求，重新啟用檔案的所有功能。
     *
     * <p>此方法實現線上文檔的恢復功能，將之前移入回收站的檔案重新啟用。
     * 恢復後的檔案將完全恢復到刪除前的狀態，包括所有協作功能和分享設定。</p>
     *
     * <p><strong>恢復功能特性：</strong></p>
     * <ul>
     *   <li>檔案狀態從已刪除恢復為正常可用</li>
     *   <li>所有版本歷史和协作記錄完整保留</li>
     *   <li>之前的分享設定和權限配置恢復生效</li>
     *   <li>恢復後可立即進行正常的編輯和協作操作</li>
     * </ul>
     *
     * <p><strong>權限要求：</strong></p>
     * <p>此操作繼承父類別的恢復權限設定，通常要求擁有者權限。</p>
     *
     * @param exchange WebFlux 伺服器請求交換物件，包含用戶資訊和請求內容
     * @param id 檔案唯一識別碼，用於定位要從回收站恢復的目標檔案
     *
     * @return Mono<ResponseEntity<?>> 包含檔案恢復結果的反應式回應物件
     *
     * @see BaseFileController#restoreFile(ServerWebExchange, String, FileEnum)
     */
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, String id) {
        return super.restoreFile(exchange, id, null);
    }
}
