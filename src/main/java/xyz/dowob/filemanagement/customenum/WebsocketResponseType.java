package xyz.dowob.filemanagement.customenum;

/**
 * WebSocket回應類型，用於區分不同類型的回應
 * @author yuan
 * @program FileManagement
 * @ClassName WebsocketResponseType
 * @create 2025/5/16
 * @Version 1.0
 **/

public enum WebsocketResponseType {
    /**
     * 通知訊息
     */
    INFO,

    /**
     * 錯誤訊息，不關閉連線
     */
    ERROR,

    /**
     * 連線錯誤，會關閉連線
     */
    CONNECTION_ERROR,
}
