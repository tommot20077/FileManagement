package xyz.dowob.filemanagement.component.provider.factory;

import jakarta.annotation.Nullable;
import xyz.dowob.filemanagement.component.provider.factory.config.ConvertConfig;
import xyz.dowob.filemanagement.component.provider.providerImplement.contentconvertprovider.WordConvertProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.ContentConvertProvider;
import xyz.dowob.filemanagement.customenum.ConvertProviderEnum;

/**
 * 內容轉換器工廠類，用於創建不同類型的內容轉換器
 * 首先會根據 {@link ConvertProviderEnum} 的類型來創建對應的轉換器
 * 然後會將 {@link ConvertConfig} 的參數傳入轉換器中，如果沒有傳入的話，則會使用預設的參數
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ContentConvertProviderFactory
 * @create 2025/4/8
 * @Version 1.0
 **/
public class ContentConvertProviderFactory {
    /**
     * 根據 {@link ConvertProviderEnum} 的類型來創建對應的轉換器
     * 如果沒有傳入 {@link ConvertConfig} 的話，則會使用預設的參數
     *
     * @param type   轉換器的類型
     * @param config 轉換器的參數設定
     *
     * @return 轉換器的實例
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
