package xyz.dowob.filemanagement.convert;

import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName EntityByteBooleanMapper
 * @create 2025/1/23
 * @Version 1.0
 **/

public class EntityByteBooleanMapper {
    @ReadingConverter
    public static class ByteToBooleanConverter implements Converter<Byte, Boolean> {
        @Override
        public Boolean convert(@NonNull Byte source) {
            return source != 0;
        }
    }

    @WritingConverter
    public static class BooleanToByteConverter implements Converter<Boolean, Byte> {
        @Override
        public Byte convert(@NonNull Boolean source) {
            return (byte) (source ? 1 : 0);
        }
    }
}
