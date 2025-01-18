package xyz.dowob.filemanagement.dto.file;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

/**
 * 文件上傳分片的數據傳輸對象，用於規範文件上傳分片的數據傳輸對象，紀錄文件上傳分片的數據
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UploadChunkDTO
 * @description
 * @create 2024-09-27 00:40
 * @Version 1.0
 **/
@Data
@AllArgsConstructor
public class UploadChunkDTO {
    /**
     * 任務ID
     */
    @NotBlank(message = "任務ID不能為空")
    private String transferTaskId;

    /**
     * 總分塊數
     */
    private int totalChunks;

    /**
     * 分塊索引
     */
    private int chunkIndex;

    /**
     * 分塊數據
     */
    private byte[] chunkData;


    /**
     * 將文件上傳分片的數據傳輸對象轉換為文件傳輸響應的數據傳輸對象
     *
     * @param isSuccess     是否成功
     * @param uploadedChunk 已上傳的分塊數
     * @param message       訊息
     *
     * @return 文件傳輸響應的數據傳輸對象
     */
    public TransferResponseDTO toTransferResponseDTO(Boolean isSuccess, Integer uploadedChunk, String message) {
        TransferResponseDTO transferResponseDTO = new TransferResponseDTO();
        transferResponseDTO.setChunkIndex(chunkIndex);
        transferResponseDTO.setTotalChunks(totalChunks);
        transferResponseDTO.setTransferTaskId(transferTaskId);
        Optional.ofNullable(isSuccess).ifPresent(transferResponseDTO::setIsSuccess);
        transferResponseDTO.setProgress((double) uploadedChunk / totalChunks);
        Optional.ofNullable(message).ifPresent(transferResponseDTO::setMessage);
        return transferResponseDTO;
    }

}
