package xyz.dowob.filemanagement.data.datainterface;

import reactor.core.publisher.Flux;

/**
 * Flux容器接口，用於規範對象內若含Flux對象的類則需實現此接口
 * 在此接口中定義了獲取Flux對象的方法
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FluxContainer
 * @create 2025/5/9
 * @Version 1.0
 **/

public interface FluxContainer {
    /**
     * 獲取Flux對象
     *
     * @return Flux<?> 數據流對象
     */
    Flux<?> getFlux();
}
