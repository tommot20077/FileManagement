package xyz.dowob.filemanagement.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import xyz.dowob.filemanagement.customenum.TransmissionEnum;

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
     * 文件上傳配置
     */
    @Data
    public static class Upload {
        /**
         * 文件傳輸類型，默認為 CHUNK 進行文件傳輸
         */
        private TransmissionEnum transmissionType = TransmissionEnum.CHUNK;

        /**
         * Websocket最大允許分塊大小，單位為 MB，默認為 20MB
         * 注意如果有設定nginx的client_max_body_size，這個值必須小於nginx的設定
         */
        private Integer payloadLength = 20;

        /**
         * 上傳分塊大小限制，單位為 MB，默認為 10MB
         * 此參數需小於 payloadLength，否則將會導致上傳失敗
         */
        private Integer chunkSize = 10;

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
    }

    /**
     * 文件下載配置
     */
    @Data
    public static class Download {
        /**
         * 設定瀏覽器緩存的過期時間，單位為秒，默認為 3600 秒
         */
        private int downloadCacheHeaderExpireTime = 3600;

        /**
         * 設定下載檔案的緩衝區大小，單位為 B，默認為 4096 KB
         */
        private int zipBufferSize = 4096;

        /**
         * 下載資料夾的壓縮檔案暫存路徑，默認為 ./temp
         */
        private String folderTempDownloadPath = "./temp/folder-zip";

        /**
         * 單次資料夾下載的最大併發數量限制，默認為 5
         * 此值用於限制資料夾一邊查詢一邊寫入到壓縮檔案的最大併發數量
         * 若此值設定過高，則可能會導致資料夾下載 GridFS 的性能下降
         */
        private int folderDownloadConcurrentLimit = 5;
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
         * 回收桶文件保留時間，默認為 30，單位為天
         */
        private Integer retentionTime = 30;

        /**
         * 線上檔案歷程記錄備份保留數量，當設置值小於等於0時，則不限制保留數量，默認為 30
         */
        private Integer maxOnlineHistoryCount = 30;
    }
}
