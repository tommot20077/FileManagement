/**
 * 包含檔案管理系統中自定義函數式介面的套件。
 * <p>
 * 此套件提供可重用的函數式介面，支援 Lambda 表達式、方法引用和高階函數等
 * 現代 Java 程式設計特性。這些介面設計靈活且可組合，提升程式碼的可讀性和可維護性。
 * </p>
 * <p>
 * 核心介面包括：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.functionInterface.Permission} - 權限驗證介面</li>
 *   <li>{@link xyz.dowob.filemanagement.functionInterface.Operation} - 通用操作執行介面</li>
 *   <li>{@link xyz.dowob.filemanagement.functionInterface.CacheRule} - 快取規則介面</li>
 * </ul>
 * 所有介面均採用響應式設計，支援非阻塞操作。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see java.util.function
 * @see reactor.core.publisher.Mono
 */
package xyz.dowob.filemanagement.functionInterface;