package xyz.dowob.filemanagement.data.file.bo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName BatchBO
 * @create 2025/4/9
 * @Version 1.0
 **/
@Getter
@Setter
public class BatchBO<T> {
    List<T> data;
}
