# Grad-AI-Community 毕业生 AI 互助社区
前后端分离的毕业生社区系统，面向求职、考研、考公群体，支持帖子发布、评论点赞收藏、私信交流；集成阿里云百炼大模型，提供 AI 对话、帖子摘要、AI 评论能力。后端基于 SpringBoot 开发。

## 功能介绍

### 社区基础功能

- 用户模块：注册登录，基于 JWT 鉴权，BCrypt 密码加密；个人主页、头像上传、兴趣标签维护
- 帖子模块：Markdown 帖子发布，帖子分类浏览、详情查看、浏览量统计
- 互动模块：评论、点赞、收藏、帖子全文检索
- 社交模块：好友管理、私信消息、系统通知
- 个人中心：我的帖子、我的点赞、我的收藏

### AI 模型能力（阿里云百炼 DashScope）

- 多角色 AI 对话：内置学长、学姐、面试官、导师等角色，针对求职、考研、考公场景给出结构化建议
- SSE 流式对话：服务端推送，前端实现打字机效果实时返回模型回答
- AI 帖子摘要：自动生成帖子简短摘要
- AI 评论生成：根据帖子内容自动生成社区评论
- 服务稳定性保障：Resilience4j 实现接口熔断、限流，保护大模型接口，防止服务雪崩

### 工程设计

- 点赞、评论计数采用 Redis 缓存 + 定时任务异步落库，减轻数据库高并发压力
- 统一响应封装 Result，全局异常处理器统一捕获处理各类异常
- JWT 拦截器完成登录校验，ThreadLocal 存储当前登录用户上下文
- 文件上传本地存储，通过静态资源映射访问上传资源

## 技术栈

表格

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 8，Spring Boot 2.7.6，MyBatis-Plus 3.5.3，MySQL，Redis |
| 安全 | JWT (jjwt)，BCrypt 密码加密 |
| AI 相关 | 阿里云百炼 DashScope，OkHttp，Resilience4j |
| 前端 | Vue3，Vite，Element Plus，Axios，marked，DOMPurify |

## 项目结构

```
grad-ai-community/
├── grad_ai_community/                # SpringBoot后端项目
│   ├── src/main/java/
│   │   └── com/graduate/graidaicommunity/
│   │       ├── controller/           # 接口层：用户、帖子、评论、点赞、AI等接口
│   │       ├── service/              # 业务层，包含DashScope大模型调用服务
│   │       ├── mapper/               # MyBatis-Plus数据访问层
│   │       ├── pojo/dto/vo/          # 实体类、入参、出参对象
│   │       ├── config/               # 配置类：拦截器、跨域、异步线程池、MyBatis-Plus
│   │       ├── interceptor/          # JWT登录拦截器
│   │       ├── common/               # 统一返回结果、全局异常、用户上下文
│   │       └── util/                 # JWT工具、密码工具、Redis工具
│   ├── src/main/resources/
│   │   └── application.properties    # 项目配置文件
│   ├── backup.sql                    # 数据库初始化脚本
│   └── pom.xml
├── v/grad-ai-front/                  # Vue3前端项目
│   ├── src/
│   │   ├── views/                    # 页面：首页、帖子、AI对话、消息、个人中心
│   │   ├── components/               # 公共组件
│   │   ├── api/                      # 接口请求封装
│   │   ├── router/                   # 路由与登录守卫
│   │   └── utils/request.js          # axios请求封装，Token统一注入
│   ├── public/
│   ├── package.json
│   └── vite.config.js                # 开发代理配置
└── .gitignore
```

## 快速开始

### 环境依赖

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Node.js 16+

### 1. 初始化数据库

```
CREATE DATABASE IF NOT EXISTS gradaicommunity DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

导入项目备份脚本：

```
mysql -u root -p gradaicommunity < grad_ai_community/backup.sql
```

backup.sql 包含帖子分类、基础演示数据。

### 2. 环境变量配置

项目敏感信息通过环境变量注入，不在配置文件硬编码：

表格

| 环境变量 | 说明 |
| --- | --- |
| DB_USERNAME | MySQL 用户名 |
| DB_PASSWORD | MySQL 密码 |
| DASHSCOPE_API_KEY | 阿里云百炼 API Key |

> 
> JWT 密钥、Redis 连接信息支持环境变量覆盖，配置文件仅作为本地开发兜底。

### 3. 启动后端（端口 8081）

```
cd grad_ai_community
mvn spring-boot:run
```

打包部署方式：

```
mvn clean package
java -jar target/grad_AI_community-0.0.1-SNAPSHOT.jar
```

### 4. 启动前端（端口 5173）

```
cd v/grad-ai-front
npm install
npm run dev
```

访问地址：[http://localhost:5173](http://localhost:5173)

## 核心配置说明 application.properties

表格

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| server.port | 8081 | 后端服务端口 |
| spring.datasource.url | jdbc:mysql://[localhost:3306/gradaicommunity](https://localhost:3306/gradaicommunity) | MySQL 连接地址 |
| spring.redis.host / port | [localhost](https://localhost) / 6379 | Redis 服务地址 |
| openai.model | qwen3.7-plus | 大模型名称 |
| jwt.expire-ms | 604800000 | JWT Token 有效期，7 天 |
| upload.local-path | ./uploads | 用户上传文件存储目录 |

## 后续优化方向

1. 当前 JWT 无服务端黑名单，无法实现用户主动踢下线，可基于 Redis 实现 Token 黑名单。
2. 私信消息目前前端轮询拉取，可改造为 WebSocket 实现实时消息推送。
3. 点赞数据定时任务全量同步，高并发场景可引入消息队列实现异步落库。
4. 帖子检索使用 MySQL 模糊查询，数据量大时可接入 Elasticsearch 优化搜索性能。
5. 项目当前单机部署，后续可基于 Docker 容器化，并使用 Nginx 做负载均衡。

## 许可证

本项目仅用于学习交流。
