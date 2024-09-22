package xyz.dowob.filemanagement.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.strategy.CsrfTokenRepositoryStrategy;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;

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
     * CSRF Token 存儲庫策略
     */
    private final CsrfTokenRepositoryStrategy csrfTokenRepository;

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
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<String> authenticate(AuthRequestDTO authRequestDTO) {
        return authenticate(authRequestDTO, null);
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
    @HideSensitive
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<String> authenticate(AuthRequestDTO authRequestDTO, ServerWebExchange request) {
        return userRepository
                .findByUsername(authRequestDTO.getUsername())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)))
                .flatMap(user -> {
                    if (passwordEncoder.matches(authRequestDTO.getPassword(), user.getPassword())) {
                        return tokenService.generateToken(user, TokenEnum.JWT_AUTHORIZATION_TOKEN);
                    }
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR));
                });
    }

    /**
     * 獲取CSRF Token
     *
     * @param request 請求對象
     *
     * @return 返回CSRF Token
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<CsrfToken> getCSRFToken(ServerWebExchange request) {
        return csrfTokenRepository.getCsrfTokenRepository().generateToken(request);
    }
}
