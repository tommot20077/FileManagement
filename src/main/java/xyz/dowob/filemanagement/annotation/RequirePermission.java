package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.PermissionEnum;

import java.lang.annotation.*;

/**
 * 方法級別權限控制註解。
 * <p>
 * 此註解是權限控制系統的核心組件，用於在方法級別宣告訪問權限需求。
 * 當方法被標記此註解時，{@link xyz.dowob.filemanagement.component.aspect.PermissionAspect}
 * 切面會在方法執行前進行權限驗證，確保當前使用者具備所需的所有權限。
 * <p>
 * 權限驗證機制支援多重權限要求（當指定多個權限時，使用者必須具備所有權限）、
 * 權限驗證失敗時的訪問拒絕與異常拋出、與角色為基礎的存取控制 (RBAC) 系統整合，
 * 以及根據使用者當前狀態的動態權限評估。
 * <p>
 * AOP 切面協作流程：
 * <p>
 * PermissionAspect 切面攔截被標記的方法
 * <p>
 * 從註解中獲取所需權限清單
 * <p>
 * 獲取當前使用者的身份資訊和權限
 * <p>
 * 逐一驗證所需權限是否符合
 * <p>
 * 權限驗證通過後繼續執行目標方法
 * <p>
 * 使用範例：
 * <pre>{@code
 * // 單一權限要求
 * @RequirePermission(PermissionEnum.FILE_READ)
 * public Mono<ResponseEntity> downloadFile(String fileId) {
 *     // 檔案下載邏輯
 * }
 * 
 * // 多重權限要求
 * @RequirePermission({PermissionEnum.FILE_WRITE, PermissionEnum.FOLDER_MANAGE})
 * public Mono<ResponseEntity> moveFile(String fileId, String targetFolderId) {
 *     // 檔案移動邏輯
 * }
 * }</pre>
 * <p>
 * 安全性考量：
 * <p>
 * 所有對外暴露的 API 端點都應該使用此註解進行權限控制
 * <p>
 * 權限驗證失敗會產生安全日誌記錄
 * <p>
 * 支援 JWT 授權機制和無狀態認證
 * <p>
 * 與 Spring Security 整合，提供多層次的安全防護
 * <p>
 * 效能考量：
 * <p>
 * 權限驗證使用快取機制減少資料庫查詢
 * <p>
 * 在 WebFlux 非阻塞環境中使用 Reactive 模式處理
 * <p>
 * 支援高並發的權限驗證處理
 *
 * @see xyz.dowob.filemanagement.component.aspect.PermissionAspect
 * @see xyz.dowob.filemanagement.customenum.PermissionEnum
 * @see xyz.dowob.filemanagement.service.serviceInterface.PermissionService
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequirePermission {
    /**
     * 指定方法執行所需的權限清單。
     * <p>
     * 此屬性定義了使用者必須具備的所有權限才能成功執行目標方法。當指定多個權限時，
     * 系統會使用 AND 邏輯進行驗證，即使用者必須同時具備所有指定的權限。
     * <p>
     * 權限驗證過程：
     * <p>
     * 獲取當前使用者的身份資訊
     * <p>
     * 查詢使用者的角色和權限分配
     * <p>
     * 逐一檢查所需權限是否在使用者的權限清單中
     * <p>
     * 所有權限驗證通過後允許執行方法
     *
     * @return 權限枚舉陣列，使用 {@link xyz.dowob.filemanagement.customenum.PermissionEnum} 定義的權限類型
     */
    PermissionEnum[] value();
}

