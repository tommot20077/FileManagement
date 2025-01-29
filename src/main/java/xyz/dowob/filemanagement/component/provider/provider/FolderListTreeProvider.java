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
    private final Map<Long, FolderTree> userFileListTree;

    public FolderListTreeProvider() {
        this.userFileListTree = new HashMap<>();
    }

    public void addFolder(Long userId, UserFileListDTO userFileListDTO) {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
        folderTree.addFolder(userFileListDTO);
    }

    public void addFolders(Long userId, List<UserFileListDTO> userFileListDTOList) {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
        userFileListDTOList.forEach(folderTree::addFolder);
    }

    public FolderTree createAndGetFileTree(Long userId) {
        return userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
    }

    public FolderTree getFileTree(Long userId) {
        return userFileListTree.get(userId);
    }

    public void initializeTree(Long userId, List<UserFileListDTO> folderList) throws ProcessException {
        FolderTree folderTree = userFileListTree.computeIfAbsent(userId, k -> new FolderTree());
        folderTree.initializeTree(folderList);
    }

    public void updateFolder(Long userId, UserFileMetadata folderMetadata, Long newParentId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            folderTree.updateFolder(folderMetadata, newParentId);
        }
    }

    public void deleteFolder(Long userId, UserFileMetadata folderMetadata) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            folderTree.deleteFolder(folderMetadata);
        }
    }

    public List<FolderNode> getPath(Long userId, Long folderId) {
        FolderTree folderTree = userFileListTree.get(userId);
        if (folderTree != null) {
            return folderTree.getPath(folderId);
        }
        return Collections.singletonList(new FolderNode(null, "root"));
    }

    @Getter
    @Setter
    public static class FolderNode {
        private Long folderId;
        private String name;
        @JsonIgnore
        private FolderNode parentFolder;
        @JsonIgnore
        private Map<Long, FolderNode> children;

        public FolderNode(Long folderId, String name) {
            this.folderId = folderId;
            this.name = name;
            this.children = new HashMap<>();
        }

        public FolderNode(UserFileMetadata userFileMetadata) {
            this.folderId = userFileMetadata.getId();
            this.name = userFileMetadata.getFilename();
            this.children = new HashMap<>();
        }
    }

    public static class FolderTree {
        private final FolderNode root;
        private final Map<Long, FolderNode> folderMap;
        private final Map<Long, List<FolderNode>> pendingNodes;

        public FolderTree() {
            this.root = new FolderNode(null, "root");
            this.folderMap = new HashMap<>();
            this.folderMap.put(null, this.root);
            this.pendingNodes = new HashMap<>();
        }


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

        private void deleteFolder(UserFileMetadata metadata) {
            FolderNode node = folderMap.get(metadata.getId());
            if (node == null) {
                return;
            }

            unlinkNode(node);
            removeSubtree(node);
        }

        private List<FolderNode> getPath(Long folderId) {
            List<FolderNode> path = new ArrayList<>();
            FolderNode current = folderMap.get(folderId);
            while (current != null) {
                path.add(current);
                current = current.getParentFolder();
            }
            return path;
        }

        private void unlinkNode(FolderNode node) {
            FolderNode parent = node.getParentFolder();
            if (parent != null) {
                parent.getChildren().remove(node.getFolderId());
            }
            node.setParentFolder(null);
        }

        private void removeSubtree(FolderNode node) {
            for (FolderNode child : new ArrayList<>(node.getChildren().values())) {
                removeSubtree(child);
            }
            folderMap.remove(node.getFolderId());
            pendingNodes.remove(node.getFolderId());
            node.getChildren().clear();
        }

        private void initializeTree(List<UserFileListDTO> folderList) throws ProcessException {
            folderList.stream().filter(UserFileListDTO::isFolder)
                      //.sorted(Comparator.comparing(UserFileListDTO::getParentFolderId, Comparator.nullsFirst(Long::compareTo)))
                      .forEach(this::addFolder);


            for (Long parentId : pendingNodes.keySet()) {
                if (!folderMap.containsKey(parentId)) {
                    throw new ProcessException(ProcessException.ErrorCode.BUILD_FILE_TREE_FAILED, "無效的 parentFolderId: " + parentId);
                }
            }
        }

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

        private void linkNodes(FolderNode parent, FolderNode child) {
            child.setParentFolder(parent);
            parent.getChildren().put(child.getFolderId(), child);

            folderMap.put(child.getFolderId(), child);
            folderMap.put(parent.getFolderId(), parent);
        }

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
