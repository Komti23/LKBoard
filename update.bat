@echo off
chcp 65001 >nul
title LKBoard - Очистка и пересборка

if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\ms-21.0.11" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.11"
    )
)

echo ==============================================
echo        LKBoard Clean & Rebuild
echo ==============================================
echo Очистка кэша сборки...
call gradlew.bat clean

echo Компиляция проекта...
call gradlew.bat compileKotlinJvm

if %ERRORLEVEL% equ 0 (
    echo.
    echo [УСПЕХ] Проект успешно пересобран!
) else (
    echo.
    echo [ОШИБКА] Ошибка компиляции.
)

echo.
pause
