package com.mushan.msenbackend.model.dto.translate;

import lombok.Data;

/**
 * 百度翻译请求参数
 */
@Data
public class TranslateRequest {
    /**
     * 待翻译文本
     */
    private String q;

    /**
     * 源语言
     */
    private String from;

    /**
     * 目标语言
     */
    private String to;

    /**
     * APP ID
     */
    private String appid;

    /**
     * 签名
     */
    private String sign;

    /**
     * 盐值
     */
    private String salt;
}