package xyz.dowob.filemanagement.component.filter;

import com.google.common.net.InetAddresses;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.Optional;

/**
 * 基於反應式模式的客戶端 IP 檢測過濾器，優先順序為最高。
 *
 * <p>本過濾器自動檢測並提取客戶端真實 IP 地址，透過多層次的請求標頭檢測機制確保準確性。
 * 檢測優先序為：X-Real-IP → CF-Connecting-IP → X-Forwarded-For → RemoteAddress。
 * 偵測到的 IP 地址會存儲於請求屬性中供後續過濾器和處理器使用。</p>
 *
 * <p>支援常見的代理伺服器和 CDN 服務，包括 Cloudflare、Nginx 等反向代理的 IP 轉發機制。
 * 採用非阻塞反應式處理模式，確保高併發環境下的處理效能。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Order(1)
@Component
public class ClientIpFilter implements WebFilter, ResponseUnity {

    /**
     * 客戶端IP地址的請求屬性名稱
     * 用於在請求屬性中存儲客戶端的IP地址
     */
    private static final String CLIENT_IP_ATTRIBUTE = "clientIp";

    /**
     * Cloudflare的轉發IP頭名稱
     * 用於獲取Cloudflare原始用戶的IP地址
     */
    private static final String CF_CONNECTING_IP = "CF-Connecting-IP";

    /**
     * 真實IP的請求頭名稱
     */
    private final String X_REAL_IP;


    /**
     * 請求頭中用於獲取轉發IP的名稱
     */
    private final String X_FORWARDED_FOR;

    /**
     * 建構方法，初始化客戶端 IP 過濾器的設定參數。
     *
     * @param globalProperties 全域組態屬性，包含轉發標頭設定
     * @throws IllegalArgumentException 當必要的標頭設定為空時
     */
    public ClientIpFilter(GlobalProperties globalProperties) {
        GlobalProperties.forwarded forwarded = globalProperties.getForwarded();
        Assert.notNull(forwarded.getXForwardedHeader(), "請求IP轉發頭不能為空");
        Assert.notNull(forwarded.getXRealIpHeader(), "真實IP頭不能為空");

        this.X_FORWARDED_FOR = forwarded.getXForwardedHeader();
        this.X_REAL_IP = forwarded.getXRealIpHeader();

    }

    /**
     * 從請求交換物件中提取客戶端 IP 地址。
     *
     * @param exchange 伺服器網頁交換物件，可為 null
     * @return 包含客戶端 IP 地址的 Optional，若無法取得則為空
     */
    public static Optional<String> getClientIpFromExchange(@Nullable ServerWebExchange exchange) {
        if (exchange != null) {
            return Optional.ofNullable((String) exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
        }
        return CustomRequestContextHolder.getExchange().map(e -> Optional.ofNullable((String) e.getAttributes().get(CLIENT_IP_ATTRIBUTE))).blockOptional().orElse(Optional.empty());
    }

    /**
     * 過濾器核心方法，檢測並儲存客戶端 IP 地址至請求屬性。
     *
     * @param exchange 伺服器網頁交換物件
     * @param chain    過濾器鏈物件
     * @return 表示過濾操作完成的 Mono
     */
    @Nonnull
    @Override
    public Mono<Void> filter(@Nonnull ServerWebExchange exchange, @Nonnull WebFilterChain chain) {
        LogUnity.trace(exchange, "獲取請求的客戶端IP地址，傳入標頭: {}", exchange.getRequest().getHeaders().toString());
        String clientIp = getClientIp(exchange);
        exchange.getAttributes().put(CLIENT_IP_ATTRIBUTE, clientIp);
        LogUnity.trace(exchange, "獲取請求的客戶端IP地址: {}", clientIp);
        return chain.filter(exchange);
    }

    /**
     * 依優先級順序檢測客戶端真實 IP 地址。
     * <p>
     * 檢測順序：
     * <ol>
     *   <li>X-Real-IP 標頭（Nginx 反向代理）</li>
     *   <li>CF-Connecting-IP 標頭（Cloudflare CDN）</li>
     *   <li>X-Forwarded-For 標頭的第一個 IP</li>
     *   <li>請求的遠端地址</li>
     * </ol>
     * </p>
     *
     * @param exchange 伺服器網頁交換物件
     * @return 客戶端 IP 地址，若無法取得有效 IP 則回傳 null
     */
    private String getClientIp(@NotNull ServerWebExchange exchange) {
        String xRealIp = exchange.getRequest().getHeaders().getFirst(X_REAL_IP);
        if (isValidIp(xRealIp)) {
            return xRealIp;
        }

        String cfIp = exchange.getRequest().getHeaders().getFirst(CF_CONNECTING_IP);
        if (isValidIp(cfIp)) {
            return cfIp;
        }

        String xForwardedFor = exchange.getRequest().getHeaders().getFirst(X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            String firstIp = getFirstIp(xForwardedFor);
            if (isValidIp(firstIp)) {
                return firstIp;
            }
        }

        return Optional
                .ofNullable(exchange.getRequest().getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress()).filter(this::isValidIp).orElse(null);
    }

    /**
     * 驗證 IP 地址格式的有效性。
     *
     * @param ip 待驗證的 IP 地址字串
     * @return 若為有效的 IPv4 或 IPv6 地址則回傳 true
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        try {
            InetAddresses.forString(ip);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 從 X-Forwarded-For 標頭中提取第一個 IP 地址。
     *
     * @param xForwardedFor 包含代理鏈路 IP 地址的轉發標頭值
     * @return 第一個 IP 地址，若輸入無效則回傳 null
     */
    private String getFirstIp(String xForwardedFor) {
        if (xForwardedFor == null || xForwardedFor.trim().isEmpty()) {
            return null;
        }
        String[] ips = xForwardedFor.split(",");
        return ips.length > 0 ? ips[0].trim() : null;
    }
}
