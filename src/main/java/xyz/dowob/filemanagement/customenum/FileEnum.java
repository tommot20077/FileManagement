package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
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
     * 個人文件類型
     */
    ONLINE_DOCUMENT("線上文件"),

    /**
     * 資料夾類型
     */
    FOLDER("資料夾"),

    /**
     * 其他類型
     */
    OTHER("其他");

    /**
     * MIME類型與文件類型的映射
     */
    private static final Map<String, FileEnum> MIME_TYPE_MAPPING = new HashMap<>();

    /**
     * 文件類型與擴展名的映射
     */
    private static final Map<FileEnum, Map<String, String>> FILE_ENUM_MAP = new HashMap<>();

    /**
     * Tika 實例
     */
    private static final Tika TIKA = new Tika();

    /**
     * Microsoft Generic MIME 類型
     */
    private static final String MICROSOFT_GENERIC = "application/x-tika-ooxml";

    static {
        // 圖片類型
        MIME_TYPE_MAPPING.put("image/jpeg", FileEnum.IMAGE);
        MIME_TYPE_MAPPING.put("image/png", FileEnum.IMAGE);
        MIME_TYPE_MAPPING.put("image/gif", FileEnum.IMAGE);
        MIME_TYPE_MAPPING.put("image/webp", FileEnum.IMAGE);
        MIME_TYPE_MAPPING.put("image", FileEnum.IMAGE);

        // 影片類型
        MIME_TYPE_MAPPING.put("video/mp4", FileEnum.VIDEO);
        MIME_TYPE_MAPPING.put("video/mpeg", FileEnum.VIDEO);
        MIME_TYPE_MAPPING.put("video/webm", FileEnum.VIDEO);
        MIME_TYPE_MAPPING.put("video", FileEnum.VIDEO);

        // 音頻類型
        MIME_TYPE_MAPPING.put("audio/mpeg", FileEnum.MUSIC);
        MIME_TYPE_MAPPING.put("audio/wav", FileEnum.MUSIC);
        MIME_TYPE_MAPPING.put("audio/ogg", FileEnum.MUSIC);
        MIME_TYPE_MAPPING.put("audio", FileEnum.MUSIC);

        // 文檔類型
        MIME_TYPE_MAPPING.put("application/pdf", FileEnum.DOCUMENT);
        MIME_TYPE_MAPPING.put("application/msword", FileEnum.DOCUMENT);
        MIME_TYPE_MAPPING.put("application/vnd.ms-excel", FileEnum.DOCUMENT);
        MIME_TYPE_MAPPING.put("application/x-tika-ooxml", FileEnum.DOCUMENT);
        MIME_TYPE_MAPPING.put("application/vnd.ms-powerpoint", FileEnum.DOCUMENT);
        MIME_TYPE_MAPPING.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FileEnum.DOCUMENT);
        MIME_TYPE_MAPPING.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", FileEnum.DOCUMENT);
        MIME_TYPE_MAPPING.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", FileEnum.DOCUMENT);

        // 壓縮文件類型
        MIME_TYPE_MAPPING.put("application/zip", FileEnum.ZIP);
        MIME_TYPE_MAPPING.put("application/x-rar-compressed", FileEnum.ZIP);
        MIME_TYPE_MAPPING.put("application/x-7z-compressed", FileEnum.ZIP);
    }

    static {
        // 圖片類型
        Map<String, String> imageMap = new HashMap<>();
        imageMap.put("jpeg", "image/jpeg");
        imageMap.put("jpg", "image/jpeg");
        imageMap.put("png", "image/png");
        imageMap.put("gif", "image/gif");
        FILE_ENUM_MAP.put(FileEnum.IMAGE, imageMap);

        // 影片類型
        Map<String, String> videoMap = new HashMap<>();
        videoMap.put("mp4", "video/mp4");
        videoMap.put("mpeg", "video/mpeg");
        videoMap.put("webm", "video/webm");
        FILE_ENUM_MAP.put(FileEnum.VIDEO, videoMap);

        // 音樂類型
        Map<String, String> musicMap = new HashMap<>();
        musicMap.put("mp3", "audio/mpeg");
        musicMap.put("wav", "audio/wav");
        musicMap.put("ogg", "audio/ogg");
        FILE_ENUM_MAP.put(FileEnum.MUSIC, musicMap);

        // 文件類型
        Map<String, String> documentMap = new HashMap<>();
        documentMap.put("pdf", "application/pdf");
        documentMap.put("doc", "application/msword");
        documentMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        FILE_ENUM_MAP.put(FileEnum.DOCUMENT, documentMap);

        // 壓縮檔類型
        Map<String, String> zipMap = new HashMap<>();
        zipMap.put("zip", "application/zip");
        zipMap.put("rar", "application/x-rar-compressed");
        zipMap.put("7z", "application/x-7z-compressed");
        FILE_ENUM_MAP.put(FileEnum.ZIP, zipMap);
    }

    /**
     * 文件類型
     */
    private final String type;

    /**
     * 根據 MIME 類型獲取文件類型，如果找不到對應的文件類型，則返回其他類型
     *
     * @param mimeType MIME 類型
     *
     * @return 返回文件類型
     */
    public static FileEnum fromMimeType(String mimeType) {
        if (MIME_TYPE_MAPPING.containsKey(mimeType)) {
            return MIME_TYPE_MAPPING.get(mimeType);
        }

        String generalType = mimeType.split("/")[0].toLowerCase();
        return MIME_TYPE_MAPPING.getOrDefault(generalType, FileEnum.OTHER);
    }


    /**
     * 根據文件名獲取文件類型，如果找不到對應的文件類型
     * 則檢查是否有對應的 Tika 類型，然後返回對應的文件類型
     * 若果都找不到，則返回默認的文件類型
     *
     * @param fileEnum 文件類型
     * @param filename 文件名
     *
     * @return 返回文件類型
     */
    public static String getMediaType(@NonNull FileEnum fileEnum, String filename) {
        Map<String, String> extensionMap = FILE_ENUM_MAP.get(fileEnum);
        if (extensionMap != null) {
            String extension = FilenameUtils.getExtension(filename).toLowerCase();
            String mimeType = extensionMap.get(extension);

            if (StringUtils.hasText(mimeType)) {
                return mimeType;
            }
        }
        return TIKA.detect(filename);
    }

    /**
     * 獲取檔案的 MIME 類型
     * 這邊使用 byte[] 來獲取檔案的 MIME 類型
     * 當檢測到的 MIME 類型為 application/x-tika-ooxml 時，則使用檔案名稱來檢測
     *
     * @param bytes 檔案的 byte[]
     *
     * @return 返回檔案的 MIME 類型
     */
    public static String getMediaType(byte[] bytes, String filename) {
        String mimeType = TIKA.detect(bytes);
        if (MICROSOFT_GENERIC.equals(mimeType)) {
            return TIKA.detect(filename);
        }
        return mimeType;
    }


    /**
     * 獲取檔案的 MIME 類型
     * 這邊使用 Flux<DataBuffer> 來獲取檔案的 MIME 類型
     * 當檢測到的 MIME 類型為 application/x-tika-ooxml 時，則使用檔案名稱來檢測
     *
     * @param dataBufferFlux 檔案的 Flux<DataBuffer>
     * @param filename       檔案的名稱
     *
     * @return 返回檔案的 MIME 類型
     */
    public static Mono<detectRecord> getMediaType(Flux<DataBuffer> dataBufferFlux, String filename) {
        return DataBufferUtils.join(dataBufferFlux).map(dataBuffer -> {
            try (InputStream is = dataBuffer.asInputStream()) {
                String mimeType = TIKA.detect(is, filename);
                if (MICROSOFT_GENERIC.equals(mimeType)) {
                    mimeType = TIKA.detect(filename);
                }
                return new detectRecord(mimeType, dataBufferFlux);
            } catch (IOException e) {
                return new detectRecord(MediaType.APPLICATION_OCTET_STREAM_VALUE, dataBufferFlux);
            } finally {
                DataBufferUtils.release(dataBuffer);
            }
        }).defaultIfEmpty(new detectRecord(MediaType.APPLICATION_OCTET_STREAM_VALUE, dataBufferFlux));
    }


    /**
     * 檢測類型紀錄類
     * 將 Flux<DataBuffer> 與 MIME 類型進行綁定
     *
     * @param mimeType       MIME 類型
     * @param dataBufferFlux 數據流
     */
    public record detectRecord(String mimeType, Flux<DataBuffer> dataBufferFlux) {
    }
}
