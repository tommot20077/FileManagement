/**
 * 數據庫操作介面，使用Spring Data JPA來操作數據庫。
 * 1. JwtSecurityContextRepository: 用於加載JWT SecurityContext
 * 2. UserRepository: 用於操作用戶數據庫 {@link xyz.dowob.filemanagement.entity.User}
 * 3. TokenRepository: 用於操作憑證數據庫 {@link xyz.dowob.filemanagement.entity.Token}
 */
package xyz.dowob.filemanagement.repostiory;