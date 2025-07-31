package xyz.dowob.filemanagement.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 基於業務規則和輸入驗證的資料驗證異常實現。
 * <p>
 * 此異常專門處理用戶輸入驗證、權限檢查、業務規則校驗等場景的失敗情況。
 * 與{@link ProcessException}的系統內部錯誤不同，此類異常主要反映用戶操作
 * 不符合業務規則或安全要求，通常需要用戶修正輸入或調整操作行為。
 * </p>
 * <p>
 * 在Spring WebFlux反應式架構中，此異常完全支援非阻塞處理模式，
 * 可透過{@code Mono.error()}和{@code Flux.error()}進行錯誤傳播。
 * 每個錯誤碼都對應適當的HTTP狀態碼，確保RESTful API的回應符合標準。
 * </p>
 * <p>
 * 安全相關的驗證失敗會觸發相應的保護機制，包括請求限流、IP封鎖、
 * 異常行為記錄等，與系統安全監控緊密整合。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ProcessException
 * @see HttpStatus
 * @see reactor.core.publisher.Mono#error(Throwable)
 */

@Getter
public class ValidationException extends Exception {
    /**
     * 業務驗證失敗的具體錯誤碼，包含對應的HTTP狀態碼和格式化訊息範本
     */
    private final ErrorCode errorCode;


    /**
     * 建構資料驗證異常實例，包含具體的錯誤碼和格式化參數。
     * <p>
     * 此建構函數處理用戶輸入驗證、權限檢查、業務規則校驗等場景的失敗情況。
     * 錯誤訊息使用{@link String#format(String, Object...)}進行格式化，
     * 支援動態參數插入以提供精確的驗證失敗上下文。
     * </p>
     * <p>
     * 在反應式環境中，此異常透過{@code Mono.error()}或{@code Flux.error()}
     * 進行非阻塞錯誤傳播，保持系統響應性。安全相關的驗證失敗會觸發
     * 額外的保護機制和監控記錄。
     * </p>
     *
     * @param errorCode 業務驗證錯誤的具體錯誤碼，不得為null
     * @param args      用於格式化錯誤訊息的參數陣列，提供驗證失敗的詳細上下文
     * @throws NullPointerException 當errorCode為null時拋出
     * @see ErrorCode
     * @see String#format(String, Object...)
     * @see reactor.core.publisher.Flux#error(Throwable)
     */
    public ValidationException(ErrorCode errorCode, Object... args) {
        super(errorCode == null ? "錯誤代碼不可為空" : String.format(errorCode.getMessage(), args));
        if (errorCode == null) {
            throw new NullPointerException("錯誤代碼不可為空");
        }
        this.errorCode = errorCode;
    }


    /**
     * 業務驗證錯誤碼列舉，定義系統中所有輸入驗證和業務規則校驗的錯誤類型。
     * <p>
     * 每個錯誤碼包含唯一數字識別符、對應的HTTP狀態碼和格式化訊息範本。
     * 涉及的驗證領域包括：用戶身份認證、JWT憑證驗證、檔案操作權限、輸入格式和長度驗證、
     * 業務邏輯約束、CSRF防護、WebSocket連線驗證、檔案安全掃描等多種關鍵場景。
     * HTTP狀態碼設計嚴格遵循RFC 7231和RESTful API標準，便於前端進行統一的錯誤處理和使用者體驗優化。
     * </p>
     * <p>
     * 錯誤碼編號範圍：1101-1199，與系統內部處理錯誤(1201-1299)和限制錯誤(1301-1399)明確區分，
     * 支援精細化的錯誤分類、安全事件監控、統計分析和自動化威脅情報收集。
     * 每個錯誤碼都對應特定的驗證規則和安全等級，便於實現精確的安全控制和審計追蹤。
     * </p>
     */
    @Getter
    public enum ErrorCode {
        /**
         * 錯誤碼: 1101
         * HTTP狀態碼: 400
         * 錯誤訊息: 傳輸資料不能為空
         */
        NULL_DTO(1101, "傳輸資料不能為空"),

        /**
         * 錯誤碼: 1102
         * HTTP狀態碼: 400
         * 錯誤訊息: 此用戶名稱不可用
         */
        USERNAME_INVALID(1102, "此用户名稱不可用: %s"),

        /**
         * 錯誤碼: 1103
         * HTTP狀態碼: 409
         * 錯誤訊息: 此信箱已經被註冊
         */
        EMAIL_ALREADY_EXISTS(1103, HttpStatus.CONFLICT, "此信箱已經被註冊: %s"),

        /**
         * 錯誤碼: 1104
         * HTTP狀態碼: 404
         * 錯誤訊息: 用戶不存在
         */
        USER_NOT_FOUND(1104, HttpStatus.NOT_FOUND, "此用戶不存在: %s"),

        /**
         * 錯誤碼: 1105
         * HTTP狀態碼: 401
         * 錯誤訊息: 用戶名或密碼錯誤
         */
        USERNAME_OR_PASSWORD_ERROR(1105, HttpStatus.UNAUTHORIZED, "用户名或密碼錯誤"),

        /**
         * 錯誤碼: 1154
         * HTTP狀態碼: 401
         * 錯誤訊息: 登入憑證錯誤
         */
        WRONG_LOGIN_CREDENTIALS(1154, HttpStatus.UNAUTHORIZED, "登入憑證錯誤"),

        /**
         * 錯誤碼: 1106
         * HTTP狀態碼: 400
         * 錯誤訊息: 密碼不一致
         */
        CONFIRM_PASSWORD_NOT_MATCH(1106, "密碼不一致"),

        /**
         * 錯誤碼: 1107
         * HTTP狀態碼: 400
         * 錯誤訊息: 密碼強度不足
         */
        PASSWORD_IS_NOT_STRONG_ENOUGH(1107, "密碼強度不足"),

        /**
         * 錯誤碼: 1108
         * HTTP狀態碼: 401
         * 錯誤訊息: JWT 驗證令牌無效
         */
        JWT_TOKEN_INVALID(1108, HttpStatus.UNAUTHORIZED, "JWT 驗證令牌無效"),

        /**
         * 錯誤碼: 1109
         * HTTP狀態碼: 400
         * 錯誤訊息: 驗證碼錯誤
         */
        VERIFICATION_CODE_ERROR(1109, "驗證碼錯誤"),

        /**
         * 錯誤碼: 1110
         * HTTP狀態碼: 401
         * 錯誤訊息: 帳號驗證失敗
         */
        AUTHENTICATION_FAILED(1110, HttpStatus.UNAUTHORIZED, "驗證身分失敗"),

        /**
         * 錯誤碼: 1111
         * HTTP狀態碼: 400
         * 錯誤訊息: 請求參數無效
         */
        REQUEST_IS_INVALID(1111, "請求參數無效: %s"),

        /**
         * 錯誤碼: 1112
         * HTTP狀態碼: 409
         * 錯誤訊息: 已有相同檔案正在上傳
         */
        EXISTING_TRANSFER_TASK(1112, HttpStatus.CONFLICT, "已有相同檔案正在上傳，MD5: %s, 現有任務ID: %s"),

        /**
         * 錯誤碼: 1113
         * HTTP狀態碼: 404
         * 錯誤訊息: 使用者檔案不存在
         */
        NOT_EXISTING_USER_FILE(1113, HttpStatus.NOT_FOUND, "使用者檔案不存在，檔案ID: %s"),

        /**
         * 錯誤碼: 1114
         * HTTP狀態碼: 403
         * 錯誤訊息: 您沒有權限存取此檔案
         */
        FILE_PERMISSION_DENIED(1114, HttpStatus.FORBIDDEN, "您沒有權限存取此檔案，檔案ID: %s"),

        /**
         * 錯誤碼: 1115
         * HTTP狀態碼: 400
         * 錯誤訊息: 檔案名稱無效
         */
        INVALID_FILE_NAME(1115, "檔案名稱無效，檔案名稱不能為空或無副檔名或含有特殊字元"),

        /**
         * 錯誤碼: 1116
         * HTTP狀態碼: 400
         * 錯誤訊息: 檔案名稱過長
         */
        NAME_TOO_LONG(1116, "名稱過長，不能超過 200 字元"),

        /**
         * 錯誤碼: 1117
         * HTTP狀態碼: 400
         * 錯誤訊息: 資料夾名稱無效
         */
        INVALID_FOLDER_NAME(1117, "資料夾名稱無效，資料夾名稱不能為空或含有特殊字元"),

        /**
         * 錯誤碼: 1118
         * HTTP狀態碼: 400
         * 錯誤訊息: 資料夾ID不是資料夾
         */
        THIS_OBJECT_ID_NOT_FOLDER(1118, "檔案ID: %s 不是資料夾"),

        /**
         * 錯誤碼: 1119
         * HTTP狀態碼: 400
         * 錯誤訊息: 檔案ID不是檔案
         */
        THIS_OBJECT_ID_NOT_FILE(1119, "檔案ID: %s 不是檔案"),

        /**
         * 錯誤碼: 1120
         * HTTP狀態碼: 400
         * 錯誤訊息: 欄位不能為空
         */
        BLANK_FIELD(1120, "欄位不能為空: %s"),

        /**
         * 錯誤碼: 1121
         * HTTP狀態碼: 404
         * 錯誤訊息: 欄位不存在
         */
        COLUMN_NOT_FOUND(1121, HttpStatus.NOT_FOUND, "欄位不存在: %s"),

        /**
         * 錯誤碼: 1122
         * HTTP狀態碼: 400
         * 錯誤訊息: 不能移動到自身子目錄下
         */
        MOVE_TO_CHILD_FOLDER(1122, "不能移動到自身或其子目錄下"),

        /**
         * 錯誤碼: 1123
         * HTTP狀態碼: 400
         * 錯誤訊息: 存儲限制超出
         */
        STORAGE_LIMIT_EXCEEDED(1123, "存儲限制超出， 存儲限制: %s, 已使用存儲: %s, 當前檔案大小: %s"),

        /**
         * 錯誤碼: 1124
         * HTTP狀態碼: 401
         * 錯誤訊息: 未授權操作
         */
        UNAUTHORIZED(1124, HttpStatus.UNAUTHORIZED, "未授權操作，請先登入"),

        /**
         * 錯誤碼: 1125
         * HTTP狀態碼: 403
         * 錯誤訊息: 權限不足
         */
        FORBIDDEN(1125, HttpStatus.FORBIDDEN, "當前沒有權限執行此操作"),

        /**
         * 錯誤碼: 1126
         * HTTP狀態碼: 400
         * 錯誤訊息: WebSocket協議錯誤，請檢查是否包含Sec-WebSocket-Protocol協議
         */
        WEBSOCKET_PROTOCOL_ERROR(1126, HttpStatus.UNAUTHORIZED, "WebSocket協議錯誤，請檢查是否包含Sec-WebSocket-Protocol協議"),

        /**
         * 錯誤碼: 1127
         * HTTP狀態碼: 401
         * 錯誤訊息: WebSocket連線錯誤
         */
        WEBSOCKET_CONNECTION_ERROR(1127, HttpStatus.UNAUTHORIZED, "WebSocket拒絕連線"),

        /**
         * 錯誤碼: 1128
         * HTTP狀態碼: 400
         * 錯誤訊息: JSON解析錯誤
         */
        INVALID_JSON_CONTENT(1128, "無效的 JSON 格式"),

        /**
         * 錯誤碼: 1129
         * HTTP狀態碼: 404
         * 錯誤訊息: 歷程記錄不存在
         */
        NOT_EXISTING_HISTORY_RECORD(1129, HttpStatus.NOT_FOUND, "歷程記錄不存在，版本號: %s"),

        /**
         * 錯誤碼: 1130
         * HTTP狀態碼: 400
         * 錯誤訊息: 字段長度過短
         */
        FIELD_LENGTH_TOO_SHORT(1130, "欄位: %s 字段長度過短，最小長度限制: %s，當前長度: %s"),

        /**
         * 錯誤碼: 1131
         * HTTP狀態碼: 400
         * 錯誤訊息: 字段長度過長
         */
        FIELD_LENGTH_TOO_LONG(1131, "欄位: %s 字段長度過長，最大長度限制: %s，當前長度: %s"),

        /**
         * 錯誤碼: 1132
         * HTTP狀態碼: 400
         * 錯誤訊息: 版本號無效
         */
        INVALID_VERSION_NUMBER(1132, "版本號無效: %s"),

        /**
         * 錯誤碼: 1133
         * HTTP狀態碼: 400
         * 錯誤訊息: 版本鏈錯誤
         */
        INVALID_VERSION_CHAIN(1133, "版本鏈錯誤，當前版本號: %s，上一版本號: %s"),

        /**
         * 錯誤碼: 1134
         * HTTP狀態碼: 400
         * 錯誤訊息: 歷程記錄鏈錯誤
         */
        INVALID_HISTORY_CHAIN(1134, "歷程記錄鏈錯誤，當前檔案ID: %s，歷程記錄ID: %s"),

        /**
         * 錯誤碼: 1135
         * HTTP狀態碼: 400
         * 錯誤訊息: 與上次記錄內容相同
         */
        NO_CHANGE_IN_CONTENT(1135, "與上次記錄內容相同"),

        /**
         * 錯誤碼: 1136
         * HTTP狀態碼: 409
         * 錯誤訊息: 部分檔案已被刪除
         */
        SOME_FILE_ALREADY_DELETED(1136, HttpStatus.CONFLICT, "部分檔案已被刪除，檔案ID: %s"),

        /**
         * 錯誤碼: 1137
         * HTTP狀態碼: 409
         * 錯誤訊息: 檔案未被刪除
         */
        SOME_FILE_NOT_DELETED(1137, HttpStatus.CONFLICT, "部分檔案未刪除無法復原，檔案ID: %s"),

        /**
         * 錯誤碼: 1138
         * HTTP狀態碼: 400
         * 錯誤訊息: 檔案類型與請求路徑不符合
         */
        FILE_TYPE_WITH_WRONG_REQUEST_PATH(1138, "檔案類型與請求路徑不符合，預期類型: %s，實際類型: %s"),

        /**
         * 錯誤碼: 1139
         * HTTP狀態碼: 409
         * 錯誤訊息: 檔案已被刪除
         */
        ALREADY_DELETED_FILE(1139, HttpStatus.CONFLICT, "檔案已被刪除，檔案ID: %s"),

        /**
         * 錯誤碼: 1140
         * HTTP狀態碼: 403
         * 錯誤訊息: 缺少 CSRF 憑證
         */
        MISSING_CSRF_TOKEN(1140, HttpStatus.FORBIDDEN, "缺少 CSRF 憑證"),

        /**
         * 錯誤碼: 1141
         * HTTP狀態碼: 403
         * 錯誤訊息: 無效的 CSRF 憑證
         */
        INVALID_CSRF_TOKEN(1141, HttpStatus.FORBIDDEN, "無效的 CSRF 憑證"),

        /**
         * 錯誤碼: 1142
         * HTTP狀態碼: 400
         * 錯誤訊息: 搜索條件不能為空
         */
        SEARCH_CRITERIA_EMPTY(1142, "搜索條件不能為空"),

        /**
         * 錯誤碼: 1143
         * HTTP狀態碼: 400
         * 錯誤訊息: 無效的搜索條件
         */
        INVALID_SEARCH_CRITERIA(1143, "無效的搜索條件: %s"),

        /**
         * 錯誤碼: 1144
         * HTTP狀態碼: 400
         * 錯誤訊息: 關鍵字過長
         */
        KEYWORD_TOO_LONG(1144, "關鍵字過長，不能超過 50 字元"),

        /**
         * 錯誤碼: 1145
         * HTTP狀態碼: 400
         * 錯誤訊息: 關鍵字過短
         */
        KEYWORD_TOO_SHORT(1145, "關鍵字過短，不能少於 2 字元"),

        /**
         * 錯誤碼: 1146
         * HTTP狀態碼: 403
         * 錯誤訊息: CSRF 憑證無效，請求來源不合法
         */
        CSRF_TOKEN_INVALID_REFERER(1146, HttpStatus.FORBIDDEN, "禁止請求CSRF 憑證，請求來源不被允許"),

        /**
         * 錯誤碼: 1147
         * HTTP狀態碼: 400
         * 錯誤訊息: 不支持的操作
         */
        UNSUPPORTED_OPERATION(1147, "不支持的操作"),

        /**
         * 錯誤碼: 1148
         * HTTP狀態碼: 400
         * 錯誤訊息: 超過最大資料夾深度限制
         */
        EXCEED_MAX_FOLDER_DEPTH(1148, "超過最大資料夾深度限制: %s，預計更新深度: %s"),

        /**
         * 錯誤碼: 1149
         * HTTP狀態碼: 404
         * 錯誤訊息: 上傳任務不存在
         */
        NOT_EXISTING_UPLOAD_TASK(1149, HttpStatus.NOT_FOUND, "上傳任務不存在，任務ID: %s"),

        /**
         * 錯誤碼: 1150
         * HTTP狀態碼: 400
         * 錯誤訊息: 檔案大小超過限制
         */
        FILE_SIZE_LIMIT(1150, HttpStatus.BAD_REQUEST, "檔案大小超過限制，檔案大小: %s，限制大小: %s"),

        /**
         * 錯誤碼: 1151
         * HTTP狀態碼: 404
         * 錯誤訊息: 路徑不存在
         */
        PATH_NOT_FOUND(1151, HttpStatus.NOT_FOUND, "請求路徑不存在"),

        /**
         * 錯誤碼: 1152
         * HTTP狀態碼: 403
         * 錯誤訊息: IP 地址已被禁止訪問
         */
        ALREADY_BAN_IP(1152, HttpStatus.FORBIDDEN, "此 IP 地址已被暫時禁止訪問"),


        /**
         * 檔案包含病毒檢測錯誤碼。
         * <p>
         * 錯誤碼: 1153<br>
         * HTTP狀態碼: 400 Bad Request<br>
         * 適用場景: ClamAV掃毒引擎檢測到惡意軟體、病毒或其他安全威脅
         * </p>
         */
        FILE_VIRUS_DETECTED(1153, HttpStatus.BAD_REQUEST, "檔案包含病毒，任務ID: %s"),
        ;


        /**
         * 唯一識別此業務驗證錯誤類型的數字代碼，用於錯誤分類、系統監控和安全審計
         */
        private final int code;

        /**
         * 與此驗證錯誤對應的HTTP狀態碼，用於產生符合RFC 7231和RESTful標準的API回應
         */
        private final HttpStatus httpStatus;

        /**
         * 支援參數格式化的錯誤訊息範本，使用{@link String#format(String, Object...)}處理，
         * 可包含輸入值、限制闾值、用戶識別、檔案ID等上下文資訊
         */
        private final String message;


        /**
         * 建構業務驗證錯誤碼列舉值，包含完整的錯誤資訊。
         * <p>
         * 建立包含錯誤碼、HTTP狀態碼和訊息範本的完整錯誤定義，
         * 用於統一的業務驗證錯誤處理和API回應生成。
         * </p>
         *
         * @param code       唯一識別此驗證錯誤類型的數字代碼
         * @param httpStatus 對應的RESTful HTTP狀態碼
         * @param message    支援參數格式化的錯誤訊息範本
         * @see HttpStatus
         * @see String#format(String, Object...)
         */
        ErrorCode(int code, HttpStatus httpStatus, String message) {
            this.code = code;
            this.httpStatus = httpStatus;
            this.message = message;
        }


        /**
         * 建構 ErrorCode 列舉值，使用預設 HTTP 狀態碼。
         * <p>
         * 這個建構函數使用預設的 HTTP 400 Bad Request 狀態碼，
         * 適用於大部分用戶輸入驗證錯誤的情況。
         * </p>
         *
         * @param code    自定義錯誤碼，用於唯一識別錯誤類型
         * @param message 錯誤訊息範本，支援參數格式化
         */
        ErrorCode(int code, String message) {
            this.code = code;
            this.httpStatus = HttpStatus.BAD_REQUEST;
            this.message = message;
        }


        /**
         * 根據錯誤名稱獲取對應的錯誤碼列舉值。
         * <p>
         * 此方法不區分大小寫，方便查詢操作。當指定的錯誤名稱
         * 不存在於錯誤碼列舉中時，回傳 null。
         * </p>
         * <p>
         * 這個方法主要用於設定檔案或動態錯誤處理場景，
         * 但建議直接使用列舉常數以獲得更好的編譯時檢查。
         * </p>
         *
         * @param errorName 錯誤名稱，不區分大小寫
         * @return 對應的 ErrorCode 列舉值，找不到時回傳 null
         */
        public static ErrorCode fromName(String errorName) {
            String upperCaseErrorName = errorName.toUpperCase();
            for (ErrorCode errorCode : ErrorCode.values()) {
                if (errorCode.name().equals(upperCaseErrorName)) {
                    return errorCode;
                }
            }
            return null;
        }
    }
}