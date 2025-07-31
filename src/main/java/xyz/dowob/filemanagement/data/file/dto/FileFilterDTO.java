package xyz.dowob.filemanagement.data.file.dto;

import lombok.Builder;
import lombok.Data;
import xyz.dowob.filemanagement.customenum.FileEnum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 檔案過濾條件資料傳輸對象，封裝檔案查詢和過濾條件。
 * 用於建立複雜的檔案搜尋和篩選條件，支援多種過濾維度。
 *
 * <p>支援的過濾條件包括關鍵字搜尋、檔案夾範圍、檔案類型、時間範圍等。
 * 提供分頁支援和特殊檔案狀態過濾（已刪除、已共享）。
 * 內建空值處理和預設值設定以簡化使用。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
public class FileFilterDTO {

    /**
     * 搜尋關鍵字，用於查詢檔案名包含此關鍵字的檔案。
     * 長度限制：2-50 個字符
     */
    private String keyword;

    /**
     * 指定資料夾範圍的識別符。
     * null = 搜尋所有資料夾，0 = 只搜尋根目錄，其他值 = 指定資料夾
     */
    private Long folderId;

    /**
     * 檔案類型限制
     */
    @Builder.Default
    private List<FileEnum> types = new ArrayList<>();

    /**
     * 分頁頁碼
     */
    @Builder.Default
    private Integer page = 1;

    /**
     * 分頁大小，默認為 0 會使用系統默認的分頁大小
     */
    @Builder.Default
    private Integer pageSize = 0;

    /**
     * 開始時間
     */
    private LocalDateTime startTime;

    /**
     * 結束時間
     */
    private LocalDateTime endTime;

    /**
     * 是否包含已刪除的檔案
     */
    @Builder.Default
    private boolean includeDeleted = false;

    /**
     * 是否包含已共享的檔案
     */
    @Builder.Default
    private boolean includeShared = false;


    /**
     * 全參數構造函數，對部分參數進行了空值處理
     *
     * @param keyword   關鍵字
     * @param folderId  檔案夾ID
     * @param types     檔案類型
     * @param page      頁碼
     * @param pageSize  分頁大小
     * @param startTime 開始時間
     * @param endTime   結束時間
     */
    public FileFilterDTO(String keyword, Long folderId, List<FileEnum> types, Integer page, Integer pageSize, LocalDateTime startTime, LocalDateTime endTime, Boolean includeDeleted, Boolean includeShared) {
        this.keyword = keyword;
        this.folderId = folderId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.types = Objects.requireNonNullElseGet(types, ArrayList::new);
        this.page = Objects.requireNonNullElse(page, 1);
        this.pageSize = Objects.requireNonNullElse(pageSize, 100);
        this.includeDeleted = Objects.requireNonNullElse(includeDeleted, false);
        this.includeShared = Objects.requireNonNullElse(includeShared, false);
    }


    /**
     * 判斷過濾條件是否為空
     *
     * @return 回傳過濾條件是否為空
     */
    public boolean isFilterEmpty() {
        return keyword == null && folderId == null && types.isEmpty() && startTime == null && endTime == null;
    }
}
