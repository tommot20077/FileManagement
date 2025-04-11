package xyz.dowob.filemanagement.customenum;

import lombok.Getter;

/**
 * 用於定義用戶文件列表排序類型，適用於 {@link xyz.dowob.filemanagement.data.file.dto.UserFileListDTO} 的排序
 * 可以根據不同的排序類型進行相應的排序，若後續有新的排序類型，可以指定排序的順序
 * 排序類型如下：
 * 1. {@link #FIRST}：最前面
 * 2. {@link #FOLDER}：資料夾
 * 3. {@link #ONLINE_DOCUMENT}：線上文件
 * 4. {@link #GENERAL_FILE}：一般檔案
 * 5. {@link #LAST}：最後面
 *
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserFileListOrderEnum
 * @create 2025/3/7
 * @Version 1.0
 **/
@Getter
public enum UserFileListOrderEnum {
    /**
     * 最前面
     */
    FIRST(Integer.MIN_VALUE),

    /**
     * 資料夾
     */
    FOLDER,

    /**
     * 線上文件
     */
    ONLINE_DOCUMENT,

    /**
     * 一般檔案
     */
    GENERAL_FILE,

    /**
     * 最後面
     */
    LAST(Integer.MAX_VALUE),
    ;

    /**
     * 排序
     */
    private final Integer order;

    /**
     * 用於定義用戶文件列表排序類型的排序
     *
     * @param order 排序
     */
    UserFileListOrderEnum(Integer order) {
        this.order = order;
    }


    /**
     * 根據枚舉類的順序來定義排序
     */
    UserFileListOrderEnum() {
        this.order = this.ordinal() * 100;
    }
}
