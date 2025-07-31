package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadChunkDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.unity.FileCrudService;

/**
 * 基於反應式模式的檔案服務介面。整合 FileCrudService、BaseFileService 和 RecoverableFile，
 * 提供完整的檔案操作功能。
 *
 * <p>採用非阻塞 I/O 設計，支援檔案上傳、下載、刪除和編輯操作。所有方法回傳 Mono 類型
 * 以確保高並發處理和資源效率。內建用戶權限驗證和安全性控制。
 *
 * <p>錯誤處理通過 Mono.error() 傳播，權限不足或操作失敗時拋出相應例外。
 * 檔案操作執行時進行權限檢查，確保用戶僅能操作有權限的檔案。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see FileCrudService
 * @see BaseFileService
 * @see RecoverableFile
 */
public interface FileService extends FileCrudService, BaseFileService, RecoverableFile<UserFileMetadata> {

    /**
     * 上傳檔案至系統。驗證用戶權限並處理檔案元資料和實際檔案內容。
     *
     * @param fileMetadataDTO 檔案元資料，包含檔案名稱、大小、類型等資訊
     * @param user 執行上傳的用戶
     * @return 上傳結果響應的 Mono，包含檔案標識符和狀態資訊
     */
    default Mono<UploadResponseDTO> uploadFile(FileMetadataDTO fileMetadataDTO, User user) {
        return Mono.empty();
    }


    /**
     * 上傳檔案分塊。支援大型檔案的分段上傳和斷點續傳功能。
     *
     * @param uploadChunkDTO 分塊上傳資料，包含分塊內容、檔案標識符、分塊序號和總數
     * @return 分塊上傳結果的 Mono，包含當前分塊狀態和進度資訊
     */
    default Mono<UploadResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO) {
        return Mono.empty();
    }


    /**
     * 下載指定檔案。驗證用戶權限並提供檔案內容的非阻塞存取。
     *
     * @param file 要下載的檔案元資料
     * @param user 執行下載的用戶
     * @param optional 可選參數，用於指定下載範圍或特殊設定
     * @return 檔案資料的 Mono，包含檔案內容和相關資訊
     */
    default Mono<UserFileDataBO> downloadFile(UserFileMetadata file, User user, String... optional) {
        return Mono.empty();
    }


    /**
     * 刪除指定檔案。檢查用戶權限並執行檔案刪除操作，支援可恢復性邏輯。
     *
     * @param file 要刪除的檔案元資料
     * @param user 執行刪除的用戶
     * @return 刪除完成信號的 Mono
     */
    default Mono<Void> deleteFile(UserFileMetadata file, User user) {
        return Mono.empty();
    }


    /**
     * 編輯指定檔案。檢查用戶權限並執行檔案內容編輯操作。
     *
     * @param fileEditBO 檔案編輯物件，包含檔案標識、編輯內容和編輯類型
     * @param user 執行編輯的用戶
     * @return 編輯完成信號的 Mono
     */
    default Mono<Void> editFile(FileEditBO fileEditBO, User user) {
        return Mono.empty();
    }

}
