package com.mushan.msenbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.msenbackend.annotation.AuthCheck;
import com.mushan.msenbackend.common.BaseResponse;
import com.mushan.msenbackend.common.DeleteRequest;
import com.mushan.msenbackend.common.ResultUtils;
import com.mushan.msenbackend.constant.UserConstant;
import com.mushan.msenbackend.exception.BusinessException;
import com.mushan.msenbackend.exception.ErrorCode;
import com.mushan.msenbackend.exception.ThrowUtils;
import com.mushan.msenbackend.model.dto.user.*;
import com.mushan.msenbackend.model.entity.User;
import com.mushan.msenbackend.model.vo.LoginUserVO;
import com.mushan.msenbackend.model.vo.UserVO;
import com.mushan.msenbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    /**
     * 用户注册（已禁用，请使用邮箱注册）
     * @deprecated 用户只能通过邮箱注册，请使用 /user/email/register 接口
     */
    @Deprecated
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号注册已禁用，请使用邮箱注册");
    }

    /**
     * 用户登录（支持账号/邮箱登录）
     * 
     * @param userLoginRequest 登录请求，userAccount 可以是账号或邮箱
     * @param request HTTP请求
     * @return 登录用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户登出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 编辑用户 (给当前登录用户使用)
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editUser(HttpServletRequest request, @RequestBody UserEditRequest userEditRequest) {
        if (userEditRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        boolean result = userService.editUser(loginUser, userEditRequest);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
    
    // ==================== 邮箱注册登录功能 ====================
    
    /**
     * 发送注册验证码
     */
    @PostMapping("/email/register/code")
    public BaseResponse<Boolean> sendRegisterCode(@RequestBody @Valid SendCodeRequest request) {
        userService.sendRegisterCode(request.getUserEmail());
        return ResultUtils.success(true);
    }
    
    /**
     * 邮箱注册
     */
    @PostMapping("/email/register")
    public BaseResponse<Long> emailRegister(@RequestBody @Valid EmailRegisterRequest request) {
        long userId = userService.emailRegister(request);
        return ResultUtils.success(userId);
    }
    
    /**
     * 发送重置密码验证码
     */
    @PostMapping("/email/reset/code")
    public BaseResponse<Boolean> sendResetPasswordCode(@RequestBody @Valid SendCodeRequest request) {
        userService.sendResetPasswordCode(request.getUserEmail());
        return ResultUtils.success(true);
    }
    
    /**
     * 重置密码
     */
    @PostMapping("/email/reset/password")
    public BaseResponse<Boolean> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        boolean result = userService.resetPassword(request);
        return ResultUtils.success(result);
    }

}
