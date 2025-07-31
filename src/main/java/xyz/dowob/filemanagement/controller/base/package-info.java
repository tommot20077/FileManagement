/**
 * 控制器基礎類套件，提供所有控制器的共用抽象實現。
 *
 * <p>所有基礎控制器繼承 {@link xyz.dowob.filemanagement.unity.ResponseUnity} 接口，
 * 提供統一的回應處理方法。設計用於支援 API 和 Web 兩種請求類型的差異化處理，
 * 其中 API 請求無需 CSRF 令牌驗證。</p>
 *
 * <p>包含六個基礎控制器：{@link xyz.dowob.filemanagement.controller.base.BaseFileController}、{@link xyz.dowob.filemanagement.controller.base.BaseFolderController}、
 * {@link xyz.dowob.filemanagement.controller.base.BaseGeneralFileController}、{@link xyz.dowob.filemanagement.controller.base.BaseGuestController}、
 * {@link xyz.dowob.filemanagement.controller.base.BaseOnlineFileController} 和 {@link xyz.dowob.filemanagement.controller.base.BaseUserController}。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.controller.base;