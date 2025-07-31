package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.FileEnum;

import java.lang.annotation.*;

/**
 * 檔案處理器類型標記註解。
 * <p>
 * 此註解用於標記和識別不同類型的檔案處理器實現，支援基於檔案類型的策略模式分派機制。
 * 系統根據檔案的具體類型選擇相應的處理器，實現檔案操作的多型化處理。
 * <p>
 * 檔案管理系統支援多種檔案類型的專門處理，包括一般檔案的基本上傳、下載、刪除操作；
 * 線上檔案的線上編輯和協作編輯支援；資料夾的目錄結構管理和檔案組織操作；
 * 以及特殊格式檔案（如圖片、視訊、檔案等）的專業處理。
 * <p>
 * 使用範例：
 * <pre>{@code
 * @FileHandlerType(FileEnum.GENERAL_FILE)
 * @Service
 * public class GeneralFileHandler implements FileService {
 *     // 一般檔案處理邏輯
 * }
 * 
 * @FileHandlerType(FileEnum.ONLINE_FILE)
 * @Service  
 * public class OnlineFileHandler implements FileService {
 *     // 線上檔案編輯處理邏輯
 * }
 * 
 * @FileHandlerType(FileEnum.FOLDER)
 * @Service
 * public class FolderHandler implements FileService {
 *     // 資料夾管理處理邏輯  
 * }
 * }</pre>
 *
 * @see xyz.dowob.filemanagement.customenum.FileEnum
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FileHandlerType {
    /**
     * 指定檔案處理器的具體類型。
     * <p>
     * 此屬性定義了被標記類別所能處理的檔案類型，用於系統運行時的檔案處理策略選擇和分派。
     * 不同的檔案類型需要不同的處理邏輯，透過此標記可實現檔案操作的專業化處理。
     *
     * @return 檔案處理器支援的檔案類型枚舉值
     * @see xyz.dowob.filemanagement.customenum.FileEnum
     */
    FileEnum value();

}
