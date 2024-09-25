package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 憑證實體類，用於定義憑證的數據庫表結構
 *
 * @author yuan
 * @program File-Management
 * @ClassName Token
 * @description
 * @create 2024-09-18 17:00
 * @Version 1.0
 **/
@Getter
@Setter
@Table(name = "tokens")
public class Token {
    /**
     * 憑證的主鍵ID
     */
    @Id
    private long id;

    /**
     * 用戶ID
     */
    @Column("user_id")
    private long userId;

    /**
     * JWT憑證版本
     */
    @Column("jwt_token_version")
    private int jwtTokenVersion;

    /**
     * JWT憑證過期時間
     */
    // todo 需要刪除過期憑證
    @Column("jwt_token_expire_time")
    private LocalDateTime jwtTokenExpireTime;

    /**
     * 重置密碼驗證碼
     */
    @Column("reset_verification_code")
    private String resetVerificationCode;

    /**
     * 重置密碼驗證碼過期時間
     */
    @Column("reset_verification_code_expire_time")
    private LocalDateTime resetVerificationCodeExpireTime;

    /**
     * 取得JWT憑證的版本並+1
     *
     * @return JWT憑證的版本
     */
    public int increaseAndGetJwtTokenVersion() {
        this.jwtTokenVersion = jwtTokenVersion + 1;
        return jwtTokenVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Token token = (Token) o;
        return id == token.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        HashMap<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("id", id);
        tokenMap.put("userId", userId);
        return tokenMap.toString();
    }
}
