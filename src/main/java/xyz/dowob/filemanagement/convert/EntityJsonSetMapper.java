package xyz.dowob.filemanagement.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.util.Set;

/**
 * 此類用於將 Set<Long> 與 JSON 進行轉換
 * Entity中存在 Set<Long> 欄位時，將其轉換為 JSON 進行存儲
 * 進行查詢時，將 JSON 轉換為 Set<Long>
 *
 * @author yuan
 * @program FileManagement
 * @ClassName EntitySetMapper
 * @create 2025/1/21
 * @Version 1.0
 **/

public class EntityJsonSetMapper {
    /**
     * ObjectMapper 用於進行 JSON 與對象之間的轉換
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * SetConverter 用於將 Set<Long> 轉換為 JSON，進行存儲
     * 使用 @WritingConverter 進行此轉換器的註冊
     */
    @WritingConverter
    public static class SetConverter implements Converter<Set<Long>, String> {
        /**
         * 將 Set<Long> 轉換為 JSON
         *
         * @param source Set<Long> 源對象
         *
         * @return String 返回 JSON 字符串
         */
        @Override
        public String convert(@NonNull Set<Long> source) {
            try {
                return objectMapper.writeValueAsString(source);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("無法將 Set<Long> 轉換為 JSON", e);
            }
        }
    }

    /**
     * JsonConverter 用於將 JSON 轉換為 Set<Long>
     * 使用 @WritingConverter 進行此轉換器的註冊
     */
    public static class JsonConverter implements Converter<String, Set<Long>> {
        /**
         * 將 JSON 轉換為 Set<Long>
         *
         * @param source String 源字符串
         *
         * @return Set<Long> 返回 Set<Long> 對象
         */
        @Override
        public Set<Long> convert(@NonNull String source) {
            try {
                return objectMapper.readValue(source, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException("無法將 JSON 轉換為 Set<Long>", e);
            }
        }
    }


}
