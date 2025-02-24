package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;

/**
 * 可恢復文件接口，定義了文件恢復的相關方法
 *
 * @author yuan
 * @program FileManagement
 * @ClassName RecoverableFile
 * @create 2025/2/22
 * @Version 1.0
 **/

public interface RecoverableFile<T> {
    /**
     * 恢復文件
     *
     * @param file 文件
     */
    Mono<T> restoreFile(T file, User user);


    /**
     * 恢復文件列表
     *
     * @return 文件列表
     */
    Flux<T> restoreFile(Iterable<T> files, User user);

    /**
     * 刪除文件
     *
     * @param file 文件
     *
     * @return 是否刪除成功
     */
    Mono<Boolean> removeFile(T file, User user);

    /**
     * 刪除文件列表
     *
     * @return 是否刪除成功
     */
    Mono<Boolean> removeFile(Iterable<T> files, User user);


}
