package xyz.dowob.filemanagement.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
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
    private StreamObserver<FileContentResponse> fileContentResponseObserver;
    
    @Mock
    private StreamObserver<FileMetadataResponse> fileMetadataResponseObserver;
    
    @Mock
    private StreamObserver<DeleteFileResponse> deleteFileResponseObserver;
    
    @Mock
    private StreamObserver<AuthenticationResponse> authResponseObserver;
    
    private FileProcessingGrpcService grpcService;
    
    @BeforeEach
    void setUp() {
        grpcService = new FileProcessingGrpcService(
            fileServiceStrategy, 
            userService, 
            permissionService, 
            tokenService,
            objectMapper
        );
    }
    
    @Test
    void testGetFileContent_Success() {
        // Given
        Long fileId = 123L;
        Long userId = 456L;
        String token = "test-token";
        
        GetFileRequest request = GetFileRequest.newBuilder()
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
        
        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.just(userId));
        when(permissionService.validateUserPermission(any(User.class), eq(fileId)))
            .thenReturn(Mono.just(file));
        when(fileServiceStrategy.getFileService(FileEnum.OTHER))
            .thenReturn(fileService);
        when(fileService.downloadFile(eq(file), any(User.class)))
            .thenReturn(Mono.just(fileData));
        
        // When
        grpcService.getFileContent(request, fileContentResponseObserver);
        
        // Then
        ArgumentCaptor<FileContentResponse> responseCaptor = ArgumentCaptor.forClass(FileContentResponse.class);
        verify(fileContentResponseObserver, timeout(1000).atLeastOnce()).onNext(responseCaptor.capture());
        verify(fileContentResponseObserver, timeout(1000)).onCompleted();
        
        FileContentResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertTrue(response.getErrorMessage().isEmpty());
    }
    
    @Test
    void testGetFileMetadata_Success() {
        // Given
        Long fileId = 123L;
        Long userId = 456L;
        String token = "test-token";
        
        GetFileMetadataRequest request = GetFileMetadataRequest.newBuilder()
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
        
        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.just(userId));
        when(permissionService.validateUserPermission(any(User.class), eq(fileId)))
            .thenReturn(Mono.just(file));
        
        // When
        grpcService.getFileMetadata(request, fileMetadataResponseObserver);
        
        // Then
        ArgumentCaptor<FileMetadataResponse> responseCaptor = ArgumentCaptor.forClass(FileMetadataResponse.class);
        verify(fileMetadataResponseObserver, timeout(1000)).onNext(responseCaptor.capture());
        verify(fileMetadataResponseObserver, timeout(1000)).onCompleted();
        
        FileMetadataResponse response = responseCaptor.getValue();
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
        
        DeleteFileRequest request = DeleteFileRequest.newBuilder()
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
        
        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.just(userId));
        when(permissionService.validateUserPermission(any(User.class), eq(fileId)))
            .thenReturn(Mono.just(file));
        when(fileServiceStrategy.getFileService(FileEnum.OTHER))
            .thenReturn(fileService);
        when(fileService.removeFile(eq(file), any(User.class)))
            .thenReturn(Mono.just(true));
        
        // When
        grpcService.deleteFile(request, deleteFileResponseObserver);
        
        // Then
        ArgumentCaptor<DeleteFileResponse> responseCaptor = ArgumentCaptor.forClass(DeleteFileResponse.class);
        verify(deleteFileResponseObserver, timeout(1000)).onNext(responseCaptor.capture());
        verify(deleteFileResponseObserver, timeout(1000)).onCompleted();
        
        DeleteFileResponse response = responseCaptor.getValue();
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
        
        AuthenticationRequest request = AuthenticationRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build();
        
        when(userService.login(any(AuthRequestDTO.class), isNull()))
            .thenReturn(Mono.just(jwtToken));
        when(tokenService.extractUserIdFromToken(jwtToken, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.just(expectedUserId));
        
        // When
        grpcService.authenticateUser(request, authResponseObserver);
        
        // Then
        ArgumentCaptor<AuthenticationResponse> responseCaptor = ArgumentCaptor.forClass(AuthenticationResponse.class);
        verify(authResponseObserver, timeout(1000)).onNext(responseCaptor.capture());
        verify(authResponseObserver, timeout(1000)).onCompleted();
        
        AuthenticationResponse response = responseCaptor.getValue();
        assertTrue(response.getSuccess());
        assertEquals(jwtToken, response.getJwtToken());
        assertEquals(expectedUserId, response.getUserId());
        
        // Verify method calls
        verify(userService).login(any(AuthRequestDTO.class), isNull());
        verify(tokenService).extractUserIdFromToken(jwtToken, TokenEnum.JWT_AUTHORIZATION_TOKEN);
    }
    
    @Test
    void testGetFileContent_PermissionDenied() {
        // Given
        Long fileId = 123L;
        Long userId = 456L;
        String token = "test-token";
        
        GetFileRequest request = GetFileRequest.newBuilder()
            .setFileId(fileId)
            .setUserId(userId)
            .setUserToken(token)
            .build();
        
        when(tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
            .thenReturn(Mono.just(userId));
        when(permissionService.validateUserPermission(any(User.class), eq(fileId)))
            .thenReturn(Mono.error(new RuntimeException("Permission denied")));
        
        // When
        grpcService.getFileContent(request, fileContentResponseObserver);
        
        // Then
        ArgumentCaptor<FileContentResponse> responseCaptor = ArgumentCaptor.forClass(FileContentResponse.class);
        verify(fileContentResponseObserver, timeout(1000)).onNext(responseCaptor.capture());
        verify(fileContentResponseObserver, timeout(1000)).onCompleted();
        
        FileContentResponse response = responseCaptor.getValue();
        assertFalse(response.getErrorMessage().isEmpty());
        assertEquals("Permission denied", response.getErrorMessage());
    }
}