package xyz.dowob.filemanagement.data.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket 響應物件，為 WebSocket 通訊提供標準化的響應封裝。
 *
 * <p>這個通用的 WebSocket 響應容器包含時間戳、響應類型、訊息內容和實際資料載體。
 * 支援泛型參數，可適應不同的響應類型與資料結構。</p>
 *
 * @param <T> 響應類型的泛型參數，通常為枚舉類型或狀態標識
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketResponse<T> {
    /**
     * 響應生成的時間戳。
     *
     * <p>以 ISO 8601 格式記錄 WebSocket 訊息的生成時間，包含微秒級精度。</p>
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    /**
     * WebSocket 響應的類型標識。
     *
     * <p>通常為枚舉類型，用於標識不同種類的響應或狀態，便於客戶端進行類型判斷。</p>
     */
    private T type;

    /**
     * 描述性訊息內容。
     *
     * <p>為使用者提供人類可讀的狀態說明、操作結果或錯誤資訊。</p>
     */
    private String message;

    /**
     * 實際資料載體。
     *
     * <p>支援任意類型的資料傳輸，為 WebSocket 通訊提供最大的靈活性和擴展性。</p>
     */
    private Object data;
}