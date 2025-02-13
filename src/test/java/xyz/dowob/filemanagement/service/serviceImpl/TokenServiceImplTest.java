package xyz.dowob.filemanagement.service.serviceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.TokenStrategy;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private TokenStrategy mockTokenStrategy;

    private TokenServiceImpl tokenServiceImplUnderTest;

    @BeforeEach
    void setUp() {
        tokenServiceImplUnderTest = new TokenServiceImpl(mockTokenStrategy);
    }

    @Test
    void testGenerateToken() {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(null);

        // Run the test
        final Mono<String> result = tokenServiceImplUnderTest.generateToken(user, TokenEnum.JWT_AUTHORIZATION_TOKEN);

        // Verify the results
    }

    @Test
    void testValidateToken() {
        // Setup
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(null);

        // Run the test
        final Mono<Long> result = tokenServiceImplUnderTest.validateToken("token", 0L, TokenEnum.JWT_AUTHORIZATION_TOKEN);

        // Verify the results
    }

    @Test
    void testRevokeToken() {
        // Setup
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(null);

        // Run the test
        final Mono<Void> result = tokenServiceImplUnderTest.revokeToken(0L, TokenEnum.JWT_AUTHORIZATION_TOKEN);

        // Verify the results
    }

    @Test
    void testCreate() {
        assertThat(tokenServiceImplUnderTest.create()).isNull();
    }

    @Test
    void testGetById() {
        assertThat(tokenServiceImplUnderTest.getById(0L)).isNull();
    }

    @Test
    void testGetAll() {
        assertThat(tokenServiceImplUnderTest.getAll()).isNull();
    }

    @Test
    void testUpdate() {
        assertThat(tokenServiceImplUnderTest.update(new Token())).isNull();
    }

    @Test
    void testDelete() {
        assertThat(tokenServiceImplUnderTest.delete(new Token())).isNull();
    }
}
