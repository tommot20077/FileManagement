package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.dowob.filemanagement.component.event.EventSink;
import xyz.dowob.filemanagement.data.event.FileEditedMessage;

/**
 * 事件發送器配置類，用於註冊事件發送器
 * 所有的事件發送器都在這裡註冊進行統一管理
 * 不同的事件發送器依照類型來區分，詳細請參考 {@link EventSink}
 * @author yuan
 * @program FileManagement
 * @ClassName EventSinkConfig
 * @create 2025/5/15
 * @Version 1.0
 **/

@Configuration
public class EventSinkConfig {
    /**
     * 文件編輯事件發送器
     * 當有文件編輯事件發生時，將使用此事件發送器來發送事件
     *
     * @return 文件編輯事件發送器
     */
    @Bean
    public EventSink<FileEditedMessage> fileEditedMessageSink() {
        return new EventSink<>();
    }
}