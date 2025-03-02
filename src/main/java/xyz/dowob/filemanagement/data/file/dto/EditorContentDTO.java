package xyz.dowob.filemanagement.data.file.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 編輯器內容 JSON 數據傳輸對象，用於封裝編輯器內容的 JSON 數據
 *
 * @author yuan
 * @program FileManagement
 * @ClassName EditorContentJsonDTO
 * @create 2025/2/12
 * @Version 1.0
 **/
@Data
@NoArgsConstructor
public class EditorContentDTO {
    /**
     * 編輯器內容的內容記錄
     */
    private List<DeltaDTO> delta;

    /**
     * 檢查此內容是否為空
     *
     * @return 如果內容為空，則返回 true，否則返回 false
     */
    @JsonIgnore
    public boolean isEmpty() {
        return delta == null || delta.isEmpty();
    }

    /**
     * 編輯器內容的內容記錄數據傳輸對象，用於封裝編輯器內容的內容記錄數據
     */
    @Data
    public static class DeltaDTO {
        /**
         * 編輯器內容的內容記錄
         */
        private String insert;

        /**
         * 編輯器內容的屬性記錄
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Map<String, Object> attributes;
    }

    /**
     * 重寫比較方法，判斷兩個對象是否相等
     *
     * @param o 要比較的對象
     *
     * @return 如果兩個對象相等，則返回 true，否則返回 false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EditorContentDTO that = (EditorContentDTO) o;

        return Objects.equals(delta, that.delta);
    }

    /**
     * 重寫哈希碼方法，返回對象的哈希碼
     *
     * @return 對象的哈希碼
     */
    @Override
    public int hashCode() {
        return Objects.hash(delta);
    }
}
