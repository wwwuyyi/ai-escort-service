# AI 赋能陪诊助医服务系统

面向老年患者及家属的陪诊助医平台，包含家属端、陪诊师端、管理端和 AI 健康助手。系统在业务流程中接入大模型与 OCR 能力，并提供本地 Mock 兜底，保证离线环境下仍可完整演示。

## 一、技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.2、MyBatis-Plus、MySQL 8、JWT、Knife4j |
| 前端 | Vue 3、Vite、Element Plus、Pinia、Vue Router |
| AI | DeepSeek（OpenAI 兼容接口）、本地 Mock Server（OCR + LLM 兜底） |
| 数据 | MySQL 8.0（生产/演示）、H2（集成测试） |

## 二、目录结构

```text
01-源代码/
├── backend/            Spring Boot 后端（pom.xml + src）
│   ├── src/
│   └── upload/         运行时上传文件目录
├── frontend/           Vue 3 前端（发布版 UI）
│   ├── src/
│   └── vite.config.js
├── ai-service/         Mock AI Server（OCR :9001 + LLM :9002）
│   └── server.js
├── sql/                数据库脚本（按 01→05 顺序执行）
├── start.bat           Windows 一键启动
├── README.md
└── .gitignore
```

## 三、环境要求

| 软件 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | `java -version` 验证 |
| Maven | 3.8+ | `mvn -v` 验证 |
| Node.js | 18+ | 前端与 Mock Server 需要 |
| MySQL | 8.0 | 端口 3306，默认账号 root/root |

## 四、快速启动

### 1. 初始化数据库

在 `01-源代码` 目录打开 cmd，依次执行：

```bat
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS escort_db DEFAULT CHARACTER SET utf8mb4;"
mysql -uroot -proot escort_db < sql/01-schema.sql
mysql -uroot -proot escort_db < sql/02-init-data.sql
mysql -uroot -proot escort_db < sql/03-alter-order-request-id.sql
mysql -uroot -proot escort_db < sql/04-add-chat-tables.sql
mysql -uroot -proot escort_db < sql/05-fix-gender-width.sql
```

> 空库按 01→05 全量执行；已有旧库升级至少补跑 `05-fix-gender-width.sql`，否则女性用户数据写入会报 500。

### 2. 配置数据库密码（通常不用改）

默认读取 `root/root`，如需修改，编辑：

```text
backend/src/main/resources/application.yml
```

或设置环境变量：

```bat
setx DB_PASSWORD "你的MySQL密码"
```

### 3. 配置 AI Key（可选）

```bat
setx AI_API_KEY "sk-xxxxxxxxxxxxxxxx"
```

设置后需**新开终端**才生效。未设置 Key 时，系统自动使用本地 Mock，可完成全部 AI 流程演示。

### 4. 一键启动

双击 `start.bat`，脚本依次完成：

1. 检查/启动 MySQL
2. 启动 Mock AI（9001 / 9002）
3. 启动后端（8080）
4. 启动前端（5173）并打开浏览器

### 5. 手动启动（三个终端）

```bat
REM 终端 1：Mock AI
cd ai-service
node server.js

REM 终端 2：后端
cd backend
mvn spring-boot:run

REM 终端 3：前端
cd frontend
npm install     REM 首次运行；已存在 node_modules 可跳过
npm run dev
```

访问 <http://localhost:5173>。

### 6. 纯离线演示（不调用真实 API）

先启动 Mock，再启动后端并覆盖 AI 地址：

```bat
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--ai.llm.base-url=http://localhost:9002/llm --ai.ocr.base-url=http://localhost:9001/ocr --ai.api-key=demo-api-key"
```

## 五、测试账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123` |
| 家属 | `13800001111` | `Test1234` |
| 陪诊师（已审核） | `13800002222` | `Test1234` |
| 陪诊师（待审核） | `13800003333` | `Test1234` |

## 六、端口说明

| 服务 | 端口 |
|---|---|
| 前端 | 5173 |
| 后端 | 8080 |
| Mock OCR | 9001 |
| Mock LLM | 9002 |
| MySQL | 3306 |

## 七、AI 调用与降级策略

- 默认调用 DeepSeek（`deepseek-reasoner`，前端显示可折叠思考过程）。
- 调用失败会按 1s/2s/4s 退避重试；连续失败达到阈值后进入熔断，自动切换到本地 Mock（`http://localhost:9002/llm`）。
- Mock 也不可用时才返回降级提示，不影响非 AI 业务演示。
- 关闭思考过程：设置 `AI_CHAT_MODEL=deepseek-chat` 后重启后端。

## 八、常见问题

| 现象 | 处理 |
|---|---|
| 登录提示 Invalid CORS request | 必须用 `http://localhost:5173` 访问，不要用 `127.0.0.1` |
| 端口被占用 | `netstat -ano \| findstr :8080` 找到 PID 后结束进程，或重启电脑 |
| 新电脑前端起不来 | 先执行 `cd frontend && npm install` |
| AI 一直降级 | 检查 `AI_API_KEY` 是否在新终端生效；或直接使用离线模式 |
| 上传图片 404 | 确认从 `backend` 目录启动后端，上传文件落在 `backend/upload/` |

## 九、迁移到新电脑

1. 拷贝整个 `01-源代码` 目录（或 `git clone` 仓库）。
2. 安装 JDK 17+、Maven、Node.js 18+、MySQL 8.0。
3. 按“快速启动”初始化数据库、按需设置 `AI_API_KEY`。
4. `cd frontend && npm install`（依赖目录不随仓库提交）。
5. 双击 `start.bat`。

> `node_modules/`、`dist/`、`target/`、`.vite-cache/` 均为可再生成产物，不纳入版本管理，也不随源码包提交。

## 十、开发约定

- 后端按 `module/<业务域>/{controller,service,mapper,entity,dto,vo}` 分层。
- 前端接口统一走 `/api`，Vite 代理到 `localhost:8080`。
- 上传文件统一存放 `backend/upload/`，通过 `/upload/**` 访问。
- 数据库脚本只增不改；旧库升级新增独立脚本编号。
