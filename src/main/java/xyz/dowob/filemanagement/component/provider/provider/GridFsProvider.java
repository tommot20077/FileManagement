package xyz.dowob.filemanagement.component.provider.provider;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.config.properties.FileProperties;

/**
 * 此類用於提供 GridFs 的操作方法，透過自定義方法操作 GridFsTemplate 來對檔案進行操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName GridFsProvider
 * @description
 * @create 2024-09-27 00:55
 * @Version 1.0
 **/
@Component
@SuppressWarnings("unused")
public class GridFsProvider {
    /**
     * GridFsTemplate 用於操作 GridFs 的模板，此模板為非阻塞的
     */
    private final ReactiveGridFsTemplate gridFsTemplate;

    /**
     * GridFSUploadOptions 用於設置 GridFS 上傳的選項
     * chunkSizeBytes 用於設置每個分塊的大小，默認為 255KB
     * 此處設置為 1022KB，因為元數據需要空間儲存，故略小於1MB
     * 並從設定類中獲取設定大小
     *
     * @see GridFSUploadOptions
     */
    private final GridFSUploadOptions uploadOptions;

    /**
     * 通過構造方法注入 GridFsTemplate 和 FileProperties
     *
     * @param gridFsTemplate GridFsTemplate
     * @param fileProperties FileProperties
     */
    public GridFsProvider(ReactiveGridFsTemplate gridFsTemplate, FileProperties fileProperties) {
        this.gridFsTemplate = gridFsTemplate;
        this.uploadOptions = new GridFSUploadOptions().chunkSizeBytes(1022 * 1024 * fileProperties.getUpload().getPayloadLength());
    }

    /**
     * 將檔案存入 GridFs
     *
     * @param dataBufferFlux 檔案的數據流
     * @param fileName       檔案名
     *
     * @return 返回存入的檔案的 ObjectId
     */
    public Mono<ObjectId> storeFile(Flux<DataBuffer> dataBufferFlux, String fileName) {
        return gridFsTemplate.store(dataBufferFlux, fileName, uploadOptions);
    }

    /**
     * 通過檔案名查找檔案
     *
     * @param fileName 檔案名
     *
     * @return 返回查找到的檔案
     */
    public Mono<GridFSFile> findFileByFileName(String fileName) {
        return gridFsTemplate.findOne(Query.query(Criteria.where("filename").is(fileName)));
    }

    /**
     * 通過檔案 ID 查找檔案
     *
     * @param id 檔案 ID
     *
     * @return 返回查找到的檔案
     */
    public Mono<GridFSFile> findFileById(ObjectId id) {
        return gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(id)));
    }

    /**
     * 獲取檔案的數據流
     *
     * @param gridFsFile 檔案
     *
     * @return 返回查找到的檔案的數據流
     */
    public Mono<ReactiveGridFsResource> getResource(GridFSFile gridFsFile) {
        return gridFsTemplate.getResource(gridFsFile);
    }

    /**
     * 通過檔案名刪除檔案
     *
     * @param fileName 檔案名
     *
     * @return 返回 Mono<Void>
     */
    public Mono<Void> deleteFileByFilename(String fileName) {
        return gridFsTemplate.delete(Query.query(Criteria.where("filename").is(fileName)));
    }

    /**
     * 通過檔案 ID 刪除檔案
     *
     * @param id 檔案 ID
     *
     * @return 返回 Mono<Void>
     */
    public Mono<Void> deleteFileById(ObjectId id) {
        return gridFsTemplate.delete(Query.query(Criteria.where("_id").is(id)));
    }


}
