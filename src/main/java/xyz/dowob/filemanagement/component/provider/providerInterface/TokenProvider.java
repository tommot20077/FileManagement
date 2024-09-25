package xyz.dowob.filemanagement.component.provider.providerInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;

/**
 * 憑證提供者接口，用於定義憑證提供者的方法
 *
 * @author yuan
 * @program File-Management
 * @ClassName TokenProvider
 * @description
 * @create 2024-09-20 13:10
 * @Version 1.0
 **/
public interface TokenProvider {
    /**
     * 生成憑證
     *
     * @param user 用戶
     *
     * @return 返回生成的憑證
     */
    Mono<String> generateToken(User user);

    /**
     * 驗證憑證
     *
     * @param token  憑證
     * @param userId 用戶ID
     *
     * @return 返回用戶ID
     */
    Mono<Long> validateToken(String token, Long userId);

    /**
     * 撤銷憑證
     *
     * @param userId 用戶ID
     */
    Mono<Void> revokeToken(Long userId);

}
