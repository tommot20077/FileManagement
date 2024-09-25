package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文件類型的枚舉類，用於標記文件的類型
 *
 * @program File-Management
 * @ClassName FileEnum
 * @description
 * @create 2024-09-20 22:30
 * @Version 1.0
 * @Author yuan
 */
@Getter
@RequiredArgsConstructor
public enum FileEnum {
    /**
     * 照片類型
     */
    IMAGE("照片"),
    /**
     * 影片類型
     */
    VIDEO("影片"),
    /**
     * 音樂類型
     */
    MUSIC("音樂"),
    /**
     * 文件類型
     */
    DOCUMENT("文件"),
    /**
     * 壓縮檔類型
     */
    ZIP("壓縮檔"),
    /**
     * 其他類型
     */
    OTHER("其他");

    /**
     * 文件類型
     */
    private final String type;
}
