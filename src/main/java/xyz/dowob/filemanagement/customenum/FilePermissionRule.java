package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileShareRecord;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.repostiory.UserFIleShareRecordRepository;

import java.util.List;
import java.util.Optional;

/**
 * 權限規則類，用戶可以通過設置不同的權限規則來實現不同的權限驗證
 * 當用戶訪問文件時，會根據設置的權限規則來進行權限驗證，如果有一條規則不通過，則會拋出 ValidationException 異常
 * 如果所有規則通過，則返回文件元數據
 * 而這邊定義了一些常用的權限規則，並封裝在 DefaultRule 枚舉類中，可以快速設置默認的權限規則
 * 用戶也可以通過 Permission 介面來自定義權限規則
 * 此類使用了 Spring 的 ApplicationContextAware 接口，用於獲取 Spring 的 ApplicationContext 對象
 * 當有需要獲取 Spring 的 Bean 對象時，可以通過 ApplicationContext 來獲取
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FilePermissionRule
 * @create 2025/2/25
 * @Version 1.0
 **/

@Component
@RequiredArgsConstructor
public class FilePermissionRule implements ApplicationContextAware {
    /**
     * Spring 的 ApplicationContext 對象，用於獲取 Spring 的 Bean 對象
     */
    private static ApplicationContext applicationContext;

    /**
     * 獲取 Spring 的 ApplicationContext 對象
     *
     * @param applicationContext Spring 的 ApplicationContext 對象
     *
     * @throws BeansException 當無法獲取 Spring 的 Bean 對象時拋出此異常
     */
    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        if (FilePermissionRule.applicationContext == null) {
            FilePermissionRule.applicationContext = applicationContext;
        }
    }


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
     * 允許共享訪問，當用戶訪問文件時，會先判斷文件的共享類型，如果文件的共享類型是公共或者文件的擁有者是用戶本人，則通過權限驗證
     * 如果文件的共享類型是無，則拒絕訪問
     * 如果文件的共享類型是私有，則判斷用戶是否有訪問權限，如果有則通過權限驗證
     * 否則拋出 ValidationException 異常 {@link ValidationException.ErrorCode#FILE_PERMISSION_DENIED}
     */
    public static final Permission<UserFileMetadata> ALLOW_SHARED = (user, file) -> {
        if (file.getShareType() == FileShareType.PUBLIC || file.getUserId().equals(user.getId())) {
            return Optional.empty();
        } else if (file.getShareType() == FileShareType.NONE) {
            return Optional.of(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));
        }

        Optional<UserFileShareRecord> recordOptional = applicationContext
                .getBean(UserFIleShareRecordRepository.class)
                .findByUserIdAndFileId(user.getId(), file.getId())
                .blockOptional();

        if (recordOptional.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new ValidationException(ValidationException.ErrorCode.FILE_PERMISSION_DENIED, file.getId()));

    };

    /**
     * 阻止保留值操作，當用戶訪問文件時，如果文件的 ID 是保留值，則拋出 ValidationException 異常 {@link ValidationException.ErrorCode#NOT_EXISTING_USER_FILE}
     * 否則通過權限驗證
     */
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