package xyz.dowob.filemanagement.component.provider.factory;

import jakarta.annotation.Nullable;
import xyz.dowob.filemanagement.component.provider.factory.config.ConvertConfig;
import xyz.dowob.filemanagement.component.provider.providerImplement.contentconvertprovider.WordConvertProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.ContentConvertProvider;
import xyz.dowob.filemanagement.customenum.ConvertProviderEnum;

/**
 * 內容轉換服務的工廠實現，根據轉換器類型動態創建相應的內容轉換提供者。
 *
 * <p>此工廠類實現標準工廠模式，支援多種內容轉換格式。根據 {@link ConvertProviderEnum} 
 * 選擇適當的轉換實現，並使用 {@link ConvertConfig} 進行個性化設定。
 * 當未提供設定時，自動使用預設設定參數。</p>
 *
 * <p>支援的轉換格式由枚舉類型定義，可輕鬆擴展新的轉換器類型。
 * 轉換過程為靜態操作，無需實例化工廠對象。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
public class ContentConvertProviderFactory {
    /**
     * 根據指定類型創建內容轉換提供者實例。
     *
     * <p>當設定參數為空時，自動創建預設設定。目前支援的轉換類型
     * 將根據枚舉值選擇對應的實現類。</p>
     *
     * @param type   轉換器類型，不可為空
     * @param config 轉換器設定參數，可為空則使用預設設定
     * @return 對應類型的內容轉換提供者實例
     * @throws IllegalArgumentException 當轉換器類型為空時
     */
    public static ContentConvertProvider createProvider(ConvertProviderEnum type, @Nullable ConvertConfig config) {
        if (config == null) {
            config = new ConvertConfig();
        }
        return switch (type) {
            case null -> throw new IllegalArgumentException("轉換器類型不能為空");
            default -> new WordConvertProvider(config);
        };
    }
}
