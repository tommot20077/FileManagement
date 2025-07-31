package xyz.dowob.filemanagement.customenum;

/**
 * 快取提供者枚舉，定義系統中不同類型的快取策略和機制。
 *
 * <p>此枚舉類型用於識別和管理系統中的快取提供者，提供一個統一且可擴展的快取管理機制。
 * 透過 {@link xyz.dowob.filemanagement.annotation.CacheProviderType} 和 
 * {@link xyz.dowob.filemanagement.component.manager.CacheManager} 實現靈活的快取策略選擇。</p>
 *
 * <p>快取策略是提高系統效能和降低資源消耗的關鍵元件，可根據不同的使用場景選擇合適的快取類型。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.annotation.CacheProviderType
 * @see xyz.dowob.filemanagement.component.manager.CacheManager
 */

public enum CacheProviderEnum {
    /**
     * 使用者快取，用於儲存和管理使用者相關的即時資訊，提高使用者資料存取效率。
     */
    USER_CACHE,

    /**
     * 檔案流快取，針對大型檔案的資料流進行快取，減少重複讀取和頻繁存取的開銷。
     */
    FILE_STREAM_CACHE,

    /**
     * 使用者檔案列表快取，快取使用者的檔案清單，加速檔案清單的載入和呈現。
     */
    USER_FILE_LIST_CACHE,
}
