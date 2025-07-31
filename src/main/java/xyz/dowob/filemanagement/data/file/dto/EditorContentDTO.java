package xyz.dowob.filemanagement.data.file.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 編輯器內容資料傳輸對象，封裝結構化文本編輯器的內容資料。
 * 用於傳輸和儲存富文本編輯器（如 Quill）的 Delta 格式內容。
 *
 * <p>此對象支援結構化的文本內容表示，包括文本格式、屬性和樣式資訊。
 * Delta 格式是一種輕量級的文本表示，支援多種文本操作和格式化。
 * 提供內容空值檢查和比較功能。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditorContentDTO {
    /**
     * Delta 格式的編輯器內容列表，包含所有文本操作和格式資訊
     */
    private List<DeltaDTO> delta;

    /**
     * 檢查編輯器內容是否為空。
     *
     * @return 如果 Delta 列表為 null 或空集合，則回傳 true
     */
    @JsonIgnore
    public boolean isEmpty() {
        return delta == null || delta.isEmpty();
    }

    /**
     * 生成對象的哈希碼。
     *
     * @return 基於 Delta 列表的哈希碼
     */
    @Override
    public int hashCode() {
        return Objects.hash(delta);
    }

    /**
     * 比較兩個編輯器內容對象是否相等。
     *
     * @param o 要比較的對象
     * @return 如果 Delta 列表內容相同，則回傳 true
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
     * Delta 操作單元，封裝單個文本操作和其屬性。
     * 每個 Delta 單元代表一個文本插入操作和其相關的格式屬性。
     */
    @Data
    public static class DeltaDTO {
        /**
         * 要插入的文本內容，可以是純文本或特殊格式內容
         */
        private String insert;

        /**
         * 文本格式屬性，包含字體、大小、顏色等樣式資訊
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Map<String, Object> attributes;
    }
}
