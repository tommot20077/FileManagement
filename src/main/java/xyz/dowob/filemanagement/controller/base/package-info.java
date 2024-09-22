/**
 * 此 package 主要放置控制器的基礎類
 * 繼承處理回應接口的抽象類 ResponseUnity{@link xyz.dowob.filemanagement.unity.ResponseUnity}
 * 用於定義控制器的基本方法
 * 因為需要根據Api請求以及一般Web請求進行不同的處理(在security中 api請求不須驗證csrf token)
 * 1. BaseFileController{@link xyz.dowob.filemanagement.controller.base.BaseFileController}
 * 2. BaseFolderController{@link xyz.dowob.filemanagement.controller.base.BaseFolderController}
 * 3. BaseGeneralFileController{@link xyz.dowob.filemanagement.controller.base.BaseGeneralFileController}
 * 4. BaseGuestController{@link xyz.dowob.filemanagement.controller.base.BaseGuestController}
 * 5. BaseOnlineFileController{@link xyz.dowob.filemanagement.controller.base.BaseOnlineFileController}
 * 6. BaseUserController{@link xyz.dowob.filemanagement.controller.base.BaseUserController}
 */
package xyz.dowob.filemanagement.controller.base;