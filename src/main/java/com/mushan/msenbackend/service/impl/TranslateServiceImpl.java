package com.mushan.msenbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mushan.msenbackend.config.BaiduTranslateConfig;
import com.mushan.msenbackend.exception.BusinessException;
import com.mushan.msenbackend.exception.ErrorCode;
import com.mushan.msenbackend.model.dto.translate.TranslateResponse;
import com.mushan.msenbackend.service.TranslateService;
import com.mushan.msenbackend.utils.DocumentParserUtil;
import com.mushan.msenbackend.utils.DocumentToHtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 百度翻译服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TranslateServiceImpl implements TranslateService {

    private final BaiduTranslateConfig baiduConfig;

    @Override
    public TranslateResponse translate(String text, String from, String to) {
        try {
            // 参数校验
            if (StrUtil.isBlank(text)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "翻译文本不能为空");
            }
            
            String salt = String.valueOf(System.currentTimeMillis());
            String sign = generateSign(baiduConfig.getAppId(), text, salt, baiduConfig.getSecretKey());

            // 构建请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("q", text);
            params.put("from", from);
            params.put("to", to);
            params.put("appid", baiduConfig.getAppId());
            params.put("salt", salt);
            params.put("sign", sign);

            // 发送HTTP请求
            String response = HttpUtil.post(baiduConfig.getApiUrl(), params);
            log.debug("百度翻译API响应: {}", response);

            // 解析JSON响应
            JSONObject jsonResponse = JSONUtil.parseObj(response);
            
            TranslateResponse translateResponse = new TranslateResponse();
            translateResponse.setFrom(jsonResponse.getStr("from", "auto"));
            translateResponse.setTo(jsonResponse.getStr("to", "zh"));
            
            // 检查是否有错误
            if (jsonResponse.containsKey("error_code")) {
                String errorCode = jsonResponse.getStr("error_code");
                String errorMsg = jsonResponse.getStr("error_msg", "未知错误");
                translateResponse.setErrorCode(Integer.valueOf(errorCode));
                translateResponse.setErrorMsg(errorMsg);
                throw new BusinessException(ErrorCode.TRANSLATE_SERVICE_ERROR, 
                    StrUtil.format("翻译API错误: {} (code: {})" , errorMsg, errorCode));
            }
            
            // 解析翻译结果
            JSONArray transResultArray = jsonResponse.getJSONArray("trans_result");
            if (transResultArray != null && !transResultArray.isEmpty()) {
                List<TranslateResponse.TransResult> transResults = new ArrayList<>();
                for (int i = 0; i < transResultArray.size(); i++) {
                    JSONObject item = transResultArray.getJSONObject(i);
                    TranslateResponse.TransResult result = new TranslateResponse.TransResult();
                    result.setSrc(item.getStr("src"));
                    result.setDst(item.getStr("dst"));
                    transResults.add(result);
                }
                translateResponse.setTransResults(transResults);
            }
            
            return translateResponse;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("翻译服务调用失败", e);
            throw new BusinessException(ErrorCode.TRANSLATE_SERVICE_ERROR, 
                StrUtil.format("翻译服务调用失败: {}", e.getMessage()));
        }
    }

    @Override
    public TranslateResponse translateToZh(String text) {
        return translate(text, "auto", "zh");
    }

    @Override
    public TranslateResponse translateToEn(String text) {
        return translate(text, "auto", "en");
    }

    @Override
    public TranslateResponse translateDocument(MultipartFile file, String from, String to) {
        try {
            // 直接解析文档内容并翻译，不进行特殊的语法保护处理
            String content = DocumentParserUtil.parseDocument(file);
            return translate(content, from, to);
        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            log.error("文档解析或翻译失败", e);
            throw new BusinessException(ErrorCode.DOCUMENT_TRANSLATE_ERROR, "文档翻译失败: " + e.getMessage());
        }
    }

    @Override
    public TranslateResponse translateDocumentToZh(MultipartFile file) {
        return translateDocument(file, "auto", "zh");
    }

    @Override
    public TranslateResponse translateDocumentToEn(MultipartFile file) {
        return translateDocument(file, "auto", "en");
    }

    @Override
    public byte[] translateDocumentWithFormat(MultipartFile file, String from, String to) {
        // 使用HTML中间格式方案保持文档格式
        return translateDocumentViaHtml(file, from, to);
    }

    @Override
    public byte[] translateDocumentToZhWithFormat(MultipartFile file) {
        return translateDocumentWithFormat(file, "auto", "zh");
    }

    @Override
    public byte[] translateDocumentToEnWithFormat(MultipartFile file) {
        return translateDocumentWithFormat(file, "auto", "en");
    }

    private byte[] translateDocumentViaHtml(MultipartFile file, String from, String to) {
        log.info("开始使用HTML中间格式方案进行文档翻译");
        
        try {
            // 1. 将Word文档转换为HTML（保持所有格式）
            byte[] docxBytes = file.getBytes();
            String htmlContent = DocumentToHtmlConverter.convertDocxToHtml(docxBytes);
            log.info("文档转换为HTML完成，HTML长度: {} 字符", htmlContent.length());
            
            // 2. 提取可翻译的文本
            List<String> originalTexts = DocumentToHtmlConverter.extractTranslatableText(htmlContent);
            log.info("提取到 {} 个可翻译文本段落", originalTexts.size());
            
            // 3. 批量翻译文本
            List<String> translatedTexts = new ArrayList<>();
            for (String text : originalTexts) {
                if (!text.trim().isEmpty()) {
                    TranslateResponse response = translate(text, from, to);
                    if (response.getTransResults() != null && !response.getTransResults().isEmpty()) {
                        translatedTexts.add(response.getTransResults().get(0).getDst());
                    } else {
                        translatedTexts.add(text); // 翻译失败时保持原文
                    }
                } else {
                    translatedTexts.add(text);
                }
            }
            
            log.info("文本翻译完成，翻译了 {} 个段落", translatedTexts.size());
            
            // 4. 将翻译后的文本注入HTML
            String translatedHtml = DocumentToHtmlConverter.injectTranslatedText(htmlContent, translatedTexts);
            log.info("翻译文本注入HTML完成");
            
            // 5. 将HTML转换回Word文档
            byte[] resultDocx = DocumentToHtmlConverter.convertHtmlToDocx(translatedHtml);
            log.info("HTML转换回Word文档完成，文档大小: {} 字节", resultDocx.length);
            
            return resultDocx;
            
        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            log.error("使用HTML方案翻译文档时出错", e);
            throw new BusinessException(ErrorCode.DOCUMENT_FORMAT_ERROR, "文档格式保持翻译失败: " + e.getMessage());
        }
    }

    /**
     * 生成签名 - 使用Hutool简化实现
     */
    private String generateSign(String appId, String text, String salt, String secretKey) {
        String signStr = appId + text + salt + secretKey;
        return SecureUtil.md5(signStr);
    }
}