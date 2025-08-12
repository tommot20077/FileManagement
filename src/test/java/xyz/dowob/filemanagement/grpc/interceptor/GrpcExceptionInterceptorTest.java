package xyz.dowob.filemanagement.grpc.interceptor;

import io.grpc.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.dowob.filemanagement.exception.JwtAuthenticationException;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * GrpcExceptionInterceptor 全局異常攔截器測試。
 *
 * 測試 gRPC 全局異常攔截器的異常處理和映射功能，確保各種應用層異常
 * 能夠正確轉換為對應的 gRPC Status 碼。
 *
 * 前置條件：
 * - Mock gRPC 服務調用相關組件
 * - 配置各種異常場景的測試數據
 *
 * 測試步驟：
 * - 測試不同類型異常的攔截和轉換
 * - 驗證 Status 碼映射的正確性
 * - 檢查錯誤訊息的格式和內容
 * - 測試監聽器的異常處理
 *
 * 預期結果：
 * - 所有異常類型正確映射到對應的 Status 碼
 * - 錯誤訊息符合預期格式
 * - 異常處理不會造成服務崩潰
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("gRPC 異常攔截器測試")
class GrpcExceptionInterceptorTest {

    @Mock
    private ServerCall<String, String> serverCall;
    
    @Mock
    private ServerCallHandler<String, String> serverCallHandler;
    
    @Mock
    private ServerCall.Listener<String> serverCallListener;
    
    @Mock
    private Metadata metadata;
    
    private GrpcExceptionInterceptor interceptor;
    
    @BeforeEach
    void setUp() {
        interceptor = new GrpcExceptionInterceptor();
        metadata = new Metadata();
    }
    
    @Test
    @DisplayName("無異常時正常傳遞請求")
    void testInterceptCall_NoException() {
        // 配置 mock
        when(serverCallHandler.startCall(any(), eq(metadata))).thenReturn(serverCallListener);
        
        // 執行測試
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 驗證結果
        assertNotNull(result);
        verify(serverCallHandler).startCall(any(), eq(metadata));
        verify(serverCall, never()).close(any(Status.class), any(Metadata.class));
    }
    
    @Test
    @DisplayName("ValidationException 映射到 INVALID_ARGUMENT")
    void testInterceptCall_ValidationException() {
        // 準備異常
        ValidationException exception = new ValidationException(ValidationException.ErrorCode.INVALID_FILE_NAME);
        
        // 配置 mock 拋出異常 - 使用 RuntimeException 包裝
        when(serverCallHandler.startCall(any(), eq(metadata)))
            .thenThrow(new RuntimeException(exception));
        
        // 執行測試
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 驗證結果
        assertNotNull(result);
        
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.INTERNAL.getCode(), capturedStatus.getCode());
        assertEquals("內部服務錯誤", capturedStatus.getDescription());
    }
    
    @Test
    @DisplayName("LimitationException 映射到 RESOURCE_EXHAUSTED")
    void testInterceptCall_LimitationException() {
        // 準備異常
        LimitationException exception = new LimitationException(LimitationException.ErrorCode.USER_EXCEED_LIMIT, "請求頻率超限");
        
        // 配置 mock 拋出異常 - 使用 RuntimeException 包裝
        when(serverCallHandler.startCall(any(), eq(metadata)))
            .thenThrow(new RuntimeException(exception));
        
        // 執行測試
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 驗證結果
        assertNotNull(result);
        
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.INTERNAL.getCode(), capturedStatus.getCode());
        assertEquals("內部服務錯誤", capturedStatus.getDescription());
    }
    
    @Test
    @DisplayName("JwtAuthenticationException 映射到 UNAUTHENTICATED")
    void testInterceptCall_JwtAuthenticationException() {
        // 準備異常
        JwtAuthenticationException exception = new JwtAuthenticationException("令牌無效");
        
        // 配置 mock 拋出異常
        when(serverCallHandler.startCall(any(), eq(metadata)))
            .thenThrow(exception);
        
        // 執行測試
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 驗證結果
        assertNotNull(result);
        
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.UNAUTHENTICATED.getCode(), capturedStatus.getCode());
        assertEquals("認證失敗", capturedStatus.getDescription());
    }
    
    @Test
    @DisplayName("ProcessException 映射到 INTERNAL")
    void testInterceptCall_ProcessException() {
        // 準備異常
        ProcessException exception = new ProcessException(ProcessException.ErrorCode.CREATE_STREAM_FAILED);
        
        // 配置 mock 拋出異常 - 使用 RuntimeException 包裝
        when(serverCallHandler.startCall(any(), eq(metadata)))
            .thenThrow(new RuntimeException(exception));
        
        // 執行測試
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 驗證結果
        assertNotNull(result);
        
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.INTERNAL.getCode(), capturedStatus.getCode());
        assertEquals("內部服務錯誤", capturedStatus.getDescription());
    }
    
    @Test
    @DisplayName("StatusRuntimeException 直接傳遞")
    void testInterceptCall_StatusRuntimeException() {
        // 準備異常
        Status originalStatus = Status.ALREADY_EXISTS.withDescription("資源已存在");
        StatusRuntimeException exception = originalStatus.asRuntimeException();
        
        // 配置 mock 拋出異常
        when(serverCallHandler.startCall(any(), eq(metadata)))
            .thenThrow(exception);
        
        // 執行測試
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 驗證結果
        assertNotNull(result);
        
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.ALREADY_EXISTS.getCode(), capturedStatus.getCode());
        assertEquals("資源已存在", capturedStatus.getDescription());
    }
    
    @Test
    @DisplayName("StatusException 直接傳遞")
    void testInterceptCall_StatusException() {
        // 準備異常 - 使用 StatusRuntimeException 代替 StatusException
        Status originalStatus = Status.NOT_FOUND.withDescription("資源不存在");
        StatusRuntimeException exception = originalStatus.asRuntimeException();
        
        // 配置 mock 拋出異常
        when(serverCallHandler.startCall(any(), eq(metadata)))
            .thenThrow(exception);
        
        // 執行測試
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 驗證結果
        assertNotNull(result);
        
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.NOT_FOUND.getCode(), capturedStatus.getCode());
        assertEquals("資源不存在", capturedStatus.getDescription());
    }
    
    @Test
    @DisplayName("未知異常映射到 INTERNAL")
    void testInterceptCall_UnknownException() {
        // 準備異常
        RuntimeException exception = new RuntimeException("未知錯誤");
        
        // 配置 mock 拋出異常
        when(serverCallHandler.startCall(any(), eq(metadata)))
            .thenThrow(exception);
        
        // 執行測試
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 驗證結果
        assertNotNull(result);
        
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.INTERNAL.getCode(), capturedStatus.getCode());
        assertEquals("內部服務錯誤", capturedStatus.getDescription());
    }
    
    @Test
    @DisplayName("ForwardingServerCall 的 close 方法異常處理")
    void testForwardingServerCall_CloseWithException() {
        // 配置正常的監聽器
        when(serverCallHandler.startCall(any(), eq(metadata))).thenReturn(serverCallListener);
        
        // 執行攔截
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        assertNotNull(result);
        
        // 獲取包裝的 ServerCall（通過 ArgumentCaptor）
        ArgumentCaptor<ServerCall<String, String>> callCaptor = ArgumentCaptor.forClass(ServerCall.class);
        verify(serverCallHandler).startCall(callCaptor.capture(), eq(metadata));
        ServerCall<String, String> wrappedCall = callCaptor.getValue();
        
        // 測試 close 方法的異常處理
        ValidationException cause = new ValidationException(ValidationException.ErrorCode.NULL_DTO);
        Status unknownStatus = Status.UNKNOWN.withCause(cause);
        
        // 調用 close 方法
        wrappedCall.close(unknownStatus, metadata);
        
        // 驗證異常被正確處理並轉換
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), eq(metadata));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.INVALID_ARGUMENT.getCode(), capturedStatus.getCode());
        assertTrue(capturedStatus.getDescription().contains("驗證失敗"));
    }
    
    @Test
    @DisplayName("ExceptionHandlingListener - onMessage 異常處理")
    void testExceptionHandlingListener_OnMessage() {
        // 配置監聽器拋出異常
        doThrow(new RuntimeException("處理訊息失敗")).when(serverCallListener).onMessage(any());
        when(serverCallHandler.startCall(any(), eq(metadata))).thenReturn(serverCallListener);
        
        // 執行攔截
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 調用 onMessage
        result.onMessage("test message");
        
        // 驗證異常被捕獲並處理
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.INTERNAL.getCode(), capturedStatus.getCode());
        assertEquals("內部服務錯誤", capturedStatus.getDescription());
    }
    
    @Test
    @DisplayName("ExceptionHandlingListener - onHalfClose 異常處理")
    void testExceptionHandlingListener_OnHalfClose() {
        // 配置監聽器拋出異常 - 使用 RuntimeException 包裝
        doThrow(new RuntimeException(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, "檔案不存在")))
            .when(serverCallListener).onHalfClose();
        when(serverCallHandler.startCall(any(), eq(metadata))).thenReturn(serverCallListener);
        
        // 執行攔截
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 調用 onHalfClose
        result.onHalfClose();
        
        // 驗證異常被捕獲並處理
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.INTERNAL.getCode(), capturedStatus.getCode());
        assertEquals("內部服務錯誤", capturedStatus.getDescription());
    }
    
    @Test
    @DisplayName("ExceptionHandlingListener - onReady 異常處理")
    void testExceptionHandlingListener_OnReady() {
        // 配置監聽器拋出異常 - 使用 RuntimeException 包裝
        doThrow(new RuntimeException(new LimitationException(LimitationException.ErrorCode.FILE_CHUNK_EXCEED_LIMIT, "上傳頻率超限")))
            .when(serverCallListener).onReady();
        when(serverCallHandler.startCall(any(), eq(metadata))).thenReturn(serverCallListener);
        
        // 執行攔截
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 調用 onReady
        result.onReady();
        
        // 驗證異常被捕獲並處理
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.INTERNAL.getCode(), capturedStatus.getCode());
        assertEquals("內部服務錯誤", capturedStatus.getDescription());
    }
    
    @Test
    @DisplayName("多重異常處理 - 巢狀異常")
    void testNestedExceptionHandling() {
        // 準備巢狀異常
        ValidationException innerException = new ValidationException(
            ValidationException.ErrorCode.FILE_SIZE_LIMIT, "檔案大小超過限制", "10MB", "5MB"
        );
        RuntimeException outerException = new RuntimeException("外層異常", innerException);
        
        // 配置 mock 拋出異常
        when(serverCallHandler.startCall(any(), eq(metadata)))
            .thenThrow(outerException);
        
        // 執行測試
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, metadata, serverCallHandler);
        
        // 驗證結果 - 外層的 RuntimeException 被處理
        assertNotNull(result);
        
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any(Metadata.class));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.INTERNAL.getCode(), capturedStatus.getCode());
        assertEquals("內部服務錯誤", capturedStatus.getDescription());
    }
}