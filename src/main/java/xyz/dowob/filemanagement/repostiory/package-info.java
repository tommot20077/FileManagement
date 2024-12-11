/**
 * 數據庫操作介面，使用Spring Data JPA來操作數據庫。
 * 1. JwtSecurityContextRepository: 用於加載JWT SecurityContext
 * 2. ServerFileMetaRepository: 用於操作伺服器檔案元數據庫 {@link xyz.dowob.filemanagement.entity.ServerFileMetadata}
 * 3. TokenRepository: 用於操作憑證數據庫 {@link xyz.dowob.filemanagement.entity.Token}
 * 4. TransfersRepository: 用於操作檔案傳輸數據庫 {@link xyz.dowob.filemanagement.entity.TransfersTask}
 * 5. UserFileMetaRepository: 用於操作用戶檔案元數據庫 {@link xyz.dowob.filemanagement.entity.UserFileMetadata}
 * 6. UserRepository: 用於操作用戶數據庫 {@link xyz.dowob.filemanagement.entity.User}
 */
package xyz.dowob.filemanagement.repostiory;