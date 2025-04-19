package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Level;

/**
 * 日誌級別枚舉類，用於標記日誌的級別
 * 當前使用的日誌框架為 log4j2，內部使用了 log4j2 的 Level 類
 * 在使用AOP 日誌框架時，會依照這個枚舉類的級別來進行日誌的輸出
 *
 * @author yuan
 * @program FileManagement
 * @ClassName LogLevel
 * @create 2025/4/18
 * @Version 1.0
 **/
@Getter
@RequiredArgsConstructor
public enum LogLevelEnum {
    /**
     * TRACE 日誌級別
     */
    TRACE(Level.TRACE),

    /**
     * DEBUG 日誌級別
     */
    DEBUG(Level.DEBUG),

    /**
     * INFO 日誌級別
     */
    INFO(Level.INFO),

    /**
     * WARN 日誌級別
     */
    WARN(Level.WARN),

    /**
     * ERROR 日誌級別
     */
    ERROR(Level.ERROR),
    ;

    /**
     * Log4j2 日誌級別
     */
    private final Level level;
}
