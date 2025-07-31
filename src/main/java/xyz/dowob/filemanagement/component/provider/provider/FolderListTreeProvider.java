package xyz.dowob.filemanagement.component.provider.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.DynamicThreadPoolExecutor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 基於記憶體的檔案樹結構管理提供者，為用戶檔案列表提供高效能的樹形資料結構操作。
 *
 * <p>此類別實現非阻塞的檔案樹管理機制，將檔案目錄結構以樹形方式組織在記憶體中，
 * 大幅提升檔案列表查詢與操作的響應速度。支援動態樹結構維護、深度限制檢查及循環引用防護。</p>
 *
 * <p>主要功能包含：檔案夾新增、更新、刪除、路徑查詢及深度計算。
 * 內建執行緒池支援非同步批次操作，確保大量檔案操作時的系統穩定性。
 * 透過 ConcurrentHashMap 實現執行緒安全的並發存取。</p>
 *
 * <p>此服務僅在設定 {@code file.global.enable-user-folder-list-tree=true} 時啟用，預設為啟用狀態。
 * 支援最大檔案夾深度限制，防止無限遞迴結構。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@Component
@RecordLevel(LogLevelEnum.DEBUG)
@ConditionalOnProperty(name = {"file.global.enable-user-folder-list-tree"}, havingValue = "true", matchIfMissing = true)
public class FolderListTreeProvider {
    /**
     * 動態執行緒池執行器，專用於處理資料夾樹的非同步操作任務。
     * 配置為核心執行緒數 2，最大執行緒數 4，閒置時間 60 秒，佇列容量 1024。
     */
    private static final DynamicThreadPoolExecutor threadPoolExecutor = new DynamicThreadPoolExecutor(2,
                                                                                                      4,
                                                                                                      60,
                                                                                                      TimeUnit.SECONDS,
                                                                                                      new LinkedBlockingQueue<>(1024)
    );

    /**
     * 基於 ConcurrentHashMap 的使用者檔案樹對映表，鍵為使用者 ID，值為對應的檔案樹結構。
     * 提供執行緒安全的並發存取能力，支援多使用者同時操作各自的檔案樹。
     */
    private final Map<Long, FolderTree> userFileListTree;

    /**
     * 最大資料夾深度限制，用於防止無限遞迴結構和過深的目錄階層。
     * 當值小於等於 0 時表示不限制深度。
     */
    private final int maxFolderDepth;


    /**
     * 建構檔案樹提供者，從檔案屬性配置中載入最大深度限制並初始化執行緒安全的使用者檔案樹對映表。
     *
     * @param fileProperties 檔案系統屬性配置，包含最大資料夾深度等設定
     */
    public FolderListTreeProvider(FileProperties fileProperties) {
        this.maxFolderDepth = fileProperties.getGlobal().getMaxFolderDepth();
        this.userFileListTree = new ConcurrentHashMap<>();
    }


    /**
     * 將新資料夾新增至指定使用者的檔案樹結構中，自動處理父子關係建立和深度驗證。
     * 
     * <p>若使用者檔案樹不存在則自動建立，僅處理資料夾類型的檔案元資料。
     * 執行循環引用檢查和深度限制驗證以確保樹結構完整性。</p>
     *
     * @param userId           使用者唯一識別碼
     * @param userFileMetadata 包含資料夾資訊的檔案元資料
     * @throws ValidationException 當資料夾深度超過系統設定的最大深度限制時
     * @throws ProcessException    當父資料夾不存在或樹結構存在循環引用時
     */
    public void addFolder(Long userId, UserFileMetadata userFileMetadata) throws ProcessException, ValidationException {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree(maxFolderDepth));
        folderTree.addFolder(userFileMetadata.getFileType() == FileEnum.FOLDER,
                             userFileMetadata.getId(),
                             userFileMetadata.getParentFolderId(),
                             userFileMetadata.getFilename()
        );
    }


    /**
     * 批次新增多個資料夾至指定使用者的檔案樹中，按序處理每個檔案元資料。
     * 
     * <p>此方法適用於大量資料夾的初始化場景，內部呼叫單一新增方法處理每個項目。
     * 任一資料夾新增失敗將中斷整個批次操作。</p>
     *
     * @param userId               使用者唯一識別碼
     * @param userFileMetadataList 包含多個資料夾資訊的檔案元資料清單
     * @throws ValidationException 當任一資料夾深度超過系統設定的最大深度限制時
     * @throws ProcessException    當任一父資料夾不存在或樹結構存在循環引用時
     */
    public void addFolders(Long userId, List<UserFileMetadata> userFileMetadataList) throws ProcessException, ValidationException {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree(maxFolderDepth));
        for (UserFileMetadata userFileListDTO : userFileMetadataList) {
            folderTree.addFolder(userFileListDTO.getFileType() == FileEnum.FOLDER,
                                 userFileListDTO.getId(),
                                 userFileListDTO.getParentFolderId(),
                                 userFileListDTO.getFilename()
            );
        }
    }


    /**
     * 取得或建立指定使用者的檔案樹結構，確保每個使用者都有可用的檔案樹實例。
     * 
     * <p>使用 computeIfAbsent 原子操作保證執行緒安全，若檔案樹不存在則自動建立新實例。</p>
     *
     * @param userId 使用者唯一識別碼
     * @return 使用者的檔案樹實例，若原本不存在則為新建立的樹結構
     */
    public FolderTree createAndGetFileTree(Long userId) {
        return userFileListTree.computeIfAbsent(userId, k -> new FolderTree(maxFolderDepth));
    }


    /**
     * 取得指定使用者的檔案樹結構，不進行自動建立操作。
     *
     * @param userId 使用者唯一識別碼
     * @return 使用者的檔案樹實例，若不存在則返回 null
     */
    public FolderTree getFileTree(Long userId) {
        return userFileListTree.get(userId);
    }


    /**
     * 使用提供的資料夾清單初始化使用者檔案樹，支援分頁載入和完整性驗證。
     * 
     * <p>當 isLastPage 為 true 時執行完整性檢查，驗證所有待處理節點的父資料夾是否存在，
     * 並啟動非同步樹結構同步作業以最佳化樹狀態。</p>
     *
     * @param userId     使用者唯一識別碼
     * @param folderList 用於建構檔案樹的資料夾資訊清單
     * @param isLastPage 是否為分頁載入的最後一頁，決定是否執行完整性驗證
     * @throws ProcessException    當父資料夾引用無效或樹結構驗證失敗時
     * @throws ValidationException 當資料夾深度超過系統設定的最大深度限制時
     */
    @RecordLevel(LogLevelEnum.INFO)
    public void initializeTree(Long userId, List<UserFileListDTO> folderList, boolean isLastPage) throws ProcessException, ValidationException {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree(maxFolderDepth));
        folderTree.initializeTree(folderList, isLastPage);
    }


    /**
     * 更新指定資料夾的屬性，包括名稱變更和父資料夾移動操作。
     * 
     * <p>自動偵測是否為移動操作，若涉及父資料夾變更則執行循環引用檢查和深度驗證。
     * 僅處理資料夾類型的檔案，忽略一般檔案的更新請求。</p>
     *
     * @param userId           使用者唯一識別碼
     * @param userFileMetadata 現有的資料夾元資料
     * @param editDTO          包含更新資訊的編輯資料傳輸物件
     * @throws ValidationException 當移動後的深度超過系統設定的最大深度限制時
     * @throws ProcessException    當移動操作會造成循環引用時
     */
    public void updateFolder(Long userId, UserFileMetadata userFileMetadata, FileEditDTO editDTO) throws ValidationException, ProcessException {
        boolean isMoveFolder = !Objects.equals(editDTO.getParentFolderId(), userFileMetadata.getParentFolderId());
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            folderTree.updateFolder(userFileMetadata.getFileType() == FileEnum.FOLDER,
                                    userFileMetadata.getId(),
                                    editDTO.getFilename(),
                                    editDTO.getParentFolderId(),
                                    isMoveFolder
            );
        }
    }


    /**
     * 以非同步方式刪除指定資料夾及其所有子資料夾，避免阻塞主執行緒。
     * 
     * <p>使用廣度優先搜尋遍歷整個子樹結構，透過執行緒池執行刪除作業。
     * 自動清理所有相關的父子關係和對映表項目。</p>
     *
     * @param userId           使用者唯一識別碼
     * @param folderMetadataId 待刪除的資料夾元資料識別碼
     */
    public void deleteFolder(Long userId, Long folderMetadataId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            folderTree.deleteFolder(folderMetadataId);
        }
    }


    /**
     * 以同步方式立即刪除指定資料夾及其所有子資料夾，確保操作完成才返回。
     * 
     * <p>不使用執行緒池，直接在當前執行緒中執行刪除作業。
     * 適用於需要立即確認刪除結果的場景。</p>
     *
     * @param userId           使用者唯一識別碼
     * @param folderMetadataId 待刪除的資料夾元資料識別碼
     */
    public void deleteFolderSync(Long userId, Long folderMetadataId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            folderTree.deleteFolderSync(folderMetadataId);
        }
    }


    /**
     * 取得從根目錄到指定資料夾的完整路徑節點序列。
     * 
     * <p>返回的清單包含從目標資料夾到根節點的所有父節點，
     * 若使用者檔案樹不存在則返回僅含根節點的預設路徑。</p>
     *
     * @param userId   使用者唯一識別碼
     * @param folderId 目標資料夾識別碼
     * @return 從目標資料夾到根節點的路徑節點清單，若樹不存在則返回根節點
     */
    public List<FolderNode> getPath(Long userId, Long folderId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            return folderTree.getPath(folderId);
        }
        return Collections.singletonList(new FolderNode(0L, "root"));
    }


    /**
     * 計算指定資料夾相對於根目錄的深度層級。
     * 
     * <p>深度計算基於路徑長度減一，根目錄深度為 0。
     * 若使用者檔案樹不存在則返回 0。</p>
     *
     * @param userId   使用者唯一識別碼
     * @param folderId 目標資料夾識別碼
     * @return 資料夾深度，根目錄為 0，若樹不存在則返回 0
     */
    public int getFolderDepth(Long userId, Long folderId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            return folderTree.getPath(folderId).size() - 1;
        }
        return 0;
    }


    /**
     * 資料夾節點實體類別，表示檔案樹中的單一資料夾節點。
     * 
     * <p>每個節點包含識別碼、名稱、父子關係及深度資訊。
     * 使用 ConcurrentHashMap 儲存子節點以支援並發存取，
     * JsonIgnore 註解避免序列化時的循環引用問題。</p>
     *
     * @author yuan
     * @version 1.0
     * @since 1.0
     */
    @Getter
    @Setter
    public static class FolderNode {
        /**
         * 資料夾唯一識別碼，與資料庫中的檔案元資料主鍵對應。
         */
        private Long folderId;

        /**
         * 資料夾顯示名稱，用於使用者介面展示和路徑構建。
         */
        private String name;

        /**
         * 父資料夾節點參考，根節點的父節點為 null。
         * JsonIgnore 避免序列化時產生循環引用。
         */
        @JsonIgnore
        private FolderNode parentFolder;

        /**
         * 子資料夾節點對映表，鍵為子資料夾 ID，值為對應的節點實例。
         * 使用 ConcurrentHashMap 確保多執行緒環境下的安全存取。
         * JsonIgnore 避免序列化時產生循環引用。
         */
        @JsonIgnore
        private Map<Long, FolderNode> children;

        /**
         * 當前節點相對於根節點的深度層級，根節點深度為 0。
         */
        private int currentDepth = 0;

        /**
         * 以此節點為根的子樹中最大深度值，用於深度限制檢查和樹結構最佳化。
         */
        private int maxSubTreeDepth = 0;


        /**
         * 建構資料夾節點，設定基本識別碼和名稱，初始化空的子節點對映表。
         *
         * @param folderId 資料夾唯一識別碼
         * @param name     資料夾顯示名稱
         */
        public FolderNode(Long folderId, String name) {
            this.folderId = folderId;
            this.name = name;
            this.children = new ConcurrentHashMap<>();
        }


        /**
         * 從檔案元資料建構資料夾節點，提取識別碼和檔案名稱。
         *
         * @param userFileMetadata 包含資料夾資訊的檔案元資料
         */
        public FolderNode(UserFileMetadata userFileMetadata) {
            this.folderId = userFileMetadata.getId();
            this.name = userFileMetadata.getFilename();
            this.children = new ConcurrentHashMap<>();
        }


        /**
         * 重新計算並更新此節點子樹的最大深度值。
         * 
         * <p>透過比較所有直接子節點的最大子樹深度，
         * 取最大值加一作為當前節點的最大子樹深度。
         * 若無子節點則設為 0。</p>
         */
        public void updateMaxSubtreeDepth() {
            if (this.children.isEmpty()) {
                this.maxSubTreeDepth = 0;
                return;
            }

            this.maxSubTreeDepth = this.children
                    .values()
                    .stream()
                    .max(Comparator.comparingInt(FolderNode::getMaxSubTreeDepth))
                    .map(node -> node.getMaxSubTreeDepth() + 1)
                    .orElse(0);
        }
    }


    /**
     * 資料夾樹結構管理類別，維護完整的檔案目錄階層關係。
     * 
     * <p>包含根節點、全域節點對映表和待處理節點佇列。
     * 支援動態樹結構維護、深度限制檢查及循環引用防護。
     * 透過 ConcurrentHashMap 實現多執行緒安全的節點管理。</p>
     *
     * @author yuan
     * @version 1.0
     * @since 1.0
     */
    public static class FolderTree {
        /**
         * 檔案樹根節點，識別碼為 0，名稱為 "root"，作為所有頂層資料夾的父節點。
         */
        private final FolderNode root;

        /**
         * 全域資料夾節點對映表，提供資料夾 ID 到節點實例的快速查詢。
         * 使用 ConcurrentHashMap 確保多執行緒環境下的安全存取。
         */
        private final Map<Long, FolderNode> folderMap;

        /**
         * 待處理節點佇列，暫存父節點尚未加入樹中的子節點。
         * 鍵為父節點 ID，值為等待連結的子節點清單。
         */
        private final Map<Long, List<FolderNode>> pendingNodes;

        /**
         * 最大資料夾深度限制值，超過此深度的新增或移動操作將被拒絕。
         * 值小於等於 0 時表示不限制深度，允許任意深度的目錄結構。
         */
        private final int maxFolderDepthLimit;


        /**
         * 建構資料夾樹，初始化根節點和各種對映表結構。
         *
         * @param maxFolderDepthLimit 最大資料夾深度限制，小於等於 0 表示無限制
         */
        public FolderTree(int maxFolderDepthLimit) {
            this.root = new FolderNode(0L, "root");
            this.pendingNodes = new ConcurrentHashMap<>();
            this.folderMap = new ConcurrentHashMap<>();
            this.folderMap.put(0L, this.root);
            this.maxFolderDepthLimit = maxFolderDepthLimit;
        }


        /**
         * 新增資料夾節點至檔案樹中，處理父子關係建立和完整性檢查。
         * 
         * <p>執行循環引用檢測、深度限制驗證，並自動處理待處理節點的連結。
         * 僅處理資料夾類型的檔案，忽略一般檔案。</p>
         *
         * @param isFolder       檔案類型標識，僅當為 true 時執行新增操作
         * @param folderId       資料夾唯一識別碼
         * @param parentFolderId 父資料夾識別碼，null 時預設為根節點
         * @param filename       資料夾顯示名稱
         * @throws ValidationException 當新增後深度超過最大深度限制時
         * @throws ProcessException    當操作會造成循環引用時
         */

        private void addFolder(Boolean isFolder, Long folderId, Long parentFolderId, String filename) throws ValidationException, ProcessException {
            if (!isFolder) {
                return;
            }

            parentFolderId = Objects.requireNonNullElse(parentFolderId, 0L);
            if (hasCircularReference(parentFolderId, folderId)) {
                throw new ProcessException(ProcessException.ErrorCode.FOLDER_TREE_EXISTING_CYCLE);
            }

            FolderNode parentFolder = folderMap.get(parentFolderId);

            int maxFolderDepth = Objects.requireNonNullElse(parentFolder, root).getCurrentDepth() + 1;
            if (parentFolder != null && maxFolderDepthLimit > 0 && maxFolderDepth > maxFolderDepthLimit) {
                throw new ValidationException(ValidationException.ErrorCode.EXCEED_MAX_FOLDER_DEPTH, maxFolderDepthLimit, maxFolderDepth);
            }

            FolderNode folderNode = new FolderNode(folderId, filename);
            folderMap.put(folderId, folderNode);

            if (parentFolder != null) {
                linkNodes(parentFolder, folderNode);
            } else {
                pendingNodes.computeIfAbsent(parentFolderId, k -> new ArrayList<>()).add(folderNode);
            }

            List<FolderNode> children = pendingNodes.remove(folderId);
            if (children != null) {
                children.forEach(child -> linkNodes(folderNode, child));
            }
        }


        /**
         * 檢測將資料夾移動至新父節點時是否會造成循環引用。
         * 
         * <p>從新父節點開始向上遍歷，檢查路徑中是否包含當前資料夾，
         * 若包含則表示會形成循環結構。</p>
         *
         * @param newParentId     目標父資料夾識別碼
         * @param currentFolderId 當前資料夾識別碼
         * @return 若會形成循環引用則返回 true，否則返回 false
         */
        private boolean hasCircularReference(Long newParentId, Long currentFolderId) {
            Set<Long> visited = new HashSet<>();
            newParentId = Objects.requireNonNullElse(newParentId, 0L);
            currentFolderId = Objects.requireNonNullElse(currentFolderId, 0L);
            visited.add(currentFolderId);

            long currentId = newParentId;
            while (currentId != 0L) {
                if (!visited.add(currentId)) {
                    return true;
                }
                FolderNode parent = folderMap.get(currentId);
                if (parent == null) {
                    return false;
                }
                currentId = parent.getParentFolder() != null ? parent.getParentFolder().getFolderId() : 0L;
            }
            return false;
        }


        /**
         * 非同步刪除指定資料夾及其完整子樹結構。
         * 
         * <p>使用廣度優先搜尋遍歷所有子節點，透過執行緒池執行刪除作業以避免阻塞。
         * 自動清理節點間的父子關係和對映表項目。</p>
         *
         * @param folderId 待刪除的資料夾識別碼
         */
        private void deleteFolder(Long folderId) {
            FolderNode node = folderMap.get(folderId);
            if (node == null) {
                return;
            }
            ConcurrentLinkedQueue<FolderNode> toDelete = new ConcurrentLinkedQueue<>();
            toDelete.offer(node);


            threadPoolExecutor.submit(() -> {
                while (!toDelete.isEmpty()) {
                    FolderNode current = toDelete.poll();
                    if (current == null) {
                        continue;
                    }
                    removeSubtree(current, toDelete);
                }
            });
        }


        /**
         * 遞迴移除指定節點的完整子樹結構，清理所有相關聯結。
         * 
         * <p>將所有子節點加入刪除佇列，從父節點移除當前節點，
         * 清除對映表項目和待處理節點記錄，重置節點的父子關係。</p>
         *
         * @param node     待移除的根節點
         * @param toDelete 用於批次刪除的節點佇列
         */
        private void removeSubtree(FolderNode node, Queue<FolderNode> toDelete) {
            for (FolderNode child : node.getChildren().values()) {
                toDelete.offer(child);
            }

            FolderNode parent = node.getParentFolder();
            if (parent != null) {
                parent.getChildren().remove(node.getFolderId());
            }

            folderMap.remove(node.getFolderId());
            pendingNodes.remove(node.getFolderId());
            node.getChildren().clear();
            node.setParentFolder(null);
        }


        /**
         * 同步刪除指定資料夾及其完整子樹結構，立即執行不使用執行緒池。
         * 
         * <p>在當前執行緒中直接執行刪除作業，確保操作完成後才返回。
         * 適用於需要立即確認刪除結果的場景。</p>
         *
         * @param folderId 待刪除的資料夾識別碼
         */
        private void deleteFolderSync(Long folderId) {
            FolderNode node = folderMap.get(folderId);
            if (node == null) {
                return;
            }
            ConcurrentLinkedQueue<FolderNode> toDelete = new ConcurrentLinkedQueue<>();
            toDelete.offer(node);

            while (!toDelete.isEmpty()) {
                FolderNode current = toDelete.poll();
                if (current == null) {
                    continue;
                }
                removeSubtree(current, toDelete);
            }
        }


        /**
         * 使用提供的資料夾清單批次建構檔案樹結構，支援分頁載入和完整性驗證。
         * 
         * <p>逐一處理清單中的資料夾項目，當 isLastPage 為 true 時執行完整性檢查，
         * 驗證所有待處理節點的父資料夾引用，並啟動非同步樹同步作業。</p>
         *
         * @param folderList 用於建構樹結構的資料夾資訊清單
         * @param isLastPage 是否為分頁載入的最後一頁，決定是否執行驗證和同步
         * @throws ValidationException 當任一資料夾深度超過最大深度限制時
         * @throws ProcessException    當存在無效的父資料夾引用或樹結構驗證失敗時
         */
        @RecordLevel(LogLevelEnum.INFO)
        private void initializeTree(List<UserFileListDTO> folderList, boolean isLastPage) throws ProcessException, ValidationException {
            for (UserFileListDTO folder : folderList) {
                if (folder.getFileType() == FileEnum.FOLDER) {
                    addFolder(true, folder.getId(), folder.getParentFolderId(), folder.getFilename());
                }
            }

            if (isLastPage) {
                for (Long parentId : pendingNodes.keySet()) {
                    if (!folderMap.containsKey(parentId)) {
                        throw new ProcessException(ProcessException.ErrorCode.BUILD_FILE_TREE_FAILED, "無效的 parentFolderId: " + parentId);
                    }
                }
                if (!validateTree()) {
                    throw new ProcessException(ProcessException.ErrorCode.BUILD_FILE_TREE_FAILED, "初始化資料夾樹失敗，存在無效的節點");
                }
                threadPoolExecutor.submit(() -> synchronizeTree(root));
            }
        }


        /**
         * 更新現有資料夾節點的屬性，支援名稱變更和父節點移動操作。
         * 
         * <p>若為移動操作則執行循環引用檢查和深度驗證，
         * 重新建立父子關係並處理相關的待處理節點。完成後啟動非同步樹同步。</p>
         *
         * @param isFolder     檔案類型標識，僅當為 true 時執行更新操作
         * @param folderId     待更新的資料夾識別碼
         * @param filename     新的資料夾名稱
         * @param newParentId  新的父資料夾識別碼
         * @param isMoveFolder 是否涉及父節點移動操作
         * @throws ValidationException 當移動後深度超過最大深度限制時
         * @throws ProcessException    當移動操作會造成循環引用時
         */
        private void updateFolder(Boolean isFolder, Long folderId, String filename, Long newParentId, boolean isMoveFolder) throws ValidationException, ProcessException {
            FolderNode node = folderMap.get(folderId);
            if (node == null || !isFolder) {
                return;
            }

            if (!isMoveFolder) {
                node.setName(filename);
                return;
            }

            newParentId = Objects.requireNonNullElse(newParentId, 0L);
            FolderNode newParent = folderMap.get(newParentId);

            if (newParent != null) {
                if (hasCircularReference(newParent.getFolderId(), folderId)) {
                    throw new ProcessException(ProcessException.ErrorCode.FOLDER_TREE_EXISTING_CYCLE);
                }

                int potentialDepth = newParent.getCurrentDepth() + 1 + node.getMaxSubTreeDepth();
                if (maxFolderDepthLimit > 0 && potentialDepth > maxFolderDepthLimit) {
                    throw new ValidationException(ValidationException.ErrorCode.EXCEED_MAX_FOLDER_DEPTH, maxFolderDepthLimit, potentialDepth);
                }
            }

            node.setName(filename);

            if (node.getChildren() != null) {
                for (FolderNode child : node.getChildren().values()) {
                    child.setParentFolder(node);
                }
            }

            FolderNode oldParent = node.getParentFolder();
            if (oldParent != null) {
                oldParent.getChildren().remove(folderId);
            }

            if (newParent != null) {
                linkNodes(newParent, node);
            } else {
                pendingNodes.computeIfAbsent(newParentId, k -> new ArrayList<>()).add(node);
            }

            List<FolderNode> children = pendingNodes.remove(node.getFolderId());
            if (children != null) {
                children.forEach(child -> linkNodes(node, child));
            }
            threadPoolExecutor.submit(() -> synchronizeTree(node));
        }


        /**
         * 建立父子節點間的雙向連結關係，更新深度資訊和對映表。
         * 
         * <p>設定子節點的父節點參考和深度值，遞迴更新所有後代節點深度，
         * 並重新計算祖先節點的最大子樹深度。</p>
         *
         * @param parent 父資料夾節點
         * @param child  待連結的子資料夾節點
         */
        @SkipRecord
        private void linkNodes(FolderNode parent, FolderNode child) {
            child.setParentFolder(parent);
            child.setCurrentDepth(parent.getCurrentDepth() + 1);

            updateChildrenDepths(child);

            parent.getChildren().put(child.getFolderId(), child);

            updateAncestorDepths(parent);

            folderMap.put(child.getFolderId(), child);
            folderMap.put(parent.getFolderId(), parent);
        }


        /**
         * 遞迴更新指定節點及其所有後代節點的深度值。
         * 
         * <p>基於父節點深度重新計算當前節點深度，並遞迴處理所有子節點。
         * 完成後更新節點的最大子樹深度資訊。</p>
         *
         * @param node 需要更新深度的根節點
         */
        private void updateChildrenDepths(FolderNode node) {
            for (FolderNode child : node.getChildren().values()) {
                child.setCurrentDepth(node.getCurrentDepth() + 1);
                updateChildrenDepths(child);
            }
            node.updateMaxSubtreeDepth();
        }


        /**
         * 向上遞迴更新指定節點到根節點路徑上所有祖先節點的最大子樹深度。
         * 
         * <p>從當前節點開始向上遍歷，重新計算每個祖先節點的最大子樹深度值。</p>
         *
         * @param node 起始更新的節點
         */
        private void updateAncestorDepths(FolderNode node) {
            FolderNode current = node;
            while (current != null) {
                current.updateMaxSubtreeDepth();
                current = current.getParentFolder();
            }
        }


        /**
         * 驗證整個檔案樹結構的完整性，檢查所有節點是否具有有效的識別碼。
         * 
         * <p>遍歷所有節點（除根節點外），確認每個節點都有非 null 的資料夾識別碼。</p>
         *
         * @return 若樹結構完整有效則返回 true，存在無效節點則返回 false
         */
        private boolean validateTree() {
            for (FolderNode node : folderMap.values()) {
                if (node == root) {
                    continue;
                }
                if (node.getFolderId() == null) {
                    return false;
                }
            }
            return true;
        }


        /**
         * 遞迴同步指定節點及其子樹到全域對映表，確保父子關係一致性。
         * 
         * <p>更新節點在對映表中的記錄，重新設定所有子節點的父節點參考，
         * 並遞迴處理整個子樹結構。</p>
         *
         * @param node 需要同步的根節點
         */
        private void synchronizeTree(FolderNode node) {
            folderMap.put(node.getFolderId(), node);

            if (node.getChildren() != null) {
                for (FolderNode child : node.getChildren().values()) {
                    child.setParentFolder(node);
                    synchronizeTree(child);
                }
            }
        }


        /**
         * 取得從指定資料夾到根節點的完整路徑節點序列。
         * 
         * <p>從目標節點開始向上遍歷到根節點，建構包含所有路徑節點的清單。</p>
         *
         * @param folderId 目標資料夾識別碼
         * @return 從目標資料夾到根節點的路徑節點清單
         */
        @SkipRecord
        private List<FolderNode> getPath(Long folderId) {
            List<FolderNode> path = new ArrayList<>();
            FolderNode current = folderMap.get(folderId);
            while (current != null) {
                path.add(current);
                current = current.getParentFolder();
            }
            return path;
        }
    }
}
