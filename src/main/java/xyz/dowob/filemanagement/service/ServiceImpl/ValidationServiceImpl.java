package xyz.dowob.filemanagement.service.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.user.RegisterDTO;
import xyz.dowob.filemanagement.dto.user.ResetPasswordDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.ServiceInterFace.ValidationService;

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
     *
     * @throws ValidationException 當數據不合法時拋出異常
     */
    /*
    @Override
    public void validateFileMetadataDTO(FileMetadataDTO fileMetadataDTO) throws ValidationException {
        validateNotNull(fileMetadataDTO);
    }

     */

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
     *
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
     *
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
}
