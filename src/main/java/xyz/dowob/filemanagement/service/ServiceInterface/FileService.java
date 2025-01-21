package xyz.dowob.filemanagement.service.ServiceInterface;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.file.FileMetadata;
import xyz.dowob.filemanagement.dto.file.UploadChunkDTO;
import xyz.dowob.filemanagement.dto.file.UploadResponseDTO;
import xyz.dowob.filemanagement.dto.file.UserFileListDTO;
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
     * 獲取用戶文件列表的接口
     *
     * @param user 用戶信息
     *
     * @return 返回用戶文件列表
     */
    default Flux<UserFileListDTO> getUserFileList (User user) {
        return null;
    }

    /**
     * 獲取用戶文件列表的接口
     *
     * @param userId 用戶ID
     *
     * @return 返回用戶文件列表
     */
    default Flux<UserFileListDTO> getUserFileList(Long userId) {
        return null;
    };

    /**
     * 上傳文件的接口
     *
     * @param fileMetadata 文件元數據
     *                     包含文件名、文件大小、文件類型等信息
     * @param user         用戶信息
     *
     * @return 返回上傳結果
     */
    default Mono<UploadResponseDTO> uploadFile(FileMetadata fileMetadata, User user) {
        return null;
    }

    /**
     * 上傳文件分塊的接口
     *
     * @param uploadChunkDTO 上傳文件數據
     *                       包含文件分塊數據、文件ID等信息
     *
     * @return 返回上傳結果
     */
    default Mono<UploadResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO) {
        return null;
    }

    /**
     * 下載文件的接口
     *
     * @param fileId 文件ID
     * @param user   用戶信息
     *
     * @return 返回文件下載流
     */
    default Flux<DataBuffer> downloadFile(String fileId, User user) {
        return null;
    }

    // Mono<ResponseEntity<?>> deleteFile(ServerWebExchange exchange, String fileId);


}
