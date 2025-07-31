package xyz.dowob.filemanagement.component.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


/**
 * 自定義異常處理器的單元測試類別。
 *
 * <p>本測試類別全面驗證 CustomExceptionHandler 的異常處理和錯誤回應機制。</p>
 *
 * <p>測試範圍：
 * 
 *   - 異常路由函數的建立和配置
 *   - 不同種類異常的處理機制
 *   - HTTP 狀態碼和錯誤回應的正確性
 *   - JSON 格式的錯誤回應序列化
 * 
 * </p>
 *
 * <p>主要測試方法：
 * 
 *   - 驗證路由函數的正確生成
 *   - 測試各種異常的處理和回應
 *   - 確認錯誤訊息的格式和內容
 *   - 檢驗 WebFlux 異常處理流程
 * 
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomExceptionHandler 邏輯處理測試")
class CustomExceptionHandlerTest {

    @Mock
    private ErrorAttributes mockErrorAttributes;

    @Mock
    private WebProperties mockWebProperties;

    @Mock
    private ApplicationContext mockApplicationContext;

    @Mock
    private ServerCodecConfigurer mockServerCodecConfigurer;

    private CustomExceptionHandler customExceptionHandlerUnderTest;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        when(mockWebProperties.getResources()).thenReturn(new WebProperties.Resources());
        when(mockApplicationContext.getClassLoader()).thenReturn(this.getClass().getClassLoader());
        customExceptionHandlerUnderTest = new CustomExceptionHandler(mockErrorAttributes,
                                                                     mockWebProperties,
                                                                     mockApplicationContext,
                                                                     mockServerCodecConfigurer
        );
        webTestClient = WebTestClient.bindToRouterFunction(customExceptionHandlerUnderTest.getRoutingFunction(mockErrorAttributes)).build();
    }

    /**
     * 測試獲取路由函數的功能。
     *
     * 測試涵蓋的邏輯或場景說明：
     * 驗證 CustomExceptionHandler 能正確建立和返回異常處理的路由函數。
     *
     * 前置條件：
     * - 初始化 CustomExceptionHandler 實例
     * - 配置 Mock ErrorAttributes
     *
     * 測試步驟：
     * - 調用 getRoutingFunction 方法
     * - 驗證返回的路由函數不為 null
     *
     * 預期結果：
     * - 成功返回非 null 的 RouterFunction 實例
     */
    @Test
    @DisplayName("獲取路由函數 - 成功返回路由函數")
    void getRoutingFunction_returnsRouterFunction() {
        RouterFunction<ServerResponse> routerFunction = customExceptionHandlerUnderTest.getRoutingFunction(mockErrorAttributes);
        assertThat(routerFunction).isNotNull();
    }

    /**
     * 測試處理內部伺服器錯誤的功能。
     *
     * 測試涵蓋的邏輯或場景說明：
     * 驗證當系統發生 RuntimeException 時，異常處理器能正確返回 HTTP 500 回應。
     *
     * 前置條件：
     * - 設置 Mock ErrorAttributes 返回 RuntimeException
     * - 初始化 WebTestClient 用於測試
     *
     * 測試步驟：
     * - 發送 GET 請求至錯誤路徑
     * - 驗證 HTTP 狀態碼為 500
     * - 檢查回應內容和格式
     *
     * 預期結果：
     * - 返回 HTTP 500 Internal Server Error
     * - 回應為 JSON 格式的 ApiResponseDTO
     * - 包含適當的錯誤訊息和路徑資訊
     */
    @Test
    @DisplayName("處理內部伺服器錯誤 - 成功返回內部伺服器錯誤響應")
    void handleException_internalServerError_returnsInternalServerErrorResponse() {
        when(mockErrorAttributes.getError(any(ServerRequest.class))).thenReturn(new RuntimeException("Test internal error"));

        webTestClient.get().uri("/test-error")
                   .exchange()
                   .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                   .expectHeader().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                   .expectBody(ApiResponseDTO.class)
                   .consumeWith(response -> {
                       ApiResponseDTO<Void> apiResponseDTO = response.getResponseBody();
                       assertThat(apiResponseDTO).isNotNull();
                       assertThat(apiResponseDTO.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
                       assertThat(apiResponseDTO.getMessage()).isEqualTo("伺服器內部處理錯誤");
                       assertThat(apiResponseDTO.getPath()).isEqualTo("/test-error");
                   });
    }

    @Test
    @DisplayName("處理驗證錯誤 - 成功返回驗證錯誤響應")
    void handleException_validationException_returnsBadRequestResponse() {
        ValidationException validationException = new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "Test validation error");
        when(mockErrorAttributes.getError(any(ServerRequest.class))).thenReturn(validationException);

        webTestClient.get().uri("/test-validation-error")
                   .exchange()
                   .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                   .expectHeader().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                   .expectBody(ApiResponseDTO.class)
                   .consumeWith(response -> {
                       ApiResponseDTO<Void> apiResponseDTO = response.getResponseBody();
                       assertThat(apiResponseDTO).isNotNull();
                       assertThat(apiResponseDTO.getStatus()).isEqualTo(ValidationException.ErrorCode.REQUEST_IS_INVALID.getCode());
                       assertThat(apiResponseDTO.getMessage()).isEqualTo("驗證時發生錯誤：請求參數無效: Test validation error");
                       assertThat(apiResponseDTO.getPath()).isEqualTo("/test-validation-error");
                   });
    }
}
