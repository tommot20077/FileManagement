package xyz.dowob.filemanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import xyz.dowob.filemanagement.customenum.RoleEnum;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * 用於定義用戶的數據庫表實體
 * @author yuan
 * @program FileManagement
 * @ClassName User
 * @description
 * @create 2024-09-23 13:43
 * @Version 1.0
 **/

@Table(name = "users")
@Getter
@Setter
public class User {
    /**
     * 用戶的主鍵ID
     */
    @Id
    private Long id;

    /**
     * 用戶名稱
     */
    private String username;

    /**
     * 用戶密碼
     */
    @JsonIgnore
    private String password;

    /**
     * 信箱
     */
    private String email;

    /**
     * 用戶角色
     */
    private RoleEnum role = RoleEnum.USER;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("id", id);
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("role", role);
        return userMap.toString();
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
