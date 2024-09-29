package xyz.dowob.filemanagement.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
     * 內部類，定義錯誤碼以及錯誤信息
     */
    @Getter
    @AllArgsConstructor
    public enum ErrorCode {
        /**
         * 錯誤碼: 1101
         * 錯誤信息: 傳輸數據不能為空
         */
        NULL_DTO(1101, "傳輸數據不能為空"),
        /**
         * 錯誤碼: 1102
         * 錯誤信息: 此用戶名稱不可用
         */
        USERNAME_INVALID(1102, "此用户名稱不可用: %s"),
        /**
         * 錯誤碼: 1103
         * 錯誤信息: 此信箱已經被註冊
         */
        EMAIL_ALREADY_EXISTS(1103, "此信箱已經被註冊: %s"),
        /**
         * 錯誤碼: 1104
         * 錯誤信息: 用戶不存在
         */
        USER_NOT_FOUND(1104, "此用戶不存在: %s"),
        /**
         * 錯誤碼: 1105
         * 錯誤信息: 用戶名或密碼錯誤
         */
        USERNAME_OR_PASSWORD_ERROR(1105, "用户名或密碼錯誤"),
        /**
         * 錯誤碼: 1106
         * 錯誤信息: 密碼不一致
         */
        CONFIRM_PASSWORD_NOT_MATCH(1106, "密碼不一致"),
        /**
         * 錯誤碼: 1107
         * 錯誤信息: 密碼強度不足
         */
        PASSWORD_IS_NOT_STRONG_ENOUGH(1107, "密碼強度不足"),
        /**
         * 錯誤碼: 1108
         * 錯誤信息: JWT 驗證令牌無效
         */
        JWT_TOKEN_INVALID(1108, "JWT 驗證令牌無效"),
        /**
         * 錯誤碼: 1109
         * 錯誤信息: 驗證碼錯誤
         */
        VERIFICATION_CODE_ERROR(1109, "驗證碼錯誤"),
        /**
         * 錯誤碼: 1110
         * 錯誤信息: 帳號驗證失敗
         */
        AUTHENTICATION_FAILED(1110, "驗證失敗");



        /**
         * 錯誤碼
         */
        private final int code;

        /**
         * 錯誤信息
         */
        private final String message;

    }
}
