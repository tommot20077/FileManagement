package xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.annotation.CacheProviderType;
import xyz.dowob.filemanagement.component.provider.provider.AbstractRedisCacheProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.UserProperties;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;

/**
 * 用戶緩存提供者實現類，用於提供用戶緩存的操作
 * 實現了CacheProvider接口以及繼承AbstractRedisCacheProvider，對於用戶緩存的操作進行了封裝
 * 提供了緩存操作的具體實現
 * 此類透過用戶設定enable-user-cache來判斷是否啟用用戶緩存，當開啟時此類才會生效，默認開啟 {@link UserProperties}
 * 用戶緩存的key前綴以及緩存的默認過期時間來自於用戶設定
 * 用戶緩存的key-value為用戶ID-用戶信息
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserCacheProviderImpl
 * @create 2025/3/10
 * @Version 1.0
 **/
@Component
@CacheProviderType(CacheProviderEnum.USER_CACHE)
@ConditionalOnProperty(prefix = "user", name = "cache.enable-user-cache", havingValue = "true", matchIfMissing = true)
public class UserCacheProviderImpl extends AbstractRedisCacheProvider {

    /**
     * 用戶緩存提供者實現類的構造方法
     *
     * @param redisProvider Redis操作提供者
     */
    public UserCacheProviderImpl(RedisProvider redisProvider, UserProperties userProperties) {
        super(redisProvider);
        super.setCACHE_PREFIX(userProperties.getCache().getUserInfoCachePrefix());
        super.setDEFAULT_EXPIRE_TIME(userProperties.getCache().getDefaultExpire());
    }
}
