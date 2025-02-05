package xyz.dowob.filemanagement.component.provider.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ProcessException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 用戶檔案列表樹提供者，用於提供用戶的檔案列表樹，用於加快檔案列表的查詢速度
 * 此類僅在啟用用戶檔案列表樹時才會初始化
 * 此配置在配置文件中設置 file.global.enable-user-folder-list-tree，默認為 true
 * @author yuan
 * @program FileManagement
 * @ClassName FileListTreeManager
 * @create 2025/1/29
 * @Version 1.0
 **/
@Getter
@Component
@ConditionalOnProperty(name = {"file.global.enable-user-folder-list-tree"}, havingValue = "true", matchIfMissing = true)
public class FolderListTreeProvider {
    /**
     * 用戶檔案列表樹映射，用於存儲用戶的檔案列表樹
     */
    private final Map<Long, FolderTree> userFileListTree;

    public FolderListTreeProvider() {
        this.userFileListTree = new HashMap<>();
    }

    /**
     * 添加新的資料夾到用戶的檔案列表樹中
     *
     * @param userId          用戶ID
     * @param userFileListDTO 文件夾元數據
     */
    public void addFolder(Long userId, UserFileListDTO userFileListDTO) {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
        folderTree.addFolder(userFileListDTO);
    }

    /**
     * 添加新的資料夾到用戶的檔案列表樹中，用於批量添加
     * @param userId 用戶ID
     * @param userFileListDTOList 文件夾元數據列表
     */
    public void addFolders(Long userId, List<UserFileListDTO> userFileListDTOList) {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
        userFileListDTOList.forEach(folderTree::addFolder);
    }

    /**
     * 獲取用戶的檔案列表樹，當用戶的檔案列表樹不存在時，創建一個新的檔案列表樹
     * @param userId 用戶ID
     * @return 用戶的檔案列表樹
     */
    public FolderTree createAndGetFileTree(Long userId) {
        return userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
    }

    /**
     * 獲取用戶的檔案列表樹
     * @param userId 用戶ID
     * @return 用戶的檔案列表樹
     */
    public FolderTree getFileTree(Long userId) {
        return userFileListTree.get(userId);
    }

    /**
     * 初始化用戶的檔案列表樹
     * @param userId 用戶ID
     * @param folderList 文件夾列表
     */
    public void initializeTree(Long userId, List<UserFileListDTO> folderList) throws ProcessException {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
        folderTree.initializeTree(folderList);
    }

    /**
     * 更新資料夾的父資料夾
     * @param userId 用戶ID
     * @param folderMetadata 資料夾元數據
     * @param newParentId 新的父資料夾ID
     */
    public void updateFolder(Long userId, UserFileMetadata folderMetadata, Long newParentId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            folderTree.updateFolder(folderMetadata, newParentId);
        }
    }

    /**
     * 刪除資料夾
     * @param userId 用戶ID
     * @param folderMetadata 資料夾元數據
     */
    public void deleteFolder(Long userId, UserFileMetadata folderMetadata) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            folderTree.deleteFolder(folderMetadata);
        }
    }

    /**
     * 獲取資料夾的路徑
     * @param userId 用戶ID
     * @param folderId 資料夾ID
     * @return 資料夾的路徑
     */
    public List<FolderNode> getPath(Long userId, Long folderId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            return folderTree.getPath(folderId);
        }
        return Collections.singletonList(new FolderNode(null, "root"));
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
         * FolderNode 構造方法
         * @param folderId 資料夾ID
         * @param name 資料夾名稱
         */
        public FolderNode(Long folderId, String name) {
            this.folderId = folderId;
            this.name = name;
            this.children = new HashMap<>();
        }

        /**
         * FolderNode 構造方法
         * @param userFileMetadata 文件元數據
         */
        public FolderNode(UserFileMetadata userFileMetadata) {
            this.folderId = userFileMetadata.getId();
            this.name = userFileMetadata.getFilename();
            this.children = new HashMap<>();
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
         * FolderTree 構造方法
         */
        public FolderTree() {
            this.root = new FolderNode(null, "root");
            this.folderMap = new HashMap<>();
            this.folderMap.put(null, this.root);
            this.pendingNodes = new HashMap<>();
        }


        /**
         * 添加資料夾
         * @param metadata 資料夾元數據
         */
        private void updateFolder(UserFileMetadata metadata, Long newParentId) {
            FolderNode node = folderMap.get(metadata.getId());
            if (node == null) {
                return;
            }

            node.setName(metadata.getFilename());

            if (node.getChildren() != null) {
                for (FolderNode child : node.getChildren().values()) {
                    child.setParentFolder(node);
                }
            }

            FolderNode oldParent = node.getParentFolder();
            if (oldParent != null) {
                oldParent.getChildren().remove(metadata.getId());
            }

            FolderNode newParent = folderMap.get(newParentId);
            if (newParent != null) {
                linkNodes(newParent, node);
            } else {
                pendingNodes.computeIfAbsent(newParentId, k -> new ArrayList<>()).add(node);
            }

            List<FolderNode> children = pendingNodes.remove(node.getFolderId());
            if (children != null) {
                children.forEach(child -> linkNodes(node, child));
            }
            CompletableFuture.runAsync(() -> synchronizeTree(node));
        }

        /**
         * 刪除資料夾
         * @param metadata 資料夾元數據
         */
        private void deleteFolder(UserFileMetadata metadata) {
            FolderNode node = folderMap.get(metadata.getId());
            if (node == null) {
                return;
            }

            unlinkNode(node);
            removeSubtree(node);
        }

        /**
         * 獲取資料夾的路徑
         * @param folderId 資料夾ID
         * @return 資料夾的路徑
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

        /**
         * 清除連接的節點將指定的節點從父節點中移除並將父節點設置為空
         * @param node 資料夾節點
         */
        private void unlinkNode(FolderNode node) {
            FolderNode parent = node.getParentFolder();
            if (parent != null) {
                parent.getChildren().remove(node.getFolderId());
            }
            node.setParentFolder(null);
        }

        /**
         * 移除子樹，將指定節點的子樹從資料夾映射中移除此操作用於批量刪除資料夾
         * @param node 資料夾節點
         */
        private void removeSubtree(FolderNode node) {
            for (FolderNode child : new ArrayList<>(node.getChildren().values())) {
                removeSubtree(child);
            }
            folderMap.remove(node.getFolderId());
            pendingNodes.remove(node.getFolderId());
            node.getChildren().clear();
        }

        /**
         * 初始化資料夾樹，當用戶的資料夾樹不存在時，創建一個新的資料夾樹
         * @param folderList 文件夾列表
         * @throws ProcessException 初始化資料夾樹失敗，當父資料夾不存在時，拋出此異常
         */
        private void initializeTree(List<UserFileListDTO> folderList) throws ProcessException {
            folderList.stream().filter(UserFileListDTO::isFolder).forEach(this::addFolder);

            for (Long parentId : pendingNodes.keySet()) {
                if (!folderMap.containsKey(parentId)) {
                    throw new ProcessException(ProcessException.ErrorCode.BUILD_FILE_TREE_FAILED, "無效的 parentFolderId: " + parentId);
                }
            }
        }

        /**
         * 添加資料夾
         * @param userFileListDTO 文件夾元數據
         * @throws RuntimeException 添加資料夾失敗，當存在循環引用時，拋出此異常
         */
        private void addFolder(UserFileListDTO userFileListDTO) {
            if (!userFileListDTO.isFolder()) {
                return;
            }

            Long folderId = userFileListDTO.getId();
            Long parentFolderId = userFileListDTO.getParentFolderId();
            if (hasCircularReference(parentFolderId)) {
                throw new RuntimeException("存在循環引用");
            }

            FolderNode folderNode = new FolderNode(folderId, userFileListDTO.getFilename());
            folderMap.put(folderId, folderNode);

            FolderNode parentFolder = folderMap.get(parentFolderId);
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
         * 檢查是否存在循環引用
         * @param parentId 父資料夾ID
         * @return 是否存在循環引用
         */
        private boolean hasCircularReference(Long parentId) {
            Set<Long> visited = new HashSet<>();
            Long currentId = parentId;
            while (currentId != null) {
                if (!visited.add(currentId)) {
                    return true;
                }
                FolderNode parent = folderMap.get(currentId);
                if (parent == null) {
                    return false;
                }
                currentId = parent.getParentFolder() != null ? parent.getParentFolder().getFolderId() : null;
            }
            return false;
        }

        /**
         * 連接節點，將子節點連接到父節點中
         * @param parent 父資料夾節點
         * @param child 子資料夾節點
         */
        private void linkNodes(FolderNode parent, FolderNode child) {
            child.setParentFolder(parent);
            parent.getChildren().put(child.getFolderId(), child);

            folderMap.put(child.getFolderId(), child);
            folderMap.put(parent.getFolderId(), parent);
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
    }
}
