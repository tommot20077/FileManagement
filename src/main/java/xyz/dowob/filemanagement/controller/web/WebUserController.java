package xyz.dowob.filemanagement.controller.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.controller.base.BaseUserController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.UserInfoTypeEnum;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.Set;

/**
 * 基於 WebFlux 的用戶 Web 控制器，處理用戶相關的 RESTful API 操作。
 * <p>
 * 提供用戶管理功能，包括用戶登出、獲取用戶信息、搜尋用戶等操作。
 * 支援多種用戶信息搜尋類型，如姓名、郵箱等。
 * <p>
 * 此控制器繼承自 {@link BaseUserController}，採用反應式編程模式處理用戶相關操作，
 * 確保在高併發場景下的性能表現。所有操作均遵循安全設定與權限控制。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/web/v1/user")
public class WebUserController extends BaseUserController {

    /**
     * 構造函數，初始化用戶控制器。
     *
     * @param fileServiceStrategy 檔案服務策略
     * @param userService         用戶服務
     * @param securityProperties  安全屬性設定
     * @param validationService   驗證服務
     */
    public WebUserController(FileServiceStrategy fileServiceStrategy, UserService userService, SecurityProperties securityProperties, ValidationService validationService) {
        super(fileServiceStrategy, userService, securityProperties, validationService);
    }


    /**
     * 用戶登出的Web請求，清除用戶認證狀態。
     * <p>
     * 執行用戶登出操作，清除用戶的認證狀態和相關的緩存資料。
     * 登出後用戶需要重新登入才能存取受保護的資源。
     * 系統會自動失效用戶的JWT令牌和清除相關的安全上下文。
     * 此方法專用於Web介面，參數isWeb固定為 true。
     *
     * @param exchange 伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     * @param isWeb    標記是否為Web介面登出，本方法固定烺true（參數未在HTTP請求中傳遞）
     *
     * @return 包含登出操作結果的響應實體 Mono，成功時返回確認訊息，失敗時返回錯誤詳情
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<?>> logout(ServerWebExchange exchange, boolean isWeb) {
        return super.logout(exchange, true);
    }


    /**
     * 獲取當前已登入用戶的個人資訊。
     * <p>
     * 查詢當前已認證用戶的詳細資訊，包括用戶名稱、電子郵件、
     * 角色資訊、帳號設定、儲存使用狀況等個人相關資料。
     * 系統會自動從JWT令牌中解析用戶身份，不需额外提供用戶識別資訊。
     *
     * @param exchange 伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     *
     * @return 包含當前用戶詳細資訊的響應實體 Mono，成功時返回用戶資料對象，失敗時返回錯誤訊息
     */
    @GetMapping("/info")
    public Mono<ResponseEntity<?>> getUserInfo(ServerWebExchange exchange) {
        return super.getUserInfo(exchange);
    }


    /**
     * 搜索用戶資訊，支援多種查詢維度。
     * <p>
     * 根據指定的搜索類型和關鍵字，在系統中搜索符合條件的用戶。
     * 可以根據用戶名、電子郵件等不同維度進行精確或模糊查詢。
     * 可以一次搜索多個用戶，適用於用戶選擇、群組管理、檔案分享等場景。
     * 搜索結果不包含敏感資訊，僅返回公開的用戶識別資訊。
     *
     * @param exchange  伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     * @param userInfos 查詢的用戶標識集合，可以是用戶名、電子郵件或用戶ID等，支持批量查詢
     * @param type      查詢類型，預設值為"name"，支持"name"、"email"、"id"等多種維度搜索
     *
     * @return 包含查詢結果的響應實體 Mono，以用戶資料列表形式返回符合條件的用戶
     */
    @GetMapping("/info/search")
    public Mono<ResponseEntity<?>> searchUserInfo(ServerWebExchange exchange,
                                                  @RequestParam Set<String> userInfos,
                                                  @RequestParam(required = false, defaultValue = "name") String type) {
        String formatType = UserInfoTypeEnum.fromString(type).name();
        return super.searchUserInfo(exchange, userInfos, formatType);
    }
}
