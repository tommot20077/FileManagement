package xyz.dowob.filemanagement.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 錯誤類型，自定義一些細部錯誤類型方便後期排查問題，此類用於內部處理過程中的異常
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ProcessException
 * @description
 * @create 2024-12-09 20:22
 * @Version 1.0
 **/
@Getter
public class ProcessException extends Exception {
    /**
     * 錯誤碼
     */
    private final ErrorCode errorCode;

    /**
     * 自定義錯誤類型
     *
     * @param errorCode 錯誤碼
     * @param args      額外補充參數
     */
    public ProcessException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
    }

    @Getter
    @AllArgsConstructor
    public enum ErrorCode {
        /**
         * 錯誤碼: 1201
         * 錯誤信息: 上傳任務不存在
         */
        NOT_EXISTING_UPLOAD_TASK(1201, "上傳任務 ID: %s 不存在"),
        /**
         * 錯誤碼: 1202
         * 錯誤信息: 當前轉換任務MD5不存在
         */
        NOT_EXISTING_MD5_TRANSFERS_TASK(1202, "當前轉換任務 MD5: %s 不存在"),
        /**
         * 錯誤碼: 1203
         * 錯誤信息: MD5校驗失敗
         */
        MD5_NOT_MATCH(1203, "MD5校驗失敗");

        /**
         * 錯誤碼
         */
        private final int code;

        /**
         * 錯誤訊息
         */
        private final String message;

    }
}
