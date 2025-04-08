package xyz.dowob.filemanagement.component.provider.providerImplement.contentconvertprovider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.factory.config.ConvertConfig;
import xyz.dowob.filemanagement.component.provider.providerInterface.ContentConvertProvider;
import xyz.dowob.filemanagement.data.file.po.QuillContentPO;
import xyz.dowob.filemanagement.exception.ProcessException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Quill 內容轉換為 Word 的實現類
 * 實現 ContentConvertProvider 接口，內部定義轉換所需要實現的方法
 * 由於 Quill 的內容是 JSON 格式的，且其內部規則較為複雜因此將此類提取出來
 * 此類是利用 {@link xyz.dowob.filemanagement.component.provider.factory.ContentConvertProviderFactory} 來生成的，並會根據所傳入的設定檔進行配置
 *
 * @author yuan
 * @program FileManagement
 * @ClassName WordConvertProvider
 * @create 2025/4/6
 * @Version 1.0
 **/
public class WordConvertProvider implements ContentConvertProvider {
    /**
     * ObjectMapper 用於將 JSON 轉換為 Java 對象
     */
    private final ObjectMapper objectMapper;

    /**
     * 轉換配置
     * 用於配置轉換器的參數設定
     */
    private final ConvertConfig config;

    /**
     * WordConvertProvider 的構造函數
     *
     * @param config 轉換配置
     */
    public WordConvertProvider(ConvertConfig config) {
        this.objectMapper = new ObjectMapper();
        this.config = config;
    }

    private static final String SENTENCE_SPLIT_LABEL = "\n";


    /**
     * FormattingState 類，其包含了當前的格式化狀態
     * 這些狀態是 Quill 編輯器的內部表示，包含了文本的格式化屬性等
     * 在原始狀態下這些屬性就是預設值，在讀取 JSON 時會根據 JSON 的內容進行更新
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
         * 當前表格的紀錄，預設為 null
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

    /**
     * 將 Quill 的 JSON 內容轉換為 Word 文檔並輸出為 byte[]
     *
     * @param content Quill 的 JSON 內容
     *
     * @return Mono<byte [ ]> 包含轉換後的 Word 文檔的 byte[]
     */
    @Override
    public Mono<byte[]> convertToByte(String content) {
        return formatToDocument(content).flatMap(document -> convertToOutputStream(document).flatMap(outputStream -> {
            byte[] bytes = outputStream.toByteArray();
            return Mono.just(bytes);
        }));
    }


    /**
     * 將 Quill 的 JSON 內容轉換為 Word 文檔並輸出為 DataBuffer
     *
     * @param content Quill 的 JSON 內容
     *
     * @return Flux<DataBuffer> 包含轉換後的 Word 文檔的 DataBuffer
     */
    @Override
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
     * 給定一個 XWPFDocument 對象，將其轉換為 ByteArrayOutputStream
     *
     * @param document 要轉換的 XWPFDocument 對象
     *
     * @return Mono<ByteArrayOutputStream> 包含轉換後的 ByteArrayOutputStream
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
     * 將 Quill 的 JSON 內容轉換為 XWPFDocument 對象
     *
     * @param content Quill 的 JSON 內容
     *
     * @return Mono<XWPFDocument> 包含轉換後的 XWPFDocument 對象
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
     * 具體的轉換邏輯，分成幾個部分
     * 1. 解析 JSON 內容，將儲存的操作轉換為 Delta 對象以及 Operation 對象
     * 2. 遍歷操作列表，根據操作的類型進行處理
     * 3. 根據操作的屬性更新格式化狀態
     * 4. 根據操作的內容進行文本插入
     * 5. 根據操作的屬性進行段落格式化
     * 6. 根據操作的屬性進行列表格式化
     * <p>
     * 在 Json 內容中，其屬性應用規範為:
     * 1. 如果當前的 Operation 是有 Attributes 則當前的 insert 就直接應用這 Attributes
     * 2. 如果當前的 Operation 是沒有 Attributes 則需要下一個 Operation 來進行判斷
     * - 如果下一個 Operation 是有 Attributes 且 insert 為空則當前的 insert 就直接應用這 Attributes
     * - 如果下一個 Operation 是沒有 Attributes 則當前的 insert 就需要使用預設的屬性
     * <p>
     * 依照處理的屬性分成幾個部分
     * 1. 一般語句屬性: 通常用於單一句子並且沒有其他屬性
     * 2. 特殊語句屬性: 通常用於單一句子並且有其他屬性
     * 3. 區塊語句屬性: 通常用於多行語句並且有其他屬性
     * 個別的語句屬性會有不同的處理方式
     * <p>
     * 整體的處理方式是:
     * 1. 將所有操作進行遍歷，並依照屬性應用規範進行處理
     * 2. 當前的操作如果沒有屬性則檢查下一個操作是否有屬性
     * 3. 確認最後使用的屬性要使用哪個
     * 4. 根據屬性進行處理
     *
     * @param document    要轉換的 XWPFDocument 對象
     * @param jsonContent Quill 的 JSON 內容
     *
     * @return Mono<XWPFDocument> 包含轉換後的 XWPFDocument 對象
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
     * 更新格式化狀態，當操作中帶有屬性時，則更新格式化狀態
     * 如果操作中沒有屬性，則使用當前的格式化狀態
     * 根據屬性進行檢查，如果當前的屬性不為 null，則更新格式化狀態
     *
     * @param state      當前的格式化狀態
     * @param attributes 當前操作的屬性
     * @param document   當前的 XWPFDocument 對象
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
     * 應用段落格式化狀態，當屬性設定更新完成後，則應用段落格式化狀態
     * 將段落的對齊方式、縮進級別、列表類型、列表編號 ID、標題級別等屬性應用到段落中
     *
     * @param paragraph 當前的段落
     * @param state     當前的格式化狀態
     * @param document  當前的 XWPFDocument 對象
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
     * 應用運行屬性，當屬性設定更新完成後，則應用運行屬性
     * 將運行的字體、大小、顏色、背景色、下標、刪除線、粗體、斜體等屬性應用到運行中
     *
     * @param run        當前的句子
     * @param attributes 當前操作的屬性
     * @param state      當前的格式化狀態
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
     * 解析段落對齊方式，將屬性轉換為段落對齊方式
     * 根據屬性進行檢查，如果當前的屬性不為 null，則更新段落對齊方式
     *
     * @param alignAttr 當前操作的屬性
     *
     * @return ParagraphAlignment 當前的段落對齊方式
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
     * 獲取或創建列表編號 ID，根據列表類型獲取或創建列表編號 ID
     * 根據列表類型進行檢查，如果當前的列表類型不為 null，則獲取或創建列表編號 ID
     *
     * @param document 當前的 XWPFDocument 對象
     * @param listType 當前的列表類型
     *
     * @return BigInteger 當前的列表編號 ID
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
     * 確保編號定義存在，根據列表類型確保編號定義存在
     * 根據列表類型進行檢查，如果當前的列表類型不為 null，則確保編號定義存在
     *
     * @param document 當前的 XWPFDocument 對象
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
     * 創建抽象編號，根據列表類型創建抽象編號
     * 根據列表類型進行檢查，如果當前的列表類型不為 null，則創建抽象編號
     *
     * @param numbering     當前的 XWPFNumbering 對象
     * @param abstractNumId 當前的抽象編號 ID
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
     * 確保十六進制顏色的安全性，將顏色轉換為十六進制顏色
     * 根據顏色進行檢查，如果當前的顏色不為 null，則轉換為十六進制顏色
     *
     * @param hex      當前的顏色
     * @param fallback 預設的顏色
     *
     * @return String 當前的十六進制顏色
     */
    private String safeHexColor(String hex, String fallback) {
        if (hex != null && hex.startsWith("#") && hex.length() == 7) {
            return hex.substring(1);
        }
        return fallback;
    }
}
