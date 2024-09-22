package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import reactor.netty.resources.ConnectionProvider;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;

/**
 * Netty配置類，用於配置 Netty 的連接池
 *
 * @author yuan
 * @program FileManagement
 * @ClassName NettyConfig
 * @create 2025/4/29
 * @Version 1.0
 **/
@Configuration
public class NettyConfig {
    /**
     * 全局配置類，用於獲取全局配置的屬性
     */
    private final GlobalProperties globalProperties;

    /**
     * NettyConfig 的構造函數
     * 檢測設定的連接池配置是否正確
     *
     * @param globalProperties 全局配置類，用於獲取全局配置的屬性
     */
    public NettyConfig(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
        Assert.isTrue(globalProperties.getNettyPool().getMaxConnections() > 0, "連接數量必須大於 0");
        Assert.isTrue(globalProperties.getNettyPool().getMaxIdleTime().isPositive(), "連接空閒時間必須大於 0");
        Assert.isTrue(globalProperties.getNettyPool().getMaxLifeTime().isPositive(), "連接生命週期必須大於 0");
        Assert.isTrue(globalProperties.getNettyPool().getPendingAcquireTimeout().isPositive(), "請求排隊超時時間必須大於 0");
    }

    /**
     * Netty 連接池配置，將設定的連接池配置註冊到 Spring 容器中
     *
     * @return ConnectionProvider 連接池提供者
     */
    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider
                .builder("scan-connection-pool")
                .maxConnections(globalProperties.getNettyPool().getMaxConnections())
                .maxIdleTime(globalProperties.getNettyPool().getMaxIdleTime())
                .maxLifeTime(globalProperties.getNettyPool().getMaxLifeTime())
                .pendingAcquireTimeout(globalProperties.getNettyPool().getPendingAcquireTimeout())
                .build();
    }
}
