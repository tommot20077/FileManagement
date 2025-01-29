package xyz.dowob.filemanagement.holder;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.Function;

class CustomRequestContextHolderTest {

    @Test
    void testGetExchange() {
        // Setup
        // Run the test
        final Mono<ServerWebExchange> result = CustomRequestContextHolder.getExchange();

        // Verify the results
    }

    @Test
    void testMutate() {
        // Setup
        final ServerWebExchange exchange = null;

        // Run the test
        final Function<Context, Context> result = CustomRequestContextHolder.mutate(exchange);

        // Verify the results
    }
}
