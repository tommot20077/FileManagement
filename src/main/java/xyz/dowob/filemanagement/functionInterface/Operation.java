package xyz.dowob.filemanagement.functionInterface;

/**
 * 表示可能拋出異常的操作的函數式介面。此介面類似於 {@link Runnable}，
 * 但允許其 {@code execute} 方法拋出受檢異常。
 * <p>
 * 此介面適用於需要異常處理的操作邏輯封裝，特別是在延遲執行、
 * 條件執行或批次執行等場景中使用。通過函數式程式設計方式，
 * 提高程式碼的可讀性和重用性。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see Runnable
 */
@FunctionalInterface
public interface Operation {
    /**
     * 執行具體的操作邏輯。
     * <p>
     * 此方法包含實際的業務邏輯實現，可能會拋出各種類型的異常。
     * 呼叫者應該適當地處理可能拋出的異常。
     * </p>
     *
     * @throws Exception 當操作執行過程中發生錯誤時拋出的異常
     */
    void execute() throws Exception;
}
