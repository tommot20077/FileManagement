package xyz.dowob.filemanagement.component.provider.providerImplement;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.TcpClient;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.provider.providerInterface.FileScanProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.exception.ProcessException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 檔案掃描提供者實現類，實現了 FileScanProvider 接口，其定義了掃描檔案的基本方法
 * 本實現類根據接口方法對於檔案掃描的要求，使用了 Netty 的 TCP 客戶端來連接到檢查的服務器，並將檔案數據發送進行掃描
 * 本實現是基於 ClamAV 的掃描實現，使用了 ClamAV 的 zINSTREAM 協議來進行檔案掃描
 * 若採用其他掃描服務器，則需要根據其協議進行相應的修改
 * 另外，這個實現類還使用了 Spring 的 @ConditionalOnProperty 註解來根據配置文件中的屬性來決定是否啟用檔案掃描功能
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileScanProviderImpl
 * @create 2025/4/28
 * @Version 1.0
 **/
@Component
@RecordLevel(LogLevelEnum.DEBUG)
@ConditionalOnProperty(prefix = "file.security", name = "enable-security-check", havingValue = "true", matchIfMissing = true)
public class FileScanProviderImpl implements FileScanProvider {
    /**
     * FileProperties 用於操作檔案上傳相關配置的類
     */
    private final FileProperties fileProperties;

    /**
     * ConnectionProvider 用於提供連接的類
     */
    private final ConnectionProvider connectionProvider;

    /**
     * 檔案掃描的超時時間
     */
    private final Duration timeout;

    /**
     * 檔案掃描提供者實現類的構造函數
     *
     * @param fileProperties FileProperties 用於操作檔案上傳相關配置的類
     */
    public FileScanProviderImpl(FileProperties fileProperties, ConnectionProvider connectionProvider) {
        this.fileProperties = fileProperties;
        this.connectionProvider = connectionProvider;
        Assert.isTrue(fileProperties.getSecurity().getTimeout().isPositive(), "設定檔案掃描的超時時間必須大於0");
        Assert.notNull(fileProperties.getSecurity().getHost(), "設定檔案掃描的服務器地址不能為空");
        Assert.isTrue(fileProperties.getSecurity().getPort() > 0, "設定檔案掃描的服務器端口必須大於0");
        this.timeout = fileProperties.getSecurity().getTimeout();
    }

    /**
     * 傳輸檔案的 ByteBuf 數組流進行掃描並返回掃描結果
     *
     * @param byteBufFlux ByteBuf 數組流
     * @param totalSize   檔案的總大小
     *
     * @return Mono<scanResult> 掃描結果
     */
    @Override
    public Mono<scanResult> scanByteBuf(Flux<ByteBuf> byteBufFlux, long totalSize) {
        return validateFileSize(totalSize).switchIfEmpty(Mono.defer(() -> sendToScanServer(byteBufFlux)));
    }

    /**
     * 傳輸檔案的 byte[] 數組流進行轉換並返回掃描結果
     *
     * @param byteFlux byte[] 數組流
     *
     * @return Mono<scanResult> 掃描結果
     */
    @Override
    public Mono<scanResult> scanBytes(Flux<byte[]> byteFlux) {
        AtomicLong totalSize = new AtomicLong(0);
        Flux<ByteBuf> byteBufFlux = byteFlux.map(bytes -> {
            totalSize.addAndGet(bytes.length);
            return Unpooled.wrappedBuffer(bytes);
        });
        return scanByteBuf(byteBufFlux, totalSize.get());
    }


    /**
     * 傳輸檔案的 DataBuffer 數組流進行轉換並返回掃描結果
     *
     * @param dataBufferFlux DataBuffer 數組流
     *
     * @return Mono<scanResult> 掃描結果
     */
    @Override
    public Mono<scanResult> scanDataBuffer(Flux<DataBuffer> dataBufferFlux) {
        AtomicLong totalSize = new AtomicLong(0);
        Flux<ByteBuf> byteBufFlux = dataBufferFlux.map(dataBuffer -> {
            totalSize.addAndGet(dataBuffer.readableByteCount());
            ByteBuf byteBuf = Unpooled.buffer(dataBuffer.readableByteCount());
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            byteBuf.writeBytes(bytes);
            DataBufferUtils.release(dataBuffer);
            return byteBuf;
        });
        return scanByteBuf(byteBufFlux, totalSize.get());
    }


    /**
     * 處理連接，並將檔案數據發送到掃描服務器
     * 透過接收一個函數來處理連接，這個函數會在連接建立後被調用
     * 這個函數會將檔案數據發送到掃描服務器，然後接收掃描結果
     *
     * @param byteBufFlux ByteBuf 數組流
     *
     * @return Mono<scanResult> 掃描結果
     */
    private Mono<scanResult> sendToScanServer(Flux<ByteBuf> byteBufFlux) {
        return TcpClient
                .create(connectionProvider)
                .host(fileProperties.getSecurity().getHost())
                .port(fileProperties.getSecurity().getPort())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.AUTO_CLOSE, true)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler((int) timeout.toSeconds()));
                    connection.addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler((int) timeout.toSeconds()));
                })
                .connect()
                .onErrorResume(ex -> Mono.error(new ProcessException(ProcessException.ErrorCode.CONNECT_SCAN_SERVER_FAILED, ex)))
                .flatMap(connection -> {
                    Mono<Void> sendCommand = connection.outbound().sendString(Mono.just("zINSTREAM\0")).then();
                    Mono<Void> sendData = connection.outbound().send(byteBufFlux.map(byteBuf -> {
                        ByteBuf lengthPrefix = Unpooled.buffer(4).writeInt(byteBuf.readableBytes());
                        return Unpooled.wrappedBuffer(lengthPrefix, byteBuf);
                    })).then();
                    Mono<Void> sendEnd = connection.outbound().sendByteArray(Mono.just(new byte[]{0, 0, 0, 0})).then();
                    Mono<scanResult> receiveResponse = connection
                            .inbound()
                            .receive()
                            .asString(StandardCharsets.UTF_8)
                            .next()
                            .timeout(timeout)
                            .flatMap(response -> {
                                response = response.trim();
                                if (response.contains("OK")) {
                                    return Mono.just(new scanResult(true, "檔案檢測通過"));
                                } else if (response.contains("FOUND")) {
                                    return Mono.just(new scanResult(false, "檔案檢測到病毒: " + response));
                                }
                                return Mono.error(new ProcessException(ProcessException.ErrorCode.RESOLVE_SCAN_FAILED, response));
                            });
                    return sendCommand.then(sendData).then(sendEnd).then(receiveResponse).doFinally(signal -> connection.dispose());
                });
    }


    /**
     * 驗證檔案大小是否符合要求
     *
     * @param totalSize 檔案的總大小
     *
     * @return Mono<scanResult> 掃描結果
     */
    private Mono<scanResult> validateFileSize(long totalSize) {
        long minSize = fileProperties.getSecurity().getMinFileSize().toBytes();
        long maxSize = fileProperties.getSecurity().getMaxFileSize().toBytes();
        if ((minSize > 0 && totalSize < minSize) || (maxSize > 0 && totalSize > maxSize)) {
            return Mono.just(new scanResult(true, "不符合掃描檔案大小限制，跳過掃描"));
        }
        return Mono.empty();
    }
}
