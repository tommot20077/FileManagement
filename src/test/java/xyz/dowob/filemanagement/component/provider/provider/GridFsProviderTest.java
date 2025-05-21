package xyz.dowob.filemanagement.component.provider.provider;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.config.properties.FileProperties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GridFsProvider 邏輯處理測試")
class GridFsProviderTest {

    @Mock
    private ReactiveGridFsTemplate mockGridFsTemplate;
    @Mock
    private FileProperties mockFileProperties;
    @Mock
    private FileProperties.Upload mockUpload;
    @Mock
    private DataBuffer mockDataBuffer;
    @Mock
    private GridFSFile mockGridFSFile;
    @Mock
    private ReactiveGridFsResource mockResource;

    private GridFsProvider gridFsProviderUnderTest;

    @BeforeEach
    void setUp() {
        when(mockFileProperties.getUpload()).thenReturn(mockUpload);
        when(mockUpload.getPayloadLength()).thenReturn(org.springframework.util.unit.DataSize.ofKilobytes(1024));
        gridFsProviderUnderTest = new GridFsProvider(mockGridFsTemplate, mockFileProperties);
    }

    @Test
    @DisplayName("存儲空檔案到 GridFS - 成功返回檔案ID")
    void storeFile_emptyFile_returnsObjectId() {
        ObjectId expectedId = new ObjectId();
        Flux<DataBuffer> emptyFlux = Flux.empty();
        String filename = "empty.txt";

        when(mockGridFsTemplate.store(any(), any(), any(GridFSUploadOptions.class))).thenReturn(Mono.just(expectedId));

        StepVerifier.create(gridFsProviderUnderTest.storeFile(emptyFlux, filename)).expectNext(expectedId).verifyComplete();
    }

    @Test
    @DisplayName("存儲檔案到 GridFS 發生錯誤 - 返回錯誤")
    void storeFile_whenError_propagatesError() {
        Flux<DataBuffer> dataBufferFlux = Flux.just(mockDataBuffer);
        String filename = "error.txt";

        when(mockGridFsTemplate.store(any(), any(), any(GridFSUploadOptions.class))).thenReturn(Mono.error(new RuntimeException("Storage error")));


        StepVerifier
                .create(gridFsProviderUnderTest.storeFile(dataBufferFlux, filename))
                .expectErrorMatches(error -> error.getMessage().equals("Storage error"))
                .verify();
    }

    @Test
    @DisplayName("存儲檔案到 GridFS - 成功返回檔案ID")
    void storeFile_validInput_returnsObjectId() {
        ObjectId expectedId = new ObjectId();
        Flux<DataBuffer> dataBufferFlux = Flux.just(mockDataBuffer);
        String filename = "test.txt";

        when(mockGridFsTemplate.store(any(), any(), any(GridFSUploadOptions.class))).thenReturn(Mono.just(expectedId));

        StepVerifier.create(gridFsProviderUnderTest.storeFile(dataBufferFlux, filename)).expectNext(expectedId).verifyComplete();
    }

    @Test
    @DisplayName("透過檔案名稱查找檔案 - 成功返回檔案")
    void findFileByFileName_existingFile_returnsGridFSFile() {
        String filename = "test.txt";
        when(mockGridFsTemplate.findOne(any(Query.class))).thenReturn(Mono.just(mockGridFSFile));

        StepVerifier.create(gridFsProviderUnderTest.findFileByFileName(filename)).expectNext(mockGridFSFile).verifyComplete();
    }

    @Test
    @DisplayName("透過檔案名稱查找不存在的檔案 - 返回空")
    void findFileByFileName_nonExistingFile_returnsEmpty() {
        String filename = "nonexistent.txt";
        when(mockGridFsTemplate.findOne(any(Query.class))).thenReturn(Mono.empty());

        StepVerifier.create(gridFsProviderUnderTest.findFileByFileName(filename)).verifyComplete();
    }

    @Test
    @DisplayName("透過檔案ID查找檔案 - 成功返回檔案")
    void findFileById_existingFile_returnsGridFSFile() {
        ObjectId id = new ObjectId();
        when(mockGridFsTemplate.findOne(any(Query.class))).thenReturn(Mono.just(mockGridFSFile));

        StepVerifier.create(gridFsProviderUnderTest.findFileById(id)).expectNext(mockGridFSFile).verifyComplete();
    }

    @Test
    @DisplayName("透過多個檔案名稱查找檔案 - 成功返回檔案映射")
    void findFilesByFileName_existingFiles_returnsMap() {
        List<String> filenames = Arrays.asList("test1.txt", "test2.txt");
        when(mockGridFSFile.getFilename()).thenReturn("test1.txt");
        when(mockGridFsTemplate.find(any(Query.class))).thenReturn(Flux.just(mockGridFSFile));

        StepVerifier
                .create(gridFsProviderUnderTest.findFilesByFileName(filenames))
                .expectNextMatches(map -> map.containsKey("test1.txt") && map.get("test1.txt").equals(mockGridFSFile))
                .verifyComplete();
    }

    @Test
    @DisplayName("透過多個檔案ID查找檔案 - 成功返回檔案映射")
    void findFilesById_existingFiles_returnsMap() {
        ObjectId id = new ObjectId();
        List<ObjectId> ids = Collections.singletonList(id);
        when(mockGridFSFile.getObjectId()).thenReturn(id);
        when(mockGridFsTemplate.find(any(Query.class))).thenReturn(Flux.just(mockGridFSFile));

        StepVerifier
                .create(gridFsProviderUnderTest.findFilesById(ids))
                .expectNextMatches(map -> map.containsKey(id) && map.get(id).equals(mockGridFSFile))
                .verifyComplete();
    }

    @Test
    @DisplayName("獲取檔案資源 - 成功返回資源")
    void getResource_validFile_returnsResource() {
        when(mockGridFsTemplate.getResource(mockGridFSFile)).thenReturn(Mono.just(mockResource));

        StepVerifier.create(gridFsProviderUnderTest.getResource(mockGridFSFile)).expectNext(mockResource).verifyComplete();
    }

    @Test
    @DisplayName("透過檔案名稱刪除檔案 - 成功刪除")
    void deleteFileByFilename_existingFile_completes() {
        String filename = "test.txt";
        when(mockGridFsTemplate.delete(any(Query.class))).thenReturn(Mono.empty());

        StepVerifier.create(gridFsProviderUnderTest.deleteFileByFilename(filename)).verifyComplete();
    }

    @Test
    @DisplayName("透過檔案ID刪除檔案 - 成功刪除")
    void deleteFileById_existingFile_completes() {
        ObjectId id = new ObjectId();
        when(mockGridFsTemplate.delete(any(Query.class))).thenReturn(Mono.empty());

        StepVerifier.create(gridFsProviderUnderTest.deleteFileById(id)).verifyComplete();
    }

    @Test
    @DisplayName("獲取 GridFsTemplate - 成功返回模板")
    void getGridFsTemplate_returnsTemplate() {
        StepVerifier.create(Mono.just(gridFsProviderUnderTest.getGridFsTemplate())).expectNext(mockGridFsTemplate).verifyComplete();
    }
}
