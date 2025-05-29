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
     * 根據用戶ID和父文件夾ID查詢檔案元數據，此方法可以蒐尋多個父文件夾ID並返回所有符合條件的檔案元數據
     *
     * @param parentFolderId   父文件夾ID
     * @param entityOperations R2dbc實體操作
     *
     * @return Flux<UserFileMetadata> 返回所有符合條件的檔案元數據
     */
    default Flux<UserFileMetadata> findAllByParentFolderIdIn(List<Long> parentFolderId, R2dbcEntityOperations entityOperations) {
        Criteria criteria = Criteria.where("parent_folder_id").in(parentFolderId).and("is_deleted").is(false);
        return entityOperations.select(UserFileMetadata.class).matching(org.springframework.data.relational.core.query.Query.query(criteria)).all();
    }


    /**
     * 查詢指定的父文件夾ID下的所有檔案元數據，並根據用戶ID和共享類型進行過濾
     * 最終僅返回用戶有權限訪問的檔案元數據
     *
     * @param parentFolderId   父文件夾ID
     * @param user             用戶
     * @param entityOperations R2dbc實體操作
     *
     * @return Flux<UserFileMetadata> 返回所有符合條件的檔案元數據
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
     * 計算該用戶擁有同一伺服器檔案的檔案數量
     *
     * @param userId           用戶ID
     * @param serverFileIds    伺服器檔案ID
     * @param entityOperations R2dbc實體操作
     *
     * @return Flux<ServerFileMetaCountDAO> 返回所有檔案計數紀錄 {@link ServerFileMetaCountDAO}
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
     * 根據用戶ID和過濾條件查詢複合檔案元數據，並包含檔案數據
     *
     * @param userId                用戶ID
     * @param fileFilterDTO         過濾條件
     * @param r2dbcEntityOperations R2dbc實體操作
     *
     * @return Flux<UserFileMetaWithDataDAO> 複合檔案元數據流，包含檔案數據
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
     * 根據用戶ID查詢所有共享給該用戶的複合檔案元數據，並包含檔案數據
     *
     * @param userId                用戶ID
     * @param r2dbcEntityOperations R2dbc實體操作
     *
     * @return Flux<UserFileMetaWithDataDAO> 複合檔案元數據流，包含檔案數據
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
     * 根據檔案ID查詢檔案共享類型
     *
     * @param fileId                檔案ID
     * @param r2dbcEntityOperations R2dbc實體操作
     *
     * @return Mono<FileShareTypeEnum> 返回檔案共享類型
     */
    default Mono<FileShareTypeEnum> getShareTypeByFileId(Long fileId, R2dbcEntityOperations r2dbcEntityOperations) {
        return r2dbcEntityOperations
                .select(UserFileMetadata.class)
                .matching(org.springframework.data.relational.core.query.Query.query(Criteria.where("id").is(fileId)))
                .one()
                .map(UserFileMetadata::getShareType);
    }


    /**
     * 根據用戶ID和過濾條件查詢複合檔案元數據，並包含檔案數據
     *
     * @param userId                用戶ID
     * @param fileFilterDTO         過濾條件
     * @param r2dbcEntityOperations R2dbc實體操作
     *
     * @return Flux<UserFileMetaWithDataDAO> 複合檔案元數據流，包含檔案數據
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
