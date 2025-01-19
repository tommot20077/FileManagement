package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebFilter;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;

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
     * 帶參數的構造方法
     *
     * @param fileProperties 文件配置文件
     */
    public WebFluxConfiguration(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
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

}
