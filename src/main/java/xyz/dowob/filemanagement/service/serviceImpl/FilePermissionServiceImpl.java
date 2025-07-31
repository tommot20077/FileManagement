package xyz.dowob.filemanagement.service.serviceImpl;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;

import java.util.Collection;
import java.util.Map;

/**
 * 檔案權限服務實現類，負責檔案存取權限的驗證和管理。採用可插拔的權限規則架構，支援靈活的權限驗證邏輯定制。
 * 當使用者存取檔案時，系統會根據設定的權限規則進行驗證，確保資料安全。
 * <p>
 * 支援多層權限規則驗證機制，提供擁有者、共享、訪客等多種存取模式。整合檔案生命週期狀態檢查，
 * 採用響應式編程模式支援高併發驗證，支援批量檔案權限驗證操作。
 * <p>
 * 權限驗證採用「全部通過」策略，所有設定的權限規則必須同時滿足才能通過驗證。
 * 系統保留 ID（如回收站 ID、根目錄 ID）會建立虛擬檔案元資料進行特殊處理。
 * <p>
 * 預設權限規則為 {@code ONLY_OWNER}，僅允許檔案擁有者存取。可透過參數傳入自定義權限規則集合。
 * 所有驗證失敗情況通過 {@code Mono.error()} 傳播 {@code ValidationException}。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.service.serviceInterface.PermissionService
 */
@Service
@RecordLevel(LogLevelEnum.DEBUG)
@RequiredArgsConstructor
public class FilePermissionServiceImpl implements PermissionService<UserFileMetadata> {

    /**
     * 用戶檔案元資料庫存取介面，用於查詢和操作檔案元資料
     */
    private final UserFileMetaRepository userFileMetaRepository;

    /**
     * 檔案權限規則管理器，提供預定義的權限規則集合和規則管理功能
     */
    private final FilePermissionRuleManager filePermissionRuleManager;

    /**
     * 驗證用戶對單一檔案的存取權限。
     * <p>
     * 此方法通過可配置的權限規則集合執行權限驗證，支援多種存取模式（擁有者、共享、訪客等）。
     * 當用戶訪問檔案時，系統依序執行：查詢檔案元資料、執行所有權限規則檢查、
     * 所有規則通過時回傳檔案元資料。
     * <p>
     * 當檔案 ID 不存在於資料庫時，系統會檢查是否為系統保留 ID（如回收站、根目錄），
     * 並建立對應的虛擬檔案元資料進行特殊處理。
     * <p>
     * 權限驗證採用「全部通過」策略，任一規則失敗將導致整個檢查失敗。
     * 若未指定權限規則，系統使用預設的 {@code ONLY_OWNER} 規則。
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * // 使用預設權限規則驗證
     * validateUserPermission(currentUser, 123L, null)
     *     .doOnSuccess(file -> log.info("用戶 {} 有權限存取檔案 {}", 
     *         currentUser.getUsername(), file.getFilename()))
     *     .subscribe();
     * 
     * // 使用自定義權限規則
     * Collection<Permission<UserFileMetadata>> customRules = 
     *     FilePermissionRuleManager.DefaultRule.SHARED_ACCESS.getRules(ruleManager);
     * validateUserPermission(currentUser, 123L, customRules)
     *     .subscribe();
     * }</pre>
     *
     * @param user 請求存取的用戶對象，不可為 {@code null}
     * @param fileId 要驗證權限的檔案 ID，不可為 {@code null}
     * @param rules 權限規則集合，{@code null} 或空集合時使用預設的 {@code ONLY_OWNER} 規則
     * @return 包含檔案元資料的 {@code Mono}，權限驗證失敗時傳播 {@code ValidationException}
     */
    @Override
    public Mono<UserFileMetadata> validateUserPermission(User user, Long fileId, @Nullable Collection<Permission<UserFileMetadata>> rules) {
        return userFileMetaRepository
                .findById(fileId.toString())
                .switchIfEmpty(reservedSearchMethod(user, fileId))
                .flatMap(file -> checkPermissions(user, file, rules));
    }


    /**
     * 批量驗證用戶對多個檔案的存取權限。
     * <p>
     * 此方法提供高效的批量權限驗證機制，適用於需要同時檢查多個檔案權限的場景，
     * 如檔案列表顯示、批量操作權限檢查等。使用響應式程式設計，並行處理所有檔案的權限驗證。
     * <p>
     * 處理流程依序執行：檢查輸入參數有效性、對每個檔案 ID 執行查詢和權限驗證、
     * 收集所有通過驗證的檔案元資料、轉換為 Map 結構方便查找。
     * <p>
     * 重要特性：
     * <ul>
     * <li>並行處理：所有檔案的權限驗證同時執行，提高性能</li>
     * <li>適當的失敗處理：任一檔案權限驗證失敗不影響其他檔案</li>
     * <li>空集合處理：當輸入為空或 null 時直接返回空 Map</li>
     * <li>保留 ID 支援：自動處理系統保留檔案 ID</li>
     * </ul>
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * List<Long> fileIds = Arrays.asList(1L, 2L, 3L, 999L); // 999L 為不存在的檔案
     * 
     * validateUserPermission(currentUser, fileIds, null)
     *     .doOnSuccess(fileMap -> {
     *         log.info("成功驗證 {} 個檔案權限", fileMap.size());
     *         fileMap.forEach((id, file) -> 
     *             log.debug("檔案 ID: {}, 名稱: {}", id, file.getFilename()));
     *     })
     *     .subscribe();
     * 
     * // 結果將只包含用戶有權限存取的檔案，不存在或無權限的檔案不會包含在結果中
     * }</pre>
     *
     * @param user 請求存取的用戶對象，不可為 {@code null}
     * @param fileIds 要驗證權限的檔案 ID 集合，可為 {@code null} 或空集合
     * @param rules 權限規則集合，{@code null} 或空集合時使用預設的 {@code ONLY_OWNER} 規則
     * @return 包含檔案 ID 到檔案元資料映射的 {@code Mono<Map>}，只包含通過權限驗證的檔案
     */
    @Override
    public Mono<Map<Long, UserFileMetadata>> validateUserPermission(User user, Iterable<Long> fileIds,
                                                                    @Nullable Collection<Permission<UserFileMetadata>> rules) {
        if (fileIds == null || !fileIds.iterator().hasNext()) {
            return Mono.just(Map.of());
        }
        return Flux
                .fromIterable(fileIds)
                .flatMap(fileId -> userFileMetaRepository.findById(fileId.toString()).switchIfEmpty(reservedSearchMethod(user, fileId)))
                .flatMap(file -> checkPermissions(user, file, rules))
                .collectMap(UserFileMetadata::getId, file -> file);
    }

    /**
     * 處理系統保留檔案 ID 的特殊方法。
     * <p>
     * 當無法在資料庫中找到指定檔案時，此方法檢查檔案 ID 是否為系統保留的特殊標識符。
     * 系統保留 ID 包括回收站 ID、根目錄 ID 等，用於表示虛擬檔案系統結構中的特殊節點。
     * <p>
     * 處理流程：
     * <ol>
     * <li>檢查檔案 ID 是否匹配任一已知的保留搜尋 ID</li>
     * <li>若匹配，建立對應的虛擬檔案元資料對象</li>
     * <li>若不匹配，傳播檔案不存在的驗證異常</li>
     * </ol>
     * <p>
     * 虛擬檔案元資料的特殊屬性設定：
     * <ul>
     * <li>檔案類型：固定設定為 {@code FileEnum.FOLDER}</li>
     * <li>擁有者：設定為當前請求用戶</li>
     * <li>刪除狀態：根據保留 ID 類型決定（回收站 ID 為 true，其他為 false）</li>
     * </ul>
     * <p>
     * 此方法為私有方法，僅供內部權限驗證流程使用。在正常情況下，
     * 虛擬檔案不允許執行修改操作，只用於讀取和導航操作。
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * // 內部方法調用示例
     * Long recycleId = ReservedSearchIdEnum.RECYCLE_FILE_ID.getId();
     * reservedSearchMethod(currentUser, recycleId)
     *     .doOnNext(virtualFile -> {
     *         assert virtualFile.getIsDeleted() == true;
     *         assert virtualFile.getFileType() == FileEnum.FOLDER;
     *         assert virtualFile.getUserId().equals(currentUser.getId());
     *         log.debug("建立回收站虛擬檔案元資料");
     *     })
     *     .subscribe();
     * 
     * // 非保留 ID 的情況
     * reservedSearchMethod(currentUser, 999L)
     *     .doOnError(ValidationException.class, error -> 
     *         log.warn("檔案 ID {} 不存在", 999L))
     *     .subscribe();
     * }</pre>
     *
     * @param user 當前用戶對象，用於設定虛擬檔案的擁有者，不可為 {@code null}
     * @param fileId 要檢查的檔案 ID，不可為 {@code null}
     * @return 包含虛擬檔案元資料的 {@code Mono}，若非保留 ID 則傳播 {@code ValidationException}
     * @see ReservedSearchIdEnum
     * @see ValidationException.ErrorCode#NOT_EXISTING_USER_FILE
     */
    private Mono<UserFileMetadata> reservedSearchMethod(User user, Long fileId) {
        ReservedSearchIdEnum reservedSearchIdEnum = ReservedSearchIdEnum.format(fileId);
        if (ReservedSearchIdEnum.format(fileId) != null) {
            UserFileMetadata dummyData = new UserFileMetadata();
            dummyData.setId(fileId);
            dummyData.setUserId(user.getId());
            dummyData.setIsDeleted(reservedSearchIdEnum == ReservedSearchIdEnum.RECYCLE_FILE_ID);
            dummyData.setFileType(FileEnum.FOLDER);
            return Mono.just(dummyData);
        }
        return Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId));
    }

    /**
     * 執行權限規則檢查的核心方法。
     * <p>
     * 此方法封裝了實際的權限驗證邏輯，支援自定義權限規則和預設規則兩種模式。
     * 為確保系統安全性，當未明確指定權限規則時，自動使用預設的 {@code ONLY_OWNER} 規則，
     * 僅允許檔案擁有者存取。
     * <p>
     * 驗證機制特性：
     * <ul>
     * <li><strong>全部通過策略</strong>：所有設定的權限規則必須都通過驗證</li>
     * <li><strong>短路機制</strong>：任一規則失敗將立即中斷檢查並返回錯誤</li>
     * <li><strong>並行處理</strong>：使用響應式流同時執行多個規則檢查</li>
     * <li><strong>錯誤優先級</strong>：返回第一個遇到的驗證錯誤</li>
     * </ul>
     * <p>
     * 檢查流程：
     * <ol>
     * <li>確定要使用的權限規則集合（自定義或預設）</li>
     * <li>將規則集合轉換為響應式流</li>
     * <li>並行執行所有權限規則的 {@code check} 方法</li>
     * <li>收集所有驗證結果（成功或異常）</li>
     * <li>若無錯誤則返回檔案元資料，否則返回第一個錯誤</li>
     * </ol>
     * <p>
     * 此方法為私有方法，僅供內部權限驗證流程使用，不對外部暴露。
     * 使用者應通過公共方法 {@link #validateUserPermission} 進行權限驗證。
     * <p>
     * <strong>內部使用示例：</strong>
     * <pre>{@code
     * // 使用自定義規則集合
     * Collection<Permission<UserFileMetadata>> customRules = Arrays.asList(
     *     (user, file) -> file.getUserId().equals(user.getId()) ? 
     *         Mono.empty() : Mono.error(new ValidationException(
     *             ValidationException.ErrorCode.PERMISSION_DENIED)),
     *     (user, file) -> !file.getIsDeleted() ? 
     *         Mono.empty() : Mono.error(new ValidationException(
     *             ValidationException.ErrorCode.FILE_DELETED))
     * );
     * 
     * checkPermissions(currentUser, fileMetadata, customRules)
     *     .doOnSuccess(file -> log.debug("所有權限規則檢查通過"))
     *     .doOnError(error -> log.warn("權限檢查失敗: {}", error.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param user 請求存取的用戶對象，不可為 {@code null}
     * @param file 要檢查權限的檔案元資料，不可為 {@code null}
     * @param rules 權限規則集合，{@code null} 或空集合時使用預設的 {@code ONLY_OWNER} 規則
     * @return 包含檔案元資料的 {@code Mono}，權限檢查失敗時傳播第一個遇到的異常
     * @see FilePermissionRuleManager.DefaultRule#ONLY_OWNER
     * @see Permission#check(User, Object) 
     */
    private Mono<UserFileMetadata> checkPermissions(User user, UserFileMetadata file, @Nullable Collection<Permission<UserFileMetadata>> rules) {
        Collection<Permission<UserFileMetadata>> validateRules;
        if (rules == null || rules.isEmpty()) {
            validateRules = FilePermissionRuleManager.DefaultRule.ONLY_OWNER.getRules(filePermissionRuleManager);
        } else {
            validateRules = rules;
        }

        Flux<Permission<UserFileMetadata>> permissionFlux = Flux.fromIterable(validateRules);
        return permissionFlux.flatMap(rule -> rule.check(user, file)).collectList().flatMap(errors -> {
            if (errors.isEmpty()) {
                return Mono.just(file);
            }
            return Mono.error(errors.getFirst());
        });
    }
}
