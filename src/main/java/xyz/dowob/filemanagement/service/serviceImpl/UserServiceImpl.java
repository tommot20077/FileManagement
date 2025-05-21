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
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.manager.CacheManager;
import xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.*;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.CacheRule;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;

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
@RecordLevel(LogLevelEnum.INFO)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    /**
     * 用戶ID緩存規則
     */
    private static final CacheRule<User> USER_ID_CACHE_RULE = CacheManager.generateCacheRule(User::getId, CacheProviderEnum.USER_CACHE);
    /**
     * 用戶名緩存規則
     */
    private static final CacheRule<User> USERNAME_CACHE_RULE = CacheManager.generateCacheRule(User::getUsername, CacheProviderEnum.USER_CACHE);

    /**
     * 用戶數據庫操作對象
     */
    private final UserRepository userRepository;

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
     * 用戶限流器策略模式
     */
    private final UserLimiterStrategy userLimiterStrategy;
    /**
     * 遊客用戶對象
     * 用於處理遊客的請求
     */
    private final User guestUser = new User();

    /**
     * 初始化緩存規則
     */
    @PostConstruct
    public void init() {
        guestUser.setId(0L);
        guestUser.setUsername("Guest");
        guestUser.setPassword("Guest");
        guestUser.setEmail("guest@example.com");
        guestUser.setRole(RoleEnum.VISITOR);
        guestUser.setStorageLimit(0L);
        guestUser.setUsedStorage(0L);

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
        return Mono.defer(() -> {
            User user = new User();
            user.setUsername(registerUserDTO.getUsername());
            user.setPassword(passwordEncoder.encode(registerUserDTO.getPassword()));
            user.setEmail(registerUserDTO.getEmail());
            return userRepository.save(user).then();
        });
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
        return Mono.defer(() -> {
            UserLimiter userLimiter = userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_LOGIN_LIMITER);
            return userLimiter.tryAcquire(authRequestDTO.getUsername()).flatMap(acquired -> {
                if (acquired) {
                    return authorizationService.authenticate(authRequestDTO, request).doOnSuccess(token -> {
                        userLimiter.release(authRequestDTO.getUsername()).subscribeOn(Schedulers.boundedElastic()).subscribe();
                    });
                }
                return Mono.error(new LimitationException(LimitationException.ErrorCode.USER_EXCEED_LIMIT, "嘗試登入次數過多，請稍後再試"));
            });
        });
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
        if (Objects.equals(userId, 0L) || exchange == null) {
            return Mono.empty();
        }

        return exchange
                .getSession()
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED, "無法獲取 Session")))
                .flatMap(session -> userRepository
                        .findById(userId)
                        .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, "用戶ID: " + userId)))
                        .flatMap(user -> tokenService.revokeToken(user.getId(), TokenEnum.JWT_AUTHORIZATION_TOKEN).then(session.invalidate())))
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
            return Mono.defer(() -> userRepository
                    .findByEmail(userEmailDTO.getEmail())
                    .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, userEmailDTO.getEmail())))
                    .flatMap(user -> tokenService.generateToken(user, TokenEnum.RESET_PASSWORD_TOKEN).flatMap(token -> {
                        String content = String.format("重置密碼的憑證為：%s\n請於%s分鐘內重置密碼",
                                                       token,
                                                       securityProperties.getResetPasswordToken().getExpiration().toMinutes()
                        );
                        return provider.sendEmail(user.getEmail(), "重置密碼", content);
                    })));
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
        return userRepository
                .findByEmail(resetPasswordDTO.getEmail())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, resetPasswordDTO.getEmail())))
                .flatMap(user -> tokenService
                        .validateToken(resetPasswordDTO.getVerificationCode(), user.getId(), TokenEnum.RESET_PASSWORD_TOKEN)
                        .then(Mono.defer(() -> {
                            user.setPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
                            return userRepository.save(user).then(tokenService.revokeToken(user.getId(), TokenEnum.RESET_PASSWORD_TOKEN));
                        })));
    }


    /**
     * 用於獲取用戶的方法，根據請求對象獲取用戶對象
     * 先從Session中獲取用戶ID，如果Session中沒有則從SecurityContext中獲取
     * 當其中一個獲取到用戶ID時，則根據用戶ID獲取用戶對象
     * 如果都沒有獲取到用戶ID，則返回遊客身分的用戶對象
     *
     * @param exchange 請求對象
     *
     * @return Mono<User> 返回用戶對象
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<User> getUser(ServerWebExchange exchange) {
        return Mono.defer(() -> ReactiveSecurityContextHolder
                .getContext()
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED)))
                .map(SecurityContext::getAuthentication)
                .map(auth -> (Long) auth.getPrincipal())
                .flatMap(this::getById));
    }

    /**
     * 根據ID獲取一個實體
     *
     * @param userId 實體ID
     *
     * @return 返回一個Optional對象
     */
    private Mono<User> getByIdWithDB(Long userId) {
        return userRepository.findById(userId);
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
     * 根據用戶ID獲取用戶對象
     * 如果用戶ID為null，則返回錯誤
     * 如果用戶ID為0，則返回遊客用戶對象
     * 不然將從緩存管理器中獲取用戶對象
     *
     * @param userId 用戶ID
     *
     * @return Mono<User> 返回用戶對象
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<User> getById(Long userId) {
        if (userId == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, "空值的用戶ID"));
        }

        if (Objects.equals(userId, 0L)) {
            return Mono.just(guestUser);
        }

        List<CacheRule<User>> cacheRules = List.of(USER_ID_CACHE_RULE, USERNAME_CACHE_RULE);
        return cacheManager
                .runAndSetCache(userId.toString(), User.class, CacheProviderEnum.USER_CACHE, this.getByIdWithDB(userId), cacheRules)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, "用戶ID: " + userId)));
    }

    /**
     * 獲取所有實體
     */

    @Override
    @RecordLevel(LogLevelEnum.WARN)
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
    @RecordLevel(LogLevelEnum.DEBUG)
    public Flux<User> getAllByParams(String type, Object... args) {


        if (args == null || args.length == 0) {
            return Flux.error(new ValidationException(ValidationException.ErrorCode.SEARCH_CRITERIA_EMPTY));
        }


        if (type == null || (!Objects.equals(type, UserInfoTypeEnum.ID.name()) && !Objects.equals(type, UserInfoTypeEnum.NAME.name()))) {
            return Flux.error(new ValidationException(ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA, "無效的查詢類型: " + type));
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
            List<Long> validIds = userInfoList.stream().filter(id -> id != null && id.matches("\\d+"))

                                              .map(Long::parseLong).collect(Collectors.toList());


            if (validIds.isEmpty()) {
                return Flux.error(new ValidationException(ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA, "提供的ID均無效"));
            }
            userRepositoryChooseFlux = userRepository.findAllByIdIn(validIds);
        } else {
            List<String> validUsernames = userInfoList.stream().filter(username -> username != null && !username.trim().isEmpty())

                                                      .collect(Collectors.toList());


            if (validUsernames.isEmpty()) {
                return Flux.error(new ValidationException(ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA, "提供的使用者名稱均無效"));
            }
            userRepositoryChooseFlux = userRepository.findAllByUsernameIn(validUsernames);
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
    public Mono<User> update(User entity) {
        return Mono.empty();
    }


    /**
     * 刪除一個實體
     *
     * @param entity 實體對象
     */
    @Override
    @RecordLevel(LogLevelEnum.WARN)
    @RequirePermission(PermissionEnum.MANAGE)
    public Mono<Void> delete(User entity) {
        return Mono.empty();
    }
}
