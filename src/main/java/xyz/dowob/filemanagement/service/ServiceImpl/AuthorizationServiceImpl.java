package xyz.dowob.filemanagement.service.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.dto.user.AuthRequestDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.ServiceInterFace.AuthorizationService;
import xyz.dowob.filemanagement.service.ServiceInterFace.TokenService;

/**
 * 授權業務邏輯實現類
 * 實現接口 @see {@link AuthorizationService}
 * 主要當用戶需要進行授權時，進行相應的業務邏輯處理，跟據用戶的請求進行授權，並返回結果
 *
 * @author yuan
 * @program File-Management
 * @ClassName AuthorizationServiceImpl
 * @description
 * @create 2024-09-16 03:13
 * @Version 1.0
 **/
@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {
    /**
     * 用戶數據庫操作對象
     */
    private final UserRepository userRepository;

    /**
     * 密碼加密器(採用BCrypt加密)
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 憑證服務
     */
    private final TokenService tokenService;

    /**
     * 根據用戶請求頭中的JWT憑證進行授權
     * 當憑證合法時，返回用戶對象
     * 否則返回空
     *
     * @param jwtToken 需要驗證的JWT憑證
     * @param request  請求對象
     */
    public Mono<Void> tokenAuthorize(String jwtToken, ServerWebExchange request) {
        if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
            jwtToken = jwtToken.substring(7);
            Mono<Long> userId = tokenService.validateToken(jwtToken, null, TokenEnum.JWT_AUTHORIZATION_TOKEN);
            if (userId != null) {
                Mono<User> optionalUser = userRepository.findById(userId);
                return optionalUser.flatMap(user -> {
                    if (user != null) {
                        return setSessionAuthorization(request, user);
                    }
                    return Mono.empty();
                });
            }
        }
        return Mono.empty();
    }

    /**
     * 根據用戶名和密碼進行授權
     * 當用戶名和密碼正確時，返回用戶對象
     * 當用戶名或密碼錯誤時，返回錯誤信息
     *
     * @param authRequestDTO 用戶驗證請求對象
     *
     * @return 返回用戶JWT憑證
     */
    @Override
    public Mono<String> authenticate(AuthRequestDTO authRequestDTO, ServerWebExchange request) {
        return userRepository
                .findByUsername(authRequestDTO.getUsername())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)))
                .flatMap(user -> {
                    if (passwordEncoder.matches(authRequestDTO.getPassword(), user.getPassword())) {
                        Mono<Void> sessionMono = request != null ? setSessionAuthorization(request, user) : Mono.empty();
                        return sessionMono.then(tokenService.generateToken(user, TokenEnum.JWT_AUTHORIZATION_TOKEN));
                    }
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR));
                });
    }

    /**
     * 根據用戶名和密碼進行授權
     * 此方法為無請求對象的授權方法，不會將授權信息存入Session
     * 當用戶名和密碼正確時，返回用戶對象
     * 否則返回空
     *
     * @param authRequestDTO 用戶驗證請求對象
     *
     * @return 返回用戶對象
     */
    public Mono<String> passwordAuthenticate(AuthRequestDTO authRequestDTO) {
        return authenticate(authRequestDTO, null);
    }
}
