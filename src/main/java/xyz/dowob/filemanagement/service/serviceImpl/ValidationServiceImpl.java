package xyz.dowob.filemanagement.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.customenum.ByteEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
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
 * 驗證服務實現類，負責系統中各類資料的完整性和合法性驗證。提供用戶註冊資料驗證、檔案元資料驗證、密碼重設驗證等功能。
 * <p>
 * 所有驗證操作都採用響應式編程模式，回傳 {@code Mono<Void>} 類型。驗證失敗時通過 {@code Mono.error()} 傳播 {@link ValidationException}，
 * 包含具體的錯誤程式和詳細訊息。
 * <p>
 * 支持多層次驗證規則：密碼強度檢查（包含大小寫字母、數字、非回文）、檔案名稱格式驗證（長度限制、非法字元檢查）、
 * 用戶存儲空間限制驗證等。檔案驗證採用正則表達式模式匹配，確保檔案名不包含系統保留字元。
 * <p>
 * 資料庫查詢採用響應式方式，檢查用戶名和電子郵件的唯一性約束。所有驗證方法支持並行執行，提高驗證效率。
 * <p>
 * <strong>使用示例：</strong>
 * <pre>{@code
 * // 驗證用戶註冊資料
 * RegisterDTO registerDTO = new RegisterDTO("user", "email@test.com", "password", "password");
 * Mono<Void> validation = validationService.validateRegisterDTO(registerDTO);
 *
 * // 驗證檔案名稱
 * Mono<Void> fileValidation = validationService.validateFileMetadataDTO(fileDTO, user);
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @see ValidationService
 * @see ValidationException
 * @since 1.0
 */

@Service
@RequiredArgsConstructor
public class ValidationServiceImpl implements ValidationService {
    /**
     * 非法字元正則表達式，用於檢查檔案名是否包含禁用字元（/ \ | " '）
     */
    private static final Pattern INVALID_CHARACTERS_PATTERN = Pattern.compile("[/\\\\|\"']");

    /**
     * 用戶資料庫存取介面，用於查詢用戶名和電子郵件存在性
     */
    private final UserRepository userRepository;


    /**
     * 驗證用戶註冊資料的完整性和合法性。
     * <p>
     * 驗證項目包括密碼與確認密碼一致性、用戶名唯一性、電子郵件唯一性、密碼強度符合安全要求（包含大小寫字母和數字，且非回文）。
     * 所有驗證項目並行執行，任一項目失敗都會導致整體驗證失敗。
     * <p>
     * 用戶名必須僅包含字母和數字，電子郵件和用戶名的唯一性通過資料庫查詢確認。
     * 密碼強度檢查採用多重條件驗證，確保密碼包含必要的字元類型且不為回文結構。
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * RegisterDTO dto = new RegisterDTO("newuser", "user@example.com", "StrongPass123", "StrongPass123");
     *
     * validateRegisterDTO(dto)
     *     .doOnSuccess(() -> log.info("註冊資料驗證成功"))
     *     .doOnError(ValidationException.class, ex -> {
     *         log.error("驗證失敗: {}", ex.getMessage());
     *     })
     *     .subscribe();
     * }</pre>
     *
     * @param registerDTO 用戶註冊資料，包含用戶名、電子郵件和密碼，不可為 {@code null}
     *
     * @return 空的 {@code Mono}，驗證成功時完成，失敗時傳播 {@code ValidationException}
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> validateRegisterDTO(RegisterDTO registerDTO) {
        return validateNotNull(registerDTO).then(Mono.defer(() -> {
            return Mono.when(validatePasswordsMatch(registerDTO.getPassword(), registerDTO.getConfirmPassword()),
                             validateUsernameNotExists(registerDTO.getUsername()),
                             validateEmailNotExists(registerDTO.getEmail()),
                             validatePasswordStrength(registerDTO.getPassword())
            );
        }));
    }


    /**
     * 驗證密碼重設資料的完整性和安全性，確保新密碼符合安全要求。
     * <p>
     * 此方法執行新密碼與確認密碼一致性檢查、新密碼強度驗證、重設密碼資料完整性確認。
     * 密碼強度驗證包含大小寫字母、數字要求以及非回文結構檢查。
     * <p>
     * 驗證失敗時會傳播相應的 {@code ValidationException}，指明具體的錯誤原因。
     * 所有驗證條件必須同時滿足才能通過驗證。
     *
     * @param resetPasswordDTO 包含新密碼和確認密碼的重設密碼資料傳輸對象
     *
     * @return 空的 {@code Mono}，驗證成功時完成操作，失敗時傳播 {@code ValidationException}
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> validateResetPasswordDTO(ResetPasswordDTO resetPasswordDTO) {
        return validateNotNull(resetPasswordDTO).then(Mono.defer(() -> {
            return Mono.when(validatePasswordsMatch(resetPasswordDTO.getNewPassword(), resetPasswordDTO.getConfirmPassword()),
                             validatePasswordStrength(resetPasswordDTO.getNewPassword())
            );
        }));
    }


    /**
     * 驗證檔案元資料 DTO 中的資料合法性。
     * <p>
     * 驗證項目包括檔案名稱格式合法性（不包含非法字元，長度不超過 200 字元）、用戶儲存空間是否足夠（根據用戶的儲存限制檢查）。
     * 檔案名稱格式檢查使用正則表達式匹配，禁止包含系統保留字元如斜線、反斜線、管道符號等。
     * <p>
     * 儲存空間檢查會比較用戶已使用空間加上新檔案大小是否超過用戶的儲存配額。
     * 如果用戶儲存限制設為 -1，則表示無限制，跳過儲存空間檢查。
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * FileMetadataDTO dto = new FileMetadataDTO();
     * dto.setFilename("document.pdf");
     * dto.setFileSize(1024 * 1024); // 1MB
     *
     * validateFileMetadataDTO(dto, currentUser)
     *     .doOnSuccess(() -> log.info("檔案元資料驗證成功"))
     *     .subscribe();
     * }</pre>
     *
     * @param fileMetadataDTO 檔案元資料 DTO，包含檔案名和大小等資訊，不可為 {@code null}
     * @param user            用戶對象，用於檢查儲存限制，不可為 {@code null}
     *
     * @return 空的 {@code Mono}，驗證成功時完成，失敗時傳播 {@code ValidationException}
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> validateFileMetadataDTO(FileMetadataDTO fileMetadataDTO, User user) {
        return validateNotNull(fileMetadataDTO).then(Mono.defer(() -> {
            return Mono.when(validFileName(fileMetadataDTO.getFilename(), false),
                             validateUserStorageLimit(user.getStorageLimit(), user.getUsedStorage(), fileMetadataDTO.getFileSize())
            );
        }));
    }


    /**
     * 驗證檔案編輯資料傳輸對象的完整性和操作合法性。
     * <p>
     * 根據編輯類型執行不同的驗證邏輯：檔案名稱編輯時驗證檔案名格式合法性（包含檔案夾特殊處理）；
     * 內容編輯時檢查內容長度不超過 1MB 限制；歷史記錄建立時同時驗證內容長度和備註長度；
     * 歷史記錄復原或刪除操作無需額外驗證。
     * <p>
     * 檔案名稱驗證採用不同規則處理一般檔案和檔案夾，檔案夾無需包含副檔名但仍需符合字元限制。
     * 內容長度檢查確保單次操作的資料量不會對系統造成負擔。
     *
     * @param fileEditDTO 檔案編輯資料傳輸對象，包含編輯類型、檔案名、內容等資訊，不可為 {@code null}
     * @param isFolder    是否為檔案夾操作，影響檔案名稱驗證規則
     *
     * @return 空的 {@code Mono}，驗證成功時完成，失敗時傳播 {@code ValidationException}
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> validateEditFileDTO(FileEditDTO fileEditDTO, boolean isFolder) {
        return validateNotNull(fileEditDTO).then(Mono.defer(() -> {
            return switch (fileEditDTO.getEditType()) {
                case EDIT_METADATA -> validFileName(fileEditDTO.getFilename(), isFolder);
                case EDIT_CONTENT -> validLength(fileEditDTO.getContent(), Math.pow(2, 20), "檔案內容");
                case BUILD_HISTORY_RECORD ->
                        validLength(fileEditDTO.getContent(), Math.pow(2, 20), "檔案內容").then(validLength(fileEditDTO.getNote(), 1000, "備註"));
                case REVERT_HISTORY_RECORD, DELETE_HISTORY_RECORD -> Mono.empty();
                case null -> Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "editType"));
            };
        }));
    }


    /**
     * 驗證使用者檔案元資料的檔案類型是否符合指定的允許類型範圍。
     * <p>
     * 此方法提供靈活的檔案類型檢查機制，支援多重檔案類型條件匹配。當未指定檔案類型限制時，
     * 所有檔案類型都被視為合法。檔案類型驗證採用枚舉比對方式，確保檔案符合特定操作的類型要求。
     * <p>
     * 常用於限制特定操作只能處理特定類型的檔案，例如線上編輯功能僅支援文字類型檔案。
     * 空檔案參照會直接返回空 Mono，不進行任何驗證處理。
     *
     * @param file     使用者檔案元資料對象，可為 {@code null}
     * @param fileType 允許的檔案類型陣列，可為空或 {@code null} 表示不限制類型
     *
     * @return 包含原檔案元資料的 {@code Mono}，驗證失敗時傳播 {@code ValidationException}
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
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
     * 驗證檔案過濾傳輸對象的搜尋條件合法性和完整性。
     * <p>
     * 執行多層次的搜尋條件驗證：檢查搜尋條件非空性、關鍵字長度限制（2-50字元）、
     * 檔案夾 ID 有效性（非負數）、時間範圍邏輯正確性（開始時間不晚於結束時間）。
     * <p>
     * 搜尋條件驗證確保系統效能和使用者體驗，防止過短關鍵字導致的大量結果或過長關鍵字造成的效能問題。
     * 時間範圍檢查避免邏輯錯誤的查詢條件，確保搜尋結果的準確性。
     *
     * @param fileFilterDTO 檔案過濾傳輸對象，包含搜尋關鍵字、檔案夾 ID、時間範圍等條件，不可為 {@code null}
     *
     * @return 空的 {@code Mono}，驗證成功時完成，失敗時傳播 {@code ValidationException}
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
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
     * 驗證使用者搜尋清單的數量限制和完整性。
     * <p>
     * 檢查搜尋清單非空性和數量合理性，限制單次搜尋的使用者數量不超過 20 位，
     * 防止大量並行查詢對系統效能造成負面影響。此限制平衡了使用者需求和系統資源消耗。
     * <p>
     * 適用於批次使用者資訊查詢、權限�查等需要處理多個使用者的操作場景。
     *
     * @param searchList 包含使用者識別資訊的搜尋清單，不可為 {@code null} 或空集合
     *
     * @return 空的 {@code Mono}，驗證成功時完成，失敗時傳播 {@code ValidationException}
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
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
     * 驗證使用者身份驗證請求資料的完整性和格式正確性。
     * <p>
     * 檢查登入請求中使用者名稱和密碼欄位的非空性，確保身份驗證流程能夠正常執行。
     * 此驗證為後續密碼比對和使用者查詢提供基本的資料完整性保障。
     * <p>
     * 驗證失敗時會明確指出缺失的欄位資訊，協助用戶端修正請求資料。
     * 此方法僅驗證資料格式，不涉及實際的身份驗證邏輯。
     *
     * @param authRequestDTO 身份驗證請求傳輸對象，包含使用者名稱和密碼，不可為 {@code null}
     *
     * @return 空的 {@code Mono}，驗證成功時完成，失敗時傳播 {@code ValidationException}
     */
    @Override
    public Mono<Void> validateAuthRequestDTO(AuthRequestDTO authRequestDTO) {
        return validateNotNull(authRequestDTO).then(Mono.defer(() -> {
            if (authRequestDTO.getUsername() == null || authRequestDTO.getUsername().isBlank()) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "username"));
            }

            if (authRequestDTO.getPassword() == null || authRequestDTO.getPassword().isBlank()) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "password"));
            }
            return Mono.empty();
        }));
    }


    /**
     * 驗證檔案名稱的格式合法性和字元限制。
     * <p>
     * 檢查檔案名稱是否包含系統保留字元（斜線、反斜線、管道符號、引號），
     * 驗證名稱長度不超過 200 字元限制，確保檔案系統相容性。
     * <p>
     * 一般檔案必須包含副檔名（含有點號），檔案夾則無此要求但仍需符合字元限制。
     * 使用預編譯正則表達式模式進行高效的非法字元檢測。
     *
     * @param filename 待驗證的檔案名稱，不可為 {@code null} 或空白字串
     * @param isFolder 是否為檔案夾，影響副檔名要求的驗證規則
     *
     * @return 空的 {@code Mono}，驗證成功時完成，失敗時傳播 {@code ValidationException}
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
     * 驗證使用者儲存空間是否足夠容納新檔案。
     * <p>
     * 計算使用者當前已使用空間加上預期檔案大小是否超過儲存配額限制。
     * 當儲存限制設為 -1 時表示無限制，跳過空間檢查。
     * <p>
     * 錯誤訊息包含人類可讀的容量資訊，使用 {@link ByteEnum} 轉換為適當的單位顯示。
     * 此檢查防止使用者超出分配的儲存配額，維護系統資源的公平分配。
     *
     * @param storageLimit 使用者的儲存空間限制（位元組），-1 表示無限制
     * @param alreadyUsed  使用者目前已使用的儲存空間（位元組）
     * @param expectSize   預期新增檔案的大小（位元組）
     *
     * @return 空的 {@code Mono}，驗證成功時完成，空間不足時傳播 {@code ValidationException}
     */
    private Mono<Void> validateUserStorageLimit(long storageLimit, long alreadyUsed, long expectSize) {
        if (storageLimit == -1) {
            return Mono.empty();
        }
        if (storageLimit < alreadyUsed + expectSize) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.STORAGE_LIMIT_EXCEEDED,
                                                      ByteEnum.toReadableSize(storageLimit),
                                                      ByteEnum.toReadableSize(alreadyUsed),
                                                      ByteEnum.toReadableSize(expectSize)
            ));
        }
        return Mono.empty();
    }


    /**
     * 驗證密碼與確認密碼的一致性。
     * <p>
     * 檢查兩個密碼參數是否完全相符，防止使用者因輸入錯誤而設定非預期的密碼。
     * 此驗證是密碼設定和重設流程的重要安全措施。
     * <p>
     * 當任一密碼參數為空時，會明確指出缺失的欄位名稱以便偵錯。
     *
     * @param password        原始密碼，不可為 {@code null}
     * @param confirmPassword 確認密碼，不可為 {@code null}
     *
     * @return 空的 {@code Mono}，驗證成功時完成，密碼不符時傳播 {@code ValidationException}
     */
    private Mono<Void> validatePasswordsMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            String message = password == null ? "password" : "confirmPassword";
            return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, message));
        }

        return Mono.defer(() -> {
            if (!password.equals(confirmPassword)) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.CONFIRM_PASSWORD_NOT_MATCH));
            }
            return Mono.empty();
        });
    }


    /**
     * 驗證使用者名稱的唯一性和格式合法性。
     * <p>
     * 執行雙重驗證：透過資料庫查詢確認使用者名稱未被註冊，
     * 以及檢查名稱僅包含英文字母和數字字元的組合規則。
     * <p>
     * 使用者名稱格式要求同時包含至少一個字母和一個數字，確保名稱的複雜性和唯一性。
     * 資料庫查詢採用響應式方式，避免阻塞執行緒。
     *
     * @param username 待驗證的使用者名稱，不可為 {@code null} 或空白字串
     *
     * @return 空的 {@code Mono}，驗證成功時完成，失敗時傳播 {@code ValidationException}
     */
    private Mono<Void> validateUsernameNotExists(String username) {
        if (username == null || username.isBlank()) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "username"));
        }

        return userRepository.findByUsername(username).flatMap(user -> {
            if (user != null) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_INVALID, username));
            }
            return Mono.empty();
        }).then(alphanumericInspection(username));
    }


    /**
     * 驗證電子郵件地址的唯一性。
     * <p>
     * 透過資料庫查詢檢查指定的電子郵件地址是否已被其他使用者註冊。
     * 確保每個電子郵件地址在系統中的唯一性，維護使用者帳戶的完整性。
     * <p>
     * 查詢操作採用響應式程式設計模式，避免阻塞主執行緒。
     * 當電子郵件地址已存在時，錯誤訊息會包含具體的郵件地址資訊。
     *
     * @param email 待驗證的電子郵件地址，不可為 {@code null} 或空白字串
     *
     * @return 空的 {@code Mono}，驗證成功時完成，郵件地址已存在時傳播 {@code ValidationException}
     */
    private Mono<Void> validateEmailNotExists(String email) {
        if (email == null || email.isBlank()) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "email"));
        }
        return userRepository.findByEmail(email).flatMap(user -> {
            if (user != null) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.EMAIL_ALREADY_EXISTS, email));
            }
            return Mono.empty();
        });
    }


    /**
     * 驗證密碼強度是否符合安全要求。
     * <p>
     * 執行兩項安全檢查：回文結構檢測（防止簡單對稱密碼）和字元類型完整性驗證
     * （必須同時包含大寫字母、小寫字母和數字）。
     * <p>
     * 回文檢查防止使用者設定如 "abccba" 等容易被猜測的對稱密碼。
     * 字元類型檢查確保密碼具有足夠的複雜性，提高安全性。
     *
     * @param password 待驗證的密碼，不可為 {@code null} 或空白字串
     *
     * @return 空的 {@code Mono}，驗證成功時完成，強度不足時傳播 {@code ValidationException}
     */
    private Mono<Void> validatePasswordStrength(String password) {
        if (password == null || password.isBlank()) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "password"));
        }
        return palindromeInspection(password).then(upperLetterAndLowerLetterAndNumberInspection(password));
    }


    /**
     * 檢查字串是否僅包含英文字母和數字字元。
     * <p>
     * 使用正則表達式驗證字串格式，要求同時包含至少一個字母和一個數字，
     * 且整個字串僅由字母和數字組成。此規則確保使用者名稱的安全性和唯一性。
     * <p>
     * 正則表達式模式 "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9]*$" 提供高效的字元驗證。
     *
     * @param username 待檢查的字串，通常為使用者名稱，不可為 {@code null} 或空白字串
     *
     * @return 空的 {@code Mono}，驗證成功時完成，格式不符時傳播 {@code ValidationException}
     */
    private Mono<Void> alphanumericInspection(String username) {
        if (username == null || username.isBlank()) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.BLANK_FIELD, "username"));
        }
        return Mono.defer(() -> {
            if (Pattern.matches("^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9]*$", username)) {
                return Mono.empty();
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_INVALID, username));
        });
    }


    /**
     * 檢查字串是否為回文結構。
     * <p>
     * 使用雙指標演算法檢測字串是否為回文（正向和反向讀取結果相同）。
     * 回文密碼被視為安全性較低，容易被猜測和破解，因此在密碼驗證中被禁止。
     * <p>
     * 演算法複雜度為 O(n/2)，提供高效的回文檢測。當檢測到回文結構時，
     * 會回傳密碼強度不足的錯誤。
     *
     * @param str 待檢查的字串，不可為 {@code null}
     *
     * @return 空的 {@code Mono}，非回文時完成，檢測到回文時傳播 {@code ValidationException}
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
     * 檢查字串是否同時包含大寫字母、小寫字母和數字。
     * <p>
     * 遍歷字串中的每個字元，分別檢測大寫字母、小寫字母和數字的存在性。
     * 只有當三種字元類型都存在時，密碼才被認為具有足夠的複雜性。
     * <p>
     * 此檢查是密碼強度驗證的核心組成部分，確保密碼具有足夠的字元多樣性，
     * 提高密碼被暴力破解的難度。使用字元分類方法進行高效檢測。
     *
     * @param str 待檢查的字串，通常為密碼，不可為 {@code null}
     *
     * @return 空的 {@code Mono}，包含所有必要字元類型時完成，缺少任一類型時傳播 {@code ValidationException}
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
}
