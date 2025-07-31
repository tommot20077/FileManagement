package xyz.dowob.filemanagement.component.filter.requestlimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.filter.ClientIpFilter;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.CacheConcurrentHashMap;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基於本地記憶體的 IP 請求限流過濾器，使用令牌桶演算法實現流量控制。
 *
 * <p>本過濾器採用 Bucket4j 令牌桶演算法與 CacheConcurrentHashMap 自訂快取實現，
 * 提供高效能、可配置的基於 IP 地址的請求流量限制功能。</p>
 *
 * <h3>主要功能特性：</h3>
 * <ul>
 *   <li><strong>令牌桶限流</strong>：支援可配置的令牌容量和補充速率</li>
 *   <li><strong>IP 封禁機制</strong>：當請求失敗次數達到閾值時自動暫時封禁 IP</li>
 *   <li><strong>記憶體緩存</strong>：使用本地記憶體快取，提供快速存取性能</li>
 *   <li><strong>定時清理</strong>：自動清理過期的限流記錄和封禁狀態</li>
 *   <li><strong>反應式設計</strong>：基於 Spring WebFlux 的非阻塞式處理</li>
 * </ul>
 *
 * <h3>工作原理：</h3>
 * <p>系統為每個 IP 地址維護一個令牌桶，令牌桶以固定速率補充令牌。
 * 每次請求消耗一個令牌，當令牌不足時拒絕請求並回傳 HTTP 429 狀態碼。
 * 若啟用 IP 封禁功能，系統會追蹤請求失敗次數，達到閾值時暫時封禁該 IP。</p>
 *
 * <h3>限制與注意事項：</h3>
 * <ul>
 *   <li>僅適用於單機部署環境，不支援叢集間狀態同步</li>
 *   <li>限流狀態與封禁列表儲存於記憶體中，服務重啟後會重置</li>
 *   <li>需要適當配置快取大小以避免記憶體溢出</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.unity.CacheConcurrentHashMap
 * @see xyz.dowob.filemanagement.component.filter.ClientIpFilter
 * @see io.github.bucket4j.Bucket
 */
@Component
@ConditionalOnProperty(prefix = "global", name = "request-limiter.type", havingValue = "local")
public class localRequestLimiterFilter implements WebFilter, ResponseUnity {
    /**
     * 限流器快取鍵的固定前綴。
     *
     * <p>用於構建每個 IP 地址對應的令牌桶快取鍵，格式為：{@code ip-request-limiter:IP地址}。
     * 此前綴確保限流器快取與其他業務快取的鍵不會發生衝突。</p>
     */
    private final static String KEY_PREFIX = "ip-request-limiter:";

    /**
     * 令牌桶的最大容量限制。
     *
     * <p>定義每個 IP 地址的令牌桶所能儲存的最大令牌數量。
     * 當設定值小於或等於 0 時，系統將使用 {@link Integer#MAX_VALUE} 作為預設值，
     * 相當於不限制請求頻率。</p>
     *
     * @see #refill
     * @see #refillDuration
     */
    private final int limit;

    /**
     * 令牌桶的補充令牌數量。
     *
     * <p>定義在 {@link #refillDuration} 時間週期內補充到令牌桶的令牌數量。
     * 此參數決定了請求的允許頻率，較高的補充率允許更頻繁的請求。</p>
     *
     * <p>當設定值小於或等於 0 時，系統將使用 {@link #limit} 的值作為補充數量，
     * 意味著令牌桶在每個補充週期內會完全填滿。</p>
     *
     * @see #limit
     * @see #refillDuration
     */
    private final int refill;

    /**
     * 令牌補充的時間週期。
     *
     * <p>定義令牌桶補充 {@link #refill} 數量令牌所需的時間間隔。
     * 例如，若 refill 為 10，refillDuration 為 1 分鐘，
     * 則表示每分鐘補充 10 個令牌，即每 6 秒補充 1 個令牌。</p>
     *
     * <p>此參數必須為正值，不能為零或負數。</p>
     *
     * @see #refill
     * @see #limit
     */
    private final Duration refillDuration;

    /**
     * IP 封禁的持續時間。
     *
     * <p>定義當 IP 地址被封禁後，封禁狀態的持續時間。
     * 在此時間週期內，該 IP 的所有請求都會被直接拒絕，回傳 HTTP 403 狀態碼。</p>
     *
     * <p>當設定值小於或等於 0 時，系統將使用預設值 1 小時。
     * 封禁時間結束後，該 IP 的封禁狀態會自動清除，可以重新發送請求。</p>
     *
     * <p><strong>注意：</strong>此參數僅在 {@link #isEnableBanIp} 為 true 時生效。</p>
     *
     * @see #isEnableBanIp
     * @see #banDuration
     * @see #failureCount
     */
    private final Duration banExpireDuration;

    /**
     * IP 封禁判斷的時間視窗。
     *
     * <p>定義統計請求失敗次數的時間視窗長度。
     * 系統會在此時間視窗內累計該 IP 的請求失敗次數，
     * 當累計失敗次數達到 {@link #failureCount} 閾值時，觸發 IP 封禁機制。</p>
     *
     * <p>當設定值小於或等於 0 時，系統將使用預設值 10 分鐘。
     * 時間視窗採用滑動視窗機制，過期的失敗記錄會自動清除。</p>
     *
     * <p><strong>注意：</strong>此參數僅在 {@link #isEnableBanIp} 為 true 時生效。</p>
     *
     * @see #isEnableBanIp
     * @see #banExpireDuration
     * @see #failureCount
     */
    private final Duration banDuration;

    /**
     * IP 封禁功能的啟用開關。
     *
     * <p>控制是否啟用基於請求失敗次數的自動 IP 封禁功能。</p>
     *
     * <p>當設定為 {@code true} 時：</p>
     * <ul>
     *   <li>系統會追蹤每個 IP 的請求失敗次數</li>
     *   <li>檢查請求的 IP 是否在封禁列表中</li>
     *   <li>當失敗次數達到閾值時自動封禁該 IP</li>
     *   <li>被封禁的 IP 請求會直接回傳 HTTP 403 狀態碼</li>
     * </ul>
     *
     * <p>當設定為 {@code false} 時，僅執行基本的令牌桶限流，不進行 IP 封禁。</p>
     *
     * @see #failureCount
     * @see #banDuration
     * @see #banExpireDuration
     */
    private final boolean isEnableBanIp;

    /**
     * 令牌桶限流器的快取儲存容器。
     *
     * <p>使用 {@link CacheConcurrentHashMap} 實現，為每個 IP 地址儲存對應的 {@link Bucket} 令牌桶實例。
     * 快取具有自動過期和清理機制，釋放不再使用的記憶體資源。</p>
     *
     * <p>快取鍵格式：{@code ip-request-limiter:IP地址}</p>
     * <p>快取值：該 IP 對應的令牌桶實例</p>
     *
     * @see CacheConcurrentHashMap
     * @see Bucket
     */
    private final CacheConcurrentHashMap<String, Bucket> ipBucketLimiterMap;

    /**
     * JSON 物件映射器。
     *
     * <p>用於將錯誤回應物件序列化為 JSON 格式，以便在 HTTP 回應中傳送結構化的錯誤訊息。
     * 主要用於 {@link #filter(ServerWebExchange, WebFilterChain)} 方法中的錯誤回應處理。</p>
     *
     * @see ObjectMapper
     * @see ResponseUnity#sendErrorResponse(ServerWebExchange, ObjectMapper, ValidationException.ErrorCode, Object...)
     */
    private final ObjectMapper objectMapper;

    /**
     * 觸發 IP 封禁的失敗次數閾值。
     *
     * <p>定義在 {@link #banDuration} 時間視窗內，當該 IP 的請求失敗次數達到此閾值時，
     * 系統會自動將該 IP 加入封禁列表，封禁持續時間為 {@link #banExpireDuration}。</p>
     *
     * <p>請求失敗包括但不限於：</p>
     * <ul>
     *   <li>令牌桶耗盡導致的請求被拒絕</li>
     *   <li>其他可能導致請求處理失敗的情況</li>
     * </ul>
     *
     * <p>當設定值小於或等於 0 時，建構子會拋出 {@link IllegalArgumentException}。</p>
     *
     * <p><strong>注意：</strong>此參數僅在 {@link #isEnableBanIp} 為 true 時生效。</p>
     *
     * @see #isEnableBanIp
     * @see #banDuration
     * @see #banExpireDuration
     */
    private int failureCount = 5;

    /**
     * IP 封禁狀態的快取儲存容器。
     *
     * <p>當 {@link #isEnableBanIp} 啟用時，使用 {@link CacheConcurrentHashMap} 
     * 儲存每個 IP 地址的請求失敗次數和封禁狀態。</p>
     *
     * <p>快取項目結構：</p>
     * <ul>
     *   <li><strong>快取鍵</strong>：{@code ip-request-limiter:IP地址}</li>
     *   <li><strong>快取值</strong>：該 IP 在時間視窗內的累計失敗次數</li>
     * </ul>
     *
     * <p>當累計失敗次數達到 {@link #failureCount} 閾值時，
     * 系統會延長該項目的過期時間至 {@link #banExpireDuration}，實現 IP 封禁。</p>
     *
     * <p>若 {@link #isEnableBanIp} 為 false，此欄位保持為 {@code null}。</p>
     *
     * @see CacheConcurrentHashMap
     * @see #isEnableBanIp
     */
    private CacheConcurrentHashMap<String, Long> banIpMap = null;


    /**
     * 建構子，初始化本地請求限流過濾器的所有參數和快取容器。
     *
     * <p>根據 {@link GlobalProperties} 中的配置初始化限流器參數，並建立對應的快取容器。
     * 初始化過程包括以下步驟：</p>
     *
     * <ol>
     *   <li>驗證所有必要參數的有效性（必須為正值）</li>
     *   <li>設定令牌桶相關參數（容量、補充數量、補充週期）</li>
     *   <li>初始化令牌桶快取容器</li>
     *   <li>根據是否啟用 IP 封禁功能，初始化封禁相關參數和快取</li>
     * </ol>
     *
     * <p><strong>參數預設值處理：</strong></p>
     * <ul>
     *   <li>若 limit ≤ 0，則設為 {@link Integer#MAX_VALUE}</li>
     *   <li>若 refill ≤ 0，則設為 limit 的值</li>
     *   <li>其他時間相關參數必須為正值，否則拋出異常</li>
     * </ul>
     *
     * @param objectMapper JSON 物件映射器，用於序列化錯誤回應
     * @param globalProperties 全域配置屬性物件，包含限流器的所有配置參數
     *
     * @throws IllegalArgumentException 當以下任一條件成立時拋出：
     *   <ul>
     *     <li>清理間隔時間不是正值</li>
     *     <li>令牌補充週期不是正值</li>
     *     <li>IP 封禁計算時間不是正值</li>
     *     <li>IP 封禁持續時間不是正值</li>
     *     <li>失敗次數閾值不是正值</li>
     *   </ul>
     *
     * @see GlobalProperties.RequestLimiter
     * @see CacheConcurrentHashMap
     * @see ObjectMapper
     */
    public localRequestLimiterFilter(ObjectMapper objectMapper, GlobalProperties globalProperties) {
        GlobalProperties.RequestLimiter requestLimiter = globalProperties.getRequestLimiter();

        Assert.isTrue(requestLimiter.getCleanInterval().isPositive(), "請求限制器的清除時間比率必須大於0");
        Assert.isTrue(requestLimiter.getRefillDuration().isPositive(), "請求限制器的補充令牌週期必須大於0");
        Assert.isTrue(requestLimiter.getBanIpDuration().isPositive(), "禁止IP的計算時間必須大於0");
        Assert.isTrue(requestLimiter.getBanExpireDuration().isPositive(), "禁止IP的封禁時間必須大於0");
        Assert.isTrue(requestLimiter.getFailureCount() > 0, "禁止IP的失敗次數必須大於0");

        this.objectMapper = objectMapper;
        Duration cleanupInterval = requestLimiter.getCleanInterval();

        this.limit = requestLimiter.getLimit() <= 0 ? Integer.MAX_VALUE : requestLimiter.getLimit();
        this.refill = requestLimiter.getRefill() <= 0 ? this.limit : requestLimiter.getRefill();
        this.refillDuration = requestLimiter.getRefillDuration();

        this.ipBucketLimiterMap = new CacheConcurrentHashMap<>(1024, this.refillDuration, this.refillDuration, cleanupInterval, false);
        this.ipBucketLimiterMap.setTag("請求限制器緩存表");

        this.banDuration = requestLimiter.getBanIpDuration();
        this.banExpireDuration = requestLimiter.getBanExpireDuration();

        this.isEnableBanIp = requestLimiter.isEnableBanIp();
        if (this.isEnableBanIp) {
            this.banIpMap = new CacheConcurrentHashMap<>(64, this.banDuration, this.banExpireDuration, cleanupInterval, false);
            this.banIpMap.setTag("封禁IP地址緩存表");
            this.failureCount = requestLimiter.getFailureCount();
        }
    }

    /**
     * WebFlux 過濾器的核心處理方法，實現基於 IP 的請求限流和封禁邏輯。
     *
     * <p>此方法採用反應式程式設計模式，非阻塞地處理每個 HTTP 請求。處理流程如下：</p>
     *
     * <ol>
     *   <li><strong>IP 地址提取</strong>：從請求中提取真實的客戶端 IP 地址</li>
     *   <li><strong>封禁檢查</strong>：若啟用 IP 封禁功能，檢查該 IP 是否被封禁</li>
     *   <li><strong>令牌桶處理</strong>：獲取或建立該 IP 對應的令牌桶，嘗試消耗令牌</li>
     *   <li><strong>請求放行或拒絕</strong>：根據令牌可用性決定放行請求或拒絕並更新失敗計數</li>
     *   <li><strong>封禁狀態更新</strong>：若請求被拒絕且啟用封禁功能，更新失敗計數並檢查是否觸發封禁</li>
     * </ol>
     *
     * <p><strong>回應狀態碼：</strong></p>
     * <ul>
     *   <li><strong>200/正常處理</strong>：請求通過限流檢查，正常處理</li>
     *   <li><strong>400 Bad Request</strong>：無法提取有效的 IP 地址</li>
     *   <li><strong>403 Forbidden</strong>：IP 地址已被封禁</li>
     *   <li><strong>429 Too Many Requests</strong>：請求頻率超過限制</li>
     * </ul>
     *
     * <p><strong>封禁機制說明：</strong></p>
     * <p>當啟用 IP 封禁功能時，系統會在 {@link #banDuration} 時間視窗內累計該 IP 的失敗次數。
     * 當累計次數達到 {@link #failureCount} 閾值時，該 IP 會被封禁 {@link #banExpireDuration} 時間。</p>
     *
     * @param exchange WebFlux 伺服器請求交換物件，包含請求和回應資訊
     * @param chain 過濾器鏈，用於傳遞請求到下一個過濾器或處理器
     *
     * @return {@link Mono<Void>} 表示異步處理結果的反應式物件
     *
     * @see ClientIpFilter#getClientIpFromExchange(ServerWebExchange)
     * @see Bucket#tryConsume(long)
     * @see CacheConcurrentHashMap#computeIfPresentOrDefault(Object, Object, Duration, java.util.function.BiFunction)
     */
    @Nonnull
    @Override
    public Mono<Void> filter(@Nonnull ServerWebExchange exchange, @Nonnull WebFilterChain chain) {
        Optional<String> ipOptional = ClientIpFilter.getClientIpFromExchange(exchange);
        if (ipOptional.isEmpty()) {
            LogUnity.debug(exchange, "無法獲取用戶 IP: %s, 拒絕連線", exchange.getRequest().getRemoteAddress());
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return sendErrorResponse(exchange, objectMapper, ValidationException.ErrorCode.REQUEST_IS_INVALID, "IP 地址");
        }
        return Mono.just(ipOptional.get()).flatMap(ip -> {
            String key = KEY_PREFIX + ip;

            if (isEnableBanIp) {
                Long failCount = banIpMap.check(key);
                if (failCount != null && failCount >= failureCount) {
                    LogUnity.debug(exchange, "IP: %s 已暫時禁止訪問", ip);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return sendErrorResponse(exchange, objectMapper, ValidationException.ErrorCode.ALREADY_BAN_IP);
                }
            }

            Bucket bucket = ipBucketLimiterMap.get(key);
            if (bucket == null) {
                bucket = Bucket.builder().addLimit(stage -> stage.capacity(limit).refillGreedy(refill, refillDuration)).build();
                ipBucketLimiterMap.set(key, bucket);
            }
            if (bucket.tryConsume(1)) {
                return chain.filter(exchange);
            }

            LogUnity.info(exchange, "IP: %s 請求超過限制值: %s", ip, limit);
            if (isEnableBanIp) {
                AtomicBoolean isBanned = new AtomicBoolean(false);
                banIpMap.computeIfPresentOrDefault(key, 0L, banDuration, (k, currentValue) -> {
                    long newValue = currentValue == null ? 1L : currentValue + 1;
                    if (newValue >= failureCount) {
                        isBanned.set(true);
                        banIpMap.set(key, newValue, banExpireDuration);
                    }
                    return newValue;
                });

                if (isBanned.get()) {
                    LogUnity.warn(exchange, "IP: %s 請求過於頻繁且狀況持續，已暫時禁止訪問", ip);
                }
            }
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return sendErrorResponse(exchange, objectMapper, LimitationException.ErrorCode.USER_EXCEED_LIMIT, "當前請求過於頻繁，請稍後再試");
        }).onErrorResume(e -> {
            LogUnity.error(exchange, "無法處理請求，IP: %s, 錯誤信息: ", e, exchange.getRequest().getRemoteAddress());
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return sendErrorResponse(exchange, objectMapper, ValidationException.ErrorCode.REQUEST_IS_INVALID, "無");
        });
    }


    /**
     * 定時清理過期的快取記錄，釋放記憶體資源。
     *
     * <p>此方法由 Spring 的 {@link Scheduled} 註解驅動，根據 {@code globalProperties.requestLimiter.cleanInterval} 
     * 配置的間隔時間定期執行。清理作業包括：</p>
     *
     * <ul>
     *   <li><strong>令牌桶快取清理</strong>：移除過期或不再使用的令牌桶實例</li>
     *   <li><strong>IP 封禁快取清理</strong>：移除過期的封禁記錄和失敗計數（若啟用封禁功能）</li>
     * </ul>
     *
     * <p>定期清理確保系統不會因長期運行而累積過多的快取記錄，
     * 有效控制記憶體使用量並維持系統效能。</p>
     *
     * <p><strong>執行時機：</strong></p>
     * <ul>
     *   <li>初始延遲：等於清理間隔時間</li>
     *   <li>執行間隔：等於清理間隔時間</li>
     *   <li>執行模式：固定延遲（上次執行完成後等待指定時間再執行下次）</li>
     * </ul>
     *
     * @see CacheConcurrentHashMap#getCleanupTask()
     * @see Scheduled
     */
    @Scheduled(fixedDelayString = "#{globalProperties.requestLimiter.cleanInterval.toMillis()}", initialDelayString = "#{globalProperties.requestLimiter.cleanInterval.toMillis()}")
    public void clean() {
        ipBucketLimiterMap.getCleanupTask().run();
        if (isEnableBanIp) {
            banIpMap.getCleanupTask().run();
        }
    }


    /**
     * Spring 容器銷毀時的資源清理方法。
     *
     * <p>此方法由 {@link PreDestroy} 註解標記，在 Spring 容器關閉或重新載入時自動執行。
     * 負責完全清理所有快取容器和相關資源，確保應用程式優雅關閉。</p>
     *
     * <p><strong>清理操作包括：</strong></p>
     * <ul>
     *   <li>清空並銷毀令牌桶快取容器</li>
     *   <li>清空並銷毀 IP 封禁快取容器（若啟用）</li>
     *   <li>停止相關的背景清理任務</li>
     *   <li>釋放所有快取佔用的記憶體資源</li>
     * </ul>
     *
     * <p>呼叫此方法後，過濾器將無法正常運作，因為所有快取容器都已被銷毀。
     * 此方法主要用於應用程式關閉時的資源清理，而非運行時呼叫。</p>
     *
     * @see PreDestroy
     * @see CacheConcurrentHashMap#destroy()
     */
    @PreDestroy
    public void destroy() {
        ipBucketLimiterMap.destroy();
        if (isEnableBanIp) {
            banIpMap.destroy();
        }
    }
}