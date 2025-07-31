package xyz.dowob.filemanagement.component.provider.providerInterface;

import io.netty.buffer.ByteBuf;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 非阻塞檔案安全掃描提供者介面，提供多種資料格式的安全掃描機制。
 *
 * <p>本介面基於 WebFlux 的反應式設計，支援非阻塞、高效能的檔案安全機制。
 * 可以處理各種不同格式的檔案流，如 ByteBuf、byte[]、DataBuffer 等。</p>
 *
 * <p>主要特性：
 * <ul>
 *   <li>支援非同步檔案掃描</li>
 *   <li>可以捲機多種資料格式</li>
 *   <li>高效能且可限制應用程式的效能流量</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

public interface FileScanProvider {

    /**
     * 傳輸檔案的 ByteBuf 數組流進行掃描並回傳掃描結果
     *
     * @param byteBufFlux ByteBuf 數組流
     * @param totalSize   檔案的總大小
     *
     * @return Mono<scanResult> 掃描結果
     */
    Mono<scanResult> scanByteBuf(Flux<ByteBuf> byteBufFlux, long totalSize);

    /**
     * 傳輸檔案的 byte[] 數組流進行掃描並回傳掃描結果
     *
     * @param byteFlux byte[] 數組流
     *
     * @return Mono<scanResult> 掃描結果
     */
    default Mono<scanResult> scanBytes(Flux<byte[]> byteFlux) {
        return Mono.empty();
    }


    /**
     * 傳輸檔案的 DataBuffer 數組流進行掃描並回傳掃描結果
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
