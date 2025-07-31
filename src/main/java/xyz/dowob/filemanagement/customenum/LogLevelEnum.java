package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Level;

/**
 * 日誌級別枚舉類，用於標記日誌的級別。
 *
 * <p>當前使用的日誌框架為 Log4j2，內部使用了 Log4j2 的 Level 類。
 * 在使用 AOP 日誌框架時，會依照這個枚舉類的級別來進行日誌的輸出。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum LogLevelEnum {
    /**
     * TRACE 日誌級別，用於記錄詳細的程式執行路徑和調試資訊。
     */
    TRACE(Level.TRACE),

    /**
     * DEBUG 日誌級別，用於記錄調試資訊和程式執行的詳細內容。
     */
    DEBUG(Level.DEBUG),

    /**
     * INFO 日誌級別，用於記錄一般的系統運行資訊和重要事件。
     */
    INFO(Level.INFO),

    /**
     * WARN 日誌級別，用於記錄警告資訊和潛在的問題。
     */
    WARN(Level.WARN),

    /**
     * ERROR 日誌級別，用於記錄錯誤資訊和例外狀況。
     */
    ERROR(Level.ERROR),

    /**
     * FATAL 日誌級別，用於記錄致命錯誤和系統無法繼續運行的故障。
     */
    FATAL(Level.FATAL),
    ;

    /**
     * Log4j2 日誌級別，對應的 Log4j2 Level 實例。
     */
    private final Level level;
}
