package xyz.dowob.filemanagement.data.file.po;

import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.ChangeDelta;
import com.github.difflib.patch.DeleteDelta;
import com.github.difflib.patch.InsertDelta;
import lombok.Data;

import java.util.List;

/**
 * 自定義 Patch 持久化對象，封裝文本差分和版本比較資料。
 * 用於儲存和傳輸檔案版本之間的差異資訊，支援版本比較和合併操作。
 *
 * <p>此類別提供 diff-utils 函庫 Patch 物件的序列化支援，包括三種基本操作：
 * 插入（INSERT）、刪除（DELETE）和修改（CHANGE）。
 * 通過內嵌的靜態類別提供完整的 Patch 生命週期管理。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
public class CustomPatchPO {
    @Data
    public static class Patch {
        /**
         * Patch 的差異記錄
         */
        private List<Delta> deltas;

        /**
         * 將 Patch 對象轉換為自定義的Patch 對象
         *
         * @param patch Patch 對象
         *
         * @return Patch 對象
         */
        public static Patch fromPatch(com.github.difflib.patch.Patch<String> patch) {
            Patch patchDTO = new Patch();
            patchDTO.setDeltas(patch.getDeltas().stream().map(Delta::fromDelta).toList());
            return patchDTO;
        }

        /**
         * 將自定義的 Patch 對象轉換為 Patch 對象
         *
         * @return Patch 對象
         */
        public com.github.difflib.patch.Patch<String> toPatch() {
            com.github.difflib.patch.Patch<String> patch = new com.github.difflib.patch.Patch<>();
            deltas.forEach(delta -> patch.addDelta(delta.toDelta()));
            return patch;
        }
    }

    @Data
    public static class Delta {
        /**
         * 差異類型
         */
        private String type;

        /**
         * 差異源
         */
        private Chunk source;

        /**
         * 差異目標
         */
        private Chunk target;

        /**
         * 將 Delta 對象轉換為自定義的 Delta 對象
         *
         * @param delta Delta 對象
         *
         * @return Delta 對象
         */
        public static Delta fromDelta(AbstractDelta<String> delta) {
            Delta deltaDTO = new Delta();
            deltaDTO.setType(delta.getType().name());
            deltaDTO.setSource(Chunk.fromChunk(delta.getSource()));
            deltaDTO.setTarget(Chunk.fromChunk(delta.getTarget()));
            return deltaDTO;
        }

        /**
         * 將自定義的 Delta 對象轉換為 Delta 對象
         *
         * @return Delta 對象
         */
        public AbstractDelta<String> toDelta() {
            com.github.difflib.patch.Chunk<String> sourceChunk = source.toChunk();
            com.github.difflib.patch.Chunk<String> targetChunk = target.toChunk();

            return switch (type) {
                case "DELETE" -> new DeleteDelta<>(sourceChunk, targetChunk);
                case "INSERT" -> new InsertDelta<>(sourceChunk, targetChunk);
                case "CHANGE" -> new ChangeDelta<>(sourceChunk, targetChunk);
                default -> throw new IllegalArgumentException("Unknown delta type: " + type);
            };
        }
    }

    @Data
    public static class Chunk {
        /**
         * 差異位置
         */
        private int position;

        /**
         * 更改位置
         */
        private List<Integer> changePosition;

        /**
         * 差異行
         */
        private List<String> lines;


        /**
         * 將 Chunk 對象轉換為自定義的 Chunk 對象
         *
         * @param chunk Chunk 對象
         *
         * @return Chunk 對象
         */
        public static Chunk fromChunk(com.github.difflib.patch.Chunk<String> chunk) {
            Chunk chunkDTO = new Chunk();
            chunkDTO.setPosition(chunk.getPosition());
            chunkDTO.setLines(chunk.getLines());
            chunkDTO.setChangePosition(chunk.getChangePosition());
            return chunkDTO;
        }


        /**
         * 將自定義的 Chunk 對象轉換為 Chunk 對象
         *
         * @return Chunk 對象
         */
        public com.github.difflib.patch.Chunk<String> toChunk() {
            return new com.github.difflib.patch.Chunk<>(position, lines, changePosition);
        }
    }
}
