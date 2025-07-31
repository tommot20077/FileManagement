package xyz.dowob.filemanagement.component.provider.providerInterface;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.factory.config.ConvertConfig;
import xyz.dowob.filemanagement.customenum.ConvertProviderEnum;

import java.io.InputStream;

/**
 * 基於反應式流的內容轉換提供者介面。定義文字內容轉換為不同資料格式的契約，
 * 支援非阻塞的內容處理和策略模式的轉換實現。
 *
 * <p>此介面採用 WebFlux 反應式設計，提供內容轉換的統一抽象層。
 * 實現類別負責具體的轉換邏輯，包括文件格式轉換、編碼處理和資源管理。
 * 所有轉換操作均以非阻塞方式執行，確保系統的高併發處理能力。</p>
 *
 * <p>轉換提供者通過 {@link ConvertProviderEnum} 進行類型識別，
 * 透過工廠模式動態選擇適當的轉換實現。預設方法提供空實現，
 * 允許實現類別僅覆寫所需的轉換方法。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ConvertProviderEnum
 * @see ConvertConfig
 */

public interface ContentConvertProvider {
    /**
     * 將文字內容轉換為輸入流。執行內容的反應式轉換處理，
     * 將字串資料轉換為可讀取的輸入流格式。
     *
     * <p>預設實現返回空的 Mono，實現類別應覆寫此方法提供具體的轉換邏輯。
     * 轉換過程應處理字元編碼、緩衝管理和錯誤處理。</p>
     *
     * @param content 待轉換的文字內容，不得為 null
     * @return 包含轉換後輸入流的 Mono，若不支援此轉換則返回空 Mono
     */
    default Mono<InputStream> convertToInputStream(String content) {
        return Mono.empty();
    }


    /**
     * 將文字內容轉換為反應式資料緩衝區流。執行非阻塞的內容轉換，
     * 生成包含資料緩衝區流和大小資訊的記錄物件。
     *
     * <p>此方法適用於大型內容的串流處理，透過資料緩衝區分段傳輸，
     * 避免記憶體溢位並提升處理效率。預設實現返回空的 Mono，
     * 實現類別應提供具體的緩衝區轉換邏輯。</p>
     *
     * @param content 待轉換的文字內容，不得為 null
     * @return 包含 DataBufferRecord 的 Mono，含有資料緩衝區流和大小資訊，
     *         若不支援此轉換則返回空 Mono
     */
    default Mono<DataBufferRecord> convertToDataBuffer(String content) {
        return Mono.empty();
    }

    /**
     * 返回內容轉換提供者的類型識別碼。用於工廠模式中識別和選擇
     * 適當的轉換實現，確保正確的轉換策略被應用。
     *
     * <p>每個實現類別必須返回唯一的轉換器類型，
     * 以便系統能夠正確路由轉換請求到對應的處理邏輯。</p>
     *
     * @return 轉換提供者的類型枚舉，不得為 null
     */
    ConvertProviderEnum getType();

    /**
     * 返回轉換提供者的配置物件。提供轉換過程中所需的參數設定，
     * 包括格式選項、編碼設定和處理參數。
     *
     * <p>預設實現返回 null，表示不提供配置資訊。實現類別可覆寫此方法
     * 返回具體的配置物件，用於自訂轉換行為和參數調整。
     * 配置物件應包含轉換所需的所有設定資訊。</p>
     *
     * @return 轉換配置物件，若無配置則返回 null
     */
    default ConvertConfig getConvertConfig() {
        return null;
    }

    /**
     * 封裝資料緩衝區流和大小資訊的記錄類。用於傳遞轉換後的反應式資料流
     * 及其對應的總大小資訊，支援串流處理和進度追蹤。
     *
     * <p>此記錄類為不可變物件，確保資料的執行緒安全性。
     * 資料緩衝區流適用於大型檔案的分段傳輸，大小資訊用於進度計算和資源管理。</p>
     *
     * @param dataBuffer 包含轉換後資料的反應式流，不得為 null
     * @param size 資料總大小（位元組），用於進度追蹤和資源分配
     */
    record DataBufferRecord(Flux<DataBuffer> dataBuffer, long size) {
    }
}
