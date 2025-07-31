/**
 * 服務提供者具體實現模組，提供各種業務功能的反應式實現類別。
 *
 * <p>此模組包含系統核心服務的具體實現，每個實現類別都遵循對應的提供者介面規範。
 * 採用反應式程式設計模型，確保所有操作均為非阻塞式處理。</p>
 *
 * <p>主要實現類別：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerImplement.EmailProviderImpl}：
 *       基於 Spring Mail 的非阻塞郵件發送實現</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl}：
 *       JWT 令牌管理實現，支援快取與版本控制</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerImplement.PasswordResetTokenProviderImpl}：
 *       密碼重置令牌實現，提供安全的密碼重設機制</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerImplement.FileScanProviderImpl}：
 *       檔案安全掃描實現，基於 ClamAV 的病毒檢測</li>
 * </ul>
 * </p>
 *
 * <p>子模組包含專門的快取提供者與內容轉換提供者實現，
 * 提供更細粒度的功能劃分與管理。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.component.provider.providerImplement;