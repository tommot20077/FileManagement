package xyz.dowob.filemanagement.holder;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.Function;

/**
 * 基於 Reactor Context 的響應式 Web 請求上下文持有者實現。
 * <p>
 * 此實現利用 Reactor 的 {@link Context} 機制在響應式程式設計環境中
 * 傳遞和存取 {@link ServerWebExchange} 物件，解決傳統 ThreadLocal
 * 在非阻塞異步操作中無法正確傳遞上下文的問題。
 * <p>
 * 上下文的生命週期與響應式流綁定，當流結束時自動清理，無需手動管理。
 * 支援跨異步邊界的上下文傳遞，確保在多層服務調用中能夠存取到原始請求資訊。
 * <p>
 * 使用範例：
 * <pre>{@code
 * // 在 WebFilter 中設定上下文
 * public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
 *     return chain.filter(exchange)
 *         .contextWrite(CustomRequestContextHolder.mutate(exchange));
 * }
 * 
 * // 在 Service 層獲取請求資訊
 * public Mono<String> getCurrentUserAgent() {
 *     return CustomRequestContextHolder.getExchange()
 *         .map(exchange -> exchange.getRequest().getHeaders().getFirst("User-Agent"));
 * }
 * }</pre>
 * <p>
 * 此實現是執行緒安全的，且與 Spring WebFlux 完全相容。必須在響應式上下文中使用，
 * 上下文設定通常在 WebFilter 或 Controller 層完成。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ServerWebExchange
 * @see Context
 * @see Mono
 * @see org.springframework.web.server.WebFilter
 */

public class CustomRequestContextHolder {
    /**
     * Reactor 上下文中用於存取 ServerWebExchange 的鍵值常數。
     * <p>
     * 此常數定義了在 Reactor Context 中用於標識 ServerWebExchange 物件的唯一鍵值。
     * 透過使用固定的鍵值，確保在整個應用程式中一致地存取請求上下文。
     * </p>
     */
    private static final String CONTEXT_KEY = "SERVER_WEB_EXCHANGE";

    /**
     * 從 Reactor 上下文中獲取當前的 ServerWebExchange。
     * <p>
     * 使用延遲上下文檢索機制，在響應式流中安全地存取 Web 交換實例。
     * 如果當前上下文中不存在 ServerWebExchange，則回傳空的 Mono。
     *
     * @return 包含 {@link ServerWebExchange} 的 Mono，如果上下文中不存在則為空
     * @see Mono#deferContextual(Function)
     * @see Context#getOrEmpty(Object)
     */
    public static Mono<ServerWebExchange> getExchange() {
        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(CONTEXT_KEY)).cast(ServerWebExchange.class));
    }


    /**
     * 建立用於將 ServerWebExchange 存入 Reactor 上下文的轉換函數。
     * <p>
     * 回傳一個函數，可用於修改當前的 Reactor 上下文，將指定的 Web 交換實例
     * 插入其中。此轉換函數通常與 {@code contextWrite()} 操作結合使用。
     *
     * @param exchange 要存入上下文的 {@link ServerWebExchange} 實例
     * @return 用於修改 Reactor 上下文的轉換函數
     * @see Context#put(Object, Object)
     * @see Mono#contextWrite(Function)
     */
    public static Function<Context, Context> mutate(ServerWebExchange exchange) {
        return context -> context.put(CONTEXT_KEY, exchange);
    }
}
