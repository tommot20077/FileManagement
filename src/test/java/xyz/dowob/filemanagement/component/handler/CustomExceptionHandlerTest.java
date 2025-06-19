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

    @Test
    @DisplayName("獲取路由函數 - 成功返回路由函數")
    void getRoutingFunction_returnsRouterFunction() {
        RouterFunction<ServerResponse> routerFunction = customExceptionHandlerUnderTest.getRoutingFunction(mockErrorAttributes);
        assertThat(routerFunction).isNotNull();
    }

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
