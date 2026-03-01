package com.mushan.msenbackend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.mushan.msenbackend.common.BaseResponse;
import com.mushan.msenbackend.common.ResultUtils;
import com.mushan.msenbackend.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static com.mushan.msenbackend.exception.ErrorCode.PARAMS_ERROR;

@RestController
@RequestMapping("/test")
public class TestController {

    // 测试登录接口
    @GetMapping("/login")
    public BaseResponse<Map<String, Object>> login() {
        // 模拟用户id为1的用户登录
        StpUtil.login(1);
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("isLogin", StpUtil.isLogin());
        
        return ResultUtils.success(data);
    }

    // 测试退出登录接口
    @GetMapping("/logout")
    public BaseResponse<String> logout() {
        StpUtil.logout();
        return ResultUtils.success("退出成功");
    }

//    // 测试需要登录的接口
//    @GetMapping("/info")
//    public BaseResponse<String> info() {
//        if (!StpUtil.isLogin()) {
//            return ResultUtils.error(PARAMS_ERROR);
//        }
//        return ResultUtils.success("恭喜你，访问成功！");
//    }
}