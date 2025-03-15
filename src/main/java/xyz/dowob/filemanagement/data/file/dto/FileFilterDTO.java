package xyz.dowob.filemanagement.data.file.dto;

import lombok.Builder;
import lombok.Data;
import xyz.dowob.filemanagement.customenum.FileEnum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 文件過濾條件數據傳輸對象，用於封裝文件過濾條件
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileFilterDto
 * @create 2025/3/3
 * @Version 1.0
 **/
@Data
@Builder
public class FileFilterDTO {

    /**
     * 關鍵字，此關鍵字用於查詢文件名包含關鍵字的文件，最少需要2個字符最大不可超過50個字符
     */
    private String keyword;

    /**
     * 指定文件夾ID，用於查詢指定文件夾下的文件，當為null時查詢所有文件，為0時查詢根文件夾下的文件
     */
    private Long folderId;

    /**
     * 文件類型限制
     */
    private List<FileEnum> types;

    /**
     * 分頁頁碼
     */
    private Integer page;

    /**
     * 分頁大小
     */
    private Integer pageSize;

    /**
     * 開始時間
     */
    private LocalDateTime startTime;

    /**
     * 結束時間
     */
    private LocalDateTime endTime;

    /**
     * 是否包含已刪除的文件
     */
    private Boolean includeDeleted;

    /**
     * 是否包含已共享的文件
     */
    private Boolean includeShared;

    /**
     * 判斷過濾條件是否為空
     *
     * @return 返回過濾條件是否為空
     */
    public boolean isFilterEmpty() {
        return keyword == null && folderId == null && types.isEmpty() && startTime == null && endTime == null;
    }

    /**
     * 全參數構造函數，對部分參數進行了空值處理
     *
     * @param keyword   關鍵字
     * @param folderId  文件夾ID
     * @param types     文件類型
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
}
