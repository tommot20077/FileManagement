package xyz.dowob.filemanagement.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 安全配置文件，用於配置安全相關的參數，在 application 中配置 security
 *
 * @author yuan
 * @program FileManagement
 * @ClassName SecurityConfig
 * @description
 * @create 2024-10-03 22:38
 * @Version 1.0
 **/

@ConfigurationProperties(prefix = "security")
@Configuration
@Data
public class SecurityProperties {

    /**
     * JWT 令牌
     */
    private JwtToken jwtToken = new JwtToken();

    /**
     * 重置密碼憑證配置
     */
    private resetPasswordToken resetPasswordToken = new resetPasswordToken();

    /**
     * 驗證 JWT 密鑰是否配置
     */
    @PostConstruct
    public void validateJwtSecret() {
        if (jwtToken.getSecret() == null || jwtToken.getSecret().trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 密鑰需要配置，請在 application 中配置 security.jwt-token.secret");
        }
    }

    /**
     * JWT 令牌設定
     */
    @Data
    public static class JwtToken {
        /**
         * JWT 密鑰
         */
        private String secret;

        /**
         * JWT 令牌過期時間，單位為分鐘，默認為 1440 分鐘
         */
        private int expiration = 1440;
    }

    /**
     * 重置密碼憑證配置
     */
    @Data
    public static class resetPasswordToken {
        /**
         * 重置密碼憑證的長度，默認為 6 位
         */
        private int length = 6;

        /**
         * 重置密碼憑證的過期時間，單位為分鐘，默認為 30 分鐘
         */
        private int expiration = 30;
    }
}
