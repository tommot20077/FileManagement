package xyz.dowob.filemanagement.component.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.holder.CustomRequestContextHolder;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ClientIpFilter 客戶端 IP 過濾器測試")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientIpFilterTest {

    private static final String CLIENT_IP_ATTRIBUTE = "clientIp";

    private static final String CF_CONNECTING_IP = "CF-Connecting-IP";

    private static final String X_REAL_IP_HEADER = "X-Real-IP";

    private static final String X_FORWARDED_FOR_HEADER = "X-FORWARDED-FOR";

    @Mock
    private GlobalProperties globalProperties;

    @Mock
    private GlobalProperties.forwarded forwarded;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private WebFilterChain chain;

    @Mock
    private HttpHeaders headers;

    private ClientIpFilter clientIpFilter;


    @BeforeEach
    void setUp() {
        // These are general mocks for most tests, constructor tests will override as needed
        when(globalProperties.getForwarded()).thenReturn(forwarded);
        when(forwarded.getXRealIpHeader()).thenReturn(X_REAL_IP_HEADER);
        when(forwarded.getXForwardedHeader()).thenReturn(X_FORWARDED_FOR_HEADER);
        clientIpFilter = new ClientIpFilter(globalProperties);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(exchange.getAttributes()).thenReturn(new HashMap<>());
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Nested
    @DisplayName("一般測試")
    class NormalCases {

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * 優先使用 X-Real-IP
         *
         * 前置條件：
         * - 模擬請求包含有效的 X-Real-IP 頭
         *
         * 測試步驟：
         * - 驗證 filter 方法被調用，且 exchange 屬性中存儲的 IP 為 X-Real-IP 的值。
         *
         * 預期結果：
         * - 過濾器成功處理請求，並將 X-Real-IP 設置為客戶端 IP。
         */
        @Test
        @DisplayName("優先使用 X-Real-IP")
        void testFilterWithXRealIp() {
            String expectedIp = "192.168.1.1";
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(expectedIp);

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * 優先使用 CF-Connecting-IP (當 X-Real-IP 無效或不存在時)
         *
         * 前置條件：
         * - 模擬請求包含無效或不存在的 X-Real-IP，但包含有效的 CF-Connecting-IP 頭
         *
         * 測試步驟：
         * - 驗證 filter 方法被調用，且 exchange 屬性中存儲的 IP 為 CF-Connecting-IP 的值。
         *
         * 預期結果：
         * - 過濾器成功處理請求，並將 CF-Connecting-IP 設置為客戶端 IP。
         */
        @Test
        @DisplayName("優先使用 CF-Connecting-IP")
        void testFilterWithCfConnectingIp() {
            String expectedIp = "10.0.0.1";
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(null);
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn(expectedIp);

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * 優先使用 X-Forwarded-For (當 X-Real-IP 和 CF-Connecting-IP 無效或不存在時)
         *
         * 前置條件：
         * - 模擬請求包含無效或不存在的 X-Real-IP 和 CF-Connecting-IP，但包含有效的 X-Forwarded-For 頭
         *
         * 測試步驟：
         * - 驗證 filter 方法被調用，且 exchange 屬性中存儲的 IP 為 X-Forwarded-For 中第一個有效 IP 的值。
         *
         * 預期結果：
         * - 過濾器成功處理請求，並將 X-Forwarded-For 中的第一個有效 IP 設置為客戶端 IP。
         */
        @Test
        @DisplayName("優先使用 X-Forwarded-For")
        void testFilterWithXForwardedFor() {
            String expectedIp = "172.16.0.1";
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(null);
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn(null);
            when(headers.getFirst(X_FORWARDED_FOR_HEADER)).thenReturn(expectedIp + ", 192.168.1.100");

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * 使用 remoteAddress (當所有請求頭都無效或不存在時)
         *
         * 前置條件：
         * - 模擬請求不包含任何有效 IP 相關的請求頭，但 remoteAddress 有效
         *
         * 測試步驟：
         * - 驗證 filter 方法被調用，且 exchange 屬性中存儲的 IP 為 remoteAddress 的值。
         *
         * 預期結果：
         * - 過濾器成功處理請求，並將 remoteAddress 設置為客戶端 IP。
         */
        @Test
        @DisplayName("使用 remoteAddress")
        void testFilterWithRemoteAddress() {
            String expectedIp = "127.0.0.1";
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(null);
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn(null);
            when(headers.getFirst(X_FORWARDED_FOR_HEADER)).thenReturn(null);
            when(request.getRemoteAddress()).thenReturn(new InetSocketAddress(expectedIp, 8080));

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * getClientIpFromExchange 獲取 IP (從 exchange 屬性)
         *
         * 前置條件：
         * - 模擬 filter 方法已執行，並在 exchange 屬性中設置了 IP。
         *
         * 測試步驟：
         * - 調用 ClientIpFilter.getClientIpFromExchange(exchange)。
         *
         * 預期結果：
         * - 返回包含正確 IP 地址的 Optional 對象。
         */
        @Test
        @DisplayName("getClientIpFromExchange 獲取 IP (從 exchange 屬性)")
        void testGetClientIpFromExchangeWithExchangeAttribute() {
            String expectedIp = "192.168.1.1";
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(CLIENT_IP_ATTRIBUTE, expectedIp);
            when(exchange.getAttributes()).thenReturn(attributes);

            Optional<String> ipOptional = ClientIpFilter.getClientIpFromExchange(exchange);

            assertTrue(ipOptional.isPresent());
            assertEquals(expectedIp, ipOptional.get());
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * getClientIpFromExchange 獲取 IP (從 CustomRequestContextHolder)
         *
         * 前置條件：
         * - 模擬 CustomRequestContextHolder 中存在 ServerWebExchange 且其屬性中設置了 IP。
         *
         * 測試步驟：
         * - 調用 ClientIpFilter.getClientIpFromExchange(null)。
         *
         * 預期結果：
         * - 返回包含正確 IP 地址的 Optional 對象。
         */
        @Test
        @DisplayName("getClientIpFromExchange 獲取 IP (從 CustomRequestContextHolder)")
        void testGetClientIpFromExchangeWithCustomRequestContextHolder() {
            String expectedIp = "192.168.1.1";
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(CLIENT_IP_ATTRIBUTE, expectedIp);

            try (MockedStatic<CustomRequestContextHolder> mockedStatic = mockStatic(CustomRequestContextHolder.class)) {
                when(exchange.getAttributes()).thenReturn(attributes);
                mockedStatic.when(CustomRequestContextHolder::getExchange).thenReturn(Mono.just(exchange));

                Optional<String> ipOptional = ClientIpFilter.getClientIpFromExchange(null);

                assertTrue(ipOptional.isPresent());
                assertEquals(expectedIp, ipOptional.get());
            }
        }
    }

    @Nested
    @DisplayName("異常測試")
    class ExceptionCases {

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * X-Real-IP 格式無效
         *
         * 前置條件：
         * - 模擬請求包含格式無效的 X-Real-IP 頭
         *
         * 測試步驟：
         * - 驗證過濾器應跳過此 IP，並嘗試獲取下一個優先級的 IP。
         *
         * 預期結果：
         * - 過濾器應跳過此 IP，並嘗試獲取下一個優先級的 IP。
         */
        @Test
        @DisplayName("X-Real-IP 格式無效")
        void testFilterWithInvalidXRealIp() {
            String expectedIp = "10.0.0.1"; // CF-Connecting-IP
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn("invalid-ip");
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn(expectedIp);

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * CF-Connecting-IP 格式無效
         *
         * 前置條件：
         * - 模擬請求包含格式無效的 CF-Connecting-IP 頭
         *
         * 測試步驟：
         * - 驗證過濾器應跳過此 IP，並嘗試獲取下一個優先級的 IP。
         *
         * 預期結果：
         * - 過濾器應跳過此 IP，並嘗試獲取下一個優先級的 IP。
         */
        @Test
        @DisplayName("CF-Connecting-IP 格式無效")
        void testFilterWithInvalidCfConnectingIp() {
            String expectedIp = "172.16.0.1"; // X-Forwarded-For
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(null);
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn("another-invalid-ip");
            when(headers.getFirst(X_FORWARDED_FOR_HEADER)).thenReturn(expectedIp);

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * X-Forwarded-For 格式無效 (第一個 IP 無效)
         *
         * 前置條件：
         * - 模擬請求包含 X-Forwarded-For 頭，但其中第一個 IP 格式無效
         *
         * 測試步驟：
         * - 驗證過濾器應跳過此 IP，並嘗試獲取下一個優先級的 IP。
         *
         * 預期結果：
         * - 過濾器應跳過此 IP，並嘗試獲取下一個優先級的 IP。
         */
        @Test
        @DisplayName("X-Forwarded-For 格式無效 (第一個 IP 無效)")
        void testFilterWithInvalidXForwardedForFirstIp() {
            String expectedIp = "127.0.0.1"; // remoteAddress
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(null);
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn(null);
            when(headers.getFirst(X_FORWARDED_FOR_HEADER)).thenReturn("invalid-ip, 192.168.1.1");
            when(request.getRemoteAddress()).thenReturn(new InetSocketAddress(expectedIp, 8080));

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * remoteAddress 返回的 IP 格式無效
         *
         * 前置條件：
         * - 模擬 exchange.getRequest().getRemoteAddress() 返回一個 InetSocketAddress，但其 getAddress().getHostAddress() 返回一個無效的 IP 字符串。
         *
         * 測試步驟：
         * - 驗證過濾器應跳過此 IP，最終 exchange 屬性中存儲的 IP 應為 null。
         *
         * 預期結果：
         * - 過濾器應跳過此 IP，最終 exchange 屬性中存儲的 IP 應為 null。
         */
        @Test
        @DisplayName("remoteAddress 返回的 IP 格式無效")
        void testFilterWithInvalidRemoteAddressIp() {
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(null);
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn(null);
            when(headers.getFirst(X_FORWARDED_FOR_HEADER)).thenReturn(null);
            InetSocketAddress mockAddress = mock(InetSocketAddress.class);
            when(request.getRemoteAddress()).thenReturn(mockAddress);
            when(mockAddress.getAddress()).thenReturn(mock(java.net.InetAddress.class));
            when(mockAddress.getAddress().getHostAddress()).thenReturn("invalid-remote-ip");

            clientIpFilter.filter(exchange, chain).block();

            assertNull(exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * GlobalProperties 中的 XForwardedHeader 為 null (構造函數異常)
         *
         * 前置條件：
         * - 模擬 GlobalProperties.getForwarded().getXForwardedHeader() 返回 null。
         *
         * 測試步驟：
         * - 嘗試實例化 ClientIpFilter。
         *
         * 預期結果：
         * - 應拋出 IllegalArgumentException，訊息為 "請求IP轉發頭不能為空"。
         */
        @Test
        @DisplayName("GlobalProperties 中的 XForwardedHeader 為 null")
        void testConstructorWithNullXForwardedHeader() {
            // Specific mocks for this test
            GlobalProperties localGlobalProperties = mock(GlobalProperties.class);
            GlobalProperties.forwarded localForwarded = mock(GlobalProperties.forwarded.class);
            when(localGlobalProperties.getForwarded()).thenReturn(localForwarded);
            when(localForwarded.getXForwardedHeader()).thenReturn(null);
            when(localForwarded.getXRealIpHeader()).thenReturn(X_REAL_IP_HEADER); // Keep this valid for this test

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    new ClientIpFilter(localGlobalProperties)
            );
            assertEquals("請求IP轉發頭不能為空", exception.getMessage());
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * GlobalProperties 中的 XRealIpHeader 為 null (構造函數異常)
         *
         * 前置條件：
         * - 模擬 GlobalProperties.getForwarded().getXRealIpHeader() 返回 null。
         *
         * 測試步驟：
         * - 嘗試實例化 ClientIpFilter。
         *
         * 預期結果：
         * - 應拋出 IllegalArgumentException，訊息為 "真實IP頭不能為空"。
         */
        @Test
        @DisplayName("GlobalProperties 中的 XRealIpHeader 為 null")
        void testConstructorWithNullXRealIpHeader() {
            // Specific mocks for this test
            GlobalProperties localGlobalProperties = mock(GlobalProperties.class);
            GlobalProperties.forwarded localForwarded = mock(GlobalProperties.forwarded.class);
            when(localGlobalProperties.getForwarded()).thenReturn(localForwarded);
            when(localForwarded.getXForwardedHeader()).thenReturn(X_FORWARDED_FOR_HEADER); // Keep this valid for this test
            when(localForwarded.getXRealIpHeader()).thenReturn(null);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    new ClientIpFilter(localGlobalProperties)
            );
            assertEquals("真實IP頭不能為空", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("邊界測試")
    class EdgeCases {

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * 所有 IP 相關請求頭均為空字符串或不存在，且 remoteAddress 為 null
         *
         * 前置條件：
         * - 模擬請求不包含任何 IP 相關的請求頭，且 exchange.getRequest().getRemoteAddress() 返回 null。
         *
         * 測試步驟：
         * - 驗證 exchange 屬性中存儲的 IP 應為 null。
         *
         * 預期結果：
         * - exchange 屬性中存儲的 IP 應為 null。
         */
        @Test
        @DisplayName("所有 IP 相關請求頭均為空且 remoteAddress 為 null")
        void testFilterWithAllNullIps() {
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(null);
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn(null);
            when(headers.getFirst(X_FORWARDED_FOR_HEADER)).thenReturn(null);
            when(request.getRemoteAddress()).thenReturn(null);

            clientIpFilter.filter(exchange, chain).block();

            assertNull(exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * X-Forwarded-For 包含多個有效 IP，且帶有空格
         *
         * 前置條件：
         * - 模擬請求包含 X-Forwarded-For 頭，其中包含多個有效的 IP 地址，且每個 IP 前後有空格。
         *
         * 測試步驟：
         * - 驗證過濾器應正確去除空格並提取第一個 IP 地址。
         *
         * 預期結果：
         * - 過濾器應正確去除空格並提取第一個 IP 地址。
         */
        @Test
        @DisplayName("X-Forwarded-For 包含多個有效 IP 且帶有空格")
        void testFilterWithXForwardedForMultipleIpsWithSpaces() {
            String expectedIp = "192.168.1.1";
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(null);
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn(null);
            when(headers.getFirst(X_FORWARDED_FOR_HEADER)).thenReturn(" 192.168.1.1 , 10.0.0.1 ");

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * X-Forwarded-For 僅包含一個有效 IP，且帶有空格
         *
         * 前置條件：
         * - 模擬請求包含 X-Forwarded-For 頭，其中只包含一個有效 IP 地址，且帶有前後空格。
         *
         * 測試步驟：
         * - 驗證過濾器應正確去除空格並提取該 IP 地址。
         *
         * 預期結果：
         * - 過濾器應正確去除空格並提取該 IP 地址。
         */
        @Test
        @DisplayName("X-Forwarded-For 僅包含一個有效 IP 且帶有空格")
        void testFilterWithXForwardedForSingleIpWithSpaces() {
            String expectedIp = "192.168.1.1";
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(null);
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn(null);
            when(headers.getFirst(X_FORWARDED_FOR_HEADER)).thenReturn(" 192.168.1.1 ");

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * X-Forwarded-For 僅包含空字符串或只有逗號
         *
         * 前置條件：
         * - 模擬請求包含 X-Forwarded-For 頭，其值為空字符串或僅包含逗號。
         *
         * 測試步驟：
         * - 驗證過濾器應跳過此 IP，並嘗試獲取下一個優先級的 IP。
         *
         * 預期結果：
         * - 過濾器應跳過此 IP，並嘗試獲取下一個優先級的 IP。
         */
        @Test
        @DisplayName("X-Forwarded-For 僅包含空字符串或只有逗號")
        void testFilterWithEmptyOrCommaOnlyXForwardedFor() {
            String expectedIp = "127.0.0.1"; // remoteAddress
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(null);
            when(headers.getFirst(CF_CONNECTING_IP)).thenReturn(null);
            when(headers.getFirst(X_FORWARDED_FOR_HEADER)).thenReturn("");
            when(request.getRemoteAddress()).thenReturn(new InetSocketAddress(expectedIp, 8080));

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));

            when(headers.getFirst(X_FORWARDED_FOR_HEADER)).thenReturn(",");
            clientIpFilter.filter(exchange, chain).block();
            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * getClientIpFromExchange 在 exchange 為 null 且 CustomRequestContextHolder 也無 exchange 時
         *
         * 前置條件：
         * - 調用 ClientIpFilter.getClientIpFromExchange(null)，且模擬 CustomRequestContextHolder.getExchange() 返回 Mono.empty()。
         *
         * 測試步驟：
         * - 驗證返回 Optional.empty()。
         *
         * 預期結果：
         * - 返回 Optional.empty()。
         */
        @Test
        @DisplayName("getClientIpFromExchange 在 exchange 為 null 且 CustomRequestContextHolder 也無 exchange 時")
        void testGetClientIpFromExchangeWithNullExchangeAndEmptyHolder() {
            try (MockedStatic<CustomRequestContextHolder> mockedStatic = mockStatic(CustomRequestContextHolder.class)) {
                mockedStatic.when(CustomRequestContextHolder::getExchange).thenReturn(Mono.empty());

                Optional<String> ipOptional = ClientIpFilter.getClientIpFromExchange(null);

                assertFalse(ipOptional.isPresent());
            }
        }

        /**
         * 測試摘要。
         *
         * 測試涵蓋的邏輯或場景說明。
         * IPv6 地址的處理
         *
         * 前置條件：
         * - 模擬請求頭或 remoteAddress 包含有效的 IPv6 地址。
         *
         * 測試步驟：
         * - 驗證過濾器應正確識別並存儲 IPv6 地址。
         *
         * 預期結果：
         * - 過濾器應正確識別並存儲 IPv6 地址。
         */
        @Test
        @DisplayName("IPv6 地址的處理")
        void testFilterWithIPv6Address() {
            String expectedIp = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
            when(headers.getFirst(X_REAL_IP_HEADER)).thenReturn(expectedIp);

            clientIpFilter.filter(exchange, chain).block();

            assertEquals(expectedIp, exchange.getAttributes().get(CLIENT_IP_ATTRIBUTE));
            verify(chain).filter(exchange);
        }
    }
}
