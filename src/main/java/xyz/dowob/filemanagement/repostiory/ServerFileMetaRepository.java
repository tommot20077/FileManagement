package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;

import java.util.Set;

/**
 * 伺服器檔案元資料響應式操作介面
 *
 * <p>基於 Spring Data R2DBC 的非阻塞資料庫操作介面，提供伺服器檔案元資料的響應式查詢能力
 * 支援與 GridFS 整合，並提供高效能的非阻塞資料庫存取機制</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface ServerFileMetaRepository extends ReactiveCrudRepository<ServerFileMetadata, Long> {
    /**
     * 根據 GridFS ID 查詢伺服器檔案元資料。
     * <p>
     * 此方法用於透過 MongoDB GridFS 的唯一識別碼查詢對應的檔案元資料，
     * 通常用於檔案下載、檔案資訊顯示等場景。GridFS 是 MongoDB 的分散式檔案儲存方案。
     * </p>
     *
     * @param gridFsId MongoDB GridFS 的唯一識別碼，不得為 null 或空字串
     * @return 包含對應檔案元資料的 {@link Mono}，如果找不到則為空
     */
    Mono<ServerFileMetadata> findByGridFsId(String gridFsId);

    /**
     * 根據 MD5 雜湊值查詢伺服器檔案元資料。
     * <p>
     * 此方法用於檔案去重和內容比對，透過 MD5 雜湊值的匹配查詢相同內容的檔案。
     * 在檔案上傳時可以避免重複儲存相同內容的檔案，節省儲存空間。
     * MD5 雜湊值是檔案內容的唯一指紋，相同內容的檔案具有相同的 MD5 值。
     * </p>
     *
     * @param md5 檔案內容的 MD5 雜湊值，不得為 null 或空字串
     * @return 包含相同 MD5 值檔案元資料的 {@link Mono}，如果找不到則為空
     */
    Mono<ServerFileMetadata> findByMd5(String md5);

    /**
     * 批次查詢指定伺服器檔案 ID 集合的檔案元資料。
     * <p>
     * 此方法用於批量查詢多個伺服器檔案的元資料資訊，提供高效的批量查詢能力。
     * 通常用於批量檔案資訊獲取、檔案清单顯示等場景，以及用戶橫爇功能中的批量操作。
     * 透過一次查詢多個檔案，減少資料庫存取次數並提高效能。
     * </p>
     *
     * @param serverFileId 要查詢的伺服器檔案 ID 集合，不得為 null 或包含 null 元素
     * @return 包含所有符合指定 ID 檔案元資料的 {@link Flux}，可能為空流
     */
    Flux<ServerFileMetadata> findAllByIdIn(Set<Long> serverFileId);
}