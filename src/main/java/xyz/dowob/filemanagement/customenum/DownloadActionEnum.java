package xyz.dowob.filemanagement.customenum;

/**
 * 檔案下載行為管理枚舉，控制檔案下載和預覽的不同機制。
 *
 * <p>此枚舉類型提供一個靈活的機制，用於管理不同類型的檔案存取行為。預設為預覽模式，並支援自動偵測下載類型。</p>
 *
 * <p>提供了一個智能的機制，可以將不同的存取行為轉換為對應的模式，方便不同狀態的檔案操作。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

public enum DownloadActionEnum {
    /**
     * 預覽模式，用於避免完整下載且只需展示檔案內容的場景。
     */
    PREVIEW,

    /**
     * 下載模式，用於將檔案完整地下載到客戶端的場景。
     */
    DOWNLOAD;

    /**
     * 根據輸入名稱智能轉換為對應的下載行為枚舉，提供彈性的類型識別機制。
     *
     * <p>此方法支援不區分大小寫的枚舉值匹配，增強了方法的容錯性和使用靈活性。
     * 若無法匹配任何已知枚舉值，將預設回傳 {@link DownloadActionEnum#PREVIEW} 模式。</p>
     *
     * @param name 待轉換的枚舉名稱字串
     *
     * @return 匹配成功的下載行為枚舉，預設為預覽模式
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
