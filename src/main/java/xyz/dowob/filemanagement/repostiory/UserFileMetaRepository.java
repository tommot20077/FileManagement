package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.domain.SqlSort;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dao.ServerFileMetaCountDao;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.entity.FileTrashRecord;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用戶檔案元數據操作介面，使用Spring Data R2DBC來操作數據庫，繼承ReactiveCrudRepository。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserFileMetaRepository
 * @description
 * @create 2024-09-26 18:46
 * @Version 1.0
 **/
@Repository
public interface UserFileMetaRepository extends ReactiveCrudRepository<UserFileMetadata, String> {

    /**
     * 根據用戶ID查詢所有檔案元數據
     *
     * @param userId 用戶ID
     *
     * @return Flux<UserFileMetadata>
     */
    Flux<UserFileMetadata> findAllByUserId(Long userId);


    /**
     * 根據用戶ID查詢所有檔案元數據並可指定是否需要顯示刪除檔案
     *
     * @param userId 用戶ID
     *
     * @return Flux<UserFileMetadata>
     */
    Flux<UserFileMetadata> findAllByUserIdAndIsDeleted(Long userId, Boolean isDeleted);

    /**
     * 根據用戶ID和父文件夾ID查詢檔案元數據，此方法可以蒐尋多個父文件夾ID並返回所有符合條件的檔案元數據
     *
     * @param parentFolderId 父文件夾ID
     *
     * @return Flux<UserFileMetadata> 返回所有符合條件的檔案元數據
     */
    default Flux<UserFileMetadata> findAllByParentFolderIdIn(List<Long> parentFolderId, R2dbcEntityOperations entityOperations) {
        return entityOperations
                .select(UserFileMetadata.class)
                .matching(org.springframework.data.relational.core.query.Query.query(Criteria.where("parent_folder_id").in(parentFolderId))
                                  .sort(SqlSort.unsafe("CASE WHEN file_type = 'folder' THEN 0 ELSE 1 END")))
                .all();
    }

    /**
     * 根據用戶ID和父文件夾ID查詢檔案元數據並可指定是否需要顯示刪除檔案，此方法可以蒐尋多個父文件夾ID並返回所有符合條件的檔案元數據
     *
     * @param parentFolderId 父文件夾ID
     *
     * @return Flux<UserFileMetadata> 返回所有符合條件的檔案元數據
     */
    default Flux<UserFileMetadata> findAllByParentFolderIdInAndIsDeleted(List<Long> parentFolderId, Boolean isDeleted, R2dbcEntityOperations entityOperations) {
        return entityOperations
                .select(UserFileMetadata.class)
                .matching(org.springframework.data.relational.core.query.Query
                                  .query(Criteria.where("parent_folder_id").in(parentFolderId).and("is_deleted").is(isDeleted))
                                  .sort(SqlSort.unsafe("CASE WHEN file_type = 'folder' THEN 0 ELSE 1 END")))
                .all();
    }

    /**
     * 根據用戶ID和父文件夾ID查詢檔案元數據(此方法為查詢根文件夾)
     *
     * @param userId 用戶ID
     *
     * @return Flux<UserFileMetadata> 返回根文件夾下的所有檔案元數據
     */
    @Query("SELECT * FROM user_file_metadata WHERE user_id = :userId AND parent_folder_id IS NULL AND is_deleted = 0 ORDER BY CASE WHEN parent_folder_id IS NULL THEN 0 ELSE 1 END, filename")
    Flux<UserFileMetadata> findAllByUserIdAndParentFolderIdIsNull(@Param("userId") Long userId);

    /**
     * 查詢所有星標檔案
     *
     * @param userId 用戶ID
     * @param isStar 是否為星標檔案
     **/
    Flux<UserFileMetadata> findAllByUserIdAndIsStarAndIsDeleted(Long userId, Boolean isStar, Boolean isDeleted);

    /**
     * 根據用戶ID和最後訪問時間查詢檔案元數據
     *
     * @param userId 用戶ID
     *
     * @return Flux<UserFileMetadata> 返回所有檔案元數據
     */
    default Flux<UserFileMetadata> findAllByUserIdOrderByLastAccessTimeDesc(Long userId, List<FileEnum> type, R2dbcEntityOperations entityOperations) {

        Criteria criteria = Criteria.where("user_id").is(userId).and("file_type").not("FOLDER").and("is_deleted").is(false);

        if (type != null && !type.isEmpty()) {
            List<String> typeList = type.stream().map(FileEnum::name).toList();
            criteria = criteria.and("file_type").in(typeList);
        }

        org.springframework.data.relational.core.query.Query query = org.springframework.data.relational.core.query.Query
                .query(criteria)
                .limit(20)
                .sort(Sort.by(Sort.Direction.DESC, "last_access_time"));

        return entityOperations.select(query, UserFileMetadata.class);
    }

    /**
     * 計算該用戶擁有同一伺服器檔案的檔案數量
     *
     * @param userId           用戶ID
     * @param serverFileIds    伺服器檔案ID
     * @param entityOperations R2dbc實體操作
     *
     * @return Mono<Long> 返回檔案數量
     */
    default Flux<ServerFileMetaCountDao> countByServerFileIdInAndUserId(
            @Param("serverFileIds") List<Long> serverFileIds, @Param("userId") Long userId, R2dbcEntityOperations entityOperations) {

        return entityOperations
                .getDatabaseClient()
                .sql("SELECT server_file_id, COUNT(*) as count FROM user_file_metadata WHERE server_file_id IN (:serverFileIds) AND user_id = :userId GROUP BY server_file_id")
                .bind("serverFileIds", serverFileIds)
                .bind("userId", userId)
                .map((row, metadata) -> new ServerFileMetaCountDao(row.get("server_file_id", Long.class), row.get("count", Long.class)))
                .all();
    }

    /**
     * 根據用戶ID查詢所有位於回收站的檔案元數據，並轉換查詢為UserFileMetadata結果，並按照是否為資料夾和檔案名稱排序
     *
     * @param userId 用戶ID
     *
     * @return Flux<UserFileMetadata>
     */
    default Flux<UserFileMetadata> findAllByUserIdOrderByIsFolder(Long userId, R2dbcEntityOperations r2dbcEntityOperations) {
        return r2dbcEntityOperations
                .select(FileTrashRecord.class)
                .matching(org.springframework.data.relational.core.query.Query.query(Criteria.where("user_id").is(userId)))
                .all()
                .collectList()
                .flatMapMany(fileIds -> {
                    if (fileIds.isEmpty()) {
                        return Flux.empty();
                    }
                    return r2dbcEntityOperations
                            .select(UserFileMetadata.class)
                            .matching(org.springframework.data.relational.core.query.Query
                                              .query(Criteria.where("id").in(fileIds))
                                              .sort(SqlSort.unsafe("CASE WHEN file_type = 'folder' THEN 0 ELSE 1 END, filename")))
                            .all();
                });
    }

    /**
     * 根據用戶ID和過濾條件查詢檔案元數據
     *
     * @param userId                用戶ID
     * @param fileFilterDTO         過濾條件
     * @param r2dbcEntityOperations R2dbc實體操作
     *
     * @return Flux<UserFileMetadata>
     */
    default Flux<UserFileMetadata> findAllByUserIdAndFilterDTO(Long userId, FileFilterDTO fileFilterDTO, R2dbcEntityOperations r2dbcEntityOperations) {
        StringBuilder sql = new StringBuilder("SELECT * FROM user_file_metadata WHERE user_id = :userId");
        String keyword = fileFilterDTO.getKeyword();
        Long folderId = fileFilterDTO.getFolderId();
        LocalDateTime startTime = fileFilterDTO.getStartTime();
        LocalDateTime endTime = fileFilterDTO.getEndTime();
        List<FileEnum> types = fileFilterDTO.getTypes();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND MATCH(filename) AGAINST(:keyword IN NATURAL LANGUAGE MODE) AND is_deleted = 0");
        }
        if (folderId != null) {
            if (folderId == 0) {
                sql.append(" AND parent_folder_id IS NULL");
            } else {
                sql.append(" AND parent_folder_id = :folderId");
            }
        }
        if (types != null && !types.isEmpty()) {
            sql.append(" AND file_type IN (:types)");
        }

        if (startTime != null) {
            sql.append(" AND last_access_time >= :startTime");
        }

        if (endTime != null) {
            sql.append(" AND last_access_time <= :endTime");
        }

        DatabaseClient.GenericExecuteSpec bindSpec = r2dbcEntityOperations.getDatabaseClient().sql(sql.toString()).bind("userId", userId);

        if (keyword != null && !keyword.isEmpty()) {
            bindSpec = bindSpec.bind("keyword", keyword);
        }
        if (folderId != null && folderId != 0) {
            bindSpec = bindSpec.bind("folderId", folderId);
        }
        if (types != null && !types.isEmpty()) {
            bindSpec = bindSpec.bind("types", types);
        }
        if (startTime != null) {
            bindSpec = bindSpec.bind("startTime", startTime);
        }
        if (endTime != null) {
            bindSpec = bindSpec.bind("endTime", endTime);
        }

        return bindSpec.map((row, metadata) -> r2dbcEntityOperations.getConverter().read(UserFileMetadata.class, row, metadata)).all();
    }
}
