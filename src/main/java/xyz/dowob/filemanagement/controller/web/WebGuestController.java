package xyz.dowob.filemanagement.controller.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.controller.base.BaseGuestController;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.ServiceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;

import java.util.HashMap;

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
@RequestMapping("/web/guest")
public class WebGuestController extends BaseGuestController {
    public WebGuestController (UserService userService, AuthorizationService authorizationService, SecurityProperties securityProperties) {
        super(authorizationService, userService, securityProperties);
    }

    /**
     * 訪客註冊的Web請求
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
     * 訪客登入的Web請求
     *
     * @param authRequestDTO 登入用戶的數據傳輸對象
     * @param exchange       請求對象
     *
     * @return Mono<ResponseEntity> 返回登入結果
     */
    @Override
    @HideSensitive
    @PostMapping("/login")
    public Mono<ResponseEntity<?>> login(AuthRequestDTO authRequestDTO, ServerWebExchange exchange, boolean isWeb) {
        return super.login(authRequestDTO, exchange, true);
    }

    /**
     * 訪客發送重置密碼驗證信的Web請求
     *
     * @param userMail 用戶信箱的數據傳輸對象
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回發送驗證信結果
     */
    @Override
    @PostMapping("/sendResetPasswordMail")
    public Mono<ResponseEntity<?>> sendResetPasswordMail(UserEmailDTO userMail, ServerWebExchange exchange) {
        return super.sendResetPasswordMail(userMail, exchange);
    }

    /**
     * 訪客重置密碼的Web請求
     *
     * @param resetPasswordDTO 重置密碼的數據傳輸對象
     * @param exchange         請求對象
     *
     * @return ResponseEntity 返回重置密碼結果
     */
    @Override
    @PutMapping("/resetPassword")
    public Mono<ResponseEntity<?>> resetPassword(ResetPasswordDTO resetPasswordDTO, ServerWebExchange exchange) {
        return super.resetPassword(resetPasswordDTO, exchange);
    }

    /**
     * 獲取CSRF Token
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回CSRF Token
     */
    @GetMapping("/getCSRFToken")
    public Mono<ResponseEntity<?>> getCSRFToken(ServerWebExchange exchange) {
        return authorizationService.getCSRFToken(exchange).flatMap(csrfToken -> {
            HashMap<String, Object> data = new HashMap<>();
            data.put("csrfToken", csrfToken.getToken());
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "獲取CSRF Token成功", data);
            return createResponseEntity(apiResponse);
        }).onErrorResume(ValidationException.class, e -> {
            String errorMessage = String.format("獲取CSRF Token失敗: %s", e.getMessage());
            int responseCode = e.getErrorCode().getCode();
            return createResponseEntity(createResponse(exchange, responseCode, errorMessage, null));
        });
    }
}
