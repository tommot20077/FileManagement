package xyz.dowob.filemanagement.data.event;

import xyz.dowob.filemanagement.customenum.EditTypeEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

/**
 * 檔案編輯事件訊息記錄，作為檔案修改事件的不可變資料容器。
 *
 * <p>提供了一個標準化的事件封裝模型，記錄檔案編輯的核心資訊，包括：
 * <ul>
 *     <li>檔案元資料：被編輯的檔案詳細資訊</li>
 *     <li>用戶資訊：執行編輯操作的用戶</li>
 *     <li>編輯類型：描述編輯的具體行為</li>
 * </ul>
 * 同時提供了靜態工廠方法，方便創建事件訊息實例。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
public record FileEditedMessage(UserFileMetadata fileMetadata, User user, EditTypeEnum type) {
    
    /**
     * 建立檔案編輯事件訊息的靜態工廠方法。
     *
     * @param fileMetadata 被編輯的檔案元資料
     * @param user 執行編輯操作的用戶
     * @param typeEnum 編輯操作的類型
     * @return 新建立的檔案編輯事件訊息實例
     */
    public static FileEditedMessage of(UserFileMetadata fileMetadata, User user, EditTypeEnum typeEnum) {
        return new FileEditedMessage(fileMetadata, user, typeEnum);
    }
}
