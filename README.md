# 学生管理系统

基于 Spring Boot + Vue.js 的学生管理系统，集成用户日志分析与自动化运维功能。

## 项目简介

本系统是一个功能完整的学生信息管理平台，提供学生信息的增删改查、课程管理、成绩管理、用户日志分析以及自动化运维脚本等功能。

### 核心功能

- **学生管理**：学生信息的增删改查、批量导入导出
- **课程管理**：课程信息维护、选课管理
- **成绩管理**：成绩录入、统计分析、报表生成
- **用户日志分析**：操作日志记录、行为分析、可视化报表
- **自动化运维**：一键部署、数据库备份恢复、健康检查、日志轮转

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | JDK 21 (Eclipse Temurin) |
| Spring Boot | 3.2.x | 应用框架 |
| Spring Security | 6.x | 安全认证与授权 |
| Spring Data JPA | 3.x | 数据持久化 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.x | 缓存与会话管理 |
| Maven | 3.9+ | 构建工具 |
| JWT | - | Token 认证 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.x | 前端框架 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| Axios | 1.x | HTTP 客户端 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| ECharts | 5.x | 数据可视化 |

### 运维

| 技术 | 说明 |
|------|------|
| Docker | 容器化部署 |
| Docker Compose | 多容器编排 |
| Nginx | 反向代理与静态文件服务 |
| Shell Scripts | 自动化运维脚本 |

## 项目结构

```
student_ManagementSystem/
├── backend/                    # 后端 Spring Boot 项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/          # Java 源代码
│   │   │   └── resources/     # 配置文件
│   │   └── test/              # 测试代码
│   └── pom.xml                # Maven 配置
├── frontend/                   # 前端 Vue.js 项目
│   ├── src/
│   │   ├── api/               # API 接口
│   │   ├── assets/            # 静态资源
│   │   ├── components/        # 公共组件
│   │   ├── layouts/           # 布局组件
│   │   ├── router/            # 路由配置
│   │   ├── stores/            # Pinia 状态管理
│   │   ├── utils/             # 工具函数
│   │   └── views/             # 页面视图
│   ├── package.json
│   └── vite.config.js
├── scripts/                    # 运维脚本
│   ├── deploy.sh              # 全量部署脚本
│   ├── backup.sh              # MySQL 备份脚本
│   ├── restore.sh             # 数据库恢复脚本
│   ├── health-check.sh        # 健康检查脚本
│   ├── start.sh               # 启动脚本
│   ├── stop.sh                # 停止脚本
│   └── log-rotate.sh          # 日志轮转脚本
├── docker/                     # Docker 配置
│   ├── docker-compose.yml     # 容器编排
│   ├── backend.Dockerfile     # 后端镜像构建
│   ├── frontend.Dockerfile    # 前端镜像构建
│   ├── nginx.conf             # Nginx 配置
│   └── .env.example           # 环境变量模板
├── README.md                   # 项目文档
└── .gitignore                  # Git 忽略配置
```

## 快速开始

### 方式一：Docker 一键启动（推荐）

1. **克隆项目**

```bash
git clone <repository-url>
cd student_ManagementSystem
```

2. **配置环境变量**

```bash
cd docker
cp .env.example .env
# 编辑 .env 文件，修改数据库密码、JWT 密钥等
vim .env
```

3. **生成 SSL 证书（可选，用于 HTTPS）**

```bash
mkdir -p ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout ssl/server.key -out ssl/server.crt
```

4. **启动所有服务**

```bash
docker compose up -d
```

5. **查看服务状态**

```bash
docker compose ps
docker compose logs -f
```

6. **访问应用**

- 前端：https://localhost
- 后端 API：http://localhost:8080/api/system/health

### 方式二：手动启动

1. **环境要求**

| 依赖 | 最低版本 |
|------|----------|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 20+ |
| npm | 9+ |
| MySQL | 8.0+ |
| Redis | 7.0+ |

2. **启动 MySQL 和 Redis**

```bash
# 使用 Docker 快速启动
docker run -d --name student-mysql \
  -e MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD} \
  -e MYSQL_DATABASE=sms_nexus \
  -p 13306:3306 \
  mysql:8.0 --character-set-server=utf8mb4

docker run -d --name student-redis \
  -p 6379:6379 \
  redis:7-alpine redis-server --appendonly yes
```

3. **构建并启动后端**

```bash
cd backend
mvn clean package -DskipTests
java -jar target/student-management-1.0.0.jar \
  --spring.profiles.active=dev
```

4. **构建并启动前端**

```bash
cd frontend
npm install
npm run dev
```

5. **访问应用**

- 前端开发服务器：http://localhost:5173
- 后端 API：http://localhost:8080

## 配置说明

### 后端配置

后端配置文件位于 `backend/src/main/resources/`：

| 文件 | 说明 |
|------|------|
| `application.yml` | 公共配置 |
| `application-dev.yml` | 开发环境配置 |
| `application-prod.yml` | 生产环境配置 |

主要配置项：

```yaml
# 数据源
spring.datasource.url: jdbc:mysql://localhost:13306/sms_nexus
spring.datasource.username: root
spring.datasource.password: ${DB_PASSWORD}

# Redis
spring.redis.host: localhost
spring.redis.port: 6379

# JWT
jwt.secret: ${JWT_SECRET}
jwt.expiration: 86400000
```

### 前端配置

前端配置文件位于 `frontend/`：

| 文件 | 说明 |
|------|------|
| `.env.development` | 开发环境变量 |
| `.env.production` | 生产环境变量 |
| `vite.config.js` | Vite 构建配置 |

## API 文档

启动后端服务后，访问以下地址查看 API 文档：

- Swagger UI：http://localhost:8080/swagger-ui.html
- OpenAPI 文档：http://localhost:8080/v3/api-docs

### 主要 API 端点

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/register` | POST | 用户注册 |
| `/api/students` | GET | 获取学生列表 |
| `/api/students/{id}` | GET | 获取学生详情 |
| `/api/students` | POST | 新增学生 |
| `/api/students/{id}` | PUT | 更新学生信息 |
| `/api/students/{id}` | DELETE | 删除学生 |
| `/api/courses` | GET | 获取课程列表 |
| `/api/grades` | GET | 获取成绩列表 |
| `/api/logs` | GET | 获取操作日志 |
| `/api/system/health` | GET | 健康检查 |

## 默认账号密码

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 请在部署时修改 |
| 教师 | teacher | 请在部署时修改 |
| 学生 | student | 请在部署时修改 |

> **注意**：生产环境请务必修改默认密码！

## Shell 脚本使用说明

所有脚本位于 `scripts/` 目录，使用前请赋予执行权限：

```bash
chmod +x scripts/*.sh
```

### deploy.sh - 全量部署

```bash
# 完整部署
./scripts/deploy.sh

# 跳过代码拉取
./scripts/deploy.sh --skip-pull

# 跳过前端构建
./scripts/deploy.sh --skip-frontend

# 自定义部署目录
./scripts/deploy.sh --deploy-dir /opt/my-app
```

### backup.sh - 数据库备份

```bash
# 使用默认配置备份
./scripts/backup.sh --db-pass ${DB_PASSWORD}

# 自定义数据库连接
./scripts/backup.sh \
  --db-host 192.168.1.100 \
  --db-port 13306 \
  --db-name sms_nexus \
  --db-user root \
  --db-pass ${DB_PASSWORD} \
  --backup-dir /data/backups

# 带告警通知
./scripts/backup.sh --db-pass ${DB_PASSWORD} --webhook-url https://hooks.example.com/notify
```

### restore.sh - 数据库恢复

```bash
# 从备份文件恢复
./scripts/restore.sh \
  --backup-file /data/backups/sms_nexus_20240101_120000.sql.gz \
  --db-pass ${DB_PASSWORD}

# 跳过确认提示（自动化场景）
./scripts/restore.sh \
  --backup-file /data/backups/backup.sql.gz \
  --db-pass ${DB_PASSWORD} \
  --force
```

### health-check.sh - 健康检查

```bash
# 基本健康检查
./scripts/health-check.sh

# 带自动重启
./scripts/health-check.sh --auto-restart

# 自定义检查参数
./scripts/health-check.sh \
  --api-url http://localhost:8080/api/system/health \
  --mysql-host localhost \
  --redis-host localhost \
  --webhook-url https://hooks.example.com/alert

# 自定义告警阈值
./scripts/health-check.sh --disk-threshold 85 --memory-threshold 80
```

### start.sh - 启动服务

```bash
# 启动所有服务（使用本地 MySQL/Redis）
./scripts/start.sh

# 使用 Docker 启动 MySQL 和 Redis
./scripts/start.sh --docker-mysql --docker-redis

# 自定义 JVM 参数
./scripts/start.sh --jvm-xms 512m --jvm-xmx 1024m
```

### stop.sh - 停止服务

```bash
# 停止后端和 Nginx
./scripts/stop.sh

# 同时停止 Docker 容器
./scripts/stop.sh --stop-docker

# 自定义优雅停止超时
./scripts/stop.sh --timeout 60
```

### log-rotate.sh - 日志轮转

```bash
# 使用默认配置
./scripts/log-rotate.sh

# 自定义日志目录和保留策略
./scripts/log-rotate.sh \
  --log-dirs /opt/app/logs,/var/log/app \
  --compress-days 3 \
  --delete-days 15

# 预览模式（不实际执行）
./scripts/log-rotate.sh --dry-run
```

## 开发指南

### 后端开发

```bash
cd backend

# 运行测试
mvn test

# 运行单个测试
mvn test -Dtest=StudentServiceTest

# 代码格式化
mvn spotless:apply

# 启动开发服务器（热重载）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 前端开发

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 运行 lint
npm run lint

# 运行测试
npm run test

# 构建生产版本
npm run build
```

### Git 工作流

1. 从 `main` 分支创建功能分支：`feature/xxx`
2. 开发完成后提交 Pull Request
3. Code Review 通过后合并到 `main`
4. 合并后自动触发 CI/CD 流水线

### 提交规范

```
feat: 新功能
fix: 修复 Bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建/工具变更
```

## 常见问题

### 1. 后端启动失败：数据库连接超时

检查 MySQL 是否已启动，以及 `application-dev.yml` 中的数据库连接配置是否正确。

### 2. 前端构建失败：Node Sass 报错

本项目使用 Dart Sass，无需安装 Node Sass。如遇到问题，删除 `node_modules` 后重新安装：

```bash
rm -rf node_modules package-lock.json
npm install
```

### 3. Docker 容器健康检查失败

查看容器日志排查问题：

```bash
docker compose logs backend
docker compose logs mysql
```

### 4. 备份脚本权限不足

确保运行脚本的用户有 MySQL 访问权限和备份目录写入权限。

## License

MIT License
