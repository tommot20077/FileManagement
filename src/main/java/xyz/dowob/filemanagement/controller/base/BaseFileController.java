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
    protected final UserService userService;
    protected final FileStrategy fileStrategy;
    protected final UserLimiterStrategy userLimiterStrategy;
    protected final ValidationService validationService;
    protected final FileProperties fileProperties;

    /**
     * 獲取用戶文件列表
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
                    result.put("parentFolder", null);
                    return fileService.getUserFileMetadataById(folderId).map(metadata -> {
                        result.put("parentFolder", metadata);
                        return result;
                    }).switchIfEmpty(Mono.just(result));
                });
            }).flatMap(result -> createResponseEntity(createResponse(exchange, "獲取用戶文件列表成功", result)));
        }), exchange);
    }
}
