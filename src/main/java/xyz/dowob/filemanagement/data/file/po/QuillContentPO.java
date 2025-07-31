package xyz.dowob.filemanagement.data.file.po;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Quill 編輯器內容持久化對象，封裝 Quill 編輯器的原生資料結構。
 * 用於儲存和處理 Quill 編輯器的 Delta 格式資料，支援直接序列化和反序列化。
 *
 * <p>此類別提供與 Quill 編輯器的直接相容性，保持原始資料結構不變。
 * 使用 @JsonIgnoreProperties 防止未知屬性造成的反序列化錯誤。
 * Delta 格式包含編輯器的所有操作和格式資訊。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
public class QuillContentPO {
    /**
     * Quill Delta 操作容器，包含編輯器的所有操作列表。
     * Delta 是 Quill 編輯器的核心資料結構，表示文本內容和格式資訊。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        public List<Operation> delta;
    }


    /**
     * Quill 單個操作元素，封裝文本插入和格式屬性。
     * 每個 Operation 代表一個文本片段和其相關的樣式資訊。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Operation {
        public String insert;
        public Map<String, Object> attributes;
    }
}
