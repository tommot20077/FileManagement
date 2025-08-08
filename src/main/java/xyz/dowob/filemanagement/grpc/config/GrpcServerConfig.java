package xyz.dowob.filemanagement.grpc.config;

import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.dowob.filemanagement.grpc.interceptor.ApiKeyAuthInterceptor;
import xyz.dowob.filemanagement.unity.LogUnity;

/**
 * gRPC 伺服器端配置類，專責管理 gRPC 服務的安全性和攔截器機制。
 * <p>
 * 此配置類是 gRPC 伺服器端安全架構的核心元件，透過註冊全域攔截器
 * 來實現統一的認證、授權和請求處理邏輯。配置類採用條件式啟動策略，
 * 與 WebDAV 功能模組緊密整合，確保在 WebDAV 服務啟用時同步提供
 * gRPC 通訊能力。
 * <p>
 * 攔截器註冊機制：
 * <ul>
 * <li>透過 {@code @GrpcGlobalServerInterceptor} 註解實現全域攔截器註冊</li>
 * <li>所有進入 gRPC 伺服器的請求都會經過註冊的攔截器處理</li>
 * <li>攔截器按照註冊順序依序執行，形成處理鏈</li>
 * <li>支援請求前處理、響應後處理和例外處理等完整生命週期</li>
 * </ul>
 * <p>
 * 與 WebDAV 整合關係：
 * <ul>
 * <li>當 {@code global.webdav.enabled=true} 時，此配置類才會被啟動</li>
 * <li>gRPC 服務作為 WebDAV 協議的高效能補充，提供二進制檔案傳輸能力</li>
 * <li>共享相同的認證機制和權限控制策略</li>
 * <li>在檔案管理系統中實現協議層面的無縫切換</li>
 * </ul>
 * <p>
 * 安全性攔截器的重要性：
 * <ul>
 * <li>API Key 認證攔截器提供第一道安全防線，驗證客戶端身份</li>
 * <li>防止未授權的 gRPC 服務調用，保護系統資源</li>
 * <li>支援細粒度的權限控制，可根據不同服務方法設定不同的存取權限</li>
 * <li>提供審計追蹤能力，記錄所有 gRPC 服務的存取行為</li>
 * </ul>
 * <p>
 * 配置範例：
 * <pre>
 * # 啟用 WebDAV 和 gRPC 整合
 * global:
 *   webdav:
 *     enabled: true
 *     grpc-support: true
 *   grpc:
 *     server:
 *       security:
 *         api-key-required: true
 * </pre>
 * <p>
 * 注意事項：
 * <ul>
 * <li>攔截器的執行順序會影響安全性檢查的效果，需謹慎設計</li>
 * <li>在高併發環境中，攔截器的效能直接影響 gRPC 服務的整體效能</li>
 * <li>攔截器中的例外處理需要符合 gRPC 協議規範</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ApiKeyAuthInterceptor
 * @see GrpcGlobalServerInterceptor
 * @see ConditionalOnProperty
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "global.webdav",
    name = "enabled",
    havingValue = "true")
public class GrpcServerConfig {
    
    /**
     * API Key 認證攔截器實例，負責 gRPC 服務的身份驗證。
     * <p>
     * 此攔截器是 gRPC 服務安全架構的核心元件，透過建構函數注入的方式
     * 獲得已配置好的認證攔截器實例。攔截器包含以下關鍵功能：
     * <ul>
     * <li>驗證客戶端提供的 API Key 是否有效</li>
     * <li>檢查 API Key 的權限範圍和過期時間</li>
     * <li>記錄認證成功或失敗的審計日誌</li>
     * <li>對未通過認證的請求返回適當的錯誤響應</li>
     * </ul>
     * <p>
     * 該攔截器會在 {@link #globalApiKeyInterceptor()} 方法中被註冊為
     * 全域攔截器，確保所有 gRPC 服務調用都經過安全性檢查。
     */
    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;
    
    /**
     * 註冊全域 API Key 認證攔截器，建立 gRPC 服務的統一安全防護機制。
     * <p>
     * 此方法是 gRPC 伺服器安全配置的核心，透過 {@code @GrpcGlobalServerInterceptor}
     * 註解將 API Key 認證攔截器註冊為全域攔截器，確保所有 gRPC 服務調用
     * 都經過統一的安全性檢查。
     * <p>
     * {@code @GrpcGlobalServerInterceptor} 註解的作用：
     * <ul>
     * <li>標識此 Bean 為全域 gRPC 攔截器</li>
     * <li>自動將攔截器註冊到 gRPC 伺服器的攔截器鏈中</li>
     * <li>確保攔截器在伺服器啟動時被正確載入</li>
     * <li>支援攔截器的優先級排序和執行順序控制</li>
     * </ul>
     * <p>
     * 全域攔截器的執行流程：
     * <ol>
     * <li>客戶端發送 gRPC 請求到伺服器</li>
     * <li>請求首先進入全域攔截器進行前置處理</li>
     * <li>攔截器驗證 API Key 並檢查權限</li>
     * <li>驗證通過後，請求繼續傳遞到具體的服務方法</li>
     * <li>服務方法執行完成後，響應經過攔截器後置處理</li>
     * <li>最終響應返回給客戶端</li>
     * </ol>
     * <p>
     * 安全性保障：
     * <ul>
     * <li>所有 gRPC 服務調用都必須提供有效的 API Key</li>
     * <li>無效或過期的 API Key 會導致請求被拒絕</li>
     * <li>攔截器會記錄所有認證失敗的嘗試，提供安全審計</li>
     * <li>支援黑名單機制，可以封鎖惡意客戶端</li>
     * </ul>
     * <p>
     * 注意事項：
     * <ul>
     * <li>攔截器的效能直接影響 gRPC 服務的響應時間</li>
     * <li>需要確保攔截器中的邏輯是執行緒安全的</li>
     * <li>攔截器中的例外必須正確轉換為 gRPC 狀態碼</li>
     * <li>在高併發場景下，需要注意攔截器的資源消耗</li>
     * </ul>
     *
     * @return 已配置的 API Key 認證攔截器實例
     */
    @Bean
    @GrpcGlobalServerInterceptor
    public ApiKeyAuthInterceptor globalApiKeyInterceptor() {
        LogUnity.info("註冊 gRPC 服務端 API Key 認證攔截器");
        return apiKeyAuthInterceptor;
    }
}