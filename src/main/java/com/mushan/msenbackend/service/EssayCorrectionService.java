package com.mushan.msenbackend.service;

import com.mushan.msenbackend.model.vo.EssayCorrectionResult;

/**
 * 作文批改服务
 */
public interface EssayCorrectionService {

    /**
     * 批改作文
     * @param essayContent 作文内容
     * @param topic 题目（可选）
     * @return 批改结果
     */
    EssayCorrectionResult correctEssay(String essayContent, String topic);
}
