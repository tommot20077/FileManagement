package xyz.dowob.filemanagement.service.serviceInterface;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;

/**
 * 用戶業務邏輯接口
 * 定義了用戶業務邏輯的相關方法
 * 用戶業務邏輯主要包括用戶註冊、用戶登入、用戶登出、用戶修改密碼、用戶修改信箱、用戶忘記密碼、用戶重置密碼等方法
 * 繼承了 {@link CrudService} 接口，內部定義了用戶的基本操作
 *
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
     */
    Mono<Void> register(RegisterDTO registerUserDTO);

    /**
     * 用戶登入
     *
     * @param authRequestDTO 用戶登入數據傳輸對象
     * @param request        請求對象
     *
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
     *
     * @return 用戶
     */
    Mono<User> changePassword(User user);

    /**
     * 用戶修改信箱
     *
     * @param user 用戶
     *
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

    /**
     * 用於獲取用戶的方法，根據請求對象獲取用戶對象
     *
     * @param exchange 請求對象
     *
     * @return Mono<User> 返回用戶對象
     */
    Mono<User> getUser(ServerWebExchange exchange);
}
