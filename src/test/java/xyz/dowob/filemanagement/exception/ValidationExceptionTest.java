package xyz.dowob.filemanagement.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 驗證異常測試。
 * 
 * <p>測試 ValidationException 驗證異常類別的各種功能，驗證異常構造器的正常使用、
 * ErrorCode 枚舉的各種方法、格式化訊息處理，以及邊界條件的處理。
 * 
 * <p>前置條件：
 * <ul>
 * <li>ValidationException 類別正常載入</li>
 * <li>ErrorCode 枚舉定義完整</li>
 * </ul>
 * 
 * <p>測試步驟：
 * <ul>
 * <li>測試異常的創建和訊息格式化</li>
 * <li>測試 ErrorCode 的各種屬性訪問</li>
 * <li>測試 fromName 方法的正常和異常情況</li>
 * <li>測試邊界值和特殊情況</li>
 * </ul>
 * 
 * <p>預期結果：
 * <ul>
 * <li>所有異常創建和訊息格式化功能正常</li>
 * <li>ErrorCode 的各種查詢功能正確工作</li>
 * <li>邊界條件得到適當處理</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("ValidationException 驗證異常測試")
class ValidationExceptionTest {

    // ==================== 一般測試 ====================

    /**
     * 測試創建帶單一參數的驗證異常。
     * 
     * <p>驗證使用單一格式化參數創建驗證異常的功能。
     * 
     * <p>前置條件：
     * <ul>
     * <li>EMAIL_ALREADY_EXISTS 錯誤碼已定義</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>使用電子郵件作為參數創建異常</li>
     * <li>驗證異常屬性和訊息格式化</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>異常創建成功且錯誤碼正確</li>
     * <li>訊息包含格式化的電子郵件</li>
     * </ul>
     */
    @Test
    @DisplayName("一般測試 - 創建異常帶單一參數")
    void testCreateException_withSingleArgument() {
        String email = "test@example.com";
        ValidationException exception = new ValidationException(
            ValidationException.ErrorCode.EMAIL_ALREADY_EXISTS, email
        );
        
        assertEquals(ValidationException.ErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals("此信箱已經被註冊: test@example.com", exception.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 創建異常帶多個參數")
    void testCreateException_withMultipleArguments() {
        ValidationException exception = new ValidationException(
            ValidationException.ErrorCode.FIELD_LENGTH_TOO_LONG, 
            "password", "20", "30"
        );
        
        assertEquals(ValidationException.ErrorCode.FIELD_LENGTH_TOO_LONG, exception.getErrorCode());
        assertEquals("欄位: password 字段長度過長，最大長度限制: 20，當前長度: 30", exception.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 創建異常不帶參數")
    void testCreateException_withoutArguments() {
        ValidationException exception = new ValidationException(
            ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH
        );
        
        assertEquals(ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH, exception.getErrorCode());
        assertEquals("密碼強度不足", exception.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 驗證 ErrorCode 基本屬性")
    void testErrorCode_basicProperties() {
        ValidationException.ErrorCode errorCode = ValidationException.ErrorCode.USER_NOT_FOUND;
        
        assertEquals(1104, errorCode.getCode());
        assertEquals(HttpStatus.NOT_FOUND, errorCode.getHttpStatus());
        assertEquals("此用戶不存在: %s", errorCode.getMessage());
    }

    @Test
    @DisplayName("一般測試 - 驗證默認 HTTP 狀態碼")
    void testErrorCode_defaultHttpStatus() {
        ValidationException.ErrorCode errorCode = ValidationException.ErrorCode.NULL_DTO;
        
        assertEquals(1101, errorCode.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, errorCode.getHttpStatus());
        assertEquals("傳輸資料不能為空", errorCode.getMessage());
    }

    /**
     * 測試 fromName 方法的正常查詢功能。
     * 
     * <p>驗證 fromName 方法能夠正確根據名稱查找 ErrorCode。
     * 
     * <p>前置條件：
     * <ul>
     * <li>USER_NOT_FOUND 錯誤碼存在</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>使用有效名稱查詢 ErrorCode</li>
     * <li>驗證返回的 ErrorCode 是否正確</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>成功返回對應的 ErrorCode</li>
     * </ul>
     */
    @Test
    @DisplayName("一般測試 - fromName 方法正常查詢")
    void testFromName_validName() {
        ValidationException.ErrorCode result = ValidationException.ErrorCode.fromName("USER_NOT_FOUND");
        
        assertEquals(ValidationException.ErrorCode.USER_NOT_FOUND, result);
    }

    @Test
    @DisplayName("一般測試 - fromName 方法不區分大小寫")
    void testFromName_caseInsensitive() {
        ValidationException.ErrorCode result1 = ValidationException.ErrorCode.fromName("user_not_found");
        ValidationException.ErrorCode result2 = ValidationException.ErrorCode.fromName("User_Not_Found");
        ValidationException.ErrorCode result3 = ValidationException.ErrorCode.fromName("USER_NOT_FOUND");
        
        assertEquals(ValidationException.ErrorCode.USER_NOT_FOUND, result1);
        assertEquals(ValidationException.ErrorCode.USER_NOT_FOUND, result2);
        assertEquals(ValidationException.ErrorCode.USER_NOT_FOUND, result3);
    }

    // ==================== 異常測試 ====================

    /**
     * 測試 fromName 方法查詢不存在名稱的處理。
     * 
     * <p>驗證當查詢不存在的 ErrorCode 名稱時的返回值。
     * 
     * <p>前置條件：
     * <ul>
     * <li>fromName 方法正常工作</li>
     * </ul>
     * 
     * <p>測試步驟：
     * <ul>
     * <li>使用不存在的名稱查詢 ErrorCode</li>
     * <li>驗證返回值為 null</li>
     * </ul>
     * 
     * <p>預期結果：
     * <ul>
     * <li>返回 null 而非拋出異常</li>
     * </ul>
     */
    @Test
    @DisplayName("異常測試 - fromName 方法查詢不存在的名稱")
    void testFromName_nonExistentName() {
        ValidationException.ErrorCode result = ValidationException.ErrorCode.fromName("NON_EXISTENT_ERROR");
        
        assertNull(result);
    }

    @Test
    @DisplayName("異常測試 - fromName 方法傳入空字符串")
    void testFromName_emptyString() {
        ValidationException.ErrorCode result = ValidationException.ErrorCode.fromName("");
        
        assertNull(result);
    }

    @Test
    @DisplayName("異常測試 - fromName 方法傳入 null")
    void testFromName_nullInput() {
        assertThrows(NullPointerException.class, () -> {
            ValidationException.ErrorCode.fromName(null);
        });
    }

    @Test
    @DisplayName("異常測試 - 異常構造器傳入 null ErrorCode")
    void testCreateException_nullErrorCode() {
        assertThrows(NullPointerException.class, () -> {
            new ValidationException(null);
        });
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 格式化參數為 null")
    void testCreateException_nullFormatArguments() {
        ValidationException exception = new ValidationException(
            ValidationException.ErrorCode.USER_NOT_FOUND, (Object) null
        );
        
        assertEquals("此用戶不存在: null", exception.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - 格式化參數數量不匹配")
    void testCreateException_mismatchedArgumentCount() {
        // 測試參數過少的情況
        assertThrows(java.util.MissingFormatArgumentException.class, () -> {
            new ValidationException(ValidationException.ErrorCode.FIELD_LENGTH_TOO_LONG, "field");
        });
    }

    @Test
    @DisplayName("邊界測試 - 格式化參數過多")
    void testCreateException_tooManyArguments() {
        // 測試參數過多的情況（應該正常工作，多餘參數被忽略）
        ValidationException exception = new ValidationException(
            ValidationException.ErrorCode.PASSWORD_IS_NOT_STRONG_ENOUGH, 
            "extra1", "extra2", "extra3"
        );
        
        assertEquals("密碼強度不足", exception.getMessage());
    }

    @Test
    @DisplayName("邊界測試 - 包含特殊字符的格式化參數")
    void testCreateException_specialCharactersInArguments() {
        ValidationException exception = new ValidationException(
            ValidationException.ErrorCode.USER_NOT_FOUND, 
            "user@domain.com<script>alert('xss')</script>"
        );
        
        assertTrue(exception.getMessage().contains("user@domain.com<script>alert('xss')</script>"));
    }

    @Test
    @DisplayName("邊界測試 - 超長字符串參數")
    void testCreateException_veryLongStringArgument() {
        String longString = "a".repeat(1000);
        ValidationException exception = new ValidationException(
            ValidationException.ErrorCode.USER_NOT_FOUND, longString
        );
        
        assertTrue(exception.getMessage().contains(longString));
    }

    @Test
    @DisplayName("邊界測試 - 驗證所有 ErrorCode 枚舉值")
    void testAllErrorCodes_haveValidProperties() {
        for (ValidationException.ErrorCode errorCode : ValidationException.ErrorCode.values()) {
            // 驗證錯誤碼不為負數
            assertTrue(errorCode.getCode() > 0, 
                "ErrorCode " + errorCode.name() + " 的錯誤碼應為正數");
            
            // 驗證 HTTP 狀態碼不為 null
            assertNotNull(errorCode.getHttpStatus(), 
                "ErrorCode " + errorCode.name() + " 的HTTP狀態碼不應為null");
            
            // 驗證錯誤訊息不為空
            assertNotNull(errorCode.getMessage(), 
                "ErrorCode " + errorCode.name() + " 的錯誤訊息不應為null");
            assertFalse(errorCode.getMessage().trim().isEmpty(), 
                "ErrorCode " + errorCode.name() + " 的錯誤訊息不應為空字符串");
        }
    }

    @Test
    @DisplayName("邊界測試 - 驗證錯誤碼的唯一性")
    void testErrorCodes_uniqueness() {
        ValidationException.ErrorCode[] allCodes = ValidationException.ErrorCode.values();
        
        for (int i = 0; i < allCodes.length; i++) {
            for (int j = i + 1; j < allCodes.length; j++) {
                assertNotEquals(allCodes[i].getCode(), allCodes[j].getCode(),
                    "發現重複的錯誤碼: " + allCodes[i].name() + " 和 " + allCodes[j].name() + 
                    " 都使用錯誤碼 " + allCodes[i].getCode());
            }
        }
    }

    @Test
    @DisplayName("邊界測試 - 測試 fromName 與實際枚舉名稱的一致性")
    void testFromName_consistencyWithEnumNames() {
        for (ValidationException.ErrorCode errorCode : ValidationException.ErrorCode.values()) {
            ValidationException.ErrorCode result = ValidationException.ErrorCode.fromName(errorCode.name());
            assertEquals(errorCode, result, 
                "fromName 方法應該能正確找到 " + errorCode.name());
        }
    }
}