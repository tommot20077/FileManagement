package xyz.dowob.filemanagement.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import xyz.dowob.filemanagement.customenum.FileEnum;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * 伺服器檔案元資料實體類別，對應資料庫中的 server_file_metadata 表。
 * <p>
 * 此實體類別管理伺服器層級的檔案實體資訊，包括檔案的物理屬性、
 * 存儲位置、內容特徵以及擁有者資訊等。此類別採用檔案去重機制，
 * 相同內容的檔案在伺服器上僅儲存一份實體。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>檔案物理屬性管理（大小、類型、MIME 類型）</li>
 *   <li>存儲位置管理（GridFS ID、MD5 檢驗碼）</li>
 *   <li>存取記錄追蹤（上傳時間、最後存取時間）</li>
 *   <li>擁有者管理（支援多使用者共享同一檔案實體）</li>
 *   <li>檔案去重機制（基於 MD5 檢驗碼）</li>
 * </ul>
 * </p>
 * <p>
 * 與 {@link UserFileMetadata} 的關係：
 * <ul>
 *   <li>ServerFileMetadata 管理檔案的物理實體</li>
 *   <li>UserFileMetadata 管理使用者對檔案的個人化設定</li>
 *   <li>多個 UserFileMetadata 可以關聯到同一個 ServerFileMetadata</li>
 *   <li>透過 serverFileId 欄位建立關聯關係</li>
 * </ul>
 * </p>
 * <p>
 * 使用範例：
 * <pre>{@code
 * ServerFileMetadata metadata = new ServerFileMetadata();
 * metadata.setFileSize(1024L);
 * metadata.setFileType(FileEnum.PDF);
 * metadata.setMimeType("application/pdf");
 * metadata.setMd5("d41d8cd98f00b204e9800998ecf8427e");
 * metadata.setGridFsId("60a5c2b4c45a3b2d8c9e1234");
 * metadata.getOwners().add(userId);
 * }</pre>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserFileMetadata
 * @see FileEnum
 */
@Getter
@Setter
@Table(name = "server_file_metadata")
public class ServerFileMetadata {
    /**
     * 伺服器檔案元資料的唯一主鍵識別碼。
     * <p>
     * 由資料庫自動生成的主鍵，確保每個檔案實體的唯一性。
     * 用於關聯 UserFileMetadata 記錄，實現檔案去重機制。
     * </p>
     */
    @Id
    private Long id;

    /**
     * 檔案的實際大小（位元組）。
     * <p>
     * 記錄檔案在儲存系統中所佔用的空間大小，用於儲存配額計算、
     * 網路傳輸預估和系統效能分析。
     * </p>
     */
    @Column("file_size")
    private Long fileSize;

    /**
     * 檔案的類型分類，使用 {@link FileEnum} 枚舉值。
     * <p>
     * 根據檔案副檔名自動識別檔案類型，用於：
     * <ul>
     *   <li>檔案處理策略的選擇</li>
     *   <li>使用者界面的展示和操作</li>
     *   <li>檔案轉換和預覽功能</li>
     * </ul>
     * </p>
     */
    @Column("file_type")
    private FileEnum fileType;

    /**
     * 檔案的 MIME 類型字串。
     * <p>
     * 標準的 MIME 類型識別碼，用於：
     * <ul>
     *   <li>HTTP 響應的 Content-Type 設定</li>
     *   <li>瀏覽器正確解析和顯示檔案</li>
     *   <li>檔案下載和開啟方式的決定</li>
     * </ul>
     * </p>
     */
    @Column("mime_type")
    private String mimeType;

    /**
     * 檔案首次上傳到系統的時間戳。
     * <p>
     * 記錄檔案實體被建立的精確時間，用於：
     * <ul>
     *   <li>檔案的時間排序和範圍篩選</li>
     *   <li>檔案生命週期管理和清理策略</li>
     *   <li>審計記錄和使用情況統計</li>
     * </ul>
     * </p>
     */
    @Column("upload_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    /**
     * 檔案最後一次被存取的時間戳。
     * <p>
     * 追蹤檔案的使用活躍度，用於：
     * <ul>
     *   <li>靜態檔案的識別和清理</li>
     *   <li>熱點檔案的快取優先級設定</li>
     *   <li>儲存空間的智慧管理</li>
     *   <li>系統效能分析和優化</li>
     * </ul>
     * </p>
     */
    @Column("last_access_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;

    /**
     * 檔案在 MongoDB GridFS 儲存系統中的唯一識別碼。
     * <p>
     * 指向檔案實際儲存位置的索引，用於：
     * <ul>
     *   <li>檔案內容的讀取和下載</li>
     *   <li>儲存系統的直接存取</li>
     *   <li>檔案串流傳輸和處理</li>
     * </ul>
     * </p>
     */
    @Column("grid_fs_id")
    private String gridFsId;

    /**
     * 檔案內容的 MD5 檢驗碼。
     * <p>
     * 用於檔案完整性驗證和去重機制，具有以下功能：
     * <ul>
     *   <li>檔案內容的唯一性識別</li>
     *   <li>上傳時的完整性檢查</li>
     *   <li>相同內容檔案的儲存空間節省</li>
     *   <li>重複檔案的快速偵測</li>
     * </ul>
     * </p>
     */
    private String md5;

    /**
     * 擁有此檔案實體的使用者 ID 集合。
     * <p>
     * 由於檔案去重機制，多個使用者可能共享同一個檔案實體。
     * 此集合記錄所有擁有者的 ID，用於：
     * <ul>
     *   <li>檢查使用者對檔案的存取權限</li>
     *   <li>儲存空間的共享管理</li>
     *   <li>檔案刪除時的影響分析</li>
     * </ul>
     * </p>
     */
    private Set<Long> owners = new HashSet<>();

    /**
     * 計算伺服器檔案元資料物件的雜湊碼，基於檔案的唯一識別碼（ID）。
     * <p>
     * 此實現確保具有相同 ID 的伺服器檔案元資料物件具有相同的雜湊碼，
     * 這對於在集合類別（如 HashSet、HashMap）中正確運作是必要的。
     * 遵循 equals-hashCode 合約的要求。
     * </p>
     *
     * @return 基於伺服器檔案 ID 的雜湊碼
     * @see #equals(Object)
     */
    @Override
    public int hashCode () {
        return id.hashCode();
    }


    /**
     * 判斷兩個伺服器檔案元資料物件是否相等，基於檔案的唯一識別碼（ID）。
     * <p>
     * 此方法遵循 equals 方法的標準實現模式，確保具有相同 ID 的伺服器檔案
     * 元資料物件被視為相等。這對於檔案去重機制和唯一性識別至關重要。
     * </p>
     *
     * @param o 用於比較的物件
     * @return 如果兩個伺服器檔案元資料物件的 ID 相同則回傳 true，否則回傳 false
     * @see #hashCode()
     */
    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServerFileMetadata that = (ServerFileMetadata) o;
        return id.equals(that.id);
    }


    /**
     * 將伺服器檔案元資料轉換為可讀的字串表示形式。
     * <p>
     * 此方法將伺服器檔案元資料的關鍵資訊組織成 HashMap 結構並轉換為字串，
     * 方便進行日誌記錄、除錯和系統監控。包含檔案的物理屬性、
     * 存儲位置和擁有者資訊等重要細節。
     * </p>
     *
     * @return 包含伺服器檔案元資料資訊的字串表示，格式為 HashMap 的字串形式
     */
    @Override
    public String toString () {
        HashMap<String, Object> fileMap = new HashMap<>();
        fileMap.put("id", id);
        fileMap.put("fileSize", fileSize);
        fileMap.put("fileType", fileType);
        fileMap.put("mimeType", mimeType);
        fileMap.put("uploadTime", uploadTime);
        fileMap.put("lastAccessTime", lastAccessTime);
        fileMap.put("gridFsId", gridFsId);
        fileMap.put("md5", md5);
        fileMap.put("owners", owners);
        return fileMap.toString();
    }

}
