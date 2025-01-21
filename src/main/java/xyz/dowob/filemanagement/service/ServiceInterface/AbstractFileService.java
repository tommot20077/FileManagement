package xyz.dowob.filemanagement.service.ServiceInterface;

import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.file.FileMetadata;
import xyz.dowob.filemanagement.dto.file.UploadChunkDTO;
import xyz.dowob.filemanagement.dto.file.UploadResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

/**
 * 檔案服務的抽象類，用於定義檔案服務的基本操作，如初始化上傳任務、上傳文件分塊、合併文件分塊等，實現FileService接口
 * 實現類需實現這些方法，並根據具體業務需求進行實現
 *
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
    protected abstract Mono<UploadResponseDTO> initialUpload(FileMetadata fileMetadata);

    /**
     * 上傳文件分塊
     *
     * @param uploadChunkDTO 上傳文件數據
     *
     * @return Mono<TransferResponseDTO> 上傳結果
     */
    @Override
    public abstract Mono<UploadResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO);

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

    /**
     * 驗證用戶權限
     *
     * @param user             用戶
     * @param userFileMetadata 用戶文件元數據
     *
     * @return Mono<Void>
     */
    protected abstract Mono<Void> validateUserPermission(User user, UserFileMetadata userFileMetadata);
}
