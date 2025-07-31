/**
 * 反應式事件組件包，執行應用程式中的事件管理和處理機制。
 *
 * <p>主要組件：</p>
 *
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.component.event.EventSink} - 事件發送器，負責事件分發和訂閱</li>
 * </ul>
 *
 * <p>重點技術：</p>
 *
 * <ul>
 *   <li>利用 {@link reactor.core.publisher.Sinks} 與 {@link reactor.core.publisher.Flux} 實現反應式事件機制</li>
 *   <li>支持非阻塞的事件處理</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.component.event;