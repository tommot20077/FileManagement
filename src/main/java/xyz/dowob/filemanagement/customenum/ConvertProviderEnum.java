package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 檔案內容轉換提供者枚舉，管理系統中檔案格式轉換的不同策略。
 *
 * <p>此枚舉類型提供一個統一且可擴展的檔案轉換機制，支援不同檔案格式之間的內容轉換。</p>
 *
 * <p>可以輕鬆地將檔案從一種格式轉換為另一種格式，但不失去原檔案的基本格式與約定。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum ConvertProviderEnum {
    /**
     * Word檔案（DOCX）轉換器，支援將 Microsoft Word 檔案轉換為其他格式。
     */
    DOCX("docx"),

    /**
     * Markdown檔案轉換器，將 Markdown 標記語言檔案轉換為其他格式，保留原始文本結構。
     */
    MARKDOWN("md");

    /**
     * 後綴名稱
     */
    private final String suffix;
}
