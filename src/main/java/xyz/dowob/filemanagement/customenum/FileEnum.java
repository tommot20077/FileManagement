package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件類型的枚舉類，用於標記文件的類型
 *
 * @author yuan
 * @program File-Management
 * @ClassName FileEnum
 * @description
 * @create 2024-09-20 22:30
 * @Version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum FileEnum {
    /**
     * 照片類型
     */
    IMAGE("照片"),
    /**
     * 影片類型
     */
    VIDEO("影片"),
    /**
     * 音樂類型
     */
    MUSIC("音樂"),
    /**
     * 文件類型
     */
    DOCUMENT("文件"),
    /**
     * 壓縮檔類型
     */
    ZIP("壓縮檔"),
    /**
     * 其他類型
     */
    OTHER("其他");

    /**
     * 文件類型
     */
    private final String type;

    private static final Map<String, FileEnum> MIME_TYPE_MAPPING = new HashMap<>();
    private static final Map<FileEnum, Map<String, String>> FILE_ENUM_MAP = new HashMap<>();

    static {
        // 圖片類型
        MIME_TYPE_MAPPING.put("image/jpeg", FileEnum.IMAGE);
        MIME_TYPE_MAPPING.put("image/png", FileEnum.IMAGE);
        MIME_TYPE_MAPPING.put("image/gif", FileEnum.IMAGE);
        MIME_TYPE_MAPPING.put("image/webp", FileEnum.IMAGE);

        // 視頻類型
        MIME_TYPE_MAPPING.put("video/mp4", FileEnum.VIDEO);
        MIME_TYPE_MAPPING.put("video/mpeg", FileEnum.VIDEO);
        MIME_TYPE_MAPPING.put("video/webm", FileEnum.VIDEO);

        // 音頻類型
        MIME_TYPE_MAPPING.put("audio/mpeg", FileEnum.MUSIC);
        MIME_TYPE_MAPPING.put("audio/wav", FileEnum.MUSIC);
        MIME_TYPE_MAPPING.put("audio/ogg", FileEnum.MUSIC);

        // 文檔類型
        MIME_TYPE_MAPPING.put("application/pdf", FileEnum.DOCUMENT);
        MIME_TYPE_MAPPING.put("application/msword", FileEnum.DOCUMENT);
        MIME_TYPE_MAPPING.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", FileEnum.DOCUMENT);

        // 壓縮文件類型
        MIME_TYPE_MAPPING.put("application/zip", FileEnum.ZIP);
        MIME_TYPE_MAPPING.put("application/x-rar-compressed", FileEnum.ZIP);
        MIME_TYPE_MAPPING.put("application/x-7z-compressed", FileEnum.ZIP);
    }

    static {
        Map<String, String> imageMap = new HashMap<>();
        imageMap.put("jpeg", "image/jpeg");
        imageMap.put("jpg", "image/jpeg");
        imageMap.put("png", "image/png");
        imageMap.put("gif", "image/gif");
        FILE_ENUM_MAP.put(FileEnum.IMAGE, imageMap);

        Map<String, String> videoMap = new HashMap<>();
        videoMap.put("mp4", "video/mp4");
        videoMap.put("mpeg", "video/mpeg");
        videoMap.put("webm", "video/webm");
        FILE_ENUM_MAP.put(FileEnum.VIDEO, videoMap);

        Map<String, String> musicMap = new HashMap<>();
        musicMap.put("mp3", "audio/mpeg");
        musicMap.put("wav", "audio/wav");
        musicMap.put("ogg", "audio/ogg");
        FILE_ENUM_MAP.put(FileEnum.MUSIC, musicMap);

        Map<String, String> documentMap = new HashMap<>();
        documentMap.put("pdf", "application/pdf");
        documentMap.put("doc", "application/msword");
        documentMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        FILE_ENUM_MAP.put(FileEnum.DOCUMENT, documentMap);

        Map<String, String> zipMap = new HashMap<>();
        zipMap.put("zip", "application/zip");
        zipMap.put("rar", "application/x-rar-compressed");
        zipMap.put("7z", "application/x-7z-compressed");
        FILE_ENUM_MAP.put(FileEnum.ZIP, zipMap);
    }

    public static FileEnum fromMimeType(String mimeType) {
        return MIME_TYPE_MAPPING.getOrDefault(mimeType, FileEnum.OTHER);
    }

    public static String getMediaType(FileEnum fileEnum, String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        Map<String, String> extensionMap = FILE_ENUM_MAP.get(fileEnum);
        if (extensionMap != null) {
            return extensionMap.getOrDefault(extension, getDefaultMediaType(fileEnum));
        }
        return getDefaultMediaType(fileEnum);
    }

    private static String getDefaultMediaType(FileEnum fileEnum) {
        return switch (fileEnum) {
            case IMAGE -> "image/jpeg";
            case VIDEO -> "video/mp4";
            case MUSIC -> "audio/mp3";
            case DOCUMENT -> "application/pdf";
            case ZIP -> "application/zip";
            default -> "application/octet-stream";
        };
    }

}
