package xyz.dowob.filemanagement.customenum;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 檔案分享類型枚舉，定義不同的檔案分享等級。
 *
 * <p>提供了完整的檔案存取控制機制，包括公開分享、預設分享、私有分享和不分享等選項。
 * 支援父資料夾權限繼承及獨立的檔案存取控制。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum FileShareTypeEnum {
    /**
     * 公開分享，所有人都可以訪問，無須設定允許的使用者。
     */
    PUBLIC("公開分享"),

    /**
     * 預設分享，不公開所有人但可以設定允許的使用者存取。
     * 此外會繼承父資料夾的分享設定，當使用者有權限訪問父資料夾時，也可以訪問此檔案。
     */
    DEFAULT("預設分享"),

    /**
     * 私有分享，只有明確指定的使用者可以訪問。
     * 即使具有父資料夾的訪問權限也無法訪問此檔案。
     */
    PRIVATE("私有分享"),

    /**
     * 不分享，完全不分享檔案，即使明確設定使用者也無法訪問。
     */
    NONE("不分享");

    /**
     * 分享類型的中文描述。
     */
    private final String describe;

    /**
     * 根據字符串轉換為檔案分享類型，不區分大小寫。
     *
     * @param type 檔案分享類型字符串
     * @return 對應的檔案分享類型，若找不到匹配則回傳 null
     */
    @JsonCreator
    public static FileShareTypeEnum fromString(String type) {
        for (FileShareTypeEnum fileType : FileShareTypeEnum.values()) {
            if (fileType.name().equalsIgnoreCase(type)) {
                return fileType;
            }
        }
        return null;
    }
}
