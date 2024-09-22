package xyz.dowob.filemanagement.convert;

import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * 用於將實體類中的Byte類型轉換為Boolean類型的轉換器
 * MySQL中以TINYINT(1)類型存儲Boolean類型，1表示true，0表示false，在此進行轉換
 * @author yuan
 * @program FileManagement
 * @ClassName EntityByteBooleanMapper
 * @create 2025/1/23
 * @Version 1.0
 **/

public class EntityByteBooleanMapper {
    /**
     * 將Byte類型轉換為Boolean類型的轉換器
     */
    @ReadingConverter
    public static class ByteToBooleanConverter implements Converter<Byte, Boolean> {
        @Override
        public Boolean convert(@NonNull Byte source) {
            return source != 0;
        }
    }


    /**
     * 將Boolean類型轉換為Byte類型的轉換器
     */
    @WritingConverter
    public static class BooleanToByteConverter implements Converter<Boolean, Byte> {
        @Override
        public Byte convert(@NonNull Boolean source) {
            return (byte) (source ? 1 : 0);
        }
    }
}
