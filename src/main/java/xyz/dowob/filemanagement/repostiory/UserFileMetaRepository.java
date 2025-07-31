package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.customenum.UserFileListOrderEnum;
import xyz.dowob.filemanagement.data.file.dao.ServerFileMetaCountDAO;
import xyz.dowob.filemanagement.data.file.dao.UserFileMetaWithDataDAO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileShareRecord;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 用戶檔案元資料響應式資料存取層介面，提供完整的檔案元資料管理功能。
 * <p>
 * 此介面繼承自 Spring Data R2DBC 的 {@link ReactiveCrudRepository}，
 * 提供對用戶檔案元資料的完整 CRUD 操作和複雜查詢功能。
 * 支援檔案分享、權限管理、檔案搜尋等進階功能。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>基本的 CRUD 操作（繼承自父介面）</li>
 *   <li>用戶檔案查詢和過濾功能</li>
 *   <li>檔案分享權限管理</li>
 *   <li>複雜的資料結合查詢（JOIN）</li>
 *   <li>批量操作和統計分析</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserFileMetadata
 * @see ReactiveCrudRepository
 */
@Repository
public interface UserFileMetaRepository extends ReactiveCrudRepository<UserFileMetadata, String> {

    /**
     * 根據用戶 ID 查詢所有檔案元資料。
     * <p>
     * 此方法用於獲取指定用戶擁有的所有檔案元資料，
     * 包括檔案、資料夾和其他類型的資料。通常用於用戶檔案清单顯示和管理功能。
     * </p>
     *
     * @param userId 用戶的唯一識別碼，不得為 null
     * @return 包含用戶所有檔案元資料的 {@link Flux}，可能為空流
     */
    Flux<UserFileMetadata> findAllByUserId(Long userId);

    /**
     * 根據父資料夾 ID 集合批次查詢檔案元資料。
     * <p>
     * 此方法允許一次性查詢多個父資料夾下的所有檔案元資料，提供高效的批量查詢能力。
     * 查詢結果會自動排除已刪除的檔案，僅回傳有效的檔案記錄。
     * 常用於資料夾樹展開、批量檔案操作等場景。
     * </p>
     *
     * @param parentFolderId 父資料夾 ID 集合，不得為 null 或包含 null 元素
     * @param entityOperations R2DBC 實體操作介面，提供底層資料庫存取能力
     * @return 包含所有符合條件檔案元資料的 {@link Flux}，可能為空流
     */
    default Flux<UserFileMetadata> findAllByParentFolderIdIn(List<Long> parentFolderId, R2dbcEntityOperations entityOperations) {
        Criteria criteria = Criteria.where("parent_folder_id").in(parentFolderId).and("is_deleted").is(false);
        return entityOperations.select(UserFileMetadata.class).matching(org.springframework.data.relational.core.query.Query.query(criteria)).all();
    }


    /**
     * 查詢指定父資料夾下用戶有權限存取的所有檔案元資料。
     * <p>
     * 此方法實現了複雜的檔案權限控制邏輯，會根據檔案的分享類型和用戶權限
     * 來決定用戶是否能夠存取特定的檔案。支援公開分享、特定用戶分享和預設繼承權限等多種分享模式。
     * 這是檔案權限管理系統的核心方法之一。
     * </p>
     * <p>
     * 權限判斷規則：
     * <ul>
     *   <li>資料夾類型檔案：用戶始終可存取</li>
     *   <li>檔案擁有者：擁有完整存取權限</li>
     *   <li>公開分享檔案：所有用戶均可存取</li>
     *   <li>特定分享檔案：僅被分享的用戶可存取</li>
     *   <li>預設分享檔案：繼承父資料夾的分享權限</li>
     * </ul>
     * </p>
     *
     * @param parentFolderId 父資料夾 ID，指定要查詢的資料夾
     * @param user 請求存取的用戶物件，用於權限驗證
     * @param entityOperations R2DBC 實體操作介面，提供底層資料庫存取能力
     * @return 包含用戶有權限存取的檔案元資料的 {@link Flux}，可能為空流
     */
    default Flux<UserFileMetadata> findAllByParentFolderIdWithShare(Long parentFolderId, User user, R2dbcEntityOperations entityOperations) {
        List<UserFileMetadata> allowFiles = new ArrayList<>();

        Criteria criteria = Criteria.where("parent_folder_id").is(parentFolderId).and("is_deleted").is(false);
        Mono<UserFileMetadata> folderMono = findById(parentFolderId.toString());
        Mono<List<UserFileMetadata>> childFileList = entityOperations
                .select(UserFileMetadata.class)
                .matching(org.springframework.data.relational.core.query.Query.query(criteria))
                .all()
                .collectList();

        return childFileList.flatMapMany(filesList -> {
            Set<Long> allFileIdSet = filesList.stream().map(UserFileMetadata::getId).collect(Collectors.toSet());
            allFileIdSet.add(parentFolderId);
            Criteria shareCriteria = Criteria.where("user_id").is(user.getId()).and("file_id").in(allFileIdSet);

            Mono<Map<Long, UserFileShareRecord>> shareRecords = entityOperations
                    .select(UserFileShareRecord.class)
                    .matching(org.springframework.data.relational.core.query.Query.query(shareCriteria))
                    .all()
                    .collectMap(UserFileShareRecord::getFileId);


            return Mono.zip(folderMono, shareRecords).flatMapMany(tuple2 -> {
                UserFileMetadata folder = tuple2.getT1();
                Map<Long, UserFileShareRecord> shareRecordMap = tuple2.getT2();

                filesList.forEach(file -> {
                    if (file.getFileType() == FileEnum.FOLDER || Objects.equals(file.getUserId(), user.getId())) {
                        allowFiles.add(file);
                        return;
                    }

                    if (file.getShareType() == FileShareTypeEnum.PUBLIC) {
                        allowFiles.add(file);
                    } else if (file.getShareType() != FileShareTypeEnum.NONE) {
                        if (shareRecordMap.get(file.getId()) != null) {
                            allowFiles.add(file);
                        } else if (file.getShareType() == FileShareTypeEnum.DEFAULT && shareRecordMap.get(folder.getId()) != null) {
                            allowFiles.add(file);
                        }
                    }
                });
                return Flux.fromIterable(allowFiles);
            });
        });
    }


    /**
     * 統計指定用戶對於伺服器檔案的引用計數。
     * <p>
     * 此方法用於計算同一個伺服器檔案被特定用戶引用的次數，主要用於檔案去重和儲存空間統計。
     * 當多個用戶檔案指向同一個實際的伺服器檔案時，此方法可以統計每個伺服器檔案的引用次數，
     * 這對於檔案刪除時的引用計數管理和儲存空間優化非常重要。
     * </p>
     *
     * @param serverFileIds 要統計的伺服器檔案 ID 集合，不得為 null 或包含 null 元素
     * @param userId 用戶 ID，指定要統計的用戶
     * @param entityOperations R2DBC 實體操作介面，提供底層資料庫存取能力
     * @return 包含檔案引用計數資料的 {@link Flux}，每個元素包含伺服器檔案 ID 和對應的引用次數
     * @see ServerFileMetaCountDAO
     */
    default Flux<ServerFileMetaCountDAO> countByServerFileIdInAndUserId(
            @Param("serverFileIds") List<Long> serverFileIds, @Param("userId") Long userId, R2dbcEntityOperations entityOperations) {

        return entityOperations
                .getDatabaseClient()
                .sql("SELECT server_file_id, COUNT(*) as count FROM user_file_metadata WHERE server_file_id IN (:serverFileIds) AND user_id = :userId GROUP BY server_file_id")
                .bind("serverFileIds", serverFileIds)
                .bind("userId", userId)
                .map((row, metadata) -> new ServerFileMetaCountDAO(row.get("server_file_id", Long.class), row.get("count", Long.class)))
                .all();
    }


    /**
     * 根據用戶 ID 和過濾條件查詢複合檔案元資料（包含檔案資料）。
     * <p>
     * 此方法執行複雜的多表聯接查詢，結合用戶檔案元資料、伺服器檔案元資料、
     * 線上檔案資料和用戶資訊，提供完整的檔案檢視。支援多種過濾條件包括關鍵字搜尋、
     * 資料夾過濾、時間範圍、檔案類型等。這是檔案搜尋和列表顯示功能的核心方法。
     * </p>
     * <p>
     * 支援的過濾條件：
     * <ul>
     *   <li>關鍵字搜尋（使用 MySQL 全文搜尋）</li>
     *   <li>指定資料夾過濾</li>
     *   <li>檔案類型過濾</li>
     *   <li>時間範圍過濾</li>
     *   <li>是否包含已刪除檔案</li>
     *   <li>是否包含分享檔案</li>
     * </ul>
     * </p>
     *
     * @param userId 用戶 ID，指定要查詢的用戶
     * @param fileFilterDTO 檔案過濾條件，包含各種搜尋和過濾參數
     * @param r2dbcEntityOperations R2DBC 實體操作介面，提供底層資料庫存取能力
     * @return 包含完整檔案資訊的複合資料流 {@link Flux}，可能為空流
     * @see UserFileMetaWithDataDAO
     * @see FileFilterDTO
     */
    default Flux<UserFileMetaWithDataDAO> findAllByUserIdAndFilterDTO(Long userId, FileFilterDTO fileFilterDTO, R2dbcEntityOperations r2dbcEntityOperations) {
        StringBuilder where = new StringBuilder(" WHERE ufm.user_id = :userId");
        Map<String, Object> bind = new HashMap<>();
        bind.put("userId", userId);

        String keyword = fileFilterDTO.getKeyword();
        Long folderId = fileFilterDTO.getFolderId();
        LocalDateTime startTime = fileFilterDTO.getStartTime();
        LocalDateTime endTime = fileFilterDTO.getEndTime();
        List<FileEnum> types = fileFilterDTO.getTypes();
        boolean includeDeleted = fileFilterDTO.isIncludeDeleted();
        boolean includeShared = fileFilterDTO.isIncludeShared();

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND MATCH(ufm.filename) AGAINST(:keyword IN BOOLEAN MODE)");
            bind.put("keyword", keyword);
        }

        if (folderId != null) {
            if (folderId == 0) {
                where.append(" AND ufm.parent_folder_id IS NULL");
            } else {
                where.append(" AND ufm.parent_folder_id = :folderId");
                bind.put("folderId", folderId);
            }
        }

        if (types != null && !types.isEmpty()) {
            where.append(" AND ufm.file_type IN (:types)");
            bind.put("types", types.stream().map(FileEnum::name).toList());
        }

        if (startTime != null) {
            where.append(" AND ufm.last_access_time >= :startTime");
            bind.put("startTime", startTime);
        }

        if (endTime != null) {
            where.append(" AND ufm.last_access_time <= :endTime");
            bind.put("endTime", endTime);
        }

        if (!includeDeleted) {
            where.append(" AND ufm.is_deleted = 0");
        }

        StringBuilder orderByBuilder = new StringBuilder(" ORDER BY CASE ufm_file_type");
        Stream.of(FileEnum.values()).forEach(fileType -> {
            int order = UserFileListOrderEnum.getOrder(fileType);
            orderByBuilder.append(" WHEN '").append(fileType.name()).append("' THEN ").append(order);
        });
        orderByBuilder.append(" ELSE ").append(UserFileListOrderEnum.LAST.getOrder()).append(" END, ufm_filename");

        String sql = """
                WITH base AS (
                    SELECT  ufm.id,
                            ufm.user_id,
                            ufm.server_file_id,
                            ufm.filename,
                            ufm.parent_folder_id,
                            ufm.is_star,
                            ufm.file_type,
                            ufm.share_type,
                            ufm.upload_time,
                            ufm.last_access_time,
                            ufm.is_deleted
                    FROM    user_file_metadata ufm
                """ + where + """
                )
                SELECT  b.id              AS ufm_id,
                        b.user_id         AS u_owner_id,
                        b.filename        AS ufm_filename,
                        b.parent_folder_id AS ufm_parent_folder_id,
                        b.is_star         AS ufm_is_star,
                        b.file_type       AS ufm_file_type,
                        b.share_type      AS ufm_share_type,
                        b.upload_time     AS ufm_upload_time,
                        b.last_access_time AS ufm_last_access_time,
                        b.is_deleted      AS ufm_is_deleted,
                        sfm.id            AS sfm_id,
                        sfm.file_size     AS sfm_file_size,
                        sfm.mime_type     AS sfm_mime_type,
                        sfm.grid_fs_id    AS sfm_grid_fs_id,
                        sfm.md5           AS sfm_md5,
                        uof.id            AS uof_id,
                        uof.file_size     AS uof_file_size,
                        u.username        AS u_owner_name
                FROM    base b
                LEFT JOIN server_file_metadata sfm ON sfm.id = b.server_file_id
                LEFT JOIN user_online_file     uof ON uof.id = b.id
                LEFT JOIN users                u   ON u.id = b.user_id
                """ + orderByBuilder;

        LogUnity.trace("查詢所使用的語句 SQL: [%s], 綁定參數: %s", sql, bind);

        DatabaseClient.GenericExecuteSpec spec = r2dbcEntityOperations.getDatabaseClient().sql(sql);
        for (Map.Entry<String, Object> entry : bind.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                spec = spec.bind(entry.getKey(), value);
            } else {
                spec = spec.bindNull(entry.getKey(), Object.class);
            }
        }

        Flux<UserFileMetaWithDataDAO> mainQueryFlux = spec.map((row, meta) -> {
            UserFileMetaWithDataDAO dao = new UserFileMetaWithDataDAO();

            dao.setUfmId(row.get("ufm_id", Long.class));
            dao.setUfmFilename(row.get("ufm_filename", String.class));
            dao.setUfmParentFolderId(row.get("ufm_parent_folder_id", Long.class));
            dao.setUfmIsStar(Boolean.TRUE.equals(row.get("ufm_is_star", Boolean.class)));

            dao.setUfmFileType(Optional.ofNullable(row.get("ufm_file_type", String.class)).map(FileEnum::valueOf).orElse(FileEnum.OTHER));
            dao.setUfmShareType(Optional
                                        .ofNullable(row.get("ufm_share_type", String.class))
                                        .map(FileShareTypeEnum::valueOf)
                                        .orElse(FileShareTypeEnum.DEFAULT));

            dao.setUfmUploadTime(row.get("ufm_upload_time", LocalDateTime.class));
            dao.setUfmLastAccessTime(row.get("ufm_last_access_time", LocalDateTime.class));
            dao.setUfmIsDeleted(Boolean.TRUE.equals(row.get("ufm_is_deleted", Boolean.class)));

            if (row.get("sfm_id", Long.class) != null) {
                dao.setSfmId(row.get("sfm_id", Long.class));
                dao.setSfmFileSize(row.get("sfm_file_size", Long.class));
                dao.setSfmMimeType(row.get("sfm_mime_type", String.class));
                dao.setSfmGridFsId(row.get("sfm_grid_fs_id", String.class));
                dao.setSfmMd5(row.get("sfm_md5", String.class));
            }

            if (dao.getUfmFileType() == FileEnum.ONLINE_DOCUMENT && row.get("uof_id", Long.class) != null) {
                dao.setUofId(row.get("uof_id", Long.class));
                dao.setSfmFileSize(row.get("uof_file_size", Long.class));
            }

            dao.setOwnerUsername(row.get("u_owner_name", String.class));
            dao.setUfmUserId(row.get("u_owner_id", Long.class));
            return dao;
        }).all();

        Flux<UserFileMetaWithDataDAO> sharedFilesFlux = Flux.empty();
        if (includeShared) {
            sharedFilesFlux = findSharedUserFileMetaWithDataDAO(userId, r2dbcEntityOperations);
        }
        return mainQueryFlux.mergeWith(sharedFilesFlux);
    }

    /**
     * 查詢所有分享給指定用戶的複合檔案元資料（包含檔案資料）。
     * <p>
     * 此方法專門用於查詢其他用戶分享給指定用戶的檔案，實現檔案分享功能的核心查詢邏輯。
     * 查詢會結合檔案分享記錄表，僅回傳確實被分享且用戶有權限存取的檔案。
     * 查詢結果包含完整的檔案資訊，包括檔案元資料、實際檔案資料和擁有者資訊。
     * </p>
     * <p>
     * 查詢邏輯：
     * <ul>
     *   <li>通過檔案分享記錄表確認分享關係</li>
     *   <li>排除已刪除和禁止分享的檔案</li>
     *   <li>提供完整的檔案詳細資訊</li>
     *   <li>包含原始檔案擁有者的用戶名資訊</li>
     * </ul>
     * </p>
     *
     * @param userId 被分享用戶的 ID，指定要查詢分享檔案的用戶
     * @param r2dbcEntityOperations R2DBC 實體操作介面，提供底層資料庫存取能力
     * @return 包含所有分享檔案資訊的複合資料流 {@link Flux}，可能為空流
     * @see UserFileMetaWithDataDAO
     */

    default Flux<UserFileMetaWithDataDAO> findSharedUserFileMetaWithDataDAO(Long userId, R2dbcEntityOperations r2dbcEntityOperations) {
        String sql = """
                WITH base AS (
                    SELECT  ufm.id,
                            ufm.user_id,
                            ufm.server_file_id,
                            ufm.filename,
                            ufm.parent_folder_id,
                            ufm.is_star,
                            ufm.file_type,
                            ufm.share_type,
                            ufm.upload_time,
                            ufm.last_access_time,
                            ufm.is_deleted
                    FROM    user_file_metadata ufm
                    WHERE ufm.id IN (SELECT ufsr.file_id FROM user_file_share_record ufsr WHERE ufsr.user_id = :sharedWithUserId
                    AND ufm.is_deleted = 0 AND ufm.share_type != 'NONE'
                ))
                SELECT  b.id              AS ufm_id,
                        b.user_id         AS u_owner_id,
                        b.filename        AS ufm_filename,
                        b.parent_folder_id AS ufm_parent_folder_id,
                        b.is_star         AS ufm_is_star,
                        b.file_type       AS ufm_file_type,
                        b.share_type      AS ufm_share_type,
                        b.upload_time     AS ufm_upload_time,
                        b.last_access_time AS ufm_last_access_time,
                        b.is_deleted      AS ufm_is_deleted,
                        sfm.id            AS sfm_id,
                        sfm.file_size     AS sfm_file_size,
                        sfm.mime_type     AS sfm_mime_type,
                        sfm.grid_fs_id    AS sfm_grid_fs_id,
                        sfm.md5           AS sfm_md5,
                        uof.id            AS uof_id,
                        uof.file_size     AS uof_file_size,
                        u.username        AS u_owner_name
                FROM    base b
                LEFT JOIN server_file_metadata sfm ON sfm.id = b.server_file_id
                LEFT JOIN user_online_file     uof ON uof.id = b.id
                LEFT JOIN users                u   ON u.id = b.user_id
                
                """;

        LogUnity.trace("查詢所使用的語句 SQL: [%s], 綁定參數: [sharedWithUserId:%s]", sql, userId);

        return r2dbcEntityOperations.getDatabaseClient().sql(sql).bind("sharedWithUserId", userId).map((row, meta) -> {
            UserFileMetaWithDataDAO dao = new UserFileMetaWithDataDAO();

            dao.setUfmId(row.get("ufm_id", Long.class));
            dao.setUfmFilename(row.get("ufm_filename", String.class));
            dao.setUfmParentFolderId(row.get("ufm_parent_folder_id", Long.class));
            dao.setUfmIsStar(Boolean.TRUE.equals(row.get("ufm_is_star", Boolean.class)));

            dao.setUfmFileType(Optional.ofNullable(row.get("ufm_file_type", String.class)).map(FileEnum::valueOf).orElse(FileEnum.OTHER));
            dao.setUfmShareType(Optional
                                        .ofNullable(row.get("ufm_share_type", String.class))
                                        .map(FileShareTypeEnum::valueOf)
                                        .orElse(FileShareTypeEnum.DEFAULT));

            dao.setUfmUploadTime(row.get("ufm_upload_time", LocalDateTime.class));
            dao.setUfmLastAccessTime(row.get("ufm_last_access_time", LocalDateTime.class));
            dao.setUfmIsDeleted(Boolean.TRUE.equals(row.get("ufm_is_deleted", Boolean.class)));

            if (row.get("sfm_id", Long.class) != null) {
                dao.setSfmId(row.get("sfm_id", Long.class));
                dao.setSfmFileSize(row.get("sfm_file_size", Long.class));
                dao.setSfmMimeType(row.get("sfm_mime_type", String.class));
                dao.setSfmGridFsId(row.get("sfm_grid_fs_id", String.class));
                dao.setSfmMd5(row.get("sfm_md5", String.class));
            }

            if (dao.getUfmFileType() == FileEnum.ONLINE_DOCUMENT && row.get("uof_id", Long.class) != null) {
                dao.setUofId(row.get("uof_id", Long.class));
                dao.setSfmFileSize(row.get("uof_file_size", Long.class));
            }

            dao.setOwnerUsername(row.get("u_owner_name", String.class));
            dao.setUfmUserId(row.get("u_owner_id", Long.class));
            return dao;
        }).all();
    }

    /**
     * 根據檔案 ID 查詢檔案的分享類型。
     * <p>
     * 此方法用於快速獲取指定檔案的分享設定，用於權限驗證和存取控制。
     * 檔案分享類型決定了檔案的可見性和存取權限範圍，是檔案權限管理的重要屬性。
     * </p>
     *
     * @param fileId 檔案的唯一識別碼，不得為 null
     * @param r2dbcEntityOperations R2DBC 實體操作介面，提供底層資料庫存取能力
     * @return 包含檔案分享類型的 {@link Mono}，如果檔案不存在則為空
     * @see FileShareTypeEnum
     */
    default Mono<FileShareTypeEnum> getShareTypeByFileId(Long fileId, R2dbcEntityOperations r2dbcEntityOperations) {
        return r2dbcEntityOperations
                .select(UserFileMetadata.class)
                .matching(org.springframework.data.relational.core.query.Query.query(Criteria.where("id").is(fileId)))
                .one()
                .map(UserFileMetadata::getShareType);
    }


    /**
     * 根據用戶 ID 和過濾條件查詢優化的複合檔案元資料（包含檔案資料）。
     * <p>
     * 此方法是高效能的檔案查詢實現，針對不同的查詢場景進行了優化，
     * 包括根目錄瀏覽、星標檔案、回收站、最近檔案等特殊檢視。
     * 使用了資料庫索引優化和條件查詢來提升查詢效能。
     * </p>
     * <p>
     * 支援的特殊檢視：
     * <ul>
     *   <li>根目錄檔案檢視（使用索引優化）</li>
     *   <li>星標檔案檢視</li>
     *   <li>回收站檔案檢視</li>
     *   <li>最近存取檔案檢視（限制數量）</li>
     *   <li>分享檔案檢視（重新導向到分享查詢）</li>
     *   <li>所有檔案檢視</li>
     * </ul>
     * </p>
     *
     * @param userId 用戶 ID，指定要查詢的用戶
     * @param fileFilterDTO 檔案過濾條件，包含特殊檢視類型和其他過濾條件
     * @param r2dbcEntityOperations R2DBC 實體操作介面，提供底層資料庫存取能力
     * @return 包含完整檔案資訊的複合資料流 {@link Flux}，根據檢視類型排序
     * @see UserFileMetaWithDataDAO
     * @see FileFilterDTO  
     * @see ReservedSearchIdEnum
     */
    default Flux<UserFileMetaWithDataDAO> getUserFileMetaWithDataDAO(Long userId, FileFilterDTO fileFilterDTO, R2dbcEntityOperations r2dbcEntityOperations) {
        StringBuilder where = new StringBuilder();
        String useIndex = "";
        Map<String, Object> bind = new HashMap<>();

        ReservedSearchIdEnum searchIdEnum = ReservedSearchIdEnum.format(fileFilterDTO.getFolderId());
        if (searchIdEnum == null) {
            where.append(" WHERE ufm.parent_folder_id = :parentFolderId AND ufm.is_deleted = 0");
            bind.put("parentFolderId", fileFilterDTO.getFolderId());
        } else if (searchIdEnum == ReservedSearchIdEnum.SHARE_FILE_ID) {
            return findSharedUserFileMetaWithDataDAO(userId, r2dbcEntityOperations);
        } else {
            where.append(" WHERE ufm.user_id = :userId");
            bind.put("userId", userId);

            switch (searchIdEnum) {
                case ROOT_FOLDER_ID -> {
                    useIndex = "USE INDEX (idx_user_deleted_filetype_filename)";
                    where.append(" AND ufm.parent_folder_id IS NULL AND ufm.is_deleted = 0");
                }
                case STAR_FILE_ID -> {
                    where.append(" AND ufm.is_star = TRUE AND ufm.is_deleted = 0");
                }
                case RECYCLE_FILE_ID -> where.append(" AND ufm.is_deleted = 1");
                case ALL_FILE_ID -> {
                    if (!fileFilterDTO.isIncludeDeleted()) {
                        where.append(" AND ufm.is_deleted = 0");
                    }
                }
                default -> {
                }
            }
        }

        StringBuilder orderByBuilder = new StringBuilder();
        String limit = "";
        if (searchIdEnum == ReservedSearchIdEnum.RECENT_FILE_ID) {
            orderByBuilder.append(" ORDER BY ufm_last_access_time DESC");
            limit = " LIMIT " + fileFilterDTO.getPageSize();
        } else {
            orderByBuilder.append(" ORDER BY CASE ufm_file_type");
            Stream.of(FileEnum.values()).forEach(fileType -> {
                int order = UserFileListOrderEnum.getOrder(fileType);
                orderByBuilder.append(" WHEN '").append(fileType.name()).append("' THEN ").append(order);
            });
            orderByBuilder.append(" ELSE ").append(UserFileListOrderEnum.LAST.getOrder()).append(" END, ufm_filename");
        }

        String sql = """
                WITH base AS (
                    SELECT  ufm.id,
                            ufm.user_id,
                            ufm.server_file_id,
                            ufm.filename,
                            ufm.parent_folder_id,
                            ufm.is_star,
                            ufm.file_type,
                            ufm.share_type,
                            ufm.upload_time,
                            ufm.last_access_time,
                            ufm.is_deleted
                    FROM    user_file_metadata ufm
                """ + useIndex + where + limit + """
                )
                SELECT  b.id              AS ufm_id,
                        b.user_id         AS u_owner_id,
                        b.filename        AS ufm_filename,
                        b.parent_folder_id AS ufm_parent_folder_id,
                        b.is_star         AS ufm_is_star,
                        b.file_type       AS ufm_file_type,
                        b.share_type      AS ufm_share_type,
                        b.upload_time     AS ufm_upload_time,
                        b.last_access_time AS ufm_last_access_time,
                        b.is_deleted      AS ufm_is_deleted,
                        sfm.id            AS sfm_id,
                        sfm.file_size     AS sfm_file_size,
                        sfm.mime_type     AS sfm_mime_type,
                        sfm.grid_fs_id    AS sfm_grid_fs_id,
                        sfm.md5           AS sfm_md5,
                        uof.id            AS uof_id,
                        uof.file_size     AS uof_file_size,
                        u.username        AS u_owner_name
                FROM    base b
                LEFT JOIN server_file_metadata sfm ON sfm.id = b.server_file_id
                LEFT JOIN user_online_file     uof ON uof.id = b.id
                LEFT JOIN users                u   ON u.id = b.user_id
                """ + orderByBuilder;

        LogUnity.trace("查詢所使用的語句 SQL: [%s], 綁定參數: [%s]", sql, bind);

        DatabaseClient.GenericExecuteSpec spec = r2dbcEntityOperations.getDatabaseClient().sql(sql);
        for (var e : bind.entrySet()) {
            spec = spec.bind(e.getKey(), e.getValue());
        }

        return spec.map((row, meta) -> {
            UserFileMetaWithDataDAO dao = new UserFileMetaWithDataDAO();

            dao.setUfmId(row.get("ufm_id", Long.class));
            dao.setUfmFilename(row.get("ufm_filename", String.class));
            dao.setUfmParentFolderId(row.get("ufm_parent_folder_id", Long.class));
            dao.setUfmIsStar(Boolean.TRUE.equals(row.get("ufm_is_star", Boolean.class)));

            dao.setUfmFileType(Optional.ofNullable(row.get("ufm_file_type", String.class)).map(FileEnum::valueOf).orElse(FileEnum.OTHER));
            dao.setUfmShareType(Optional
                                        .ofNullable(row.get("ufm_share_type", String.class))
                                        .map(FileShareTypeEnum::valueOf)
                                        .orElse(FileShareTypeEnum.DEFAULT));

            dao.setUfmUploadTime(row.get("ufm_upload_time", LocalDateTime.class));
            dao.setUfmLastAccessTime(row.get("ufm_last_access_time", LocalDateTime.class));
            dao.setUfmIsDeleted(Boolean.TRUE.equals(row.get("ufm_is_deleted", Boolean.class)));

            if (row.get("sfm_id", Long.class) != null) {
                dao.setSfmId(row.get("sfm_id", Long.class));
                dao.setSfmFileSize(row.get("sfm_file_size", Long.class));
                dao.setSfmMimeType(row.get("sfm_mime_type", String.class));
                dao.setSfmGridFsId(row.get("sfm_grid_fs_id", String.class));
                dao.setSfmMd5(row.get("sfm_md5", String.class));
            }

            if (dao.getUfmFileType() == FileEnum.ONLINE_DOCUMENT && row.get("uof_id", Long.class) != null) {
                dao.setUofId(row.get("uof_id", Long.class));
                dao.setSfmFileSize(row.get("uof_file_size", Long.class));
            }

            dao.setOwnerUsername(row.get("u_owner_name", String.class));
            dao.setUfmUserId(row.get("u_owner_id", Long.class));
            return dao;
        }).all();
    }
}
