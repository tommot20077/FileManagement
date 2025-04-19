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
 * 用戶檔案列表樹提供者，用於提供用戶的檔案列表樹，用於加快檔案列表的查詢速度
 * 此類僅在啟用用戶檔案列表樹時才會初始化
 * 此配置在配置文件中設置 file.global.enable-user-folder-list-tree，默認為 true
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileListTreeManager
 * @create 2025/1/29
 * @Version 1.0
 **/
@Getter
@Component
@RecordLevel(LogLevelEnum.DEBUG)
@ConditionalOnProperty(name = {"file.global.enable-user-folder-list-tree"}, havingValue = "true", matchIfMissing = true)
public class FolderListTreeProvider {
    /**
     * 用戶檔案列表樹映射，用於存儲用戶的檔案列表樹
     */
    private final Map<Long, FolderTree> userFileListTree;

    /**
     * 最大資料夾深度
     */
    private final int maxFolderDepth;

    /**
     * FolderListTreeProvider 構造方法
     * 初始化用戶檔案列表樹映射
     */
    public FolderListTreeProvider(FileProperties fileProperties) {
        this.maxFolderDepth = fileProperties.getGlobal().getMaxFolderDepth();
        this.userFileListTree = new ConcurrentHashMap<>();
    }


    /**
     * 線程池執行器，用於執行線程池任務
     */
    private static final DynamicThreadPoolExecutor threadPoolExecutor = new DynamicThreadPoolExecutor(2,
                                                                                                      4,
                                                                                                      60,
                                                                                                      TimeUnit.SECONDS,
                                                                                                      new LinkedBlockingQueue<>(1024)
    );


    /**
     * 添加新的資料夾到用戶的檔案列表樹中
     *
     * @param userId           用戶ID
     * @param userFileMetadata 文件夾元數據
     *
     * @throws ValidationException 當資料夾深度超過最大深度時，拋出此異常
     * @throws ProcessException    初始化資料夾樹失敗，當父資料夾不存在時，拋出此異常
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
     * 添加新的資料夾到用戶的檔案列表樹中，用於批量添加
     *
     * @param userId               用戶ID
     * @param userFileMetadataList 文件夾元數據列表
     *
     * @throws ValidationException 當資料夾深度超過最大深度時，拋出此異常
     * @throws ProcessException    初始化資料夾樹失敗，當父資料夾不存在時，拋出此異常
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
     * 獲取用戶的檔案列表樹，當用戶的檔案列表樹不存在時，創建一個新的檔案列表樹
     *
     * @param userId 用戶ID
     *
     * @return 用戶的檔案列表樹
     */
    public FolderTree createAndGetFileTree(Long userId) {
        return userFileListTree.computeIfAbsent(userId, k -> new FolderTree(maxFolderDepth));
    }


    /**
     * 獲取用戶的檔案列表樹
     *
     * @param userId 用戶ID
     *
     * @return 用戶的檔案列表樹
     */
    public FolderTree getFileTree(Long userId) {
        return userFileListTree.get(userId);
    }


    /**
     * 初始化用戶的檔案列表樹
     *
     * @param userId     用戶ID
     * @param folderList 文件夾列表
     * @param isLastPage 是否為最後一頁
     *
     * @throws ProcessException    初始化資料夾樹失敗，當父資料夾不存在時，拋出此異常
     * @throws ValidationException 當資料夾深度超過最大深度時，拋出此異常
     */
    @RecordLevel(LogLevelEnum.INFO)
    public void initializeTree(Long userId, List<UserFileListDTO> folderList, boolean isLastPage) throws ProcessException, ValidationException {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree(maxFolderDepth));
        folderTree.initializeTree(folderList, isLastPage);
    }


    /**
     * 更新資料夾的父資料夾
     *
     * @param userId           用戶ID
     * @param userFileMetadata 資料夾元數據
     * @param editDTO          文件編輯DTO
     */
    public void updateFolder(Long userId, UserFileMetadata userFileMetadata, FileEditDTO editDTO) throws ValidationException {
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
     * 刪除資料夾
     *
     * @param userId           用戶ID
     * @param folderMetadataId 資料夾元數據ID
     */
    public void deleteFolder(Long userId, Long folderMetadataId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            folderTree.deleteFolder(folderMetadataId);
        }
    }


    /**
     * 獲取資料夾的路徑
     *
     * @param userId   用戶ID
     * @param folderId 資料夾ID
     *
     * @return 資料夾的路徑列表
     */
    public List<FolderNode> getPath(Long userId, Long folderId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            return folderTree.getPath(folderId);
        }
        return Collections.singletonList(new FolderNode(0L, "root"));
    }


    /**
     * 獲取資料夾的深度
     *
     * @param userId   用戶ID
     * @param folderId 資料夾ID
     *
     * @return 資料夾的深度
     */
    public int getFolderDepth(Long userId, Long folderId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            return folderTree.getPath(folderId).size() - 1;
        }
        return 0;
    }


    /**
     * 子類: 資料夾節點
     * 用於構建資料夾樹節點，包含資料夾ID、名稱、父資料夾、子資料夾
     */
    @Getter
    @Setter
    public static class FolderNode {
        /**
         * 資料夾ID
         */
        private Long folderId;

        /**
         * 資料夾名稱
         */
        private String name;

        /**
         * 父資料夾
         */
        @JsonIgnore
        private FolderNode parentFolder;

        /**
         * 子資料夾
         */
        @JsonIgnore
        private Map<Long, FolderNode> children;

        /**
         * 當前資料夾深度
         */
        private int currentDepth = 0;

        /**
         * 最大子資料夾深度
         */
        private int maxSubTreeDepth = 0;

        /**
         * FolderNode 構造方法
         *
         * @param folderId 資料夾ID
         * @param name     資料夾名稱
         */
        public FolderNode(Long folderId, String name) {
            this.folderId = folderId;
            this.name = name;
            this.children = new ConcurrentHashMap<>();
        }

        /**
         * FolderNode 構造方法
         *
         * @param userFileMetadata 文件元數據
         */
        public FolderNode(UserFileMetadata userFileMetadata) {
            this.folderId = userFileMetadata.getId();
            this.name = userFileMetadata.getFilename();
            this.children = new ConcurrentHashMap<>();
        }


        /**
         * 更新最大子資料夾深度
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
     * 子類: 資料夾樹
     * 用於構建資料夾樹，包含根節點、資料夾映射、待處理節點
     */
    public static class FolderTree {
        /**
         * 根節點
         */
        private final FolderNode root;

        /**
         * 資料夾映射
         */
        private final Map<Long, FolderNode> folderMap;

        /**
         * 待處理節點
         */
        private final Map<Long, List<FolderNode>> pendingNodes;

        /**
         * 最大資料夾深度
         */
        private final int maxFolderDepthLimit;

        /**
         * FolderTree 構造方法
         */
        public FolderTree(int maxFolderDepthLimit) {
            this.root = new FolderNode(0L, "root");
            this.pendingNodes = new ConcurrentHashMap<>();
            this.folderMap = new ConcurrentHashMap<>();
            this.folderMap.put(0L, this.root);
            this.maxFolderDepthLimit = maxFolderDepthLimit;
        }


        /**
         * 添加資料夾
         *
         * @param isFolder       是否為資料夾
         * @param folderId       資料夾ID
         * @param parentFolderId 父資料夾ID
         * @param filename       資料夾名稱
         *
         * @throws ValidationException 當資料夾深度超過最大深度時，拋出此異常
         * @throws ProcessException    當資料夾樹存在循環引用時，拋出此異常
         */

        private void addFolder(Boolean isFolder, Long folderId, Long parentFolderId, String filename) throws ValidationException, ProcessException {
            if (!isFolder) {
                return;
            }

            parentFolderId = Objects.requireNonNullElse(parentFolderId, 0L);
            if (hasCircularReference(parentFolderId)) {
                throw new ProcessException(ProcessException.ErrorCode.FOLDER_TREE_EXISTING_CYCLE);
            }

            FolderNode parentFolder = folderMap.get(parentFolderId);

            int maxFolderDepth = Objects.requireNonNullElse(parentFolder, root).getCurrentDepth() + 1;
            if (parentFolder != null && maxFolderDepth > maxFolderDepthLimit) {
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
         * 更新資料夾樹
         *
         * @param isFolder     是否為資料夾
         * @param folderId     資料夾ID
         * @param filename     資料夾名稱
         * @param newParentId  新的父資料夾ID
         * @param isMoveFolder 是否移動資料夾
         *
         * @throws ValidationException 當資料夾深度超過最大深度時，拋出此異常
         */
        private void updateFolder(Boolean isFolder, Long folderId, String filename, Long newParentId, boolean isMoveFolder) throws ValidationException {
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
                int potentialDepth = newParent.getCurrentDepth() + 1 + node.getMaxSubTreeDepth();
                if (potentialDepth > maxFolderDepthLimit) {
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
         * 刪除資料夾
         *
         * @param folderId 資料夾元數據ID
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
         * 移除子樹，將指定節點的子樹從資料夾映射中移除此操作用於批量刪除資料夾
         * 並清除連接的節點將指定的節點從父節點中移除並將父節點設置為空
         *
         * @param node 資料夾節點
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
         * 初始化資料夾樹，當用戶的資料夾樹不存在時，創建一個新的資料夾樹
         *
         * @param folderList 文件夾列表
         * @param isLastPage 是否為最後一頁
         *
         * @throws ValidationException 當資料夾深度超過最大深度時，拋出此異常
         * @throws ProcessException    初始化資料夾樹失敗，當父資料夾不存在時，拋出此異常
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
         * 檢查是否存在循環引用
         *
         * @param parentId 父資料夾ID
         *
         * @return 是否存在循環引用
         */
        private boolean hasCircularReference(long parentId) {
            Set<Long> visited = new HashSet<>();
            long currentId = parentId;
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
         * 連接節點，將子節點連接到父節點中
         *
         * @param parent 父資料夾節點
         * @param child  子資料夾節點
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
         * 更新子資料夾的深度
         *
         * @param node 資料夾節點
         */
        private void updateChildrenDepths(FolderNode node) {
            for (FolderNode child : node.getChildren().values()) {
                child.setCurrentDepth(node.getCurrentDepth() + 1);
                updateChildrenDepths(child);
            }
            node.updateMaxSubtreeDepth();
        }


        /**
         * 更新父節點的深度
         *
         * @param node 資料夾節點
         */
        private void updateAncestorDepths(FolderNode node) {
            FolderNode current = node;
            while (current != null) {
                current.updateMaxSubtreeDepth();
                current = current.getParentFolder();
            }
        }


        /**
         * 檢查樹結構是否有效，當樹結構中存在無效的節點時，返回 false
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
         * 同步樹，將資料夾樹同步到資料夾映射中
         *
         * @param node 資料夾節點
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
         * 獲取資料夾的路徑
         *
         * @param folderId 資料夾ID
         *
         * @return 資料夾的路徑列表
         */
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
