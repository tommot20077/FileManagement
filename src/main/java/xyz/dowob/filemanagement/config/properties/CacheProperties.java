package xyz.dowob.filemanagement.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * 緩存設定屬性類，用於統一管理系統中所有緩存相關的設定參數。
 * <p>
 * 此類提供了對於檔案下載流緩存、用戶檔案列表緩存、用戶資訊緩存等各種緩存機制的設定支援。
 * 透過 Spring Boot 的 {@code @ConfigurationProperties} 機制，可在 application.yml 中使用 "cache" 前綴進行設定。
 * <p>
 * 設定範例：
 * <pre>
 * cache:
 *   enable-file-download-stream-cache: true
 *   download-cache-expire-time: 30m
 *   file-list-cache-expire-time: 1h
 *   chunk-size: 5MB
 * </pre>
 * <p>
 * 主要功能包括：
 * - 檔案下載流緩存控制
 * - 用戶檔案列表緩存管理
 * - 用戶資訊緩存設定
 * - 緩存分塊大小設定
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    /**
     * 是否啟用檔案下載流緩存功能。
     * <p>
     * 當啟用時，系統會將檔案下載流暫存在記憶體中以提升下載效能。
     * 注意：啟用此功能會根據用戶存儲的檔案大小佔用大量記憶體，
     * 建議根據伺服器記憶體容量和用戶檔案大小進行合理設定。
     * <p>
     * 預設值：{@code true}
     */
    private Boolean enableFileDownloadStreamCache = true;

    /**
     * 下載流緩存的鍵值前綴。
     * <p>
     * 用於在緩存儲存系統中區分下載流緩存與其他類型的緩存。
     * 所有下載流緩存的鍵值都會以此前綴開頭。
     * <p>
     * 預設值：{@code "download_stream_cache"}
     */
    private String downloadCachePrefix = "download_stream_cache";

    /**
     * 下載流緩存的過期時間。
     * <p>
     * 設定檔案下載流在緩存中的保存時間，超過此時間後緩存會自動失效並被清除。
     * 這有助於釋放記憶體空間並避免長期佔用系統資源。
     * <p>
     * 約束條件：此值必須大於 0，否則會拋出設定錯誤。
     * <p>
     * 預設值：30 分鐘
     */
    private Duration downloadCacheExpireTime = Duration.ofMinutes(30);

    /**
     * 是否啟用用戶檔案列表緩存功能。
     * <p>
     * 當啟用時，系統會緩存用戶的檔案列表資訊，大幅提升檔案列表查詢的響應速度，
     * 特別是對於擁有大量檔案的用戶。建議保持啟用狀態以獲得最佳效能。
     * <p>
     * 預設值：{@code true}
     */
    private Boolean enableUserFileListCache = true;

    /**
     * 檔案列表緩存的過期時間。
     * <p>
     * 設定用戶檔案列表在緩存中的保存時間，超過此時間後緩存會自動失效。
     * 合理的過期時間可以平衡查詢效能與資料一致性。
     * <p>
     * 約束條件：此值必須大於 0，否則會拋出設定錯誤。
     * <p>
     * 預設值：60 分鐘
     */
    private Duration fileListCacheExpireTime = Duration.ofMinutes(60);

    /**
     * 是否啟用用戶資訊緩存功能。
     * <p>
     * 當啟用時，系統會緩存用戶的基本資訊，減少資料庫查詢次數，
     * 提升用戶資訊相關操作的響應速度。
     * <p>
     * 預設值：{@code true}
     */
    private Boolean enableUserInfoCache = true;

    /**
     * 用戶資訊緩存的鍵值前綴。
     * <p>
     * 用於在緩存儲存系統中區分用戶資訊緩存與其他類型的緩存。
     * 所有用戶資訊緩存的鍵值都會以此前綴開頭。
     * <p>
     * 預設值：{@code "user_info_cache"}
     */
    private String userInfoCachePrefix = "user_info_cache";

    /**
     * 用戶資訊緩存的過期時間。
     * <p>
     * 設定用戶資訊在緩存中的保存時間，超過此時間後緩存會自動失效。
     * 由於用戶資訊相對穩定，可以設定較長的過期時間。
     * <p>
     * 約束條件：此值必須大於 0，否則會拋出設定錯誤。
     * <p>
     * 預設值：1 天
     */
    private Duration userInfoCacheExpireTime = Duration.ofDays(1);

    /**
     * 緩存分塊的大小限制。
     * <p>
     * 用於處理大檔案的緩存策略，當檔案大小超過此限制時，
     * 系統會自動將檔案分割成多個較小的區塊進行緩存處理。
     * 這有助於提升大檔案的緩存效率並減少記憶體使用峰值。
     * <p>
     * 預設值：5MB
     */
    private DataSize chunkSize = DataSize.ofMegabytes(5);
}
