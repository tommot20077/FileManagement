package xyz.dowob.filemanagement.component.provider.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ProcessException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
@ConditionalOnProperty(name = {"file.global.enable-user-folder-list-tree"}, havingValue = "true", matchIfMissing = true)
public class FolderListTreeProvider {
    /**
     * 用戶檔案列表樹映射，用於存儲用戶的檔案列表樹
     */
    private final Map<Long, FolderTree> userFileListTree;


    public FolderListTreeProvider() {
        this.userFileListTree = new ConcurrentHashMap<>();
    }

    /**
     * 添加新的資料夾到用戶的檔案列表樹中
     *
     * @param userId           用戶ID
     * @param userFileMetadata 文件夾元數據
     */
    public void addFolder(Long userId, UserFileMetadata userFileMetadata) {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
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
     */
    public void addFolders(Long userId, List<UserFileMetadata> userFileMetadataList) {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
        userFileMetadataList.forEach(userFileListDTO -> folderTree.addFolder(userFileListDTO.getFileType() == FileEnum.FOLDER,
                                                                             userFileListDTO.getId(),
                                                                             userFileListDTO.getParentFolderId(),
                                                                             userFileListDTO.getFilename()
        ));
    }


    /**
     * 獲取用戶的檔案列表樹，當用戶的檔案列表樹不存在時，創建一個新的檔案列表樹
     *
     * @param userId 用戶ID
     *
     * @return 用戶的檔案列表樹
     */
    public FolderTree createAndGetFileTree(Long userId) {
        return userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
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
     */
    public void initializeTree(Long userId, List<UserFileListDTO> folderList, boolean isLastPage) throws ProcessException {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
        folderTree.initializeTree(folderList, isLastPage);
    }

    /**
     * 更新資料夾的父資料夾
     *
     * @param userId               用戶ID
     * @param userFileMetadataList 資料夾元數據
     * @param newParentId          新的父資料夾ID
     */
    public void updateFolder(Long userId, UserFileMetadata userFileMetadataList, Long newParentId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            folderTree.updateFolder(userFileMetadataList.getFileType() == FileEnum.FOLDER,
                                    userFileMetadataList.getId(),
                                    userFileMetadataList.getFilename(),
                                    newParentId
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
            this.root = new FolderNode(0L, "root");
            this.folderMap = new ConcurrentHashMap<>();
            this.folderMap.put(0L, this.root);
            this.pendingNodes = new ConcurrentHashMap<>();
        }


        /**
         * 更新資料夾樹
         *
         * @param isFolder    是否為資料夾
         * @param folderId    資料夾ID
         * @param filename    資料夾名稱
         * @param newParentId 新的父資料夾ID
         */
        private void updateFolder(Boolean isFolder, Long folderId, String filename, Long newParentId) {
            FolderNode node = folderMap.get(folderId);
            if (node == null || !isFolder) {
                return;
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

            newParentId = Objects.requireNonNullElse(newParentId, 0L);
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
         *
         * @param folderId 資料夾元數據ID
         */
        private void deleteFolder(Long folderId) {
            FolderNode node = folderMap.get(folderId);
            if (node == null) {
                return;
            }

            unlinkNode(node);
            removeSubtree(node);
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

        /**
         * 清除連接的節點將指定的節點從父節點中移除並將父節點設置為空
         *
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
         *
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
         *
         * @param folderList 文件夾列表
         * @param isLastPage 是否為最後一頁
         *
         * @throws ProcessException 初始化資料夾樹失敗，當父資料夾不存在時，拋出此異常
         */
        private void initializeTree(List<UserFileListDTO> folderList, boolean isLastPage) throws ProcessException {
            folderList
                    .stream().filter(folder -> folder.getFileType() == FileEnum.FOLDER)
                    .forEach(folder -> addFolder(true, folder.getId(), folder.getParentFolderId(), folder.getFilename()));

            if (isLastPage) {
                for (Long parentId : pendingNodes.keySet()) {
                    if (!folderMap.containsKey(parentId)) {
                        throw new ProcessException(ProcessException.ErrorCode.BUILD_FILE_TREE_FAILED, "無效的 parentFolderId: " + parentId);
                    }
                }
                if (!validateTree()) {
                    throw new ProcessException(ProcessException.ErrorCode.BUILD_FILE_TREE_FAILED, "初始化資料夾樹失敗，存在無效的節點");
                }
                CompletableFuture.runAsync(() -> synchronizeTree(root));
            }
        }


        /**
         * 添加資料夾
         *
         * @param isFolder       是否為資料夾
         * @param folderId       資料夾ID
         * @param parentFolderId 父資料夾ID
         * @param filename       資料夾名稱
         *
         * @throws RuntimeException 添加資料夾失敗，當存在循環引用時，拋出此異常
         */
        private void addFolder(Boolean isFolder, Long folderId, Long parentFolderId, String filename) {
            if (!isFolder) {
                return;
            }

            parentFolderId = Objects.requireNonNullElse(parentFolderId, 0L);
            if (hasCircularReference(parentFolderId)) {
                throw new RuntimeException("存在循環引用");
            }

            FolderNode folderNode = new FolderNode(folderId, filename);
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
    }
}
