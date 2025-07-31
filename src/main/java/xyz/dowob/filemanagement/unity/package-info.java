/**
 * 此套件存放了通用的類以及接口，用於提供給其他套件使用。減少程式碼重複，提高程式碼的可讀性。
 * <p>
 * 主要工具類別：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.unity.CacheConcurrentHashMap} - 用於存儲快取的 HashMap 類</li>
 *   <li>{@link xyz.dowob.filemanagement.unity.DynamicThreadPoolExecutor} - 用於動態調整執行緒池大小的執行緒池類</li>
 *   <li>{@link xyz.dowob.filemanagement.unity.FileCrudService} - 用於對檔案進行 CRUD 操作的接口</li>
 *   <li>{@link xyz.dowob.filemanagement.unity.LogUnity} - 日誌處理工具類</li>
 *   <li>{@link xyz.dowob.filemanagement.unity.ResponseUnity} - 用於建立回傳結果的統一接口</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.unity;