package xyz.dowob.filemanagement.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.controller.base.BaseGuestController;
import xyz.dowob.filemanagement.dto.user.AuthRequestDTO;
import xyz.dowob.filemanagement.dto.user.RegisterDTO;
import xyz.dowob.filemanagement.dto.user.ResetPasswordDTO;
import xyz.dowob.filemanagement.dto.user.UserEmailDTO;
import xyz.dowob.filemanagement.service.ServiceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;

/**
 * 用於處理訪客相關的API請求的控制器
 *
 * @author yuan
 * @program File-Management
 * @ClassName ApiGuestController
 * @description
 * @create 2024-09-14 20:22
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/guest")
public class ApiGuestController extends BaseGuestController {
    public ApiGuestController(UserService userService, AuthorizationService authorizationService) {
        super(userService, authorizationService);
    }

    /**
     * 訪客註冊的API請求
     *
     * @param registerUserDTO 註冊用戶的數據傳輸對象
     * @param exchange        請求對象
     *
     * @return Mono<ResponseEntity> 返回註冊結果
     */
    @Override
    @PostMapping("/register")
    public Mono<ResponseEntity<?>> register(RegisterDTO registerUserDTO, ServerWebExchange exchange) {
        return super.register(registerUserDTO, exchange);
    }

    /**
     * 訪客登入的API請求
     *
     * @param authRequestDTO 登入用戶的數據傳輸對象
     * @param exchange       請求對象
     *
     * @return Mono<ResponseEntity> 返回登入結果
     */
    @Override
    @PostMapping("/login")
    public Mono<ResponseEntity<?>> login(AuthRequestDTO authRequestDTO, ServerWebExchange exchange) {
        return super.login(authRequestDTO, exchange);
    }

    /**
     * 訪客發送重置密碼驗證信的API請求
     *
     * @param userMail 用戶信箱的數據傳輸對象
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回發送重置密碼驗證信結果
     */
    @Override
    @PostMapping("/sendResetPasswordMail")
    public Mono<ResponseEntity<?>> sendResetPasswordMail(UserEmailDTO userMail, ServerWebExchange exchange) {
        return super.sendResetPasswordMail(userMail, exchange);
    }

    /**
     * 訪客重置密碼的API請求
     *
     * @param resetPasswordDTO 重置密碼的數據傳輸對象
     * @param exchange         請求對象
     *
     * @return Mono<ResponseEntity> 返回重置密碼結果
     */
    @Override
    @PutMapping("/resetPassword")
    public Mono<ResponseEntity<?>> resetPassword(ResetPasswordDTO resetPasswordDTO, ServerWebExchange exchange) {
        return super.resetPassword(resetPasswordDTO, exchange);
    }
}
