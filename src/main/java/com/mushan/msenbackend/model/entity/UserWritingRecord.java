package com.mushan.msenbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 作文题目表
 * @TableName user_writing_record
 */
@TableName(value ="user_writing_record")
@Data
public class UserWritingRecord {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 作文题目 ID（对应 writing_topic.id）
     */
    private Long topicId;

    /**
     * 用户作文原文
     */
    private String content;

    /**
     * 批改状态（0-待批改 1-批改中 2-已完成 3-批改失败）
     */
    private Integer correctStatus;

    /**
     * 得分
     */
    private BigDecimal score;

    /**
     * 满分
     */
    private BigDecimal fullScore;

    /**
     * 评语
     */
    private String comment;

    /**
     * 错词列表（JSON 格式）
     */
    private String errorWords;

    /**
     * 推荐词列表（JSON 格式）
     */
    private String recommendedWords;

    /**
     * 批改时间
     */
    private Date correctTime;

    /**
     * 作文字数统计
     */
    private Integer wordCount;

    /**
     * 是否删除（0/1）
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}