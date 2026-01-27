package com.mushan.msenbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushan.msenbackend.exception.BusinessException;
import com.mushan.msenbackend.exception.ErrorCode;
import com.mushan.msenbackend.mapper.EngdictMapper;
import com.mushan.msenbackend.mapper.UserwordrecordMapper;
import com.mushan.msenbackend.model.dto.wordrecord.WordCollectRequest;
import com.mushan.msenbackend.model.dto.wordrecord.WordLearnRequest;
import com.mushan.msenbackend.model.dto.wordrecord.WordReviewRequest;
import com.mushan.msenbackend.model.entity.Engdict;
import com.mushan.msenbackend.model.entity.Userlearnplan;
import com.mushan.msenbackend.model.entity.Userwordrecord;
import com.mushan.msenbackend.model.enums.WordTypeEnum;
import com.mushan.msenbackend.model.vo.LearnProgressVO;
import com.mushan.msenbackend.model.vo.WordCardVO;
import com.mushan.msenbackend.model.vo.WordRecordVO;
import com.mushan.msenbackend.service.EngdictService;
import com.mushan.msenbackend.service.UserlearnplanService;
import com.mushan.msenbackend.service.UserwordrecordService;
import com.mushan.msenbackend.utils.WordExchangeUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Danie
 * @description 针对表【userwordrecord(用户单词学习记录表)】的数据库操作Service实现
 * @createDate 2025-12-16 14:32:22
 */
@Service
public class UserwordrecordServiceImpl extends ServiceImpl<UserwordrecordMapper, Userwordrecord>
        implements UserwordrecordService {

    @Resource
    private EngdictMapper engdictMapper;

    @Resource
    @Lazy
    private EngdictService engdictService;

    @Resource
    @Lazy
    private UserlearnplanService userlearnplanService;

    /**
     * 艾宾浩斯记忆等级升级规则
     * key: 当前等级, value: [new_level, days_to_next_review]
     */
    private static final Map<Integer, int[]> UPGRADE_RULES = new HashMap<>();

    /**
     * 艾宾浩斯记忆等级降级规则
     * key: 当前等级, value: [new_level, days, hours]
     */
    private static final Map<Integer, int[]> DOWNGRADE_RULES = new HashMap<>();

    static {
        // 复习正确升级规则
        UPGRADE_RULES.put(0, new int[]{1, 1});      // 0 -> 1, +1天
        UPGRADE_RULES.put(1, new int[]{2, 3});      // 1 -> 2, +3天
        UPGRADE_RULES.put(2, new int[]{3, 7});      // 2 -> 3, +7天
        UPGRADE_RULES.put(3, new int[]{4, 14});     // 3 -> 4, +14天
        UPGRADE_RULES.put(4, new int[]{5, 0});      // 4 -> 5, 永久掌握

        // 复习错误降级规则
        DOWNGRADE_RULES.put(0, new int[]{0, 0, 12}); // 0 -> 0, +12小时
        DOWNGRADE_RULES.put(1, new int[]{0, 0, 12}); // 1 -> 0, +12小时
        DOWNGRADE_RULES.put(2, new int[]{1, 1, 0});  // 2 -> 1, +1天
        DOWNGRADE_RULES.put(3, new int[]{2, 3, 0});  // 3 -> 2, +3天
        DOWNGRADE_RULES.put(4, new int[]{3, 7, 0});  // 4 -> 3, +7天
    }

    @Override
    public Long countLearnedWords(Long userId, String wordType) {
        if (userId == null || StringUtils.isBlank(wordType)) {
            return 0L;
        }
        QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("wordType", wordType);
        queryWrapper.isNotNull("learnTime"); // 只统计真正学习过的单词
        return this.count(queryWrapper);
    }

    @Override
    public Set<Integer> getLearnedWordIds(Long userId, String wordType) {
        if (userId == null || StringUtils.isBlank(wordType)) {
            return Collections.emptySet();
        }
        QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("wordId");
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("wordType", wordType);
        queryWrapper.isNotNull("learnTime"); // 只获取真正学习过的单词ID
        List<Userwordrecord> records = this.list(queryWrapper);
        return records.stream()
                .map(Userwordrecord::getWordId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveLearnRecord(Long userId, WordLearnRequest request) {
        if (userId == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Integer wordId = request.getWordId();
        String wordType = request.getWordType();

        if (wordId == null || StringUtils.isBlank(wordType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "单词ID和词书类型不能为空");
        }

        // 检查是否已有该单词的记录（可能是收藏记录或学习记录）
        QueryWrapper<Userwordrecord> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("userId", userId);
        checkWrapper.eq("wordId", wordId);
        checkWrapper.eq("wordType", wordType);
        Userwordrecord existingRecord = this.getOne(checkWrapper);
        
        if (existingRecord != null && existingRecord.getLearnTime() != null) {
            // 已经学习过该单词，直接返回记录ID（不重复学习）
            return existingRecord.getId();
        }

        // 根据第一轮和第二轮测试结果自动计算记忆等级
        // choiceCorrect必须为true才能提交，choiceErrorCount反映真实掌握程度
        int memLevel;
        Date nextReviewTime;
        Date now = new Date();

        Integer firstKnow = request.getFirstKnow();
        Boolean choiceCorrect = request.getChoiceCorrect();
        Integer choiceErrorCount = request.getChoiceErrorCount();

        // 验证：第二轮测试必须通过才能提交
        if (choiceCorrect != null && !choiceCorrect) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "选词测试未通过，无法提交学习记录");
        }

        // 根据第一轮认知和选词错误次数判断记忆等级
        if (Integer.valueOf(1).equals(firstKnow)) {
            // 第一轮认识
            if (choiceErrorCount == null || choiceErrorCount == 0) {
                // 一次答对 = 我会了
                memLevel = 2;
                nextReviewTime = DateUtils.addDays(now, 3);
            } else if (choiceErrorCount == 1) {
                // 错1次后答对 = 有点模糊
                memLevel = 1;
                nextReviewTime = DateUtils.addDays(now, 1);
            } else {
                // 错2次及以上 = 没记住
                memLevel = 0;
                nextReviewTime = DateUtils.addMinutes(now, 5);
            }
        } else if (Integer.valueOf(0).equals(firstKnow)) {
            // 第一轮不认识
            if (choiceErrorCount == null || choiceErrorCount == 0) {
                // 一次答对 = 有点模糊
                memLevel = 1;
                nextReviewTime = DateUtils.addDays(now, 1);
            } else {
                // 错1次及以上 = 没记住
                memLevel = 0;
                nextReviewTime = DateUtils.addMinutes(now, 5);
            }
        } else {
            // 未进行第一轮标记，默认为没记住
            memLevel = 0;
            nextReviewTime = DateUtils.addMinutes(now, 5);
        }

        Userwordrecord record;
        
        if (existingRecord != null) {
            // 情况：已有收藏记录但未学习过，更新为学习记录
            record = existingRecord;
            record.setMemLevel(memLevel);
            record.setLearnTime(now);
            record.setNextReviewTime(nextReviewTime);
            // 保持原有的收藏状态，但如果请求中明确指定了收藏状态，则更新
            if (request.getIsCollect() != null) {
                record.setIsCollect(request.getIsCollect());
            }
            // 确保学习相关计数器已初始化（收藏时可能未设置）
            if (record.getReviewTimes() == null) {
                record.setReviewTimes(0);
            }
            if (record.getCorrectTimes() == null) {
                record.setCorrectTimes(0);
            }
            if (record.getErrorTimes() == null) {
                record.setErrorTimes(0);
            }
            if (record.getIsMastered() == null) {
                record.setIsMastered(0);
            }
        } else {
            // 情况：完全新的学习记录
            record = new Userwordrecord();
            record.setUserId(userId);
            record.setWordId(wordId);
            record.setWordType(wordType);
            record.setMemLevel(memLevel);
            record.setLearnTime(now);
            record.setNextReviewTime(nextReviewTime);
            record.setReviewTimes(0);
            record.setCorrectTimes(0);
            record.setErrorTimes(0);
            record.setIsCollect(request.getIsCollect() != null ? request.getIsCollect() : 0);
            record.setIsMastered(0);
            record.setCreateTime(now);
        }
        
        // 保存学习过程数据
        if (request.getFirstKnow() != null) {
            record.setFirstKnow(request.getFirstKnow());
        }
        if (request.getChoiceCorrect() != null) {
            record.setChoiceCorrect(request.getChoiceCorrect() ? 1 : 0);
        }
        if (request.getChoiceErrorCount() != null) {
            record.setChoiceErrorCount(request.getChoiceErrorCount());
        }
        if (request.getSpellingCorrect() != null) {
            record.setSpellingCorrect(request.getSpellingCorrect() ? 1 : 0);
        }
        
        record.setUpdateTime(now);

        // 保存或更新记录
        if (existingRecord != null) {
            this.updateById(record);
        } else {
            this.save(record);
        }

        // 更新学习计划进度
        userlearnplanService.incrementProgress(userId, 1);

        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateReviewRecord(Long userId, WordReviewRequest request) {
        if (userId == null || request == null || request.getRecordId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Userwordrecord record = this.getById(request.getRecordId());
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学习记录不存在");
        }

        // 验证记录属于当前用户
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此记录");
        }

        Date now = new Date();
        int currentLevel = record.getMemLevel();
        boolean isCorrect = Boolean.TRUE.equals(request.getIsCorrect());

        if (isCorrect) {
            // 复习正确：升级
            int[] upgrade = UPGRADE_RULES.getOrDefault(currentLevel, new int[]{5, 0});
            record.setMemLevel(upgrade[0]);

            if (upgrade[0] == 5) {
                // 达到最高等级，标记为已掌握
                record.setIsMastered(1);
                record.setNextReviewTime(null);
            } else {
                record.setNextReviewTime(DateUtils.addDays(now, upgrade[1]));
            }
            record.setCorrectTimes(record.getCorrectTimes() + 1);
        } else {
            // 复习错误：降级
            int[] downgrade = DOWNGRADE_RULES.getOrDefault(currentLevel, new int[]{0, 0, 12});
            record.setMemLevel(downgrade[0]);

            if (downgrade[2] > 0) {
                record.setNextReviewTime(DateUtils.addHours(now, downgrade[2]));
            } else {
                record.setNextReviewTime(DateUtils.addDays(now, downgrade[1]));
            }
            record.setErrorTimes(record.getErrorTimes() + 1);
        }

        record.setLastReviewTime(now);
        record.setReviewTimes(record.getReviewTimes() + 1);
        record.setUpdateTime(now);

        return this.updateById(record);
    }

    @Override
    public List<WordCardVO> getReviewWordList(Long userId, String wordType, int limit) {
        if (userId == null || StringUtils.isBlank(wordType) || limit <= 0) {
            return Collections.emptyList();
        }

        // 查询待复习的单词记录
        QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("wordType", wordType);
        queryWrapper.le("nextReviewTime", new Date());
        queryWrapper.lt("memLevel", 5); // 未完全掌握
        queryWrapper.orderByAsc("nextReviewTime");
        queryWrapper.last("LIMIT " + limit);

        List<Userwordrecord> records = this.list(queryWrapper);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取单词详细信息
        Set<Integer> wordIds = records.stream()
                .map(Userwordrecord::getWordId)
                .collect(Collectors.toSet());

        QueryWrapper<Engdict> engdictWrapper = new QueryWrapper<>();
        engdictWrapper.in("id", wordIds);
        engdictWrapper.eq("tag", wordType); // 必须带上分片键，精确匹配
        List<Engdict> words = engdictMapper.selectList(engdictWrapper);

        Map<Integer, Engdict> wordMap = words.stream()
                .collect(Collectors.toMap(Engdict::getId, w -> w));

        // 组装WordCardVO
        return records.stream()
                .map(record -> {
                    Engdict word = wordMap.get(record.getWordId());
                    if (word == null) {
                        return null;
                    }
                    return convertToWordCardVO(word, wordType, record);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Integer countTodayReviewPending(Long userId, String wordType) {
        if (userId == null || StringUtils.isBlank(wordType)) {
            return 0;
        }
        QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("wordType", wordType);
        queryWrapper.le("nextReviewTime", new Date());
        queryWrapper.lt("memLevel", 5);
        return (int) this.count(queryWrapper);
    }

    @Override
    public Integer countTodayNewWords(Long userId, String wordType) {
        if (userId == null || StringUtils.isBlank(wordType)) {
            return 0;
        }
        // 获取今天开始和结束时间
        Date todayStart = DateUtils.truncate(new Date(), Calendar.DATE);
        Date todayEnd = DateUtils.addDays(todayStart, 1);

        QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("wordType", wordType);
        queryWrapper.isNotNull("learnTime"); // 确保是真正学习的记录
        queryWrapper.ge("learnTime", todayStart);
        queryWrapper.lt("learnTime", todayEnd);
        return (int) this.count(queryWrapper);
    }

    @Override
    public Integer countTodayReviewedWords(Long userId, String wordType) {
        if (userId == null || StringUtils.isBlank(wordType)) {
            return 0;
        }
        Date todayStart = DateUtils.truncate(new Date(), Calendar.DATE);
        Date todayEnd = DateUtils.addDays(todayStart, 1);

        QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("wordType", wordType);
        queryWrapper.ge("lastReviewTime", todayStart);
        queryWrapper.lt("lastReviewTime", todayEnd);
        queryWrapper.gt("reviewTimes", 0); // 至少复习过一次
        return (int) this.count(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCollectStatus(Long userId, WordCollectRequest request) {
        if (userId == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Integer isCollect = request.getIsCollect();
        if (isCollect == null || (isCollect != 0 && isCollect != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收藏状态不合法");
        }

        Userwordrecord record = null;

        // 情兵1：如果提供了recordId，直接更新
        if (request.getRecordId() != null) {
            record = this.getById(request.getRecordId());
            if (record == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学习记录不存在");
            }
            if (!record.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此记录");
            }
        }
        // 情兵2：如果提供了wordId和wordType，查找或创建记录
        else if (request.getWordId() != null && StringUtils.isNotBlank(request.getWordType())) {
            // 先查找是否已有记录
            QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId);
            queryWrapper.eq("wordId", request.getWordId());
            queryWrapper.eq("wordType", request.getWordType());
            record = this.getOne(queryWrapper);

            // 如果没有记录，创建一个新的学习记录
            if (record == null) {
                Date now = new Date();
                record = new Userwordrecord();
                record.setUserId(userId);
                record.setWordId(request.getWordId());
                record.setWordType(request.getWordType());
                record.setMemLevel(0); // 未学习状态
                record.setLearnTime(null); // 还未学习
                record.setNextReviewTime(null);
                record.setReviewTimes(0);
                record.setCorrectTimes(0);
                record.setErrorTimes(0);
                record.setIsCollect(isCollect);
                record.setIsMastered(0);
                record.setCreateTime(now);
                record.setUpdateTime(now);
                this.save(record);
                return true;
            }
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请提供recordId或者wordId+wordType");
        }

        // 更新收藏状态
        record.setIsCollect(isCollect);
        record.setUpdateTime(new Date());
        return this.updateById(record);
    }

    @Override
    public LearnProgressVO getLearnProgress(Long userId, String wordType) {
        if (userId == null || StringUtils.isBlank(wordType)) {
            return null;
        }

        LearnProgressVO vo = new LearnProgressVO();
        vo.setWordType(wordType);

        // 获取词书名称
        WordTypeEnum wordTypeEnum = WordTypeEnum.getEnumByType(wordType);
        if (wordTypeEnum != null) {
            vo.setWordTypeName(wordTypeEnum.getName());
        }

        // 获取学习计划
        Userlearnplan plan = userlearnplanService.getCurrentPlan(userId);
        if (plan != null && wordType.equals(plan.getWordType())) {
            vo.setTodayNewTarget(plan.getDailyNewCount());
            vo.setPlanStatus(plan.getPlanStatus());
        } else {
            vo.setTodayNewTarget(50); // 默认目标
            vo.setPlanStatus(0);
        }

        // 今日进度
        vo.setTodayNewCount(countTodayNewWords(userId, wordType));
        vo.setTodayReviewCount(countTodayReviewedWords(userId, wordType));
        vo.setTodayReviewPending(countTodayReviewPending(userId, wordType));

        // 词书总进度
        vo.setLearnedCount(countLearnedWords(userId, wordType).intValue());
        vo.setTotalWordCount(engdictService.countByWordType(wordType));
        vo.setMasteredCount(countMasteredWords(userId, wordType));
        vo.setCollectedCount(countCollectedWords(userId, wordType));

        // 计算进度百分比
        if (vo.getTotalWordCount() > 0) {
            double percent = (vo.getLearnedCount() * 100.0) / vo.getTotalWordCount();
            vo.setProgressPercent(Math.round(percent * 100.0) / 100.0);
        } else {
            vo.setProgressPercent(0.0);
        }

        // 计算预计完成天数
        if (vo.getTodayNewTarget() > 0 && vo.getTotalWordCount() > vo.getLearnedCount()) {
            int remaining = (int) (vo.getTotalWordCount() - vo.getLearnedCount());
            vo.setEstimatedDays((int) Math.ceil((double) remaining / vo.getTodayNewTarget()));
        } else {
            vo.setEstimatedDays(0);
        }

        return vo;
    }

    @Override
    public Page<WordRecordVO> getCollectedWords(Long userId, String wordType, int pageNo, int pageSize) {
        if (userId == null) {
            return new Page<>();
        }

        Page<Userwordrecord> page = new Page<>(pageNo, pageSize);
        QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isCollect", 1);
        if (StringUtils.isNotBlank(wordType)) {
            queryWrapper.eq("wordType", wordType);
        }
        queryWrapper.orderByDesc("updateTime");

        Page<Userwordrecord> recordPage = this.page(page, queryWrapper);

        // 转换为WordRecordVO
        Page<WordRecordVO> resultPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        if (recordPage.getRecords().isEmpty()) {
            resultPage.setRecords(Collections.emptyList());
            return resultPage;
        }


        // 批量查询所有单词信息
        Set<Integer> wordIds = recordPage.getRecords().stream()
                .map(Userwordrecord::getWordId)
                .collect(Collectors.toSet());
        
        QueryWrapper<Engdict> engdictWrapper = new QueryWrapper<>();
        engdictWrapper.in("id", wordIds);
        List<Engdict> words = engdictMapper.selectList(engdictWrapper);
        
        Map<Integer, Engdict> wordMap = words.stream()
                .collect(Collectors.toMap(Engdict::getId, w -> w));

        List<WordRecordVO> voList = recordPage.getRecords().stream()
                .map(record -> {
                    Engdict word = wordMap.get(record.getWordId());
                    return convertToWordRecordVO(record, word);
                })
                .collect(Collectors.toList());

        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public Integer countMasteredWords(Long userId, String wordType) {
        if (userId == null || StringUtils.isBlank(wordType)) {
            return 0;
        }
        QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("wordType", wordType);
        queryWrapper.eq("memLevel", 5);
        queryWrapper.isNotNull("learnTime"); // 确保是真正学习过的单词
        return (int) this.count(queryWrapper);
    }

    @Override
    public Integer countCollectedWords(Long userId, String wordType) {
        if (userId == null || StringUtils.isBlank(wordType)) {
            return 0;
        }
        QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("wordType", wordType);
        queryWrapper.eq("isCollect", 1);
        return (int) this.count(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer clearCollectedWords(Long userId, String wordType) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 构建查询条件
        QueryWrapper<Userwordrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isCollect", 1);
        if (StringUtils.isNotBlank(wordType)) {
            queryWrapper.eq("wordType", wordType);
        }

        // 先统计数量
        int count = (int) this.count(queryWrapper);
        if (count == 0) {
            return 0;
        }

        // 批量更新收藏状态为0
        Userwordrecord updateRecord = new Userwordrecord();
        updateRecord.setIsCollect(0);
        updateRecord.setUpdateTime(new Date());
        this.update(updateRecord, queryWrapper);

        return count;
    }

    /**
     * 将Engdict和学习记录转换为WordCardVO
     */
    private WordCardVO convertToWordCardVO(Engdict word, String wordType, Userwordrecord record) {
        WordCardVO vo = new WordCardVO();
        vo.setWordId(word.getId());
        vo.setWord(word.getWord());
        vo.setPhonetic(word.getPhonetic());
        vo.setDefinition(word.getDefinition());
        vo.setTranslation(word.getTranslation());
        vo.setPos(word.getPos());
        vo.setCollins(word.getCollins());
        vo.setOxford(word.getOxford());
        vo.setExchange(word.getExchange());
        vo.setExchangeInfo(WordExchangeUtil.parseExchange(word.getExchange()));
        vo.setAudio(word.getAudio());
        vo.setWordType(wordType);

        if (record != null) {
            vo.setRecordId(record.getId());
            vo.setMemLevel(record.getMemLevel());
            vo.setIsCollect(record.getIsCollect());
            vo.setReviewTimes(record.getReviewTimes());
        } else {
            // 没有记录时，设置默认值
            vo.setIsCollect(0);
        }

        return vo;
    }

    /**
     * 将学习记录转换为WordRecordVO
     */
    private WordRecordVO convertToWordRecordVO(Userwordrecord record, Engdict word) {
        WordRecordVO vo = new WordRecordVO();
        vo.setId(record.getId());
        vo.setWordId(record.getWordId());
        vo.setWordType(record.getWordType());
        vo.setMemLevel(record.getMemLevel());
        vo.setLearnTime(record.getLearnTime());
        vo.setNextReviewTime(record.getNextReviewTime());
        vo.setReviewTimes(record.getReviewTimes());
        vo.setIsCollect(record.getIsCollect());
        vo.setIsMastered(record.getIsMastered());

        if (word != null) {
            vo.setWord(word.getWord());
            vo.setPhonetic(word.getPhonetic());
            vo.setTranslation(word.getTranslation());
            vo.setAudio(word.getAudio());
        }

        return vo;
    }
}
