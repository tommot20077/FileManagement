package xyz.dowob.filemanagement.unity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.response.WebSocketResponse;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.net.URI;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ResponseUnity 響應工具介面的單元測試。
 *
 * 測試統一響應處理介面的所有預設方法，包括API響應創建、錯誤處理、
 * WebSocket響應生成和異常映射功能。驗證響應格式化、狀態碼處理和錯誤轉換機制。
 *
 * 前置條件：
 * - Mock Spring WebFlux 相關組件
 * - 配置測試用的響應資料和狀態碼
 * - 設定ObjectMapper和DataBuffer模擬
 *
 * 測試步驟：
 * - 測試API響應創建的各種變體
 * - 驗證錯誤處理和異常映射
 * - 檢查WebSocket響應生成
 * - 測試邊界條件和異常情況
 *
 * 預期結果：
 * - 所有響應創建方法應正確執行
 * - 錯誤處理應正確映射各種異常類型
 * - WebSocket響應應包含正確的格式和內容
 * - 邊界條件應得到適當處理
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResponseUnity 介面功能測試")
class ResponseUnityTest {

    private final String testPath = "/test/path";
    
    private final String testMessage = "測試訊息";
    
    @Mock
    private ServerWebExchange exchange;
    
    @Mock
    private ServerHttpRequest request;
    
    @Mock
    private ServerHttpResponse response;
    
    @Mock
    private WebSocketSession webSocketSession;
    
    @Mock
    private HandshakeInfo handshakeInfo;
    
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DataBufferFactory dataBufferFactory;

    @Mock
    private DataBuffer dataBuffer;

    private ResponseUnity responseUnity;


    @BeforeEach
    void setUp() {
        // 創建 ResponseUnity 的實現實例用於測試
        responseUnity = new ResponseUnity() {};
        
        // 設定基本的 mock 行為 - 使用 lenient 避免 UnnecessaryStubbingException
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getResponse()).thenReturn(response);
        lenient().when(request.getURI()).thenReturn(URI.create("http://localhost" + testPath));
        lenient().when(request.getPath()).thenReturn(org.springframework.http.server.RequestPath.parse(testPath, null));
        
        // WebSocket 相關設定
        lenient().when(webSocketSession.getHandshakeInfo()).thenReturn(handshakeInfo);
        lenient().when(handshakeInfo.getUri()).thenReturn(URI.create("ws://localhost" + testPath));
        
        // Response 相關設定
        lenient().when(response.bufferFactory()).thenReturn(dataBufferFactory);
        lenient().when(dataBufferFactory.wrap(any(byte[].class))).thenReturn(dataBuffer);
        
        // 創建 mock HttpHeaders
        HttpHeaders mockHeaders = mock(HttpHeaders.class);
        lenient().when(response.getHeaders()).thenReturn(mockHeaders);
    }

    // ==================== 一般測試 ====================

    /**
     * 測試創建ResponseEntity使用預設狀態碼的功能。
     *
     * 驗證當不指定自訂狀態碼時，ResponseEntity能夠使用ApiResponse中的狀態碼創建響應。
     *
     * 前置條件：
     * - 創建包含狀態碼200的ApiResponseDTO實例
     *
     * 測試步驟：
     * - 調用createResponseEntity方法（不指定狀態碼）
     * - 驗證返回的ResponseEntity狀態碼和內容
     *
     * 預期結果：
     * - ResponseEntity狀態碼應與ApiResponse中的狀態碼一致
     * - 響應體應包含正確的ApiResponse內容
     */
    @Test
    @DisplayName("一般測試 - 創建 ResponseEntity (預設狀態碼)")
    void testCreateResponseEntity_DefaultStatus() {
        // 準備測試資料
        ApiResponseDTO<String> apiResponse = new ApiResponseDTO<>(
            LocalDateTime.now(), 200, testPath, testMessage, "testData"
        );

        // 執行測試
        StepVerifier.create(responseUnity.createResponseEntity(apiResponse))
                .assertNext(responseEntity -> {
                    assertEquals(200, responseEntity.getStatusCode().value());
                    assertEquals(apiResponse, responseEntity.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 創建 ResponseEntity (指定狀態碼)")
    void testCreateResponseEntity_CustomStatus() {
        // 準備測試資料
        ApiResponseDTO<String> apiResponse = new ApiResponseDTO<>(
            LocalDateTime.now(), 400, testPath, testMessage, "testData"
        );
        int customStatus = 201;

        // 執行測試
        StepVerifier.create(responseUnity.createResponseEntity(apiResponse, customStatus))
                .assertNext(responseEntity -> {
                    assertEquals(customStatus, responseEntity.getStatusCode().value());
                    assertEquals(apiResponse, responseEntity.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 創建 ResponseEntity (帶 Headers)")
    void testCreateResponseEntity_WithHeaders() {
        // 準備測試資料
        ApiResponseDTO<String> apiResponse = new ApiResponseDTO<>(
            LocalDateTime.now(), 200, testPath, testMessage, "testData"
        );
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Custom-Header", "HeaderValue");

        // 執行測試
        StepVerifier.create(responseUnity.createResponseEntity(apiResponse, headers))
                .assertNext(responseEntity -> {
                    assertEquals(200, responseEntity.getStatusCode().value());
                    assertEquals(apiResponse, responseEntity.getBody());
                    assertEquals("HeaderValue", responseEntity.getHeaders().getFirst("Custom-Header"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 創建 ApiResponse (指定路徑)")
    void testCreateApiResponse_WithPath() {
        // 準備測試資料
        String testData = "testData";
        int status = 200;

        // 執行測試
        ApiResponseDTO<String> result = responseUnity.createApiResponse(testPath, status, testMessage, testData);

        // 驗證結果
        assertNotNull(result);
        assertEquals(status, result.getStatus());
        assertEquals(testPath, result.getPath());
        assertEquals(testMessage, result.getMessage());
        assertEquals(testData, result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("一般測試 - 創建 ApiResponse (WebSocket)")
    void testCreateApiResponse_WebSocket() {
        // 準備測試資料
        String testData = "testData";
        int status = 200;

        // 執行測試
        ApiResponseDTO<String> result = responseUnity.createApiResponse(webSocketSession, status, testMessage, testData);

        // 驗證結果
        assertNotNull(result);
        assertEquals(status, result.getStatus());
        assertEquals(testPath, result.getPath());
        assertEquals(testMessage, result.getMessage());
        assertEquals(testData, result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("一般測試 - 創建 ApiResponse (ServerWebExchange)")
    void testCreateApiResponse_ServerWebExchange() {
        // 準備測試資料
        String testData = "testData";

        // 執行測試
        ApiResponseDTO<String> result = responseUnity.createApiResponse(exchange, testMessage, testData);

        // 驗證結果
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertEquals(testPath, result.getPath());
        assertEquals(testMessage, result.getMessage());
        assertEquals(testData, result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("一般測試 - 創建 ApiResponse (ServerWebExchange 帶狀態碼)")
    void testCreateApiResponse_ServerWebExchangeWithStatus() {
        // 準備測試資料
        String testData = "testData";
        int status = 201;

        // 執行測試
        ApiResponseDTO<String> result = responseUnity.createApiResponse(exchange, status, testMessage, testData);

        // 驗證結果
        assertNotNull(result);
        assertEquals(status, result.getStatus());
        assertEquals(testPath, result.getPath());
        assertEquals(testMessage, result.getMessage());
        assertEquals(testData, result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("一般測試 - 創建 WebSocket 響應 (帶資料)")
    void testCreateWebSocketResponse_WithData() {
        // 準備測試資料
        String type = "INFO";
        String data = "testData";

        // 執行測試
        WebSocketResponse<String> result = responseUnity.createWebSocketResponse(type, testMessage, data);

        // 驗證結果
        assertNotNull(result);
        assertEquals(type, result.getType());
        assertEquals(testMessage, result.getMessage());
        assertEquals(data, result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("一般測試 - 創建 WebSocket 響應 (無資料)")
    void testCreateWebSocketResponse_WithoutData() {
        // 準備測試資料
        String type = "INFO";

        // 執行測試
        WebSocketResponse<String> result = responseUnity.createWebSocketResponse(type, testMessage);

        // 驗證結果
        assertNotNull(result);
        assertEquals(type, result.getType());
        assertEquals(testMessage, result.getMessage());
        assertNull(result.getData());
        assertNotNull(result.getTimestamp());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - 處理 ValidationException")
    void testHandleError_ValidationException() {
        // 準備測試資料
        ValidationException validationException = new ValidationException(
            ValidationException.ErrorCode.NULL_DTO, "測試參數"
        );
        Mono<ResponseEntity<?>> operation = Mono.error(validationException);

        // 執行測試
        StepVerifier.create(responseUnity.handleError(operation, exchange))
                .assertNext(responseEntity -> {
                    assertEquals(validationException.getErrorCode().getHttpStatus().value(), 
                               responseEntity.getStatusCode().value());
                    ApiResponseDTO<?> body = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(body);
                    assertTrue(body.getMessage().contains("處理失敗"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 處理 LimitationException")
    void testHandleError_LimitationException() {
        // 準備測試資料
        LimitationException limitationException = new LimitationException(
            LimitationException.ErrorCode.USER_EXCEED_LIMIT, "測試用戶限制"
        );
        Mono<ResponseEntity<?>> operation = Mono.error(limitationException);

        // 執行測試
        StepVerifier.create(responseUnity.handleError(operation, exchange))
                .assertNext(responseEntity -> {
                    assertEquals(limitationException.getErrorCode().getHttpStatus().value(), 
                               responseEntity.getStatusCode().value());
                    ApiResponseDTO<?> body = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(body);
                    assertTrue(body.getMessage().contains("限制錯誤"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 發送錯誤響應 (LimitationException)")
    void testSendErrorResponse_LimitationException() throws JsonProcessingException {
        // 準備測試資料
        LimitationException.ErrorCode errorCode = LimitationException.ErrorCode.USER_EXCEED_LIMIT;
        String errorMessage = "請求過於頻繁";
        
        when(response.isCommitted()).thenReturn(false);
        when(objectMapper.writeValueAsBytes(any(ApiResponseDTO.class))).thenReturn("{}".getBytes());
        when(response.writeWith(any())).thenReturn(Mono.empty());

        // 執行測試
        StepVerifier.create(responseUnity.sendErrorResponse(exchange, objectMapper, errorCode, errorMessage))
                .verifyComplete();

        // 驗證互動
        verify(response).setStatusCode(errorCode.getHttpStatus());
        verify(response.getHeaders()).set(HttpHeaders.CONTENT_TYPE, "application/json");
        verify(response).writeWith(any());
    }

    @Test
    @DisplayName("異常測試 - 發送錯誤響應 (ValidationException)")
    void testSendErrorResponse_ValidationException() throws JsonProcessingException {
        // 準備測試資料
        ValidationException.ErrorCode errorCode = ValidationException.ErrorCode.NULL_DTO;
        Object[] args = {"測試參數"};
        
        when(response.isCommitted()).thenReturn(false);
        when(objectMapper.writeValueAsBytes(any(ApiResponseDTO.class))).thenReturn("{}".getBytes());
        when(response.writeWith(any())).thenReturn(Mono.empty());

        // 執行測試
        StepVerifier.create(responseUnity.sendErrorResponse(exchange, objectMapper, errorCode, args))
                .verifyComplete();

        // 驗證互動
        verify(response).setStatusCode(errorCode.getHttpStatus());
        verify(response.getHeaders()).set(HttpHeaders.CONTENT_TYPE, "application/json");
        verify(response).writeWith(any());
    }

    @Test
    @DisplayName("異常測試 - 響應已提交時發送錯誤響應")
    void testSendErrorResponse_ResponseCommitted() {
        // 準備測試資料
        when(response.isCommitted()).thenReturn(true);

        // 執行測試
        StepVerifier.create(responseUnity.sendErrorResponse(
            exchange, objectMapper, "錯誤訊息", 400, HttpStatus.BAD_REQUEST))
                .verifyComplete();

        // 驗證沒有設置響應
        verify(response, never()).setStatusCode(any());
        verify(response, never()).writeWith(any());
    }

    @Test
    @DisplayName("異常測試 - JSON 處理異常")
    void testSendErrorResponse_JsonProcessingException() throws JsonProcessingException {
        // 準備測試資料
        when(response.isCommitted()).thenReturn(false);
        when(objectMapper.writeValueAsBytes(any(ApiResponseDTO.class)))
                .thenThrow(new JsonProcessingException("JSON轉換失敗") {});

        // 執行測試
        StepVerifier.create(responseUnity.sendErrorResponse(
            exchange, objectMapper, "錯誤訊息", 400, HttpStatus.BAD_REQUEST))
                .expectError(ProcessException.class)
                .verify();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - ResponseEntity 狀態碼為 null")
    void testCreateResponseEntity_NullStatusCode() {
        // 準備測試資料
        ApiResponseDTO<String> apiResponse = new ApiResponseDTO<>(
            LocalDateTime.now(), 200, testPath, testMessage, "testData"
        );

        // 執行測試 (null 狀態碼應該默認為 200)
        StepVerifier.create(responseUnity.createResponseEntity(apiResponse, (Integer) null))
                .assertNext(responseEntity -> {
                    assertEquals(200, responseEntity.getStatusCode().value());
                    assertEquals(apiResponse, responseEntity.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - ResponseEntity 非成功狀態碼")
    void testCreateResponseEntity_FailureStatus() {
        // 準備測試資料
        ApiResponseDTO<String> apiResponse = new ApiResponseDTO<>(
            LocalDateTime.now(), 500, testPath, testMessage, "testData"
        );

        // 執行測試 (非200狀態碼應該映射為400)
        StepVerifier.create(responseUnity.createResponseEntity(apiResponse))
                .assertNext(responseEntity -> {
                    assertEquals(400, responseEntity.getStatusCode().value());
                    assertEquals(apiResponse, responseEntity.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極大狀態碼")
    void testCreateResponseEntity_LargeStatusCode() {
        // 準備測試資料
        ApiResponseDTO<String> apiResponse = new ApiResponseDTO<>(
            LocalDateTime.now(), 200, testPath, testMessage, "testData"
        );
        int largeStatus = 999;

        // 執行測試
        StepVerifier.create(responseUnity.createResponseEntity(apiResponse, largeStatus))
                .assertNext(responseEntity -> {
                    assertEquals(largeStatus, responseEntity.getStatusCode().value());
                    assertEquals(apiResponse, responseEntity.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - null 資料的 ApiResponse")
    void testCreateApiResponse_NullData() {
        // 執行測試
        ApiResponseDTO<String> result = responseUnity.createApiResponse(testPath, 200, testMessage, null);

        // 驗證結果
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertEquals(testPath, result.getPath());
        assertEquals(testMessage, result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("邊界測試 - 空字符串參數")
    void testCreateApiResponse_EmptyStrings() {
        // 準備測試資料
        String emptyPath = "";
        String emptyMessage = "";
        String emptyData = "";

        // 執行測試
        ApiResponseDTO<String> result = responseUnity.createApiResponse(emptyPath, 200, emptyMessage, emptyData);

        // 驗證結果
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertEquals(emptyPath, result.getPath());
        assertEquals(emptyMessage, result.getMessage());
        assertEquals(emptyData, result.getData());
    }

    @Test
    @DisplayName("邊界測試 - WebSocket 響應 null 類型")
    void testCreateWebSocketResponse_NullType() {
        // 執行測試
        WebSocketResponse<String> result = responseUnity.createWebSocketResponse(null, testMessage, "data");

        // 驗證結果
        assertNotNull(result);
        assertNull(result.getType());
        assertEquals(testMessage, result.getMessage());
        assertEquals("data", result.getData());
    }

    @Test
    @DisplayName("邊界測試 - 零狀態碼")
    void testCreateApiResponse_ZeroStatusCode() {
        // 執行測試
        ApiResponseDTO<String> result = responseUnity.createApiResponse(testPath, 0, testMessage, "data");

        // 驗證結果
        assertNotNull(result);
        assertEquals(0, result.getStatus());
        assertEquals(testPath, result.getPath());
        assertEquals(testMessage, result.getMessage());
        assertEquals("data", result.getData());
    }
}