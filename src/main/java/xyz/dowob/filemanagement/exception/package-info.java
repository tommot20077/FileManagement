/**
 * 異常處理套件，包含檔案管理系統中所有自定義異常類別。
 * <p>
 * 此套件提供了結構化的異常處理機制，將不同類型的錯誤進行分類管理，
 * 便於系統維護、除錯和錯誤追蹤。每個異常類別都包含詳細的錯誤碼
 * 和對應的 HTTP 狀態碼，支援國際化和前端錯誤處理。
 * </p>
 * <p>
 * 異常類別分類：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.exception.ValidationException} - 資料驗證相關異常</li>
 *   <li>{@link xyz.dowob.filemanagement.exception.ProcessException} - 系統內部處理異常</li>
 *   <li>{@link xyz.dowob.filemanagement.exception.LimitationException} - 用戶操作限制異常</li>
 *   <li>{@link xyz.dowob.filemanagement.exception.JwtAuthenticationException} - JWT 認證相關異常</li>
 * </ul>
 * </p>
 * <p>
 * 設計特點：
 * <ul>
 *   <li>結構化錯誤碼設計，便於錯誤分類和追蹤</li>
 *   <li>支援參數化錯誤訊息，提供詳細的上下文資訊</li>
 *   <li>與 Spring 框架深度整合，支援自動錯誤處理</li>
 *   <li>包含對應的 HTTP 狀態碼，便於 RESTful API 回應</li>
 *   <li>支援異常鏈追蹤，便於根本原因分析</li>
 * </ul>
 * </p>
 * <p>
 * 異常處理流程：
 * <ol>
 *   <li>業務邏輯層拋出具體的異常類別</li>
 *   <li>全域異常處理器捕獲並處理異常</li>
 *   <li>轉換為標準化的 API 回應格式</li>
 *   <li>記錄到系統日誌供後續分析</li>
 * </ol>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.component.handler.CustomExceptionHandler
 * @see org.springframework.web.bind.annotation.ExceptionHandler
 */
package xyz.dowob.filemanagement.exception;