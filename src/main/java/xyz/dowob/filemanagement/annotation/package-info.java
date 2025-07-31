/**
 * 自定義註解包。
 * <p>
 * 此包包含了檔案管理系統中所有自定義的註解定義，這些註解是系統架構的重要組成部分，
 * 支援策略模式、AOP 攔截、安全控制和日誌管理等功能的實現。
 * </p>
 * <p>
 * 註解分類說明：
 * </p>
 * <p>
 * <strong>策略標記註解：</strong><br>
 * 用於標記不同策略實現的類型，支援系統運行時的動態選擇：
 * </p>
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.annotation.CacheProviderType} - 緩存提供器類型標記，支援多種緩存後端的策略選擇</li>
 *   <li>{@link xyz.dowob.filemanagement.annotation.CsrfRepositoryType} - CSRF 令牌儲存庫類型標記，支援多種安全策略</li>
 *   <li>{@link xyz.dowob.filemanagement.annotation.FileHandlerType} - 檔案處理器類型標記，支援不同檔案類型的專門處理</li>
 *   <li>{@link xyz.dowob.filemanagement.annotation.UserLimiterType} - 使用者限流器類型標記，支援多種限流策略</li>
 * </ul>
 * <p>
 * <strong>安全和隱私註解：</strong><br>
 * 用於保護敏感資訊和控制資料外洩風險：
 * </p>
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.annotation.HideSensitive} - 敏感資訊隱藏標記，防止機密資料在日誌中洩露</li>
 *   <li>{@link xyz.dowob.filemanagement.annotation.HideOverLength} - 超長字串隱藏標記，防止日誌過於冗長</li>
 *   <li>{@link xyz.dowob.filemanagement.annotation.RequirePermission} - 權限驗證標記，強制方法級別的存取控制</li>
 * </ul>
 * <p>
 * <strong>日誌控制註解：</strong><br>
 * 用於精細化控制系統日誌的記錄行為：
 * </p>
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.annotation.RecordLevel} - 日誌級別標記，控制不同場景下的日誌詳細程度</li>
 *   <li>{@link xyz.dowob.filemanagement.annotation.SkipRecord} - 跳過日誌記錄標記，排除特定方法或類別的日誌輸出</li>
 * </ul>
 * <p>
 * 這些註解透過 AOP（面向切面程式設計）機制實現功能，在系統運行時提供橫切關注點的處理，
 * 包括但不限於權限驗證、日誌記錄、效能監控、安全防護等功能。
 * </p>
 * 
 * @since 1.0
 * @author yuan
 * @version 1.0
 * @see xyz.dowob.filemanagement.component.aspect 相關的 AOP 切面實現
 * @see xyz.dowob.filemanagement.component.strategy 策略模式相關實現
 */

package xyz.dowob.filemanagement.annotation;
