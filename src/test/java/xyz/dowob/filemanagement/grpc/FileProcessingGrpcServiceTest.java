package xyz.dowob.filemanagement.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.dto.UserInfoDto;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.JwtAuthenticationException;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.grpc.service.FileProcessingGrpcService;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 測試 FileProcessingGrpcService
 * 重構後移除了所有 WebDAV 相關邏輯
 */
@ExtendWith(MockitoExtension.class)
public class FileProcessingGrpcServiceTest {

    @Mock
    private FileServiceStrategy fileServiceStrategy;

    @Mock
    private FileService fileService;

    @Mock
    private UserService userService;

    @Mock
    private PermissionService<UserFileMetadata> permissionService;

    @Mock
    private TokenService tokenService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StreamObserver<xyz.dowob.filemanagement.grpc.FileContentResponse> fileContentResponseObserver;

    @Mock
    private StreamObserver<xyz.dowob.filemanagement.grpc.FileMetadataResponse> fileMetadataResponseObserver;

    @Mock
    private StreamObserver<xyz.dowob.filemanagement.grpc.DeleteFileResponse> deleteFileResponseObserver;

    @Mock
    private StreamObserver<xyz.dowob.filemanagement.grpc.AuthenticationResponse> authResponseObserver;

    private FileProcessingGrpcService grpcService;


    @BeforeEach
    void setUp() {
        grpcService = new FileProcessingGrpcService(fileServiceStrategy, userService, permissionService, tokenService, objectMapper);
    }


    @Test
    void testGetFileContent_Success() {
        // Given
        Long fileId = 123L;
        Long userId = 456L;
        String token = "test-token";

        xyz.dowob.filemanagement.grpc.GetFileRequest request = xyz.dowob.filemanagement.grpc.GetFileRequest
                .newBuilder()
                .setFileId(fileId)
                .setUserId(userId)
                .setUserToken(token)
                .build();

        UserFileMetadata file = new UserFileMetadata();
        file.setId(fileId);
        file.setFilename("test.txt");
        file.setFileType(FileEnum.OTHER);

        UserFileDataBO fileData = new UserFileDataBO();
        fileData.setDataBufferByte(Mono.just("test content".getBytes()));

        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(userId));
        when(permissionService.validateUserPermission(any(User.class), eq(fileId))).thenReturn(Mono.just(file));
        when(fileServiceStrategy.getFileService(FileEnum.OTHER)).thenReturn(fileService);
        when(fileService.downloadFile(eq(file), any(User.class))).thenReturn(Mono.just(fileData));

        // When
        grpcService.getFileContent(request, fileContentResponseObserver);

        // Then
        ArgumentCaptor<xyz.dowob.filemanagement.grpc.FileContentResponse> responseCaptor = ArgumentCaptor.forClass(xyz.dowob.filemanagement.grpc.FileContentResponse.class);
        verify(fileContentResponseObserver, timeout(1000).atLeastOnce()).onNext(responseCaptor.capture());
        verify(fileContentResponseObserver, timeout(1000)).onCompleted();

        xyz.dowob.filemanagement.grpc.FileContentResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertTrue(response.getErrorMessage().isEmpty());
    }


    @Test
    void testGetFileMetadata_Success() {
        // Given
        Long fileId = 123L;
        Long userId = 456L;
        String token = "test-token";

        xyz.dowob.filemanagement.grpc.GetFileMetadataRequest request = xyz.dowob.filemanagement.grpc.GetFileMetadataRequest
                .newBuilder()
                .setFileId(fileId)
                .setUserId(userId)
                .setUserToken(token)
                .build();

        UserFileMetadata file = new UserFileMetadata();
        file.setId(fileId);
        file.setFilename("test.txt");
        file.setFileType(FileEnum.OTHER);
        file.setUploadTime(java.time.LocalDateTime.now());
        file.setLastAccessTime(java.time.LocalDateTime.now());

        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(userId));
        when(permissionService.validateUserPermission(any(User.class), eq(fileId))).thenReturn(Mono.just(file));

        // When
        grpcService.getFileMetadata(request, fileMetadataResponseObserver);

        // Then
        ArgumentCaptor<xyz.dowob.filemanagement.grpc.FileMetadataResponse> responseCaptor = ArgumentCaptor.forClass(xyz.dowob.filemanagement.grpc.FileMetadataResponse.class);
        verify(fileMetadataResponseObserver, timeout(1000)).onNext(responseCaptor.capture());
        verify(fileMetadataResponseObserver, timeout(1000)).onCompleted();

        xyz.dowob.filemanagement.grpc.FileMetadataResponse response = responseCaptor.getValue();
        assertTrue(response.getSuccess());
        assertEquals(fileId, response.getFileInfo().getId());
        assertEquals("test.txt", response.getFileInfo().getName());
    }


    @Test
    void testDeleteFile_Success() {
        // Given
        Long fileId = 123L;
        Long userId = 456L;
        String token = "test-token";

        xyz.dowob.filemanagement.grpc.DeleteFileRequest request = xyz.dowob.filemanagement.grpc.DeleteFileRequest
                .newBuilder()
                .setFileId(fileId)
                .setUserId(userId)
                .setUserToken(token)
                .build();

        UserFileMetadata file = new UserFileMetadata();
        file.setId(fileId);
        file.setUserId(userId);
        file.setFileType(FileEnum.OTHER);
        file.setUploadTime(java.time.LocalDateTime.now());
        file.setLastAccessTime(java.time.LocalDateTime.now());

        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(userId));
        when(permissionService.validateUserPermission(any(User.class), eq(fileId))).thenReturn(Mono.just(file));
        when(fileServiceStrategy.getFileService(FileEnum.OTHER)).thenReturn(fileService);
        when(fileService.removeFile(eq(file), any(User.class))).thenReturn(Mono.just(true));

        // When
        grpcService.deleteFile(request, deleteFileResponseObserver);

        // Then
        ArgumentCaptor<xyz.dowob.filemanagement.grpc.DeleteFileResponse> responseCaptor = ArgumentCaptor.forClass(xyz.dowob.filemanagement.grpc.DeleteFileResponse.class);
        verify(deleteFileResponseObserver, timeout(1000)).onNext(responseCaptor.capture());
        verify(deleteFileResponseObserver, timeout(1000)).onCompleted();

        xyz.dowob.filemanagement.grpc.DeleteFileResponse response = responseCaptor.getValue();
        assertTrue(response.getSuccess());
        assertEquals("已移動到回收站", response.getMessage());
    }


    @Test
    void testAuthenticateUser_Success() {
        // Given
        String username = "testuser";
        String password = "testpass";
        String jwtToken = "jwt-token-12345";
        Long expectedUserId = 123L;
        String expectedRole = "USER";

        UserInfoDto userInfo = UserInfoDto.builder().userId(expectedUserId).username(username).role("USER").build();

        xyz.dowob.filemanagement.grpc.AuthenticationRequest request = xyz.dowob.filemanagement.grpc.AuthenticationRequest
                .newBuilder()
                .setUsername(username)
                .setPassword(password)
                .build();

        when(userService.login(any(AuthRequestDTO.class), isNull())).thenReturn(Mono.just(jwtToken));
        when(tokenService.extractUserInfoFromToken(jwtToken, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(userInfo));

        // When
        grpcService.authenticateUser(request, authResponseObserver);

        // Then
        ArgumentCaptor<xyz.dowob.filemanagement.grpc.AuthenticationResponse> responseCaptor = ArgumentCaptor.forClass(xyz.dowob.filemanagement.grpc.AuthenticationResponse.class);
        verify(authResponseObserver, timeout(1000)).onNext(responseCaptor.capture());
        verify(authResponseObserver, timeout(1000)).onCompleted();

        xyz.dowob.filemanagement.grpc.AuthenticationResponse response = responseCaptor.getValue();
        assertTrue(response.getSuccess());
        assertEquals(jwtToken, response.getJwtToken());
        assertEquals(expectedUserId, response.getUserId());

        // Verify method calls
        verify(userService).login(any(AuthRequestDTO.class), isNull());
        verify(tokenService).extractUserInfoFromToken(jwtToken, TokenEnum.JWT_AUTHORIZATION_TOKEN);
    }


    @Test
    void testGetFileContent_PermissionDenied() {
        // Given
        Long fileId = 123L;
        Long userId = 456L;
        String token = "test-token";

        xyz.dowob.filemanagement.grpc.GetFileRequest request = xyz.dowob.filemanagement.grpc.GetFileRequest
                .newBuilder()
                .setFileId(fileId)
                .setUserId(userId)
                .setUserToken(token)
                .build();

        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(userId));
        when(permissionService.validateUserPermission(any(User.class), eq(fileId))).thenReturn(Mono.error(new RuntimeException("Permission denied")));

        // When
        grpcService.getFileContent(request, fileContentResponseObserver);

        // Then - 驗證 onError 被調用而不是 onNext
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(fileContentResponseObserver, timeout(1000)).onError(errorCaptor.capture());
        
        // 驗證錯誤訊息
        Throwable capturedError = errorCaptor.getValue();
        assertTrue(capturedError instanceof io.grpc.StatusRuntimeException || 
                   capturedError instanceof io.grpc.StatusException);
        assertTrue(capturedError.getMessage().contains("INTERNAL"));
        
        // 驗證 onNext 和 onCompleted 沒有被調用
        verify(fileContentResponseObserver, never()).onNext(any());
        verify(fileContentResponseObserver, never()).onCompleted();
    }

    // ==================== 錯誤處理測試 ====================

    @Test
    void testGetFileContent_ValidationError() {
        // Given
        Long fileId = 123L;
        Long userId = 456L;
        String token = "test-token";
        
        xyz.dowob.filemanagement.grpc.GetFileRequest request = xyz.dowob.filemanagement.grpc.GetFileRequest
                .newBuilder()
                .setFileId(fileId)
                .setUserId(userId)
                .setUserToken(token)
                .build();
        
        ValidationException exception = new ValidationException(ValidationException.ErrorCode.FILE_TYPE_WITH_WRONG_REQUEST_PATH, "GENERAL", "ONLINE");
        
        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.error(exception));
        
        // When
        grpcService.getFileContent(request, fileContentResponseObserver);
        
        // Then
        verify(fileContentResponseObserver, timeout(1000)).onError(any(StatusRuntimeException.class));
        verify(fileContentResponseObserver, never()).onNext(any());
        verify(fileContentResponseObserver, never()).onCompleted();
    }

    @Test
    void testGetFileMetadata_AuthenticationError() {
        // Given
        Long fileId = 123L;
        Long userId = 456L;
        String token = "invalid-token";
        
        xyz.dowob.filemanagement.grpc.GetFileMetadataRequest request = xyz.dowob.filemanagement.grpc.GetFileMetadataRequest
                .newBuilder()
                .setFileId(fileId)
                .setUserId(userId)
                .setUserToken(token)
                .build();
        
        JwtAuthenticationException exception = new JwtAuthenticationException("令牌已過期");
        
        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.error(exception));
        
        // When
        grpcService.getFileMetadata(request, fileMetadataResponseObserver);
        
        // Then
        ArgumentCaptor<StatusRuntimeException> errorCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(fileMetadataResponseObserver, timeout(1000)).onError(errorCaptor.capture());
        
        StatusRuntimeException capturedError = errorCaptor.getValue();
        assertEquals(io.grpc.Status.UNAUTHENTICATED.getCode(), capturedError.getStatus().getCode());
        assertEquals("認證失敗", capturedError.getStatus().getDescription());
        
        verify(fileMetadataResponseObserver, never()).onNext(any());
        verify(fileMetadataResponseObserver, never()).onCompleted();
    }

    @Test
    void testDeleteFile_LimitationError() {
        // Given
        Long fileId = 123L;
        Long userId = 456L;
        String token = "test-token";
        
        xyz.dowob.filemanagement.grpc.DeleteFileRequest request = xyz.dowob.filemanagement.grpc.DeleteFileRequest
                .newBuilder()
                .setFileId(fileId)
                .setUserId(userId)
                .setUserToken(token)
                .build();
        
        UserFileMetadata file = new UserFileMetadata();
        file.setId(fileId);
        file.setFileType(FileEnum.OTHER);
        
        LimitationException exception = new LimitationException(
            LimitationException.ErrorCode.USER_EXCEED_LIMIT, "請求頻率超限"
        );
        
        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.just(userId));
        when(permissionService.validateUserPermission(any(User.class), eq(fileId)))
            .thenReturn(Mono.just(file));
        when(fileServiceStrategy.getFileService(FileEnum.OTHER)).thenReturn(fileService);
        when(fileService.removeFile(eq(file), any(User.class)))
            .thenReturn(Mono.error(exception));
        
        // When
        grpcService.deleteFile(request, deleteFileResponseObserver);
        
        // Then
        ArgumentCaptor<StatusRuntimeException> errorCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(deleteFileResponseObserver, timeout(1000)).onError(errorCaptor.capture());
        
        StatusRuntimeException capturedError = errorCaptor.getValue();
        assertEquals(io.grpc.Status.RESOURCE_EXHAUSTED.getCode(), capturedError.getStatus().getCode());
        assertTrue(capturedError.getStatus().getDescription().contains("資源限制"));
        
        verify(deleteFileResponseObserver, never()).onNext(any());
        verify(deleteFileResponseObserver, never()).onCompleted();
    }

    @Test
    void testAuthenticateUser_InvalidCredentials() {
        // Given
        String username = "testuser";
        String password = "wrongpass";
        
        xyz.dowob.filemanagement.grpc.AuthenticationRequest request = xyz.dowob.filemanagement.grpc.AuthenticationRequest
                .newBuilder()
                .setUsername(username)
                .setPassword(password)
                .build();
        
        JwtAuthenticationException exception = new JwtAuthenticationException("無效的憑證");
        
        when(userService.login(any(AuthRequestDTO.class), isNull()))
            .thenReturn(Mono.error(exception));
        
        // When
        grpcService.authenticateUser(request, authResponseObserver);
        
        // Then
        ArgumentCaptor<StatusRuntimeException> errorCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(authResponseObserver, timeout(1000)).onError(errorCaptor.capture());
        
        StatusRuntimeException capturedError = errorCaptor.getValue();
        assertEquals(io.grpc.Status.UNAUTHENTICATED.getCode(), capturedError.getStatus().getCode());
        assertEquals("認證失敗", capturedError.getStatus().getDescription());
        
        verify(authResponseObserver, never()).onNext(any());
        verify(authResponseObserver, never()).onCompleted();
    }

    @Test
    void testUploadFileSimple_ProcessError() {
        // Given
        Long userId = 123L;
        String token = "test-token";
        String filename = "test.txt";
        
        xyz.dowob.filemanagement.grpc.UploadFileRequest request = xyz.dowob.filemanagement.grpc.UploadFileRequest
                .newBuilder()
                .setUserId(userId)
                .setUserToken(token)
                .setFilename(filename)
                .setParentFolderId(0L)
                .setMd5("test-md5")
                .setContent(com.google.protobuf.ByteString.copyFromUtf8("test content"))
                .setMimeType("text/plain")
                .build();
        
        ProcessException exception = new ProcessException(ProcessException.ErrorCode.CREATE_STREAM_FAILED);
        
        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.error(exception));
        
        // When
        StreamObserver<xyz.dowob.filemanagement.grpc.UploadFileResponse> uploadResponseObserver = 
            mock(StreamObserver.class);
        grpcService.uploadFileSimple(request, uploadResponseObserver);
        
        // Then
        ArgumentCaptor<StatusRuntimeException> errorCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(uploadResponseObserver, timeout(1000)).onError(errorCaptor.capture());
        
        StatusRuntimeException capturedError = errorCaptor.getValue();
        assertEquals(io.grpc.Status.INTERNAL.getCode(), capturedError.getStatus().getCode());
        assertTrue(capturedError.getStatus().getDescription().contains("處理錯誤"));
        
        verify(uploadResponseObserver, never()).onNext(any());
        verify(uploadResponseObserver, never()).onCompleted();
    }

    @Test
    void testListFolder_UnknownError() {
        // Given
        Long userId = 123L;
        String token = "test-token";
        Long folderId = 456L;
        
        xyz.dowob.filemanagement.grpc.ListFolderRequest request = xyz.dowob.filemanagement.grpc.ListFolderRequest
                .newBuilder()
                .setUserId(userId)
                .setUserToken(token)
                .setFolderId(folderId)
                .build();
        
        RuntimeException exception = new RuntimeException("未知錯誤");
        
        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.error(exception));
        
        // When
        StreamObserver<xyz.dowob.filemanagement.grpc.ListFolderResponse> listFolderResponseObserver = 
            mock(StreamObserver.class);
        grpcService.listFolder(request, listFolderResponseObserver);
        
        // Then
        ArgumentCaptor<StatusRuntimeException> errorCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(listFolderResponseObserver, timeout(1000)).onError(errorCaptor.capture());
        
        StatusRuntimeException capturedError = errorCaptor.getValue();
        assertEquals(io.grpc.Status.INTERNAL.getCode(), capturedError.getStatus().getCode());
        assertEquals("內部服務錯誤", capturedError.getStatus().getDescription());
        
        verify(listFolderResponseObserver, never()).onNext(any());
        verify(listFolderResponseObserver, never()).onCompleted();
    }

    @Test
    void testCreateFolder_ValidationError() {
        // Given
        Long userId = 123L;
        String token = "test-token";
        String folderName = "";  // Invalid folder name
        Long parentId = 0L;
        
        xyz.dowob.filemanagement.grpc.CreateFolderRequest request = xyz.dowob.filemanagement.grpc.CreateFolderRequest
                .newBuilder()
                .setUserId(userId)
                .setUserToken(token)
                .setFolderName(folderName)
                .setParentId(parentId)
                .build();
        
        ValidationException exception = new ValidationException(
            ValidationException.ErrorCode.INVALID_FILE_NAME
        );
        
        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.error(exception));
        
        // When
        StreamObserver<xyz.dowob.filemanagement.grpc.CreateFolderResponse> createFolderResponseObserver = 
            mock(StreamObserver.class);
        grpcService.createFolder(request, createFolderResponseObserver);
        
        // Then
        ArgumentCaptor<StatusRuntimeException> errorCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(createFolderResponseObserver, timeout(1000)).onError(errorCaptor.capture());
        
        StatusRuntimeException capturedError = errorCaptor.getValue();
        assertEquals(io.grpc.Status.INVALID_ARGUMENT.getCode(), capturedError.getStatus().getCode());
        assertTrue(capturedError.getStatus().getDescription().contains("驗證失敗"));
        
        verify(createFolderResponseObserver, never()).onNext(any());
        verify(createFolderResponseObserver, never()).onCompleted();
    }

    @Test
    void testUploadFile_StreamError() {
        // Given - 使用 uploadFile 流式上傳方法
        StreamObserver<xyz.dowob.filemanagement.grpc.UploadProgress> uploadProgressObserver = 
            mock(StreamObserver.class);
        
        // When - 創建上傳流
        StreamObserver<xyz.dowob.filemanagement.grpc.FileUploadChunk> uploadStream = 
            grpcService.uploadFile(uploadProgressObserver);
        
        // 發送元數據
        xyz.dowob.filemanagement.grpc.FileUploadMetadata metadata = 
            xyz.dowob.filemanagement.grpc.FileUploadMetadata.newBuilder()
                .setUserId(123L)
                .setUserToken("invalid-token")
                .setFilename("test.txt")
                .setParentFolderId(0L)
                .setTotalSize(100L)
                .setMimeType("text/plain")
                .build();
        
        xyz.dowob.filemanagement.grpc.FileUploadChunk metadataChunk = 
            xyz.dowob.filemanagement.grpc.FileUploadChunk.newBuilder()
                .setMetadata(metadata)
                .build();
        
        uploadStream.onNext(metadataChunk);
        
        // 發送錯誤
        uploadStream.onError(new RuntimeException("網絡錯誤"));
        
        // Then - 驗證 onError 被調用
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(uploadProgressObserver, timeout(1000)).onError(errorCaptor.capture());
        
        // 驗證錯誤訊息
        Throwable capturedError = errorCaptor.getValue();
        assertTrue(capturedError instanceof io.grpc.StatusRuntimeException || 
                   capturedError instanceof io.grpc.StatusException);
        assertTrue(capturedError.getMessage().contains("INTERNAL"));
        
        // 驗證 onCompleted 沒有被調用
        verify(uploadProgressObserver, never()).onCompleted();
    }
}