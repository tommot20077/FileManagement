package xyz.dowob.filemanagement.dto.file;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

/**
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
    @NotBlank(message = "任務ID不能為空")
    private String transferTaskId;

    private int totalChunks;

    private int chunkIndex;

    private byte[] chunkData;


    public TransferResponseDTO toTransferResponseDTO(Boolean isSuccess, Integer uploadedChunk, String message) {
        TransferResponseDTO transferResponseDTO = new TransferResponseDTO();
        transferResponseDTO.setChunkIndex(chunkIndex);
        transferResponseDTO.setTransferTaskId(transferTaskId);
        Optional.ofNullable(isSuccess).ifPresent(transferResponseDTO::setIsSuccess);
        transferResponseDTO.setProgress((double) uploadedChunk / totalChunks);
        Optional.ofNullable(message).ifPresent(transferResponseDTO::setMessage);
        return transferResponseDTO;
    }

}
