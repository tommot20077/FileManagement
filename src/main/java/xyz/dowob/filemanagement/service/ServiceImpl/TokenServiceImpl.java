package xyz.dowob.filemanagement.service.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.component.factory.TokenStrategyFactory;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.service.ServiceInterFace.TokenService;

/**
 * @author yuan
 * @program File-Management
 * @ClassName TokenServiceImpl
 * @description
 * @create 2024-09-20 00:47
 * @Version 1.0
 **/
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final TokenStrategyFactory tokenStrategyFactory;

    /**
     * 根據憑證類型，交由組件生成憑證
     *
     * @param user    用戶ID
     * @param tokenType 憑證類型
     *
     * @return 返回憑證
     */
    @Override
    @HideSensitive
    public Mono<String> generateToken(User user, TokenEnum tokenType) {
        return tokenStrategyFactory.getTokenProvider(tokenType).generateToken(user);
    }

    /**
     * 驗證憑證，並返回用戶ID
     * 此實現類中，直接調用JWT Token提供者的驗證憑證方法
     *
     * @param token     憑證
     * @param userId    用戶ID
     * @param tokenType 憑證類型
     *
     * @return 返回用戶ID
     */
    @Override
    public Mono<Long> validateToken(String token, Long userId, TokenEnum tokenType) {
        return tokenStrategyFactory.getTokenProvider(tokenType).validateToken(token, userId);
    }

    /**
     * 根據憑證類型刪除憑證
     *
     * @param userId    用戶ID
     * @param tokenType 憑證類型
     */
    @Override
    public Mono<Void> revokeToken(Long userId, TokenEnum tokenType) {
        return tokenStrategyFactory.getTokenProvider(tokenType).revokeToken(userId);
    }

    /**
     * 創建一個新的實體
     *
     * @return 返回一個新的實體對象
     */
    @Override
    public Mono<Token> create() {
        return null;
    }

    /**
     * 根據ID獲取一個實體
     *
     * @param tokenId 實體ID
     *
     * @return 返回一個Optional對象
     */
    @Override
    public Mono<Token> getById(Long tokenId) {
        return null;
    }

    /**
     * 獲取所有實體
     */
    @Override
    public Flux<Token> getAll() {
        return null;
    }

    /**
     * 更新一個實體
     *
     * @param token 實體對象
     */
    @Override
    public Mono<Void> update(Token token) {
        return null;
    }

    /**
     * 刪除一個實體
     *
     * @param token 實體對象
     */
    @Override
    public Mono<Void> delete(Token token) {
        return null;
    }
}
