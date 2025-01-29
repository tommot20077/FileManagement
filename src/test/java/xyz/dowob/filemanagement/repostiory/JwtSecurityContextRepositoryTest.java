package xyz.dowob.filemanagement.repostiory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.manager.JwtAuthenticationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtSecurityContextRepositoryTest {

    @Mock
    private JwtAuthenticationManager mockAuthenticationManager;

    private JwtSecurityContextRepository jwtSecurityContextRepositoryUnderTest;

    @BeforeEach
    void setUp() {
        jwtSecurityContextRepositoryUnderTest = new JwtSecurityContextRepository(mockAuthenticationManager);
    }

    @Test
    void testSave() {
        assertThat(jwtSecurityContextRepositoryUnderTest.save(null, null)).isNull();
    }

    @Test
    void testLoad() {
        // Setup
        final ServerWebExchange exchange = null;

        // Configure JwtAuthenticationManager.authenticate(...).
        final Mono<Authentication> authenticationMono = Mono.just(new TestingAuthenticationToken("user", "pass", "ROLE_USER"));
        when(mockAuthenticationManager.authenticate(new TestingAuthenticationToken("user", "pass", "ROLE_USER"))).thenReturn(
                authenticationMono);

        // Run the test
        final Mono<SecurityContext> result = jwtSecurityContextRepositoryUnderTest.load(exchange);

        // Verify the results
    }

    @Test
    void testLoad_JwtAuthenticationManagerReturnsNoItem() {
        // Setup
        final ServerWebExchange exchange = null;
        when(mockAuthenticationManager.authenticate(new TestingAuthenticationToken("user", "pass", "ROLE_USER"))).thenReturn(Mono.empty());

        // Run the test
        final Mono<SecurityContext> result = jwtSecurityContextRepositoryUnderTest.load(exchange);

        // Verify the results
    }

    @Test
    void testLoad_JwtAuthenticationManagerReturnsError() {
        // Setup
        final ServerWebExchange exchange = null;

        // Configure JwtAuthenticationManager.authenticate(...).
        final Mono<Authentication> authenticationMono = Mono.error(new Exception("message"));
        when(mockAuthenticationManager.authenticate(new TestingAuthenticationToken("user", "pass", "ROLE_USER"))).thenReturn(
                authenticationMono);

        // Run the test
        final Mono<SecurityContext> result = jwtSecurityContextRepositoryUnderTest.load(exchange);

        // Verify the results
    }
}
