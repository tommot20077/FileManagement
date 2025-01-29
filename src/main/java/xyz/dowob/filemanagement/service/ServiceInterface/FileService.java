package xyz.dowob.filemanagement.service.ServiceInterface;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.unity.FileCrudService;

import java.util.List;

/**
 * 文件業務邏輯接口，定義了文件業務邏輯的相關方法
 * @author yuan
 * @program FileManagement
 * @ClassName FileService
 * @description
 * @create 2024-09-30 20:13
 * @Version 1.0
 **/
public interface FileService extends FileCrudService {

    /**
     * 獲取用戶文件列表的接口
     *
     * @param user 用戶信息
     *
     * @return 返回用戶文件列表
     */
    Flux<UserFileListDTO> getUserFileList(User user, Long folderId);


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
    default Mono<UserFileDataBO> downloadFile(String fileId, User user) {
        return null;
    }

    /**
     * 刪除文件的接口
     *
     * @param fileId 文件ID
     * @param user   用戶信息
     *
     * @return 返回刪除結果
     */
    default Mono<Void> deleteFile(String fileId, User user) {
        return null;
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
        return null;
    }

    /**
     * 創建文件夾的接口
     *
     * @param fileEditDTO 文件夾數據
     *                    包含文件夾名稱、父文件夾ID等信息
     * @param user        用戶信息
     *
     * @return 返回創建結果
     */
    default Mono<Void> createFolder(FileEditDTO fileEditDTO, User user) {
        return null;
    }

    /**
     * 編輯文件夾的接口
     *
     * @param fileEditDTO 文件夾數據
     *                    包含文件夾ID、文件夾名稱等信息
     * @param user        用戶信息
     *
     * @return 返回編輯結果
     */
    default Mono<Void> editFolder(FileEditDTO fileEditDTO, User user) {
        return null;
    }

    /**
     * 刪除文件夾的接口
     *
     * @param folderId 文件夾ID
     * @param user     用戶信息
     *
     * @return 返回刪除結果
     */
    default Mono<Void> deleteFolder(String folderId, User user) {
        return null;
    }

    default Mono<List<FolderListTreeProvider.FolderNode>> getUserFilePaths(Long fileId, User user) {
        return null;
    }


}
