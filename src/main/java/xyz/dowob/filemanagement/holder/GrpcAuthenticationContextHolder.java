package xyz.dowob.filemanagement.holder;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import xyz.dowob.filemanagement.data.user.dto.UserInfoDto;
import xyz.dowob.filemanagement.entity.User;

import java.util.function.Function;

/**
 * gRPC 認證上下文持有者，用於在 Reactor Context 中傳遞認證資訊。
 * 
 * <p>此類專門為 gRPC 請求設計，解決 gRPC 攔截器（同步）與 WebFlux 業務邏輯（反應式）
 * 之間的上下文傳遞問題。提供統一的介面在 Reactor 響應式流中存取和傳遞認證資訊。
 * 
 * <p><strong>設計原則：</strong>
 * <ul>
 *   <li><strong>響應式相容：</strong>完全基於 Reactor Context，支援執行緒切換</li>
 *   <li><strong>型別安全：</strong>使用強型別的 Context key，避免鍵值衝突</li>
 *   <li><strong>記憶體安全：</strong>Context 隨響應式流自動清理，無需手動管理</li>
 *   <li><strong>效能優化：</strong>延遲獲取機制，只在需要時才存取 Context</li>
 * </ul>
 * 
 * <p><strong>使用場景：</strong>
 * <ul>
 *   <li>gRPC 業務邏輯中獲取當前認證用戶</li>
 *   <li>跨服務層傳遞認證上下文</li>
 *   <li>在響應式流中保持認證狀態</li>
 * </ul>
 * 
 * <p><strong>架構流程：</strong>
 * <pre>
 * gRPC 攔截器 → ThreadLocal → BaseGrpcController → Reactor Context → 業務邏輯
 *                  ↓              ↓                  ↓                ↓
 *               臨時存儲         橋接轉換           上下文注入        響應式存取
 * </pre>
 * 
 * <p><strong>使用範例：</strong>
 * <pre>{@code
 * // 在 BaseGrpcController 中注入認證上下文
 * public Mono<Response> handleRequest() {
 *     User user = AuthenticationContext.getCurrentUser();
 *     UserInfoDto userInfo = AuthenticationContext.getUserInfo();
 *     
 *     return businessLogic()
 *         .contextWrite(GrpcAuthenticationContextHolder.withAuth(user, userInfo));
 * }
 * 
 * // 在業務服務中獲取認證資訊
 * public Mono<Result> doSomething() {
 *     return GrpcAuthenticationContextHolder.getCurrentUser()
 *         .flatMap(user -> processForUser(user));
 * }
 * }</pre>
 * 
 * <p><strong>注意事項：</strong>
 * <ul>
 *   <li>必須在 Reactor Context 中使用，否則會返回空的 Mono</li>
 *   <li>Context 設定應在響應式鏈的起點進行</li>
 *   <li>不要在同步程式碼中直接使用，應透過橋接機制</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see Context
 * @see Mono#contextWrite(Function)
 * @see xyz.dowob.filemanagement.holder.CustomRequestContextHolder
 */
public class GrpcAuthenticationContextHolder {
    
    /**
     * Reactor Context 中用於存取認證用戶的鍵值常數。
     * 
     * <p>此常數定義了在 Reactor Context 中用於標識當前認證用戶的唯一鍵值。
     * 使用字串常數而非 Class 作為 key，確保更好的序列化支援和除錯體驗。
     */
    public static final String USER_KEY = "GRPC_AUTH_USER";
    
    /**
     * Reactor Context 中用於存取用戶資訊 DTO 的鍵值常數。
     * 
     * <p>此常數用於存取從 JWT 解析出的用戶基本資訊，包含用戶 ID、使用者名稱、
     * 角色等認證相關的輕量級資訊。
     */
    public static final String USER_INFO_KEY = "GRPC_AUTH_USER_INFO";
    
    /**
     * 從 Reactor Context 中獲取當前的認證用戶。
     * 
     * <p>使用延遲上下文檢索機制，在響應式流中安全地存取認證用戶實例。
     * 如果當前上下文中不存在認證用戶，則回傳空的 Mono。
     * 
     * <p><strong>使用時機：</strong>
     * <ul>
     *   <li>業務邏輯需要當前用戶的完整資訊時</li>
     *   <li>權限檢查需要用戶實體時</li>
     *   <li>審計日誌需要記錄用戶操作時</li>
     * </ul>
     * 
     * <p><strong>錯誤處理：</strong>
     * 如果 Context 中沒有用戶資訊，返回 {@code Mono.empty()}，
     * 呼叫者應適當處理空值情況。
     * 
     * @return 包含 {@link User} 的 Mono，如果上下文中不存在則為空
     * @see Mono#deferContextual(Function)
     * @see Context#getOrEmpty(Object)
     */
    public static Mono<User> getCurrentUser() {
        return Mono.deferContextual(contextView -> 
            Mono.justOrEmpty(contextView.getOrEmpty(USER_KEY))
                .cast(User.class)
        );
    }
    

    /**
     * 建立用於將認證資訊注入 Reactor Context 的轉換函數。
     *
     * <p>回傳一個函數，可用於修改當前的 Reactor Context，將認證用戶和
     * 用戶資訊同時注入其中。此轉換函數通常與 {@code contextWrite()} 操作結合使用。
     *
     * <p><strong>注入策略：</strong>
     * <ul>
     *   <li>同時注入 User 實體和 UserInfoDto</li>
     *   <li>保持資料的一致性和完整性</li>
     *   <li>支援鏈式操作和組合</li>
     * </ul>
     *
     * <p><strong>使用範例：</strong>
     * <pre>{@code
     * return businessService.processRequest(request)
     *     .contextWrite(GrpcAuthenticationContextHolder.withAuth(user, userInfo));
     * }</pre>
     *
     * @param user 要注入的認證用戶實體，不可為 null
     * @param userInfo 要注入的用戶資訊 DTO，不可為 null
     * @return 用於修改 Reactor Context 的轉換函數
     * @throws IllegalArgumentException 如果 user 或 userInfo 為 null
     * @see Context#put(Object, Object)
     * @see Mono#contextWrite(Function)
     */
    public static Function<Context, Context> withAuth(User user, UserInfoDto userInfo) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (userInfo == null) {
            throw new IllegalArgumentException("UserInfo cannot be null");
        }

        return context -> context
            .put(USER_KEY, user)
            .put(USER_INFO_KEY, userInfo);
    }
    

    /**
     * 建立僅注入用戶實體的 Context 轉換函數。
     *
     * <p>適用於只需要用戶實體而不需要 JWT 資訊的場景。
     * 通常在內部服務調用或測試環境中使用。
     *
     * @param user 要注入的認證用戶實體，不可為 null
     * @return 用於修改 Reactor Context 的轉換函數
     * @throws IllegalArgumentException 如果 user 為 null
     */
    public static Function<Context, Context> withUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return context -> context.put(USER_KEY, user);
    }
    

    /**
     * 建立僅注入用戶資訊的 Context 轉換函數。
     *
     * <p>適用於只需要基本用戶資訊的輕量級操作。
     * 可以避免載入完整的用戶實體，提升效能。
     *
     * @param userInfo 要注入的用戶資訊 DTO，不可為 null
     * @return 用於修改 Reactor Context 的轉換函數
     * @throws IllegalArgumentException 如果 userInfo 為 null
     */
    public static Function<Context, Context> withUserInfo(UserInfoDto userInfo) {
        if (userInfo == null) {
            throw new IllegalArgumentException("UserInfo cannot be null");
        }

        return context -> context.put(USER_INFO_KEY, userInfo);
    }
    

    /**
     * 檢查當前 Context 是否包含認證資訊。
     *
     * <p>提供快速的認證狀態檢查，避免不必要的 Context 存取。
     * 返回 Mono&lt;Boolean&gt; 以保持響應式特性。
     *
     * @return 包含認證狀態的 Mono，true 表示已認證，false 表示未認證
     */
    public static Mono<Boolean> isAuthenticated() {
        return Mono.deferContextual(contextView ->
            Mono.just(contextView.hasKey(USER_KEY) && contextView.hasKey(USER_INFO_KEY))
        );
    }
    

    /**
     * 獲取認證用戶的 ID。
     *
     * <p>提供便捷的用戶 ID 存取方法，避免獲取完整用戶物件的開銷。
     * 優先從 UserInfoDto 中獲取，確保資料的時效性。
     *
     * @return 包含用戶 ID 的 Mono，如果未認證則為空
     */
    public static Mono<Long> getCurrentUserId() {
        return getCurrentUserInfo()
            .map(UserInfoDto::getUserId);
    }
    

    /**
     * 從 Reactor Context 中獲取當前的用戶資訊 DTO。
     *
     * <p>提供對用戶基本資訊的快速存取，包含從 JWT 解析出的認證資訊。
     * 相比完整的 User 實體，UserInfoDto 更加輕量，適合頻繁存取。
     *
     * <p><strong>包含資訊：</strong>
     * <ul>
     *   <li>用戶 ID</li>
     *   <li>使用者名稱</li>
     *   <li>用戶角色</li>
     *   <li>令牌過期時間</li>
     * </ul>
     *
     * @return 包含 {@link UserInfoDto} 的 Mono，如果上下文中不存在則為空
     */
    public static Mono<UserInfoDto> getCurrentUserInfo() {
        return Mono.deferContextual(contextView ->
            Mono.justOrEmpty(contextView.getOrEmpty(USER_INFO_KEY))
                .cast(UserInfoDto.class)
        );
    }
    

    /**
     * 獲取認證用戶的使用者名稱。
     * 
     * <p>提供便捷的使用者名稱存取方法，常用於日誌記錄和除錯。
     * 
     * @return 包含使用者名稱的 Mono，如果未認證則為空
     */
    public static Mono<String> getCurrentUsername() {
        return getCurrentUserInfo()
            .map(UserInfoDto::getUsername);
    }
    
    /**
     * 獲取認證用戶的角色。
     * 
     * <p>提供便捷的角色資訊存取，用於權限檢查和功能控制。
     * 
     * @return 包含用戶角色的 Mono，如果未認證則為空
     */
    public static Mono<String> getCurrentUserRole() {
        return getCurrentUserInfo()
            .map(UserInfoDto::getRole);
    }
}