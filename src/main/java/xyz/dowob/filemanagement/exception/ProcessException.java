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
        MD5_NOT_MATCH(1203, "MD5校驗失敗"),
        /**
         * 錯誤碼: 1204
         * 錯誤信息: 伺服器文件不存在
         */
        USER_HAVE_NOT_EXIST_SERVER_FILE(1204, "用戶擁有不存在於伺服器的文件，伺服器檔案ID:%s，用戶檔案ID:%s"),
        /**
         * 錯誤碼: 1205
         * 錯誤信息:
         */
        GRIDFS_FILE_NOT_FOUND(1205, "無法獲取GradFS的檔案，伺服器檔案ID：%s"),
        /**
         * 錯誤碼: 1206
         * 錯誤信息: 無法獲取檔案流
         */
        CANNOT_GET_FILE_STREAM(1206, "無法獲取檔案流 任務ID：%s"),
        /**
         * 錯誤碼: 1207
         * 錯誤信息: 無法將數據格式化為JSON
         */
        FORMAT_DATA_TO_JSON_FAILED(1207, "無法將數據格式化為JSON"),
        /**
         * 錯誤碼: 1208
         * 錯誤信息: 構建文件樹失敗
         */
        BUILD_FILE_TREE_FAILED(1208, "構建文件樹失敗: %s"),
        ;



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
