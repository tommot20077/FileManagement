/**
 * 事件資料模型包，定義專案中反應式事件驅動架構的核心資料結構。
 *
 * <p>本包提供了標準化的事件模型，用於支持系統內部的事件驅動通訊：
 * <ul>
 *     <li>{@link xyz.dowob.filemanagement.data.event.FileEditedMessage}：檔案編輯事件的標準化資料容器</li>
 * </ul>
 * 這些事件類型可以被注入到 {@link xyz.dowob.filemanagement.component.event.EventSink} 提供的事件流中，
 * 實現系統內部的非阻塞、事件驅動的通訊機制。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

package xyz.dowob.filemanagement.data.event;