package xyz.dowob.filemanagement.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import xyz.dowob.filemanagement.customenum.TransmissionEnum;

/**
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

    private TransmissionEnum transmissionType = TransmissionEnum.MULTIPART;
    private Upload upload = new Upload();

    @Data
    public static class Upload {
        private String tempDirectory = "./temp/uploads/";
        private Integer maxFramePayloadLength = 10;
    }

}
