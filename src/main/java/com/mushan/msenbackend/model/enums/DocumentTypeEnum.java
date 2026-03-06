package com.mushan.msenbackend.model.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 文档类型枚举
 * 定义支持的文档类型及其扩展名
 *
 * @author mushan
 * @date 2026-02-05
 */
public enum DocumentTypeEnum {
    
    /**
     * Word文档
     */
    DOCX("docx"),
    
    /**
     * PDF文档
     */
    PDF("pdf"),
    
    /**
     * Markdown文档
     */
    MD("md"),
    
    /**
     * 纯文本文档
     */
    TXT("txt");
    
    private final String extension;
    
    DocumentTypeEnum(String extension) {
        this.extension = extension;
    }
    
    /**
     * 获取文件扩展名
     * 
     * @return 文件扩展名
     */
    public String getExtension() {
        return extension;
    }
    
    /**
     * 根据扩展名获取文档类型
     * 
     * @param extension 文件扩展名
     * @return 文档类型枚举，如不存在则返回null
     */
    public static DocumentTypeEnum fromExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return null;
        }
        
        Optional<DocumentTypeEnum> optional = Arrays.stream(values())
                .filter(type -> type.extension.equalsIgnoreCase(extension.trim()))
                .findFirst();
        
        return optional.orElse(null);
    }
    
    /**
     * 检查扩展名是否支持
     * 
     * @param extension 文件扩展名
     * @return 是否支持
     */
    public static boolean isSupported(String extension) {
        return fromExtension(extension) != null;
    }
}