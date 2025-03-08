package xyz.dowob.filemanagement.data.file.po;

import lombok.Data;

import java.util.HashMap;
import java.util.Objects;

/**
 * 用於映射分享用戶的PO類，用於檔案修改時快速查找用戶以及相對應的操作類型
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ShareUserEditPO
 * @create 2025/3/5
 * @Version 1.0
 **/
@Data
public class ShareUserEditPO {
    /**
     * 用戶ID
     */
    private Long UserId;

    /**
     * 編輯類型
     */
    private EditTypeEnum editType;

    public enum EditTypeEnum {
        /**
         * 添加
         */
        ADD,
        /**
         * 更新
         */
        UPDATE,
        /**
         * 刪除
         */
        REMOVE
    }

    /**
     * 重寫toString方法
     *
     * @return 返回對象的字符串形式
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("UserId", UserId);
        map.put("editType", editType);
        return map.toString();
    }

    /**
     * 重寫equals方法
     *
     * @param o 要比較的對象
     *
     * @return 返回比較結果
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
        return UserId.equals(that.UserId);
    }

    /**
     * 重寫hashCode方法
     *
     * @return 返回對象的hashCode
     */
    @Override
    public int hashCode() {
        return Objects.hash(UserId);
    }
}
