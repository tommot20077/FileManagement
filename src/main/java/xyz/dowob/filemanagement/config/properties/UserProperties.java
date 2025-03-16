package xyz.dowob.filemanagement.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 用戶配置文件，用於配置用戶相關的參數，在 application 中配置 user
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserProperties
 * @create 2025/3/12
 * @Version 1.0
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "user")
public class UserProperties {
    /**
     * 緩存配置
     */
    public Cache cache = new Cache();

    @Data
    public static class Cache {
        /**
         * 是否啟用用戶緩存，默認為啟用
         */
        public Boolean enableUserCache = true;

        /**
         * 緩存前綴
         */
        public String userInfoCachePrefix = "user_info_cache";

        /**
         * 緩存過期時間，默認為1小時，單位為分鐘
         */
        public Integer defaultExpire = 60;
    }
}
