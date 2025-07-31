package xyz.dowob.filemanagement.component.handler;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.LocalDateTime;

/**
 * 基於 Spring WebFlux 的反應式全域異常處理器，提供統一的錯誤處理機制。
 *
 * <p>本處理器繼承自 AbstractErrorWebExceptionHandler，實現對 WebFlux 應用程式中未捕獲異常的集中處理。
 * 自動攔截路由處理過程中的異常，並轉換為標準化的 API 響應格式。支援不同異常類型的差異化處理，
 * 確保錯誤回應的一致性和安全性。</p>
 *
 * <p>具備優先級設定（@Order(-3)），確保在其他異常處理器之前執行。
 * 針對 ValidationException 提供特化處理，其他未知異常統一回傳內部伺服器錯誤響應。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@Order(-3)
public class CustomExceptionHandler extends AbstractErrorWebExceptionHandler {

    /**
     * 建構方法，初始化全域異常處理器的相關組件。
     *
     * @param errorAttributes       錯誤屬性提取器，用於獲取異常資訊
     * @param webProperties         Web 相關設定屬性
     * @param applicationContext    Spring 應用上下文
     * @param serverCodecConfigurer 伺服器編解碼器設定，用於設定訊息讀寫器
     */
    public CustomExceptionHandler(ErrorAttributes errorAttributes, WebProperties webProperties, ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }


    /**
     * 定義異常處理的路由函數。
     *
     * @param errorAttributes 錯誤屬性提取器
     * @return 路由函數，將所有請求導向異常處理方法
     */
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::handleException);
    }


    /**
     * 異常處理核心方法，將異常轉換為統一的 API 響應格式。
     *
     * <p>根據異常類型進行特化處理：
     * - ValidationException：回傳 400 狀態碼和詳細錯誤訊息
     * - 其他異常：回傳 500 狀態碼和通用錯誤訊息</p>
     *
     * @param request 伺服器請求物件
     * @return 包含錯誤資訊的伺服器響應 Mono
     */
    @NotNull
    @RecordLevel(LogLevelEnum.ERROR)
    private Mono<ServerResponse> handleException(ServerRequest request) {
        Throwable error = getError(request);
        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                                   HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                                   request.exchange().getRequest().getPath().value(),
                                                                   "伺服器內部處理錯誤",
                                                                   null
        );
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (error instanceof ValidationException validationException) {
            apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                  validationException.getErrorCode().getCode(),
                                                  request.exchange().getRequest().getPath().value(),
                                                  String.format("驗證時發生錯誤：%s", validationException.getMessage()),
                                                  null
            );
            status = HttpStatus.BAD_REQUEST;
        }
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).body(Mono.just(apiResponseDTO), ApiResponseDTO.class);
    }
}
