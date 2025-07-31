/**
 * 工廠模式實現模組，提供動態創建可設定服務實例的機制。
 *
 * <p>當服務需要根據不同使用情境進行個性化設定時，透過工廠模式提供靈活的實例創建方案。
 * 支援靜態工廠方法，無需實例化即可創建所需的服務對象。</p>
 *
 * <p>包含的工廠類：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.factory.ContentConvertProviderFactory}：
 *       內容轉換提供者工廠，支援多種轉換格式</li>
 * </ul>
 * </p>
 *
 * <p>工廠設定由 config 子包提供，確保創建的實例符合特定需求。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

package xyz.dowob.filemanagement.component.provider.factory;