package xyz.dowob.filemanagement.service.ServiceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.user.RegisterDTO;
import xyz.dowob.filemanagement.dto.user.ResetPasswordDTO;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.util.Objects;

/**
 * 驗證服務接口，用於驗證數據的合法性，如用戶名稱唯一性、密碼強度等
 * 定義了驗證數據的方法
 *
 * @author yuan
 * @program File-Management
 * @ClassName ValidationService
 * @description
 * @create 2024-09-15 23:58
 * @Version 1.0
 **/
public interface ValidationService {
    /**
     * 驗證用戶註冊數據類RegisterUserDTO中的數據是否合法
     *
     * @param registerDTO 用戶註冊數據傳輸對象
     *
     */
    Mono<Void> validateRegisterDTO(RegisterDTO registerDTO);

    /**
     * 驗證重製密碼數據類ResetPasswordDTO中的數據是否合法
     *
     * @param resetPasswordDTO 重置密碼數據傳輸對象
     *
     */
    Mono<Void> validateResetPasswordDTO(ResetPasswordDTO resetPasswordDTO);

    /**
     * 驗證文件元數據DTO中的數據是否合法
     *
     * @param fileMetadataDTO 文件元數據DTO
     *
     * @throws ValidationException 當數據不合法時拋出異常
     */
    //Mono<Void> validateFileMetadataDTO(FileMetadataDTO fileMetadataDTO);

    /**
     * 驗證數據傳輸對象是否為空
     *
     * @param <T> 數據傳輸對象類型
     * @param dto 數據傳輸對象
     *
     */
    default <T> Mono<Void> validateNotNull(T dto) {
        return Mono.defer(() -> {
            if (Objects.isNull(dto)) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
            }
            return Mono.empty();
        });
    }
}
