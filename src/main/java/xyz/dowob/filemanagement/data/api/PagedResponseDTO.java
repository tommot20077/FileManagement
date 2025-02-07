package xyz.dowob.filemanagement.data.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName PagedResponseDTO
 * @create 2025/2/6
 * @Version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PagedResponseDTO<T> {
    private List<T> data = List.of();
    private int totalPages = 0;
    private int currentPage = 0;
    private int pageSize = 0;
    private long totalElements = 0;
}
