package xyz.dowob.filemanagement.holder;

import xyz.dowob.filemanagement.data.user.dto.UserInfoDto;
import xyz.dowob.filemanagement.entity.User;

/**
 * 認證上下文管理器，提供 ThreadLocal 級別的認證資訊存儲。
 * 
 * <p><strong>重要說明：</strong>此類專門設計為 gRPC 攔截器與 WebFlux 業務邏輯之間的橋樑。
 * 由於 gRPC 攔截器運行在同步環境中，無法直接使用 Reactor Context，因此需要使用
 * ThreadLocal 作為臨時存儲機制。
 * 
 * <p><strong>架構定位：</strong>
 * <ul>
 *   <li><strong>gRPC 層橋樑：</strong>在 gRPC 攔截器中使用 ThreadLocal 存儲認證資訊</li>
 *   <li><strong>臨時存儲：</strong>僅在請求處理期間短暫存儲，請求結束後立即清理</li>
 *   <li><strong>轉換媒介：</strong>在 BaseGrpcController 中將資料轉移到 Reactor Context</li>
 * </ul>
 * 
 * <p><strong>使用限制：</strong>
 * <ul>
 *   <li>僅限於 gRPC 攔截器和 BaseGrpcController 中使用</li>
 *   <li>不應在 WebFlux 業務邏輯中直接使用（應使用 GrpcAuthenticationContextHolder）</li>
 *   <li>必須確保在請求結束後呼叫 clear() 方法清理資源</li>
 * </ul>
 * 
 * <p><strong>生命週期管理：</strong>
 * <pre>
 * gRPC 請求開始 → 攔截器設置 ThreadLocal → BaseGrpcController 讀取並轉換 → 清理 ThreadLocal → gRPC 請求結束
 * </pre>
 * 
 * <p><strong>記憶體安全：</strong>
 * ThreadLocal 如果未正確清理，可能導致記憶體洩漏。本類實現了以下安全機制：
 * <ul>
 *   <li>在 gRPC 攔截器的 finally 區塊中自動清理</li>
 *   <li>提供 isSet() 方法檢查是否有未清理的資源</li>
 *   <li>支援防護性清理，重複清理不會產生副作用</li>
 * </ul>
 * 
 * <p><strong>線程安全性：</strong>
 * ThreadLocal 天然保證執行緒安全，每個執行緒都有自己的資料副本，
 * 不會出現執行緒間的資料干擾問題。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see GrpcAuthenticationContextHolder
 * @see xyz.dowob.filemanagement.grpc.interceptor.AuthenticationInterceptor
 * @see xyz.dowob.filemanagement.grpc.controller.base.BaseGrpcController
 */
public class AuthenticationContext {
    
    /**
     * 當前執行緒的認證用戶存儲。
     * 
     * <p>存儲完整的用戶實體，包含用戶的所有詳細資訊。
     * 主要用於需要完整用戶資料的業務場景。
     */
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();
    
    /**
     * 當前執行緒的用戶資訊 DTO 存儲。
     * 
     * <p>存儲從 JWT 解析出的輕量級用戶資訊，包含：
     * <ul>
     *   <li>用戶 ID</li>
     *   <li>使用者名稱</li>
     *   <li>用戶角色</li>
     *   <li>令牌過期時間</li>
     * </ul>
     */
    private static final ThreadLocal<UserInfoDto> USER_INFO = new ThreadLocal<>();
    

    /**
     * 清理當前執行緒的所有認證上下文。
     *
     * <p><strong>重要：</strong>此方法必須在每個 gRPC 請求結束後呼叫，
     * 否則可能導致記憶體洩漏或資料污染。
     *
     * <p><strong>清理內容：</strong>
     * <ul>
     *   <li>清除當前用戶實體</li>
     *   <li>清除用戶資訊 DTO</li>
     *   <li>釋放 ThreadLocal 資源</li>
     * </ul>
     *
     * <p><strong>呼叫位置：</strong>
     * <ul>
     *   <li>gRPC 攔截器的 finally 區塊</li>
     *   <li>請求完成回呼</li>
     *   <li>請求取消回呼</li>
     * </ul>
     *
     * <p><strong>防護性設計：</strong>
     * 重複呼叫此方法是安全的，不會產生副作用。
     */
    public static void clear() {
        CURRENT_USER.remove();
        USER_INFO.remove();
    }
    

    /**
     * 檢查當前執行緒是否已認證。
     *
     * <p>通過檢查是否同時設定了用戶實體和用戶資訊來判斷認證狀態。
     * 只有兩者都存在時才認為是完全認證狀態。
     *
     * <p><strong>判斷邏輯：</strong>
     * 認證狀態 = (用戶實體 != null) && (用戶資訊 != null)
     *
     * @return true 表示已認證，false 表示未認證或認證不完整
     */
    public static boolean isAuthenticated() {
        return CURRENT_USER.get() != null && USER_INFO.get() != null;
    }
    

    /**
     * 檢查 ThreadLocal 是否設定了任何資料。
     *
     * <p>用於偵測可能的記憶體洩漏或資源未清理情況。
     * 在除錯或監控場景中很有用。
     *
     * @return true 表示 ThreadLocal 中有資料，false 表示已清理或未設定
     */
    public static boolean isSet() {
        return CURRENT_USER.get() != null || USER_INFO.get() != null;
    }
    

    /**
     * 獲取當前認證用戶的 ID。
     *
     * <p>便捷方法，避免獲取完整用戶物件的開銷。
     * 優先從用戶資訊 DTO 中獲取，因為它更輕量且來自快取。
     *
     * @return 用戶 ID，未認證時返回 null
     */
    public static Long getCurrentUserId() {
        UserInfoDto userInfo = USER_INFO.get();
        return userInfo != null ? userInfo.getUserId() : null;
    }
    

    /**
     * 獲取當前認證用戶的使用者名稱。
     *
     * <p>便捷方法，常用於日誌記錄和除錯目的。
     *
     * @return 使用者名稱，未認證時返回 null
     */
    public static String getCurrentUsername() {
        UserInfoDto userInfo = USER_INFO.get();
        return userInfo != null ? userInfo.getUsername() : null;
    }
    

    /**
     * 獲取當前認證用戶的角色。
     *
     * <p>便捷方法，用於權限檢查和功能控制。
     *
     * @return 用戶角色，未認證時返回 null
     */
    public static String getCurrentUserRole() {
        UserInfoDto userInfo = USER_INFO.get();
        return userInfo != null ? userInfo.getRole() : null;
    }
    

    /**
     * 設定完整的認證上下文。
     *
     * <p>便捷方法，同時設定用戶實體和用戶資訊。
     * 確保資料的一致性和原子性。
     *
     * @param user 認證用戶實體
     * @param userInfo 用戶資訊 DTO
     */
    public static void setAuthContext(User user, UserInfoDto userInfo) {
        setCurrentUser(user);
        setUserInfo(userInfo);
    }
    

    /**
     * 建立認證上下文的快照。
     *
     * <p>用於除錯或審計目的，捕獲當前的認證狀態。
     * 返回包含基本認證資訊的字串表示。
     *
     * @return 認證上下文的字串描述
     */
    public static String getContextSnapshot() {
        User user = getCurrentUser();
        UserInfoDto userInfo = getUserInfo();

        if (user == null && userInfo == null) {
            return "AuthenticationContext[未認證]";
        }

        StringBuilder snapshot = new StringBuilder("AuthenticationContext[");

        if (userInfo != null) {
            snapshot.append("userId=").append(userInfo.getUserId())
                   .append(", username=").append(userInfo.getUsername())
                   .append(", role=").append(userInfo.getRole());
        }

        if (user != null) {
            snapshot.append(", userEntityLoaded=true");
        }

        snapshot.append("]");
        return snapshot.toString();
    }
    

    /**
     * 獲取當前執行緒的認證用戶。
     *
     * <p>此方法主要在 BaseGrpcController 中使用，用於將 ThreadLocal
     * 中的用戶資訊轉移到 Reactor Context。
     *
     * <p><strong>返回值說明：</strong>
     * <ul>
     *   <li>如果當前執行緒已認證，返回用戶實體</li>
     *   <li>如果當前執行緒未認證或已清理，返回 null</li>
     * </ul>
     *
     * @return 當前認證的用戶實體，未認證時返回 null
     */
    public static User getCurrentUser() {
        return CURRENT_USER.get();
    }
    

    /**
     * 設定當前執行緒的認證用戶。
     *
     * <p>此方法由 gRPC 攔截器在認證成功後呼叫，將完整的用戶實體
     * 存儲到當前執行緒的 ThreadLocal 中。
     *
     * <p><strong>呼叫時機：</strong>
     * <ul>
     *   <li>gRPC 攔截器完成認證後</li>
     *   <li>用戶上下文快取服務返回用戶實體後</li>
     * </ul>
     *
     * <p><strong>注意事項：</strong>
     * <ul>
     *   <li>應該與 setUserInfo 配對使用，確保資料一致性</li>
     *   <li>不要在業務邏輯中直接呼叫此方法</li>
     * </ul>
     *
     * @param user 要設定的認證用戶實體，允許為 null（用於清理）
     */
    public static void setCurrentUser(User user) {
        if (user == null) {
            CURRENT_USER.remove();
        } else {
            CURRENT_USER.set(user);
        }
    }
    

    /**
     * 獲取當前執行緒的用戶資訊 DTO。
     *
     * <p>返回從 JWT 令牌解析出的輕量級用戶資訊。
     *
     * @return 當前的用戶資訊 DTO，未設定時返回 null
     */
    public static UserInfoDto getUserInfo() {
        return USER_INFO.get();
    }
    

    /**
     * 設定當前執行緒的用戶資訊 DTO。
     *
     * <p>存儲從 JWT 令牌解析出的基本用戶資訊。這些資訊通常來自
     * JWT 快取服務的解析結果。
     *
     * @param userInfo 要設定的用戶資訊 DTO，允許為 null（用於清理）
     */
    public static void setUserInfo(UserInfoDto userInfo) {
        if (userInfo == null) {
            USER_INFO.remove();
        } else {
            USER_INFO.set(userInfo);
        }
    }
}