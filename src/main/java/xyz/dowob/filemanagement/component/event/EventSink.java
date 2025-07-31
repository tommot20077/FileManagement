package xyz.dowob.filemanagement.component.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import xyz.dowob.filemanagement.unity.LogUnity;

/**
 * 反應式事件發送器，執行事件分發及訂閱的核心機制。
 *
 * <p>本類利用 Project Reactor 的 {@link reactor.core.publisher.Sinks} 執行事件管理，提供內建的事件流控制能力。</p>
 *
 * <p>主要特點：
 * <ul>
 *   <li>採用多播型訂閱模型</li>
 *   <li>使用背壓緩衝區控制事件流量</li>
 *   <li>自動處理事件發送及訂閱的例外</li>
 * </ul>
 * </p>
 *
 * @param <T> 事件的資料類型
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
public class EventSink<T> {

    /**
     * 反應式事件發送器，使用 Project Reactor 的 {@link Sinks} 實現事件發送和訂閱機制。
     *
     * <p>設定為多播模式，支援多個訂閱者同時接收事件，並使用背壓緩衝區控制事件流量。
     * 此設定不允許查閱已發送的歷史事件。</p>
     */
    private final Sinks.Many<T> sink = Sinks.many().multicast().onBackpressureBuffer();

    /**
     * 發送事件到所有訂閱者。
     *
     * <p>此方法會嘗試發送事件到所有目前訂閱中的訂閱者。若事件為 null，
     * 則直接忽略。若發送過程中發生失敗，將記錄錯誤日誌。</p>
     *
     * @param event 要發送的事件對象，不可為 null
     */
    public void emit(T event) {
        if (event == null) {
            return;
        }

        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result != Sinks.EmitResult.OK) {
            LogUnity.error("無法發送事件: %s, 發送結果: %s", event, result);
        }
    }

    /**
     * 訂閱事件流，獲取事件的反應式流。
     *
     * <p>回傳的 {@link Flux} 將發送所有後續發送的事件。每個訂閱者都會獨立接收
     * 相同的事件。訂閱者可以使用 Flux 的各種操作符進行事件處理、過濾和轉換。</p>
     *
     * @return 事件的反應式流，接收後續發送的所有事件
     */
    public Flux<T> subscribe() {
        return sink.asFlux();
    }
}
