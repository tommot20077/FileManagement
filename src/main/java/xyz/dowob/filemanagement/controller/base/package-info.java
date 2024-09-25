/**
 * 此 package 主要放置控制器的基礎類
 * 主要由一個基礎控制器接口 BaseController{@link xyz.dowob.filemanagement.controller.base.BaseController}
 * 以及繼承這接口的抽象類 {@link xyz.dowob.filemanagement.controller.base}
 * 用於定義控制器的基本方法
 * 因為需要根據Api請求以及一般Web請求進行不同的處理(在security中 api請求不須驗證csrf token)
 */
package xyz.dowob.filemanagement.controller.base;