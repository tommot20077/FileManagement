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
     * 文件傳輸類型，默認為 MULTIPART，即使用 Multipart 進行文件傳輸
     */
    private TransmissionEnum transmissionType = TransmissionEnum.CHUNK;

    /**
     * 建立文件上傳配置
     */
    private Upload upload = new Upload();

    /**
     * 建立全局配置
     */
    private global global = new global();

    /**
     * 文件上傳配置
     */
    @Data
    public static class Upload {
        /**
         * 文件上傳臨時目錄，默認為 ./temp/uploads/
         */
        private String tempDirectory = "./temp/uploads/";

        /**
         * Websocket最大允許分塊大小，單位為 MB，默認為 20MB
         */
        private Integer payloadLength = 20;

        /**
         * 上傳分塊大小限制，單位為 MB，默認為 10MB，此參數需小於 payloadLength
         */
        private Integer chunkSize = 10;

        /**
         * 最大上傳任務限制，默認為 3
         */
        private Integer maxUploadTaskLimit = 3;

        /**
         * 合併處理任務限制，默認為 3
         */
        private Integer combineProcessCountLimit = 3;
    }

    /**
     * 全局配置
     */
    @Data
    public static class global {
        private Boolean enableUserFolderListTree = true;
    }
}
