package xyz.dowob.filemanagement.config.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityProperties 安全屬性配置測試類別
 * 
 * <p>全面測試 {@link xyz.dowob.filemanagement.config.properties.SecurityProperties} 安全配置屬性的各種功能和行為。</p>
 * 
 * <p>測試範圍包括：
 * 
 *   - JWT 驗證令牌配置
 *   - 重置密碼令牌配置
 *   - Cookie 安全配置
 *   - 跨域資源共享（CORS）配置
 *   - 跨站請求偽造（CSRF）驗證配置
 *   - HTTP 嚴格安全傳輸（HSTS）配置
 *   - 訪客用戶安全設置
 *   - 登入限制配置
 *   - 路徑安全規則配置
 * 
 * </p>
 * 
 * <p>測試前置條件：
 * 
 *   - SecurityProperties 能正常實例化
 *   - 內嵌子配置類別能正常作用
 *   - 所有屬性值都具有有效預設值
 * 
 * </p>
 * 
 * <p>測試策略：
 * 
 *   - 設置不同的配置參數與預設值
 *   - 測試各元件的預設值和實際設置
 *   - 處理各種邊界情況和異常模式
 * 
 * </p>
 * 
 * <p>預期測試結果：
 * 
 *   - 所有安全配置參數正確且可配置
 *   - JWT、CSRF、重置密碼等令牌具有適當預設值
 *   - 路徑安全規則一致且可擴展
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SecurityProperties 安全屬性配置測試")
class SecurityPropertiesTest {

    private SecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - SecurityProperties 初始化")
    void testSecurityPropertiesInitialization() {
        assertNotNull(securityProperties);
        assertNotNull(securityProperties.getJwtToken());
        assertNotNull(securityProperties.getResetPasswordToken());
        assertNotNull(securityProperties.getCookie());
        assertNotNull(securityProperties.getCors());
        assertNotNull(securityProperties.getCsrf());
        assertNotNull(securityProperties.getHsts());
        assertNotNull(securityProperties.getGuestUser());
        assertNotNull(securityProperties.getLogin());
        assertNotNull(securityProperties.getPaths());
    }

    @Test
    @DisplayName("一般測試 - JwtToken 配置")
    void testJwtTokenConfiguration() {
        SecurityProperties.JwtToken jwtToken = securityProperties.getJwtToken();
        
        // 驗證默認值
        assertNull(jwtToken.getSecret()); // 預設為 null，需要在配置中設定
        assertEquals(Duration.ofDays(1), jwtToken.getExpiration());
        assertEquals("jwt.", jwtToken.getWebSocketTokenPrefix());
        
        // 測試設定值
        jwtToken.setSecret("test-secret-key");
        jwtToken.setExpiration(Duration.ofHours(2));
        jwtToken.setWebSocketTokenPrefix("token.");
        
        assertEquals("test-secret-key", jwtToken.getSecret());
        assertEquals(Duration.ofHours(2), jwtToken.getExpiration());
        assertEquals("token.", jwtToken.getWebSocketTokenPrefix());
    }

    @Test
    @DisplayName("一般測試 - ResetPasswordToken 配置")
    void testResetPasswordTokenConfiguration() {
        SecurityProperties.resetPasswordToken resetToken = securityProperties.getResetPasswordToken();
        
        // 驗證默認值
        assertEquals(6, resetToken.getLength());
        assertEquals(Duration.ofMinutes(30), resetToken.getExpiration());
        
        // 測試設定值
        resetToken.setLength(8);
        resetToken.setExpiration(Duration.ofMinutes(60));
        
        assertEquals(8, resetToken.getLength());
        assertEquals(Duration.ofMinutes(60), resetToken.getExpiration());
    }

    @Test
    @DisplayName("一般測試 - Cookie 配置")
    void testCookieConfiguration() {
        SecurityProperties.Cookie cookie = securityProperties.getCookie();
        
        // 驗證默認值
        assertTrue(cookie.isSecure());
        assertTrue(cookie.isHttpOnly());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals("jwtToken", cookie.getTokenName());
        
        // 測試設定值
        cookie.setSecure(false);
        cookie.setHttpOnly(false);
        cookie.setSameSite("Strict");
        cookie.setTokenName("customToken");
        
        assertFalse(cookie.isSecure());
        assertFalse(cookie.isHttpOnly());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("customToken", cookie.getTokenName());
    }

    @Test
    @DisplayName("一般測試 - CORS 配置")
    void testCorsConfiguration() {
        SecurityProperties.Cors cors = securityProperties.getCors();
        
        // 驗證默認值
        assertEquals(List.of(), cors.getAllowedOrigins());
        assertEquals(List.of(), cors.getAllowedOriginsPattern());
        assertEquals(List.of(), cors.getAllowExposedHeaders());
        assertEquals(List.of("*"), cors.getAllowedMethods());
        assertEquals(List.of("*"), cors.getAllowedHeaders());
        assertEquals(Duration.ofHours(1), cors.getMaxAge());
        assertTrue(cors.isAllowCredentials());
        
        // 測試設定值
        cors.setAllowedOrigins(List.of("http://localhost:3000"));
        cors.setAllowedOriginsPattern(List.of("http://localhost:*"));
        cors.setAllowExposedHeaders(List.of("X-Custom-Header"));
        cors.setAllowedMethods(List.of("GET", "POST"));
        cors.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        cors.setMaxAge(Duration.ofHours(2));
        cors.setAllowCredentials(false);
        
        assertEquals(List.of("http://localhost:3000"), cors.getAllowedOrigins());
        assertEquals(List.of("http://localhost:*"), cors.getAllowedOriginsPattern());
        assertEquals(List.of("X-Custom-Header"), cors.getAllowExposedHeaders());
        assertEquals(List.of("GET", "POST"), cors.getAllowedMethods());
        assertEquals(List.of("Content-Type", "Authorization"), cors.getAllowedHeaders());
        assertEquals(Duration.ofHours(2), cors.getMaxAge());
        assertFalse(cors.isAllowCredentials());
    }

    @Test
    @DisplayName("一般測試 - CSRF 配置")
    void testCsrfConfiguration() {
        SecurityProperties.Csrf csrf = securityProperties.getCsrf();
        
        // 驗證默認值
        assertEquals("X-Csrf-Token", csrf.getHeaderName());
        assertEquals("_csrf", csrf.getParameterName());
        assertEquals(Duration.ofMinutes(5), csrf.getExpiration());
        assertEquals(CsrfTokenRepositoryEnum.REDIS, csrf.getCsrfTokenRepository());
        assertEquals("^https?://.*$", csrf.getAllowRefererPatten());
        
        // 測試設定值
        csrf.setHeaderName("X-CSRF-TOKEN");
        csrf.setParameterName("csrf_token");
        csrf.setExpiration(Duration.ofMinutes(10));
        csrf.setCsrfTokenRepository(CsrfTokenRepositoryEnum.LOCAL);
        csrf.setAllowRefererPatten("^https://example\\.com$");
        
        assertEquals("X-CSRF-TOKEN", csrf.getHeaderName());
        assertEquals("csrf_token", csrf.getParameterName());
        assertEquals(Duration.ofMinutes(10), csrf.getExpiration());
        assertEquals(CsrfTokenRepositoryEnum.LOCAL, csrf.getCsrfTokenRepository());
        assertEquals("^https://example\\.com$", csrf.getAllowRefererPatten());
    }

    @Test
    @DisplayName("一般測試 - HSTS 配置")
    void testHstsConfiguration() {
        SecurityProperties.Hsts hsts = securityProperties.getHsts();
        
        // 驗證默認值
        assertEquals(Duration.ofDays(365), hsts.getMaxAge());
        assertTrue(hsts.isIncludeSubDomains());
        assertTrue(hsts.isPreload());
        
        // 測試設定值
        hsts.setMaxAge(Duration.ofDays(180));
        hsts.setIncludeSubDomains(false);
        hsts.setPreload(false);
        
        assertEquals(Duration.ofDays(180), hsts.getMaxAge());
        assertFalse(hsts.isIncludeSubDomains());
        assertFalse(hsts.isPreload());
    }

    @Test
    @DisplayName("一般測試 - GuestUser 配置")
    void testGuestUserConfiguration() {
        SecurityProperties.GuestUser guestUser = securityProperties.getGuestUser();
        
        // 驗證默認值
        assertTrue(guestUser.isEnable());
        
        // 測試設定值
        guestUser.setEnable(false);
        assertFalse(guestUser.isEnable());
    }

    @Test
    @DisplayName("一般測試 - Login 配置")
    void testLoginConfiguration() {
        SecurityProperties.Login login = securityProperties.getLogin();
        
        // 驗證默認值
        assertEquals(5, login.getMaxFailure());
        assertEquals(Duration.ofMinutes(30), login.getLockTime());
        
        // 測試設定值
        login.setMaxFailure(3);
        login.setLockTime(Duration.ofMinutes(60));
        
        assertEquals(3, login.getMaxFailure());
        assertEquals(Duration.ofMinutes(60), login.getLockTime());
    }

    @Test
    @DisplayName("一般測試 - Paths 默認路徑規則")
    void testPathsDefaultRules() {
        SecurityProperties.Paths paths = securityProperties.getPaths();
        
        Map<String, SecurityProperties.Paths.PathRuleConfig> defaultRules = paths.getDefaultRules();
        assertNotNull(defaultRules);
        assertFalse(defaultRules.isEmpty());
        
        // 驗證一些關鍵的默認路徑規則
        assertTrue(defaultRules.containsKey("api-guest"));
        assertTrue(defaultRules.containsKey("web-guest"));
        assertTrue(defaultRules.containsKey("websocket"));
        assertTrue(defaultRules.containsKey("actuator-health"));
        
        SecurityProperties.Paths.PathRuleConfig apiGuestRule = defaultRules.get("api-guest");
        assertEquals("/api/v1/guest/**", apiGuestRule.getPattern());
        assertEquals(RoleEnum.ANONYMOUS, apiGuestRule.getRole());
        assertNull(apiGuestRule.getMethod());
    }

    @Test
    @DisplayName("一般測試 - Paths 有效規則獲取")
    void testPathsEffectiveRules() {
        SecurityProperties.Paths paths = securityProperties.getPaths();
        
        Collection<SecurityProperties.Paths.PathRuleConfig> effectiveRules = paths.getEffectiveRules();
        assertNotNull(effectiveRules);
        assertFalse(effectiveRules.isEmpty());
        
        // 驗證有效規則包含默認規則
        long anonymousRules = effectiveRules.stream()
                .filter(rule -> RoleEnum.ANONYMOUS.equals(rule.getRole()))
                .count();
        assertTrue(anonymousRules > 0);
    }

    @Test
    @DisplayName("一般測試 - PathRuleConfig 構造和設定")
    void testPathRuleConfigConstruction() {
        // 測試默認構造函數
        SecurityProperties.Paths.PathRuleConfig rule1 = new SecurityProperties.Paths.PathRuleConfig();
        assertNull(rule1.getPattern());
        assertNull(rule1.getMethod());
        assertNull(rule1.getRole());
        
        // 測試帶參數的構造函數
        SecurityProperties.Paths.PathRuleConfig rule2 = new SecurityProperties.Paths.PathRuleConfig(
            "/test/**", HttpMethod.GET, RoleEnum.USER
        );
        assertEquals("/test/**", rule2.getPattern());
        assertEquals(HttpMethod.GET, rule2.getMethod());
        assertEquals(RoleEnum.USER, rule2.getRole());
        
        // 測試設定方法
        rule1.setPattern("/admin/**");
        rule1.setMethod(HttpMethod.POST);
        rule1.setRole(RoleEnum.ADMIN);
        
        assertEquals("/admin/**", rule1.getPattern());
        assertEquals(HttpMethod.POST, rule1.getMethod());
        assertEquals(RoleEnum.ADMIN, rule1.getRole());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - init 方法 JWT 密鑰為空")
    void testInit_emptyJwtSecret() {
        securityProperties.getJwtToken().setSecret("");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            securityProperties.init();
        });
        
        assertTrue(exception.getMessage().contains("JWT 密鑰需要設定"));
    }

    @Test
    @DisplayName("異常測試 - init 方法 JWT 密鑰為 null")
    void testInit_nullJwtSecret() {
        securityProperties.getJwtToken().setSecret(null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            securityProperties.init();
        });
        
        assertTrue(exception.getMessage().contains("JWT 密鑰需要設定"));
    }

    @Test
    @DisplayName("異常測試 - init 方法 JWT 密鑰只有空格")
    void testInit_whitespaceJwtSecret() {
        securityProperties.getJwtToken().setSecret("   ");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            securityProperties.init();
        });
        
        assertTrue(exception.getMessage().contains("JWT 密鑰需要設定"));
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - init 方法正常 JWT 密鑰")
    void testInit_validJwtSecret() {
        securityProperties.getJwtToken().setSecret("valid-secret-key");
        
        assertDoesNotThrow(() -> {
            securityProperties.init();
        });
    }

    @Test
    @DisplayName("邊界測試 - Paths 自定義規則覆蓋")
    void testPathsCustomRuleOverride() {
        SecurityProperties.Paths paths = securityProperties.getPaths();
        
        // 添加自定義規則覆蓋默認規則
        SecurityProperties.Paths.PathRuleConfig customRule = new SecurityProperties.Paths.PathRuleConfig(
            "/api/v1/guest/custom/**", HttpMethod.POST, RoleEnum.USER
        );
        paths.getRules().put("api-guest", customRule);
        
        Collection<SecurityProperties.Paths.PathRuleConfig> effectiveRules = paths.getEffectiveRules();
        
        // 找到被覆蓋的規則
        SecurityProperties.Paths.PathRuleConfig overriddenRule = effectiveRules.stream()
                .filter(rule -> "/api/v1/guest/custom/**".equals(rule.getPattern()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(overriddenRule);
        assertEquals(HttpMethod.POST, overriddenRule.getMethod());
        assertEquals(RoleEnum.USER, overriddenRule.getRole());
    }

    @Test
    @DisplayName("邊界測試 - Paths 新增自定義規則")
    void testPathsAddCustomRule() {
        SecurityProperties.Paths paths = securityProperties.getPaths();
        
        // 添加不存在於默認規則中的自定義規則
        SecurityProperties.Paths.PathRuleConfig newRule = new SecurityProperties.Paths.PathRuleConfig(
            "/custom/api/**", HttpMethod.GET, RoleEnum.ADVANCED_USER
        );
        paths.getRules().put("custom-api", newRule);
        
        Collection<SecurityProperties.Paths.PathRuleConfig> effectiveRules = paths.getEffectiveRules();
        
        // 驗證新規則被添加
        boolean foundNewRule = effectiveRules.stream()
                .anyMatch(rule -> "/custom/api/**".equals(rule.getPattern()));
        
        assertTrue(foundNewRule);
    }

    @Test
    @DisplayName("邊界測試 - 極端時間配置")
    void testExtremeTimeConfigurations() {
        // JWT 令牌極短過期時間
        securityProperties.getJwtToken().setExpiration(Duration.ofSeconds(1));
        assertEquals(Duration.ofSeconds(1), securityProperties.getJwtToken().getExpiration());
        
        // JWT 令牌極長過期時間
        securityProperties.getJwtToken().setExpiration(Duration.ofDays(365));
        assertEquals(Duration.ofDays(365), securityProperties.getJwtToken().getExpiration());
        
        // 重置密碼令牌極短過期時間
        securityProperties.getResetPasswordToken().setExpiration(Duration.ofSeconds(30));
        assertEquals(Duration.ofSeconds(30), securityProperties.getResetPasswordToken().getExpiration());
        
        // CSRF 令牌極短過期時間
        securityProperties.getCsrf().setExpiration(Duration.ofSeconds(10));
        assertEquals(Duration.ofSeconds(10), securityProperties.getCsrf().getExpiration());
    }

    @Test
    @DisplayName("邊界測試 - 各種角色類型的路徑規則")
    void testPathRulesWithAllRoles() {
        SecurityProperties.Paths paths = securityProperties.getPaths();
        
        Collection<SecurityProperties.Paths.PathRuleConfig> effectiveRules = paths.getEffectiveRules();
        
        // 驗證包含所有角色類型的規則
        boolean hasAnonymous = effectiveRules.stream().anyMatch(rule -> RoleEnum.ANONYMOUS.equals(rule.getRole()));
        boolean hasVisitor = effectiveRules.stream().anyMatch(rule -> RoleEnum.VISITOR.equals(rule.getRole()));
        boolean hasUser = effectiveRules.stream().anyMatch(rule -> RoleEnum.USER.equals(rule.getRole()));
        boolean hasAdmin = effectiveRules.stream().anyMatch(rule -> RoleEnum.ADMIN.equals(rule.getRole()));
        
        assertTrue(hasAnonymous);
        assertTrue(hasVisitor);
        assertTrue(hasUser);
        assertTrue(hasAdmin);
    }

    @Test
    @DisplayName("邊界測試 - 空字串配置處理")
    void testEmptyStringConfigurations() {
        // 測試空字串配置不會導致異常
        securityProperties.getCookie().setTokenName("");
        securityProperties.getCookie().setSameSite("");
        securityProperties.getCsrf().setHeaderName("");
        securityProperties.getCsrf().setParameterName("");
        
        assertEquals("", securityProperties.getCookie().getTokenName());
        assertEquals("", securityProperties.getCookie().getSameSite());
        assertEquals("", securityProperties.getCsrf().getHeaderName());
        assertEquals("", securityProperties.getCsrf().getParameterName());
    }

    @Test
    @DisplayName("邊界測試 - 重置密碼令牌長度邊界")
    void testResetPasswordTokenLengthBoundaries() {
        SecurityProperties.resetPasswordToken resetToken = securityProperties.getResetPasswordToken();
        
        // 測試極小長度
        resetToken.setLength(1);
        assertEquals(1, resetToken.getLength());
        
        // 測試極大長度
        resetToken.setLength(100);
        assertEquals(100, resetToken.getLength());
        
        // 測試零長度
        resetToken.setLength(0);
        assertEquals(0, resetToken.getLength());
        
        // 測試負數長度
        resetToken.setLength(-1);
        assertEquals(-1, resetToken.getLength());
    }

    @Test
    @DisplayName("邊界測試 - Paths 有效規則緩存機制")
    void testPathsEffectiveRulesCaching() {
        SecurityProperties.Paths paths = securityProperties.getPaths();
        
        // 第一次調用
        Collection<SecurityProperties.Paths.PathRuleConfig> rules1 = paths.getEffectiveRules();
        
        // 第二次調用，應該返回緩存的結果
        Collection<SecurityProperties.Paths.PathRuleConfig> rules2 = paths.getEffectiveRules();
        
        // 驗證返回的是同一個集合實例（緩存機制）
        assertSame(rules1, rules2);
    }
}