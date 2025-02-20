package xyz.dowob.filemanagement.component.provider.provider;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.customenum.PermissionEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.repostiory.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName UserListProvider
 * @create 2025/2/20
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
public class UserListProvider implements ApplicationRunner {
    /**
     * 用戶列表，用於存儲所有用戶的信息
     */
    private static final HashMap<String, User> USER_LIST = new HashMap<>();
    /**
     * 用戶數據庫操作對象
     */
    private final UserRepository userRepository;

    /**
     * @param args 啟動參數
     *
     * @throws Exception 啟動異常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        userRepository.findAll().flatMap(user -> {
            User cacheUser = new User();
            cacheUser.setId(user.getId());
            cacheUser.setUsername(user.getUsername());
            cacheUser.setEmail(user.getEmail());
            USER_LIST.put(cacheUser.getEmail(), cacheUser);
            return Mono.empty();
        }).subscribe();
    }

    public Flux<User> getUserList(@NonNull List<String> mailList) {
        if (mailList.isEmpty()) {
            return Flux.empty();
        } else if (mailList.size() > 10) {
            return Flux.error(new IllegalArgumentException("The number of mails cannot exceed 10"));
        }

        Set<User> users = USER_LIST
                .entrySet()
                .stream()
                .filter(entry -> mailList.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        return Flux.fromIterable(users);
    }

    public Mono<User> getUserById(String mail) {
        return Mono.justOrEmpty(USER_LIST.get(mail));
    }

    @RequirePermission(PermissionEnum.MANAGE)
    public Flux<User> getUserListWithAll() {
        return Flux.fromIterable(USER_LIST.values());
    }
}
