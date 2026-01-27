package com.mushan.msenbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mushan.msenbackend.model.dto.wordrecord.WordCollectRequest;
import com.mushan.msenbackend.model.dto.wordrecord.WordLearnRequest;
import com.mushan.msenbackend.model.dto.wordrecord.WordReviewRequest;
import com.mushan.msenbackend.model.entity.Userwordrecord;
import com.mushan.msenbackend.model.vo.LearnProgressVO;
import com.mushan.msenbackend.model.vo.WordCardVO;
import com.mushan.msenbackend.model.vo.WordRecordVO;

import java.util.List;
import java.util.Set;

/**
 * @author Danie
 * @description 针对表【userwordrecord(用户单词学习记录表)】的数据库操作Service
 * @createDate 2025-12-16 14:32:22
 */
public interface UserwordrecordService extends IService<Userwordrecord> {

    /**
     * 统计用户已学单词数量
     *
     * @param userId   用户ID
     * @param wordType 词书类型
     * @return 已学单词数
     */
    Long countLearnedWords(Long userId, String wordType);

    /**
     * 获取用户已学过的单词ID集合
     *
     * @param userId   用户ID
     * @param wordType 词书类型
     * @return 单词ID集合
     */
    Set<Integer> getLearnedWordIds(Long userId, String wordType);

    /**
     * 保存新词学习记录
     *
     * @param userId  用户ID
     * @param request 学习请求
     * @return 记录ID
     */
    Long saveLearnRecord(Long userId, WordLearnRequest request);

    /**
     * 更新复习记录（基于艾宾浩斯算法）
     *
     * @param userId  用户ID
     * @param request 复习请求
     * @return 是否成功
     */
    Boolean updateReviewRecord(Long userId, WordReviewRequest request);

    /**
     * 获取今日待复习单词列表
     *
     * @param userId   用户ID
     * @param wordType 词书类型
     * @param limit    限制数量
     * @return 待复习单词卡片列表
     */
    List<WordCardVO> getReviewWordList(Long userId, String wordType, int limit);

    /**
     * 统计今日待复习单词数量
     *
     * @param userId   用户ID
     * @param wordType 词书类型
     * @return 待复习数量
     */
    Integer countTodayReviewPending(Long userId, String wordType);

    /**
     * 统计今日已学新词数量
     *
     * @param userId   用户ID
     * @param wordType 词书类型
     * @return 今日已学新词数
     */
    Integer countTodayNewWords(Long userId, String wordType);

    /**
     * 统计今日已复习单词数量
     *
     * @param userId   用户ID
     * @param wordType 词书类型
     * @return 今日已复习数
     */
    Integer countTodayReviewedWords(Long userId, String wordType);

    /**
     * 更新收藏状态
     *
     * @param userId  用户ID
     * @param request 收藏请求
     * @return 是否成功
     */
    Boolean updateCollectStatus(Long userId, WordCollectRequest request);

    /**
     * 获取用户学习进度概览
     *
     * @param userId   用户ID
     * @param wordType 词书类型
     * @return 学习进度VO
     */
    LearnProgressVO getLearnProgress(Long userId, String wordType);

    /**
     * 获取收藏列表
     *
     * @param userId   用户ID
     * @param wordType 词书类型（可为null表示所有）
     * @param pageNo   页码
     * @param pageSize 每页数量
     * @return 收藏单词列表
     */
    Page<WordRecordVO> getCollectedWords(Long userId, String wordType, int pageNo, int pageSize);

    /**
     * 统计已掌握单词数（memLevel=5）
     *
     * @param userId   用户ID
     * @param wordType 词书类型
     * @return 已掌握数
     */
    Integer countMasteredWords(Long userId, String wordType);

    /**
     * 统计收藏单词数
     *
     * @param userId   用户ID
     * @param wordType 词书类型
     * @return 收藏数
     */
    Integer countCollectedWords(Long userId, String wordType);

    /**
     * 清空生词本（取消收藏）
     *
     * @param userId   用户ID
     * @param wordType 词书类型（可为null表示所有）
     * @return 清空的单词数量
     */
    Integer clearCollectedWords(Long userId, String wordType);
}
