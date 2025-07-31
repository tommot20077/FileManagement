/**
 * 資料傳輸物件（Data Transfer Object）包，包含檔案相關的資料傳輸物件。
 * 
 * <p>此包中的類別用於在不同層級之間傳輸資料，特別是在控制器、服務和用戶端之間。
 * DTO 物件專注於資料的序列化、反序列化和驗證，確保資料傳輸的安全性和一致性。
 * 
 * <p>包含的主要類別：
 * <ul>
 * <li>{@link xyz.dowob.filemanagement.data.file.dto.EditorContentDTO} - 編輯器內容傳輸</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.dto.FileEditDTO} - 檔案編輯請求</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.dto.FileFilterDTO} - 檔案過濾條件</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO} - 檔案元資料</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.dto.FileVersionDTO} - 檔案版本資訊</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.dto.UploadChunkDTO} - 檔案上傳分片</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.dto.UploadResponseDTO} - 上傳響應</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.dto.UserFileListDTO} - 用戶檔案列表</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.data.file.dto;