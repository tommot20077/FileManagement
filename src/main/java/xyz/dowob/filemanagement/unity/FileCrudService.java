package xyz.dowob.filemanagement.unity;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

/**
 * 檔案元資料實體的統一 CRUD 操作介面，定義使用者檔案和伺服器檔案元資料的基本操作規範。
 * 此介面提供了針對 UserFileMetadata 和 ServerFileMetadata 實體的完整 CRUD 操作支援，
 * 包括建立、查詢、更新和刪除功能，所有操作均采用 Spring WebFlux 反應式編程模式。
 *
 * <p>此介面重點特色：
 * <ul>
 *   <li>非阻塞 I/O 操作，支援高並發檔案處理場景</li>
 *   <li>統一的異常處理機制，确保錯誤回應的一致性</li>
 *   <li>支援事務性操作，維持資料一致性</li>
 *   <li>適用於不同存儲後端的實作策略</li>
 *   <li>反應式流支援，可組合複雜的檔案操作流程</li>
 * </ul>
 *
 * <p>實作要求：
 * <ol>
 *   <li>確保所有操作的原子性，特別是在并發環境下</li>
 *   <li>遵循 Reactive Streams 規範，正確處理背壓和取消信號</li>
 *   <li>提供適當的錯誤訊息，便於用戶理解和處理失敗情況</li>
 *   <li>支援標準的 JPA/R2DBC 實體生命周期管理</li>
 *   <li>考慮效能優化，避免不必要的資料庫查詢</li>
 * </ol>
 *
 * <p>使用範例：
 * <pre>{@code
 * @Service
 * public class FileServiceImpl implements FileCrudService {
 *     
 *     @Autowired
 *     private UserFileMetaRepository userFileRepo;
 *     
 *     @Autowired
 *     private ServerFileMetaRepository serverFileRepo;
 *     
 *     @Override
 *     public Mono<UserFileMetadata> createUserFileMetadata() {
 *         return Mono.fromCallable(() -> new UserFileMetadata())
 *             .flatMap(userFileRepo::save)
 *             .doOnSuccess(file -> LogUnity.info("建立使用者檔案元資料成功，ID: {}", file.getId()))
 *             .onErrorMap(Exception.class, ex -> 
 *                 new ProcessException(ProcessException.ErrorCode.CREATE_ENTITY_FAILED, ex));
 *     }
 *     
 *     @Override
 *     @Transactional(readOnly = true)
 *     public Flux<UserFileMetadata> getAllUserFileMetadata() {
 *         return userFileRepo.findAll()
 *             .onBackpressureBuffer(1000)
 *             .onErrorResume(ex -> {
 *                 LogUnity.error("查詢所有使用者檔案失敗", ex);
 *                 return Flux.empty();
 *             });
 *     }
 * }
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserFileMetadata
 * @see ServerFileMetadata
 * @see Mono
 * @see Flux
 */
public interface FileCrudService {
    /**
     * 建立新的使用者檔案元資料實體，初始化預設值和必要的關聯資訊。
     * 此方法會建立一個新的 UserFileMetadata 實體並持久化到資料庫，
     * 同時設定必要的初始屬性如建立時間、狀態等。
     *
     * <p>建立流程包括：
     * <ol>
     *   <li>實例化新的 UserFileMetadata 物件</li>
     *   <li>設定預設的屬性值（如建立時間、狀態等）</li>
     *   <li>驗證實體的合法性</li>
     *   <li>持久化到資料庫</li>
     *   <li>返回包含完整資訊的實體</li>
     * </ol>
     *
     * <p>該操作為原子性操作，在任何步驟失敗時會回滾整個交易。
     * 對於并發場景，應考慮適當的鎖定機制以避免競用條件。
     *
     * @return 包含新建使用者檔案元資料實體的 Mono，失敗時發出 ProcessException
     */
    Mono<UserFileMetadata> createUserFileMetadata();

    /**
     * 根據唯一識別碼查詢指定的使用者檔案元資料實體。
     * 此方法會從資料庫中查找指定 ID 的 UserFileMetadata 實體，
     * 並加載相關的關聯資料（如必要）。
     *
     * <p>查詢特性：
     * <ul>
     *   <li>支援惱性加載，減少不必要的資料庫查詢</li>
     *   <li>自動處理實體狀態檢查，過濾已刪除或失效的記錄</li>
     *   <li>支援讀取優化，使用適當的查詢緩存策略</li>
 *   <li>線程安全，可在并發環境中安全使用</li>
     * </ul>
     *
     * <p>日誌資訊：
     * 方法執行時會記錄相關的查詢資訊，包括查詢參數、執行時間和結果狀態。
     * 對於查詢不到的情況，不會記錄為錯誤，而是正常的業務狀態。
     *
     * @param id 使用者檔案元資料的唯一識別碼，不可為 null
     * @return 包含查詢結果的 Mono，若未找到匹配記錄則為空 Mono
     * @throws IllegalArgumentException 當 id 參數為 null 或無效時
     */
    Mono<UserFileMetadata> getUserFileMetadataById(Long id);

    /**
     * 查詢所有可用的使用者檔案元資料實體，適用於批量處理和管理操作。
     * 此方法會返回資料庫中所有的 UserFileMetadata 實體，並支援流式處理以應對大量資料。
     *
     * <p>性能考慮：
     * <ul>
     *   <li>使用流式查詢，避免一次性加載所有資料到內存</li>
     *   <li>支援背壓控制，自動調節數據流速度</li>
     *   <li>提供分頁支援，可配合前端分頁顯示</li>
     *   <li>优先加載常用屬性，延遲加載關聯資料</li>
     * </ul>
     *
     * <p>使用建議：
     * <ol>
     *   <li>對於大量資料，建議使用 .buffer() 或 .window() 進行批量處理</li>
     *   <li>結合 .onBackpressureBuffer() 來設定緩衝區大小</li>
     *   <li>使用 .doOnNext() 進行逐筆資料處理</li>
     *   <li>通過 .filter() 提前過濾不需要的資料</li>
     * </ol>
     *
     * <p>警告：
     * 此方法可能返回大量資料，在生產環境中使用時請考慮性能影響。
     * 建議結合適當的過濾條件或分頁機制來減少資源消耗。
     *
     * @return 包含所有使用者檔案元資料實體的 Flux，支援流式處理
     */
    Flux<UserFileMetadata> getAllUserFileMetadata();

    /**
     * 更新指定的使用者檔案元資料實體，支援部分更新和完整更新。
     * 此方法會驗證實體的合法性，更新資料庫中的對應記錄，
     * 並返回更新後的實體物件。
     *
     * <p>更新特性：
     * <ul>
     *   <li>支援樂觀鎖定，防止并發更新衝突</li>
     *   <li>自動更新修改時間和版本號</li>
     *   <li>驗證實體狀態和權限，確保更新的合法性</li>
     *   <li>支援部分屬性更新，不影響其他屬性</li>
     *   <li>事務性保證，失敗時自動回滾</li>
     * </ul>
     *
     * <p>更新流程：
     * <ol>
     *   <li>驗證實體的存在性和合法性</li>
     *   <li>檢查版本號，防止並發修改衝突</li>
     *   <li>應用業務規則驗證</li>
     *   <li>執行資料庫更新操作</li>
     *   <li>返回更新後的實體</li>
     * </ol>
     *
     * <p>錯誤處理：
     * 當實體不存在、版本衝突或驗證失敗時，會拋出相應的異常。
     * 建議在上層業務邏輯中進行適當的錯誤處理和重試機制。
     *
     * @param entity 要更新的使用者檔案元資料實體，不可為 null
     * @return 包含更新後實體的 Mono，包含最新的版本資訊
     * @throws OptimisticLockingFailureException 當發生並發更新衝突時
     */
    Mono<UserFileMetadata> updateUserFileMetadata(UserFileMetadata entity);

    /**
     * 刪除指定的使用者檔案元資料實體，支援軟刪除和物理刪除兩種模式。
     * 此方法會根據系統配置和業務規則選擇適當的刪除策略，
     * 並處理相關的關聯資料清理工作。
     *
     * <p>刪除策略：
     * <ul>
     *   <li><b>軟刪除</b>：標記實體為已刪除狀態，保留數據以供程式復原</li>
     *   <li><b>物理刪除</b>：從資料庫中永久移除記錄，不可復原</li>
     *   <li><b>批量刪除</b>：支援同時刪除多個相關記錄</li>
     * </ul>
     *
     * <p>刪除前檢查：
     * <ol>
     *   <li>驗證用戶的刪除權限</li>
     *   <li>檢查是否存在依賴此檔案的其他資源</li>
     *   <li>確認實體狀態允許刪除操作</li>
     *   <li>備份重要資訊以供復原使用</li>
     * </ol>
     *
     * <p>清理作業：
     * 刪除操作完成後，系統會自動執行以下清理作業：
     * <ul>
     *   <li>清理相關的檔案快取</li>
     *   <li>更新統計資訊和索引</li>
     *   <li>通知相關的訂閱者或監聽器</li>
     *   <li>記錄操作日誌以供稍後審計</li>
     * </ul>
     *
     * @param entity 要刪除的使用者檔案元資料實體，不可為 null
     * @return 表示刪除操作完成的空 Mono，成功時不返回任何數據
     * @throws DataIntegrityViolationException 當刪除會造成資料一致性問題時
     */
    Mono<Void> deleteUserFileMetadata(UserFileMetadata entity);

    /**
     * 建立新的伺服器檔案元資料實體，用於記錄檔案的物理存儲資訊。
     * ServerFileMetadata 主要用於記錄檔案在伺服器端的存儲位置、
     * 檢查碼、壓縮資訊等物理屬性，與使用者層面的元資料分離。
     *
     * <p>實體初始化包括：
     * <ul>
     *   <li>生成唯一的檔案識別碼（通常為 UUID）</li>
     *   <li>設定預設的存儲策略和壓縮參數</li>
     *   <li>初始化安全相關的屬性（加密狀態、權限等）</li>
     *   <li>記錄建立時間和初始狀態</li>
     *   <li>設定相關的效能優化參數</li>
     * </ul>
     *
     * <p>與 UserFileMetadata 的關係：
     * ServerFileMetadata 通常與一個或多個 UserFileMetadata 實體關聯，
     * 實現檔案去重和共享存儲的功能。多個用戶可以擁有相同內容的檔案，
     * 但在伺服器端僅存儲一份物理檔案。
     *
     * @return 包含新建伺服器檔案元資料實體的 Mono，失敗時發出 ProcessException
     */
    Mono<ServerFileMetadata> createServerFileMetadata();

    /**
     * 根據唯一識別碼查詢指定的伺服器檔案元資料實體及其關聯資訊。
     * 此方法用於取得檔案的物理存儲資訊，包括檔案路徑、大小、
     * 檢查碼、壓縮狀態等關鍵資訊，通常用於檔案下載、備份或遷移操作。
     *
     * <p>查詢優化：
     * <ul>
     *   <li>支援讀取快取，減少資料庫負載</li>
     *   <li>自動負載平衡，選擇最佳的讀取節點</li>
     *   <li>支援部分屬性加載，減少不必要的數據傳輸</li>
     *   <li>自動過濾已損壞或不可用的檔案記錄</li>
     * </ul>
     *
     * <p>安全性考慮：
     * 查詢結果會進行權限檢查，確保調用者有權訪問指定的檔案資訊。
     * 敬感資訊（如加密密鑰、存儲憑證）會根據調用上下文進行適當的過濾。
     *
     * @param id 伺服器檔案元資料的唯一識別碼，不可為 null
     * @return 包含查詢結果的 Mono，若未找到或無權訪問則為空 Mono
     * @throws IllegalArgumentException 當 id 參數為 null 或格式不正確時
     */
    Mono<ServerFileMetadata> getByServerFileMetadataId(Long id);

    /**
     * 查詢所有可用的伺服器檔案元資料實體，主要用於系統管理和維護操作。
     * 此方法通常用於檔案存儲管理、磁碟空間分析、備份策略執行等管理層面操作，
     * 不建議在一般的業務邏輯中直接使用。
     *
     * <p>性能警告：
     * 此操作可能返回大量資料，在生產環境中使用時必須謹慎考慮性能影響。
     * 強烈建議：
     * <ul>
     *   <li>使用適當的分頁機制或過濾條件</li>
     *   <li>結合 .buffer() 進行批量處理</li>
     *   <li>設定適當的背壓緩衝區</li>
     *   <li>在非尖峰時段執行大量查詢</li>
     * </ul>
     *
     * <p>權限控制：
     * 此操作通常需要管理員權限，系統會檢查調用者的身份和權限。
     * 未授權的訪問會被拒絕，並記錄相關的安全日誌。
     *
     * <p>資料過濾：
     * 返回的結果會自動過濾掉以下類型的記錄：
     * <ul>
     *   <li>已刪除或標記為垃圾的檔案</li>
     *   <li>已損壞或不一致的檔案</li>
     *   <li>正在遷移或備份中的檔案</li>
     *   <li>超出保存期限的臨時檔案</li>
     * </ul>
     *
     * @return 包含所有可用伺服器檔案元資料實體的 Flux，支援流式處理
     */
    Flux<ServerFileMetadata> getAllServerFileMetadata();

    /**
     * 更新指定的伺服器檔案元資料實體，通常用於更新檔案的物理屬性和存儲資訊。
     * 此方法主要用於更新檔案的存儲位置、壓縮狀態、安全設定等系統層面的屬性，
     * 不同於 UserFileMetadata 的業務屬性更新。
     *
     * <p>常見更新場景：
     * <ul>
     *   <li>檔案壓縮或解壓縮後更新相關資訊</li>
     *   <li>檔案遷移到不同存儲位置後更新路徑</li>
     *   <li>重新計算檔案檢查碼或籤名</li>
     *   <li>更新加密狀態或安全策略</li>
     *   <li>調整存儲配置或效能參數</li>
     * </ul>
     *
     * <p>權限限制：
     * 此操作通常需要管理員或系統級別權限，因為直接影響檔案的物理存儲。
     * 一般用戶無法直接修改這些屬性，只能通過特定的業務流程進行間接更新。
     *
     * <p>安全性考慮：
     * <ol>
     *   <li>驗證更新操作的合法性和安全性</li>
     *   <li>檢查檔案的一致性和完整性</li>
     *   <li>備份關鍵資訊以供故障復原</li>
     *   <li>記錄詳細的更新日誌以供審計</li>
     * </ol>
     *
     * @param entity 要更新的伺服器檔案元資料實體，不可為 null
     * @return 包含更新後實體的 Mono，包含最新的系統屬性
     * @throws SecurityException 當缺乏足夠權限時
     */
    Mono<ServerFileMetadata> updateServerFileMetadata(ServerFileMetadata entity);

    /**
     * 刪除指定的伺服器檔案元資料實體及其關聯的物理檔案資源。
     * 此操作是非常危險的系統級別操作，通常只在系統維護、數據清理或復災情況下使用。
     * 一旦執行，所有關聯的物理檔案都將被永久刪除，不可復原。
     *
     * <p>刪除影響範圍：
     * <ul>
     *   <li>永久刪除資料庫中的元資料記錄</li>
     *   <li>刪除所有關聯的物理檔案（原始檔、縮圖、備份等）</li>
     *   <li>清理相關的緩存資料和索引</li>
     *   <li>更新統計資訊和存儲配額</li>
     *   <li>通知相關的監控系統和管理工具</li>
     * </ul>
     *
     * <p>先決條件檢查：
     * <ol>
     *   <li>確認沒有任何 UserFileMetadata 仍然引用此檔案</li>
     *   <li>驗證操作者具有足夠的管理員權限</li>
     *   <li>檢查是否存在正在進行的相關操作</li>
     *   <li>確認系統資源足夠支持清理操作</li>
     * </ol>
     *
     * <p>安全機制：
     * 為了防止意外的資料遺失，此方法包含多層安全檢查：
     * <ul>
     *   <li>双重身份驗證和權限確認</li>
     *   <li>事務性保證，失敗時完整回滾</li>
     *   <li>詳細的操作日誌記錄</li>
     *   <li>必要時的緊急停止機制</li>
     * </ul>
     *
     * @param entity 要刪除的伺服器檔案元資料實體，不可為 null
     * @return 表示刪除操作完成的空 Mono，成功時不返回任何數據
     * @throws SecurityException 當缺乏足夠的管理員權限時
     * @throws IllegalStateException 當檔案仍被其他用戶引用時
     * @throws DataIntegrityViolationException 當刪除會造成系統不一致時
     */
    Mono<Void> deleteServerFileMetadata(ServerFileMetadata entity);
}
