package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import xyz.dowob.filemanagement.convert.EntityByteBooleanMapper;
import xyz.dowob.filemanagement.convert.EntityJsonSetMapper;

import java.util.Arrays;
import java.util.List;

/**
 * R2DBC 資料庫設定類，負責設定反應式關聯資料庫連接相關設定。
 * <p>
 * 此設定類主要提供以下功能：
 * <p>
 * 1. 自定義資料類型轉換器設定：支援複雜資料類型與資料庫欄位之間的轉換
 * <p>
 * 2. 反應式事務管理器設定：提供非阻塞的事務處理能力
 * <p>
 * 3. 資料庫實體映射支援：確保 Java 物件與資料庫記錄的正確對應
 * <p>
 * 轉換器包含：
 * - JSON 與 Set 集合的雙向轉換
 * - Boolean 與 Byte 的雙向轉換
 * <p>
 * 支援的資料庫操作特性：
 * - 非阻塞 I/O 操作
 * - 反應式事務管理
 * - 高並發資料存取
 * <p>
 * 此設定確保系統能夠高效處理大量並發的資料庫操作請求。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class R2dbcConfig {
    /**
     * 設定 R2DBC 自定義轉換器，提供資料類型轉換支援。
     * <p>
     * 此方法註冊以下轉換器：
     * <p>
     * 1. Set 與 JSON 字串的雙向轉換器：{@link xyz.dowob.filemanagement.convert.EntityJsonSetMapper.SetConverter} 和 {@link xyz.dowob.filemanagement.convert.EntityJsonSetMapper.JsonConverter}
     * <p>
     * 2. Boolean 與 Byte 的雙向轉換器：{@link xyz.dowob.filemanagement.convert.EntityByteBooleanMapper.BooleanToByteConverter} 和 {@link xyz.dowob.filemanagement.convert.EntityByteBooleanMapper.ByteToBooleanConverter}
     * <p>
     * 這些轉換器確保複雜資料類型能夠正確地在 Java 物件與資料庫記錄之間進行轉換，
     * 特別適用於需要儲存集合資料或布林值的場景。
     *
     * @return 設定完成的 R2DBC 自定義轉換器實例
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Object> converters = Arrays.asList(new EntityJsonSetMapper.SetConverter(),
                                                new EntityJsonSetMapper.JsonConverter(),
                                                new EntityByteBooleanMapper.BooleanToByteConverter(),
                                                new EntityByteBooleanMapper.ByteToBooleanConverter()
        );
        return new R2dbcCustomConversions(CustomConversions.StoreConversions.NONE, converters);
    }


    /**
     * 設定反應式事務操作器，提供非阻塞事務管理能力。
     * <p>
     * 此操作器基於 Spring R2DBC 的反應式事務管理器，支援：
     * <p>
     * 1. 非阻塞事務操作：不會阻塞執行緒，提高系統並發能力
     * <p>
     * 2. 反應式流事務控制：與 Mono 和 Flux 完美整合
     * <p>
     * 3. 自動事務回滾：在發生異常時自動回滾事務
     * <p>
     * 4. 事務傳播機制：支援多種事務傳播行為
     * <p>
     * 使用此操作器可以確保資料庫操作的一致性和完整性，
     * 同時維持反應式程式設計模型的非阻塞特性。
     *
     * @param transactionManager Spring 提供的反應式事務管理器實例
     * @return 設定完成的事務操作器實例
     */
    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }

}
