package xyz.dowob.filemanagement.unity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.response.WebSocketResponse;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 基於反應式編程的統一響應處理工具介面，提供標準化的 API 和 WebSocket 響應格式建立機制。
 * 此介面確保整個檔案管理系統中所有端點的響應格式一致性，並提供自動化的錯誤處理和上下文資訊整合功能。
 *
 * <p>採用 Spring WebFlux 反應式編程模式，支援非阻塞 I/O 操作，適用於高並發的檔案上傳、下載和管理場景。
 * 所有響應物件均包含統一的時間戳、請求路徑和狀態碼資訊，確保前端可以進行一致性的錯誤處理和狀態追蹤。
 *
 * <p>主要功能包括：
 * <ul>
 *   <li>建立標準化的 ResponseEntity 物件，自動設定 HTTP 狀態碼</li>
 *   <li>產生包含上下文資訊的 ApiResponseDTO 響應物件</li>
 *   <li>支援 WebSocket 即時通訊的響應格式</li>
 *   <li>統一處理 ValidationException 和 LimitationException 錯誤</li>
 *   <li>自動整合請求路徑、時間戳和使用者上下文資訊</li>
 * </ul>
 *
 * <p>使用範例：
 * <pre>{@code
 * // 建立成功響應
 * @Override
 * public Mono<ResponseEntity<?>> uploadFile(ServerWebExchange exchange) {
 *     return fileService.upload()
 *         .map(result -> createApiResponse(exchange, "檔案上傳成功", result))
 *         .flatMap(this::createResponseEntity)
 *         .as(operation -> handleError(operation, exchange));
 * }
 *
 * // 建立 WebSocket 響應
 * WebSocketResponse<String> response = createWebSocketResponse(
 *     "UPLOAD_PROGRESS", 
 *     "檔案上傳進度更新", 
 *     progressData
 * );
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ApiResponseDTO
 * @see WebSocketResponse
 */
public interface ResponseUnity {
    /**
     * 建立 ResponseEntity 響應實體，根據 API 響應狀態自動設定 HTTP 狀態碼。
     * 此方法會根據 ApiResponseDTO 中的狀態碼自動判斷 HTTP 回應狀態：
     * 狀態碼為 200 時使用 HTTP 200 OK，其他狀態碼均使用 HTTP 400 Bad Request。
     *
     * <p>適用於大多數標準 API 響應場景，可確保狀態碼的一致性處理。
     * 對於需要特定 HTTP 狀態碼的場景，建議使用帶有 responseCode 參數的重載方法。
     *
     * @param apiResponse API 響應資料傳輸物件，包含狀態碼、訊息和資料
     * @return 包含響應資料的 Mono ResponseEntity，HTTP 狀態碼根據響應狀態自動設定
     */
    default Mono<ResponseEntity<?>> createResponseEntity(ApiResponseDTO<?> apiResponse) {
        int responseCode = apiResponse.getStatus() == 200 ? 200 : 400;
        return createResponseEntity(apiResponse, responseCode);
    }


    /**
     * 建立指定 HTTP 狀態碼的 ResponseEntity 響應實體。
     * 此方法允許明確指定 HTTP 狀態碼，適用於需要返回特定狀態碼的場景，
     * 例如：201 Created、204 No Content、301 Moved Permanently 等。
     *
     * <p>當 responseCode 參數為 null 時，預設使用 HTTP 200 OK 狀態碼。
     * 此方法提供最大的彈性，可滿足 RESTful API 設計中的各種狀態碼需求。
     *
     * @param apiResponse API 響應資料傳輸物件，包含業務層面的狀態和資料
     * @param responseCode HTTP 狀態碼（標準三位數格式），為 null 時預設為 200
     * @return 包含響應資料的 Mono ResponseEntity，使用指定的 HTTP 狀態碼
     */
    default Mono<ResponseEntity<?>> createResponseEntity(ApiResponseDTO<?> apiResponse, Integer responseCode) {
        return Mono.just(ResponseEntity.status(Objects.requireNonNullElse(responseCode, 200)).body(apiResponse));
    }


    /**
     * 建立包含自訂標頭的 ResponseEntity 響應實體。
     * 此方法使用 ApiResponseDTO 內建的狀態碼作為 HTTP 狀態碼，並添加指定的 HTTP 標頭資訊。
     * 適用於需要設定快取控制、內容類型、跨域資源共享等特殊標頭的響應場景。
     *
     * <p>常用標頭設定範例：
     * <ul>
     *   <li>Cache-Control: 控制快取行為</li>
     *   <li>Content-Disposition: 檔案下載時的檔案名稱設定</li>
     *   <li>Access-Control-Allow-Origin: CORS 跨域設定</li>
     *   <li>Location: 重新導向位置</li>
     * </ul>
     *
     * @param apiResponse API 響應資料傳輸物件，其狀態碼將用作 HTTP 狀態碼
     * @param headers HTTP 標頭資訊的多值映射，支援多個值的標頭設定
     * @return 包含響應資料和自訂標頭的 Mono ResponseEntity
     */
    default Mono<ResponseEntity<?>> createResponseEntity(ApiResponseDTO<?> apiResponse, MultiValueMap<String, String> headers) {
        return Mono.just(new ResponseEntity<>(apiResponse, headers, apiResponse.getStatus()));
    }


    /**
     * 建立指定路徑的 API 響應資料傳輸物件。
     * 此方法適用於無法從請求上下文中自動提取路徑資訊的場景，
     * 例如批次處理、排程任務或內部服務呼叫等情況。
     *
     * <p>手動指定路徑有助於在日誌追蹤和錯誤分析時定位問題來源，
     * 特別是在複雜的微服務架構中進行跨服務呼叫時。
     *
     * @param path 請求路徑，通常為 API 端點的 URI 路徑部分
     * @param status HTTP 狀態碼，用於表示操作結果
     * @param message 響應訊息，提供操作結果的描述資訊
     * @param data 響應資料，可為任意類型的業務資料
     * @param <T> 響應資料的泛型類型
     * @return 包含完整上下文資訊的 API 響應資料傳輸物件
     */
    default <T> ApiResponseDTO<T> createApiResponse(String path, int status, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), status, path, message, data);
    }


    /**
     * 建立 WebSocket 請求的 API 響應資料傳輸物件。
     * 自動從 WebSocket 會話中提取路徑資訊。
     *
     * @param session WebSocket 會話物件
     * @param status HTTP 狀態碼
     * @param message 響應訊息
     * @param data 響應資料
     * @param <T> 響應資料的泛型類型
     * @return API 響應資料傳輸物件
     */
    default <T> ApiResponseDTO<T> createApiResponse(WebSocketSession session, int status, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), status, session.getHandshakeInfo().getUri().getPath(), message, data);
    }


    /**
     * 建立成功響應的 API 資料傳輸物件，預設狀態碼為 200。
     * 自動從伺服器端請求交換物件中提取路徑資訊。
     *
     * @param request 伺服器端請求交換物件
     * @param message 響應訊息
     * @param data 響應資料
     * @param <T> 響應資料的泛型類型
     * @return API 響應資料傳輸物件
     */
    default <T> ApiResponseDTO<T> createApiResponse(ServerWebExchange request, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), 200, request.getRequest().getURI().getPath(), message, data);
    }


    /**
     * 建立指定路徑的成功響應 API 資料傳輸物件，預設狀態碼為 200。
     * 適用於需要明確指定請求路徑的成功響應場景。
     *
     * @param path 請求路徑
     * @param message 響應訊息
     * @param data 響應資料
     * @param <T> 響應資料的泛型類型
     * @return API 響應資料傳輸物件
     */
    default <T> ApiResponseDTO<T> createApiResponse(String path, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), 200, path, message, data);
    }


    /**
     * 建立包含完整資料的 WebSocket 響應物件。
     * 此方法用於建立包含類型標識、訊息和資料內容的完整 WebSocket 響應，
     * 適用於需要向客戶端推送複雜資料結構的即時通訊場景。
     *
     * <p>WebSocket 響應結構包含：
     * <ul>
     *   <li>timestamp: 自動產生的響應時間戳，用於客戶端排序和除錯</li>
     *   <li>type: 響應類型標識，客戶端可據此進行不同的處理邏輯</li>
     *   <li>message: 人類可讀的響應訊息，通常用於使用者介面顯示</li>
     *   <li>data: 具體的業務資料，可為任意複雜的物件結構</li>
     * </ul>
     *
     * <p>常見的響應類型範例："UPLOAD_PROGRESS"、"FILE_CONVERTED"、"ERROR"、"SUCCESS" 等。
     *
     * @param type 響應類型標識，建議使用有意義的常數或列舉值
     * @param message 響應訊息，提供操作結果的描述資訊
     * @param data 響應資料，包含具體的業務資料內容
     * @param <T> 響應類型標識的泛型類型
     * @return 包含完整資訊的 WebSocket 響應物件
     */
    default <T> WebSocketResponse<T> createWebSocketResponse(T type, String message, Object data) {
        return new WebSocketResponse<>(LocalDateTime.now(), type, message, data);
    }


    /**
     * 建立僅包含類型和訊息的 WebSocket 響應物件。
     * 此方法適用於僅需傳送狀態通知或簡單訊息的 WebSocket 通訊場景，
     * 例如連線確認、操作完成通知、心跳檢測等不需要額外資料的情況。
     *
     * <p>相較於包含資料的響應，此方法建立的響應物件更輕量，
     * 減少網路傳輸負載，適合高頻率的狀態更新或通知場景。
     *
     * <p>典型使用場景：
     * <ul>
     *   <li>連線建立成功通知</li>
     *   <li>檔案操作完成狀態</li>
     *   <li>系統維護通知</li>
     *   <li>使用者權限變更通知</li>
     * </ul>
     *
     * @param type 響應類型標識，用於客戶端識別響應類型
     * @param message 響應訊息，提供具體的狀態或操作描述
     * @param <T> 響應類型標識的泛型類型
     * @return 不包含業務資料的輕量級 WebSocket 響應物件
     */
    default <T> WebSocketResponse<T> createWebSocketResponse(T type, String message) {
        return new WebSocketResponse<>(LocalDateTime.now(), type, message, null);
    }


    /**
     * 統一處理反應式操作中的錯誤異常，提供標準化的錯誤響應格式。
     * 此方法會自動捕獲 ValidationException 和 LimitationException，
     * 並將其轉換為包含適當 HTTP 狀態碼和錯誤訊息的標準響應格式。
     *
     * <p>錯誤處理機制：
     * <ul>
     *   <li>ValidationException: 處理資料驗證錯誤，通常返回 400 Bad Request</li>
     *   <li>LimitationException: 處理限制相關錯誤，如速率限制、容量限制等</li>
     *   <li>自動提取錯誤碼和 HTTP 狀態碼，確保回應格式一致</li>
     *   <li>整合請求上下文資訊，便於錯誤追蹤和除錯</li>
     * </ul>
     *
     * <p>使用此方法可確保所有 Controller 層的錯誤處理邏輯保持一致，
     * 減少重複的錯誤處理程式碼，提升程式碼維護性。
     *
     * @param operation 要執行的反應式操作，通常為業務邏輯的 Mono 物件
     * @param exchange 伺服器端請求交換物件，用於提取請求上下文資訊
     * @return 處理後的 Mono ResponseEntity，成功時包含業務資料，失敗時包含錯誤資訊
     */
    default Mono<ResponseEntity<?>> handleError(Mono<ResponseEntity<?>> operation, ServerWebExchange exchange) {
        return operation.onErrorResume(ValidationException.class, validationException -> {
                                           String errorMessage = String.format("處理失敗: %s", validationException.getMessage());
                                           ApiResponseDTO<?> apiResponse = createApiResponse(exchange, validationException.getErrorCode().getCode(), errorMessage, null);
                                           return createResponseEntity(apiResponse, validationException.getErrorCode().getHttpStatus().value());
                                       }
        ).onErrorResume(LimitationException.class, limitationException -> {
                            String errorMessage = String.format("限制錯誤: %s", limitationException.getMessage());
                            ApiResponseDTO<?> apiResponse = createApiResponse(exchange, limitationException.getErrorCode().getCode(), errorMessage, null);
                            return createResponseEntity(apiResponse, limitationException.getErrorCode().getHttpStatus().value());
                        }
        );
    }


    /**
     * 建立完整的 API 響應資料傳輸物件，包含自訂狀態碼。
     * 自動從請求物件中提取路徑資訊並添加時間戳。
     *
     * @param request 伺服器端請求交換物件
     * @param status HTTP 狀態碼
     * @param message 響應訊息
     * @param data 響應資料
     * @param <T> 響應資料的泛型類型
     * @return API 響應資料傳輸物件
     */
    default <T> ApiResponseDTO<T> createApiResponse(ServerWebExchange request, int status, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), status, request.getRequest().getURI().getPath(), message, data);
    }


    /**
     * 處理 LimitationException 錯誤並發送標準化的 JSON 錯誤響應。
     * 此方法是 LimitationException 專用的錯誤響應處理器，會自動提取異常中的
     * 錯誤碼和 HTTP 狀態碼，確保限制類錯誤的回應格式一致性。
     *
     * <p>LimitationException 通常用於處理：
     * <ul>
     *   <li>API 呼叫頻率限制</li>
     *   <li>檔案大小或數量限制</li>
     *   <li>使用者權限限制</li>
     *   <li>系統資源限制</li>
     * </ul>
     *
     * @param exchange 伺服器端請求交換物件，用於取得請求上下文和寫入響應
     * @param objectMapper JSON 物件映射器，用於序列化響應物件
     * @param error 限制異常錯誤碼物件，包含錯誤碼和對應的 HTTP 狀態碼
     * @param errorMessage 具體的錯誤訊息，將顯示給使用者
     * @return 表示異步寫入完成的 Mono，寫入失敗時會產生 ProcessException
     */
    default Mono<Void> sendErrorResponse(ServerWebExchange exchange, ObjectMapper objectMapper, LimitationException.ErrorCode error, String errorMessage) {
        return sendErrorResponse(exchange, objectMapper, errorMessage, error.getCode(), error.getHttpStatus());
    }

    /**
     * 建立並發送標準化的錯誤響應到 HTTP 響應流。
     * 此方法是核心的錯誤響應發送機制，將錯誤資訊封裝為 ApiResponseDTO 物件，
     * 並以 JSON 格式直接寫入 HTTP 響應流，同時設定適當的狀態碼和內容類型標頭。
     *
     * <p>響應處理流程：
     * <ol>
     *   <li>檢查響應是否已提交，避免重複設定標頭</li>
     *   <li>建立包含時間戳、路徑、錯誤碼和訊息的 ApiResponseDTO</li>
     *   <li>設定 Content-Type 為 application/json</li>
     *   <li>設定指定的 HTTP 狀態碼</li>
     *   <li>序列化響應物件並寫入響應流</li>
     * </ol>
     *
     * <p>此方法通常在過濾器、處理器或異常處理器中使用，
     * 適用於需要直接控制 HTTP 響應的低階錯誤處理場景。
     *
     * @param exchange 伺服器端請求交換物件，提供請求上下文和響應寫入介面
     * @param objectMapper JSON 物件映射器，用於將響應物件序列化為 JSON
     * @param errorMessage 錯誤訊息，將包含在響應中供客戶端顯示
     * @param errorCode 應用層級的錯誤代碼，用於客戶端進行特定錯誤處理
     * @param httpStatus HTTP 狀態碼，表示錯誤的性質和嚴重程度
     * @return 表示異步寫入完成的 Mono，JSON 序列化失敗時會產生 ProcessException
     */
    default Mono<Void> sendErrorResponse(ServerWebExchange exchange, ObjectMapper objectMapper, String errorMessage, int errorCode, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            LogUnity.warn(exchange, "響應已提交，無法再次設定狀態碼或頭部，錯誤碼: %s, 訊息: %s", errorCode, errorMessage);
            return Mono.empty();
        }

        try {
            ApiResponseDTO<?> apiResponseDTO = ApiResponseDTO
                    .builder()
                    .timestamp(LocalDateTime.now())
                    .path(exchange.getRequest().getPath().value())
                    .message(errorMessage)
                    .status(errorCode)
                    .build();

            response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.setStatusCode(httpStatus);

            Mono<DataBuffer> responseBody = Mono.just(exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(apiResponseDTO)));
            return response.writeWith(responseBody);
        } catch (JsonProcessingException ex) {
            LogUnity.error(exchange, "資料轉換 JSON 格式失敗", ex);
            return Mono.error(new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, ex));
        }
    }

    /**
     * 處理 ValidationException 錯誤並發送參數化的錯誤響應。
     * 此方法專門處理資料驗證錯誤，支援 printf 風格的訊息格式化，
     * 可動態替換錯誤訊息中的參數，提供更具體和有用的錯誤資訊。
     *
     * <p>ValidationException 常見於：
     * <ul>
     *   <li>請求參數驗證失敗</li>
     *   <li>業務規則驗證不通過</li>
     *   <li>資料格式不符合要求</li>
     *   <li>必填欄位缺失或無效</li>
     * </ul>
     *
     * <p>訊息格式化範例：
     * <pre>{@code
     * // 錯誤訊息模板："檔案大小 %d MB 超過限制 %d MB"
     * // 格式化參數：[50, 10]
     * // 最終訊息："檔案大小 50 MB 超過限制 10 MB"
     * }</pre>
     *
     * @param exchange 伺服器端請求交換物件，提供請求上下文資訊
     * @param objectMapper JSON 物件映射器，用於序列化錯誤響應
     * @param error 驗證異常錯誤碼物件，包含錯誤模板和狀態碼
     * @param args 錯誤訊息格式化參數陣列，用於替換訊息模板中的佔位符
     * @return 表示異步寫入完成的 Mono，處理失敗時會產生相應異常
     */
    default Mono<Void> sendErrorResponse(ServerWebExchange exchange, ObjectMapper objectMapper, ValidationException.ErrorCode error, Object... args) {
        return sendErrorResponse(exchange, objectMapper, String.format(error.getMessage(), args), error.getCode(), error.getHttpStatus());
    }
}