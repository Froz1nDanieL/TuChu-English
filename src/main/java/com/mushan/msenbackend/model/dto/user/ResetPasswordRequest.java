package com.mushan.msenbackend.model.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 重置密码请求
 */
@Data
public class ResetPasswordRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String userEmail;
    
    /**
     * 邮箱验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String emailCode;
    
    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
    
    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;
}
