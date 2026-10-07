@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ============================================================
echo   Express Station System - Database initialization
echo   (creates database express_station, 12 tables + seed data)
echo ============================================================
echo.

rem ---------- locate mysql client ----------
set "MYSQL_EXE="
for %%P in (
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    "C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe"
    "D:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
) do (
    if exist %%P set "MYSQL_EXE=%%~P"
)
if not defined MYSQL_EXE (
    where mysql >nul 2>nul
    if not errorlevel 1 set "MYSQL_EXE=mysql"
)
if not defined MYSQL_EXE (
    echo [ERROR] mysql.exe not found.
    echo         Please add the MySQL bin directory to PATH, or edit this script.
    echo.
    pause
    exit /b 1
)
echo [OK] mysql client : %MYSQL_EXE%
echo.

rem ---------- credentials ----------
set "DB_USER=root"
set /p "INPUT_USER=MySQL user [root]: "
if not "%INPUT_USER%"=="" set "DB_USER=%INPUT_USER%"
set /p "DB_PASS=MySQL password: "
if "%DB_PASS%"=="" (
    echo [ERROR] password cannot be empty.
    pause
    exit /b 1
)
echo.

rem ---------- run scripts ----------
set "SQL_DIR=%~dp0..\sql"
if not exist "%SQL_DIR%\01_schema.sql" (
    echo [ERROR] cannot find %SQL_DIR%\01_schema.sql
    pause
    exit /b 1
)

echo [1/2] creating database and tables ... (this will DROP the existing express_station database)
"%MYSQL_EXE%" -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 < "%SQL_DIR%\01_schema.sql"
if errorlevel 1 goto fail

echo [2/2] inserting seed data ...
"%MYSQL_EXE%" -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 < "%SQL_DIR%\02_data.sql"
if errorlevel 1 goto fail

echo.
echo ============================================================
echo   DONE. Database express_station is ready.
echo   Demo accounts (password is 123456 for all):
echo     admin    - system administrator
echo     staff01  - station staff  (Xingfu Community Station)
echo     staff02  - station staff  (University Town Station)
echo     user01   - normal user (receiver)
echo     user02   - normal user (receiver)
echo ============================================================
echo.
pause
exit /b 0

:fail
echo.
echo [ERROR] initialization failed. Please check the messages above.
echo         Common causes: wrong password, MySQL service not running,
echo         or the client version is incompatible with the server.
echo.
pause
exit /b 1
