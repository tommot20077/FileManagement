package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.User;

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
     * @param user 用戶信息
     *
     * @return 返回用戶文件列表
     */
    Mono<PagedResponseDTO<UserFileListDTO>> getUserFileList(User user, Long folderId, int page, int pageSize, List<FileEnum> type);

    /**
     * 獲取用戶文件路徑的接口
     *
     * @param fileId 文件ID
     * @param user   用戶信息
     *
     * @return 返回用戶文件路徑
     */
    default Mono<List<FolderListTreeProvider.FolderNode>> getUserFilePaths(Long fileId, User user) {
        return null;
    }
}
