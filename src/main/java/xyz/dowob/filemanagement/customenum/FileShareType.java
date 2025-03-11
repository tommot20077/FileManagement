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
public enum FileShareType {
    /**
     * 公開分享
     */
    PUBLIC("公開分享"),

    /**
     * 私有分享
     */
    PRIVATE("私有分享"),

    /**
     * 不分享
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
    public static FileShareType format(String type) {
        for (FileShareType fileType : FileShareType.values()) {
            if (fileType.name().equalsIgnoreCase(type)) {
                return fileType;
            }
        }
        return null;
    }
}
