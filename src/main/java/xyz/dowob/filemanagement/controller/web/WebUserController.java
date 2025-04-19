package xyz.dowob.filemanagement.controller.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.controller.base.BaseUserController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.UserInfoTypeEnum;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.Set;

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
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/web/v1/user")
public class WebUserController extends BaseUserController {
    public WebUserController(FileServiceStrategy fileServiceStrategy, UserService userService, SecurityProperties securityProperties, ValidationService validationService) {
        super(fileServiceStrategy, userService, securityProperties, validationService);
    }


    /**
     * 用戶登出的Web請求
     *
     * @param exchange 用於處理Web請求的交換器
     *
     * @return Mono<ResponseEntity> 返回登出結果
     */
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
    public Mono<ResponseEntity<?>> getUserInfo(ServerWebExchange exchange) {
        return super.getUserInfo(exchange);
    }


    /**
     * 搜索用戶信息的Web請求
     *
     * @param exchange  用於處理Web請求的交換器
     * @param userInfos 搜索列表
     * @param type      搜索類型
     *
     * @return Mono<ResponseEntity> 返回搜索結果
     */
    @GetMapping("/info/search")
    public Mono<ResponseEntity<?>> searchUserInfo(ServerWebExchange exchange,
                                                  @RequestParam Set<String> userInfos,
                                                  @RequestParam(required = false, defaultValue = "name") String type) {
        String formatType = UserInfoTypeEnum.getUserInfoType(type).name();
        return super.searchUserInfo(exchange, userInfos, formatType);
    }
}
