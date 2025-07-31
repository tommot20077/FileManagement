/**
 * 資料類型轉換工具套件，實現不同資料格式與存儲介質之間的雙向轉換。
 *
 * <p>包含 Byte/Boolean 轉換器（{@link xyz.dowob.filemanagement.convert.EntityByteBooleanMapper}，用於 MySQL TINYINT 欄位）、
 * Set/JSON 序列化器（{@link xyz.dowob.filemanagement.convert.EntityJsonSetMapper}，處理集合資料）和 String/Byte 編碼器
 * （{@link xyz.dowob.filemanagement.convert.StringByteCodeMapper}，用於 Redis 資料轉換）三個核心轉換器。</p>
 *
 * <p>所有轉換器支援 Spring Data 和 Lettuce 框架，確保資料一致性和轉換效能。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

package xyz.dowob.filemanagement.convert;