package xyz.dowob.filemanagement.controller.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.controller.base.BaseUserController;

/**
 * 用於處理用戶相關的Web請求的控制器
 *
 * @author yuan
 * @program File-Management
 * @ClassName WebUserController
 * @description
 * @create 2024-09-16 19:49
 * @Version 1.0
 **/
@RestController
@RequestMapping("/web/user")
public class WebUserController extends BaseUserController {
    /**
     * 用戶登出的Web請求
     *
     * @param exchange 用於處理Web請求的交換器
     *
     * @return Mono<ResponseEntity> 返回登出結果
     */
    @Override
    @PostMapping("/logout")
    public Mono<ResponseEntity<?>> logout(ServerWebExchange exchange) {
        return super.logout(exchange);
    }

}
