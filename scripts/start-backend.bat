@echo off
chcp 65001 >nul
setlocal

echo ============================================================
echo   Express Station System - start backend (Spring Boot)
echo ============================================================
echo.

set "BACKEND_DIR=%~dp0..\backend"
if not exist "%BACKEND_DIR%\pom.xml" (
    echo [ERROR] cannot find %BACKEND_DIR%\pom.xml
    pause
    exit /b 1
)

rem ---------- locate JDK 17 ----------
if not defined JAVA_HOME (
    for %%P in (
        "D:\major\tool\jdk-17.0.2"
        "C:\Program Files\Java\jdk-17"
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9-hotspot"
    ) do (
        if exist %%P\bin\java.exe set "JAVA_HOME=%%~P"
    )
)
if not defined JAVA_HOME (
    echo [WARN] JAVA_HOME is not set. Maven will use the java on PATH.
    echo        Make sure it is JDK 17 or newer.
) else (
    echo [OK] JAVA_HOME = %JAVA_HOME%
)

rem ---------- locate maven ----------
set "MVN_CMD="
where mvn >nul 2>nul
if not errorlevel 1 set "MVN_CMD=mvn"
if not defined MVN_CMD (
    rem IntelliJ IDEA bundles a Maven distribution, reuse it when present
    for %%P in (
        "D:\idea\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd"
        "C:\Program Files\JetBrains\IntelliJ IDEA\plugins\maven\lib\maven3\bin\mvn.cmd"
    ) do (
        if exist %%P set "MVN_CMD=%%~P"
    )
)
if not defined MVN_CMD (
    echo [ERROR] maven not found. Install Maven and add it to PATH,
    echo         or open the backend folder in IntelliJ IDEA and run DeliveryApplication.
    pause
    exit /b 1
)
echo [OK] maven       : %MVN_CMD%
echo.
echo Starting on http://localhost:8080/api   (API docs: /swagger-ui.html)
echo Press Ctrl+C to stop.
echo.

cd /d "%BACKEND_DIR%"
call "%MVN_CMD%" -B -DskipTests spring-boot:run

echo.
echo Backend stopped.
pause
