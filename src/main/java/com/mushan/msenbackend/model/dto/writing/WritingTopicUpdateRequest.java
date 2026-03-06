package com.mushan.msenbackend.model.dto.writing;

import lombok.Data;

/**
 * 更新作文题目请求
 */
@Data
public class WritingTopicUpdateRequest {
    /**
     * 主键
     */
    private Long id;

    /**
     * 作文题目标题
     */
    private String title;

    /**
     * 作文题目描述/写作要求
     */
    private String description;

    /**
     * 考试类型（cet4/cet6/ielts/toefl/gk/zk/ky等）
     */
    private String examType;

    /**
     * 字数要求
     */
    private Integer wordLimit;

    /**
     * 时间限制（分钟）
     */
    private Integer timeLimit;
}