package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;

/**
 * 可恢復檔案操作核心介面，定義檔案生命週期管理的完整恢復和刪除機制。
 * <p>
 * 本介面提供企業級的檔案回收站功能，支援檔案的軟刪除、恢復和永久移除操作。
 * 採用反應式編程模式確保在高並發環境下的卓越效能，特別適用於需要檔案版本控制、
 * 誤刪保護和資料安全管理的應用場景。
 * <p>
 * <strong>核心設計特性：</strong>
 * <ul>
 *   <li><strong>非阻塞操作：</strong>基於 Spring WebFlux 反應式編程，支援高併發處理</li>
 *   <li><strong>類型安全：</strong>泛型設計支援多種檔案類型和自定義檔案物件</li>
 *   <li><strong>權限控制：</strong>內建使用者權限驗證，確保操作安全性</li>
 *   <li><strong>批量處理：</strong>高效的批量恢復和刪除操作</li>
 *   <li><strong>錯誤隔離：</strong>單一檔案操作失敗不影響其他檔案處理</li>
 * </ul>
 * <p>
 * <strong>檔案生命週期管理：</strong>
 * <ul>
 *   <li><strong>軟刪除：</strong>檔案標記為已刪除但不立即移除，保留恢復可能</li>
 *   <li><strong>檔案恢復：</strong>將已刪除的檔案恢復到正常狀態</li>
 *   <li><strong>永久刪除：</strong>徹底移除檔案，無法恢復</li>
 *   <li><strong>批量操作：</strong>支援對多個檔案進行統一管理</li>
 * </ul>
 * <p>
 * <strong>權限驗證機制：</strong>
 * <ul>
 *   <li>每個操作都會驗證使用者的權限等級</li>
 *   <li>支援檔案所有者和管理員權限的分級管理</li>
 *   <li>權限不足時會拋出 {@code SecurityException}</li>
 *   <li>可與外部權限系統整合</li>
 * </ul>
 * <p>
 * <strong>效能最佳化：</strong>
 * <ul>
 *   <li><strong>並行處理：</strong>批量操作使用並行流處理，提升效能</li>
 *   <li><strong>懶載入：</strong>按需載入檔案資料，減少記憶體占用</li>
 *   <li><strong>非阻塞 I/O：</strong>避免執行緒阻塞，提升系統吞吐量</li>
 *   <li><strong>錯誤恢復：</strong>內建重試機制和降級處理</li>
 * </ul>
 * <p>
 * <strong>使用場景：</strong>
 * <ul>
 *   <li><strong>文檔管理系統：</strong>支援檔案版本控制和回收站功能</li>
 *   <li><strong>資料備份恢復：</strong>實現檔案的安全備份和快速恢復</li>
 *   <li><strong>多用戶檔案系統：</strong>支援多用戶環境下的檔案管理</li>
 *   <li><strong>雲端儲存服務：</strong>提供企業級的檔案生命週期管理</li>
 * </ul>
 * <p>
 * <strong>完整使用範例：</strong>
 * <pre>{@code
 * // 注入服務
 * @Autowired
 * private RecoverableFile<UserFileMetadata> fileRecoveryService;
 * 
 * // 單一檔案恢復
 * public Mono<UserFileMetadata> recoverSingleFile(Long fileId, User user) {
 *     return fileRepository.findById(fileId)
 *         .filter(file -> file.getIsDeleted())
 *         .switchIfEmpty(Mono.error(new IllegalStateException("檔案未被刪除")))
 *         .flatMap(file -> fileRecoveryService.restoreFile(file, user))
 *         .doOnSuccess(recovered -> log.info("檔案恢復成功: {}", recovered.getFilename()));
 * }
 * 
 * // 批量檔案恢復
 * public Flux<UserFileMetadata> recoverMultipleFiles(List<Long> fileIds, User user) {
 *     return fileRepository.findAllById(fileIds)
 *         .filter(file -> file.getIsDeleted())
 *         .collectList()
 *         .flatMapMany(files -> fileRecoveryService.restoreFile(files, user))
 *         .doOnNext(recovered -> log.info("檔案已恢復: {}", recovered.getFilename()))
 *         .doOnComplete(() -> log.info("所有檔案恢復完成"));
 * }
 * 
 * // 永久刪除檔案
 * public Mono<Boolean> permanentlyDeleteFile(Long fileId, User user) {
 *     return fileRepository.findById(fileId)
 *         .flatMap(file -> fileRecoveryService.removeFile(file, user))
 *         .doOnSuccess(result -> {
 *             if (result) {
 *                 log.info("檔案永久刪除成功");
 *             } else {
 *                 log.warn("檔案刪除失敗");
 *             }
 *         });
 * }
 * 
 * // 清理回收站（批量永久刪除）
 * public Mono<Boolean> cleanupTrash(User user) {
 *     return fileRepository.findDeletedFilesByUser(user.getId())
 *         .collectList()
 *         .flatMap(deletedFiles -> fileRecoveryService.removeFile(deletedFiles, user))
 *         .doOnSuccess(result -> log.info("回收站清理完成"));
 * }
 * 
 * // 錯誤處理範例
 * public Mono<UserFileMetadata> safeFileRecover(UserFileMetadata file, User user) {
 *     return fileRecoveryService.restoreFile(file, user)
 *         .onErrorResume(SecurityException.class, ex -> {
 *             log.warn("用戶 {} 無權恢復檔案 {}: {}", user.getUsername(), file.getFilename(), ex.getMessage());
 *             return Mono.error(new AccessDeniedException("檔案恢復權限不足"));
 *         })
 *         .onErrorResume(Exception.class, ex -> {
 *             log.error("檔案恢復過程中發生錯誤: {}", ex.getMessage(), ex);
 *             return Mono.error(new RuntimeException("檔案恢復失敗", ex));
 *         });
 * }
 * }</pre>
 * <p>
 * <strong>實作建議：</strong>
 * <ul>
 *   <li>實作類別應考慮實現檔案恢復的事務性，確保操作的原子性</li>
 *   <li>批量操作應實現並行處理以提升效能</li>
 *   <li>建議實現檔案恢復的審計日誌功能</li>
 *   <li>考慮實現檔案恢復的階段性檢查點</li>
 *   <li>永久刪除操作應有額外的安全確認機制</li>
 * </ul>
 * <p>
 * <strong>注意事項：</strong>
 * <ul>
 *   <li>所有操作都是非同步的，需要正確處理反應式流</li>
 *   <li>權限檢查失敗會拋出 SecurityException，需要適當處理</li>
 *   <li>批量操作中部分失敗不會影響其他檔案的處理</li>
 *   <li>永久刪除操作是不可逆的，應謹慎使用</li>
 * </ul>
 *
 * @param <T> 可恢復的檔案類型，通常是檔案元資料類別或檔案實體類別
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see reactor.core.publisher.Mono
 * @see reactor.core.publisher.Flux
 * @see xyz.dowob.filemanagement.entity.User
 * @see SecurityException
 */

public interface RecoverableFile<T> {
    /**
     * 恢復單一檔案。提供非阻塞的單一檔案恢復操作，採用反應式編程模式，當使用者缺乏恢復檔案的權限時拋出 SecurityException。
     * <p>
     * 範例用法：
     * <pre>
     * {@code
     * recoverableFile.restoreFile(fileToRecover, currentUser)
     *     .map(recoveredFile -> {
     *         // 處理恢復後的檔案
     *         return recoveredFile;
     *     })
     *     .subscribe();
     * }
     * </pre>
     *
     * @param file 待恢復的檔案物件
     * @param user 執行恢復操作的使用者
     * @return 恢復成功後的檔案 {@code Mono} 流，代表非阻塞的恢復結果
     * @throws SecurityException 當使用者缺乏恢復檔案的權限時拋出
     */
    Mono<T> restoreFile(T file, User user);


    /**
     * 批次恢復多個檔案。支援同時恢復多個檔案，採用反應式串流（Reactive Streams）處理，當使用者缺乏部分或全部檔案的恢復權限時拋出 SecurityException。
     * <p>
     * 範例用法：
     * <pre>
     * {@code
     * recoverableFile.restoreFile(filesToRecover, currentUser)
     *     .doOnNext(recoveredFile -> {
     *         // 處理每個成功恢復的檔案
     *     })
     *     .doOnComplete(() -> {
     *         // 所有檔案恢復完成
     *     })
     *     .subscribe();
     * }
     * </pre>
     *
     * @param files 待恢復的檔案集合
     * @param user 執行批次恢復操作的使用者
     * @return 恢復成功的檔案 {@code Flux} 串流，每個元素代表一個已恢復的檔案
     * @throws SecurityException 當使用者缺乏部分或全部檔案的恢復權限時拋出
     */
    Flux<T> restoreFile(Iterable<T> files, User user);

    /**
     * 刪除單一檔案。執行非阻塞的單一檔案刪除作業，確保在反應式環境中高效處理，當使用者缺乏刪除檔案的權限時拋出 SecurityException。
     * <p>
     * 範例用法：
     * <pre>
     * {@code
     * recoverableFile.removeFile(fileToDelete, currentUser)
     *     .filter(Boolean::booleanValue)
     *     .switchIfEmpty(Mono.error(new RuntimeException("檔案刪除失敗")))
     *     .subscribe();
     * }
     * </pre>
     *
     * @param file 待刪除的檔案物件
     * @param user 執行刪除操作的使用者
     * @return 表示刪除作業結果的 {@code Mono<Boolean>}，{@code true} 代表刪除成功
     * @throws SecurityException 當使用者缺乏刪除檔案的權限時拋出
     */
    Mono<Boolean> removeFile(T file, User user);

    /**
     * 批次刪除多個檔案。提供同時刪除多個檔案的非阻塞操作，支援完整的反應式刪除流程，當使用者缺乏部分或全部檔案的刪除權限時拋出 SecurityException。
     * <p>
     * 範例用法：
     * <pre>
     * {@code
     * recoverableFile.removeFile(filesToDelete, currentUser)
     *     .doOnSuccess(deleteResult -> {
     *         if (deleteResult) {
     *             log.info("所有檔案刪除成功");
     *         } else {
     *             log.warn("部分檔案刪除失敗");
     *         }
     *     })
     *     .subscribe();
     * }
     * </pre>
     *
     * @param files 待刪除的檔案集合
     * @param user 執行批次刪除操作的使用者
     * @return 表示刪除作業結果的 {@code Mono<Boolean>}，{@code true} 代表所有檔案刪除成功
     * @throws SecurityException 當使用者缺乏部分或全部檔案的刪除權限時拋出
     */
    Mono<Boolean> removeFile(Iterable<T> files, User user);


}
