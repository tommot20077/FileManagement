package xyz.dowob.filemanagement.controller.base;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.dto.file.UserFileListDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * 獲取用戶文件列表
     *
     * @param exchange 請求對象
     *
     * @return 返回用戶文件列表
     */
    public Mono<ResponseEntity<?>> getUserFileList (ServerWebExchange exchange) {
        return userService.getUser(exchange).flatMap(user -> fileService.getUserFileList(user).collectList().flatMap(files -> {
            Map<String, Object> dataMap = Map.of("files", files);
            ApiResponseDTO<?> apiResponseDTO = createResponse(exchange, "成功獲取用戶文件列表", dataMap);
            return createResponseEntity(apiResponseDTO);
        }));
    }
}
