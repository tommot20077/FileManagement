package xyz.dowob.filemanagement.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 驗證相關異常，當驗證相關的異常發生時，將拋出此異常
 *
 * @author yuan
 * @program File-Management
 * @ClassName ValidationException
 * @description
 * @create 2024-09-16 00:40
 * @Version 1.0
 **/

@Getter
public class ValidationException extends Exception {
    /**
     * 錯誤碼
     */
    private final ErrorCode errorCode;

    public ValidationException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
    }

    /**
     * 內部類，自定義錯誤碼、Http狀態碼以及錯誤信息
     */
    @Getter
    public enum ErrorCode {
        /**
         * 錯誤碼: 1101
         * HTTP狀態碼: 400
         * 錯誤信息: 傳輸數據不能為空
         */
        NULL_DTO(1101, "傳輸數據不能為空"),
        /**
         * 錯誤碼: 1102
         * HTTP狀態碼: 400
         * 錯誤信息: 此用戶名稱不可用
         */
        USERNAME_INVALID(1102, "此用户名稱不可用: %s"),
        /**
         * 錯誤碼: 1103
         * HTTP狀態碼: 409
         * 錯誤信息: 此信箱已經被註冊
         */
        EMAIL_ALREADY_EXISTS(1103, HttpStatus.CONFLICT, "此信箱已經被註冊: %s"),
        /**
         * 錯誤碼: 1104
         * HTTP狀態碼: 404
         * 錯誤信息: 用戶不存在
         */
        USER_NOT_FOUND(1104, HttpStatus.NOT_FOUND, "此用戶不存在: %s"),
        /**
         * 錯誤碼: 1105
         * HTTP狀態碼: 401
         * 錯誤信息: 用戶名或密碼錯誤
         */
        USERNAME_OR_PASSWORD_ERROR(1105, HttpStatus.UNAUTHORIZED, "用户名或密碼錯誤"),
        /**
         * 錯誤碼: 1106
         * HTTP狀態碼: 400
         * 錯誤信息: 密碼不一致
         */
        CONFIRM_PASSWORD_NOT_MATCH(1106, "密碼不一致"),
        /**
         * 錯誤碼: 1107
         * HTTP狀態碼: 400
         * 錯誤信息: 密碼強度不足
         */
        PASSWORD_IS_NOT_STRONG_ENOUGH(1107, "密碼強度不足"),
        /**
         * 錯誤碼: 1108
         * HTTP狀態碼: 401
         * 錯誤信息: JWT 驗證令牌無效
         */
        JWT_TOKEN_INVALID(1108, HttpStatus.UNAUTHORIZED, "JWT 驗證令牌無效"),
        /**
         * 錯誤碼: 1109
         * HTTP狀態碼: 400
         * 錯誤信息: 驗證碼錯誤
         */
        VERIFICATION_CODE_ERROR(1109, "驗證碼錯誤"),
        /**
         * 錯誤碼: 1110
         * HTTP狀態碼: 401
         * 錯誤信息: 帳號驗證失敗
         */
        AUTHENTICATION_FAILED(1110, HttpStatus.UNAUTHORIZED, "驗證身分失敗"),
        /**
         * 錯誤碼: 1111
         * HTTP狀態碼: 400
         * 錯誤信息: 請求參數無效
         */
        REQUEST_IS_INVALID(1111, "請求參數無效: %s"),
        /**
         * 錯誤碼: 1112
         * HTTP狀態碼: 409
         * 錯誤信息: 已有相同文件正在上傳
         */
        EXISTING_TRANSFER_TASK(1112, HttpStatus.CONFLICT, "已有相同文件正在上傳，MD5: %s, 現有任務ID: %s"),
        /**
         * 錯誤碼: 1113
         * HTTP狀態碼: 404
         * 錯誤信息: 用戶文件不存在
         */
        NOT_EXISTING_USER_FILE(1113, HttpStatus.NOT_FOUND, "用戶文件不存在，檔案ID: %s"),
        /**
         * 錯誤碼: 1114
         * HTTP狀態碼: 403
         * 錯誤信息: 您沒有權限訪問此文件
         */
        FILE_PERMISSION_DENIED(1114, HttpStatus.FORBIDDEN, "您沒有權限訪問此文件，檔案ID: %s"),
        /**
         * 錯誤碼: 1115
         * HTTP狀態碼: 400
         * 錯誤信息: 文件名稱無效
         */
        INVALID_FILE_NAME(1115, "文件名稱無效，文件名稱不能為空或無副檔名或含有特殊字元"),
        /**
         * 錯誤碼: 1116
         * HTTP狀態碼: 400
         * 錯誤信息: 文件名稱過長
         */
        NAME_TOO_LONG(1116, "名稱過長，不能超過 200 字元"),
        /**
         * 錯誤碼: 1117
         * HTTP狀態碼: 400
         * 錯誤信息: 文件夾名稱無效
         */
        INVALID_FOLDER_NAME(1117, "文件夾名稱無效，文件夾名稱不能為空或含有特殊字元"),
        /**
         * 錯誤碼: 1118
         * HTTP狀態碼: 400
         * 錯誤信息: 文件夾ID不是文件夾
         */
        THIS_OBJECT_ID_NOT_FOLDER(1118, "檔案ID: %s 不是文件夾"),
        /**
         * 錯誤碼: 1119
         * HTTP狀態碼: 400
         * 錯誤信息: 文件ID不是文件
         */
        THIS_OBJECT_ID_NOT_FILE(1119, "檔案ID: %s 不是文件"),
        /**
         * 錯誤碼: 1120
         * HTTP狀態碼: 400
         * 錯誤信息: 欄位不能為空
         */
        BLANK_FIELD(1120, "欄位不能為空: %s"),
        /**
         * 錯誤碼: 1121
         * HTTP狀態碼: 404
         * 錯誤信息: 欄位不存在
         */
        COLUMN_NOT_FOUND(1121, HttpStatus.NOT_FOUND, "欄位不存在: %s"),
        /**
         * 錯誤碼: 1122
         * HTTP狀態碼: 400
         * 錯誤信息: 不能移動到自身子目錄下
         */
        MOVE_TO_CHILD_FOLDER(1122, "不能移動到自身或其子目錄下"),
        /**
         * 錯誤碼: 1123
         * HTTP狀態碼: 400
         * 錯誤信息: 存儲限制超出
         */
        STORAGE_LIMIT_EXCEEDED(1123, "存儲限制超出， 存儲限制: %s, 已使用存儲: %s, 當前檔案大小: %s"),
        /**
         * 錯誤碼: 1124
         * HTTP狀態碼: 401
         * 錯誤信息: 未授權操作
         */
        UNAUTHORIZED(1124, HttpStatus.UNAUTHORIZED, "未授權操作，請先登入"),
        /**
         * 錯誤碼: 1125
         * HTTP狀態碼: 403
         * 錯誤信息: 權限不足
         */
        FORBIDDEN(1125, HttpStatus.FORBIDDEN, "權限不足"),
        /**
         * 錯誤碼: 1126
         * HTTP狀態碼: 400
         * 錯誤信息: WebSocket協議錯誤，請檢查是否包含Sec-WebSocket-Protocol協議與JWT憑證
         */
        WEBSOCKET_PROTOCOL_ERROR(1126, HttpStatus.UNAUTHORIZED, "WebSocket協議錯誤，請檢查是否包含Sec-WebSocket-Protocol協議和JWT憑證"),
        /**
         * 錯誤碼: 1127
         * HTTP狀態碼: 401
         * 錯誤信息: WebSocket連線錯誤
         */
        WEBSOCKET_CONNECTION_ERROR(1127, HttpStatus.UNAUTHORIZED, "WebSocket連線錯誤"),

        ;

        /**
         * 錯誤碼
         */
        private final int code;

        /**
         * HTTP狀態碼
         */
        private final HttpStatus httpStatus;

        /**
         * 錯誤信息
         */
        private final String message;

        /**
         * ErrorCode建構子
         *
         * @param code       錯誤碼
         * @param httpStatus HTTP狀態碼
         * @param message    錯誤信息
         */
        ErrorCode(int code, HttpStatus httpStatus, String message) {
            this.code = code;
            this.httpStatus = httpStatus;
            this.message = message;
        }

        /**
         * ErrorCode建構子，默認HTTP狀態碼為400
         *
         * @param code    錯誤碼
         * @param message 錯誤信息
         */
        ErrorCode(int code, String message) {
            this.code = code;
            this.httpStatus = HttpStatus.BAD_REQUEST;
            this.message = message;
        }
    }
}