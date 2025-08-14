package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

/**
 * 檔案夾管理服務介面。繼承 BaseFileService，提供檔案夾特定的業務操作功能。
 *
 * <p>支援檔案夾建立、編輯、刪除和下載操作。所有方法採用反應式設計，回傳 Mono 類型
 * 以確保非阻塞處理。包含檔案夾層級結構管理、權限控制和批量操作支援。
 *
 * <p>錯誤情況通過 Mono.error() 傳播，權限不足或操作失敗時拋出相應例外。
 * 檔案夾操作執行時進行權限檢查，確保用戶僅能操作有權限的檔案夾。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see BaseFileService
 */

public interface FolderService extends BaseFileService {

    /**
     * 建立新檔案夾。檢查用戶權限並在指定位置建立檔案夾。
     *
     * @param fileEditDTO 檔案夾建立資料，包含檔案夾名稱和父檔案夾位置
     * @param user 執行建立的用戶
     * @return 建立完成信號的 Mono
     */
    default Mono<UserFileMetadata> createFolder(FileEditDTO fileEditDTO, User user) {
        return Mono.empty();
    }


    /**
     * 編輯檔案夾屬性。檢查用戶權限並更新檔案夾名稱或其他可編輯屬性。
     *
     * @param fileEditBO 檔案夾編輯物件，包含檔案夾標識符和新屬性值
     * @param user 執行編輯的用戶
     * @return 編輯完成信號的 Mono
     */
    default Mono<Void> editFolder(FileEditBO fileEditBO, User user) {
        return Mono.empty();
    }


    /**
     * 刪除指定檔案夾。檢查用戶權限並遞歸刪除檔案夾及其內容。
     *
     * @param folder 要刪除的檔案夾元資料
     * @param user 執行刪除的用戶
     * @return 刪除完成信號的 Mono
     */
    default Mono<Void> deleteFolder(UserFileMetadata folder, User user) {
        return Mono.empty();
    }


    /**
     * 下載檔案夾內容。檢查用戶權限並將檔案夾壓縮為下載檔案。
     *
     * @param folder 要下載的檔案夾元資料
     * @param user 執行下載的用戶
     * @return 檔案夾壓縮資料的 Mono，包含壓縮檔案內容和相關資訊
     */
    default Mono<UserFileDataBO> downloadFolder(UserFileMetadata folder, User user) {
        return Mono.empty();
    }

}
