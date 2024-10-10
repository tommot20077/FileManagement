package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName WebFluxConfiguration
 * @description
 * @create 2024-12-08 23:07
 * @Version 1.0
 **/
@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024);
    }
}
