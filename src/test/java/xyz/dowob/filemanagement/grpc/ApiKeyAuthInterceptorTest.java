package xyz.dowob.filemanagement.grpc;

import io.grpc.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.grpc.interceptor.ApiKeyAuthInterceptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApiKeyAuthInterceptorTest {
    
    private static final String VALID_API_KEY = "test-api-key";
    
    private static final String METHOD_NAME = "xyz.dowob.filemanagement.grpc.FileProcessingService/ResolvePath";
    
    @Mock
    private ServerCall<String, String> serverCall;
    
    @Mock
    private ServerCallHandler<String, String> serverCallHandler;
    
    @Mock
    private ServerCall.Listener<String> serverCallListener;

    @Mock
    private MethodDescriptor<String, String> methodDescriptor;
    
    @Mock
    private GlobalProperties globalProperties;
    
    @Mock
    private GlobalProperties.WebDav webDavProperties;
    
    private ApiKeyAuthInterceptor interceptor;

    private Metadata headers;
    

    @BeforeEach
    void setUp() {
        // 設置 GlobalProperties 和 WebDav 的 Mock 行為
        when(globalProperties.getWebdav()).thenReturn(webDavProperties);
        when(webDavProperties.getApiKey()).thenReturn(VALID_API_KEY);
        
        // 使用帶參數的建構子創建 interceptor
        interceptor = new ApiKeyAuthInterceptor(globalProperties);
        headers = new Metadata();
        
        // 模擬方法描述器
        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);
        when(methodDescriptor.getFullMethodName()).thenReturn(METHOD_NAME);
        // 移除不必要的 startCall stub，將在需要的測試中單獨配置
    }
    
    @Test
    void testInterceptCall_ValidApiKey() {
        // 設置有效的 API Key
        Metadata.Key<String> apiKeyHeader = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(apiKeyHeader, VALID_API_KEY);
        
        // 模擬成功的處理器調用
        when(serverCallHandler.startCall(eq(serverCall), eq(headers))).thenReturn(serverCallListener);
        
        // 執行攔截
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, headers, serverCallHandler);
        
        // 驗證結果
        assertSame(serverCallListener, result);
        verify(serverCallHandler).startCall(serverCall, headers);
        verify(serverCall, never()).close(any(Status.class), any(Metadata.class));
    }
    
    @Test
    void testInterceptCall_MissingApiKey() {
        // 不設置 API Key（headers 為空）
        
        // 執行攔截
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, headers, serverCallHandler);
        
        // 驗證結果
        assertNotSame(serverCallListener, result);
        
        // 驗證調用被拒絕
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), eq(headers));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.UNAUTHENTICATED.getCode(), capturedStatus.getCode());
        assertEquals("缺少 API 令牌", capturedStatus.getDescription());
        
        // 驗證沒有調用下一個處理器
        verify(serverCallHandler, never()).startCall(any(), any());
    }
    
    @Test
    void testInterceptCall_InvalidApiKey() {
        // 設置無效的 API Key
        Metadata.Key<String> apiKeyHeader = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(apiKeyHeader, "invalid-api-key");
        
        // 執行攔截
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, headers, serverCallHandler);
        
        // 驗證結果
        assertNotSame(serverCallListener, result);
        
        // 驗證調用被拒絕
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), eq(headers));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.UNAUTHENTICATED.getCode(), capturedStatus.getCode());
        assertEquals("錯誤的 API 令牌", capturedStatus.getDescription());
        
        // 驗證沒有調用下一個處理器
        verify(serverCallHandler, never()).startCall(any(), any());
    }
    
    @Test
    void testInterceptCall_EmptyApiKey() {
        // 設置空的 API Key
        Metadata.Key<String> apiKeyHeader = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(apiKeyHeader, "");
        
        // 執行攔截
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, headers, serverCallHandler);
        
        // 驗證結果
        assertNotSame(serverCallListener, result);
        
        // 驗證調用被拒絕
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), eq(headers));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.UNAUTHENTICATED.getCode(), capturedStatus.getCode());
        assertEquals("錯誤的 API 令牌", capturedStatus.getDescription());
        
        // 驗證沒有調用下一個處理器
        verify(serverCallHandler, never()).startCall(any(), any());
    }
    
    @Test
    void testInterceptCall_NullApiKey() {
        // 不設置 API Key（headers 中不包含 x-api-key）
        // 不需要將 null 放入 headers，因為這會導致 NullPointerException
        
        // 執行攔截
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, headers, serverCallHandler);
        
        // 驗證結果
        assertNotSame(serverCallListener, result);
        
        // 驗證調用被拒絕
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), eq(headers));
        
        Status capturedStatus = statusCaptor.getValue();
        assertEquals(Status.UNAUTHENTICATED.getCode(), capturedStatus.getCode());
        assertEquals("缺少 API 令牌", capturedStatus.getDescription());
        
        // 驗證沒有調用下一個處理器
        verify(serverCallHandler, never()).startCall(any(), any());
    }
    
    @Test
    void testInterceptCall_DifferentMethodName() {
        // 設置有效的 API Key
        Metadata.Key<String> apiKeyHeader = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(apiKeyHeader, VALID_API_KEY);
        
        // 設置不同的方法名稱
        when(methodDescriptor.getFullMethodName()).thenReturn("xyz.dowob.filemanagement.grpc.FileProcessingService/ListFolder");
        
        // 模擬成功的處理器調用
        when(serverCallHandler.startCall(eq(serverCall), eq(headers))).thenReturn(serverCallListener);
        
        // 執行攔截
        ServerCall.Listener<String> result = interceptor.interceptCall(serverCall, headers, serverCallHandler);
        
        // 驗證結果（應該仍然通過，因為 API Key 是有效的）
        assertSame(serverCallListener, result);
        verify(serverCallHandler).startCall(serverCall, headers);
        verify(serverCall, never()).close(any(Status.class), any(Metadata.class));
    }
}