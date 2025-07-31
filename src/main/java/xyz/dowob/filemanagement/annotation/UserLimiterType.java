package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.UserLimiterEnum;

import java.lang.annotation.*;

/**
 * 用戶限流器類型標記註解。
 * <p>
 * 此註解用於標記不同類型的用戶限流器實現類別，為系統的策略模式架構提供支援。
 * 透過此註解，系統能夠於執行時期動態選擇適當的限流策略實現，實現可插拔的
 * 用戶限流管理。
 * <p>
 * 支援的限流策略類型：
 * <p>
 * LOGIN：登入限流器，控制用戶登入頻率和失敗次數
 * <p>
 * UPLOAD：上傳限流器，管理檔案上傳的大小、頻率和並發次數
 * <p>
 * REQUEST：請求限流器，控制 API 請求的頻率和並發數
 * <p>
 * DOWNLOAD：下載限流器，管理檔案下載的頻率和流量
 * <p>
 * 策略模式整合：
 * <p>
 * 此註解與 {@link xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy} 策略類別
 * 緊密配合，創建了一個完整的可擴展限流管理架構。策略類別會根據此註解
 * 的標記自動注冊和選擇對應的限流器實現。
 * <p>
 * 實現架構：
 * <p>
 * 實現 {@link xyz.dowob.filemanagement.component.limiter.UserLimiter} 接口
 * <p>
 * 使用 @UserLimiterType 註解標記實現類別
 * <p>
 * 系統啟動時自動掃描和注冊所有標記的實現
 * <p>
 * 透過 UserLimiterStrategy 獲取適當的限流器實例
 * <p>
 * 用戶限流功能：
 * <p>
 * 支援本地記憶體和 Redis 分散式快取兩種實現
 * <p>
 * 可設定的限流參數（時間窗口、最大次數等）
 * <p>
 * 支援動態設定更新，無需重啟應用
 * <p>
 * 異常狀態處理和降級策略
 * <p>
 * 統計和監控支援
 * <p>
 * 使用範例：
 * <pre>{@code
 * // 登入限流器實現
 * @Component
 * @UserLimiterType(UserLimiterEnum.LOGIN)
 * public class RedisUserLoginLimiter implements UserLimiter {
 *     // 實現登入限流邏輯
 * }
 * 
 * // 上傳限流器實現
 * @Component
 * @UserLimiterType(UserLimiterEnum.UPLOAD)
 * public class LocalUserUploadLimiter implements UserLimiter {
 *     // 實現上傳限流邏輯
 * }
 * 
 * // 使用策略類別獲取限流器
 * @Autowired
 * private UserLimiterStrategy userLimiterStrategy;
 * 
 * public void handleUserLogin(String userId) {
 *     UserLimiter limiter = userLimiterStrategy.getUserLimiter(UserLimiterEnum.LOGIN);
 *     if (limiter.isAllowed(userId)) {
 *         // 執行登入邏輯
 *     }
 * }
 * }</pre>
 * <p>
 * 效能考量：
 * <p>
 * 本地實現適合單機部署，性能優異但無法跨節點共享
 * <p>
 * Redis 實現適合集群部署，支援分散式限流但有網路開銷
 * <p>
 * 支援非阻塞式處理，適合 WebFlux 反應式環境
 * <p>
 * 細粒度的時間窗口管理，減少記憶體使用
 * <p>
 * 安全性考量：
 * <p>
 * 防止暴力破解和 DDoS 攻擊
 * <p>
 * 支援逐步增加限制的懲罰機制
 * <p>
 * 異常流量監控和告警
 * <p>
 * 暴力攻擊和快取穿透防護
 *
 * @see xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy
 * @see xyz.dowob.filemanagement.component.limiter.UserLimiter
 * @see xyz.dowob.filemanagement.customenum.UserLimiterEnum
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UserLimiterType {
    /**
     * 指定用戶限流器的類型。
     * <p>
     * 此屬性定義了當前實現類別所負責的限流管理範圍和特性。系統會根據此類型
     * 自動將實現類別注冊到對應的策略管理器中，並在需要時提供相應的限流服務。
     * <p>
     * 不同類型的限流器具有不同的特性和應用場景：
     * <p>
     * LOGIN 類型：適用於用戶身份驗證、密碼重設等安全操作
     * <p>
     * UPLOAD 類型：適用於檔案上傳、內容發佈等資源消耗操作
     * <p>
     * REQUEST 類型：適用於 API 調用、資料查詢等一般性操作
     * <p>
     * DOWNLOAD 類型：適用於檔案下載、內容流輸出等頻寬消耗操作
     *
     * @return 用戶限流器類型，使用 {@link xyz.dowob.filemanagement.customenum.UserLimiterEnum} 定義的類型
     */
    UserLimiterEnum value();
}
