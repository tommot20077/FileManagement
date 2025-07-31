package xyz.dowob.filemanagement.convert;

import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * 基於 Spring Data 的 Byte 與 Boolean 型別互相轉換器。提供資料庫 TINYINT(1) 欄位與 Java 布林值之間的雙向轉換。
 * 
 * <p>讀取轉換器將非零值轉換為 true，零值轉換為 false。寫入轉換器將 true 轉換為 1，false 轉換為 0。
 * 適用於 MySQL 中使用 TINYINT(1) 儲存布林值的場景。</p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

public class EntityByteBooleanMapper {
    /**
     * 將位元組型別轉換為布林值的讀取轉換器。
     *
     * <p>實現 Spring Data 的讀取轉換介面，用於從資料庫讀取 TINYINT(1) 欄位值並轉換為 Java Boolean 型別。
     * 轉換規則：非零值（包括負數）轉換為 true，零值轉換為 false。適用於 R2DBC 與 MySQL 的資料型別對應。</p>
     *
     * @author yuan
     * @version 1.0
     * @since 1.0
     * @see ReadingConverter
     * @see Converter
     */
    @ReadingConverter
    public static class ByteToBooleanConverter implements Converter<Byte, Boolean> {
        /**
         * 執行位元組到布林值的轉換。
         *
         * @param source 來源位元組值，不可為 null
         * @return 轉換結果：非零值返回 true，零值返回 false
         */
        @Override
        public Boolean convert(@NonNull Byte source) {
            return source != 0;
        }
    }


    /**
     * 將布林值轉換為位元組值的寫入轉換器。
     *
     * <p>實現 Spring Data 的寫入轉換介面，用於將 Java Boolean 型別轉換為資料庫 TINYINT(1) 欄位值。
     * 轉換規則：true 轉換為 1，false 轉換為 0。確保與 MySQL TINYINT(1) 欄位的正確對應。</p>
     *
     * @author yuan
     * @version 1.0
     * @since 1.0
     * @see WritingConverter
     * @see Converter
     */
    @WritingConverter
    public static class BooleanToByteConverter implements Converter<Boolean, Byte> {
        /**
         * 執行布林值到位元組的轉換。
         *
         * @param source 來源布林值，不可為 null
         * @return 轉換結果：true 返回 1，false 返回 0
         */
        @Override
        public Byte convert(@NonNull Boolean source) {
            return (byte) (source ? 1 : 0);
        }
    }
}
