package xyz.dowob.filemanagement.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.manager.FolderListTreeManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseFolderController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.FolderService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.List;

/**
 * 基於 WebFlux 反應式編程的資料夾 RESTful API 控制器實現。
 * <p>
 * 此控制器實現檔案管理系統中的資料夾操作核心功能，採用非阻塞 I/O 架構提供高併發處理能力。
 * 透過策略模式整合多種檔案服務實現，支援完整的 CRUD 操作以及進階檔案管理功能。
 * <p>
 * 主要功能範圍包括：
 * <ul>
 * <li>資料夾瀏覽與檔案列表查詢，支援分頁與檔案類型過濾</li>
 * <li>特殊檔案集合管理：星標檔案、最近存取檔案、分享檔案</li>
 * <li>回收站操作：軟刪除、還原、永久刪除</li>
 * <li>資料夾完整生命週期管理：建立、編輯、刪除、下載</li>
 * <li>目錄樹結構初始化與路徑查詢</li>
 * </ul>
 * <p>
 * 安全性實現：
 * <ul>
 * <li>所有端點均要求用戶身份認證</li>
 * <li>基於角色的權限控制系統</li>
 * <li>檔案存取權限細粒度驗證</li>
 * <li>請求參數驗證與惡意輸入防護</li>
 * </ul>
 * <p>
 * WebFlux 反應式特性：
 * <ul>
 * <li>非阻塞 I/O 操作，提升系統併發性能</li>
 * <li>背壓處理機制，避免記憶體溢位</li>
 * <li>流式資料處理，支援大型檔案操作</li>
 * <li>反應式錯誤處理與恢復策略</li>
 * </ul>
 * <p>
 * 繼承自 {@link BaseFolderController}，共享基礎業務邏輯與錯誤處理機制。
 * 透過 {@code @RecordLevel} 註解實現操作日誌記錄，支援系統審計與問題追蹤。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/api/v1/folders")
public class ApiFolderController extends BaseFolderController {
    /**
     * 初始化資料夾 API 控制器，透過依賴注入設定所需服務組件。
     * <p>
     * 此建構函式將所有必要的服務組件注入到控制器中，建立完整的資料夾管理功能架構。
     * 透過策略模式整合不同的檔案處理實現，提供靈活且可擴展的服務架構。
     *
     * @param userService 用戶服務介面實現，負責用戶身份認證、用戶資料管理與會話處理
     * @param permissionService 權限服務泛型實現，針對 {@link UserFileMetadata} 實體提供細粒度權限驗證
     * @param fileServiceStrategy 檔案服務策略實現，透過策略模式提供多種檔案操作方式的動態選擇
     * @param fileProperties 檔案配置屬性對象，包含檔案大小限制、存儲路徑、支援格式等系統參數
     * @param validationService 驗證服務介面，執行請求參數的格式檢查、業務規則驗證與資料完整性確認
     * @param folderService 資料夾服務介面，管理資料夾的完整業務邏輯，包括 CRUD 操作與樹狀結構維護
     * @param objectMapper Jackson JSON 對象映射器，處理請求與響應的序列化與反序列化操作
     * @param filePermissionRuleManager 檔案權限規則管理器，實現動態權限規則配置與驗證邏輯
     * @param folderListTreeManager 資料夾樹狀結構管理器，可選注入，負責目錄樹的快取與優化查詢功能
     */
    public ApiFolderController(UserService userService, PermissionService<UserFileMetadata> permissionService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, FolderService folderService, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager,
                               @Nullable FolderListTreeManager folderListTreeManager) {
        super(userService,
              permissionService,
              fileServiceStrategy,
              fileProperties,
              validationService,
              folderService,
              objectMapper,
              filePermissionRuleManager,
              folderListTreeManager
        );
    }


    /**
     * 獲取指定資料夾的檔案清單，支援分頁查詢與檔案類型過濾功能。
     * <p>
     * 此端點實現基於資料夾識別碼的檔案瀏覽功能，透過反應式程式設計模式提供高效能的檔案列表查詢。
     * 支援動態分頁載入與多種檔案類型過濾，適用於大型目錄的效能優化瀏覽。
     * <p>
     * HTTP 方法：GET
     * <br>路徑模式：{@code /api/v1/folders/{id}}
     * <br>Content-Type：application/json
     * <p>
     * 安全性考量：
     * <ul>
     * <li>要求用戶身份認證，透過 JWT 驗證用戶身份</li>
     * <li>驗證用戶對指定資料夾的讀取權限</li>
     * <li>檢查資料夾識別碼的有效性，拒絕負數或無效值</li>
     * <li>透過 {@code @HideOverLength} 註解避免過長響應內容的日誌洩露</li>
     * </ul>
     * <p>
     * 反應式處理特性：
     * <ul>
     * <li>使用 {@link Mono#defer} 實現延遲載入，避免不必要的資源消耗</li>
     * <li>透過 {@code handleError} 方法統一處理異常與錯誤響應</li>
     * <li>支援背壓控制，防止大量資料查詢造成記憶體溢位</li>
     * </ul>
     *
     * @param id 資料夾識別碼，必須為正整數，對應資料庫中的資料夾主鍵
     * @param page 分頁頁碼，從 1 開始計算，預設值為 1，用於實現分頁查詢功能
     * @param size 每頁顯示的項目數量，可選參數，若未指定則使用系統預設值
     * @param type 檔案類型過濾清單，可選參數，支援多種檔案格式的組合過濾
     * @param exchange WebFlux 伺服器交換物件，包含完整的 HTTP 請求上下文、用戶認證資訊與會話狀態
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含檔案清單的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與分頁檔案列表，失敗時返回對應的錯誤狀態碼與錯誤資訊
     */
    @HideOverLength
    @GetMapping("/{id}")
    public Mono<ResponseEntity<?>> getFolderFiles(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type, ServerWebExchange exchange) {
        return handleError(Mono.defer(() -> {
                               if (id < 0) {
                                   return Mono.error(new ValidationException(ValidationException.ErrorCode.PATH_NOT_FOUND));
                               }
                               return super.getUserFileList(exchange, id, page, size, getFileEnums(type));
                           }), exchange
        );
    }


    /**
     * 獲取用戶標記為星標的檔案清單，支援分頁查詢與檔案類型過濾。
     * <p>
     * 此端點提供用戶個人化檔案管理功能，返回用戶主動標記為重要的檔案集合。
     * 透過反應式程式設計實現高效能的星標檔案查詢，適用於快速存取常用檔案的場景。
     * <p>
     * HTTP 方法：GET
     * <br>路徑模式：{@code /api/v1/folders/star}
     * <br>Content-Type：application/json
     * <p>
     * 功能特性：
     * <ul>
     * <li>基於用戶身份的個人化檔案篩選</li>
     * <li>支援跨資料夾的星標檔案聚合查詢</li>
     * <li>分頁載入機制，適用於大量星標檔案的效能優化</li>
     * <li>檔案類型過濾，支援特定格式檔案的精確查詢</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code getUserFileList} 方法實現，使用 {@link ReservedSearchIdEnum#STAR_FILE_ID}
     * 作為特殊識別碼，觸發星標檔案的專門查詢邏輯。
     *
     * @param exchange WebFlux 伺服器交換物件，攜帶用戶認證資訊與完整請求上下文
     * @param page 分頁頁碼，從 1 開始計算，預設值為 1，控制查詢結果的分頁顯示
     * @param size 每頁顯示的項目數量，可選參數，未指定時使用系統預設分頁大小
     * @param type 檔案類型過濾清單，可選參數，支援多種檔案格式的複合條件篩選
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含星標檔案清單的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與分頁星標檔案列表，無資料時返回空列表
     */
    @GetMapping("/star")
    public Mono<ResponseEntity<?>> getStarFiles(ServerWebExchange exchange,
                                                @RequestParam(required = false, defaultValue = "1") Integer page,
                                                @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.STAR_FILE_ID.getId(), page, size, getFileEnums(type));
    }


    /**
     * 獲取用戶最近存取的檔案清單，提供快速存取歷史檔案的功能。
     * <p>
     * 此端點基於用戶的檔案存取歷史記錄，返回按時間倒序排列的最近檔案清單。
     * 適用於提升用戶工作效率，快速重新開啟最近使用過的檔案。
     * <p>
     * HTTP 方法：GET
     * <br>路徑模式：{@code /api/v1/folders/recently}
     * <br>Content-Type：application/json
     * <p>
     * 業務邏輯特性：
     * <ul>
     * <li>按最後存取時間倒序排列，最新存取的檔案排在前面</li>
     * <li>僅顯示用戶有權限存取的檔案</li>
     * <li>固定使用第一頁顯示，不支援分頁功能（適用於最近檔案的快速瀏覽場景）</li>
     * <li>支援檔案類型過濾，便於查找特定格式的最近檔案</li>
     * </ul>
     * <p>
     * 實現方式透過繼承的 {@code getUserFileList} 方法，使用
     * {@link ReservedSearchIdEnum#RECENT_FILE_ID} 特殊識別碼觸發最近檔案查詢邏輯。
     *
     * @param exchange WebFlux 伺服器交換物件，包含用戶身份認證與完整的 HTTP 請求上下文
     * @param type 檔案類型過濾清單，可選參數，支援多種檔案格式的篩選條件
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含最近檔案清單的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與最近存取檔案列表，按時間倒序排列
     */
    @GetMapping("/recently")
    public Mono<ResponseEntity<?>> getRecentlyFiles(ServerWebExchange exchange, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.RECENT_FILE_ID.getId(), 1, null, getFileEnums(type));
    }


    /**
     * 獲取回收站中的檔案清單，支援分頁查詢與檔案類型過濾，實現檔案軟刪除管理。
     * <p>
     * 此端點提供已刪除檔案的瀏覽與管理功能，用戶可以查看被標記為刪除的檔案，
     * 並可進行還原或永久刪除操作。透過回收站機制避免意外刪除造成的資料遺失。
     * <p>
     * HTTP 方法：GET
     * <br>路徑模式：{@code /api/v1/folders/recycle}
     * <br>Content-Type：application/json
     * <p>
     * 回收站功能特性：
     * <ul>
     * <li>僅顯示用戶自己刪除的檔案，確保資料隔離</li>
     * <li>保留檔案的完整元資料資訊，包括原始路徑與刪除時間</li>
     * <li>支援分頁載入，適用於大量刪除檔案的高效瀏覽</li>
     * <li>檔案類型過濾功能，便於查找特定格式的已刪除檔案</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code getUserFileList} 方法實現，使用
     * {@link ReservedSearchIdEnum#RECYCLE_FILE_ID} 特殊識別碼觸發回收站檔案查詢。
     *
     * @param exchange WebFlux 伺服器交換物件，包含用戶認證資訊與完整請求上下文
     * @param page 分頁頁碼，從 1 開始計算，預設值為 1，控制回收站檔案的分頁顯示
     * @param size 每頁顯示的項目數量，可選參數，未指定時使用系統預設分頁大小
     * @param type 檔案類型過濾清單，可選參數，支援多種檔案格式的篩選條件
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含回收站檔案清單的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與分頁回收站檔案列表，無資料時返回空列表
     */
    @GetMapping("/recycle")
    public Mono<ResponseEntity<?>> getRecycleFiles(ServerWebExchange exchange,
                                                   @RequestParam(required = false, defaultValue = "1") Integer page,
                                                   @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.RECYCLE_FILE_ID.getId(), page, size, getFileEnums(type));
    }


    /**
     * 獲取用戶所有檔案的完整清單，提供跨資料夾的檔案聚合檢視功能。
     * <p>
     * 此端點實現全域檔案瀏覽功能，返回用戶擁有的所有檔案（不包括回收站檔案），
     * 適用於檔案搜尋、統計分析與批次操作的場景。
     * <p>
     * HTTP 方法：GET
     * <br>路徑模式：{@code /api/v1/folders/all}
     * <br>Content-Type：application/json
     * <p>
     * 全檔案列表特性：
     * <ul>
     * <li>聚合顯示所有資料夾下的檔案，提供統一檢視</li>
     * <li>僅包含用戶有權限存取的檔案</li>
     * <li>排除已刪除至回收站的檔案</li>
     * <li>支援大容量分頁載入，適用於擁有大量檔案的用戶</li>
     * <li>檔案類型過濾，便於特定格式檔案的批次查詢</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code getUserFileList} 方法實現，使用
     * {@link ReservedSearchIdEnum#ALL_FILE_ID} 特殊識別碼觸發全檔案查詢邏輯。
     *
     * @param exchange WebFlux 伺服器交換物件，攜帶用戶身份認證與完整 HTTP 請求上下文
     * @param page 分頁頁碼，從 1 開始計算，預設值為 1，控制查詢結果的分頁顯示
     * @param size 每頁顯示的項目數量，可選參數，未指定時使用系統預設分頁大小
     * @param type 檔案類型過濾清單，可選參數，支援多種檔案格式的複合條件篩選
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含所有檔案清單的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與分頁檔案列表，包含檔案完整元資料資訊
     */
    @GetMapping("/all")
    public Mono<ResponseEntity<?>> getAllFiles(ServerWebExchange exchange,
                                               @RequestParam(required = false, defaultValue = "1") Integer page,
                                               @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.ALL_FILE_ID.getId(), page, size, getFileEnums(type));
    }


    /**
     * 獲取用戶主動分享的檔案清單，提供共享檔案管理功能。
     * <p>
     * 此端點返回用戶設定為共享狀態的檔案集合，用於管理與其他用戶協作的檔案。
     * 支援不同共享策略（公開、私人、受限）的統一管理。
     * <p>
     * HTTP 方法：GET
     * <br>路徑模式：{@code /api/v1/folders/shared}
     * <br>Content-Type：application/json
     * <p>
     * 共享檔案管理特性：
     * <ul>
     * <li>顯示用戶作為共享發起者的所有檔案</li>
     * <li>包含各種共享策略：公開共享、受限共享、密碼保護共享</li>
     * <li>提供共享狀態資訊，包括存取次數與失效時間</li>
     * <li>支援分頁載入，適用於管理大量共享檔案</li>
     * <li>檔案類型過濾，便於特定格式共享檔案的查詢</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code getUserFileList} 方法實現，使用
     * {@link ReservedSearchIdEnum#SHARE_FILE_ID} 特殊識別碼觸發共享檔案查詢。
     *
     * @param exchange WebFlux 伺服器交換物件，包含用戶認證資訊與完整請求上下文
     * @param page 分頁頁碼，從 1 開始計算，預設值為 1，控制共享檔案的分頁顯示
     * @param size 每頁顯示的項目數量，可選參數，未指定時使用系統預設分頁大小
     * @param type 檔案類型過濾清單，可選參數，支援多種檔案格式的篩選條件
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含分享檔案清單的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與分頁共享檔案列表，包含共享狀態詳細資訊
     */
    @GetMapping("/shared")
    public Mono<ResponseEntity<?>> getSharedFiles(ServerWebExchange exchange,
                                                  @RequestParam(required = false, defaultValue = "1") Integer page,
                                                  @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.SHARE_FILE_ID.getId(), page, size, getFileEnums(type));
    }


    /**
     * 永久刪除資料夾及其所有內容，實現不可逆的檔案刪除操作。
     * <p>
     * 此端點執行資料夾的完全移除操作，包括所有子檔案與子資料夾的刪除。
     * 此操作不可逆轉，刪除後的檔案無法還原，需謹慎使用。
     * <p>
     * HTTP 方法：DELETE
     * <br>路徑模式：{@code /api/v1/folders/{id}}
     * <br>Content-Type：application/json
     * <p>
     * 安全性考量：
     * <ul>
     * <li>要求用戶擁有資料夾的完整管理權限</li>
     * <li>驗證用戶對所有子檔案與子資料夾的刪除權限</li>
     * <li>檢查是否存在其他用戶的共享權限，防止意外刪除共享檔案</li>
     * <li>記錄刪除操作的完整日誌，支援審計追蹤</li>
     * </ul>
     * <p>
     * 操作特性：
     * <ul>
     * <li>遞迴刪除所有子內容，包括子資料夾、檔案與元資料</li>
     * <li>清理相關的快取資料與索引資訊</li>
     * <li>更新父資料夾的目錄結構</li>
     * <li>觸發相關的事件通知與日誌記錄</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code deleteFolder} 方法實現，在基礎控制器中實現完整的刪除邏輯。
     *
     * @param id 資料夾識別碼，可為數字或字串格式，對應資料庫中的資料夾主鍵
     * @param exchange WebFlux 伺服器交換物件，包含用戶認證資訊與完整 HTTP 請求上下文
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含刪除操作結果的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與操作確認訊息，失敗時返回相應的錯誤狀態碼
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFolder(@PathVariable String id, ServerWebExchange exchange) {
        return super.deleteFolder(id, exchange);
    }


    /**
     * 編輯資料夾的元資料資訊，支援名稱修改、權限設定與共享配置。
     * <p>
     * 此端點提供資料夾屬性的全面編輯功能，包括基本資訊修改與進階權限管理。
     * 透過驗證機制確保資料完整性與操作安全性。
     * <p>
     * HTTP 方法：PUT
     * <br>路徑模式：{@code /api/v1/folders}
     * <br>Content-Type：application/json
     * <p>
     * 支援的編輯操作：
     * <ul>
     * <li>資料夾名稱修改：更新顯示名稱與識別資訊</li>
     * <li>共享權限設定：配置公開、私人或受限共享模式</li>
     * <li>存取權限修改：設定特定用戶或群組的存取權限</li>
     * <li>遞迴設定：選擇性地將權限變更應用到所有子內容</li>
     * </ul>
     * <p>
     * 安全性與驗證：
     * <ul>
     * <li>使用 {@code @Validated} 註解確保輸入資料的有效性</li>
     * <li>驗證用戶對資料夾的編輯權限</li>
     * <li>檢查資料夾名稱的唯一性與合法性</li>
     * <li>防止路徑遍歷攻擊與惡意輸入</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code editFolder} 方法實現，在基礎控制器中實現完整的編輯邏輯。
     *
     * @param fileEditDTO 檔案編輯資料傳輸物件，包含資料夾識別碼、新名稱、權限設定等資訊，經過 Bean Validation 驗證
     * @param exchange WebFlux 伺服器交換物件，攜帶用戶認證資訊與完整 HTTP 請求上下文
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含編輯操作結果的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與更新後的資料夾資訊，失敗時返回相應錯誤訊息
     */
    @PutMapping
    public Mono<ResponseEntity<?>> editFolder(@RequestBody @Validated FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.editFolder(fileEditDTO, exchange);
    }


    /**
     * 在指定位置創建新的資料夾，支援巢狀目錄結構建立。
     * <p>
     * 此端點實現資料夾的完整創建流程，包括目錄結構驗證、權限設定與元資料初始化。
     * 透過反應式程式設計提供高效的創建操作，適用於高併發場景。
     * <p>
     * HTTP 方法：POST
     * <br>路徑模式：{@code /api/v1/folders}
     * <br>Content-Type：application/json
     * <p>
     * 創建功能特性：
     * <ul>
     * <li>支援在任意父資料夾下創建子資料夾</li>
     * <li>自動繼承父資料夾的預設權限設定</li>
     * <li>支援自定義初始權限配置</li>
     * <li>自動更新父資料夾的目錄結構資訊</li>
     * <li>生成唯一的資料夾識別碼與元資料</li>
     * </ul>
     * <p>
     * 安全性與驗證：
     * <ul>
     * <li>驗證用戶對父資料夾的寫入權限</li>
     * <li>檢查資料夾名稱的合法性與唯一性</li>
     * <li>防止路徑遍歷攻擊與目錄注入</li>
     * <li>實施資料夾深度與數量限制</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code createFolder} 方法實現，在基礎控制器中實現完整的創建邏輯。
     *
     * @param fileEditDTO 檔案編輯資料傳輸物件，包含資料夾名稱、父資料夾識別碼、初始權限設定等必要資訊
     * @param exchange WebFlux 伺服器交換物件，包含用戶認證資訊與完整 HTTP 請求上下文
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含創建操作結果的反應式響應實體，
     *         成功時返回 HTTP 201 狀態碼與新創建資料夾的完整資訊，失敗時返回相應錯誤狀態碼
     */
    @PostMapping
    public Mono<ResponseEntity<?>> createFolder(@RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.createFolder(fileEditDTO, exchange);
    }


    /**
     * 初始化用戶個人化的資料夾樹狀結構，根據系統配置自動建立預設目錄。
     * <p>
     * 此端點為新用戶或需要重置目錄結構的用戶提供自動化的資料夾結構初始化功能。
     * 基於系統配置的樣板，建立標準化的目錄架構，提升用戶體驗。
     * <p>
     * HTTP 方法：POST
     * <br>路徑模式：{@code /api/v1/folders/tree}
     * <br>Content-Type：application/json
     * <p>
     * 樹狀結構初始化特性：
     * <ul>
     * <li>根據系統配置的樣板自動建立預設資料夾</li>
     * <li>支援多層級巢狀目錄結構的一次性建立</li>
     * <li>自動設定適當的權限與存取控制</li>
     * <li>建立系統索引與快取結構</li>
     * <li>初始化相關的元資料與追蹤資訊</li>
     * </ul>
     * <p>
     * 安全性考量：
     * <ul>
     * <li>驗證用戶已經通過身份認證</li>
     * <li>檢查用戶是否已經擁有初始化的目錄結構</li>
     * <li>防止重複初始化造成的資源浪費</li>
     * <li>實施資料夾數量與深度限制</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code buildTree} 方法實現，整合 {@link FolderListTreeManager} 實現快速目錄結構建立。
     *
     * @param exchange WebFlux 伺服器交換物件，包含用戶認證資訊與完整 HTTP 請求上下文
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含樹狀結構建立結果的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與建立的目錄結構資訊，失敗時返回相應錯誤訊息
     */
    @PostMapping("/tree")
    public Mono<ResponseEntity<?>> buildTree(ServerWebExchange exchange) {
        return super.buildTree(exchange);
    }


    /**
     * 將資料夾移動至回收站，實現安全的軟刪除機制。
     * <p>
     * 此端點提供可逆的資料夾刪除功能，資料夾被標記為已刪除但保留在系統中，
     * 用戶可以在需要時還原資料，有效防止意外刪除造成的資料遺失。
     * <p>
     * HTTP 方法：POST
     * <br>路徑模式：{@code /api/v1/folders/remove/{id}}
     * <br>Content-Type：application/json
     * <p>
     * 軟刪除功能特性：
     * <ul>
     * <li>保留資料夾及其所有內容的完整結構</li>
     * <li>記錄刪除時間與原始位置資訊，便於後續還原</li>
     * <li>隱藏在一般檔案瀏覽中，僅在回收站中顯示</li>
     * <li>支援遞迴軟刪除，子資料夾與檔案一併移動</li>
     * <li>更新父資料夾的目錄結構資訊</li>
     * </ul>
     * <p>
     * 安全性與權限：
     * <ul>
     * <li>驗證用戶對資料夾的刪除權限</li>
     * <li>檢查資料夾是否已經在回收站中</li>
     * <li>處理共享資料夾的特殊情況</li>
     * <li>記錄操作日誌以供審計追蹤</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code removeFile} 方法實現，在基礎控制器中實現軟刪除邏輯。
     *
     * @param exchange WebFlux 伺服器交換物件，包含用戶認證資訊與完整 HTTP 請求上下文
     * @param id 資料夾識別碼，可為數字或字串格式，指定要移動至回收站的資料夾
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含軟刪除操作結果的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與操作確認訊息，失敗時返回相應錯誤訊息
     */
    @PostMapping("/remove/{id}")
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.removeFile(exchange, id);
    }


    /**
     * 從回收站還原已刪除的資料夾，恢復原始目錄結構。
     * <p>
     * 此端點實現資料夾的完整還原功能，將在回收站中的資料夾恢復到原始位置，
     * 並重建完整的目錄結構與權限設定。適用於意外刪除後的資料救回場景。
     * <p>
     * HTTP 方法：POST
     * <br>路徑模式：{@code /api/v1/folders/restore/{id}}
     * <br>Content-Type：application/json
     * <p>
     * 還原功能特性：
     * <ul>
     * <li>恢復資料夾至原始位置，維持原有的目錄結構</li>
     * <li>遞迴還原所有子資料夾與檔案</li>
     * <li>恢復原有的權限設定與共享狀態</li>
     * <li>重建索引與快取結構</li>
     * <li>更新父資料夾的目錄結構資訊</li>
     * </ul>
     * <p>
     * 安全性與驗證：
     * <ul>
     * <li>驗證用戶對資料夾的還原權限</li>
     * <li>檢查資料夾是否確實在回收站中</li>
     * <li>驗證原始位置的有效性與可存取性</li>
     * <li>處理名稱衝突與權限衝突</li>
     * <li>記錄還原操作的完整日誌</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code restoreFile} 方法實現，在基礎控制器中實現完整的還原邏輯。
     *
     * @param exchange WebFlux 伺服器交換物件，包含用戶認證資訊與完整 HTTP 請求上下文
     * @param id 資料夾識別碼，指定要從回收站還原的資料夾
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含還原操作結果的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與還原確認訊息，失敗時返回相應錯誤訊息
     */
    @PostMapping("/restore/{id}")
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.restoreFile(exchange, id);
    }


    /**
     * 下載資料夾完整內容，以壓縮檔案格式提供流式下載。
     * <p>
     * 此端點實現資料夾的完整打包下載功能，將指定資料夾及其所有子內容遞迴打包為壓縮檔案。
     * 透過流式處理機制支援大型資料夾的高效下載，無需完整載入到記憶體。
     * <p>
     * HTTP 方法：GET
     * <br>路徑模式：{@code /api/v1/folders/{id}/download}
     * <br>Content-Type：application/octet-stream
     * <p>
     * 下載功能特性：
     * <ul>
     * <li>遞迴打包所有子資料夾與檔案，保持目錄結構</li>
     * <li>實時壓縮與流式傳輸，節省伺服器資源</li>
     * <li>支援斷點續傳與部分下載</li>
     * <li>自動設定適當的檔案名稱與 MIME 類型</li>
     * <li>支援背壓控制，防止記憶體溢位</li>
     * </ul>
     * <p>
     * 安全性考量：
     * <ul>
     * <li>驗證用戶對資料夾的下載權限</li>
     * <li>檢查所有子檔案的讀取權限</li>
     * <li>記錄下載操作的完整日誌</li>
     * <li>實施下載速率與併發數限制</li>
     * </ul>
     * <p>
     * 反應式特性：
     * <ul>
     * <li>返回 {@link Flux}<{@link org.springframework.core.io.buffer.DataBuffer}> 流式資料</li>
     * <li>支援非阻塞 I/O，提升伵發性能</li>
     * <li>自動處理流式錯誤與資源釋放</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code downloadFolder} 方法實現，整合檔案服務策略實現多元化下載支援。
     *
     * @param id 資料夾識別碼，指定要下載的資料夾主鍵
     * @param exchange WebFlux 伺服器交換物件，包含用戶認證資訊與完整 HTTP 請求上下文
     * @return {@link Mono}<{@link ResponseEntity}<{@link Flux}<{@link org.springframework.core.io.buffer.DataBuffer}>>> 包含壓縮檔案流式資料的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與流式壓縮檔案資料，失敗時返回相應錯誤訊息
     */
    @GetMapping("/{id}/download")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFolder(@PathVariable Long id, ServerWebExchange exchange) {
        return super.downloadFolder(id, exchange);
    }


    /**
     * 獲取指定資料夾的完整路徑資訊，提供階層式導航支援。
     * <p>
     * 此端點返回從根目錄到指定資料夾的完整路徑鏈，包含每個層級的資料夾資訊。
     * 適用於麵包屑導航、路徑顯示與快速導航功能的實現。
     * <p>
     * HTTP 方法：GET
     * <br>路徑模式：{@code /api/v1/folders/path/{id}}
     * <br>Content-Type：application/json
     * <p>
     * 路徑查詢特性：
     * <ul>
     * <li>返回完整的階層式路徑結構，從根目錄到目標資料夾</li>
     * <li>包含路徑中每個資料夾的基本資訊（識別碼、名稱、權限等）</li>
     * <li>支援跨多層級目錄的快速路徑解析</li>
     * <li>提供路徑深度與結構驗證</li>
     * <li>快取優化，提升重複查詢的效能</li>
     * </ul>
     * <p>
     * 安全性考量：
     * <ul>
     * <li>驗證用戶對目標資料夾的讀取權限</li>
     * <li>檢查路徑中每個資料夾的存取權限</li>
     * <li>隱藏用戶無權限存取的路徑段</li>
     * <li>防止路徑遍歷攻擊與未授權存取</li>
     * </ul>
     * <p>
     * 響應格式包含：
     * <ul>
     * <li>完整路徑陣列，按層級順序排列</li>
     * <li>每個路徑段的資料夾基本資訊</li>
     * <li>目標資料夾的詳細元資料</li>
     * <li>路徑深度與階層關係資訊</li>
     * </ul>
     * <p>
     * 透過繼承的 {@code getFolderPath} 方法實現，在基礎控制器中實現路徑解析邏輯。
     *
     * @param id 資料夾識別碼，指定要查詢路徑的目標資料夾主鍵
     * @param exchange WebFlux 伺服器交換物件，包含用戶認證資訊與完整 HTTP 請求上下文
     * @return {@link Mono}<{@link ResponseEntity}<?>> 包含資料夾路徑資訊的反應式響應實體，
     *         成功時返回 HTTP 200 狀態碼與完整路徑結構資料，失敗時返回相應錯誤狀態碼
     */
    @GetMapping("/path/{id}")
    public Mono<ResponseEntity<?>> getFolderPath(@PathVariable Long id, ServerWebExchange exchange) {
        return super.getFolderPath(exchange, id);
    }
}
