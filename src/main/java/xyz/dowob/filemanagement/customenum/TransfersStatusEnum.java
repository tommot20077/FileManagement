package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 檔案傳輸任務狀態列舉。
 * <p>定義檔案上傳和下載任務的各種執行狀態，用於追蹤和管理非同步檔案傳輸進度。
 * 支援上傳中、下載中、已完成和失敗等狀態，提供完整的任務生命週期管理。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RequiredArgsConstructor
@Getter
public enum TransfersStatusEnum {
    /**
     * 檔案上傳進行中狀態。
     * 表示檔案正在從客戶端傳輸至伺服器，包括分塊上傳和多部分上傳模式。
     */
    UPLOADING("上傳檔案中"),

    /**
     * 檔案下載進行中狀態。
     * 表示檔案正在從伺服器傳輸至客戶端，支援斷點續傳和流式下載。
     */
    DOWNLOADING("下載檔案中"),

    /**
     * 傳輸任務完成狀態。
     * 表示檔案上傳或下載已成功完成，檔案已安全儲存或傳輸至目標位置。
     */
    COMPLETED("已完成"),

    /**
     * 傳輸任務失敗狀態。
     * 表示檔案傳輸過程中發生錯誤，可能因為網路問題、檔案損壞或權限不足等原因導致。
     */
    FAILED("失敗");

    /**
     * 任務狀態描述字串。
     * 提供任務狀態的中文描述，用於使用者介面顯示、日誌記錄和狀態報告。
     */
    private final String status;

}
