package xyz.dowob.filemanagement.data.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 響應的通用資料傳輸物件，用於標準化服務端回應。
 *
 * <p>此類別定義了統一的 API 響應結構，包含響應時間戳、狀態碼、請求路徑、訊息和資料。
 * 支持泛型，可承載不同類型的響應資料，確保前後端通信的一致性和可擴展性。</p>
 *
 * @param <T> 響應資料的具體類型，支持任意 POJO 或集合
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO<T> {
    /**
     * 請求處理完成的時間戳。
     *
     * <p>以 ISO 8601 格式記錄響應生成的精確時間，包含微秒級精度。</p>
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    /**
     * HTTP 狀態碼，標示請求的處理結果。
     *
     * <p>遵循標準 HTTP 狀態碼規範，如 200 表示成功，4xx 表示客戶端錯誤，5xx 表示伺服器錯誤。</p>
     */
    private int status;

    /**
     * 請求的 URL 路徑，用於追蹤請求來源。
     *
     * <p>記錄完整的請求路徑資訊，便於除錯和日誌追蹤。</p>
     */
    private String path;

    /**
     * 回應訊息，提供處理結果的詳細說明或錯誤資訊。
     *
     * <p>為客戶端提供人類可讀的狀態說明，包含成功訊息、錯誤描述或警告資訊。</p>
     */
    private String message;

    /**
     * 響應的實際資料載體。
     *
     * <p>支援泛型，可承載任意類型的業務資料，由泛型參數 T 指定具體類型。</p>
     */
    private T data;

}
