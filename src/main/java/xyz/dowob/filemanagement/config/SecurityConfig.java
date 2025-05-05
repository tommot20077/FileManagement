package xyz.dowob.filemanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;
import xyz.dowob.filemanagement.repostiory.JwtSecurityContextRepository;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.List;
import java.util.UUID;

/**
 * 安全配置類，用於配置安全相關的設置。
 * 用於管理用戶權限、預授權以及請求安全設定
 *
 * @author yuan
 * @program FileManagement
 * @ClassName SecurityConfig
 * @description
 * @create 2024-09-23 14:28
 * @Version 1.0
 **/

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig implements ResponseUnity {
    /**
     * JwtSecurityContextRepository 用於操作安全上下文的數據庫操作類
     */
    private final JwtSecurityContextRepository securityContextRepository;

    /**
     * ObjectMapper 用於對象與 JSON 之間的轉換
     */
    private final ObjectMapper mapper;

    /**
     * SecurityProperties 用於配置安全相關的參數
     */
    private final SecurityProperties securityProperties;

    /**
     * CSRF Token 儲存庫策略
     */
    private final CsrfTokenRepositoryStrategy csrfTokenRepositoryStrategy;


    /**
     * 不需要驗證CSRF的方法
     */
    private final List<HttpMethod> PASS_METHODS = List.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);


    /**
     * SecurityConfig 的構造函數
     *
     * @param securityContextRepository JwtSecurityContextRepository 用於操作安全上下文的數據庫操作類
     * @param mapper                    ObjectMapper 用於對象與 JSON 之間的轉換
     * @param securityProperties        SecurityProperties 用於配置安全相關的參數
     * @param csrfTokenRepositoryStrategy CSRF Token 儲存庫策略
     */
    public SecurityConfig(JwtSecurityContextRepository securityContextRepository, ObjectMapper mapper, SecurityProperties securityProperties, CsrfTokenRepositoryStrategy csrfTokenRepositoryStrategy) {
        this.securityContextRepository = securityContextRepository;
        this.mapper = mapper;
        this.securityProperties = securityProperties;
        this.csrfTokenRepositoryStrategy = csrfTokenRepositoryStrategy;
    }


    /**
     * 配置安全過濾器鏈
     *
     * @param http ServerHttpSecurity 用於配置安全過濾器鏈的類
     *
     * @return 返回配置好的安全過濾器鏈
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
                .authorizeExchange(pathSecurity())
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
     * 配置安全過濾器鏈，此過濾器會依照自定義的{@link CustomRequestContextHolder} 進行上下文的設置
     *
     * @return SecurityWebFilterChain 安全過濾器鏈
     */
    @Bean
    public WebFilter contextWebFilter() {
        return (exchange, chain) -> chain.filter(exchange).contextWrite(CustomRequestContextHolder.mutate(exchange));
    }


    /**
     * 密碼加密處理的 Bean
     *
     * @return PasswordEncoder BCrypt算法加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * 生成唯一的請求 ID
     * 用於追蹤請求的唯一標識符
     *
     * @return WebFilter 請求 ID 過濾器
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
     * 用戶信息過濾器
     * 用於獲取用戶 ID 並將其存儲在請求屬性中
     *
     * @return WebFilter 用戶信息過濾器
     */
    @Bean
    public WebFilter userInfoFilter() {
        return ((exchange, chain) -> Mono
                .defer(() -> ReactiveSecurityContextHolder
                        .getContext()
                        .map(SecurityContext::getAuthentication)
                        .map(auth -> (Long) auth.getPrincipal())
                        .flatMap(userId -> {
                            exchange.getAttributes().put("userId", userId.toString());
                            return Mono.empty();
                        }))
                .then(chain.filter(exchange)));
    }


    /**
     * 配置 CORS
     * 配置允許跨域的來源、方法、是否允許携帶憑證
     *
     * @return 跨域配置
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
     * 配置 CSRF Token 驗證過濾器，用於驗證 CSRF Token 的合法性
     * 當請求的路徑不是以 /web 開頭或是安全方法時，不進行驗證 CSRF Token 安全方法請參考 {@link #isSafeMethod(ServerWebExchange)}
     * 當請求的路徑為 /api/v1/guest/csrf/token 時，會檢查 Referer 是否合法，因為 CSRF TOKEN 需要進行保護限制
     * 當驗證失敗時，返回錯誤信息，並設置 HTTP 狀態碼為 403
     * 當驗證成功，會刪除Redis中的Token
     *
     * @return WebFilter CSRF Token 驗證過濾器
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
     * 判斷是否為安全方法，安全方法不進行 CSRF Token 驗證
     * 安全方法包括 GET、HEAD、OPTIONS、TRACE 方法，定義在 {@link #PASS_METHODS}
     *
     * @param exchange 伺服器 Web 交換對象
     *
     * @return 是否為安全方法
     */
    public boolean isSafeMethod(ServerWebExchange exchange) {
        return PASS_METHODS.contains(exchange.getRequest().getMethod());
    }


    /**
     * 獲取允許訪客訪問的路徑
     *
     * @return 允許訪問的路徑
     */
    private String[] allowGuestPath() {
        return new String[]{"/docs/**", "/api/v1/user/info", "/web/v1/user/info", "/api/v1/folders/*", "/web/v1/folders/*", "/api/v1/folders/*/download", "/web/v1/folders/*/download", "/api/v1/files/*", "/web/v1/files/*", "/api/v1/files/*/info", "/web/v1/files/*/info", "/api/v1/docs/*", "/web/v1/docs/*", "/api/v1/docs/history/*", "/web/v1/docs/history/*",};
    }


    /**
     * 獲取禁止訪客訪問的路徑
     *
     * @return 禁止訪問的路徑
     */
    private String[] denyGuestPath() {
        return new String[]{"/api/v1/folders/star", "/web/v1/folders/star", "/api/v1/folders/recently", "/web/v1/folders/recently", "/api/v1/folders/recycle", "/web/v1/folders/recycle", "/api/v1/folders/shared", "/web/v1/folders/shared", "/api/v1/folders/all", "/web/v1/folders/all", "/api/v1/folders/path/*", "/web/v1/folders/path/*", "/api/v1/files/user-file-list", "/web/v1/files/user-file-list", "/api/v1/files/search", "/web/v1/files/search",};
    }


    /**
     * 獲取訪問權限的路徑
     *
     * @return 訪問權限的路徑
     */
    private Customizer<ServerHttpSecurity.AuthorizeExchangeSpec> pathSecurity() {
        return exchange -> exchange
                .pathMatchers(denyGuestPath())
                .hasAnyAuthority("USER", "ADVANCE_USER", "ADMIN")
                .pathMatchers(HttpMethod.GET, allowGuestPath())
                .permitAll()
                .pathMatchers("/api/v1/guest/**", "/web/v1/guest/**", "/actuator/health")
                .permitAll()
                .pathMatchers("/actuator/**")
                .hasAnyAuthority("ADMIN")
                .anyExchange()
                .hasAnyAuthority("USER", "ADVANCE_USER", "ADMIN");
    }
}