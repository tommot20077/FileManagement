package xyz.dowob.filemanagement.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
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

    private JwtToken jwtToken = new JwtToken();
    private resetPasswordToken resetPasswordToken = new resetPasswordToken();

    @Data
    public static class JwtToken {
        private String secret;
        private int expiration = 1440;
    }

    @Data
    public static class resetPasswordToken {
        private int length = 6;
        private int expiration = 30;
    }

    @PostConstruct
    public void validateJwtSecret() {
        if (jwtToken.getSecret() == null || jwtToken.getSecret().trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 密鑰需要配置，請在 application 中配置 security.jwt-token.secret");
        }
    }
}
