package xyz.dowob.filemanagement.controller.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.controller.base.BaseUserController;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;

/**
 * 用於處理用戶 Web 控制器，用於處理用戶的Web請求。
 * 此類繼承自 BaseUserController，提供用戶登出、獲取用戶信息等功能。
 *
 * @author yuan
 * @program File-Management
 * @ClassName WebBaseUserController
 * @description
 * @create 2024-09-16 19:49
 * @Version 1.0
 **/
@RestController
@RequestMapping("/web/v1/user")
public class WebUserController extends BaseUserController {
    public WebUserController(FileServiceStrategy fileServiceStrategy, UserService userService, SecurityProperties securityProperties) {
        super(fileServiceStrategy, userService, securityProperties);
    }

    /**
     * 用戶登出的Web請求
     *
     * @param exchange 用於處理Web請求的交換器
     *
     * @return Mono<ResponseEntity> 返回登出結果
     */
    @Override
    @PostMapping("/logout")
    public Mono<ResponseEntity<?>> logout(ServerWebExchange exchange, boolean isWeb) {
        return super.logout(exchange, true);
    }

    /**
     * 獲取用戶信息的Web請求
     *
     * @param exchange 用於處理Web請求的交換器
     *
     * @return Mono<ResponseEntity> 返回用戶信息
     */
    @GetMapping("/info")
    @Override
    public Mono<ResponseEntity<?>> getUserInfo(ServerWebExchange exchange) {
        return super.getUserInfo(exchange);
    }

}
