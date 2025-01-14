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
    private TransmissionEnum transmissionType = TransmissionEnum.MULTIPART;

    /**
     * 建立文件上傳配置
     */
    private Upload upload = new Upload();

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
         * 最大允許分塊大小，單位為 MB，默認為 20MB
         */
        private Integer payloadLength = 20;
    }
}
