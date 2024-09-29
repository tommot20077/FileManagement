package xyz.dowob.filemanagement.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.controller.base.BaseUserController;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;

/**
 * 用於處理用戶相關的API請求的控制器
 *
 * @author yuan
 * @program File-Management
 * @ClassName WebUserController
 * @description
 * @create 2024-09-16 19:49
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/user")
public class ApiUserController extends BaseUserController {
    public ApiUserController(UserService userService) {
        super(userService);
    }

    /**
     * 用戶登出的API請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回登出結果
     */
    @PostMapping("/logout")
    @Override
    public Mono<ResponseEntity<?>> logout(ServerWebExchange exchange) {
        return super.logout(exchange);
    }

    /**
     * 獲取所有用戶信息的API請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回用戶信息
     */
    @GetMapping("/getUserInfo")
    @Override
    public Mono<ResponseEntity<?>> getUserInfo(ServerWebExchange exchange, @RequestParam Long userid) {
        return super.getUserInfo(exchange, userid);
    }

    /**
     * 獲取所有用戶信息的API請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回用戶信息
     */
    @GetMapping("/getAllUserInfo")
    @Override
    public Flux<ResponseEntity<?>> getAllUserInfo(ServerWebExchange exchange) {
        return super.getAllUserInfo(exchange);
    }

}
