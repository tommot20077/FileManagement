package xyz.dowob.filemanagement.data.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件傳輸響應的數據傳輸對象，用於規範文件傳輸響應的數據傳輸對象，用於文件傳輸響應的數據
 *
 * @author yuan
 * @program FileManagement
 * @ClassName TransferResponseDTO
 * @description
 * @create 2024-09-26 23:58
 * @Version 1.0
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
