package xyz.dowob.filemanagement.component.handler;

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
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.LocalDateTime;

/**
 * 自定義異常處理器，用於處理全局異常，當內部發生未知異常時，返回統一的格式
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CustomExceptionHandler
 * @description
 * @create 2024-09-25 01:57
 * @Version 1.0
 **/
@Component
@Order(-3)
public class CustomExceptionHandler extends AbstractErrorWebExceptionHandler {

    /**
     * 自定義異常處理器構造方法，繼承 AbstractErrorWebExceptionHandler 類
     *
     * @param errorAttributes       錯誤屬性
     * @param webProperties         Web屬性
     * @param applicationContext    應用上下文
     * @param serverCodecConfigurer 服務器編解碼器
     */
    public CustomExceptionHandler(ErrorAttributes errorAttributes, WebProperties webProperties, ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());

    }

    /**
     * 獲取路由函數
     *
     * @param errorAttributes 錯誤屬性
     *
     * @return RouterFunction<ServerResponse> 路由函數
     */
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::handleException);
    }

    /**
     * 異常處理方法
     * 此方法用於處理異常，當內部發生未知異常時，返回統一的格式
     *
     * @param request 請求
     *
     * @return Mono<ServerResponse> 服務器響應
     */
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
