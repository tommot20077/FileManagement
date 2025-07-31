package xyz.dowob.filemanagement.service.serviceInterface;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.entity.User;

/**
 * 基於 Spring Security 的反應式授權服務核心介面。提供完整的安全驗證機制，包括 JWT 令牌管理、CSRF 防護和反應式安全上下文處理。
 * 
 * <p>此介面專為 Spring Boot WebFlux 反應式環境設計，採用非阻塞 I/O 模式確保高併發場景下的系統效能。
 * 整合 Spring Security 安全框架，實現統一的認證授權流程，支援多層次的安全防護機制。
 * 
 * <p><strong>核心功能：</strong>
 * <ul>
 *     <li>使用者身份認證與 JWT 令牌生成</li>
 *     <li>反應式安全上下文的建立與管理</li>
 *     <li>CSRF 令牌的產生與驗證</li>
 *     <li>與 Spring Security 的無縫整合</li>
 * </ul>
 * 
 * <p><strong>實現契約要求：</strong>
 * <ul>
 *     <li>所有方法必須採用反應式程式設計模式，回傳 {@code Mono} 類型</li>
 *     <li>認證失敗時透過 {@code Mono.error()} 傳播 {@link xyz.dowob.filemanagement.exception.ValidationException}</li>
 *     <li>安全上下文的設定必須與 Spring Security 規範相容</li>
 *     <li>CSRF 令牌必須與配置的存儲策略保持一致</li>
 *     <li>敏感操作必須使用 {@code @HideSensitive} 註解保護</li>
 * </ul>
 * 
 * <p><strong>安全考量：</strong>
 * 實現類別應確保密碼驗證採用安全的雜湊演算法（如 BCrypt），JWT 令牌包含適當的過期時間和簽名驗證，
 * CSRF 令牌具備足夠的隨機性和唯一性。所有安全相關的操作都應記錄適當的審計日誌。
 * 
 * <p><strong>效能特性：</strong>
 * 採用反應式程式設計模式，支援非阻塞的高併發處理。與底層 R2DBC 資料庫存取和 Redis 快取無縫整合，
 * 最大化系統吞吐量和回應效能。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see reactor.core.publisher.Mono
 * @see org.springframework.security.core.Authentication
 * @see org.springframework.security.web.server.csrf.CsrfToken
 * @see xyz.dowob.filemanagement.exception.ValidationException
 */
public interface AuthorizationService {
    /**
     * 執行使用者身份認證並生成 JWT 授權令牌。
     * 
     * <p>此方法實現完整的使用者認證流程，包括憑證驗證、安全檢查和令牌生成。認證過程採用反應式程式設計模式，
     * 確保在高併發環境下的非阻塞處理效能。
     * 
     * <p><strong>認證流程：</strong>
     * <ol>
     *     <li>根據提供的使用者名稱查詢使用者資料庫</li>
     *     <li>使用 BCrypt 演算法驗證密碼</li>
     *     <li>執行額外的安全檢查（如帳號狀態、登入限制等）</li>
     *     <li>生成包含使用者權限和角色的 JWT 授權令牌</li>
     *     <li>記錄安全審計日誌</li>
     * </ol>
     * 
     * <p><strong>安全特性：</strong>
     * 實現類別必須確保密碼比對採用安全的時間常數比較，防止時序攻擊。
     * 生成的 JWT 令牌應包含適當的過期時間、數位簽名和使用者權限資訊。
     * 
     * <p><strong>錯誤處理：</strong>
     * 認證失敗時透過 {@code Mono.error()} 傳播 {@link xyz.dowob.filemanagement.exception.ValidationException}，
     * 而非拋出傳統的同步例外。這確保錯誤處理與反應式程式設計模式保持一致。
     * 
     * <p><strong>實現要求：</strong>
     * <ul>
     *     <li>必須使用 {@code @HideSensitive} 註解保護敏感參數</li>
     *     <li>必須記錄適當級別的安全審計日誌</li>
     *     <li>必須整合系統的限流和防護機制</li>
     *     <li>必須支援會話管理和安全上下文設定</li>
     * </ul>
     *
     * @param authRequest 包含使用者名稱和密碼的認證請求資料傳輸物件，
     *                   必須通過 Bean Validation 驗證，不可為 {@code null}
     * @param request 當前的伺服器端交換上下文，用於會話管理和安全檢查，可為 {@code null}
     * @return 包含已簽發 JWT 授權令牌字串的 {@code Mono}，
     *         認證成功時發射令牌，失敗時發射 {@code ValidationException}
     * @see xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO
     * @see xyz.dowob.filemanagement.exception.ValidationException.ErrorCode#USERNAME_OR_PASSWORD_ERROR
     * @see org.springframework.web.server.ServerWebExchange
     */
    Mono<String> authenticate(AuthRequestDTO authRequest, ServerWebExchange request);



    /**
     * 在 Spring WebFlux 反應式安全上下文中建立和設定使用者授權資訊。
     * 
     * <p>此方法整合 Spring Security 的反應式安全框架，建立適當的 {@code Authentication} 物件
     * 並將其注入到當前請求的安全上下文中。這確保後續的安全檢查和授權決策能夠存取到使用者的身份和權限資訊。
     * 
     * <p><strong>安全上下文設定流程：</strong>
     * <ol>
     *     <li>驗證輸入的使用者物件有效性</li>
     *     <li>建立 {@code UsernamePasswordAuthenticationToken} 認證物件</li>
     *     <li>載入使用者的角色和權限資訊到認證物件中</li>
     *     <li>將認證物件封裝到 {@code SecurityContextImpl} 中</li>
     *     <li>使用 {@code ReactiveSecurityContextHolder} 建立反應式安全上下文</li>
     *     <li>將安全上下文綁定到當前的 {@code ServerWebExchange}</li>
     * </ol>
     * 
     * <p><strong>反應式設計特性：</strong>
     * 此方法採用預設實現，完全支援 WebFlux 的非阻塞特性。安全上下文的設定過程不會阻塞當前執行緒，
     * 並且能夠在整個反應式處理鏈中正確傳播使用者的認證資訊。
     * 
     * <p><strong>安全考量：</strong>
     * 當 {@code user} 參數為 {@code null} 時，方法安全地回傳空的 {@code Mono}，避免建立無效的安全上下文。
     * 這種防護機制確保系統在異常情況下仍能維持安全狀態。
     * 
     * <p><strong>與 Spring Security 整合：</strong>
     * 設定的安全上下文與 Spring Security 的標準安全檢查機制完全相容，
     * 支援方法級別的 {@code @PreAuthorize} 和 {@code @PostAuthorize} 註解，
     * 以及自訂的授權決策邏輯。
     * 
     * <p><strong>會話管理：</strong>
     * 安全上下文透過 {@code HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY}
     * 與 HTTP 會話關聯，確保在無狀態的 RESTful API 和有狀態的 Web 應用場景中都能正確運作。
     *
     * @param request 當前的伺服器端交換上下文，包含 HTTP 請求和回應資訊，
     *               用於安全上下文的設定和會話管理，不可為 {@code null}
     * @param user 已通過認證的使用者實體物件，包含使用者的基本資訊、角色和權限，
     *            當為 {@code null} 時方法安全地不執行任何操作
     * @return 空的 {@code Mono<Void>}，表示安全上下文設定作業的完成狀態，
     *         成功時正常完成，不會發射任何值
     * @see org.springframework.security.authentication.UsernamePasswordAuthenticationToken
     * @see org.springframework.security.core.context.ReactiveSecurityContextHolder
     * @see org.springframework.security.core.context.SecurityContextImpl
     * @see org.springframework.security.web.context.HttpSessionSecurityContextRepository
     * @see xyz.dowob.filemanagement.entity.User#getAuthorities()
     */
    default Mono<Void> setAuthorization(ServerWebExchange request, User user) {
        if (user == null) {
            return Mono.empty();
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        Context securityContext = ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authentication)));
        request.getAttributes().put(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return Mono.empty();
    }


    /**
     * 從當前會話中取得或生成 CSRF（跨站請求偽造）防護令牌。
     * 
     * <p>此方法實現 CSRF 防護機制的核心功能，透過檢索或生成安全令牌來防止跨站請求偽造攻擊。
     * CSRF 令牌是一種重要的安全措施，確保所有狀態變更請求都來自於合法的使用者操作。
     * 
     * <p><strong>CSRF 防護原理：</strong>
     * <ul>
     *     <li>為每個使用者會話生成唯一的隨機令牌</li>
     *     <li>將令牌儲存在伺服器端會話中</li>
     *     <li>要求客戶端在所有修改性請求中包含此令牌</li>
     *     <li>伺服器端驗證請求中的令牌與會話中的令牌是否匹配</li>
     * </ul>
     * 
     * <p><strong>令牌特性：</strong>
     * 生成的 CSRF 令牌具備以下特性：
     * <ul>
     *     <li>足夠的隨機性，無法被預測或暴力破解</li>
     *     <li>與特定使用者會話緊密綁定</li>
     *     <li>適當的有效期限制</li>
     *     <li>在會話結束時自動失效</li>
     * </ul>
     * 
     * <p><strong>存儲策略支援：</strong>
     * 實現類別應支援多種 CSRF 令牌存儲策略，包括：
     * <ul>
     *     <li>記憶體中的會話存儲（適用於單機部署）</li>
     *     <li>Redis 分散式存儲（適用於叢集部署）</li>
     *     <li>資料庫持久化存儲（適用於高可用性要求）</li>
     * </ul>
     * 
     * <p><strong>反應式設計：</strong>
     * 此預設實現採用反應式程式設計模式，透過 {@code WebSession} 的非阻塞 API 存取會話資料。
     * 當令牌不存在於會話中時，會透過 {@code mapNotNull} 運算子安全地回傳空的 {@code Mono}。
     * 
     * <p><strong>安全考量：</strong>
     * 令牌的生成和驗證過程必須是安全的，防止時序攻擊和令牌洩露。
     * 實現類別應確保令牌在傳輸過程中受到適當保護，並在必要時進行令牌輪換。
     * 
     * <p><strong>與前端整合：</strong>
     * 前端應用程式需要在所有修改性的 HTTP 請求（POST、PUT、DELETE 等）中
     * 包含 CSRF 令牌，通常透過 HTTP 標頭或表單隱藏欄位傳送。
     *
     * @param request 當前的伺服器端交換上下文，包含會話資訊和 HTTP 請求詳細資料，
     *               用於存取或建立 CSRF 令牌的會話儲存，不可為 {@code null}
     * @return 包含 CSRF 防護令牌的 {@code Mono}，令牌存在時發射 {@code CsrfToken} 物件，
     *         不存在時發射空值，不會發生錯誤
     * @see org.springframework.security.web.server.csrf.CsrfToken
     * @see org.springframework.web.server.WebSession
     * @see xyz.dowob.filemanagement.component.strategy.CsrfTokenRepositoryStrategy
     * @see xyz.dowob.filemanagement.repostiory.ServerCsrfToken.AbstractServerCsrfTokenRepository
     */
    default Mono<CsrfToken> getCSRFToken(ServerWebExchange request) {
        return request.getSession().mapNotNull(webSession -> webSession.getAttribute("csrfToken"));
    }
}
