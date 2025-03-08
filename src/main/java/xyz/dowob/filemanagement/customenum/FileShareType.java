package xyz.dowob.filemanagement.customenum;

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

    private final String value;
}
