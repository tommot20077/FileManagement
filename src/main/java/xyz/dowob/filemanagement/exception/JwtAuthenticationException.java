package xyz.dowob.filemanagement.exception;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * JWT驗證異常類，security 驗證需要繼承 AuthenticationException
 *
 * @author yuan
 * @program File-Management
 * @ClassName JwtAuthenticationException
 * @description
 * @create 2024-09-18 21:52
 * @Version 1.0
 **/
@Getter
public class JwtAuthenticationException extends AuthenticationException {
    /**
     * 異常信息
     *
     * @param msg 異常信息
     */
    public JwtAuthenticationException(String msg) {
        super(msg);
    }
}
