/**
 * 具體服務提供者實現模組，封裝複雜的底層操作邏輯並提供簡化的操作介面。
 *
 * <p>此模組採用提供者模式（Provider Pattern），將複雜的技術實現細節隱藏在簡潔的 API 後面。
 * 每個提供者專注於特定領域的操作，提供高內聚、低耦合的服務實現。</p>
 *
 * <p>包含的服務提供者：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider}：
 *       檔案樹結構管理，提供非阻塞的樹形資料結構操作</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.provider.GridFsProvider}：
 *       MongoDB GridFS 操作封裝，支援大檔案的反應式存取</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.provider.RedisProvider}：
 *       Redis 資料操作，提供多種資料結構的非阻塞操作</li>
 * </ul>
 * </p>
 *
 * <p>所有提供者均支援 Spring WebFlux 反應式編程模型，確保高效能的非阻塞操作。
 * 透過封裝底層技術複雜性，提升程式碼的可讀性與可維護性。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.component.provider.provider;