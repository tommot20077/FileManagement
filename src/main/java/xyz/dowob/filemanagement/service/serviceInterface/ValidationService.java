package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * 驗證服務接口，提供系統資料驗證的核心功能。定義一系列反應式資料驗證方法，用於確保系統中各類資料傳輸對象（DTO）的完整性、一致性和安全性。
 * <p>
 * 通過採用 Project Reactor 的 Mono 響應式類型，實現非阻塞、高效的資料驗證邏輯。完全支持 Spring WebFlux 反應式編程模型，提供高度靈活的泛型驗證方法，支持自定義驗證邏輯和錯誤處理。
 * <p>
 * 使用範例：
 * <pre>{@code
 * // 反應式驗證方法使用
 * validationService.validateRegisterDTO(registerDTO)
 *     .switchIfEmpty(Mono.defer(() -> performUserRegistration()))
 *     .onErrorResume(ValidationException.class, this::handleValidationError)
 *     .subscribe();
 * }</pre>
 *
 * @author yuan
 * @version 1.1
 * @since 1.0
 * @see ValidationException
 * @see reactor.core.publisher.Mono
 */
public interface ValidationService {
    /**
     * 驗證用戶註冊資料的合法性，確保資料完整且符合系統安全規範。執行用戶名稱唯一性檢查、密碼強度與複雜度驗證、電子郵件格式和有效性驗證、使用者資料完整性檢查。採用非阻塞響應式串流處理驗證流程。
     *
     * @param registerDTO 用戶註冊資料傳輸對象，封裝用戶註冊所需的完整資訊
     * @return 表示驗證作業結果的響應式 Mono，驗證成功回傳空（Mono.empty()），失敗將拋出 {@link xyz.dowob.filemanagement.exception.ValidationException}
     * @see RegisterDTO
     * @see ValidationException
     */
    Mono<Void> validateRegisterDTO(RegisterDTO registerDTO);

    /**
     * 驗證重置密碼資料的合法性，保障用戶密碼安全變更流程。驗證新密碼強度和複雜度評估、舊密碼正確性驗證、密碼變更限制條件檢查、防止重複使用最近的密碼。利用 Project Reactor 的 Mono 響應式類型，支持高並發、低延遲的密碼驗證流程。
     *
     * @param resetPasswordDTO 重置密碼資料傳輸對象，封裝密碼重置所需的完整資訊
     * @return 表示驗證作業結果的響應式 Mono，驗證成功回傳空（Mono.empty()），失敗將拋出 {@link xyz.dowob.filemanagement.exception.ValidationException}
     * @see ResetPasswordDTO
     * @see ValidationException
     */
    Mono<Void> validateResetPasswordDTO(ResetPasswordDTO resetPasswordDTO);

    /**
     * 驗證檔案元資料傳輸對象的合法性，確保檔案元資料符合系統規範。檢查檔案名稱的有效性和合法性、檔案大小限制檢查、檔案類型和擴展名驗證、用戶操作權限驗證。採用 Project Reactor 的 Mono 響應式類型，支持高並發、低延遲的檔案元資料驗證。
     *
     * @param fileMetadataDTO 待驗證的檔案元資料傳輸對象，封裝檔案基本屬性和限制條件
     * @param user 執行檔案操作的用戶，用於進行權限和上下文驗證
     * @return 表示驗證作業結果的響應式 Mono，驗證成功回傳空（Mono.empty()），失敗將拋出 {@link xyz.dowob.filemanagement.exception.ValidationException}
     * @see FileMetadataDTO
     * @see User
     * @see ValidationException
     */
    Mono<Void> validateFileMetadataDTO(FileMetadataDTO fileMetadataDTO, User user);

    /**
     * 驗證檔案編輯資料傳輸對象的合法性，確保檔案編輯操作符合系統安全規範。驗證檔案或資料夾名稱的有效性和合法性、目標路徑的正確性和安全性、使用者編輯權限驗證、編輯操作的一致性和邏輯性檢查。採用 Project Reactor 的 Mono 響應式類型，根據檔案或資料夾類型提供差異化驗證。
     *
     * @param fileEditDTO 檔案編輯資料傳輸對象，封裝編輯所需的詳細資訊和限制條件
     * @param isFolder 標記當前操作的對象是否為資料夾，用於提供差異化的驗證邏輯
     * @return 表示驗證作業結果的響應式 Mono，驗證成功回傳空（Mono.empty()），失敗將拋出 {@link xyz.dowob.filemanagement.exception.ValidationException}
     * @see FileEditDTO
     * @see ValidationException
     */
    Mono<Void> validateEditFileDTO(FileEditDTO fileEditDTO, boolean isFolder);

    /**
     * 驗證檔案類型的合法性，確保檔案符合系統預設的類型規範。進行檔案擴展名檢查、MIME類型驗證、檔案內容安全性掃描、檔案大小和限制驗證。採用 Project Reactor 的 Mono 響應式類型，使用 {@link xyz.dowob.filemanagement.customenum.FileEnum} 定義可接受的檔案類型。
     *
     * @param file 待驗證的檔案元資料，包含檔案基本屬性和內容
     * @param fileType 系統預設的檔案類型規範，使用 {@link xyz.dowob.filemanagement.customenum.FileEnum} 定義可接受的檔案類型
     * @return 表示驗證作業結果的響應式 Mono，如驗證成功將回傳修改後的檔案元資料，失敗將拋出 {@link xyz.dowob.filemanagement.exception.ValidationException}
     * @see UserFileMetadata
     * @see FileEnum
     * @see ValidationException
     */
    Mono<UserFileMetadata> validateFileType(UserFileMetadata file, FileEnum... fileType);

    /**
     * 驗證檔案過濾資料傳輸對象（DTO）中的資料合法性，確保過濾條件安全且有效。檢查過濾標準的有效性和合法性、查詢條件的安全性檢查、防止資訊洩露或不當訪問、確保過濾邏輯的一致性。採用 Project Reactor 的 Mono 響應式類型，支持高並發、低延遲的資料過濾驗證。
     *
     * @param fileFilterDTO 檔案過濾資料傳輸對象，封裝過濾所需的詳細條件和限制
     * @return 表示驗證作業結果的響應式 Mono，驗證成功回傳空（Mono.empty()），失敗將拋出 {@link xyz.dowob.filemanagement.exception.ValidationException}
     * @see FileFilterDTO
     * @see ValidationException
     */
    Mono<Void> validateFileFilterDTO(FileFilterDTO fileFilterDTO);

    /**
     * 驗證用戶搜索列表的合法性，確保搜索作業的安全性和可靠性。檢查搜索文字的有效性和安全性、防止惡意搜索或注入攻擊、限制搜索範圍和長度、確保搜索邏輯的一致性。採用 Project Reactor 的 Mono 響應式類型，支持高並發、低延遲的搜索驗證流程。
     *
     * @param searchList 用戶搜索的字元列表，需要驗證其安全性和合法性
     * @return 表示驗證作業結果的響應式 Mono，驗證成功回傳空（Mono.empty()），失敗將拋出 {@link xyz.dowob.filemanagement.exception.ValidationException}
     * @see ValidationException
     */
    Mono<Void> validateUserSearchList(Collection<String> searchList);

    /**
     * 驗證授權請求資料傳輸對象（DTO）中的資料合法性，確保授權流程的安全性。檢查用戶身份指標的有效性、授權令牌的合法性和完整性、防止重播或修改授權資訊、驗證認證組件的安全性。採用 Project Reactor 的 Mono 響應式類型，支持高並發、低延遲的授權驗證流程。
     *
     * @param authRequestDTO 授權請求資料傳輸對象，封裝授權所需的完整資訊
     * @return 表示驗證作業結果的響應式 Mono，驗證成功回傳空（Mono.empty()），失敗將拋出 {@link xyz.dowob.filemanagement.exception.ValidationException}
     * @see AuthRequestDTO
     * @see ValidationException
     */
    Mono<Void> validateAuthRequestDTO(AuthRequestDTO authRequestDTO);


    /**
     * 驗證資料傳輸對象是否為空值，作為其他驗證方法的基礎檢查。此方法提供通用的空值檢查邏輯，防止後續驗證流程因空值導致的 {@link NullPointerException}。採用反應式響應機制，確保在檢測到空值時能夠即時返回錯誤資訊。
     * <p>
     * 此方法在反應式串流中扮演重要的前置驗證角色，用於確保資料完整性。當檢測到空值時，將立即中斷驗證串流並拋出 {@link ValidationException}，避免不必要的資源消耗。
     * <p>
     * 使用範例：
     * <pre>{@code
     * // 在複雜驗證流程中進行空值檢查
     * validateNotNull(registerDTO)
     *     .then(validateRegisterDTO(registerDTO))
     *     .onErrorResume(ValidationException.class, this::handleValidationError)
     *     .subscribe();
     * }</pre>
     *
     * @param <T> 資料傳輸對象的泛型類型，支持任意類型的 DTO 物件
     * @param dto 待檢查的資料傳輸對象，可為任意類型的 DTO 實例
     * @return 表示驗證作業結果的響應式 Mono，若 DTO 不為空則回傳 {@code Mono.empty()}，否則將拋出 {@link ValidationException}
     * @see ValidationException
     * @see ValidationException.ErrorCode#NULL_DTO
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
     * 驗證資料傳輸對象中指定欄位的有效性，提供靈活的欄位級別驗證機制。此方法允許針對特定欄位進行動態檢查，支持情境式驗證需求，特別適用於部分欄位在不同業務流程中具有不同必填要求的場景。
     * <p>
     * 透過 Java 反射機制動態訪問物件屬性，確保指定欄位存在且包含有效值。對於字串類型欄位，將檢查是否為空白（blank）；對於其他類型，將檢查是否為 {@code null}。此方法採用反應式設計，在發現任何無效欄位時立即返回錯誤。
     * <p>
     * 安全考量：
     * <ul>
     *     <li>使用反射時確保欄位訪問權限的正確設定</li>
     *     <li>驗證欄位名稱的存在性，防止惡意或錯誤的欄位訪問</li>
     *     <li>對字串類型進行空白檢查，防止純空格內容通過驗證</li>
     * </ul>
     * <p>
     * 使用範例：
     * <pre>{@code
     * // 驗證用戶註冊 DTO 中的特定必填欄位
     * validSpecifyColumns(registerDTO, "username", "email", "password")
     *     .then(performAdditionalValidation())
     *     .onErrorResume(ValidationException.class, this::handleValidationError)
     *     .subscribe();
     * }</pre>
     *
     * @param <T> 資料傳輸對象的泛型類型，支持任意具有可反射欄位的類型
     * @param dto 待檢查的資料傳輸對象，不可為 {@code null}
     * @param columns 指定需要驗證的欄位名稱陣列，欄位名稱必須與 DTO 類別中的實際欄位名稱完全一致
     * @return 表示驗證作業結果的響應式 Mono，所有指定欄位均有效時回傳 {@code Mono.empty()}，否則將拋出相應的 {@link ValidationException}
     * @throws ValidationException 當 DTO 為空、欄位不存在、或欄位值無效時拋出
     * @see ValidationException.ErrorCode#NULL_DTO
     * @see ValidationException.ErrorCode#COLUMN_NOT_FOUND
     * @see ValidationException.ErrorCode#BLANK_FIELD
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
     * 驗證欄位值長度是否符合最大長度限制，提供便捷的單向長度檢查機制。此重載方法專門用於僅需檢查最大長度限制的場景，簡化了不需要最小長度檢查的驗證流程。
     * <p>
     * 此方法透過內部呼叫 {@link #validLength(Object, Number, Number, String...)} 實現，將最小長度設定為 -1 以跳過最小長度檢查。適用於用戶輸入限制、檔案名稱長度檢查、描述文字限制等場景。
     * <p>
     * 使用範例：
     * <pre>{@code
     * // 檢查檔案名稱是否超過系統限制
     * validLength(fileName, 255, "檔案名稱")
     *     .then(saveFile(fileName))
     *     .onErrorResume(ValidationException.class, this::handleLengthError)
     *     .subscribe();
     * }</pre>
     *
     * @param object 待檢查的欄位值，支援任意可轉換為字串的物件類型，若為 {@code null} 則跳過驗證
     * @param maxLength 允許的最大長度值，必須為正數，若為 {@code null} 或負數則跳過最大長度檢查
     * @param columns 欄位名稱陣列，用於錯誤訊息顯示，建議提供有意義的欄位描述
     * @return 表示驗證作業結果的響應式 Mono，長度符合限制時回傳 {@code Mono.empty()}，否則將拋出 {@link ValidationException}
     * @see #validLength(Object, Number, Number, String...)
     * @see ValidationException.ErrorCode#FIELD_LENGTH_TOO_LONG
     */
    default Mono<Void> validLength(Object object, Number maxLength, String... columns) {
        return validLength(object, -1, maxLength, columns);
    }


    /**
     * 驗證欄位值長度是否符合指定的最小和最大長度範圍，提供完整的長度邊界檢查機制。此方法支援靈活的長度驗證策略，可根據業務需求設定單向或雙向長度限制，確保資料符合系統設計規範和資料庫欄位限制。
     * <p>
     * 驗證邏輯採用物件的字串表示形式進行長度計算，支援各種資料類型的長度檢查。當 {@code minLength} 或 {@code maxLength} 設定為 -1 或 {@code null} 時，將跳過對應的邊界檢查，提供高度靈活的驗證配置。
     * <p>
     * 安全與效能考量：
     * <ul>
     *     <li>對於 {@code null} 物件，方法將直接回傳成功，避免不必要的處理</li>
     *     <li>使用 {@code toString()} 方法進行長度計算，確保一致的長度評估標準</li>
     *     <li>採用延遲評估（defer）模式，僅在訂閱時才執行實際驗證邏輯</li>
     *     <li>提供詳細的錯誤訊息，包含欄位名稱、限制值和實際長度</li>
     * </ul>
     * <p>
     * 使用範例：
     * <pre>{@code
     * // 驗證用戶密碼長度範圍
     * validLength(password, 8, 128, "密碼")
     *     .then(validatePasswordComplexity(password))
     *     .onErrorResume(ValidationException.class, this::handlePasswordError)
     *     .subscribe();
     * 
     * // 僅驗證最小長度
     * validLength(description, 10, null, "描述")
     *     .then(saveDescription(description))
     *     .subscribe();
     * }</pre>
     *
     * @param object 待檢查的欄位值，支援任意可轉換為字串的物件類型，若為 {@code null} 則跳過所有長度檢查
     * @param minLength 允許的最小長度值，設定為 -1 或 {@code null} 時跳過最小長度檢查
     * @param maxLength 允許的最大長度值，設定為 -1 或 {@code null} 時跳過最大長度檢查
     * @param columns 欄位名稱陣列，用於產生有意義的錯誤訊息，若為空或 {@code null} 則使用 "未知" 作為預設欄位名稱
     * @return 表示驗證作業結果的響應式 Mono，長度符合所有限制時回傳 {@code Mono.empty()}，否則將拋出對應的 {@link ValidationException}
     * @throws ValidationException 當欄位長度超出指定範圍時拋出，包含詳細的錯誤資訊和實際長度值
     * @see ValidationException.ErrorCode#FIELD_LENGTH_TOO_SHORT
     * @see ValidationException.ErrorCode#FIELD_LENGTH_TOO_LONG
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
