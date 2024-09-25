package xyz.dowob.filemanagement.service.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.dto.user.AuthRequestDTO;
import xyz.dowob.filemanagement.dto.user.RegisterDTO;
import xyz.dowob.filemanagement.dto.user.ResetPasswordDTO;
import xyz.dowob.filemanagement.dto.user.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.ServiceInterFace.AuthorizationService;
import xyz.dowob.filemanagement.service.ServiceInterFace.TokenService;
import xyz.dowob.filemanagement.service.ServiceInterFace.UserService;
import xyz.dowob.filemanagement.service.ServiceInterFace.ValidationService;

/**
 * 用戶業務邏輯實現類，主要用於處理用戶相關的業務邏輯
 * 實現接口 @see {@link UserService}
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserServiceImpl
 * @description
 * @create 2024-09-23 16:27
 * @Version 1.0
 **/
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    /**
     * 用戶數據庫操作對象
     */
    private final UserRepository userRepository;

    /**
     * 驗證服務
     */
    private final ValidationService validationService;

    /**
     * 授權服務
     */
    private final AuthorizationService authorizationService;

    /**
     * 憑證服務
     */
    private final TokenService tokenService;

    /**
     * 驗證碼服務
     */
    private final EmailProvider emailProvider;

    /**
     * 驗證碼過期時間
     */
    @Value("${security.verificationcode.expiration}")
    private int verificationCodeExpiration;

    /**
     * 此方法之後為UserService接口中的方法實現
     * 用戶註冊
     *
     * @param registerUserDTO 用戶註冊數據傳輸對象
     *
     * @return 用戶
     */
    @Override
    public Mono<Void> register(RegisterDTO registerUserDTO) {
        return validationService.validateRegisterDTO(registerUserDTO).then(Mono.defer(() -> {
            User user = new User();
            user.setUsername(registerUserDTO.getUsername());
            user.setPassword(registerUserDTO.getPassword());
            user.setEmail(registerUserDTO.getEmail());
            return userRepository.save(user).then();
        }));
    }

    /**
     * 用戶登入
     *
     * @param authRequestDTO 用戶登入數據傳輸對象
     * @param request        請求對象
     *
     * @return 用戶
     */
    @Override
    @HideSensitive
    public Mono<String> login(AuthRequestDTO authRequestDTO, ServerWebExchange request) {
        return validationService.validateNotNull(authRequestDTO).then(authorizationService.authenticate(authRequestDTO, request));
    }

    /**
     * 用戶登出，並調用憑證服務進行憑證撤銷
     *
     * @param exchange 用戶請求對象
     *
     * @return 用戶
     */
    @Override
    public Mono<Void> logout(Long userId, ServerWebExchange exchange) {
        return exchange
                .getSession()
                .flatMap(session -> userRepository
                        .findById(userId)
                        .flatMap(user -> tokenService.revokeToken(user.getId(), TokenEnum.JWT_AUTHORIZATION_TOKEN))
                        .then(session.invalidate()))
                .doFinally(signalType -> SecurityContextHolder.clearContext());
    }

    /**
     * 用戶修改密碼
     *
     * @param user 用戶
     *
     * @return 用戶
     */
    @Override
    public Mono<User> changePassword(User user) {
        return null;
    }

    /**
     * 用戶修改信箱
     *
     * @param user 用戶
     *
     * @return 用戶
     */
    @Override
    public Mono<User> changeEmail(User user) {
        return null;
    }

    /**
     * 發送重置密碼郵件，並依照VerificationCodeExpiration設定的時間內有效
     *
     * @param userEmailDTO 用戶郵箱數據傳輸對象
     *
     * @return 用戶
     */
    @Override
    public Mono<Void> sendResetPasswordMail(UserEmailDTO userEmailDTO) {
        return validationService
                .validateNotNull(userEmailDTO)
                .then(Mono.defer(() -> userRepository
                        .findByEmail(userEmailDTO.getEmail())
                        .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND,
                                                                          userEmailDTO.getEmail())))
                        .flatMap(user -> tokenService.generateToken(user, TokenEnum.RESET_PASSWORD_TOKEN).flatMap(token -> {
                            String content = String.format("重置密碼的憑證為：%s\n請於%s分鐘內重置密碼", token, verificationCodeExpiration);
                            return emailProvider.sendEmail(user.getEmail(), "重置密碼", content);
                        }))));
    }

    /**
     * 重置密碼，並且撤銷憑證
     *
     * @param resetPasswordDTO 重置密碼數據傳輸對象
     *
     * @return 用戶
     */
    @Override
    public Mono<Void> resetPassword(ResetPasswordDTO resetPasswordDTO) {
        return validationService
                .validateResetPasswordDTO(resetPasswordDTO)
                .then(Mono.defer(() -> userRepository
                        .findByEmail(resetPasswordDTO.getEmail())
                        .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND,
                                                                          resetPasswordDTO.getEmail())))
                        .flatMap(user -> tokenService
                                .validateToken(resetPasswordDTO.getVerificationCode(), user.getId(), TokenEnum.RESET_PASSWORD_TOKEN)
                                .then(Mono.defer(() -> {
                                    user.setPassword(resetPasswordDTO.getNewPassword());
                                    return userRepository
                                            .save(user)
                                            .then(tokenService.revokeToken(user.getId(), TokenEnum.RESET_PASSWORD_TOKEN));
                                })))));
    }

    /**
     * 此方法之後為CrudService接口中的方法實現
     * 創建一個新的實體
     *
     * @return 返回一個新的實體對象
     */
    @Override
    public Mono<User> create() {
        return null;
    }

    /**
     * 根據ID獲取一個實體
     *
     * @param userId 實體ID
     *
     * @return 返回一個Optional對象
     */
    @Override
    public Mono<User> getById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 獲取所有實體
     */
    @Override
    public Flux<User> getAll() {
        return userRepository.findAll();
    }

    /**
     * 更新一個實體
     *
     * @param entity 實體對象
     */
    @Override
    public Mono<Void> update(User entity) {
        return null;
    }

    /**
     * 刪除一個實體
     *
     * @param entity 實體對象
     */
    @Override
    public Mono<Void> delete(User entity) {
        return null;
    }


}
