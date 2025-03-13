package xyz.dowob.filemanagement.customenum;

import lombok.Getter;

import java.util.Objects;

/**
 * 預留的搜索 ID，用於標記特殊的搜索 ID
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ReservedSearchId
 * @create 2025/2/24
 * @Version 1.0
 **/
@Getter
public enum ReservedSearchIdEnum {
    /**
     * 獲取用戶根目錄文件列表
     */
    ROOT_FOLDER_ID(0L, null),

    /**
     * 獲取用戶所有文件列表(不包括已刪除的文件)
     */
    ALL_FILE_ID(-1L),

    /**
     * 獲取用戶星號文件列表
     */
    STAR_FILE_ID(-2L),

    /**
     * 獲取用戶最近文件列表
     */
    RECENT_FILE_ID(-3L),

    /**
     * 獲取用戶回收站文件列表
     */
    RECYCLE_FILE_ID(-4L),

    /**
     * 獲取用戶分享文件列表
     */
    SHARE_FILE_ID(-5L);

    /**
     * 預留的搜索 ID
     */
    private final Long[] id;


    ReservedSearchIdEnum(Long... id) {
        this.id = id;
    }

    /**
     * 獲取預留的搜索 ID，此方法返回主要代表的 ID
     *
     * @return 返回預留的搜索 ID
     */
    public Long getId() {
        return this.id[0];
    }

    /**
     * 判斷是否包含指定的 ID
     *
     * @param id 指定的 ID
     *
     * @return 返回是否包含指定的 ID
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
     * 格式化預留的搜索 ID
     *
     * @param id 預留的搜索 ID
     *
     * @return 返回格式化後的預留的搜索 ID
     */
    public static ReservedSearchIdEnum format(Long id) {
        for (ReservedSearchIdEnum reservedSearchIdEnum : ReservedSearchIdEnum.values()) {
            if (reservedSearchIdEnum.contain(id)) {
                return reservedSearchIdEnum;
            }
        }
        return null;
    }

}
