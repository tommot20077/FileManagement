package xyz.dowob.filemanagement.component.provider.providerImplement;

import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.config.properties.MailProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.OAuthProviderEnum;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.unity.CacheConcurrentHashMap;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基於 Spring Boot 的電子郵件發送提供者實現。
 *
 * <p>此類別實現了 {@link xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider} 介面，
 * 提供非同步且安全的電子郵件發送服務。通過 JavaMailSender 實現郵件傳送，支援 UTF-8 編碼，
 * 並提供基本的異常處理機制。</p>
 *
 * <p>特性：
 * <ul>
 *   <li>基於 Spring Boot JavaMailSender 實現</li>
 *   <li>支援非同步郵件發送（反應式編程）</li>
 *   <li>自動設定寄件人信箱</li>
 *   <li>安全性：使用 @HideSensitive 標記，防止敏感資訊洩露</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@ConditionalOnProperty(name = {"spring.mail.username", "spring.mail.password", "global.email.mail-sender"})
public class EmailProviderImpl implements xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider {
    /**
     * Spring Boot 提供的 JavaMailSender，用於非同步且安全地傳送電子郵件。
     *
     * @see org.springframework.mail.javamail.JavaMailSender
     */
    private final JavaMailSender javaMailSender;

    /**
     * 郵件服務配置屬性，包含 OAuth 相關設定。
     *
     * @see xyz.dowob.filemanagement.config.properties.MailProperties
     */
    private final MailProperties customMailProperties;


    /**
     * Spring Boot 自動配置的郵件屬性，包含 SMTP 伺服器設定。
     *
     * @see org.springframework.boot.autoconfigure.mail.MailProperties
     */
    private final org.springframework.boot.autoconfigure.mail.MailProperties springMailProperties;

    /**
     * WebClient 實例，用於執行 OAuth token 請求。
     */
    private final WebClient webClient = WebClient.builder().build();

    /**
     * OAuth token 快取，避免頻繁請求新的 token。
     * 使用 CacheConcurrentHashMap 自動管理過期時間。
     */
    private final CacheConcurrentHashMap<String, String> tokenCache;


    /**
     * 建構函式，注入必要的依賴。
     *
     * @param javaMailSender       Spring Mail 的 JavaMailSender 實例
     * @param customMailProperties 郵件服務配置屬性
     */
    public EmailProviderImpl(JavaMailSender javaMailSender, MailProperties customMailProperties, org.springframework.boot.autoconfigure.mail.MailProperties springMailProperties) {
        this.javaMailSender = javaMailSender;
        this.customMailProperties = customMailProperties;
        this.springMailProperties = springMailProperties;

        this.tokenCache = new CacheConcurrentHashMap<>(16, Duration.ofMinutes(55), Duration.ofHours(1), Duration.ofMinutes(10), true);
        this.tokenCache.setTag("OAuth-Token-Cache");
    }


    /**
     * 非同步發送電子郵件，支援 UTF-8 編碼。
     *
     * <p>此方法透過 Reactor 的 Mono 實現非阻塞郵件發送，並處理可能的發送異常。
     * 若發送失敗，將回傳 {@link xyz.dowob.filemanagement.exception.ProcessException}。</p>
     *
     * @param sendToEmail 收件人電子郵件地址
     * @param subject     郵件主旨
     * @param content     郵件內容（純文字格式）
     *
     * @return {@link reactor.core.publisher.Mono<Void>} 表示郵件發送作業的非同步結果
     */
    @Override
    @HideSensitive
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<Void> sendEmail(String sendToEmail, String subject, String content) {
        if (customMailProperties.getOauth().isEnabled()) {
            return getOAuthToken()
                    .flatMap(token -> sendEmailWithOAuth(sendToEmail, subject, content, token))
                    .doOnError(e -> LogUnity.error("OAuth 郵件發送失敗: %s", e.getMessage()))
                    .onErrorResume(e -> {
                        LogUnity.warn("OAuth 認證失敗，嘗試使用傳統 SMTP 認證");
                        return sendEmailWithSmtp(sendToEmail, subject, content);
                    });
        } else {
            return sendEmailWithSmtp(sendToEmail, subject, content);
        }
    }


    /**
     * 獲取 OAuth 存取令牌。
     *
     * @return 存取令牌
     */
    @HideSensitive
    private Mono<String> getOAuthToken() {
        OAuthProviderEnum provider = customMailProperties.getOauth().getProvider();
        String cacheKey = provider.getName();

        String cachedToken = tokenCache.check(cacheKey);
        if (cachedToken != null) {
            LogUnity.trace("使用快取的 OAuth token: %s", provider.getName());
            return Mono.just(cachedToken);
        }

        return fetchNewToken(provider).doOnNext(token -> {
            int cacheDuration = customMailProperties.getOauth().getTokenCacheDuration();
            Duration cacheDurationDuration = Duration.ofSeconds(cacheDuration);
            tokenCache.set(cacheKey, token, cacheDurationDuration);
            LogUnity.debug("快取新的 OAuth token: %s, 過期時間: %s 秒", provider.getName(), cacheDuration);
        });
    }


    /**
     * 使用 OAuth token 發送郵件。
     *
     * @param sendToEmail 收件人電子郵件地址
     * @param subject     郵件主旨
     * @param content     郵件內容
     * @param token       OAuth 存取令牌
     *
     * @return 發送結果
     */
    private Mono<Void> sendEmailWithOAuth(String sendToEmail, String subject, String content, String token) {
        return Mono.fromCallable(() -> {
            java.util.Properties props = new java.util.Properties();

            props.putAll(springMailProperties.getProperties());

            props.put("mail.smtp.host", springMailProperties.getHost());
            props.put("mail.smtp.port", String.valueOf(springMailProperties.getPort()));
            props.put("mail.smtp.auth", "true");

            props.put("mail.smtp.auth.mechanisms", "XOAUTH2");

            if (springMailProperties.getProperties().containsKey("mail.smtp.starttls.enable")) {
                props.put("mail.smtp.starttls.enable", springMailProperties.getProperties().get("mail.smtp.starttls.enable"));
            }
            if (springMailProperties.getProperties().containsKey("mail.smtp.starttls.required")) {
                props.put("mail.smtp.starttls.required", springMailProperties.getProperties().get("mail.smtp.starttls.required"));
            }

            jakarta.mail.Session session = jakarta.mail.Session.getInstance(props);

            try {
                MimeMessage mimeMessage = new MimeMessage(session);
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(customMailProperties.getMailSender().getMailSender(), "帳號安全管理組");
                helper.setTo(sendToEmail);
                helper.setSubject(subject);
                helper.setText(content, false);

                jakarta.mail.Transport transport = session.getTransport("smtp");
                transport.connect(springMailProperties.getHost(), springMailProperties.getPort(), springMailProperties.getUsername(), token);
                transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                transport.close();

                return null;
            } catch (Exception e) {
                throw new ProcessException(ProcessException.ErrorCode.SEND_MAIL_FAILED, e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }


    /**
     * 使用傳統 SMTP 認證發送郵件。
     *
     * @param sendToEmail 收件人電子郵件地址
     * @param subject     郵件主旨
     * @param content     郵件內容
     *
     * @return 發送結果
     */
    private Mono<Void> sendEmailWithSmtp(String sendToEmail, String subject, String content) {
        return Mono.defer(() -> {
            try {
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(customMailProperties.getMailSender().getMailSender(), "帳號安全管理組");
                helper.setTo(sendToEmail);
                helper.setSubject(subject);
                helper.setText(content, false);

                javaMailSender.send(mimeMessage);
                return Mono.empty();
            } catch (Exception e) {
                return Mono.error(new ProcessException(ProcessException.ErrorCode.SEND_MAIL_FAILED, e));
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }


    /**
     * 從 OAuth 提供者獲取新的存取令牌。
     *
     * @param provider OAuth 提供者
     *
     * @return token 資訊
     */
    private Mono<String> fetchNewToken(OAuthProviderEnum provider) {
        MailProperties.OAuth oauth = customMailProperties.getOauth();

        String tokenUrl = getTokenUrl(provider, oauth);
        Map<String, String> requestBody = buildTokenRequestBody(provider, oauth);

        return webClient
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(buildFormData(requestBody))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("access_token"))
                .doOnError(e -> LogUnity.error("獲取 OAuth token 失敗: %s", e.getMessage()));
    }


    /**
     * 獲取 OAuth token 端點 URL。
     *
     * @param provider OAuth 提供者
     * @param oauth    OAuth 配置
     *
     * @return token 端點 URL
     */
    private String getTokenUrl(OAuthProviderEnum provider, MailProperties.OAuth oauth) {
        if (oauth.getTokenUrl() != null) {
            return oauth.getTokenUrl();
        }

        return provider.getTokenUrl(oauth.getTenantId());
    }


    /**
     * 建立 token 請求的表單資料。
     *
     * @param provider OAuth 提供者
     * @param oauth    OAuth 配置
     *
     * @return 請求參數映射
     */
    private Map<String, String> buildTokenRequestBody(OAuthProviderEnum provider, MailProperties.OAuth oauth) {
        Map<String, String> params = new ConcurrentHashMap<>();
        params.put("client_id", oauth.getClientId());
        params.put("client_secret", oauth.getClientSecret());
        params.put("grant_type", "client_credentials");

        String scope = oauth.getScope();
        if (scope == null || scope.isEmpty()) {
            scope = provider.getDefaultScope();
        }
        params.put("scope", scope);

        return params;
    }


    /**
     * 將參數映射轉換為表單資料字串。
     *
     * @param params 參數映射
     *
     * @return 表單資料字串
     */
    private String buildFormData(Map<String, String> params) {
        return params.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).reduce((a, b) -> a + "&" + b).orElse("");
    }


    /**
     * 清理資源，在 Spring 容器銷毀 Bean 時執行。
     * <p>
     * 確保正確釋放 token 快取所使用的資源，包括背景清理執行緒。
     */
    @PreDestroy
    public void destroy() {
        if (tokenCache != null) {
            LogUnity.info("正在清理 OAuth token 快取資源");
            tokenCache.destroy();
        }
    }
}
