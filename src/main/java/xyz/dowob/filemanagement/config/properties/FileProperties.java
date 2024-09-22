package xyz.dowob.filemanagement.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import xyz.dowob.filemanagement.customenum.TransmissionEnum;

import java.time.Duration;

/**
 * 文件配置文件，用於配置文件處理的相關參數，在 application 中配置 file
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileProperties
 * @description
 * @create 2024-10-04 01:00
 * @Version 1.0
 **/
@Configuration
@ConfigurationProperties(prefix = "file")
@Data
public class FileProperties {

    /**
     * 建立文件上傳配置
     */
    private Upload upload = new Upload();

    /**
     * 建立文件下載配置
     */
    private Download download = new Download();

    /**
     * 建立全局配置
     */
    private global global = new global();

    /**
     * 建立備份配置
     */
    private Backup backup = new Backup();

    /**
     * 建立安全性配置
     */
    private Security security = new Security();

    /**
     * 文件上傳配置
     */
    @Data
    public static class Upload {
        /**
         * 文件傳輸類型，默認為 CHUNK 進行文件傳輸
         * 可以選擇的傳輸類型有 CHUNK、MULTIPART
         */
        private TransmissionEnum defaultUploadType = TransmissionEnum.CHUNK;

        /**
         * 是否強制使用伺服器端的上傳設定，默認為 false
         * 當設置為 true 時，用戶只能使用伺服器端的上傳模式 {@link #defaultUploadType}
         * 當設置為 false 時，用戶可以使用其指定的上傳模式 {@link TransmissionEnum}
         */
        private boolean forceUseServerConfig = false;

        /**
         * WebSocket上傳檔案的路徑，默認為 /file/upload
         */
        private String uploadWebSocketPath = "/file/upload";

        /**
         * WebSocket編輯線上檔案的路徑
         * 默認為 /file/editing
         */
        private String editOnlineFileWebSocketPath = "/file/editing";

        /**
         * Websocket最大允許分塊大小，默認為 20MB
         * 注意如果有設定 nginx 的 client_max_body_size，這個值必須小於nginx的設定
         */
        private DataSize payloadLength = DataSize.ofMegabytes(20);

        /**
         * 上傳分塊大小限制，默認為 10MB
         * 此參數需小於 payloadLength，否則將會導致上傳失敗
         */
        private DataSize chunkSize = DataSize.ofMegabytes(10);

        /**
         * 最大上傳任務限制，默認為 3
         * 此值設定過高容易導致導致資料庫處理不及而使得處理性能下降
         */
        private Integer maxUploadTaskLimit = 3;

        /**
         * 單一位用戶合併檔案分塊的最大數量限制，默認為 3
         * 建議此值設定為與 maxUploadTaskLimit 相同
         */
        private Integer combineProcessCountLimit = 3;

        /**
         * 上傳檔案的大小限制，默認為 10GB
         * 當設置值小於等於0時，則不限制上傳檔案的大小
         */
        private DataSize maxUploadFileSize = DataSize.ofGigabytes(10);

        /**
         * 上傳檔案最大的時間限制，默認為 6小時
         */
        private Duration maxUploadDuration = Duration.ofHours(6);
    }


    /**
     * 文件下載配置
     */
    @Data
    public static class Download {
        /**
         * 設定瀏覽器緩存的過期時間，默認為 1小時
         */
        private Duration downloadCacheHeaderExpireTime = Duration.ofHours(1);

        /**
         * 設定下載檔案的緩衝區大小，默認為 64 KB
         */
        private DataSize zipBufferSize = DataSize.ofKilobytes(256);

        /**
         * 下載資料夾的壓縮檔案暫存路徑，默認為 ./temp
         */
        private String folderTempDownloadPath = "./temp/folder-zip";
    }


    /**
     * 全局配置
     */
    @Data
    public static class global {
        /**
         * 是否啟用用戶文件列表樹，默認為 true
         * 建議啟用，可以有效提升檔案路徑查詢效率。
         * 若關閉時查詢檔案路徑時則不會顯示檔案路徑，以及無法啟用資料夾深度限制
         */
        private Boolean enableUserFolderListTree = true;

        /**
         * 文件列表樹最大深度，默認為 20
         * 此參數僅在啟用用戶文件列表樹時生效
         * 當設置值小於等於0時，則不限制顯示深度
         */
        private Integer maxFolderDepth = 20;

        /**
         * 文件列表每頁顯示數量，默認為 100
         */
        private Integer pageSize = 100;

        /**
         * 顯示最近文件數量，默認為 20，當設置值小於等於0時，則不限制顯示數量
         * 此參數用於用戶查詢最近使用的檔案所顯示的數量
         */
        private Integer showRecentFileCount = 20;
    }

    @Data
    public static class Backup {
        /**
         * 回收桶文件保留時間，默認為 30天
         */
        private Duration retentionTime = Duration.ofDays(30);

        /**
         * 線上檔案歷程記錄備份保留數量，當設置值小於等於0時，則不限制保留數量，默認為 30
         */
        private Integer maxOnlineHistoryCount = 30;
    }

    @Data
    public static class Security {
        /**
         * 是否啟用安全性檢查，默認為 true
         */
        private Boolean enableSecurityCheck = true;

        /**
         * 連線到防毒軟體的主機名稱或 IP 位址，默認為 localhost
         */
        private String host = "localhost";

        /**
         * 連線到防毒軟體的埠號，默認為 3310
         */
        private Integer port = 3310;

        /**
         * 安全性檢查的超時時間，默認為 30秒
         */
        private Duration timeout = Duration.ofSeconds(30);

        /**
         * 安全性檢查的最小檔案大小，默認為 0
         * 當檔案大小小於此值時，則不進行安全性檢查
         * 若設置值小於等於0時，則不限制檔案大小
         */
        private DataSize minFileSize = DataSize.ofBytes(0);

        /**
         * 安全性檢查的最大檔案大小，默認為 10GB
         * 當檔案大小大於此值時，則不進行安全性檢查
         * 若設置值小於等於0時，則不限制檔案大小
         */
        private DataSize maxFileSize = DataSize.ofGigabytes(10);
    }
}
