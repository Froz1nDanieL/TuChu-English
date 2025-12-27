package com.mushan.msenbackend.service;

/**
 * 邮件服务
 */
public interface EmailService {
    /**
     * 发送验证码邮件
     * @param toEmail 收件人邮箱
     * @param code 验证码
     * @param type 验证码类型（register/reset）
     */
    void sendVerifyCode(String toEmail, String code, String type);
}
