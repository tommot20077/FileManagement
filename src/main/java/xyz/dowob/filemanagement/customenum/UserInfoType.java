package xyz.dowob.filemanagement.customenum;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 查詢用戶信息的類型枚舉類，根據不同的查詢類型，返回不同的查詢結果。
 * 當以用戶名查詢時，返回用戶ID；當以用戶ID查詢時，返回用戶名。
 * 如果查詢類型不在枚舉類中，則默認返回用戶名的模式。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserInfoType
 * @create 2025/3/11
 * @Version 1.0
 **/

public enum UserInfoType {
    /**
     * 用戶名稱模式
     */
    NAME,

    /**
     * 用戶ID模式
     */
    ID;

    /**
     * 根據查詢類型的值，返回對應的查詢類型枚舉對象，默認返回用戶名的模式。
     *
     * @param value 查詢類型的值
     *
     * @return 查詢類型枚舉對象
     */
    @JsonCreator
    public static UserInfoType getUserInfoType(String value) {
        for (UserInfoType type : UserInfoType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return NAME;
    }
}