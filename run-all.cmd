@echo off
setlocal
set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"
set "BACKEND_PORT=9999"
if not "%~1"=="" set "BACKEND_PORT=%~1"

call :ensure_node
if errorlevel 1 goto :error

rem Install dependencies if missing
if not exist "%BACKEND_DIR%\node_modules" (
  pushd "%BACKEND_DIR%"
  call npm install || goto :error
  popd
)
if not exist "%FRONTEND_DIR%\node_modules" (
  pushd "%FRONTEND_DIR%"
  call npm install || goto :error
  popd
)

rem Launch backend and frontend in separate windows (escape && for nested cmd)
start "Backend" cmd /k pushd "%BACKEND_DIR%" ^&^& set "PORT=%BACKEND_PORT%" ^&^& npm run dev
start "Frontend" cmd /k pushd "%FRONTEND_DIR%" ^&^& npm run dev

echo Backend port: %BACKEND_PORT%
echo Frontend: http://localhost:3000
goto :eof

:ensure_node
node -v >nul 2>&1
if errorlevel 1 (
  echo Node.js not found. Please install Node.js 18+.
  exit /b 1
)
exit /b 0

:error
echo Script failed.
pause
exit /b 1
