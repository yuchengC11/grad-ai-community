<<<<<<< HEAD
# 🎓 Grad AI Community 毕业生 AI 互助社区

一个面向毕业生的 AI 互助社区，前后端分离。用户可以在社区中发布求职、考研、考公等经验帖子，进行评论、点赞、收藏与私信交流；同时内置接入大模型的 **AI 智能助手**，支持多角色对话、帖子摘要与 AI 评论，帮助毕业生高效获取就业、升学、考公等实用建议。

## ✨ 功能特性
=======
# Grad AI Community 毕业生 AI 互助社区

一个面向毕业生的 AI 互助社区，前后端分离。用户可以在社区中发布求职、考研、考公等经验帖子，进行评论、点赞、收藏与私信交流；同时内置接入大模型的 **AI 智能助手**，支持多角色对话、帖子摘要与 AI 评论，帮助毕业生高效获取就业、升学、考公等实用建议。

## 功能特性
>>>>>>> 7d66e6598905a706ac9848c0334abe2c4159a34d

### 社区功能
- **用户系统**：注册 / 登录（JWT 鉴权 + BCrypt 密码加密）、个人主页、头像上传、兴趣标签
- **帖子社区**：Markdown 发布帖子、分类浏览（就业求职 / 考研升学 / 考公编制）、帖子详情、浏览量统计
- **互动体系**：评论、点赞、收藏、全文搜索
- **社交功能**：好友关系、私信聊天、系统消息通知
- **我的中心**：我的帖子、我的点赞、我的收藏

### AI 能力（阿里云百炼 DashScope）
<<<<<<< HEAD
- 🤖 **多角色 AI 助手**：内置学长 / 学姐 / 面试官 / 导师等角色，以及就业建议、考研经验、考公备考、求职感悟等专属场景，回答按「核心建议 / 详细分析 / 行动清单 / 注意事项」结构化输出
- ⚡ **流式输出**：基于 SSE 实现打字机式实时回复
- 📄 **AI 帖子摘要**：一键生成帖子 100 字精简摘要
- 💬 **AI 生成评论**：根据帖子内容 + 用户指令生成友好社区评论
- 🛡️ **稳定性保障**：基于 Resilience4j 对 AI 接口做熔断与限流，防止大模型调用雪崩
=======
-  **多角色 AI 助手**：内置学长 / 学姐 / 面试官 / 导师等角色，以及就业建议、考研经验、考公备考、求职感悟等专属场景，回答按「核心建议 / 详细分析 / 行动清单 / 注意事项」结构化输出
-  **流式输出**：基于 SSE 实现打字机式实时回复
-  **AI 帖子摘要**：一键生成帖子 100 字精简摘要
-  **AI 生成评论**：根据帖子内容 + 用户指令生成友好社区评论
-  **稳定性保障**：基于 Resilience4j 对 AI 接口做熔断与限流，防止大模型调用雪崩
>>>>>>> 7d66e6598905a706ac9848c0334abe2c4159a34d

### 工程机制
- 点赞数 / 评论数通过定时任务异步落库同步，避免高并发计数热点
- 统一响应体 `Result` + 全局异常处理
- JWT 拦截器统一鉴权，预留 Redis 实现 Token 黑名单、验证码托管的扩展能力
- 上传文件本地存储并映射为静态资源

<<<<<<< HEAD
## 🛠️ 技术栈
=======
##  技术栈
>>>>>>> 7d66e6598905a706ac9848c0334abe2c4159a34d

| 端 | 技术 |
|---|---|
| 后端 | Java 8 · Spring Boot 2.7.6 · MyBatis-Plus 3.5.3 · MySQL · Redis |
| 安全 | JWT (jjwt) · Spring Security Crypto (BCrypt) |
| AI | 阿里云百炼 DashScope（qwen3.7-plus）· OkHttp · Resilience4j |
| 前端 | Vue 3 · Vite · Element Plus · vue-router · axios · marked + DOMPurify |

## 📁 项目结构

```
grad-ai-community/
├── grad_ai_community/              # 后端（Spring Boot）
│   ├── src/main/java/
│   │   └── com/graduate/graidaicommunity/
│   │       ├── controller/         # 接口层（用户、帖子、评论、点赞、收藏、好友、私信、AI）
│   │       ├── service/            # 业务层（含 AI 服务：DashScope 客户端、AI 帖子服务）
│   │       ├── mapper/             # MyBatis-Plus 数据访问层
│   │       ├── pojo/ dto/ vo/      # 实体与出入参
│   │       ├── config/             # 配置（拦截器、跨域、MyBatis-Plus、异步线程池）
│   │       ├── interceptor/        # JWT 鉴权拦截器
│   │       ├── common/             # 统一响应、全局异常、用户上下文
│   │       └── util/               # JWT / 密码 / Redis 工具
│   ├── src/main/resources/
│   │   └── application.properties  # 配置文件
│   ├── backup.sql                  # 数据库备份（含初始分类数据）
│   └── pom.xml
├── v/grad-ai-front/                # 前端（Vue 3 + Vite）
│   ├── src/
│   │   ├── views/                  # 页面（首页、帖子、聊天、AI 对话、消息、我的…）
│   │   ├── components/             # 组件（含全局悬浮 AI 助手）
│   │   ├── api/                    # 接口封装
│   │   ├── router/                 # 路由与登录守卫
│   │   └── utils/request.js        # axios 封装（Token 注入、统一错误处理）
│   ├── public/                     # 静态资源
│   ├── package.json
│   └── vite.config.js              # 开发代理（/api、/uploads → 8081）
└── .gitignore
```

<<<<<<< HEAD
## 🚀 快速开始
=======
##  快速开始
>>>>>>> 7d66e6598905a706ac9848c0334abe2c4159a34d

### 环境要求
- JDK 1.8+
- Maven 3.6+
- Node.js 16+（建议 18+）
- MySQL 8.0+
- Redis 6.0+

### 1. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS gradaicommunity DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

导入表结构与初始数据：

```bash
mysql -u root -p gradaicommunity < grad_ai_community/backup.sql
```

> 说明：数据库连接串以 `application.properties` 为准（库名 `gradaicommunity`）；`backup.sql` 内含分类、示例帖子等初始化数据。

### 2. 配置环境变量

后端通过环境变量注入敏感信息（不硬编码在配置文件中）：

| 变量 | 说明 |
|---|---|
| `DB_USERNAME` | MySQL 用户名 |
| `DB_PASSWORD` | MySQL 密码 |
| `DASHSCOPE_API_KEY` | 阿里云百炼 API Key（[百炼控制台](https://bailian.console.aliyun.com/)获取） |

### 3. 启动后端（端口 8081）

```bash
cd grad_ai_community
mvn spring-boot:run
```

或打包运行：

```bash
mvn clean package
java -jar target/grad_AI_community-0.0.1-SNAPSHOT.jar
```

### 4. 启动前端（端口 5173）

```bash
cd v/grad-ai-front
npm install
npm run dev
```

浏览器访问 **http://localhost:5173** 即可体验。

## ⚙️ 主要配置项（application.properties）

| 配置 | 默认值 | 说明 |
|---|---|---|
| `server.port` | 8081 | 后端端口 |
| `spring.datasource.url` | jdbc:mysql://localhost:3306/gradaicommunity | MySQL 连接 |
| `spring.redis.host/port` | localhost / 6379 | Redis 地址 |
| `openai.model` | qwen3.7-plus | 大模型名称 |
| `openai.base.url` | 阿里云百炼兼容 OpenAI 接口地址 | 模型网关 |
| `jwt.expire-ms` | 604800000 | Token 有效期（7 天） |
| `upload.local-path` | ./uploads | 上传文件目录 |

## 📄 许可证

本项目仅供学习交流使用。
