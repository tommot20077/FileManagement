package xyz.dowob.filemanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.component.strategy.CsrfTokenRepositoryStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;
import xyz.dowob.filemanagement.repostiory.JwtSecurityContextRepository;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security WebFlux 安全設定類，負責整個應用程式的安全架構設定。
 * <p>
 * 此設定類實現了基於角色的權限控制（RBAC）、JWT 認證、CSRF 保護、CORS 跨域設定等完整的安全機制。
 * 透過 WebFlux 的反應式安全過濾器鏈，提供高效能的非阻塞安全驗證。
 * <p>
 * 主要安全功能包括：
 * <ol>
 *   <li>JWT 認證機制：基於無狀態令牌的用戶身分驗證</li>
 *   <li>角色權限控制：支援 ANONYMOUS、VISITOR、USER、ADVANCED_USER、ADMIN 五種角色</li>
 *   <li>CSRF 保護：防止跨站請求偽造攻擊，支援 Redis 和本地兩種存儲策略</li>
 *   <li>CORS 跨域設定：靈活的跨域資源共享設定</li>
 *   <li>安全標頭：HSTS、CSP 等 HTTP 安全標頭自動添加</li>
 *   <li>請求追蹤：為每個請求分配唯一 ID，便於日志追蹤和問題排查</li>
 * </ol>
 * <p>
 * 過濾器鏈順序：
 * <ol>
 *   <li>Trace ID Filter - 請求追蹤識別</li>
 *   <li>CSRF Validation Filter - CSRF 令牌驗證</li>
 *   <li>Context Web Filter - 請求上下文設定</li>
 *   <li>User Info Filter - 用戶資訊提取</li>
 * </ol>
 * <p>
 * 安全策略特點：
 * - 預設安全：所有端點預設需要認證
 * - 路徑級權限：基於路徑模式的細粒度權限控制
 * - 方法級權限：支援 HTTP 方法級別的權限限制
 * - 異常處理：統一的認證和授權異常處理機制
 * <p>
 * 此類實現 {@link ResponseUnity} 介面，提供統一的錯誤回應處理功能。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig implements ResponseUnity {
    /**
     * JWT 安全上下文儲存庫實例。
     * <p>
     * 負責從 HTTP 請求中提取和驗證 JWT 令牌，將驗證結果轉換為 Spring Security 的安全上下文。
     * 支援從 Cookie 和 Authorization 標頭中提取令牌。
     */
    private final JwtSecurityContextRepository securityContextRepository;

    /**
     * Jackson 物件映射器實例。
     * <p>
     * 用於安全設定中的 JSON 序列化和反序列化操作，特別是錯誤回應的 JSON 格式化。
     */
    private final ObjectMapper mapper;

    /**
     * 安全設定屬性實例。
     * <p>
     * 包含所有安全相關的設定參數，如 CORS 設定、CSRF 設定、HSTS 設定、路徑權限規則等。
     * 透過此實例動態調整安全策略。
     */
    private final SecurityProperties securityProperties;

    /**
     * CSRF 令牌儲存庫策略實例。
     * <p>
     * 提供 CSRF 令牌的生成、儲存、驗證和刪除功能。支援 Redis 和本地記憶體兩種儲存策略，
     * 可根據系統需求動態選擇合適的實現方式。
     */
    private final CsrfTokenRepositoryStrategy csrfTokenRepositoryStrategy;

    /**
     * 安全的 HTTP 方法列表。
     * <p>
     * 這些 HTTP 方法被認為是安全的（不會修改伺服器狀態），因此不需要進行 CSRF 令牌驗證。
     * 包括 GET、HEAD、OPTIONS、TRACE 方法。
     */
    private final List<HttpMethod> PASS_METHODS = List.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);

    /**
     * 建構安全設定實例。
     * <p>
     * 透過依賴注入接收所需的安全組件，包括 JWT 上下文儲存庫、JSON 映射器、
     * 安全設定屬性和 CSRF 令牌策略等核心安全組件。
     *
     * @param securityContextRepository   JWT 安全上下文儲存庫，負責令牌驗證和安全上下文建立
     * @param mapper                      Jackson 物件映射器，用於錯誤回應的 JSON 序列化
     * @param securityProperties          安全設定屬性，包含所有安全相關的設定參數
     * @param csrfTokenRepositoryStrategy CSRF 令牌儲存庫策略，提供 CSRF 保護功能
     */
    public SecurityConfig(JwtSecurityContextRepository securityContextRepository, ObjectMapper mapper, SecurityProperties securityProperties, CsrfTokenRepositoryStrategy csrfTokenRepositoryStrategy) {
        this.securityContextRepository = securityContextRepository;
        this.mapper = mapper;
        this.securityProperties = securityProperties;
        this.csrfTokenRepositoryStrategy = csrfTokenRepositoryStrategy;
    }


    /**
     * 建構和設定 WebFlux 安全過濾器鏈，定義完整的請求安全處理流程。
     * <p>
     * 此方法建立了分層的安全架構，每一層都有特定的安全職責：
     * <ol>
     *   <li>CORS 跨域控制：{@link #corsConfigurationSource()} 提供靈活的跨域資源共享設定</li>
     *   <li>CSRF 保護機制：停用預設 CSRF 並使用自訂的 {@link #csrfValidationFilter()} 驗證器</li>
     *   <li>HTTP 安全標頭：自動添加 HSTS 和 CSP 標頭，強化傳輸層安全性</li>
     *   <li>認證與授權：使用 {@link #securityContextRepository} 進行 JWT 令牌驗證</li>
     *   <li>路徑權限控制：{@link #configurePathSecurity} 實現基於角色的路徑存取控制</li>
     * </ol>
     * <p>
     * 過濾器執行順序（按優先級）：
     * <ul>
     * <li>{@link #traceIdFilter()}: 請求追蹤 ID 生成（HTTP_HEADERS_WRITER 之前）</li>
     * <li>{@link #csrfValidationFilter()}: CSRF 令牌驗證（CSRF 位置）</li>
     * <li>{@link #contextWebFilter()}: 請求上下文設定（AUTHENTICATION 之前）</li>
     * <li>{@link #userInfoFilter()}: 用戶資訊提取（AUTHENTICATION 之後）</li>
     * </ul>
     * <p>
     * 異常處理策略：
     * <ul>
     * <li>認證失敗：回傳 401 Unauthorized</li>
     * <li>授權失敗：回傳 403 Forbidden</li>
     * <li>統一錯誤格式：使用 {@link ResponseUnity} 提供一致的錯誤回應</li>
     * </ul>
     * <p>
     * 安全特性：
     * - 停用表單登入，純 API 架構
     * - HSTS 強制 HTTPS（可設定子網域包含）
     * - CSP 內容安全政策防止 XSS 攻擊
     * - 細粒度的路徑權限控制
     *
     * @param http Spring WebFlux 安全設定構建器
     * @return 完整設定的安全過濾器鏈
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .headers(headerSpec -> headerSpec
                        .hsts(hsts -> hsts
                                .includeSubdomains(securityProperties.getHsts().isIncludeSubDomains())
                                .maxAge(securityProperties.getHsts().getMaxAge()))
                        .contentSecurityPolicy(contentSecurityPolicySpec -> {
                            contentSecurityPolicySpec.policyDirectives("default-src 'self'; script-src 'self'");
                        }))
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(this::configurePathSecurity)
                .securityContextRepository(securityContextRepository)
                .addFilterBefore(traceIdFilter(), SecurityWebFiltersOrder.HTTP_HEADERS_WRITER)
                .addFilterAt(csrfValidationFilter(), SecurityWebFiltersOrder.CSRF)
                .addFilterBefore(contextWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAfter(userInfoFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                        .authenticationEntryPoint((exchange, e) -> sendErrorResponse(exchange, mapper, ValidationException.ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((exchange, e) -> sendErrorResponse(exchange, mapper, ValidationException.ErrorCode.FORBIDDEN)))
                .build();
    }

    /**
     * 設定跨域資源共享（CORS）設定，控制瀏覽器的跨域請求行為。
     * <p>
     * 此方法基於 {@link SecurityProperties} 的 CORS 設定建立完整的跨域策略：
     * <p>
     * <ol>
     *   <li>允許來源設定：支援精確網域和模式匹配兩種方式</li>
     *   <li>HTTP 方法控制：可指定允許的 HTTP 方法（GET、POST、PUT、DELETE 等）</li>
     *   <li>請求標頭管理：控制客戶端可以發送的自訂標頭</li>
     *   <li>回應標頭公開：指定哪些回應標頭可以被客戶端 JavaScript 存取</li>
     *   <li>憑證支援：控制是否允許跨域請求攜帶 Cookie 和認證資訊</li>
     * </ol>
     * <p>
     * 自動公開的安全標頭：
     * - Content-Disposition：檔案下載時的檔名資訊
     * - Authorization：JWT 令牌更新資訊
     * - X-Csrf-Token：CSRF 保護令牌
     * <p>
     * 安全考量：
     * - 允許憑證時不可使用萬用字元來源（*）
     * - 預檢請求快取時間控制，平衡效能與安全
     * - 嚴格的來源驗證，防止未授權的跨域存取
     *
     * @return 完整設定的 CORS 來源設定
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        for (String allowedOrigin : securityProperties.getCors().getAllowedOrigins()) {
            configuration.addAllowedOrigin(allowedOrigin);
        }

        for (String allowedOriginPattern : securityProperties.getCors().getAllowedOriginsPattern()) {
            configuration.addAllowedOriginPattern(allowedOriginPattern);
        }

        for (String allowedMethod : securityProperties.getCors().getAllowedMethods()) {
            configuration.addAllowedMethod(allowedMethod);
        }

        for (String allowedHeader : securityProperties.getCors().getAllowedHeaders()) {
            configuration.addAllowedHeader(allowedHeader);
        }

        for (String exposedHeader : securityProperties.getCors().getAllowExposedHeaders()) {
            configuration.addExposedHeader(exposedHeader);
        }
        configuration.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION, HttpHeaders.AUTHORIZATION, "X-Csrf-Token"));

        configuration.setAllowCredentials(securityProperties.getCors().isAllowCredentials());

        configuration.setMaxAge(securityProperties.getCors().getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 設定基於路徑的安全權限控制，實現細粒度的API存取管理。
     * <p>
     * 此方法根據 {@link SecurityProperties#getPaths()} 的規則設定，
     * 為不同的 API 路徑設定相應的角色權限要求。支援路徑模式匹配和 HTTP 方法限制。
     * <p>
     * 角色層級結構（由低到高）：
     * - ANONYMOUS：匿名用戶，無需認證
     * - VISITOR：訪客用戶，基本認證但功能受限
     * - USER：一般用戶，完整的基本功能
     * - ADVANCED_USER：進階用戶，額外的進階功能
     * - ADMIN：管理員，完整的系統管理權限
     * <p>
     * 權限繼承原則：
     * 高權限角色自動包含低權限角色的所有存取權限，例如 ADMIN 可以存取所有 USER 級別的 API。
     * <p>
     * 路徑設定特性：
     * - 支援 Ant 路徑模式（/api/v1/user/**）
     * - 支援 HTTP 方法限制（GET、POST 等）
     * - 支援動態路徑參數（/api/v1/files/*）
     * - 預設拒絕未匹配的路徑
     *
     * @param exchange WebFlux 授權交換規格，用於設定路徑安全規則
     */
    private void configurePathSecurity(ServerHttpSecurity.AuthorizeExchangeSpec exchange) {
        Collection<SecurityProperties.Paths.PathRuleConfig> rules = securityProperties.getPaths().getEffectiveRules();

        applyRulesForRole(exchange, rules, RoleEnum.ANONYMOUS);
        applyRulesForRole(exchange, rules, RoleEnum.VISITOR);
        applyRulesForRole(exchange, rules, RoleEnum.USER);
        applyRulesForRole(exchange, rules, RoleEnum.ADVANCED_USER);
        applyRulesForRole(exchange, rules, RoleEnum.ADMIN);

        exchange.anyExchange().hasAnyAuthority(RoleEnum.USER.name(), RoleEnum.ADVANCED_USER.name(), RoleEnum.ADMIN.name());
    }

    /**
     * 建立請求追蹤 ID 過濾器，為每個 HTTP 請求分配唯一識別符。
     * <p>
     * 此過濾器在安全過濾器鏈的最早期執行，確保每個請求都有唯一的追蹤 ID。
     * 追蹤 ID 主要用於：
     * <ol>
     *   <li>日誌關聯：將同一請求的所有日誌條目關聯起來</li>
     *   <li>錯誤追蹤：快速定位特定請求的錯誤資訊</li>
     *   <li>效能監控：追蹤單一請求的完整處理時間</li>
     *   <li>問題排查：協助開發人員快速定位問題請求</li>
     * </ol>
     * <p>
     * ID 生成特性：
     * - 使用 UUID 確保全域唯一性
     * - 存儲在請求屬性中，整個請求生命週期可用
     * - 冪等性：同一請求多次呼叫回傳相同 ID
     * - 執行緒安全：每個請求都有獨立的 ID
     *
     * @return 設定完成的請求追蹤 ID 過濾器
     */
    @Bean
    public WebFilter traceIdFilter() {
        return (exchange, chain) -> {
            String traceId = exchange.getAttribute("requestId");
            if (traceId == null) {
                traceId = UUID.randomUUID().toString();
                exchange.getAttributes().put("requestId", traceId);
            }
            return chain.filter(exchange);
        };
    }

    /**
     * 建立 CSRF 令牌驗證過濾器，提供跨站請求偽造攻擊防護。
     * <p>
     * 此過濾器實現了智慧型的 CSRF 保護策略，根據請求特性動態決定是否需要驗證：
     * <p>
     * 驗證策略：
     * <ol>
     *   <li>路徑判斷：僅對 `/web` 開頭的路徑進行 CSRF 驗證</li>
     *   <li>方法過濾：安全方法（GET、HEAD、OPTIONS、TRACE）自動跳過驗證</li>
     *   <li>特殊路徑保護：`/api/v1/guest/csrf/token` 需要 Referer 驗證</li>
     *   <li>令牌驗證：使用 {@link CsrfTokenRepositoryStrategy} 進行令牌驗證</li>
     * </ol>
     * <p>
     * 安全特性：
     * <ol>
     *   <li>Referer 檢查：CSRF 令牌獲取端點需要合法的 Referer</li>
     *   <li>一次性令牌：驗證成功後立即刪除令牌，防止重複使用</li>
     *   <li>非同步刪除：使用 boundedElastic 調度器避免阻塞主執行緒</li>
     *   <li>異常處理：驗證失敗時回傳統一的錯誤格式</li>
     * </ol>
     * <p>
     * 錯誤處理：
     * - Referer 不合法：CSRF_TOKEN_INVALID_REFERER
     * - 令牌驗證失敗：相應的 ValidationException 錯誤碼
     * - 統一使用 {@link ResponseUnity} 格式化錯誤回應
     *
     * @return 設定完成的 CSRF 驗證過濾器
     */
    @Bean
    public WebFilter csrfValidationFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().toString();
            if (!path.startsWith("/web") || isSafeMethod(exchange)) {
                if (path.equals("/api/v1/guest/csrf/token")) {
                    List<String> refererHeaders = exchange.getRequest().getHeaders().get("Referer");
                    if (refererHeaders == null || refererHeaders.isEmpty() || refererHeaders
                            .stream()
                            .noneMatch(referer -> referer.matches(securityProperties.getCsrf().getAllowRefererPatten()))) {
                        return sendErrorResponse(exchange, mapper, ValidationException.ErrorCode.CSRF_TOKEN_INVALID_REFERER);
                    }
                }
                return chain.filter(exchange);
            }

            return csrfTokenRepositoryStrategy
                    .getCsrfTokenRepository()
                    .loadToken(exchange)
                    .flatMap(token -> chain.filter(exchange).doFinally(signalType -> {
                        csrfTokenRepositoryStrategy.getCsrfTokenRepository().deleteToken(token).subscribeOn(Schedulers.boundedElastic()).subscribe();
                    }))
                    .onErrorResume(ValidationException.class, e -> sendErrorResponse(exchange, mapper, e.getErrorCode()));
        };
    }

    /**
     * 建立請求上下文過濾器，設定反應式請求處理的上下文環境。
     * <p>
     * 此過濾器使用 {@link CustomRequestContextHolder} 將當前的 {@link ServerWebExchange}
     * 綁定到反應式上下文中，使得在整個請求處理鏈中都能存取到請求資訊。
     * <p>
     * 上下文功能：
     * <p>
     * 1. 請求資訊存取：在任何地方都能獲取當前請求的 ServerWebExchange
     * <p>
     * 2. 執行緒安全：基於 Reactor Context，確保多執行緒環境下的資料隔離
     * <p>
     * 3. 透明傳播：上下文會自動傳播到所有下游的反應式操作
     * <p>
     * 4. 記憶體效率：使用 Reactor 的不可變上下文，避免記憶體洩漏
     * <p>
     * 使用場景：
     * - 在 Service 層獲取當前請求資訊
     * - 在日誌中記錄請求相關資訊
     * - 在異常處理中存取請求詳情
     * - 在權限檢查中獲取請求路徑
     *
     * @return 設定完成的請求上下文過濾器
     */
    @Bean
    public WebFilter contextWebFilter() {
        return (exchange, chain) -> chain.filter(exchange).contextWrite(CustomRequestContextHolder.mutate(exchange));
    }

    /**
     * 建立用戶資訊提取過濾器，從安全上下文中提取並快取用戶識別資訊。
     * <p>
     * 此過濾器在認證完成後執行，負責從 Spring Security 的 {@link SecurityContext}
     * 中提取用戶資訊並將其儲存在請求屬性中，便於後續處理使用。
     * <p>
     * 處理流程：
     * <p>
     * 1. 獲取安全上下文：從 {@link ReactiveSecurityContextHolder} 中獲取當前用戶的認證資訊
     * <p>
     * 2. 認證狀態檢查：確認用戶已通過認證且認證物件有效
     * <p>
     * 3. 用戶 ID 提取：從認證物件的 principal 中提取用戶 ID（Long 型別）
     * <p>
     * 4. 屬性儲存：將用戶 ID 以字串形式儲存在請求屬性 "userId" 中
     * <p>
     * 使用優勢：
     * - 效能優化：避免在每次需要用戶 ID 時重複解析 JWT
     * - 簡化存取：後續元件可直接從請求屬性中獲取用戶 ID
     * - 類型安全：確保用戶 ID 為 Long 型別，避免類型轉換錯誤
     * - 延遲載入：僅在認證成功時才進行資訊提取
     *
     * @return 設定完成的用戶資訊過濾器
     */
    @Bean
    public WebFilter userInfoFilter() {
        return ((exchange, chain) -> Mono
                .defer(() -> ReactiveSecurityContextHolder
                        .getContext()
                        .map(SecurityContext::getAuthentication)
                        .filter(auth -> auth != null && auth.isAuthenticated())
                        .map(auth -> (Long) auth.getPrincipal())
                        .flatMap(userId -> {
                            exchange.getAttributes().put("userId", userId.toString());
                            return Mono.empty();
                        }))
                .then(chain.filter(exchange)));
    }

    /**
     * 為特定角色設定路徑存取權限，實現基於角色的存取控制（RBAC）。
     * <p>
     * 此方法從所有路徑規則中篩選出指定角色的規則，並將其應用到安全設定中。
     * 實現了權限繼承機制，高權限角色自動擁有低權限角色的所有存取權限。
     * <p>
     * 處理邏輯：
     * <p>
     * 1. 規則篩選：從全部規則中篩選出符合指定角色的路徑規則
     * <p>
     * 2. 路徑匹配：支援精確路徑和 Ant 模式路徑匹配
     * <p>
     * 3. 方法限制：可選的 HTTP 方法限制，未指定時允許所有方法
     * <p>
     * 4. 權限設定：根據角色層級設定相應的存取權限
     * <p>
     * 角色權限映射：
     * - ANONYMOUS：permitAll() - 允許所有用戶（包括未認證）
     * - VISITOR：hasAnyAuthority(VISITOR, USER, ADVANCED_USER, ADMIN)
     * - USER：hasAnyAuthority(USER, ADVANCED_USER, ADMIN)
     * - ADVANCED_USER：hasAnyAuthority(ADVANCED_USER, ADMIN)
     * - ADMIN：hasAuthority(ADMIN)
     * - 未知角色：denyAll() - 拒絕所有存取
     * <p>
     * 日誌記錄：
     * 每個路徑規則的設定都會記錄 TRACE 級別的日誌，便於除錯和審計。
     *
     * @param exchange WebFlux 授權交換規格，用於設定路徑安全規則
     * @param allRules 完整的路徑規則集合
     * @param role     要設定的目標角色
     */
    private void applyRulesForRole(ServerHttpSecurity.AuthorizeExchangeSpec exchange, Collection<SecurityProperties.Paths.PathRuleConfig> allRules, RoleEnum role) {
        List<SecurityProperties.Paths.PathRuleConfig> roleSpecificRules = allRules.stream().filter(rule -> role.equals(rule.getRole())).toList();

        for (SecurityProperties.Paths.PathRuleConfig rule : roleSpecificRules) {
            ServerHttpSecurity.AuthorizeExchangeSpec.Access spec;
            if (rule.getMethod() != null) {
                spec = exchange.pathMatchers(rule.getMethod(), rule.getPattern());
            } else {
                spec = exchange.pathMatchers(rule.getPattern());
            }

            switch (role) {
                case RoleEnum.ANONYMOUS:
                    LogUnity.trace("設定的路徑: " + rule.getPattern() + "，將允許匿名訪問");
                    spec.permitAll();
                    break;
                case RoleEnum.VISITOR:
                    LogUnity.trace("設定的路徑: " + rule.getPattern() + "，將允許訪客以上訪問");
                    spec.hasAnyAuthority(RoleEnum.VISITOR.name(), RoleEnum.USER.name(), RoleEnum.ADVANCED_USER.name(), RoleEnum.ADMIN.name());
                    break;
                case RoleEnum.USER:
                    LogUnity.trace("設定的路徑: " + rule.getPattern() + "，將允許用戶以上訪問");
                    spec.hasAnyAuthority(RoleEnum.USER.name(), RoleEnum.ADVANCED_USER.name(), RoleEnum.ADMIN.name());
                    break;
                case RoleEnum.ADVANCED_USER:
                    LogUnity.trace("設定的路徑: " + rule.getPattern() + "，將允許高級用戶以上訪問");
                    spec.hasAnyAuthority(RoleEnum.ADVANCED_USER.name(), RoleEnum.ADMIN.name());
                    break;
                case RoleEnum.ADMIN:
                    LogUnity.trace("設定的路徑: " + rule.getPattern() + "，將允許管理員訪問");
                    spec.hasAuthority(RoleEnum.ADMIN.name());
                    break;
                default:
                    LogUnity.warn("未知的角色: " + rule.getRole() + " 設定的路徑: " + rule.getPattern() + "，將自動拒絕訪問");
                    spec.denyAll();
                    break;
            }
        }
    }

    /**
     * 判斷 HTTP 請求是否使用安全方法，安全方法無需 CSRF 令牌驗證。
     * <p>
     * 根據 RFC 7231 規範，安全方法是指不會對伺服器狀態產生副作用的 HTTP 方法。
     * 這些方法被認為是「唯讀」操作，因此不存在 CSRF 攻擊的風險。
     * <p>
     * 安全方法定義：
     * - GET：獲取資源，不修改伺服器狀態
     * - HEAD：獲取資源標頭，不回傳響應體
     * - OPTIONS：查詢伺服器支援的方法和選項
     * - TRACE：回應請求內容，用於診斷
     * <p>
     * CSRF 防護原理：
     * CSRF 攻擊主要針對會修改伺服器狀態的操作（如 POST、PUT、DELETE），
     * 而安全方法由於不會產生副作用，因此可以安全地跳過 CSRF 驗證。
     * <p>
     * 注意事項：
     * 雖然 GET 等方法在語義上是安全的，但開發者應確保實際實現也遵循這一原則，
     * 不在 GET 請求中執行資料修改操作。
     *
     * @param exchange 伺服器 Web 交換物件，包含 HTTP 請求資訊
     * @return {@code true} 如果是安全方法，{@code false} 否則
     */
    public boolean isSafeMethod(ServerWebExchange exchange) {
        return PASS_METHODS.contains(exchange.getRequest().getMethod());
    }

    /**
     * 設定密碼加密器，提供安全的密碼雜湊功能。
     * <p>
     * 使用 BCrypt 演算法進行密碼加密，這是目前業界推薦的密碼雜湊標準。
     * BCrypt 具有以下安全特性：
     * <p>
     * 1. 自適應成本：計算成本可隨硬體效能調整，抵禦暴力破解
     * <p>
     * 2. 內建鹽值：每次加密都會產生隨機鹽值，防止彩虹表攻擊
     * <p>
     * 3. 時間恆定：驗證時間相對恆定，防止時序攻擊
     * <p>
     * 4. 向後相容：支援不同版本的 BCrypt 格式
     * <p>
     * 使用場景：
     * - 用戶註冊時的密碼加密
     * - 用戶登入時的密碼驗證
     * - 密碼重設功能中的新密碼加密
     * - 管理員重設用戶密碼
     * <p>
     * 安全建議：
     * - 不要在日誌中記錄原始密碼
     * - 定期提醒用戶更新密碼
     * - 實施密碼複雜度要求
     *
     * @return BCrypt 密碼加密器實例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}