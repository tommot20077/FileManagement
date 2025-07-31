package xyz.dowob.filemanagement.functionInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.User;

/**
 * 檢查使用者對指定資源存取權限的函數式介面。此介面採用響應式程式設計模式，
 * 以非阻塞方式執行權限檢查。
 * <p>
 * 權限檢查通過時回傳空的 Mono，檢查失敗時回傳包含異常的 Mono。
 * 通過泛型設計，可針對不同類型的資源進行權限驗證。
 * </p>
 * <p>
 * 典型應用包括檔案存取權限驗證、資料夾操作權限檢查和用戶功能權限控制等。
 * </p>
 *
 * @param <T> 需要進行權限檢查的資源類型
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see User
 * @see Mono
 */

@FunctionalInterface
public interface Permission<T> {
    /**
     * 檢查指定用戶對特定資源的存取權限。
     * <p>
     * 此方法以非阻塞方式執行權限檢查。當權限檢查通過時，回傳空的 Mono；
     * 當權限檢查失敗時，回傳包含具體異常的 Mono。
     * </p>
     *
     * @param user 需要檢查權限的用戶物件
     * @param t    需要進行權限檢查的資源物件
     * @return 包含異常的 Mono 表示權限檢查失敗，空的 Mono 表示權限檢查通過
     */
    Mono<Throwable> check(User user, T t);
}
