package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.CacheProviderEnum;

import java.lang.annotation.*;

/**
 * 快取提供器類型標記註解。
 * <p>
 * 此註解用於標記和區分不同類型的快取提供器實現，透過策略模式支援多種快取儲存後端的動態選擇。
 * 系統根據設定或條件選擇合適的快取提供器，例如本地記憶體快取、Redis 分散式快取等。
 * 支援快取提供器工廠模式的類型識別、依賴注入時的條件性 Bean 選擇，以及系統啟動時的快取策略自動設定。
 * <p>
 * 使用範例：
 * <pre>{@code
 * @CacheProviderType(CacheProviderEnum.REDIS)
 * @Component
 * public class RedisCacheProvider implements CacheProvider {
 *     // Redis 快取實現
 * }
 * 
 * @CacheProviderType(CacheProviderEnum.LOCAL)
 * @Component  
 * public class LocalCacheProvider implements CacheProvider {
 *     // 本地快取實現
 * }
 * }</pre>
 *
 * @see xyz.dowob.filemanagement.customenum.CacheProviderEnum
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheProviderType {
    /**
     * 指定快取提供器的具體類型。
     * <p>
     * 此屬性定義了被標記類別所實現的快取提供器類型，用於系統運行時的策略選擇和依賴注入。
     *
     * @return 快取提供器的類型枚舉值
     * @see xyz.dowob.filemanagement.customenum.CacheProviderEnum
     */
    CacheProviderEnum value();
}
