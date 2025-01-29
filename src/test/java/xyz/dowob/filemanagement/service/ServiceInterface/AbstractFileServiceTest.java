package xyz.dowob.filemanagement.service.ServiceInterface;

import com.mongodb.client.gridfs.model.GridFSFile;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.apache.tika.Tika;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.data.file.bo.UploadTaskBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.TransfersTask;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.repostiory.ServerFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractFileServiceTest {

    @Mock
    private ServerFileMetaRepository mockServerFileMetaRepository;
    @Mock
    private UserFileMetaRepository mockUserFileMetaRepository;
    @Mock
    private RedisProvider mockRedisProvider;
    @Mock
    private GridFsProvider mockGridFsProvider;
    @Mock
    private TransfersTasksManager mockTransfersTasksManager;
    @Mock
    private FileProperties mockFileProperties;
    @Mock
    private DatabaseClient mockDatabaseClient;
    @Mock
    private CircuitBreakerConfig mockCircuitBreakerConfig;
    @Mock
    private Tika mockTika;

    private AbstractFileService abstractFileServiceUnderTest;

    @BeforeEach
    void setUp() throws Exception {
        abstractFileServiceUnderTest = new AbstractFileService(mockServerFileMetaRepository,
                                                               mockUserFileMetaRepository,
                                                               mockRedisProvider,
                                                               mockGridFsProvider,
                                                               mockTransfersTasksManager,
                                                               mockFileProperties,
                                                               mockDatabaseClient,
                                                               mockCircuitBreakerConfig
        ) {
        };
        // TODO: Set the following fields: tika.
    }

    @Test
    void testInit() throws Exception {
        // Setup
        // Configure FileProperties.getUpload(...).
        final FileProperties.Upload upload = new FileProperties.Upload();
        upload.setTempDirectory("tempDirectory");
        upload.setPayloadLength(0);
        upload.setChunkSize(0);
        upload.setMaxUploadTaskLimit(0);
        upload.setCombineProcessCountLimit(0);
        when(mockFileProperties.getUpload()).thenReturn(upload);

        // Run the test
        abstractFileServiceUnderTest.init();

        // Verify the results
    }

    @Test
    void testGetUserFileList() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(0L)).thenReturn(userFileMetadataFlux);

        // Configure UserFileMetaRepository.findAllByUserId(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux1 = Flux.just(userFileMetadata1);
        when(mockUserFileMetaRepository.findAllByUserId(0L)).thenReturn(userFileMetadataFlux1);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata2 = new UserFileMetadata();
        userFileMetadata2.setId(0L);
        userFileMetadata2.setUserId(0L);
        userFileMetadata2.setServerFileId(0L);
        userFileMetadata2.setFilename("filename");
        userFileMetadata2.setParentFolderId(0L);
        userFileMetadata2.setIsFolder(false);
        userFileMetadata2.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata2.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata2.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux2 = Flux.just(userFileMetadata2);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux2);

        // Configure ServerFileMetaRepository.findAllByIdIn(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Flux<ServerFileMetadata> serverFileMetadataFlux = Flux.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findAllByIdIn(Set.of(0L))).thenReturn(serverFileMetadataFlux);

        // Run the test
        final Flux<UserFileListDTO> result = abstractFileServiceUnderTest.getUserFileList(user, 0L);

        // Verify the results
    }

    @Test
    void testGetUserFileList_UserFileMetaRepositoryFindAllByUserIdAndParentFolderIdIsNullReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(0L)).thenReturn(Flux.empty());

        // Configure ServerFileMetaRepository.findAllByIdIn(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Flux<ServerFileMetadata> serverFileMetadataFlux = Flux.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findAllByIdIn(Set.of(0L))).thenReturn(serverFileMetadataFlux);

        // Run the test
        final Flux<UserFileListDTO> result = abstractFileServiceUnderTest.getUserFileList(user, 0L);

        // Verify the results
    }

    @Test
    void testGetUserFileList_UserFileMetaRepositoryFindAllByUserIdAndParentFolderIdIsNullReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(...).
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.error(new Exception("message"));
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(0L)).thenReturn(userFileMetadataFlux);

        // Configure ServerFileMetaRepository.findAllByIdIn(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Flux<ServerFileMetadata> serverFileMetadataFlux = Flux.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findAllByIdIn(Set.of(0L))).thenReturn(serverFileMetadataFlux);

        // Run the test
        final Flux<UserFileListDTO> result = abstractFileServiceUnderTest.getUserFileList(user, 0L);

        // Verify the results
    }

    @Test
    void testGetUserFileList_UserFileMetaRepositoryFindAllByUserIdReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockUserFileMetaRepository.findAllByUserId(0L)).thenReturn(Flux.empty());

        // Configure ServerFileMetaRepository.findAllByIdIn(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Flux<ServerFileMetadata> serverFileMetadataFlux = Flux.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findAllByIdIn(Set.of(0L))).thenReturn(serverFileMetadataFlux);

        // Run the test
        final Flux<UserFileListDTO> result = abstractFileServiceUnderTest.getUserFileList(user, 0L);

        // Verify the results
    }

    @Test
    void testGetUserFileList_UserFileMetaRepositoryFindAllByUserIdReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findAllByUserId(...).
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.error(new Exception("message"));
        when(mockUserFileMetaRepository.findAllByUserId(0L)).thenReturn(userFileMetadataFlux);

        // Configure ServerFileMetaRepository.findAllByIdIn(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Flux<ServerFileMetadata> serverFileMetadataFlux = Flux.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findAllByIdIn(Set.of(0L))).thenReturn(serverFileMetadataFlux);

        // Run the test
        final Flux<UserFileListDTO> result = abstractFileServiceUnderTest.getUserFileList(user, 0L);

        // Verify the results
    }

    @Test
    void testGetUserFileList_UserFileMetaRepositoryFindAllByUserIdAndParentFolderIdInReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(Flux.empty());

        // Configure ServerFileMetaRepository.findAllByIdIn(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Flux<ServerFileMetadata> serverFileMetadataFlux = Flux.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findAllByIdIn(Set.of(0L))).thenReturn(serverFileMetadataFlux);

        // Run the test
        final Flux<UserFileListDTO> result = abstractFileServiceUnderTest.getUserFileList(user, 0L);

        // Verify the results
    }

    @Test
    void testGetUserFileList_UserFileMetaRepositoryFindAllByUserIdAndParentFolderIdInReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.error(new Exception("message"));
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        // Configure ServerFileMetaRepository.findAllByIdIn(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Flux<ServerFileMetadata> serverFileMetadataFlux = Flux.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findAllByIdIn(Set.of(0L))).thenReturn(serverFileMetadataFlux);

        // Run the test
        final Flux<UserFileListDTO> result = abstractFileServiceUnderTest.getUserFileList(user, 0L);

        // Verify the results
    }

    @Test
    void testGetUserFileList_ServerFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(0L)).thenReturn(userFileMetadataFlux);

        when(mockServerFileMetaRepository.findAllByIdIn(Set.of(0L))).thenReturn(Flux.empty());

        // Run the test
        final Flux<UserFileListDTO> result = abstractFileServiceUnderTest.getUserFileList(user, 0L);

        // Verify the results
    }

    @Test
    void testGetUserFileList_ServerFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(0L)).thenReturn(userFileMetadataFlux);

        // Configure ServerFileMetaRepository.findAllByIdIn(...).
        final Flux<ServerFileMetadata> serverFileMetadataFlux = Flux.error(new Exception("message"));
        when(mockServerFileMetaRepository.findAllByIdIn(Set.of(0L))).thenReturn(serverFileMetadataFlux);

        // Run the test
        final Flux<UserFileListDTO> result = abstractFileServiceUnderTest.getUserFileList(user, 0L);

        // Verify the results
    }

    @Test
    void testUploadFile() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_UserFileMetaRepositoryFindByIdReturnsNoItem() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockUserFileMetaRepository.findById("id")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_UserFileMetaRepositoryFindByIdReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_ServerFileMetaRepositoryFindByMd5ReturnsNoItem() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_ServerFileMetaRepositoryFindByMd5ReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.error(new Exception("message"));
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_ServerFileMetaRepositorySaveReturnsNoItem() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_ServerFileMetaRepositorySaveReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.error(new Exception("message"));
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_UserFileMetaRepositorySaveReturnsNoItem() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_UserFileMetaRepositorySaveReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.error(new Exception("message"));
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_TransfersTasksManagerRegisterUploadTaskReturnsNoItem() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.empty());

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_TransfersTasksManagerRegisterUploadTaskReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1,
                                                          "transferTaskId"
        )).thenReturn(Mono.error(new Exception("message")));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_RedisProviderSetHashMapReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.error(new Exception("message")));

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_RedisProviderGenerateChunkSetReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFile_TransfersTasksManagerGetTransfersTaskReturnsNoItems() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findByMd5(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findByMd5("md5")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono1);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(Collections.emptyList());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFile(fileMetadataDTO, user);

        // Verify the results
    }

    @Test
    void testUploadFileChunk() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_RedisProviderGetHashMapReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.empty());
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_RedisProviderGetHashMapReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_RedisProviderIsChunkSetPendingReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_RedisProviderIsChunkSetPendingReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_RedisProviderDeleteSet1ReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.error(new Exception("message")));

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_GridFsProviderStoreFileReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(Mono.empty());
        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_GridFsProviderStoreFileReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.error(new Exception("message"));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_RedisProviderIncrementHashMapReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.empty());
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_RedisProviderIncrementHashMapReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.error(new Exception(
                "message")));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_GridFsProviderFindFileByFileNameReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(Mono.empty());

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_GridFsProviderFindFileByFileNameReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Mono<GridFSFile> gridFSFileMono = Mono.error(new Exception("message"));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_GridFsProviderGetResourceReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(Mono.empty());

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_GridFsProviderGetResourceReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.error(new Exception("message"));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_TransfersTasksManagerUpdateTransfersTaskReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.error(new Exception("message")));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_GridFsProviderDeleteFileByFilenameReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_RedisProviderDeleteHashReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_RedisProviderDeleteSet2ReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_TransfersTasksManagerFinishTransfersTaskReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.error(new Exception(
                "message")));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_ServerFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_ServerFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.error(new Exception("message"));
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_UserFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(Mono.empty());

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_UserFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testUploadFileChunk_RedisProviderSetSetReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockRedisProvider.isChunkSetPending("key", 0)).thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.uploadFileChunk(uploadChunkDTO);

        // Verify the results
    }

    @Test
    void testDownloadFile() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure GridFsProvider.findFileById(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileById(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0))).thenReturn(
                gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        // Run the test
        final Mono<UserFileDataBO> result = abstractFileServiceUnderTest.downloadFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDownloadFile_UserFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure GridFsProvider.findFileById(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileById(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0))).thenReturn(
                gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        // Run the test
        final Mono<UserFileDataBO> result = abstractFileServiceUnderTest.downloadFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDownloadFile_UserFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure GridFsProvider.findFileById(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileById(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0))).thenReturn(
                gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        // Run the test
        final Mono<UserFileDataBO> result = abstractFileServiceUnderTest.downloadFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDownloadFile_ServerFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockServerFileMetaRepository.findById("id")).thenReturn(Mono.empty());

        // Configure GridFsProvider.findFileById(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileById(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0))).thenReturn(
                gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        // Run the test
        final Mono<UserFileDataBO> result = abstractFileServiceUnderTest.downloadFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDownloadFile_ServerFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findById(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.error(new Exception("message"));
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure GridFsProvider.findFileById(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileById(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0))).thenReturn(
                gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        // Run the test
        final Mono<UserFileDataBO> result = abstractFileServiceUnderTest.downloadFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDownloadFile_GridFsProviderFindFileByIdReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        when(mockGridFsProvider.findFileById(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                          0
        ))).thenReturn(Mono.empty());

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        // Run the test
        final Mono<UserFileDataBO> result = abstractFileServiceUnderTest.downloadFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDownloadFile_GridFsProviderFindFileByIdReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure GridFsProvider.findFileById(...).
        final Mono<GridFSFile> gridFSFileMono = Mono.error(new Exception("message"));
        when(mockGridFsProvider.findFileById(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0))).thenReturn(
                gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        // Run the test
        final Mono<UserFileDataBO> result = abstractFileServiceUnderTest.downloadFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDownloadFile_GridFsProviderGetResourceReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure GridFsProvider.findFileById(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileById(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0))).thenReturn(
                gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UserFileDataBO> result = abstractFileServiceUnderTest.downloadFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDownloadFile_GridFsProviderGetResourceReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure GridFsProvider.findFileById(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileById(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0))).thenReturn(
                gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.error(new Exception("message"));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        // Run the test
        final Mono<UserFileDataBO> result = abstractFileServiceUnderTest.downloadFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFile() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        when(mockUserFileMetaRepository.deleteById("id")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFile_UserFileMetaRepositoryFindByIdReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(Mono.empty());
        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        when(mockUserFileMetaRepository.deleteById("id")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFile_UserFileMetaRepositoryFindByIdReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        when(mockUserFileMetaRepository.deleteById("id")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFile_UserFileMetaRepositoryCountByServerFileIdAndUserIdReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        when(mockUserFileMetaRepository.deleteById("id")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFile_UserFileMetaRepositoryCountByServerFileIdAndUserIdReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L),
                                                                     eq(0L),
                                                                     any(DatabaseClient.class)
        )).thenReturn(Mono.error(new Exception("message")));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        when(mockUserFileMetaRepository.deleteById("id")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFile_ServerFileMetaRepositoryFindByIdReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));
        when(mockServerFileMetaRepository.findById("id")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        when(mockUserFileMetaRepository.deleteById("id")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFile_ServerFileMetaRepositoryFindByIdReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.error(new Exception("message"));
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        when(mockUserFileMetaRepository.deleteById("id")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFile_ServerFileMetaRepositorySaveReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        when(mockUserFileMetaRepository.deleteById("id")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFile_ServerFileMetaRepositorySaveReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.error(new Exception("message"));
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        when(mockUserFileMetaRepository.deleteById("id")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFile("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFile_UserFileMetaRepositoryDeleteByIdReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        when(mockUserFileMetaRepository.deleteById("id")).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFile("fileId", user);

        // Verify the results
    }

    @Test
    void testEditFile() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono1);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.editFile(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testEditFile_UserFileMetaRepositoryFindByIdReturnsNoItem() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.editFile(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testEditFile_UserFileMetaRepositoryFindByIdReturnsError() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata);
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono1);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.editFile(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testEditFile_UserFileMetaRepositorySaveReturnsNoItem() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.editFile(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testEditFile_UserFileMetaRepositorySaveReturnsError() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.error(new Exception("message"));
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono1);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.editFile(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testCreateFolder() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono1);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.createFolder(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testCreateFolder_UserFileMetaRepositoryFindByIdReturnsNoItem() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockUserFileMetaRepository.findById("id")).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.createFolder(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testCreateFolder_UserFileMetaRepositoryFindByIdReturnsError() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata);
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono1);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.createFolder(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testCreateFolder_UserFileMetaRepositorySaveReturnsNoItem() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.createFolder(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testCreateFolder_UserFileMetaRepositorySaveReturnsError() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.error(new Exception("message"));
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono1);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.createFolder(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testEditFolder() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata1);
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono1);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.editFolder(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testEditFolder_UserFileMetaRepositoryFindByIdReturnsNoItem() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.editFolder(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testEditFolder_UserFileMetaRepositoryFindByIdReturnsError() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.just(userFileMetadata);
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono1);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.editFolder(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testEditFolder_UserFileMetaRepositorySaveReturnsNoItem() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.editFolder(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testEditFolder_UserFileMetaRepositorySaveReturnsError() throws Exception {
        // Setup
        final FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("fileId");
        fileEditDTO.setFileName("filename");
        fileEditDTO.setParentFolderId(0L);
        fileEditDTO.setShareUserIds(Set.of(0L));

        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final Mono<UserFileMetadata> userFileMetadataMono1 = Mono.error(new Exception("message"));
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono1);

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.editFolder(fileEditDTO, user);

        // Verify the results
    }

    @Test
    void testDeleteFolder() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata1);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_UserFileMetaRepositoryFindByIdReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_UserFileMetaRepositoryFindByIdReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_UserFileMetaRepositoryFindAllByUserIdAndParentFolderIdInReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(Flux.empty());
        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_UserFileMetaRepositoryFindAllByUserIdAndParentFolderIdInReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.error(new Exception("message"));
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_UserFileMetaRepositoryCountByServerFileIdAndUserIdReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata1);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_UserFileMetaRepositoryCountByServerFileIdAndUserIdReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata1);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L),
                                                                     eq(0L),
                                                                     any(DatabaseClient.class)
        )).thenReturn(Mono.error(new Exception("message")));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_ServerFileMetaRepositoryFindByIdReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata1);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));
        when(mockServerFileMetaRepository.findById("id")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_ServerFileMetaRepositoryFindByIdReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata1);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.error(new Exception("message"));
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_ServerFileMetaRepositorySaveReturnsNoItem() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata1);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_ServerFileMetaRepositorySaveReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata1);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.error(new Exception("message"));
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testDeleteFolder_UserFileMetaRepositoryDeleteReturnsError() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("fileId")).thenReturn(userFileMetadataMono);

        // Configure UserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(...).
        final UserFileMetadata userFileMetadata1 = new UserFileMetadata();
        userFileMetadata1.setId(0L);
        userFileMetadata1.setUserId(0L);
        userFileMetadata1.setServerFileId(0L);
        userFileMetadata1.setFilename("filename");
        userFileMetadata1.setParentFolderId(0L);
        userFileMetadata1.setIsFolder(false);
        userFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata1.setSharedWithUsers(Set.of(0L));
        final Flux<UserFileMetadata> userFileMetadataFlux = Flux.just(userFileMetadata1);
        when(mockUserFileMetaRepository.findAllByUserIdAndParentFolderIdIn(0L, List.of(0L))).thenReturn(userFileMetadataFlux);

        when(mockUserFileMetaRepository.countByServerFileIdAndUserId(eq(0L), eq(0L), any(DatabaseClient.class))).thenReturn(Mono.just(0L));

        // Configure ServerFileMetaRepository.findById(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        when(mockServerFileMetaRepository.findById("id")).thenReturn(serverFileMetadataMono);

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata1 = new ServerFileMetadata();
        serverFileMetadata1.setId(0L);
        serverFileMetadata1.setFileSize(0L);
        serverFileMetadata1.setFileType(FileEnum.IMAGE);
        serverFileMetadata1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata1.setGridFsId("gridFsId");
        serverFileMetadata1.setMd5("md5");
        serverFileMetadata1.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono1 = Mono.just(serverFileMetadata1);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono1);

        // Configure UserFileMetaRepository.delete(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.delete(entity1)).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.deleteFolder("fileId", user);

        // Verify the results
    }

    @Test
    void testValidateUserPermission1() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.validateUserPermission(user, userFileMetadata);

        // Verify the results
    }

    @Test
    void testAssociateUserFile() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<UserFileMetadata> result = abstractFileServiceUnderTest.associateUserFile(0L, fileMetadataDTO);

        // Verify the results
    }

    @Test
    void testAssociateUserFile_UserFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UserFileMetadata> result = abstractFileServiceUnderTest.associateUserFile(0L, fileMetadataDTO);

        // Verify the results
    }

    @Test
    void testAssociateUserFile_UserFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        // Configure UserFileMetaRepository.save(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        final UserFileMetadata entity = new UserFileMetadata();
        entity.setId(0L);
        entity.setUserId(0L);
        entity.setServerFileId(0L);
        entity.setFilename("filename");
        entity.setParentFolderId(0L);
        entity.setIsFolder(false);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<UserFileMetadata> result = abstractFileServiceUnderTest.associateUserFile(0L, fileMetadataDTO);

        // Verify the results
    }

    @Test
    void testInitialUpload() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.initialUpload(fileMetadataDTO);

        // Verify the results
    }

    @Test
    void testInitialUpload_TransfersTasksManagerRegisterUploadTaskReturnsNoItem() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.empty());

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.initialUpload(fileMetadataDTO);

        // Verify the results
    }

    @Test
    void testInitialUpload_TransfersTasksManagerRegisterUploadTaskReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1,
                                                          "transferTaskId"
        )).thenReturn(Mono.error(new Exception("message")));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Configure TransfersTasksManager.getTransfersTask(...).
        final TransfersTask transfersTask = new TransfersTask();
        transfersTask.setId(0L);
        transfersTask.setTransferTaskId("transferTaskId");
        transfersTask.setMd5("md5");
        transfersTask.setGridFsId("gridFsId");
        transfersTask.setFileSize(0L);
        final List<TransfersTask> transfersTasks = List.of(transfersTask);
        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(transfersTasks);

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.initialUpload(fileMetadataDTO);

        // Verify the results
    }

    @Test
    void testInitialUpload_RedisProviderSetHashMapReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.error(new Exception("message")));

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.initialUpload(fileMetadataDTO);

        // Verify the results
    }

    @Test
    void testInitialUpload_RedisProviderGenerateChunkSetReturnsError() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        // Configure RedisProvider.setHashMap(...).
        final UploadTaskBO value = new UploadTaskBO();
        value.setTransferTaskId("transferTaskId");
        value.setFileType(FileEnum.IMAGE);
        value.setFileSize(0L);
        value.setMd5("md5");
        value.setUserId(0L);
        when(mockRedisProvider.setHashMap("hashKey", "DTO", value, 6L, ChronoUnit.HOURS)).thenReturn(Mono.empty());

        when(mockRedisProvider.generateChunkSet("key", 0)).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.initialUpload(fileMetadataDTO);

        // Verify the results
    }

    @Test
    void testInitialUpload_TransfersTasksManagerGetTransfersTaskReturnsNoItems() throws Exception {
        // Setup
        final FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName("filename");
        fileMetadataDTO.setParentFolderId(0L);
        fileMetadataDTO.setMd5("md5");
        fileMetadataDTO.setFileSize(0L);
        fileMetadataDTO.setUserId(0L);

        // Configure TransfersTasksManager.registerUploadTask(...).
        final FileMetadataDTO fileMetadataDTO1 = new FileMetadataDTO();
        fileMetadataDTO1.setFileName("filename");
        fileMetadataDTO1.setParentFolderId(0L);
        fileMetadataDTO1.setMd5("md5");
        fileMetadataDTO1.setFileSize(0L);
        fileMetadataDTO1.setUserId(0L);
        when(mockTransfersTasksManager.registerUploadTask(fileMetadataDTO1, "transferTaskId")).thenReturn(Mono.just(false));

        when(mockTransfersTasksManager.getTransfersTask("md5", TransfersStatusEnum.UPLOADING)).thenReturn(Collections.emptyList());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.initialUpload(fileMetadataDTO);

        // Verify the results
    }

    @Test
    void testGetTotalChunks() throws Exception {
        assertThat(abstractFileServiceUnderTest.getTotalChunks(0L)).isEqualTo(0);
    }

    @Test
    void testValidateUserPermission2() throws Exception {
        // Setup
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);

        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.validateUserPermission(user, userFileMetadata, false);

        // Verify the results
    }

    @Test
    void testDetectFileType() throws Exception {
        // Setup
        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Run the test
        final FileEnum result = abstractFileServiceUnderTest.detectFileType("content".getBytes());

        // Verify the results
        assertThat(result).isEqualTo(FileEnum.IMAGE);
    }

    @Test
    void testCombineBytes() throws Exception {
        // Setup
        // Run the test
        final Mono<byte[]> result = abstractFileServiceUnderTest.combineBytes(List.of("content".getBytes()));

        // Verify the results
    }

    @Test
    void testRemoveTempData() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.removeTempData(uploadTaskBO);

        // Verify the results
    }

    @Test
    void testRemoveTempData_RedisProviderGetHashMapReturnsNoItem() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.removeTempData(uploadTaskBO);

        // Verify the results
    }

    @Test
    void testRemoveTempData_RedisProviderGetHashMapReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.error(new Exception("message")));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.removeTempData(uploadTaskBO);

        // Verify the results
    }

    @Test
    void testRemoveTempData_GridFsProviderReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.removeTempData(uploadTaskBO);

        // Verify the results
    }

    @Test
    void testRemoveTempData_RedisProviderDeleteHashReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.removeTempData(uploadTaskBO);

        // Verify the results
    }

    @Test
    void testRemoveTempData_RedisProviderDeleteSetReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.removeTempData(uploadTaskBO);

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_GridFsProviderStoreFileReturnsNoItem() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(Mono.empty());
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_GridFsProviderStoreFileReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.error(new Exception("message"));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_TransfersTasksManagerReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.error(new Exception(
                "message")));

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_ServerFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_ServerFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.error(new Exception("message"));
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_UserFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(Mono.empty());

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_UserFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_RedisProviderGetHashMapReturnsNoItem() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_RedisProviderGetHashMapReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.error(new Exception("message")));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_GridFsProviderDeleteFileByFilenameReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_RedisProviderDeleteHashReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testProcessFileAfterMd5Check_RedisProviderDeleteSetReturnsError() throws Exception {
        // Setup
        final UploadTaskBO uploadTaskBO = new UploadTaskBO();
        uploadTaskBO.setTransferTaskId("transferTaskId");
        uploadTaskBO.setFileType(FileEnum.IMAGE);
        uploadTaskBO.setFileSize(0L);
        uploadTaskBO.setMd5("md5");
        uploadTaskBO.setUserId(0L);

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.getHashMap("hashKey", "total_chunks")).thenReturn(Mono.just("value"));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<Void> result = abstractFileServiceUnderTest.processFileAfterMd5Check(uploadTaskBO, "content".getBytes());

        // Verify the results
    }

    @Test
    void testCombineChunks() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_RedisProviderGetHashMapReturnsNoItem() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.empty());
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_RedisProviderGetHashMapReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.error(new Exception("message")));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_GridFsProviderFindFileByFileNameReturnsNoItem() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(Mono.empty());

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_GridFsProviderFindFileByFileNameReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Mono<GridFSFile> gridFSFileMono = Mono.error(new Exception("message"));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_GridFsProviderGetResourceReturnsNoItem() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(Mono.empty());

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_GridFsProviderGetResourceReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.error(new Exception("message"));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_TransfersTasksManagerUpdateTransfersTaskReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.error(new Exception("message")));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_GridFsProviderDeleteFileByFilenameReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_RedisProviderDeleteHashReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_RedisProviderDeleteSetReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_GridFsProviderStoreFileReturnsNoItem() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_GridFsProviderStoreFileReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.error(new Exception("message"));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_TransfersTasksManagerFinishTransfersTaskReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.error(new Exception(
                "message")));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_ServerFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_ServerFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.error(new Exception("message"));
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_UserFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(Mono.empty());

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testCombineChunks_UserFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Run the test
        final Mono<ObjectId> result = abstractFileServiceUnderTest.combineChunks("transferTaskId", 0);

        // Verify the results
    }

    @Test
    void testProcessChunk() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_RedisProviderDeleteSet1ReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.error(new Exception("message")));

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_GridFsProviderStoreFileReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(Mono.empty());
        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_GridFsProviderStoreFileReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.error(new Exception("message"));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_RedisProviderIncrementHashMapReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.empty());
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_RedisProviderIncrementHashMapReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.error(new Exception(
                "message")));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_RedisProviderGetHashMapReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.empty());
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_RedisProviderGetHashMapReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.error(new Exception("message")));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_GridFsProviderFindFileByFileNameReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(Mono.empty());

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_GridFsProviderFindFileByFileNameReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Mono<GridFSFile> gridFSFileMono = Mono.error(new Exception("message"));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_GridFsProviderGetResourceReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(Mono.empty());

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_GridFsProviderGetResourceReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.error(new Exception("message"));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_TransfersTasksManagerUpdateTransfersTaskReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.error(new Exception("message")));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_GridFsProviderDeleteFileByFilenameReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_RedisProviderDeleteHashReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_RedisProviderDeleteSet2ReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.error(new Exception("message")));
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_TransfersTasksManagerFinishTransfersTaskReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.error(new Exception(
                "message")));
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_ServerFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(Mono.empty());

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_ServerFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.error(new Exception("message"));
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_UserFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(Mono.empty());

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_UserFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTika.detect(any(byte[].class))).thenReturn("result");
        when(mockTransfersTasksManager.finishTransfersTask("md5", "transferTaskId", "gridFsId")).thenReturn(Mono.empty());

        // Configure ServerFileMetaRepository.save(...).
        final ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
        serverFileMetadata.setId(0L);
        serverFileMetadata.setFileSize(0L);
        serverFileMetadata.setFileType(FileEnum.IMAGE);
        serverFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        serverFileMetadata.setGridFsId("gridFsId");
        serverFileMetadata.setMd5("md5");
        serverFileMetadata.setOwners(Set.of(0L));
        final Mono<ServerFileMetadata> serverFileMetadataMono = Mono.just(serverFileMetadata);
        final ServerFileMetadata entity = new ServerFileMetadata();
        entity.setId(0L);
        entity.setFileSize(0L);
        entity.setFileType(FileEnum.IMAGE);
        entity.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity.setGridFsId("gridFsId");
        entity.setMd5("md5");
        entity.setOwners(Set.of(0L));
        when(mockServerFileMetaRepository.save(entity)).thenReturn(serverFileMetadataMono);

        // Configure UserFileMetaRepository.save(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        final UserFileMetadata entity1 = new UserFileMetadata();
        entity1.setId(0L);
        entity1.setUserId(0L);
        entity1.setServerFileId(0L);
        entity1.setFilename("filename");
        entity1.setParentFolderId(0L);
        entity1.setIsFolder(false);
        entity1.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        entity1.setSharedWithUsers(Set.of(0L));
        when(mockUserFileMetaRepository.save(entity1)).thenReturn(userFileMetadataMono);

        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testProcessChunk_RedisProviderSetSetReturnsError() throws Exception {
        // Setup
        final UploadChunkDTO uploadChunkDTO = new UploadChunkDTO("transferTaskId", 0, 0, "content".getBytes());
        when(mockRedisProvider.deleteSet("pendingChunkKey", 0)).thenReturn(Mono.empty());

        // Configure GridFsProvider.storeFile(...).
        final Mono<ObjectId> objectIdMono = Mono.just(new ObjectId(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(), 0));
        when(mockGridFsProvider.storeFile(any(Flux.class), eq("fileName"))).thenReturn(objectIdMono);

        when(mockRedisProvider.incrementHashMap("key", "uploaded_count", 1L, 1L, ChronoUnit.HOURS)).thenReturn(Mono.just("value"));
        when(mockRedisProvider.getHashMap("hashKey", "DTO")).thenReturn(Mono.just("value"));
        when(mockTransfersTasksManager.getAvailableThreadCount()).thenReturn(0);

        // Configure GridFsProvider.findFileByFileName(...).
        final Document document = new Document();
        final Mono<GridFSFile> gridFSFileMono = Mono.just(new GridFSFile(null,
                                                                         "filename",
                                                                         0L,
                                                                         0,
                                                                         new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                                         document
        ));
        when(mockGridFsProvider.findFileByFileName("fileName")).thenReturn(gridFSFileMono);

        // Configure GridFsProvider.getResource(...).
        final Mono<ReactiveGridFsResource> reactiveGridFsResourceMono = Mono.just(new ReactiveGridFsResource("filename", null));
        final Document document1 = new Document();
        final GridFSFile gridFsFile = new GridFSFile(null,
                                                     "filename",
                                                     0L,
                                                     0,
                                                     new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime(),
                                                     document1
        );
        when(mockGridFsProvider.getResource(gridFsFile)).thenReturn(reactiveGridFsResourceMono);

        when(mockTransfersTasksManager.updateTransfersTask("md5",
                                                           "transferTaskId",
                                                           TransfersStatusEnum.FAILED,
                                                           "MD5校驗失敗",
                                                           "gridFsId",
                                                           false
        )).thenReturn(Mono.empty());
        when(mockGridFsProvider.deleteFileByFilename("fileName")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteHash("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteSet("key")).thenReturn(Mono.empty());
        when(mockRedisProvider.setSet("pendingChunkKey", 0)).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<UploadResponseDTO> result = abstractFileServiceUnderTest.processChunk(uploadChunkDTO,
                                                                                         "transferTaskId",
                                                                                         "key",
                                                                                         "pendingChunkKey"
        );

        // Verify the results
    }

    @Test
    void testCreateUserFileMetadata() throws Exception {
        assertThat(abstractFileServiceUnderTest.createUserFileMetadata()).isNull();
    }

    @Test
    void testGetUserFileMetadataById() throws Exception {
        // Setup
        // Configure UserFileMetaRepository.findById(...).
        final UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setId(0L);
        userFileMetadata.setUserId(0L);
        userFileMetadata.setServerFileId(0L);
        userFileMetadata.setFilename("filename");
        userFileMetadata.setParentFolderId(0L);
        userFileMetadata.setIsFolder(false);
        userFileMetadata.setUploadTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setLastAccessTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        userFileMetadata.setSharedWithUsers(Set.of(0L));
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.just(userFileMetadata);
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<UserFileMetadata> result = abstractFileServiceUnderTest.getUserFileMetadataById(0L);

        // Verify the results
    }

    @Test
    void testGetUserFileMetadataById_UserFileMetaRepositoryReturnsNoItem() throws Exception {
        // Setup
        when(mockUserFileMetaRepository.findById("id")).thenReturn(Mono.empty());

        // Run the test
        final Mono<UserFileMetadata> result = abstractFileServiceUnderTest.getUserFileMetadataById(0L);

        // Verify the results
    }

    @Test
    void testGetUserFileMetadataById_UserFileMetaRepositoryReturnsError() throws Exception {
        // Setup
        // Configure UserFileMetaRepository.findById(...).
        final Mono<UserFileMetadata> userFileMetadataMono = Mono.error(new Exception("message"));
        when(mockUserFileMetaRepository.findById("id")).thenReturn(userFileMetadataMono);

        // Run the test
        final Mono<UserFileMetadata> result = abstractFileServiceUnderTest.getUserFileMetadataById(0L);

        // Verify the results
    }

    @Test
    void testGetAllUserFileMetadata() throws Exception {
        assertThat(abstractFileServiceUnderTest.getAllUserFileMetadata()).isNull();
    }

    @Test
    void testUpdateUserFileMetadata() throws Exception {
        assertThat(abstractFileServiceUnderTest.updateUserFileMetadata(new UserFileMetadata())).isNull();
    }

    @Test
    void testDeleteUserFileMetadata() throws Exception {
        assertThat(abstractFileServiceUnderTest.deleteUserFileMetadata(new UserFileMetadata())).isNull();
    }

    @Test
    void testCreateServerFileMetadata() throws Exception {
        assertThat(abstractFileServiceUnderTest.createServerFileMetadata()).isNull();
    }

    @Test
    void testGetByServerFileMetadataId() throws Exception {
        assertThat(abstractFileServiceUnderTest.getByServerFileMetadataId(0L)).isNull();
    }

    @Test
    void testGetAllServerFileMetadata() throws Exception {
        assertThat(abstractFileServiceUnderTest.getAllServerFileMetadata()).isNull();
    }

    @Test
    void testUpdateServerFileMetadata() throws Exception {
        assertThat(abstractFileServiceUnderTest.updateServerFileMetadata(new ServerFileMetadata())).isNull();
    }

    @Test
    void testDeleteServerFileMetadata() throws Exception {
        assertThat(abstractFileServiceUnderTest.deleteServerFileMetadata(new ServerFileMetadata())).isNull();
    }
}
