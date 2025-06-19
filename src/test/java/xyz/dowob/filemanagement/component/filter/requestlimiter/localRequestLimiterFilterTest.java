package xyz.dowob.filemanagement.component.filter.requestlimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;

@ExtendWith(MockitoExtension.class)
class localRequestLimiterFilterTest {

    @Mock
    private GlobalProperties mockGlobalProperties;

    private localRequestLimiterFilter localRequestLimiterFilterUnderTest;


    @BeforeEach
    void setUp() {
        localRequestLimiterFilterUnderTest = new localRequestLimiterFilter(new ObjectMapper(), mockGlobalProperties);
    }

}
