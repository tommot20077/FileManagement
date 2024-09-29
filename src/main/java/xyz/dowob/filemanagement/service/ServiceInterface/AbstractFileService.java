package xyz.dowob.filemanagement.service.ServiceInterface;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.file.FileMetadata;
import xyz.dowob.filemanagement.dto.file.TransferResponseDTO;
import xyz.dowob.filemanagement.dto.file.UploadChunkDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName FileService
 * @description
 * @create 2024-09-26 21:58
 * @Version 1.0
 **/
public abstract class AbstractFileService implements FileService {

    /**
     * 初始化上傳任務，若需要則返回Mono<String> taskId
     *
     * @param fileMetadata 檔案元數據
     *
     * @return Mono<String> taskId
     */
    protected abstract Mono<TransferResponseDTO> initialUpload(FileMetadata fileMetadata);

    /**
     * 上傳文件分塊
     *
     * @param uploadChunkDTO 上傳文件數據
     *
     * @return Mono<TransferResponseDTO> 上傳結果
     */
    @Override
    public abstract Mono<TransferResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO);

    /**
     * 合併已上傳的文件分塊
     *
     * @param transferTaskId 任務ID
     * @param totalChunks    總分塊數
     *
     * @return Mono<Void>
     */
    protected abstract Mono<ObjectId> combineChunks(String transferTaskId, int totalChunks);

    /**
     * 關聯用戶與文件
     *
     * @param serverFileMetadataId 服務器文件ID
     *                             用於關聯用戶與文件
     *                             用戶文件表中的serverFileId
     * @param fileMetadata         文件元數據
     *
     * @return Mono<String> 文件ID
     */
    protected abstract Mono<UserFileMetadata> associateUserFile(Long serverFileMetadataId, FileMetadata fileMetadata);

}
