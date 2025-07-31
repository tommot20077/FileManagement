package xyz.dowob.filemanagement.annotation;

import xyz.dowob.filemanagement.customenum.LogLevelEnum;

import java.lang.annotation.*;

/**
 * AOP 日誌記錄跳過控制註解。
 * <p>
 * 此註解用於精確控制 AOP 日誌記錄系統的輸出範圍，允許開發者排除低重要性的日誌記錄，
 * 提高系統效能並減少日誌噴音。當某些方法、類別或欄位因為高頻率執行或輸出
 * 內容對除錯無益時，可以使用此註解來跳過相應級別的日誌記錄。
 * <p>
 * 日誌級別層次結構（Log4j2）：
 * <ul>
 *   <li>FATAL (100) - 最高優先級，致命錯誤</li>
 *   <li>ERROR (200) - 錯誤資訊</li>
 *   <li>WARN (300) - 警告資訊</li>
 *   <li>INFO (400) - 一般資訊</li>
 *   <li>DEBUG (500) - 調試資訊</li>
 *   <li>TRACE (600) - 最低優先級，詳細追蹤</li>
 * </ul>
 * <p>
 * 跳過邏輯：
 * <p>
 * 當方法的實際日誌級別數值 >= 任一指定跳過級別的數值時，該日誌將被跳過。
 * 例如：@SkipRecord({WARN}) 會跳過 WARN(300)、INFO(400)、DEBUG(500)、TRACE(600)，
 * 但保留 ERROR(200) 和 FATAL(100) 的日誌記錄。
 * <p>
 * 功能特性：
 * <ul>
 *   <li>支援選擇性的日誌級別跳過，可以指定要忽略的日誌等級</li>
 *   <li>支援方法、類別和欄位級別的應用</li>
 *   <li>預設跳過 WARN 級別及以下的日誌記錄</li>
 *   <li>保留 ERROR 和 FATAL 級別日誌以確保關鍵錯誤資訊不會遺失</li>
 * </ul>
 * <p>
 * 使用場景：
 * <ul>
 *   <li>高頻率執行的工具方法（如資料轉換、格式化等）</li>
 *   <li>Getter/Setter 方法或簡單的屬性存取</li>
 *   <li>內部工具類別或私有方法</li>
 *   <li>繁雜的計算或資料處理邏輯中的輔助方法</li>
 *   <li>具有敏感資訊的方法（配合 @HideSensitive 使用）</li>
 * </ul>
 * <p>
 * 註解優先級：
 * <ul>
 *   <li>方法級別的 @SkipRecord 優先級最高</li>
 *   <li>類別級別的 @SkipRecord 作為預設設定</li>
 *   <li>欄位級別的 @SkipRecord 只影響特定欄位的記錄</li>
 * </ul>
 * <p>
 * 使用範例：
 * <pre>{@code
 * // 使用預設設定：跳過 WARN 及以下級別，只記錄 ERROR 和 FATAL
 * @SkipRecord
 * public String formatTimestamp(long timestamp) {
 *     // 時間格式化邏輯
 * }
 * 
 * // 只跳過 INFO 及以下級別，保留 WARN、ERROR 和 FATAL
 * @SkipRecord(LogLevelEnum.INFO)
 * public void processData() {
 *     // 資料處理邏輯
 * }
 * 
 * // 為整個類別設定跳過規則
 * @SkipRecord
 * public class DataUtility {
 *     // 類別實作
 * }
 * }</pre>
 * <p>
 * 注意事項：
 * <ul>
 *   <li>不建議跳過 ERROR 級別日誌，以免遺失關鍵錯誤資訊</li>
 *   <li>在關鍵業務邏輯中謹慎使用，確保不會影響問題追蹤</li>
 *   <li>與 @RecordLevel 同時使用時，@SkipRecord 具有更高優先級</li>
 *   <li>可以與 @HideSensitive 結合使用以提供更細粒度的日誌控制</li>
 * </ul>
 *
 * @see xyz.dowob.filemanagement.customenum.LogLevelEnum
 * @see xyz.dowob.filemanagement.annotation.RecordLevel
 * @see xyz.dowob.filemanagement.annotation.HideSensitive
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
public @interface SkipRecord {
    /**
     * 指定要跳過的日誌記錄級別清單。
     * <p>
     * 此屬性定義了哪些級別及其以下優先級的日誌記錄應該被跳過。當 AOP 日誌記錄系統處理
     * 標記了此註解的程式元素時，會檢查當前日誌級別數值是否 >= 任一指定級別的數值，
     * 如果是，則不會產生相應的日誌記錄。
     * <p>
     * 預設設定跳過 WARN 級別及以下（WARN、INFO、DEBUG、TRACE），保留 ERROR 和 FATAL 級別
     * 以確保關鍵錯誤資訊不會遺失。這種預設設定適合大多數情況，既能減少日誌噪音，
     * 又能保留重要的錯誤資訊。
     * <p>
     * 日誌級別說明（依重要性排序）：
     * <ul>
     *   <li>FATAL (100)：致命錯誤，系統無法繼續運行</li>
     *   <li>ERROR (200)：錯誤資訊，表示嚴重問題需要立即處理</li>
     *   <li>WARN (300)：警告資訊，表示潛在問題但不影響執行</li>
     *   <li>INFO (400)：一般性資訊，用於追蹤程式執行流程</li>
     *   <li>DEBUG (500)：詳細的除錯資訊，生產環境中通常關閉</li>
     *   <li>TRACE (600)：最詳細的追蹤資訊，用於深度除錯</li>
     * </ul>
     *
     * @return 要跳過的日誌級別陣列，預設為 WARN（跳過 WARN 及以下級別）
     */
    LogLevelEnum[] value() default LogLevelEnum.WARN;
}