package xyz.dowob.filemanagement.unity;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.server.ServerWebExchange;
import xyz.dowob.filemanagement.component.filter.ClientIpFilter;
import xyz.dowob.filemanagement.component.handler.CustomWebSocketSession;

import java.util.Objects;
import java.util.Optional;

/**
 * 基於 Log4j2 的統一日誌輸出工具類，提供帶有上下文資訊的結構化日誌記錄功能。
 * 此工具類封裝了 Log4j2 日誌框架，自動整合請求上下文資訊（請求ID、客戶端IP、用戶ID），
 * 提供統一的日誌格式和多級別的日誌輸出支持。
 *
 * <p>針對 Spring WebFlux 反應式架構優化，支援 ServerWebExchange 和 WebSocketSession 物件的
 * 上下文資訊提取。日誌級別檢查機制可避免不必要的字串格式化操作，提升高並發環境下的效能。
 *
 * <p>主要特色：
 * <ul>
 *   <li>支援 TRACE、DEBUG、INFO、WARN、ERROR、FATAL 六個日誌級別</li>
 *   <li>自動整合請求ID、客戶端 IP 和用戶ID 資訊</li>
 *   <li>支援 printf 風格的訊息格式化</li>
 *   <li>提供多種重載方法適應不同使用場景</li>
 *   <li>自動處理異常堆棧追蹤資訊</li>
 *   <li>日誌級別檢查優化，提升運行時效能</li>
 * </ul>
 *
 * <p>日誌格式說明：
 * 每個日誌項目會自動包含以下格式的上下文資訊：
 * <pre>
 * [請求ID: {requestId}] [請求IP: {clientIp}] (用戶ID:{userId}) | {message}
 * </pre>
 * 當請求上下文為 null 時，日誌會標記為伺服器端請求。
 *
 * <p>使用範例：
 * <pre>{@code
 * // Controller 中使用
 * public Mono<ResponseEntity<?>> uploadFile(ServerWebExchange exchange) {
 *     LogUnity.info(exchange, "開始處理檔案上傳請求");
 *     return fileService.upload()
 *         .doOnSuccess(result -> LogUnity.info(exchange, "檔案上傳成功，檔案ID: %s", result.getId()))
 *         .doOnError(error -> LogUnity.error(exchange, "檔案上傳失敗", error));
 * }
 *
 * // WebSocket 中使用
 * @Override
 * public Mono<Void> handle(WebSocketSession session) {
 *     LogUnity.debug(session, "WebSocket 連線建立，會話ID: %s", session.getId());
 *     // ...
 * }
 *
 * // 伺服器端請求使用
 * LogUnity.warn("系統資源使用率過高：%d%%", usagePercentage);
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ServerWebExchange
 * @see WebSocketSession
 * @see CustomWebSocketSession
 */
@Log4j2
public class LogUnity {
    /**
     * 輸出 TRACE 級別的日誌，用於細粒度的程式執行追蹤。
     * TRACE 級別通常用於記錄詳細的程式流程、變數狀態和方法進入/離開資訊，
     * 適用於開發階段的深度除錯和性能分析。
     *
     * <p>此為簡化版本，不包含異常資訊。若需要記錄異常，請使用帶有 Throwable 參數的重載方法。
     *
     * @param exchange ServerWebExchange 物件，提供請求上下文資訊
     * @param message 日誌訊息，支援 printf 風格的格式化字串
     * @param args 訊息格式化參數，用於替換訊息中的佔位符
     */
    public static void trace(ServerWebExchange exchange, @NotNull String message, Object... args) {
        trace(exchange, message, null, args);
    }

    /**
     * 輸出 TRACE 級別的日誌，包含異常堆棧追蹤資訊。
     * 此方法為 TRACE 日誌的完整版本，適用於需要同時記錄異常資訊的場景。
     *
     * <p>如果異常為 null，則僅記錄訊息內容而不包含堆棧追蹤。
     * 日誌訊息會在正式輸出前進行級別檢查，這可避免不必要的字串格式化操作。
     *
     * @param exchange ServerWebExchange 物件，提供 HTTP 請求上下文資訊
     * @param message 日誌訊息，支援 printf 風格的格式化字串
     * @param throwable 異常物件，為 null 時不記錄堆棧追蹤
     * @param args 訊息格式化參數，用於替換訊息中的佔位符
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
     * 格式化日誌訊息，自動整合請求上下文資訊為結構化的日誌前綴。
     * 此方法是所有日誌輸出的核心格式化機制，負責從不同類型的上下文物件中
     * 提取相關資訊並統一格式化。
     *
     * <p>支援的上下文物件類型：
     * <ul>
     *   <li>ServerWebExchange: HTTP 請求上下文，提取請求ID、客戶端 IP 和用戶ID</li>
     *   <li>CustomWebSocketSession: WebSocket 連線上下文，支援相同的資訊提取</li>
     *   <li>null: 伺服器內部請求，標記為 "Server" 來源</li>
     * </ul>
     *
     * <p>格式化結果範例：
     * <pre>
     * [請求ID: req-12345] [請求IP: 192.168.1.100] (用戶ID:1001) | 檔案上傳成功
     * [請求ID: 無] [請求IP: Server] | 系統初始化完成
     * </pre>
     *
     * <p>資訊提取邏輯：
     * <ol>
     *   <li>請求ID: 從屬性中提取，缺失時顯示為 "無"</li>
     *   <li>客戶端 IP: 從 ClientIpFilter 或會話屬性中取得，預設為 "Server"</li>
     *   <li>用戶ID: 僅當用戶已登入且ID不為 0 時顯示</li>
     * </ol>
     *
     * @param info 請求上下文物件，可為 ServerWebExchange、CustomWebSocketSession 或 null
     * @param message 原始日誌訊息，不包含上下文資訊
     * @return 包含完整上下文前綴和原始訊息的格式化日誌內容
     */
    private static String getFormatMessage(@Nullable Object info, String message) {
        String requestId;
        String requestIp;
        String identify = null;

        if (info == null) {
            requestId = "無";
            requestIp = "Server";
        } else {
            String userId = null;
            if (info instanceof ServerWebExchange exchange) {
                userId = exchange.getAttribute("userId");
                requestIp = ClientIpFilter.getClientIpFromExchange(exchange).orElse("Server");
                requestId = exchange.getAttribute("requestId") != null ? exchange.getAttribute("requestId") : "無";

            } else if (info instanceof CustomWebSocketSession session) {
                userId = Optional.ofNullable(session.getUser()).map(user -> user.getId().toString()).orElse(null);
                requestIp = session.getAttribute("clientIp") != null ? session.getAttribute("clientIp").toString() : "Server";
                requestId = session.getAttribute("requestId") != null ? session.getAttribute("requestId").toString() : "無";
            } else {
                requestId = "無";
                requestIp = "Server";
            }
            if (userId != null && !Objects.equals(userId, "0")) {
                identify = "(用戶ID:" + userId + ") ";
            }

        }
        if (identify == null) {
            identify = "";
        }
        return String.format("[請求ID: %s] [請求IP: %s] %s| %s", requestId, requestIp, identify, message);
    }

    /**
     * 輸出伺服器端 TRACE 級別日誌，不包含異常資訊。
     * 此方法適用於伺服器內部的系統初始化、排程任務或其他非請求相關的記錄場景。
     * 日誌會自動標記為伺服器端來源。
     *
     * <p>相較於帶有上下文的方法，此方法的調用開銷更低，適合高頻率的日誌輸出。
     *
     * @param message 日誌訊息，支援 printf 風格的格式化
     * @param args 訊息格式化參數陣列
     */
    public static void trace(@NotNull String message, Object... args) {
        trace((ServerWebExchange) null, message, null, args);
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
        trace((ServerWebExchange) null, message, throwable, args);
    }

    /**
     * 輸出 TRACE 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param session  WebSocketSession 對象
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void trace(WebSocketSession session, @NotNull String message, Object... args) {
        trace(session, message, null, args);
    }

    /**
     * 輸出 TRACE 級別的日誌
     *
     * @param session    WebSocketSession 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void trace(WebSocketSession session, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isTraceEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.trace(getFormatMessage(session, message), throwable);
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
     * 輸出 DEBUG 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void debug(@NotNull String message, Object... args) {
        debug((ServerWebExchange) null, message, null, args);
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
        debug((ServerWebExchange) null, message, throwable, args);
    }

    /**
     * 輸出 DEBUG 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param session  WebSocketSession 對象
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void debug(WebSocketSession session, @NotNull String message, Object... args) {
        debug(session, message, null, args);
    }

    /**
     * 輸出 DEBUG 級別的日誌
     *
     * @param session    WebSocketSession 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void debug(WebSocketSession session, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isDebugEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.debug(getFormatMessage(session, message), throwable);
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
     * 輸出 INFO 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void info(@NotNull String message, Object... args) {
        info((ServerWebExchange) null, message, null, args);
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
        info((ServerWebExchange) null, message, throwable, args);
    }

    /**
     * 輸出 INFO 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param session  WebSocketSession 對象
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void info(WebSocketSession session, @NotNull String message, Object... args) {
        info(session, message, null, args);
    }

    /**
     * 輸出 INFO 級別的日誌
     *
     * @param session    WebSocketSession 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void info(WebSocketSession session, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isInfoEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.info(getFormatMessage(session, message), throwable);
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
     * 輸出 WARN 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void warn(@NotNull String message, Object... args) {
        warn((ServerWebExchange) null, message, null, args);
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
        warn((ServerWebExchange) null, message, throwable, args);
    }

    /**
     * 輸出 WARN 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param session  WebSocketSession 對象
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void warn(WebSocketSession session, @NotNull String message, Object... args) {
        warn(session, message, null, args);
    }

    /**
     * 輸出 WARN 級別的日誌
     *
     * @param session    WebSocketSession 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void warn(WebSocketSession session, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isWarnEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.warn(getFormatMessage(session, message), throwable);
        }
    }

    /**
     * 輸出 ERROR 級別的日誌，用於記錄嚴重錯誤但不影響系統繼續運行的問題。
     * ERROR 級別通常用於記錄業務異常、資料處理錯誤、外部服務呼叫失敗等情況。
     *
     * <p>此為簡化版本，不包含異常堆棧追蹤。若需要記錄異常詳細資訊，
     * 建議使用帶有 Throwable 參數的重載方法。
     *
     * @param exchange ServerWebExchange 物件，提供請求上下文資訊
     * @param message 錯誤訊息，建議包含具體的錯誤描述和影響範圍
     * @param args 訊息格式化參數，用於動態內容的插入
     */
    public static void error(ServerWebExchange exchange, @NotNull String message, Object... args) {
        error(exchange, message, null, args);
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
     * 輸出 ERROR 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void error(@NotNull String message, Object... args) {
        error((ServerWebExchange) null, message, null, args);
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
        error((ServerWebExchange) null, message, throwable, args);
    }

    /**
     * 輸出 ERROR 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param session  WebSocketSession 對象
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void error(WebSocketSession session, @NotNull String message, Object... args) {
        error(session, message, null, args);
    }

    /**
     * 輸出 ERROR 級別的日誌
     *
     * @param session    WebSocketSession 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void error(WebSocketSession session, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isErrorEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.error(getFormatMessage(session, message), throwable);
        }
    }

    /**
     * 輸出 FATAL 級別的日誌，用於記錄導致系統無法繼續運行的致命錯誤。
     * FATAL 級別為最高級別的日誌，通常用於記錄系統崩潰、關鍵資源不可用、
     * 配置錯誤等需要立即人工介入的嚴重問題。
     *
     * <p>此為簡化版本，不包含異常堆棧追蹤。對於致命錯誤，
     * 強烈建議使用帶有 Throwable 參數的重載方法以記錄完整的錯誤堆棧。
     *
     * @param exchange ServerWebExchange 物件，提供請求上下文資訊
     * @param message 致命錯誤訊息，應詳細描述問題的性質和影響
     * @param args 訊息格式化參數，用於提供具體的錯誤詳情
     */
    public static void fatal(ServerWebExchange exchange, @NotNull String message, Object... args) {
        fatal(exchange, message, null, args);
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
     * 輸出 FATAL 級別的日誌
     * 此為重載方法，表示不需要傳入 ServerWebExchange 以及 throwable 參數
     * 將自動設定成為伺服器端請求
     *
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void fatal(@NotNull String message, Object... args) {
        fatal((ServerWebExchange) null, message, null, args);
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
        fatal((ServerWebExchange) null, message, throwable, args);
    }

    /**
     * 輸出 FATAL 級別的日誌
     * 此為重載方法，表示不需要傳入 throwable 參數
     *
     * @param session  WebSocketSession 對象
     * @param message 日誌訊息
     * @param args    日誌訊息的參數
     */
    public static void fatal(WebSocketSession session, @NotNull String message, Object... args) {
        fatal(session, message, null, args);
    }

    /**
     * 輸出 FATAL 級別的日誌
     *
     * @param session    WebSocketSession 對象
     * @param message   日誌訊息
     * @param throwable 異常對象
     * @param args      日誌訊息的參數
     */
    public static void fatal(WebSocketSession session, @NotNull String message, Throwable throwable, Object... args) {
        if (log.isFatalEnabled()) {
            if (args != null && args.length > 0) {
                message = String.format(message, args);
            }
            log.fatal(getFormatMessage(session, message), throwable);
        }
    }
}
