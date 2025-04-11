package xyz.dowob.filemanagement.customenum;

/**
 * 緩存提供器的枚舉類型，應用於區分不同的緩存提供器
 * 並在CacheManager中使用 CacheProviderType 來標記緩存提供器的類型
 * {@link xyz.dowob.filemanagement.annotation.CacheProviderType}
 * {@link xyz.dowob.filemanagement.component.manager.CacheManager}
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CacheProviderEnum
 * @create 2025/3/15
 * @Version 1.0
 **/

public enum CacheProviderEnum {
    /**
     * 使用者緩存
     */
    USER_CACHE,

    /**
     * 檔案流緩存
     */
    FILE_STREAM_CACHE,

    /**
     * 檔案列表緩存
     */
    USER_FILE_LIST_CACHE,
}
