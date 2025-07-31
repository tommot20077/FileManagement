package xyz.dowob.filemanagement.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import xyz.dowob.filemanagement.customenum.TransmissionEnum;

import java.time.Duration;

/**
 * 檔案系統設定屬性類，提供檔案處理相關的所有核心設定參數。
 * <p>
 * 此類包含檔案上傳、下載、全域設定、備份與安全性等各個面向的設定，
 * 透過 Spring Boot 的 {@code @ConfigurationProperties} 機制，可在 application.yml 中使用 "file" 前綴進行設定。
 * <p>
 * 設定範例：
 * <pre>
 * file:
 *   upload:
 *     default-upload-type: CHUNK
 *     max-upload-file-size: 10GB
 *     chunk-size: 10MB
 *   download:
 *     zip-buffer-size: 256KB
 *   global:
 *     page-size: 100
 *     max-folder-depth: 20
 *   security:
 *     enable-security-check: true
 *     host: localhost
 *     port: 3310
 * </pre>
 * <p>
 * 主要功能模組包括：
 * - {@link Upload} - 檔案上傳相關設定
 * - {@link Download} - 檔案下載相關設定
 * - {@link Global} - 全域檔案系統設定
 * - {@link Backup} - 備份與回收桶設定
 * - {@link Security} - 檔案安全性檢查設定
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "file")
@Data
public class FileProperties {

    /**
     * 檔案上傳設定實例。
     * <p>
     * 包含所有與檔案上傳相關的設定參數，如上傳方式、分塊大小、檔案大小限制等。
     */
    private Upload upload = new Upload();

    /**
     * 檔案下載設定實例。
     * <p>
     * 包含檔案下載相關的設定參數，如緩存時間、緩衝區大小、壓縮檔案暫存路徑等。
     */
    private Download download = new Download();

    /**
     * 全域檔案系統設定實例。
     * <p>
     * 包含檔案系統的全域設定，如分頁大小、資料夾深度限制、檔案列表樹等。
     */
    private Global global = new Global();

    /**
     * 備份與回收桶設定實例。
     * <p>
     * 包含檔案備份、回收桶保留時間、歷史記錄數量限制等相關設定。
     */
    private Backup backup = new Backup();

    /**
     * 檔案安全性設定實例。
     * <p>
     * 包含檔案安全掃描、病毒檢測連接設定、檔案大小限制等安全相關設定。
     */
    private Security security = new Security();

    /**
     * 檔案上傳設定內部類。
     * <p>
     * 定義所有與檔案上傳相關的設定參數，包括傳輸方式、WebSocket路徑、
     * 分塊大小限制、上傳任務限制、檔案大小限制等。支援分塊上傳和多部分上傳兩種方式。
     */
    @Data
    public static class Upload {
        /**
         * 預設的檔案傳輸類型。
         * <p>
         * 系統支援兩種傳輸方式：
         * - {@code CHUNK} - 分塊傳輸，適合大檔案上傳，支援斷點續傳
         * - {@code MULTIPART} - 多部分傳輸，適合小檔案快速上傳
         * <p>
         * 預設值：{@code TransmissionEnum.CHUNK}
         */
        private TransmissionEnum defaultUploadType = TransmissionEnum.CHUNK;

        /**
         * 是否強制使用伺服器端的上傳設定。
         * <p>
         * 當設為 {@code true} 時，所有用戶只能使用 {@link #defaultUploadType} 指定的上傳模式，
         * 忽略客戶端的上傳方式請求。當設為 {@code false} 時，允許用戶選擇合適的上傳方式。
         * <p>
         * 預設值：{@code false}
         */
        private boolean forceUseServerConfig = false;

        /**
         * WebSocket 檔案上傳的端點路徑。
         * <p>
         * 客戶端需要連接到此路徑進行 WebSocket 檔案上傳操作。
         * <p>
         * 預設值：{@code "/file/upload"}
         */
        private String uploadWebSocketPath = "/file/upload";

        /**
         * WebSocket 線上檔案編輯的端點路徑。
         * <p>
         * 客戶端需要連接到此路徑進行線上檔案編輯操作，支援即時協作編輯功能。
         * <p>
         * 預設值：{@code "/file/editing"}
         */
        private String editOnlineFileWebSocketPath = "/file/editing";

        /**
         * WebSocket 連接的最大載荷長度限制。
         * <p>
         * 定義單個 WebSocket 訊息的最大大小，用於控制記憶體使用和網路傳輸效率。
         * 重要：如果前端使用 Nginx 等反向代理，此值必須小於或等於
         * Nginx 的 {@code client_max_body_size} 設定，否則會導致連接失敗。
         * <p>
         * 預設值：20MB
         */
        private DataSize payloadLength = DataSize.ofMegabytes(20);

        /**
         * 檔案上傳時的分塊大小限制。
         * <p>
         * 定義檔案分塊上傳時每個分塊的最大大小。合理的分塊大小有助於提升上傳效率，
         * 同時減少網路中斷對上傳過程的影響。
         * <p>
         * 約束條件：此值必須小於 {@link #payloadLength}，否則會導致上傳失敗。
         * <p>
         * 預設值：10MB
         */
        private DataSize chunkSize = DataSize.ofMegabytes(10);

        /**
         * 單一用戶同時進行的最大上傳任務數量限制。
         * <p>
         * 限制每個用戶同時執行的上傳任務數量，有助於控制系統資源使用和資料庫負載。
         * 注意：設定過高可能導致資料庫處理能力不足，影響整體系統效能。
         * <p>
         * 預設值：3
         */
        private Integer maxUploadTaskLimit = 3;

        /**
         * 單一用戶同時進行檔案分塊合併的最大處理數量限制。
         * <p>
         * 當檔案分塊上傳完成後，系統需要將分塊合併成完整檔案。此參數限制同時進行的合併操作數量，
         * 避免大量合併操作同時執行造成系統負載過高。
         * <p>
         * 建議：建議設定為與 {@link #maxUploadTaskLimit} 相同的值以保持一致性。
         * <p>
         * 預設值：3
         */
        private Integer combineProcessCountLimit = 3;

        /**
         * 單一檔案的最大上傳大小限制。
         * <p>
         * 定義用戶可以上傳的單一檔案的最大大小。此限制有助於控制存儲空間使用和傳輸時間。
         * 當設定值小於或等於 0 時，表示不限制檔案大小（不建議在生產環境中使用）。
         * <p>
         * 預設值：10GB
         */
        private DataSize maxUploadFileSize = DataSize.ofGigabytes(10);

        /**
         * 檔案上傳操作的最大時間限制。
         * <p>
         * 定義檔案上傳操作允許的最長時間，超過此時間的上傳操作會被自動中止。
         * 此設定有助於避免長時間佔用系統資源和處理異常情況。
         * <p>
         * 預設值：6 小時
         */
        private Duration maxUploadDuration = Duration.ofHours(6);
    }


    /**
     * 檔案下載設定內部類。
     * <p>
     * 定義所有與檔案下載相關的設定參數，包括瀏覽器緩存時間、
     * 壓縮檔案緩衝區大小、資料夾壓縮下載的暫存路徑等。
     */
    @Data
    public static class Download {
        /**
         * 瀏覽器緩存檔案的過期時間設定。
         * <p>
         * 透過 HTTP 標頭控制瀏覽器對檔案的緩存時間，合理的緩存時間可以減少重複下載，
         * 提升用戶體驗並減輕伺服器負載。
         * <p>
         * 預設值：1 小時
         */
        private Duration downloadCacheHeaderExpireTime = Duration.ofHours(1);

        /**
         * 壓縮檔案操作的緩衝區大小設定。
         * <p>
         * 用於資料夾打包下載時的 ZIP 壓縮操作，合理的緩衝區大小可以平衡記憶體使用和壓縮效率。
         * 較大的緩衝區可以提升壓縮速度，但會佔用更多記憶體。
         * <p>
         * 預設值：256KB
         */
        private DataSize zipBufferSize = DataSize.ofKilobytes(256);

        /**
         * 資料夾壓縮下載的暫存檔案儲存路徑。
         * <p>
         * 當用戶下載資料夾時，系統會先將資料夾打包成 ZIP 檔案儲存在此路徑，
         * 完成下載後會自動清除暫存檔案。確保此路徑具有足夠的磁碟空間和寫入權限。
         * <p>
         * 預設值：{@code "./temp/folder-zip"}
         */
        private String folderTempDownloadPath = "./temp/folder-zip";
    }


    /**
     * 全域檔案系統設定內部類。
     * <p>
     * 定義影響整個檔案系統運作的全域設定參數，包括檔案列表樹啟用狀態、
     * 資料夾深度限制、分頁設定、最近檔案顯示數量等。
     */
    @Data
    public static class Global {
        /**
         * 是否啟用用戶檔案列表樹功能。
         * <p>
         * 檔案列表樹可以大幅提升檔案路徑查詢效率，特別是對於擁有複雜資料夾結構的用戶。
         * 建議保持啟用狀態以獲得最佳效能。
         * <p>
         * 注意：關閉此功能時，檔案路徑查詢將無法顯示完整路徑資訊，
         * 同時資料夾深度限制功能也會失效。
         * <p>
         * 預設值：{@code true}
         */
        private Boolean enableUserFolderListTree = true;

        /**
         * 檔案列表樹的最大深度限制。
         * <p>
         * 限制用戶可以建立的資料夾嵌套層級，有助於避免過深的資料夾結構影響系統效能。
         * 此參數僅在 {@link #enableUserFolderListTree} 為 {@code true} 時生效。
         * <p>
         * 當設定值小於或等於 0 時，表示不限制資料夾深度（不建議在生產環境中使用）。
         * <p>
         * 預設值：20
         */
        private Integer maxFolderDepth = 20;

        /**
         * 檔案列表分頁顯示的每頁項目數量。
         * <p>
         * 控制檔案列表查詢時每頁回傳的最大項目數量，合理的分頁大小可以平衡載入速度和用戶體驗。
         * 過大的值可能影響頁面載入速度，過小的值則可能需要頻繁翻頁。
         * <p>
         * 預設值：100
         */
        private Integer pageSize = 100;

        /**
         * 最近使用檔案的顯示數量限制。
         * <p>
         * 定義用戶查詢最近使用檔案時顯示的最大檔案數量。適當的數量可以幫助用戶快速找到
         * 最近使用的檔案，同時避免列表過長影響查找效率。
         * <p>
         * 當設定值小於或等於 0 時，表示不限制顯示數量。
         * <p>
         * 預設值：20
         */
        private Integer showRecentFileCount = 20;
    }

    /**
     * 備份與回收桶設定內部類。
     * <p>
     * 定義檔案備份機制和回收桶相關的設定參數，包括檔案保留時間、
     * 線上檔案歷史記錄數量限制等。
     */
    @Data
    public static class Backup {
        /**
         * 回收桶中檔案的保留時間。
         * <p>
         * 被刪除的檔案會先移至回收桶，在此期間內用戶可以恢復檔案。
         * 超過此時間的檔案會被永久刪除且無法恢復。
         * <p>
         * 預設值：30 天
         */
        private Duration retentionTime = Duration.ofDays(30);

        /**
         * 線上檔案歷史版本的最大保留數量。
         * <p>
         * 限制每個線上檔案保留的歷史版本數量，有助於控制存儲空間使用。
         * 當檔案版本數量超過此限制時，最舊的版本會被自動刪除。
         * <p>
         * 當設定值小於或等於 0 時，表示不限制保留數量（可能導致存儲空間不足）。
         * <p>
         * 預設值：30
         */
        private Integer maxOnlineHistoryCount = 30;
    }

    /**
     * 檔案安全性設定內部類。
     * <p>
     * 定義檔案安全掃描和病毒檢測相關的設定參數，包括安全檢查啟用狀態、
     * 防毒軟體連接設定、檔案大小限制等。支援與外部防毒軟體（如 ClamAV）整合。
     */
    @Data
    public static class Security {
        /**
         * 是否啟用檔案安全性檢查功能。
         * <p>
         * 啟用後，所有上傳的檔案都會經過安全掃描，檢測潛在的惡意軟體和病毒。
         * 建議在生產環境中保持啟用以確保系統安全。
         * <p>
         * 預設值：{@code true}
         */
        private Boolean enableSecurityCheck = true;

        /**
         * 防毒軟體服務的主機位址。
         * <p>
         * 指定防毒軟體（如 ClamAV）服務運行的主機名稱或 IP 位址。
         * 系統會透過此位址連接到防毒軟體進行檔案掃描。
         * <p>
         * 預設值：{@code "localhost"}
         */
        private String host = "localhost";

        /**
         * 防毒軟體服務的連接埠號。
         * <p>
         * 指定防毒軟體服務監聽的埠號，通常 ClamAV 使用埠號 3310。
         * <p>
         * 預設值：3310
         */
        private Integer port = 3310;

        /**
         * 安全性檢查操作的超時時間限制。
         * <p>
         * 定義檔案安全掃描操作的最大等待時間，超過此時間的掃描操作會被中止。
         * 合理的超時時間可以避免大檔案掃描時間過長影響用戶體驗。
         * <p>
         * 預設值：30 秒
         */
        private Duration timeout = Duration.ofSeconds(30);

        /**
         * 進行安全性檢查的最小檔案大小限制。
         * <p>
         * 小於此大小的檔案將跳過安全性檢查，有助於提升小檔案的處理速度。
         * 一般而言，非常小的檔案（如文字檔案）的安全風險較低。
         * <p>
         * 當設定值小於或等於 0 時，表示所有檔案都會進行安全性檢查。
         * <p>
         * 預設值：0 bytes
         */
        private DataSize minFileSize = DataSize.ofBytes(0);

        /**
         * 進行安全性檢查的最大檔案大小限制。
         * <p>
         * 大於此大小的檔案將跳過安全性檢查，主要是因為大檔案的掃描時間可能過長，
         * 影響系統效能和用戶體驗。管理者需要在安全性和效能之間找到平衡。
         * <p>
         * 當設定值小於或等於 0 時，表示不限制檔案大小，所有檔案都會進行檢查。
         * <p>
         * 預設值：10GB
         */
        private DataSize maxFileSize = DataSize.ofGigabytes(10);
    }
}
