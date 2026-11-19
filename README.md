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
├── sql/                数据库脚本（按 01→07 顺序执行）
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
mysql -uroot -proot escort_db < sql/06-security-token-version-and-request-id-scope.sql
mysql -uroot -proot escort_db < sql/07-add-order-remark.sql
```

> 空库按 01→07 全量执行；已有旧库按 05→06→07 顺序补跑。06 增加 `token_version` 与订单 `(family_id, request_id)` 唯一键，07 确保 `escort_order.remark` 存在；06/07 均可重复执行。

### 2. 配置 JWT_SECRET（必填）

后端启动时强制校验 `JWT_SECRET`：未配置、仍为旧默认值或长度小于 32 字节会拒绝启动。

```bat
set JWT_SECRET=请替换为至少32字节的随机字符串
```

`start.bat` 在未设置时会自动生成本次运行专用的随机密钥（重启后旧 Token 失效），仅适合本机演示。
生产环境必须通过环境变量或密钥管理系统注入，不能使用仓库内或启动脚本生成的临时值。

### 3. 配置数据库（演示默认 root/root）

demo 环境默认读取 `root/root`；生产环境使用 prod profile 并注入非默认口令：

```bat
set SPRING_PROFILES_ACTIVE=prod
set DB_USERNAME=你的最小权限账号
set DB_PASSWORD=你的非默认数据库口令
set AES_KEY=你的AES密钥（至少16字节）
```

> **AES_KEY 迁移注意**：prod 会拒绝 demo 默认 AES 密钥。若从 demo 数据迁移，必须使用加密现有数据时所用的 AES 密钥，或先完成敏感字段重加密，否则身份证号/地址等历史密文无法解密。

### 4. 配置 AI Key（可选）

```bat
setx AI_API_KEY "sk-xxxxxxxxxxxxxxxx"
```

设置后需**新开终端**才生效。未设置 Key 时，系统自动使用本地 Mock，可完成全部 AI 流程演示。

### 5. 一键启动（demo profile）

双击 `start.bat`，脚本依次完成：

1. 检查/启动 MySQL
2. 启动 Mock AI（9001 / 9002）
3. 启动后端（8080）
4. 启动前端（5173）并打开浏览器

### 6. 手动启动（三个终端）

```bat
REM 终端 1：Mock AI
cd ai-service
node server.js

REM 终端 2：后端（demo profile + JWT 密钥）
cd backend
set SPRING_PROFILES_ACTIVE=demo
set JWT_SECRET=请替换为至少32字节的随机字符串
mvn spring-boot:run

REM 终端 3：前端
cd frontend
npm install     REM 首次运行；已存在 node_modules 可跳过
npm run dev
```

访问 <http://localhost:5173>。

### 7. 纯离线演示（不调用真实 API）

先启动 Mock，再启动后端并覆盖 AI 地址：

```bat
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--ai.llm.base-url=http://localhost:9002/llm --ai.ocr.base-url=http://localhost:9001/ocr --ai.api-key=demo-api-key"
```

### 8. 生产部署（prod profile）

```bat
cd backend
set SPRING_PROFILES_ACTIVE=prod
set JWT_SECRET=至少32字节随机密钥
set AES_KEY=至少16字节的生产AES密钥
set DB_USERNAME=最小权限数据库账号
set DB_PASSWORD=非默认数据库口令
java -jar target/escort-1.0.0-SNAPSHOT.jar
```

- prod 会拒绝启动：未注入 `JWT_SECRET`、`AES_KEY`、`DB_PASSWORD`，或 `DB_PASSWORD=root`、`AES_KEY=escort-aes-key-2026-demo`、JWT 仍为默认/过短。
- **AES_KEY 必须与库中已有密文匹配**；更换密钥前需先重加密 `elderly_profile.id_card/address`、`escort_profile.id_card` 等敏感字段。
- 接口文档默认关闭（`/doc.html`、`/v3/api-docs` → 404）；本地调试可设置 `API_DOCS_ENABLED=true`。
- 登录限流默认：同一 IP+账号 5 分钟内连续失败 5 次锁定 15 分钟；可通过 `security.login.max-failures/window-minutes/lock-minutes` 调整。部署在反向代理后时，必须把代理出口 IP 配置到 `security.login.trusted-proxies`（环境变量 `LOGIN_TRUSTED_PROXIES`），否则不会信任 `X-Forwarded-For`。
- 所有响应带 `X-Content-Type-Options/X-Frame-Options/Referrer-Policy/Permissions-Policy/CSP` 安全头。

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
- 上传文件统一存放 `backend/upload/`；前端通过 `/api/file/signed-url` 获取 5 分钟短期签名 URL，再由 `/api/file/download` 加载，URL 中不携带登录 JWT。`/upload/**` 直链仍需登录并按业务归属授权。
- 上传内容必须同时通过扩展名、文件头魔数和图片解码校验；允许 jpg/jpeg/png/gif/bmp/pdf，最大 10MB。
- AI 生成的 HTML 在前端统一经 DOMPurify 白名单消毒后再渲染。
- 数据库脚本只增不改；旧库升级新增独立脚本编号（当前至 `07-add-order-remark.sql`）。
