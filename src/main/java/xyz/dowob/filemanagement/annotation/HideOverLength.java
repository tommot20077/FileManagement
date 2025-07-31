package xyz.dowob.filemanagement.annotation;

import java.lang.annotation.*;

/**
 * 超長字串隱藏標記註解。
 * <p>
 * 此註解用於標記需要在日誌記錄或輸出時隱藏超過指定長度的字串內容，防止日誌檔案過大或洩露過多詳細資訊。
 * 當被標記的字段、方法回傳值或類別在進行日誌記錄時，AOP 攔截器會自動處理超長內容的隱藏邏輯。
 * <p>
 * 主要應用於大型檔案內容的日誌記錄（避免將整個檔案內容寫入日誌）、
 * 長字串參數的方法調用記錄（保持日誌的可讀性）、資料庫查詢結果的輸出控制（防止大量資料影響日誌效能），
 * 以及 API 響應內容的調試輸出（控制詳細程度）。
 * <p>
 * 使用範例：
 * <pre>{@code
 * public class FileService {
 *     @HideOverLength
 *     private String fileContent;
 *     
 *     @HideOverLength
 *     public String getFileContent() {
 *         return this.fileContent;
 *     }
 *     
 *     public void processLargeData(@HideOverLength String data) {
 *         // 處理大型資料，參數在日誌中會被隱藏
 *     }
 * }
 * 
 * @HideOverLength
 * public class LargeDataTransferObject {
 *     // 整個類別的輸出都會被隱藏超長內容
 * }
 * }</pre>
 *
 * @since 1.0
 * @author yuan
 * @version 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface HideOverLength {
}
