@echo off
chcp 936 >nul
setlocal EnableDelayedExpansion

REM ---------- JWT 密钥 ----------
REM 安全要求：后端在未配置 JWT_SECRET 时拒绝启动。
REM 本机演示未设置时自动生成本次运行专用的随机密钥（重启后旧令牌失效）。
if not defined JWT_SECRET (
    for /f "delims=" %%i in ('powershell -NoProfile -Command "[guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')"') do set JWT_SECRET=%%i
    echo [SECURITY] 未检测到 JWT_SECRET，已生成本次运行专用的随机密钥
)
REM ============================================
REM AI赋能陪诊助医服务系统 - 一键启动
REM 用法：双击本文件
REM 前置：JDK 17+ / Maven 3.8+ / Node.js 18+ / MySQL 8.0
REM 可选：环境变量 AI_API_KEY（未设置时自动使用本地 Mock 离线演示）
REM ============================================

cd /d "%~dp0"

echo ============================================
echo   AI赋能陪诊助医服务系统 - 一键启动
echo ============================================

REM ---------- AI 模式 ----------
if defined AI_API_KEY (
    set AI_LLM_BASE_URL=https://api.deepseek.com/v1
    echo [AI] 已检测到 AI_API_KEY，使用 DeepSeek 真实 API
) else (
    set AI_LLM_BASE_URL=http://localhost:9002/llm
    echo [AI] 未检测到 AI_API_KEY，使用本地 Mock 离线模式
)
set AI_OCR_BASE_URL=http://localhost:9001/ocr
set MAVEN_OPTS=-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false

REM ---------- 1. MySQL ----------
netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [1/4] MySQL 已运行
) else if exist "D:\MySQL\mysql-8.0.40-winx64\bin\mysqld.exe" (
    echo [1/4] 正在启动 MySQL...
    start "MySQL" "D:\MySQL\mysql-8.0.40-winx64\bin\mysqld.exe" --defaults-file=D:\MySQL\my.ini
    ping -n 5 127.0.0.1 >nul
) else (
    echo [1/4] [警告] MySQL 未运行，且未找到 D:\MySQL 便携版，请手动启动
)

REM ---------- 2. Mock AI（OCR + LLM 兜底） ----------
netstat -ano | findstr ":9001 " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [2/4] Mock AI 已运行
) else (
    echo [2/4] 正在启动 Mock AI...
    start "Mock AI" /D "%~dp0ai-service" cmd /k "node server.js"
    ping -n 3 127.0.0.1 >nul
)

REM ---------- 3. 后端 ----------
netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [3/4] 后端已运行
) else (
    echo [3/4] 正在启动后端...
    start "Backend" /D "%~dp0backend" cmd /k "mvn spring-boot:run"
)

REM ---------- 4. 前端 ----------
netstat -ano | findstr ":5173 " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [4/4] 前端已运行
) else (
    echo [4/4] 正在启动前端...
    start "Frontend" /D "%~dp0frontend" cmd /k "npm run dev"
)

echo 等待服务启动...
ping -n 10 127.0.0.1 >nul
start "" http://localhost:5173/
echo 启动完成。如果页面暂时打不开，等待几秒后刷新即可。
pause
exit /b 0
