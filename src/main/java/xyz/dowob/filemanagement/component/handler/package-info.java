/**
 * 反應式請求處理器包，提供 WebFlux 環境下的異常處理和 WebSocket 通訊機制。
 *
 * <p>核心處理器實現：</p>
 *
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.component.handler.CustomExceptionHandler} - 全域異常處理器</li>
 *   <li>{@link xyz.dowob.filemanagement.component.handler.CustomWebSocketSession} - 增強型 WebSocket 會話封裝器</li>
 *   <li>{@link xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler} - 檔案上傳 WebSocket 處理器</li>
 *   <li>{@link xyz.dowob.filemanagement.component.handler.OnlineFileWebSocketHandler} - 線上協作編輯處理器</li>
 *   <li>{@link xyz.dowob.filemanagement.component.handler.WebSocketFailHandler} - WebSocket 連線錯誤處理器</li>
 * </ul>
 *
 * <p>技術特點：</p>
 *
 * <ul>
 *   <li>完全支持 Spring WebFlux 反應式編程模型</li>
 *   <li>統一的錯誤處理和響應格式化機制</li>
 *   <li>非阻塞的 WebSocket 通訊實現</li>
 *   <li>可擴展的處理器架構設計</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.component.handler;