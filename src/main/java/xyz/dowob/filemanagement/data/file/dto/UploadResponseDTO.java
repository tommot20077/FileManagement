package xyz.dowob.filemanagement.data.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 檔案傳輸響應的資料傳輸對象，用於規範檔案傳輸響應的資料傳輸對象，用於檔案傳輸響應的資料
 *
 * @author yuan
 * @since 1.0
 * @version 1.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadResponseDTO {
    /**
     * 傳輸任務ID
     */
    private String transferTaskId;

    /**
     * 總分塊數
     */
    private Integer totalChunks;

    /**
     * 規範分塊大小，此單位為Byte
     */
    private Long chunkSize;

    /**
     * 分塊索引
     */
    private Integer chunkIndex;

    /**
     * 進度
     */
    private Double progress;

    /**
     * 是否成功
     */
    private Boolean isSuccess;

    /**
     * 是否完成
     */
    private Boolean isFinished;

    /**
     * 信息
     */
    private String message;
}
