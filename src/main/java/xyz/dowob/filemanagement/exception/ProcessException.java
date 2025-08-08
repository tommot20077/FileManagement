package xyz.dowob.filemanagement.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

/**
 * 基於反應式程式設計的系統內部處理異常實現。
 * <p>
 * 此異常專門處理系統內部技術操作失敗的情況，包括檔案系統I/O操作、GridFS資料庫操作、
 * MongoDB與Redis快取操作、第三方服務整合、郵件發送、文件格式轉換等。與{@link ValidationException}
 * 的用戶輸入驗證不同，此類異常反映系統內部元件的處理失敗，通常需要技術人員介入排查和修復。
 * </p>
 * <p>
 * 在Spring WebFlux反應式環境中，此異常完全支援非阻塞處理模式，可透過
 * {@code Mono.error()}和{@code Flux.error()}進行反應式錯誤傳播，保持系統的非阻塞特性。
 * 異常鏈保留機制確保底層技術異常的堆疊追蹤資訊不遺失，便於問題診斷和根本原因分析。
 * </p>
 * <p>
 * 錯誤處理策略整合了智慧重試機制(Retry)、斷路器保護(Circuit Breaker)、
 * 失敗回退(Fallback)等容錯模式，與系統監控、日誌記錄和告警機制緊密整合，
 * 確保服務高可用性和故障快速恢復。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ValidationException
 * @see LimitationException
 * @see reactor.core.publisher.Mono#error(Throwable)
 * @see reactor.core.publisher.Flux#error(Throwable)
 * @see reactor.core.publisher.Mono#retry()
 */
@Getter
public class ProcessException extends Exception {
    /**
     * 系統內部處理失敗的具體錯誤碼，用於分類和識別不同類型的技術性錯誤
     */
    private final ErrorCode errorCode;


    /**
     * 建構系統內部處理異常實例，包含具體的錯誤碼和格式化參數。
     * <p>
     * 此建構函數處理系統內部技術性錯誤，包括檔案系統I/O失敗、GridFS資料庫操作異常、
     * Redis快取錯誤、MongoDB資料操作失敗、第三方服務呼叫失敗(ClamAV掃毒、郵件服務)、
     * JSON轉換異常、WebSocket連線處理錯誤等。錯誤訊息使用{@link String#format(String, Object...)}
     * 進行格式化，支援動態參數插入以提供詳細的技術錯誤上下文和診斷資訊。
     * </p>
     * <p>
     * 在Spring WebFlux反應式環境中，此異常適合透過{@code Mono.error()}或{@code Flux.error()}
     * 進行非阻塞錯誤傳播，保持系統的響應性能和資源利用效率。異常處理時會自動觸發
     * 相應的容錯機制，如重試、回退或斷路器保護。
     * </p>
     *
     * @param errorCode 系統處理錯誤的具體錯誤碼，用於識別技術錯誤類型，不得為null
     * @param args      用於格式化錯誤訊息的參數陣列，提供錯誤的詳細上下文，如檔案ID、任務ID等
     * @throws NullPointerException 當errorCode為null時拋出
     * @see ErrorCode
     * @see String#format(String, Object...)
     * @see reactor.core.publisher.Mono#error(Throwable)
     * @see reactor.core.publisher.Flux#error(Throwable)
     */
    public ProcessException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
    }


    /**
     * 建構帶有原因鏈的系統內部處理異常實例。
     * <p>
     * 此建構函數用於包裝底層技術異常，保留完整的異常鏈和堆疊追蹤資訊，
     * 便於深度問題診斷和根本原因分析。特別適用於處理Java NIO異常、MongoDB連線錯誤、
     * Redis網路異常、檔案系統I/O失敗、JSON解析錯誤等需要保留原始錯誤上下文的場景。
     * </p>
     * <p>
     * 在Spring WebFlux反應式程式設計中，此模式確保異常在{@code Mono}和{@code Flux}的錯誤流中
     * 傳播時不遺失關鍵的診斷資訊，支援適當的錯誤轉換和重試策略，提高故障排查效率。
     * 這對於維護高可用性和快速問題定位至關重要。
     * </p>
     *
     * @param errorCode 系統處理錯誤的具體錯誤碼，用於分類技術錯誤類型，不得為null
     * @param cause     引起此異常的底層原始異常，用於保留完整的異常鏈和堆疊追蹤，可為null
     * @param args      用於格式化錯誤訊息的參數陣列，提供額外的上下文資訊，如操作對象、狀態資訊等
     * @throws NullPointerException 當errorCode為null時拋出
     * @see ErrorCode
     * @see Throwable#getCause()
     * @see reactor.core.publisher.Flux#onErrorMap(Function)
     * @see reactor.core.publisher.Mono#onErrorMap(Function)
     */
    public ProcessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(buildMessage(errorCode, cause, args), cause);
        this.errorCode = errorCode;
    }


    /**
     * 建立包含原因鏈資訊的完整錯誤訊息。
     * <p>
     * 此靜態輔助方法智慧地組合基本錯誤訊息和底層異常資訊，產生包含完整上下文的
     * 結構化錯誤描述。當存在原因異常時，會將其類型、訊息和關鍵堆疊資訊附加到基本訊息後面，
     * 為技術人員提供全面的故障診斷和排查資訊。
     * </p>
     * <p>
     * 訊息格式化策略遵循結構化日誌的最佳實踐，確保即使在高併發的反應式環境中，
     * 也能提供清晰、可解析的錯誤資訊，便於日誌聚合分析、自動化監控和問題追蹤。
     * </p>
     *
     * @param errorCode 系統錯誤碼，用於獲取基本錯誤訊息範本和錯誤分類識別
     * @param cause     引起異常的底層原因，包含堆疊追蹤和錯誤上下文，可為null
     * @param args      用於格式化錯誤訊息的參數陣列，提供操作上下文和狀態資訊
     * @return 組合後的完整錯誤訊息字串，包含格式化的基本訊息和原因鏈資訊
     * @see String#format(String, Object...)
     * @see Throwable#toString()
     */
    private static String buildMessage(ErrorCode errorCode, Throwable cause, Object... args) {
        String baseMessage = String.format(errorCode.getMessage(), args);
        if (cause != null) {
            return baseMessage + "\n原因: " + cause;
        }
        return baseMessage;
    }


    /**
     * 系統內部處理錯誤碼列舉，定義所有技術性操作失敗的錯誤類型。
     * <p>
     * 每個錯誤碼包含唯一數字識別符和支援參數格式化的訊息範本，專門處理系統內部
     * 技術操作失敗。涉及的技術領域包括：檔案系統I/O操作、GridFS資料庫操作、
     * MongoDB與Redis資料訪問、JSON資料轉換、第三方服務整合(ClamAV、SMTP)、
     * WebSocket連線處理、檔案排列樹管理等關鍵技術場景。
     * </p>
     * <p>
     * 錯誤碼編號範圍：1201-1299，與驗證錯誤(1101-1199)和限制錯誤(1301-1399)明確區分，
     * 支援精細化的系統監控、效能分析、錯誤統計和自動化告警機制。
     * 每個錯誤碼都對應特定的技術元件和操作場景，便於穩定性問題定位和效能優化。
     * </p>
     */
    @Getter
    @AllArgsConstructor
    public enum ErrorCode {
        /**
         * 創建檔案流失敗錯誤碼。
         * <p>
         * 錯誤碼: 1201<br>
         * 適用場景: GridFS檔案流創建失敗、資料庫連線異常、檔案系統I/O錯誤
         * </p>
         */
        CREATE_STREAM_FAILED(1201, "創建檔案流失敗"),

        /**
         * 轉換任務MD5不存在錯誤碼。
         * <p>
         * 錯誤碼: 1202<br>
         * 適用場景: 檔案上傳任務查詢失敗、任務狀態不同步、資料庫一致性問題
         * </p>
         */
        NOT_EXISTING_MD5_TRANSFERS_TASK(1202, "當前轉換任務 MD5: %s 不存在"),

        /**
         * 錯誤碼: 1203
         * 錯誤訊息: MD5校驗失敗
         */
        MD5_NOT_MATCH(1203, "MD5校驗失敗"),

        /**
         * 錯誤碼: 1204
         * 錯誤訊息: 伺服器檔案不存在
         */
        USER_HAVE_NOT_EXIST_SERVER_FILE(1204, "使用者擁有不存在於伺服器的檔案，伺服器檔案ID:%s，使用者檔案ID:%s"),

        /**
         * 錯誤碼: 1205
         * 錯誤訊息: 無法獲取 GridFS 檔案
         */
        GRIDFS_FILE_NOT_FOUND(1205, "無法獲取GradFS的檔案，伺服器檔案ID：%s"),

        /**
         * 錯誤碼: 1206
         * 錯誤訊息: 無法獲取檔案流
         */
        CANNOT_GET_FILE_STREAM(1206, "無法獲取檔案流 任務ID：%s"),

        /**
         * 錯誤碼: 1207
         * 錯誤訊息: 無法將資料格式化為JSON
         */
        FORMAT_DATA_TO_JSON_FAILED(1207, "無法將資料格式化為JSON"),

        /**
         * 錯誤碼: 1208
         * 錯誤訊息: 構建檔案樹失敗
         */
        BUILD_FILE_TREE_FAILED(1208, "構建檔案樹失敗: %s"),

        /**
         * 錯誤碼: 1209
         * 錯誤訊息: 檔案大小不匹配
         */
        FILE_SIZE_NOT_MATCH(1209, "檔案大小不匹配"),

        /**
         * 錯誤碼: 1210
         * 錯誤訊息: 計算檔案差異失敗
         */
        CALCULATE_CONTENT_DIFFERENCE_FAILED(1210, "計算檔案差異失敗"),

        /**
         * 錯誤碼: 1211
         * 錯誤訊息: 檔案垃圾桶記錄不存在
         */
        NOT_EXISTING_FILE_TRASH_RECORD(1211, "檔案回收記錄不存在 ID: %s"),

        /**
         * 錯誤碼: 1212
         * 錯誤訊息: 應用差異檔案到內容失敗
         */
        APPLY_PATCH_TO_CONTENT_FAILED(1212, "應用差異檔案到內容失敗"),

        /**
         * 錯誤碼: 1213
         * 錯誤訊息: 線上檔案ID: %s 版本: %s ，存在差異檔案和快照
         */
        EXISTING_DIFF_AND_SNAPSHOT(1213, "線上檔案ID: %s 版本: %s ，存在差異檔案和快照"),

        /**
         * 錯誤碼: 1214
         * 錯誤訊息: 資料夾樹存在循環引用
         */
        FOLDER_TREE_EXISTING_CYCLE(1214, "資料夾樹存在循環引用"),

        /**
         * 錯誤碼: 1215
         * 錯誤訊息: 刪除臨時檔案失敗
         */
        DELETE_TEMP_FILE_FAILED(1215, "刪除臨時檔案失敗，檔案位置: %s"),

        /**
         * 錯誤碼: 1216
         * 錯誤訊息: 創建臨時下載資料夾失敗
         */
        CREATE_TEMP_DOWNLOAD_FOLDER_FAILED(1216, "創建臨時下載資料夾失敗，資料夾位置: %s"),

        /**
         * 錯誤碼: 1217
         * 錯誤訊息: 轉換 JSON 到目標格式失敗
         */
        CONVERT_JSON_TO_TARGET_FAILED(1217, "轉換 JSON 到目標格式 %s 失敗"),

        /**
         * 錯誤碼: 1218
         * 錯誤訊息: 寫入緩存到 Redis 失敗
         */
        WRITE_CACHE_TO_REDIS_FAILED(1218, "寫入緩存到 Redis 失敗"),

        /**
         * 錯誤碼: 1219
         * 錯誤訊息: 發送郵件失敗
         */
        SEND_MAIL_FAILED(1219, "發送郵件失敗"),

        /**
         * 錯誤碼: 1220
         * 錯誤訊息: 解析掃描結果失敗
         */
        RESOLVE_SCAN_FAILED(1220, "解析掃描結果失敗 %s"),

        /**
         * 連接掃描服務器失敗錯誤碼。
         * <p>
         * 錯誤碼: 1221<br>
         * 適用場景: ClamAV防毒服務連接失敗、網路通訊異常、服務不可用
         * </p>
         */
        CONNECT_SCAN_SERVER_FAILED(1221, "連接掃描服務器失敗"),

        /**
         * 錯誤碼: 1222
         * 錯誤訊息: 處理WebSocket連線失敗
         */
        HANDLE_WEBSOCKET_FAILED(1222, "處理WebSocket連線失敗 %s"),

        /**
         * 錯誤碼: 1223
         * 錯誤訊息: 更新資料夾樹失敗
         */
        UPDATE_FOLDER_TREE_FAILED(1223, "更新資料夾樹失敗, 使用者ID: %s, 資料夾ID: %s"),

        /**
         * 錯誤碼: 1224
         * 錯誤訊息: 認證憑證時發生意外錯誤
         */
        AUTHENTICATION_ERROR(1224, "認證憑證時發生意外錯誤"),

        /**
         * 錯誤碼: 1225
         * 錯誤訊息: 檔案檢查失敗
         */
        FILE_CHECK_FAILED(1225, "檔案檢查失敗，任務ID: %s")

        ;


        /**
         * 唯一識別此系統錯誤類型的數字代碼，用於錯誤分類、日誌記錄、監控統計和告警規則配置
         */
        private final int code;

        /**
         * 支援參數格式化的錯誤訊息範本，使用{@link String#format(String, Object...)}處理，
         * 可包含檔案ID、任務ID、操作對象、狀態資訊等技術上下文
         */
        private final String message;

    }
}
