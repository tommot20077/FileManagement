package xyz.dowob.filemanagement.customenum;

import lombok.Getter;

import java.util.Objects;

/**
 * 檔案系統中預留的特殊搜索識別碼列舉。
 * <p>定義系統內建的檔案搜索和分類識別碼，用於標識不同類型的檔案檢視模式，
 * 包括根目錄、全部檔案、收藏檔案、最近檔案、回收站和分享檔案等特殊搜索場景。
 * 每個列舉值對應特定的檔案分類和過濾邏輯。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
public enum ReservedSearchIdEnum {
    /**
     * 使用者根目錄檔案列表識別碼。
     * 用於取得使用者檔案系統的根層級目錄內容。
     */
    ROOT_FOLDER_ID(0L, null),

    /**
     * 全部檔案列表識別碼。
     * 取得使用者擁有的所有檔案，排除已刪除的檔案。
     */
    ALL_FILE_ID(-1L),

    /**
     * 收藏檔案列表識別碼。
     * 取得使用者標記為收藏或星號的檔案清單。
     */
    STAR_FILE_ID(-2L),

    /**
     * 最近檔案列表識別碼。
     * 取得使用者最近存取或修改的檔案清單。
     */
    RECENT_FILE_ID(-3L),

    /**
     * 回收站檔案列表識別碼。
     * 取得使用者已刪除但尚未永久移除的檔案清單。
     */
    RECYCLE_FILE_ID(-4L),

    /**
     * 分享檔案列表識別碼。
     * 取得使用者已分享給其他使用者的檔案清單。
     */
    SHARE_FILE_ID(-5L);

    /**
     * 預留搜索識別碼陣列。
     * 儲存該列舉值對應的一組識別碼，支援多個識別碼對應同一搜索類型。
     */
    private final Long[] id;


    ReservedSearchIdEnum(Long... id) {
        this.id = id;
    }

    /**
     * 根據識別碼查找對應的預留搜索類型。
     * 遍歷所有列舉值，檢查指定識別碼是否屬於某個預留搜索類型。
     *
     * @param id 待查找的檔案搜索識別碼
     * @return 匹配的預留搜索類型列舉，若無匹配則回傳 null
     */
    public static ReservedSearchIdEnum format(Long id) {
        for (ReservedSearchIdEnum reservedSearchIdEnum : ReservedSearchIdEnum.values()) {
            if (reservedSearchIdEnum.contain(id)) {
                return reservedSearchIdEnum;
            }
        }
        return null;
    }

    /**
     * 檢查當前列舉值是否包含指定的識別碼。
     * 在當前列舉值的識別碼陣列中查找是否存在指定的識別碼。
     *
     * @param id 待檢查的識別碼
     * @return 若包含指定識別碼則回傳 true，否則回傳 false
     */
    public boolean contain(Long id) {
        for (Long reservedId : this.id) {
            if (Objects.equals(reservedId, id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 取得預留搜索的主要識別碼。
     * 回傳當前列舉值識別碼陣列中的第一個識別碼，作為該搜索類型的主要代表識別碼。
     *
     * @return 主要搜索識別碼
     */
    public Long getId() {
        return this.id[0];
    }

}
