package xyz.dowob.filemanagement.grpc.config;

import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import xyz.dowob.filemanagement.grpc.service.FileProcessingGrpcService;
import xyz.dowob.filemanagement.unity.LogUnity;

/**
 * gRPC 服務條件式配置類，提供彈性的 gRPC 服務註冊機制。
 * <p>
 * 此配置類採用條件式配置策略，透過 {@code @ConditionalOnProperty} 註解實現
 * gRPC 服務的智慧化註冊。只有當應用程式配置檔案中的 {@code global.grpc.enabled}
 * 設定為 {@code true} 時，該配置類才會被 Spring 容器啟動，進而註冊相關的 gRPC 服務。
 * <p>
 * 條件式配置的主要優勢：
 * <ul>
 * <li>在測試環境中，可透過設定 {@code global.grpc.enabled=false} 來停用 gRPC 服務，
 *     避免端口衝突和不必要的網路連接</li>
 * <li>在生產環境中，可根據實際需求動態開啟或關閉 gRPC 功能，提升資源利用效率</li>
 * <li>支援微服務架構中的功能模組化，不同服務節點可選擇性地啟用 gRPC 能力</li>
 * </ul>
 * <p>
 * 透過 {@code @Import} 註解自動匯入 Spring Boot gRPC 的核心自動配置類，
 * 確保 gRPC 伺服器工廠、服務註冊機制等基礎設施正常運作。這種設計遵循
 * Spring Boot Auto-Configuration 的最佳實務，實現了配置的自動化和標準化。
 * <p>
 * 配置範例：
 * <pre>
 * # 啟用 gRPC 服務
 * global:
 *   grpc:
 *     enabled: true
 *     port: 9090
 * 
 * # 停用 gRPC 服務（測試環境）
 * global:
 *   grpc:
 *     enabled: false
 * </pre>
 * <p>
 * 注意事項：
 * <ul>
 * <li>該配置類與 Spring 容器生命週期緊密結合，啟動順序受 gRPC 自動配置類影響</li>
 * <li>在叢集環境中，需確保 gRPC 端口配置不衝突</li>
 * <li>條件不滿足時，整個配置類及其 Bean 都不會被註冊到容器中</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ConditionalOnProperty
 * @see GrpcService
 * @see FileProcessingGrpcService
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "global.grpc", name = "enabled", havingValue = "true")
@Import({GrpcServerAutoConfiguration.class, GrpcServerFactoryAutoConfiguration.class})
public class GrpcServiceConfiguration {
    
    /**
     * 檔案處理 gRPC 服務實例，透過建構函數注入。
     * <p>
     * 此服務實例包含檔案操作相關的 gRPC 方法實現，如檔案上傳、下載、
     * 轉換等核心功能。透過 Spring 的依賴注入機制，確保服務實例在
     * 配置類實例化時即可使用。
     * <p>
     * 注入的服務實例會在 {@link #grpcFileProcessingService()} 方法中
     * 被註冊為正式的 gRPC 服務，供外部客戶端調用。
     */
    private final FileProcessingGrpcService fileProcessingGrpcService;
    
    /**
     * 註冊檔案處理 gRPC 服務 Bean，實現條件式服務註冊。
     * <p>
     * 此方法是 gRPC 服務註冊的核心，透過 {@code @Bean} 和 {@code @GrpcService}
     * 註解的組合，將注入的 {@code FileProcessingGrpcService} 實例正式註冊為
     * Spring 管理的 gRPC 服務 Bean。
     * <p>
     * 註冊流程說明：
     * <ol>
     * <li>Spring 容器在條件滿足時（global.grpc.enabled=true）啟動此配置類</li>
     * <li>透過建構函數注入獲得 FileProcessingGrpcService 實例</li>
     * <li>此方法將該實例包裝為 Spring Bean 並標記為 gRPC 服務</li>
     * <li>gRPC 伺服器自動發現並註冊該服務，開始監聽客戶端請求</li>
     * </ol>
     * <p>
     * {@code @GrpcService} 註解的作用：
     * <ul>
     * <li>標識此 Bean 為 gRPC 服務實現</li>
     * <li>觸發 gRPC 框架的自動服務發現機制</li>
     * <li>確保服務在 gRPC 伺服器啟動時被正確註冊</li>
     * <li>支援服務攔截器、元數據處理等進階功能</li>
     * </ul>
     * <p>
     * 注意事項：
     * <ul>
     * <li>此方法僅在配置條件滿足時執行，否則不會建立 Bean</li>
     * <li>返回的實例與注入的實例為同一個物件，避免重複實例化</li>
     * <li>gRPC 服務的生命週期與 Spring 容器同步</li>
     * </ul>
     *
     * @return 已註冊的 gRPC 檔案處理服務實例
     */
    @Bean
    @GrpcService
    public FileProcessingGrpcService grpcFileProcessingService() {
        LogUnity.info("註冊檔案處理 gRPC 服務");
        return fileProcessingGrpcService;
    }
}