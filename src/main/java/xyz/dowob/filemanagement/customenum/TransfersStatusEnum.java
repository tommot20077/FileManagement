package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文件傳輸狀態的枚舉類
 *
 * @author yuan
 * @program File-Management
 * @ClassName TransfersStatusEnum
 * @description
 * @create 2024-012-11 17:09
 * @Version 1.0
 */
@RequiredArgsConstructor
@Getter
public enum TransfersStatusEnum {
    /**
     * 上傳檔案中
     */
    UPLOADING("上傳檔案中"),

    /**
     * 下載檔案中
     */
    DOWNLOADING("下載檔案中"),

    /**
     * 已完成
     */
    COMPLETED("已完成"),

    /**
     * 失敗
     */
    FAILED("失敗");

    /**
     * 任務狀態
     */
    private final String status;

}
