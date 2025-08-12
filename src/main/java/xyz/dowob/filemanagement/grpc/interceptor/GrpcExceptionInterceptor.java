package xyz.dowob.filemanagement.grpc.interceptor;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.exception.JwtAuthenticationException;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.LogUnity;

/**
 * gRPC 全局異常攔截器。
 *
 * <p>提供統一的異常處理機制，將應用層異常轉換為適當的 gRPC Status 碼。
 * 此攔截器會攔截所有 gRPC 服務方法的異常，並進行標準化的錯誤處理和日誌記錄。
 *
 * <p>異常映射規則：
 * <ul>
 *   <li>{@link ValidationException} → {@link Status#INVALID_ARGUMENT}</li>
 *   <li>{@link LimitationException} → {@link Status#RESOURCE_EXHAUSTED}</li>
 *   <li>{@link JwtAuthenticationException} → {@link Status#UNAUTHENTICATED}</li>
 *   <li>{@link ProcessException} → {@link Status#INTERNAL}</li>
 *   <li>其他異常 → {@link Status#INTERNAL}</li>
 * </ul>
 *
 * <p>功能特點：
 * <ul>
 *   <li>統一的異常捕獲和處理</li>
 *   <li>詳細的錯誤日誌記錄</li>
 *   <li>安全的錯誤信息返回（避免洩露敏感信息）</li>
 *   <li>與 HTTP 錯誤處理保持一致性</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class GrpcExceptionInterceptor implements ServerInterceptor {

    /**
     * 攔截 gRPC 服務調用，提供統一的異常處理。
     *
     * @param call    服務調用對象
     * @param headers 請求元數據
     * @param next    下一個處理器
     * @param <ReqT>  請求類型
     * @param <RespT> 響應類型
     * @return 包裝後的服務調用監聽器
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                if (status.getCode() == Status.Code.UNKNOWN && status.getCause() != null) {
                    status = handleException(status.getCause());
                }
                super.close(status, trailers);
            }
        };
        
        try {
            return new ExceptionHandlingListener<>(next.startCall(wrappedCall, headers), wrappedCall);
        } catch (Exception e) {
            Status status = handleException(e);
            call.close(status, new Metadata());
            return new ServerCall.Listener<>() {};
        }
    }

    /**
     * 處理異常並轉換為適當的 gRPC Status。
     *
     * @param throwable 要處理的異常
     * @return 對應的 gRPC Status
     */
    private Status handleException(Throwable throwable) {
        // 如果已經是 StatusRuntimeException，直接返回其 Status
        if (throwable instanceof StatusRuntimeException) {
            return ((StatusRuntimeException) throwable).getStatus();
        }
        if (throwable instanceof StatusException) {
            return ((StatusException) throwable).getStatus();
        }
        
        // 處理應用層異常
        if (throwable instanceof ValidationException) {
            ValidationException ve = (ValidationException) throwable;
            String errorMessage = String.format("驗證失敗: %s", ve.getMessage());
            LogUnity.warn("gRPC 驗證錯誤: %s", ve.getMessage());
            return Status.INVALID_ARGUMENT.withDescription(errorMessage).withCause(ve);
        }
        
        if (throwable instanceof LimitationException) {
            LimitationException le = (LimitationException) throwable;
            String errorMessage = String.format("資源限制: %s", le.getMessage());
            LogUnity.warn("gRPC 限制錯誤: %s", le.getMessage());
            return Status.RESOURCE_EXHAUSTED.withDescription(errorMessage).withCause(le);
        }
        
        if (throwable instanceof JwtAuthenticationException) {
            JwtAuthenticationException ae = (JwtAuthenticationException) throwable;
            LogUnity.warn("gRPC 認證錯誤: %s", ae.getMessage());
            return Status.UNAUTHENTICATED.withDescription("認證失敗").withCause(ae);
        }
        
        if (throwable instanceof ProcessException) {
            ProcessException pe = (ProcessException) throwable;
            String errorMessage = String.format("處理錯誤: %s", pe.getMessage());
            LogUnity.error("gRPC 處理錯誤: %s", pe, pe.getMessage());
            return Status.INTERNAL.withDescription(errorMessage).withCause(pe);
        }
        
        // 處理未知異常
        LogUnity.error("gRPC 內部錯誤: %s", throwable, throwable.getMessage());
        return Status.INTERNAL.withDescription("內部服務錯誤").withCause(throwable);
    }

    /**
     * 異常處理監聽器，包裝原始監聽器以提供異常捕獲功能。
     *
     * @param <ReqT> 請求類型
     */
    private class ExceptionHandlingListener<ReqT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        private final ServerCall<ReqT, ?> serverCall;

        ExceptionHandlingListener(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, ?> serverCall) {
            super(listener);
            this.serverCall = serverCall;
        }

        @Override
        public void onMessage(ReqT message) {
            try {
                super.onMessage(message);
            } catch (Exception e) {
                handleListenerException(e);
            }
        }


        private void handleListenerException(Exception e) {
            Status status = handleException(e);
            serverCall.close(status, new Metadata());
        }


        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (Exception e) {
                handleListenerException(e);
            }
        }


        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (Exception e) {
                handleListenerException(e);
            }
        }
    }
}