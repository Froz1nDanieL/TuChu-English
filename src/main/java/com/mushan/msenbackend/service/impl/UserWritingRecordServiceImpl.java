package com.mushan.msenbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushan.msenbackend.model.entity.UserWritingRecord;
import com.mushan.msenbackend.service.UserWritingRecordService;
import com.mushan.msenbackend.mapper.UserWritingRecordMapper;
import org.springframework.stereotype.Service;

/**
* @author Danie
* @description 针对表【user_writing_record(作文题目表)】的数据库操作Service实现
* @createDate 2026-03-04 15:46:46
*/
@Service
public class UserWritingRecordServiceImpl extends ServiceImpl<UserWritingRecordMapper, UserWritingRecord>
    implements UserWritingRecordService{

}




