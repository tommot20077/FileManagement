package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;

import java.util.Collection;

/**
 * 用戶資料存取層介面，提供響應式的用戶資料庫操作功能。
 * <p>
 * 此介面繼承自 Spring Data R2DBC 的 {@link ReactiveCrudRepository}，
 * 提供完整的響應式 CRUD 操作能力，支援非阻塞的資料庫存取。
 * 專門用於管理系統用戶的基本資訊、認證資料和權限設定。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>基本的 CRUD 操作（繼承自父介面）</li>
 *   <li>根據用戶名進行單筆和批次查詢</li>
 *   <li>根據電子信箱進行單筆和批次查詢</li>
 *   <li>根據用戶 ID 進行批次查詢</li>
 *   <li>支援響應式流程的非阻塞資料庫操作</li>
 * </ul>
 * </p>
 * <p>
 * 使用範例：
 * <pre>{@code
 * @Autowired
 * private UserRepository userRepository;
 * 
 * // 根據用戶名查詢用戶
 * Mono<User> user = userRepository.findByUsername("admin");
 * 
 * // 批次查詢多個用戶
 * List<String> usernames = Arrays.asList("user1", "user2", "user3");
 * Flux<User> users = userRepository.findAllByUsernameIn(usernames);
 * }</pre>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see User
 * @see ReactiveCrudRepository
 * @see Repository
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    /**
     * 根據用戶名查詢單一用戶資訊。
     * <p>
     * 此方法提供基於用戶名的精確匹配查詢，通常用於用戶登入驗證、
     * 用戶名唯一性檢查和用戶資料獲取等場景。查詢結果以響應式流的形式回傳。
     * </p>
     *
     * @param username 要查詢的用戶名，不得為 null 或空字串
     * @return 包含符合條件用戶的 {@link Mono}，如果找不到則為空
     */
    Mono<User> findByUsername(String username);

    /**
     * 根據用戶名集合進行批量查詢用戶資訊。
     * <p>
     * 此方法允許一次性查詢多個用戶的資訊，提供高效的批量處理能力。
     * 通常用於用戶權限檢查、批量資料獲取和系統管理功能。
     * 查詢結果以響應式流的形式逐個回傳。
     * </p>
     *
     * @param usernames 要查詢的用戶名集合，不得為 null 或包含 null 元素
     * @return 包含所有符合條件用戶的 {@link Flux}，可能為空流
     */
    Flux<User> findAllByUsernameIn(Collection<String> usernames);

    /**
     * 根據用戶 ID 集合進行批量查詢用戶資訊。
     * <p>
     * 此方法通過主鍵 ID 進行高效的批量查詢，適用於需要根據用戶 ID 
     * 集合獲取詳細資訊的場景，如檔案擁有者資訊查詢、用戶權限驗證等。
     * 使用主鍵查詢確保了最佳的效能表現。
     * </p>
     *
     * @param userIds 要查詢的用戶 ID 集合，不得為 null 或包含 null 元素
     * @return 包含所有符合條件用戶的 {@link Flux}，可能為空流
     */
    Flux<User> findAllByIdIn(Collection<Long> userIds);

    /**
     * 根據電子信箱查詢單一用戶資訊。
     * <p>
     * 此方法提供基於電子信箱的精確匹配查詢，通常用於用戶註冊時的
     * 信箱唯一性檢查、密碼重設功能和用戶身分驗證等場景。
     * 查詢結果以響應式流的形式回傳。
     * </p>
     *
     * @param email 要查詢的電子信箱地址，不得為 null 或空字串
     * @return 包含符合條件用戶的 {@link Mono}，如果找不到則為空
     */
    Mono<User> findByEmail(String email);

    /**
     * 根據電子信箱集合進行批量查詢用戶資訊。
     * <p>
     * 此方法允許一次性查詢多個電子信箱對應的用戶資訊，提供高效的批量處理能力。
     * 通常用於批量用戶邀請、郵件通知系統和用戶資料同步等場景。
     * 查詢結果以響應式流的形式逐個回傳。
     * </p>
     *
     * @param emails 要查詢的電子信箱地址集合，不得為 null 或包含 null 元素
     * @return 包含所有符合條件用戶的 {@link Flux}，可能為空流
     */
    Flux<User> findAllByEmailIn(Collection<String> emails);

}
