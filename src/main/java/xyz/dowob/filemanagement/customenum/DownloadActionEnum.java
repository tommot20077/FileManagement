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

    public static DownloadActionEnum getType(String name) {
        for (DownloadActionEnum value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return PREVIEW;
    }
}
