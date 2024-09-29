package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName MongoConfig
 * @description
 * @create 2024-09-28 17:45
 * @Version 1.0
 **/
@Configuration
public class MongoConfig {
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory, MappingMongoConverter mappingMongoConverter) {
        return new ReactiveMongoTemplate(reactiveMongoDatabaseFactory, mappingMongoConverter);
    }

}
