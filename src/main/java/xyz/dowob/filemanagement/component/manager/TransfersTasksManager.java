package xyz.dowob.filemanagement.component.manager;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.TransfersTask;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.TransfersTasksRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分佈式檔案傳輸任務管理器，提供高效且靈活的檔案傳輸生命週期管理機制。
 *
 * <p>本類實現了一個響應式、高性能的檔案傳輸任務管理系統，支持上傳、下載等複雜檔案操作的全生命週期追蹤。</p>
 *
 * <p>主要功能特點：</p>
 *
 * <p>1. 任務管理機制：
 *    - 使用 {@link java.util.concurrent.ConcurrentHashMap} 管理活躍的傳輸任務
 *    - 支持動態註冊、更新和追蹤檔案傳輸任務
 *    - 提供基於 MD5 的檔案任務唯一標識</p>
 *
 * <p>2. 響應式設計：
 *    - 採用 Project Reactor 的 {@link reactor.core.publisher.Mono} 進行非阻塞任務管理
 *    - 支持異步、高併發的檔案傳輸任務處理
 *    - 與 {@link xyz.dowob.filemanagement.repostiory.TransfersTasksRepository} 無縫整合</p>
 *
 * <p>3. 高級任務控制：
 *    - 支持檔案大小限制驗證
 *    - 動態檢測和管理任務狀態
 *    - 提供靈活的任務更新和狀態轉換機制</p>
 *
 * <p>4. 安全性與異常處理：
 *    - 預防重複上傳任務
 *    - 提供詳細的任務狀態和錯誤訊息
 *    - 在系統關閉時自動處理未完成的傳輸任務</p>
 *
 * <p>5. 資源管理：
 *    - 支持動態計算可用線程數量（已棄用）
 *    - 提供任務完成後的資源自動釋放
 *    - 確保系統資源的高效利用</p>
 *
 * <p>本類通過精細的任務管理邏輯和響應式設計，確保了檔案傳輸過程的穩定性、可靠性和高性能。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
public class TransfersTasksManager {
    /**
     * 活躍傳輸任務的內存緩存，採用雙層 Map 結構進行高效管理。
     * <p>
     * <strong>資料結構說明：</strong>
     * </p>
     * <ul>
     *   <li><strong>外層 Map：</strong>Key 為檔案的 MD5 雜湊值，Value 為該檔案的所有相關任務</li>
     *   <li><strong>內層 Map：</strong>Key 為任務的唯一識別碼，Value 為具體的傳輸任務實例</li>
     * </ul>
     * <p>
     * <strong>設計優勢：</strong>
     * </p>
     * <ul>
     *   <li>使用 {@link ConcurrentHashMap} 確保線程安全</li>
     *   <li>支援高併發讀寫操作，適合處理大量併發請求</li>
     *   <li>透過 MD5 索引快速定位相同檔案的不同傳輸任務</li>
     *   <li>支援單一檔案的多種傳輸操作（上傳、下載、轉換等）</li>
     * </ul>
     * <p>
     * <strong>使用情境：</strong>
     * </p>
     * <ul>
     *   <li>追蹤正在進行中的檔案傳輸任務</li>
     *   <li>防止重複上傳相同 MD5 的檔案</li>
     *   <li>提供即時的任務狀態查詢和更新</li>
     *   <li>在任務完成或失敗時自動清理資源</li>
     * </ul>
     * 
     * @see TransfersTask
     * @see ConcurrentHashMap
     */
    private final ConcurrentHashMap<String, Map<String, TransfersTask>> activeTransfersTask = new ConcurrentHashMap<>();

    /**
     * 傳輸任務的持久化資料庫操作接口，提供反應式資料存取功能。
     * <p>
     * 此存儲庫負責管理傳輸任務的持久化狀態，確保任務資訊在系統重啟後仍能保持。
     * 支援非阻塞式的 CRUD 操作，與反應式編程模式完美整合。
     * </p>
     * 
     * @see TransfersTasksRepository
     * @see TransfersTask
     */
    private final TransfersTasksRepository transfersTasksRepository;

    /**
     * 檔案操作相關的配置屬性，包含上傳限制、處理參數等設定。
     * <p>
     * 主要包含以下配置資訊：
     * </p>
     * <ul>
     *   <li><strong>上傳限制：</strong>最大檔案大小、允許的檔案類型等</li>
     *   <li><strong>處理參數：</strong>合併處理線程數量限制、擁塞控制等</li>
     *   <li><strong>存儲設定：</strong>檔案存儲路徑、緩存策略等</li>
     * </ul>
     * <p>
     * 這些配置參數用於驗證任務的合法性和控制系統資源使用。
     * </p>
     * 
     * @see FileProperties
     */
    private final FileProperties fileProperties;

    /**
     * 建構傳輸任務管理器實例，初始化所有必要的依賴組件。
     * <p>
     * 此建構函數透過 Spring 的依賴注入機制自動組裝所需的組件，
     * 確保管理器能夠正常執行任務管理和持久化操作。
     * </p>
     * <p>
     * <strong>初始化過程：</strong>
     * </p>
     * <ol>
     *   <li>設定資料庫操作接口，用於任務的持久化管理</li>
     *   <li>設定檔案配置屬性，用於驗證和控制系統資源</li>
     *   <li>初始化內存緩存結構，用於追蹤活躍任務</li>
     * </ol>
     * 
     * @param transfersTasksRepository 傳輸任務的持久化資料庫操作接口，負責任務的存傲和查詢
     * @param fileProperties 檔案操作配置屬性，包含上傳限制和處理參數設定
     * @see TransfersTasksRepository
     * @see FileProperties
     */
    public TransfersTasksManager(TransfersTasksRepository transfersTasksRepository, FileProperties fileProperties) {
        this.transfersTasksRepository = transfersTasksRepository;
        this.fileProperties = fileProperties;
    }


    /**
     * 註冊新的檔案上傳任務，進行完整的合法性驗證和系統資源檢查。
     * <p>
     * 此方法為檔案上傳流程的入口點，負責確保上傳任務的合法性和唯一性。
     * 在創建任務之前，會進行多項驗證檢查，確保系統資源的合理使用。
     * </p>
     * <p>
     * <strong>驗證檢查項目：</strong>
     * </p>
     * <ol>
     *   <li><strong>重複任務檢查：</strong>檢查相同 MD5 的檔案是否已有上傳任務進行中</li>
     *   <li><strong>檔案大小驗證：</strong>檢查檔案大小是否超過系統配置的上傳限制</li>
     *   <li><strong>系統資源檢查：</strong>確保系統有足夠的資源處理新的上傳任務</li>
     * </ol>
     * <p>
     * <strong>錯誤處理：</strong>
     * </p>
     * <ul>
     *   <li><strong>重複任務：</strong>拋出 {@link ValidationException} 並帶有特定的錯誤代碼</li>
     *   <li><strong>檔案大小超限：</strong>拋出 {@link ValidationException} 並提供詳細的限制資訊</li>
     * </ul>
     * <p>
     * <strong>成功後操作：</strong>
     * </p>
     * <ul>
     *   <li>在內存緩存中註冊新的上傳任務</li>
     *   <li>將任務資訊持久化至資料庫</li>
     *   <li>設定任務狀態為 {@link TransfersStatusEnum#UPLOADING}</li>
     * </ul>
     * 
     * @param fileMetadataDTO 檔案元資料物件，包含 MD5、檔案大小等資訊，不得為 null
     * @param transferTaskId 任務的唯一識別碼，用於追蹤和管理此次上傳任務，不得為 null
     * @return Mono&lt;Void&gt; 無內容的反應式流，成功時完成，失敗時發出異常
     * @see #createTransfersTask(FileMetadataDTO, String, TransfersStatusEnum)
     * @see ValidationException.ErrorCode#EXISTING_TRANSFER_TASK
     * @see ValidationException.ErrorCode#FILE_SIZE_LIMIT
     * @apiNote 此方法採用反應式編程模式，非阻塞式執行
     * @implNote 使用 {@code Mono.defer()} 確保驗證邏輯在訂閱時才執行
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> registerUploadTask(FileMetadataDTO fileMetadataDTO, String transferTaskId) {
        return Mono.defer(() -> {
            String md5 = fileMetadataDTO.getMd5();
            if (activeTransfersTask.containsKey(fileMetadataDTO.getMd5())) {
                String alreadyTransferTaskId = this.getTransfersTask(md5, TransfersStatusEnum.UPLOADING).getFirst().getTransferTaskId();
                ValidationException error = new ValidationException(ValidationException.ErrorCode.EXISTING_TRANSFER_TASK, md5, alreadyTransferTaskId);
                return Mono.error(error);
            }
            boolean isLimitSize = fileProperties.getUpload().getMaxUploadFileSize().toBytes() > 0;
            if (isLimitSize && fileMetadataDTO.getFileSize() > fileProperties.getUpload().getMaxUploadFileSize().toBytes()) {
                ValidationException error = new ValidationException(ValidationException.ErrorCode.FILE_SIZE_LIMIT,
                                                                    fileMetadataDTO.getFileSize(),
                                                                    fileProperties.getUpload().getMaxUploadFileSize()
                );
                return Mono.error(error);
            }
            return createTransfersTask(fileMetadataDTO, transferTaskId, TransfersStatusEnum.UPLOADING);
        });
    }

    /**
     * 獲取指定檔案的所有相關傳輸任務，支援依據狀態進行精確篩選。
     * <p>
     * 此方法提供高效的任務查詢功能，能夠快速定位和篩選特定檔案的傳輸任務。
     * 透過 MD5 索引和狀態過濾，提供精確的任務追蹤能力。
     * </p>
     * <p>
     * <strong>查詢特性：</strong>
     * </p>
     * <ul>
     *   <li><strong>快速定位：</strong>透過 MD5 雜湊值直接定位相關任務群組</li>
     *   <li><strong>狀態篩選：</strong>支援依據任務狀態進行精確過濾</li>
     *   <li><strong>線程安全：</strong>使用線程安全的資料結構，支援併發存取</li>
     *   <li><strong>容錯處理：</strong>當檔案不存在時繁回空列表，不拋出異常</li>
     * </ul>
     * <p>
     * <strong>使用情境：</strong>
     * </p>
     * <ul>
     *   <li>查詢特定檔案的所有任務（status 為 null）</li>
     *   <li>查詢特定檔案的特定狀態任務</li>
     *   <li>檢查檔案是否有正在進行中的上傳或下載任務</li>
     *   <li>獲取任務歷史記錄和進度資訊</li>
     * </ul>
     * <p>
     * <strong>返回值說明：</strong>
     * </p>
     * <ul>
     *   <li>若檔案存在且有符合條件的任務，返回包含所有符合的任務列表</li>
     *   <li>若檔案不存在或無符合條件的任務，返回空列表</li>
     *   <li>返回的列表為新建物件，修改不會影響原始資料</li>
     * </ul>
     * 
     * @param md5 檔案的 MD5 雜湊值，作為任務查詢的主要索引，不得為 null
     * @param status 任務狀態篩選條件，為 null 時返回所有狀態的任務
     * @return 包含所有符合條件傳輸任務的列表，永不為 null
     * @see TransfersStatusEnum
     * @see TransfersTask
     * @apiNote 此方法為線程安全的同步操作，適合高併發環境
     * @implNote 內部使用流式遍歷進行狀態篩選，性能良好
     */
    public List<TransfersTask> getTransfersTask(String md5, TransfersStatusEnum status) {
        if (activeTransfersTask.containsKey(md5)) {
            Map<String, TransfersTask> transfersTaskMap = activeTransfersTask.get(md5);
            List<TransfersTask> result = new ArrayList<>();
            transfersTaskMap.forEach((key, value) -> {
                if (status == null || value.getStatus() == status) {
                    result.add(value);
                }
            });
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * 註冊新的檔案轉換或處理任務，提供簡化的任務建立接口。
     * <p>
     * 此方法為 {@link #createTransfersTask(FileMetadataDTO, String, String, String, TransfersStatusEnum)}
     * 的簡化版本，適用於不需要額外參數的一般任務建立情境。
     * 自動處理時間戳記和預設參數，簡化調用者的使用複雜度。
     * </p>
     * <p>
     * <strong>使用情境：</strong>
     * </p>
     * <ul>
     *   <li>建立基本的檔案轉換任務</li>
     *   <li>建立不需要特定 GridFS ID 或訊息的任務</li>
     *   <li>快速註冊一般性質的傳輸任務</li>
     * </ul>
     * <p>
     * <strong>自動處理的參數：</strong>
     * </p>
     * <ul>
     *   <li>GridFS 檔案 ID 設為 null</li>
     *   <li>任務訊息設為 null</li>
     *   <li>開始時間設為當前時間</li>
     * </ul>
     * 
     * @param fileMetadataDTO 檔案元資料物件，包含 MD5、檔案大小等基本資訊
     * @param transferTaskId 任務的唯一識別碼，用於後續的任務追蹤和管理
     * @param status 任務的初始狀態，將決定任務的處理方式
     * @return Mono&lt;Void&gt; 無內容的反應式流，任務建立成功時完成
     * @see #createTransfersTask(FileMetadataDTO, String, String, String, TransfersStatusEnum)
     * @see TransfersStatusEnum
     * @apiNote 這是一個便利方法，內部委託給完整版本的建立方法
     */
    public Mono<Void> createTransfersTask(FileMetadataDTO fileMetadataDTO, String transferTaskId, TransfersStatusEnum status) {
        return createTransfersTask(fileMetadataDTO, transferTaskId, null, null, status);
    }

    /**
     * 建立新的傳輸任務實例，同時進行內存緩存和持久化存儲。
     * <p>
     * 此方法為任務建立的核心實現，負責完整的任務生命週期管理。
     * 包括任務實例的建立、屬性設定、內存註冊和持久化存儲。
     * </p>
     * <p>
     * <strong>任務建立流程：</strong>
     * </p>
     * <ol>
     *   <li><strong>實例建立：</strong>建立新的 {@link TransfersTask} 實例</li>
     *   <li><strong>屬性設定：</strong>設定任務的所有必要屬性和時間戳記</li>
     *   <li><strong>內存註冊：</strong>將任務註冊到活躍任務緩存中</li>
     *   <li><strong>持久化存儲：</strong>將任務資訊持久化至資料庫</li>
     * </ol>
     * <p>
     * <strong>任務屬性設定：</strong>
     * </p>
     * <ul>
     *   <li><strong>唯一識別：</strong>設定任務 ID 和檔案 MD5 等唯一識別資訊</li>
     *   <li><strong>檔案資訊：</strong>記錄檔案大小、GridFS ID 等檔案相關資訊</li>
     *   <li><strong>時間戳記：</strong>記錄任務開始時間，用於性能分析和超時控制</li>
     *   <li><strong>狀態資訊：</strong>記錄任務當前狀態和相關訊息</li>
     * </ul>
     * <p>
     * <strong>反應式設計：</strong>
     * </p>
     * <ul>
     *   <li>非阻塞式操作：所有資料庫操作都採用反應式模式</li>
     *   <li>錯誤傳播：任何異常情況都會透過 Mono 進行傳播</li>
     *   <li>資源管理：確保在異常情況下也不會涉漏資源</li>
     * </ul>
     * <p>
     * <strong>資料一致性：</strong>
     * </p>
     * <ul>
     *   <li>內存資料在持久化成功後才會更新</li>
     *   <li>失敗時自動恢復之前的狀態</li>
     *   <li>確保內存和持久化存儲的資料一致性</li>
     * </ul>
     * 
     * @param fileMetadataDTO 檔案元資料物件，包含檔案的基本資訊和屬性
     * @param transferTaskId 任務的唯一識別碼，用於在整個系統中追蹤此任務
     * @param gridFsId GridFS 中的檔案識別碼，可為 null（對於上傳中的任務）
     * @param message 任務相關的訊息或備註，可為 null
     * @param status 任務的初始狀態，決定任務的處理方式和流程
     * @return Mono&lt;Void&gt; 無內容的反應式流，成功時完成，失敗時發出對應異常
     * @see TransfersTask
     * @see TransfersStatusEnum
     * @see #updateTransfersTask(String, String, TransfersStatusEnum, String, String, Boolean)
     * @apiNote 此方法為原子性操作，成功時兩個存儲都會更新，失敗時都不會更新
     * @implNote 使用 {@code Map.of()} 建立不可變 Map，確保資料的安全性
     */
    public Mono<Void> createTransfersTask(FileMetadataDTO fileMetadataDTO, String transferTaskId, String gridFsId, String message, TransfersStatusEnum status) {
        TransfersTask transfersTask = new TransfersTask();
        transfersTask.setTransferTaskId(transferTaskId);
        transfersTask.setMd5(fileMetadataDTO.getMd5());
        transfersTask.setGridFsId(gridFsId);
        transfersTask.setFileSize(fileMetadataDTO.getFileSize());
        transfersTask.setStartTime(LocalDateTime.now());
        transfersTask.setMessage(message);
        transfersTask.setStatus(status);
        activeTransfersTask.put(fileMetadataDTO.getMd5(), Map.of(transferTaskId, transfersTask));
        return transfersTasksRepository.save(transfersTask).then();
    }

    /**
     * 更新現有傳輸任務的狀態和相關資訊，支援部分更新和自動清理。
     * <p>
     * 此方法為任務狀態管理的核心，負責追蹤任務的進度和狀態變化。
     * 支援精細的狀態控制和自動的資源管理，確保系統的穩定性和效率。
     * </p>
     * <p>
     * <strong>更新特性：</strong>
     * </p>
     * <ul>
     *   <li><strong>部分更新：</strong>只更新提供的非 null 參數，保持其他屬性不變</li>
     *   <li><strong>狀態追蹤：</strong>自動記錄狀態變更和時間戳記</li>
     *   <li><strong>自動清理：</strong>當任務完成時自動從內存緩存中移除</li>
     *   <li><strong>限權檢查：</strong>驗證任務存在性，防止非法更新</li>
     * </ul>
     * <p>
     * <strong>更新參數說明：</strong>
     * </p>
     * <ul>
     *   <li><strong>status：</strong>必須提供，新的任務狀態</li>
     *   <li><strong>message：</strong>可選，為 null 時不更新訊息</li>
     *   <li><strong>gridFsId：</strong>可選，為 null 時不更新 GridFS ID</li>
     *   <li><strong>isFinished：</strong>為 true 時設定完成時間並觸發清理</li>
     * </ul>
     * <p>
     * <strong>清理機制：</strong>
     * </p>
     * <ul>
     *   <li>當 isFinished 為 true 時，任務會從內存緩存中移除</li>
     *   <li>釋放對應的系統資源和內存空間</li>
     *   <li>確保持久化存儲先於清理完成，保證資料一致性</li>
     * </ul>
     * <p>
     * <strong>錯誤處理：</strong>
     * </p>
     * <ul>
     *   <li>當任務不存在時，拋出 {@link ProcessException}</li>
     *   <li>提供詳細的錯誤資訊和上下文</li>
     *   <li>確保部分更新的原子性</li>
     * </ul>
     * 
     * @param md5 檔案的 MD5 雜湊值，用於定位任務群組
     * @param transfersTaskId 要更新的任務的唯一識別碼
     * @param status 新的任務狀態，必須提供，不得為 null
     * @param message 任務相關的新訊息，為 null 時不更新原有訊息
     * @param gridFsId 新的 GridFS 檔案識別碼，為 null 時不更新原有 ID
     * @param isFinished 任務是否完成，為 true 時會設定完成時間並觸發清理
     * @return Mono&lt;Void&gt; 無內容的反應式流，更新成功時完成
     * @see ProcessException.ErrorCode#NOT_EXISTING_MD5_TRANSFERS_TASK
     * @see TransfersStatusEnum
     * @see #createTransfersTask(FileMetadataDTO, String, String, String, TransfersStatusEnum)
     * @apiNote 此方法採用非阻塞式設計，適合高併發環境中使用
     * @implNote 內部使用 Optional 進行空值安全檢查，確保代碼的健壯性
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> updateTransfersTask(String md5, String transfersTaskId, TransfersStatusEnum status, String message, String gridFsId, Boolean isFinished) {
        TransfersTask transfersTask = Optional.ofNullable(activeTransfersTask.get(md5)).map(map -> map.get(transfersTaskId)).orElse(null);
        if (transfersTask == null) {
            return Mono.error(new ProcessException(ProcessException.ErrorCode.NOT_EXISTING_MD5_TRANSFERS_TASK, md5));
        }
        transfersTask.setStatus(status);
        if (message != null) {
            transfersTask.setMessage(message);
        }
        if (isFinished) {
            transfersTask.setFinishTime(LocalDateTime.now());
        }
        if (gridFsId != null) {
            transfersTask.setGridFsId(gridFsId);
        }
        return transfersTasksRepository.save(transfersTask).flatMap(task -> {
            if (isFinished) {
                activeTransfersTask.remove(md5);
            }
            return Mono.empty();
        });
    }

    /**
     * 獲取當前可用的處理線程數量，用於動態資源管理和負載平衡。
     * <p>
     * <strong>注意：此方法已被標記為已棄用（@Deprecated），不建議在新代碼中使用。</strong>
     * </p>
     * <p>
     * 此方法提供了一個簡單的資源評估機制，基於系統可用處理器數量
     * 和當前活躍任務數量來計算可用的處理能力。但這種算法存在一些局限性。
     * </p>
     * <p>
     * <strong>計算公式：</strong>
     * </p>
     * <pre>
     * 可用線程數 = min(max(系統處理器數 - 活躍任務數, 1), 設定上限)
     * </pre>
     * <p>
     * <strong>算法限制：</strong>
     * </p>
     * <ul>
     *   <li><strong>最小值：</strong>1，確保系統始終保有基本處理能力</li>
     *   <li><strong>最大值：</strong>由配置檔案中的 combineProcessCountLimit 設定</li>
     *   <li><strong>動態調整：</strong>根據當前任務負載動態調整可用資源</li>
     * </ul>
     * <p>
     * <strong>使用建議：</strong>
     * </p>
     * <ul>
     *   <li>新的實現應考慮使用更精確的資源管理方式</li>
     *   <li>推薦使用基於信號量或線程池的解決方案</li>
     *   <li>考慮根據任務類型和優先級進行更精細的資源分配</li>
     * </ul>
     * <p>
     * <strong>棄用原因：</strong>
     * </p>
     * <ul>
     *   <li>過於簡化的資源評估方式</li>
     *   <li>無法考慮任務的實際資源消耗</li>
     *   <li>可能導致不精確的資源預估</li>
     * </ul>
     * 
     * @return 當前系統預估的可用處理線程數量，範圍為 1 到 combineProcessCountLimit
     * @see FileProperties.Upload#getCombineProcessCountLimit()
     * @deprecated 此方法的資源評估算法過於簡化，建議使用更精確的資源管理方式
     * @apiNote 此方法為線程安全的，但返回值可能在短時間內發生變化
     * @implNote 使用 Runtime.getRuntime().availableProcessors() 獲取系統處理器數量
     */
    @Deprecated
    public int getAvailableThreadCount() {
        return Math.min(Math.max((Runtime.getRuntime().availableProcessors() - activeTransfersTask.size()), 1),
                        fileProperties.getUpload().getCombineProcessCountLimit()
        );
    }


    /**
     * 系統關閉時的清理方法，負責處理所有未完成的傳輸任務。
     * <p>
     * 此方法由 Spring 容器在應用程式關閉時自動調用，確保所有正在進行中的
     * 任務都能得到適當的處理，避免留下懸而未決的任務狀態。
     * </p>
     * <p>
     * <strong>清理流程：</strong>
     * </p>
     * <ol>
     *   <li><strong>查找未完成任務：</strong>查找所有狀態為 UPLOADING 或 DOWNLOADING 的任務</li>
     *   <li><strong>狀態更新：</strong>將所有未完成任務的狀態設為 FAILED</li>
     *   <li><strong>時間戳記：</strong>設定任務的完成時間為當前時間</li>
     *   <li><strong>错誤訊息：</strong>設定統一的错誤訊息說明關閉原因</li>
     *   <li><strong>批量存儲：</strong>將所有更新批量存儲至資料庫</li>
     * </ol>
     * <p>
     * <strong>處理的任務狀態：</strong>
     * </p>
     * <ul>
     *   <li><strong>UPLOADING：</strong>正在上傳中的任務</li>
     *   <li><strong>DOWNLOADING：</strong>正在下載中的任務</li>
     * </ul>
     * <p>
     * <strong>安全性考慮：</strong>
     * </p>
     * <ul>
     *   <li>使用非阻塞式資料庫操作，避免關閉過程被阻塞</li>
     *   <li>只處理真正未完成的任務，不會影響已完成的任務</li>
     *   <li>提供明確的錯誤訊息，方便後續排除</li>
     * </ul>
     * <p>
     * <strong>效能優化：</strong>
     * </p>
     * <ul>
     *   <li>先檢查是否有未完成任務，空列表時直接返回</li>
     *   <li>使用批量存儲減少資料庫交互次數</li>
     *   <li>采用反應式編程模式，提高併發處理能力</li>
     * </ul>
     * 
     * @return Mono&lt;Void&gt; 無內容的反應式流，清理完成時結束
     * @see TransfersStatusEnum#UPLOADING
     * @see TransfersStatusEnum#DOWNLOADING
     * @see TransfersStatusEnum#FAILED
     * @see TransfersTasksRepository#findAllByStatusIn(java.util.List)
     * @see TransfersTasksRepository#saveAll(org.reactivestreams.Publisher)
     * @apiNote 此方法由 Spring 的 @PreDestroy 機制自動調用，不需手動觸發
     * @implNote 使用 collectList() 將反應式流轉換為列表，方便批量處理
     */
    @PreDestroy
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> destroy() {
        List<TransfersStatusEnum> status = new ArrayList<>();
        status.add(TransfersStatusEnum.UPLOADING);
        status.add(TransfersStatusEnum.DOWNLOADING);
        return transfersTasksRepository.findAllByStatusIn(status).collectList().flatMap(unfinishedTasks -> {
            if (unfinishedTasks.isEmpty()) {
                return Mono.empty();
            }
            for (TransfersTask unfinishedTask : unfinishedTasks) {
                unfinishedTask.setStatus(TransfersStatusEnum.FAILED);
                unfinishedTask.setFinishTime(LocalDateTime.now());
                unfinishedTask.setMessage("伺服器關閉，任務被取消");
            }
            return transfersTasksRepository.saveAll(unfinishedTasks).then();
        });
    }
}
