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
import com.mushan.msenbackend.model.dto.writing.WritingTopicAddRequest;
import com.mushan.msenbackend.model.dto.writing.WritingTopicUpdateRequest;
import com.mushan.msenbackend.model.entity.WritingTopic;
import com.mushan.msenbackend.service.WritingTopicService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 作文题目控制器
 */
@RestController
@RequestMapping("/writing-topic")
public class WritingTopicController {
    @Resource
    private WritingTopicService writingTopicService;

    /**
     * 添加作文题目
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addWritingTopic(@RequestBody WritingTopicAddRequest writingTopicAddRequest) {
        ThrowUtils.throwIf(writingTopicAddRequest == null, ErrorCode.PARAMS_ERROR);
        WritingTopic writingTopic = new WritingTopic();
        BeanUtils.copyProperties(writingTopicAddRequest, writingTopic);
        boolean result = writingTopicService.save(writingTopic);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(writingTopic.getId());
    }

    /**
     * 删除作文题目
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteWritingTopic(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = writingTopicService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新作文题目
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateWritingTopic(@RequestBody WritingTopicUpdateRequest writingTopicUpdateRequest) {
        if (writingTopicUpdateRequest == null || writingTopicUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        WritingTopic writingTopic = new WritingTopic();
        BeanUtils.copyProperties(writingTopicUpdateRequest, writingTopic);
        boolean result = writingTopicService.updateById(writingTopic);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取作文题目
     */
    @GetMapping("/get")
    public BaseResponse<WritingTopic> getWritingTopicById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        WritingTopic writingTopic = writingTopicService.getById(id);
        ThrowUtils.throwIf(writingTopic == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(writingTopic);
    }

    /**
     * 分页获取作文题目列表
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<WritingTopic>> listWritingTopicByPage(long current, long pageSize) {
        ThrowUtils.throwIf(current <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);
        Page<WritingTopic> writingTopicPage = writingTopicService.page(new Page<>(current, pageSize));
        return ResultUtils.success(writingTopicPage);
    }
}