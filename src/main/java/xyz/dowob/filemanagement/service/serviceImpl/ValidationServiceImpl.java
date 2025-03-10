package xyz.dowob.filemanagement.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.ByteEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 驗證服務實現類，主要用於驗證數據的合法性，如用戶名稱唯一性、密碼強度等
 * 實現接口 @see {@link ValidationService}
 *
 * @author yuan
 * @program File-Management
 * @ClassName ValidationServiceImpl
 * @description
 * @create 2024-09-15 23:59
 * @Version 1.0
 **/

@Service
@RequiredArgsConstructor
public class ValidationServiceImpl implements ValidationService {
    /**
     * 用戶數據庫操作對象
     */
    private final UserRepository userRepository;

    /**
     * 非法字符正則表達式，用於檢查文件名是否包含非法字符
     */
    private static final Pattern INVALID_CHARACTERS_PATTERN = Pattern.compile("[/\\\\|\"']");

    /**
     * 驗證用戶註冊數據類RegisterDTO中的數據是否合法
     * 當數據不合法時，拋出ValidationException
     *
     * @param registerDTO 用戶註冊數據傳輸對象
     */
    @Override
    public Mono<Void> validateRegisterDTO(RegisterDTO registerDTO) {
        return validateNotNull(registerDTO)
                .then(validatePasswordsMatch(registerDTO.getPassword(), registerDTO.getConfirmPassword()))
                .then(validateUsernameNotExists(registerDTO))
                .then(validateEmailNotExists(registerDTO))
                .then(validatePasswordStrength(registerDTO.getPassword()));
    }

    /**
     * 驗證重製密碼數據類ResetPasswordDTO中的數據是否合法
     * 調用此方法會檢查
     * 1. 新密碼與確認密碼是否一致
     * 2. 新密碼的強度是否足夠
     * 3. 重置密碼數據傳輸對象是否為空
     *
     * @param resetPasswordDTO 重置密碼數據傳輸對象
     */
    @Override
    public Mono<Void> validateResetPasswordDTO(ResetPasswordDTO resetPasswordDTO) {
        return validateNotNull(resetPasswordDTO)
                .then(validatePasswordsMatch(resetPasswordDTO.getNewPassword(), resetPasswordDTO.getConfirmPassword()))
                .then(validatePasswordStrength(resetPasswordDTO.getNewPassword()));
    }

    /**
     * 驗證文件元數據DTO中的數據是否合法
     *
     * @param fileMetadataDTO 文件元數據DTO
     */
    @Override
    public Mono<Void> validateFileMetadataDTO(FileMetadataDTO fileMetadataDTO, User user) {
        return validateNotNull(fileMetadataDTO).then(validFileName(fileMetadataDTO.getFilename(), false))
                .then(validateUserStorageLimit(user, fileMetadataDTO.getFileSize()));
    }

    /**
     * 驗證文件編輯數據DTO中的數據是否合法
     *
     * @param fileEditDTO 文件編輯數據DTO
     * @param isFolder    是否為文件夾
     */
    @Override
    public Mono<Void> validateEditFileDTO(FileEditDTO fileEditDTO, boolean isFolder) {
        return validateNotNull(fileEditDTO).then(Mono.defer(() -> switch (fileEditDTO.getEditType()) {
            case EDIT_METADATA -> validFileName(fileEditDTO.getFilename(), isFolder);
            case EDIT_CONTENT -> validLength(fileEditDTO.getContent(), Math.pow(2, 20), "檔案內容");
            case BUILD_HISTORY_RECORD ->
                    validLength(fileEditDTO.getContent(), Math.pow(2, 20), "檔案內容").then(validLength(fileEditDTO.getNote(), 1000, "備註"));
            case REVERT_HISTORY_RECORD -> Mono.empty();
        }));
    }

    /**
     * 驗證檔案類型是否合法，當檔案類型不在指定的類型中時，拋出ValidationException
     * 如果不指定檔案類型，則不進行檢查
     *
     * @param file     文件
     * @param fileType 文件類型
     *
     * @return Mono<UserFileMetadata>
     */
    @Override
    public Mono<UserFileMetadata> validateFileType(UserFileMetadata file, FileEnum... fileType) {
        if (file == null) {
            return Mono.empty();
        }
        return Mono.just(file).flatMap(userFileMetadata -> {
            if (fileType == null || fileType.length == 0 || Arrays
                    .stream(fileType)
                    .anyMatch(fileEnum -> fileEnum == userFileMetadata.getFileType())) {
                return Mono.just(userFileMetadata);
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.FILE_TYPE_WITH_WRONG_REQUEST_PATH,
                                                      Arrays.toString(fileType),
                                                      userFileMetadata.getFileType().name()
            ));

        });
    }

    /**
     * 驗證文件過濾DTO中的數據是否合法
     *
     * @param fileFilterDTO 文件過濾DTO
     */
    @Override
    public Mono<Void> validateFileFilterDTO(FileFilterDTO fileFilterDTO) {
        return validateNotNull(fileFilterDTO).then(Mono.defer(() -> {
            if (fileFilterDTO.isFilterEmpty()) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.SEARCH_CRITERIA_EMPTY));
            }
            if (fileFilterDTO.getFolderId() != null && fileFilterDTO.getFolderId() < 0) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA, "無效的資料夾Id"));
            }
            if (fileFilterDTO.getKeyword() != null) {
                if (fileFilterDTO.getKeyword().trim().length() > 50) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.KEYWORD_TOO_LONG));
                } else if (fileFilterDTO.getKeyword().trim().length() < 2) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.KEYWORD_TOO_SHORT));
                }
            }

            if (fileFilterDTO.getStartTime() != null && fileFilterDTO.getEndTime() != null) {
                if (fileFilterDTO.getStartTime().isAfter(fileFilterDTO.getEndTime())) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA, "開始時間不能晚於結束時間"));
                }
            }

            return Mono.empty();
        }));
    }

    /**
     * 驗證用戶搜索列表是否合法，當搜索列表為空或者搜索列表數量超過20時，拋出ValidationException
     *
     * @param searchList 搜索列表
     */
    @Override
    public Mono<Void> validateUserSearchList(Collection<String> searchList) {
        if (searchList == null || searchList.isEmpty()) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.SEARCH_CRITERIA_EMPTY));
        }
        if (searchList.size() > 20) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA, "請求搜索用戶數量不能超過20位"));
        }
        return Mono.empty();
    }

    /**
     * 驗證用戶密碼與確認密碼是否一致
     * 當密碼與確認密碼不一致時，拋出ValidationException
     *
     * @param password        密碼
     * @param confirmPassword 確認密碼
     */
    private Mono<Void> validatePasswordsMatch(String password, String confirmPassword) {
        return Mono.defer(() -> {
            if (!password.equals(confirmPassword)) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.CONFIRM_PASSWORD_NOT_MATCH));
            }
            return Mono.empty();
        });
    }

    /**
     * 檢查用戶名稱是否合法
     * 須符合以下條件：
     * 1. 用戶名稱沒有被註冊過
     * 2. 用戶名稱只包含字母和數字
     *
     * @param registerUserDTO 用戶註冊數據傳輸對象
     */
    private Mono<Void> validateUsernameNotExists(RegisterDTO registerUserDTO) {
        return userRepository.findByUsername(registerUserDTO.getUsername()).flatMap(user -> {
            if (user != null) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_INVALID, registerUserDTO.getUsername()));
            }
            return Mono.empty();
        }).then(alphanumericInspection(registerUserDTO.getUsername()));
    }

    /**
     * 檢查用戶信箱是否已存在
     * 當信箱已存在時，拋出ValidationException
     *
     * @param registerUserDTO 用戶註冊數據傳輸對象
     */
    private Mono<Void> validateEmailNotExists(RegisterDTO registerUserDTO) {
        return userRepository.findByEmail(registerUserDTO.getEmail()).flatMap(user -> {
            if (user != null) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.EMAIL_ALREADY_EXISTS, registerUserDTO.getEmail()));
            }
            return Mono.empty();
        });
    }


    /**
     * 驗證用戶密碼強度
     * 當密碼強度不足時，拋出ValidationException
     * 需要滿足以下條件：
     * 1. 密碼不是回文
     * 2. 密碼包含大寫字母、小寫字母和數字
     *
     * @param password 密碼
     */
    private Mono<Void> validatePasswordStrength(String password) {
        return palindromeInspection(password).then(upperLetterAndLowerLetterAndNumberInspection(password));
    }

    /**
     * 檢查是否為回文
     * 當檢測到回文時返回錯誤
     *
     * @param str 字符串
     */
    private Mono<Void> palindromeInspection(String str) {
        return Mono.defer(() -> {
            int n = str.length();
            for (int i = 0; i < (n / 2); ++i) {
                if (str.charAt(i) != str.charAt(n - i - 1)) {
                    return Mono.empty();
                }
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH));
        }).then();
    }

    /**
     * 檢查是否包含大寫字母、小寫字母和數字
     * 當檢測到大寫字母、小寫字母和數字有一個不存在時回傳錯誤
     *
     * @param str 字符串
     */
    private Mono<Void> upperLetterAndLowerLetterAndNumberInspection(String str) {
        var ref = new Object() {
            boolean hasUpperLetter = false;

            boolean hasNumber = false;

            boolean hasLowerLetter = false;
        };

        for (char c : str.toCharArray()) {
            if (Character.isUpperCase(c)) {
                ref.hasUpperLetter = true;
            }
            if (Character.isLowerCase(c)) {
                ref.hasLowerLetter = true;
            }
            if (Character.isDigit(c)) {
                ref.hasNumber = true;
            }
        }
        return Mono.defer(() -> {
            if (ref.hasLowerLetter && ref.hasUpperLetter && ref.hasNumber) {
                return Mono.empty();
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH));
        });
    }

    /**
     * 檢查是否只包含字母和數字
     * 當檢測到非字母和數字時返回錯誤
     *
     * @param username 字符串
     */
    private Mono<Void> alphanumericInspection(String username) {
        return Mono.defer(() -> {
            if (Pattern.matches("^[a-zA-Z0-9]*$", username)) {
                return Mono.empty();
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_INVALID, username));
        });
    }

    /**
     * 驗證檔案名稱是否出現非法字符
     * 當檔案名稱為空、包含非法字符或者長度超過200時、檔案名稱不包含"."時，拋出ValidationException
     * 當檔案為文件夾時，不檢查是否包含"."，但是檢查是否包含非法字符
     *
     * @param filename 檔案名稱
     * @param isFolder 是否為文件夾
     *
     * @return Mono<Void>
     */
    private Mono<Void> validFileName(String filename, boolean isFolder) {
        if (filename == null || filename.isBlank() || (!isFolder && !filename.contains(".")) || INVALID_CHARACTERS_PATTERN.matcher(filename).find()) {
            if (isFolder) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_FOLDER_NAME));
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_FILE_NAME));
        }
        if (filename.length() > 200) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.NAME_TOO_LONG));
        }
        return Mono.empty();
    }

    /**
     * 檢查用戶是否有足夠的存儲空間來存儲文件，如果用戶的存儲限制為-1，則不進行檢查
     * 當用戶存儲空間不足時，拋出ValidationException
     *
     * @param user       用戶
     * @param expectSize 預期存儲大小
     *
     * @return Mono<Void>
     */
    private Mono<Void> validateUserStorageLimit(User user, long expectSize) {
        if (user.getStorageLimit() == -1) {
            return Mono.empty();
        }
        if (user.getStorageLimit() < user.getUsedStorage() + expectSize) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.STORAGE_LIMIT_EXCEEDED,
                                                      ByteEnum.toReadableSize(user.getStorageLimit()),
                                                      ByteEnum.toReadableSize(user.getUsedStorage()),
                                                      ByteEnum.toReadableSize(expectSize)
            ));
        }
        return Mono.empty();
    }
}
