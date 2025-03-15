package xyz.dowob.filemanagement.component.manager;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.repostiory.UserFIleShareRecordRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 權限規則類，用戶可以通過設置不同的權限規則來實現不同的權限驗證
 * 當用戶訪問文件時，會根據設置的權限規則來進行權限驗證，如果有一條規則不通過，則會拋出 ValidationException 異常
 * 如果所有規則通過，則返回文件元數據
 * 而這邊定義了一些常用的權限規則，並封裝在 DefaultRule 枚舉類中，可以快速設置默認的權限規則
 * 用戶也可以通過 Permission 介面來自定義權限規則
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FilePermissionRuleManager
 * @create 2025/2/25
 * @Version 1.0
 **/
@Component
public class FilePermissionRuleManager {
    /**
     * 用戶文件分享記錄數據庫操作類
     */
    private final UserFIleShareRecordRepository shareRecordRepository;

    /**
     * 用戶文件元數據庫操作類
     */
    private final UserFileMetaRepository userFileMetaRepository;

    /**
     * R2DBC數據庫操作類
     */
    private final R2dbcEntityOperations r2dbcEntityOperations;

    /**
     * 允許擁有者訪問的權限規則
     */
    @Getter
    private Permission<UserFileMetadata> allowOwner;

    /**
     * 允許共享者訪問的權限規則
     */
    @Getter
    private Permission<UserFileMetadata> allowShared;

    /**
     * 阻止保留搜索ID的權限規則
     */
    @Getter
    private Permission<UserFileMetadata> blockNotSearchOperation;

    /**
     * 阻止已刪除的文件的權限規則
     */
    @Getter
    private Permission<UserFileMetadata> blockDeleted;

    /**
     * 構造方法
     *
     * @param shareRecordRepository  用戶文件分享記錄數據庫操作類
     * @param userFileMetaRepository 用戶文件元數據庫操作類
     * @param r2dbcEntityOperations  R2DBC數據庫操作類
     */
    public FilePermissionRuleManager(UserFIleShareRecordRepository shareRecordRepository, UserFileMetaRepository userFileMetaRepository, R2dbcEntityOperations r2dbcEntityOperations) {
        this.shareRecordRepository = shareRecordRepository;
        this.userFileMetaRepository = userFileMetaRepository;
        this.r2dbcEntityOperations = r2dbcEntityOperations;
    }


    /**
     * 初始化權限規則，建立常用的權限規則
     * 1. allowOwner: 僅允許擁有者訪問，將驗證文件的擁有者是否為當前用戶
     * 2. allowShared: 允許共享者訪問，基於以下規則進行權限檢查：
     * - 如果文件是公開的(PUBLIC)或當前用戶是擁有者，則允許訪問
     * - 如果文件是私有的(NONE)，則拒絕訪問
     * - 如果文件是默認的(DEFAULT)，則檢查父資料夾的權限：
     * - 如果父資料夾是公開的(PUBLIC)，則允許訪問
     * - 如果當前用戶有父資料夾權限且不是關閉分享的(NONE)，則允許訪問
     * - 如果當前用戶有該文件的共享記錄，則允許訪問
     * 3. blockNotSearchOperation: 阻止保留搜索ID的操作，確保不能直接操作系統保留的搜索ID
     * 4. blockDeleted: 阻止已刪除的文件的操作，確保不能訪問已標記為刪除的文件
     */
    @PostConstruct
    public void initPermissions() {
        allowOwner = (user, file) -> {
            if (file.getUserId().equals(user.getId())) {
                return Mono.empty();
            }
            return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
        };

        allowShared = (user, file) -> {
            if (file.getShareType() == FileShareTypeEnum.PUBLIC || file.getUserId().equals(user.getId())) {
                return Mono.empty();
            } else if (file.getShareType() == FileShareTypeEnum.NONE) {
                return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
            } else if (file.getShareType() == FileShareTypeEnum.PRIVATE) {
                return shareRecordRepository.existsByUserIdAndFileId(user.getId(), file.getId()).flatMap(hasRecord -> {
                    if (hasRecord) {
                        return Mono.empty();
                    }
                    return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
                });
            }

            return shareRecordRepository.existsByUserIdAndFileId(user.getId(), file.getId()).flatMap(hasRecord -> {
                if (hasRecord) {
                    return Mono.empty();
                }

                Mono<Boolean> existParentRecordMono;
                Mono<Optional<FileShareTypeEnum>> parentShareTypeOptionalMono;

                if (file.getParentFolderId() != null) {
                    existParentRecordMono = shareRecordRepository.existsByUserIdAndFileId(user.getId(), file.getParentFolderId());
                    parentShareTypeOptionalMono = userFileMetaRepository
                            .getShareTypeByFileId(file.getParentFolderId(), r2dbcEntityOperations)
                            .map(Optional::of)
                            .switchIfEmpty(Mono.just(Optional.empty()));
                } else {
                    existParentRecordMono = Mono.just(false);
                    parentShareTypeOptionalMono = Mono.just(Optional.empty());
                }

                return Mono.zip(existParentRecordMono, parentShareTypeOptionalMono).flatMap(tuple -> {
                    Boolean parentExistRecord = tuple.getT1();
                    Optional<FileShareTypeEnum> parentShareTypeOptional = tuple.getT2();
                    if (parentShareTypeOptional.isPresent()) {
                        if (parentShareTypeOptional.get() == FileShareTypeEnum.PUBLIC || parentExistRecord && parentShareTypeOptional.get() != FileShareTypeEnum.NONE) {
                            return Mono.empty();
                        }
                    }
                    return Mono.just(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
                });
            });
        };

        blockNotSearchOperation = (user, file) -> {
            if (ReservedSearchIdEnum.format(file.getId()) == null) {
                return Mono.empty();
            }
            return Mono.just(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, file.getId()));
        };

        blockDeleted = (user, file) -> {
            if (file.getIsDeleted()) {
                return Mono.just(new ValidationException(ValidationException.ErrorCode.ALREADY_DELETED_FILE, file.getId()));
            }
            return Mono.empty();
        };
    }

    /**
     * 預設的權限規則
     */
    @Getter
    @RequiredArgsConstructor
    public enum DefaultRule {
        /**
         * 只允許擁有者訪問，阻止已刪除的文件，阻止保留搜索ID
         */
        ONLY_OWNER,

        /**
         * 允許擁有者和共享者訪問，阻止已刪除的文件，阻止保留搜索ID
         */
        WITH_SHARED;

        /**
         * 獲取默認的權限規則
         */
        public List<Permission<UserFileMetadata>> getRules(FilePermissionRuleManager manager) {
            return switch (this) {
                case ONLY_OWNER -> List.of(manager.getAllowOwner(), manager.getBlockDeleted(), manager.getBlockNotSearchOperation());
                case WITH_SHARED -> List.of(manager.getAllowShared(), manager.getBlockDeleted(), manager.getBlockNotSearchOperation());
            };
        }
    }
}