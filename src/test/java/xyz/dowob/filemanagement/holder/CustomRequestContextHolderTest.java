package xyz.dowob.filemanagement.holder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CustomRequestContextHolder 上下文管理器測試類別。
 * 
 * 驗證在 WebFlux 響應式環境中對 ServerWebExchange 的上下文管理功能，包括 Reactor Context 中的存儲、
 * 獲取和線程隔離特性。測試涵蓋正常操作、異常處理和邊界條件，確保響應式程式設計中請求範圍的正確管理。
 * 
 * <p>測試範圍包括：響應式鏈中的上下文傳遞、併發環境下的線程隔離、null 值處理和深層嵌套操作中的上下文一致性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomRequestContextHolder 上下文管理測試")
class CustomRequestContextHolderTest {

    private final String testPath = "/test/path";
    
    @Mock
    private ServerWebExchange mockExchange;

    @Mock
    private ServerHttpRequest mockRequest;


    @BeforeEach
    void setUp() {
        // 設定基本的 mock 行為 - 使用 lenient 避免 UnnecessaryStubbingException
        lenient().when(mockExchange.getRequest()).thenReturn(mockRequest);
        lenient().when(mockRequest.getURI()).thenReturn(URI.create("http://localhost" + testPath));
    }

    // ==================== 一般測試 ====================

    /**
     * 驗證 ServerWebExchange 的正常存儲和獲取操作。
     * 
     * 測試在 Reactor Context 中正確存儲 ServerWebExchange 並成功獲取的基本功能。
     * 
     * 前置條件：
     * - 已初始化 mock ServerWebExchange 對象
     * 
     * 測試步驟：
     * - 使用 mutate 方法創建包含 exchange 的上下文
     * - 使用 getExchange 方法從上下文中獲取 exchange
     * 
     * 預期結果：
     * - 成功獲取相同的 ServerWebExchange 實例
     * - URI 路徑與預期一致
     */
    @Test
    @DisplayName("一般測試 - 正常存儲和獲取 ServerWebExchange")
    void testMutateAndGetExchange_Success() {
        // 執行測試：創建帶有 exchange 的上下文並獲取
        Mono<ServerWebExchange> result = CustomRequestContextHolder.getExchange()
                .contextWrite(CustomRequestContextHolder.mutate(mockExchange));

        // 驗證結果
        StepVerifier.create(result)
                .assertNext(exchange -> {
                    assertNotNull(exchange);
                    assertEquals(mockExchange, exchange);
                    assertEquals(testPath, exchange.getRequest().getURI().getPath());
                })
                .verifyComplete();
    }

    /**
     * 驗證上下文在響應式鏈中的正確傳遞。
     * 
     * 測試多個 flatMap 操作中 Reactor Context 的一致性和持續性。
     * 
     * 前置條件：
     * - 已設定 mock ServerWebExchange
     * 
     * 測試步驟：
     * - 在多個 flatMap 操作中連續獲取 exchange
     * - 驗證每個步驟都能獲取到相同的上下文資料
     * 
     * 預期結果：
     * - 所有操作都能訪問相同的 ServerWebExchange
     * - 字串拼接結果符合預期
     */
    @Test
    @DisplayName("一般測試 - 在反應式鏈中傳遞上下文")
    void testContextPropagationInReactiveChain() {
        // 執行測試：在多個操作中保持上下文
        Mono<String> result = Mono.just("start")
                .flatMap(value -> CustomRequestContextHolder.getExchange()
                        .map(exchange -> value + exchange.getRequest().getURI().getPath()))
                .flatMap(value -> CustomRequestContextHolder.getExchange()
                        .map(exchange -> value + "-end"))
                .contextWrite(CustomRequestContextHolder.mutate(mockExchange));

        // 驗證結果
        StepVerifier.create(result)
                .assertNext(value -> {
                    assertEquals("start" + testPath + "-end", value);
                })
                .verifyComplete();
    }

    /**
     * 驗證 mutate 函數創建 Context 的正確性。
     * 
     * 測試 mutate 方法返回的 Function 能正確將 ServerWebExchange 存儲到 Reactor Context 中。
     * 
     * 前置條件：
     * - 準備空的 Context 實例
     * 
     * 測試步驟：
     * - 調用 mutate 方法獲取 Context 轉換函數
     * - 將函數應用到空 Context 上
     * - 驗證生成的 Context 內容
     * 
     * 預期結果：
     * - Context 包含正確的 SERVER_WEB_EXCHANGE 鍵
     * - 存儲的值為指定的 ServerWebExchange 實例
     */
    @Test
    @DisplayName("一般測試 - mutate 函數正確創建上下文")
    void testMutateFunction_CreatesCorrectContext() {
        // 準備測試資料
        Context emptyContext = Context.empty();
        
        // 執行測試
        Function<Context, Context> mutateFunction = CustomRequestContextHolder.mutate(mockExchange);
        Context resultContext = mutateFunction.apply(emptyContext);

        // 驗證結果
        assertNotNull(resultContext);
        assertTrue(resultContext.hasKey("SERVER_WEB_EXCHANGE"));
        assertEquals(mockExchange, resultContext.get("SERVER_WEB_EXCHANGE"));
    }

    /**
     * 驗證 mutate 函數覆蓋現有 exchange 的行為。
     * 
     * 測試當 Context 中已存在 ServerWebExchange 時，新的 mutate 操作能正確覆蓋舊值。
     * 
     * 前置條件：
     * - 準備包含現有 exchange 的 Context
     * - 準備新的 ServerWebExchange 實例
     * 
     * 測試步驟：
     * - 對已有 exchange 的 Context 應用 mutate 函數
     * - 驗證新 exchange 替換了舊 exchange
     * 
     * 預期結果：
     * - Context 中的 exchange 為新設定的實例
     * - 舊的 exchange 不再存在於 Context 中
     */
    @Test
    @DisplayName("一般測試 - 覆蓋現有上下文中的 exchange")
    void testMutateFunction_OverrideExistingExchange() {
        // 準備測試資料
        ServerWebExchange anotherExchange = mock(ServerWebExchange.class);
        Context contextWithExchange = Context.of("SERVER_WEB_EXCHANGE", anotherExchange);
        
        // 執行測試
        Function<Context, Context> mutateFunction = CustomRequestContextHolder.mutate(mockExchange);
        Context resultContext = mutateFunction.apply(contextWithExchange);

        // 驗證結果：新的 exchange 應該覆蓋舊的
        assertNotNull(resultContext);
        assertTrue(resultContext.hasKey("SERVER_WEB_EXCHANGE"));
        assertEquals(mockExchange, resultContext.get("SERVER_WEB_EXCHANGE"));
        assertNotEquals(anotherExchange, resultContext.get("SERVER_WEB_EXCHANGE"));
    }

    // ==================== 異常測試 ====================

    /**
     * 驗證空上下文獲取 ServerWebExchange 的處理。
     * 
     * 測試在未設定任何上下文資料的情況下調用 getExchange 方法的行為。
     * 
     * 前置條件：
     * - 使用預設的空 Reactor Context
     * 
     * 測試步驟：
     * - 直接調用 getExchange 方法
     * - 驗證返回的 Mono 行為
     * 
     * 預期結果：
     * - 返回完成但不發射任何元素的 Mono
     */
    @Test
    @DisplayName("異常測試 - 空上下文獲取 ServerWebExchange")
    void testGetExchange_EmptyContext() {
        // 執行測試：在沒有設置 exchange 的上下文中獲取
        Mono<ServerWebExchange> result = CustomRequestContextHolder.getExchange();

        // 驗證結果：應該返回空的 Mono
        StepVerifier.create(result)
                .verifyComplete();
    }

    /**
     * 驗證上下文中不存在 SERVER_WEB_EXCHANGE 鍵時的處理。
     * 
     * 測試當 Context 包含其他資料但缺少 SERVER_WEB_EXCHANGE 鍵時的行為。
     * 
     * 前置條件：
     * - 準備包含其他鍵值對但無 SERVER_WEB_EXCHANGE 的 Context
     * 
     * 測試步驟：
     * - 在包含其他資料的上下文中調用 getExchange
     * - 驗證獲取行為
     * 
     * 預期結果：
     * - 返回完成但不發射元素的 Mono
     */
    @Test
    @DisplayName("異常測試 - 上下文中不存在 SERVER_WEB_EXCHANGE key")
    void testGetExchange_NoExchangeKey() {
        // 執行測試：在有其他資料但沒有 exchange 的上下文中獲取
        Mono<ServerWebExchange> result = CustomRequestContextHolder.getExchange()
                .contextWrite(Context.of("OTHER_KEY", "other_value"));

        // 驗證結果：應該返回空的 Mono
        StepVerifier.create(result)
                .verifyComplete();
    }

    /**
     * 驗證上下文中存在錯誤類型值時的異常處理。
     * 
     * 測試當 SERVER_WEB_EXCHANGE 鍵對應的值不是 ServerWebExchange 類型時的行為。
     * 
     * 前置條件：
     * - 準備包含錯誤類型值的 Context
     * 
     * 測試步驟：
     * - 在上下文中存儲字串而非 ServerWebExchange
     * - 調用 getExchange 方法
     * 
     * 預期結果：
     * - 拋出 ClassCastException
     */
    @Test
    @DisplayName("異常測試 - 上下文中存在錯誤類型的值")
    void testGetExchange_WrongTypeInContext() {
        // 執行測試：在上下文中存儲錯誤類型的值
        Mono<ServerWebExchange> result = CustomRequestContextHolder.getExchange()
                .contextWrite(Context.of("SERVER_WEB_EXCHANGE", "not_an_exchange"));

        // 驗證結果：類型轉換應該失敗，返回空 Mono
        StepVerifier.create(result)
                .expectError(ClassCastException.class)
                .verify();
    }

    // ==================== 邊界測試 ====================

    /**
     * 驗證 mutate 方法處理 null ServerWebExchange 的行為。
     * 
     * 測試傳入 null 值給 mutate 方法時的異常處理，驗證 Reactor Context 不支援 null 值的特性。
     * 
     * 前置條件：
     * - 準備空的 Context 實例
     * 
     * 測試步驟：
     * - 使用 null 值調用 mutate 方法
     * - 嘗試將結果函數應用到 Context
     * 
     * 預期結果：
     * - 拋出 NullPointerException
     */
    @Test
    @DisplayName("邊界測試 - mutate null ServerWebExchange")
    void testMutate_NullExchange() {
        // 準備測試資料
        Context emptyContext = Context.empty();
        
        // 執行測試：Reactor Context 不支持 null 值，應該會拋出異常
        assertThrows(NullPointerException.class, () -> {
            Function<Context, Context> mutateFunction = CustomRequestContextHolder.mutate(null);
            mutateFunction.apply(emptyContext);
        });
    }

    /**
     * 驗證 mutate 函數在 null 上下文上的行為。
     * 
     * 測試 mutate 方法返回的函數對空 Context 的處理能力。
     * 
     * 前置條件：
     * - 準備有效的 ServerWebExchange
     * 
     * 測試步驟：
     * - 獲取 mutate 函數
     * - 將函數應用到空 Context
     * 
     * 預期結果：
     * - 成功創建包含 exchange 的新 Context
     */
    @Test
    @DisplayName("邊界測試 - 在 null 上下文上使用 mutate")
    void testMutate_NullContext() {
        // 執行測試
        Function<Context, Context> mutateFunction = CustomRequestContextHolder.mutate(mockExchange);
        
        // 驗證：應該能處理 null 上下文（雖然實際上 Reactor 不會傳遞 null context）
        assertDoesNotThrow(() -> {
            Context resultContext = mutateFunction.apply(Context.empty());
            assertNotNull(resultContext);
        });
    }

    /**
     * 驗證嘗試存儲 null exchange 時的異常處理。
     * 
     * 測試 Reactor Context 不支援 null 值的特性，確認在嘗試存儲 null exchange 時的行為。
     * 
     * 前置條件：
     * - 無特殊前置條件
     * 
     * 測試步驟：
     * - 嘗試使用 null 值調用 mutate 並獲取結果
     * 
     * 預期結果：
     * - 拋出 NullPointerException
     */
    @Test
    @DisplayName("邊界測試 - 獲取存儲為 null 的 exchange")
    void testGetExchange_NullStoredExchange() {
        // 執行測試：嘗試存儲 null exchange，但 Reactor Context 不支持 null 值
        // 這個測試應該驗證當試圖使用 null 值時的行為
        assertThrows(NullPointerException.class, () -> {
            CustomRequestContextHolder.getExchange()
                    .contextWrite(CustomRequestContextHolder.mutate(null))
                    .block();
        });
    }

    /**
     * 驗證併發環境下的上下文隔離性。
     * 
     * 測試多線程環境中每個 Reactor Context 的獨立性，確保不同線程的上下文不會相互干擾。
     * 
     * 前置條件：
     * - 準備多線程執行環境
     * - 為每個線程準備獨立的 ServerWebExchange
     * 
     * 測試步驟：
     * - 啟動多個併發線程
     * - 每個線程使用獨立的 exchange 和上下文
     * - 驗證每個線程獲取到正確的 exchange
     * 
     * 預期結果：
     * - 所有線程都獲取到各自的 exchange
     * - 不存在上下文污染或混淆
     */
    @Test
    @DisplayName("邊界測試 - 併發環境下的上下文隔離")
    void testConcurrentContextIsolation() throws InterruptedException {
        // 準備測試資料
        ExecutorService executor = Executors.newFixedThreadPool(10);
        int numberOfThreads = 100;
        CompletableFuture<Void>[] futures = new CompletableFuture[numberOfThreads];

        try {
            // 執行測試：多個線程同時使用不同的 exchange
            for (int i = 0; i < numberOfThreads; i++) {
                final int threadId = i;
                ServerWebExchange threadExchange = mock(ServerWebExchange.class);
                ServerHttpRequest threadRequest = mock(ServerHttpRequest.class);
                when(threadExchange.getRequest()).thenReturn(threadRequest);
                when(threadRequest.getURI()).thenReturn(URI.create("http://localhost/thread-" + threadId));

                futures[i] = CompletableFuture.runAsync(() -> {
                    // 每個線程使用自己的上下文
                    Mono<String> result = CustomRequestContextHolder.getExchange()
                            .map(exchange -> exchange.getRequest().getURI().getPath())
                            .contextWrite(CustomRequestContextHolder.mutate(threadExchange));

                    StepVerifier.create(result)
                            .assertNext(path -> {
                                assertEquals("/thread-" + threadId, path);
                            })
                            .verifyComplete();
                }, executor);
            }

            // 等待所有線程完成
            try {
                CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
                fail("併發測試執行失敗: " + e.getMessage());
            }

        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /**
     * 驗證深層嵌套響應式鏈中的上下文傳遞。
     * 
     * 測試在多層 flatMap 嵌套操作中 Reactor Context 的傳遞一致性。
     * 
     * 前置條件：
     * - 已設定 mock ServerWebExchange
     * 
     * 測試步驟：
     * - 構建三層嵌套的 flatMap 操作
     * - 在最深層獲取 ServerWebExchange
     * - 驗證上下文傳遞的完整性
     * 
     * 預期結果：
     * - 最深層能正確獲取 ServerWebExchange
     * - 字串拼接結果包含正確的路徑資訊
     */
    @Test
    @DisplayName("邊界測試 - 深層嵌套的反應式鏈中的上下文傳遞")
    void testDeepNestedContextPropagation() {
        // 執行測試：在深層嵌套的反應式鏈中傳遞上下文
        Mono<String> result = Mono.just("level1")
                .flatMap(level1 -> 
                    Mono.just("level2")
                            .flatMap(level2 -> 
                                Mono.just("level3")
                                        .flatMap(level3 -> 
                                            CustomRequestContextHolder.getExchange()
                                                    .map(exchange -> level1 + "-" + level2 + "-" + level3 + 
                                                         exchange.getRequest().getURI().getPath()))))
                .contextWrite(CustomRequestContextHolder.mutate(mockExchange));

        // 驗證結果
        StepVerifier.create(result)
                .assertNext(value -> {
                    assertEquals("level1-level2-level3" + testPath, value);
                })
                .verifyComplete();
    }

    /**
     * 驗證不同訂閱間的上下文隔離。
     * 
     * 測試同一個操作的不同訂閱使用各自獨立的 Reactor Context，確保訂閱間不會相互影響。
     * 
     * 前置條件：
     * - 準備兩個不同的 ServerWebExchange 實例
     * 
     * 測試步驟：
     * - 創建兩個使用不同上下文的 Mono
     * - 分別訂閱並驗證結果
     * 
     * 預期結果：
     * - 每個訂閱獲取到各自設定的 exchange
     * - 訂閱間的上下文完全隔離
     */
    @Test
    @DisplayName("邊界測試 - 在不同的訂閱中的上下文隔離")
    void testContextIsolationBetweenSubscriptions() {
        // 準備另一個 exchange
        ServerWebExchange anotherExchange = mock(ServerWebExchange.class);
        ServerHttpRequest anotherRequest = mock(ServerHttpRequest.class);
        lenient().when(anotherExchange.getRequest()).thenReturn(anotherRequest);
        lenient().when(anotherRequest.getURI()).thenReturn(URI.create("http://localhost/another"));

        // 執行測試：兩個獨立的訂閱應該有隔離的上下文
        Mono<String> mono1 = CustomRequestContextHolder.getExchange()
                .map(exchange -> exchange.getRequest().getURI().getPath())
                .contextWrite(CustomRequestContextHolder.mutate(mockExchange));

        Mono<String> mono2 = CustomRequestContextHolder.getExchange()
                .map(exchange -> exchange.getRequest().getURI().getPath())
                .contextWrite(CustomRequestContextHolder.mutate(anotherExchange));

        // 驗證結果：每個訂閱應該獲取到自己的 exchange
        StepVerifier.create(mono1)
                .assertNext(path -> assertEquals(testPath, path))
                .verifyComplete();

        StepVerifier.create(mono2)
                .assertNext(path -> assertEquals("/another", path))
                .verifyComplete();
    }

    /**
     * 驗證空上下文與有效上下文的混合場景處理。
     * 
     * 測試在同一個響應式鏈中處理空上下文和有效上下文的切換行為。
     * 
     * 前置條件：
     * - 已設定 mock ServerWebExchange
     * 
     * 測試步驟：
     * - 先在空上下文中嘗試獲取 exchange
     * - 使用 switchIfEmpty 處理空結果
     * - 在後續操作中設定有效上下文
     * 
     * 預期結果：
     * - 空上下文時使用預設值
     * - 設定上下文後能正確獲取 exchange
     */
    @Test
    @DisplayName("邊界測試 - 空上下文與有效上下文的混合場景")
    void testMixedEmptyAndValidContext() {
        // 執行測試：先嘗試從空上下文獲取，然後設置上下文再獲取
        Mono<String> result = CustomRequestContextHolder.getExchange()
                .map(exchange -> "found")
                .switchIfEmpty(Mono.just("not-found"))
                .flatMap(firstResult -> 
                    CustomRequestContextHolder.getExchange()
                            .map(exchange -> firstResult + exchange.getRequest().getURI().getPath())
                            .contextWrite(CustomRequestContextHolder.mutate(mockExchange)));

        // 驗證結果
        StepVerifier.create(result)
                .assertNext(value -> {
                    assertEquals("not-found" + testPath, value);
                })
                .verifyComplete();
    }
}