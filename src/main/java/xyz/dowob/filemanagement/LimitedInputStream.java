package xyz.dowob.filemanagement;

import org.jetbrains.annotations.NotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 過濾流，用於限制輸入流的大小
 * 原有的InputStream在流式處理中如果需要跳過一部分的數據，會有堵塞的問題
 * 這裡對InputStream進行了封裝，並且限制了輸入流的大小
 * 當輸入流的大小超過限制時，會自動關閉流
 *
 * @author yuan
 * @program FileManagement
 * @ClassName LimitedInputStream
 * @create 2025/3/15
 * @Version 1.0
 **/
public class LimitedInputStream extends FilterInputStream {
    /**
     * 剩餘的字節數
     */
    private long remaining;

    /**
     * 創建一個新的LimitedInputStream
     *
     * @param in    輸入流
     * @param start 開始位置
     * @param end   結束位置
     *
     * @throws IOException IO異常
     */
    @SuppressWarnings("all")
    public LimitedInputStream(InputStream in, long start, long end) throws IOException {
        super(in);
        this.remaining = end - start + 1;
        in.skip(start);
    }


    /**
     * 讀取字節數據
     *
     * @param b   字節數組
     * @param off 偏移量
     * @param len 長度
     *
     * @return 讀取的字節數
     *
     * @throws IOException IO異常
     */
    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) {
            return -1;
        }
        int bytesRead = super.read(b, off, (int) Math.min(len, remaining));
        if (bytesRead > 0) {
            remaining -= bytesRead;
        }
        return bytesRead;
    }
}