package xyz.dowob.filemanagement.controller.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.controller.base.BaseGuestController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;

/**
 * 用於處理訪客相關的Web請求的控制器
 *
 * @author yuan
 * @program File-Management
 * @ClassName BaseGuestController
 * @description
 * @create 2024-09-14 20:22
 * @Version 1.0
 **/
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/web/v1/guest")
public class WebGuestController extends BaseGuestController {
    /**
     * 構造函數，初始化訪客控制器。
     *
     * @param authorizationService 授權服務
     * @param userService          用戶服務
     * @param securityProperties   安全屬性配置
     */
    protected WebGuestController(AuthorizationService authorizationService, UserService userService, SecurityProperties securityProperties) {
        super(authorizationService, userService, securityProperties);
    }


    /**
     * 訪客註冊 Web請求
     *
     * @param registerUserDTO 註冊用戶的數據傳輸對象
     * @param exchange        當前請求對象
     * @return Mono<ResponseEntity < ?>> 註冊結果
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<?>> register(@RequestBody RegisterDTO registerUserDTO, ServerWebExchange exchange) {
        return super.register(registerUserDTO, exchange);
    }


    /**
     * 訪客登入 Web請求
     *
     * @param authRequestDTO 登入請求數據
     * @param exchange       當前請求對象
     * @return Mono<ResponseEntity < ?>> 登入結果
     */
    @HideSensitive
    @PostMapping("/login")
    public Mono<ResponseEntity<?>> login(@RequestBody AuthRequestDTO authRequestDTO, ServerWebExchange exchange) {
        return super.login(authRequestDTO, exchange, true);
    }


    /**
     * 發送重置密碼郵件
     * 用戶請求重置密碼時，系統將發送包含重置鏈接的郵件至用戶郵箱。
     *
     * @param userMail 用戶郵箱數據傳輸對象
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回發送結果
     */
    @PostMapping("/sendResetPasswordMail")
    public Mono<ResponseEntity<?>> sendResetPasswordMail(@RequestBody UserEmailDTO userMail, ServerWebExchange exchange) {
        return super.sendResetPasswordMail(userMail, exchange);
    }


    /**
     * 重置密碼的請求
     * 用戶提供重置密碼的驗證信息後，系統將更新用戶的密碼。
     *
     * @param resetPasswordDTO 重置密碼的數據傳輸對象
     * @param exchange         請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回重置密碼結果
     */
    @PutMapping("/resetPassword")
    public Mono<ResponseEntity<?>> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO, ServerWebExchange exchange) {
        return super.resetPassword(resetPasswordDTO, exchange);
    }


    /**
     * 確認當前用戶授權狀態，並返回用戶信息
     * 用於檢查當前用戶是否已經授權，並返回用戶的基本信息。
     * 如果用戶未授權，會返回401 Unauthorized錯誤。
     *
     * @param exchange 請求對象，包含請求上下文
     *
     * @return Mono<ResponseEntity < ?>> 返回用戶授權狀態的結果
     */
    @SkipRecord
    @GetMapping("/checkAuthenticationStatus")
    public Mono<ResponseEntity<?>> checkAuthenticationStatus(ServerWebExchange exchange) {
        return super.checkAuthenticationStatus(exchange);
    }
}
