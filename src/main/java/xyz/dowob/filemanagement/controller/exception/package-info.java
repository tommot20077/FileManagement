/**
 * 自定義異常控制器Package
 * 此包負責管理請求過程中的異常處理，並返回格式化的異常信息{@link xyz.dowob.filemanagement.dto.api.ApiResponseDTO}
 * 但不負責業務處理過程中的錯誤，此類錯誤交由錯誤處理器處理{@link xyz.dowob.filemanagement.component.handler.CustomExceptionHandler}
 */
package xyz.dowob.filemanagement.controller.exception;