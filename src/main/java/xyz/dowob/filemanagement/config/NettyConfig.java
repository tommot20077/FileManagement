package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import reactor.netty.resources.ConnectionProvider;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;

/**
 * Netty 連線池設定類，管理 WebFlux 非同步 I/O 連線資源。
 * <p>提供以 "scan-connection-pool" 命名的 Netty 連線池配置，支援最大連線數、
 * 空閒時間、生命週期和獲取超時等參數調整。連線池採用建構器模式配置，
 * 確保網路連線的高效管理和資源節約。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class NettyConfig {
    /**
     * 全局設定屬性，用於獲取系統設定的相關參數。
     */
    private final GlobalProperties globalProperties;

    /**
     * 構造方法，初始化 NettyConfig 設定類。
     * <p>
     * 在構造過程中會驗證所有連線池設定參數的合法性，確保設定參數符合預期。
     * 包括檢測最大連線數、空闒時間、生命週期和排隊超時時間等參數。
     *
     * @param globalProperties 全局設定屬性，包含 Netty 連線池的所有設定參數
     * @throws IllegalArgumentException 當設定參數不合法時拋出
     */
    public NettyConfig(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
        Assert.isTrue(globalProperties.getNettyPool().getMaxConnections() > 0, "連接數量必須大於 0");
        Assert.isTrue(globalProperties.getNettyPool().getMaxIdleTime().isPositive(), "連接空閒時間必須大於 0");
        Assert.isTrue(globalProperties.getNettyPool().getMaxLifeTime().isPositive(), "連接生命週期必須大於 0");
        Assert.isTrue(globalProperties.getNettyPool().getPendingAcquireTimeout().isPositive(), "請求排隊超時時間必須大於 0");
    }

    /**
     * 創建 Netty 連線池提供者 Bean。
     * <p>
     * 該方法根據全局設定屬性創建一個連線池提供者，用於管理 HTTP 客戶端連線。
     * 連線池參數包括：
     * <p>
     * - 最大連線數：控制同時最大連線數量
     * <p>
     * - 最大空闒時間：連線空闒時的最長保持時間
     * <p>
     * - 最大生命週期：連線的最長存活時間
     * <p>
     * - 排隊超時時間：獲取連線時的最長等待時間
     *
     * @return 設定好的 Netty 連線池提供者實例
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
