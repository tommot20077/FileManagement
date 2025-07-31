/**
 * 異常處理控制器套件，統一管理 WebFlux 請求處理的異常攔截與響應。
 *
 * <p>攔截 HTTP 請求階段的各類異常，轉換為標準化的 {@link xyz.dowob.filemanagement.data.response.ApiResponseDTO} 響應格式。
 * 根據異常類型映射適當的 HTTP 狀態碼與錯誤信息，涵蓋參數驗證錯誤、資源未找到、
 * 請求方法不支持和資料庫操作異常等常見錯誤情況。</p>
 *
 * <p>注意：業務層內部的異常處理由 {@link xyz.dowob.filemanagement.component.handler.CustomExceptionHandler} 負責。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.controller.exception;