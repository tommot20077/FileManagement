package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum;

import java.lang.annotation.*;

/**
 * CSRF 令牌儲存庫類型標記註解。
 * <p>
 * 此註解用於標記不同類型的 CSRF 令牌儲存庫實現，支援策略模式下的動態選擇機制。
 * 系統根據安全需求和部署環境選擇適當的 CSRF 令牌儲存策略，例如會話式儲存、Redis 分散式儲存等。
 * <p>
 * CSRF（跨站請求偽造）防護是 Web 安全的重要組成部分。本地會話儲存適用於單節點部署環境，
 * Redis 分散式儲存適用於多節點叢集環境，資料庫儲存適用於需要持久化的場景。
 * <p>
 * 使用範例：
 * <pre>{@code
 * @CsrfRepositoryType(CsrfTokenRepositoryEnum.REDIS)
 * @Component
 * public class RedisCsrfTokenRepository implements ServerCsrfTokenRepository {
 *     // Redis 基朮的 CSRF 令牌儲存實現
 * }
 * 
 * @CsrfRepositoryType(CsrfTokenRepositoryEnum.LOCAL)
 * @Component
 * public class LocalCsrfTokenRepository implements ServerCsrfTokenRepository {
 *     // 本地會話基朮的 CSRF 令牌儲存實現
 * }
 * }</pre>
 *
 * @see xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CsrfRepositoryType {
    /**
     * 指定 CSRF 令牌儲存庫的具體實現類型。
     * <p>
     * 此屬性定義了被標記類別所實現的 CSRF 令牌儲存策略類型，
     * 用於系統運行時的策略選擇和安全設定的動態切換。
     *
     * @return CSRF 令牌儲存庫的類型枚舉值
     * @see xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum
     */
    CsrfTokenRepositoryEnum value();
}
