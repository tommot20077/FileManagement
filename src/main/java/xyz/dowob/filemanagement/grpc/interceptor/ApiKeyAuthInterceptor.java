package xyz.dowob.filemanagement.grpc.interceptor;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.unity.LogUnity;

/**
 * gRPC API Key 認證攔截器，提供基於預共享金鑰的服務間認證機制。
 * <p>
 * 此攔截器負責驗證來自 WebDAV 子服務的 gRPC 請求，確保只有持有正確 API Key 的請求能夠存取內部服務。
 * 透過攔截所有 gRPC 呼叫並檢查請求標頭中的 "x-api-key" 值，實現服務間的安全通訊。
 * <p>
 * <strong>安全機制說明：</strong>
 * <ul>
 * <li>使用預共享金鑰（PSK）進行服務間認證</li>
 * <li>API Key 透過 HTTP 標頭 "x-api-key" 傳輸</li>
 * <li>所有驗證失敗的請求將被拒絕，並返回 UNAUTHENTICATED 狀態</li>
 * <li>支援詳細的安全日誌記錄，便於監控和審計</li>
 * </ul>
 * <p>
 * <strong>使用場景：</strong>
 * <ul>
 * <li>WebDAV 子服務與主檔案管理服務的通訊</li>
 * <li>微服務架構中的內部服務認證</li>
 * <li>gRPC 服務的安全存取控制</li>
 * </ul>
 * <p>
 * <strong>配置要求：</strong>
 * 此攔截器僅在 global.webdav.enabled=true 時啟用，並需要正確配置 global.webdav.api-key 屬性。
 * <p>
 * <strong>使用範例：</strong>
 * <pre>
 * # application.yml 配置
 * global:
 *   webdav:
 *     enabled: true
 *     api-key: "your-secure-api-key-here"
 * </pre>
 * <p>
 * <strong>注意事項：</strong>
 * <ul>
 * <li>API Key 應使用高強度隨機字串，建議長度至少 32 位元</li>
 * <li>生產環境中應定期輪換 API Key</li>
 * <li>確保 API Key 在傳輸過程中使用 TLS 加密</li>
 * <li>避免在日誌中記錄實際的 API Key 值</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @see GlobalProperties.WebDav
 * @see ServerInterceptor
 * @since 1.0
 */
@RequiredArgsConstructor
@GrpcGlobalServerInterceptor
@ConditionalOnProperty(prefix = "global.webdav", name = "enabled", havingValue = "true")
public class ApiKeyAuthInterceptor implements ServerInterceptor {

    /**
     * gRPC 請求標頭中 API Key 的中繼資料金鑰常數。
     * <p>
     * 定義用於識別 API Key 的 HTTP 標頭名稱為 "x-api-key"，使用 ASCII 字串編組器進行序列化。
     * 此常數確保標頭名稱的一致性，避免硬編碼字串造成的維護問題。
     */
    private static final Metadata.Key<String> API_KEY_HEADER = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * 全域系統設定屬性，包含 WebDAV 整合相關的配置資訊。
     * <p>
     * 透過此屬性取得 WebDAV 子服務的 API Key 設定，用於驗證來源請求的合法性。
     * 配置來源為 application.yml 中的 global.webdav 區段。
     */
    private final GlobalProperties globalProperties;


    /**
     * 攔截並驗證 gRPC 服務呼叫，執行 API Key 認證流程。
     * <p>
     * 此方法實現 gRPC ServerInterceptor 介面，在每個 gRPC 請求到達實際服務方法前進行攔截和驗證。
     * 驗證流程包括檢查請求標頭中的 API Key 是否存在、是否與預期值匹配，並據此決定是否允許請求繼續執行。
     * <p>
     * <strong>驗證流程：</strong>
     * <ol>
     * <li>從請求標頭中提取 "x-api-key" 值</li>
     * <li>檢查 API Key 是否存在（非 null）</li>
     * <li>比較提供的 API Key 與系統配置的預期值</li>
     * <li>根據驗證結果決定是否繼續處理請求或返回認證失敗</li>
     * </ol>
     * <p>
     * <strong>安全處理：</strong>
     * <ul>
     * <li>驗證失敗時立即關閉連接，返回 UNAUTHENTICATED 狀態</li>
     * <li>記錄安全事件日誌，包含請求來源和失敗原因</li>
     * <li>不在日誌中暴露實際的 API Key 值，保護敏感資訊</li>
     * </ul>
     * <p>
     * <strong>效能考量：</strong>
     * 認證過程使用字串比較操作，對於高頻率請求具有良好的效能表現。
     * 所有驗證操作都在同一執行緒中同步執行，不會產生額外的執行緒切換開銷。
     *
     * @param <ReqT>  gRPC 請求訊息的泛型型別參數
     * @param <RespT> gRPC 回應訊息的泛型型別參數
     * @param call    gRPC 伺服器呼叫物件，包含請求的中繼資料和方法描述資訊，用於存取請求詳情和控制回應行為
     * @param headers gRPC 請求標頭中繼資料，包含客戶端傳送的所有標頭資訊，特別是認證相關的 API Key
     * @param next    下一個處理器，當認證通過時將請求轉發至實際的服務處理邏輯
     *
     * @return ServerCall.Listener 實例，用於監聽和處理後續的請求生命週期事件。
     * 認證失敗時返回空的 Listener；認證成功時返回由 next.startCall() 建立的 Listener
     *
     * @throws SecurityException 當系統配置中缺少預期的 API Key 時拋出（隱式異常，透過 call.close() 處理）
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        LogUnity.debug("gRPC 呼叫方法: %s", methodName);

        String apiKey = headers.get(API_KEY_HEADER);
        String expectedApiKey = globalProperties.getWebdav().getApiKey();

        if (apiKey == null) {
            LogUnity.warn("缺少 API 金鑰的 gRPC 呼叫: %s from: %s", methodName, call.getAuthority());
            call.close(Status.UNAUTHENTICATED.withDescription("缺少 API 令牌"), headers);
            return new ServerCall.Listener<>() {
            };
        }

        if (!apiKey.equals(expectedApiKey)) {
            LogUnity.warn("無效的 API 金鑰的 gRPC 呼叫: %s from: %s", methodName, call.getAuthority());
            call.close(Status.UNAUTHENTICATED.withDescription("錯誤的 API 令牌"), headers);
            return new ServerCall.Listener<>() {
            };
        }

        LogUnity.debug("API 金鑰驗證成功: %s", methodName);
        return next.startCall(call, headers);
    }
}