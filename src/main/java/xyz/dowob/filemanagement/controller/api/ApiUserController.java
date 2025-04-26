package xyz.dowob.filemanagement.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.controller.base.BaseUserController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.PermissionEnum;
import xyz.dowob.filemanagement.customenum.UserInfoTypeEnum;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.Set;

/**
 * 用於處理用戶API 控制器，用於處理用戶的API請求。
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
@RequestMapping("/api/v1/user")
public class ApiUserController extends BaseUserController {
    public ApiUserController(FileServiceStrategy fileServiceStrategy, UserService userService, SecurityProperties securityProperties, ValidationService validationService) {
        super(fileServiceStrategy, userService, securityProperties, validationService);
    }


    /**
     * 用戶登出的API請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回登出結果
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<?>> logout(ServerWebExchange exchange, boolean isWeb) {
        return super.logout(exchange, false);
    }


    /**
     * 獲取所有用戶信息的API請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回用戶信息
     */
    @RecordLevel(LogLevelEnum.WARN)
    @GetMapping("/getAllUserInfo")
    @RequirePermission(PermissionEnum.MANAGE)
    public Mono<ResponseEntity<?>> getAllUserInfo(ServerWebExchange exchange) {
        return super.getAllUserInfo(exchange);
    }


    /**
     * 獲取所有用戶信息的API請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回用戶信息
     */
    @GetMapping("/info")
    public Mono<ResponseEntity<?>> getUserInfo(ServerWebExchange exchange) {
        return super.getUserInfo(exchange);
    }


    /**
     * 根據輸入的用戶名稱獲取指定用戶信息的API請求
     *
     * @param exchange  請求對象
     * @param userInfos 查詢列表
     * @param type      查詢類型
     *
     * @return Mono<ResponseEntity> 返回用戶信息
     */
    @GetMapping("/info/search")
    public Mono<ResponseEntity<?>> searchUserInfo(ServerWebExchange exchange,
                                                  @RequestParam Set<String> userInfos,
                                                  @RequestParam(required = false, defaultValue = "name") String type) {
        String formatType = UserInfoTypeEnum.getUserInfoType(type).name();
        return super.searchUserInfo(exchange, userInfos, formatType);
    }
}
