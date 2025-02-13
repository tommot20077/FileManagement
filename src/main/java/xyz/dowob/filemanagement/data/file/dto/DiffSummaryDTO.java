package xyz.dowob.filemanagement.data.file.dto;

import lombok.Data;

/**
 * 文件差異摘要數據傳輸對象，用於封裝文件差異摘要的數據
 * @author yuan
 * @program FileManagement
 * @ClassName DiffSummary
 * @create 2025/2/11
 * @Version 1.0
 **/

@Data
public class DiffSummaryDTO {
    /**
     * 插入操作
     */
    private int insertions;
    /**
     * 刪除操作
     */
    private int deletions;
    /**
     * 修改操作
     */
    private int changes;
}