package xyz.dowob.filemanagement.data.file.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

/**
 * 用於映射數據緩衝區的PO類，封裝了數據緩衝區的Flux對象。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName DataBufferPO
 * @create 2025/3/16
 * @Version 1.0
 **/

@Data
public class DataBufferPO {
    /**
     * 數據緩衝區的Flux對象
     */
    @JsonIgnore
    private Flux<DataBuffer> dataBufferFlux;
}
