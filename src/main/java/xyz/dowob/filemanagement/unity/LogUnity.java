package xyz.dowob.filemanagement.unity;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.server.ServerWebExchange;
import xyz.dowob.filemanagement.component.filter.ClientIpFilter;

import java.util.Objects;

/**
 * 日誌通實現類，裡面封裝了日誌的輸出方法，並提供統一的日誌格式
 * 在這裡使用了 log4j2 作為日誌框架
 * 會在處理傳入訊息之前檢查當前設定的日誌級別，若當前級別不符合則不會輸出，減少不必要的執行操作
 * 對日誌訊息會進行格式化，並添加請求 ID 和請求 IP
 *
 * @author yuan
 * @program FileManagement
 * @ClassName LogUnity
 * @create 2025/4/23
 * @Version 1.0
 **/
@Log4j2
public class LogUnity {

    /**
     * 輸出 TRACE 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param exchange ServerWebExchange 對象
     * @param message  日誌訊息
     * @param args     日誌訊息的參數
     */
    public static void trace(ServerWebExchange exchange, @NotNull String message, Object... args) {
        trace(exchange, message, null, args);
    }


    /**
     * 輸出 TRACE 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void trace(@NotNull String message, Object... args) {
        trace(null, message, null, args);
    }


    /**
     * 輸出 TRACE 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void trace(@NotNull String message, Throwable throwable, Object... args) {
        trace(null, message, throwable, args);
    }


    /**
     * 輸出 TRACE 級別的日誌
     *
     * @param exchange  ServerWebExchange 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void trace(ServerWebExchange exchange, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isTraceEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.trace(getFormatMessage(exchange, message), throwable);
        }
    }


    /**
     * 輸出 DEBUG 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param exchange ServerWebExchange 對象
     * @param message  日誌訊息
     * @param args     日誌訊息的參數
     */
    public static void debug(ServerWebExchange exchange, @NotNull String message, Object... args) {
        debug(exchange, message, null, args);
    }


    /**
     * 輸出 DEBUG 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void debug(@NotNull String message, Object... args) {
        trace(null, message, null, args);
    }


    /**
     * 輸出 DEBUG 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void debug(@NotNull String message, Throwable throwable, Object... args) {
        trace(null, message, throwable, args);
    }


    /**
     * 輸出 DEBUG 級別的日誌
     *
     * @param exchange  ServerWebExchange 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void debug(ServerWebExchange exchange, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isDebugEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.debug(getFormatMessage(exchange, message), throwable);
        }
    }


    /**
     * 輸出 INFO 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param exchange ServerWebExchange 對象
     * @param message  日誌訊息
     * @param args     日誌訊息的參數
     */
    public static void info(ServerWebExchange exchange, @NotNull String message, Object... args) {
        info(exchange, message, null, args);
    }


    /**
     * 輸出 INFO 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void info(@NotNull String message, Object... args) {
        trace(null, message, null, args);
    }


    /**
     * 輸出 INFO 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void info(@NotNull String message, Throwable throwable, Object... args) {
        trace(null, message, throwable, args);
    }


    /**
     * 輸出 INFO 級別的日誌
     *
     * @param exchange  ServerWebExchange 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void info(ServerWebExchange exchange, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isInfoEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.info(getFormatMessage(exchange, message), throwable);
        }
    }


    /**
     * 輸出 WARN 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param exchange ServerWebExchange 對象
     * @param message  日誌訊息
     * @param args     日誌訊息的參數
     */
    public static void warn(ServerWebExchange exchange, @NotNull String message, Object... args) {
        warn(exchange, message, null, args);
    }


    /**
     * 輸出 WARN 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void warn(@NotNull String message, Object... args) {
        trace(null, message, null, args);
    }


    /**
     * 輸出 WARN 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void warn(@NotNull String message, Throwable throwable, Object... args) {
        trace(null, message, throwable, args);
    }


    /**
     * 輸出 WARN 級別的日誌
     *
     * @param exchange  ServerWebExchange 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void warn(ServerWebExchange exchange, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isWarnEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.warn(getFormatMessage(exchange, message), throwable);
        }
    }


    /**
     * 輸出 ERROR 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param exchange ServerWebExchange 對象
     * @param message  日誌訊息
     * @param args     日誌訊息的參數
     */
    public static void error(ServerWebExchange exchange, @NotNull String message, Object... args) {
        error(exchange, message, null, args);
    }


    /**
     * 輸出 ERROR 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void error(@NotNull String message, Object... args) {
        trace(null, message, null, args);
    }


    /**
     * 輸出 ERROR 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void error(@NotNull String message, Throwable throwable, Object... args) {
        trace(null, message, throwable, args);
    }


    /**
     * 輸出 ERROR 級別的日誌
     *
     * @param exchange  ServerWebExchange 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void error(ServerWebExchange exchange, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isErrorEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.error(getFormatMessage(exchange, message), throwable);
        }
    }


    /**
     * 輸出 FATAL 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param exchange ServerWebExchange 對象
     * @param message  日誌訊息
     * @param args     日誌訊息的參數
     */
    public static void fatal(ServerWebExchange exchange, @NotNull String message, Object... args) {
        fatal(exchange, message, null, args);
    }


    /**
     * 輸出 FATAL 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void fatal(@NotNull String message, Object... args) {
        trace(null, message, null, args);
    }


    /**
     * 輸出 FATAL 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void fatal(@NotNull String message, Throwable throwable, Object... args) {
        trace(null, message, throwable, args);
    }


    /**
     * 輸出 FATAL 級別的日誌
     *
     * @param exchange  ServerWebExchange 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void fatal(ServerWebExchange exchange, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isFatalEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.fatal(getFormatMessage(exchange, message), throwable);
        }
    }


    /**
     * 獲取請求ID、請求者名稱和用戶ID
     * 當前請求如果為空，則其為伺服器端請求
     * 如果請求者名稱為空，則其為尚未登錄的用戶請求
     * 如果請求者名稱不為空，則其為用戶請求
     *
     * @param exchange ServerWebExchange 對象
     *
     * @return UserRequestInfo 請求信息
     */
    private static String getFormatMessage(@Nullable ServerWebExchange exchange, String message) {
        String requestId;
        String requestIp;
        String identify = null;

        if (exchange == null) {
            requestId = "無";
            requestIp = "Server";
        } else {
            String userId = exchange.getAttribute("userId");
            requestIp = ClientIpFilter.getClientIpFromExchange(exchange).orElse("Server");
            requestId = exchange.getAttribute("requestId") != null ? exchange.getAttribute("requestId") : "無";
            if (exchange.getAttribute("userId") != null && !Objects.equals(userId, "0")) {
                identify = "(用戶ID:" + userId + ") ";
            }
        }
        if (identify == null) {
            identify = "";
        }
        return String.format("[請求ID: %s] [請求IP: %s] %s| %s", requestId, requestIp, identify, message);
    }
}
