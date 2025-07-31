package xyz.dowob.filemanagement.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.util.Set;

/**
 * 基於 Spring Data 的 Set&lt;Long&gt; 與 JSON 字串互相轉換器。此類別提供兩個靜態內部類別實現雙向資料轉換，
 * 使用 Jackson ObjectMapper 處理序列化與反序列化操作。
 * 
 * <p>SetConverter 負責將 Set&lt;Long&gt; 集合轉換為 JSON 字串格式以進行資料庫儲存。
 * JsonConverter 負責將儲存的 JSON 字串還原為 Set&lt;Long&gt; 集合物件。兩個轉換器皆採用 @NonNull 參數驗證，
 * 確保輸入資料的有效性。轉換過程中發生的 JsonProcessingException 將被包裝為 RuntimeException 擲出。
 * 
 * <p>此轉換器設計符合 Spring Data 的轉換器模式，透過 @WritingConverter 和 @ReadingConverter 註解
 * 自動整合到 Spring Data 的資料存取層中。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

public class EntityJsonSetMapper {
    /**
     * Jackson ObjectMapper 實例，用於執行 JSON 序列化與反序列化操作。
     * 此靜態實例在類別載入時初始化，並於所有轉換操作中重複使用以提升效能。
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 將 Set&lt;Long&gt; 集合轉換為 JSON 字串的寫入轉換器。實現 Spring Data 的 Converter 介面，
     * 在資料寫入資料庫時自動執行轉換操作。
     * 
     * <p>此轉換器將 Long 類型的集合序列化為 JSON 陣列格式，例如將 {1, 2, 3} 轉換為 "[1,2,3]"。
     * 轉換過程採用 Jackson ObjectMapper 進行序列化，確保與標準 JSON 格式相容。
     */
    @WritingConverter
    public static class SetConverter implements Converter<Set<Long>, String> {
        /**
         * 將指定的 Set&lt;Long&gt; 集合轉換為 JSON 字串格式。
         * 
         * @param source 要轉換的 Long 集合，不可為 null
         * @return 轉換後的 JSON 字串表示
         * @throws RuntimeException 當 JSON 序列化失敗時擲出，包裝原始的 JsonProcessingException
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
     * 將 JSON 字串轉換為 Set&lt;Long&gt; 集合的讀取轉換器。實現 Spring Data 的 Converter 介面，
     * 在資料從資料庫讀取時自動執行轉換操作。
     * 
     * <p>此轉換器將 JSON 陣列格式的字串反序列化為 Long 類型的集合，例如將 "[1,2,3]" 轉換為 {1, 2, 3}。
     * 轉換過程採用 Jackson ObjectMapper 搭配 TypeReference 進行型別安全的反序列化操作。
     */
    @ReadingConverter
    public static class JsonConverter implements Converter<String, Set<Long>> {
        /**
         * 將指定的 JSON 字串轉換為 Set&lt;Long&gt; 集合。
         * 
         * @param source 要轉換的 JSON 字串，不可為 null
         * @return 反序列化後的 Long 集合
         * @throws RuntimeException 當 JSON 反序列化失敗時擲出，包裝原始的 JsonProcessingException
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
