/**
 * 此包用於存放provider的實現類，provider主要用於提供對應的操作方法
 * 利用provider對外提供操作方法，對外隱藏實現細節
 * 這樣可以使代碼更加模塊化，提高代碼的可讀性和可維護性
 * 1. AbstractRedisCacheProvider: 用於提供 Redis 緩存的操作方法 {@link xyz.dowob.filemanagement.component.provider.provider.AbstractRedisCacheProvider}
 * 2. FolderListTreeProvider: 用於提供 FolderListTree 的操作方法 {@link xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider}
 * 3. GridFsProvider: 用於提供 GridFs 的操作方法 {@link xyz.dowob.filemanagement.component.provider.provider.GridFsProvider}
 * 4. RedisProvider: 用於提供 Redis 的操作方法 {@link xyz.dowob.filemanagement.component.provider.provider.RedisProvider}
 */
package xyz.dowob.filemanagement.component.provider.provider;