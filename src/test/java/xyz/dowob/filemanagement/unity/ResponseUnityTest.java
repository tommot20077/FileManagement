package xyz.dowob.filemanagement.unity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;

import static org.junit.jupiter.api.Assertions.*;

class ResponseUnityTest {
    private ResponseUnity responseUnity;
    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        responseUnity = new ResponseUnity() {
        };
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        exchange = MockServerWebExchange.from(request);
    }

    @Nested
    @DisplayName("創建 ResponseEntity 的測試")
    class CreateResponseEntityTests {

        @Test
        @DisplayName("成功的 ApiResponse 應返回 200 狀態碼")
        void createResponseEntity_WithSuccessResponse_ShouldReturn200() {
            // given
            ApiResponseDTO<String> apiResponse = new ApiResponseDTO<>(null, 200, "/test", "success", "test data");

            // when
            Mono<ResponseEntity<?>> result = responseUnity.createResponseEntity(apiResponse);

            // then
            StepVerifier.create(result).assertNext(response -> {
                assertTrue(response.getStatusCode().is2xxSuccessful());
                assertInstanceOf(ApiResponseDTO.class, response.getBody());
                assertEquals("test data", ((ApiResponseDTO<?>) response.getBody()).getData());
            }).verifyComplete();
        }

        @Test
        @DisplayName("失敗的 ApiResponse 應返回 400 狀態碼")
        void createResponseEntity_WithFailureResponse_ShouldReturn400() {
            // given
            ApiResponseDTO<String> apiResponse = new ApiResponseDTO<>(null, 400, "/test", "failure", null);

            // when
            Mono<ResponseEntity<?>> result = responseUnity.createResponseEntity(apiResponse);

            // then
            StepVerifier.create(result).assertNext(response -> {
                assertEquals(400, response.getStatusCode().value());
                assertInstanceOf(ApiResponseDTO.class, response.getBody());
            }).verifyComplete();
        }

        @Test
        @DisplayName("具備自定義標頭的 ApiResponse 應正確設置標頭")
        void createResponseEntity_WithCustomHeaders_ShouldReturnHeadersCorrectly() {
            // given
            ApiResponseDTO<String> apiResponse = new ApiResponseDTO<>(null, 200, "/test", "success", "test data");
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json");

            // when
            Mono<ResponseEntity<?>> result = responseUnity.createResponseEntity(apiResponse, headers);

            // then
            StepVerifier.create(result).assertNext(response -> {
                assertEquals(200, response.getStatusCode().value());
                assertTrue(response.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
                assertEquals("application/json", response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
            }).verifyComplete();
        }
    }

    @Nested
    @DisplayName("創建 ApiResponse 的測試")
    class CreateResponseTests {

        @Test
        @DisplayName("使用 Exchange 創建的 ApiResponse 應包含正確資訊")
        void createResponse_WithExchange_ShouldCreateCorrectResponse() {
            // when
            ApiResponseDTO<String> response = responseUnity.createResponse(exchange, 200, "success", "test data");

            // then
            assertEquals(200, response.getStatus());
            assertEquals("/test", response.getPath());
            assertEquals("success", response.getMessage());
            assertEquals("test data", response.getData());
            assertNotNull(response.getTimestamp());
        }

        @Test
        @DisplayName("使用路徑創建的 ApiResponse 應包含正確資訊")
        void createResponse_WithPath_ShouldCreateCorrectResponse() {
            // when
            ApiResponseDTO<String> response = responseUnity.createResponse("/custom-path", "success", "test data");

            // then
            assertEquals(200, response.getStatus());
            assertEquals("/custom-path", response.getPath());
            assertEquals("success", response.getMessage());
            assertEquals("test data", response.getData());
        }
    }

    @Nested
    @DisplayName("處理錯誤的測試")
    class HandleErrorTests {

        @Test
        @DisplayName("ValidationException 應返回 400 狀態碼")
        void handleError_WithValidationException_ShouldReturn400() {
            // given
            Mono<ResponseEntity<?>> operation = Mono.error(new ValidationException(ValidationException.ErrorCode.NULL_DTO));

            // when
            Mono<ResponseEntity<?>> result = responseUnity.handleError(operation, exchange);

            // then
            StepVerifier.create(result).assertNext(response -> {
                assertTrue(response.getStatusCode().is4xxClientError());
                ApiResponseDTO<?> apiResponse = (ApiResponseDTO<?>) response.getBody();
                assertNotNull(apiResponse);
                assertEquals(ValidationException.ErrorCode.NULL_DTO.getCode(), apiResponse.getStatus());
                assertTrue(apiResponse.getMessage().contains("傳輸數據不能為空"));
            }).verifyComplete();
        }

        @Test
        @DisplayName("LimitationException 應返回 429 狀態碼")
        void handleError_WithLimitationException_ShouldReturn429() {
            // given
            Mono<ResponseEntity<?>> operation = Mono.error(new LimitationException(LimitationException.ErrorCode.USER_EXCEED_LIMIT,
                                                                                   "用戶到達限制"
            ));

            // when
            Mono<ResponseEntity<?>> result = responseUnity.handleError(operation, exchange);

            // then
            StepVerifier.create(result).assertNext(response -> {
                assertTrue(response.getStatusCode().is4xxClientError());
                ApiResponseDTO<?> apiResponse = (ApiResponseDTO<?>) response.getBody();
                assertNotNull(apiResponse);
                assertEquals(LimitationException.ErrorCode.USER_EXCEED_LIMIT.getCode(), apiResponse.getStatus());
                assertTrue(apiResponse.getMessage().contains("用戶到達限制"));
            }).verifyComplete();
        }

        @Test
        @DisplayName("未知的異常不應該處理")
        void handleError_WithUnknownException_ShouldReturn500() {
            // given
            Mono<ResponseEntity<?>> operation = Mono.error(new RuntimeException("未知錯誤"));

            // when
            Mono<ResponseEntity<?>> result = responseUnity.handleError(operation, exchange);

            // then
            StepVerifier.create(result).expectError(RuntimeException.class).verify();
        }
    }
}