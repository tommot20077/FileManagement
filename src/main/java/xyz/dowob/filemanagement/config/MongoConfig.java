package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

/**
 * MongoDB 反應式資料庫設定類，用於設定 MongoDB 相關的反應式操作元件。
 * <p>
 * 此設定類主要提供以下功能：
 * <p>
 * 1. ReactiveMongoTemplate 設定：提供非阻塞式 MongoDB 操作支援
 * <p>
 * 2. 反應式資料庫連接管理：確保高效的資料庫連接處理
 * <p>
 * 3. 文檔映射轉換支援：支援 Java 物件與 MongoDB 文檔之間的轉換
 * <p>
 * 與 WebFlux 反應式程式設計模型完美整合，提供高併發、非阻塞的
 * MongoDB 資料存取能力，適用於大規模並發場景。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class MongoConfig {
    /**
     * 創建 ReactiveMongoTemplate Bean，用於 MongoDB 非同步資料庫操作。
     * <p>
     * ReactiveMongoTemplate 是 Spring Data MongoDB 的核心組件，
     * 提供了對 MongoDB 的非同步操作支持，包括查詢、插入、更新、刪除等操作。
     * <p>
     * 與 WebFlux 的非同步模型完美結合，支持 Mono 和 Flux 操作，
     * 提供高效的非阻塞式資料庫存取能力。
     *
     * @param reactiveMongoDatabaseFactory 非同步 MongoDB 資料庫工廠，用於創建資料庫連線
     * @param mappingMongoConverter        Mongo 映射轉換器，用於 Java 物件和 MongoDB 文檔之間的轉換
     * @return 設定好的 ReactiveMongoTemplate 實例
     */
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory, MappingMongoConverter mappingMongoConverter) {
        return new ReactiveMongoTemplate(reactiveMongoDatabaseFactory, mappingMongoConverter);
    }

}
