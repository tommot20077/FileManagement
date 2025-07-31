package xyz.dowob.filemanagement.convert;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 實現 Redis Lettuce 客戶端的 {@link RedisCodec} 編碼器，提供 String 鍵與 byte[] 值之間的序列化與反序列化操作。
 * 
 * <p>鍵使用 UTF-8 字符編碼在 ByteBuffer 與 String 之間進行轉換，確保多國語言字符的正確處理。
 * 值直接操作原始位元組陣列，避免額外的序列化開銷。此實現特別適用於需要將 IP 地址等字串鍵
 * 與原始位元組資料配對的場景，如 Bucket4j 分散式限流器的令牌桶狀態存儲。
 * 
 * <p>線程安全：此類無狀態，可安全地在多線程環境中使用。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see RedisCodec
 */

public class StringByteCodeMapper implements RedisCodec<String, byte[]> {
    /**
     * 將 UTF-8 編碼的 ByteBuffer 解碼為字串鍵。
     * 
     * <p>將 Redis 存儲的位元組緩衝區轉換回原始字串形式，使用 UTF-8 字符集確保國際化字符的正確解碼。
     * ByteBuffer 的 position 和 limit 會在解碼過程中被調整。
     *
     * @param byteBuffer 包含 UTF-8 編碼字串的位元組緩衝區，不可為 null
     * @return 解碼後的字串鍵，保證不為 null
     */
    @Override
    public String decodeKey(ByteBuffer byteBuffer) {
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    /**
     * 將 ByteBuffer 解碼為原始位元組陣列值。
     * 
     * <p>從 Redis 存儲的 ByteBuffer 中提取所有剩餘位元組到新的陣列中。此方法不執行任何字符編碼轉換，
     * 直接複製原始位元組資料。ByteBuffer 的 position 會移動到 limit 位置。
     *
     * @param byteBuffer 包含原始位元組的緩衝區，不可為 null
     * @return 解碼後的位元組陣列，長度等於 byteBuffer.remaining()
     */
    @Override
    public byte[] decodeValue(ByteBuffer byteBuffer) {
        byte[] array = new byte[byteBuffer.remaining()];
        byteBuffer.get(array);
        return array;
    }

    /**
     * 將字串鍵編碼為 UTF-8 的 ByteBuffer。
     * 
     * <p>將 Java 字串轉換為 UTF-8 編碼的 ByteBuffer，以便存儲到 Redis 中。
     * 編碼過程確保所有 Unicode 字符都能正確表示為位元組序列。
     *
     * @param s 要編碼的原始字串鍵，不可為 null
     * @return UTF-8 編碼的 ByteBuffer，position 為 0，limit 為編碼後的位元組長度
     */
    @Override
    public ByteBuffer encodeKey(String s) {
        return StandardCharsets.UTF_8.encode(s);
    }

    /**
     * 將位元組陣列值包裝為 ByteBuffer。
     * 
     * <p>將 Java 位元組陣列直接包裝為 ByteBuffer，不進行資料複製。包裝後的 ByteBuffer
     * 與原始陣列共享相同的資料，position 為 0，limit 和 capacity 為陣列長度。
     *
     * @param bytes 要寫入的原始位元組陣列，不可為 null
     * @return 包裝後的 ByteBuffer，與原始陣列共享資料
     */
    @Override
    public ByteBuffer encodeValue(byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }
}
