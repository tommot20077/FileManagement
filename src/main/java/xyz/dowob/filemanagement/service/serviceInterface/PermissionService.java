package xyz.dowob.filemanagement.service.serviceInterface;

import jakarta.annotation.Nullable;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.functionInterface.Permission;

import java.util.Collection;
import java.util.Map;

/**
 * 檔案權限服務核心介面，定義完整的檔案存取控制和權限驗證機制。
 * <p>
 * 本介面提供靈活、高效能且可擴展的權限驗證框架，專為處理複雜的檔案存取控制場景而設計。
 * 採用反應式編程模式確保在高並發環境下的優異效能表現，支援單一檔案和批量檔案的權限驗證操作。
 * <p>
 * <strong>核心設計理念：</strong>
 * <ul>
 *   <li><strong>靈活性：</strong>可插拔的權限規則系統，支援自定義權限邏輯</li>
 *   <li><strong>非阻塞：</strong>基於 WebFlux 反應式編程，確保高併發處理能力</li>
 *   <li><strong>可擴展：</strong>支援多種驗證策略和複雜的權限控制邏輯</li>
 *   <li><strong>類型安全：</strong>泛型設計支援多種權限結果類型</li>
 *   <li><strong>批量處理：</strong>高效的批量權限檢查機制</li>
 * </ul>
 * <p>
 * <strong>權限驗證模式：</strong>
 * <ul>
 *   <li><strong>預設驗證：</strong>使用系統內建的權限規則進行標準檢查</li>
 *   <li><strong>自定義驗證：</strong>透過 {@link Permission} 函數介面實現客製化權限邏輯</li>
 *   <li><strong>組合驗證：</strong>支援多個權限規則的組合使用</li>
 *   <li><strong>條件驗證：</strong>基於上下文的動態權限評估</li>
 * </ul>
 * <p>
 * <strong>支援的權限類型：</strong>
 * <ul>
 *   <li><strong>Boolean 類型：</strong>簡單的允許/拒絕權限判斷</li>
 *   <li><strong>枚舉類型：</strong>多級權限等級（如：READ, WRITE, ADMIN）</li>
 *   <li><strong>自定義類型：</strong>複雜的權限物件和結構</li>
 *   <li><strong>複合類型：</strong>包含多種權限資訊的組合物件</li>
 * </ul>
 * <p>
 * <strong>效能最佳化：</strong>
 * <ul>
 *   <li>並行處理批量權限檢查，降低總體延遲</li>
 *   <li>非阻塞 I/O 操作，提升系統吞吐量</li>
 *   <li>智慧快取機制（由實作類決定）</li>
 *   <li>懶載入權限資料，減少不必要的資料庫查詢</li>
 * </ul>
 * <p>
 * <strong>錯誤處理策略：</strong>
 * <ul>
 *   <li>使用 {@code Mono.error()} 傳播驗證失敗和系統錯誤</li>
 *   <li>提供詳細的錯誤資訊和錯誤碼</li>
 *   <li>支援錯誤恢復和降級處理</li>
 *   <li>遵循反應式編程的錯誤處理最佳實踐</li>
 * </ul>
 * <p>
 * <strong>使用範例：</strong>
 * <pre>{@code
 * // 基本權限驗證
 * PermissionService<Boolean> permissionService = ...;
 * 
 * // 單一檔案權限檢查
 * Mono<Boolean> hasReadPermission = permissionService
 *     .validateUserPermission(user, fileId);
 * 
 * // 使用自定義權限規則
 * Collection<Permission<Boolean>> customRules = List.of(
 *     Permission.of("owner", (u, f) -> f.getOwnerId().equals(u.getId())),
 *     Permission.of("admin", (u, f) -> u.hasRole("ADMIN"))
 * );
 * 
 * Mono<Boolean> customPermission = permissionService
 *     .validateUserPermission(user, fileId, customRules);
 * 
 * // 批量檔案權限檢查
 * Set<Long> fileIds = Set.of(1L, 2L, 3L, 4L, 5L);
 * Mono<Map<Long, Boolean>> batchPermissions = permissionService
 *     .validateUserPermission(user, fileIds);
 * 
 * // 處理結果
 * batchPermissions.subscribe(results -> {
 *     results.forEach((fileId, hasPermission) -> {
 *         if (hasPermission) {
 *             log.info("用戶可存取檔案: {}", fileId);
 *         } else {
 *             log.warn("用戶無權存取檔案: {}", fileId);
 *         }
 *     });
 * });
 * 
 * // 複雜權限類型範例
 * public enum FilePermission { READ, WRITE, DELETE, ADMIN }
 * PermissionService<FilePermission> advancedService = ...;
 * 
 * Mono<FilePermission> userPermissionLevel = advancedService
 *     .validateUserPermission(user, fileId);
 * }</pre>
 * <p>
 * <strong>實作建議：</strong>
 * <ul>
 *   <li>實作類別應考慮快取機制以提升效能</li>
 *   <li>批量操作應實現並行處理以降低延遲</li>
 *   <li>自定義權限規則應支援短路評估</li>
 *   <li>錯誤處理應提供有意義的錯誤訊息</li>
 *   <li>考慮實現權限繼承和權限委派機制</li>
 * </ul>
 *
 * @param <T> 權限驗證結果的類型，支援 Boolean、枚舉或自定義權限物件
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see reactor.core.publisher.Mono
 * @see xyz.dowob.filemanagement.functionInterface.Permission
 * @see xyz.dowob.filemanagement.entity.User
 * @see java.util.Map
 * @see java.util.Collection
 */

public interface PermissionService<T> {
    /**
     * 使用默認權限規則驗證用戶對單一檔案的存取權限。
     * <p>
     * 此方法是 {@link #validateUserPermission(User, Long, Collection)} 的預設實現，
     * 提供了一個便捷的方式來驗證用戶對特定檔案的基本權限。
     * 當未指定自定義權限規則時，將使用系統預設的權限檢查機制。
     * </p>
     * <p>
     * 運作流程：
     * <ul>
     *   <li>若用戶為 null，通常回傳拒絕存取的結果</li>
     *   <li>若檔案 ID 不存在，拋出對應的反應式例外</li>
     *   <li>根據預設規則評估使用者權限</li>
     * </ul>
     * </p>
     *
     * @param user   執行權限驗證的使用者對象，不可為 null
     * @param fileId 需要驗證權限的檔案唯一識別碼
     * @return 包裝權限驗證結果的 Mono 反應流，代表是否允許存取
     * @see #validateUserPermission(User, Long, Collection)
     * @see reactor.core.publisher.Mono
     */
    default Mono<T> validateUserPermission(User user, Long fileId) {
        return validateUserPermission(user, fileId, null);
    }

    /**
     * 使用指定或自定義的權限規則驗證用戶對單一檔案的存取權限。提供靈活的權限驗證機制，可透過傳入自定義權限規則來微調權限檢查流程，支援複雜的權限邏輯，如多層級驗證、條件式存取控制等。
     * <p>
     * 使用場景包括需要針對特定檔案實施特殊存取控制、要求更細粒度的權限管理、實現動態、可插拔的權限驗證策略。完全支持非阻塞、反應式編程模式，權限驗證失敗時透過 Mono.error() 傳播錯誤，允許傳入 null 使用預設權限規則。
     * <p>
     * 範例用法：
     * <pre>
     * // 使用預設規則
     * Mono<Boolean> defaultPermission = permissionService
     *     .validateUserPermission(user, fileId);
     * 
     * // 使用自定義權限規則
     * Collection<Permission<Boolean>> customRules = ...; // 自定義規則
     * Mono<Boolean> customPermission = permissionService
     *     .validateUserPermission(user, fileId, customRules);
     * </pre>
     *
     * @param user   執行權限驗證的使用者對象，不可為 null
     * @param fileId 需要驗證權限的檔案唯一識別碼
     * @param rules  可選的自定義權限規則集合，若為 null 則使用預設規則
     * @return 包裝權限驗證結果的 Mono 反應流
     * @see Permission
     * @see reactor.core.publisher.Mono
     */
    Mono<T> validateUserPermission(User user, Long fileId, @Nullable Collection<Permission<T>> rules);

    /**
     * 使用預設權限規則批次驗證用戶對多個檔案的存取權限。此方法是 {@link #validateUserPermission(User, Iterable, Collection)} 的預設實現，提供了一次性檢查多個檔案權限的便捷方法，系統將針對每個檔案 ID 進行並行權限驗證。
     * <p>
     * 支援大量檔案的高效率權限檢查，回傳一個映射，鍵為檔案 ID，值為對應的權限驗證結果，使用系統預設的權限檢查機制。
     * <p>
     * 範例用法：
     * <pre>
     * Set<Long> fileIds = Set.of(1L, 2L, 3L);
     * Mono<Map<Long, Boolean>> permissions = permissionService
     *     .validateUserPermission(user, fileIds);
     * 
     * // 結果將類似於：{1L: true, 2L: false, 3L: true}
     * </pre>
     *
     * @param user   執行權限驗證的使用者對象，不可為 null
     * @param fileId 需要驗證權限的檔案唯一識別碼集合
     * @return 包裝檔案權限映射的 Mono 反應流
     * @see #validateUserPermission(User, Iterable, Collection)
     * @see reactor.core.publisher.Mono
     */
    default Mono<Map<Long, T>> validateUserPermission(User user, Iterable<Long> fileId) {
        return validateUserPermission(user, fileId, null);
    }

    /**
     * 使用指定或自定義的權限規則批次驗證用戶對多個檔案的存取權限。提供最靈活且強大的多檔案權限驗證機制，支援高度客製化的權限檢查策略。可針對不同檔案應用不同的權限規則，實現複雜的存取控制邏輯。
     * <p>
     * 使用場景包括需要對大量檔案進行差異化權限管理、實現複雜的多層級權限檢查、支援高級權限策略，如上下文相關的條件存取。完全支持非阻塞、反應式編程模式，並行處理多個檔案的權限驗證，若驗證失敗，透過 Mono.error() 傳播錯誤，允許傳入 null 使用預設權限規則。
     * <p>
     * 範例用法：
     * <pre>
     * Set<Long> fileIds = Set.of(1L, 2L, 3L);
     * Collection<Permission<Boolean>> customRules = ...; // 自定義規則
     * Mono<Map<Long, Boolean>> permissions = permissionService
     *     .validateUserPermission(user, fileIds, customRules);
     * 
     * // 結果將包含每個檔案的個別權限狀態
     * </pre>
     *
     * @param user   執行權限驗證的使用者對象，不可為 null
     * @param fileId 需要驗證權限的檔案唯一識別碼集合
     * @param rules  可選的自定義權限規則集合，若為 null 則使用預設規則
     * @return 包裝檔案權限映射的 Mono 反應流
     * @see Permission
     * @see reactor.core.publisher.Mono
     */
    Mono<Map<Long, T>> validateUserPermission(User user, Iterable<Long> fileId, @Nullable Collection<Permission<T>> rules);
}
