package com.mushan.msenbackend.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import com.mushan.msenbackend.model.enums.DocumentTypeEnum;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

/**
 * 文档解析工具类
 * 支持Word(.docx)和PDF文档的内容提取
 * 作为工具类提供静态方法，便于多个模块复用
 *
 * @author mushan
 * @date 2026-02-05
 */
@Slf4j
public class DocumentParserUtil {

    /**
     * 解析文档内容
     *
     * @param file MultipartFile文件对象
     * @return 解析后的文本内容
     * @throws IOException IO异常
     * @throws IllegalArgumentException 文件类型不支持
     */
    public static String parseDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (StringUtils.isBlank(filename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(filename);
        DocumentTypeEnum fileType = DocumentTypeEnum.fromExtension(extension);
        
        if (fileType == null) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }

        log.info("开始解析文档: {}, 类型: {}", filename, fileType.getExtension());
        
        try (InputStream inputStream = file.getInputStream()) {
            String content = parseDocument(inputStream, fileType);
            log.info("文档解析完成，提取字符数: {}", content.length());
            return content;
        }
    }

    /**
     * 解析文档内容（字节数组版本）
     *
     * @param fileBytes 文件字节数组
     * @param fileType 文件类型
     * @return 解析后的文本内容
     * @throws IOException IO异常
     */
    public static String parseDocument(byte[] fileBytes, DocumentTypeEnum fileType) throws IOException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空");
        }

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            return parseDocument(inputStream, fileType);
        }
    }

    /**
     * 解析文档内容（InputStream版本）
     *
     * @param inputStream 文件输入流
     * @param fileType 文件类型
     * @return 解析后的文本内容
     * @throws IOException IO异常
     */
    public static String parseDocument(InputStream inputStream, DocumentTypeEnum fileType) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }

        if (fileType == null) {
            throw new IllegalArgumentException("文件类型不能为空");
        }

        switch (fileType) {
            case DOCX:
                return parseWordDocument(inputStream);
            case PDF:
                return parsePdfDocument(inputStream);
            case MD:
                return parseMarkdownDocument(inputStream);
            case TXT:
                return parseTextDocument(inputStream);
            default:
                throw new IllegalArgumentException("不支持的文件类型: " + fileType);
        }
    }

    /**
     * 解析Word文档(.docx)
     *
     * @param inputStream Word文档输入流
     * @return 解析后的文本内容
     * @throws IOException IO异常
     */
    private static String parseWordDocument(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            // 提取段落文本（包括列表项）
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String text = extractParagraphText(paragraph);
                if (StringUtils.isNotBlank(text)) {
                    content.append(text).append("\n");
                }
            }

            // 提取表格内容
            List<XWPFTable> tables = document.getTables();
            for (XWPFTable table : tables) {
                content.append("\n[表格开始]\n");
                List<XWPFTableRow> rows = table.getRows();
                for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                    XWPFTableRow row = rows.get(rowIndex);
                    List<XWPFTableCell> cells = row.getTableCells();
                    for (int cellIndex = 0; cellIndex < cells.size(); cellIndex++) {
                        XWPFTableCell cell = cells.get(cellIndex);
                        String cellText = extractCellText(cell);
                        content.append(cellText);
                        if (cellIndex < cells.size() - 1) {
                            content.append(" | "); // 使用竖线分隔单元格，更清晰
                        }
                    }
                    content.append("\n");
                }
                content.append("[表格结束]\n");
            }
        }

        return content.toString().trim();
    }
    
    /**
     * 提取段落文本，包括列表项和格式化内容
     */
    private static String extractParagraphText(XWPFParagraph paragraph) {
        StringBuilder text = new StringBuilder();
        
        // 获取段落的编号信息
        String numFmt = paragraph.getNumFmt();
        BigInteger numIlvl = paragraph.getNumIlvl();
        BigInteger numId = paragraph.getNumID();
        
        // 如果是列表项，添加编号前缀
        if (numId != null && numIlvl != null) {
            // 添加适当的缩进和编号
            int level = numIlvl.intValue();
            for (int i = 0; i < level; i++) {
                text.append("  "); // 每级缩进两个空格
            }
            text.append(numId.toString()).append(". ");
        }
        
        // 获取段落的所有文本运行
        text.append(paragraph.getText());
        
        return text.toString();
    }
    
    /**
     * 提取表格单元格文本，包括内部段落
     */
    private static String extractCellText(XWPFTableCell cell) {
        StringBuilder cellText = new StringBuilder();
        
        // 获取单元格内的所有段落
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph paragraph = paragraphs.get(i);
            String paraText = paragraph.getText();
            if (StringUtils.isNotBlank(paraText)) {
                cellText.append(paraText);
                if (i < paragraphs.size() - 1) {
                    cellText.append(" "); // 段落间用空格分隔
                }
            }
        }
        
        return cellText.toString().trim();
    }

    /**
     * 解析PDF文档
     *
     * @param inputStream PDF文档输入流
     * @return 解析后的文本内容
     * @throws IOException IO异常
     */
    private static String parsePdfDocument(InputStream inputStream) throws IOException {
        // 将InputStream转换为byte数组
        byte[] pdfBytes = inputStream.readAllBytes();
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // 设置更好的文本提取参数
            stripper.setSortByPosition(true); // 按位置排序，保持阅读顺序
            stripper.setAddMoreFormatting(true); // 添加更多格式化信息
            
            // 可以设置页码范围
            // stripper.setStartPage(1);
            // stripper.setEndPage(document.getNumberOfPages());
            
            String text = stripper.getText(document);
            
            // 清理和格式化文本
            return cleanAndFormatText(text);
        }
    }
    
    /**
     * 解析Markdown文档
     *
     * @param inputStream Markdown文档输入流
     * @return 解析后的文本内容
     * @throws IOException IO异常
     */
    private static String parseMarkdownDocument(InputStream inputStream) throws IOException {
        return parseTextContent(inputStream);
    }
    
    /**
     * 解析纯文本文档
     *
     * @param inputStream 文本文件输入流
     * @return 解析后的文本内容
     * @throws IOException IO异常
     */
    private static String parseTextDocument(InputStream inputStream) throws IOException {
        return parseTextContent(inputStream);
    }
    
    /**
     * 解析文本内容的通用方法
     *
     * @param inputStream 输入流
     * @return 解析后的文本内容
     * @throws IOException IO异常
     */
    private static String parseTextContent(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString().trim();
    }
    
    /**
     * 清理和格式化提取的文本
     */
    private static String cleanAndFormatText(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        
        // 移除多余的空白行
        String cleaned = text.replaceAll("\n\s*\n\s*\n", "\n\n");
        
        // 标准化换行符
        cleaned = cleaned.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        
        // 移除行首行尾的多余空格
        String[] lines = cleaned.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                result.append(trimmedLine).append("\n");
            }
        }
        
        return result.toString().trim();
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 文件扩展名（不包含点号）
     */
    private static String getFileExtension(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        
        return "";
    }

    /**
     * 验证文件类型是否支持
     *
     * @param filename 文件名
     * @return 是否支持
     */
    public static boolean isSupportedFileType(String filename) {
        String extension = getFileExtension(filename);
        return DocumentTypeEnum.isSupported(extension);
    }

    /**
     * 获取支持的文件类型列表
     *
     * @return 支持的文件类型数组
     */
    public static String[] getSupportedFileTypes() {
        DocumentTypeEnum[] types = DocumentTypeEnum.values();
        String[] extensions = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            extensions[i] = types[i].getExtension();
        }
        return extensions;
    }

    /**
     * 获取文件大小格式化显示
     *
     * @param bytes 字节数
     * @return 格式化后的文件大小字符串
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 测试方法 - 可用于验证工具类功能
     */
    public static void main(String[] args) {
        System.out.println("支持的文件类型: " + String.join(", ", getSupportedFileTypes()));
        System.out.println("DOCX是否支持: " + isSupportedFileType("test.docx"));
        System.out.println("PDF是否支持: " + isSupportedFileType("test.pdf"));
        System.out.println("MD是否支持: " + isSupportedFileType("test.md"));
        System.out.println("TXT是否支持: " + isSupportedFileType("test.txt"));
        System.out.println("文件大小格式化(1024): " + formatFileSize(1024));
        System.out.println("文件大小格式化(1048576): " + formatFileSize(1048576));
    }
}