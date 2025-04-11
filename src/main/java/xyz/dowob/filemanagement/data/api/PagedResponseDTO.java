package xyz.dowob.filemanagement.data.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

/**
 * 分頁響應數據傳輸對象，用於封裝分頁響應數據
 * 包含數據、總頁數、當前頁碼、每頁大小、總元素數量等信息
 * @author yuan
 * @program FileManagement
 * @ClassName PagedResponseDTO
 * @create 2025/2/6
 * @Version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PagedResponseDTO<T> {

    /**
     * 數據
     */
    private List<T> data = List.of();

    /**
     * 總頁數
     */
    private int totalPages = 0;

    /**
     * 當前頁碼
     */
    private int currentPage = 0;

    /**
     * 每頁大小
     */
    private int pageSize = 0;

    /**
     * 總元素數量
     */
    private long totalElements = 0;

    /**
     * 重寫toString方法，返回對象的字符串表示
     *
     * @return 對象的字符串表示
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("data", subListToString(data));
        map.put("totalPages", totalPages);
        map.put("currentPage", currentPage);
        map.put("pageSize", pageSize);
        map.put("totalElements", totalElements);
        return map.toString();
    }


    /**
     * 將列表轉換為字符串，如果列表長度大於30，則截取前30個字符
     *
     * @param o 要轉換的對象
     *
     * @return 轉換後的字符串
     */
    private String subListToString(Object o) {
        String s = o.toString();
        if (s.length() > 30) {
            return s.substring(0, 30) + "...";
        }
        return s;
    }
}
