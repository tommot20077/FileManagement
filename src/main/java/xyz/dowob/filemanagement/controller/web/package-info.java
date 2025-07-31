/**
 * Web 控制器套件，基於 WebFlux 的反應式 Web API 控制器實現。
 *
 * <p>提供檔案管理系統完整的 Web API 控制器，採用 RESTful 架構設計，
 * 支援高併發反應式請求處理。包含資料夾管理（{@link xyz.dowob.filemanagement.controller.web.WebFolderController}）、
 * 檔案操作（{@link xyz.dowob.filemanagement.controller.web.WebGeneralFileController}）、訪客驗證（{@link xyz.dowob.filemanagement.controller.web.WebGuestController}）、
 * 線上協作（{@link xyz.dowob.filemanagement.controller.web.WebOnlineFileController}）和用戶管理（{@link xyz.dowob.filemanagement.controller.web.WebUserController}）五個控制器。</p>
 *
 * <p>所有控制器遵循統一的錯誤處理和權限控制機制，確保系統安全性和一致性。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.controller.web;