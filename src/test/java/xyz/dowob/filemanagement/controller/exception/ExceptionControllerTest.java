package xyz.dowob.filemanagement.controller.exception;

import io.r2dbc.spi.R2dbcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.*;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ExceptionController 測試類別
 * 
 * <p>全面測試全域異常控制器 {@link xyz.dowob.filemanagement.controller.exception.ExceptionController} 的各種異常處理功能，
 * 確保系統在各種異常情況下都能正確響應並提供適當的錯誤信息。</p>
 * 
 * <p>測試範圍包括：
 * 
 * - ValidationException 驗證異常處理
 * - 404 找不到資源異常處理
 * - 405 方法不允許異常處理
 * - 415 不支持的媒體類型異常處理
 * - 轉換失敗異常處理機制
 * - 資料驗證和綁定異常處理
 * - JSON 格式錯誤異常處理
 * - 不支持的操作異常處理
 * - R2dbc 資料庫異常處理
 * - 資料庫操作異常處理
 * - 重試次數過多異常處理
 * - 未知異常的默認處理機制
 * 
 * </p>
 * 
 * <p>測試前置條件：
 * 
 * - ExceptionController 正常實例化
 * - MockServerWebExchange 環境正確設置
 * - 各種異常對象可正常模擬
 * - LogUnity 日誌記錄功能可用
 * 
 * </p>
 * 
 * <p>測試策略：
 * 
 * - 建立各種異常實例並配置模擬環境
 * - 調用對應的異常處理方法進行測試
 * - 驗證返回的 ResponseEntity 狀態碼和內容
 * - 檢查異常處理邏輯的正確性和完整性
 * - 驗證註解配置和方法簽名規範
 * 
 * </p>
 * 
 * <p>預期測試結果：
 * 
 * - 所有異常都能被正確處理並返回適當的 HTTP 狀態碼
 * - 返回的 ApiResponseDTO 包含準確的錯誤信息
 * - 異常處理機制健壯且用戶友好
 * - 日誌記錄和錯誤追蹤功能正常
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ExceptionController 全域異常控制器測試")
class ExceptionControllerTest {

    private ExceptionController exceptionController;
    private ServerWebExchange testExchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        exceptionController = new ExceptionController();
        
        MockServerHttpRequest request = MockServerHttpRequest.get("/test/endpoint").build();
        testExchange = MockServerWebExchange.from(request);
        testExchange.getAttributes().put("requestId", "test-request-123");
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 控制器初始化")
    void testControllerInitialization() {
        assertNotNull(exceptionController);
        assertTrue(exceptionController instanceof xyz.dowob.filemanagement.unity.ResponseUnity);
    }

    @Test
    @DisplayName("一般測試 - handleValidationException 處理驗證異常")
    void testHandleValidationException_basicFunctionality() {
        ValidationException validationException = new ValidationException(
            ValidationException.ErrorCode.REQUEST_IS_INVALID, "testField"
        );

        StepVerifier.create(exceptionController.handleValidationException(validationException, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(400, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals(ValidationException.ErrorCode.REQUEST_IS_INVALID.getCode(), response.getStatus());
                    assertEquals(ValidationException.ErrorCode.REQUEST_IS_INVALID.getMessage(), response.getMessage());
                    assertEquals("/test/endpoint", response.getPath());
                    assertNull(response.getData());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - handleNotFound 處理 404 異常")
    void testHandleNotFound_noResourceFoundException() {
        NoResourceFoundException notFoundException = new NoResourceFoundException("Resource not found");

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleNotFound(notFoundException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(404, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals(404, response.getStatus());
                        assertEquals("請求的資源不存在", response.getMessage());
                        assertEquals("/test/endpoint", response.getPath());
                    })
                    .verifyComplete();

            logUnityMock.verify(() -> LogUnity.debug(eq(testExchange), anyString(), anyString(), anyString()));
        }
    }

    @Test
    @DisplayName("一般測試 - handleHttpRequestMethodNotSupportedException 處理 405 異常")
    void testHandleHttpRequestMethodNotSupportedException_basicFunctionality() {
        MethodNotAllowedException methodNotAllowedException = new MethodNotAllowedException(
            org.springframework.http.HttpMethod.GET, java.util.Set.of(org.springframework.http.HttpMethod.POST, org.springframework.http.HttpMethod.PUT)
        );

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleHttpRequestMethodNotSupportedException(methodNotAllowedException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(405, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals(405, response.getStatus());
                        assertEquals("不支持的請求方法", response.getMessage());
                    })
                    .verifyComplete();

            logUnityMock.verify(() -> LogUnity.debug(eq(testExchange), anyString(), anyString()));
        }
    }

    @Test
    @DisplayName("一般測試 - handleUnsupportedMediaTypeStatusException 處理 415 異常")
    void testHandleUnsupportedMediaTypeStatusException_basicFunctionality() {
        UnsupportedMediaTypeStatusException unsupportedMediaTypeException = 
            new UnsupportedMediaTypeStatusException("Unsupported media type");

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleUnsupportedMediaTypeStatusException(unsupportedMediaTypeException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(415, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals(415, response.getStatus());
                        assertEquals("不支持的媒體類型", response.getMessage());
                    })
                    .verifyComplete();

            logUnityMock.verify(() -> LogUnity.debug(eq(testExchange), anyString(), anyString()));
        }
    }

    @Test
    @DisplayName("一般測試 - handleConversionFailException 處理轉換失敗異常")
    void testHandleConversionFailException_withPatternMatch() {
        TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
        TypeDescriptor targetType = TypeDescriptor.valueOf(Integer.class);
        ConversionFailedException conversionException = new ConversionFailedException(
            sourceType, targetType, "invalid-number", new NumberFormatException("Invalid number")
        );

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleConversionFailException(conversionException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(400, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals(400, response.getStatus());
                        assertTrue(response.getMessage().contains("轉換類型時發生問題"));
                    })
                    .verifyComplete();

            logUnityMock.verify(() -> LogUnity.debug(eq(testExchange), anyString(), anyString()));
        }
    }

    @Test
    @DisplayName("一般測試 - handleValidationExceptions 處理資料驗證異常")
    void testHandleValidationExceptions_webExchangeBindException() {
        // 模擬 WebExchangeBindException
        WebExchangeBindException bindException = mock(WebExchangeBindException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        FieldError fieldError = new FieldError("testObject", "testField", "必填欄位不能為空");

        when(bindException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(java.util.List.of(fieldError));

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleValidationExceptions(bindException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(400, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals(400, response.getStatus());
                        assertTrue(response.getMessage().contains("資料驗證失敗"));
                        assertTrue(response.getMessage().contains("必填欄位不能為空"));
                    })
                    .verifyComplete();

            logUnityMock.verify(() -> LogUnity.debug(eq(testExchange), anyString(), any()));
        }
    }

    @Test
    @DisplayName("一般測試 - handleInvalidJsonException 處理 JSON 格式錯誤")
    void testHandleInvalidJsonException_basicFunctionality() {
        ServerWebInputException jsonException = new ServerWebInputException("Invalid JSON format");

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleInvalidJsonException(jsonException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(400, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals(400, response.getStatus());
                        assertEquals("JSON 格式錯誤", response.getMessage());
                    })
                    .verifyComplete();

            logUnityMock.verify(() -> LogUnity.debug(eq(testExchange), anyString(), anyString()));
        }
    }

    @Test
    @DisplayName("一般測試 - handleUnsupportedOperationException 處理不支持的操作")
    void testHandleUnsupportedOperationException_basicFunctionality() {
        UnsupportedOperationException unsupportedException = new UnsupportedOperationException("Operation not supported");

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleUnsupportedOperationException(unsupportedException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(400, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals(400, response.getStatus());
                        assertEquals("不支持的操作", response.getMessage());
                    })
                    .verifyComplete();

            logUnityMock.verify(() -> LogUnity.debug(eq(testExchange), anyString(), anyString()));
        }
    }

    @Test
    @DisplayName("一般測試 - handleR2dbcException 處理 R2dbc 異常")
    void testHandleR2dbcException_basicFunctionality() {
        R2dbcException r2dbcException = mock(R2dbcException.class);
        when(r2dbcException.getMessage()).thenReturn("R2dbc connection failed");

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleR2dbcException(r2dbcException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(500, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals(500, response.getStatus());
                        assertTrue(response.getMessage().contains("超出操作限制"));
                        assertTrue(response.getMessage().contains("test-request-123"));
                    })
                    .verifyComplete();

            logUnityMock.verify(() -> LogUnity.error(eq(testExchange), anyString(), eq(r2dbcException)));
        }
    }

    @Test
    @DisplayName("一般測試 - handleException 處理重試次數過多異常")
    void testHandleException_retryExhaustedException() {
        RuntimeException retryException = new RuntimeException("Retry exhausted");

        try (MockedStatic<Exceptions> exceptionsMock = mockStatic(Exceptions.class);
             MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            
            exceptionsMock.when(() -> Exceptions.isRetryExhausted(retryException)).thenReturn(true);

            StepVerifier.create(exceptionController.handleException(retryException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(429, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals(400, response.getStatus()); // 內部使用 BAD_REQUEST
                        assertTrue(response.getMessage().contains("超出操作限制"));
                    })
                    .verifyComplete();

            logUnityMock.verify(() -> LogUnity.error(eq(testExchange), anyString(), eq(retryException)));
        }
    }

    @Test
    @DisplayName("一般測試 - handleException 處理未知異常")
    void testHandleException_unknownException() {
        RuntimeException unknownException = new RuntimeException("Unknown error");

        try (MockedStatic<Exceptions> exceptionsMock = mockStatic(Exceptions.class);
             MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            
            exceptionsMock.when(() -> Exceptions.isRetryExhausted(unknownException)).thenReturn(false);

            StepVerifier.create(exceptionController.handleException(unknownException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(500, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals(500, response.getStatus());
                        assertTrue(response.getMessage().contains("伺服器內部處理錯誤"));
                        assertTrue(response.getMessage().contains("test-request-123"));
                    })
                    .verifyComplete();

            logUnityMock.verify(() -> LogUnity.error(eq(testExchange), anyString(), eq(unknownException), eq("test-request-123")));
        }
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - handleValidationExceptions MissingRequestValueException")
    void testHandleValidationExceptions_missingRequestValueException() {
        MissingRequestValueException missingException = mock(MissingRequestValueException.class);
        when(missingException.getReason()).thenReturn("Missing required parameter testParam");

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleValidationExceptions(missingException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(400, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertTrue(response.getMessage().contains("資料驗證失敗"));
                    })
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("異常測試 - handleConversionFailException 無模式匹配")
    void testHandleConversionFailException_noPatternMatch() {
        ConversionFailedException conversionException = mock(ConversionFailedException.class);
        when(conversionException.getMessage()).thenReturn("Simple conversion error");

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleConversionFailException(conversionException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(400, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals("轉換類型時發生問題，請檢查請求參數", response.getMessage());
                    })
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("異常測試 - handleNotFound ResponseStatusException")
    void testHandleNotFound_responseStatusException() {
        ResponseStatusException statusException = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            StepVerifier.create(exceptionController.handleNotFound(statusException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(404, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertEquals("請求的資源不存在", response.getMessage());
                    })
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("異常測試 - 資料庫操作異常")
    void testHandleDatabaseException_nonTransientDataAccessResourceException() {
        NonTransientDataAccessResourceException dbException = 
            new NonTransientDataAccessResourceException("Database connection failed");

        try (MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            // 使用反射調用私有方法
            try {
                Method method = ExceptionController.class.getDeclaredMethod("handleDatabaseException", Throwable.class, ServerWebExchange.class);
                method.setAccessible(true);
                
                @SuppressWarnings("unchecked")
                Mono<ResponseEntity<?>> result = (Mono<ResponseEntity<?>>) method.invoke(exceptionController, dbException, testExchange);
                
                StepVerifier.create(result)
                        .assertNext(responseEntity -> {
                            assertNotNull(responseEntity);
                            assertEquals(500, responseEntity.getStatusCode().value());
                            
                            ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                            assertNotNull(response);
                            assertEquals(500, response.getStatus());
                            assertTrue(response.getMessage().contains("操作失敗"));
                        })
                        .verifyComplete();

                logUnityMock.verify(() -> LogUnity.error(eq(testExchange), anyString(), eq(dbException)));
            } catch (Exception e) {
                fail("Failed to invoke private method: " + e.getMessage());
            }
        }
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 註解驗證")
    void testAnnotations() {
        // 驗證類別註解
        assertTrue(exceptionController.getClass().isAnnotationPresent(RestControllerAdvice.class));
        assertTrue(exceptionController.getClass().isAnnotationPresent(RecordLevel.class));
        
        RecordLevel classLevel = exceptionController.getClass().getAnnotation(RecordLevel.class);
        assertEquals(LogLevelEnum.ERROR, classLevel.value());

        // 驗證方法註解
        Method[] methods = exceptionController.getClass().getDeclaredMethods();
        int exceptionHandlerCount = 0;
        int recordLevelCount = 0;

        for (Method method : methods) {
            if (method.isAnnotationPresent(ExceptionHandler.class)) {
                exceptionHandlerCount++;
            }
            if (method.isAnnotationPresent(RecordLevel.class)) {
                recordLevelCount++;
            }
        }

        assertTrue(exceptionHandlerCount >= 10, "應該有至少 10 個異常處理方法");
        assertTrue(recordLevelCount >= 10, "應該有至少 10 個方法有 RecordLevel 註解");
    }

    @Test
    @DisplayName("邊界測試 - 空請求 ID 處理")
    void testHandleException_nullRequestId() {
        RuntimeException unknownException = new RuntimeException("Test exception");
        testExchange.getAttributes().remove("requestId");

        try (MockedStatic<Exceptions> exceptionsMock = mockStatic(Exceptions.class);
             MockedStatic<LogUnity> logUnityMock = mockStatic(LogUnity.class)) {
            
            exceptionsMock.when(() -> Exceptions.isRetryExhausted(unknownException)).thenReturn(false);

            StepVerifier.create(exceptionController.handleException(unknownException, testExchange))
                    .assertNext(responseEntity -> {
                        assertNotNull(responseEntity);
                        assertEquals(500, responseEntity.getStatusCode().value());
                        
                        ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                        assertNotNull(response);
                        assertTrue(response.getMessage().contains("null")); // requestId 為 null
                    })
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("邊界測試 - 所有 ExceptionHandler 方法驗證")
    void testAllExceptionHandlerMethods() {
        Method[] methods = exceptionController.getClass().getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(ExceptionHandler.class)) {
                // 驗證方法參數
                Class<?>[] paramTypes = method.getParameterTypes();
                assertTrue(paramTypes.length >= 2, "ExceptionHandler 方法應該有至少 2 個參數");
                
                // 第一個參數應該是某種異常類型
                assertTrue(Throwable.class.isAssignableFrom(paramTypes[0]), 
                    "第一個參數應該是 Throwable 或其子類");
                
                // 第二個參數應該是 ServerWebExchange
                assertEquals(ServerWebExchange.class, paramTypes[1], 
                    "第二個參數應該是 ServerWebExchange");
                
                // 返回類型應該是 Mono<ResponseEntity<?>>
                assertEquals(Mono.class, method.getReturnType(), 
                    "返回類型應該是 Mono");
            }
        }
    }

    @Test
    @DisplayName("邊界測試 - ResponseUnity 接口實現驗證")
    void testResponseUnityImplementation() {
        assertTrue(exceptionController instanceof xyz.dowob.filemanagement.unity.ResponseUnity);
        
        // 驗證 ResponseUnity 提供的方法可用
        try {
            Method createResponseEntityMethod = exceptionController.getClass()
                .getSuperclass().getDeclaredMethod("createResponseEntity", Object.class, int.class);
            assertNotNull(createResponseEntityMethod);
        } catch (NoSuchMethodException e) {
            // 如果在父類中找不到，檢查接口默認方法
            Method[] interfaceMethods = xyz.dowob.filemanagement.unity.ResponseUnity.class.getDeclaredMethods();
            boolean foundCreateResponseEntity = false;
            for (Method method : interfaceMethods) {
                if ("createResponseEntity".equals(method.getName())) {
                    foundCreateResponseEntity = true;
                    break;
                }
            }
            assertTrue(foundCreateResponseEntity, "應該實現 createResponseEntity 方法");
        }
    }
}