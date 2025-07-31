/**
 * 響應式 Web 應用程式的上下文持有者工具類套件。
 * <p>
 * 提供基於 Reactor Context 的上下文管理實現，解決傳統 ThreadLocal
 * 在非阻塞異步操作中無法正確傳遞上下文的問題。透過 Reactor 的
 * Context 機制確保上下文資訊在整個響應式流程中安全傳遞。
 * <p>
 * 核心類別：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.holder.CustomRequestContextHolder} - 響應式請求上下文持有者</li>
 * </ul>
 * <p>
 * 上下文生命週期與響應式流綁定，當流結束時自動清理，無需手動管理。
 * 支援跨異步邊界的上下文傳遞，在 Service 層可安全存取 HTTP 請求資訊。
 * <p>
 * 使用範例：
 * <pre>{@code
 * // 在 WebFilter 中設定上下文
 * @Component
 * public class RequestContextFilter implements WebFilter {
 *     @Override 
 *     public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
 *         return chain.filter(exchange)
 *             .contextWrite(CustomRequestContextHolder.mutate(exchange));
 *     }
 * }
 * 
 * // 在 Service 層使用
 * @Service
 * public class UserService {
 *     public Mono<String> getCurrentUserInfo() {
 *         return CustomRequestContextHolder.getExchange()
 *             .flatMap(this::extractUserInfo);
 *     }
 * }
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see reactor.util.context.Context
 * @see org.springframework.web.server.ServerWebExchange
 * @see org.springframework.web.server.WebFilter
 */

package xyz.dowob.filemanagement.holder;