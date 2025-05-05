package xyz.dowob.filemanagement.data.file.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Flux;
import xyz.dowob.filemanagement.data.datainterface.FluxContainer;

/**
 * 用於映射並包裝Flux對象的成一個PO類
 * 透過FluxDataPO對Flux進行包裝，可以方便的進行Mono操作
 * 以及提供安全的轉換方法
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FluxDataPO
 * @create 2025/3/16
 * @Version 1.0
 **/

@Getter
@Setter
public class FluxDataPO<T> implements FluxContainer {
    /**
     * Flux對象
     */
    @JsonIgnore
    private Flux<T> tFlux;


    /**
     * 安全轉換輸入的Flux對象變成指定的類型
     *
     * @param flux        Flux對象
     * @param targetClass 目標類型
     *
     * @return Flux<T>
     */
    public Flux<T> formatAndSet(Flux<?> flux, Class<T> targetClass) {
        Flux<T> formatFlux = flux.mapNotNull(f -> {
            if (targetClass.isInstance(f)) {
                return targetClass.cast(f);
            }
            return null;
        });
        this.tFlux = formatFlux;
        return formatFlux;
    }


    /**
     * 通過Flux對象構造FluxDataPO對象
     *
     * @param tFlux Flux對象
     */
    public FluxDataPO(@NotNull Flux<T> tFlux) {
        this.tFlux = tFlux;
    }


    /**
     * 無參構造方法
     */
    public FluxDataPO() {
        this.tFlux = Flux.empty();
    }


    /**
     * 獲取Flux對象
     *
     * @return Flux<?> 未指定類型的Flux對象
     */
    @Override
    public Flux<?> getFlux() {
        return tFlux;
    }
}


