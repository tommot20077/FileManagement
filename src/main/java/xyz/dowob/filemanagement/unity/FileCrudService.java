package xyz.dowob.filemanagement.unity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

/**
 * 此接口用於定義基本的增刪改查方法
 * 服務層內部的實體處理都需要實現這些方法
 * 這些方法是對實體進行基本的操作
 * 1. create 用於創建一個新的實體
 * 2. getById 用於根據ID獲取一個實體
 * 3. update 用於更新一個實體
 * 4. delete 用於刪除一個實體
 * 5. getAll 用於獲取所有實體
 *
 * @author yuan
 * @program File-Management
 * @ClassName FileCrudService
 * @description
 * @create 2024-09-14 22:57
 * @Version 1.0
 **/
public interface FileCrudService {
    /**
     * 創建一個新的用戶文件元數據實體
     *
     * @return 返回一個新的實體對象
     */
    Mono<UserFileMetadata> createUserFileMetadata();

    /**
     * 根據ID獲取一個用戶文件元數據實體
     *
     * @param id 服務器文件元數據ID
     *
     * @return 用戶文件元數據實體對象
     */
    Mono<UserFileMetadata> getUserFileMetadataById(Long id);

    /**
     * 獲取所有用戶文件元數據實體
     */
    Flux<UserFileMetadata> getAllUserFileMetadata();

    /**
     * 更新一個用戶文件元數據實體
     *
     * @param entity 用戶文件元數據實體對象
     */
    Mono<Void> updateUserFileMetadata(UserFileMetadata entity);

    /**
     * 刪除一個用戶文件元數據實體
     *
     * @param entity 用戶文件元數據實體對象
     */
    Mono<Void> deleteUserFileMetadata(UserFileMetadata entity);

    /**
     * 創建一個新的服務器文件元數據實體
     *
     * @return 返回一個新的服務器文件元數據實體對象
     */
    Mono<ServerFileMetadata> createServerFileMetadata();

    /**
     * 根據ID獲取一個服務器文件元數據實體
     *
     * @param id 服務器文件元數據ID
     *
     * @return 服務器文件元數據實體對象
     */
    Mono<ServerFileMetadata> getByServerFileMetadataId(Long id);

    /**
     * 獲取所有服務器文件元數據實體
     */
    Flux<ServerFileMetadata> getAllServerFileMetadata();

    /**
     * 更新一個服務器文件元數據實體
     *
     * @param entity 服務器文件元數據實體對象
     */
    Mono<ServerFileMetadata> updateServerFileMetadata(ServerFileMetadata entity);

    /**
     * 刪除一個服務器文件元數據實體
     *
     * @param entity 服務器文件元數據實體對象
     */
    Mono<Void> deleteServerFileMetadata(ServerFileMetadata entity);
}
