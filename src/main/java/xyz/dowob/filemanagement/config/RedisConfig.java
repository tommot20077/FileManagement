package xyz.dowob.filemanagement.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 反應式設定類，負責設定 Redis 連接、序列化和連接池相關設定。
 * <p>
 * 此設定類主要提供以下功能：
 * <p>
 * 1. 反應式 Redis 模板設定：支援非阻塞 I/O 操作的 Redis 操作模板
 * <p>
 * 2. JSON 序列化設定：使用 Jackson 進行物件與 JSON 的雙向轉換
 * <p>
 * 3. Lettuce 連接池設定：高效能的 Redis 連接池管理
 * <p>
 * 4. 自動重連機制：提供網路中斷後的自動重連功能
 * <p>
 * 序列化特性：
 * - 鍵值使用 UTF-8 字串序列化
 * - 物件值使用 Jackson JSON 序列化
 * - 支援 Java 8 時間 API
 * - 啟用多型類型資訊保存
 * <p>
 * 連接池特性：
 * - 支援自動重連機制
 * - 啟用 TCP KeepAlive 和 NoDelay
 * - 發布操作使用調度器優化
 * - 支援連接超時和命令超時設定
 * <p>
 * 此設定確保 Redis 操作的高效能和可靠性，特別適用於高並發的反應式應用程式。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    /**
     * Redis 設定屬性實例。
     * <p>
     * 透過 Spring Boot 自動設定機制注入的 Redis 連接參數，
     * 包含主機位址、埠號、資料庫索引、認證密碼等資訊。
     */
    private final RedisProperties redisProperties;

    /**
     * 設定反應式 Redis 操作模板，提供非阻塞的 Redis 資料存取能力。
     * <p>
     * 此方法設定以下序列化設定：
     * <p>
     * 1. 鍵值序列化：使用 UTF-8 字串序列化器，確保鍵值的一致性
     * <p>
     * 2. 物件序列化：使用 Jackson JSON 序列化器，支援複雜物件的儲存
     * <p>
     * 3. 類型資訊保存：啟用多型類型驗證器，確保物件反序列化的正確性
     * <p>
     * 4. 時間 API 支援：註冊 JavaTimeModule，支援 LocalDateTime 等時間類型
     * <p>
     * 序列化設定特色：
     * - 支援所有 Object 子類型的多型序列化
     * - 啟用所有欄位的可見性檢測
     * - 類型資訊作為屬性儲存，便於反序列化
     * - 雜湊鍵值同樣使用一致的序列化策略
     *
     * @param reactiveRedisConnectionFactory 反應式 Redis 連接工廠實例
     * @return 設定完成的反應式 Redis 操作模板
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);


        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext()
                .key(StringRedisSerializer.UTF_8)
                .value(serializer)
                .hashKey(StringRedisSerializer.UTF_8)
                .hashValue(serializer)
                .string(StringRedisSerializer.UTF_8)
                .build();
        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, serializationContext);
    }

    /**
     * 設定 Jackson 物件映射器，提供 JSON 序列化和反序列化功能。
     * <p>
     * 此映射器主要用於非 Redis 相關的 JSON 處理操作，
     * 註冊了 JavaTimeModule 以支援 Java 8 時間 API 的序列化。
     * <p>
     * 支援的時間類型包括：
     * - LocalDateTime
     * - LocalDate
     * - LocalTime
     * - ZonedDateTime
     * - Instant
     * - Duration
     *
     * @return 設定完成的 Jackson 物件映射器
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }


    /**
     * 設定 Lettuce Redis 客戶端，提供高效能的非阻塞 Redis 連接。
     * <p>
     * 此方法設定以下連接特性：
     * <p>
     * 1. 連接參數設定：基於 RedisProperties 建立 Redis URI
     * <p>
     * 2. 自動重連機制：網路中斷時自動嘗試重新連接
     * <p>
     * 3. 發布操作優化：啟用排程器優化發布訂閱效能
     * <p>
     * 4. TCP 網路優化：啟用 KeepAlive 和 NoDelay 提升網路效能
     * <p>
     * 連接優化特性：
     * - autoReconnect: 自動重連，提升連接可靠性
     * - publishOnScheduler: 優化發布操作的執行緒處理
     * - keepAlive: 保持連接活躍，減少連接建立開銷
     * - tcpNoDelay: 立即發送資料，降低延遲
     * <p>
     * 客戶端生命週期由 Spring 容器管理，應用程式關閉時會自動調用 shutdown 方法。
     *
     * @return 設定完成的 Lettuce Redis 客戶端
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient() {
        RedisURI redisUri = RedisURI
                .builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .withPassword(redisProperties.getPassword().toCharArray())
                .withDatabase(redisProperties.getDatabase())
                .build();

        RedisClient redisClient = RedisClient.create(redisUri);
        redisClient.setOptions(ClientOptions
                                       .builder()
                                       .autoReconnect(true)
                                       .publishOnScheduler(true)
                                       .socketOptions(SocketOptions.builder().keepAlive(true).tcpNoDelay(true).build())
                                       .build());
        return redisClient;
    }


    /**
     * 設定反應式 Redis 連接工廠，管理 Redis 連接的建立和連接池。
     * <p>
     * 此方法設定以下連接設定：
     * <p>
     * 1. 獨立模式設定：設定 Redis 伺服器的基本連接參數
     * <p>
     * 2. 連接池設定：使用 Apache Commons Pool2 提供高效的連接池管理
     * <p>
     * 3. 命令超時設定：防止長時間等待造成的系統阻塞
     * <p>
     * 4. 客戶端選項：優化網路連接和斷線處理行為
     * <p>
     * 連接設定包括：
     * - 主機名稱和埠號
     * - 資料庫索引選擇
     * - 認證使用者名稱和密碼
     * - 連接池大小和超時設定
     * <p>
     * 進階設定特性：
     * - 自動重連機制
     * - 發布操作排程器優化
     * - 預設的斷線處理行為
     * - 連接池健康檢查
     *
     * @return 設定完成的反應式 Redis 連接工廠
     */
    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration();
        redisConfiguration.setHostName(redisProperties.getHost());
        redisConfiguration.setPort(redisProperties.getPort());
        redisConfiguration.setDatabase(redisProperties.getDatabase());
        redisConfiguration.setPassword(redisProperties.getPassword());
        redisConfiguration.setUsername(redisProperties.getUsername());

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration
                .builder()
                .poolConfig(new org.apache.commons.pool2.impl.GenericObjectPoolConfig<>())
                .commandTimeout(Duration.ofSeconds(redisProperties.getTimeout().getSeconds()))
                .clientOptions(ClientOptions
                                       .builder()
                                       .autoReconnect(true)
                                       .publishOnScheduler(true)
                                       .disconnectedBehavior(ClientOptions.DisconnectedBehavior.DEFAULT)
                                       .build())
                .build();
        return new LettuceConnectionFactory(redisConfiguration, clientConfig);
    }
}
