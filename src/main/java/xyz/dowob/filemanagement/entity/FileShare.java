package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName FileShare
 * @description
 * @create 2024-09-26 17:30
 * @Version 1.0
 **/
@Table("shared_files")
@Getter
@Setter
public class FileShare {
    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("user_file_metadata_id")
    private Long userFileMetadataId;

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FileShare fileShare = (FileShare) obj;
        return Objects.equals(id, fileShare.id);
    }

}
