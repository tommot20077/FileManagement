/**
 * 數據庫操作介面，使用Spring Data JPA來操作數據庫。
 * 1. JwtSecurityContextRepository: 用於加載JWT SecurityContext
 * 2. ServerFileMetaRepository: 用於操作伺服器檔案元數據庫 {@link xyz.dowob.filemanagement.entity.ServerFileMetadata}
 * 3. TokenRepository: 用於操作憑證數據庫 {@link xyz.dowob.filemanagement.entity.Token}
 * 4. TransfersRepository: 用於操作檔案傳輸數據庫 {@link xyz.dowob.filemanagement.entity.TransfersTask}
 * 5. UserFileMetaRepository: 用於操作用戶檔案元數據庫 {@link xyz.dowob.filemanagement.entity.UserFileMetadata}
 * 6. UserOnlineFileRepository: 用於操作用戶在線檔案數據庫 {@link xyz.dowob.filemanagement.entity.UserOnlineFile}
 * 7. UserOnlineFileHistoryRepository: 用於操作用戶在線檔案歷史數據庫 {@link xyz.dowob.filemanagement.entity.UserOnlineFileHistory}
 * 8. UserRepository: 用於操作用戶數據庫 {@link xyz.dowob.filemanagement.entity.User}
 */
package xyz.dowob.filemanagement.repostiory;