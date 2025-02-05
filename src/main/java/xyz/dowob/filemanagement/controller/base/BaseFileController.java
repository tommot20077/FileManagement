package xyz.dowob.filemanagement.controller.base;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.FileStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;
import xyz.dowob.filemanagement.service.ServiceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.HashMap;

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
     * 檔案業務層對象
     */
    protected final FileService fileService;
    /**
     * 用戶業務層對象
     */
    protected final UserService userService;
    /**
     * 檔案策略
     */
    protected final FileStrategy fileStrategy;
    /**
     * 用戶限制策略
     */
    protected final UserLimiterStrategy userLimiterStrategy;
    /**
     * 驗證業務層對象
     */
    protected final ValidationService validationService;
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
    public Mono<ResponseEntity<?>> getUserFileList(ServerWebExchange exchange, Long folderId) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            FileService fileService = fileStrategy.getFileService(null);
            return Mono.defer(() -> {
                HashMap<String, Object> result = new HashMap<>();
                return fileService.getUserFileList(user, folderId).collectList().flatMap(files -> {
                    result.put("userId", user.getId());
                    result.put("username", user.getUsername());
                    result.put("files", files);
                    return Mono.just(result);
                });
            }).flatMap(result -> fileService.getUserFilePaths(folderId, user).flatMap(list -> {
                result.put("filePaths", list);
                return createResponseEntity(createResponse(exchange, "獲取用戶文件列表成功", result));
            }));
        }), exchange);
    }

    //todo 併發請求最後整合 加快速度
}
