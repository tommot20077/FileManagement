package xyz.dowob.filemanagement.grpc.controller.base;

import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.grpc.*;
import xyz.dowob.filemanagement.service.grpc.JwtCacheService;
import xyz.dowob.filemanagement.service.grpc.UserContextCacheService;
import xyz.dowob.filemanagement.service.serviceInterface.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC 資料夾控制器基礎類。
 *
 * <p>提供資料夾相關的 gRPC 服務基礎實現，包括：
 * <ul>
 *   <li>創建資料夾（包含驗證）</li>
 *   <li>列出資料夾內容</li>
 *   <li>刪除資料夾</li>
 *   <li>資料夾權限管理</li>
 * </ul>
 *
 * <p>特點：
 * <ul>
 *   <li>整合 ValidationService 進行輸入驗證</li>
 *   <li>支援階層式資料夾結構</li>
 *   <li>提供分頁查詢功能</li>
 *   <li>統一的錯誤處理</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
public class BaseGrpcFolderController extends BaseGrpcController {

    /**
     * 檔案服務策略管理器。
     */
    private final FileServiceStrategy fileServiceStrategy;


    /**
     * 建構函數。
     *
     * @param validationService        驗證服務
     * @param userService              用戶服務
     * @param tokenService             令牌服務
     * @param permissionService        權限服務
     * @param jwtCacheService          JWT 快取服務
     * @param userContextCacheService  用戶上下文快取服務
     * @param fileServiceStrategy      檔案服務策略
     */
    public BaseGrpcFolderController(ValidationService validationService, 
                                    UserService userService, 
                                    TokenService tokenService, 
                                    PermissionService<UserFileMetadata> permissionService,
                                    JwtCacheService jwtCacheService,
                                    UserContextCacheService userContextCacheService,
                                    FileServiceStrategy fileServiceStrategy) {
        super(validationService, userService, tokenService, permissionService, jwtCacheService, userContextCacheService);
        this.fileServiceStrategy = fileServiceStrategy;
    }


    /**
     * 創建資料夾。
     *
     * <p>此方法包含完整的驗證流程：
     * <ol>
     *   <li>驗證 JWT 令牌</li>
     *   <li>驗證資料夾名稱和路徑</li>
     *   <li>檢查父資料夾權限</li>
     *   <li>創建新資料夾</li>
     * </ol>
     *
     * @param request          創建資料夾請求
     * @param responseObserver 響應觀察者
     */
    public void createFolder(CreateFolderRequest request, StreamObserver<CreateFolderResponse> responseObserver) {
        Long parentId = request.getParentId() == 0 ? null : request.getParentId();
        FileEditDTO fileEditDTO = FileEditDTO.builder().filename(request.getFolderName()).parentFolderId(parentId).build();

        Mono<CreateFolderResponse> createMono = validateAndGetUser(request.getAuth()).flatMap(user -> {
            // 驗證資料夾編輯 DTO
            return validationService.validateEditFileDTO(fileEditDTO, true).then(Mono.defer(() -> {
                FolderService folderService = (FolderService) fileServiceStrategy.getFileService(FileEnum.FOLDER);
                return folderService
                        .createFolder(fileEditDTO, user)
                        .map(folder -> CreateFolderResponse
                                .newBuilder()
                                .setSuccess(true)
                                .setFolderId(folder.getId())
                                .setMessage("資料夾創建成功")
                                .build());
            }));
        });

        subscribeWithGrpcHandler(createMono, responseObserver);
    }


    /**
     * 列出資料夾內容。
     *
     * <p>獲取指定資料夾下的所有檔案和子資料夾。
     * 如果資料夾 ID 為 0，則返回根目錄內容。
     *
     * @param request          列出資料夾請求
     * @param responseObserver 響應觀察者
     */
    public void listFolder(ListFolderRequest request, StreamObserver<ListFolderResponse> responseObserver) {
        Mono<ListFolderResponse> listMono = validateAndGetUser(request.getAuth()).flatMap(user -> {
            // 構建過濾條件
            // ListFolderRequest 沒有 page 和 pageSize 屬性，使用預設值
            FileFilterDTO filterDTO = FileFilterDTO
                    .builder()
                    .folderId(request.getFolderId() == 0 ? null : request.getFolderId())
                    .page(1)
                    .pageSize(Integer.MAX_VALUE)
                    .includeDeleted(false)
                    .build();

            // 獲取檔案列表
            FileService fileService = fileServiceStrategy.getFileService(FileEnum.FOLDER);
            return fileService.getUserFileList(user, filterDTO).map(pagedResponse -> (List<UserFileListDTO>) pagedResponse.getData());
        }).map(files -> {
            // 轉換為 gRPC 格式
            List<FileInfo> fileInfos = files.stream().map(this::convertToFileInfo).collect(Collectors.toList());

            return ListFolderResponse.newBuilder().setSuccess(true).addAllFiles(fileInfos).setTotalCount(fileInfos.size()).build();
        });

        subscribeWithGrpcHandler(listMono, responseObserver);
    }


    /**
     * 將檔案列表 DTO 轉換為 gRPC FileInfo。
     */
    private FileInfo convertToFileInfo(UserFileListDTO dto) {
        return FileInfo
                .newBuilder()
                .setId(dto.getId())
                .setName(dto.getFilename())
                .setSize(dto.getFileSize() != null ? dto.getFileSize() : 0L)
                .setIsDirectory(dto.getFileType() == FileEnum.FOLDER)
                .setParentId(dto.getParentFolderId() != null ? dto.getParentFolderId() : 0L)
                .setCreatedTime(dto.getCreateTime() != null ? dto.getCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli() : 0L)
                .setModifiedTime(dto.getLastAccessTime() != null ? dto.getLastAccessTime().toInstant(ZoneOffset.UTC).toEpochMilli() : 0L)
                .setMimeType(dto.getMimeType() != null ? dto.getMimeType() : "")
                .setOwner(dto.getOwnerUsername() != null ? dto.getOwnerUsername() : "")
                .build();
    }


    /**
     * 移動或重命名檔案/資料夾。
     *
     * <p>此方法支援：
     * <ul>
     *   <li>移動到新的父資料夾</li>
     *   <li>重命名檔案或資料夾</li>
     *   <li>同時移動和重命名</li>
     * </ul>
     *
     * @param request          移動檔案請求
     * @param responseObserver 響應觀察者
     */
    public void moveFile(MoveFileRequest request, StreamObserver<MoveFileResponse> responseObserver) {
        Mono<MoveFileResponse> moveMono = validateAndGetUser(request.getAuth())
                .flatMap(user -> permissionService.validateUserPermission(user, request.getFileId()))
                .flatMap(file -> {
                    // 構建編輯 DTO
                    FileEditDTO editDTO = FileEditDTO
                            .builder()
                            .fileId(String.valueOf(request.getFileId()))
                            .parentFolderId(request.getNewParentId())
                            .filename(!request.getNewName().isEmpty() ? request.getNewName() : null)
                            .build();

                    // 驗證編輯操作
                    boolean isFolder = file.getFileType() == FileEnum.FOLDER;
                    return validationService.validateEditFileDTO(editDTO, isFolder).then(Mono.defer(() -> {
                        FileService fileService = fileServiceStrategy.getFileService(file.getFileType());
                        User user = new User();
                        user.setId(request.getAuth().getUserId() != 0 ? request.getAuth().getUserId() : null);

                        // 創建 FileEditBO 物件
                        xyz.dowob.filemanagement.data.file.bo.FileEditBO fileEditBO = new xyz.dowob.filemanagement.data.file.bo.FileEditBO(editDTO);
                        fileEditBO.setUserFileMetadata(file);

                        return fileService
                                .editFile(fileEditBO, user)
                                .then(Mono.fromCallable(() -> MoveFileResponse
                                        .newBuilder()
                                        .setSuccess(true)
                                        .setNewFileId(file.getId())
                                        .setMessage("移動成功")
                                        .build()));
                    }));
                });

        subscribeWithGrpcHandler(moveMono, responseObserver);
    }


    /**
     * 複製檔案或資料夾。
     *
     * <p>創建檔案或資料夾的副本，包括：
     * <ul>
     *   <li>複製檔案內容</li>
     *   <li>複製元資料</li>
     *   <li>可選的重命名</li>
     * </ul>
     *
     * @param request          複製檔案請求
     * @param responseObserver 響應觀察者
     */
    public void copyFile(CopyFileRequest request, StreamObserver<CopyFileResponse> responseObserver) {
        Mono<CopyFileResponse> copyMono = validateAndGetUser(request.getAuth())
                .flatMap(user -> permissionService.validateUserPermission(user, request.getFileId()))
                .flatMap(file -> {
                    FileEditDTO editDTO = FileEditDTO
                            .builder()
                            .fileId(String.valueOf(request.getFileId()))
                            .parentFolderId(request.getTargetParentId())
                            .filename(!request.getNewName().isEmpty() ? request.getNewName() : file.getFilename())
                            .build();

                    // 驗證新檔案名稱
                    boolean isFolder = file.getFileType() == FileEnum.FOLDER;
                    return validationService.validateEditFileDTO(editDTO, isFolder).then(Mono.defer(() -> {
                        FileService fileService = fileServiceStrategy.getFileService(file.getFileType());
                        User user = new User();
                        user.setId(request.getAuth().getUserId() != 0 ? request.getAuth().getUserId() : null);

                        // 注意：這裡需要實現複製邏輯，可能需要擴展 FileService
                        return fileService
                                .copyFile(file, editDTO.getParentFolderId(), editDTO.getFilename(), user)
                                .map(newFile -> CopyFileResponse
                                        .newBuilder()
                                        .setSuccess(true)
                                        .setNewFileId(newFile.getId())
                                        .setMessage("複製成功")
                                        .build());
                    }));
                });

        subscribeWithGrpcHandler(copyMono, responseObserver);
    }
}