/**
 * 快取服務提供者實現模組，提供多種類型的高效能快取解決方案。
 *
 * <p>此模組實現基於 Redis 的快取提供者，針對不同資料類型和使用場景提供專門的快取策略。
 * 所有實現均支援反應式操作，確保快取存取的非阻塞特性。</p>
 *
 * <p>快取提供者實現：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider.FileListCacheProviderImpl}：
 *       檔案列表快取，針對檔案清單查詢提供高速快取機制</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider.StreamCacheProviderImpl}：
 *       串流資料快取，支援大型檔案資料流的分塊快取存儲</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider.UserCacheProviderImpl}：
 *       使用者資料快取，提供使用者資訊的快速存取機制</li>
 * </ul>
 * </p>
 *
 * <p>每個快取提供者都支援自定義過期時間、批次操作及條件式快取控制，
 * 透過註解驅動的設定機制實現靈活的啟用與停用控制。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

package xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider;