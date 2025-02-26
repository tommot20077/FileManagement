package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;

import java.util.List;
import java.util.Objects;
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
 * @ClassName FilePermissionRule
 * @create 2025/2/25
 * @Version 1.0
 **/

@Component
public class FilePermissionRule {
    /**
     * 允許擁有者訪問，當用戶訪問文件時，如果文件的擁有者是用戶本人，則通過權限驗證
     * 否則拋出 ValidationException 異常 {@link ValidationException.ErrorCode#FILE_PERMISSION_DENIED}
     */
    public static final Permission<UserFileMetadata> ALLOW_OWNER = (user, file) -> {
        if (file.getUserId().equals(user.getId())) {
            return Optional.empty();
        }
        return Optional.of(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
    };

    /**
     * 允許共享訪問，當用戶訪問文件時，如果文件被共享給用戶或者文件的擁有者是用戶本人，則通過權限驗證
     * 否則拋出 ValidationException 異常 {@link ValidationException.ErrorCode#FILE_PERMISSION_DENIED}
     */
    public static final Permission<UserFileMetadata> ALLOW_SHARED = (user, file) -> {
        if (file.getSharedWithUsers().contains(user.getId()) || Objects.equals(file.getUserId(), user.getId())) {
            return Optional.empty();
        }
        return Optional.of(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
    };

    public static final Permission<UserFileMetadata> BLOCK_NOT_SEARCH_OPERATION = (user, file) -> {
        if (ReservedSearchIdEnum.format(file.getId()) == null) {
            return Optional.empty();
        }
        return Optional.of(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, file.getId()));
    };

    /**
     * 阻止已刪除的文件訪問，當用戶訪問文件時，如果文件已經被刪除，則拋出 ValidationException 異常 {@link ValidationException.ErrorCode#ALREADY_DELETED_FILE}
     * 否則通過權限驗證
     */
    public static final Permission<UserFileMetadata> BLOCK_DELETED = (user, file) -> {
        if (file.getIsDeleted()) {
            return Optional.of(new ValidationException(ValidationException.ErrorCode.ALREADY_DELETED_FILE, file.getId()));
        }
        return Optional.empty();
    };

    /**
     * 默認的權限規則
     * 1. 只允許擁有者訪問
     * 2. 允許共享訪問
     */
    @Getter
    @RequiredArgsConstructor
    public enum DefaultRule {
        /**
         * 只允許擁有者訪問，不允許已刪除的文件訪問，不允許保留值操作
         */
        ONLY_OWNER(List.of(ALLOW_OWNER, BLOCK_DELETED, BLOCK_NOT_SEARCH_OPERATION)),
        /**
         * 允許共享訪問，不允許已刪除的文件訪問，不允許保留值操作
         */
        WITH_SHARED(List.of(ALLOW_SHARED, BLOCK_DELETED, BLOCK_NOT_SEARCH_OPERATION)),
        ;

        private final List<Permission<UserFileMetadata>> rules;
    }

}