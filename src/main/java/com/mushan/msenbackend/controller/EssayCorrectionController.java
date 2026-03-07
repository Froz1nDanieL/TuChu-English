package com.mushan.msenbackend.controller;

import com.mushan.msenbackend.common.BaseResponse;
import com.mushan.msenbackend.common.ResultUtils;
import com.mushan.msenbackend.exception.BusinessException;
import com.mushan.msenbackend.exception.ErrorCode;
import com.mushan.msenbackend.exception.ThrowUtils;
import com.mushan.msenbackend.model.dto.writing.EssayCorrectionRequest;
import com.mushan.msenbackend.model.vo.EssayCorrectionResult;
import com.mushan.msenbackend.service.EssayCorrectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作文批改控制器
 */
@Tag(name = "作文批改接口")
@RestController
@RequestMapping("/essay-correction")
public class EssayCorrectionController {

    @Resource
    private EssayCorrectionService essayCorrectionService;

    /**
     * 批改作文
     */
    @Operation(summary = "批改作文")
    @PostMapping("/correct")
    public BaseResponse<EssayCorrectionResult> correctEssay(@RequestBody EssayCorrectionRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String content = request.getContent();
        ThrowUtils.throwIf(content == null || content.isBlank(), ErrorCode.PARAMS_ERROR, "作文内容不能为空");
        
        try {
            EssayCorrectionResult result = essayCorrectionService.correctEssay(content, request.getTopic());
            return ResultUtils.success(result);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "作文批改失败：" + e.getMessage());
        }
    }
}
