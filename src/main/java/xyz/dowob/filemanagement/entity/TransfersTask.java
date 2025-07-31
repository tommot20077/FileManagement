package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 檔案傳輸任務實體類，基於 Spring Data R2DBC 的資料庫映射實現。
 * 對應資料庫中的 transfers_task 表，管理檔案上傳、下載和傳輸任務的完整生命週期，
 * 提供異步傳輸操作的狀態追蹤、錯誤處理和效能監控功能。
 * <p>
 * 支援分塊上傳和檔案去重機制的傳輸最佳化，透過 MD5 雜湊值確保檔案完整性，
 * 並通過 GridFS 識別碼實現分散式檔案儲存管理。每個傳輸任務記錄包含完整的
 * 執行時間統計、狀態變更歷史和詳細的錯誤資訊，確保系統可觀測性和故障排除能力。
 * <p>
 * 此實體支援 WebFlux 響應式程式設計模式，適用於高並發檔案傳輸場景，
 * 並提供非阻塞的資料庫操作和事務管理。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see TransfersStatusEnum
 * @see ServerFileMetadata
 */
@Getter
@Setter
@Table(name = "transfers_task")
public class TransfersTask {
    /**
     * 傳輸任務資料庫主鍵識別碼。
     * 採用自動遞增策略生成，確保每個傳輸任務記錄的唯一性，
     * 用於資料庫關聯查詢和實體對象比較操作。
     */
    @Id
    private Long id;

    /**
     * 傳輸任務業務識別碼。
     * 由系統生成的唯一字串，用於追蹤特定傳輸任務的執行狀態，
     * 客戶端可透過此識別碼查詢傳輸進度和結果。通常採用 UUID 格式確保全域唯一性。
     */
    @Column("transfer_task_id")
    private String transferTaskId;

    /**
     * 檔案內容 MD5 雜湊值。
     * 用於檔案完整性驗證和去重機制，確保傳輸過程中檔案內容未被篡改。
     * 系統透過此值判斷檔案是否已存在，避免重複上傳相同內容的檔案，
     * 提升儲存效率和傳輸效能。
     */
    @Column("md5")
    private String md5;

    /**
     * MongoDB GridFS 檔案儲存識別碼。
     * 指向檔案在 GridFS 中的實際儲存位置，用於檔案內容的讀取和管理操作。
     * 當傳輸任務完成後，此欄位記錄檔案的最終儲存位置，
     * 支援大檔案的分塊儲存和高效率存取。
     */
    @Column("grid_fs_id")
    private String gridFsId;

    /**
     * 檔案內容大小，以位元組為單位。
     * 記錄待傳輸或已傳輸檔案的總大小，用於傳輸進度計算、
     * 儲存空間管理和傳輸效能分析。支援大檔案處理，
     * 最大可記錄 Long 型別範圍內的檔案大小。
     */
    @Column("file_size")
    private Long fileSize;

    /**
     * 傳輸任務開始執行時間。
     * 記錄任務建立並開始處理的精確時間戳，用於傳輸時間統計、
     * 效能分析和任務超時檢測。採用系統本地時區的 LocalDateTime 格式，
     * 確保時間記錄的準確性和一致性。
     */
    @Column("start_time")
    private LocalDateTime startTime;

    /**
     * 傳輸任務完成時間。
     * 記錄任務執行結束的精確時間戳，無論任務成功完成或因錯誤終止。
     * 與開始時間結合可計算任務執行總時長，用於效能監控和
     * 系統最佳化分析。未完成的任務此欄位為 null。
     */
    @Column("finish_time")
    private LocalDateTime finishTime;

    /**
     * 傳輸任務執行狀態訊息。
     * 記錄任務執行過程中的重要資訊，包括成功完成的確認訊息、
     * 錯誤詳情、警告資訊或執行進度說明。此欄位對於故障排除、
     * 系統監控和使用者狀態回饋至關重要。
     */
    private String message;

    /**
     * 傳輸任務當前執行狀態。
     * 使用 {@link TransfersStatusEnum} 列舉值表示任務的生命週期狀態，
     * 包括等待中、執行中、已完成、已失敗等。系統透過此欄位追蹤
     * 任務進度，並據此執行相應的業務邏輯和狀態通知。
     */
    private TransfersStatusEnum status;

    /**
     * 計算傳輸任務實體物件的雜湊碼值。
     * 基於主鍵 ID 計算雜湊值，確保相同 ID 的實體物件具有相同的雜湊碼，
     * 支援 HashSet、HashMap 等雜湊基礎集合的正確運作。
     *
     * @return 基於主鍵 ID 計算的雜湊碼值
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * 判斷當前傳輸任務實體與指定物件是否相等。
     * 採用主鍵 ID 作為相等性判斷的唯一標準，符合實體物件的業務語義。
     * 此方法確保與 hashCode 方法的契約一致性，滿足 Object.equals 規範。
     *
     * @param o 待比較的物件，可為任意型別
     * @return 若兩個物件為相同實體或具有相同主鍵 ID 則返回 true，否則返回 false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransfersTask that = (TransfersTask) o;
        return id.equals(that.id);
    }

    /**
     * 產生傳輸任務實體的字串表示形式。
     * 將所有重要屬性封裝為 HashMap 並轉換為字串格式，便於除錯、日誌記錄
     * 和系統監控。字串內容包含任務識別資訊、檔案屬性、時間統計和執行狀態等關鍵資料。
     *
     * @return 包含所有實體屬性的格式化字串表示
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("transferTaskId", transferTaskId);
        map.put("md5", md5);
        map.put("gridFsId", gridFsId);
        map.put("fileSize", fileSize);
        map.put("startTime", startTime);
        map.put("finishTime", finishTime);
        map.put("message", message);
        map.put("status", status);
        return map.toString();
    }
}
