package xyz.dowob.filemanagement.component.strategy;

import lombok.Getter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.annotation.CsrfRepositoryType;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.repostiory.ServerCsrfToken.CustomServerCsrfTokenRepository;

import java.util.List;

/**
 * CsrfTokenRepository 策略模式，當有多種 CsrfTokenRepository 時
 * 會根據 {@link SecurityProperties} 中的設定選擇對應的 CsrfTokenRepository
 * 並且將其注入到 {@link CsrfTokenRepositoryStrategy#csrfTokenRepository} 中
 * 當無法找到對應的 CsrfTokenRepository 時會拋出 IllegalArgumentException
 * 這樣就可以在其他地方直接使用 {@link CsrfTokenRepositoryStrategy#csrfTokenRepository} 來操作 CsrfTokenRepository
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CsrfTokenRepositoryStrategy
 * @create 2025/3/6
 * @Version 1.0
 **/
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
            return type != null && type.value().equals(securityProperties.getCsrf().getCsrfTokenRepository());
        }).findFirst().orElseThrow(() -> new IllegalArgumentException("找不到對應的 CsrfTokenRepository"));
    }
}
