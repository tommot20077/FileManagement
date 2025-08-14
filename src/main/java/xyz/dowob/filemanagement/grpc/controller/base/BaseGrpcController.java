package xyz.dowob.filemanagement.grpc.controller.base;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.data.user.dto.UserInfoDto;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.JwtAuthenticationException;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.grpc.AuthRequest;
import xyz.dowob.filemanagement.holder.AuthenticationContext;
import xyz.dowob.filemanagement.holder.GrpcAuthenticationContextHolder;
import xyz.dowob.filemanagement.service.grpc.JwtCacheService;
import xyz.dowob.filemanagement.service.grpc.UserContextCacheService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.function.Function;

/**
 * gRPC 基礎控制器抽象類。
 *
 * <p>提供 gRPC 服務的基礎功能，包括：
 * <ul>
 *   <li>統一的錯誤處理機制</li>
 *   <li>用戶認證和權限驗證</li>
 *   <li>通用的驗證邏輯</li>
 *   <li>反應式編程支援</li>
 * </ul>
 *
 * <p>此類實現了 ResponseUnity 介面，提供與 HTTP 控制器一致的響應處理機制，
 * 確保系統的一致性和可維護性。
 *
 * <p>安全性特點：
 * <ul>
 *   <li>所有操作都需要 JWT 令牌驗證</li>
 *   <li>提供統一的權限檢查機制</li>
 *   <li>敏感資訊處理和日誌記錄</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public abstract class BaseGrpcController implements ResponseUnity {

    /**
     * 驗證服務介面。
     * 提供各種數據驗證功能，確保輸入數據的合法性和安全性。
     */
    protected final ValidationService validationService;

    /**
     * 用戶服務介面。
     * 處理用戶相關的業務邏輯，包括認證、資訊查詢等。
     */
    protected final UserService userService;

    /**
     * 令牌服務介面。
     * 負責 JWT 令牌的生成、驗證和解析。
     */
    protected final TokenService tokenService;

    /**
     * 檔案權限服務介面。
     * 處理檔案和資料夾的權限驗證。
     */
    protected final PermissionService<UserFileMetadata> permissionService;

    /**
     * JWT 快取服務。
     * 提供高效能的 JWT 令牌解析和使用者資訊快取。
     */
    protected final JwtCacheService jwtCacheService;

    /**
     * 用戶上下文快取服務。
     * 提供高效能的用戶實體快取機制。
     */
    protected final UserContextCacheService userContextCacheService;


    /**
     * 將位元組陣列轉換為十六進位字串。
     *
     * <p>主要用於 MD5 校驗碼的轉換。
     *
     * @param bytes 要轉換的位元組陣列
     * @return 十六進位表示的字串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }


    /**
     * 驗證用戶令牌並獲取用戶資訊。
     *
     * <p>此方法執行以下步驟：
     * <ol>
     *   <li>驗證 JWT 令牌的有效性</li>
     *   <li>檢查令牌是否與用戶 ID 匹配</li>
     *   <li>獲取完整的用戶資訊</li>
     * </ol>
     *
     * @param token  JWT 令牌
     * @param userId 用戶 ID
     * @return 包含用戶資訊的 Mono
     */
    protected Mono<User> validateAndGetUser(String token, Long userId) {
        return tokenService
            .validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN)
            .flatMap(userService::getById);
    }


    /**
     * 使用統一的認證請求驗證用戶並獲取用戶資訊。
     *
     * <p>此方法為新的統一認證架構，執行以下步驟：
     * <ol>
     *   <li>從 AuthRequest 中提取 JWT 令牌</li>
     *   <li>驗證 JWT 令牌的有效性</li>
     *   <li>如果提供了用戶 ID，檢查令牌是否與用戶 ID 匹配</li>
     *   <li>獲取完整的用戶資訊</li>
     * </ol>
     *
     * @param authRequest 統一認證請求，包含 JWT 令牌和可選的用戶 ID
     * @return 包含用戶資訊的 Mono
     */
    protected Mono<User> validateAndGetUser(AuthRequest authRequest) {
        String token = authRequest.getJwtToken();
        Long userId = authRequest.getUserId() != 0 ? authRequest.getUserId() : null;

        return tokenService
            .validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN)
            .flatMap(userService::getById);
    }


    /**
     * 使用快取的高效能認證和用戶資訊獲取方法。
     *
     * <p>此方法為快取最佳化版本，執行以下步驟：
     * <ol>
     *   <li>使用 JWT 快取服務解析令牌並獲取使用者資訊</li>
     *   <li>如果提供了用戶 ID，驗證令牌歸屬</li>
     *   <li>使用用戶上下文快取服務獲取完整的用戶實體</li>
     *   <li>返回完整的用戶資訊</li>
     * </ol>
     *
     * <p><strong>效能優勢：</strong>
     * <ul>
     *   <li>JWT 解析快取命中：避免重複解析，延遲 < 1ms</li>
     *   <li>用戶實體快取命中：避免資料庫查詢，延遲 < 1ms</li>
     *   <li>雙層快取策略：最大化快取效益</li>
     *   <li>透明操作：對呼叫者完全透明</li>
     * </ul>
     *
     * @param authRequest 統一認證請求，包含 JWT 令牌和可選的用戶 ID
     * @return 包含用戶資訊的 Mono
     */
    protected Mono<User> validateAndGetUserWithCache(AuthRequest authRequest) {
        String token = authRequest.getJwtToken();
        Long expectedUserId = authRequest.getUserId() != 0 ? authRequest.getUserId() : null;

        return jwtCacheService.getUserInfo(token)
            .flatMap(userInfo -> {
                if (expectedUserId != null && !userInfo.getUserId().equals(expectedUserId)) {
                    return Mono.error(new JwtAuthenticationException("JWT 令牌與用戶 ID 不匹配"));
                }

                return userContextCacheService.getUser(userInfo.getUserId());
            });
    }
    

    /**
     * 執行需要認證的 gRPC 操作，並自動注入認證上下文。
     *
     * <p>此方法為 gRPC 和 WebFlux 架構之間的橋樑，負責：
     * <ul>
     *   <li><strong>上下文橋接：</strong>從 ThreadLocal（gRPC 層）轉移到 Reactor Context（業務層）</li>
     *   <li><strong>認證檢查：</strong>確保當前執行緒已通過認證</li>
     *   <li><strong>上下文注入：</strong>將認證資訊注入到響應式流中</li>
     *   <li><strong>錯誤處理：</strong>統一處理認證和業務錯誤</li>
     * </ul>
     *
     * <p><strong>架構流程：</strong>
     * <pre>
     * gRPC 攔截器設置 ThreadLocal → executeWithAuth 讀取並橋接 → Reactor Context → 業務邏輯
     * </pre>
     *
     * <p><strong>使用範例：</strong>
     * <pre>{@code
     * public void getFile(GetFileRequest request, StreamObserver<FileResponse> responseObserver) {
     *     Mono<FileResponse> response = executeWithAuth(request.getAuth(), user ->
     *         fileService.getFile(request.getFileId(), user.getId())
     *             .map(file -> buildFileResponse(file))
     *     );
     *
     *     subscribeWithGrpcHandler(response, responseObserver);
     * }
     * }</pre>
     *
     * <p><strong>安全性保證：</strong>
     * <ul>
     *   <li>只有通過 gRPC 攔截器認證的請求才能正常執行</li>
     *   <li>未認證的請求會被自動拒絕</li>
     *   <li>上下文資訊隨響應式流自動清理</li>
     * </ul>
     *
     * @param <T> 業務邏輯的回傳類型
     * @param authRequest 認證請求（由攔截器驗證，此處僅做一致性檢查）
     * @param businessLogic 需要執行的業務邏輯，接收認證用戶作為參數
     * @return 包含業務邏輯結果的 Mono，已注入認證上下文
     * @throws JwtAuthenticationException 當 ThreadLocal 中沒有認證資訊時
     */
    protected <T> Mono<T> executeWithAuth(AuthRequest authRequest, Function<User, Mono<T>> businessLogic) {
        User user = AuthenticationContext.getCurrentUser();
        UserInfoDto userInfo = AuthenticationContext.getUserInfo();

        if (user == null || userInfo == null) {
            return Mono.error(new JwtAuthenticationException("未找到認證上下文，請確保請求已通過認證攔截器"));
        }

        if (authRequest.getUserId() != 0 && !userInfo.getUserId().equals(authRequest.getUserId())) {
            return Mono.error(new JwtAuthenticationException("認證上下文與請求用戶 ID 不匹配"));
        }

        return businessLogic.apply(user)
            .contextWrite(GrpcAuthenticationContextHolder.withAuth(user, userInfo))
            .doOnSuccess(result -> LogUnity.debug("gRPC 操作成功完成，用戶: {}", user.getUsername()))
            .doOnError(error -> LogUnity.debug("gRPC 操作失敗，用戶: {}, 錯誤: {}",
                user.getUsername(), error.getMessage()));
    }
    

    /**
     * 簡化版本的認證執行方法，不需要顯式傳入 AuthRequest。
     *
     * <p>適用於不需要進行額外用戶 ID 驗證的場景，直接使用 ThreadLocal 中的認證資訊。
     *
     * @param <T> 業務邏輯的回傳類型
     * @param businessLogic 需要執行的業務邏輯
     * @return 包含業務邏輯結果的 Mono，已注入認證上下文
     */
    protected <T> Mono<T> executeWithAuth(Function<User, Mono<T>> businessLogic) {
        // 從 ThreadLocal 獲取認證資訊
        User user = AuthenticationContext.getCurrentUser();
        UserInfoDto userInfo = AuthenticationContext.getUserInfo();

        // 檢查認證狀態
        if (user == null || userInfo == null) {
            return Mono.error(new JwtAuthenticationException("未找到認證上下文"));
        }

        // 執行業務邏輯並注入 Reactor Context
        return businessLogic.apply(user)
            .contextWrite(GrpcAuthenticationContextHolder.withAuth(user, userInfo));
    }
    

    /**
     * 檢查當前 gRPC 請求是否已通過認證。
     *
     * <p>檢查 ThreadLocal 中是否存在完整的認證資訊。
     * 用於防護性編程和除錯目的。
     *
     * @return true 表示已認證，false 表示未認證
     */
    protected boolean isAuthenticated() {
        return AuthenticationContext.isAuthenticated();
    }


    /**
     * 獲取當前認證用戶的基本資訊摘要。
     *
     * <p>用於日誌記錄和除錯目的，返回安全的用戶資訊摘要。
     *
     * @return 用戶資訊摘要字串，未認證時返回 "未認證"
     */
    protected String getCurrentUserSummary() {
        if (!AuthenticationContext.isAuthenticated()) {
            return "未認證";
        }

        String username = AuthenticationContext.getCurrentUsername();
        String role = AuthenticationContext.getCurrentUserRole();
        Long userId = AuthenticationContext.getCurrentUserId();

        return String.format("用戶[ID=%d, 名稱=%s, 角色=%s]", userId, username, role);
    }


    /**
     * 統一處理 gRPC 操作中的錯誤。
     *
     * <p>將各種異常轉換為適當的 gRPC Status：
     * <ul>
     *   <li>ValidationException → INVALID_ARGUMENT</li>
     *   <li>LimitationException → RESOURCE_EXHAUSTED</li>
     *   <li>JwtAuthenticationException → UNAUTHENTICATED</li>
     *   <li>ProcessException → INTERNAL</li>
     *   <li>其他異常 → INTERNAL</li>
     * </ul>
     *
     * @param error            要處理的異常
     * @param responseObserver gRPC 響應觀察者
     */
    @Override
    public void handleGrpcError(Throwable error, StreamObserver<?> responseObserver) {
        Status status;
        String errorMessage;

        switch (error) {
            case ValidationException ve -> {
                errorMessage = String.format("驗證失敗: %s", ve.getMessage());
                status = Status.INVALID_ARGUMENT.withDescription(errorMessage);
                LogUnity.warn("gRPC 驗證錯誤: %s", ve.getMessage());
            }
            case LimitationException le -> {
                errorMessage = String.format("資源限制: %s", le.getMessage());
                status = Status.RESOURCE_EXHAUSTED.withDescription(errorMessage);
                LogUnity.warn("gRPC 限制錯誤: %s", le.getMessage());
            }
            case JwtAuthenticationException ae -> {
                errorMessage = "認證失敗";
                status = Status.UNAUTHENTICATED.withDescription(errorMessage);
                LogUnity.warn("gRPC 認證錯誤: %s", ae.getMessage());
            }
            case ProcessException pe -> {
                errorMessage = String.format("處理錯誤: %s", pe.getMessage());
                status = Status.INTERNAL.withDescription(errorMessage);
                LogUnity.error("gRPC 處理錯誤: %s", pe, pe.getMessage());
            }
            case null, default -> {
                errorMessage = "內部錯誤";
                status = Status.INTERNAL.withDescription(errorMessage);
                if (error != null) {
                    LogUnity.error("gRPC 未知錯誤: %s", error, error.getMessage());
                } else {
                    LogUnity.error("gRPC 未知錯誤");
                }
            }
        }

        responseObserver.onError(status.asRuntimeException());
    }
}