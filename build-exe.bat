@echo off
chcp 65001 >nul
title LKBoard - Сборка Windows .exe

if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\ms-21.0.11" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.11"
    )
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ==============================================
echo        LKBoard Windows .exe Builder
echo ==============================================
echo JAVA_HOME: %JAVA_HOME%
echo.
echo Сборка дистрибутива со встроенным JDK...
echo Пожалуйста, подождите...
echo.

call gradlew.bat packageExe

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo [УСПЕХ] Сборка .exe успешно завершена!
    echo Файлы находятся в папке: build\compose\binaries\main\exe
    echo ========================================================
    if exist "build\compose\binaries\main\exe" (
        explorer.exe "build\compose\binaries\main\exe"
    )
) else (
    echo.
    echo [ОШИБКА] Произошла ошибка при сборке дистрибутива.
)

echo.
pause
