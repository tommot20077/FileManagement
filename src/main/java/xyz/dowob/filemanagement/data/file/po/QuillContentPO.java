package xyz.dowob.filemanagement.data.file.po;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * QuillContentPO 類，用於存儲 Quill 編輯器的內容
 *
 * @author yuan
 * @program FileManagement
 * @ClassName QuillContentPO
 * @create 2025/4/8
 * @Version 1.0
 **/
public class QuillContentPO {
    /**
     * Delta 類，其包含了 Quill 的所有操作列表
     * 這些操作是 Quill 編輯器的內部表示，包含了文本的插入、刪除、格式化等操作
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        public List<Operation> delta;
    }


    /**
     * Operation 類，其包含了 Quill 的單個操作
     * 這些操作是 Quill 編輯器的內部表示，包含了文本的插入、語句的屬性等
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Operation {
        public String insert;
        public Map<String, Object> attributes;
    }
}
