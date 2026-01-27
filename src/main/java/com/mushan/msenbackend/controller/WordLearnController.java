package com.mushan.msenbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.msenbackend.common.BaseResponse;
import com.mushan.msenbackend.common.ResultUtils;
import com.mushan.msenbackend.exception.BusinessException;
import com.mushan.msenbackend.exception.ErrorCode;
import com.mushan.msenbackend.exception.ThrowUtils;
import com.mushan.msenbackend.model.dto.wordrecord.BatchWordLearnRequest;
import com.mushan.msenbackend.model.dto.wordrecord.BatchWordReviewRequest;
import com.mushan.msenbackend.model.dto.wordrecord.WordCollectRequest;
import com.mushan.msenbackend.model.dto.wordrecord.WordLearnRequest;
import com.mushan.msenbackend.model.dto.wordrecord.WordReviewRequest;
import com.mushan.msenbackend.model.entity.User;
import com.mushan.msenbackend.model.entity.Userlearnplan;
import com.mushan.msenbackend.model.enums.WordTypeEnum;
import com.mushan.msenbackend.model.vo.LearnProgressVO;
import com.mushan.msenbackend.model.vo.WordBookVO;
import com.mushan.msenbackend.model.vo.WordCardVO;
import com.mushan.msenbackend.model.vo.WordRecordVO;
import com.mushan.msenbackend.service.EngdictService;
import com.mushan.msenbackend.service.UserService;
import com.mushan.msenbackend.service.UserlearnplanService;
import com.mushan.msenbackend.service.UserwordrecordService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

/**
 * 背单词核心接口
 */
@RestController
@RequestMapping("/word-learn")
@Slf4j
public class WordLearnController {

    @Resource
    private EngdictService engdictService;

    @Resource
    private UserwordrecordService userwordrecordService;

    @Resource
    private UserlearnplanService userlearnplanService;

    @Resource
    private UserService userService;

    // ==================== 词书相关接口 ====================

    /**
     * 获取所有词书列表
     */
    @GetMapping("/word-books")
    public BaseResponse<List<WordBookVO>> getWordBookList(HttpServletRequest httpRequest) {
        Long userId = null;
        try {
            User loginUser = userService.getLoginUser(httpRequest);
            userId = loginUser.getId();
        } catch (Exception e) {
            // 未登录也可以查看词书列表
        }
        List<WordBookVO> wordBookList = engdictService.getWordBookList(userId);
        return ResultUtils.success(wordBookList);
    }

    /**
     * 预览词书单词（随机展示样例）
     */
    @GetMapping("/preview/{wordType}")
    public BaseResponse<List<WordCardVO>> previewWordBook(@PathVariable String wordType,
                                                          @RequestParam(defaultValue = "10") int limit) {
        ThrowUtils.throwIf(!WordTypeEnum.isValidType(wordType), ErrorCode.PARAMS_ERROR, "词书类型不合法");
        ThrowUtils.throwIf(limit > 20, ErrorCode.PARAMS_ERROR, "预览数量不能超过20");
        List<WordCardVO> previewList = engdictService.previewWordBook(wordType, limit);
        return ResultUtils.success(previewList);
    }

    // ==================== 新词学习接口 ====================

    /**
     * 获取今日待学新词列表
     */
    @GetMapping("/new-words")
    public BaseResponse<List<WordCardVO>> getNewWordList(@RequestParam(required = false) String wordType,
                                                         @RequestParam(required = false) Integer limit,
                                                         HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);

        // 如果未指定词书类型，使用当前学习计划的词书
        if (StringUtils.isBlank(wordType)) {
            Userlearnplan plan = userlearnplanService.getCurrentPlan(loginUser.getId());
            if (plan == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "请先选择词书创建学习计划");
            }
            wordType = plan.getWordType();
            if (limit == null) {
                limit = plan.getDailyNewCount();
            }
        }

        // 默认每日新词量
        if (limit == null) {
            limit = 50;
        }

        ThrowUtils.throwIf(limit > 100, ErrorCode.PARAMS_ERROR, "单次获取新词数量不能超过100");

        List<WordCardVO> newWordList = engdictService.getNewWordList(loginUser.getId(), wordType, limit);
        return ResultUtils.success(newWordList);
    }

    /**
     * 提交单个单词学习记录
     */
    @PostMapping("/learn")
    public BaseResponse<Long> submitLearnRecord(@RequestBody WordLearnRequest request,
                                                HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        Long recordId = userwordrecordService.saveLearnRecord(loginUser.getId(), request);
        return ResultUtils.success(recordId);
    }

    /**
     * 批量提交单词学习记录
     */
    @PostMapping("/learn/batch")
    public BaseResponse<List<Long>> batchSubmitLearnRecord(@RequestBody BatchWordLearnRequest request,
                                                           HttpServletRequest httpRequest) {
        if (request == null || request.getLearnRecords() == null || request.getLearnRecords().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ThrowUtils.throwIf(request.getLearnRecords().size() > 100, ErrorCode.PARAMS_ERROR, "单次提交不能超过100个单词");

        User loginUser = userService.getLoginUser(httpRequest);
        List<Long> recordIds = new ArrayList<>();

        for (WordLearnRequest learnRequest : request.getLearnRecords()) {
            // 如果单个请求未指定wordType，使用批量请求的wordType
            if (StringUtils.isBlank(learnRequest.getWordType())) {
                learnRequest.setWordType(request.getWordType());
            }
            Long recordId = userwordrecordService.saveLearnRecord(loginUser.getId(), learnRequest);
            recordIds.add(recordId);
        }

        return ResultUtils.success(recordIds);
    }

    // ==================== 智能复习接口 ====================

    /**
     * 获取今日待复习单词列表
     */
    @GetMapping("/review-words")
    public BaseResponse<List<WordCardVO>> getReviewWordList(@RequestParam(required = false) String wordType,
                                                            @RequestParam(defaultValue = "50") int limit,
                                                            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);

        // 如果未指定词书类型，使用当前学习计划的词书
        if (StringUtils.isBlank(wordType)) {
            Userlearnplan plan = userlearnplanService.getCurrentPlan(loginUser.getId());
            if (plan == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "请先选择词书创建学习计划");
            }
            wordType = plan.getWordType();
        }

        ThrowUtils.throwIf(limit > 100, ErrorCode.PARAMS_ERROR, "单次获取复习单词数量不能超过100");

        List<WordCardVO> reviewWordList = userwordrecordService.getReviewWordList(loginUser.getId(), wordType, limit);
        return ResultUtils.success(reviewWordList);
    }

    /**
     * 提交单个单词复习结果
     */
    @PostMapping("/review")
    public BaseResponse<Boolean> submitReviewRecord(@RequestBody WordReviewRequest request,
                                                    HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        Boolean result = userwordrecordService.updateReviewRecord(loginUser.getId(), request);
        return ResultUtils.success(result);
    }

    /**
     * 批量提交单词复习结果
     */
    @PostMapping("/review/batch")
    public BaseResponse<Boolean> batchSubmitReviewRecord(@RequestBody BatchWordReviewRequest request,
                                                         HttpServletRequest httpRequest) {
        if (request == null || request.getReviewRecords() == null || request.getReviewRecords().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ThrowUtils.throwIf(request.getReviewRecords().size() > 100, ErrorCode.PARAMS_ERROR, "单次提交不能超过100个单词");

        User loginUser = userService.getLoginUser(httpRequest);

        for (WordReviewRequest reviewRequest : request.getReviewRecords()) {
            userwordrecordService.updateReviewRecord(loginUser.getId(), reviewRequest);
        }

        return ResultUtils.success(true);
    }

    // ==================== 进度与统计接口 ====================

    /**
     * 获取学习进度概览
     */
    @GetMapping("/progress")
    public BaseResponse<LearnProgressVO> getLearnProgress(@RequestParam(required = false) String wordType,
                                                          HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);

        // 如果未指定词书类型，使用当前学习计划的词书
        if (StringUtils.isBlank(wordType)) {
            Userlearnplan plan = userlearnplanService.getCurrentPlan(loginUser.getId());
            if (plan == null) {
                return ResultUtils.success(null);
            }
            wordType = plan.getWordType();
        }

        LearnProgressVO progress = userwordrecordService.getLearnProgress(loginUser.getId(), wordType);
        return ResultUtils.success(progress);
    }

    // ==================== 收藏相关接口 ====================

    /**
     * 收藏/取消收藏单词
     */
    @PostMapping("/collect")
    public BaseResponse<Boolean> toggleCollect(@RequestBody WordCollectRequest request,
                                               HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        Boolean result = userwordrecordService.updateCollectStatus(loginUser.getId(), request);
        return ResultUtils.success(result);
    }

    /**
     * 获取收藏列表
     */
    @GetMapping("/collected")
    public BaseResponse<Page<WordRecordVO>> getCollectedWords(@RequestParam(required = false) String wordType,
                                                              @RequestParam(defaultValue = "1") int pageNo,
                                                              @RequestParam(defaultValue = "20") int pageSize,
                                                              HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "每页数量不能超过50");

        Page<WordRecordVO> collectedWords = userwordrecordService.getCollectedWords(
                loginUser.getId(), wordType, pageNo, pageSize);
        return ResultUtils.success(collectedWords);
    }

    /**
     * 清空生词本
     */
    @DeleteMapping("/collected/clear")
    public BaseResponse<Integer> clearCollectedWords(@RequestParam(required = false) String wordType,
                                                      HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Integer count = userwordrecordService.clearCollectedWords(loginUser.getId(), wordType);
        return ResultUtils.success(count);
    }

    // ==================== 单词发音接口 ====================

    /**
     * 获取单词发音
     * 直接返回有道词典的音频流
     * 
     * @param word 单词
     * @param type 发音类型：1-英音(uk)，2-美音(us)，默认为1
     * @param response HTTP响应
     */
    @GetMapping("/pronunciation")
    public void getWordPronunciation(@RequestParam String word,
                                     @RequestParam(defaultValue = "1") Integer type,
                                     HttpServletResponse response) {
        try {
            // 参数校验
            ThrowUtils.throwIf(StringUtils.isBlank(word), ErrorCode.PARAMS_ERROR, "单词不能为空");
            ThrowUtils.throwIf(type != 1 && type != 2, ErrorCode.PARAMS_ERROR, "发音类型只能是1(英音)或2(美音)");
            
            // 构建有道词典发音URL
            String encodedWord = URLEncoder.encode(word.trim(), StandardCharsets.UTF_8);
            String youdaoUrl = String.format("https://dict.youdao.com/dictvoice?audio=%s&type=%d", 
                                            encodedWord, type);
            
            log.info("获取单词发音: word={}, type={}, url={}", word, type, youdaoUrl);
            
            // 创建HTTP连接
            URL url = new URL(youdaoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.error("获取发音失败: word={}, responseCode={}", word, responseCode);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取发音失败");
            }
            
            // 设置响应头
            response.setContentType("audio/mpeg");
            response.setHeader("Content-Disposition", 
                             String.format("inline; filename=\"%s_%s.mp3\"", word, type == 1 ? "uk" : "us"));
            response.setHeader("Cache-Control", "public, max-age=86400"); // 缓存1天
            
            // 读取音频流并写入响应
            try (InputStream inputStream = connection.getInputStream();
                 OutputStream outputStream = response.getOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            
            log.info("单词发音获取成功: word={}, type={}", word, type);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取单词发音异常: word={}, type={}", word, type, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取发音失败: " + e.getMessage());
        }
    }
}
