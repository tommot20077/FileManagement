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
 * 檔案權限服務實現類，分離檔案權限驗證邏輯
 * 通過設置不同的權限規則來實現不同的權限驗證，並且可以自定義權限規則
 * 當用戶訪問文件時，會根據設置的權限規則來進行權限驗證，如果有一條規則不通過，則會拋出 ValidationException 異常
 * 如果所有規則通過，則返回文件元數據
 * 在這裡定義了一些常用的權限規則，如允許擁有者訪問、允許共享訪問、阻止已刪除的文件訪問
 * 用戶可以通過 PermissionDefaultRule 枚舉類來設置默認的權限規則
 * 通過 PermissionRule 介面來定義權限規則，用戶可以通過實現 PermissionRule 介面來自定義權限規則
 * 實現 PermissionService 介面，用戶可以通過調用 validateUserPermission 方法來驗證用戶是否有權限
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FilePermissionServiceImpl
 * @create 2025/2/25
 * @Version 1.0
 **/
@Service
@RecordLevel(LogLevelEnum.DEBUG)
@RequiredArgsConstructor
public class FilePermissionServiceImpl implements PermissionService<UserFileMetadata> {

    /**
     * 用戶檔案元數據庫操作類
     */
    private final UserFileMetaRepository userFileMetaRepository;

    /**
     * 權限規則管理器
     */
    private final FilePermissionRuleManager filePermissionRuleManager;

    /**
     * 驗證用戶是否有權限，通過設置不同的權限規則來實現不同的權限驗證
     * 當用戶訪問文件時，會根據設置的權限規則來進行權限驗證，如果有一條規則不通過，則會拋出 ValidationException 異常
     * 如果所有規則通過，則返回文件元數據
     * 用戶可以指定權限規則，如果不指定，則使用默認的權限規則
     *
     * @param user   用戶
     * @param fileId 文件ID
     * @param rules  權限規則
     *
     * @return UserFileMetadata 文件元數據
     */
    @Override
    public Mono<UserFileMetadata> validateUserPermission(User user, Long fileId, @Nullable Collection<Permission<UserFileMetadata>> rules) {
        return userFileMetaRepository
                .findById(fileId.toString())
                .switchIfEmpty(reservedSearchMethod(user, fileId))
                .flatMap(file -> checkPermissions(user, file, rules));
    }


    /**
     * 驗證用戶是否有權限多個檔案Id
     * 通過設置不同的權限規則來實現不同的權限驗證
     * 當用戶訪問文件時，會根據設置的權限規則來進行權限驗證，如果有一條規則不通過，則會拋出 ValidationException 異常
     * 如果所有規則通過，則返回文件元數據
     * 用戶可以指定權限規則，如果不指定，則使用默認的權限規則
     *
     * @param user    用戶
     * @param fileIds 文件ID集合
     * @param rules   權限規則
     *
     * @return Mono<Map < Long, UserFileMetadata>> 文件元數據集合
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
     * 保留值搜索方法，在搜索用戶檔案元數據時，如果找不到文件，則檢查輸入的文件ID是否是保留值
     * 當文件ID是保留值時，則返回一個虛擬的文件元數據，否則拋出 ValidationException 異常
     * 這虛擬的文件元數據用於後續驗證時進行檢查，在一般規則下不允許對保留值進行操作 {@link FilePermissionRuleManager.DefaultRule}
     * 則拋出 ValidationException 異常
     *
     * @param user   用戶
     * @param fileId 文件ID
     *
     * @return UserFileMetadata 文件元數據
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
     * 驗證用戶是否有權限，當沒有指定權限規則時，使用默認的權限規則(只允許擁有者訪問)
     *
     * @param user  用戶
     * @param file  文件
     * @param rules 權限規則
     *
     * @return UserFileMetadata 文件元數據
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
