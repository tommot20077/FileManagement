package xyz.dowob.filemanagement.config;

import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.unity.ResponseUnity;

/**
 * Spring WebFlux 反應式 Web 設定類，負責全域的 WebFlux 參數調整和優化設定。
 * <p>
 * 此設定類實現了 {@link WebFluxConfigurer} 接口，提供對 WebFlux 核心組件的自訂設定能力。
 * 主要聚焦於 HTTP 訊息編解碼器的設定，以適應檔案管理系統的特殊需求。
 * <p>
 * 此類同時實現了 {@link ResponseUnity} 介面，提供統一的錯誤回應處理功能。
 * <p>
 * 主要功能包括：
 * <ol>
 *   <li>HTTP 訊息編解碼器設定：調整記憶體使用上限以支援大檔案上傳</li>
 *   <li>負載平衡優化：根據檔案上傳需求調整編解碼器參數</li>
 *   <li>記憶體管理：防止超大請求導致的記憶體溢位問題</li>
 * </ol>
 * <p>
 * 設定原則：
 * - 基於 {@link FileProperties} 的檔案上傳參數動態調整
 * - 確保與前端代理伺服器（如 Nginx）的設定一致
 * - 平衡系統效能與資源使用率
 * <p>
 * 此設定確保 WebFlux 可以高效處理檔案上傳下載等高負載操作，
 * 同時保持系統穩定性和可靠性。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class WebFluxConfig implements WebFluxConfigurer, ResponseUnity {
    /**
     * 檔案系統設定屬性實例。
     * <p>
     * 包含檔案上傳、下載、存儲等所有檔案相關的設定參數。
     * 此實例用於從檔案設定中獲取 WebFlux 編解碼器所需的參數。
     */
    private final FileProperties fileProperties;


    /**
     * 建構 WebFlux 設定實例，執行必要的參數驗證。
     * <p>
     * 透過依賴注入接收檔案系統設定屬性，確保設定對象的有效性。
     * 若設定對象為 null，將拋出 NullPointerException 以防止後續操作出現異常。
     * <p>
     * 這種早期驗證的設計遵循了 "Fail Fast" 原則，
     * 能夠在系統啟動階段就發現設定問題，而不是在運行時才暴露。
     *
     * @param fileProperties 檔案系統設定屬性實例，包含上傳參數等設定資訊
     * @throws NullPointerException 當 fileProperties 為 {@code null} 時拋出
     */
    public WebFluxConfig(FileProperties fileProperties) {
        if (fileProperties == null) {
            throw new NullPointerException("FileProperties 不可為空");
        }
        this.fileProperties = fileProperties;
    }


    /**
     * 設定 HTTP 訊息編解碼器，調整記憶體使用上限以支援大檔案操作。
     * <p>
     * 此方法覆寫了 WebFlux 的預設編解碼器設定，主要調整以下參數：
     * <ol>
     *   <li>記憶體緩衝區大小：設定編解碼器在處理請求時可使用的最大記憶體</li>
     *   <li>基於檔案設定動態調整：使用 {@link FileProperties#getUpload()#getPayloadLength()} 的值</li>
     *   <li>平衡效能與資源：高於預設值以支援大檔案，但不會過度消耗記憶體</li>
     * </ol>
     * <p>
     * 記憶體管理特性：
     * - 自動記憶體釋放：請求處理完成後自動清理緩衝區
     * - 背壓支援：當內存不足時自動減緩資料流速度
     * - 流式處理：對於超過緩衝區的資料使用流式處理
     * - 內存監控：防止惡意請求消耗過多系統資源
     * <p>
     * 設定範例：
     * 若 {@code file.upload.payload-length} 設定為 20MB，
     * 則編解碼器將允許單個請求使用最多 20MB 的記憶體緩衝區。
     * <p>
     * 注意事項：
     * - 此設定必須與前端代理伺服器（如 Nginx）的 client_max_body_size 保持一致
     * - 過大的值可能導致記憶體不足，過小則會影響大檔案上傳
     * - 建議在高並發環境中適當調低此值以節省記憶體
     *
     * @param configurer 服務器編解碼器設定器，用於設定編解碼器參數
     * @throws NullPointerException 當 configurer 為 {@code null} 時拋出
     */
    @Override
    public void configureHttpMessageCodecs(@Nonnull ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize((int) fileProperties.getUpload().getPayloadLength().toBytes());
    }
}
