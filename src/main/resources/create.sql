CREATE TABLE IF NOT EXISTS `engdict` (
  `id` INTEGER NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word` VARCHAR(128) NOT NULL COMMENT '单词名称',
  `phonetic` VARCHAR(64) DEFAULT NULL COMMENT '音标',
  `definition` TEXT COMMENT '英文释义',
  `translation` TEXT COMMENT '中文释义',
  `pos` VARCHAR(16) DEFAULT NULL COMMENT '词性',
  `collins` INTEGER DEFAULT 0 COMMENT '柯林斯星级',
  `oxford` INTEGER DEFAULT 0 COMMENT '是否是牛津三千核心词汇',
  `tag` VARCHAR(64) DEFAULT NULL COMMENT '标签',
  `bnc` INTEGER DEFAULT NULL COMMENT '英国国家语料库词频顺序',
  `frq` INTEGER DEFAULT NULL COMMENT '当代语料库词频顺序',
  `exchange` TEXT COMMENT '时态复数等变换',
  `detail` TEXT COMMENT '扩展信息',
  `audio` TEXT DEFAULT NULL COMMENT '音频URL',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_general_ci COMMENT='英语单词表';

CREATE TABLE `user_learn_plan` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                 `userId` BIGINT NOT NULL COMMENT '用户ID',
                                 `wordType` VARCHAR(32) NOT NULL COMMENT '词书类型（cet4/cet6/zk/gk/ky/ielts/toefl）',
                                 `dailyNewCount` INT NOT NULL DEFAULT 50 COMMENT '每日新词目标量（默认50）',
                                 `currentProgress` INT NOT NULL DEFAULT 0 COMMENT '当前学习进度（已学单词数）',
                                 `planStatus` TINYINT NOT NULL DEFAULT 1 COMMENT '计划状态（0暂停/1启用）',
                                 `remindTime` TIME DEFAULT NULL COMMENT '每日复习提醒时间',
                                 `startDate` DATE DEFAULT NULL COMMENT '计划开始日期',
                                 `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_id` (`userId`) COMMENT '用户唯一计划索引',
                                 KEY `idx_word_type` (`wordType`) COMMENT '词书类型索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户学习计划表';

CREATE TABLE `user_word_record` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
     `userId` BIGINT NOT NULL COMMENT '用户ID',
     `wordId` INT NOT NULL COMMENT '单词ID（对应engdict.id）',
     `wordType` VARCHAR(32) NOT NULL COMMENT '词书类型（便于分表查询）',
    `memLevel` TINYINT NOT NULL DEFAULT 0 COMMENT '记忆等级（0-5）',
    `learnTime` DATETIME DEFAULT NULL COMMENT '首次学习时间',
    `lastReviewTime` DATETIME DEFAULT NULL COMMENT '最近复习时间',
    `nextReviewTime` DATETIME DEFAULT NULL COMMENT '下次复习时间',
    `reviewTimes` INT NOT NULL DEFAULT 0 COMMENT '复习次数',
    `correctTimes` INT NOT NULL DEFAULT 0 COMMENT '正确次数',
    `errorTimes` INT NOT NULL DEFAULT 0 COMMENT '错误次数',
    `isCollect` TINYINT NOT NULL DEFAULT 0 COMMENT '是否收藏（0/1）',
    `isMastered` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已掌握（0/1）',
    `firstKnow` TINYINT DEFAULT NULL COMMENT '第一轮：是否认识（0-不认识，1-认识）',
    `choiceCorrect` TINYINT DEFAULT NULL COMMENT '第二轮：选词测试是否答对（0/1/null）',
    `spellingCorrect` TINYINT DEFAULT NULL COMMENT '第三轮：拼写测试是否正确（0/1/null）',
                                  `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`),
                 UNIQUE KEY `uk_user_word` (`userId`, `wordId`, `wordType`) COMMENT '用户单词唯一索引',
    KEY `idx_user_word_type` (`userId`, `wordType`) COMMENT '用户词书类型索引',
    KEY `idx_user_review` (`userId`, `nextReviewTime`) COMMENT '用户复习时间索引',
    KEY `idx_user_collect` (`userId`, `isCollect`) COMMENT '用户收藏索引',
    KEY `idx_user_level` (`userId`, `memLevel`) COMMENT '用户记忆等级索引',
    KEY `idx_user_error` (`userId`, `errorTimes`) COMMENT '用户错误次数索引'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户单词学习记录表';

CREATE TABLE `article` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文章ID',
    `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
    `content` TEXT NOT NULL COMMENT '文章正文',
    `difficulty` TINYINT NOT NULL DEFAULT 2 COMMENT '难度等级(1-简单 2-中等 3-困难)',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类(科技/文化/新闻等)',
    `wordCount` INT DEFAULT 0 COMMENT '字数统计',
    `readCount` INT DEFAULT 0 COMMENT '阅读次数',
    `source` VARCHAR(200) DEFAULT NULL COMMENT '来源',
    `publishTime` DATETIME DEFAULT NULL COMMENT '发布时间',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_difficulty` (`difficulty`),
    KEY `idx_category` (`category`),
    KEY `idx_publish_time` (`publishTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文章表';

CREATE TABLE `user_article_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `userId` BIGINT NOT NULL COMMENT '用户ID',
    `articleId` BIGINT NOT NULL COMMENT '文章ID',
    `lastReadTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后阅读时间',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_article` (`userId`, `articleId`) COMMENT '用户文章唯一索引',
    KEY `idx_last_read_time` (`lastReadTime`) COMMENT '最后阅读时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户阅读记录表';

-- auto-generated definition
create table user
(
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userEmail    varchar(256)                           null comment '邮箱',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    constraint uk_userAccount
        unique (userAccount)
)
    comment '用户';

-- 为用户表添加邮箱唯一索引（确保一个邮箱只能注册一个账号）
ALTER TABLE `user` ADD UNIQUE KEY `uk_email` (`userEmail`);

-- 为用户表添加邮箱普通索引（优化邮箱查询性能）
ALTER TABLE `user` ADD KEY `idx_email` (`userEmail`);

-- 作文题目表
CREATE TABLE `writing_topic` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '作文题目标题（简短概括）',
    `description` TEXT NOT NULL COMMENT '作文题目描述/写作要求（详细内容）',
    `examType` VARCHAR(32) NOT NULL COMMENT '考试类型（cet4/cet6/ielts/toefl/gk/zk/ky等）',
    `wordLimit` INT DEFAULT NULL COMMENT '字数要求',
    `timeLimit` INT DEFAULT NULL COMMENT '时间限制（分钟）',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_exam_type` (`examType`) COMMENT '考试类型索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='作文题目表';

-- 用户作文记录表
CREATE TABLE `user_writing_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `userId` BIGINT NOT NULL COMMENT '用户 ID',
    `topicId` BIGINT NOT NULL COMMENT '作文题目 ID（对应 writing_topic.id）',
    `content` TEXT NOT NULL COMMENT '用户作文原文',
    `correctStatus` TINYINT NOT NULL DEFAULT 0 COMMENT '批改状态（0-待批改 1-批改中 2-已完成 3-批改失败）',
    `score` DECIMAL(5,2) DEFAULT NULL COMMENT '得分',
    `fullScore` DECIMAL(5,2) DEFAULT NULL COMMENT '满分',
    `comment` TEXT COMMENT '评语',
    `errorWords` TEXT COMMENT '错词列表（JSON 格式）',
    `recommendedWords` TEXT COMMENT '推荐词列表（JSON 格式）',
    `correctTime` DATETIME DEFAULT NULL COMMENT '批改时间',
    `wordCount` INT DEFAULT 0 COMMENT '作文字数统计',
    `isDeleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0/1）',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`userId`) COMMENT '用户 ID 索引',
    KEY `idx_topic_id` (`topicId`) COMMENT '题目 ID 索引',
    KEY `idx_correct_status` (`correctStatus`),
    KEY `idx_create_time` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='作文题目表';
