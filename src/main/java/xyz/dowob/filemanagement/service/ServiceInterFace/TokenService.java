package xyz.dowob.filemanagement.service.ServiceInterFace;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;

/**
 * 憑證服務接口
 * 定義憑證會根據在工廠中的生成、驗證、刪除方法
 * @author yuan
 * @program File-Management
 * @ClassName TokenService
 * @description
 * @create 2024-09-20 00:40
 * @Version 1.0
 **/
public interface TokenService extends CrudService<Token, Long> {
    /**
     * 根據憑證類型生成憑證
     *
     * @param user    用戶實體
     * @param tokenType 憑證類型
     *
     * @return 返回憑證
     */
    Mono<String> generateToken(User user, TokenEnum tokenType);

    /**
     * 根據憑證類型驗證憑證
     *
     * @param token     憑證
     * @param tokenType 憑證類型
     *
     * @return 返回用戶ID
     */
    Mono<Long> validateToken(String token, Long userId, TokenEnum tokenType);

    /**
     * 根據憑證類型刪除憑證
     *
     * @param userId    用戶ID
     * @param tokenType 憑證類型
     */
    Mono<Void> revokeToken(Long userId, TokenEnum tokenType);
}
