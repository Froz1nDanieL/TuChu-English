package com.mushan.msenbackend.exception;

import lombok.Getter;

/**
 * 自定义错误码
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    
    // 邮箱相关错误码
    EMAIL_FORMAT_ERROR(40001, "邮箱格式错误"),
    EMAIL_ALREADY_EXISTS(40002, "邮箱已被注册"),
    EMAIL_NOT_EXISTS(40003, "邮箱未注册"),
    VERIFY_CODE_ERROR(40004, "验证码错误或已过期"),
    SEND_CODE_TOO_FREQUENT(40005, "发送验证码过于频繁，请稍后再试"),
    EMAIL_SEND_FAILED(50002, "邮件发送失败"),
    PASSWORD_NOT_MATCH(40006, "两次输入的密码不一致"),
    OLD_PASSWORD_ERROR(40007, "原密码错误"),
    PASSWORD_SAME_AS_OLD(40008, "新密码不能与原密码相同");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}