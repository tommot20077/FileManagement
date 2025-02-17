package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.User;

/**
 * 文件夾業務邏輯接口，定義了文件夾業務邏輯的相關方法
 * 用於定義文件夾的增刪改查操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FolderService
 * @create 2025/2/15
 * @Version 1.0
 **/

public interface FolderService extends BaseFileService {

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
        return Mono.empty();
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
        return Mono.empty();
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
        return Mono.empty();
    }

}
