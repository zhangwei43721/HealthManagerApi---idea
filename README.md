# HealthManagerApi

## 项目简介
HealthManagerApi 是一个基于 Spring Boot 的健康管理系统后端，支持用户信息管理、运动健康数据管理、AI 智能接口等功能。适用于健康管理类 Web/移动应用的数据服务。

## 主要功能
- 用户注册、登录、信息管理
- 运动健康数据记录与查询
- 运动知识与建议查询
- AI 智能接口（如 OpenAI 集成）
- 权限与角色管理
- 菜单与权限分配
- 体征信息及备注管理

## 技术栈
- Spring Boot 2.7.5
- MyBatis-Plus
- Spring Data JPA
- MySQL
- Fastjson2
- JWT（用户认证）
- Lombok
- OpenAI/DeepSeek（AI接口）

## 快速启动

1. **克隆项目**
   ```bash
   git clone <your-repo-url>
   ```

2. **数据库配置**
   - 修改 `src/main/resources/application.yml`，配置你的 MySQL 数据库地址、用户名和密码。

3. **AI Key 配置（可选）**
   - 在 `application.yml` 中配置 `ai.deepseek.api-key`。

4. **构建并运行**
   ```bash
   mvn clean package
   java -jar target/HealthManagerApi-0.0.1-SNAPSHOT.jar
   ```
   或直接在 IDE 中运行 `HealthManagerApiApplication`。

5. **访问接口**
   - 默认端口：`http://localhost:9401/`

## 目录结构

```
HealthManagerApi
├── src
│   └── main
│       ├── java
│       │   └── com.rabbiter.healthsys
│       │        ├── controller      # 控制器，接口入口
│       │        ├── service         # 业务接口
│       │        ├── service.impl    # 业务实现
│       │        ├── entity          # 实体类
│       │        ├── mapper          # MyBatis 映射
│       │        └── config/common   # 配置与通用类
│       └── resources
│            ├── application.yml    # 配置文件
│            └── mapper             # MyBatis XML
├── pom.xml                         # Maven 配置
└── api.md                          # 接口文档
```

## 配置说明

- `server.port`：服务端口，默认 9401
- `spring.datasource`：MySQL 数据库连接
- `logging.level.com.rabbiter`：日志级别
- `mybatis-plus`：MyBatis-Plus 配置
- `ai.deepseek.api-key`：AI 接口 Key

## 贡献指南

欢迎提交 Issue 和 PR！如需贡献代码，请遵循以下流程：
1. Fork 本仓库
2. 新建分支：`git checkout -b feature/xxx`
3. 提交修改：`git commit -m 'feat: 新功能说明'`
4. 推送分支：`git push origin feature/xxx`
5. 提交 Pull Request

---
