package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 轉換器的提供者枚舉類型
 * 用於標識不同的轉換器提供者
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ConvertProviderEnum
 * @create 2025/4/8
 * @Version 1.0
 **/
@Getter
@RequiredArgsConstructor
public enum ConvertProviderEnum {
    /**
     * docx 轉換器
     */
    DOCX("docx"),

    /**
     * markdown 轉換器
     */
    MARKDOWN("md");

    /**
     * 後綴名稱
     */
    private final String suffix;
}
