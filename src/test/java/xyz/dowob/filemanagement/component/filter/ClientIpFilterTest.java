package xyz.dowob.filemanagement.component.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;

@ExtendWith(MockitoExtension.class)
class ClientIpFilterTest {

    @Mock
    private GlobalProperties mockGlobalProperties;

    private ClientIpFilter clientIpFilterUnderTest;


    @BeforeEach
    void setUp() {
        clientIpFilterUnderTest = new ClientIpFilter(mockGlobalProperties);
    }
}
