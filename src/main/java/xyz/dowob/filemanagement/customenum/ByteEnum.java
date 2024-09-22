package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用於文件大小單位轉換的枚舉類
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ByteEnum
 * @create 2025/2/4
 * @Version 1.0
 **/

@Getter
@RequiredArgsConstructor
public enum ByteEnum {
    /**
     * Byte類型的枚舉
     */
    BYTE(1L, "B"),

    /**
     * KB類型的枚舉
     */
    KILOBYTE(1024L, "KB"),

    /**
     * MB類型的枚舉
     */
    MEGABYTE(1024L * 1024, "MB"),

    /**
     * GB類型的枚舉
     */
    GIGABYTE(1024L * 1024 * 1024, "GB"),

    /**
     * TB類型的枚舉
     */
    TERABYTE(1024L * 1024 * 1024 * 1024, "TB");

    /**
     * 單位對應的字節數
     */
    private final long bytes;

    /**
     * 單位的名稱
     */
    private final String unit;

    /**
     * 重載convertToByte方法，將文件單位指定為GB
     *
     * @param size 文件大小
     *
     * @return 轉換後的GB
     */
    public static long convertToByte(long size) {
        return convertToByte(size, GIGABYTE);
    }


    /**
     * 將文件大小轉換為字節數
     *
     * @param size   文件大小
     * @param target 目標單位
     *
     * @return 轉換後的字節數
     */
    public static long convertToByte(long size, ByteEnum target) {
        return size * target.bytes;
    }


    /**
     * 將字節數轉換為可讀的文件大小，保留兩位小數
     * 當超過1024時，轉換為更大的單位
     * 在最大單位TB時停止轉換
     *
     * @param size 字節數
     *
     * @return 可讀的文件大小
     */
    public static String toReadableSize(long size) {
        int i = 0;
        double s = size;
        while (s >= 1024 && i < values().length - 1) {
            s /= 1024;
            i++;
        }
        return String.format("%.2f %s", s, values()[i].unit);
    }
}

