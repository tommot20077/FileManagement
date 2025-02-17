package xyz.dowob.filemanagement.data.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import xyz.dowob.filemanagement.entity.UserOnlineFileHistory;

import java.time.LocalDateTime;

/**
 * 文件版本數據傳輸對象，用於封裝文件版本的數據
 * @author yuan
 * @program FileManagement
 * @ClassName FileVersionDTO
 * @create 2025/2/11
 * @Version 1.0
 **/
@Data
public class FileVersionDTO {
    /**
     * 當前版本號
     */
    private Long version;
    /**
     * 修改者
     */
    private Long modifiedBy;
    /**
     * 修改時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedTime;
    /**
     * 修改註釋
     */
    private String note;


    public FileVersionDTO(UserOnlineFileHistory userOnlineFileHistory) {
        this.version = userOnlineFileHistory.getVersion();
        this.modifiedBy = userOnlineFileHistory.getModifiedBy();
        this.modifiedTime = userOnlineFileHistory.getModifiedTime();
        this.note = userOnlineFileHistory.getNote();
    }
}