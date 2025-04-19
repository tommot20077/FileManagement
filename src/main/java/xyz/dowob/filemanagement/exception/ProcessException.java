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


    /**
     * 自定義錯誤類型，此為帶有異常原因的錯誤類型
     *
     * @param errorCode 錯誤碼
     * @param cause     異常原因
     * @param args      額外補充參數
     */
    public ProcessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(buildMessage(errorCode, cause, args), cause);
        this.errorCode = errorCode;
    }


    /**
     * 建立錯誤訊息
     *
     * @param errorCode 錯誤碼
     * @param cause     異常原因
     * @param args      額外補充參數
     */
    private static String buildMessage(ErrorCode errorCode, Throwable cause, Object... args) {
        String baseMessage = String.format(errorCode.getMessage(), args);
        if (cause != null) {
            return baseMessage + "\n原因: " + cause;
        }
        return baseMessage;
    }

    @Getter
    @AllArgsConstructor
    public enum ErrorCode {
        /**
         * 錯誤碼: 1201
         * 錯誤信息: 創建檔案流失敗
         */
        CREATE_STREAM_FAILED(1218, "創建檔案流失敗"),

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

        /**
         * 錯誤碼: 1209
         * 錯誤信息: 文件大小不匹配
         */
        FILE_SIZE_NOT_MATCH(1209, "檔案大小不匹配"),

        /**
         * 錯誤碼: 1210
         * 錯誤信息: 計算文件差異失敗
         */
        CALCULATE_CONTENT_DIFFERENCE_FAILED(1210, "計算文件差異失敗"),

        /**
         * 錯誤碼: 1211
         * 錯誤信息: 文件垃圾桶記錄不存在
         */
        NOT_EXISTING_FILE_TRASH_RECORD(1211, "文件回收記錄不存在 ID: %s"),

        /**
         * 錯誤碼: 1212
         * 錯誤信息: 應用差異文件到內容失敗
         */
        APPLY_PATCH_TO_CONTENT_FAILED(1212, "應用差異文件到內容失敗"),

        /**
         * 錯誤碼: 1213
         * 錯誤信息: 線上檔案ID: %s 版本: %s ，存在差異文件和快照
         */
        EXISTING_DIFF_AND_SNAPSHOT(1213, "線上檔案ID: %s 版本: %s ，存在差異文件和快照"),

        /**
         * 錯誤碼: 1214
         * 錯誤信息: 文件夾樹存在循環引用
         */
        FOLDER_TREE_EXISTING_CYCLE(1214, "文件夾樹存在循環引用"),

        /**
         * 錯誤碼: 1215
         * 錯誤信息: 刪除臨時檔案失敗
         */
        DELETE_TEMP_FILE_FAILED(1215, "刪除臨時檔案失敗，檔案位置: %s"),

        /**
         * 錯誤碼: 1216
         * 錯誤信息: 創建臨時下載資料夾失敗
         */
        CREATE_TEMP_DOWNLOAD_FOLDER_FAILED(1216, "創建臨時下載資料夾失敗，資料夾位置: %s"),

        /**
         * 錯誤碼: 1217
         * 錯誤信息: 轉換 JSON 到目標格式失敗
         */
        CONVERT_JSON_TO_TARGET_FAILED(1217, "轉換 JSON 到目標格式 %s 失敗"),

        /**
         * 錯誤碼: 1218
         * 錯誤信息: 寫入緩存到 Redis 失敗
         */
        WRITE_CACHE_TO_REDIS_FAILED(1218, "寫入緩存到 Redis 失敗")

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
