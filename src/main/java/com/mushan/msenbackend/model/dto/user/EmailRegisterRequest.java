package com.mushan.msenbackend.model.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 邮箱注册请求
 */
@Data
public class EmailRegisterRequest implements Serializable {
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
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,}$", message = "账号格式不正确，只能包含字母、数字和下划线，且长度不少于4位")
    private String userAccount;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String userPassword;
    
    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;
}
