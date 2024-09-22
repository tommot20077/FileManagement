package xyz.dowob.filemanagement.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.JwtSecurityContextRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

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
@Log4j2
public class SecurityConfig {
    /**
     * JwtSecurityContextRepository 用於操作安全上下文的數據庫操作類
     */
    private final JwtSecurityContextRepository securityContextRepository;

    /**
     * ObjectMapper 用於對象與 JSON 之間的轉換
     */
    private final ObjectMapper objectMapper;

    /**
     * SecurityProperties 用於配置安全相關的參數
     */
    private final SecurityProperties securityProperties;

    /**
     * WebFilter 用於處理安全上下文的過濾器
     */
    @Resource(name = "contextWebFilter")
    private WebFilter contextWebFilter;

    /**
     * WebFilter 用於處理 CSRF Token 的過濾器
     */
    @Resource(name = "csrfValidationFilter")
    private WebFilter csrfTokenResponseFilter;

    /**
     * SecurityConfig 的構造函數
     *
     * @param securityContextRepository JwtSecurityContextRepository 用於操作安全上下文的數據庫操作類
     * @param objectMapper              ObjectMapper 用於對象與 JSON 之間的轉換
     * @param securityProperties        SecurityProperties 用於配置安全相關的參數
     */
    public SecurityConfig(JwtSecurityContextRepository securityContextRepository, ObjectMapper objectMapper, SecurityProperties securityProperties) {
        this.securityContextRepository = securityContextRepository;
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
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
                                .maxAge(Duration.ofMinutes(securityProperties.getHsts().getMaxAge())))
                        .contentSecurityPolicy(contentSecurityPolicySpec -> {
                            contentSecurityPolicySpec.policyDirectives("default-src 'self'; script-src 'self'");
                        }))
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/web/v1/guest/**", "/api/v1/guest/**", "/docs/**", "/ws/**", "/actuator/health")
                        .permitAll()
                        .anyExchange()
                        .authenticated())
                .securityContextRepository(securityContextRepository)
                .addFilterAt(csrfTokenResponseFilter, SecurityWebFiltersOrder.CSRF)
                .addFilterAt(contextWebFilter, SecurityWebFiltersOrder.EXCEPTION_TRANSLATION)
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                        .authenticationEntryPoint((exchange, e) -> writeJsonResponse(exchange, ValidationException.ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((exchange, e) -> writeJsonResponse(exchange, ValidationException.ErrorCode.FORBIDDEN)))
                .build();
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
     * 將自定義的 ApiResponseDTO 轉換為 JSON 格式的響應消息
     *
     * @param exchange 請求交換對象
     * @param error    錯誤信息
     *
     * @return Mono<Void>
     */
    private Mono<Void> writeJsonResponse(ServerWebExchange exchange, ValidationException.ErrorCode error) {
        try {
            ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                                       error.getCode(),
                                                                       exchange.getRequest().getPath().value(),
                                                                       error.getMessage(),
                                                                       null
            );
            exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.getResponse().setStatusCode(error.getHttpStatus());
            return exchange
                    .getResponse()
                    .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(apiResponseDTO))));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
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
}