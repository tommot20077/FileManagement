package xyz.dowob.filemanagement.dto.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用於定義控制器請求的返回結果的數據傳輸對象
 *
 * @param <T> 泛型
 *
 * @author yuan
 * @program File-Management
 * @ClassName ApiResponseDTO
 * @description
 * @create 2024-09-15 03:36
 * @Version 1.0
 */
@Data
@AllArgsConstructor
public class ApiResponseDTO <T> {
    /**
     * 返回的時間戳
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    /**
     * 返回的狀態碼
     */
    private int status;

    /**
     * 返回的請求路徑
     */
    private String path;

    /**
     * 返回的消息
     */
    private String message;

    /**
     * 返回的數據
     */
    private T data;
}
