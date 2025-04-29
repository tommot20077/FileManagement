package xyz.dowob.filemanagement.component.provider.providerInterface;

import io.netty.buffer.ByteBuf;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 檔案掃描提供者接口，定義了掃描檔案的基本方法
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileScanProvider
 * @create 2025/4/28
 * @Version 1.0
 **/

public interface FileScanProvider {

    /**
     * 傳輸檔案的 ByteBuf 數組流進行掃描並返回掃描結果
     *
     * @param byteBufFlux ByteBuf 數組流
     * @param totalSize   檔案的總大小
     *
     * @return Mono<scanResult> 掃描結果
     */
    Mono<scanResult> scanByteBuf(Flux<ByteBuf> byteBufFlux, long totalSize);

    /**
     * 傳輸檔案的 byte[] 數組流進行掃描並返回掃描結果
     *
     * @param byteFlux byte[] 數組流
     *
     * @return Mono<scanResult> 掃描結果
     */
    default Mono<scanResult> scanBytes(Flux<byte[]> byteFlux) {
        return Mono.empty();
    }


    /**
     * 傳輸檔案的 DataBuffer 數組流進行掃描並返回掃描結果
     *
     * @param dataBufferFlux DataBuffer 數組流
     *
     * @return Mono<scanResult> 掃描結果
     */
    default Mono<scanResult> scanDataBuffer(Flux<DataBuffer> dataBufferFlux) {
        return Mono.empty();
    }


    /**
     * 掃描結果類，用於封裝掃描結果
     *
     * @param isSafe  掃描結果是否安全
     * @param message 掃描結果的訊息
     */
    record scanResult(Boolean isSafe, Object message) {
    }
}
