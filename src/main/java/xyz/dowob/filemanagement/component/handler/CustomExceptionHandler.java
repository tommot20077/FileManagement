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

    public CustomExceptionHandler(
            ErrorAttributes errorAttributes, WebProperties webProperties, ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());

    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::handleException);


    }

    private Mono<ServerResponse> handleException(ServerRequest request) {
        Throwable error = getError(request);
        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                                   HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                                   request.exchange().getRequest().getPath().value(),
                                                                   "伺服器內部處理錯誤",
                                                                   null);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;


        if (error instanceof ValidationException validationException) {
            apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                  validationException.getErrorCode().getCode(),
                                                  request.exchange().getRequest().getPath().value(),
                                                  String.format("驗證時發生錯誤：%s", validationException.getMessage()),
                                                  null);
            status = HttpStatus.BAD_REQUEST;
        }
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).body(Mono.just(apiResponseDTO), ApiResponseDTO.class);
    }
}
