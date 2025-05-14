package xyz.dowob.filemanagement.data.event;

import xyz.dowob.filemanagement.customenum.EditTypeEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

/**
 * 文件編輯消息類，用於封裝文件編輯的消息
 * 包括文件元數據、用戶和編輯類型
 * 這邊提供了靜態工廠方法來創建文件編輯消息對象
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileEditedMessage
 * @create 2025/5/15
 * @Version 1.0
 **/
public record FileEditedMessage(UserFileMetadata fileMetadata, User user, EditTypeEnum type) {
    public static FileEditedMessage of(UserFileMetadata fileMetadata, User user, EditTypeEnum typeEnum) {
        return new FileEditedMessage(fileMetadata, user, typeEnum);
    }
}
