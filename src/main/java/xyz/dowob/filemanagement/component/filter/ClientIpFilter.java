package xyz.dowob.filemanagement.component.filter;

import com.google.common.net.InetAddresses;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
 * 用戶IP過濾器，用於獲取用戶的IP地址並將其存儲在請求屬性中
 * 方便後續日誌以及限流器進行使用，因此這個過濾器的優先級設置為1
 * 此類實現了WebFilter接口，並在過濾器鏈中處理請求
 * 以及ResponseUnity接口，內部封裝一些常用的響應方法
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ClientIpFilter
 * @create 2025/4/21
 * @Version 1.0
 **/
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
     * 構造函數，初始化ClientIpFilter
     *
     * @param globalProperties 全局配置屬性
     */
    public ClientIpFilter(GlobalProperties globalProperties) {
        GlobalProperties.forwarded forwarded = globalProperties.getForwarded();
        Assert.notNull(forwarded.getXForwardedHeader(), "請求IP轉發頭不能為空");
        Assert.notNull(forwarded.getXRealIpHeader(), "真實IP頭不能為空");

        this.X_FORWARDED_FOR = forwarded.getXForwardedHeader();
        this.X_REAL_IP = forwarded.getXRealIpHeader();

    }

    /**
     * 過濾器方法，處理請求並獲取客戶端IP地址並存儲在請求屬性中
     *
     * @param exchange 請求交換對象
     * @param chain    過濾器鏈對象
     *
     * @return Mono<Void> 異步響應對象
     */
    @NotNull
    @Override
    public Mono<Void> filter(@NotNull ServerWebExchange exchange, @NotNull WebFilterChain chain) {
        LogUnity.trace(exchange, "獲取請求的客戶端IP地址，傳入標頭: {}", exchange.getRequest().getHeaders().toString());
        String clientIp = getClientIp(exchange);
        exchange.getAttributes().put(CLIENT_IP_ATTRIBUTE, clientIp);
        LogUnity.trace(exchange, "獲取請求的客戶端IP地址: {}", clientIp);
        return chain.filter(exchange);
    }


    /**
     * 獲取客戶端IP地址
     * 1. 優先使用 X-Real-IP
     * 2. 再來嘗試 CF-Connecting-IP
     * 3. 然後使用 X-Forwarded-For 的第一個IP
     * 如果以上都無效，則使用請求的遠程地址
     * 若都無效，則返回 null
     *
     * @param exchange 請求交換對象
     *
     * @return 客戶端IP地址
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
                .map(addr -> addr.getAddress().getHostAddress())
                .filter(this::isValidIp)
                .orElse(null);
    }

    /**
     * 檢查IP地址是否有效
     *
     * @param ip IP地址
     *
     * @return true 如果有效，否則 false
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
     * 獲取轉發頭第一個IP地址
     *
     * @param xForwardedFor 轉發的IP地址
     *
     * @return 第一個IP地址
     */
    private String getFirstIp(String xForwardedFor) {
        if (xForwardedFor == null || xForwardedFor.trim().isEmpty()) {
            return null;
        }
        String[] ips = xForwardedFor.split(",");
        return ips.length > 0 ? ips[0].trim() : null;
    }


    /**
     * 獲取客戶端IP地址
     * 根據請求交換對象獲取客戶端的IP地址
     *
     * @param exchange 請求交換對象
     *
     * @return 客戶端IP地址的Optional對象
     */
    public static Optional<String> getClientIpFromExchange(@Nullable ServerWebExchange exchange) {
        if (exchange != null) {
            return Optional.ofNullable((String) exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
        }
        return CustomRequestContextHolder.getExchange().map(e -> Optional.ofNullable((String) e.getAttributes().get(CLIENT_IP_ATTRIBUTE))).block();
    }
}
