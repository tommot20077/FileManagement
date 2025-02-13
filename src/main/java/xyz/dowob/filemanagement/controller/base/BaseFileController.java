package xyz.dowob.filemanagement.controller.base;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.HashMap;
import java.util.List;

/**
 * 檔案控制器的基礎類
 *
 * @author yuan
 * @program FileManagement
 * @ClassName BaseFileController
 * @create 2025/1/14
 * @Version 1.0
 **/
@RequiredArgsConstructor
public abstract class BaseFileController implements ResponseUnity {
    /**
     * 用戶業務層對象
     */
    protected final UserService userService;
    /**
     * 檔案策略
     */
    protected final FileServiceStrategy fileServiceStrategy;
    /**
     * 檔案屬性
     */
    protected final FileProperties fileProperties;

    /**
     * 獲取用戶文件列表，此 ID 為資料夾 ID
     * 有2個特定的ID作為特殊用途
     * 1. 0: 獲取用戶根目錄文件列表
     * 2. -1: 獲取用戶所有文件列表
     *
     * @param exchange 請求對象
     *
     * @return 返回用戶文件列表
     */
    public Mono<ResponseEntity<?>> getUserFileList(ServerWebExchange exchange, Long folderId, Integer page) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            FileService fileService = fileServiceStrategy.getFileService(null);
            int pageSize = fileProperties.getGlobal().getPageSize();
            Mono<PagedResponseDTO<UserFileListDTO>> fileListMono = fileService.getUserFileList(user, folderId, Math.max(page, 1), pageSize);
            Mono<List<FolderListTreeProvider.FolderNode>> filePathsMono = fileService.getUserFilePaths(folderId, user);
            return Mono.zip(fileListMono, filePathsMono).flatMap(tuple -> {
                HashMap<String, Object> result = new HashMap<>();
                result.put("userId", user.getId());
                result.put("username", user.getUsername());
                result.put("files", tuple.getT1());
                result.put("filePaths", tuple.getT2());
                return createResponseEntity(createResponse(exchange, "獲取用戶文件列表成功", result));
            });
        }), exchange);
    }

}
