/**
 * 此 package 用於存放 service 業務邏輯層內的方法定義接口
 * 規範 service 層的方法基礎需要的方法，後續交由實現類進行具體的實現
 * 1. AbstractFileService 用於定義文件相關的方法 {@link xyz.dowob.filemanagement.service.serviceInterface.AbstractFileService}
 * 1. AuthorizationService 用於定義授權相關的方法 {@link xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService}
 * 2. CrudService 用於定義基本的增刪改查方法 {@link xyz.dowob.filemanagement.service.serviceInterface.CrudService}
 * 3. FileService 用於定義文件相關的方法 {@link xyz.dowob.filemanagement.service.serviceInterface.FileService}
 * 4. TokenService 用於定義 token 相關的方法 {@link xyz.dowob.filemanagement.service.serviceInterface.TokenService}
 * 5. UserService 用於定義用戶相關的方法 {@link xyz.dowob.filemanagement.service.serviceInterface.UserService}
 * 6. ValidationService 用於定義驗證相關的方法 {@link xyz.dowob.filemanagement.service.serviceInterface.ValidationService}
 */
package xyz.dowob.filemanagement.service.serviceInterface;