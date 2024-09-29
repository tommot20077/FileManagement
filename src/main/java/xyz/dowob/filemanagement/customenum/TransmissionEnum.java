package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 傳輸類型的枚舉類，用於區分檔案使用的傳輸協議
 *
 * @program File-Management
 * @ClassName FileEnum
 * @description
 * @create 2024-10-04 02:30
 * @Version 1.0
 * @Author yuan
 */
@Getter
@RequiredArgsConstructor
public enum TransmissionEnum {
    /**
     * Tus 協議，適合大檔案上傳，支持斷點續傳
     */
    TUS("tus"),
    /**
     * Multipart 協議，適合小檔案上傳
     */
    MULTIPART("multipart"),
    /**
     * 混合協議，根據檔案大小自動選擇協議
     */
    MIXED("mixed");

    private final String type;

}
