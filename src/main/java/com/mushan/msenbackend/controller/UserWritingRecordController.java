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
import com.mushan.msenbackend.model.dto.writing.UserWritingRecordAddRequest;
import com.mushan.msenbackend.model.entity.User;
import com.mushan.msenbackend.model.entity.UserWritingRecord;
import com.mushan.msenbackend.service.UserService;
import com.mushan.msenbackend.service.UserWritingRecordService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 用户写作文记录控制器
 */
@RestController
@RequestMapping("/user-writing-record")
public class UserWritingRecordController {
    @Resource
    private UserWritingRecordService userWritingRecordService;

    @Resource
    private UserService userService;

    /**
     * 添加用户写作文记录
     */
    @PostMapping("/add")
    public BaseResponse<Long> addUserWritingRecord(@RequestBody UserWritingRecordAddRequest userWritingRecordAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userWritingRecordAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        UserWritingRecord userWritingRecord = new UserWritingRecord();
        BeanUtils.copyProperties(userWritingRecordAddRequest, userWritingRecord);
        userWritingRecord.setUserId(loginUser.getId());
        userWritingRecord.setCorrectStatus(0); // 初始状态为待批改
        userWritingRecord.setIsDeleted(0); // 初始状态为未删除
        // 计算作文字数
        if (userWritingRecordAddRequest.getContent() != null) {
            userWritingRecord.setWordCount(userWritingRecordAddRequest.getContent().length());
        }
        boolean result = userWritingRecordService.save(userWritingRecord);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(userWritingRecord.getId());
    }

    /**
     * 删除用户写作文记录
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUserWritingRecord(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        UserWritingRecord userWritingRecord = userWritingRecordService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(userWritingRecord == null, ErrorCode.NOT_FOUND_ERROR);
        // 检查是否是用户自己的记录或管理员
        if (!userWritingRecord.getUserId().equals(loginUser.getId())) {
            // 检查是否是管理员
            if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        boolean result = userWritingRecordService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取用户写作文记录
     */
    @GetMapping("/get")
    public BaseResponse<UserWritingRecord> getUserWritingRecordById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        UserWritingRecord userWritingRecord = userWritingRecordService.getById(id);
        ThrowUtils.throwIf(userWritingRecord == null, ErrorCode.NOT_FOUND_ERROR);
        // 检查是否是用户自己的记录或管理员
        if (!userWritingRecord.getUserId().equals(loginUser.getId())) {
            // 检查是否是管理员
            if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        return ResultUtils.success(userWritingRecord);
    }

    /**
     * 分页获取当前用户的写作文记录列表
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<UserWritingRecord>> listUserWritingRecordByPage(long current, long pageSize, HttpServletRequest request) {
        ThrowUtils.throwIf(current <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Page<UserWritingRecord> userWritingRecordPage = userWritingRecordService.page(
                new Page<>(current, pageSize),
                userWritingRecordService.lambdaQuery().eq(UserWritingRecord::getUserId, loginUser.getId())
        );
        return ResultUtils.success(userWritingRecordPage);
    }

    /**
     * 分页获取所有用户的写作文记录列表（仅管理员）
     */
    @GetMapping("/list/page/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserWritingRecord>> listAllUserWritingRecordByPage(long current, long pageSize) {
        ThrowUtils.throwIf(current <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);
        Page<UserWritingRecord> userWritingRecordPage = userWritingRecordService.page(new Page<>(current, pageSize));
        return ResultUtils.success(userWritingRecordPage);
    }
}
