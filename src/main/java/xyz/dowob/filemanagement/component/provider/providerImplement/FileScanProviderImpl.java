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
 * 非同步檔案安全掃描提供者實現，透過反應式程式設計實現檔案安全檢查。
 *
 * <p>此類別實現了 {@link xyz.dowob.filemanagement.component.provider.providerInterface.FileScanProvider} 介面，
 * 主要基於 Netty TCP 客戶端和 ClamAV 的 zINSTREAM 協議來執行檔案安全掃描。</p>
 *
 * <p>技術特點：
 * <ul>
 *   <li>使用 Reactor Netty 進行非阻塞 TCP 連線</li>
 *   <li>支援多種檔案格式的安全掃描（ByteBuf、byte[]、DataBuffer）</li>
 *   <li>可設定的檔案大小限制</li>
 *   <li>彈性的服務器連線設定</li>
 * </ul>
 * </p>
 *
 * <p>設定特性：
 * <ul>
 *   <li>透過 @ConditionalOnProperty 根據設定動態啟用</li>
 *   <li>可自定義掃描服務器主機、埠和超時時間</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@RecordLevel(LogLevelEnum.DEBUG)
@ConditionalOnProperty(prefix = "file.security", name = "enable-security-check", havingValue = "true", matchIfMissing = true)
public class FileScanProviderImpl implements FileScanProvider {
    /**
     * 檔案安全設定屬性，用於載入和管理檔案上傳相關的安全設定。
     *
     * @see xyz.dowob.filemanagement.config.properties.FileProperties
     */
    private final FileProperties fileProperties;

    /**
     * Reactor Netty 連線提供者，管理非同步 TCP 連線的生命週期和資源。
     *
     * @see reactor.netty.resources.ConnectionProvider
     */
    private final ConnectionProvider connectionProvider;

    /**
     * 檔案安全掃描作業的超時時間設定，控制連線和響應等待的最大時間。
     *
     * @see java.time.Duration
     */
    private final Duration timeout;

    /**
     * 初始化檔案安全掃描提供者，載入必要的設定和連線資源。
     *
     * <p>進行嚴格的設定參數驗證，確保檔案掃描服務可正常運作：
     * <ul>
     *   <li>驗證超時時間必須為正值</li>
     *   <li>確認掃描服務器位址不為空</li>
     *   <li>檢查服務器埠號的有效性</li>
     * </ul>
     * </p>
     *
     * @param fileProperties 檔案安全設定屬性
     * @param connectionProvider Netty 連線資源管理器
     * @throws IllegalArgumentException 當設定參數不符合要求時
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
     * 透過非同步方式處理 ByteBuf 資料流進行檔案安全掃描。
     *
     * <p>執行流程：
     * <ol>
     *   <li>首先驗證檔案大小是否符合安全限制</li>
     *   <li>若檔案大小合法，則將資料流傳送至安全掃描服務器</li>
     *   <li>處理並回傳掃描結果</li>
     * </ol>
     * </p>
     *
     * @param byteBufFlux Netty ByteBuf 資料流
     * @param totalSize 檔案總大小（位元組）
     * @return {@link reactor.core.publisher.Mono<scanResult>} 檔案安全掃描結果
     */
    @Override
    public Mono<scanResult> scanByteBuf(Flux<ByteBuf> byteBufFlux, long totalSize) {
        return validateFileSize(totalSize).switchIfEmpty(Mono.defer(() -> sendToScanServer(byteBufFlux)));
    }

    /**
     * 將 byte[] 資料流轉換為 ByteBuf 並進行檔案安全掃描。
     *
     * <p>提供額外的資料流轉換功能，支援不同來源的位元組資料。
     * 轉換過程中會計算總檔案大小並委派給 {@link #scanByteBuf(Flux, long)} 方法處理。
     * </p>
     *
     * @param byteFlux 位元組陣列資料流
     * @return {@link reactor.core.publisher.Mono<scanResult>} 檔案安全掃描結果
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
     * 將 Spring DataBuffer 資料流轉換為 ByteBuf 並進行檔案安全掃描。
     *
     * <p>支援 Spring WebFlux 的 DataBuffer 資料流，確保與 WebFlux 生態系統的相容性。
     * 轉換過程會釋放原始 DataBuffer 資源，防止記憶體洩漏。
     * </p>
     *
     * @param dataBufferFlux Spring DataBuffer 資料流
     * @return {@link reactor.core.publisher.Mono<scanResult>} 檔案安全掃描結果
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
     * 非同步建立 TCP 連線並執行檔案安全掃描的核心方法。
     *
     * <p>執行複雜的反應式作業流程：
     * <ol>
     *   <li>建立 Netty TCP 客戶端連線</li>
     *   <li>傳送 zINSTREAM 協議命令</li>
     *   <li>以資料流方式發送檔案內容</li>
     *   <li>接收並解析掃描服務器的回應</li>
     *   <li>處理連線資源的釋放</li>
     * </ol>
     * </p>
     *
     * @param byteBufFlux Netty ByteBuf 資料流
     * @return {@link reactor.core.publisher.Mono<scanResult>} 檔案安全掃描結果
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
     * 根據預設的檔案大小限制，驗證檔案是否需要進行安全掃描。
     *
     * <p>檢查依據：
     * <ul>
     *   <li>最小檔案大小限制（若設定）</li>
     *   <li>最大檔案大小限制（若設定）</li>
     * </ul>
     * </p>
     *
     * @param totalSize 檔案的總大小（位元組）
     * @return {@link reactor.core.publisher.Mono<scanResult>} 檔案大小驗證結果
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
