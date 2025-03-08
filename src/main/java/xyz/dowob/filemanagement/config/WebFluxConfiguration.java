package xyz.dowob.filemanagement.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.component.strategy.CsrfTokenRepositoryStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebFlux 配置類，用於配置 WebFlux 相關的配置，實現 WebFluxConfigurer 接口
 *
 * @author yuan
 * @program FileManagement
 * @ClassName WebFluxConfiguration
 * @description
 * @create 2024-12-08 23:07
 * @Version 1.0
 **/
@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {
    /**
     * 文件配置文件
     */
    private final FileProperties fileProperties;

    /**
     * 安全配置文件
     */
    private final SecurityProperties securityProperties;

    /**
     * ObjectMapper 用於對象與 JSON 之間的轉換
     */
    private final ObjectMapper objectMapper;

    /**
     * 不需要驗證CSRF的方法
     */
    private final List<HttpMethod> PASS_METHODS = List.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);

    private final CsrfTokenRepositoryStrategy csrfTokenRepositoryStrategy;

    /**
     * 帶參數的構造方法
     *
     * @param fileProperties 文件配置文件
     */
    public WebFluxConfiguration(FileProperties fileProperties, SecurityProperties securityProperties, ObjectMapper objectMapper, CsrfTokenRepositoryStrategy csrfTokenRepositoryStrategy) {
        this.fileProperties = fileProperties;
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
        this.csrfTokenRepositoryStrategy = csrfTokenRepositoryStrategy;
    }

    /**
     * 配置服務器編解碼器，用於設定服務器編解碼器的最大內存大小
     *
     * @param configurer 服務器編解碼器配置器
     */
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(fileProperties.getUpload().getPayloadLength() * 1024 * 1024);
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
     * 配置 CSRF Token 驗證過濾器，用於驗證 CSRF Token 的合法性
     * 當驗證失敗時，返回錯誤信息，並設置 HTTP 狀態碼為 403
     * 當驗證成功，會刪除Redis中的Token
     *
     * @return WebFilter CSRF Token 驗證過濾器
     */
    @Bean
    public WebFilter csrfValidationFilter() {
        return (exchange, chain) -> {
            if (!exchange.getRequest().getPath().toString().startsWith("/web") || isPassMethod(exchange)) {
                return chain.filter(exchange);
            }

            return csrfTokenRepositoryStrategy
                    .getCsrfTokenRepository()
                    .loadToken(exchange)
                    .flatMap(token -> chain.filter(exchange).doFinally(signalType -> {
                        csrfTokenRepositoryStrategy.getCsrfTokenRepository().deleteToken(token).subscribeOn(Schedulers.boundedElastic()).subscribe();
                    }))
                    .onErrorResume(ValidationException.class, e -> writeJsonResponse(exchange, e.getErrorCode()));

        };
    }

    /**
     * 寫入 JSON 響應
     *
     * @param exchange 伺服器 Web 交換對象
     * @param error    錯誤信息
     *
     * @return 空 Mono
     */
    private Mono<Void> writeJsonResponse(ServerWebExchange exchange, ValidationException.ErrorCode error) {
        try {
            ApiResponseDTO<?> apiResponse = ApiResponseDTO
                    .builder()
                    .timestamp(LocalDateTime.now())
                    .path(exchange.getRequest().getPath().value())
                    .message(error.getMessage())
                    .status(error.getCode())
                    .build();
            exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.getResponse().setStatusCode(error.getHttpStatus());
            return exchange
                    .getResponse()
                    .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(apiResponse))));
        } catch (JsonProcessingException jsonProcessingException) {
            return Mono.error(jsonProcessingException);
        }
    }

    public boolean isPassMethod(ServerWebExchange exchange) {
        return PASS_METHODS.contains(exchange.getRequest().getMethod());
    }

}
