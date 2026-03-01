package com.mushan.msenbackend.service;

import com.mushan.msenbackend.model.dto.translate.TranslateResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 百度翻译服务接口
 */
public interface TranslateService {
    /**
     * 文本翻译
     *
     * @param text 待翻译文本
     * @param from 源语言
     * @param to 目标语言
     * @return 翻译结果
     */
    TranslateResponse translate(String text, String from, String to);
    
    /**
     * 翻译为中文
     *
     * @param text 待翻译文本
     * @return 翻译结果
     */
    TranslateResponse translateToZh(String text);
    
    /**
     * 翻译为英文
     *
     * @param text 待翻译文本
     * @return 翻译结果
     */
    TranslateResponse translateToEn(String text);
    
    /**
     * 翻译文档内容
     *
     * @param file 文档文件
     * @param from 源语言
     * @param to 目标语言
     * @return 翻译结果
     */
    TranslateResponse translateDocument(MultipartFile file, String from, String to);
    
    /**
     * 翻译文档内容为中文
     *
     * @param file 文档文件
     * @return 翻译结果
     */
    TranslateResponse translateDocumentToZh(MultipartFile file);
    
    /**
     * 翻译文档内容为英文
     *
     * @param file 文档文件
     * @return 翻译结果
     */
    TranslateResponse translateDocumentToEn(MultipartFile file);
    
    /**
     * 翻译文档并保持格式
     *
     * @param file 文档文件
     * @param from 源语言
     * @param to 目标语言
     * @return 翻译后的文档字节数组
     */
    byte[] translateDocumentWithFormat(MultipartFile file, String from, String to);
    
    /**
     * 翻译文档为中文并保持格式
     *
     * @param file 文档文件
     * @return 翻译后的文档字节数组
     */
    byte[] translateDocumentToZhWithFormat(MultipartFile file);
    
    /**
     * 翻译文档为英文并保持格式
     *
     * @param file 文档文件
     * @return 翻译后的文档字节数组
     */
    byte[] translateDocumentToEnWithFormat(MultipartFile file);
}