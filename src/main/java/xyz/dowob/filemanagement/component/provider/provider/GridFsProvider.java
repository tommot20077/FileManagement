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
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;

import java.util.Collection;
import java.util.Map;

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
     * 此處設置為 1020KB，因為元數據需要空間儲存，故略小於1MB
     * 並從設定類中獲取設定大小
     *
     * @see GridFSUploadOptions GridFS上傳選項
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
        this.uploadOptions = new GridFSUploadOptions().chunkSizeBytes((int) (fileProperties.getUpload().getPayloadLength().toKilobytes() * 1020));
    }


    /**
     * 將檔案存入 GridFs
     *
     * @param dataBufferFlux 檔案的數據流
     * @param filename       檔案名
     *
     * @return 返回存入的檔案的 ObjectId
     */
    public Mono<ObjectId> storeFile(Flux<DataBuffer> dataBufferFlux, String filename) {
        return gridFsTemplate.store(dataBufferFlux, filename, uploadOptions);
    }


    /**
     * 通過檔案名查找檔案
     *
     * @param filename 檔案名
     *
     * @return 返回查找到的檔案
     */
    public Mono<GridFSFile> findFileByFileName(String filename) {
        return gridFsTemplate.findOne(Query.query(Criteria.where("filename").is(filename)));
    }


    /**
     * 通過檔案名集合查找檔案
     *
     * @param filenames 檔案名集合
     *
     * @return 返回查找到的檔案
     */
    public Mono<Map<String, GridFSFile>> findFilesByFileName(Collection<String> filenames) {
        return gridFsTemplate
                .find(Query.query(Criteria.where("filename").in(filenames)))
                .collectMap(GridFSFile::getFilename, gridFSFile -> gridFSFile);

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
     * 通過檔案 ID 集合查找檔案
     *
     * @param ids 檔案 ID 集合
     *
     * @return 返回查找到的檔案
     */
    public Mono<Map<ObjectId, GridFSFile>> findFilesById(Collection<ObjectId> ids) {
        return gridFsTemplate.find(Query.query(Criteria.where("_id").in(ids))).collectMap(GridFSFile::getObjectId, gridFSFile -> gridFSFile);
    }


    /**
     * 獲取檔案的數據流
     *
     * @param gridFsFile 檔案
     *
     * @return 返回查找到的檔案的數據流
     */
    @SkipRecord
    public Mono<ReactiveGridFsResource> getResource(GridFSFile gridFsFile) {
        return gridFsTemplate.getResource(gridFsFile);
    }


    /**
     * 通過檔案名刪除檔案
     *
     * @param filename 檔案名
     *
     * @return 返回 Mono<Void>
     */
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<Void> deleteFileByFilename(String filename) {
        return gridFsTemplate.delete(Query.query(Criteria.where("filename").is(filename)));
    }


    /**
     * 通過檔案 ID 刪除檔案
     *
     * @param id 檔案 ID
     *
     * @return 返回 Mono<Void>
     */
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<Void> deleteFileById(ObjectId id) {
        return gridFsTemplate.delete(Query.query(Criteria.where("_id").is(id)));
    }


    /**
     * 獲取 GridFsTemplate
     *
     * @return GridFsTemplate
     */
    @SkipRecord
    public ReactiveGridFsTemplate getGridFsTemplate() {
        return gridFsTemplate;
    }
}
