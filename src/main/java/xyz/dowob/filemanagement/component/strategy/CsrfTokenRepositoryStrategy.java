package xyz.dowob.filemanagement.component.strategy;

import lombok.Getter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.annotation.CsrfRepositoryType;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.repostiory.ServerCsrfToken.CustomServerCsrfTokenRepository;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.util.List;

/**
 * CSRF 令牌儲存庫策略，實現可插拔的跨站請求偽造（CSRF）防禦機制。
 *
 * <p>本類別採用策略模式（Strategy Pattern）動態選擇 CSRF 令牌儲存庫實作，
 * 根據 {@link SecurityProperties} 中的設定，靈活切換不同的 CSRF 防護策略。</p>
 *
 * <p>主要特性：
 * <ul>
 *   <li>支援多種 CSRF 令牌儲存庫實作</li>
 *   <li>動態根據設定選擇適當的儲存庫</li>
 *   <li>當找不到對應的儲存庫時，拋出具體的 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 *
 * <p>設計目的：
 * 通過依賴注入和策略模式，實現 CSRF 防護機制的高度可設定性，
 * 使系統能夠輕鬆適應不同的安全需求和部署環境。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@Component
public class CsrfTokenRepositoryStrategy {
    /**
     * 選定的 CsrfTokenRepository，根據 {@link SecurityProperties} 中的設定
     */
    private final CustomServerCsrfTokenRepository csrfTokenRepository;

    /**
     * CsrfTokenRepositoryStrategy 的構造方法
     *
     * @param csrfTokenRepositories CsrfTokenRepository 的實現類
     * @param securityProperties    安全設定
     *
     * @throws IllegalArgumentException 當找不到對應的 CsrfTokenRepository 時拋出
     */
    public CsrfTokenRepositoryStrategy(List<CustomServerCsrfTokenRepository> csrfTokenRepositories, SecurityProperties securityProperties) {
        this.csrfTokenRepository = csrfTokenRepositories.stream().filter(csrfTokenRepository -> {
            CsrfRepositoryType type = AnnotatedElementUtils.findMergedAnnotation(csrfTokenRepository.getClass(), CsrfRepositoryType.class);
            LogUnity.trace("檢查 CsrfTokenRepository: %s, 註解: %s", csrfTokenRepository.getClass().getName(), type);
            if (type != null && type.value().equals(securityProperties.getCsrf().getCsrfTokenRepository())) {
                LogUnity.debug("註冊 CsrfTokenRepository: %s, 類型: %s", csrfTokenRepository.getClass().getName(), type.value());
                return true;
            }
            LogUnity.trace("CsrfTokenRepository 不匹配: %s, 類型: %s", csrfTokenRepository.getClass().getName(), type != null ? type.value() : "無");
            return false;
        }).findFirst().orElseThrow(() -> new IllegalArgumentException("找不到對應的 CsrfTokenRepository"));
    }


}
