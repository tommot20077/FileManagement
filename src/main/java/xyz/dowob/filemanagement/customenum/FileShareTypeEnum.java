package xyz.dowob.filemanagement.customenum;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文件分享類型
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileShareType
 * @create 2025/3/6
 * @Version 1.0
 **/
@Getter
@RequiredArgsConstructor
public enum FileShareTypeEnum {
    /**
     * 公開分享: 所有人都可以訪問，無須設定允許的用戶
     */
    PUBLIC("公開分享"),

    /**
     * 默認分享: 不公開所有人但可以設定允許的用戶使用，此外會跟隨文件的父文件夾的分享設定，當用戶有權限訪問父文件夾時，也可以訪問此文件
     */
    DEFAULT("預設分享"),

    /**
     * 私有分享: 只有設定的用戶可以訪問，即使具有父文件夾的訪問權限也無法訪問
     */
    PRIVATE("私有分享"),

    /**
     * 不分享: 不分享文件，即使設定用戶也無法訪問
     */
    NONE("不分享");

    /**
     * 描述
     */
    private final String describe;

    /**
     * 根據字符串格式化文件分享類型，此為不區分大小寫的格式化
     *
     * @param type 文件分享類型字符串
     *
     * @return 文件分享類型
     */
    @JsonCreator
    public static FileShareTypeEnum format(String type) {
        for (FileShareTypeEnum fileType : FileShareTypeEnum.values()) {
            if (fileType.name().equalsIgnoreCase(type)) {
                return fileType;
            }
        }
        return null;
    }
}
