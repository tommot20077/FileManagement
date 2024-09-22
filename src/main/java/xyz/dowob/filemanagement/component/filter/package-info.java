/**
 * 此包放置過濾器的實現類，過濾器會在正式開始執行之前進行操作
 * 可以實現對於請求的一些過濾以及預處理
 * 1. RequestFilter: IP請求過濾器，用於限制IP的請求速度 {@link xyz.dowob.filemanagement.component.filter.requestlimiter}
 * 2. ClientIpFilter: 獲取用戶的IP地址以及儲存到交換訊息中 {@link xyz.dowob.filemanagement.component.filter.ClientIpFilter}
 */

package xyz.dowob.filemanagement.component.filter;