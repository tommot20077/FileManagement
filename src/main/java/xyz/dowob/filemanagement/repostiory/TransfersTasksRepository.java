package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.entity.TransfersTask;

import java.util.List;

/**
 * 文件傳輸任務數據庫操作介面，用於操作TransfersTask 實體與數據庫的轉換，繼承ReactiveCrudRepository接口，實現對TransfersTask數據庫的非阻塞操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName TransfersTasksRepository
 * @description
 * @create 2024-12-10 23:58
 * @Version 1.0
 **/
@Repository
public interface TransfersTasksRepository extends ReactiveCrudRepository<TransfersTask, String> {
    /**
     * 根據MD5值查詢文件傳輸任務
     *
     * @param md5 MD5值
     *
     * @return Mono<TransfersTask>
     */
    Mono<TransfersTask> findByMd5(String md5);

    /**
     * 根據傳輸任務ID查詢文件傳輸任務
     *
     * @param transferTaskId 傳輸任務ID
     *
     * @return Mono<TransfersTask>
     */
    Mono<TransfersTask> findByTransferTaskId(String transferTaskId);

    /**
     * 依照指定的狀態查詢未包含在指定狀態中的文件傳輸任務
     *
     * @param status 狀態列表
     *
     * @return Flux<TransfersTask>
     */
    Flux<TransfersTask> findAllByStatusNotIn(@Param("status") List<TransfersStatusEnum> status);


}
