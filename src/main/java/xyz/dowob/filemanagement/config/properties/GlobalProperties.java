package xyz.dowob.filemanagement.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 全域系統設定屬性類，管理整個應用程式的核心系統參數。
 * <p>
 * 此類包含請求限制器、網路轉發設定、Netty 連接池與郵件發送等全域性設定。
 * 透過 Spring Boot 的 {@code @ConfigurationProperties} 機制，可在 application.yml 中使用 "global" 前綴進行設定。
 * <p>
 * 設定範例：
 * <pre>
 * global:
 *   request-limiter:
 *     type: redis
 *     limit: 500
 *     refill-duration: 1m
 *   forwarded:
 *     x-forwarded-header: X-Forwarded-For
 *     x-real-ip-header: X-Real-IP
 *   netty-pool:
 *     max-connections: 10
 *     max-idle-time: 1m
 *   email:
 *     mail-sender: noreply@example.com
 * </pre>
 * <p>
 * 主要功能模組包括：
 * - {@link RequestLimiter} - 請求限制器設定
 * - {@link forwarded} - 網路轉發設定
 * - {@link NettyPool} - Netty 連接池設定
 * - {@link Email} - 郵件發送設定
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "global")
public class GlobalProperties {
    /**
     * 請求限制器設定實例。
     * <p>
     * 提供基於令牌桶演算法的請求限制功能，支援 Redis 和本地兩種實現方式。
     */
    private RequestLimiter requestLimiter = new RequestLimiter();

    /**
     * HTTP 請求轉發設定實例。
     * <p>
     * 用於在反向代理或負載平衡器環境中正確獲取用戶的真實 IP 位址。
     */
    private forwarded forwarded = new forwarded();

    /**
     * Netty 連接池設定實例。
     * <p>
     * 管理 WebFlux 使用的 Netty 連接池資源，包括連接數量、生命週期等設定。
     */
    private NettyPool nettyPool = new NettyPool();

    /**
     * 郵件發送設定實例。
     * <p>
     * 定義系統發送郵件時使用的發送者資訊。
     */
    private Email email = new Email();

    /**
     * 請求限制器設定內部類。
     * <p>
     * 基於令牌桶演算法實現的請求速率限制機制，支援 Redis 分散式限制和本地記憶體限制兩種模式。
     * 提供惡意 IP 自動封禁功能，有效防護 DDoS 攻擊和惡意存取。
     */
    @Data
    public static class RequestLimiter {
        /**
         * 請求限制器的實現類型。
         * <p>
         * 決定令牌桶的儲存後端和分散式支援能力：
         * - {@code redis} - 使用 Redis 實現分散式限制，適合多實例部署
         * - {@code local} - 使用本地記憶體實現，適合單實例部署
         * - {@code none} - 關閉請求限制功能
         * <p>
         * 預設值：{@code LimitType.redis}
         */
        private LimitType type = LimitType.redis;

        /**
         * 令牌桶的最大容量限制。
         * <p>
         * 定義每個 IP 位址在令牌桶中可以存儲的最大令牌數量。
         * 當請求次數超過此限制時，系統會回傳 HTTP 429 Too Many Requests 錯誤。
         * <p>
         * 當設定值小於或等於 0 時，使用預設值 {@code Integer.MAX_VALUE}。
         * <p>
         * 預設值：500
         */
        private int limit = 500;

        /**
         * 每個補充週期內恢復的令牌數量。
         * <p>
         * 定義在每個 {@link #refillDuration} 時間內恢復的令牌數量。
         * 合理的補充率可以平衡使用者正常使用和惡意攻擊防護。
         * <p>
         * 當設定值小於或等於 0 時，補充數量等於 {@link #limit} 的值。
         * <p>
         * 預設值：-1（補充全部令牌）
         */
        private int refill = -1;

        /**
         * 令牌補充的時間間隔。
         * <p>
         * 定義令牌桶進行令牌補充的時間週期。在此時間內，系統會逐漸向令牌桶添加新的令牌。
         * 較短的補充週期能更快恢復服務可用性，但可能增加系統負載。
         * <p>
         * 約束條件：此值必須大於 0，否則會拋出設定異常。
         * <p>
         * 預設值：1 分鐘
         */
        private Duration refillDuration = Duration.ofMinutes(1);

        /**
         * 令牌桶清理的時間間隔。
         * <p>
         * 定義系統清理非活躍令牌桶的時間間隔，有助於釋放記憶體資源和維護系統效能。
         * 超過此時間沒有活動的 IP 位址對應的令牌桶會被清除。
         * <p>
         * 約束條件：此值必須大於 0，否則會拋出設定異常。
         * <p>
         * 預設值：10 分鐘
         */
        private Duration cleanInterval = Duration.ofMinutes(10);

        /**
         * 是否啟用 IP 自動封禁機制。
         * <p>
         * 當啟用時，系統會追蹤每個 IP 位址的請求失敗記錄。
         * 若某個 IP 在 {@link #banIpDuration} 時間窗口內失敗次數超過 {@link #failureCount}，
         * 該 IP 將被暫時封禁 {@link #banExpireDuration} 時間，期間無法存取系統。
         * <p>
         * 預設值：{@code true}
         */
        private boolean enableBanIp = true;

        /**
         * IP 封禁判定的時間窗口。
         * <p>
         * 定義系統計算 IP 失敗次數的時間範圍。在此時間窗口內，
         * 若某個 IP 的請求失敗次數超過 {@link #failureCount}，將觸發封禁機制。
         * <p>
         * 約束條件：此值必須大於 0，否則會拋出設定異常。
         * <p>
         * 預設值：10 分鐘
         */
        private Duration banIpDuration = Duration.ofMinutes(10);

        /**
         * IP 封禁的持續時間。
         * <p>
         * 定義被封禁的 IP 位址無法存取系統的時間長度。
         * 超過此時間後，封禁狀態會自動解除，該 IP 可以重新正常存取系統。
         * <p>
         * 約束條件：此值必須大於 0，否則會拋出設定異常。
         * <p>
         * 預設值：1 小時
         */
        private Duration banExpireDuration = Duration.ofHours(1);

        /**
         * 觸發 IP 封禁的失敗次數閾值。
         * <p>
         * 定義在 {@link #banIpDuration} 時間窗口內，某個 IP 位址可以累積的最大失敗請求次數。
         * 當失敗次數達到或超過此閾值時，該 IP 將被自動封禁。
         * <p>
         * 約束條件：此值必須大於 0，否則會拋出設定異常。
         * <p>
         * 預設值：5
         */
        private int failureCount = 5;


        /**
         * 請求限制器類型列舉。
         * <p>
         * 定義請求限制器的實現方式和儲存後端選項。
         */
        private enum LimitType {
            /**
             * Redis 分散式實現。
             * <p>
             * 使用 Bucket4j 結合 Redis 實現分散式令牌桶，
             * 適合多實例部署環境，令牌狀態在所有實例間共享。
             */
            redis,

            /**
             * 本地記憶體實現。
             * <p>
             * 使用 Bucket4j 的本地記憶體實現，
             * 適合單實例部署，效能較高但不支援分散式。
             */
            local,

            /**
             * 關閉請求限制。
             * <p>
             * 停用所有請求限制功能，所有請求都會被允許通過。
             * 僅建議在開發環境或特殊情況下使用。
             */
            none
        }
    }

    /**
     * HTTP 請求轉發設定內部類。
     * <p>
     * 用於在反向代理或負載平衡器環境中正確識別用戶的真實 IP 位址。
     * 支援多種常見的轉發標頭格式，確保 IP 限制和日誌記錄的準確性。
     */
    @Data
    public static class forwarded {
        /**
         * X-Forwarded-For 請求標頭名稱。
         * <p>
         * 指定包含客戶端原始 IP 位址的 HTTP 標頭名稱。
         * 此標頭通常由反向代理（如 Nginx、Apache）或負載平衡器設定，
         * 用於保存通過多層代理時的原始客戶端 IP 位址。
         * <p>
         * 預設值：{@code "X-Forwarded-For"}
         */
        private String xForwardedHeader = "X-Forwarded-For";

        /**
         * X-Real-IP 請求標頭名稱。
         * <p>
         * 指定包含客戶端真實 IP 位址的 HTTP 標頭名稱。
         * 此標頭通常由反向代理設定，提供比 X-Forwarded-For 更直接的客戶端 IP 識別方式。
         * <p>
         * 預設值：{@code "X-Real-IP"}
         */
        private String xRealIpHeader = "X-Real-IP";
    }

    /**
     * Netty 連接池設定內部類。
     * <p>
     * 管理 Spring WebFlux 使用的 Netty 連接池資源，包括連接生命週期、
     * 併發限制和等待超時等核心參數。合理的連接池設定可以顯著提升系統效能。
     */
    @Data
    public static class NettyPool {
        /**
         * 連接的最大閒置時間。
         * <p>
         * 定義連接在連接池中保持閒置狀態的最長時間。
         * 超過此時間的閒置連接會被自動關閉以釋放資源。
         * <p>
         * 預設值：1 分鐘
         */
        private Duration maxIdleTime = Duration.ofMinutes(1);

        /**
         * 連接的最大生命週期。
         * <p>
         * 定義單個連接在連接池中的最長存活時間，無論是否處於使用狀態。
         * 超過此時間的連接會被強制關閉並重新建立，有助於避免長期連接的穩定性問題。
         * <p>
         * 預設值：1 小時
         */
        private Duration maxLifeTime = Duration.ofHours(1);

        /**
         * 連接池的最大併發連接數量。
         * <p>
         * 限制同時活躍的連接數量，有助於控制資源使用和避免連接洩漏。
         * 當達到此限制時，新的連接請求會進入等待佇列。
         * <p>
         * 預設值：10
         */
        private int maxConnections = 10;

        /**
         * 連接獲取的最大等待超時時間。
         * <p>
         * 定義當連接池滿載時，新連接請求在佇列中等待的最長時間。
         * 超過此時間的請求會拋出超時異常。
         * <p>
         * 預設值：30 秒
         */
        private Duration pendingAcquireTimeout = Duration.ofSeconds(30);
    }

    /**
     * 郵件發送設定內部類。
     * <p>
     * 管理系統發送郵件時使用的發送者資訊和相關參數。
     */
    @Data
    public static class Email {
        /**
         * 系統郵件發送者的電子郵件地址。
         * <p>
         * 定義系統發送各種通知郵件（如密碼重設、帳號驗證等）時使用的發送者地址。
         * 此地址會顯示在收件者的郵件客戶端中作為發送者資訊。
         * <p>
         * 注意：此地址必須與 SMTP 伺服器設定中的認證資訊相符，
         * 否則可能導致郵件發送失敗或被標記為垃圾郵件。
         * <p>
         * 預設值：{@code "sender@example.com"}
         */
        private String mailSender = "sender@example.com";
    }
}
