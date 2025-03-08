package xyz.dowob.filemanagement.customenum;

import lombok.Getter;

/**
 * 用於定義用戶文件列表排序類型，適用於 {@link xyz.dowob.filemanagement.data.file.dto.UserFileListDTO} 的排序
 * 可以根據不同的排序類型進行相應的排序，若後續有新的排序類型，可以指定排序的順序
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserFileListOrderEnum
 * @create 2025/3/7
 * @Version 1.0
 **/
@Getter
public enum UserFileListOrderEnum {
    FIRST(Integer.MIN_VALUE),
    FOLDER,
    ONLINE_DOCUMENT,
    GENERAL_FILE,
    LAST(Integer.MAX_VALUE),
    ;
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
