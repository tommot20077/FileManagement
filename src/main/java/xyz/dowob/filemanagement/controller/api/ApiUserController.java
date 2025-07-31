package xyz.dowob.filemanagement.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.controller.base.BaseUserController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.PermissionEnum;
import xyz.dowob.filemanagement.customenum.UserInfoTypeEnum;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.Set;

/**
 * 基於 WebFlux 反應式編程的用戶 RESTful API 控制器實現。
 * <p>
 * 提供非阻塞的用戶相關操作，包括用戶登出、個人資訊查詢、
 * 管理員獲取所有用戶資訊和用戶搜尋功能。支持多種搜尋類型，
 * 包括依用戶名、電子郵件和 ID 進行搜尋。
 * <p>
 * 繼承自 {@link BaseUserController}，採用策略模式實現多種服務操作。
 * 所有端點要求用戶認證，並透過註解實現細粒度權限控制。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/api/v1/user")
public class ApiUserController extends BaseUserController {
    /**
     * 初始化用戶 API 控制器，透過依賴注入設定所需服務組件。
     *
     * @param fileServiceStrategy 檔案服務策略，提供多種檔案操作實現
     * @param userService 用戶服務，處理用戶身份認證與用戶資料管理
     * @param securityProperties 安全設定屬性，定義安全策略和參數
     * @param validationService 驗證服務，執行請求參數的格式檢查
     */
    public ApiUserController(FileServiceStrategy fileServiceStrategy, UserService userService, SecurityProperties securityProperties, ValidationService validationService) {
        super(fileServiceStrategy, userService, securityProperties, validationService);
    }


    /**
     * 處理用戶登出請求。
     * <p>
     * 執行用戶登出操作，清除用戶的認證狀態和相關的緩存資料。
     * 登出後用戶需要重新登入才能存取受保護的資源。
     * 系統會自動失效用戶的 JWT 令牌和清除相關的安全上下文。
     *
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和用戶認證資訊
     * @param isWeb 標記是否為 Web 介面登出，本 API 端點固定為 false
     * @return 包含登出操作結果的反應式響應實體，成功時返回確認訊息
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<?>> logout(ServerWebExchange exchange, boolean isWeb) {
        return super.logout(exchange, false);
    }


    /**
     * 獲取所有用戶資訊，僅限管理員訪問。
     * <p>
     * 提供系統中所有用戶的基本資訊查詢功能，包括用戶名稱、電子郵件、
     * 角色訊息、帳戶狀態等。此端點只有具備管理權限的用戶才能訪問，
     * 用於系統管理和用戶監控目的。所有敏感資訊均已被過濾。
     *
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和管理員認證資訊
     * @return 包含所有用戶資訊清單的反應式響應實體，以陣列形式返回用戶資料
     */
    @GetMapping("/info/all")
    @RecordLevel(LogLevelEnum.WARN)
    @RequirePermission(PermissionEnum.MANAGE)
    public Mono<ResponseEntity<?>> getAllUserInfo(ServerWebExchange exchange) {
        return super.getAllUserInfo(exchange);
    }


    /**
     * 獲取當前已登入用戶的個人資訊。
     * <p>
     * 查詢當前已認證用戶的詳細資訊，包括用戶名稱、電子郵件、
     * 角色訊息、帳戶設定、儲存使用情況等個人相關資料。
     * 系統會自動從請求中的 JWT 令牌解析用戶身份。
     *
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和用戶認證資訊
     * @return 包含當前用戶詳細資訊的反應式響應實體，以用戶資料物件形式返回
     */
    @GetMapping("/info")
    public Mono<ResponseEntity<?>> getUserInfo(ServerWebExchange exchange) {
        return super.getUserInfo(exchange);
    }


    /**
     * 搜尋用戶資訊，支持多種搜尋類型。
     * <p>
     * 提供多維度的用戶搜尋功能，支持按用戶名、電子郵件、用戶ID等
     * 不同維度進行精確或模糊查詢。可以一次搜尋多個用戶，
     * 適用於用戶選擇、群組管理、檔案分享等場景。
     * 搜尋結果不包含敏感資訊，僅返回公開的用戶識別資訊。
     *
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和用戶認證資訊
     * @param userInfos 查詢的用戶標識集合，可以是用戶名、電子郵件或用戶ID等，支持批量查詢
     * @param type 查詢類型，預設值為 "name"，支持 "name"、"email"、"id" 等多種維度搜尋
     * @return 包含查詢結果的反應式響應實體，以用戶資料清單形式返回符合條件的用戶
     */
    @GetMapping("/info/search")
    public Mono<ResponseEntity<?>> searchUserInfo(ServerWebExchange exchange,
                                                  @RequestParam Set<String> userInfos,
                                                  @RequestParam(required = false, defaultValue = "name") String type) {
        String formatType = UserInfoTypeEnum.fromString(type).name();
        return super.searchUserInfo(exchange, userInfos, formatType);
    }
}
