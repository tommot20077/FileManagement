package xyz.dowob.filemanagement.customenum;

import lombok.Getter;

/**
 * 使用者檔案列表排序優先級列舉。
 * <p>定義 {@link xyz.dowob.filemanagement.data.file.dto.UserFileListDTO} 清單中不同檔案類型的顯示排序優先級。
 * 系統會按照指定的排序值將檔案進行分類排列，確保資料夾優先顯示，其次是線上檔案，最後是一般檔案。
 * <p>排序優先級：
 * <ol>
 * <li>{@link #FIRST} - 最高優先級，用於特殊情況</li>
 * <li>{@link #FOLDER} - 資料夾類型</li>
 * <li>{@link #ONLINE_DOCUMENT} - 線上編輯檔案</li>
 * <li>{@link #GENERAL_FILE} - 一般檔案類型</li>
 * <li>{@link #LAST} - 最低優先級，用於特殊情況</li>
 * </ol>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
public enum UserFileListOrderEnum {
    /**
     * 最高優先級排序。
     * 用於特殊檔案或系統檔案，具有最小整數值以確保在清單頂部顯示。
     */
    FIRST(Integer.MIN_VALUE),

    /**
     * 資料夾類型排序。
     * 資料夾在檔案列表中優先顯示，方便使用者快速導航和組織檔案。
     */
    FOLDER,

    /**
     * 線上編輯檔案排序。
     * 線上可編輯的檔案在資料夾之後顯示，但優先於一般檔案。
     */
    ONLINE_DOCUMENT,

    /**
     * 一般檔案排序。
     * 一般檔案在檔案列表中排在資料夾和線上檔案之後顯示。
     */
    GENERAL_FILE,

    /**
     * 最低優先級排序。
     * 用於特殊檔案或系統檔案，具有最大整數值以確保在清單底部顯示。
     */
    LAST(Integer.MAX_VALUE),
    ;

    /**
     * 排序優先級數值。
     * 數值越小表示優先級越高，用於檔案列表的排序排列。
     */
    private final Integer order;


    /**
     * 使用指定優先級數值建構排序類型。
     * 用於需要特定排序值的列舉常數，如 FIRST 和 LAST。
     *
     * @param order 排序優先級數值
     */
    UserFileListOrderEnum(Integer order) {
        this.order = order;
    }


    /**
     * 使用列舉常數的序號位置自動計算優先級。
     * 根據在列舉中的定義順序自動產生排序值，每個類型間隔 100 個單位。
     */
    UserFileListOrderEnum() {
        this.order = this.ordinal() * 100;
    }


    /**
     * 根據檔案類型取得對應的排序優先級。
     * 系統會按照檔案類型分配適當的顯示優先級，確保檔案列表的有序顯示。
     * 若檔案類型為 null 或無法識別，會使用預設的備用排序值。
     *
     * @param fileEnum 檔案類型列舉，可為 null
     * @return 對應的排序優先級數值；若為 null 則回傳 LAST 的值；若無匹配則回傳 GENERAL_FILE 的值
     */
    public static int getOrder(FileEnum fileEnum) {
        if (fileEnum == null) {
            return LAST.getOrder();
        }

        for (UserFileListOrderEnum orderEnum : UserFileListOrderEnum.values()) {
            if (orderEnum.name().equals(fileEnum.name())) {
                return orderEnum.getOrder();
            }
        }
        return GENERAL_FILE.getOrder();
    }
}
