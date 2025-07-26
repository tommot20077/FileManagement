package xyz.dowob.filemanagement.unity;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ServerWebExchange;
import xyz.dowob.filemanagement.component.filter.ClientIpFilter;
import xyz.dowob.filemanagement.component.handler.CustomWebSocketSession;
import xyz.dowob.filemanagement.entity.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * LogUnity 測試類別。
 * 
 * 測試 LogUnity 統一日誌工具的核心功能，包括格式化輸出、
 * 多級別日誌、參數化訊息、異常處理等。
 * 
 * 前置條件：
 * - Mock Spring WebFlux 組件
 * - 模擬各種輸入情境
 * - 驗證方法調用不會拋出異常
 * 
 * 測試步驟：
 * - 驗證日誌方法正常執行
 * - 測試參數替換功能
 * - 檢查異常處理邏輯
 * 
 * 預期結果：
 * - 所有日誌級別正常工作
 * - 參數化訊息正常處理
 * - 異常情況優雅處理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogUnity 統一日誌工具測試")
class LogUnityTest {

    @Mock
    private ServerWebExchange mockExchange;
    
    @Mock
    private CustomWebSocketSession mockCustomWebSocketSession;
    
    @Mock
    private User mockUser;
    
    private MockedStatic<ClientIpFilter> mockedClientIpFilter;
    
    @BeforeEach
    void setUp() {
        // Mock ClientIpFilter 靜態方法
        mockedClientIpFilter = mockStatic(ClientIpFilter.class);
        
        // 重置所有 mock
        reset(mockExchange, mockCustomWebSocketSession, mockUser);
    }
    
    @AfterEach
    void tearDown() {
        // 清理資源
        if (mockedClientIpFilter != null) {
            mockedClientIpFilter.close();
        }
    }
    
    /**
     * 設定基本的 ServerWebExchange mock
     */
    private void setupBasicExchange() {
        lenient().when(mockExchange.getAttribute("requestId")).thenReturn("REQ-12345");
        lenient().when(mockExchange.getAttribute("userId")).thenReturn("1001");
        mockedClientIpFilter.when(() -> ClientIpFilter.getClientIpFromExchange(mockExchange))
            .thenReturn(Optional.of("192.168.1.100"));
    }
    
    /**
     * 設定無用戶資訊的 ServerWebExchange mock
     */
    private void setupExchangeWithoutUser() {
        lenient().when(mockExchange.getAttribute("requestId")).thenReturn("REQ-456");
        lenient().when(mockExchange.getAttribute("userId")).thenReturn(null);
        mockedClientIpFilter.when(() -> ClientIpFilter.getClientIpFromExchange(mockExchange))
            .thenReturn(Optional.of("192.168.1.100"));
    }
    
    /**
     * 設定基本的 WebSocket mock
     */
    private void setupBasicWebSocket() {
        lenient().when(mockUser.getId()).thenReturn(1001L);
        lenient().when(mockCustomWebSocketSession.getUser()).thenReturn(mockUser);
        lenient().when(mockCustomWebSocketSession.getAttribute("requestId")).thenReturn("WS-12345");
        lenient().when(mockCustomWebSocketSession.getAttribute("clientIp")).thenReturn("192.168.1.100");
    }
    
    /**
     * 設定無用戶的 WebSocket mock
     */
    private void setupWebSocketWithoutUser() {
        lenient().when(mockCustomWebSocketSession.getUser()).thenReturn(null);
        lenient().when(mockCustomWebSocketSession.getAttribute("requestId")).thenReturn("WS-789");
        lenient().when(mockCustomWebSocketSession.getAttribute("clientIp")).thenReturn("192.168.1.200");
    }
    
    /**
     * 一般測試 - 驗證基本功能
     */
    @Nested
    @DisplayName("一般測試 - 基本功能驗證")
    class GeneralTests {
        
        /**
         * 日誌級別測試 - 測試所有日誌級別方法
         */
        @Nested
        @DisplayName("日誌級別測試")
        class LogLevelTests {
            
            @Test
            @DisplayName("TRACE級別日誌 - ServerWebExchange")
            void testTraceLogging_WithExchange() {
                // 準備
                setupBasicExchange();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.trace(mockExchange, "測試TRACE訊息"));
            }
            
            @Test
            @DisplayName("DEBUG級別日誌 - ServerWebExchange")
            void testDebugLogging_WithExchange() {
                // 準備
                setupBasicExchange();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.debug(mockExchange, "測試DEBUG訊息"));
            }
            
            @Test
            @DisplayName("INFO級別日誌 - ServerWebExchange")
            void testInfoLogging_WithExchange() {
                // 準備
                setupBasicExchange();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.info(mockExchange, "測試INFO訊息"));
            }
            
            @Test
            @DisplayName("WARN級別日誌 - ServerWebExchange")
            void testWarnLogging_WithExchange() {
                // 準備
                setupBasicExchange();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.warn(mockExchange, "測試WARN訊息"));
            }
            
            @Test
            @DisplayName("ERROR級別日誌 - ServerWebExchange")
            void testErrorLogging_WithExchange() {
                // 準備
                setupBasicExchange();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.error(mockExchange, "測試ERROR訊息"));
            }
            
            @Test
            @DisplayName("FATAL級別日誌 - ServerWebExchange")
            void testFatalLogging_WithExchange() {
                // 準備
                setupBasicExchange();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.fatal(mockExchange, "測試FATAL訊息"));
            }
        }
        
        /**
         * 參數化訊息測試
         */
        @Nested
        @DisplayName("參數化訊息測試")
        class ParameterizedMessageTests {
            
            @Test
            @DisplayName("單參數替換")
            void testParameterizedMessage_SingleParam() {
                // 準備
                setupBasicExchange();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.info(mockExchange, "用戶 %s 執行操作", "testUser"));
            }
            
            @Test
            @DisplayName("多參數替換")
            void testParameterizedMessage_MultipleParams() {
                // 準備
                setupBasicExchange();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.info(mockExchange, "用戶 %s 在 %s 執行 %s 操作", "testUser", "2024-01-01", "上傳"));
            }
            
            @Test
            @DisplayName("數字參數替換")
            void testParameterizedMessage_NumberParams() {
                // 準備
                setupBasicExchange();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.info(mockExchange, "處理了 %d 個檔案，大小 %d bytes", 5, 1024));
            }
        }
        
        /**
         * 輸入源測試
         */
        @Nested
        @DisplayName("輸入源測試")
        class InputSourceTests {
            
            @Test
            @DisplayName("包含用戶資訊的Exchange")
            void testServerWebExchange_WithUserInfo() {
                // 準備
                setupBasicExchange();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.info(mockExchange, "用戶操作"));
            }
            
            @Test
            @DisplayName("無用戶資訊的Exchange")
            void testServerWebExchange_WithoutUserInfo() {
                // 準備
                setupExchangeWithoutUser();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.info(mockExchange, "匿名操作"));
            }
            
            @Test
            @DisplayName("包含用戶的WebSocket")
            void testWebSocketSession_WithUser() {
                // 準備
                setupBasicWebSocket();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.info(mockCustomWebSocketSession, "WebSocket 用戶操作"));
            }
            
            @Test
            @DisplayName("無用戶的WebSocket")
            void testWebSocketSession_WithoutUser() {
                // 準備
                setupWebSocketWithoutUser();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.info(mockCustomWebSocketSession, "WebSocket 匿名操作"));
            }
            
            @Test
            @DisplayName("純字串日誌")
            void testStringOnly_BasicLogging() {
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.info("系統啟動完成"));
                assertDoesNotThrow(() -> LogUnity.warn("系統警告"));
                assertDoesNotThrow(() -> LogUnity.error("系統錯誤"));
            }
        }
        
        /**
         * WebSocket 級別測試
         */
        @Nested
        @DisplayName("WebSocket 日誌級別測試")
        class WebSocketLogLevelTests {
            
            @Test
            @DisplayName("WebSocket TRACE級別")
            void testWebSocketTrace() {
                // 準備
                setupBasicWebSocket();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.trace(mockCustomWebSocketSession, "WebSocket TRACE"));
            }
            
            @Test
            @DisplayName("WebSocket DEBUG級別")
            void testWebSocketDebug() {
                // 準備
                setupBasicWebSocket();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.debug(mockCustomWebSocketSession, "WebSocket DEBUG"));
            }
            
            @Test
            @DisplayName("WebSocket WARN級別")
            void testWebSocketWarn() {
                // 準備
                setupBasicWebSocket();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.warn(mockCustomWebSocketSession, "WebSocket WARN"));
            }
            
            @Test
            @DisplayName("WebSocket ERROR級別")
            void testWebSocketError() {
                // 準備
                setupBasicWebSocket();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.error(mockCustomWebSocketSession, "WebSocket ERROR"));
            }
            
            @Test
            @DisplayName("WebSocket FATAL級別")
            void testWebSocketFatal() {
                // 準備
                setupBasicWebSocket();
                
                // 執行 & 驗證 - 不應拋出異常
                assertDoesNotThrow(() -> LogUnity.fatal(mockCustomWebSocketSession, "WebSocket FATAL"));
            }
        }
    }
    
    /**
     * 異常測試 - 驗證錯誤處理
     */
    @Nested
    @DisplayName("異常測試 - 錯誤處理驗證")
    class ExceptionTests {
        
        @Test
        @DisplayName("含例外的錯誤日誌 - ServerWebExchange")
        void testErrorLogging_WithThrowable_Exchange() {
            // 準備
            setupBasicExchange();
            RuntimeException testException = new RuntimeException("測試異常");
            
            // 執行 & 驗證 - 不應拋出異常
            assertDoesNotThrow(() -> LogUnity.error(mockExchange, "系統發生錯誤", testException));
        }
        
        @Test
        @DisplayName("含例外的錯誤日誌 - WebSocket")
        void testErrorLogging_WithThrowable_WebSocket() {
            // 準備
            setupBasicWebSocket();
            RuntimeException testException = new RuntimeException("WebSocket異常");
            
            // 執行 & 驗證 - 不應拋出異常
            assertDoesNotThrow(() -> LogUnity.error(mockCustomWebSocketSession, "WebSocket發生錯誤", testException));
        }
        
        @Test
        @DisplayName("含例外的純字串日誌")
        void testErrorLogging_WithThrowable_StringOnly() {
            // 準備
            RuntimeException testException = new RuntimeException("系統級異常");
            
            // 執行 & 驗證 - 不應拋出異常
            assertDoesNotThrow(() -> LogUnity.error("系統發生嚴重錯誤", testException));
        }
        
        @Test
        @DisplayName("無效參數格式處理")
        void testInvalidFormatString_HandlesGracefully() {
            // 準備
            setupBasicExchange();
            
            // 執行 & 驗證 - 參數數量不匹配會拋出 MissingFormatArgumentException（僅當有 args 時）
            assertThrows(java.util.MissingFormatArgumentException.class, () -> {
                LogUnity.info(mockExchange, "格式化 %s %s", "單一參數");
            });
            // 無參數但有格式 - 由於 args 為空，不會調用 String.format，不會拋出異常
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, "無參數但有格式 %s"));
            // 多參數少格式 - 這個應該不會拋出異常，因為額外參數會被忽略
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, "多參數少格式", "參數1", "參數2"));
        }
        
        @Test
        @DisplayName("null Exchange 處理")
        void testNullExchange_HandlesGracefully() {
            // 執行 & 驗證 - null exchange 不應拋出異常
            assertDoesNotThrow(() -> LogUnity.info((ServerWebExchange) null, "null exchange 測試"));
        }
        
        @Test
        @DisplayName("null WebSocket 處理")
        void testNullWebSocket_HandlesGracefully() {
            // 執行 & 驗證 - null websocket 不應拋出異常
            assertDoesNotThrow(() -> LogUnity.info((CustomWebSocketSession) null, "null websocket 測試"));
        }
        
        @Test
        @DisplayName("null 訊息處理")
        void testNullMessage_HandlesGracefully() {
            // 準備
            setupBasicExchange();
            
            // 執行 & 驗證 - null 訊息不應拋出異常
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, null));
            assertDoesNotThrow(() -> LogUnity.info((String) null));
        }
    }
    
    /**
     * 邊界測試 - 驗證邊界條件
     */
    @Nested
    @DisplayName("邊界測試 - 邊界條件驗證")
    class BoundaryTests {
        
        @Test
        @DisplayName("空訊息處理")
        void testEmptyMessage_HandlesCorrectly() {
            // 準備
            setupBasicExchange();
            
            // 執行 & 驗證 - 空字串不應拋出異常
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, ""));
            assertDoesNotThrow(() -> LogUnity.info(""));
        }
        
        @Test
        @DisplayName("零參數處理")
        void testZeroParameters_HandlesCorrectly() {
            // 準備
            setupBasicExchange();
            
            // 執行 & 驗證 - 零參數不應拋出異常
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, "無參數訊息", (Object[]) null));
        }
        
        @Test
        @DisplayName("長訊息格式化")
        void testLongMessage_HandlesCorrectly() {
            // 準備
            setupBasicExchange();

            // 執行 & 驗證 - 長訊息不應拋出異常
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, "這是一個很長的測試訊息，用來驗證日誌系統對長訊息的處理能力。".repeat(100)));
        }
        
        @Test
        @DisplayName("特殊字符處理")
        void testSpecialCharacters_HandlesCorrectly() {
            // 準備
            setupBasicExchange();
            
            // 執行 & 驗證 - 特殊字符不應拋出異常
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, "特殊字符: {}[]()!@#$%^&*"));
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, "Unicode: 測試中文🔥🎉"));
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, "換行\n符號\t測試"));
        }
        
        @Test
        @DisplayName("大量參數處理")
        void testManyParameters_HandlesCorrectly() {
            // 準備
            setupBasicExchange();
            
            // 執行 & 驗證 - 大量參數不應拋出異常
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, "多參數: %s %s %s %s %s", 
                "參數1", "參數2", "參數3", "參數4", "參數5"));
        }
        
        @Test
        @DisplayName("無請求ID和IP處理")
        void testMissingRequestIdAndIp_HandlesCorrectly() {
            // 準備 - 不設定 requestId 和 IP
            lenient().when(mockExchange.getAttribute("requestId")).thenReturn(null);
            lenient().when(mockExchange.getAttribute("userId")).thenReturn("1001");
            mockedClientIpFilter.when(() -> ClientIpFilter.getClientIpFromExchange(mockExchange))
                .thenReturn(Optional.empty());
            
            // 執行 & 驗證 - 缺少請求資訊不應拋出異常
            assertDoesNotThrow(() -> LogUnity.info(mockExchange, "缺少請求資訊測試"));
        }
        
        @Test
        @DisplayName("ClientIpFilter 異常處理")
        void testClientIpFilterException_HandlesGracefully() {
            // 準備
            lenient().when(mockExchange.getAttribute("requestId")).thenReturn("REQ-123");
            lenient().when(mockExchange.getAttribute("userId")).thenReturn("1001");
            mockedClientIpFilter.when(() -> ClientIpFilter.getClientIpFromExchange(mockExchange))
                .thenThrow(new RuntimeException("IP獲取失敗"));
            
            // 執行 & 驗證 - ClientIpFilter 異常會傳播上來
            assertThrows(RuntimeException.class, () -> LogUnity.info(mockExchange, "IP獲取異常測試"));
        }
    }
}