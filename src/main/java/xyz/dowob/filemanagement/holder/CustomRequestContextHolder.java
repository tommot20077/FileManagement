package xyz.dowob.filemanagement.holder;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.Function;

/**
 * 用於存放 ServerWebExchange 的 Holder 類，用於存放 ServerWebExchange 的上下文
 * 通過此類可以在任何地方獲取 ServerWebExchange
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ReactiveRequestContextHolder
 * @create 2025/1/19
 * @Version 1.0
 **/

public class CustomRequestContextHolder {
    /**
     * 上下文 key
     */
    private static final String CONTEXT_KEY = "SERVER_WEB_EXCHANGE";

    /**
     * 獲取 ServerWebExchange
     *
     * @return Mono<ServerWebExchange>
     */
    public static Mono<ServerWebExchange> getExchange() {
        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(CONTEXT_KEY)).cast(ServerWebExchange.class));
    }


    /**
     * 將 ServerWebExchange 存入上下文
     *
     * @param exchange ServerWebExchange
     *
     * @return Function<Context, Context>
     */
    public static Function<Context, Context> mutate(ServerWebExchange exchange) {
        return context -> context.put(CONTEXT_KEY, exchange);
    }
}
