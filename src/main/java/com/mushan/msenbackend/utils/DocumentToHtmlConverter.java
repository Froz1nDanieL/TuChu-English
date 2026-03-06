package com.mushan.msenbackend.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档与HTML互转工具
 * 通过HTML中间格式实现完整的格式保持
 *
 * @author mushan
 * @date 2026-02-08
 */
@Slf4j
public class DocumentToHtmlConverter {

    /**
     * 将Word文档转换为HTML格式（保持所有格式信息）
     *
     * @param docxBytes Word文档字节数组
     * @return HTML字符串
     * @throws IOException IO异常
     */
    public static String convertDocxToHtml(byte[] docxBytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<!DOCTYPE html>\n<html>\n<head>\n");
            htmlBuilder.append("<meta charset=\"UTF-8\">\n");
            htmlBuilder.append("<style>\n");
            htmlBuilder.append("body { font-family: 'Microsoft YaHei', sans-serif; }\n");
            htmlBuilder.append(".paragraph { margin: 0; }\n");
            htmlBuilder.append(".table { border-collapse: collapse; }\n");
            htmlBuilder.append(".table td { border: 1px solid #000; padding: 5px; }\n");
            htmlBuilder.append("</style>\n</head>\n<body>\n");
            
            List<IBodyElement> bodyElements = document.getBodyElements();
            int elementIndex = 0;
            
            for (IBodyElement element : bodyElements) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;
                    String htmlParagraph = convertParagraphToHtml(paragraph, elementIndex++);
                    htmlBuilder.append(htmlParagraph);
                } else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;
                    String htmlTable = convertTableToHtml(table, elementIndex++);
                    htmlBuilder.append(htmlTable);
                }
            }
            
            htmlBuilder.append("\n</body>\n</html>");
            
            log.info("文档转换为HTML完成，HTML长度: {} 字符", htmlBuilder.length());
            return htmlBuilder.toString();
        }
    }

    /**
     * 将HTML转换回Word文档
     *
     * @param htmlContent HTML内容
     * @return Word文档字节数组
     * @throws IOException IO异常
     */
    public static byte[] convertHtmlToDocx(String htmlContent) throws IOException {
        Document doc = Jsoup.parse(htmlContent);
        try (XWPFDocument document = new XWPFDocument()) {
            
            // 处理body中的所有元素
            Element body = doc.body();
            if (body != null) {
                for (Node node : body.childNodes()) {
                    if (node instanceof Element) {
                        Element element = (Element) node;
                        if (element.tagName().equals("p")) {
                            createParagraphFromHtml(document, element);
                        } else if (element.tagName().equals("table")) {
                            createTableFromHtml(document, element);
                        }
                    }
                }
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 将段落转换为HTML（保持所有格式）
     */
    private static String convertParagraphToHtml(XWPFParagraph paragraph, int index) {
        StringBuilder html = new StringBuilder();
        html.append(String.format("<p class=\"paragraph\" data-index=\"%d\"", index));
        
        // 添加段落样式
        String styleAttr = getParagraphStyleAttributes(paragraph);
        if (!styleAttr.isEmpty()) {
            html.append(" style=\"").append(styleAttr).append("\"");
        }
        html.append(">");
        
        // 处理每个run
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) {
            // 如果没有run，处理段落文本
            String text = paragraph.getText();
            if (text != null && !text.isEmpty()) {
                html.append(escapeHtml(text));
            }
        } else {
            for (XWPFRun run : runs) {
                html.append(convertRunToHtml(run));
            }
        }
        
        html.append("</p>\n");
        return html.toString();
    }

    /**
     * 将Run转换为HTML span
     */
    private static String convertRunToHtml(XWPFRun run) {
        StringBuilder html = new StringBuilder();
        html.append("<span");
        
        // 添加样式属性
        String styleAttr = getRunStyleAttributes(run);
        if (!styleAttr.isEmpty()) {
            html.append(" style=\"").append(styleAttr).append("\"");
        }
        html.append(">");
        
        String text = run.getText(0);
        if (text != null) {
            html.append(escapeHtml(text));
        }
        
        html.append("</span>");
        return html.toString();
    }

    /**
     * 获取段落样式属性
     */
    private static String getParagraphStyleAttributes(XWPFParagraph paragraph) {
        List<String> styles = new ArrayList<>();
        
        // 对齐方式
        ParagraphAlignment alignment = paragraph.getAlignment();
        if (alignment != null) {
            switch (alignment) {
                case CENTER:
                    styles.add("text-align: center");
                    break;
                case RIGHT:
                    styles.add("text-align: right");
                    break;
                case BOTH:
                    styles.add("text-align: justify");
                    break;
                default:
                    styles.add("text-align: left");
            }
        }
        
        // 缩进（使用int类型）
        int indentLeft = paragraph.getIndentationLeft();
        if (indentLeft > 0) {
            styles.add(String.format("margin-left: %.2fpt", indentLeft / 20.0));
        }
        
        int indentRight = paragraph.getIndentationRight();
        if (indentRight > 0) {
            styles.add(String.format("margin-right: %.2fpt", indentRight / 20.0));
        }
        
        int firstLineIndent = paragraph.getIndentationFirstLine();
        if (firstLineIndent != 0) {
            styles.add(String.format("text-indent: %.2fpt", firstLineIndent / 20.0));
        }
        
        // 行距和段落间距（使用int类型）
        int spacingBefore = paragraph.getSpacingBefore();
        if (spacingBefore > 0) {
            styles.add(String.format("margin-top: %.2fpt", spacingBefore / 20.0));
        }
        
        int spacingAfter = paragraph.getSpacingAfter();
        if (spacingAfter > 0) {
            styles.add(String.format("margin-bottom: %.2fpt", spacingAfter / 20.0));
        }
        
        return String.join("; ", styles);
    }

    /**
     * 获取Run样式属性
     */
    private static String getRunStyleAttributes(XWPFRun run) {
        List<String> styles = new ArrayList<>();
        
        // 字体
        String fontFamily = run.getFontFamily();
        if (fontFamily != null && !fontFamily.isEmpty()) {
            styles.add("font-family: '" + fontFamily + "'");
        }
        
        // 字号
        int fontSize = run.getFontSize();
        if (fontSize > 0) {
            styles.add("font-size: " + fontSize + "pt");
        }
        
        // 粗体
        if (run.isBold()) {
            styles.add("font-weight: bold");
        }
        
        // 斜体
        if (run.isItalic()) {
            styles.add("font-style: italic");
        }
        
        // 下划线
        if (run.getUnderline() != null && run.getUnderline() != UnderlinePatterns.NONE) {
            styles.add("text-decoration: underline");
        }
        
        // 颜色
        String color = run.getColor();
        if (color != null && !color.isEmpty()) {
            styles.add("color: #" + color);
        }
        return String.join("; ", styles);
    }

    /**
     * 将表格转换为HTML
     */
    private static String convertTableToHtml(XWPFTable table, int index) {
        StringBuilder html = new StringBuilder();
        html.append(String.format("<table class=\"table\" data-index=\"%d\">\n", index));
        
        List<XWPFTableRow> rows = table.getRows();
        for (XWPFTableRow row : rows) {
            html.append("<tr>\n");
            List<XWPFTableCell> cells = row.getTableCells();
            for (XWPFTableCell cell : cells) {
                html.append("<td>");
                // 处理单元格内的段落
                List<XWPFParagraph> cellParagraphs = cell.getParagraphs();
                for (XWPFParagraph para : cellParagraphs) {
                    html.append(convertParagraphToHtml(para, -1).replaceAll("^<p[^>]*>|</p>$", ""));
                }
                html.append("</td>\n");
            }
            html.append("</tr>\n");
        }
        
        html.append("</table>\n");
        return html.toString();
    }

    /**
     * 从HTML创建段落
     */
    private static void createParagraphFromHtml(XWPFDocument document, Element pElement) {
        XWPFParagraph paragraph = document.createParagraph();
        
        // 解析段落样式
        String style = pElement.attr("style");
        applyParagraphStyles(paragraph, style);
        
        // 处理子元素
        for (Node node : pElement.childNodes()) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                if (!textNode.text().trim().isEmpty()) {
                    XWPFRun run = paragraph.createRun();
                    run.setText(textNode.text());
                    applyDefaultRunStyles(run);
                }
            } else if (node instanceof Element) {
                Element element = (Element) node;
                if (element.tagName().equals("span")) {
                    XWPFRun run = paragraph.createRun();
                    String text = element.ownText();
                    if (!text.isEmpty()) {
                        run.setText(text);
                    }
                    String runStyle = element.attr("style");
                    applyRunStyles(run, runStyle);
                }
            }
        }
    }

    /**
     * 从HTML创建表格
     */
    private static void createTableFromHtml(XWPFDocument document, Element tableElement) {
        Elements rows = tableElement.select("tr");
        if (rows.isEmpty()) return;
        
        // 创建表格
        XWPFTable table = document.createTable(rows.size(), 1);
        
        for (int i = 0; i < rows.size(); i++) {
            Element rowElement = rows.get(i);
            Elements cells = rowElement.select("td");
            
            // 调整列数
            XWPFTableRow row = table.getRow(i);
            while (row.getTableCells().size() < cells.size()) {
                row.addNewTableCell();
            }
            while (row.getTableCells().size() > cells.size()) {
                // 移除多余的单元格比较复杂，这里简化处理
                break;
            }
            
            // 填充单元格内容
            for (int j = 0; j < cells.size() && j < row.getTableCells().size(); j++) {
                XWPFTableCell cell = row.getCell(j);
                Element cellElement = cells.get(j);
                
                // 清空现有段落
                cell.removeParagraph(0);
                
                // 创建新段落
                XWPFParagraph para = cell.addParagraph();
                String cellText = cellElement.text();
                if (!cellText.isEmpty()) {
                    XWPFRun run = para.createRun();
                    run.setText(cellText);
                }
            }
        }
    }

    /**
     * 应用段落样式
     */
    private static void applyParagraphStyles(XWPFParagraph paragraph, String styleString) {
        if (styleString == null || styleString.isEmpty()) return;
        
        String[] styles = styleString.split(";");
        for (String style : styles) {
            String[] parts = style.split(":");
            if (parts.length == 2) {
                String property = parts[0].trim().toLowerCase();
                String value = parts[1].trim().toLowerCase();
                
                switch (property) {
                    case "text-align":
                        if (value.equals("center")) {
                            paragraph.setAlignment(ParagraphAlignment.CENTER);
                        } else if (value.equals("right")) {
                            paragraph.setAlignment(ParagraphAlignment.RIGHT);
                        } else if (value.equals("justify")) {
                            paragraph.setAlignment(ParagraphAlignment.BOTH);
                        } else {
                            paragraph.setAlignment(ParagraphAlignment.LEFT);
                        }
                        break;
                    case "margin-left":
                        try {
                            double ptValue = Double.parseDouble(value.replaceAll("[^\\d.]", ""));
                            paragraph.setIndentationLeft((int)(ptValue * 20));
                        } catch (NumberFormatException e) {
                            // 忽略无效值
                        }
                        break;
                }
            }
        }
    }

    /**
     * 应用Run样式
     */
    private static void applyRunStyles(XWPFRun run, String styleString) {
        if (styleString == null || styleString.isEmpty()) {
            applyDefaultRunStyles(run);
            return;
        }
        
        String[] styles = styleString.split(";");
        for (String style : styles) {
            String[] parts = style.split(":");
            if (parts.length == 2) {
                String property = parts[0].trim().toLowerCase();
                String value = parts[1].trim().toLowerCase();
                
                switch (property) {
                    case "font-family":
                        run.setFontFamily(value.replace("'", "").replace("\"", ""));
                        break;
                    case "font-size":
                        try {
                            int size = Integer.parseInt(value.replaceAll("[^\\d]", ""));
                            run.setFontSize(size);
                        } catch (NumberFormatException e) {
                            run.setFontSize(12); // 默认字号
                        }
                        break;
                    case "font-weight":
                        run.setBold(value.contains("bold"));
                        break;
                    case "font-style":
                        run.setItalic(value.contains("italic"));
                        break;
                    case "text-decoration":
                        run.setUnderline(value.contains("underline") ? 
                            UnderlinePatterns.SINGLE : UnderlinePatterns.NONE);
                        break;
                    case "color":
                        if (value.startsWith("#")) {
                            run.setColor(value.substring(1));
                        }
                        break;
                }
            }
        }
    }

    /**
     * 应用默认Run样式
     */
    private static void applyDefaultRunStyles(XWPFRun run) {
        run.setFontFamily("宋体");
        run.setFontSize(12);
    }

    /**
     * HTML转义
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }

    /**
     * 提取HTML中的可翻译文本
     *
     * @param htmlContent HTML内容
     * @return 可翻译的文本列表
     */
    public static List<String> extractTranslatableText(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        List<String> texts = new ArrayList<>();
        
        // 提取所有文本节点
        Elements textElements = doc.select("p, span, td");
        for (Element element : textElements) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                texts.add(text);
            }
        }
        
        return texts;
    }

    /**
     * 将翻译后的文本注入HTML
     *
     * @param htmlContent 原始HTML
     * @param translatedTexts 翻译后的文本列表
     * @return 更新后的HTML
     */
    public static String injectTranslatedText(String htmlContent, List<String> translatedTexts) {
        Document doc = Jsoup.parse(htmlContent);
        Elements textElements = doc.select("p, span, td");
        
        int textIndex = 0;
        for (Element element : textElements) {
            if (textIndex < translatedTexts.size()) {
                String originalText = element.text().trim();
                if (!originalText.isEmpty()) {
                    element.text(translatedTexts.get(textIndex++));
                }
            }
        }
        
        return doc.html();
    }
}