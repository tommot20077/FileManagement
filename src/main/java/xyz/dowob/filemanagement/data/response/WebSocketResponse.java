package xyz.dowob.filemanagement.data.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket回應類，用於封裝WebSocket的回應內容
 * 包括時間戳、回應類型、消息和數據
 *
 * @author yuan
 * @program FileManagement
 * @ClassName WebSocketResponse
 * @create 2025/5/14
 * @Version 1.0
 **/

@Data
@Builder
@AllArgsConstructor
public class WebSocketResponse<T> {
    /**
     * 返回的時間戳
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    /**
     * 回應的類型
     */
    private T type;

    /**
     * 返回的消息
     */
    private String message;

    /**
     * 返回的數據
     */
    private Object data;
}