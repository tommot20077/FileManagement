package xyz.dowob.filemanagement.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.repostiory.JwtSecurityContextRepository;

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
@RequiredArgsConstructor
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
     * 配置安全過濾器鏈
     *
     * @param http ServerHttpSecurity 用於配置安全過濾器鏈的類
     *
     * @return 返回配置好的安全過濾器鏈
     */

    // todo 補上HSTS
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrfSpec -> csrfSpec
                        .csrfTokenRepository(webSessionServerCsrfTokenRepository())
                        .requireCsrfProtectionMatcher(exchange -> ServerWebExchangeMatchers.pathMatchers("/web/**").matches(exchange)))
                .headers(headers -> headers.contentSecurityPolicy(contentSecurityPolicySpec -> {
                    contentSecurityPolicySpec.policyDirectives("default-src 'self'; script-src 'self'");
                }))

                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/web/guest/**", "/api/guest/**", "/docs/**")
                        .permitAll()
                        .pathMatchers("/api/user/getAllUserInfo")
                        .hasRole("ADMIN")
                        .anyExchange()
                        .authenticated())
                .securityContextRepository(securityContextRepository)
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                        .authenticationEntryPoint((exchange, e) -> writeJsonResponse(exchange, "請先登入", HttpStatus.UNAUTHORIZED.value()))
                        .accessDeniedHandler((exchange, e) -> writeJsonResponse(exchange, "權限不足", HttpStatus.FORBIDDEN.value())))
                .build();
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
     * 配置 CSRF Token Repository
     *
     * @return CSRF憑證庫
     */
    @Bean
    public ServerCsrfTokenRepository webSessionServerCsrfTokenRepository() {
        WebSessionServerCsrfTokenRepository csrfTokenRepository = new WebSessionServerCsrfTokenRepository();
        csrfTokenRepository.setHeaderName("X-CSRF-TOKEN");
        return csrfTokenRepository;
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
        configuration.addAllowedOriginPattern("http://*localhost:*");
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 將自定義的 ApiResponseDTO 轉換為 JSON 格式的響應消息
     *
     * @param exchange   請求交換對象
     * @param message    響應消息
     * @param statusCode 狀態碼
     *
     * @return Mono<Void>
     */
    private Mono<Void> writeJsonResponse(ServerWebExchange exchange, String message, int statusCode) {
        try {
            ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                                       statusCode,
                                                                       exchange.getRequest().getPath().value(),
                                                                       message,
                                                                       null);
            return exchange
                    .getResponse()
                    .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(apiResponseDTO))));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }
}