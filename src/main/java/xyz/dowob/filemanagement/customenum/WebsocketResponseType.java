package xyz.dowob.filemanagement.customenum;

/**
 * WebSocket 連線回應訊息類型列舉。
 * <p>定義 WebSocket 連線中伺服器向客戶端傳送的不同類型回應訊息。
 * 每種訊息類型具有不同的處理方式和連線管理策略，用於區分一般資訊、錯誤提示和致命錯誤。
 * 支援適當的錯誤等級和連線狀態管理，確保 WebSocket 連線的穩定性和可靠性。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

public enum WebsocketResponseType {
    /**
     * 一般資訊通知訊息。
     * 用於傳送一般性的狀態更新、進度通知或提示資訊，不影響 WebSocket 連線狀態。
     */
    INFO,

    /**
     * 可復原錯誤訊息。
     * 用於傳送非致命性錯誤訊息，如輸入驗證失敗或操作警告，保持 WebSocket 連線正常運作。
     */
    ERROR,

    /**
     * 連線致命錯誤訊息。
     * 用於傳送致命性錯誤訊息，如認證失敗或連線異常，傳送後將主動關閉 WebSocket 連線。
     */
    CONNECTION_ERROR,
}
