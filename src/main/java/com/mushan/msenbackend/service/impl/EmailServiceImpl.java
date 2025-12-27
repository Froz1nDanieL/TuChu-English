package com.mushan.msenbackend.service.impl;

import com.mushan.msenbackend.exception.BusinessException;
import com.mushan.msenbackend.exception.ErrorCode;
import com.mushan.msenbackend.service.EmailService;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现类
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    @Resource // 设置为非必需，避免启动失败
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    @Override
    public void sendVerifyCode(String toEmail, String code, String type) {
        // 开发环境：如果没有配置邮箱或配置错误，直接打印验证码到控制台
        if (mailSender == null) {
            log.warn("【开发模式】邮箱服务不可用，验证码将打印到控制台");
            String action = "register".equals(type) ? "注册账号" : "重置密码";
            log.info("\n" +
                    "==============================================\n" +
                    "【邮箱验证码】\n" +
                    "收件人：{}\n" +
                    "类型：{}\n" +
                    "验证码：{}\n" +
                    "有效期：5分钟\n" +
                    "==============================================\n",
                    toEmail, action, code);
            return;
        }
        
        // 生产环境：真实发送邮件
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            
            String subject = "register".equals(type) ? "注册验证码" : "密码重置验证码";
            helper.setSubject("【英语学习平台】" + subject);
            
            String content = buildEmailContent(code, type);
            helper.setText(content, true);
            
            mailSender.send(message);
            log.info("邮件发送成功，收件人：{}", toEmail);
        } catch (Exception e) {
            log.error("邮件发送失败，收件人：{}", toEmail, e);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
    
    private String buildEmailContent(String code, String type) {
        String action = "register".equals(type) ? "注册账号" : "重置密码";
        return "<html>" +
               "<body style='font-family: Arial, sans-serif;'>" +
               "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>" +
               "<h2 style='color: #333; text-align: center;'>验证码</h2>" +
               "<p style='color: #666; font-size: 14px;'>您正在" + action + "，验证码为：</p>" +
               "<div style='background-color: #f5f5f5; padding: 20px; text-align: center; border-radius: 4px; margin: 20px 0;'>" +
               "<h1 style='color: #4CAF50; margin: 0; font-size: 36px; letter-spacing: 5px;'>" + code + "</h1>" +
               "</div>" +
               "<p style='color: #999; font-size: 12px;'>• 验证码5分钟内有效，请勿泄露给他人。</p>" +
               "<p style='color: #999; font-size: 12px;'>• 如非本人操作，请忽略此邮件。</p>" +
               "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>" +
               "<p style='color: #999; font-size: 12px; text-align: center;'>此邮件由系统自动发送，请勿回复。</p>" +
               "</div>" +
               "</body>" +
               "</html>";
    }
}
