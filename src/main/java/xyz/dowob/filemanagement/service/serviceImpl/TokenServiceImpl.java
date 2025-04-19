package xyz.dowob.filemanagement.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.strategy.TokenStrategy;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;

/**
 * 憑證業務邏輯實現類，實現接口 @see {@link TokenService}
 * 主要當用戶需要進行憑證操作時，進行相應的業務邏輯處理，根據用戶的請求進行憑證操作，並返回結果
 *
 * @author yuan
 * @program File-Management
 * @ClassName TokenServiceImpl
 * @description
 * @create 2024-09-20 00:47
 * @Version 1.0
 **/
@Service
@RecordLevel(LogLevelEnum.DEBUG)
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    /**
     * 憑證策略，根據憑證類型選擇憑證提供者
     */
    private final TokenStrategy tokenStrategy;

    /**
     * 根據憑證類型，交由組件生成憑證
     *
     * @param user      用戶ID
     * @param tokenType 憑證類型
     *
     * @return 返回憑證
     */
    @Override
    @HideSensitive
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<String> generateToken(User user, TokenEnum tokenType) {
        return tokenStrategy.getTokenProvider(tokenType).generateToken(user);
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
        return tokenStrategy.getTokenProvider(tokenType).validateToken(token, userId);
    }


    /**
     * 根據憑證類型刪除憑證
     *
     * @param userId    用戶ID
     * @param tokenType 憑證類型
     */
    @Override
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<Void> revokeToken(Long userId, TokenEnum tokenType) {
        return tokenStrategy.getTokenProvider(tokenType).revokeToken(userId);
    }


    /**
     * 創建一個新的實體
     *
     * @return 返回一個新的實體對象
     */
    @Override
    public Mono<Token> create() {
        return Mono.empty();
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
        return Mono.empty();
    }


    /**
     * 獲取所有實體
     */
    @Override
    public Flux<Token> getAll() {
        return Flux.empty();
    }


    /**
     * 獲取所有實體，根據參數進行過濾
     *
     * @param type 類型
     * @param args 參數
     *
     * @return 返回所有實體
     */
    @Override
    public Flux<Token> getAllByParams(String type, Object... args) {
        return Flux.empty();
    }


    /**
     * 更新一個實體
     *
     * @param token 實體對象
     */
    @Override
    public Mono<Void> update(Token token) {
        return Mono.empty();
    }


    /**
     * 刪除一個實體
     *
     * @param token 實體對象
     */
    @Override
    public Mono<Void> delete(Token token) {
        return Mono.empty();
    }
}
