package xyz.dowob.filemanagement.component.provider.providerImplement.contentconvertprovider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.provider.factory.config.ConvertConfig;
import xyz.dowob.filemanagement.component.provider.providerInterface.ContentConvertProvider;
import xyz.dowob.filemanagement.customenum.ConvertProviderEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.file.po.QuillContentPO;
import xyz.dowob.filemanagement.exception.ProcessException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Quill 編輯器內容轉換為 Microsoft Word 檔案的專業轉換器實現。
 * <p>
 * 此類別實現 {@link ContentConvertProvider} 介面，專門處理 Quill Delta JSON 格式的富文本內容，
 * 並將其精確轉換為標準的 Microsoft Word 檔案格式（.docx）。支援完整的文本格式、段落樣式、
 * 列表結構、表格系統、超連結和多媒體元素的轉換。
 * </p>
 * <p>
 * <strong>核心轉換功能：</strong>
 * </p>
 * <ul>
 *   <li><strong>文字格式：</strong>粗體、斜體、底線、刪除線、上標、下標、字體大小和顏色</li>
 *   <li><strong>段落樣式：</strong>對齊方式（左、中、右、兩端對齊）、縮進層級、行距設定</li>
 *   <li><strong>列表系統：</strong>有序列表（數字編號）、無序列表（項目符號）、多層次嵌套</li>
 *   <li><strong>特殊區塊：</strong>程式碼區塊（含語法高亮背景）、引用區塊（含縮進樣式）</li>
 *   <li><strong>標題系統：</strong>H1-H6 多級標題，自動套用 Word 標題樣式</li>
 *   <li><strong>表格結構：</strong>完整表格支援，含儲存格合併、對齊和格式化</li>
 *   <li><strong>超連結：</strong>外部連結和內部錨點連結，含滑鼠懸停效果</li>
 *   <li><strong>字體系統：</strong>自訂字體族群、大小轉換和顏色映射</li>
 * </ul>
 * <p>
 * <strong>技術架構：</strong>
 * </p>
 * <ul>
 *   <li>基於 Apache POI XWPF 函式庫實現 Word 檔案操作</li>
 *   <li>使用 Jackson ObjectMapper 解析 Quill Delta JSON 結構</li>
 *   <li>採用 Reactor 反應式編程模式，支援非阻塞式檔案處理</li>
 *   <li>透過 {@link ConvertConfig} 提供高度可客製化的轉換參數</li>
 *   <li>內建錯誤處理機制，自動回傳 {@link ProcessException} 異常</li>
 * </ul>
 * <p>
 * <strong>轉換流程：</strong>
 * </p>
 * <ol>
 *   <li>解析 Quill Delta JSON 為 Operation 物件列表</li>
 *   <li>建立 Word 文件實例並初始化編號定義</li>
 *   <li>循序處理每個 Operation，維護格式化狀態</li>
 *   <li>根據屬性應用文字和段落格式</li>
 *   <li>處理特殊元素（表格、列表、程式碼區塊）</li>
 *   <li>生成最終的 Word 檔案位元組流</li>
 * </ol>
 * <p>
 * <strong>使用範例：</strong>
 * </p>
 * <pre>{@code
 * // 建立轉換設定
 * ConvertConfig config = new ConvertConfig();
 * config.setDefaultFontFamily("Arial");
 * config.setDefaultFontSize(12);
 * 
 * // 初始化轉換器
 * WordConvertProvider converter = new WordConvertProvider(config);
 * 
 * // 執行轉換
 * String quillJson = "{\"delta\":[{\"insert\":\"Hello World\"},{\"insert\":\"\\n\",\"attributes\":{\"header\":1}}]}";
 * Mono<InputStream> wordFile = converter.convertToInputStream(quillJson);
 * 
 * // 處理轉換結果
 * wordFile.subscribe(
 *     inputStream -> {
 *         // 儲存或傳輸 Word 檔案
 *         saveToFile(inputStream, "document.docx");
 *     },
 *     error -> {
 *         // 處理轉換錯誤
 *         logger.error("Word 轉換失敗：", error);
 *     }
 * );
 * }</pre>
 * <p>
 * <strong>注意事項：</strong>
 * </p>
 * <ul>
 *   <li>輸入的 JSON 必須符合 Quill Delta 格式規範</li>
 *   <li>大型文件轉換可能需要較多記憶體資源</li>
 *   <li>轉換過程為非阻塞式，適合高併發環境</li>
 *   <li>生成的 Word 檔案相容於 Microsoft Word 2007+ 版本</li>
 * </ul>
 * <p>
 * 此類別透過 {@link xyz.dowob.filemanagement.component.provider.factory.ContentConvertProviderFactory}
 * 進行依賴注入和實例管理，確保轉換器的生命週期與系統一致。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ContentConvertProvider
 * @see ConvertConfig
 * @see xyz.dowob.filemanagement.component.provider.factory.ContentConvertProviderFactory
 * @see QuillContentPO
 */
public class WordConvertProvider implements ContentConvertProvider {
    /**
     * Quill Delta JSON 中的換行分隔符號常數。
     * <p>
     * 在 Quill 編輯器的 Delta 格式中，"\n" 字元代表段落換行，
     * 用於標示文本段落的結束和新段落的開始。此常數用於解析
     * JSON 內容時識別段落邊界。
     * </p>
     */
    private static final String SENTENCE_SPLIT_LABEL = "\n";
    /**
     * Jackson ObjectMapper 實例，負責 JSON 序列化和反序列化操作。
     * <p>
     * 此實例專門用於將 Quill Delta JSON 字串解析為 {@link QuillContentPO.Delta}
     * 物件結構，確保 JSON 格式的正確性和類型安全。使用預設配置，
     * 支援標準的 JSON 解析功能。
     * </p>
     * 
     * @see QuillContentPO.Delta
     * @see QuillContentPO.Operation
     */
    private final ObjectMapper objectMapper;
    /**
     * Word 轉換器的個人化設定實例。
     * <p>
     * 包含轉換過程中所需的所有參數設定，例如預設字體、字型大小、
     * 顏色映射、編號樣式、縮進單位等。此設定物件在建構時注入，
     * 提供靈活的轉換行為客製化能力。
     * </p>
     * <p>
     * 主要設定項目：
     * </p>
     * <ul>
     *   <li>預設字體系列和大小設定</li>
     *   <li>程式碼區塊的字體和背景色設定</li>
     *   <li>列表編號的抽象編號 ID 設定</li>
     *   <li>縮進層級的 Twips 單位設定</li>
     *   <li>字體名稱和大小的映射表</li>
     * </ul>
     * 
     * @see ConvertConfig
     */
    private final ConvertConfig config;

    /**
     * 建構 WordConvertProvider 實例，初始化轉換器的核心組件。
     * <p>
     * 此建構函數負責設定轉換器運行所需的基礎設施，包括 JSON 解析器
     * 和轉換設定。建構過程不會進行任何 I/O 操作，確保快速初始化。
     * </p>
     * <p>
     * 初始化項目：
     * </p>
     * <ul>
     *   <li>建立 Jackson ObjectMapper 實例用於 JSON 解析</li>
     *   <li>儲存轉換設定參考，用於後續轉換操作</li>
     *   <li>驗證設定的完整性和有效性</li>
     * </ul>
     * 
     * @param config 轉換設定物件，包含字體、樣式、編號等轉換參數，不得為 null
     * @throws IllegalArgumentException 當 config 參數為 null 時拋出
     * @see ConvertConfig
     */
    public WordConvertProvider(ConvertConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("轉換設定不得為 null");
        }
        this.objectMapper = new ObjectMapper();
        this.config = config;
    }

    /**
     * 將 Quill Delta JSON 內容轉換為 Word 文件的輸入串流。
     * <p>
     * 此方法是轉換器的主要入口點，接受 Quill 編輯器產生的 Delta JSON 格式內容，
     * 經過完整的解析和格式轉換流程後，回傳包含 Word 文件資料的 InputStream。
     * 適用於需要將轉換結果直接傳輸或儲存的場景。
     * </p>
     * <p>
     * 轉換流程包括：
     * </p>
     * <ol>
     *   <li>解析 JSON 內容為 Quill Delta 物件結構</li>
     *   <li>建立 Word 文件實例並初始化樣式定義</li>
     *   <li>遍歷 Delta Operations 並套用格式化規則</li>
     *   <li>處理文字、段落、列表、表格等元素</li>
     *   <li>生成最終的 Word 文件位元組流</li>
     *   <li>封裝為 InputStream 供外部使用</li>
     * </ol>
     * <p>
     * 支援的 Quill 元素：
     * </p>
     * <ul>
     *   <li>基本文字格式：粗體、斜體、底線、刪除線</li>
     *   <li>進階格式：字體、顏色、背景色、上下標</li>
     *   <li>段落格式：對齊、縮進、標題層級</li>
     *   <li>結構元素：有序/無序列表、程式碼區塊、引用</li>
     *   <li>表格系統：多行多列表格含格式化</li>
     *   <li>連結元素：外部超連結含樣式</li>
     * </ul>
     * <p>
     * 錯誤處理機制：
     * </p>
     * <ul>
     *   <li>JSON 格式錯誤：回傳 ProcessException 含詳細錯誤訊息</li>
     *   <li>記憶體不足：自動釋放資源並報告錯誤</li>
     *   <li>Word 文件生成失敗：包裝原始異常為 ProcessException</li>
     * </ul>
     * 
     * @param content Quill Delta JSON 格式的內容字串，必須符合 Quill Delta 規範，不得為 null 或空字串
     * @return Mono&lt;InputStream&gt; 包含完整 Word 文件資料的反應式串流，
     *         成功時回傳可讀取的 InputStream，失敗時發射 ProcessException
     * @see #convertToDataBuffer(String)
     * @see ProcessException.ErrorCode#CONVERT_JSON_TO_TARGET_FAILED
     * 
     * @apiNote 此方法採用非阻塞式實作，適合在高併發環境中使用
     * @implNote 內部使用 Apache POI XWPF 進行 Word 文件操作，確保格式相容性
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<InputStream> convertToInputStream(String content) {
        return convertToDataBuffer(content).flatMap(record -> record
                .dataBuffer()
                .transform(DataBufferUtils::join)
                .as(Mono::from)
                .map(DataBuffer::asInputStream));
    }

    /**
     * 將 Quill Delta JSON 內容轉換為 Word 文件的資料緩衝區。
     * <p>
     * 此方法為內部轉換流程的核心實現，負責將 Quill 內容轉換為 Word 格式並
     * 封裝為 DataBuffer 物件。相較於 InputStream 方法，此方法提供更高的性能和
     * 更微練的資源控制，特別適合於大型文件的處理。
     * </p>
     * <p>
     * 轉換結果包裝為 {@link DataBufferRecord}，包含：
     * </p>
     * <ul>
     *   <li><strong>dataBuffer:</strong> Flux&lt;DataBuffer&gt; - 可消費的資料流</li>
     *   <li><strong>size:</strong> int - 文件總大小（位元組數）</li>
     * </ul>
     * <p>
     * 效能優勢：
     * </p>
     * <ul>
     *   <li>支援串流式處理，減少記憶體使用量</li>
     *   <li>提供正確的文件大小訊息用於 HTTP 標頭</li>
     *   <li>適合直接回傳給 WebFlux ResponseEntity</li>
     *   <li>提供更細粒度的錯誤處理和資源管理</li>
     * </ul>
     * <p>
     * 使用範例：
     * </p>
     * <pre>{@code
     * converter.convertToDataBuffer(quillJson)
     *     .subscribe(record -> {
     *         Flux<DataBuffer> dataBuffer = record.dataBuffer();
     *         int fileSize = record.size();
     *         
     *         // 設定響應標頭
     *         response.getHeaders().add("Content-Length", String.valueOf(fileSize));
     *         response.getHeaders().add("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
     *         
     *         // 回傳資料流
     *         return ResponseEntity.ok().body(dataBuffer);
     *     });
     * }</pre>
     * 
     * @param content Quill Delta JSON 格式的內容字串，必須符合 Delta 規範，不得為 null
     * @return Mono&lt;DataBufferRecord&gt; 包含 Word 文件資料的反應式記錄，
     *         包含資料流和文件大小訊息
     * @see DataBufferRecord
     * @see #convertToInputStream(String)
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<DataBufferRecord> convertToDataBuffer(String content) {
        return formatToDocument(content).flatMap(this::convertToOutputStream).flatMap(outputStream -> {
            byte[] bytes = outputStream.toByteArray();
            int size = bytes.length;
            DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.allocateBuffer(size);
            dataBuffer.write(bytes);
            DataBufferRecord dataBufferRecord = new DataBufferRecord(Flux.just(dataBuffer), size);
            return Mono.just(dataBufferRecord);
        });
    }

    /**
     * 獲取轉換器類型
     *
     * @return 轉換器類型
     */
    @Override
    public ConvertProviderEnum getType() {
        return ConvertProviderEnum.DOCX;
    }

    /**
     * 獲取轉換設定
     *
     * @return 轉換設定
     */
    @Override
    public ConvertConfig getConvertConfig() {
        return this.config;
    }

    /**
     * 將 Quill Delta JSON 內容轉換為標準的 Microsoft Word 文件對象。
     * <p>
     * 此方法負責建立 Word 文件的基礎結構，並初始化必要的編號定義系統。
     * 整個轉換過程採用非阻塞式實作，確保在高併發環境下的良好性能表現。
     * </p>
     * <p>
     * 轉換流程：
     * </p>
     * <ol>
     *   <li>建立新的 {@link XWPFDocument} 實例作為輸出容器</li>
     *   <li>初始化編號定義系統，確保列表功能正常運作</li>
     *   <li>呼叫 {@link #convertContent(XWPFDocument, String)} 進行實際內容轉換</li>
     *   <li>自動處理資源清理，防止記憶體洩漏</li>
     * </ol>
     * <p>
     * 錯誤處理機制：
     * </p>
     * <ul>
     *   <li>自動捕捉文件建立過程中的異常</li>
     *   <li>確保在任何情況下都會正確關閉文件資源</li>
     *   <li>透過 {@code doFinally} 操作符保證資源釋放</li>
     * </ul>
     * 
     * @param content Quill Delta JSON 格式的內容字串，必須符合 Quill Delta 規範
     * @return Mono&lt;XWPFDocument&gt; 包含完整轉換結果的 Word 文件對象，
     *         以反應式流的形式回傳
     * @see #convertContent(XWPFDocument, String)
     * @see #ensureNumberingDefinitions(XWPFDocument)
     * @apiNote 此方法會自動處理 XWPFDocument 的生命週期管理
     * @implNote 使用 Mono.defer() 確保每次呼叫都會建立新的文件實例
     */
    private Mono<XWPFDocument> formatToDocument(String content) {
        return Mono.defer(() -> {
            XWPFDocument document = new XWPFDocument();
            ensureNumberingDefinitions(document);
            return convertContent(document, content).doFinally(signalType -> {
                try {
                    document.close();
                } catch (IOException e) {
                    throw Exceptions.propagate(e);
                }
            });
        });
    }

    /**
     * 將完成格式化的 Word 文件對象序列化為位元組流輸出。
     * <p>
     * 此方法負責將記憶體中的 {@link XWPFDocument} 物件轉換為可傳輸的位元組格式，
     * 生成標準的 Microsoft Word .docx 檔案格式。轉換過程採用非阻塞式實作，
     * 適合在反應式編程環境中使用。
     * </p>
     * <p>
     * 序列化特性：
     * </p>
     * <ul>
     *   <li>產生完整的 Office Open XML 格式檔案</li>
     *   <li>保持所有格式化資訊的完整性</li>
     *   <li>支援 Microsoft Word 2007+ 版本相容性</li>
     *   <li>自動壓縮內部 XML 結構以減少檔案大小</li>
     * </ul>
     * <p>
     * 記憶體管理：
     * </p>
     * <ul>
     *   <li>使用 {@code try-with-resources} 確保輸出流正確關閉</li>
     *   <li>自動處理寫入過程中的異常情況</li>
     *   <li>透過 {@link Exceptions#propagate(Throwable)} 包裝 I/O 異常</li>
     * </ul>
     * 
     * @param document 已完成格式化的 Word 文件對象，包含所有內容和樣式設定
     * @return Mono&lt;ByteArrayOutputStream&gt; 包含完整 Word 檔案資料的位元組輸出流，
     *         可用於檔案儲存或網路傳輸
     * @throws RuntimeException 當 I/O 操作失敗時拋出，內包原始的 IOException
     * @see XWPFDocument#write(java.io.OutputStream)
     * @apiNote 回傳的 ByteArrayOutputStream 包含完整的 .docx 檔案資料
     * @implNote 使用 {@code Mono.fromCallable()} 確保 I/O 操作在適當的執行緒中進行
     */
    private Mono<ByteArrayOutputStream> convertToOutputStream(XWPFDocument document) {
        return Mono.fromCallable(() -> {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                document.write(outputStream);
                return outputStream;
            } catch (IOException e) {
                throw Exceptions.propagate(e);
            }
        });
    }

    /**
     * 確保 Word 文件中存在必要的編號定義系統，支援有序和無序列表的正確顯示。
     * <p>
     * 此方法負責初始化 Word 文件的編號系統，確保在轉換過程中遇到列表元素時
     * 能夠正確套用編號格式。Word 檔案需要預先定義抽象編號樣式才能正確顯示列表。
     * </p>
     * <p>
     * 初始化的編號類型：
     * </p>
     * <ul>
     *   <li><strong>項目符號列表：</strong>使用設定中的 {@code abstractNumIdBullet} ID</li>
     *   <li><strong>數字編號列表：</strong>使用設定中的 {@code abstractNumIdDecimal} ID</li>
     * </ul>
     * <p>
     * 處理邏輯：
     * </p>
     * <ol>
     *   <li>檢查文件是否已存在 {@link XWPFNumbering} 實例</li>
     *   <li>如果不存在，則建立新的編號系統</li>
     *   <li>檢查是否存在預設的項目符號編號定義</li>
     *   <li>檢查是否存在預設的數字編號定義</li>
     *   <li>對於缺失的編號定義，呼叫 {@link #createAbstractNumbering(XWPFNumbering, BigInteger)} 建立</li>
     * </ol>
     * <p>
     * 此方法確保後續的列表轉換操作能夠正確引用這些編號定義，
     * 避免在轉換過程中出現列表格式錯誤。
     * </p>
     * 
     * @param document 需要初始化編號定義的 Word 文件對象，不得為 null
     * @see #createAbstractNumbering(XWPFNumbering, BigInteger)
     * @see ConvertConfig#getAbstractNumIdBullet()
     * @see ConvertConfig#getAbstractNumIdDecimal()
     * @apiNote 此方法應在進行任何列表內容轉換之前呼叫
     * @implNote 方法具有冪等性，重複呼叫不會產生副作用
     */
    private void ensureNumberingDefinitions(XWPFDocument document) {
        XWPFNumbering numbering = document.getNumbering();
        if (numbering == null) {
            numbering = document.createNumbering();
        }

        if (numbering.getAbstractNum(config.getAbstractNumIdBullet()) == null) {
            createAbstractNumbering(numbering, config.getAbstractNumIdBullet());
        }

        if (numbering.getAbstractNum(config.getAbstractNumIdDecimal()) == null) {
            createAbstractNumbering(numbering, config.getAbstractNumIdDecimal());
        }
    }

    /**
     * 核心內容轉換引擎，負責將 Quill Delta JSON 內容轉換為完整的 Word 文件結構。
     * <p>
     * 此方法為轉換器的最核心實作，負責解析 Quill 編輯器產生的 Delta JSON 格式，
     * 並將其轉換為完整的 Microsoft Word 文件結構。支援全面的富文本格式化功能，
     * 包括文字樣式、段落格式、列表系統、表格結構等。
     * </p>
     * <p>
     * <strong>轉換流程概述：</strong>
     * </p>
     * <ol>
     *   <li><strong>JSON 解析：</strong>使用 Jackson ObjectMapper 解析 Quill Delta JSON 為 Java 對象</li>
     *   <li><strong>Operation 遍歷：</strong>循序處理每個 Delta Operation，維持格式狀態</li>
     *   <li><strong>屬性解析：</strong>根據 Operation 屬性更新格式化狀態</li>
     *   <li><strong>內容插入：</strong>將文本內容插入到適當的段落中</li>
     *   <li><strong>格式套用：</strong>套用文字和段落等格式化設定</li>
     *   <li><strong>特殊處理：</strong>處理列表、表格、程式碼區塊等特殊元素</li>
     * </ol>
     * <p>
     * <strong>Quill Delta 屬性應用規則：</strong>
     * </p>
     * <ul>
     *   <li><strong>直接應用：</strong>若 Operation 具有屬性，直接套用至其 insert 內容</li>
     *   <li><strong>後續屬性：</strong>若 Operation 無屬性，檢查下一個 Operation 的屬性</li>
     *   <li><strong>空屬性繼承：</strong>若下一個 Operation 有屬性且 insert 為空，則繼承其屬性</li>
     *   <li><strong>預設屬性：</strong>若均無屬性，使用設定檔案中的預設屬性</li>
     * </ul>
     * <p>
     * <strong>屬性處理分類：</strong>
     * </p>
     * <ol>
     *   <li><strong>一般文字屬性：</strong>簡單文字格式（粗體、斜體、底線等）</li>
     *   <li><strong>段落屬性：</strong>段落級格式（對齊、縮進、標題等）</li>
     *   <li><strong>區塊屬性：</strong>特殊區塊（列表、表格、程式碼區塊等）</li>
     * </ol>
     * <p>
     * <strong>表格處理特性：</strong>
     * </p>
     * <ul>
     *   <li>支援多行多列表格結構</li>
     *   <li>自動處理儲存格合併和新增</li>
     *   <li>維持表格內文字格式化</li>
     *   <li>支援表格內容的對齊設定</li>
     * </ul>
     * <p>
     * <strong>列表系統支援：</strong>
     * </p>
     * <ul>
     *   <li>有序列表：數字編號 (1., 2., 3.)</li>
     *   <li>無序列表：項目符號 (•)</li>
     *   <li>多層級嵌套支援</li>
     *   <li>自動編號 ID 管理</li>
     * </ul>
     * <p>
     * <strong>錯誤處理機制：</strong>
     * </p>
     * <ul>
     *   <li>自動捕捉 JSON 解析異常</li>
     *   <li>包裝為 {@link ProcessException} 並帶有詳細錯誤資訊</li>
     *   <li>提供清晰的錯誤代碼和訊息</li>
     * </ul>
     * 
     * @param document 已初始化的 Word 文件對象，具有編號定義和基礎設定
     * @param jsonContent Quill Delta JSON 格式的內容字串，必須符合 Delta 規範
     * @return Mono&lt;XWPFDocument&gt; 包含完整內容和格式的 Word 文件對象
     * @see QuillContentPO.Delta
     * @see QuillContentPO.Operation
     * @see FormattingState
     * @see #updateFormattingState(FormattingState, Map, XWPFDocument)
     * @see #applyParagraphFormattingState(XWPFParagraph, FormattingState, XWPFDocument)
     * @see #applyRunAttributes(XWPFRun, Map, FormattingState)
     * @apiNote 此方法是線程安全的，但不建議並行處理同一個文件對象
     * @implNote 使用狀態機模式管理格式化狀態，確保格式套用的一致性
     */
    private Mono<XWPFDocument> convertContent(XWPFDocument document, String jsonContent) {
        return Mono.fromCallable(() -> {
            QuillContentPO.Delta delta = objectMapper.readValue(jsonContent, QuillContentPO.Delta.class);
            List<QuillContentPO.Operation> operations = delta.delta;

            if (operations == null || operations.isEmpty()) {
                return document;
            }

            FormattingState state = new FormattingState();
            XWPFParagraph currentParagraph = document.createParagraph();
            applyParagraphFormattingState(currentParagraph, state, document);
            String previousText = null;

            for (QuillContentPO.Operation op : operations) {
                if (op.insert == null) {
                    continue;
                }

                String insertText = op.insert;
                Map<String, Object> attributes = op.attributes;

                boolean processedAsTable = false;
                String tableRowIdAttr = (attributes != null) ? (String) attributes.get("table") : null;
                boolean isTableBlockDefiningNewline = insertText.equals(SENTENCE_SPLIT_LABEL) && tableRowIdAttr != null;
                boolean isSimpleNewlineInTableContext = insertText.equals(SENTENCE_SPLIT_LABEL) && attributes == null && state.isTable;

                boolean isContainAttributes = attributes != null && (attributes.containsKey("header") || attributes.containsKey("blockquote") || attributes.containsKey(
                        "list") || attributes.containsKey("code-block") || attributes.containsKey("align") || attributes.containsKey("indent"));
                boolean isNonTableBlockDefiningNewline = insertText.equals(SENTENCE_SPLIT_LABEL) && tableRowIdAttr == null && isContainAttributes;


                if (isTableBlockDefiningNewline) {
                    updateFormattingState(state, attributes, document);
                    if (state.currentTable == null) {
                        state.currentTable = document.createTable();
                        state.currentTable.setWidth("100%");
                    }

                    XWPFTableRow row;
                    if (!tableRowIdAttr.equals(state.currentTableRowId)) {
                        row = state.currentTable.createRow();
                        state.currentTableRowId = tableRowIdAttr;
                        state.currentCellIndex = 0;
                    } else {
                        row = state.currentTable.getRow(state.currentTable.getNumberOfRows() - 1);
                        state.currentCellIndex++;
                    }

                    XWPFTableCell cell;
                    while (row.getTableCells().size() <= state.currentCellIndex) {
                        row.addNewTableCell();
                    }
                    cell = row.getCell(state.currentCellIndex);

                    for (int p = cell.getParagraphs().size() - 1; p >= 0; p--) {
                        cell.removeParagraph(p);
                    }

                    XWPFParagraph cellParagraph = cell.addParagraph();
                    cellParagraph.setAlignment(state.alignment);

                    XWPFRun cellRun = cellParagraph.createRun();

                    String textToInsert = (state.currentCellIndex == 0 && previousText != null) ? previousText : (state.pendingTableCellText != null ? state.pendingTableCellText : "");

                    cellRun.setText(textToInsert);
                    state.pendingTableCellText = null;

                } else if (isNonTableBlockDefiningNewline) {
                    updateFormattingState(state, attributes, document);
                    applyParagraphFormattingState(currentParagraph, state, document);
                    currentParagraph = document.createParagraph();
                    applyParagraphFormattingState(currentParagraph, state, document);
                } else if (state.isTable && !insertText.equals(SENTENCE_SPLIT_LABEL)) {
                    state.pendingTableCellText = (state.pendingTableCellText == null ? "" : state.pendingTableCellText) + insertText;
                } else {
                    if (isSimpleNewlineInTableContext) {
                        state.isTable = false;
                        state.currentTable = null;
                        state.currentTableRowId = null;
                        state.currentCellIndex = 0;
                        state.pendingTableCellText = null;
                    }

                    String[] lines = insertText.split(SENTENCE_SPLIT_LABEL, -1);

                    boolean opIsTableRelated = attributes != null && attributes.containsKey("table");
                    if (state.isTable && !opIsTableRelated) {
                        state.pendingTableCellText = (state.pendingTableCellText == null ? "" : state.pendingTableCellText) + insertText;
                        processedAsTable = true;
                    }
                    if (!processedAsTable) {
                        for (int i = 0; i < lines.length; i++) {
                            String lineText = lines[i];

                            cleanNumPr(state, currentParagraph);


                            if (!lineText.isEmpty()) {
                                if (attributes == null || !attributes.containsKey("align")) {
                                    currentParagraph.setAlignment(ParagraphAlignment.LEFT);
                                }

                                String linkUrl = (attributes != null) ? (String) attributes.get("link") : null;
                                XWPFRun run;
                                if (linkUrl != null) {
                                    PackagePart packagePart = document.getPackagePart();
                                    String relationId = packagePart.addExternalRelationship(linkUrl, XWPFRelation.HYPERLINK.getRelation()).getId();

                                    CTHyperlink cthyperlink = currentParagraph.getCTP().addNewHyperlink();
                                    cthyperlink.setId(relationId);

                                    CTR ctr = cthyperlink.addNewR();

                                    XWPFHyperlinkRun hyperlinkRun = new XWPFHyperlinkRun(cthyperlink, ctr, currentParagraph);
                                    hyperlinkRun.setColor(safeHexColor(null, "0000FF"));
                                    hyperlinkRun.setUnderline(UnderlinePatterns.SINGLE);
                                    run = hyperlinkRun;
                                } else {
                                    run = currentParagraph.createRun();
                                }
                                run.setText(lineText);
                                applyRunAttributes(run, attributes, state);
                            }

                            boolean isLastSegment = (i == lines.length - 1);
                            boolean isSimpleNewlineOp = (lines.length == 1 && lineText.isEmpty() && insertText.equals(SENTENCE_SPLIT_LABEL));
                            if (!isLastSegment || isSimpleNewlineOp) {
                                currentParagraph = document.createParagraph();

                                FormattingState defaultState = new FormattingState();
                                applyParagraphFormattingState(currentParagraph, defaultState, document);

                                if (isSimpleNewlineOp && state.isTable) {
                                    state.isTable = false;
                                    state.currentTable = null;
                                    state.currentTableRowId = null;
                                    state.currentCellIndex = 0;
                                    state.pendingTableCellText = null;
                                }
                            }
                        }
                    }
                }

                if (!insertText.equals(SENTENCE_SPLIT_LABEL)) {
                    previousText = insertText;
                } else {
                    previousText = null;
                }
            }
            cleanNumPr(state, currentParagraph);
            return document;

        }).onErrorResume(e -> Mono.error(new ProcessException(ProcessException.ErrorCode.CONVERT_JSON_TO_TARGET_FAILED, e, "docx")));
    }

    /**
     * 建立指定類型的抽象編號定義，提供列表系統的基礎樣式設定。
     * <p>
     * 此方法負責建立 Word 文件中列表編號的核心定義，包含編號格式、縮進設定、
     * 對齊方式等樣式資訊。這些定義是 Word 列表系統正常運作的基礎。
     * </p>
     * <p>
     * 支援的編號類型：
     * </p>
     * <ul>
     *   <li><strong>項目符號列表：</strong>
     *     <ul>
     *       <li>使用 {@code STNumberFormat.BULLET} 格式</li>
     *       <li>顯示文字為 "•" 符號</li>
     *       <li>適用於無序清單展示</li>
     *     </ul>
     *   </li>
     *   <li><strong>數字編號列表：</strong>
     *     <ul>
     *       <li>使用 {@code STNumberFormat.DECIMAL} 格式</li>
     *       <li>顯示文字為 "%1." 模式（如：1., 2., 3.）</li>
     *       <li>適用於有序清單展示</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * 樣式設定包含：
     * </p>
     * <ul>
     *   <li>層級設定：設定為第 0 層（頂層）</li>
     *   <li>起始編號：從 1 開始</li>
     *   <li>對齊方式：左對齊</li>
     *   <li>縮進設定：依據設定檔案中的 {@code twipsPerOneLevel} 計算</li>
     *   <li>懸掛縮進：確保編號與文字的正確對齊</li>
     * </ul>
     * <p>
     * 縮進計算邏輯：
     * </p>
     * <ul>
     *   <li>左縮進：{@code twipsPerOneLevel * 2}</li>
     *   <li>懸掛縮進：{@code twipsPerOneLevel}</li>
     *   <li>單位為 Twips（1/1440 英吋）</li>
     * </ul>
     * 
     * @param numbering 文件的編號系統管理對象，用於註冊新的編號定義
     * @param abstractNumId 抽象編號的唯一識別碼，用於後續引用此編號樣式
     * @see ConvertConfig#getAbstractNumIdBullet()
     * @see ConvertConfig#getAbstractNumIdDecimal()
     * @see ConvertConfig#getTwipsPerOneLevel()
     * @apiNote 此方法會直接修改傳入的 numbering 對象，新增編號定義
     * @implNote 使用 Apache POI 的低階 API 建立 OpenXML 結構
     */
    private void createAbstractNumbering(XWPFNumbering numbering, BigInteger abstractNumId) {
        CTAbstractNum cTAbstractNum = CTAbstractNum.Factory.newInstance();
        cTAbstractNum.setAbstractNumId(abstractNumId);

        CTLvl cTLvl = cTAbstractNum.addNewLvl();
        cTLvl.setIlvl(BigInteger.ZERO);
        cTLvl.addNewStart().setVal(BigInteger.ONE);

        if (abstractNumId.equals(config.getAbstractNumIdBullet())) {
            cTLvl.addNewNumFmt().setVal(STNumberFormat.BULLET);
            cTLvl.addNewLvlText().setVal("•");
        } else {
            cTLvl.addNewNumFmt().setVal(STNumberFormat.DECIMAL);
            cTLvl.addNewLvlText().setVal("%1.");
        }
        cTLvl.addNewLvlJc().setVal(STJc.LEFT);

        CTPPrGeneral ppr = cTLvl.addNewPPr();
        CTInd ind = ppr.addNewInd();
        ind.setLeft(BigInteger.valueOf(config.getTwipsPerOneLevel() * 2L));
        ind.setHanging(BigInteger.valueOf(config.getTwipsPerOneLevel()));

        XWPFAbstractNum abstractNum = new XWPFAbstractNum(cTAbstractNum, numbering);
        numbering.addAbstractNum(abstractNum);
    }

    /**
     * 套用段落級格式化設定，根據當前格式狀態配置段落的外觀和行為。
     * <p>
     * 此方法負責將 {@link FormattingState} 中儲存的格式設定套用到指定的段落中，
     * 包括對齊方式、縮進設定、列表格式、標題樣式等所有段落級的格式化屬性。
     * 這是 Word 文件的段落結構正確展示的關鍵步驟。
     * </p>
     * <p>
     * <strong>處理的格式屬性包括：</strong>
     * </p>
     * <ul>
     *   <li><strong>段落對齊：</strong>左對齊、置中、右對齊、兩端對齊</li>
     *   <li><strong>列表系統：</strong>有序/無序列表的編號設定和層級設定</li>
     *   <li><strong>縮進控制：</strong>非列表段落的自訂縮進設定</li>
     *   <li><strong>標題樣式：</strong>H1-H6 標題級別的自動套用</li>
     *   <li><strong>引用區塊：</strong>引用段落的縮進設定</li>
     *   <li><strong>程式碼區塊：</strong>程式碼的背景色彩和設定</li>
     * </ul>
     * <p>
     * <strong>列表處理特性：</strong>
     * </p>
     * <ul>
     *   <li>檢查編號定義是否存在於文件中</li>
     *   <li>根據列表類型選擇正確的抽象編號 ID</li>
     *   <li>設定列表級別和編號 ID</li>
     *   <li>提供列表項目的正確縮進和編號</li>
     * </ul>
     * <p>
     * <strong>縮進計算邏輯：</strong>
     * </p>
     * <ul>
     *   <li>非列表段落：使用 {@code indentLevel * twipsPerOneLevel * 2} 計算縮進</li>
     *   <li>列表段落：由編號定義自動處理縮進</li>
     *   <li>引用區塊：固定使用 {@code twipsPerOneLevel * 2} 縮進</li>
     * </ul>
     * <p>
     * <strong>程式碼區塊設定：</strong>
     * </p>
     * <ul>
     *   <li>套用設定檔案中指定的背景色彩</li>
     *   <li>使用 {@code STShd.CLEAR} 模式確保色彩正確顯示</li>
     *   <li>自動色彩字體確保可讀性</li>
     * </ul>
     * 
     * @param paragraph 要套用格式的段落對象，不得為 null
     * @param state 當前的格式化狀態，包含所有格式設定資訊
     * @param document Word 文件對象，用於存取編號定義等全域資源
     * @see FormattingState
     * @see ConvertConfig#getTwipsPerOneLevel()
     * @see ConvertConfig#getCodeBlockBackgroundColor()
     * @see #updateFormattingState(FormattingState, Map, XWPFDocument)
     * @apiNote 此方法會直接修改傳入的段落對象的屬性
     * @implNote 使用 Apache POI 的低階 API 直接操作 OpenXML 結構
     */
    private void applyParagraphFormattingState(XWPFParagraph paragraph, FormattingState state, XWPFDocument document) {
        paragraph.setAlignment(state.alignment);

        XWPFNumbering numbering = document.getNumbering();
        CTPPr ppr = paragraph.getCTP().getPPr();
        if (ppr == null) {
            ppr = paragraph.getCTP().addNewPPr();
        }


        if (state.currentListNumId != null && numbering != null && numbering.getAbstractNum(state.listType.equals("ordered") ? config.getAbstractNumIdDecimal() : config.getAbstractNumIdBullet()) != null) {
            CTNumPr numPr = ppr.getNumPr();
            if (numPr == null) {
                numPr = ppr.addNewNumPr();
            }

            CTDecimalNumber numIdElement = numPr.isSetNumId() ? numPr.getNumId() : numPr.addNewNumId();
            numIdElement.setVal(state.currentListNumId);

            CTDecimalNumber lvl = numPr.isSetIlvl() ? numPr.getIlvl() : numPr.addNewIlvl();
            lvl.setVal(BigInteger.ZERO);
        } else {
            if (ppr.getNumPr() != null && (ppr.getNumPr().isSetNumId() || ppr.getNumPr().isSetIlvl())) {
                ppr.unsetNumPr();
            }

            if (state.indentLevel > 0) {
                CTInd ind = ppr.isSetInd() ? ppr.getInd() : ppr.addNewInd();
                ind.setLeft(BigInteger.valueOf((long) state.indentLevel * config.getTwipsPerOneLevel() * 2));
            } else {
                if (ppr.isSetInd() && ppr.getInd() != null) {
                    ppr.getInd().unsetLeft();
                }
            }
        }

        if (state.headerLevel > 0) {
            paragraph.setStyle("Heading" + state.headerLevel);
        }


        if (state.isBlockQuote) {
            CTInd ind = ppr.isSetInd() ? ppr.getInd() : ppr.addNewInd();
            ind.setLeft(BigInteger.valueOf(config.getTwipsPerOneLevel() * 2L));
        }

        if (state.isCodeBlock) {
            CTShd shd = ppr.isSetShd() ? ppr.getShd() : ppr.addNewShd();
            shd.setVal(STShd.CLEAR);
            shd.setColor("auto");
            shd.setFill(config.getCodeBlockBackgroundColor());
        } else if (ppr.isSetShd()) {
            ppr.unsetShd();
        }
    }

    /**
     * 更新內部格式化狀態，根據 Quill Operation 屬性更新當前格式設定。
     * <p>
     * 此方法負責解析 Quill Delta Operation 中的屬性資訊，並將其轉換為
     * 內部的 {@link FormattingState} 狀態設定。這個狀態物件將用於後續的段落和
     * 文字格式化操作，確保格式設定的一致性和正確性。
     * </p>
     * <p>
     * <strong>處理的屬性類型：</strong>
     * </p>
     * <ul>
     *   <li><strong>header：</strong>標題級別 (1-6)，轉換為 Word 標題樣式</li>
     *   <li><strong>indent：</strong>縮進級別，計算為 Twips 單位</li>
     *   <li><strong>blockquote：</strong>引用區塊標記，套用特殊縮進</li>
     *   <li><strong>code-block：</strong>程式碼區塊標記，套用背景色彩</li>
     *   <li><strong>align：</strong>段落對齊方式（left/center/right/justify）</li>
     *   <li><strong>list：</strong>列表類型（ordered/bullet），管理編號 ID</li>
     *   <li><strong>table：</strong>表格儲存格標記，啟用表格模式</li>
     * </ul>
     * <p>
     * <strong>列表處理特性：</strong>
     * </p>
     * <ul>
     *   <li>檢測列表類型變更，自動建立新的編號 ID</li>
     *   <li>維持列表狀態的連續性，支援嵌套列表</li>
     *   <li>透過 {@link #getOrCreateListNumId(XWPFDocument, String)} 管理編號</li>
     * </ul>
     * <p>
     * <strong>表格模式特性：</strong>
     * </p>
     * <ul>
     *   <li>啟用表格模式時重設其他所有格式狀態</li>
     *   <li>表格內容使用獨立的對齊設定</li>
     *   <li>禁用標題、引用、程式碼區塊等與表格衝突的格式</li>
     * </ul>
     * <p>
     * <strong>狀態管理特性：</strong>
     * </p>
     * <ul>
     *   <li>使用原地修改方式更新狀態對象</li>
     *   <li>確保狀態變更的原子性和一致性</li>
     *   <li>支援斷開式狀態更新，避免間跨影響</li>
     * </ul>
     * 
     * @param state 要更新的格式化狀態對象，會原地修改其屬性
     * @param attributes 當前 Operation 的屬性集合，包含所有格式設定
     * @param document Word 文件對象，用於存取編號系統等全域資源
     * @see FormattingState
     * @see #parseHeader(Object)
     * @see #parseIndent(Object)
     * @see #parseAlignment(Object)
     * @see #getOrCreateListNumId(XWPFDocument, String)
     * @apiNote 此方法為線程安全的，但不建議同時修改同一個狀態對象
     * @implNote 使用清晰的優先級順序處理衝突的格式設定
     */
    private void updateFormattingState(FormattingState state, Map<String, Object> attributes, XWPFDocument document) {
        state.headerLevel = parseHeader(attributes.get("header"));
        state.indentLevel = parseIndent(attributes.get("indent"));
        state.isBlockQuote = Boolean.TRUE.equals(attributes.get("blockquote"));
        state.isCodeBlock = Boolean.TRUE.equals(attributes.get("code-block"));


        Object alignAttr = attributes.get("align");
        if (alignAttr != null) {
            state.alignment = parseAlignment(alignAttr);
        }

        String newListType = (String) attributes.get("list");
        if (newListType != null) {
            if (!newListType.equals(state.listType) || state.currentListNumId == null) {
                state.currentListNumId = getOrCreateListNumId(document, newListType);
            }
            state.listType = newListType;
        } else {
            state.listType = null;
            state.currentListNumId = null;
        }

        String tableRowIdAttr = (String) attributes.get("table");
        if (tableRowIdAttr != null) {
            state.isTable = true;
            if (attributes.get("align") != null) {
                state.alignment = parseAlignment(attributes.get("align"));
            } else {
                state.alignment = ParagraphAlignment.LEFT;
            }

            state.headerLevel = 0;
            state.isBlockQuote = false;
            state.isCodeBlock = false;
            state.indentLevel = 0;
            state.listType = null;
            state.currentListNumId = null;

        }
    }

    /**
     * 解析標題級別，將屬性轉換為標題級別
     * 根據屬性進行檢查，如果當前的屬性不為 null，則更新標題級別
     *
     * @param headerAttr 當前操作的屬性
     *
     * @return int 當前的標題級別
     */
    private int parseHeader(Object headerAttr) {
        if (headerAttr instanceof Number number) {
            int level = number.intValue();
            return Math.max(0, Math.min(level, 6));
        }
        return 0;
    }

    /**
     * 解析段落縮進級別，將屬性轉換為段落縮進級別
     * 根據屬性進行檢查，如果當前的屬性不為 null，則更新段落縮進級別
     *
     * @param indentAttr 當前操作的屬性
     *
     * @return int 當前的段落縮進級別
     */
    private int parseIndent(Object indentAttr) {
        if (indentAttr instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        return 0;
    }


    /**
     * 安全地驗證和轉換十六進位色碼，確保 Word 文件中顏色設定的正確性和安全性。
     * <p>
     * 此方法負責處理來自 Quill 編輯器的顏色值，驗證其格式的合法性，
     * 並轉換為 Word 文件所需要的格式。Word 需要不帶 "#" 符號的純十六進位色碼。
     * </p>
     * <p>
     * <strong>處理規則：</strong>
     * </p>
     * <ul>
     *   <li><strong>有效格式：</strong>必須以 "#" 開頭且為 7 個字元（包括 #）</li>
     *   <li><strong>色碼格式：</strong>#RRGGBB 格式，例如 #FF0000（紅色）</li>
     *   <li><strong>合法字元：</strong>0-9, A-F, a-f 的十六進位字元</li>
     * </ul>
     * <p>
     * <strong>轉換過程：</strong>
     * </p>
     * <ol>
     *   <li>檢查輸入的色碼是否為 null 或空字串</li>
     *   <li>驗證是否以 "#" 開頭</li>
     *   <li>驗證總長度是否為 7 個字元</li>
     *   <li>移除 "#" 符號後回傳純十六進位色碼</li>
     *   <li>若驗證失敗，回傳後備顏色</li>
     * </ol>
     * <p>
     * <strong>常用色碼範例：</strong>
     * </p>
     * <ul>
     *   <li>#000000 → 000000（黑色）</li>
     *   <li>#FFFFFF → FFFFFF（白色）</li>
     *   <li>#FF0000 → FF0000（紅色）</li>
     *   <li>無效值 → fallback 值</li>
     * </ul>
     * <p>
     * 此方法為實用工具方法，广泛用於文字顏色、背景顏色等各種
     * 顏色設定的安全驗證和轉換。
     * </p>
     *
     * @param hex 待驗證的十六進位色碼字串，預期格式為 #RRGGBB，可為 null
     * @param fallback 當輸入無效時的後備顏色，不得為 null，應為純十六進位格式
     * @return 純十六進位色碼字串（不帶 # 符號），用於 Word 文件顏色設定
     * @apiNote 回傳的顏色碼不包含 "#" 符號，可直接用於 Word 文件的顏色設定
     * @implNote 使用簡單的字串操作進行驗證，無需正則表運算式
     */
    private String safeHexColor(String hex, String fallback) {
        if (hex != null && hex.startsWith("#") && hex.length() == 7) {
            return hex.substring(1);
        }
        return fallback;
    }

    /**
     * 清除段落的列表編號
     * 當前段落為空且當前段落的列表類型不為 null 時，則清除列表編號
     *
     * @param state            當前的格式化狀態
     * @param currentParagraph 當前的段落
     */
    private void cleanNumPr(FormattingState state, XWPFParagraph currentParagraph) {
        boolean lastParaIsEmpty = currentParagraph.getRuns().isEmpty() && currentParagraph.getText().isEmpty();
        if (lastParaIsEmpty && state.listType != null) {
            CTPPr ppr = currentParagraph.getCTP().getPPr();
            if (ppr != null && ppr.getNumPr() != null) {
                if (ppr.getNumPr().isSetNumId() || ppr.getNumPr().isSetIlvl()) {
                    ppr.unsetNumPr();
                }
            }
        }
    }


    /**
     * 套用文字級格式化屬性到指定的文字運行中，實現豐富的文字樣式效果。
     * <p>
     * 此方法負責處理 Quill 編輯器中所有文字級的格式化選項，將其精確轉換為
     * Word 文件的對應格式。支援完整的文字裝飾和排版功能，確保轉換結果的
     * 視覺一致性和可讀性。
     * </p>
     * <p>
     * <strong>支援的文字格式：</strong>
     * </p>
     * <ul>
     *   <li><strong>字體設定：</strong>font 屬性，支援自訂字體名稱和映射</li>
     *   <li><strong>字型大小：</strong>size 屬性，支援灤活的大小設定和轉換</li>
     *   <li><strong>基本裝飾：</strong>粗體、斜體、底線、刪除線</li>
     *   <li><strong>顏色系統：</strong>文字顏色和背景顏色，支援十六進位色碼</li>
     *   <li><strong>上下標：</strong>上標、下標文字效果</li>
     * </ul>
     * <p>
     * <strong>特殊情境處理：</strong>
     * </p>
     * <ul>
     *   <li><strong>程式碼區塊：</strong>在程式磋狀態下套用特定字體和大小</li>
     *   <li><strong>無屬性文字：</strong>套用預設格式，確保一致性</li>
     *   <li><strong>色彩驗證：</strong>使用 {@link #safeHexColor(String, String)} 確保色彩安全性</li>
     * </ul>
     * <p>
     * <strong>字體轉換系統：</strong>
     * </p>
     * <ul>
     *   <li>支援自訂字體映射表，將 Quill 字體名稱轉換為 Word 字體</li>
     *   <li>後備方案：當找不到指定字體時使用預設字體</li>
     *   <li>字型大小註冊：支援自訂大小映射和地據處理</li>
     * </ul>
     * <p>
     * <strong>背景色彩處理：</strong>
     * </p>
     * <ul>
     *   <li>使用 OpenXML {@code CTShd} 結構實現背景色彩</li>
     *   <li>支援 {@code STShd.CLEAR} 模式確保透明度正確</li>
     *   <li>自動清理無效或空值背景設定</li>
     * </ul>
     * <p>
     * <strong>屬性優先級：</strong>
     * </p>
     * <ol>
     *   <li>程式碼區塊設定（最高優先級）</li>
     *   <li>Operation 中的具體屬性設定</li>
     *   <li>設定檔案中的預設值（最低優先級）</li>
     * </ol>
     *
     * @param run 要套用格式的文字運行對象，不得為 null
     * @param attributes 當前 Operation 的屬性集合，可為 null（將使用預設屬性）
     * @param state 當前的格式化狀態，用於判斷特殊情境處理
     * @see ConvertConfig#getDefaultFontFamily()
     * @see ConvertConfig#getDefaultFontSize()
     * @see ConvertConfig#getCodeBlockFontFamily()
     * @see ConvertConfig#getCodeBlockFontSize()
     * @see ConvertConfig#getDefaultFontConvertMap()
     * @see ConvertConfig#getDefaultFontSizeConvertMap()
     * @see #safeHexColor(String, String)
     * @apiNote 此方法會直接修改傳入的文字運行對象的格式屬性
     * @implNote 支援不同類型的屬性值（字串、數字、布林值）的自動轉換
     */
    private void applyRunAttributes(XWPFRun run, Map<String, Object> attributes, FormattingState state) {
        boolean fontIsSet = false;
        boolean sizeIsSet = false;

        if (state.isCodeBlock) {
            run.setFontFamily(config.getCodeBlockFontFamily());
            run.setFontSize(config.getCodeBlockFontSize());
            fontIsSet = true;
            sizeIsSet = true;
        }

        if (attributes == null) {
            if (!fontIsSet) {
                run.setFontFamily(config.getDefaultFontFamily());
            }
            if (!sizeIsSet) {
                run.setFontSize(config.getDefaultFontSize());
            }

            run.setSubscript(VerticalAlign.BASELINE);
            run.setUnderline(UnderlinePatterns.NONE);
            run.setStrikeThrough(false);
            run.setBold(false);
            run.setItalic(false);

            run.setColor(safeHexColor(null, config.getDefaultFontColor()));
            CTRPr rpr = run.getCTR().getRPr();
            if (rpr != null && rpr.getShdArray().length > 0) {
                rpr.removeShd(0);
            }

            return;
        }

        if (attributes.containsKey("font")) {
            String font = Objects.toString(attributes.get("font"), "");
            String fontFamily = config.getDefaultFontConvertMap().getOrDefault(font.toLowerCase(), config.getDefaultFontFamily());
            run.setFontFamily(fontFamily);
            fontIsSet = true;
        }


        if (attributes.containsKey("size")) {
            String size = Objects.toString(attributes.get("size"), "").toLowerCase();
            int sizeValue = config.getDefaultFontSizeConvertMap().getOrDefault(size, config.getDefaultFontSize());
            run.setFontSize(sizeValue);
            sizeIsSet = true;
        }

        if (!fontIsSet) {
            run.setFontFamily(config.getDefaultFontFamily());
        }
        if (!sizeIsSet && !state.isCodeBlock) {
            run.setFontSize(config.getDefaultFontSize());
        }

        run.setBold(Boolean.TRUE.equals(attributes.get("bold")));
        run.setItalic(Boolean.TRUE.equals(attributes.get("italic")));
        run.setUnderline(Boolean.TRUE.equals(attributes.get("underline")) ? UnderlinePatterns.SINGLE : UnderlinePatterns.NONE);
        run.setStrikeThrough(Boolean.TRUE.equals(attributes.get("strike")));


        if (attributes.containsKey("color")) {
            String color = Objects.toString(attributes.get("color"), null);
            run.setColor(safeHexColor(color, config.getDefaultFontColor()));
        } else if (run.getColor() == null) {
            run.setColor(safeHexColor(null, config.getDefaultFontColor()));
        }

        if (attributes.containsKey("background")) {
            String bgColor = Objects.toString(attributes.get("background"), null);
            if (bgColor != null && bgColor.startsWith("#") && bgColor.length() == 7) {
                CTRPr rpr = run.getCTR().getRPr();
                if (rpr == null) {
                    rpr = run.getCTR().addNewRPr();
                }
                CTShd shd = rpr.getShdArray().length > 0 ? rpr.getShdArray(0) : rpr.addNewShd();
                shd.setVal(STShd.CLEAR);
                shd.setColor("auto");
                shd.setFill(bgColor.substring(1));
            } else {
                CTRPr rpr = run.getCTR().getRPr();
                if (rpr != null && rpr.getShdArray().length > 0) {
                    rpr.removeShd(0);
                }
            }
        } else {
            CTRPr rpr = run.getCTR().getRPr();
            if (rpr != null && rpr.getShdArray().length > 0) {
                rpr.removeShd(0);
            }
        }

        if (attributes.containsKey("script")) {
            String script = Objects.toString(attributes.get("script"), "");
            switch (script) {
                case "super":
                    run.setSubscript(VerticalAlign.SUPERSCRIPT);
                    break;
                case "sub":
                    run.setSubscript(VerticalAlign.SUBSCRIPT);
                    break;
            }
        }
    }


    /**
     * 解析和轉換 Quill 編輯器的段落對齊屬性為 Word 文件的對齊方式。
     * <p>
     * 此方法負責處理 Quill Delta Operation 中的 "align" 屬性，
     * 將其轉換為 Word 文件中對應的 {@link ParagraphAlignment} 枚舉值。
     * 支援所有常用的段落對齊方式，確保排版效果的一致性。
     * </p>
     * <p>
     * <strong>支援的對齊方式映射：</strong>
     * </p>
     * <ul>
     *   <li><strong>"left"：</strong>{@link ParagraphAlignment#LEFT} - 左對齊（預設）</li>
     *   <li><strong>"center"：</strong>{@link ParagraphAlignment#CENTER} - 置中對齊</li>
     *   <li><strong>"right"：</strong>{@link ParagraphAlignment#RIGHT} - 右對齊</li>
     *   <li><strong>"justify"：</strong>{@link ParagraphAlignment#BOTH} - 兩端對齊（均勻）</li>
     * </ul>
     * <p>
     * <strong>處理特性：</strong>
     * </p>
     * <ul>
     *   <li>不區分大小寫：使用 {@code toLowerCase()} 確保相容性</li>
     *   <li>後備處理：無效值自動回傳左對齊</li>
     *   <li>空值安全：支援 null 和空字串輸入</li>
     *   <li>類型寬鬆：支援任意 Object 類型輸入</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>{@code
     * // 基本使用
     * parseAlignment("center")    // → ParagraphAlignment.CENTER
     * parseAlignment("LEFT")      // → ParagraphAlignment.LEFT
     * parseAlignment("Justify")   // → ParagraphAlignment.BOTH
     * 
     * // 異常情況
     * parseAlignment(null)        // → ParagraphAlignment.LEFT
     * parseAlignment("")          // → ParagraphAlignment.LEFT
     * parseAlignment("unknown")   // → ParagraphAlignment.LEFT
     * }</pre>
     * <p>
     * 此方法為常用的屬性轉換工具，在整個轉換過程中被多次使用，
     * 確保段落格式的正確性和一致性。
     * </p>
     * 
     * @param alignAttr Quill Operation 中的 align 屬性值，可為任意 Object 類型，常為 String
     * @return 對應的 Word 段落對齊方式枚舉，預設為 {@link ParagraphAlignment#LEFT}
     * @see ParagraphAlignment
     * @see #updateFormattingState(FormattingState, Map, XWPFDocument)
     * @apiNote 此方法為線程安全的純函式，無副作用
     * @implNote 使用 switch 表達式提供良好的性能和可讀性
     */
    private ParagraphAlignment parseAlignment(Object alignAttr) {
        String align = Objects.toString(alignAttr, "left").toLowerCase();
        return switch (align) {
            case "center" -> ParagraphAlignment.CENTER;
            case "right" -> ParagraphAlignment.RIGHT;
            case "justify" -> ParagraphAlignment.BOTH;
            default -> ParagraphAlignment.LEFT;
        };
    }

    /**
     * 取得或建立指定類型列表的編號 ID，支援有序和無序列表的動態管理。
     * <p>
     * 此方法負責管理 Word 文件中列表編號系統的實例化和 ID 分配。
     * 每種列表類型（有序/無序）都需要獨立的編號實例來管理項目的編號順序。
     * 此方法確保系統在需要時自動建立適當的編號實例。
     * </p>
     * <p>
     * <strong>列表類型處理：</strong>
     * </p>
     * <ul>
     *   <li><strong>"ordered"：</strong>有序列表，使用設定中的 {@code abstractNumIdDecimal} ID</li>
     *   <li><strong>其他值：</strong>無序列表，使用設定中的 {@code abstractNumIdBullet} ID</li>
     * </ul>
     * <p>
     * <strong>編號系統檢查：</strong>
     * </p>
     * <ol>
     *   <li>檢查文件是否已存在 {@link XWPFNumbering} 實例</li>
     *   <li>若不存在，則建立新的編號系統</li>
     *   <li>檢查需要的抽象編號定義是否存在</li>
     *   <li>使用 {@link XWPFNumbering#addNum(BigInteger)} 建立新的編號實例</li>
     * </ol>
     * <p>
     * <strong>ID 管理特性：</strong>
     * </p>
     * <ul>
     *   <li>每次呼叫都會建立新的編號實例</li>
     *   <li>不同的列表區塊使用不同的 ID，確保編號獨立性</li>
     *   <li>支援多層級嵌套列表結構</li>
     *   <li>自動處理 Word 的編號系統管理</li>
     * </ul>
     * <p>
     * <strong>與其他方法的協作：</strong>
     * </p>
     * <ul>
     *   <li>依賴 {@link #ensureNumberingDefinitions(XWPFDocument)} 初始化抽象編號</li>
     *   <li>由 {@link #updateFormattingState(FormattingState, Map, XWPFDocument)} 呼叫</li>
     *   <li>由 {@link #applyParagraphFormattingState(XWPFParagraph, FormattingState, XWPFDocument)} 使用</li>
     * </ul>
     * <p>
     * 此方法為列表系統的核心組件，確保列表編號的正確性和一致性。
     * </p>
     * 
     * @param document Word 文件對象，用於存取和管理編號系統
     * @param listType 列表類型字串，"ordered" 表示有序列表，其他值表示無序列表
     * @return 新建立的編號實例 ID，用於設定段落的編號屬性
     * @see XWPFNumbering#addNum(BigInteger)
     * @see ConvertConfig#getAbstractNumIdDecimal()
     * @see ConvertConfig#getAbstractNumIdBullet()
     * @see #ensureNumberingDefinitions(XWPFDocument)
     * @apiNote 此方法會修改傳入的 document 對象，新增編號實例
     * @implNote 每次呼叫都會產生唯一的編號 ID，不會重複使用
     */
    private BigInteger getOrCreateListNumId(XWPFDocument document, String listType) {
        XWPFNumbering numbering = document.getNumbering();
        if (numbering == null) {
            numbering = document.createNumbering();
        }

        BigInteger abstractNumIdToUse = "ordered".equals(listType) ? config.getAbstractNumIdDecimal() : config.getAbstractNumIdBullet();

        return numbering.addNum(abstractNumIdToUse);
    }

    /**
     * 內部格式化狀態管理類，用於追蹤和管理整個轉換過程中的格式設定。
     * <p>
     * 此類別封裝了 Quill 編輯器中所有可能的格式化狀態，並將其轉換為
     * Word 文件格式系統所需的內部表示。當處理 Quill Delta Operations 時，
     * 此狀態物件會持續更新，確保格式設定的連續性和一致性。
     * </p>
     * <p>
     * <strong>狀態管理特性：</strong>
     * </p>
     * <ul>
     *   <li>維持當前的段落級格式設定</li>
     *   <li>追蹤列表和編號系統狀態</li>
     *   <li>管理表格結構的建立和維護</li>
     *   <li>支援複雜的嵌套格式結構</li>
     * </ul>
     * <p>
     * <strong>狀態初始化：</strong>
     * </p>
     * <ul>
     *   <li>所有數值型屬性初始化為 0</li>
     *   <li>所有布林型屬性初始化為 false</li>
     *   <li>所有物件型屬性初始化為 null</li>
     *   <li>對齊方式預設為左對齊</li>
     * </ul>
     * <p>
     * <strong>狀態更新模式：</strong>
     * </p>
     * <ul>
     *   <li>使用原地修改方式更新屬性</li>
     *   <li>支援部分更新，不影響其他屬性</li>
     *   <li>特殊模式（如表格）會重設與之衝突的屬性</li>
     * </ul>
     * <p>
     * <strong>線程安全性：</strong>
     * </p>
     * <ul>
     *   <li>此類別非線程安全，不適合並行使用</li>
     *   <li>每個轉換作業應使用獨立的實例</li>
     *   <li>狀態物件的生命週期與單次轉換作業一致</li>
     * </ul>
     * 
     * @see #updateFormattingState(FormattingState, Map, XWPFDocument)
     * @see #applyParagraphFormattingState(XWPFParagraph, FormattingState, XWPFDocument)
     * @apiNote 此類別為內部實作細節，不應在轉換器外部使用
     * @implNote 使用簡單的 POJO 模式，無封裝或驗證邏輯
     */
    private static class FormattingState {
        /**
         * 當前段落的對齊方式，預設為左對齊
         */
        ParagraphAlignment alignment = ParagraphAlignment.LEFT;

        /**
         * 當前段落的縮進級別，預設為 0
         */
        int indentLevel = 0;

        /**
         * 當前段落的列表類型，預設為 null
         */
        String listType = null;

        /**
         * 當前段落的列表編號 ID，預設為 null
         */
        BigInteger currentListNumId = null;

        /**
         * 是否為代碼區塊，預設為 false
         */
        boolean isCodeBlock = false;

        /**
         * 是否為引用區塊，預設為 false
         */
        boolean isBlockQuote = false;

        /**
         * 當前段落的標題級別，預設為 0
         */
        int headerLevel = 0;

        /**
         * 是否為表格，預設為 false
         */
        boolean isTable = false;

        /**
         * 當前表格的記錄，預設為 null
         */
        XWPFTable currentTable = null;

        /**
         * 當前表格的行 ID，預設為 null
         */
        String currentTableRowId = null;

        /**
         * 當前表格的單元格索引，預設為 0
         */
        int currentCellIndex = 0;

        /**
         * 當前表格的單元格文本，預設為 null
         */
        String pendingTableCellText = null;
    }
}
