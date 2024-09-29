package xyz.dowob.filemanagement.service.ServiceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.file.FileMetadata;
import xyz.dowob.filemanagement.dto.file.TransferResponseDTO;
import xyz.dowob.filemanagement.dto.file.UploadChunkDTO;
import xyz.dowob.filemanagement.entity.User;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName FileService
 * @description
 * @create 2024-09-30 20:13
 * @Version 1.0
 **/
public interface FileService {

    /**
     * 上傳文件的接口
     *
     * @param fileMetadata 文件元數據
     *                     包含文件名、文件大小、文件類型等信息
     * @param user         用戶信息
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳結果
     */
    Mono<TransferResponseDTO> uploadFile(FileMetadata fileMetadata, User user);

    Mono<TransferResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO);

    // Mono<ResponseEntity<?>> downloadFile(ServerWebExchange exchange, String fileId);

    // Mono<ResponseEntity<?>> deleteFile(ServerWebExchange exchange, String fileId);


}
