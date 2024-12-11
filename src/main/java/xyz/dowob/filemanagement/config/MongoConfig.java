package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

/**
 * Mongo 配置類，用於配置 Mongo 相關的配置
 *
 * @author yuan
 * @program FileManagement
 * @ClassName MongoConfig
 * @description
 * @create 2024-09-28 17:45
 * @Version 1.0
 **/
@Configuration
public class MongoConfig {
    /**
     * 配置 ReactiveGridFsTemplate
     *
     * @param reactiveMongoDatabaseFactory ReactiveMongoDatabaseFactory 用於創建 ReactiveMongoTemplate
     * @param mappingMongoConverter        MappingMongoConverter 用於對 Mongo 數據進行映射
     *
     * @return ReactiveGridFsTemplate
     */
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(
            ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
            MappingMongoConverter mappingMongoConverter) {
        return new ReactiveMongoTemplate(reactiveMongoDatabaseFactory, mappingMongoConverter);
    }

}
