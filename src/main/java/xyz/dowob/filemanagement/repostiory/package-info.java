/**
 * 數據庫操作介面，使用Spring Data JPA來操作數據庫。
 * 1. FileTrashRecordRepository: 用於操作檔案回收站數據庫 {@link xyz.dowob.filemanagement.entity.FileTrashRecord}
 * 2. JwtSecurityContextRepository: 用於加載JWT SecurityContext {@link xyz.dowob.filemanagement.repostiory.JwtSecurityContextRepository}
 * 3. ServerFileMetaRepository: 用於操作伺服器檔案元數據庫 {@link xyz.dowob.filemanagement.entity.ServerFileMetadata}
 * 4. TokenRepository: 用於操作憑證數據庫 {@link xyz.dowob.filemanagement.entity.Token}
 * 5. TransfersRepository: 用於操作檔案傳輸數據庫 {@link xyz.dowob.filemanagement.entity.TransfersTask}
 * 6. UserFileMetaRepository: 用於操作用戶檔案元數據庫 {@link xyz.dowob.filemanagement.entity.UserFileMetadata}
 * 7. UserFileShareRecordRepository: 用於操作用戶檔案分享記錄數據庫 {@link xyz.dowob.filemanagement.entity.UserFileShareRecord}
 * 8. UserOnlineFileRepository: 用於操作用戶在線檔案數據庫 {@link xyz.dowob.filemanagement.entity.UserOnlineFile}
 * 9. UserOnlineFileHistoryRepository: 用於操作用戶在線檔案歷史數據庫 {@link xyz.dowob.filemanagement.entity.UserOnlineFileHistory}
 * 10. UserRepository: 用於操作用戶數據庫 {@link xyz.dowob.filemanagement.entity.User}
 */
package xyz.dowob.filemanagement.repostiory;