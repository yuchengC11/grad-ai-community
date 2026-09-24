-- ============================================================
-- 毕业生 AI 互助社区 建库建表脚本
-- 依据后端 pojo 实体类逐字段生成，字符集 utf8mb4 / InnoDB
-- 用法：先 CREATE DATABASE，再 USE，然后整段执行
-- ============================================================

CREATE DATABASE IF NOT EXISTS gradaicommunity
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE gradaicommunity;

-- ---------------- 用户表（实体 SysUser） ----------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  username     VARCHAR(50)  NOT NULL COMMENT '登录名',
  password     VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
  nickname     VARCHAR(50)           COMMENT '昵称',
  avatar       VARCHAR(255)          COMMENT '头像相对路径',
  school       VARCHAR(100)          COMMENT '学校',
  major        VARCHAR(100)          COMMENT '专业',
  interest_tag INT                   COMMENT '兴趣标签',
  create_time  DATETIME              COMMENT '创建时间',
  update_time  DATETIME              COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------- 帖子表（实体 Post） ----------------
DROP TABLE IF EXISTS sys_post;
CREATE TABLE sys_post (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  title         VARCHAR(200) NOT NULL COMMENT '标题',
  content       LONGTEXT             COMMENT '正文',
  category_id   INT                  COMMENT '分类 id',
  user_id       BIGINT               COMMENT '作者 id',
  view_count    INT         DEFAULT 0 COMMENT '浏览数',
  like_count    INT         DEFAULT 0 COMMENT '点赞数（定时任务刷入）',
  comment_count INT         DEFAULT 0 COMMENT '评论数（定时任务刷入）',
  create_time   DATETIME             COMMENT '创建时间',
  update_time   DATETIME             COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_category_id (category_id),
  KEY idx_user_id (user_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

-- ---------------- 分类表（实体 Category） ----------------
DROP TABLE IF EXISTS category;
CREATE TABLE category (
  id          INT      NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50)         COMMENT '分类名',
  sort        INT                 COMMENT '排序（实体字段 sortOrder）',
  status      INT                 COMMENT '状态',
  create_time DATETIME            COMMENT '创建时间',
  parent_id   BIGINT              COMMENT '父分类 id',
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 初始分类（方便测试发帖，可自行增删）
INSERT INTO category (name, sort, status, create_time) VALUES
('求职经验', 1, 1, NOW()),
('考研经验', 2, 1, NOW()),
('考公经验', 3, 1, NOW()),
('校园生活', 4, 1, NOW()),
('技术分享', 5, 1, NOW());

-- ---------------- 评论表（实体 Comment） ----------------
DROP TABLE IF EXISTS comment;
CREATE TABLE comment (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  post_id     BIGINT          COMMENT '帖子 id',
  user_id     BIGINT          COMMENT '评论人 id',
  content     TEXT            COMMENT '内容',
  create_time DATETIME        COMMENT '创建时间',
  parent_id   BIGINT          COMMENT '父评论 id（楼中楼，暂未使用）',
  PRIMARY KEY (id),
  KEY idx_post_id (post_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ---------------- 点赞明细表（实体 PostLike） ----------------
DROP TABLE IF EXISTS post_like;
CREATE TABLE post_like (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  post_id     BIGINT       COMMENT '帖子 id',
  user_id     BIGINT       COMMENT '点赞用户 id',
  create_time DATETIME     COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_post_user (post_id, user_id) COMMENT '一人一帖一条，防重复点赞',
  KEY idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞明细表';

-- ---------------- 收藏表（实体 Favorite） ----------------
DROP TABLE IF EXISTS sys_favorite;
CREATE TABLE sys_favorite (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  user_id     BIGINT   COMMENT '用户 id',
  post_id     BIGINT   COMMENT '帖子 id',
  create_time DATETIME COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_post (user_id, post_id) COMMENT '防重复收藏'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ---------------- 好友关系表（实体 Friend） ----------------
DROP TABLE IF EXISTS friend;
CREATE TABLE friend (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  user_id     BIGINT   COMMENT '发起人 id',
  friend_id   BIGINT   COMMENT '接收人 id',
  status      INT      COMMENT '0 待接受 / 1 已接受 / 2 已拒绝',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- ---------------- 私信表（实体 PrivateMessage） ----------------
DROP TABLE IF EXISTS private_message;
CREATE TABLE private_message (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  from_user_id BIGINT       COMMENT '发送方 id',
  to_user_id   BIGINT       COMMENT '接收方 id',
  content      VARCHAR(500) COMMENT '内容',
  is_read      INT DEFAULT 0 COMMENT '0 未读 / 1 已读',
  create_time  DATETIME     COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_from_to (from_user_id, to_user_id),
  KEY idx_to_user (to_user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信表';

-- ---------------- 系统消息表（实体 SysMessage） ----------------
DROP TABLE IF EXISTS sys_message;
CREATE TABLE sys_message (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       COMMENT '接收用户 id',
  from_user_id  BIGINT       COMMENT '来源用户 id',
  from_nickname VARCHAR(50)  COMMENT '来源用户昵称',
  content       VARCHAR(500) COMMENT '内容',
  type          INT          COMMENT '消息类型',
  is_read       INT DEFAULT 0 COMMENT '0 未读 / 1 已读',
  create_time   DATETIME     COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息表';
