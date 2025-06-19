package xyz.dowob.filemanagement.component.filter.requestlimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;

@ExtendWith(MockitoExtension.class)
class RedisRequestLimiterFilterTest {

    @Mock
    private GlobalProperties mockGlobalProperties;

    @Mock
    private RedisClient mockRedisClient;

    @Mock
    private RedisProvider mockRedisProvider;

    private RedisRequestLimiterFilter redisRequestLimiterFilterUnderTest;


    @BeforeEach
    void setUp() {
        redisRequestLimiterFilterUnderTest = new RedisRequestLimiterFilter(mockGlobalProperties,
                                                                           new ObjectMapper(),
                                                                           mockRedisClient,
                                                                           mockRedisProvider
        );
    }
}
