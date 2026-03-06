package com.mushan.msenbackend.model.dto.writing;

import lombok.Data;

/**
 * 添加用户写作文记录请求
 */
@Data
public class UserWritingRecordAddRequest {
    /**
     * 作文题目 ID
     */
    private Long topicId;

    /**
     * 用户作文原文
     */
    private String content;
}