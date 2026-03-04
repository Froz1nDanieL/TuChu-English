package com.mushan.msenbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 作文题目表
 * @TableName writing_topic
 */
@TableName(value ="writing_topic")
@Data
public class WritingTopic {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 作文题目标题（简短概括）
     */
    private String title;

    /**
     * 作文题目描述/写作要求（详细内容）
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}