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
     * 注意：如果開啟的話，根據用戶存處的檔案大小，會佔用大量的記憶體，需要根據實際情況進行調整
     */
    private Boolean enableFileDownloadStreamCache = true;

    /**
     * 下載流緩存前綴
     */
    private String downloadCachePrefix = "download_stream_cache";

    /**
     * 下載流緩存過期時間，默認為 30，單位為分鐘，此值必須大於0否則會報錯
     * 此參數用於設置檔案下載流緩存的過期時間，當檔案下載流不再使用時，會自動清除緩存
     */
    private Integer downloadCacheExpireTime = 30;

    /**
     * 是否啟用用戶檔案列表緩存，默認為 true
     * 建議開啟，這樣可以有效提升檔案列表的查詢效率
     */
    private Boolean enableUserFileListCache = true;

    /**
     * 文件列表緩存過期時間，默認為 60，單位為分鐘，此值必須大於0否則會報錯
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
     * 用戶資訊緩存過期時間，默認為1440，單位為分鐘，此值必須大於0否則會報錯
     */
    private Integer userInfoCacheExpireTime = 1440;

    /**
     * 緩存分塊大小，默認為 5MB，用於處理大文件的緩存，單位為Byte
     * 用於大檔案的緩存，當檔案大小超過這個值時，會自動分割成多個小檔案進行緩存
     */
    private Integer chunkSize = 5 * 1024 * 1024;
}
