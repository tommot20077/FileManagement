package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 檔案傳輸協議類型列舉。
 * <p>定義檔案上傳所使用的不同傳輸協議和方式，支援小檔案的 Multipart 傳輸和大檔案的分塊傳輸。
 * 每種傳輸類型具有不同的效能特性和適用場景，系統會根據檔案大小和網路狀態選擇適合的傳輸方式。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum TransmissionEnum {
    /**
     * HTTP Multipart 傳輸協議。
     * 適用於小型檔案的一次性上傳，使用標準 HTTP multipart/form-data 格式，具有簡單高效的特性。
     */
    MULTIPART("multipart"),

    /**
     * 分塊上傳傳輸協議。
     * 適用於大型檔案的分段上傳，支援斷點續傳和上傳進度追蹤功能，需要客戶端配合實現。
     */
    CHUNK("chunk");

    /**
     * 傳輸協議標識字串。
     * 用於識別和區分不同傳輸方式的字串標識，在 API 請求和系統設定中使用。
     */
    private final String type;

    /**
     * 根據字串類型解析傳輸協議列舉。
     * 將字串格式的傳輸類型轉換為對應的列舉值，支援不分大小寫的比對和空白字元處理。
     *
     * @param type 傳輸協議類型字串，如 "multipart" 或 "chunk"
     * @return 匹配的傳輸協議列舉，若無匹配或參數為空則回傳 null
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
