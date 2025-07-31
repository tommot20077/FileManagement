package xyz.dowob.filemanagement.data.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

/**
 * 分頁響應資料傳輸物件，提供標準化的分頁資料封裝。
 *
 * <p>本類別管理分頁資料的所有元素，包括當前頁面資料、頁面訊息和總體統計。
 * 支援泛型，可承載任意類型的分頁內容。</p>
 *
 * @param <T> 分頁資料的具體元素類型，支援任意實體或資料結構
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PagedResponseDTO<T> {

    /**
     * 當前頁面的資料內容列表。
     *
     * <p>包含當前頁面所有元素的集合，支援泛型以適應不同的資料類型。</p>
     */
    private List<T> data = List.of();

    /**
     * 總頁數，表示所有資料可分割的頁面總數。
     *
     * <p>根據總元素數和每頁大小計算得出，用於分頁導航。</p>
     */
    private int totalPages = 0;

    /**
     * 當前頁碼，標示目前顯示的頁面。
     *
     * <p>以 0 為起始值，代表第一頁，適合前端分頁導航使用。</p>
     */
    private int currentPage = 0;

    /**
     * 每頁元素數量，決定每個頁面可顯示的最大元素數。
     *
     * <p>用於控制分頁大小和計算總頁數，影響分頁查詢的效能。</p>
     */
    private int pageSize = 0;

    /**
     * 總元素數量，表示所有符合條件的資料總數。
     *
     * <p>用於計算總頁數和提供給使用者了解資料範圍，支援大數據集。</p>
     */
    private long totalElements = 0;

    /**
     * 將分頁響應物件轉換為可讀的字串表示。
     *
     * <p>為了避免過長的輸出，會對資料內容進行適當的截斷。</p>
     *
     * @return 格式化後的字串表示
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
     * 將物件轉換為限制長度的字串表示。
     *
     * <p>為了保持輸出的可讀性，當字串長度超過 30 個字元時會進行截斷並添加省略號。</p>
     *
     * @param o 待轉換的物件
     * @return 截斷後的字串表示
     */
    private String subListToString(Object o) {
        String s = o.toString();
        if (s.length() > 30) {
            return s.substring(0, 30) + "...";
        }
        return s;
    }
}
