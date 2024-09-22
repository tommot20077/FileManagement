/**
 * 此包主要放置一些有關於自定義CsrfToken的類
 * 包含主要定義接口 {@link xyz.dowob.filemanagement.repostiory.ServerCsrfToken.CustomServerCsrfTokenRepository}
 * 以及部分實現的抽象類 {@link xyz.dowob.filemanagement.repostiory.ServerCsrfToken.AbstractServerCsrfTokenRepository}
 * 還有具體的實現類
 * 1. LocalServerCsrfTokenRepository: 用於存儲CsrfToken的本地存儲庫 {@link xyz.dowob.filemanagement.repostiory.ServerCsrfToken.LocalServerCsrfTokenRepository}
 * 2. RedisServerCsrfTokenRepository: 用於存儲CsrfToken的Redis存儲庫
 * {@link xyz.dowob.filemanagement.repostiory.ServerCsrfToken.RedisServerCsrfTokenRepository}
 */

package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;