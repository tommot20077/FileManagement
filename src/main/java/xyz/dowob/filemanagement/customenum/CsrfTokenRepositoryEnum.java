package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CsrfTokenRepositoryEnum，用於定義CsrfTokenRepository的存儲方式
 * 並可以依照不同的存儲方式進行相應的處理
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CsrfTokenRepositoryEnum
 * @create 2025/3/6
 * @Version 1.0
 **/
@Getter
@RequiredArgsConstructor
public enum CsrfTokenRepositoryEnum {
    /**
     * 本地存儲
     */
    LOCAL,

    /**
     * Redis存儲
     */
    REDIS;
}
