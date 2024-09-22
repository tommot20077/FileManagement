package xyz.dowob.filemanagement.convert;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Redis 字符串編碼器，將字符串編碼為字節數組，並將字節數組解碼為字符串
 * 本類為 {@link RedisCodec} 的實現類，實現了編碼和解碼的接口
 * 在 Bucket4j 中會強制需要使用 byte[] 作為值的類型，而在 IP 請求限制器中，使用了 IP 地址(String)作為請求限制的鍵
 * 因此實現了這個編碼器來將字符串編碼為字節數組
 *
 * @author yuan
 * @program FileManagement
 * @ClassName StringByteCodeMapper
 * @create 2025/4/21
 * @Version 1.0
 **/

public class StringByteCodeMapper implements RedisCodec<String, byte[]> {
    /**
     * 編碼器的名稱
     *
     * @param byteBuffer 字節緩衝區
     *
     * @return 編碼器的名稱
     */
    @Override
    public String decodeKey(ByteBuffer byteBuffer) {
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    /**
     * 解碼器的名稱
     *
     * @param byteBuffer 字節緩衝區
     *
     * @return 解碼器的名稱
     */
    @Override
    public byte[] decodeValue(ByteBuffer byteBuffer) {
        byte[] array = new byte[byteBuffer.remaining()];
        byteBuffer.get(array);
        return array;
    }

    /**
     * 編碼器的名稱
     *
     * @param s 字符串
     *
     * @return 編碼器的名稱
     */
    @Override
    public ByteBuffer encodeKey(String s) {
        return StandardCharsets.UTF_8.encode(s);
    }

    /**
     * 解碼器的名稱
     *
     * @param bytes 字節數組
     *
     * @return 解碼器的名稱
     */
    @Override
    public ByteBuffer encodeValue(byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }
}
