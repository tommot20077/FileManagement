/**
 * 服務業務邏輯層接口定義套件，規範服務層的方法契約。定義各種業務功能的接口規範，後續由實現類進行具體實現。
 * <p>
 * 主要接口定義：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService} - 抽象檔案服務接口</li>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService} - 授權服務接口</li>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.BaseFileService} - 基本檔案業務邏輯接口</li>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.CrudService} - 基本增刪改查接口</li>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.FileService} - 檔案服務接口</li>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.FolderService} - 檔案夾服務接口</li>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.PermissionService} - 權限服務接口</li>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.RecoverableFile} - 檔案恢復服務接口</li>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.TokenService} - 憑證服務接口</li>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.UserService} - 用戶服務接口</li>
 *   <li>{@link xyz.dowob.filemanagement.service.serviceInterface.ValidationService} - 驗證服務接口</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.service.serviceInterface;