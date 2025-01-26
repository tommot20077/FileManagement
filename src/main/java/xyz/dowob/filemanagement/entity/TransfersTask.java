package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 文件傳輸任務，用於紀錄文件傳輸任務的相關信息，當傳輸任務有問題時，可以通過此表查看相關信息
 *
 * @author yuan
 * @program FileManagement
 * @ClassName TransfersTask
 * @description
 * @create 2024-12-10 22:58
 * @Version 1.0
 **/
@Getter
@Setter
@Table(name = "transfers_task")
public class TransfersTask {
    /**
     * 主鍵ID
     */
    @Id
    private Long id;

    /**
     * 傳輸任務ID
     */
    @Column("transfer_task_id")
    private String transferTaskId;

    /**
     * 文件MD5值
     */
    @Column("md5")
    private String md5;

    /**
     * GridFS ID
     */
    @Column("grid_fs_id")
    private String gridFsId;

    /**
     * 文件大小
     */
    @Column("file_size")
    private Long fileSize;

    /**
     * 開始時間
     */
    @Column("start_time")
    private LocalDateTime startTime;

    /**
     * 完成時間
     */
    @Column("finish_time")
    private LocalDateTime finishTime;

    /**
     * 訊息
     */
    private String message;

    /**
     * 任務狀態
     */
    private TransfersStatusEnum status;

    /**
     * 重寫equals方法，判斷兩個對象是否相等
     *
     * @param o 對象
     *
     * @return 是否相等
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransfersTask that = (TransfersTask) o;
        return id.equals(that.id);
    }

    /**
     * 重寫hashCode方法，獲取對象的hashCode
     *
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * 重寫toString方法，獲取對象的字符串表示
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("transferTaskId", transferTaskId);
        map.put("md5", md5);
        map.put("gridFsId", gridFsId);
        map.put("fileSize", fileSize);
        map.put("startTime", startTime);
        map.put("finishTime", finishTime);
        map.put("message", message);
        map.put("status", status);
        return map.toString();
    }
}
