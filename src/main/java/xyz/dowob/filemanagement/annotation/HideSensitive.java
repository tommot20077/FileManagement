package xyz.dowob.filemanagement.annotation;

import java.lang.annotation.*;

/**
 * 敏感資訊隱藏標記註解。
 * <p>
 * 此註解是系統安全防護的重要組件，用於標記包含敏感資訊的字段、方法或類別，確保在日誌記錄、
 * 調試輸出或 AOP 攔截處理時不會洩露機密資料。被標記的內容在日誌中會被隱藏或使用遮罩替代。
 * <p>
 * 根據 CLAUDE.md 專案規範，任何可能輸出或互動敏感資料的方法都必須使用此註解進行標記。
 * 這是一個強制性的安全要求，用於防止敏感資訊透過日誌、錯誤訊息或調試資訊洩露。
 * <p>
 * 敏感資訊類型包括 JWT 令牌和各種認證憑證、資料庫連接字串和密鑰、使用者密碼和敏感個人資訊、
 * API 金鑰和第三方服務認證資訊、加密金鑰和安全設定參數，以及內部系統架構和設定細節。
 * <p>
 * 使用範例：
 * <pre>{@code
 * @Service
 * public class AuthService {
 *     @HideSensitive
 *     private String jwtSecret;
 *     
 *     @HideSensitive
 *     public String generateToken(User user) {
 *         // 生成 JWT 令牌的敏感操作
 *         return jwtTokenProvider.createToken(user);
 *     }
 *     
 *     @HideSensitive
 *     public boolean validateCredentials(String username, String password) {
 *         // 驗證使用者憑證，涉及敏感資料比對
 *         return passwordEncoder.matches(password, user.getPassword());
 *     }
 * }
 * 
 * @HideSensitive
 * @Configuration
 * public class SecurityConfig {
 *     // 整個安全設定類別都包含敏感資訊
 * }
 * }</pre>
 * <p>
 * 重要安全提醒：此註解是安全防護的第一道防線，但不能替代其他安全措施。
 * 開發者有責任識別和標記所有潛在的敏感資訊，定期審查和更新敏感資訊的標記範圍，
 * 並在生產環境中建議關閉詳細的調試日誌。
 *
 * @since 1.0
 * @author yuan
 * @version 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface HideSensitive {
}