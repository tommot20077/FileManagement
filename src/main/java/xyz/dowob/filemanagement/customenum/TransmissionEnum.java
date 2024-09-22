package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 傳輸類型的枚舉類，用於區分檔案使用的傳輸協議
 *
 * @program File-Management
 * @ClassName FileEnum
 * @description
 * @create 2024-10-04 02:30
 * @Version 1.0
 * @Author yuan
 */
@Getter
@RequiredArgsConstructor
public enum TransmissionEnum {
    /**
     * Multipart 協議，適合小檔案上傳
     */
    MULTIPART("multipart"),

    /**
     * 分塊協議，適合大檔案上傳，支持斷點續傳(需要前端支持)
     */
    CHUNK("chunk");

    /**
     * 傳輸類型
     */
    private final String type;

    /**
     * 根據傳輸類型獲取對應的傳輸協議
     *
     * @param type 傳輸類型
     *
     * @return 傳輸協議
     */
    public static TransmissionEnum fromType(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        String standardType = type.toUpperCase().trim();
        for (TransmissionEnum transmissionEnum : TransmissionEnum.values()) {
            if (transmissionEnum.name().equals(standardType)) {
                return transmissionEnum;
            }
        }
        return null;
    }
}
