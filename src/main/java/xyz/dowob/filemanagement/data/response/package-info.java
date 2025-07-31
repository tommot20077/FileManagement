/**
 * 響應資料傳輸物件（DTOs）包，定義專案中所有反應式響應標準。
 *
 * <p>本包提供了統一的響應模型，包含三種主要響應類型：</p>
 *
 * <ul>
 *     <li>{@link xyz.dowob.filemanagement.data.response.ApiResponseDTO}：標準化 HTTP API 響應的通用資料傳輸對象</li>
 *     <li>{@link xyz.dowob.filemanagement.data.response.PagedResponseDTO}：支持分頁的資料響應傳輸對象</li>
 *     <li>{@link xyz.dowob.filemanagement.data.response.WebSocketResponse}：WebSocket 通訊的反應式響應對象</li>
 * </ul>
 *
 * <p>這些響應模型確保了系統內部和外部通訊的一致性和可擴展性。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.data.response;