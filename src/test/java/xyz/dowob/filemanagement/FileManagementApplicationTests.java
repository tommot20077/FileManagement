package xyz.dowob.filemanagement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
@DisplayName("FileManagementApplication 邏輯處理測試")
class FileManagementApplicationTests {

    @Test
    @DisplayName("上下文加載測試 - 成功加載")
    void contextLoads() {
    }

}
