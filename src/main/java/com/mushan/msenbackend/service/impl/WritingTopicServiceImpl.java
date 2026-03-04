package com.mushan.msenbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushan.msenbackend.model.entity.WritingTopic;
import com.mushan.msenbackend.service.WritingTopicService;
import com.mushan.msenbackend.mapper.WritingTopicMapper;
import org.springframework.stereotype.Service;

/**
* @author Danie
* @description 针对表【writing_topic(作文题目表)】的数据库操作Service实现
* @createDate 2026-03-04 15:40:41
*/
@Service
public class WritingTopicServiceImpl extends ServiceImpl<WritingTopicMapper, WritingTopic>
    implements WritingTopicService{

}




