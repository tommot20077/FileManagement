package xyz.dowob.filemanagement.customenum;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName DownloadActionEnum
 * @create 2025/3/12
 * @Version 1.0
 **/

public enum DownloadActionEnum {
    /**
     * 預覽
     */
    PREVIEW,
    /**
     * 下載
     */
    DOWNLOAD;

    /**
     * 根據名稱獲取對應的枚舉類型，此方法不區分大小寫
     * 預設返回 {@link DownloadActionEnum#PREVIEW}
     *
     * @param name 名稱
     *
     * @return 對應的枚舉類型
     */
    public static DownloadActionEnum getType(String name) {
        for (DownloadActionEnum value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return PREVIEW;
    }
}
