package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadChunkDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.unity.FileCrudService;

/**
 * 文件業務邏輯接口，定義了文件業務邏輯的相關方法
 * 繼承了 {@link FileCrudService} 和 {@link BaseFileService} 接口
 * 這兩個接口分別定義了文件的基本操作和底層文件業務邏輯的規範
 * 並且實現了 {@link RecoverableFile} 接口
 * 該接口定義了文件的可恢復操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileService
 * @description
 * @create 2024-09-30 20:13
 * @Version 1.0
 **/
public interface FileService extends FileCrudService, BaseFileService, RecoverableFile<UserFileMetadata> {

    /**
     * 上傳文件的接口
     *
     * @param fileMetadataDTO 文件元數據
     *                        包含文件名、文件大小、文件類型等信息
     * @param user            用戶信息
     *
     * @return 返回上傳結果
     */
    default Mono<UploadResponseDTO> uploadFile(FileMetadataDTO fileMetadataDTO, User user) {
        return Mono.empty();
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
        return Mono.empty();
    }


    /**
     * 下載文件的接口
     *
     * @param file     文件
     * @param user     用戶信息
     * @param optional 其他可選參數
     *
     * @return 返回文件下載流
     */
    default Mono<UserFileDataBO> downloadFile(UserFileMetadata file, User user, String... optional) {
        return Mono.empty();
    }


    /**
     * 刪除文件的接口
     *
     * @param file 文件
     * @param user 用戶信息
     *
     * @return 返回刪除結果
     */
    default Mono<Void> deleteFile(UserFileMetadata file, User user) {
        return Mono.empty();
    }


    /**
     * 編輯文件的接口
     *
     * @param fileEditDTO 文件ID
     * @param user        用戶信息
     *
     * @return 返回編輯結果
     */
    default Mono<Void> editFile(FileEditDTO fileEditDTO, User user) {
        return Mono.empty();
    }

}
