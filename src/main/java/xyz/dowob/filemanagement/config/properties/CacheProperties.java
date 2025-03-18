package xyz.dowob.filemanagement.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 緩存配置文件，用於配置緩存相關的參數，在 application 中配置 cache
 * 用於統一配置緩存相關的參數
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CacheProperties
 * @create 2025/3/18
 * @Version 1.0
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    /**
     * 是否啟用下載流緩存，默認為啟用
     */
    private Boolean enableFileDownloadStreamCache = true;

    /**
     * 下載流緩存前綴
     */
    private String downloadCachePrefix = "download_stream_cache";

    /**
     * 下載流緩存過期時間，默認為 30，單位為分鐘
     */
    private Integer downloadCacheExpireTime = 30;


    /**
     * 是否啟用文件列表緩存，默認為啟用
     */
    private Boolean enableUserFileListCache = true;

    /**
     * 文件列表緩存過期時間，默認為 60，單位為分鐘
     */
    private Integer fileListCacheExpireTime = 60;


    /**
     * 是否啟用用戶資訊緩存，默認為啟用
     */
    private Boolean enableUserInfoCache = true;

    /**
     * 用戶資訊緩存前綴
     */
    private String userInfoCachePrefix = "user_info_cache";

    /**
     * 用戶資訊緩存過期時間，默認為1440，單位為分鐘
     */
    private Integer userInfoCacheExpireTime = 1440;
}
