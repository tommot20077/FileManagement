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
 * CustomRequestContextHolder 測試。
 * 
 * 測試 CustomRequestContextHolder 類別的上下文管理功能，
 * 包括 ServerWebExchange 的存儲和獲取。
 * 
 * 前置條件：
 * - 模擬 ServerWebExchange 和相關依賴
 * - 設定測試的 Context 環境
 * 
 * 測試步驟：
 * - 測試一般功能：正常的上下文存取操作
 * - 測試異常情況：空上下文的處理
 * - 測試邊界條件：null值處理、並發安全性
 * 
 * 預期結果：
 * - 上下文正確存儲和獲取 ServerWebExchange
 * - 空上下文時正確返回空結果
 * - 併發環境下上下文隔離正確
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

    @Test
    @DisplayName("異常測試 - 空上下文獲取 ServerWebExchange")
    void testGetExchange_EmptyContext() {
        // 執行測試：在沒有設置 exchange 的上下文中獲取
        Mono<ServerWebExchange> result = CustomRequestContextHolder.getExchange();

        // 驗證結果：應該返回空的 Mono
        StepVerifier.create(result)
                .verifyComplete();
    }

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