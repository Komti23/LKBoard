@echo off
chcp 65001 >nul
title LKBoard - Запуск тестов

if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\ms-21.0.11" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.11"
    )
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ==============================================
echo            LKBoard Test Runner
echo ==============================================
echo JAVA_HOME: %JAVA_HOME%
echo.
echo Запуск юнит-тестов (jvmTest)...
echo.

call gradlew.bat jvmTest

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==============================================
    echo [УСПЕХ] Все тесты пройдены без ошибок!
    echo Отчет о тестах: build\reports\tests\jvmTest\index.html
    echo ==============================================
) else (
    echo.
    echo [ОШИБКА] Обнаружены ошибки при выполнении тестов.
)

echo.
pause
