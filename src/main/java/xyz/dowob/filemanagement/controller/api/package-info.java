/**
 * 基於 WebFlux 的 RESTful API 控制器套件，提供檔案管理系統完整 API 端點。
 *
 * <p>包含資料夾管理（{@link xyz.dowob.filemanagement.controller.api.ApiFolderController}）、檔案操作（{@link xyz.dowob.filemanagement.controller.api.ApiGeneralFileController}）、
 * 訪客服務（{@link xyz.dowob.filemanagement.controller.api.ApiGuestController}）、線上編輯（{@link xyz.dowob.filemanagement.controller.api.ApiOnlineFileController}）
 * 和用戶管理（{@link xyz.dowob.filemanagement.controller.api.ApiUserController}）五大核心 API 控制器。所有控制器採用非阻塞響應式設計，
 * 支援高併發操作和流式資料處理。</p>
 *
 * <p>API 端點遵循 RESTful 設計原則，使用 JSON 資料格式，
 * 整合權限驗證、請求限流和統一錯誤處理機制。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.controller.api;