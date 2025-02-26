package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.lang.reflect.Field;
import java.util.Arrays;
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
     */
    Mono<Void> validateRegisterDTO(RegisterDTO registerDTO);

    /**
     * 驗證重製密碼數據類ResetPasswordDTO中的數據是否合法
     *
     * @param resetPasswordDTO 重置密碼數據傳輸對象
     */
    Mono<Void> validateResetPasswordDTO(ResetPasswordDTO resetPasswordDTO);

    /**
     * 驗證文件元數據DTO中的數據是否合法
     *
     * @param fileMetadataDTO 文件元數據DTO
     */
    Mono<Void> validateFileMetadataDTO(FileMetadataDTO fileMetadataDTO, User user);

    /**
     * 驗證編輯文件DTO中的數據是否合法
     *
     * @param fileEditDTO 編輯文件DTO
     * @param isFolder    是否為文件夾
     */
    Mono<Void> validateEditFileDTO(FileEditDTO fileEditDTO, boolean isFolder);

    /**
     * 驗證檔案類型是否合法
     *
     * @param file   文件
     * @param fileType 規範的文件類型
     *
     * @return Mono<UserFileMetadata> 返回文件元數據
     */
    Mono<UserFileMetadata> validateFileType(UserFileMetadata file, FileEnum... fileType);

    /**
     * 驗證數據傳輸對象是否為空
     *
     * @param <T> 數據傳輸對象類型
     * @param dto 數據傳輸對象
     */
    default <T> Mono<Void> validateNotNull(T dto) {
        return Mono.defer(() -> {
            if (Objects.isNull(dto)) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
            }
            return Mono.empty();
        });
    }

    /**
     * 自訂義檢測字段欄位的方法，部分字段有時可以為空，但是有時又不能為空
     * 這時可以使用這個方法來檢測指定的字段是否為空
     *
     * @param dto     數據傳輸對象
     * @param columns 指定的字段
     * @param <T>     泛型
     *
     * @return Mono<Void>
     */
    default <T> Mono<Void> validSpecifyColumns(T dto, String... columns) {
        return Mono.defer(() -> {
            if (Objects.isNull(dto)) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
            }

            Field[] fields = dto.getClass().getDeclaredFields();
            if (fields.length == 0) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));
            }

            for (String column : columns) {
                boolean columnFound = Arrays.stream(fields).anyMatch(field -> field.getName().equals(column));
                if (!columnFound) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.COLUMN_NOT_FOUND, column));
                }

                try {
                    Field field = dto.getClass().getDeclaredField(column);
                    field.setAccessible(true);
                    Object value = field.get(dto);
                    if (value instanceof String && ((String) value).isBlank()) {
                        return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, column));
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.COLUMN_NOT_FOUND, column));
                }
            }
            return Mono.empty();
        });
    }

    /**
     * 驗證字段長度是否合法，此為重載方法只驗證最大長度
     *
     * @param object    字段值
     * @param maxLength 最大長度
     * @param columns   字段名
     *
     * @return Mono<Void>
     */
    default Mono<Void> validLength(Object object, Number maxLength, String... columns) {
        return validLength(object, -1, maxLength, columns);
    }

    /**
     * 驗證字段長度是否合法
     *
     * @param object    字段值
     * @param minLength 最小長度
     * @param maxLength 最大長度
     * @param columns   字段名
     *
     * @return Mono<Void>
     */
    default Mono<Void> validLength(Object object, Number minLength, Number maxLength, String... columns) {
        if (object == null) {
            return Mono.empty();
        }
        double min = minLength == null ? -1 : minLength.doubleValue();
        double max = maxLength == null ? -1 : maxLength.doubleValue();
        return Mono.defer(() -> {
            if (min > -1 && object.toString().length() < min) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.FIELD_LENGTH_TOO_SHORT,
                                                          columns[0] == null ? "未知" : columns[0],
                                                          minLength,
                                                          object.toString().length()
                ));
            }
            if (max > -1 && object.toString().length() > max) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.FIELD_LENGTH_TOO_LONG,
                                                          columns[0] == null ? "未知" : columns[0],
                                                          maxLength,
                                                          object.toString().length()
                ));
            }
            return Mono.empty();
        });
    }
}
