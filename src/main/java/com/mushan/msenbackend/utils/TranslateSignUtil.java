package com.mushan.msenbackend.utils;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 百度翻译签名工具类
 */
@Slf4j
public class TranslateSignUtil {

    /**
     * 生成签名
     *
     * @param appId   应用ID
     * @param query   待翻译文本
     * @param salt    随机数
     * @param secret  密钥
     * @return 签名字符串
     */
    public static String generateSign(String appId, String query, String salt, String secret) {
        // 严格按照百度API要求的顺序拼接: appid + q + salt + 密钥
        String signStr = appId + query + salt + secret;
        
        log.info("签名生成 - 拼接字符串: {}", signStr);
        log.info("参数详情 - appid:{}, q:'{}', salt:{}, secret_length:{}", 
                appId, query, salt, secret.length());
        
        // MD5加密并转换为小写
        String sign = DigestUtil.md5Hex(signStr).toLowerCase();
        log.info("签名结果: {}", sign);
        
        return sign;
    }
}