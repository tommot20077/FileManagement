package xyz.dowob.filemanagement.component.provider.providerImplement;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.unit.DataSize;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.TcpClient;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.exception.ProcessException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileScanProvider 邏輯處理測試")
class FileScanProviderImplTest {

    private final String SCAN_HOST = "localhost";

    private final int SCAN_PORT = 3310;

    private final Duration SCAN_TIMEOUT = Duration.ofSeconds(1);

    @Mock
    private FileProperties mockFileProperties;

    @Mock
    private FileProperties.Security mockSecurityProperties;

    @Mock
    private ConnectionProvider mockConnectionProvider;

    @Mock(answer = Answers.RETURNS_SELF)
    private TcpClient mockTcpClient;

    @Mock
    private Connection mockConnection;

    @Mock
    private NettyOutbound mockNettyOutbound;

    @Mock
    private NettyInbound mockNettyInbound;

    @Mock
    private ByteBufFlux mockByteBufFlux;

    private FileScanProviderImpl fileScanProviderImplUnderTest;


    @BeforeEach
    void setUp() {
        when(mockFileProperties.getSecurity()).thenReturn(mockSecurityProperties);
        lenient().when(mockSecurityProperties.getTimeout()).thenReturn(SCAN_TIMEOUT);
        lenient().when(mockSecurityProperties.getHost()).thenReturn(SCAN_HOST);
        lenient().when(mockSecurityProperties.getPort()).thenReturn(SCAN_PORT);
        lenient().when(mockSecurityProperties.getMinFileSize()).thenReturn(DataSize.ofBytes(0));
        lenient().when(mockSecurityProperties.getMaxFileSize()).thenReturn(DataSize.ofBytes(Long.MAX_VALUE));

        lenient().when(mockConnection.addHandlerLast(any(ChannelHandler.class))).thenReturn(mockConnection);
    }

    @Test
    @DisplayName("構造函數初始化 - 正常情況 - 成功創建實例")
    void constructor_validProperties_createsInstance() {
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);
    }

    @Test
    @DisplayName("構造函數初始化 - 超時時間不大於0 - 拋出 IllegalArgumentException")
    void constructor_nonPositiveTimeout_throwsIllegalArgumentException() {
        when(mockSecurityProperties.getTimeout()).thenReturn(Duration.ZERO);
        assertThrows(IllegalArgumentException.class, () -> new FileScanProviderImpl(mockFileProperties, mockConnectionProvider));
    }

    @Test
    @DisplayName("構造函數初始化 - 主機為空 - 拋出 IllegalArgumentException")
    void constructor_nullHost_throwsIllegalArgumentException() {
        when(mockSecurityProperties.getHost()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> new FileScanProviderImpl(mockFileProperties, mockConnectionProvider));
    }

    @Test
    @DisplayName("構造函數初始化 - 端口不大於0 - 拋出 IllegalArgumentException")
    void constructor_nonPositivePort_throwsIllegalArgumentException() {
        when(mockSecurityProperties.getPort()).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> new FileScanProviderImpl(mockFileProperties, mockConnectionProvider));
    }


    @Test
    @DisplayName("scanByteBuf - 檔案大小小於最小限制 - 跳過掃描")
    void scanByteBuf_fileSizeLessThanMin_skipsScan() {
        when(mockSecurityProperties.getMinFileSize()).thenReturn(DataSize.ofKilobytes(1));
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);

        Flux<ByteBuf> testFlux = Flux.just(Unpooled.buffer(100).writeBytes(new byte[100]));
        long totalSize = 100L;

        StepVerifier
                .create(fileScanProviderImplUnderTest.scanByteBuf(testFlux, totalSize))
                .expectNextMatches(result -> result.isSafe() && result.message().toString().contains("不符合掃描檔案大小限制，跳過掃描"))
                .verifyComplete();
    }


    @Test
    @DisplayName("scanByteBuf - 檔案大小大於最大限制 - 跳過掃描")
    void scanByteBuf_fileSizeGreaterThanMax_skipsScan() {
        when(mockSecurityProperties.getMaxFileSize()).thenReturn(DataSize.ofKilobytes(1));
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);

        Flux<ByteBuf> testFlux = Flux.just(Unpooled.buffer(2048).writeBytes(new byte[2048]));
        long totalSize = 2048L;

        StepVerifier
                .create(fileScanProviderImplUnderTest.scanByteBuf(testFlux, totalSize))
                .expectNextMatches(result -> result.isSafe() && result.message().toString().contains("不符合掃描檔案大小限制，跳過掃描"))
                .verifyComplete();
    }


    @Test
    @DisplayName("scanByteBuf - 檔案大小符合限制且掃描通過 - 返回安全結果")
    void scanByteBuf_validSizeAndScanOk_returnsSafe() {
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);
        Flux<ByteBuf> testFlux = Flux.just(Unpooled.wrappedBuffer("test data".getBytes(StandardCharsets.UTF_8)));
        long totalSize = "test data".getBytes(StandardCharsets.UTF_8).length;

        try (MockedStatic<TcpClient> mockedTcpClientStatic = Mockito.mockStatic(TcpClient.class)) {
            mockedTcpClientStatic.when(() -> TcpClient.create(mockConnectionProvider)).thenReturn(mockTcpClient);
            doReturn(Mono.just(mockConnection)).when(mockTcpClient).connect();
            setupScanMocksForSuccess();

            StepVerifier
                    .create(fileScanProviderImplUnderTest.scanByteBuf(testFlux, totalSize))
                    .expectNextMatches(result -> result.isSafe() && result.message().toString().contains("檔案檢測通過"))
                    .verifyComplete();
        }
    }


    private void setupScanMocksForSuccess() {
        when(mockConnection.outbound()).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.sendString(any(Mono.class))).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.send(any(Flux.class))).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.sendByteArray(any(Mono.class))).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.then()).thenReturn(Mono.empty());
        when(mockConnection.inbound()).thenReturn(mockNettyInbound);
        when(mockNettyInbound.receive()).thenReturn(mockByteBufFlux);
        when(mockByteBufFlux.asString(StandardCharsets.UTF_8)).thenReturn(Flux.just("OK"));
    }


    @Test
    @DisplayName("scanByteBuf - 檔案大小符合限制且檢測到病毒 - 返回不安全結果")
    void scanByteBuf_validSizeAndVirusFound_returnsUnsafe() {
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);
        Flux<ByteBuf> testFlux = Flux.just(Unpooled.wrappedBuffer("virus data".getBytes(StandardCharsets.UTF_8)));
        long totalSize = "virus data".getBytes(StandardCharsets.UTF_8).length;

        try (MockedStatic<TcpClient> mockedTcpClientStatic = Mockito.mockStatic(TcpClient.class)) {
            mockedTcpClientStatic.when(() -> TcpClient.create(mockConnectionProvider)).thenReturn(mockTcpClient);
            doReturn(Mono.just(mockConnection)).when(mockTcpClient).connect();
            setupScanMocksForVirusFound();

            StepVerifier
                    .create(fileScanProviderImplUnderTest.scanByteBuf(testFlux, totalSize))
                    .expectNextMatches(result -> !result.isSafe() && result.message().toString().contains("檔案檢測到病毒"))
                    .verifyComplete();
        }
    }


    private void setupScanMocksForVirusFound() {
        when(mockConnection.outbound()).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.sendString(any(Mono.class))).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.send(any(Flux.class))).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.sendByteArray(any(Mono.class))).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.then()).thenReturn(Mono.empty());

        when(mockConnection.inbound()).thenReturn(mockNettyInbound);
        when(mockNettyInbound.receive()).thenReturn(mockByteBufFlux);
        when(mockByteBufFlux.asString(StandardCharsets.UTF_8)).thenReturn(Flux.just("stream: Eicar-Test-Signature FOUND"));
    }


    @Test
    @DisplayName("scanByteBuf - 連接掃描服務器失敗 - 拋出 ProcessException")
    void scanByteBuf_connectScanServerFailed_throwsProcessException() {
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);
        Flux<ByteBuf> testFlux = Flux.just(Unpooled.wrappedBuffer("test".getBytes()));
        long totalSize = 4L;

        try (MockedStatic<TcpClient> mockedTcpClientStatic = Mockito.mockStatic(TcpClient.class)) {
            mockedTcpClientStatic.when(() -> TcpClient.create(mockConnectionProvider)).thenReturn(mockTcpClient);
            when(mockTcpClient.connect()).thenReturn(Mono.error(new RuntimeException("Connection failed!")));

            StepVerifier
                    .create(fileScanProviderImplUnderTest.scanByteBuf(testFlux, totalSize))
                    .expectErrorMatches(throwable -> throwable instanceof ProcessException && ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.CONNECT_SCAN_SERVER_FAILED)
                    .verify();
        }
    }


    @Test
    @DisplayName("scanByteBuf - 掃描服務器返回未知響應 - 拋出 ProcessException")
    void scanByteBuf_scanServerErrorResponse_throwsProcessException() {
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);
        Flux<ByteBuf> testFlux = Flux.just(Unpooled.wrappedBuffer("test".getBytes()));
        long totalSize = 4L;

        try (MockedStatic<TcpClient> mockedTcpClientStatic = Mockito.mockStatic(TcpClient.class)) {
            mockedTcpClientStatic.when(() -> TcpClient.create(mockConnectionProvider)).thenReturn(mockTcpClient);
            doReturn(Mono.just(mockConnection)).when(mockTcpClient).connect();
            setupScanMocksForScanError();

            StepVerifier
                    .create(fileScanProviderImplUnderTest.scanByteBuf(testFlux, totalSize))
                    .expectErrorMatches(throwable -> throwable instanceof ProcessException && ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.RESOLVE_SCAN_FAILED)
                    .verify();
        }
    }


    private void setupScanMocksForScanError() {
        when(mockConnection.outbound()).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.sendString(any(Mono.class))).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.send(any(Flux.class))).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.sendByteArray(any(Mono.class))).thenReturn(mockNettyOutbound);
        when(mockNettyOutbound.then()).thenReturn(Mono.empty());

        when(mockConnection.inbound()).thenReturn(mockNettyInbound);
        when(mockNettyInbound.receive()).thenReturn(mockByteBufFlux);
        when(mockByteBufFlux.asString(StandardCharsets.UTF_8)).thenReturn(Flux.just("ERROR SCANNING"));
    }


    @Test
    @DisplayName("scanByteBuf - 掃描超時 - 拋出 TimeoutException")
    void scanByteBuf_scanTimeout_throwsTimeoutException() {
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);
        Flux<ByteBuf> testFlux = Flux.just(Unpooled.wrappedBuffer("test data".getBytes(StandardCharsets.UTF_8)));
        long totalSize = "test data".getBytes(StandardCharsets.UTF_8).length;

        try (MockedStatic<TcpClient> mockedTcpClientStatic = Mockito.mockStatic(TcpClient.class)) {
            mockedTcpClientStatic.when(() -> TcpClient.create(mockConnectionProvider)).thenReturn(mockTcpClient);
            doReturn(Mono.just(mockConnection)).when(mockTcpClient).connect();

            when(mockConnection.outbound()).thenReturn(mockNettyOutbound);
            when(mockNettyOutbound.sendString(any(Mono.class))).thenReturn(mockNettyOutbound);
            when(mockNettyOutbound.send(any(Flux.class))).thenReturn(mockNettyOutbound);
            when(mockNettyOutbound.sendByteArray(any(Mono.class))).thenReturn(mockNettyOutbound);
            when(mockNettyOutbound.then()).thenReturn(Mono.empty());


            when(mockConnection.inbound()).thenReturn(mockNettyInbound);
            when(mockNettyInbound.receive()).thenReturn(mockByteBufFlux);
            when(mockByteBufFlux.asString(StandardCharsets.UTF_8)).thenReturn(Flux.never());

            StepVerifier
                    .create(fileScanProviderImplUnderTest.scanByteBuf(testFlux, totalSize))
                    .expectError(TimeoutException.class)
                    .verify(SCAN_TIMEOUT.plusMillis(100));
        }
    }

    @Test
    @DisplayName("scanBytes - 空 Flux - 應處理為大小為0並跳過掃描")
    void scanBytes_emptyFlux_callsScanByteBufWithZeroSizeAndSkips() {
        when(mockSecurityProperties.getMinFileSize()).thenReturn(DataSize.ofBytes(1));
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);

        Flux<byte[]> emptyByteFlux = Flux.empty();

        StepVerifier
                .create(fileScanProviderImplUnderTest.scanBytes(emptyByteFlux))
                .expectNextMatches(result -> result.isSafe() && result.message().toString().contains("不符合掃描檔案大小限制，跳過掃描"))
                .verifyComplete();
    }

    @Test
    @DisplayName("scanBytes - 有數據 Flux 且掃描通過 - 返回安全結果")
    void scanBytes_withDataAndScanOk_returnsSafe() {
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);
        Flux<byte[]> byteFlux = Flux.just("test data".getBytes(StandardCharsets.UTF_8));

        try (MockedStatic<TcpClient> mockedTcpClientStatic = Mockito.mockStatic(TcpClient.class)) {
            mockedTcpClientStatic.when(() -> TcpClient.create(mockConnectionProvider)).thenReturn(mockTcpClient);
            doReturn(Mono.just(mockConnection)).when(mockTcpClient).connect();
            setupScanMocksForSuccess();

            StepVerifier
                    .create(fileScanProviderImplUnderTest.scanBytes(byteFlux))
                    .expectNextMatches(result -> result.isSafe() && result.message().toString().contains("檔案檢測通過"))
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("scanDataBuffer - 空 Flux - 應處理為大小為0並跳過掃描")
    void scanDataBuffer_emptyFlux_callsScanByteBufWithZeroSizeAndSkips() {
        when(mockSecurityProperties.getMinFileSize()).thenReturn(DataSize.ofBytes(1));
        fileScanProviderImplUnderTest = new FileScanProviderImpl(mockFileProperties, mockConnectionProvider);

        Flux<DataBuffer> emptyDataBufferFlux = Flux.empty();

        StepVerifier
                .create(fileScanProviderImplUnderTest.scanDataBuffer(emptyDataBufferFlux))
                .expectNextMatches(result -> result.isSafe() && result.message().toString().contains("不符合掃描檔案大小限制，跳過掃描"))
                .verifyComplete();
    }
}
