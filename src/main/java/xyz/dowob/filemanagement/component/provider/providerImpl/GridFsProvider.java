package xyz.dowob.filemanagement.component.provider.providerImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName GridFsProvider
 * @description
 * @create 2024-09-27 00:55
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
public class GridFsProvider {
    private final GridFsTemplate gridFsTemplate;



}
