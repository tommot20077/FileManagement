package xyz.dowob.filemanagement.component.manager;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.repostiory.UserFIleShareRecordRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 檔案權限規則管理器，提供多層次的檔案訊問控制與權限驗證機制。
 * 
 * <p>本類實現了一套完整的檔案權限控制系統，透過可組合的權限規則對檔案操作進行細粒度控制。
 * 權限驗證採用責任鏈模式，支援複雜的檔案分享邏輯和存取權限管理。</p>
 * 
 * <p>權限控制機制：基於檔案分享類型（{@link xyz.dowob.filemanagement.customenum.FileShareTypeEnum}）
 * 執行不同的權限檢查策略。支援檔案擁有者權限、分享權限、父資料夾繼承權限等多種驗證模式。</p>
 * 
 * <p>內建權限規則：包含擁有者驗證、分享狀態檢查、已刪除檔案阻擋、系統保留ID限制等常用規則。
 * 所有規則均實現 {@link xyz.dowob.filemanagement.functionInterface.Permission} 介面，支援自定義擴展。</p>
 * 
 * <p>驗證失敗時拋出 {@link xyz.dowob.filemanagement.exception.ValidationException} 異常，
 * 包含具體的錯誤代碼和相關資訊。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@SkipRecord
public class FilePermissionRuleManager {
    /**
     * 檔案分享記錄資料庫操作介面
     */
    private final UserFIleShareRecordRepository shareRecordRepository;

    /**
     * 用戶檔案元資料資料庫操作介面  
     */
    private final UserFileMetaRepository userFileMetaRepository;

    /**
     * R2DBC 響應式資料庫操作類
     */
    private final R2dbcEntityOperations r2dbcEntityOperations;

    /**
     * 擁有者權限驗證規則，僅檔案擁有者可存取
     */
    @Getter
    private Permission<UserFileMetadata> allowOwner;

    /**
     * 分享權限驗證規則，檢查檔案分享狀態和使用者權限
     */
    @Getter
    private Permission<UserFileMetadata> allowShared;

    /**
     * 系統保留ID阻擋規則，禁止操作系統內建搜索ID
     */
    @Getter
    private Permission<UserFileMetadata> blockNotSearchOperation;

    /**
     * 已刪除檔案阻擋規則，禁止存取標記為已刪除的檔案
     */
    @Getter
    private Permission<UserFileMetadata> blockDeleted;

    /**
     * 構造權限規則管理器並注入相依的資料庫操作類。
     * 
     * <p>透過依賴注入初始化檔案權限管理所需的資料庫存取介面。
     * 這些介面將用於後續的權限驗證過程中的資料查詢和狀態檢查。</p>
     *
     * @param shareRecordRepository  檔案分享記錄資料庫操作介面，用於查詢檔案分享權限記錄
     * @param userFileMetaRepository 用戶檔案元資料資料庫操作介面，用於獲取檔案基本資訊和分享狀態
     * @param r2dbcEntityOperations  R2DBC 響應式資料庫操作類，提供響應式的資料庫查詢能力
     */
    public FilePermissionRuleManager(UserFIleShareRecordRepository shareRecordRepository, UserFileMetaRepository userFileMetaRepository, R2dbcEntityOperations r2dbcEntityOperations) {
        this.shareRecordRepository = shareRecordRepository;
        this.userFileMetaRepository = userFileMetaRepository;
        this.r2dbcEntityOperations = r2dbcEntityOperations;
    }


    /**
     * 初始化預設權限規則，建立完整的檔案存取控制機制。
     * 
     * <p>此方法在 Spring 容器初始化完成後自動執行，建立四種核心權限規則的 Lambda 表達式實現。
     * 每個權限規則都實現 {@link Permission} 介面，返回 {@link Mono}&lt;{@link ValidationException}&gt;，
     * 其中空的 Mono 表示權限檢查通過，包含異常的 Mono 表示權限檢查失敗。</p>
     * 
     * <p><strong>建立的四種核心權限規則：</strong></p>
     * <ol>
     * <li><strong>allowOwner</strong>: 擁有者權限驗證，僅檔案擁有者可存取
     * <ul>
     * <li>比較檔案的 userId 與當前使用者 ID 是否相同</li>
     * <li>權限檢查失敗時返回 FILE_PERMISSION_DENIED 錯誤</li>
     * </ul>
     * </li>
     * <li><strong>allowShared</strong>: 分享權限驗證，支援多層次分享邏輯：
     * <ul>
     * <li>PUBLIC：公開檔案，所有用戶可存取</li>
     * <li>NONE：私有檔案，僅擁有者可存取</li>
     * <li>PRIVATE：私有分享，需要明確的分享記錄</li>
     * <li>DEFAULT：繼承父資料夾權限，遞迴檢查父資料夾的分享狀態和權限記錄</li>
     * </ul>
     * </li>
     * <li><strong>blockNotSearchOperation</strong>: 系統保留ID保護，禁止操作內建搜索標識
     * <ul>
     * <li>使用 {@link ReservedSearchIdEnum#format} 檢查檔案ID是否為系統保留ID</li>
     * <li>保護系統內建的搜索功能不被誤操作</li>
     * </ul>
     * </li>
     * <li><strong>blockDeleted</strong>: 已刪除檔案保護，禁止存取標記為已刪除的檔案
     * <ul>
     * <li>檢查檔案的 isDeleted 標記</li>
     * <li>防止存取已刪除的檔案資源</li>
     * </ul>
     * </li>
     * </ol>
     * 
     * <p><strong>分享邏輯的複雜性處理：</strong></p>
     * <p>對於 DEFAULT 類型的檔案分享，系統會執行以下步驟：</p>
     * <ol>
     * <li>首先檢查當前使用者是否有直接的檔案分享記錄</li>
     * <li>如果沒有直接記錄，則檢查父資料夾的分享狀態</li>
     * <li>同時查詢使用者對父資料夾的分享記錄</li>
     * <li>使用 {@link Mono#zip} 組合兩個查詢結果進行綜合判斷</li>
     * <li>父資料夾為 PUBLIC 或使用者有父資料夾分享記錄且父資料夾非 NONE 類型時允許存取</li>
     * </ol>
     */
    @PostConstruct
    @RecordLevel(LogLevelEnum.INFO)
    public void initPermissions() {
        // 擁有者權限驗證 Lambda 表達式：檢查檔案的 userId 是否與當前使用者 ID 相同
        allowOwner = (user, file) -> {
            if (file.getUserId().equals(user.getId())) {
                return Mono.empty(); // 權限檢查通過，返回空 Mono
            }
            // 權限檢查失敗，返回包含異常的 Mono
            return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
        };

        // 分享權限驗證 Lambda 表達式：實現複雜的多層次分享邏輯
        allowShared = (user, file) -> {
            // 公開檔案或檔案擁有者直接允許存取
            if (file.getShareType() == FileShareTypeEnum.PUBLIC || file.getUserId().equals(user.getId())) {
                return Mono.empty();
            } else if (file.getShareType() == FileShareTypeEnum.NONE) {
                // 私有檔案（非分享）禁止存取
                return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
            } else if (file.getShareType() == FileShareTypeEnum.PRIVATE) {
                // 私有分享檔案：需要明確的分享記錄
                return shareRecordRepository.existsByUserIdAndFileId(user.getId(), file.getId()).flatMap(hasRecord -> {
                    if (hasRecord) {
                        return Mono.empty(); // 有分享記錄，允許存取
                    }
                    return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
                });
            }

            // DEFAULT 類型：繼承父資料夾權限的複雜邏輯處理
            return shareRecordRepository.existsByUserIdAndFileId(user.getId(), file.getId()).flatMap(hasRecord -> {
                if (hasRecord) {
                    return Mono.empty(); // 有直接分享記錄，允許存取
                }

                // 準備父資料夾權限檢查的響應式操作
                Mono<Boolean> existParentRecordMono;
                Mono<Optional<FileShareTypeEnum>> parentShareTypeOptionalMono;

                if (file.getParentFolderId() != null) {
                    // 查詢使用者對父資料夾的分享記錄
                    existParentRecordMono = shareRecordRepository.existsByUserIdAndFileId(user.getId(), file.getParentFolderId());
                    // 查詢父資料夾的分享類型
                    parentShareTypeOptionalMono = userFileMetaRepository
                            .getShareTypeByFileId(file.getParentFolderId(), r2dbcEntityOperations)
                            .map(Optional::of)
                            .switchIfEmpty(Mono.just(Optional.empty()));
                } else {
                    // 根目錄檔案沒有父資料夾
                    existParentRecordMono = Mono.just(false);
                    parentShareTypeOptionalMono = Mono.just(Optional.empty());
                }

                // 組合父資料夾權限檢查結果進行綜合判斷
                return Mono.zip(existParentRecordMono, parentShareTypeOptionalMono).flatMap(tuple -> {
                    Boolean parentExistRecord = tuple.getT1(); // 是否有父資料夾分享記錄
                    Optional<FileShareTypeEnum> parentShareTypeOptional = tuple.getT2(); // 父資料夾分享類型
                    if (parentShareTypeOptional.isPresent()) {
                        // 父資料夾為公開分享或使用者有父資料夾分享記錄且父資料夾非私有類型時允許存取
                        if (parentShareTypeOptional.get() == FileShareTypeEnum.PUBLIC || 
                            parentExistRecord && parentShareTypeOptional.get() != FileShareTypeEnum.NONE) {
                            return Mono.empty();
                        }
                    }
                    // 其他情況拒絕存取
                    return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
                });
            });
        };

        // 系統保留ID阻擋 Lambda 表達式：檢查檔案ID是否為系統保留的搜索標識
        blockNotSearchOperation = (user, file) -> {
            if (ReservedSearchIdEnum.format(file.getId()) == null) {
                return Mono.empty(); // 非保留ID，允許操作
            }
            // 系統保留ID，禁止操作
            return Mono.just(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, file.getId()));
        };

        // 已刪除檔案阻擋 Lambda 表達式：檢查檔案是否已被標記為刪除
        blockDeleted = (user, file) -> {
            if (file.getIsDeleted()) {
                // 檔案已刪除，禁止存取
                return Mono.just(new ValidationException(ValidationException.ErrorCode.ALREADY_DELETED_FILE, file.getId()));
            }
            return Mono.empty(); // 檔案未刪除，允許存取
        };
    }


    /**
     * 預設權限規則枚舉，提供常用的權限組合策略。
     * 
     * <p>此枚舉定義了系統中常用的權限規則組合，每種規則都包含基本的安全保護機制
     * （阻止已刪除檔案存取和系統保留ID操作），並根據不同的業務需求選擇適當的存取控制策略。</p>
     * 
     * <p>所有預設規則都會自動包含以下基礎保護：</p>
     * <ul>
     * <li>阻止存取已標記為刪除的檔案</li>
     * <li>阻止對系統保留搜索ID的操作</li>
     * </ul>
     * 
     * <p>使用範例：</p>
     * <pre>{@code
     * List<Permission<UserFileMetadata>> rules = DefaultRule.ONLY_OWNER.getRules(manager);
     * // 獲得僅允許擁有者存取的完整權限規則集合
     * }</pre>
     */
    @SkipRecord
    @RequiredArgsConstructor
    public enum DefaultRule {
        /**
         * 嚴格擁有者模式：僅允許檔案擁有者存取，提供最高安全級別。
         * 
         * <p>此模式適用於：</p>
         * <ul>
         * <li>私人檔案管理</li>
         * <li>敏感資料存取</li>
         * <li>個人專用檔案操作</li>
         * </ul>
         * 
         * <p>包含的權限規則：</p>
         * <ul>
         * <li>{@link #allowOwner} - 擁有者權限驗證</li>
         * <li>{@link #blockDeleted} - 已刪除檔案阻擋</li>
         * <li>{@link #blockNotSearchOperation} - 系統保留ID保護</li>
         * </ul>
         */
        ONLY_OWNER,

        /**
         * 分享協作模式：允許擁有者和獲得分享權限的使用者存取，支援協作場景。
         * 
         * <p>此模式適用於：</p>
         * <ul>
         * <li>團隊協作檔案</li>
         * <li>公開分享檔案</li>
         * <li>多使用者存取場景</li>
         * </ul>
         * 
         * <p>包含的權限規則：</p>
         * <ul>
         * <li>{@link #allowShared} - 分享權限驗證（包含複雜的多層次分享邏輯）</li>
         * <li>{@link #blockDeleted} - 已刪除檔案阻擋</li>
         * <li>{@link #blockNotSearchOperation} - 系統保留ID保護</li>
         * </ul>
         */
        WITH_SHARED;

        /**
         * 根據枚舉值獲取對應的權限規則清單。
         * 
         * <p>此方法將枚舉常數轉換為具體的權限規則實現清單，提供給權限驗證系統使用。
         * 每個規則都是 {@link Permission} 介面的實現，支援響應式的權限檢查流程。</p>
         * 
         * <p>權限規則的執行順序：</p>
         * <ol>
         * <li>首先執行主要的存取控制規則（ONLY_OWNER 或 WITH_SHARED）</li>
         * <li>然後執行已刪除檔案阻擋規則</li>
         * <li>最後執行系統保留ID保護規則</li>
         * </ol>
         * 
         * <p>使用 Java 14+ 的 switch 表達式提供清晰簡潔的規則映射。</p>
         *
         * @param manager 檔案權限規則管理器實例，提供具體的權限規則實現
         * @return 權限規則清單，包含該枚舉值對應的所有權限檢查規則
         * @throws NullPointerException 如果 manager 參數為 null
         */
        public List<Permission<UserFileMetadata>> getRules(FilePermissionRuleManager manager) {
            return switch (this) {
                case ONLY_OWNER -> List.of(
                    manager.getAllowOwner(),           // 擁有者權限驗證
                    manager.getBlockDeleted(),         // 已刪除檔案阻擋
                    manager.getBlockNotSearchOperation() // 系統保留ID保護
                );
                case WITH_SHARED -> List.of(
                    manager.getAllowShared(),          // 分享權限驗證
                    manager.getBlockDeleted(),         // 已刪除檔案阻擋  
                    manager.getBlockNotSearchOperation() // 系統保留ID保護
                );
            };
        }
    }
}