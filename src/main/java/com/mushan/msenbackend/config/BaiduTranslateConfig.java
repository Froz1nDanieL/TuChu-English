package com.mushan.msenbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 百度翻译API配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "baidu.translate")
public class BaiduTranslateConfig {
    /**
     * APP ID
     */
    private String appId;

    /**
     * 密钥
     */
    private String secretKey;
    
    /**
     * API地址
     */
    private String apiUrl = "https://fanyi-api.baidu.com/api/trans/vip/translate";
}