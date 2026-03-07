package com.mushan.msenbackend.model.dto.writing;

import lombok.Data;

import java.io.Serializable;

/**
 * 作文批改请求
 */
@Data
public class EssayCorrectionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 作文内容
     */
    private String content;

    /**
     * 作文题目（可选）
     */
    private String topic;
}
