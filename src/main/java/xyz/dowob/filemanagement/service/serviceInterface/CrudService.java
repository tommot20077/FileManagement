package xyz.dowob.filemanagement.service.serviceInterface;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 基於反應式編程的 CRUD 操作介面，為所有實體服務層提供標準化的資料訪問契約。
 * 採用 Spring WebFlux 反應式程式設計模式，支援高併發、低延遲的非阻塞資料庫操作。
 *
 * <p>此介面設計遵循反應式流規範（Reactive Streams），使用 Reactor 框架的 Mono 和 Flux 發佈者模式。
 * 所有操作均為非阻塞式，透過反應式資料流提供背壓支援和資源管理。
 * 實現類必須確保線程安全和事務一致性，特別是在高併發環境下的資料完整性。
 *
 * <p>泛型設計特點：
 * <ul>
 *   <li>T 泛型提供實體類型安全，避免類型轉換錯誤</li>
 *   <li>ID 泛型支援多種標識符類型，增強介面靈活性</li>
 *   <li>類型參數在編譯時進行檢查，確保類型安全</li>
 * </ul>
 *
 * <p>事務性與一致性保證：
 * <ol>
 *   <li>所有更新和刪除操作必須在事務邊界內執行</li>
 *   <li>實現類應處理併發衝突和樂觀鎖競爭</li>
 *   <li>失敗操作必須通過 Mono.error() 或 Flux.error() 傳播異常</li>
 *   <li>事務回滾機制應在業務邏輯層或服務層實現</li>
 * </ol>
 *
 * <p>異常處理策略：
 * <ul>
 *   <li>參數驗證失敗：IllegalArgumentException</li>
 *   <li>實體不存在：返回空的 Mono 或 Flux</li>
 *   <li>資料庫連接錯誤：DataAccessException</li>
 *   <li>併發衝突：OptimisticLockingFailureException</li>
 *   <li>權限不足：AccessDeniedException</li>
 * </ul>
 *
 * <p>反應式操作範例：
 * <pre>
 * // 建立新實體
 * Mono&lt;User&gt; newUser = crudService.create()
 *     .doOnSuccess(user -&gt; log.info("建立新使用者: {}", user.getId()));
 *
 * // 查詢特定實體
 * Mono&lt;User&gt; user = crudService.getById(userId)
 *     .switchIfEmpty(Mono.error(new EntityNotFoundException("使用者不存在")));
 *
 * // 更新實體
 * Mono&lt;User&gt; updatedUser = crudService.update(user)
 *     .doOnError(ex -&gt; log.error("更新失敗", ex));
 *
 * // 刪除實體
 * Mono&lt;Void&gt; deleteResult = crudService.delete(user)
 *     .doOnSuccess(v -&gt; log.info("成功刪除使用者"));
 * </pre>
 *
 * <p>實現規範：
 * <ol>
 *   <li>所有方法必須返回非 null 的 Mono 或 Flux</li>
 *   <li>空結果應使用 Mono.empty() 或 Flux.empty()</li>
 *   <li>異常情況應使用 Mono.error() 或 Flux.error()</li>
 *   <li>實現類應支援取消操作和背壓控制</li>
 *   <li>長時間運行的操作應提供進度回報機制</li>
 * </ol>
 *
 * @param <T>  實體類型，必須是有效的 JPA 實體或領域模型，提供類型安全的操作保證
 * @param <ID> 實體主鍵類型，支援 Long、String、UUID 等可序列化類型，確保唯一性約束
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see reactor.core.publisher.Mono
 * @see reactor.core.publisher.Flux
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.orm.ObjectOptimisticLockingFailureException
 */
public interface CrudService<T, ID> {
    /**
     * 建立新的實體物件，執行初始化邏輯但不立即持久化到資料庫。
     *
     * <p>此方法負責實體的工廠建立模式，產生一個具有預設值的新實體實例。
     * 建立過程包括：
     * <ol>
     *   <li>實例化實體物件</li>
     *   <li>設定預設屬性值</li>
     *   <li>執行業務邏輯初始化</li>
     *   <li>準備實體供後續操作使用</li>
     * </ol>
     *
     * <p>實現契約：
     * <ul>
     *   <li>必須返回非 null 的 Mono 包裝器</li>
     *   <li>新建立的實體應具有有效的預設狀態</li>
     *   <li>不應執行任何資料庫寫入操作</li>
     *   <li>實體的唯一標識符可以在此階段生成或保留為空</li>
     * </ul>
     *
     * <p>使用場景：適用於需要預先配置實體狀態的業務邏輯，例如設定建立時間戳、
     * 分配預設權限、初始化關聯關係等。建立後的實體需要透過 {@link #update(Object)} 方法進行持久化。
     *
     * <p>錯誤處理：如果實體建立過程中發生錯誤（如記憶體不足、初始化失敗），
     * 應返回包含相應異常的 Mono.error()。
     *
     * @return 包含新建立實體的 Mono 發佈者，絕不為 null，可能包含初始化後的實體或錯誤信息
     * @implSpec 實現類必須確保返回的實體處於一致且有效的初始狀態
     * @implNote 此方法通常用於表單初始化或 API 端點的新實體準備階段
     */
    Mono<T> create();

    /**
     * 根據唯一標識符執行非阻塞式實體檢索操作，支援反應式資料流處理。
     *
     * <p>此方法執行單一實體的精確查詢，基於主鍵或唯一識別碼進行資料庫查找。
     * 查詢操作完全非阻塞，適合高併發環境下的資料存取需求。
     *
     * <p>查詢流程：
     * <ol>
     *   <li>驗證輸入參數的有效性</li>
     *   <li>構建查詢條件並執行資料庫查找</li>
     *   <li>將查詢結果封裝為反應式資料流</li>
     *   <li>處理快取邏輯（如果實現支援）</li>
     * </ol>
     *
     * <p>實現契約：
     * <ul>
     *   <li>必須返回非 null 的 Mono 包裝器</li>
     *   <li>找不到實體時返回 Mono.empty()，而非 Mono.of(null)</li>
     *   <li>參數驗證失敗時返回 Mono.error(IllegalArgumentException)</li>
     *   <li>資料庫錯誤時返回 Mono.error(DataAccessException)</li>
     * </ul>
     *
     * <p>效能考量：
     * <ul>
     *   <li>支援資料庫索引最佳化查詢</li>
     *   <li>可結合快取機制減少資料庫負載</li>
     *   <li>查詢超時控制避免資源鎖定</li>
     *   <li>支援查詢取消操作</li>
     * </ul>
     *
     * <p>使用範例：
     * <pre>
     * // 標準查詢
     * Mono&lt;User&gt; user = userService.getById(userId);
     * 
     * // 處理不存在的情況
     * Mono&lt;User&gt; userWithDefault = userService.getById(userId)
     *     .switchIfEmpty(Mono.just(defaultUser));
     * 
     * // 錯誤處理
     * Mono&lt;User&gt; userWithErrorHandling = userService.getById(userId)
     *     .onErrorMap(DataAccessException.class, ex -&gt; new ServiceException("查詢失敗", ex));
     * </pre>
     *
     * @param id 實體的唯一標識符，必須非 null 且符合 ID 類型規範
     * @return 包含查詢結果的 Mono 發佈者：成功時包含實體，未找到時為空，錯誤時包含異常
     * @throws IllegalArgumentException 當 id 參數為 null 或格式無效時拋出
     * @implSpec 實現類必須確保 id 驗證邏輯的一致性，並妥善處理各種邊界情況
     * @implNote 建議實現類支援查詢快取以提升效能，特別是對於頻繁查詢的實體
     */
    Mono<T> getById(ID id);

    /**
     * 執行全表掃描查詢，以反應式資料流形式返回所有實體記錄。
     *
     * <p>此方法使用 Flux 發佈者模式提供背壓支援的資料流處理，
     * 允許消費者根據處理能力控制資料接收速率，避免記憶體溢位問題。
     *
     * <p>操作特性：
     * <ol>
     *   <li>採用非阻塞式資料庫查詢</li>
     *   <li>支援資料流背壓控制</li>
     *   <li>可被中途取消以節省資源</li>
     *   <li>遵循資料庫預設排序或實體自然順序</li>
     * </ol>
     *
     * <p>實現契約：
     * <ul>
     *   <li>必須返回非 null 的 Flux 包裝器</li>
     *   <li>空結果集應返回 Flux.empty()，而非包含 null 元素的流</li>
     *   <li>查詢過程中的錯誤應通過 Flux.error() 傳播</li>
     *   <li>支援訂閱者的取消請求</li>
     * </ul>
     *
     * <p>效能與記憶體管理：
     * <ul>
     *   <li><strong>警告</strong>：大型資料集可能導致記憶體壓力</li>
     *   <li>建議與分頁查詢方法結合使用</li>
     *   <li>考慮實施查詢結果快取策略</li>
     *   <li>支援資料流的惰性載入機制</li>
     * </ul>
     *
     * <p>適用場景：
     * <ul>
     *   <li>小型至中型資料集的完整載入</li>
     *   <li>資料匯出和批次處理操作</li>
     *   <li>需要對全部資料進行統計分析</li>
     *   <li>快取預熱和資料同步任務</li>
     * </ul>
     *
     * <p>使用範例：
     * <pre>
     * // 基本使用
     * Flux&lt;User&gt; allUsers = userService.getAll();
     * 
     * // 背壓控制
     * Flux&lt;User&gt; controlledStream = userService.getAll()
     *     .take(1000)  // 限制處理數量
     *     .limitRate(50);  // 控制處理速率
     * 
     * // 錯誤處理
     * Flux&lt;User&gt; safeStream = userService.getAll()
     *     .onErrorResume(ex -&gt; Flux.empty())
     *     .doOnError(ex -&gt; log.error("查詢全部實體失敗", ex));
     * </pre>
     *
     * @return 包含所有實體的 Flux 發佈者，支援背壓控制和取消操作，絕不為 null
     * @implSpec 實現類應考慮大資料集的記憶體影響，並提供適當的資源管理機制
     * @implNote 對於超大型資料集，建議實現類提供警告或限制機制，引導使用者採用分頁查詢
     */
    Flux<T> getAll();

    /**
     * 執行基於類型和動態參數的條件化查詢，提供靈活的資料檢索機制。
     *
     * <p>此方法提供高度可客製化的查詢介面，允許實現類根據業務需求定義多種查詢模式。
     * 透過類型識別符和可變參數列表，支援複雜的查詢條件組合和動態查詢構建。
     *
     * <p>查詢機制：
     * <ol>
     *   <li>解析查詢類型識別符，確定查詢策略</li>
     *   <li>驗證和轉換動態參數為查詢條件</li>
     *   <li>構建動態查詢並執行非阻塞式資料庫操作</li>
     *   <li>將結果封裝為背壓支援的反應式資料流</li>
     * </ol>
     *
     * <p>實現契約：
     * <ul>
     *   <li>必須返回非 null 的 Flux 包裝器</li>
     *   <li>不支援的查詢類型應返回 Flux.error(UnsupportedOperationException)</li>
     *   <li>參數驗證失敗應返回 Flux.error(IllegalArgumentException)</li>
     *   <li>空結果應返回 Flux.empty()</li>
     * </ul>
     *
     * <p>類型識別符規範：
     * <ul>
     *   <li>建議使用常量定義支援的查詢類型</li>
     *   <li>類型名稱應具有描述性和一致性</li>
     *   <li>支援大小寫不敏感的類型匹配</li>
     *   <li>提供類型說明文件供客戶端參考</li>
     * </ul>
     *
     * <p>參數處理策略：
     * <ul>
     *   <li>支援多種資料類型的參數轉換</li>
     *   <li>提供參數數量和類型的驗證機制</li>
     *   <li>處理 null 參數和空參數陣列</li>
     *   <li>支援複雜物件作為查詢參數</li>
     * </ul>
     *
     * <p>常見查詢類型範例：
     * <pre>
     * // 按狀態查詢
     * Flux&lt;Order&gt; activeOrders = orderService.getAllByParams("status", "ACTIVE");
     * 
     * // 按日期範圍查詢
     * Flux&lt;Order&gt; recentOrders = orderService.getAllByParams("dateRange", 
     *     LocalDate.now().minusDays(7), LocalDate.now());
     * 
     * // 複雜條件查詢
     * Flux&lt;User&gt; qualifiedUsers = userService.getAllByParams("qualification",
     *     "age", 18, "status", "ACTIVE", "region", "Asia");
     * 
     * // 分頁查詢
     * Flux&lt;Product&gt; pagedProducts = productService.getAllByParams("pagination",
     *     PageRequest.of(0, 20));
     * </pre>
     *
     * <p>效能考量：
     * <ul>
     *   <li>複雜查詢應考慮資料庫索引最佳化</li>
     *   <li>大量結果應提供分頁或限制機制</li>
     *   <li>頻繁查詢可考慮結果快取</li>
     *   <li>支援查詢計畫分析和效能監控</li>
     * </ul>
     *
     * @param type 查詢類型識別符，指定查詢策略和參數解析方式，不可為 null 或空字串
     * @param args 動態查詢參數陣列，參數的數量、類型和順序由查詢類型決定，可以為空但不可為 null
     * @return 符合查詢條件的實體 Flux 發佈者，支援背壓控制，絕不為 null
     * @throws IllegalArgumentException 當查詢類型不支援、參數格式錯誤或參數數量不符時拋出
     * @throws UnsupportedOperationException 當查詢類型未實現時拋出
     * @implSpec 實現類必須明確定義支援的查詢類型和對應的參數規範
     * @implNote 建議實現類提供查詢類型常量和參數說明文件，以提升 API 的可用性
     */
    Flux<T> getAllByParams(String type, Object... args);

    /**
     * 執行實體更新操作，將變更以事務性方式持久化到資料庫，支援新增和修改兩種模式。
     *
     * <p>此方法採用 "upsert" 語義，對於新實體執行插入操作，對於現有實體執行更新操作。
     * 整個過程遵循反應式編程模式，提供非阻塞式的資料庫操作和完整的事務支援。
     *
     * <p>更新流程：
     * <ol>
     *   <li>實體有效性驗證（欄位檢查、業務規則驗證）</li>
     *   <li>確定操作類型（新增 vs 更新）</li>
     *   <li>執行樂觀鎖檢查（如果支援版本控制）</li>
     *   <li>應用業務邏輯和資料轉換</li>
     *   <li>執行資料庫寫入操作</li>
     *   <li>處理關聯實體和索引更新</li>
     *   <li>返回更新後的完整實體</li>
     * </ol>
     *
     * <p>實現契約：
     * <ul>
     *   <li>必須返回非 null 的 Mono 包裝器</li>
     *   <li>成功時返回更新後的實體，包含所有伺服器端變更</li>
     *   <li>驗證失敗時返回 Mono.error(IllegalArgumentException)</li>
     *   <li>併發衝突時返回 Mono.error(OptimisticLockingFailureException)</li>
     *   <li>資料庫錯誤時返回 Mono.error(DataAccessException)</li>
     * </ul>
     *
     * <p>事務性保證：
     * <ul>
     *   <li>所有更新操作必須在事務邊界內執行</li>
     *   <li>支援 ACID 特性，確保資料一致性</li>
     *   <li>失敗時自動回滾所有變更</li>
     *   <li>支援分散式事務（如果需要）</li>
     * </ul>
     *
     * <p>併發控制：
     * <ul>
     *   <li>支援樂觀鎖定機制防止併發衝突</li>
     *   <li>版本號自動遞增和驗證</li>
     *   <li>提供重試機制處理輕微衝突</li>
     *   <li>併發失敗時提供詳細錯誤信息</li>
     * </ul>
     *
     * <p>資料驗證：
     * <ul>
     *   <li>執行 Bean Validation 註解檢查</li>
     *   <li>業務規則和約束條件驗證</li>
     *   <li>外鍵關聯性檢查</li>
     *   <li>自定義驗證邏輯支援</li>
     * </ul>
     *
     * <p>使用範例：
     * <pre>
     * // 標準更新操作
     * Mono&lt;User&gt; updatedUser = userService.update(user)
     *     .doOnSuccess(u -&gt; log.info("使用者更新成功: {}", u.getId()));
     * 
     * // 新增操作（實體無 ID）
     * User newUser = new User();
     * newUser.setName("新使用者");
     * Mono&lt;User&gt; createdUser = userService.update(newUser);
     * 
     * // 錯誤處理
     * Mono&lt;User&gt; safeUpdate = userService.update(user)
     *     .onErrorMap(OptimisticLockingFailureException.class, 
     *         ex -&gt; new ConcurrencyException("資料已被其他使用者修改", ex))
     *     .retry(3);  // 重試機制
     * </pre>
     *
     * @param entity 要更新的實體物件，必須非 null 且通過基本驗證
     * @return 包含更新後實體的 Mono 發佈者，包含所有伺服器端產生的值（如 ID、時間戳等）
     * @throws IllegalArgumentException 當實體為 null、驗證失敗或違反業務規則時拋出
     * @throws OptimisticLockingFailureException 當發生併發衝突時拋出
     * @throws DataAccessException 當資料庫操作失敗時拋出
     * @implSpec 實現類必須確保事務完整性和併發安全性，並提供適當的錯誤處理機制
     * @implNote 建議實現類支援批次更新最佳化，並提供更新前後的資料變更追蹤功能
     */
    Mono<T> update(T entity);

    /**
     * 執行實體刪除操作，以事務性方式從資料庫中安全移除指定實體及其關聯資料。
     *
     * <p>此方法提供完整的刪除邏輯，包括權限檢查、關聯性處理、事務管理和資料清理。
     * 採用反應式編程模式，確保刪除操作的非阻塞性和高效能處理。
     *
     * <p>刪除流程：
     * <ol>
     *   <li>驗證實體有效性和存在性</li>
     *   <li>檢查刪除權限和業務規則</li>
     *   <li>分析外鍵關聯和依賴關係</li>
     *   <li>執行級聯刪除或關聯清理</li>
     *   <li>在事務邊界內執行實體刪除</li>
     *   <li>清理相關索引和快取資料</li>
     *   <li>記錄刪除操作審計日誌</li>
     * </ol>
     *
     * <p>實現契約：
     * <ul>
     *   <li>必須返回非 null 的 Mono&lt;Void&gt; 包裝器</li>
     *   <li>成功刪除時完成 Mono，不包含任何值</li>
     *   <li>實體不存在時可選擇完成或返回錯誤（由實現決定）</li>
     *   <li>權限不足時返回 Mono.error(AccessDeniedException)</li>
     *   <li>關聯約束違反時返回 Mono.error(DataIntegrityViolationException)</li>
     * </ul>
     *
     * <p>事務性保證：
     * <ul>
     *   <li>所有刪除操作必須在事務邊界內執行</li>
     *   <li>支援 ACID 特性，確保刪除的原子性</li>
     *   <li>失敗時自動回滾所有相關變更</li>
     *   <li>支援跨服務的分散式事務協調</li>
     * </ul>
     *
     * <p>關聯處理策略：
     * <ul>
     *   <li><strong>級聯刪除</strong>：自動刪除相關的子實體</li>
     *   <li><strong>設為 NULL</strong>：將外鍵欄位設為 null</li>
     *   <li><strong>限制刪除</strong>：存在關聯時拒絕刪除操作</li>
     *   <li><strong>軟刪除</strong>：標記為已刪除而非實際移除</li>
     * </ul>
     *
     * <p>安全性考量：
     * <ul>
     *   <li>執行權限驗證，確保操作者有刪除權限</li>
     *   <li>防止未授權的批次刪除操作</li>
     *   <li>記錄詳細的審計追蹤信息</li>
     *   <li>支援軟刪除以保持資料可恢復性</li>
     * </ul>
     *
     * <p>效能最佳化：
     * <ul>
     *   <li>批次處理關聯實體的刪除操作</li>
     *   <li>最佳化資料庫查詢和索引使用</li>
     *   <li>非同步處理大型關聯資料清理</li>
     *   <li>提供刪除進度監控和取消機制</li>
     * </ul>
     *
     * <p>使用範例：
     * <pre>
     * // 標準刪除操作
     * Mono&lt;Void&gt; deleteResult = userService.delete(user)
     *     .doOnSuccess(v -&gt; log.info("使用者刪除成功: {}", user.getId()));
     * 
     * // 條件式刪除
     * Mono&lt;Void&gt; conditionalDelete = userService.delete(user)
     *     .filter(v -&gt; user.canBeDeleted())
     *     .switchIfEmpty(Mono.error(new IllegalStateException("無法刪除此使用者")));
     * 
     * // 錯誤處理
     * Mono&lt;Void&gt; safeDelete = userService.delete(user)
     *     .onErrorMap(DataIntegrityViolationException.class,
     *         ex -&gt; new BusinessException("無法刪除：存在關聯資料", ex))
     *     .doOnError(ex -&gt; log.error("刪除失敗", ex));
     * 
     * // 軟刪除範例
     * Mono&lt;Void&gt; softDelete = userService.delete(user)
     *     .doOnSuccess(v -&gt; log.info("使用者已標記為刪除：{}", user.getId()));
     * </pre>
     *
     * @param entity 要刪除的實體物件，必須非 null 且包含有效的唯一標識符
     * @return 表示刪除操作完成的 Mono&lt;Void&gt; 發佈者，成功時完成，失敗時包含異常
     * @throws IllegalArgumentException 當實體為 null、無效或缺少必要標識符時拋出
     * @throws AccessDeniedException 當操作者沒有刪除權限時拋出
     * @throws DataIntegrityViolationException 當存在外鍵約束或關聯依賴時拋出
     * @throws EntityNotFoundException 當要刪除的實體不存在時拋出（可選行為）
     * @implSpec 實現類必須明確定義關聯處理策略和權限檢查邏輯，確保刪除操作的安全性和完整性
     * @implNote 建議實現類支援軟刪除模式和審計日誌記錄，並提供批次刪除最佳化功能
     */
    Mono<Void> delete(T entity);
}
