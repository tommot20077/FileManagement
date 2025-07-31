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
 * 基於 ReactiveGridFsTemplate 的 MongoDB GridFS 檔案操作封裝類別。
 *
 * <p>提供對 MongoDB GridFS 分散式檔案系統的完整操作介面，支援大檔案的分塊存儲、
 * 非阻塞檔案查詢、批次檔案處理及反應式資源串流。採用反應式程式設計模型，
 * 確保高併發環境下的檔案操作效能。</p>
 *
 * <p>預設分塊大小設定為 1020KB，在 MongoDB 16MB 文件限制下預留元資料存儲空間。
 * 支援基於檔名或 ObjectId 的單一及批次檔案查詢，提供檔案資源的串流存取。
 * 所有檔案操作透過 Mono 和 Flux 返回類型實現非阻塞處理。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ReactiveGridFsTemplate
 * @see GridFSUploadOptions
 */
@Component
@SuppressWarnings("unused")
public class GridFsProvider {
    /**
     * MongoDB GridFS 反應式操作模板，提供非阻塞檔案系統存取能力
     */
    private final ReactiveGridFsTemplate gridFsTemplate;

    /**
     * GridFS 檔案上傳配置選項，定義分塊存儲參數
     *
     * <p>分塊大小設定為檔案配置中指定的上傳大小限制乘以 1020 倍數，
     * 在 MongoDB 16MB 文件限制下預留元資料存儲空間，確保大檔案的高效分塊處理。</p>
     *
     * @see GridFSUploadOptions GridFS上傳選項
     */
    private final GridFSUploadOptions uploadOptions;

    /**
     * 建構 GridFS 檔案操作提供者實例
     *
     * <p>初始化 ReactiveGridFsTemplate 和上傳配置選項，根據檔案屬性設定
     * 計算適當的分塊大小，確保在 MongoDB 限制內實現最佳效能。</p>
     *
     * @param gridFsTemplate MongoDB GridFS 反應式操作模板
     * @param fileProperties 檔案系統配置屬性，包含上傳大小限制等參數
     */
    public GridFsProvider(ReactiveGridFsTemplate gridFsTemplate, FileProperties fileProperties) {
        this.gridFsTemplate = gridFsTemplate;
        this.uploadOptions = new GridFSUploadOptions().chunkSizeBytes((int) (fileProperties.getUpload().getPayloadLength().toKilobytes() * 1020));
    }


    /**
     * 將檔案資料流存儲至 GridFS 檔案系統
     *
     * <p>採用分塊存儲機制處理大檔案，根據預設的上傳配置選項進行分塊切割。
     * 檔案將以指定檔名存儲，並返回生成的 MongoDB ObjectId 供後續檔案操作使用。</p>
     *
     * @param dataBufferFlux 檔案內容的反應式資料緩衝區串流
     * @param filename 存儲檔案的名稱標識
     * @return 包含新建檔案 ObjectId 的 Mono，用於檔案識別和後續操作
     */
    public Mono<ObjectId> storeFile(Flux<DataBuffer> dataBufferFlux, String filename) {
        return gridFsTemplate.store(dataBufferFlux, filename, uploadOptions);
    }


    /**
     * 根據檔案名稱查詢 GridFS 檔案
     *
     * <p>在 GridFS 檔案系統中執行精確檔名匹配查詢，返回符合條件的第一個檔案。
     * 若檔案不存在，返回空的 Mono。</p>
     *
     * @param filename 要查詢的檔案名稱
     * @return 包含找到的 GridFS 檔案物件的 Mono，若無匹配檔案則為空
     */
    public Mono<GridFSFile> findFileByFileName(String filename) {
        return gridFsTemplate.findOne(Query.query(Criteria.where("filename").is(filename)));
    }


    /**
     * 根據檔案名稱集合批次查詢 GridFS 檔案
     *
     * <p>執行批次檔案查詢，針對提供的檔名集合在 GridFS 中進行匹配搜尋。
     * 結果以檔名為鍵、GridFS 檔案物件為值的 Map 形式返回，便於後續檔案處理。</p>
     *
     * @param filenames 要查詢的檔案名稱集合
     * @return 包含檔名與對應 GridFS 檔案物件映射的 Mono Map
     */
    public Mono<Map<String, GridFSFile>> findFilesByFileName(Collection<String> filenames) {
        return gridFsTemplate
                .find(Query.query(Criteria.where("filename").in(filenames)))
                .collectMap(GridFSFile::getFilename, gridFSFile -> gridFSFile);

    }


    /**
     * 根據 MongoDB ObjectId 查詢 GridFS 檔案
     *
     * <p>使用 MongoDB 的唯一物件識別碼進行檔案查詢，提供精確的檔案定位。
     * ObjectId 確保檔案的唯一性識別，適用於需要精確檔案定位的場景。</p>
     *
     * @param id MongoDB ObjectId 檔案唯一識別碼
     * @return 包含找到的 GridFS 檔案物件的 Mono，若檔案不存在則為空
     */
    public Mono<GridFSFile> findFileById(ObjectId id) {
        return gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(id)));
    }


    /**
     * 根據 MongoDB ObjectId 集合批次查詢 GridFS 檔案
     *
     * <p>執行批次檔案查詢，針對提供的 ObjectId 集合在 GridFS 中進行精確匹配。
     * 結果以 ObjectId 為鍵、GridFS 檔案物件為值的 Map 形式返回，提供高效的批次檔案存取。</p>
     *
     * @param ids 要查詢的 MongoDB ObjectId 集合
     * @return 包含 ObjectId 與對應 GridFS 檔案物件映射的 Mono Map
     */
    public Mono<Map<ObjectId, GridFSFile>> findFilesById(Collection<ObjectId> ids) {
        return gridFsTemplate.find(Query.query(Criteria.where("_id").in(ids))).collectMap(GridFSFile::getObjectId, gridFSFile -> gridFSFile);
    }


    /**
     * 取得 GridFS 檔案的反應式資源串流
     *
     * <p>將 GridFS 檔案轉換為可讀取的反應式資源，支援檔案內容的串流存取。
     * 返回的資源可用於檔案下載、內容讀取或進一步的資料處理操作。</p>
     *
     * @param gridFsFile 要取得資源的 GridFS 檔案物件
     * @return 包含檔案反應式資源的 Mono，提供檔案內容的串流存取介面
     */
    @SkipRecord
    public Mono<ReactiveGridFsResource> getResource(GridFSFile gridFsFile) {
        return gridFsTemplate.getResource(gridFsFile);
    }


    /**
     * 根據檔案名稱刪除 GridFS 檔案
     *
     * <p>在 GridFS 檔案系統中查找並刪除指定檔名的檔案。
     * 若存在多個同名檔案，將刪除所有匹配的檔案。操作完成後返回空的 Mono。</p>
     *
     * @param filename 要刪除的檔案名稱
     * @return 表示刪除操作完成的空 Mono
     */
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<Void> deleteFileByFilename(String filename) {
        return gridFsTemplate.delete(Query.query(Criteria.where("filename").is(filename)));
    }


    /**
     * 根據 MongoDB ObjectId 刪除 GridFS 檔案
     *
     * <p>使用檔案的唯一識別碼精確刪除指定的 GridFS 檔案。
     * ObjectId 保證檔案的唯一性，避免誤刪其他檔案。操作完成後返回空的 Mono。</p>
     *
     * @param id 要刪除檔案的 MongoDB ObjectId
     * @return 表示刪除操作完成的空 Mono
     */
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<Void> deleteFileById(ObjectId id) {
        return gridFsTemplate.delete(Query.query(Criteria.where("_id").is(id)));
    }


    /**
     * 取得 ReactiveGridFsTemplate 實例
     *
     * <p>提供對底層 MongoDB GridFS 操作模板的直接存取，供需要執行
     * 自定義 GridFS 操作或進階查詢的場景使用。</p>
     *
     * @return ReactiveGridFsTemplate 實例，用於進階 GridFS 操作
     */
    @SkipRecord
    public ReactiveGridFsTemplate getGridFsTemplate() {
        return gridFsTemplate;
    }
}
