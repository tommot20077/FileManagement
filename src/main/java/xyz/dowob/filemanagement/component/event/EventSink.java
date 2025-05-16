package xyz.dowob.filemanagement.component.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import xyz.dowob.filemanagement.config.EventSinkConfig;
import xyz.dowob.filemanagement.unity.LogUnity;

/**
 * 事件發送器，用於發送事件到訂閱者，可視為一個消息隊列
 * 當有事件發生時，將事件發送到這裡，然後訂閱者可以訂閱這個事件
 * 每一個事件發送器依照類型來區分，統一於 {@link EventSinkConfig} 中進行註冊
 *
 * @author yuan
 * @program FileManagement
 * @ClassName EventSink
 * @create 2025/5/15
 * @Version 1.0
 **/
public class EventSink<T> {

    /**
     * 此處使用了 Project Reactor 的 Sinks 類來實現事件的發送和訂閱
     * 設定為多播模式，並使用背壓緩衝區來處理事件的流量控制，不可查閱已發送的事件
     */
    private final Sinks.Many<T> sink = Sinks.many().multicast().onBackpressureBuffer();

    /**
     * 發送事件到訂閱者
     * 當有事件發生時，將事件發送到這裡，然後訂閱者可以訂閱這個事件
     * 當事件發送失敗時，將會記錄錯誤日誌
     *
     * @param event 要發送的事件
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
     * 訂閱事件
     * 在訂閱之後，當有事件發生時，將會收到事件的通知
     *
     * @return 訂閱的事件流
     */
    public Flux<T> subscribe() {
        return sink.asFlux();
    }
}
