package xyz.dowob.filemanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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
     * 配置 ReactiveRedisTemplate，定義序列化方式，統一使用 GenericJackson2JsonRedisSerializer 進行序列化
     *
     * @param reactiveRedisConnectionFactory ReactiveRedisConnectionFactory 用於創建 ReactiveRedisTemplate
     *
     * @return ReactiveRedisTemplate
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext()
                .key(StringRedisSerializer.UTF_8)
                .value(new GenericJackson2JsonRedisSerializer())
                .hashKey(StringRedisSerializer.UTF_8)
                .hashValue(new GenericJackson2JsonRedisSerializer())
                .string(StringRedisSerializer.UTF_8)
                .build();
        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, serializationContext);
    }

}
