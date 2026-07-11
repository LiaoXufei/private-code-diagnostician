@echo off
setlocal
set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%back-end"
set "FRONTEND_DIR=%ROOT%frontend"
set "BACKEND_PORT=9999"
set "JAVA_HOME=C:\Users\fei\Desktop\DevelopTools\Java\jdk\openjdk-21.0.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"
if not "%~1"=="" set "BACKEND_PORT=%~1"
where node >nul 2>nul || (echo Node.js not found & exit /b 1)
if not exist "%FRONTEND_DIR%\node_modules" (
  pushd "%FRONTEND_DIR%" && call npm install || (echo npm install failed & exit /b 1) & popd
)

REM ---------- 启动后端 ----------
if exist "%BACKEND_DIR%\mvnw.cmd" (
  start "Backend" cmd /k pushd "%BACKEND_DIR%" ^&^& call mvnw.cmd -q clean -DskipTests spring-boot:run -Dspring-boot.run.arguments=--server.port=%BACKEND_PORT%
) else (
  where mvn >nul 2>nul && (
    start "Backend" cmd /k pushd "%BACKEND_DIR%" ^&^& mvn -q clean -DskipTests spring-boot:run -Dspring-boot.run.arguments=--server.port=%BACKEND_PORT%
  ) || (
    echo Maven not found. Install Maven or add mvnw.cmd.
    exit /b 1
  )
)

REM ---------- 等待后端就绪后再启动前端 ----------
echo Waiting for backend http://localhost:%BACKEND_PORT% to be ready...
powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%wait-backend.ps1" -Port %BACKEND_PORT% -TimeoutSeconds 120
set "BE_RC=%errorlevel%"
if not "%BE_RC%"=="0" (
  echo [WARN] Backend not ready, starting frontend anyway. Refresh the page once backend finishes starting.
) else (
  echo Backend is ready, starting frontend...
)

REM ---------- 启动前端 ----------
start "Frontend" cmd /k pushd "%FRONTEND_DIR%" ^&^& npm run dev
echo Backend: http://localhost:%BACKEND_PORT%
echo Frontend: http://localhost:3000
exit /b 0
