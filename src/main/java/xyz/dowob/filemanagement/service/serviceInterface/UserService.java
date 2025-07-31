package xyz.dowob.filemanagement.service.serviceInterface;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;

/**
 * 用戶業務邏輯服務介面，提供完整的用戶生命週期管理、身份認證與授權、密碼安全管理以及郵件服務等核心功能。
 * <p>
 * 此介面基於 Spring WebFlux 反應式程式設計架構，實現非阻塞的高併發用戶管理服務。繼承自 {@link xyz.dowob.filemanagement.service.serviceInterface.CrudService} 
 * 提供基礎 CRUD 操作，並擴展用戶特定的反應式業務邏輯，確保在大型分散式系統中的高效能表現。
 * <p>
 * <strong>核心功能範疇：</strong>
 * <ul>
 *   <li><strong>用戶生命週期管理：</strong>完整的用戶註冊、啟用、停用、刪除流程</li>
 *   <li><strong>身份認證與授權：</strong>基於 JWT 的無狀態認證機制，支援角色權限控制</li>
 *   <li><strong>密碼安全管理：</strong>密碼加密、重設、變更，遵循 OWASP 安全標準</li>
 *   <li><strong>會話管理：</strong>登入狀態追蹤、令牌失效、併發會話控制</li>
 *   <li><strong>郵件服務整合：</strong>密碼重設、帳號啟用、安全通知等郵件功能</li>
 *   <li><strong>安全性稽核：</strong>登入失敗記錄、異常行為監控、安全事件追蹤</li>
 * </ul>
 * <p>
 * <strong>反應式程式設計特性：</strong>
 * <ul>
 *   <li>所有方法回傳 {@link reactor.core.publisher.Mono} 類型，支援非阻塞異步操作</li>
 *   <li>使用 {@link reactor.core.publisher.Mono#error(Throwable)} 進行錯誤傳播</li>
 *   <li>支援背壓處理與流量控制，適用於高負載場景</li>
 *   <li>整合 Spring Security Reactive 安全框架</li>
 *   <li>使用 R2DBC 反應式資料庫存取，避免執行緒阻塞</li>
 * </ul>
 * <p>
 * <strong>安全性設計原則：</strong>
 * <ul>
 *   <li><strong>最小權限原則：</strong>用戶僅能存取被明確授權的資源</li>
 *   <li><strong>深度防禦：</strong>多層次安全驗證，包含輸入驗證、業務邏輯驗證、資料存取控制</li>
 *   <li><strong>敏感資料保護：</strong>密碼使用 BCrypt 雜湊，JWT 令牌包含最小必要資訊</li>
 *   <li><strong>會話安全：</strong>令牌時效控制、併發登入限制、異常登出處理</li>
 *   <li><strong>稽核追蹤：</strong>完整記錄用戶操作軌跡，支援安全事件調查</li>
 * </ul>
 * <p>
 * <strong>使用範例：</strong>
 * <pre>{@code
 * // 用戶註冊操作
 * RegisterDTO registerDTO = new RegisterDTO("username", "email@example.com", "password");
 * userService.register(registerDTO)
 *     .doOnSuccess(v -> log.info("用戶註冊成功"))
 *     .doOnError(error -> log.error("用戶註冊失敗", error))
 *     .subscribe();
 * 
 * // 用戶登入操作
 * AuthRequestDTO authDTO = new AuthRequestDTO("username", "password");
 * userService.login(authDTO, serverWebExchange)
 *     .doOnSuccess(token -> log.info("用戶登入成功，產生令牌"))
 *     .doOnError(error -> log.warn("登入嘗試失敗", error))
 *     .subscribe();
 * 
 * // 反應式鏈式操作範例
 * userService.getUser(exchange)
 *     .flatMap(user -> userService.changePassword(user))
 *     .then(userService.logout(userId, exchange))
 *     .subscribe();
 * }</pre>
 * <p>
 * <strong>實作須知：</strong>
 * <ol>
 *   <li>實作類別必須確保所有操作的原子性，避免資料不一致</li>
 *   <li>敏感操作需要適當的稽核日誌記錄</li>
 *   <li>錯誤處理應提供足夠的除錯資訊，但不洩露敏感資料</li>
 *   <li>快取策略需考慮資料一致性與效能平衡</li>
 *   <li>併發操作需要適當的同步機制</li>
 * </ol>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.service.serviceInterface.CrudService
 * @see reactor.core.publisher.Mono
 * @see org.springframework.security.core.userdetails.UserDetailsService
 * @see org.springframework.security.crypto.password.PasswordEncoder
 */
public interface UserService extends CrudService<User, Long> {

    /**
     * 處理用戶註冊流程，建立新的用戶帳號並初始化用戶基本設定。
     * <p>
     * 此方法執行完整的用戶註冊驗證與創建流程：
     * <ol>
     *   <li><strong>資料驗證：</strong>驗證用戶名、郵件地址格式，檢查必填欄位完整性</li>
     *   <li><strong>唯一性檢查：</strong>確保用戶名和郵件地址在系統中唯一，避免重複註冊</li>
     *   <li><strong>密碼安全處理：</strong>使用 BCrypt 演算法對密碼進行不可逆雜湊加密</li>
     *   <li><strong>帳號初始化：</strong>設定預設角色權限、建立用戶目錄結構、初始化偏好設定</li>
     *   <li><strong>持久化儲存：</strong>將用戶資料安全儲存至資料庫</li>
     *   <li><strong>後續處理：</strong>觸發歡迎郵件發送、記錄註冊事件、清理暫存資料</li>
     * </ol>
     * <p>
     * <strong>安全性考量：</strong>
     * <ul>
     *   <li>密碼強度驗證，要求符合最低安全標準</li>
     *   <li>防止惡意註冊攻擊，實施註冊頻率限制</li>
     *   <li>敏感資料保護，密碼不會被記錄或快取</li>
     *   <li>輸入資料清理，防止 SQL 注入與 XSS 攻擊</li>
     * </ul>
     * <p>
     * <strong>實作契約：</strong>
     * <ul>
     *   <li>實作類別必須確保註冊操作的原子性</li>
     *   <li>重複註冊嘗試應回傳明確的錯誤訊息</li>
     *   <li>註冊失敗時必須清理任何已創建的暫存資料</li>
     *   <li>必須記錄註冊嘗試的稽核日誌</li>
     * </ul>
     *
     * @param registerUserDTO 用戶註冊資訊傳輸對象，包含用戶名、郵件地址、密碼等必要註冊資訊
     * @return {@link Mono} 空的響應式單值流，註冊成功時完成操作並回傳 empty，失敗時透過 {@link Mono#error} 傳播詳細錯誤訊息
     * @throws org.springframework.dao.DuplicateKeyException 當用戶名或郵件地址已存在時
     * @see RegisterDTO
     * @see org.springframework.security.crypto.password.PasswordEncoder
     */
    Mono<Void> register(RegisterDTO registerUserDTO);

    /**
     * 處理用戶身份認證與登入授權，產生安全的 JWT 令牌作為後續請求的認證憑證。
     * <p>
     * 此方法執行完整的登入驗證與令牌生成流程：
     * <ol>
     *   <li><strong>憑證驗證：</strong>使用 BCrypt 驗證用戶提供的密碼與儲存的雜湊值</li>
     *   <li><strong>帳號狀態檢查：</strong>確認用戶帳號未被鎖定、停用或刪除</li>
     *   <li><strong>登入限制檢查：</strong>驗證是否觸發登入嘗試頻率限制或併發登入限制</li>
     *   <li><strong>權限載入：</strong>查詢用戶角色與權限資訊，準備授權資料</li>
     *   <li><strong>JWT 令牌生成：</strong>創建包含用戶身份與權限的安全令牌</li>
     *   <li><strong>會話建立：</strong>記錄登入時間、更新最後活動時間、建立安全上下文</li>
     *   <li><strong>稽核記錄：</strong>記錄成功登入事件，包含 IP 位址、用戶代理等資訊</li>
     * </ol>
     * <p>
     * <strong>JWT 令牌特性：</strong>
     * <ul>
     *   <li><strong>無狀態設計：</strong>令牌自包含用戶身份與權限資訊，無需伺服器端會話儲存</li>
     *   <li><strong>時效性控制：</strong>設定合理的過期時間，平衡安全性與使用體驗</li>
     *   <li><strong>數位簽章：</strong>使用密鑰簽署令牌，防止偽造與篡改</li>
     *   <li><strong>最小資訊原則：</strong>僅包含必要的身份識別與權限資訊</li>
     * </ul>
     * <p>
     * <strong>安全性防護：</strong>
     * <ul>
     *   <li><strong>暴力破解防護：</strong>實施登入嘗試次數限制與延遲機制</li>
     *   <li><strong>會話安全：</strong>檢測異常登入行為，支援強制登出機制</li>
     *   <li><strong>令牌安全：</strong>防止令牌洩露，支援令牌撤銷與黑名單機制</li>
     *   <li><strong>稽核追蹤：</strong>完整記錄登入嘗試與結果，支援安全分析</li>
     * </ul>
     * <p>
     * <strong>實作契約：</strong>
     * <ul>
     *   <li>登入失敗時必須記錄嘗試次數，實施適當的防護措施</li>
     *   <li>成功登入後應清除之前的失敗記錄</li>
     *   <li>令牌生成必須包含足夠的隨機性與唯一性</li>
     *   <li>敏感資料如密碼不得出現在日誌或回應中</li>
     * </ul>
     *
     * @param authRequestDTO 包含用戶名和密碼的登入資訊傳輸對象，用於身份驗證
     * @param request Web 請求交換對象，提供請求上下文資訊，包含 IP 位址、用戶代理等安全相關資料
     * @return {@link Mono} 包含 JWT 令牌字串的響應式單值流，認證成功時回傳有效令牌，失敗時透過 {@link Mono#error} 傳播認證錯誤
     * @throws xyz.dowob.filemanagement.exception.JwtAuthenticationException 當用戶名密碼錯誤、帳號被鎖定或其他認證失敗情況
     * @see AuthRequestDTO
     * @see ServerWebExchange
     * @see org.springframework.security.authentication.ReactiveAuthenticationManager
     */
    Mono<String> login(AuthRequestDTO authRequestDTO, ServerWebExchange request);

    /**
     * 處理用戶安全登出操作，完整清除身份認證資訊、會話資料和相關快取。
     * <p>
     * 此方法執行全面的登出清理流程：
     * <ol>
     *   <li><strong>令牌失效：</strong>將目前 JWT 令牌加入黑名單，防止令牌被重複使用</li>
     *   <li><strong>會話清理：</strong>清除伺服器端儲存的用戶會話資訊與狀態</li>
     *   <li><strong>安全上下文清除：</strong>移除 Spring Security 上下文中的認證資訊</li>
     *   <li><strong>快取清理：</strong>清除用戶相關的快取資料，包含權限快取、檔案列表快取等</li>
     *   <li><strong>暫存資料清理：</strong>刪除用戶上傳的暫存檔案、編輯鎖定等臨時資源</li>
     *   <li><strong>併發會話處理：</strong>通知其他活躍會話進行同步登出（可選）</li>
     *   <li><strong>稽核記錄：</strong>記錄登出事件，包含登出時間、IP 位址等資訊</li>
     * </ol>
     * <p>
     * <strong>安全性考量：</strong>
     * <ul>
     *   <li><strong>令牌撤銷：</strong>確保被撤銷的令牌無法用於後續請求</li>
     *   <li><strong>資料清理：</strong>徹底清除敏感的會話資料，防止資料洩露</li>
     *   <li><strong>審計軌跡：</strong>完整記錄登出操作，支援安全稽核</li>
     *   <li><strong>異常處理：</strong>即使部分清理操作失敗，也要確保核心安全功能完成</li>
     * </ul>
     * <p>
     * <strong>併發會話管理：</strong>
     * <ul>
     *   <li>支援單一用戶多重會話的獨立管理</li>
     *   <li>提供全域登出功能，可同時終止所有活躍會話</li>
     *   <li>處理會話衝突與競爭條件</li>
     * </ul>
     * <p>
     * <strong>實作契約：</strong>
     * <ul>
     *   <li>登出操作必須具有冪等性，多次呼叫不會產生副作用</li>
     *   <li>即使用戶已登出或令牌已失效，操作仍應正常完成</li>
     *   <li>清理操作失敗不應影響令牌失效的核心功能</li>
     *   <li>必須記錄登出操作的完整稽核資訊</li>
     * </ul>
     *
     * @param userId 要登出的用戶唯一識別碼，用於識別特定用戶會話
     * @param request Web 請求交換對象，提供當前請求上下文，包含令牌資訊、會話狀態等
     * @return {@link Mono} 空的響應式單值流，登出完成時回傳 empty，失敗時透過 {@link Mono#error} 傳播錯誤訊息
     * @see ServerWebExchange
     */
    Mono<Void> logout(Long userId, ServerWebExchange request);

    /**
     * 處理用戶密碼變更操作，安全更新用戶登入憑證並執行相關安全措施。
     * <p>
     * 此方法執行完整的密碼變更安全流程：
     * <ol>
     *   <li><strong>身份驗證：</strong>確認操作者身份，驗證是否為帳號擁有者或具備管理權限</li>
     *   <li><strong>密碼強度檢查：</strong>驗證新密碼是否符合系統安全策略要求</li>
     *   <li><strong>歷史密碼檢查：</strong>確保新密碼未在近期使用過，防止密碼重複使用</li>
     *   <li><strong>安全加密：</strong>使用 BCrypt 演算法對新密碼進行不可逆雜湊處理</li>
     *   <li><strong>資料更新：</strong>原子性更新用戶密碼與相關安全資訊</li>
     *   <li><strong>會話失效：</strong>強制登出所有現有會話，要求重新登入</li>
     *   <li><strong>快取清理：</strong>清除用戶相關的認證快取與權限快取</li>
     *   <li><strong>安全通知：</strong>發送密碼變更通知郵件，提醒帳號安全狀態</li>
     * </ol>
     * <p>
     * <strong>密碼安全策略：</strong>
     * <ul>
     *   <li><strong>複雜度要求：</strong>最少字元數、大小寫字母、數字、特殊字元組合</li>
     *   <li><strong>歷史檢查：</strong>防止使用最近 N 次使用過的密碼</li>
     *   <li><strong>常見密碼檢查：</strong>拒絕使用弱密碼或洩露密碼</li>
     *   <li><strong>個人資訊檢查：</strong>確保密碼不包含用戶名、郵件等個人資訊</li>
     * </ul>
     * <p>
     * <strong>安全性影響：</strong>
     * <ul>
     *   <li><strong>會話管理：</strong>密碼變更後立即失效所有活躍會話</li>
     *   <li><strong>令牌撤銷：</strong>撤銷所有已發行的 JWT 令牌</li>
     *   <li><strong>存取控制：</strong>重新驗證用戶權限與存取範圍</li>
     *   <li><strong>稽核記錄：</strong>記錄密碼變更事件與相關安全措施</li>
     * </ul>
     * <p>
     * <strong>實作契約：</strong>
     * <ul>
     *   <li>密碼變更操作必須具有原子性，確保資料一致性</li>
     *   <li>舊密碼必須從記憶體中安全清除</li>
     *   <li>變更失敗時不應洩露任何密碼相關資訊</li>
     *   <li>必須發送適當的安全通知</li>
     * </ul>
     *
     * @param user 要修改密碼的用戶實體對象，必須包含新的已驗證密碼資訊
     * @return {@link Mono} 包含更新後的用戶實體的響應式單值流，成功時回傳更新後的用戶（不含敏感資訊），失敗時透過 {@link Mono#error} 傳播錯誤訊息
     * @see org.springframework.security.crypto.password.PasswordEncoder
     */
    Mono<User> changePassword(User user);

    /**
     * 處理用戶郵件地址變更操作，安全更新用戶聯絡資訊並執行相關驗證流程。
     * <p>
     * 此方法執行完整的郵件地址變更驗證與更新流程：
     * <ol>
     *   <li><strong>身份驗證：</strong>確認操作者身份，驗證是否為帳號擁有者或具備管理權限</li>
     *   <li><strong>格式驗證：</strong>檢查新郵件地址格式是否符合標準規範</li>
     *   <li><strong>唯一性檢查：</strong>確保新郵件地址未被其他用戶使用，維持系統唯一性</li>
     *   <li><strong>驗證郵件發送：</strong>向新郵件地址發送確認連結，驗證郵件地址有效性</li>
     *   <li><strong>暫存狀態：</strong>將變更請求暫存，等待郵件確認完成</li>
     *   <li><strong>舊郵件通知：</strong>向原郵件地址發送變更通知，提醒安全狀態</li>
     *   <li><strong>稽核記錄：</strong>記錄郵件變更請求與確認過程</li>
     * </ol>
     * <p>
     * <strong>郵件驗證機制：</strong>
     * <ul>
     *   <li><strong>雙重確認：</strong>新舊郵件地址都會收到相關通知</li>
     *   <li><strong>時效性控制：</strong>驗證連結具有合理的過期時間</li>
     *   <li><strong>一次性使用：</strong>驗證連結僅能使用一次，防止重複確認</li>
     *   <li><strong>安全連結：</strong>使用加密令牌確保驗證連結安全性</li>
     * </ul>
     * <p>
     * <strong>安全性考量：</strong>
     * <ul>
     *   <li><strong>防止劫持：</strong>確保郵件變更操作不會被惡意利用</li>
     *   <li><strong>通知機制：</strong>任何郵件變更都會通知原郵件地址</li>
     *   <li><strong>回滾機制：</strong>支援在確認前取消郵件變更操作</li>
     *   <li><strong>稽核追蹤：</strong>完整記錄郵件變更過程與結果</li>
     * </ul>
     * <p>
     * <strong>變更流程狀態：</strong>
     * <ul>
     *   <li><strong>待確認：</strong>變更請求已提交，等待新郵件確認</li>
     *   <li><strong>已確認：</strong>新郵件已確認，郵件地址成功更新</li>
     *   <li><strong>已過期：</strong>確認連結已過期，需重新申請變更</li>
     *   <li><strong>已取消：</strong>用戶主動取消或系統自動取消變更</li>
     * </ul>
     * <p>
     * <strong>實作契約：</strong>
     * <ul>
     *   <li>郵件變更必須經過有效性驗證才能完成</li>
     *   <li>變更過程中原郵件地址保持有效，直到確認完成</li>
     *   <li>必須發送適當的通知郵件</li>
     *   <li>變更失敗時應提供明確的錯誤原因</li>
     * </ul>
     *
     * @param user 要修改郵件地址的用戶實體對象，必須包含新的郵件地址資訊
     * @return {@link Mono} 包含更新後的用戶實體的響應式單值流，成功時回傳更新後的用戶，失敗時透過 {@link Mono#error} 傳播錯誤訊息
     * @see xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider
     */
    Mono<User> changeEmail(User user);

    /**
     * 處理用戶忘記密碼請求，啟動安全的密碼重設流程並發送重設郵件。
     * <p>
     * 此方法執行完整的密碼重設初始化流程：
     * <ol>
     *   <li><strong>郵件地址驗證：</strong>確認提供的郵件地址是否存在於系統中</li>
     *   <li><strong>帳號狀態檢查：</strong>驗證對應帳號未被鎖定、停用或刪除</li>
     *   <li><strong>頻率限制檢查：</strong>防止密碼重設請求被濫用，實施合理的請求頻率限制</li>
     *   <li><strong>安全令牌生成：</strong>產生具有強隨機性與時效性的密碼重設令牌</li>
     *   <li><strong>令牌持久化：</strong>安全儲存重設令牌與相關元資料</li>
     *   <li><strong>重設郵件構建：</strong>建立包含安全重設連結的專業郵件內容</li>
     *   <li><strong>郵件發送：</strong>透過郵件服務提供者發送重設郵件</li>
     *   <li><strong>稽核記錄：</strong>記錄密碼重設請求事件與相關安全資訊</li>
     * </ol>
     * <p>
     * <strong>重設令牌特性：</strong>
     * <ul>
     *   <li><strong>高度隨機性：</strong>使用密碼學安全的隨機數生成器</li>
     *   <li><strong>時效性控制：</strong>設定合理的過期時間，平衡安全性與使用便利性</li>
     *   <li><strong>一次性使用：</strong>令牌使用後立即失效，防止重複使用</li>
     *   <li><strong>關聯性驗證：</strong>令牌與特定用戶帳號綁定，無法跨帳號使用</li>
     * </ul>
     * <p>
     * <strong>安全性防護：</strong>
     * <ul>
     *   <li><strong>資訊洩露防護：</strong>不論郵件地址是否存在，都回傳成功訊息</li>
     *   <li><strong>頻率限制：</strong>防止暴力攻擊與郵件轟炸</li>
     *   <li><strong>令牌保護：</strong>重設連結包含足夠的熵值，難以猜測</li>
     *   <li><strong>過期機制：</strong>自動清理過期的重設令牌</li>
     * </ul>
     * <p>
     * <strong>郵件內容規範：</strong>
     * <ul>
     *   <li><strong>清晰指示：</strong>明確說明重設操作步驟與注意事項</li>
     *   <li><strong>安全提醒：</strong>提醒用戶保護重設連結，勿轉發他人</li>
     *   <li><strong>時效說明：</strong>明確說明連結的有效期限</li>
     *   <li><strong>聯絡資訊：</strong>提供客服聯絡方式，處理異常情況</li>
     * </ul>
     * <p>
     * <strong>實作契約：</strong>
     * <ul>
     *   <li>無論郵件地址是否存在，都必須回傳成功響應以防止郵件地址枚舉攻擊</li>
     *   <li>重設令牌必須具有足夠的安全強度</li>
     *   <li>必須實施適當的頻率限制機制</li>
     *   <li>郵件發送失敗時應有適當的重試機制</li>
     * </ul>
     *
     * @param userEmailDTO 包含用戶郵件地址的資料傳輸對象，用於識別目標用戶帳號
     * @return {@link Mono} 空的響應式單值流，操作完成時回傳 empty（無論郵件地址是否存在），失敗時透過 {@link Mono#error} 傳播錯誤訊息
     * @see UserEmailDTO
     * @see xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider
     * @see xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider
     */
    Mono<Void> sendResetPasswordMail(UserEmailDTO userEmailDTO);

    /**
     * 執行密碼重設操作，使用有效的重設令牌安全更新用戶密碼。
     * <p>
     * 此方法執行完整的密碼重設驗證與更新流程：
     * <ol>
     *   <li><strong>令牌驗證：</strong>檢查重設令牌的格式、簽章與完整性</li>
     *   <li><strong>時效性檢查：</strong>確認令牌未過期且在有效期內</li>
     *   <li><strong>使用狀態檢查：</strong>驗證令牌未被使用過，防止重複使用</li>
     *   <li><strong>用戶關聯驗證：</strong>確認令牌與對應用戶帳號的關聯性</li>
     *   <li><strong>帳號狀態檢查：</strong>驗證目標帳號未被鎖定、停用或刪除</li>
     *   <li><strong>新密碼驗證：</strong>檢查新密碼是否符合安全策略要求</li>
     *   <li><strong>密碼加密：</strong>使用 BCrypt 演算法對新密碼進行安全雜湊</li>
     *   <li><strong>原子性更新：</strong>同時更新密碼與失效令牌，確保資料一致性</li>
     *   <li><strong>會話清理：</strong>強制登出所有現有會話，要求重新登入</li>
     *   <li><strong>通知發送：</strong>發送密碼重設成功通知郵件</li>
     *   <li><strong>稽核記錄：</strong>記錄密碼重設成功事件與相關安全資訊</li>
     * </ol>
     * <p>
     * <strong>令牌安全驗證：</strong>
     * <ul>
     *   <li><strong>數位簽章：</strong>驗證令牌的數位簽章，確保未被篡改</li>
     *   <li><strong>時間窗口：</strong>嚴格檢查令牌的發行時間與過期時間</li>
     *   <li><strong>用戶綁定：</strong>確保令牌僅能用於指定的用戶帳號</li>
     *   <li><strong>一次性保障：</strong>令牌使用後立即標記為已使用並失效</li>
     * </ul>
     * <p>
     * <strong>密碼安全處理：</strong>
     * <ul>
     *   <li><strong>強度驗證：</strong>新密碼必須符合系統安全策略</li>
     *   <li><strong>歷史檢查：</strong>防止使用最近使用過的密碼</li>
     *   <li><strong>安全加密：</strong>使用高強度 BCrypt 雜湊演算法</li>
     *   <li><strong>記憶體清理：</strong>處理完成後安全清除明文密碼</li>
     * </ul>
     * <p>
     * <strong>安全性影響：</strong>
     * <ul>
     *   <li><strong>全域登出：</strong>密碼重設後自動登出所有活躍會話</li>
     *   <li><strong>令牌撤銷：</strong>撤銷所有已發行的 JWT 令牌</li>
     *   <li><strong>安全通知：</strong>向用戶郵件發送密碼變更確認通知</li>
     *   <li><strong>稽核記錄：</strong>記錄重設操作的完整稽核軌跡</li>
     * </ul>
     * <p>
     * <strong>實作契約：</strong>
     * <ul>
     *   <li>令牌驗證失敗時必須提供明確的錯誤訊息</li>
     *   <li>密碼重設操作必須具有原子性</li>
     *   <li>操作失敗時不應洩露敏感資訊</li>
     *   <li>必須記錄操作結果的完整稽核日誌</li>
     * </ul>
     *
     * @param resetPasswordDTO 包含重設令牌和新密碼的資料傳輸對象，用於完成密碼重設操作
     * @return {@link Mono} 空的響應式單值流，重設成功時回傳 empty，失敗時透過 {@link Mono#error} 傳播詳細錯誤訊息
     * @see ResetPasswordDTO
     * @see xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider
     * @see org.springframework.security.crypto.password.PasswordEncoder
     */
    Mono<Void> resetPassword(ResetPasswordDTO resetPasswordDTO);

    /**
     * 從 Web 請求安全上下文中檢索當前已認證的用戶資訊。
     * <p>
     * 此方法執行安全的用戶身份檢索流程：
     * <ol>
     *   <li><strong>安全上下文檢索：</strong>從 Spring Security 上下文中取得認證資訊</li>
     *   <li><strong>認證狀態驗證：</strong>確認用戶已完成身份認證且會話有效</li>
     *   <li><strong>令牌有效性檢查：</strong>驗證 JWT 令牌未過期且未被撤銷</li>
     *   <li><strong>用戶實體載入：</strong>根據認證資訊載入完整的用戶實體資料</li>
     *   <li><strong>權限驗證：</strong>確認用戶帳號狀態正常，未被鎖定或停用</li>
     *   <li><strong>快取整合：</strong>優先從快取中取得用戶資訊，提升效能</li>
     * </ol>
     * <p>
     * <strong>安全上下文處理：</strong>
     * <ul>
     *   <li><strong>認證檢查：</strong>驗證 {@link org.springframework.security.core.Authentication} 對象的有效性</li>
     *   <li><strong>主體識別：</strong>從認證主體中提取用戶身份識別資訊</li>
     *   <li><strong>權限載入：</strong>載入用戶的角色與權限資訊</li>
     *   <li><strong>會話驗證：</strong>確認當前會話的有效性與活躍狀態</li>
     * </ul>
     * <p>
     * <strong>效能最佳化：</strong>
     * <ul>
     *   <li><strong>快取策略：</strong>利用用戶快取減少資料庫查詢</li>
     *   <li><strong>懒載入：</strong>僅載入必要的用戶資訊欄位</li>
     *   <li><strong>非阻塞操作：</strong>使用反應式資料庫驅動程式</li>
     *   <li><strong>連接池最佳化：</strong>有效利用資料庫連接資源</li>
     * </ul>
     * <p>
     * <strong>錯誤處理場景：</strong>
     * <ul>
     *   <li><strong>未認證狀態：</strong>用戶未登入或令牌已過期時回傳空值</li>
     *   <li><strong>帳號異常：</strong>用戶帳號被鎖定、停用或刪除時回傳空值</li>
     *   <li><strong>權限不足：</strong>用戶缺乏必要權限時進行適當處理</li>
     *   <li><strong>系統錯誤：</strong>資料庫或快取服務異常時的容錯處理</li>
     * </ul>
     * <p>
     * <strong>實作契約：</strong>
     * <ul>
     *   <li>未認證用戶必須回傳空的 Mono，不應拋出異常</li>
     *   <li>用戶實體不應包含敏感資訊如密碼雜湊</li>
     *   <li>必須驗證用戶帳號的有效性與活躍狀態</li>
     *   <li>應充分利用快取機制提升效能</li>
     * </ul>
     *
     * @param exchange Web 請求交換對象，包含 HTTP 請求、響應與 Spring Security 安全上下文
     * @return {@link Mono} 包含當前認證用戶實體的響應式單值流，認證有效時回傳用戶對象，未認證或認證無效時回傳空值
     * @see ServerWebExchange
     * @see org.springframework.security.core.context.SecurityContext
     * @see org.springframework.security.core.Authentication
     */
    Mono<User> getUser(ServerWebExchange exchange);

}