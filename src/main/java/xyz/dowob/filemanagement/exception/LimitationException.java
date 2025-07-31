package xyz.dowob.filemanagement.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 基於速率限制和資源配額的用戶操作限制異常實現。
 * <p>
 * 此異常專門處理用戶超出系統預設限制的情況，包括API請求頻率限制、檔案上傳限制、
 * 併發連線數量限制、登入失敗次數限制等多種場景。異常設計遵循RFC 6585標準，
 * 統一使用HTTP 429 Too Many Requests狀態碼，與Spring WebFlux的非阻塞特性完全相容。
 * </p>
 * <p>
 * 系統採用多層次限流策略確保服務穩定性：Redis分散式限流適用於多節點部署環境，
 * 本地記憶體限流提供高效能保護，使用者級限流防止個別用戶濫用，檔案操作限流控制I/O壓力。
 * 當任何限制被觸發時，會拋出此異常並記錄詳細的限制上下文資訊供後續監控分析。
 * </p>
 * <p>
 * 在WebFlux反應式環境中，此異常透過{@code Mono.error()}或{@code Flux.error()}進行
 * 非阻塞錯誤傳播，保持反應式流的響應特性。全域異常處理器會攔截此異常，
 * 根據具體錯誤碼生成包含限制重置時間的HTTP回應。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see HttpStatus#TOO_MANY_REQUESTS
 * @see reactor.core.publisher.Mono#error(Throwable)
 * @see reactor.core.publisher.Flux#error(Throwable)
 * @see org.springframework.web.reactive.function.server.ServerResponse
 */
@Getter
public class LimitationException extends Exception {
    /**
     * 特定的限制類型錯誤碼，用於識別具體的限制場景和生成相應的HTTP狀態碼
     */
    private final ErrorCode errorCode;

    /**
     * 建構用戶操作限制異常實例，包含具體的錯誤碼和描述參數。
     * <p>
     * 此建構函數建立表示用戶超出系統限制的異常，自動對應HTTP 429 Too Many Requests
     * 狀態碼。錯誤訊息採用{@link String#format(String, Object...)}進行格式化，
     * 支援動態參數插入以提供精確的限制情境描述，包括限制閾值、當前使用量、重置時間等。
     * </p>
     * <p>
     * 在Spring WebFlux反應式程式設計環境中，此異常通常透過{@code Mono.error()}或
     * {@code Flux.error()}方式拋出，保持非阻塞特性。系統會自動記錄限制觸發事件，
     * 包括用戶識別、觸發時間、限制類型等資訊，供後續監控分析和安全審計使用。
     * </p>
     * <p>
     * 異常拋出後，全域異常處理器會產生包含重試建議和限制重置時間的HTTP回應，
     * 幫助客戶端實現智慧重試機制。
     * </p>
     *
     * @param errorCode 具體的限制類型錯誤碼，用於識別觸發的限制規則，不得為null
     * @param args      用於格式化錯誤訊息的參數陣列，提供限制的詳細上下文資訊，如當前限制值、使用量等
     * @throws NullPointerException 當errorCode為null時拋出
     * @see ErrorCode
     * @see HttpStatus#TOO_MANY_REQUESTS
     * @see String#format(String, Object...)
     * @see reactor.core.publisher.Mono#error(Throwable)
     * @see reactor.core.publisher.Flux#error(Throwable)
     */
    public LimitationException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
    }

    /**
     * 限制類型錯誤碼列舉，定義系統中所有速率限制和資源配額相關的錯誤類型。
     * <p>
     * 每個錯誤碼包含唯一數字識別符、對應的HTTP狀態碼和支援格式化的訊息範本，
     * 專門處理用戶超出系統限制的情況。所有錯誤碼統一對應HTTP 429 Too Many Requests狀態碼，
     * 符合RFC 6585規範和RESTful API設計標準。
     * </p>
     * <p>
     * 錯誤碼編號範圍：1301-1399，與驗證錯誤(1101-1199)和處理錯誤(1201-1299)區分，
     * 便於系統監控、錯誤統計分析和自動化告警規則配置。每個錯誤碼都對應特定的限制策略，
     * 支援針對性的限制調整和優化。
     * </p>
     */
    @Getter
    @AllArgsConstructor
    public enum ErrorCode {
        /**
         * 使用者請求頻率超出限制錯誤碼。
         * <p>
         * 錯誤碼: 1301<br>
         * HTTP狀態碼: 429 Too Many Requests<br>
         * 適用場景: API請求頻率限制、登入失敗次數限制、操作頻率控制、搜尋請求限制<br>
         * 限制策略: 支援滑動窗口、固定窗口、Token Bucket等多種演算法
         * </p>
         */
        USER_EXCEED_LIMIT(1301, HttpStatus.TOO_MANY_REQUESTS, "%s"),

        /**
         * 檔案分塊上傳請求超出限制錯誤碼。
         * <p>
         * 錯誤碼: 1302<br>
         * HTTP狀態碼: 429 Too Many Requests<br>
         * 適用場景: 檔案分塊上傳頻率限制、併發上傳數量限制、單個檔案分塊速率控制<br>
         * 限制策略: 基於用戶級別和全域級別的雙重限制保護
         * </p>
         */
        FILE_CHUNK_EXCEED_LIMIT(1302, HttpStatus.TOO_MANY_REQUESTS, "%s"),
        ;

        /**
         * 唯一識別此錯誤類型的數字代碼，用於系統日誌記錄、錯誤追蹤和監控統計分析
         */
        private final int code;

        /**
         * 與此錯誤碼對應的HTTP狀態碼，用於產生符合RESTful標準的HTTP回應，統一使用429狀態碼
         */
        private final HttpStatus httpStatus;

        /**
         * 支援參數格式化的錯誤訊息範本，使用{@link String#format(String, Object...)}進行處理，
         * 可包含限制閾值、當前使用量、重置時間等動態資訊
         */
        private final String message;
    }
}
