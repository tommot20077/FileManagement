package xyz.dowob.filemanagement.component.manager;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.repostiory.UserFIleShareRecordRepository;

import java.util.List;

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
    private final UserFIleShareRecordRepository shareRecordRepository;

    @Getter
    private Permission<UserFileMetadata> allowOwner;

    @Getter
    private Permission<UserFileMetadata> allowShared;

    @Getter
    private Permission<UserFileMetadata> blockNotSearchOperation;

    @Getter
    private Permission<UserFileMetadata> blockDeleted;

    public FilePermissionRuleManager(UserFIleShareRecordRepository shareRecordRepository) {
        this.shareRecordRepository = shareRecordRepository;
    }


    /**
     * 初始化權限規則，建立常用的權限規則
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
            }

            return shareRecordRepository
                    .findByUserIdAndFileId(user.getId(), file.getId())
                    .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId())))
                    .flatMap(record -> Mono.empty());
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