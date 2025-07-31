package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 檔案儲存大小單位轉換工具，提供從位元組到不同儲存單位的精確換算。
 *
 * <p>此枚舉類提供了一個強大且靈活的檔案大小單位轉換機制，支援從位元組(B)到太位元組(TB)的精確換算。
 * 設計目的是提供一個統一且易於使用的檔案大小管理工具，可以在系統的各個層面進行儲存單位換算。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

@Getter
@RequiredArgsConstructor
public enum ByteEnum {
    /**
     * 位元組(Byte)，基本儲存單位，用於精確表示最小的儲存單位。
     */
    BYTE(1L, "B"),

    /**
     * 千位元組(Kilobyte)，表示1024個位元組，常用於小型檔案大小描述。
     */
    KILOBYTE(1024L, "KB"),

    /**
     * 百萬位元組(Megabyte)，表示1024個千位元組，適用於中等大小的檔案。
     */
    MEGABYTE(1024L * 1024, "MB"),

    /**
     * 十億位元組(Gigabyte)，表示1024個百萬位元組，常用於大型檔案和儲存設備容量。
     */
    GIGABYTE(1024L * 1024 * 1024, "GB"),

    /**
     * 太位元組(Terabyte)，表示1024個十億位元組，用於描述大型儲存系統和資料中心的儲存容量。
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
     * 重載convertToByte方法，將檔案單位指定為GB
     *
     * @param size 檔案大小
     *
     * @return 轉換後的GB
     */
    public static long convertToByte(long size) {
        return convertToByte(size, GIGABYTE);
    }


    /**
     * 將檔案大小轉換為字節數
     *
     * @param size   檔案大小
     * @param target 目標單位
     *
     * @return 轉換後的字節數
     */
    public static long convertToByte(long size, ByteEnum target) {
        return size * target.bytes;
    }


    /**
     * 將字節數轉換為可讀的檔案大小，保留兩位小數
     * 當超過1024時，轉換為更大的單位
     * 在最大單位TB時停止轉換
     *
     * @param size 字節數
     *
     * @return 可讀的檔案大小
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

