package xyz.dowob.filemanagement.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.controller.base.BaseFileController;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName ApiFileController
 * @create 2025/1/14
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/file")
public class ApiFileController extends BaseFileController {
    public ApiFileController(FileService fileService, UserService userService) {
        super(fileService, userService);
    }

    @GetMapping("/getUserFileList")
    public Mono<ResponseEntity<?>> getUserFileList(ServerWebExchange exchange) {
        return super.getUserFileList(exchange);
    }


}
