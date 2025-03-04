package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.FileVersionDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.util.List;

/**
 * 規範底層文件業務邏輯的接口，檔案業務邏輯接口和文件夾業務邏輯接口都繼承了該接口共用方法
 *
 * @author yuan
 * @program FileManagement
 * @ClassName BaseFileService
 * @create 2025/2/15
 * @Version 1.0
 **/

public interface BaseFileService {

    /**
     * 獲取用戶文件列表的接口
     *
     * @param user          用戶信息
     * @param fileFilterDTO 文件過濾條件
     *
     * @return 返回用戶文件列表
     */
    default Mono<PagedResponseDTO<UserFileListDTO>> getUserFileList(User user, FileFilterDTO fileFilterDTO) {
        return Mono.empty();
    }

    /**
     * 獲取用戶文件路徑的接口
     *
     * @param file 文件
     * @param user 用戶信息
     *
     * @return 返回用戶文件路徑
     */
    default Mono<List<FolderListTreeProvider.FolderNode>> getUserFilePaths(UserFileMetadata file, User user) {
        return Mono.empty();
    }

    /**
     * 獲取文件版本列表的接口
     *
     * @param user     用戶信息
     * @param file     文件
     * @param page     分頁頁碼
     * @param pageSize 分頁大小
     *
     * @return 返回文件版本列表
     */
    default Mono<PagedResponseDTO<FileVersionDTO>> getFileVersionList(User user, UserFileMetadata file, Integer page, Integer pageSize) {
        return Mono.empty();
    }

    /**
     * 搜索用戶文件的接口
     *
     * @param user          用戶信息
     * @param fileFilterDTO 文件過濾條件
     *
     * @return 返回搜索用戶文件
     */
    default Mono<PagedResponseDTO<UserFileListDTO>> searchUserFile(User user, FileFilterDTO fileFilterDTO) {
        return Mono.empty();
    }


}
