package xyz.dowob.filemanagement.data.datainterface;

import reactor.core.publisher.Flux;

/**
 * 響應式資料流容器介面，定義了獲取通用響應式資料流的標準方法。
 *
 * <p>此介面為專案中的反應式編程模式提供統一的資料流訪問接口，
 * 確保實現類能夠提供一個標準化的 Flux 資料流，支持非阻塞、事件驅動的資料處理邏輯。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

public interface FluxContainer {
    /**
     * 取得當前物件的響應式資料流。
     *
     * <p>此方法提供一個統一的接口，用於獲取泛型的反應式資料流（Flux）。
     * 實現類別應回傳一個具體類型的 Flux，代表該物件可觀察的資料流。</p>
     *
     * @return Flux<?> 表示非同步、事件驅動的資料流，支持多值非阻塞處理
     * @see reactor.core.publisher.Flux
     */
    Flux<?> getFlux();
}
