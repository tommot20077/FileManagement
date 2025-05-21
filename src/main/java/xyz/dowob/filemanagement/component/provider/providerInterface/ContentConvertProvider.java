package xyz.dowob.filemanagement.component.provider.providerInterface;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.factory.config.ConvertConfig;
import xyz.dowob.filemanagement.customenum.ConvertProviderEnum;

import java.io.InputStream;

/**
 * 內容轉換器接口，提供將內容轉換為字節數組或數據緩衝區的功能
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ContentConvertProvider
 * @create 2025/4/6
 * @Version 1.0
 **/

public interface ContentConvertProvider {
    /**
     * 將內容轉換為輸入流
     *
     * @param content 要轉換的內容
     *
     * @return 轉換後的輸入流
     */
    default Mono<InputStream> convertToInputStream(String content) {
        return Mono.empty();
    }


    /**
     * 將內容轉換為數據緩衝區
     *
     * @param content 要轉換的內容
     *
     * @return 轉換後的數據緩衝區
     */
    default Mono<DataBufferRecord> convertToDataBuffer(String content) {
        return Mono.empty();
    }


    /**
     * 數據緩衝區記錄類，用於存儲數據緩衝區和大小
     *
     * @param dataBuffer 數據緩衝區
     * @param size       大小
     */
    record DataBufferRecord(Flux<DataBuffer> dataBuffer, long size) {
    }

    /**
     * 獲取轉換器類型
     *
     * @return 轉換器類型
     */
    ConvertProviderEnum getType();

    /**
     * 獲取轉換設定
     * 預設為不支援獲取，而交由實作類別來決定
     *
     * @return 轉換設定
     */
    default ConvertConfig getConvertConfig() {
        return null;
    }
}
