package com.mushan.msenbackend.model.dto.translate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * 百度翻译响应结果
 */
@Data
public class TranslateResponse {
    /**
     * 翻译结果状态码
     */
    @JsonProperty("error_code")
    private Integer errorCode;
    
    /**
     * 错误信息
     */
    @JsonProperty("error_msg")
    private String errorMsg;

    /**
     * 源语言
     */
    private String from;

    /**
     * 目标语言
     */
    private String to;

    /**
     * 翻译结果列表
     */
    @JsonProperty("trans_result")
    private List<TransResult> transResults;


    /**
     * 翻译结果项（包含src和dst字段）
     */
    @Data
    public static class TransResult {
        /**
         * 源文本
         */
        private String src;

        /**
         * 翻译文本
         */
        private String dst;
    }
}