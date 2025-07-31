/**
 * 提供 AOP 切面的實現，用於橫切關注點的動態處理。
 *
 * <p>包含兩個主要的切面實現：</p>
 *
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.component.aspect.LoggerAspect} - 日誌記錄切面，自動攔截並記錄方法執行日誌</li>
 *   <li>{@link xyz.dowob.filemanagement.component.aspect.PermissionAspect} - 權限驗證切面，自動攔截並檢查方法執行的權限</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.component.aspect;