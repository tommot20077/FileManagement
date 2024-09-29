package xyz.dowob.filemanagement.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
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
public class TransferResponseDTO {
    private String transferTaskId;

    private Integer chunkIndex;

    private Double progress;

    private Boolean isSuccess;

    private Boolean isFinished;

    private String message;
}
