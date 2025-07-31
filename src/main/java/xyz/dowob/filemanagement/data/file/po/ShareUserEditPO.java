package xyz.dowob.filemanagement.data.file.po;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;

/**
 * 用於映射分享用戶的PO類，用於檔案修改時快速查找用戶以及相對應的操作類型
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 **/
@Data
public class ShareUserEditPO {
    /**
     * 用戶訊息
     */
    @NotNull(message = "用戶ID不能為空")
    private Long userId;

    /**
     * 編輯類型
     */
    @NotNull(message = "編輯類型不能為空")
    private EditTypeEnum editType;

    /**
     * 檔案ID
     */
    private Long fileId;

    /**
     * 編輯內容
     */
    private String editContent;

    /**
     * 編輯時間
     */
    private LocalDateTime editTime;

    /**
     * 權限
     */
    private String permission;


    /**
     * 重寫hashCode方法
     *
     * @return 回傳對象的hashCode
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }


    /**
     * 重寫equals方法
     *
     * @param o 要比較的對象
     *
     * @return 回傳比較結果
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShareUserEditPO that = (ShareUserEditPO) o;
        return userId.equals(that.userId);
    }


    /**
     * 重寫toString方法
     *
     * @return 回傳對象的字符串形式
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("UserId", userId);
        map.put("editType", editType);
        return map.toString();
    }


    public enum EditTypeEnum {
        /**
         * 添加新用戶
         */
        ADD,

        /**
         * 更新用戶狀態
         */
        UPDATE,

        /**
         * 刪除分享用戶
         */
        REMOVE;


        /**
         * 重寫fromString方法，用於根據key查找對應的枚舉類，不區分大小寫
         *
         * @param key 要查找的key
         *
         * @return 回傳查找結果
         */
        @JsonCreator
        public static EditTypeEnum fromString(String key) {
            for (EditTypeEnum e : EditTypeEnum.values()) {
                if (e.name().equalsIgnoreCase(key)) {
                    return e;
                }
            }
            return null;
        }
    }
}
