package xyz.dowob.filemanagement.component.provider.factory.config;

import lombok.Getter;
import lombok.Setter;
import xyz.dowob.filemanagement.component.provider.factory.ContentConvertProviderFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * 內容轉換器的設定參數類，定義轉換過程中使用的格式化設定。
 *
 * <p>提供檔案轉換所需的詳細參數設定，包含字體、編號、縮進等格式化選項。
 * 這些設定通過 {@link ContentConvertProviderFactory} 傳遞給具體的轉換器實現。</p>
 *
 * <p>支援的設定項目涵蓋：列表編號、代碼區塊樣式、預設字體設定、
 * 字體轉換映射以及段落縮進參數。所有設定均提供合理的預設值。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@Setter
public class ConvertConfig {
    /**
     * 無序列表的編號
     */
    private BigInteger abstractNumIdBullet = BigInteger.valueOf(0);

    /**
     * 有序列表的編號
     */
    private BigInteger abstractNumIdDecimal = BigInteger.valueOf(1);

    /**
     * 代碼區塊的背景顏色
     */
    private String codeBlockBackgroundColor = "F0F0F0";

    /**
     * 代碼區塊的字體
     */
    private String codeBlockFontFamily = "Courier New";

    /**
     * 代碼區塊的字體大小
     */
    private int codeBlockFontSize = 10;

    /**
     * 預設的字體
     */
    private String defaultFontFamily = "Calibri";

    /**
     * 預設的字體大小
     */
    private int defaultFontSize = 12;

    /**
     * 預設的字體顏色
     */
    private String defaultFontColor = "000000";

    /**
     * 預設的字體的轉換對應表
     * 這邊的字體轉換對應表是用來將不同的字體轉換成預設的字體
     */
    private Map<String, String> defaultFontConvertMap = new HashMap<>(Map.of("serif", "Times New Roman", "monospace", "Courier New"));

    /**
     * 預設的字體大小的轉換對應表
     * 這邊的字體大小轉換對應表是用來將不同的字體大小轉換成預設的字體大小
     */
    private Map<String, Integer> defaultFontSizeConvertMap = new HashMap<>(Map.of("small", 10, "normal", 12, "large", 18, "huge", 24));

    /**
     * 縮進的尺寸大小，360 twips = 0.25 inch
     */
    private int twipsPerOneLevel = 360;
}
