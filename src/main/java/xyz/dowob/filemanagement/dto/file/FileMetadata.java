package xyz.dowob.filemanagement.dto.file;

import lombok.Data;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName FileMetadata
 * @description
 * @create 2024-09-26 23:54
 * @Version 1.0
 **/
@Data
public class FileMetadata {
    private String fileName;

    private String filePath;

    private String md5;

    private Long fileSize;

    private Integer totalChunks;

    private Long userId;


    public UserFileMetadata formatToUserFileMetadata(Long serverFileId) {
        UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setFilename(this.fileName);
        userFileMetadata.setFilePath(this.filePath);
        userFileMetadata.setServerFileId(serverFileId);
        userFileMetadata.setUserId(this.userId);
        userFileMetadata.setLastAccessTime(LocalDateTime.now());
        userFileMetadata.setUploadTime(LocalDateTime.now());

        return userFileMetadata;
    }

    public TransferTask formatToTransferTask(String uploadTaskId, String message) {
        TransferTask task = new TransferTask();
        task.setTransferTaskId(uploadTaskId);
        task.setTotalChunks(this.getTotalChunks());
        task.setFilePath(formatFilePath(this.getFilePath(), this.getFileName()));
        task.setFileName(this.getFileName());
        task.setMd5(this.getMd5());
        task.setUserId(this.userId);
        task.setMessage(message);
        return task;
    }

    private String formatFilePath(String filePath, String filename) {
        return filePath.substring(0, filePath.lastIndexOf(filename));
    }
}
