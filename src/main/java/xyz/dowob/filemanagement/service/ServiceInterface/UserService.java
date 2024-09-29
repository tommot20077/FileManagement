package xyz.dowob.filemanagement.service.ServiceInterface;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.user.AuthRequestDTO;
import xyz.dowob.filemanagement.dto.user.RegisterDTO;
import xyz.dowob.filemanagement.dto.user.ResetPasswordDTO;
import xyz.dowob.filemanagement.dto.user.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName UserService
 * @description
 * @create 2024-09-23 16:25
 * @Version 1.0
 **/
public interface UserService extends CrudService<User, Long> {

    /**
     * 用戶註冊
     *
     * @param registerUserDTO 用戶註冊數據傳輸對象
     *
     */
    Mono<Void> register(RegisterDTO registerUserDTO);

    /**
     * 用戶登入
     *
     * @param authRequestDTO 用戶登入數據傳輸對象
     * @param request        請求對象
     * @return 用戶
     */
    Mono<String> login(AuthRequestDTO authRequestDTO, ServerWebExchange request);

    /**
     * 用戶登出
     *
     * @param request 用戶請求對象
     */
    Mono<Void> logout(Long userId, ServerWebExchange request);

    /**
     * 用戶修改密碼
     *
     * @param user 用戶
     * @return 用戶
     */
    Mono<User> changePassword(User user);

    /**
     * 用戶修改信箱
     *
     * @param user 用戶
     * @return 用戶
     */
    Mono<User> changeEmail(User user);

    /**
     * 用戶忘記密碼
     *
     * @param userEmailDTO 用戶信箱數據傳輸對象
     */
    Mono<Void> sendResetPasswordMail(UserEmailDTO userEmailDTO);

    /**
     * 用戶重置密碼
     *
     * @param resetPasswordDTO 重置密碼數據傳輸對象
     */
    Mono<Void> resetPassword(ResetPasswordDTO resetPasswordDTO);

}
