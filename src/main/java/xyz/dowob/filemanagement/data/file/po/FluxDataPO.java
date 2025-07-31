package xyz.dowob.filemanagement.data.file.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Flux;
import xyz.dowob.filemanagement.data.datainterface.FluxContainer;

/**
 * Flux 資料流持久化對象，封裝非阻塞式資料流的安全操作。
 * 用於將 Reactive Streams Flux 對象包裝成可管理的持久化物件。
 *
 * <p>此類別提供安全的類型轉換和 Flux 操作封裝，支援簡單的 Mono 操作。
 * 實現 FluxContainer 介面以提供統一的 Flux 存取方式。
 * 通過泛型參數支援任意類型的資料流處理。
 *
 * @param <T> Flux 中包含的資料類型
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

@Getter
@Setter
public class FluxDataPO<T> implements FluxContainer {
    /**
     * 封裝的 Reactive Streams Flux 對象
     */
    @JsonIgnore
    private Flux<T> tFlux;
    


    /**
     * 通過指定的 Flux 對象建構 FluxDataPO。
     *
     * @param tFlux 要封裝的 Flux 對象，不可為 null
     */
    public FluxDataPO(@NotNull Flux<T> tFlux) {
        this.tFlux = tFlux;
    }


    /**
     * 預設建構方法，建立空的 Flux 對象。
     */
    public FluxDataPO() {
        this.tFlux = Flux.empty();
    }

    /**
     * 安全地將輸入的 Flux 轉換為指定類型並設定到當前對象。
     *
     * @param flux        要轉換的原始 Flux 對象
     * @param targetClass 目標轉換類型的 Class 對象
     * @return 轉換後的類型安全 Flux 對象
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
     * 獲取封裝的 Flux 對象。
     *
     * @return 以不指定類型回傳的 Flux 對象
     */
    @Override
    public Flux<?> getFlux() {
        return tFlux;
    }
}


