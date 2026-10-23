@echo off
chcp 65001 >nul
title LKBoard - Запуск приложения

if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\ms-21.0.11" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.11"
    )
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ==============================================
echo           LKBoard Desktop Launcher
echo ==============================================
echo JAVA_HOME: %JAVA_HOME%
echo.
echo Запуск приложения...
echo.

call gradlew.bat run

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ОШИБКА] Приложение завершилось с кодом ошибки %ERRORLEVEL%.
    pause
)
