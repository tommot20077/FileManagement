package xyz.dowob.filemanagement.data.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import xyz.dowob.filemanagement.entity.UserOnlineFileHistory;

import java.time.LocalDateTime;

/**
 * 檔案版本資料傳輸對象，用於封裝檔案版本的資料
 * @author yuan
 * @since 1.0
 * @version 1.0
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


    /**
     * 構造函數，從 UserOnlineFileHistory 對象中初始化 FileVersionDTO 對象
     *
     * @param userOnlineFileHistory 用戶在線檔案歷史對象
     */
    public FileVersionDTO(UserOnlineFileHistory userOnlineFileHistory) {
        this.version = userOnlineFileHistory.getVersion();
        this.modifiedBy = userOnlineFileHistory.getModifiedBy();
        this.modifiedTime = userOnlineFileHistory.getModifiedTime();
        this.note = userOnlineFileHistory.getNote();
    }
}