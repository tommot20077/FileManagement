package xyz.dowob.filemanagement.data.file.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;

import java.util.Optional;

/**
 * 檔案上傳分片的資料傳輸對象，用於規範檔案上傳分片的資料傳輸對象，記錄檔案上傳分片的資料
 *
 * @author yuan
 * @since 1.0
 * @version 1.0
 **/
@Data
@NoArgsConstructor
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
     * 分塊資料
     */
    private byte[] chunkData;

    /**
     * 分塊資料流
     */
    @JsonIgnore
    private Flux<DataBuffer> chunkDataFlux;

    /**
     * 構造函數
     *
     * @param transferTaskId 任務ID
     * @param totalChunks    總分塊數
     * @param chunkIndex     分塊索引
     * @param chunkData      分塊資料
     */
    public UploadChunkDTO(String transferTaskId, int totalChunks, int chunkIndex, byte[] chunkData) {
        this.transferTaskId = transferTaskId;
        this.totalChunks = totalChunks;
        this.chunkIndex = chunkIndex;
        this.chunkData = chunkData;
    }

    /**
     * 構造函數
     *
     * @param transferTaskId 任務ID
     * @param totalChunks    總分塊數
     * @param chunkIndex     分塊索引
     * @param chunkDataFlux  分塊資料流
     */
    public UploadChunkDTO(String transferTaskId, int totalChunks, int chunkIndex, Flux<DataBuffer> chunkDataFlux) {
        this.transferTaskId = transferTaskId;
        this.totalChunks = totalChunks;
        this.chunkIndex = chunkIndex;
        this.chunkDataFlux = chunkDataFlux;
    }

    /**
     * 獲取分塊資料流，當 chunkDataFlux 不為空時，回傳 chunkDataFlux
     * 否則檢查 chunkData 是否為空，若不為空則將其包裝為 DataBuffer 並回傳
     * 否則回傳空的 Flux
     *
     * @return 分塊資料流
     */
    public Flux<DataBuffer> getChunkDataFlux() {
        if (chunkDataFlux != null) {
            return chunkDataFlux;
        } else if (chunkData != null) {
            return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(chunkData));
        }
        return Flux.empty();
    }

    /**
     * 將檔案上傳分片的資料傳輸對象轉換為檔案傳輸響應的資料傳輸對象
     *
     * @param isSuccess     是否成功
     * @param uploadedChunk 已上傳的分塊數
     * @param message       訊息
     *
     * @return 檔案傳輸響應的資料傳輸對象
     */
    public UploadResponseDTO toTransferResponseDTO(Boolean isSuccess, Integer uploadedChunk, String message) {
        UploadResponseDTO uploadResponseDTO = new UploadResponseDTO();
        uploadResponseDTO.setChunkIndex(chunkIndex);
        uploadResponseDTO.setTotalChunks(totalChunks);
        uploadResponseDTO.setTransferTaskId(transferTaskId);
        Optional.ofNullable(isSuccess).ifPresent(uploadResponseDTO::setIsSuccess);
        uploadResponseDTO.setProgress((double) uploadedChunk / totalChunks);
        Optional.ofNullable(message).ifPresent(uploadResponseDTO::setMessage);
        return uploadResponseDTO;
    }

}
