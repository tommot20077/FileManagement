package xyz.dowob.filemanagement.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 用於定義用戶限制異常類，當用戶操作超出限制時拋出
 * @author yuan
 * @program FileManagement
 * @ClassName LimitionException
 * @create 2025/1/20
 * @Version 1.0
 **/
@Getter
public class LimitationException extends Exception {
    /**
     * 錯誤碼
     */
    private final ErrorCode errorCode;

    /**
     * 錯誤的構造函數
     *
     * @param errorCode 錯誤碼
     * @param args      錯誤信息參數
     */
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
        USER_EXCEED_LIMIT(1301, HttpStatus.TOO_MANY_REQUESTS, "%s"),

        /**
         * 錯誤碼: 1114
         * 錯誤信息: 獲取文件分塊超出限制
         */
        FILE_CHUNK_EXCEED_LIMIT(1302, HttpStatus.TOO_MANY_REQUESTS, "%s"),
        ;

        /**
         * 自定義錯誤碼
         */
        private final int code;

        /**
         * 錯誤碼對應的 HTTP 狀態碼
         */
        private final HttpStatus httpStatus;

        /**
         * 錯誤信息
         */
        private final String message;
    }
}
