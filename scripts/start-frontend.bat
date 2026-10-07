@echo off
chcp 65001 >nul
setlocal

echo ============================================================
echo   Express Station System - start frontend (Vue 3 + Vite)
echo ============================================================
echo.

set "FRONTEND_DIR=%~dp0..\frontend"
if not exist "%FRONTEND_DIR%\package.json" (
    echo [ERROR] cannot find %FRONTEND_DIR%\package.json
    pause
    exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
    echo [ERROR] npm not found. Please install Node.js 18+ and add it to PATH.
    pause
    exit /b 1
)

cd /d "%FRONTEND_DIR%"

if not exist "node_modules" (
    echo [1/2] installing dependencies, this may take a few minutes ...
    call npm install
    if errorlevel 1 (
        echo [ERROR] npm install failed.
        pause
        exit /b 1
    )
) else (
    echo [1/2] dependencies already installed, skip npm install.
)

echo [2/2] starting dev server on http://localhost:5173
echo       API requests under /api are proxied to http://localhost:8080
echo       Press Ctrl+C to stop.
echo.

call npm run dev

echo.
echo Frontend stopped.
pause
