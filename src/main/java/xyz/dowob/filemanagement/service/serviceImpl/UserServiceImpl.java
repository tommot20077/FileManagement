package xyz.dowob.filemanagement.service.serviceImpl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.manager.CacheManager;
import xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.customenum.PermissionEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.customenum.UserInfoTypeEnum;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.CacheRule;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final Optional<EmailProvider> emailProvider;

    /**
     * 安全配置屬性
     */
    private final SecurityProperties securityProperties;

    /**
     * 密碼加密器(採用BCrypt加密)
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Cache處理器
     */
    private final CacheManager cacheManager;

    /**
     * 用戶ID緩存規則
     */
    private CacheRule<User> USER_ID_CACHE_RULE;

    /**
     * 用戶名緩存規則
     */
    private CacheRule<User> USERNAME_CACHE_RULE;

    /**
     * 初始化緩存規則
     */
    @PostConstruct
    public void init() {
        USERNAME_CACHE_RULE = cacheManager.generateCacheRule(User::getUsername, CacheProviderEnum.USER_CACHE);
        USER_ID_CACHE_RULE = cacheManager.generateCacheRule(User::getId, CacheProviderEnum.USER_CACHE);
    }

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
            user.setPassword(passwordEncoder.encode(registerUserDTO.getPassword()));
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
        return emailProvider.map(provider -> {
            return validationService
                    .validateNotNull(userEmailDTO)
                    .then(Mono.defer(() -> userRepository
                            .findByEmail(userEmailDTO.getEmail())
                            .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, userEmailDTO.getEmail())))
                            .flatMap(user -> tokenService.generateToken(user, TokenEnum.RESET_PASSWORD_TOKEN).flatMap(token -> {
                                String content = String.format("重置密碼的憑證為：%s\n請於%s分鐘內重置密碼",
                                                               token,
                                                               securityProperties.getResetPasswordToken().getExpiration()
                                );
                                return provider.sendEmail(user.getEmail(), "重置密碼", content);
                            }))));
        }).orElseGet(() -> Mono.error(new ValidationException(ValidationException.ErrorCode.UNSUPPORTED_OPERATION)));
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
                .then(userRepository
                              .findByEmail(resetPasswordDTO.getEmail())
                              .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND,
                                                                                resetPasswordDTO.getEmail()
                              )))
                              .flatMap(user -> tokenService
                                      .validateToken(resetPasswordDTO.getVerificationCode(), user.getId(), TokenEnum.RESET_PASSWORD_TOKEN)
                                      .then(Mono.defer(() -> {
                                          user.setPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
                                          return userRepository
                                                  .save(user)
                                                  .then(tokenService.revokeToken(user.getId(), TokenEnum.RESET_PASSWORD_TOKEN));
                                      }))));
    }

    /**
     * 用於獲取用戶的方法，根據請求對象獲取用戶對象
     * 先從Session中獲取用戶ID，如果Session中沒有則從SecurityContext中獲取
     * 當其中一個獲取到用戶ID時，則根據用戶ID獲取用戶對象
     * 如果都沒有獲取到用戶ID，則返回空
     *
     * @param exchange 請求對象
     *
     * @return Mono<User> 返回用戶對象
     */
    @Override
    @SkipRecord
    public Mono<User> getUser(ServerWebExchange exchange) {
        final Object[] userId = new Object[1];
        return Mono.defer(() -> {
            userId[0] = exchange.getAttributes().getOrDefault("userId", null);
            if (userId[0] == null) {
                return ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication).flatMap(authentication -> {
                    if (authentication != null && authentication.isAuthenticated()) {
                        userId[0] = Long.valueOf(authentication.getPrincipal().toString());
                        return cacheManager.runAndSetCache(userId[0].toString(),
                                                           User.class,
                                                           CacheProviderEnum.USER_CACHE,
                                                           userRepository.findById((Long) userId[0]),
                                                           List.of(USER_ID_CACHE_RULE, USERNAME_CACHE_RULE)
                        );
                    }
                    return Mono.empty();
                });
            }
            return cacheManager.runAndSetCache(userId[0].toString(),
                                               User.class,
                                               CacheProviderEnum.USER_CACHE,
                                               userRepository.findById((Long) userId[0]),
                                               List.of(USER_ID_CACHE_RULE, USERNAME_CACHE_RULE)
            );
        }).switchIfEmpty(Mono.empty());
    }

    /**
     * 此方法之後為CrudService接口中的方法實現
     * 創建一個新的實體
     *
     * @return 返回一個新的實體對象
     */
    @Override
    public Mono<User> create() {
        return Mono.empty();
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
    @RequirePermission(PermissionEnum.MANAGE)
    public Flux<User> getAll() {
        return userRepository.findAll();
    }

    /**
     * 根據參數獲取所有實體
     *
     * @param args 參數
     *
     * @return 返回所有實體
     */
    @Override
    public Flux<User> getAllByParams(String type, Object... args) {
        if (args.length == 0) {
            return Flux.empty();
        }

        List<String> userInfoList = new LinkedList<>();
        boolean isId = Objects.equals(type, UserInfoTypeEnum.ID.name());

        Stream.of(args).forEach(arg -> {
            userInfoList.add(arg.toString());
        });
        Flux<User> cacheUserFlux = cacheManager.getCachesAsConcat(userInfoList, User.class, CacheProviderEnum.USER_CACHE).doOnNext(user -> {
            Object userType = isId ? user.getId().toString() : user.getUsername();
            userInfoList.remove(userType);
        });

        if (userInfoList.isEmpty()) {
            return cacheUserFlux;
        }


        Flux<User> userRepositoryChooseFlux;
        if (isId) {
            userRepositoryChooseFlux = userRepository.findAllByIdIn(userInfoList
                                                                            .stream()
                                                                            .filter(id -> id.matches("\\d+"))
                                                                            .map(Long::parseLong)
                                                                            .collect(Collectors.toList()));
        } else {
            userRepositoryChooseFlux = userRepository.findAllByUsernameIn(userInfoList);
        }


        Flux<User> userRepositoryFlux = cacheManager.runAndSetCache(userInfoList,
                                                                    User.class,
                                                                    CacheProviderEnum.USER_CACHE,
                                                                    userRepositoryChooseFlux,
                                                                    List.of(USER_ID_CACHE_RULE, USERNAME_CACHE_RULE)
        );
        return cacheUserFlux.concatWith(userRepositoryFlux);
    }

    /**
     * 更新一個實體
     *
     * @param entity 實體對象
     */
    @Override
    public Mono<Void> update(User entity) {
        return Mono.empty();
    }

    /**
     * 刪除一個實體
     *
     * @param entity 實體對象
     */
    @Override
    public Mono<Void> delete(User entity) {
        return Mono.empty();
    }


}
