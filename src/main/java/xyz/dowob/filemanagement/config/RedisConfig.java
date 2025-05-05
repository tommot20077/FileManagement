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
 * Redis 配置類，用於配置 Redis 相關的配置
 *
 * @author yuan
 * @program FileManagement
 * @ClassName RedisConfig
 * @description
 * @create 2024-09-27 03:18
 * @Version 1.0
 **/
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    /**
     * RedisProperties 用於獲取 Redis 的配置
     */
    private final RedisProperties redisProperties;

    /**
     * 配置 ReactiveRedisTemplate，定義序列化方式，統一使用 GenericJackson2JsonRedisSerializer 進行序列化
     *
     * @param reactiveRedisConnectionFactory ReactiveRedisConnectionFactory 用於創建 ReactiveRedisTemplate
     *
     * @return ReactiveRedisTemplate
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
     * 配置 ObjectMapper，註冊 JavaTimeModule 模組
     *
     * @return ObjectMapper 物件映射器
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }


    /**
     * 配置 RedisClient，設置連接池
     * 這裡使用 Lettuce 作為 Redis 客戶端
     * 這邊的配置主要是針對 Redis 的連接池進行配置
     * 啟用了自動重連、SocketOptions、PublishOnScheduler 等選項
     *
     * @return RedisClient
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
     * 配置 LettuceConnectionFactory，啟用連接池
     * 這裡使用 Lettuce 作為 Redis 客戶端
     * 設定了 Redis 的主機、端口、數據庫、密碼等配置
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
