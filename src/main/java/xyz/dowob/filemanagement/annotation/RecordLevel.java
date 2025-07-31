package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.LogLevelEnum;

import java.lang.annotation.*;

/**
 * AOP 日誌記錄級別控制註解。
 * <p>
 * 此註解用於精確控制 AOP 日誌記錄系統的輸出級別，允許開發者為不同的方法、類別或欄位
 * 指定特定的日誌記錄等級。當標記了此註解的程式元素被執行時，日誌記錄系統會根據指定的
 * 級別決定是否記錄該操作的詳細資訊。
 * <p>
 * 支援的應用場景：
 * <p>
 * 方法級別：控制特定方法執行時的日誌記錄級別
 * <p>
 * 類別級別：為整個類別設定預設的日誌記錄級別
 * <p>
 * 欄位級別：控制欄位存取或修改時的日誌記錄級別
 * <p>
 * 日誌級別優先順序：ERROR > WARN > INFO > DEBUG
 * <p>
 * 使用範例：
 * <pre>{@code
 * @RecordLevel(LogLevelEnum.INFO)
 * public void processUserData() {
 *     // 方法實作
 * }
 * 
 * @RecordLevel(LogLevelEnum.WARN)
 * public class CriticalService {
 *     // 類別實作
 * }
 * }</pre>
 * <p>
 * 注意事項：
 * <p>
 * 此註解與 AOP 日誌記錄切面協同工作
 * <p>
 * 如果未指定此註解，系統將使用預設的日誌記錄級別
 * <p>
 * 方法級別的註解會覆蓋類別級別的設定
 *
 * @see xyz.dowob.filemanagement.customenum.LogLevelEnum
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
public @interface RecordLevel {
    /**
     * 指定日誌記錄的級別。
     * <p>
     * 此屬性定義當前程式元素執行時應該使用的日誌記錄級別。日誌記錄系統會根據
     * 此級別決定是否輸出日誌訊息，只有當系統設定的日誌級別等於或低於此設定時，
     * 相關的日誌訊息才會被記錄。
     *
     * @return 日誌記錄級別，使用 {@link xyz.dowob.filemanagement.customenum.LogLevelEnum} 定義的級別
     */
    LogLevelEnum value();
}

