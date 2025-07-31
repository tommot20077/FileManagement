/**
 * 持久化物件（Persistent Object）包，包含資料持久化和序列化的特殊物件。
 * 
 * <p>此包中的類別主要用於處理複雜的資料結構持久化，包括 JSON 序列化、
 * 外部函式庫物件的適配和反應式程式設計中的資料流管理。
 * 
 * <p>包含的主要類別：
 * <ul>
 * <li>{@link xyz.dowob.filemanagement.data.file.po.CustomPatchPO} - 文本差分持久化</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.po.FluxDataPO} - 反應式資料流封裝</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.po.QuillContentPO} - Quill 編輯器內容</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.po.ShareUserEditPO} - 共享用戶編輯記錄</li>
 * </ul>
 * 
 * <p>這些物件專注於特定技術需求的資料處理，提供與外部系統和函式庫的整合支援。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

package xyz.dowob.filemanagement.data.file.po;