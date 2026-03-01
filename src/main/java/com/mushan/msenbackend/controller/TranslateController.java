package com.mushan.msenbackend.controller;

import com.mushan.msenbackend.common.BaseResponse;
import com.mushan.msenbackend.common.ResultUtils;
import com.mushan.msenbackend.exception.BusinessException;
import com.mushan.msenbackend.exception.ErrorCode;
import com.mushan.msenbackend.model.dto.translate.TranslateResponse;
import com.mushan.msenbackend.service.TranslateService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * 百度翻译控制器
 */
@RestController
@RequestMapping("/translate")
@Slf4j
public class TranslateController {

    @Resource
    private TranslateService translateService;

    @GetMapping("/text")
    public BaseResponse<TranslateResponse> translateText(
            @RequestParam("text") String text,
            @RequestParam(value = "from", defaultValue = "auto") String from,
            @RequestParam(value = "to", defaultValue = "zh") String to) {
        
        TranslateResponse response = translateService.translate(text, from, to);
        return ResultUtils.success(response);
    }

    @GetMapping("/to-zh")
    public BaseResponse<TranslateResponse> translateToChinese(@RequestParam("text") String text) {
        TranslateResponse response = translateService.translateToZh(text);
        return ResultUtils.success(response);
    }

    @GetMapping("/to-en")
    public BaseResponse<TranslateResponse> translateToEnglish(@RequestParam("text") String text) {
        TranslateResponse response = translateService.translateToEn(text);
        return ResultUtils.success(response);
    }
    
    @PostMapping("/document")
    public BaseResponse<TranslateResponse> translateDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "from", defaultValue = "auto") String from,
            @RequestParam(value = "to", defaultValue = "zh") String to) {
        
        log.info("收到文档翻译请求: 文件名={}, 源语言={}, 目标语言={}", 
                file.getOriginalFilename(), from, to);
        
        TranslateResponse response = translateService.translateDocument(file, from, to);
        return ResultUtils.success(response);
    }
    
    @PostMapping("/document/to-zh")
    public BaseResponse<TranslateResponse> translateDocumentToChinese(
            @RequestParam("file") MultipartFile file) {
        
        log.info("收到文档翻译为中文请求: 文件名={}", file.getOriginalFilename());
        
        TranslateResponse response = translateService.translateDocumentToZh(file);
        return ResultUtils.success(response);
    }
    
    @PostMapping("/document/to-en")
    public BaseResponse<TranslateResponse> translateDocumentToEnglish(
            @RequestParam("file") MultipartFile file) {
        
        log.info("收到文档翻译为英文请求: 文件名={}", file.getOriginalFilename());
        
        TranslateResponse response = translateService.translateDocumentToEn(file);
        return ResultUtils.success(response);
    }
    
    @PostMapping("/document/format/to-zh")
    public ResponseEntity<byte[]> translateDocumentToChineseWithFormat(
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("收到文档翻译为中文并保持格式请求: 文件名={}", file.getOriginalFilename());
            
            byte[] translatedDocument = translateService.translateDocumentToZhWithFormat(file);
            
            String filename = "translated_" + file.getOriginalFilename();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(translatedDocument);
        } catch (Exception e) {
            log.error("文档格式化翻译失败", e);
            throw new BusinessException(ErrorCode.DOCUMENT_FORMAT_ERROR, "文档翻译失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/document/format/to-en")
    public ResponseEntity<byte[]> translateDocumentToEnglishWithFormat(
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("收到文档翻译为英文并保持格式请求: 文件名={}", file.getOriginalFilename());
            
            byte[] translatedDocument = translateService.translateDocumentToEnWithFormat(file);
            
            String filename = "translated_" + file.getOriginalFilename();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(translatedDocument);
        } catch (Exception e) {
            log.error("文档格式化翻译失败", e);
            throw new BusinessException(ErrorCode.DOCUMENT_FORMAT_ERROR, "文档翻译失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/document/format")
    public ResponseEntity<byte[]> translateDocumentWithFormat(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "from", defaultValue = "auto") String from,
            @RequestParam(value = "to", defaultValue = "zh") String to) {
        
        try {
            log.info("收到文档翻译并保持格式请求: 文件名={}, 源语言={}, 目标语言={}", 
                    file.getOriginalFilename(), from, to);
            
            byte[] translatedDocument = translateService.translateDocumentWithFormat(file, from, to);
            
            String filename = "translated_" + file.getOriginalFilename();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(translatedDocument);
        } catch (Exception e) {
            log.error("文档格式化翻译失败", e);
            throw new BusinessException(ErrorCode.DOCUMENT_FORMAT_ERROR, "文档翻译失败: " + e.getMessage());
        }
    }
}