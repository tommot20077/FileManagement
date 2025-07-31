package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.dowob.filemanagement.component.event.EventSink;
import xyz.dowob.filemanagement.data.event.FileEditedMessage;

/**
 * 事件發送器設定類，管理系統中的反應式事件傳遞 Bean。
 *
 * <p>提供檔案編輯事件發送器的設定，支援非同步事件處理和訂閱機制。
 * 所有事件發送器統一在此設定類中註冊，便於系統管理和維護。
 * 基於 Reactor 的 Sinks 實現高效能事件廣播。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

@Configuration
public class EventSinkConfig {
    /**
     * 建立檔案編輯事件發送器 Bean，處理檔案編輯相關事件廣播。
     *
     * <p>當檔案編輯操作發生時，透過此發送器將事件訊息廣播給所有訂閱者。
     * 支援 WebFlux 非同步事件處理，提供高效的反應式事件傳遞能力。</p>
     *
     * @return 檔案編輯事件發送器實例
     */
    @Bean
    public EventSink<FileEditedMessage> fileEditedMessageSink() {
        return new EventSink<>();
    }
}