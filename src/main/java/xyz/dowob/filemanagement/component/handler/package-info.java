/**
 * 用於處理外部請求的類別，包含了請求的處理以及回應的處理
 * 使用抽象類以及接口來規範實現類別的行為
 * 1. CustomExceptionHandler: 自定義錯誤處理器 {@link xyz.dowob.filemanagement.component.handler.CustomExceptionHandler}
 * 2. CustomWebSocketHandler: 自定義WebSocket Session {@link xyz.dowob.filemanagement.component.handler.CustomWebSocketSession}
 * 3. FileUploadWebSocketHandler: 用於檔案上傳的WebSocket處理器 {@link xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler}
 * 4. OnlineFileWebSocketHandler: 用於線上編輯的WebSocket處理器 {@link xyz.dowob.filemanagement.component.handler.OnlineFileWebSocketHandler}
 * 5. WebSocketFailHandler: 用於處理WebSocket連接失敗的處理器 {@link xyz.dowob.filemanagement.component.handler.WebSocketFailHandler}
 */
package xyz.dowob.filemanagement.component.handler;