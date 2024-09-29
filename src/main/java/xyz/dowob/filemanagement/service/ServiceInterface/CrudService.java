package xyz.dowob.filemanagement.service.ServiceInterface;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
 * @param <T>  實體類型
 * @param <ID> 實體ID類型(通常為Long)
 *
 * @author yuan
 * @program File-Management
 * @ClassName CrudService
 * @description
 * @create 2024-09-14 22:57
 * @Version 1.0
 **/
public interface CrudService <T, ID> {
    /**
     * 創建一個新的實體
     *
     * @return 返回一個新的實體對象
     */
    Mono<T> create();

    /**
     * 根據ID獲取一個實體
     *
     * @param id 實體ID
     *
     * @return 返回一個Optional對象
     */
    Mono<T> getById(ID id);

    /**
     * 獲取所有實體
     */
    Flux<T> getAll();

    /**
     * 更新一個實體
     *
     * @param entity 實體對象
     */
    Mono<Void> update(T entity);

    /**
     * 刪除一個實體
     *
     * @param entity 實體對象
     */
    Mono<Void> delete(T entity);
}
