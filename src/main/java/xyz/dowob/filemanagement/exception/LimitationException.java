package xyz.dowob.filemanagement.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName LimitionException
 * @create 2025/1/20
 * @Version 1.0
 **/
@Getter
public class LimitationException extends Exception {
    private final ErrorCode errorCode;

    public LimitationException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
    }

    @Getter
    @AllArgsConstructor
    public enum ErrorCode {
        /**
         * 錯誤碼: 1113
         * 錯誤信息: 用戶限制
         */
        USER_EXCEED_LIMIT(1301, "%s");

        private final int code;

        private final String message;
    }
}
