package xyz.dowob.filemanagement.component.provider.providerImpl;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName GridFsProvider
 * @description
 * @create 2024-09-27 00:55
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
public class GridFsProvider {
    private final ReactiveGridFsTemplate gridFsTemplate;

    GridFSUploadOptions uploadOptions = new GridFSUploadOptions().chunkSizeBytes(1022 * 1024 * 10);

    public Mono<ObjectId> storeFile(Flux<DataBuffer> dataBufferFlux, String fileName) {
        return gridFsTemplate.store(dataBufferFlux, fileName, uploadOptions);
    }

    public Mono<GridFSFile> findFileByFileName(String fileName) {
        return gridFsTemplate.findOne(Query.query(Criteria.where("filename").is(fileName)));
    }

    public Mono<GridFSFile> findFileById(ObjectId id) {
        return gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(id)));
    }

    public Mono<ReactiveGridFsResource> getResource(GridFSFile gridFsFile) {
        return gridFsTemplate.getResource(gridFsFile);
    }

    public Mono<Void> deleteFileByFilename(String fileName) {
        return gridFsTemplate.delete(Query.query(Criteria.where("filename").is(fileName)));
    }

    public Mono<Void> deleteFileById(ObjectId id) {
        return gridFsTemplate.delete(Query.query(Criteria.where("_id").is(id)));
    }


}
