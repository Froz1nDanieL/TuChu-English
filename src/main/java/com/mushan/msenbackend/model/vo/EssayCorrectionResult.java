package com.mushan.msenbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 作文批改结果VO
 */
@Data
public class EssayCorrectionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 概括性评语
     */
    private String summaryComment;

    /**
     * 四维度评分
     */
    private DimensionScores dimensionScores;

    /**
     * 错词列表
     */
    private List<ErrorWord> errorWords;

    /**
     * 语法错误列表
     */
    private List<GrammarError> grammarErrors;

    /**
     * 推荐词列表
     */
    private List<RecommendedWord> recommendedWords;

    /**
     * 四维度评分
     */
    @Data
    public static class DimensionScores implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 内容切题评分（满分25分）
         */
        private Integer contentRelevance;

        /**
         * 逻辑结构评分（满分25分）
         */
        private Integer logicalStructure;

        /**
         * 语法质量评分（满分25分）
         */
        private Integer grammarQuality;

        /**
         * 词汇水平评分（满分25分）
         */
        private Integer vocabularyLevel;

        /**
         * 总分（满分100分）
         */
        private Integer totalScore;
    }

    /**
     * 错词
     */
    @Data
    public static class ErrorWord implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 原词（错误拼写）
         */
        private String originalWord;

        /**
         * 正确拼写
         */
        private String correctWord;

        /**
         * 所在句子上下文
         */
        private String context;

        /**
         * 修改建议说明
         */
        private String suggestion;
    }

    /**
     * 语法错误
     */
    @Data
    public static class GrammarError implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 原句（错误句子）
         */
        private String originalSentence;

        /**
         * 修正后的句子
         */
        private String correctedSentence;

        /**
         * 错误类型（时态错误/主谓一致/冠词错误/介词错误/从句错误等）
         */
        private String errorType;

        /**
         * 错误位置
         */
        private String errorPosition;
    }

    /**
     * 推荐词
     */
    @Data
    public static class RecommendedWord implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 原词（用户使用的词）
         */
        private String originalWord;

        /**
         * 推荐词（更高级/地道的表达）
         */
        private String recommendedWord;

        /**
         * 所在句子上下文
         */
        private String context;

        /**
         * 例句
         */
        private String example;
    }
}
